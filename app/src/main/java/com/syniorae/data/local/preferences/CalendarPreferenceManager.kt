package com.syniorae.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.syniorae.data.remote.google.GoogleCalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager pour la sauvegarde sécurisée des préférences calendrier
 * Utilise EncryptedSharedPreferences pour la sécurité
 */
class CalendarPreferenceManager(private val context: Context) {

    companion object {
        private const val TAG = "CalendarPreferenceManager"
        private const val PREFS_FILE_NAME = "calendar_preferences_encrypted"

        // Clés des préférences
        private const val KEY_SELECTED_CALENDAR_ID = "selected_calendar_id"
        private const val KEY_SELECTED_CALENDAR_NAME = "selected_calendar_name"
        private const val KEY_SELECTED_CALENDAR_DESCRIPTION = "selected_calendar_description"
        private const val KEY_SELECTED_CALENDAR_IS_SHARED = "selected_calendar_is_shared"
        private const val KEY_SELECTED_CALENDAR_BACKGROUND_COLOR = "selected_calendar_background_color"
        private const val KEY_AVAILABLE_CALENDARS_JSON = "available_calendars_json"
        private const val KEY_LAST_CALENDAR_SYNC = "last_calendar_sync"
        private const val KEY_CALENDAR_SYNC_COUNT = "calendar_sync_count"
        private const val KEY_IS_CALENDAR_CONFIGURED = "is_calendar_configured"
        private const val KEY_MAX_WEEKS_AHEAD = "max_weeks_ahead"
        private const val KEY_MAX_EVENTS_COUNT = "max_events_count"
        private const val KEY_SYNC_FREQUENCY_HOURS = "sync_frequency_hours"
    }

    // SharedPreferences chiffrées
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            EncryptedSharedPreferences.create(
                PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur création EncryptedSharedPreferences, fallback vers SharedPreferences normales", e)
            // Fallback vers SharedPreferences normales en cas de problème
            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private val gson = Gson()

    /**
     * Sauvegarde le calendrier sélectionné
     */
    suspend fun saveSelectedCalendar(calendar: GoogleCalendarInfo) {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .putString(KEY_SELECTED_CALENDAR_ID, calendar.id)
                    .putString(KEY_SELECTED_CALENDAR_NAME, calendar.name)
                    .putString(KEY_SELECTED_CALENDAR_DESCRIPTION, calendar.description)
                    .putBoolean(KEY_SELECTED_CALENDAR_IS_SHARED, calendar.isShared)
                    .putString(KEY_SELECTED_CALENDAR_BACKGROUND_COLOR, calendar.backgroundColor)
                    .putBoolean(KEY_IS_CALENDAR_CONFIGURED, true)
                    .apply()

                Log.i(TAG, "Calendrier sauvegardé: ${calendar.name} (${calendar.id})")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sauvegarde calendrier sélectionné", e)
                throw e
            }
        }
    }

    /**
     * Récupère le calendrier sélectionné
     */
    suspend fun getSelectedCalendar(): GoogleCalendarInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val id = sharedPreferences.getString(KEY_SELECTED_CALENDAR_ID, null)
                if (id.isNullOrBlank()) {
                    return@withContext null
                }

                val name = sharedPreferences.getString(KEY_SELECTED_CALENDAR_NAME, "") ?: ""
                val description = sharedPreferences.getString(KEY_SELECTED_CALENDAR_DESCRIPTION, "") ?: ""
                val isShared = sharedPreferences.getBoolean(KEY_SELECTED_CALENDAR_IS_SHARED, false)
                val backgroundColor = sharedPreferences.getString(KEY_SELECTED_CALENDAR_BACKGROUND_COLOR, "#1976d2") ?: "#1976d2"

                GoogleCalendarInfo(
                    id = id,
                    name = name,
                    description = description,
                    isShared = isShared,
                    backgroundColor = backgroundColor
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération calendrier sélectionné", e)
                null
            }
        }
    }

    /**
     * Sauvegarde la liste complète des calendriers disponibles
     */
    suspend fun saveAvailableCalendars(calendars: List<GoogleCalendarInfo>) {
        withContext(Dispatchers.IO) {
            try {
                val calendarsJson = gson.toJson(calendars)
                sharedPreferences.edit()
                    .putString(KEY_AVAILABLE_CALENDARS_JSON, calendarsJson)
                    .apply()

                Log.i(TAG, "Liste des calendriers sauvegardée: ${calendars.size} calendriers")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sauvegarde liste calendriers", e)
                throw e
            }
        }
    }

    /**
     * Récupère la liste des calendriers disponibles
     */
    suspend fun getAvailableCalendars(): List<GoogleCalendarInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val calendarsJson = sharedPreferences.getString(KEY_AVAILABLE_CALENDARS_JSON, null)
                if (calendarsJson.isNullOrBlank()) {
                    return@withContext emptyList()
                }

                val type = object : TypeToken<List<GoogleCalendarInfo>>() {}.type
                gson.fromJson<List<GoogleCalendarInfo>>(calendarsJson, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération liste calendriers", e)
                emptyList()
            }
        }
    }

    /**
     * Sauvegarde les paramètres de synchronisation
     */
    suspend fun saveSyncSettings(
        maxWeeksAhead: Int,
        maxEventsCount: Int,
        syncFrequencyHours: Int
    ) {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .putInt(KEY_MAX_WEEKS_AHEAD, maxWeeksAhead)
                    .putInt(KEY_MAX_EVENTS_COUNT, maxEventsCount)
                    .putInt(KEY_SYNC_FREQUENCY_HOURS, syncFrequencyHours)
                    .apply()

                Log.i(TAG, "Paramètres de sync sauvegardés: $maxWeeksAhead semaines, $maxEventsCount événements, ${syncFrequencyHours}h")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur sauvegarde paramètres sync", e)
                throw e
            }
        }
    }

    /**
     * Récupère les paramètres de synchronisation
     */
    suspend fun getSyncSettings(): SyncSettings {
        return withContext(Dispatchers.IO) {
            try {
                SyncSettings(
                    maxWeeksAhead = sharedPreferences.getInt(KEY_MAX_WEEKS_AHEAD, 4),
                    maxEventsCount = sharedPreferences.getInt(KEY_MAX_EVENTS_COUNT, 50),
                    syncFrequencyHours = sharedPreferences.getInt(KEY_SYNC_FREQUENCY_HOURS, 4)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération paramètres sync", e)
                SyncSettings() // Valeurs par défaut
            }
        }
    }

    /**
     * Met à jour les statistiques de synchronisation
     */
    suspend fun updateSyncStats() {
        withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                val currentCount = sharedPreferences.getInt(KEY_CALENDAR_SYNC_COUNT, 0)

                sharedPreferences.edit()
                    .putLong(KEY_LAST_CALENDAR_SYNC, currentTime)
                    .putInt(KEY_CALENDAR_SYNC_COUNT, currentCount + 1)
                    .apply()

                Log.d(TAG, "Stats sync mises à jour: sync #${currentCount + 1}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur mise à jour stats sync", e)
            }
        }
    }

    /**
     * Récupère les statistiques de synchronisation
     */
    suspend fun getSyncStats(): SyncStats {
        return withContext(Dispatchers.IO) {
            try {
                SyncStats(
                    lastSyncTime = sharedPreferences.getLong(KEY_LAST_CALENDAR_SYNC, 0L),
                    syncCount = sharedPreferences.getInt(KEY_CALENDAR_SYNC_COUNT, 0)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération stats sync", e)
                SyncStats()
            }
        }
    }

    /**
     * Vérifie si le calendrier est configuré
     */
    suspend fun isCalendarConfigured(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                sharedPreferences.getBoolean(KEY_IS_CALENDAR_CONFIGURED, false) &&
                        !sharedPreferences.getString(KEY_SELECTED_CALENDAR_ID, "").isNullOrBlank()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur vérification configuration calendrier", e)
                false
            }
        }
    }

    /**
     * Remet à zéro toute la configuration calendrier
     */
    suspend fun clearCalendarConfiguration() {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .remove(KEY_SELECTED_CALENDAR_ID)
                    .remove(KEY_SELECTED_CALENDAR_NAME)
                    .remove(KEY_SELECTED_CALENDAR_DESCRIPTION)
                    .remove(KEY_SELECTED_CALENDAR_IS_SHARED)
                    .remove(KEY_SELECTED_CALENDAR_BACKGROUND_COLOR)
                    .remove(KEY_AVAILABLE_CALENDARS_JSON)
                    .remove(KEY_IS_CALENDAR_CONFIGURED)
                    .remove(KEY_MAX_WEEKS_AHEAD)
                    .remove(KEY_MAX_EVENTS_COUNT)
                    .remove(KEY_SYNC_FREQUENCY_HOURS)
                    .remove(KEY_LAST_CALENDAR_SYNC)
                    .remove(KEY_CALENDAR_SYNC_COUNT)
                    .apply()

                Log.i(TAG, "Configuration calendrier réinitialisée")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur réinitialisation configuration", e)
                throw e
            }
        }
    }

    /**
     * Exporte les préférences pour debugging
     */
    suspend fun exportPreferencesForDebug(): Map<String, Any?> {
        return withContext(Dispatchers.IO) {
            try {
                val allPrefs = sharedPreferences.all
                val filteredPrefs = mutableMapOf<String, Any?>()

                // Ne pas exporter les données sensibles, juste les métadonnées
                filteredPrefs["is_configured"] = allPrefs[KEY_IS_CALENDAR_CONFIGURED]
                filteredPrefs["selected_calendar_name"] = allPrefs[KEY_SELECTED_CALENDAR_NAME]
                filteredPrefs["sync_count"] = allPrefs[KEY_CALENDAR_SYNC_COUNT]
                filteredPrefs["last_sync"] = allPrefs[KEY_LAST_CALENDAR_SYNC]
                filteredPrefs["weeks_ahead"] = allPrefs[KEY_MAX_WEEKS_AHEAD]
                filteredPrefs["max_events"] = allPrefs[KEY_MAX_EVENTS_COUNT]
                filteredPrefs["sync_frequency"] = allPrefs[KEY_SYNC_FREQUENCY_HOURS]

                filteredPrefs
            } catch (e: Exception) {
                Log.e(TAG, "Erreur export préférences", e)
                emptyMap()
            }
        }
    }
}

/**
 * Paramètres de synchronisation
 */
data class SyncSettings(
    val maxWeeksAhead: Int = 4,
    val maxEventsCount: Int = 50,
    val syncFrequencyHours: Int = 4
)

/**
 * Statistiques de synchronisation
 */
data class SyncStats(
    val lastSyncTime: Long = 0L,
    val syncCount: Int = 0
) {
    fun getLastSyncFormatted(): String {
        return if (lastSyncTime > 0) {
            val date = java.time.Instant.ofEpochMilli(lastSyncTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()

            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            date.format(formatter)
        } else {
            "Jamais"
        }
    }

    fun isRecentSync(maxAgeHours: Int = 4): Boolean {
        if (lastSyncTime == 0L) return false
        val ageMs = System.currentTimeMillis() - lastSyncTime
        val ageHours = ageMs / (1000 * 60 * 60)
        return ageHours < maxAgeHours
    }
}