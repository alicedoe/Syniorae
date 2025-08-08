package com.syniorae.presentation.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.syniorae.core.di.DependencyInjection
import kotlinx.coroutines.launch

/**
 * Activité pour gérer les callbacks OAuth 2.0 de Google
 * Version corrigée qui compile
 */
class OAuthCallbackActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OAuthCallbackActivity"
        const val EXTRA_AUTH_RESULT = "auth_result"
        const val EXTRA_USER_EMAIL = "user_email"
        const val EXTRA_ERROR_MESSAGE = "error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "Callback OAuth reçu")

        // Récupérer l'URI de callback
        val uri = intent.data

        if (uri != null) {
            Log.d(TAG, "URI reçue: $uri")

            // Vérifier si c'est une erreur
            val error = uri.getQueryParameter("error")
            if (!error.isNullOrBlank()) {
                handleError(error, uri.getQueryParameter("error_description"))
                return
            }

            // Récupérer le code d'autorisation
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")

            if (!code.isNullOrBlank() && !state.isNullOrBlank()) {
                handleAuthorizationCode(code, state)
            } else {
                handleError("invalid_request", "Code ou state manquant")
            }
        } else {
            handleError("no_data", "Aucune donnée reçue")
        }
    }

    /**
     * Traite le code d'autorisation reçu
     */
    private fun handleAuthorizationCode(code: String, state: String) {
        Log.d(TAG, "Code d'autorisation reçu, traitement...")

        lifecycleScope.launch {
            try {
                // Récupérer le gestionnaire d'authentification
                val authManager = DependencyInjection.getGoogleAuthManager()

                // Traiter le callback OAuth
                val result = authManager.handleOAuthCallback(code, state)

                when (result) {
                    is com.syniorae.data.remote.google.GoogleAuthResult.Success -> {
                        Log.i(TAG, "Authentification réussie: ${result.userEmail}")
                        finishWithSuccess(result.userEmail)
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Error -> {
                        Log.e(TAG, "Erreur d'authentification: ${result.message}")
                        finishWithError(result.message)
                    }
                    is com.syniorae.data.remote.google.GoogleAuthResult.Cancelled -> {
                        Log.w(TAG, "Authentification annulée")
                        finishWithError("Authentification annulée")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du traitement du callback", e)
                finishWithError("Erreur lors du traitement: ${e.message}")
            }
        }
    }

    /**
     * Gère les erreurs OAuth
     */
    private fun handleError(error: String, description: String?) {
        Log.e(TAG, "Erreur OAuth: $error - $description")

        val errorMessage = when (error) {
            "access_denied" -> "Accès refusé. Vous devez accepter les permissions pour continuer."
            "invalid_request" -> "Requête invalide. Veuillez réessayer."
            "invalid_scope" -> "Permissions non supportées."
            "server_error" -> "Erreur serveur Google. Veuillez réessayer plus tard."
            "temporarily_unavailable" -> "Service temporairement indisponible."
            else -> description ?: "Erreur inconnue: $error"
        }

        finishWithError(errorMessage)
    }

    /**
     * Termine l'activité avec succès
     */
    private fun finishWithSuccess(userEmail: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AUTH_RESULT, "success")
            putExtra(EXTRA_USER_EMAIL, userEmail)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Termine l'activité avec une erreur
     */
    private fun finishWithError(errorMessage: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_AUTH_RESULT, "error")
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        setResult(Activity.RESULT_CANCELED, resultIntent)
        finish()
    }
}