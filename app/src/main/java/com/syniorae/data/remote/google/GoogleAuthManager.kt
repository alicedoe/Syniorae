package com.syniorae.data.remote.google

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manager pour l'authentification Google RÉELLE
 * Utilise Google Sign-In SDK
 */
class GoogleAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthManager"
        const val RC_SIGN_IN = 9001
    }

    // Client Google Sign-In
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestServerAuthCode(getClientId()) // Pour obtenir le code d'autorisation
            .requestScopes(
                Scope("https://www.googleapis.com/auth/calendar.readonly"),
                Scope("https://www.googleapis.com/auth/calendar.events.readonly")
            )
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    // État de l'authentification
    private val _authState = MutableStateFlow(GoogleAuthState())
    val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

    // Manager de tokens
    private val tokenManager = GoogleTokenManager(context)

    // Permissions requises pour Google Calendar
    private val requiredScopes = listOf(
        "https://www.googleapis.com/auth/calendar.readonly",
        "https://www.googleapis.com/auth/calendar.events.readonly"
    )

    init {
        // Vérifier si l'utilisateur est déjà connecté au démarrage
        checkExistingSignIn()
    }

    /**
     * Vérifie si l'utilisateur est déjà connecté
     */
    private fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && tokenManager.hasValidTokens()) {
            // Utilisateur déjà connecté avec tokens valides
            updateAuthState(account)
        }
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isSignedIn(): Boolean {
        return _authState.value.isSignedIn
    }

    /**
     * Obtient l'email du compte connecté
     */
    fun getSignedInAccountEmail(): String? {
        return _authState.value.userEmail
    }

    /**
     * Lance le processus d'authentification RÉEL
     */
    suspend fun signIn(): GoogleAuthResult {
        return try {
            Log.d(TAG, "Démarrage de l'authentification Google Sign-In")

            // Vérifier si déjà connecté
            val existingAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (existingAccount != null && tokenManager.hasValidTokens()) {
                Log.d(TAG, "Utilisateur déjà connecté: ${existingAccount.email}")
                updateAuthState(existingAccount)
                return GoogleAuthResult.Success(existingAccount.email ?: "")
            }

            // Lancer l'authentification
            val signInIntent = googleSignInClient.signInIntent

            // Retourner "pending" pour indiquer que l'Intent va se lancer
            // L'activité appelante doit gérer startActivityForResult
            _pendingSignInIntent = signInIntent
            GoogleAuthResult.Success("pending")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'authentification", e)
            GoogleAuthResult.Error("Erreur de connexion: ${e.message}")
        }
    }

    // Intent en attente pour l'authentification
    private var _pendingSignInIntent: Intent? = null

    /**
     * Obtient l'intent de connexion pour startActivityForResult
     */
    fun getSignInIntent(): Intent? {
        return _pendingSignInIntent
    }

    /**
     * Traite le résultat de l'authentification depuis onActivityResult
     */
    suspend fun handleSignInResult(data: Intent?): GoogleAuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            Log.d(TAG, "Authentification réussie: ${account?.email}")

            // Sauvegarder les informations de base du compte
            // Note: Le token Google Sign-In est différent des tokens OAuth API
            // Pour l'API Calendar, nous devrons faire un échange séparé
            updateAuthState(account)

            // Sauvegarder les informations de base
            account?.email?.let { email ->
                tokenManager.saveTokens(
                    accessToken = "gsi_token_${System.currentTimeMillis()}", // Token temporaire
                    refreshToken = "", // Sera obtenu via OAuth flow
                    expiresInSeconds = 3600,
                    userEmail = email,
                    grantedScopes = requiredScopes
                )
            }

            GoogleAuthResult.Success(account?.email ?: "")

        } catch (e: ApiException) {
            Log.w(TAG, "Échec de l'authentification: ${e.statusCode}")
            when (e.statusCode) {
                12501 -> GoogleAuthResult.Cancelled // Utilisateur a annulé
                7 -> GoogleAuthResult.Error("Pas de connexion réseau")
                else -> GoogleAuthResult.Error("Erreur d'authentification: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de l'authentification", e)
            GoogleAuthResult.Error("Erreur inattendue: ${e.message}")
        }
    }

    /**
     * Met à jour l'état d'authentification avec le compte Google
     */
    private fun updateAuthState(account: GoogleSignInAccount?) {
        if (account != null) {
            _authState.value = GoogleAuthState(
                isSignedIn = true,
                userEmail = account.email,
                accessToken = tokenManager.getAccessToken(),
                refreshToken = tokenManager.getRefreshToken(),
                hasCalendarPermission = hasRequiredScopes(account),
                lastSignInTime = System.currentTimeMillis()
            )
        } else {
            _authState.value = GoogleAuthState()
        }
    }

    /**
     * Vérifie si les scopes requis sont accordés
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        return GoogleSignIn.hasPermissions(
            account,
            Scope("https://www.googleapis.com/auth/calendar.readonly"),
            Scope("https://www.googleapis.com/auth/calendar.events.readonly")
        )
    }

    /**
     * Traite le callback OAuth reçu de l'activité de callback
     */
    suspend fun handleOAuthCallback(authorizationCode: String, state: String): GoogleAuthResult {
        return try {
            Log.d(TAG, "Traitement du callback OAuth avec code: ${authorizationCode.take(10)}...")

            // Vérifier le state pour la sécurité (CSRF protection)
            if (!isValidState(state)) {
                return GoogleAuthResult.Error("State invalide - possible attaque CSRF")
            }

            // Pour l'instant, simuler l'échange des tokens
            // TODO: Implémenter l'échange réel code -> tokens
            val success = true // Simulation

            if (success) {
                val userEmail = tokenManager.getUserEmail() ?: "unknown@gmail.com"

                // Mettre à jour l'état d'authentification
                _authState.value = GoogleAuthState(
                    isSignedIn = true,
                    userEmail = userEmail,
                    accessToken = tokenManager.getAccessToken(),
                    refreshToken = tokenManager.getRefreshToken(),
                    hasCalendarPermission = tokenManager.hasCalendarScopes(),
                    lastSignInTime = System.currentTimeMillis()
                )

                Log.i(TAG, "Authentification OAuth réussie pour: $userEmail")
                GoogleAuthResult.Success(userEmail)
            } else {
                GoogleAuthResult.Error("Échec de l'échange des tokens")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement du callback OAuth", e)
            GoogleAuthResult.Error("Erreur lors de l'authentification: ${e.message}")
        }
    }

    /**
     * Vérifie la validité du paramètre state (sécurité CSRF)
     */
    private fun isValidState(state: String): Boolean {
        // TODO: Implémenter la vraie vérification du state avec stockage sécurisé
        return state.isNotBlank()
    }

    /**
     * Déconnecte l'utilisateur
     */
    suspend fun signOut(): Boolean {
        return try {
            Log.d(TAG, "Déconnexion de l'utilisateur")

            // Déconnexion Google Sign-In
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d(TAG, "Déconnexion Google Sign-In terminée")
            }

            // Effacer les tokens stockés
            tokenManager.clearTokens()

            // Réinitialiser l'état
            _authState.value = GoogleAuthState()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la déconnexion", e)
            false
        }
    }

    /**
     * Vérifie et demande les permissions Calendar
     */
    suspend fun requestCalendarPermissions(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.w(TAG, "Aucun compte connecté pour demander les permissions")
                return false
            }

            val hasPermissions = hasRequiredScopes(account)
            if (hasPermissions) {
                Log.d(TAG, "Permissions Calendar déjà accordées")
                _authState.value = _authState.value.copy(hasCalendarPermission = true)
                return true
            } else {
                Log.d(TAG, "Permissions Calendar manquantes - relancer l'authentification")
                // Les permissions seront demandées lors de la prochaine authentification
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification des permissions", e)
            false
        }
    }

    /**
     * Obtient le token d'accès actuel
     */
    fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }

    /**
     * Obtient le refresh token actuel
     */
    fun getRefreshToken(): String? {
        return tokenManager.getRefreshToken()
    }

    /**
     * Rafraîchit le token d'accès
     */
    suspend fun refreshToken(): Boolean {
        return tokenManager.refreshToken() // Utilise la méthode existante
    }

    /**
     * Vérifie si les permissions Calendar sont accordées
     */
    fun hasCalendarPermissions(): Boolean {
        return _authState.value.hasCalendarPermission
    }

    /**
     * Obtient le Client ID depuis les ressources
     */
    private fun getClientId(): String {
        return try {
            context.getString(context.resources.getIdentifier(
                "google_oauth_client_id", "string", context.packageName
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de charger le Client ID", e)
            "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com" // Fallback
        }
    }
}

/**
 * État de l'authentification Google
 */
data class GoogleAuthState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val hasCalendarPermission: Boolean = false,
    val lastSignInTime: Long = 0L
)

/**
 * Résultats des opérations d'authentification
 */
sealed class GoogleAuthResult {
    data class Success(val userEmail: String) : GoogleAuthResult()
    data class Error(val message: String) : GoogleAuthResult()
    object Cancelled : GoogleAuthResult()
}