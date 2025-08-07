package com.syniorae.data.remote.google

import android.util.Log
import org.json.JSONObject
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException

/**
 * Types d'erreurs Google catégorisées
 */
sealed class GoogleError(
    open val userMessage: String,
    open val technicalMessage: String,
    open val isRetryable: Boolean
) {
    data class NetworkError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = true
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class AuthenticationError(
        override val userMessage: String,
        override val technicalMessage: String,
        val requiresReauth: Boolean = true,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class PermissionError(
        override val userMessage: String,
        override val technicalMessage: String,
        val missingScopes: List<String>,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class QuotaError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = true
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class RateLimitError(
        override val userMessage: String,
        override val technicalMessage: String,
        val retryAfterSeconds: Int,
        override val isRetryable: Boolean = true
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class ValidationError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class ResourceError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class ConfigurationError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class ServerError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = true
    ) : GoogleError(userMessage, technicalMessage, isRetryable)

    data class UnknownError(
        override val userMessage: String,
        override val technicalMessage: String,
        override val isRetryable: Boolean = false
    ) : GoogleError(userMessage, technicalMessage, isRetryable)
}

/**
 * Contexte dans lequel l'erreur s'est produite
 */
enum class ErrorContext {
    AUTHENTICATION,
    CALENDAR_SYNC,
    CALENDAR_LIST,
    EVENT_FETCH
}

/**
 * Niveaux de gravité des erreurs
 */
enum class ErrorSeverity {
    LOW,    // Erreur mineure, peut être ignorée
    MEDIUM, // Erreur modérée, afficher à l'utilisateur
    HIGH    // Erreur critique, nécessite action immédiate
}

/**
 * Détails d'une erreur Google parsée
 */
private data class ErrorDetails(
    val code: String,
    val message: String,
    val reason: String,
    val domain: String
)

/**
 * Gestionnaire d'erreurs spécifiques aux APIs Google
 * Analyse, catégorise et fournit des messages utilisateur appropriés
 */
object GoogleErrorHandler {

    private const val TAG = "GoogleErrorHandler"

    /**
     * Analyse une exception et retourne un GoogleError approprié
     */
    fun handleException(exception: Exception): GoogleError {
        Log.w(TAG, "Gestion d'exception: ${exception::class.simpleName} - ${exception.message}")

        return when (exception) {
            is UnknownHostException -> {
                GoogleError.NetworkError(
                    userMessage = "Pas de connexion Internet",
                    technicalMessage = "DNS resolution failed",
                    isRetryable = true
                )
            }
            is SocketTimeoutException -> {
                GoogleError.NetworkError(
                    userMessage = "Connexion trop lente, veuillez réessayer",
                    technicalMessage = "Socket timeout",
                    isRetryable = true
                )
            }
            is IOException -> {
                GoogleError.NetworkError(
                    userMessage = "Erreur de connexion",
                    technicalMessage = exception.message ?: "IO Exception",
                    isRetryable = true
                )
            }
            else -> {
                GoogleError.UnknownError(
                    userMessage = "Erreur inattendue",
                    technicalMessage = exception.message ?: "Unknown exception",
                    isRetryable = false
                )
            }
        }
    }

    /**
     * Analyse une réponse HTTP d'erreur de Google et retourne un GoogleError
     */
    fun handleHttpError(httpCode: Int, responseBody: String): GoogleError {
        Log.w(TAG, "Gestion d'erreur HTTP $httpCode: $responseBody")

        return when (httpCode) {
            400 -> handleBadRequest(responseBody)
            401 -> handleUnauthorized(responseBody)
            403 -> handleForbidden(responseBody)
            404 -> handleNotFound(responseBody)
            429 -> handleRateLimit(responseBody)
            500, 502, 503, 504 -> handleServerError(httpCode, responseBody)
            else -> GoogleError.UnknownError(
                userMessage = "Erreur du serveur ($httpCode)",
                technicalMessage = responseBody,
                isRetryable = httpCode >= 500
            )
        }
    }

    /**
     * Gère les erreurs 400 (Bad Request)
     */
    private fun handleBadRequest(responseBody: String): GoogleError {
        val errorDetails = parseGoogleErrorResponse(responseBody)

        return when (errorDetails.reason) {
            "invalid_grant" -> GoogleError.AuthenticationError(
                userMessage = "Session expirée, reconnexion nécessaire",
                technicalMessage = "Invalid or expired refresh token",
                requiresReauth = true
            )
            "invalid_request" -> GoogleError.ConfigurationError(
                userMessage = "Configuration incorrecte",
                technicalMessage = errorDetails.message,
                isRetryable = false
            )
            "invalid_client" -> GoogleError.ConfigurationError(
                userMessage = "Application mal configurée",
                technicalMessage = "Invalid OAuth client configuration",
                isRetryable = false
            )
            else -> GoogleError.ValidationError(
                userMessage = "Requête invalide",
                technicalMessage = errorDetails.message,
                isRetryable = false
            )
        }
    }

    /**
     * Gère les erreurs 401 (Unauthorized)
     */
    private fun handleUnauthorized(responseBody: String): GoogleError {
        val errorDetails = parseGoogleErrorResponse(responseBody)

        return GoogleError.AuthenticationError(
            userMessage = "Authentification requise",
            technicalMessage = errorDetails.message,
            requiresReauth = true
        )
    }

    /**
     * Gère les erreurs 403 (Forbidden)
     */
    private fun handleForbidden(responseBody: String): GoogleError {
        val errorDetails = parseGoogleErrorResponse(responseBody)

        return when (errorDetails.reason) {
            "insufficientPermissions" -> GoogleError.PermissionError(
                userMessage = "Permissions insuffisantes pour accéder au calendrier",
                technicalMessage = "Calendar access not granted",
                missingScopes = listOf("calendar.readonly")
            )
            "dailyLimitExceeded" -> GoogleError.QuotaError(
                userMessage = "Limite quotidienne dépassée, réessayez demain",
                technicalMessage = "Daily quota exceeded",
                isRetryable = false
            )
            "userRateLimitExceeded" -> GoogleError.QuotaError(
                userMessage = "Trop de requêtes, veuillez patienter",
                technicalMessage = "User rate limit exceeded",
                isRetryable = true
            )
            "calendarUsageLimitsExceeded" -> GoogleError.QuotaError(
                userMessage = "Limite d'utilisation du calendrier atteinte",
                technicalMessage = "Calendar usage limits exceeded",
                isRetryable = true
            )
            else -> GoogleError.PermissionError(
                userMessage = "Accès refusé",
                technicalMessage = errorDetails.message,
                missingScopes = emptyList()
            )
        }
    }

    /**
     * Gère les erreurs 404 (Not Found)
     */
    private fun handleNotFound(responseBody: String): GoogleError {
        return GoogleError.ResourceError(
            userMessage = "Calendrier introuvable",
            technicalMessage = "Calendar or resource not found",
            isRetryable = false
        )
    }

    /**
     * Gère les erreurs 429 (Rate Limiting)
     */
    private fun handleRateLimit(responseBody: String): GoogleError {
        val retryAfter = extractRetryAfterFromResponse(responseBody)

        return GoogleError.RateLimitError(
            userMessage = "Trop de requêtes, veuillez patienter ${retryAfter}s",
            technicalMessage = "Rate limit exceeded",
            retryAfterSeconds = retryAfter,
            isRetryable = true
        )
    }

    /**
     * Gère les erreurs serveur 5xx
     */
    private fun handleServerError(httpCode: Int, responseBody: String): GoogleError {
        val serviceName = when {
            responseBody.contains("calendar", ignoreCase = true) -> "calendrier"
            responseBody.contains("oauth", ignoreCase = true) -> "authentification"
            else -> "serveur"
        }

        return GoogleError.ServerError(
            userMessage = "Service $serviceName temporairement indisponible",
            technicalMessage = "HTTP $httpCode: $responseBody",
            isRetryable = true
        )
    }

    /**
     * Parse une réponse d'erreur JSON de Google
     */
    private fun parseGoogleErrorResponse(responseBody: String): ErrorDetails {
        return try {
            val json = JSONObject(responseBody)

            // Format standard Google API error
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                ErrorDetails(
                    code = error.optString("code", "unknown"),
                    message = error.optString("message", "Unknown error"),
                    reason = error.optString("reason", "unknown"),
                    domain = error.optString("domain", "unknown")
                )
            }
            // Format OAuth error
            else if (json.has("error_description")) {
                ErrorDetails(
                    code = json.optString("error", "unknown"),
                    message = json.optString("error_description", "Unknown error"),
                    reason = json.optString("error", "unknown"),
                    domain = "oauth"
                )
            }
            // Format simple
            else {
                ErrorDetails(
                    code = "unknown",
                    message = responseBody.take(200), // Limiter la longueur
                    reason = "unknown",
                    domain = "unknown"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur parsing réponse d'erreur JSON", e)
            ErrorDetails(
                code = "parse_error",
                message = "Unable to parse error response",
                reason = "parse_error",
                domain = "client"
            )
        }
    }

    /**
     * Extrait le délai de retry d'une réponse de rate limiting
     */
    private fun extractRetryAfterFromResponse(responseBody: String): Int {
        return try {
            val json = JSONObject(responseBody)
            json.optInt("retryAfter", 60) // Défaut: 1 minute
        } catch (e: Exception) {
            60 // Défaut: 1 minute
        }
    }

    /**
     * Détermine si une erreur nécessite une nouvelle authentification
     */
    fun requiresReauthentication(error: GoogleError): Boolean {
        return when (error) {
            is GoogleError.AuthenticationError -> error.requiresReauth
            else -> false
        }
    }

    /**
     * Détermine si une erreur est temporaire et peut être retentée
     */
    fun isRetryable(error: GoogleError): Boolean {
        return error.isRetryable
    }

    /**
     * Obtient le délai recommandé avant retry (en millisecondes)
     */
    fun getRetryDelayMs(error: GoogleError, attemptNumber: Int): Long {
        return when (error) {
            is GoogleError.RateLimitError -> error.retryAfterSeconds * 1000L
            is GoogleError.NetworkError -> minOf(30000, 1000L * (1 shl attemptNumber)) // Backoff exponentiel
            is GoogleError.ServerError -> minOf(60000, 2000L * attemptNumber) // Backoff linéaire
            else -> 5000L // 5 secondes par défaut
        }
    }

    /**
     * Génère un message d'erreur contextuel selon l'action en cours
     */
    fun getContextualMessage(error: GoogleError, context: ErrorContext): String {
        val baseMessage = error.userMessage

        return when (context) {
            ErrorContext.AUTHENTICATION -> when (error) {
                is GoogleError.NetworkError -> "Impossible de se connecter à Google. Vérifiez votre connexion Internet."
                is GoogleError.AuthenticationError -> "Échec de la connexion Google. Veuillez réessayer."
                is GoogleError.PermissionError -> "Permissions refusées. L'application a besoin d'accéder à vos calendriers."
                else -> "Erreur lors de la connexion Google: $baseMessage"
            }
            ErrorContext.CALENDAR_SYNC -> when (error) {
                is GoogleError.NetworkError -> "Impossible de synchroniser. Vérifiez votre connexion."
                is GoogleError.AuthenticationError -> "Session expirée. Reconnectez-vous dans les paramètres."
                is GoogleError.PermissionError -> "Accès au calendrier refusé. Vérifiez les permissions."
                is GoogleError.QuotaError -> "Limite de synchronisation atteinte. Réessayez plus tard."
                else -> "Erreur de synchronisation: $baseMessage"
            }
            ErrorContext.CALENDAR_LIST -> when (error) {
                is GoogleError.NetworkError -> "Impossible de récupérer vos calendriers."
                is GoogleError.PermissionError -> "Accès aux calendriers refusé."
                else -> "Erreur lors du chargement des calendriers: $baseMessage"
            }
            ErrorContext.EVENT_FETCH -> when (error) {
                is GoogleError.ResourceError -> "Calendrier non trouvé ou supprimé."
                is GoogleError.PermissionError -> "Plus d'accès à ce calendrier."
                else -> "Erreur lors du chargement des événements: $baseMessage"
            }
        }
    }

    /**
     * Log une erreur avec le niveau approprié
     */
    fun logError(error: GoogleError, context: ErrorContext) {
        val message = "Erreur $context: ${error.technicalMessage}"

        when (error) {
            is GoogleError.NetworkError -> Log.w(TAG, message)
            is GoogleError.AuthenticationError -> Log.w(TAG, message)
            is GoogleError.PermissionError -> Log.w(TAG, message)
            is GoogleError.QuotaError -> Log.w(TAG, message)
            is GoogleError.RateLimitError -> Log.i(TAG, message)
            is GoogleError.ValidationError -> Log.w(TAG, message)
            is GoogleError.ResourceError -> Log.w(TAG, message)
            is GoogleError.ConfigurationError -> Log.e(TAG, message)
            is GoogleError.ServerError -> Log.w(TAG, message)
            is GoogleError.UnknownError -> Log.e(TAG, message)
        }
    }

    /**
     * Analyse une chaîne d'erreur générale et suggère des actions
     */
    fun analyzeErrorAndSuggestActions(error: GoogleError): List<String> {
        val suggestions = mutableListOf<String>()

        when (error) {
            is GoogleError.NetworkError -> {
                suggestions.add("Vérifiez votre connexion Internet")
                suggestions.add("Réessayez dans quelques minutes")
                if (error.technicalMessage.contains("timeout", ignoreCase = true)) {
                    suggestions.add("Essayez avec une meilleure connexion")
                }
            }
            is GoogleError.AuthenticationError -> {
                if (error.requiresReauth) {
                    suggestions.add("Reconnectez-vous à votre compte Google")
                    suggestions.add("Vérifiez les paramètres de l'application")
                }
                suggestions.add("Redémarrez l'application")
            }
            is GoogleError.PermissionError -> {
                suggestions.add("Vérifiez les permissions dans les paramètres Google")
                suggestions.add("Reconnectez-vous pour accorder à nouveau les permissions")
                if (error.missingScopes.contains("calendar.readonly")) {
                    suggestions.add("Autorisez l'accès aux calendriers lors de la connexion")
                }
            }
            is GoogleError.QuotaError -> {
                suggestions.add("Attendez avant de réessayer")
                suggestions.add("Réduisez la fréquence de synchronisation")
                if (error.userMessage.contains("quotidienne", ignoreCase = true)) {
                    suggestions.add("Réessayez demain")
                }
            }
            is GoogleError.RateLimitError -> {
                suggestions.add("Patientez ${error.retryAfterSeconds} secondes")
                suggestions.add("Évitez les synchronisations trop fréquentes")
            }
            is GoogleError.ValidationError -> {
                suggestions.add("Vérifiez votre configuration")
                suggestions.add("Reconfigurer le widget")
            }
            is GoogleError.ResourceError -> {
                suggestions.add("Vérifiez que le calendrier existe toujours")
                suggestions.add("Reconfigurer le widget si nécessaire")
            }
            is GoogleError.ConfigurationError -> {
                suggestions.add("Contactez le support de l'application")
                suggestions.add("Réinstallez l'application si le problème persiste")
            }
            is GoogleError.ServerError -> {
                suggestions.add("Réessayez dans quelques minutes")
                suggestions.add("Le problème est temporaire côté Google")
            }
            is GoogleError.UnknownError -> {
                suggestions.add("Redémarrez l'application")
                suggestions.add("Vérifiez votre connexion Internet")
                suggestions.add("Contactez le support si le problème persiste")
            }
        }

        return suggestions
    }

    /**
     * Détermine la gravité d'une erreur pour les notifications
     */
    fun getErrorSeverity(error: GoogleError): ErrorSeverity {
        return when (error) {
            is GoogleError.NetworkError -> ErrorSeverity.LOW
            is GoogleError.RateLimitError -> ErrorSeverity.LOW
            is GoogleError.QuotaError -> ErrorSeverity.MEDIUM
            is GoogleError.AuthenticationError -> if (error.requiresReauth) ErrorSeverity.HIGH else ErrorSeverity.MEDIUM
            is GoogleError.PermissionError -> ErrorSeverity.HIGH
            is GoogleError.ConfigurationError -> ErrorSeverity.HIGH
            is GoogleError.ServerError -> ErrorSeverity.MEDIUM
            is GoogleError.ResourceError -> ErrorSeverity.MEDIUM
            is GoogleError.ValidationError -> ErrorSeverity.MEDIUM
            is GoogleError.UnknownError -> ErrorSeverity.HIGH
        }
    }

    /**
     * Génère un résumé technique pour les logs détaillés
     */
    fun generateTechnicalSummary(error: GoogleError, context: ErrorContext, attemptNumber: Int = 1): String {
        val summary = StringBuilder()
        summary.append("=== GOOGLE API ERROR SUMMARY ===\n")
        summary.append("Context: $context\n")
        summary.append("Attempt: $attemptNumber\n")
        summary.append("Error Type: ${error::class.simpleName}\n")
        summary.append("User Message: ${error.userMessage}\n")
        summary.append("Technical: ${error.technicalMessage}\n")
        summary.append("Retryable: ${error.isRetryable}\n")

        when (error) {
            is GoogleError.AuthenticationError -> {
                summary.append("Requires Reauth: ${error.requiresReauth}\n")
            }
            is GoogleError.PermissionError -> {
                summary.append("Missing Scopes: ${error.missingScopes.joinToString(", ")}\n")
            }
            is GoogleError.RateLimitError -> {
                summary.append("Retry After: ${error.retryAfterSeconds}s\n")
            }
            else -> { /* Pas d'infos supplémentaires */ }
        }

        summary.append("Severity: ${getErrorSeverity(error)}\n")
        summary.append("Next Retry Delay: ${getRetryDelayMs(error, attemptNumber)}ms\n")
        summary.append("=== END SUMMARY ===")

        return summary.toString()
    }
}