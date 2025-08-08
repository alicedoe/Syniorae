package com.syniorae.data.remote.google

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
 * Client HTTP pour les appels OAuth 2.0 vers Google
 */
class GoogleOAuthHttpClient {

    companion object {
        private const val TAG = "GoogleOAuthHttpClient"
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
        private const val REVOKE_URL = "https://oauth2.googleapis.com/revoke"
    }

    // Client HTTP configuré
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Échange le code d'autorisation contre des tokens
     */
    suspend fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        redirectUri: String,
        codeVerifier: String? = null
    ): TokenResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Échange du code contre des tokens...")

            // Préparer les paramètres
            val params = mutableMapOf(
                "client_id" to clientId,
                "code" to code,
                "grant_type" to "authorization_code",
                "redirect_uri" to redirectUri
            )

            // Ajouter le code verifier si PKCE est utilisé
            if (!codeVerifier.isNullOrBlank()) {
                params["code_verifier"] = codeVerifier
            }

            // Créer le body de la requête
            val formBody = FormBody.Builder().apply {
                params.forEach { (key, value) -> add(key, value) }
            }.build()

            // Créer la requête
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            // Exécuter la requête
            val response = httpClient.newCall(request).execute()

            response.use { resp ->
                val responseBody = resp.body?.string()
                Log.d(TAG, "Réponse token: HTTP ${resp.code}")

                if (resp.isSuccessful && !responseBody.isNullOrBlank()) {
                    parseTokenResponse(responseBody)
                } else {
                    Log.e(TAG, "Erreur lors de l'échange de tokens: ${resp.code} - $responseBody")
                    null
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "Erreur réseau lors de l'échange de tokens", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'échange de tokens", e)
            null
        }
    }

    /**
     * Récupère les informations utilisateur avec le token d'accès
     */
    suspend fun getUserInfo(accessToken: String): UserInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Récupération des informations utilisateur...")

            val request = Request.Builder()
                .url(USERINFO_URL)
                .get()
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            response.use { resp ->
                val responseBody = resp.body?.string()
                Log.d(TAG, "Réponse userinfo: HTTP ${resp.code}")

                if (resp.isSuccessful && !responseBody.isNullOrBlank()) {
                    parseUserInfoResponse(responseBody)
                } else {
                    Log.e(TAG, "Erreur lors de la récupération des infos utilisateur: ${resp.code} - $responseBody")
                    null
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "Erreur réseau lors de la récupération des infos utilisateur", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des infos utilisateur", e)
            null
        }
    }

    /**
     * Rafraîchit le token d'accès
     */
    suspend fun refreshAccessToken(
        refreshToken: String,
        clientId: String
    ): TokenResponse? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Rafraîchissement du token d'accès...")

            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = httpClient.newCall(request).execute()

            response.use { resp ->
                val responseBody = resp.body?.string()
                Log.d(TAG, "Réponse refresh: HTTP ${resp.code}")

                if (resp.isSuccessful && !responseBody.isNullOrBlank()) {
                    parseTokenResponse(responseBody)
                } else {
                    Log.e(TAG, "Erreur lors du refresh: ${resp.code} - $responseBody")
                    null
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du refresh", e)
            null
        }
    }

    /**
     * Révoque un token (déconnexion)
     */
    suspend fun revokeToken(token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Révocation du token...")

            val formBody = FormBody.Builder()
                .add("token", token)
                .build()

            val request = Request.Builder()
                .url(REVOKE_URL)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = httpClient.newCall(request).execute()

            response.use { resp ->
                Log.d(TAG, "Révocation: HTTP ${resp.code}")
                resp.isSuccessful
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la révocation", e)
            false
        }
    }

    /**
     * Parse la réponse JSON des tokens
     */
    private fun parseTokenResponse(json: String): TokenResponse? {
        return try {
            val jsonObj = JSONObject(json)

            TokenResponse(
                accessToken = jsonObj.getString("access_token"),
                refreshToken = jsonObj.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresIn = jsonObj.optLong("expires_in", 3600),
                tokenType = jsonObj.optString("token_type", "Bearer"),
                scope = jsonObj.optString("scope")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing de la réponse token", e)
            null
        }
    }

    /**
     * Parse la réponse JSON des informations utilisateur
     */
    private fun parseUserInfoResponse(json: String): UserInfo? {
        return try {
            val jsonObj = JSONObject(json)

            UserInfo(
                email = jsonObj.getString("email"),
                name = jsonObj.optString("name").takeIf { it.isNotBlank() },
                picture = jsonObj.optString("picture").takeIf { it.isNotBlank() },
                id = jsonObj.optString("id").takeIf { it.isNotBlank() },
                verifiedEmail = jsonObj.optBoolean("verified_email", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing des infos utilisateur", e)
            null
        }
    }

    /**
     * Ferme le client HTTP
     */
    fun close() {
        httpClient.connectionPool.evictAll()
    }
}

/**
 * Réponse des tokens OAuth
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long,
    val tokenType: String,
    val scope: String? = null
)

/**
 * Informations utilisateur Google
 */
data class UserInfo(
    val email: String,
    val name: String?,
    val picture: String?,
    val id: String? = null,
    val verifiedEmail: Boolean = false
)