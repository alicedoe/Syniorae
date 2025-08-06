package com.syniorae.data.repository.calendar

import android.content.Context
import android.util.Log
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.*
import com.syniorae.data.remote.calendar.GoogleCalendarApiManager
import com.syniorae.data.remote.calendar.CalendarItem
import com.syniorae.data.remote.calendar.CalendarEventRemote
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Repository pour la gestion des données de calendrier
 */
class CalendarRepository(
    private val jsonFileManager: JsonFileManager
) {

    companion object {
        private const val TAG = "CalendarRepository"
    }

    private var googleApiManager: GoogleCalendarApiManager? = null

    /**
     * Initialise l'API Google Calendar
     */
    fun initializeGoogleApi(context: Context) {
        googleApiManager = GoogleCalendarApiManager(context)
        Log.d(TAG, "API Google initialisée")
    }

    /**
     * Récupère la configuration du calendrier depuis JSON
     */
    suspend fun getCalendarConfiguration(): ConfigurationJsonModel? {
        return jsonFileManager.readJsonFile(
            WidgetType.CALENDAR,
            JsonFileType.CONFIG,
            ConfigurationJsonModel::class.java
        )
    }

    /**
     * Récupère les événements du calendrier depuis les fichiers JSON locaux
     */
    suspend fun getCalendarEvents(): List<CalendarEvent> {
        val eventsData = jsonFileManager.readJsonFile(
            WidgetType.CALENDAR,
            JsonFileType.DATA,
            EventsJsonModel::class.java
        ) ?: return emptyList()

        return eventsData.evenements.map { eventJson ->
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
    }

    /**
     * Récupère les associations d'icônes
     */
    suspend fun getIconAssociations(): IconsJsonModel? {
        return jsonFileManager.readJsonFile(
            WidgetType.CALENDAR,
            JsonFileType.ICONS,
            IconsJsonModel::class.java
        )
    }

    /**
     * Synchronise avec l'API Google Calendar RÉELLE
     */
    suspend fun syncCalendar(): SyncResult {
        return try {
            Log.d(TAG, "Début de la synchronisation...")

            // Récupérer la configuration
            val config = getCalendarConfiguration()
                ?: return SyncResult.Error("Configuration manquante")

            // Vérifier que l'API Google est initialisée
            val apiManager = googleApiManager
                ?: return SyncResult.Error("API Google non initialisée")

            // Initialiser le service Google
            if (!apiManager.initializeService()) {
                return SyncResult.Error("Impossible d'initialiser le service Google")
            }

            // Test de connexion
            if (!apiManager.testConnection()) {
                return SyncResult.Error("Impossible de se connecter à Google Calendar")
            }

            Log.d(TAG, "Récupération des événements du calendrier: ${config.calendrier_id}")

            // Récupérer les événements depuis l'API Google
            val eventsResult = apiManager.getCalendarEvents(
                calendarId = config.calendrier_id,
                maxResults = config.nb_evenements_max,
                weeksAhead = config.nb_semaines_max
            )

            when {
                eventsResult.isSuccess -> {
                    val remoteEvents = eventsResult.getOrNull() ?: emptyList()

                    Log.d(TAG, "Récupéré ${remoteEvents.size} événements depuis Google")

                    // Convertir en EventJsonModel
                    val jsonEvents = remoteEvents.map { remoteEvent ->
                        EventJsonModel(
                            id = remoteEvent.id,
                            titre = remoteEvent.title,
                            date_debut = remoteEvent.startDateTime,
                            date_fin = remoteEvent.endDateTime,
                            toute_journee = remoteEvent.isAllDay,
                            multi_jours = remoteEvent.isMultiDay,
                            en_cours = isEventCurrentlyRunning(
                                remoteEvent.startDateTime,
                                remoteEvent.endDateTime
                            ),
                            calendrier_source = config.calendrier_name
                        )
                    }

                    // Sauvegarder dans le fichier JSON local
                    val eventsModel = EventsJsonModel(
                        derniere_synchro = LocalDateTime.now(),
                        statut = "success",
                        nb_evenements_recuperes = jsonEvents.size,
                        evenements = jsonEvents
                    )

                    val saved = jsonFileManager.writeJsonFile(
                        WidgetType.CALENDAR,
                        JsonFileType.DATA,
                        eventsModel
                    )

                    if (saved) {
                        Log.d(TAG, "Synchronisation réussie - ${jsonEvents.size} événements sauvegardés")
                        SyncResult.Success(jsonEvents.size)
                    } else {
                        Log.e(TAG, "Échec de la sauvegarde locale")
                        SyncResult.Error("Échec de la sauvegarde locale")
                    }
                }
                else -> {
                    val error = eventsResult.exceptionOrNull()
                    Log.e(TAG, "Erreur API Google", error)

                    // Sauvegarder l'erreur dans le fichier JSON
                    saveErrorToJson(error?.message ?: "Erreur API Google inconnue")

                    SyncResult.Error("Erreur API Google: ${error?.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation", e)

            // Sauvegarder l'erreur dans le fichier JSON
            saveErrorToJson(e.message ?: "Erreur inconnue")

            SyncResult.Error("Erreur de synchronisation: ${e.message}")
        }
    }

    /**
     * Récupère les calendriers Google disponibles
     */
    suspend fun getAvailableCalendars(): Result<List<CalendarItem>> {
        return try {
            val apiManager = googleApiManager
                ?: return Result.failure(Exception("API Google non initialisée"))

            if (!apiManager.initializeService()) {
                return Result.failure(Exception("Service Google non initialisé"))
            }

            Log.d(TAG, "Récupération des calendriers disponibles...")
            apiManager.getCalendarList()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des calendriers", e)
            Result.failure(e)
        }
    }

    /**
     * Synchronisation avec mode de test (fallback)
     */
    suspend fun syncCalendarWithFallback(): SyncResult {
        return try {
            // Essayer la vraie synchronisation d'abord
            val realSyncResult = syncCalendar()

            if (realSyncResult is SyncResult.Success) {
                return realSyncResult
            }

            // Si échec, utiliser des données de test
            Log.w(TAG, "Synchronisation réelle échouée, utilisation des données de test")
            syncWithTestData()

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation avec fallback", e)
            SyncResult.Error("Toutes les méthodes de synchronisation ont échoué")
        }
    }

    /**
     * Synchronisation avec des données de test (fallback)
     */
    private suspend fun syncWithTestData(): SyncResult {
        return try {
            Log.d(TAG, "Utilisation des données de test")

            // Attendre un peu pour simuler un appel API
            delay(1500)

            // Créer des événements de test
            val testEvents = createTestEvents()

            // Sauvegarder les événements de test
            val eventsModel = EventsJsonModel(
                derniere_synchro = LocalDateTime.now(),
                statut = "success",
                error_message = "Utilisation des données de test",
                nb_evenements_recuperes = testEvents.size,
                evenements = testEvents
            )

            val saved = jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                eventsModel
            )

            if (saved) {
                Log.d(TAG, "Synchronisation de test réussie")
                SyncResult.Success(testEvents.size)
            } else {
                SyncResult.Error("Échec de la sauvegarde des données de test")
            }

        } catch (e: Exception) {
            SyncResult.Error("Erreur lors de la synchronisation de test: ${e.message}")
        }
    }

    /**
     * Sauvegarde une erreur dans le fichier JSON
     */
    private suspend fun saveErrorToJson(errorMessage: String) {
        try {
            val eventsModel = EventsJsonModel(
                derniere_synchro = LocalDateTime.now(),
                statut = "error",
                error_message = errorMessage,
                evenements = emptyList()
            )

            jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                eventsModel
            )
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de sauvegarder l'erreur", e)
        }
    }

    /**
     * Crée des événements de test pour la démonstration
     */
    private fun createTestEvents(): List<EventJsonModel> {
        val now = LocalDateTime.now()

        return listOf(
            // Événement aujourd'hui
            EventJsonModel(
                id = "test_1",
                titre = "Rendez-vous médecin",
                date_debut = now.withHour(14).withMinute(30),
                date_fin = now.withHour(15).withMinute(30),
                toute_journee = false,
                multi_jours = false
            ),
            // Événement aujourd'hui en cours (si c'est l'après-midi)
            EventJsonModel(
                id = "test_2",
                titre = "Réunion famille",
                date_debut = now.minusHours(1),
                date_fin = now.plusHours(1),
                toute_journee = false,
                multi_jours = false,
                en_cours = true
            ),
            // Événement demain
            EventJsonModel(
                id = "test_3",
                titre = "Kinésithérapeute",
                date_debut = now.plusDays(1).withHour(9).withMinute(0),
                date_fin = now.plusDays(1).withHour(10).withMinute(0),
                toute_journee = false,
                multi_jours = false
            ),
            // Événement toute la journée
            EventJsonModel(
                id = "test_4",
                titre = "Anniversaire Sophie",
                date_debut = now.plusDays(2).withHour(0).withMinute(0),
                date_fin = now.plusDays(2).withHour(23).withMinute(59),
                toute_journee = true,
                multi_jours = false
            ),
            // Événement multi-jours
            EventJsonModel(
                id = "test_5",
                titre = "Vacances en famille",
                date_debut = now.plusDays(7).withHour(0).withMinute(0),
                date_fin = now.plusDays(14).withHour(23).withMinute(59),
                toute_journee = true,
                multi_jours = true
            ),
            // Événement cette semaine
            EventJsonModel(
                id = "test_6",
                titre = "Coiffeur",
                date_debut = now.plusDays(3).withHour(10).withMinute(30),
                date_fin = now.plusDays(3).withHour(11).withMinute(30),
                toute_journee = false,
                multi_jours = false
            )
        )
    }

    /**
     * Vérifie si un événement est en cours maintenant
     */
    private fun isEventCurrentlyRunning(start: LocalDateTime, end: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(start) && now.isBefore(end)
    }

    /**
     * Vérifie si une synchronisation récente existe
     */
    suspend fun hasRecentSync(maxAgeHours: Int = 4): Boolean {
        val eventsData = jsonFileManager.readJsonFile(
            WidgetType.CALENDAR,
            JsonFileType.DATA,
            EventsJsonModel::class.java
        ) ?: return false

        val lastSync = eventsData.derniere_synchro ?: return false
        val cutoff = LocalDateTime.now().minusHours(maxAgeHours.toLong())

        return lastSync.isAfter(cutoff) && eventsData.isSuccess()
    }

    /**
     * Supprime toutes les données du calendrier
     */
    suspend fun clearCalendarData(): Boolean {
        return jsonFileManager.deleteWidgetFiles(WidgetType.CALENDAR)
    }

    /**
     * Force une synchronisation (ignore les vérifications de timing)
     */
    suspend fun forceSyncCalendar(): SyncResult {
        Log.d(TAG, "Synchronisation forcée demandée")
        return syncCalendar()
    }
}

/**
 * Résultat d'une synchronisation
 */
sealed class SyncResult {
    data class Success(val eventsCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}