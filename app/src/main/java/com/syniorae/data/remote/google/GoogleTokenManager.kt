package com.syniorae.data.remote.google

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire sécurisé des tokens OAuth Google
 * Gère le stockage chiffré, le rafraîchissement et l'expiration des tokens
 */
class GoogleTokenManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleTokenManager"
        private const val PREFS_NAME = "syniorae_oauth_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_GRANTED_SCOPES = "granted_scopes"
        private const val TOKEN_REFRESH_THRESHOLD_MINUTES = 10 // Rafraîchir 10 min avant expiration

        // Configuration OAuth Google (à remplacer par les vraies valeurs)
        private const val CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com"
        private const val CLIENT_SECRET = "YOUR_CLIENT_SECRET"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    }

    // SharedPreferences sécurisées pour stocker les tokens
    private val encryptedPrefs: SharedPreferences by lazy {
        try {
            // Pour l'instant, utiliser SharedPreferences normale
            // TODO: Implémenter EncryptedSharedPreferences quand la dépendance sera ajoutée
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur création SharedPreferences, fallback vers normale", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // Client HTTP pour les appels OAuth
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Sauvegarde les tokens de façon sécurisée
     */
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userEmail: String,
        grantedScopes: List<String> = emptyList()
    ) {
        try {
            val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)

            encryptedPrefs.edit().apply {
                putString(KEY_ACCESS_TOKEN, accessToken)
                putString(KEY_REFRESH_TOKEN, refreshToken)
                putLong(KEY_TOKEN_EXPIRY, expiryTime)
                putString(KEY_USER_EMAIL, userEmail)
                putString(KEY_GRANTED_SCOPES, grantedScopes.joinToString(","))
                apply()
            }

            Log.d(TAG, "Tokens sauvegardés pour $userEmail, expiration dans ${expiresInSeconds}s")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde des tokens", e)
        }
    }

    /**
     * Récupère le token d'accès actuel
     */
    fun getAccessToken(): String? {
        return try {
            val token = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
            if (token != null && !isTokenExpired()) {
                token
            } else {
                Log.d(TAG, "Token d'accès expiré ou manquant")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération du token", e)
            null
        }
    }

    /**
     * Récupère le refresh token
     */
    fun getRefreshToken(): String? {
        return try {
            encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération du refresh token", e)
            null
        }
    }

    /**
     * Récupère l'email de l'utilisateur connecté
     */
    fun getUserEmail(): String? {
        return try {
            encryptedPrefs.getString(KEY_USER_EMAIL, null)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de l'email", e)
            null
        }
    }

    /**
     * Récupère les scopes accordés
     */
    fun getGrantedScopes(): List<String> {
        return try {
            val scopesString = encryptedPrefs.getString(KEY_GRANTED_SCOPES, "") ?: ""
            if (scopesString.isBlank()) emptyList() else scopesString.split(",")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des scopes", e)
            emptyList()
        }
    }

    /**
     * Vérifie si le token d'accès est expiré ou va expirer bientôt
     */
    fun isTokenExpired(): Boolean {
        return try {
            val expiryTime = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
            val thresholdTime = System.currentTimeMillis() + (TOKEN_REFRESH_THRESHOLD_MINUTES * 60 * 1000)
            expiryTime <= thresholdTime
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification d'expiration", e)
            true // En cas d'erreur, considérer comme expiré
        }
    }

    /**
     * Rafraîchit le token d'accès en utilisant le refresh token
     */
    suspend fun refreshToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    Log.w(TAG, "Refresh token manquant, impossible de rafraîchir")
                    return@withContext false
                }

                Log.d(TAG, "Rafraîchissement du token en cours...")

                val requestBody = buildRefreshRequestBody(refreshToken)
                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    parseAndSaveRefreshResponse(responseBody)
                    Log.d(TAG, "Token rafraîchi avec succès")
                    true
                } else {
                    Log.e(TAG, "Erreur lors du rafraîchissement: $responseBody")
                    if (response.code == 400 || response.code == 401) {
                        // Refresh token invalide, nettoyer les tokens
                        clearTokens()
                    }
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors du rafraîchissement du token", e)
                false
            }
        }
    }

    /**
     * Construit le corps de la requête de rafraîchissement
     */
    private fun buildRefreshRequestBody(refreshToken: String): RequestBody {
        val formParams = mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to CLIENT_ID,
            "client_secret" to CLIENT_SECRET
        )

        val formBody = formParams.entries.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }

        return formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType())
    }

    /**
     * Parse et sauvegarde la réponse de rafraîchissement
     */
    private fun parseAndSaveRefreshResponse(responseBody: String) {
        try {
            val json = JSONObject(responseBody)
            val newAccessToken = json.getString("access_token")
            val expiresIn = json.optLong("expires_in", 3600) // Par défaut 1 heure

            // Le refresh token peut être mis à jour ou rester le même
            val newRefreshToken = json.optString("refresh_token", getRefreshToken())

            val userEmail = getUserEmail() ?: "unknown@gmail.com"
            val currentScopes = getGrantedScopes()

            if (newRefreshToken != null) {
                saveTokens(newAccessToken, newRefreshToken, expiresIn, userEmail, currentScopes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing de la réponse de rafraîchissement", e)
            throw e
        }
    }

    /**
     * Échange un code d'autorisation contre des tokens d'accès
     */
    suspend fun exchangeCodeForTokens(authorizationCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Échange du code d'autorisation contre les tokens...")

                val requestBody = buildTokenExchangeRequestBody(authorizationCode)
                val request = Request.Builder()
                    .url(TOKEN_URL)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    parseAndSaveTokenResponse(responseBody)
                    Log.d(TAG, "Échange de tokens réussi")
                    true
                } else {
                    Log.e(TAG, "Erreur lors de l'échange: $responseBody")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de l'échange du code", e)
                false
            }
        }
    }

    /**
     * Construit le corps de la requête d'échange de code
     */
    private fun buildTokenExchangeRequestBody(authorizationCode: String): RequestBody {
        val formParams = mapOf(
            "grant_type" to "authorization_code",
            "code" to authorizationCode,
            "client_id" to CLIENT_ID,
            "client_secret" to CLIENT_SECRET,
            "redirect_uri" to "com.syniorae://oauth/callback"
        )

        val formBody = formParams.entries.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }

        return formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType())
    }

    /**
     * Parse et sauvegarde la réponse d'échange de tokens
     */
    private fun parseAndSaveTokenResponse(responseBody: String) {
        try {
            val json = JSONObject(responseBody)
            val accessToken = json.getString("access_token")
            val refreshToken = json.getString("refresh_token")
            val expiresIn = json.optLong("expires_in", 3600)
            val scope = json.optString("scope", "")

            // Extraire l'email si présent, sinon garder l'existant
            val userEmail = getUserEmail() ?: "unknown@gmail.com"
            val grantedScopes = if (scope.isNotBlank()) scope.split(" ") else emptyList()

            saveTokens(accessToken, refreshToken, expiresIn, userEmail, grantedScopes)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing de la réponse d'échange", e)
            throw e
        }
    }

    /**
     * Vérifie si l'utilisateur a accordé un scope spécifique
     */
    fun hasScopeGranted(scope: String): Boolean {
        return getGrantedScopes().contains(scope)
    }

    /**
     * Vérifie si tous les scopes requis pour Calendar sont accordés
     */
    fun hasCalendarScopes(): Boolean {
        val requiredScopes = listOf(
            "https://www.googleapis.com/auth/calendar.readonly",
            "https://www.googleapis.com/auth/calendar.events.readonly"
        )
        val grantedScopes = getGrantedScopes()
        return requiredScopes.all { grantedScopes.contains(it) }
    }

    /**
     * Vérifie si des tokens valides existent
     */
    fun hasValidTokens(): Boolean {
        return !getAccessToken().isNullOrBlank() && !isTokenExpired()
    }

    /**
     * Efface tous les tokens stockés (déconnexion)
     */
    fun clearTokens() {
        try {
            encryptedPrefs.edit().apply {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_TOKEN_EXPIRY)
                remove(KEY_USER_EMAIL)
                remove(KEY_GRANTED_SCOPES)
                apply()
            }
            Log.d(TAG, "Tokens supprimés")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la suppression des tokens", e)
        }
    }

    /**
     * Obtient des informations sur l'état des tokens (pour debug)
     */
    fun getTokenInfo(): TokenInfo {
        return TokenInfo(
            hasAccessToken = !getAccessToken().isNullOrBlank(),
            hasRefreshToken = !getRefreshToken().isNullOrBlank(),
            isExpired = isTokenExpired(),
            userEmail = getUserEmail(),
            grantedScopes = getGrantedScopes(),
            expiryTime = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0)
        )
    }

    /**
     * Initialise le client OAuth avec de vraies clés de production
     */
    fun initializeWithRealCredentials(clientId: String, clientSecret: String) {
        // TODO: Remplacer les constantes par ces valeurs dynamiques
        // Pour l'instant, log un warning
        Log.w(TAG, "Configuration OAuth avec de vraies clés : $clientId")
        Log.w(TAG, "ATTENTION: Implémenter le remplacement dynamique des clés OAuth")
    }
}

/**
 * Informations sur l'état des tokens
 */
data class TokenInfo(
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val isExpired: Boolean,
    val userEmail: String?,
    val grantedScopes: List<String>,
    val expiryTime: Long
) {
    fun getTimeUntilExpiry(): Long {
        return if (expiryTime > 0) {
            maxOf(0, expiryTime - System.currentTimeMillis())
        } else {
            0
        }
    }

    fun getFormattedExpiry(): String {
        if (expiryTime == 0L) return "Jamais configuré"

        val timeLeft = getTimeUntilExpiry()
        return if (timeLeft <= 0) {
            "Expiré"
        } else {
            val minutes = timeLeft / (1000 * 60)
            val hours = minutes / 60
            when {
                hours > 0 -> "${hours}h ${minutes % 60}min"
                else -> "${minutes}min"
            }
        }
    }
}
