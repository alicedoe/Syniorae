package com.syniorae.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.syniorae.core.constants.AppConstants

/**
 * Gestionnaire centralisé pour les préférences utilisateur
 * Simplifie l'accès aux SharedPreferences avec type safety
 */
class PreferencesManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Clés des préférences
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_VERSION = "last_version"
        private const val KEY_TUTORIAL_SHOWN = "tutorial_shown"
        private const val KEY_LANGUAGE_SELECTED = "language_selected"

        // Préférences de synchronisation
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_SYNC_FREQUENCY = "sync_frequency_hours"

        // Préférences d'affichage
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_TIME_FORMAT = "time_format_24h"
        private const val KEY_FONT_SIZE_MULTIPLIER = "font_size_multiplier"

        // Préférences de debug (pour les développeurs)
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_MOCK_DATA = "use_mock_data"
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        AppConstants.SHARED_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // === Préférences générales ===

    var isFirstLaunch: Boolean
        get() = sharedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var lastVersion: String
        get() = sharedPrefs.getString(KEY_LAST_VERSION, "") ?: ""
        set(value) = sharedPrefs.edit().putString(KEY_LAST_VERSION, value).apply()

    var isTutorialShown: Boolean
        get() = sharedPrefs.getBoolean(KEY_TUTORIAL_SHOWN, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_TUTORIAL_SHOWN, value).apply()

    var languageSelected: String?
        get() = sharedPrefs.getString(KEY_LANGUAGE_SELECTED, null)
        set(value) = sharedPrefs.edit().putString(KEY_LANGUAGE_SELECTED, value).apply()

    // === Préférences de synchronisation ===

    var isAutoSyncEnabled: Boolean
        get() = sharedPrefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, value).apply()

    var lastSyncTime: Long
        get() = sharedPrefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = sharedPrefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var syncFrequencyHours: Int
        get() = sharedPrefs.getInt(KEY_SYNC_FREQUENCY, AppConstants.DEFAULT_SYNC_FREQUENCY_HOURS)
        set(value) = sharedPrefs.edit().putInt(KEY_SYNC_FREQUENCY, value).apply()

    // === Préférences d'affichage ===

    var dateFormat: String
        get() = sharedPrefs.getString(KEY_DATE_FORMAT, "EEEE d MMMM yyyy") ?: "EEEE d MMMM yyyy"
        set(value) = sharedPrefs.edit().putString(KEY_DATE_FORMAT, value).apply()

    var is24HourFormat: Boolean
        get() = sharedPrefs.getBoolean(KEY_TIME_FORMAT, true)
        set(value) = sharedPrefs.edit().putBoolean(KEY_TIME_FORMAT, value).apply()

    var fontSizeMultiplier: Float
        get() = sharedPrefs.getFloat(KEY_FONT_SIZE_MULTIPLIER, 1.0f)
        set(value) = sharedPrefs.edit().putFloat(KEY_FONT_SIZE_MULTIPLIER, value.coerceIn(0.8f, 2.0f)).apply()

    // === Préférences de debug ===

    var isDebugMode: Boolean
        get() = sharedPrefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    var useMockData: Boolean
        get() = sharedPrefs.getBoolean(KEY_MOCK_DATA, false)
        set(value) = sharedPrefs.edit().putBoolean(KEY_MOCK_DATA, value).apply()

    // === Méthodes utilitaires ===

    /**
     * Marque le premier lancement comme terminé
     */
    fun markFirstLaunchComplete() {
        isFirstLaunch = false
        lastVersion = AppConstants.APP_VERSION
    }

    /**
     * Vérifie si c'est une nouvelle version
     */
    fun isNewVersion(): Boolean {
        return lastVersion != AppConstants.APP_VERSION
    }

    /**
     * Met à jour la version après migration
     */
    fun updateToCurrentVersion() {
        lastVersion = AppConstants.APP_VERSION
    }

    /**
     * Remet à zéro toutes les préférences (pour reset complet)
     */
    fun resetAllPreferences() {
        sharedPrefs.edit().clear().apply()
    }

    /**
     * Remet à zéro seulement les préférences de synchronisation
     */
    fun resetSyncPreferences() {
        sharedPrefs.edit()
            .remove(KEY_AUTO_SYNC_ENABLED)
            .remove(KEY_LAST_SYNC_TIME)
            .remove(KEY_SYNC_FREQUENCY)
            .apply()
    }

    /**
     * Exporte les préférences pour sauvegarde
     */
    fun exportPreferences(): Map<String, Any> {
        return sharedPrefs.all.mapNotNull { (key, value) ->
            if (value != null) key to value else null
        }.toMap()
    }

    /**
     * Importe des préférences depuis une sauvegarde
     */
    fun importPreferences(preferences: Map<String, Any>) {
        val editor = sharedPrefs.edit()
        preferences.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.apply()
    }

    /**
     * Vérifie si une synchronisation récente existe
     */
    fun hasRecentSync(maxAgeHours: Int = syncFrequencyHours): Boolean {
        val lastSync = lastSyncTime
        if (lastSync == 0L) return false

        val maxAge = maxAgeHours * 60 * 60 * 1000L // Conversion en millisecondes
        return (System.currentTimeMillis() - lastSync) < maxAge
    }

    /**
     * Enregistre une synchronisation réussie
     */
    fun recordSuccessfulSync() {
        lastSyncTime = System.currentTimeMillis()
    }

    /**
     * Obtient le délai depuis la dernière synchronisation
     */
    fun getTimeSinceLastSync(): String {
        val lastSync = lastSyncTime
        if (lastSync == 0L) return "Jamais"

        val diffMs = System.currentTimeMillis() - lastSync
        val diffMinutes = diffMs / (60 * 1000)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "À l'instant"
            diffMinutes < 60 -> "Il y a ${diffMinutes}min"
            diffHours < 24 -> "Il y a ${diffHours}h"
            diffDays < 7 -> "Il y a ${diffDays}j"
            else -> "Il y a plus d'une semaine"
        }
    }
}