package com.syniorae.domain.exceptions

/**
 * Exception de base pour l'application SyniOrae
 */
open class SyniOraeException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exceptions liées à l'authentification Google
 */
class AuthenticationException(
    message: String,
    cause: Throwable? = null
) : SyniOraeException(message, cause) {

    companion object {
        fun googleConnectionFailed(cause: Throwable? = null) =
            AuthenticationException("Impossible de se connecter à Google", cause)

        fun tokenExpired() =
            AuthenticationException("Token d'authentification expiré")

        fun permissionDenied() =
            AuthenticationException("Permissions insuffisantes")

        fun accountNotFound() =
            AuthenticationException("Compte Google non trouvé")
    }
}

/**
 * Exceptions liées à la synchronisation
 */
class SyncException(
    message: String,
    cause: Throwable? = null
) : SyniOraeException(message, cause) {

    companion object {
        fun networkError(cause: Throwable? = null) =
            SyncException("Erreur réseau lors de la synchronisation", cause)

        fun calendarNotFound(calendarId: String) =
            SyncException("Calendrier non trouvé: $calendarId")

        fun quotaExceeded() =
            SyncException("Quota d'API dépassé, réessayez plus tard")

        fun dataCorrupted() =
            SyncException("Données corrompues détectées")

        fun syncInProgress() =
            SyncException("Une synchronisation est déjà en cours")

        fun tooManyRetries() =
            SyncException("Trop de tentatives de synchronisation échouées")
    }
}

/**
 * Exceptions liées à la configuration
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : SyniOraeException(message, cause) {

    companion object {
        fun invalidConfig(field: String) =
            ConfigurationException("Configuration invalide pour le champ: $field")

        fun missingRequiredField(field: String) =
            ConfigurationException("Champ requis manquant: $field")

        fun configFileCorrupted() =
            ConfigurationException("Fichier de configuration corrompu")

        fun unsupportedVersion() =
            ConfigurationException("Version de configuration non supportée")

        fun configurationNotFound(widgetType: String) =
            ConfigurationException("Configuration non trouvée pour le widget: $widgetType")
    }
}

/**
 * Exceptions liées aux widgets
 */
class WidgetException(
    message: String,
    cause: Throwable? = null
) : SyniOraeException(message, cause) {

    companion object {
        fun widgetNotFound(widgetType: String) =
            WidgetException("Widget non trouvé: $widgetType")

        fun widgetNotConfigured(widgetType: String) =
            WidgetException("Widget non configuré: $widgetType")

        fun widgetAlreadyActive(widgetType: String) =
            WidgetException("Widget déjà actif: $widgetType")

        fun configurationInProgress(widgetType: String) =
            WidgetException("Configuration en cours pour le widget: $widgetType")

        fun dependencyMissing(widgetType: String, dependency: String) =
            WidgetException("Dépendance manquante pour $widgetType: $dependency")
    }
}

/**
 * Exceptions liées aux fichiers JSON
 */
class JsonException(
    message: String,
    cause: Throwable? = null
) : SyniOraeException(message, cause) {

    companion object {
        fun fileNotFound(fileName: String) =
            JsonException("Fichier JSON non trouvé: $fileName")

        fun invalidFormat(fileName: String) =
            JsonException("Format JSON invalide: $fileName")

        fun readError(fileName: String, cause: Throwable? = null) =
            JsonException("Erreur lors de la lecture de $fileName", cause)

        fun writeError(fileName: String, cause: Throwable? = null) =
            JsonException("Erreur lors de l'écriture de $fileName", cause)

        fun corruptedData(fileName: String) =
            JsonException("Données corrompues dans $fileName")

        fun validationFailed(fileName: String, errors: List<String>) =
            JsonException("Validation échouée pour $fileName: ${errors.joinToString(", ")}")
    }
}