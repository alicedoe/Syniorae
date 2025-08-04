package com.syniorae.core.constants

/**
 * Constantes globales de l'application
 */
object AppConstants {

    // Informations application
    const val APP_NAME = "SyniOrae"
    const val APP_VERSION = "1.0.0"

    // Préférences
    const val SHARED_PREFS_NAME = "syniorae_prefs"

    // Fichiers JSON
    const val JSON_CONFIG_SUFFIX = "_config.json"
    const val JSON_DATA_SUFFIX = "_data.json"
    const val JSON_ICONS_SUFFIX = "_icons.json"

    // Widgets
    const val WIDGET_CALENDAR = "calendar"

    // Navigation - Durée de l'appui long en millisecondes (1 seconde selon le cahier des charges)
    const val LONG_PRESS_DURATION = 1000L

    // Synchronisation
    const val DEFAULT_SYNC_FREQUENCY_HOURS = 4
    const val MIN_SYNC_FREQUENCY_HOURS = 1
    const val MAX_SYNC_FREQUENCY_HOURS = 24

    // Calendrier
    const val DEFAULT_WEEKS_AHEAD = 4
    const val DEFAULT_MAX_EVENTS = 50
    const val MAX_WEEKS_AHEAD = 12
    const val MAX_EVENTS_LIMIT = 200
}

/**
 * Constantes pour la navigation
 */
object NavigationConstants {
    const val HOME_FRAGMENT = "home"
    const val CONFIG_FRAGMENT = "configuration"
    const val SETTINGS_FRAGMENT = "settings"
}

/**
 * Constantes pour le stockage
 */
object StorageConstants {
    const val JSON_DIR = "syniorae_data"
    const val BACKUP_DIR = "syniorae_backup"
    const val LOGS_DIR = "syniorae_logs"
}