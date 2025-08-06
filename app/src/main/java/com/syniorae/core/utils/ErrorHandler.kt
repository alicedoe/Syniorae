package com.syniorae.core.utils

import android.util.Log
import com.syniorae.domain.exceptions.AuthenticationException
import com.syniorae.domain.exceptions.ConfigurationException
import com.syniorae.domain.exceptions.JsonException
import com.syniorae.domain.exceptions.SyncException
import com.syniorae.domain.exceptions.WidgetException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Gestionnaire centralisé pour les erreurs de l'application
 * Convertit les exceptions en messages utilisateur compréhensibles
 */
object ErrorHandler {

    private const val TAG = "ErrorHandler"

    /**
     * Convertit une exception en message utilisateur
     */
    fun getErrorMessage(throwable: Throwable): String {
        Log.e(TAG, "Erreur capturée", throwable)

        return when (throwable) {
            // Exceptions spécifiques à l'application
            is AuthenticationException -> handleAuthenticationError(throwable)
            is SyncException -> handleSyncError(throwable)
            is ConfigurationException -> handleConfigurationError(throwable)
            is WidgetException -> handleWidgetError(throwable)
            is JsonException -> handleJsonError(throwable)

            // Exceptions réseau
            is ConnectException, is UnknownHostException ->
                "Pas de connexion internet. Vérifiez votre réseau."
            is SocketTimeoutException ->
                "Délai d'attente dépassé. Réessayez plus tard."

            // Exceptions génériques
            is IllegalArgumentException ->
                "Paramètre invalide : ${throwable.message}"
            is IllegalStateException ->
                "État invalide de l'application : ${throwable.message}"
            is SecurityException ->
                "Permissions insuffisantes"

            // Exception générique
            else -> throwable.message ?: "Une erreur inattendue s'est produite"
        }
    }

    /**
     * Gère les erreurs d'authentification
     */
    private fun handleAuthenticationError(exception: AuthenticationException): String {
        return when {
            exception.message?.contains("connexion") == true ->
                "Impossible de se connecter à Google. Vérifiez votre connexion."
            exception.message?.contains("token") == true ->
                "Session expirée. Reconnectez-vous à votre compte Google."
            exception.message?.contains("permission") == true ->
                "Autorisations insuffisantes. Acceptez les permissions demandées."
            exception.message?.contains("compte") == true ->
                "Compte Google non trouvé. Vérifiez votre configuration."
            else ->
                "Erreur d'authentification : ${exception.message}"
        }
    }

    /**
     * Gère les erreurs de synchronisation
     */
    private fun handleSyncError(exception: SyncException): String {
        return when {
            exception.message?.contains("réseau") == true ->
                "Problème de connexion lors de la synchronisation."
            exception.message?.contains("calendrier non trouvé") == true ->
                "Le calendrier sélectionné n'existe plus."
            exception.message?.contains("quota") == true ->
                "Quota API dépassé. Réessayez dans quelques minutes."
            exception.message?.contains("corrompues") == true ->
                "Données corrompues détectées. Une réinitialisation peut être nécessaire."
            exception.message?.contains("en cours") == true ->
                "Une synchronisation est déjà en cours."
            exception.message?.contains("tentatives") == true ->
                "Trop d'échecs de synchronisation. Vérifiez votre configuration."
            else ->
                "Erreur de synchronisation : ${exception.message}"
        }
    }

    /**
     * Gère les erreurs de configuration
     */
    private fun handleConfigurationError(exception: ConfigurationException): String {
        return when {
            exception.message?.contains("invalide") == true ->
                "Configuration invalide. Vérifiez vos paramètres."
            exception.message?.contains("manquant") == true ->
                "Configuration incomplète. Certains champs sont requis."
            exception.message?.contains("corrompu") == true ->
                "Fichier de configuration corrompu. Reconfiguration nécessaire."
            exception.message?.contains("version") == true ->
                "Version de configuration non supportée."
            exception.message?.contains("non trouvée") == true ->
                "Configuration non trouvée. Configurez d'abord le widget."
            else ->
                "Erreur de configuration : ${exception.message}"
        }
    }

    /**
     * Gère les erreurs de widgets
     */
    private fun handleWidgetError(exception: WidgetException): String {
        return when {
            exception.message?.contains("non trouvé") == true ->
                "Widget non trouvé."
            exception.message?.contains("non configuré") == true ->
                "Widget non configuré. Configurez-le d'abord."
            exception.message?.contains("déjà actif") == true ->
                "Ce widget est déjà actif."
            exception.message?.contains("en cours") == true ->
                "Configuration en cours pour ce widget."
            exception.message?.contains("dépendance") == true ->
                "Une dépendance est manquante pour ce widget."
            else ->
                "Erreur de widget : ${exception.message}"
        }
    }

    /**
     * Gère les erreurs de fichiers JSON
     */
    private fun handleJsonError(exception: JsonException): String {
        return when {
            exception.message?.contains("non trouvé") == true ->
                "Fichier de données non trouvé."
            exception.message?.contains("format") == true ->
                "Format de données invalide."
            exception.message?.contains("lecture") == true ->
                "Impossible de lire les données."
            exception.message?.contains("écriture") == true ->
                "Impossible de sauvegarder les données."
            exception.message?.contains("corrompues") == true ->
                "Données corrompues détectées."
            exception.message?.contains("validation") == true ->
                "Validation des données échouée."
            else ->
                "Erreur de fichier : ${exception.message}"
        }
    }

    /**
     * Log une erreur avec contexte
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Log un avertissement
     */
    fun logWarning(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * Vérifie si une erreur est critique (nécessite un redémarrage)
     */
    fun isCriticalError(throwable: Throwable): Boolean {
        return when (throwable) {
            is OutOfMemoryError,
            is StackOverflowError -> true
            is ConfigurationException ->
                throwable.message?.contains("corrompu") == true
            is JsonException ->
                throwable.message?.contains("corrompues") == true
            else -> false
        }
    }

    /**
     * Suggère une action de récupération
     */
    fun getRecoveryAction(throwable: Throwable): String? {
        return when (throwable) {
            is AuthenticationException -> "Reconnectez-vous à votre compte Google"
            is SyncException -> when {
                throwable.message?.contains("réseau") == true -> "Vérifiez votre connexion internet"
                throwable.message?.contains("quota") == true -> "Réessayez dans quelques minutes"
                else -> "Réessayez la synchronisation"
            }
            is ConfigurationException -> "Reconfigurez le widget"
            is JsonException -> when {
                throwable.message?.contains("corrompues") == true -> "Réinitialisez la configuration"
                else -> "Redémarrez l'application"
            }
            else -> null
        }
    }
}