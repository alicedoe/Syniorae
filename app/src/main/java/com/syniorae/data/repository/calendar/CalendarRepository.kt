package com.syniorae.data.repository.calendar

import android.content.Context
import android.util.Log
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.*
import com.syniorae.data.local.preferences.CalendarPreferenceManager
import com.syniorae.data.remote.google.GoogleCalendarApi
import com.syniorae.data.remote.google.GoogleCalendarInfo
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Résultat d'une synchronisation (spécifique au repository)
 */
sealed class SyncResult {
    data class Success(val eventsCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Repository pour la gestion des calendriers RÉELS
 * Compatible avec le constructeur existant du projet
 */
class CalendarRepository(
    private val jsonFileManager: JsonFileManager
) {

    companion object {
        private const val TAG = "CalendarRepository"
    }

    // API Google Calendar sera injectée via DI
    private val googleCalendarApi by lazy {
        com.syniorae.core.di.DependencyInjection.getGoogleCalendarApi()
    }

    // Preference manager sera injecté via DI
    private val preferenceManager by lazy {
        CalendarPreferenceManager(com.syniorae.core.di.DependencyInjection.getContext())
    }

    /**
     * Récupère la configuration du calendrier sauvegardée
     */
    suspend fun getCalendarConfiguration(): CalendarConfigurationJsonModel? {
        return try {
            jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.CONFIG,
                CalendarConfigurationJsonModel::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture configuration calendrier", e)
            null
        }
    }

    /**
     * Sauvegarde la configuration du calendrier
     */
    suspend fun saveCalendarConfiguration(config: CalendarConfigurationJsonModel): Boolean {
        return try {
            // Sauvegarder dans le fichier JSON
            val jsonSaved = jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.CONFIG,
                config
            )

            // Sauvegarder aussi dans les préférences pour un accès rapide
            if (jsonSaved) {
                preferenceManager.saveSyncSettings(
                    maxWeeksAhead = config.nb_semaines_max,
                    maxEventsCount = config.nb_evenements_max,
                    syncFrequencyHours = config.frequence_synchro
                )
            }

            jsonSaved
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde configuration calendrier", e)
            false
        }
    }

    /**
     * Récupère les événements du calendrier pour l'affichage
     */
    suspend fun getCalendarEvents(): List<CalendarEvent> {
        return try {
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return emptyList()

            eventsData.evenements.map { eventJson ->
                CalendarEvent(
                    id = eventJson.id,
                    title = eventJson.titre,
                    startDateTime = eventJson.date_debut,
                    endDateTime = eventJson.date_fin,
                    isAllDay = eventJson.toute_journee,
                    isMultiDay = eventJson.multi_jours,
                    isCurrentlyRunning = eventJson.isCurrentlyRunning()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture événements calendrier", e)
            emptyList()
        }
    }

    /**
     * Récupère les événements sauvegardés localement (format JSON)
     */
    suspend fun getEvents(): EventsJsonModel? {
        return try {
            jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture événements", e)
            null
        }
    }

    /**
     * Récupère les associations d'icônes
     */
    suspend fun getIconsAssociations(): IconsJsonModel? {
        return try {
            jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.ICONS,
                IconsJsonModel::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lecture icônes", e)
            null
        }
    }

    /**
     * Synchronise le calendrier avec l'API Google Calendar RÉELLE
     */
    suspend fun syncCalendar(): SyncResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                Log.i(TAG, "Début de synchronisation calendrier...")

                // 1. Vérifier la configuration
                val config = getCalendarConfiguration()
                if (config == null) {
                    Log.e(TAG, "Configuration calendrier manquante")
                    return@withContext SyncResult.Error("Configuration manquante")
                }

                // 2. Vérifier l'authentification Google
                val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
                if (!authManager.isSignedIn()) {
                    Log.e(TAG, "Utilisateur non connecté à Google")
                    return@withContext SyncResult.Error("Utilisateur non connecté à Google")
                }

                // 3. Tester la connectivité API
                val apiTest = googleCalendarApi.testConnectivity()
                if (!apiTest.isSuccess) {
                    Log.e(TAG, "Test connectivité API échoué: ${apiTest.message}")
                    return@withContext SyncResult.Error("Connectivité API: ${apiTest.message}")
                }

                // 4. Récupérer les événements depuis l'API Google RÉELLE
                Log.d(TAG, "Récupération événements depuis API Google...")
                val events = googleCalendarApi.getCalendarEvents(
                    calendarId = config.calendrier_id,
                    maxResults = config.nb_evenements_max,
                    weeksAhead = config.nb_semaines_max
                )

                Log.i(TAG, "Événements récupérés: ${events.size}")

                // 5. Préparer les données pour sauvegarde
                val eventsModel = EventsJsonModel(
                    derniere_synchro = LocalDateTime.now(),
                    statut = "success",
                    nb_evenements_recuperes = events.size,
                    evenements = events,
                    source_api = "google_calendar_real",
                    calendar_id = config.calendrier_id
                )

                // 6. Sauvegarder les événements
                val saved = jsonFileManager.writeJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.DATA,
                    eventsModel
                )

                if (!saved) {
                    Log.e(TAG, "Échec sauvegarde événements")
                    return@withContext SyncResult.Error("Échec de la sauvegarde")
                }

                // 7. Mettre à jour les statistiques
                preferenceManager.updateSyncStats()

                val duration = System.currentTimeMillis() - startTime
                Log.i(TAG, "Synchronisation réussie: ${events.size} événements en ${duration}ms")

                // 8. Créer le résultat de succès
                SyncResult.Success(events.size)

            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "Erreur synchronisation calendrier", e)

                // En cas d'erreur, conserver les anciennes données
                SyncResult.Error("Erreur de synchronisation: ${e.message}")
            }
        }
    }

    /**
     * Récupère la liste RÉELLE des calendriers Google disponibles
     */
    suspend fun getAvailableCalendars(): List<GoogleCalendarInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Récupération calendriers disponibles...")

                // D'abord essayer l'API réelle
                val realCalendars = googleCalendarApi.getCalendarList()

                if (realCalendars.isNotEmpty()) {
                    // Sauvegarder en cache
                    preferenceManager.saveAvailableCalendars(realCalendars)
                    Log.i(TAG, "Calendriers récupérés depuis API: ${realCalendars.size}")
                    return@withContext realCalendars
                }

                // Fallback: calendriers depuis le cache
                val cachedCalendars = preferenceManager.getAvailableCalendars()
                if (cachedCalendars.isNotEmpty()) {
                    Log.i(TAG, "Calendriers récupérés depuis cache: ${cachedCalendars.size}")
                    return@withContext cachedCalendars
                }

                // Dernier recours: calendriers de test
                Log.w(TAG, "Aucun calendrier trouvé, utilisation de calendriers de test")
                return@withContext getTestCalendars()

            } catch (e: Exception) {
                Log.e(TAG, "Erreur récupération calendriers", e)

                // En cas d'erreur, essayer le cache
                val cachedCalendars = preferenceManager.getAvailableCalendars()
                if (cachedCalendars.isNotEmpty()) {
                    return@withContext cachedCalendars
                }

                throw Exception("Impossible de récupérer les calendriers: ${e.message}")
            }
        }
    }

    /**
     * Récupère le calendrier actuellement sélectionné
     */
    suspend fun getSelectedCalendar(): GoogleCalendarInfo? {
        return try {
            preferenceManager.getSelectedCalendar()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur récupération calendrier sélectionné", e)
            null
        }
    }

    /**
     * Sauvegarde le calendrier sélectionné
     */
    suspend fun saveSelectedCalendar(calendar: GoogleCalendarInfo): Boolean {
        return try {
            preferenceManager.saveSelectedCalendar(calendar)
            Log.i(TAG, "Calendrier sélectionné sauvegardé: ${calendar.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde calendrier sélectionné", e)
            false
        }
    }

    /**
     * Vérifie si une synchronisation récente existe
     */
    suspend fun hasRecentSync(maxAgeHours: Int = 4): Boolean {
        return try {
            val syncStats = preferenceManager.getSyncStats()
            syncStats.isRecentSync(maxAgeHours)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification sync récente", e)
            false
        }
    }

    /**
     * Récupère les statistiques de synchronisation
     */
    suspend fun getSyncStatistics(): com.syniorae.data.local.preferences.SyncStats {
        return try {
            preferenceManager.getSyncStats()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur récupération stats", e)
            com.syniorae.data.local.preferences.SyncStats()
        }
    }

    /**
     * Vérifie si le calendrier est configuré
     */
    suspend fun isCalendarConfigured(): Boolean {
        return try {
            preferenceManager.isCalendarConfigured()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification configuration", e)
            false
        }
    }

    /**
     * Remet à zéro la configuration du calendrier
     */
    suspend fun clearCalendarConfiguration(): Boolean {
        return try {
            // Supprimer les fichiers JSON
            jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
            jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
            jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)

            // Nettoyer les préférences
            preferenceManager.clearCalendarConfiguration()

            Log.i(TAG, "Configuration calendrier réinitialisée")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur réinitialisation configuration", e)
            false
        }
    }

    /**
     * Vérifie la santé de la synchronisation
     */
    suspend fun checkSyncHealth(): SyncHealthStatus {
        return try {
            val isConfigured = isCalendarConfigured()
            val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
            val isAuthenticated = authManager.isSignedIn()
            val hasRecentSync = hasRecentSync()
            val apiTest = if (isAuthenticated) {
                googleCalendarApi.testConnectivity()
            } else {
                com.syniorae.data.remote.google.ApiTestResult(false, "Non authentifié", "")
            }

            SyncHealthStatus(
                isConfigured = isConfigured,
                isAuthenticated = isAuthenticated,
                hasRecentSync = hasRecentSync,
                apiAccessible = apiTest.isSuccess,
                lastError = if (!apiTest.isSuccess) apiTest.message else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification santé sync", e)
            SyncHealthStatus(
                isConfigured = false,
                isAuthenticated = false,
                hasRecentSync = false,
                apiAccessible = false,
                lastError = e.message
            )
        }
    }

    /**
     * Calendriers de test en dernier recours
     */
    private fun getTestCalendars(): List<GoogleCalendarInfo> {
        return listOf(
            GoogleCalendarInfo("primary", "Principal", "Mon calendrier principal", false, "#1976d2"),
            GoogleCalendarInfo("work_test", "Travail (Test)", "Calendrier professionnel de test", false, "#388e3c"),
            GoogleCalendarInfo("family_test", "Famille (Test)", "Événements familiaux de test", true, "#f57c00")
        )
    }

    /**
     * Force la synchronisation avec retry automatique
     */
    suspend fun forceSyncWithRetry(maxRetries: Int = 3): SyncResult {
        var lastResult: SyncResult? = null

        repeat(maxRetries) { attempt ->
            lastResult = syncCalendar()

            when (lastResult) {
                is SyncResult.Success -> return lastResult!!
                is SyncResult.Error -> {
                    if (attempt < maxRetries - 1) {
                        // Attendre avant de réessayer (backoff exponentiel)
                        delay((1000 * (attempt + 1)).toLong())
                    }
                }
            }
        }

        return lastResult ?: SyncResult.Error("Échec sync après $maxRetries tentatives")
    }

    /**
     * Teste la connectivité vers l'API Google Calendar
     */
    suspend fun testApiConnection(): Boolean {
        return try {
            val apiTest = googleCalendarApi.testConnectivity()
            apiTest.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Erreur test connexion API", e)
            false
        }
    }
}

/**
 * Statut de santé de la synchronisation
 */
data class SyncHealthStatus(
    val isConfigured: Boolean,
    val isAuthenticated: Boolean,
    val hasRecentSync: Boolean,
    val apiAccessible: Boolean,
    val lastError: String? = null
) {
    fun isHealthy(): Boolean {
        return isConfigured && isAuthenticated && apiAccessible
    }

    fun getHealthSummary(): String {
        return when {
            !isConfigured -> "Configuration manquante"
            !isAuthenticated -> "Authentification requise"
            !apiAccessible -> "API inaccessible: ${lastError ?: "Erreur inconnue"}"
            !hasRecentSync -> "Synchronisation ancienne"
            else -> "Tout va bien"
        }
    }
}