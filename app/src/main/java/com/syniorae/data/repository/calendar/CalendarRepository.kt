package com.syniorae.data.repository.calendar

import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.*
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Repository pour la gestion des données de calendrier
 * Combine les fichiers JSON et les appels API Google Calendar
 */
class CalendarRepository(
    private val jsonFileManager: JsonFileManager
) {

    // API Google Calendar sera injectée via DI
    private val googleCalendarApi by lazy {
        com.syniorae.core.di.DependencyInjection.getGoogleCalendarApi()
    }

    /**
     * Récupère la configuration du calendrier
     */
    suspend fun getCalendarConfiguration(): ConfigurationJsonModel? {
        return jsonFileManager.readJsonFile(
            WidgetType.CALENDAR,
            JsonFileType.CONFIG,
            ConfigurationJsonModel::class.java
        )
    }

    /**
     * Récupère les événements du calendrier
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
     * Synchronise le calendrier avec l'API Google Calendar
     */
    suspend fun syncCalendar(): SyncResult {
        return try {
            // Récupérer la configuration
            val config = getCalendarConfiguration()
                ?: return SyncResult.Error("Configuration manquante")

            // Vérifier l'authentification Google
            val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
            if (!authManager.isSignedIn()) {
                return SyncResult.Error("Utilisateur non connecté à Google")
            }

            // Récupérer les événements depuis l'API Google
            val events = googleCalendarApi.getCalendarEvents(
                calendarId = config.calendrier_id,
                maxResults = config.nb_evenements_max,
                weeksAhead = config.nb_semaines_max
            )

            // Sauvegarder les événements
            val eventsModel = EventsJsonModel(
                derniere_synchro = LocalDateTime.now(),
                statut = "success",
                nb_evenements_recuperes = events.size,
                evenements = events
            )

            val saved = jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                eventsModel
            )

            if (saved) {
                SyncResult.Success(events.size)
            } else {
                SyncResult.Error("Échec de la sauvegarde")
            }

        } catch (e: Exception) {
            // En cas d'erreur, garder les anciennes données
            SyncResult.Error("Erreur de synchronisation: ${e.message}")
        }
    }

    /**
     * Récupère la liste des calendriers Google disponibles
     */
    suspend fun getAvailableCalendars(): List<com.syniorae.data.remote.google.GoogleCalendarInfo> {
        return try {
            val authManager = com.syniorae.core.di.DependencyInjection.getGoogleAuthManager()
            if (!authManager.isSignedIn()) {
                throw Exception("Utilisateur non connecté")
            }

            googleCalendarApi.getCalendarList()
        } catch (e: Exception) {
            throw Exception("Impossible de récupérer les calendriers: ${e.message}")
        }
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

        return lastSync.isAfter(cutoff)
    }

    /**
     * Supprime toutes les données du calendrier
     */
    suspend fun clearCalendarData(): Boolean {
        return jsonFileManager.deleteWidgetFiles(WidgetType.CALENDAR)
    }

    /**
     * Teste la connexion avec l'API Google Calendar
     */
    suspend fun testApiConnection(): Boolean {
        return try {
            googleCalendarApi.checkApiAccess()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Force une synchronisation avec retry en cas d'échec
     */
    suspend fun forceSyncWithRetry(maxRetries: Int = 3): SyncResult {
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            when (val result = syncCalendar()) {
                is SyncResult.Success -> return result
                is SyncResult.Error -> {
                    lastError = result.message
                    if (attempt < maxRetries - 1) {
                        // Attendre avant de réessayer (backoff exponentiel)
                        delay((1000 * (attempt + 1)).toLong())
                    }
                }
            }
        }

        return SyncResult.Error("Échec après $maxRetries tentatives. Dernière erreur: $lastError")
    }
}

/**
 * Résultat d'une synchronisation
 */
sealed class SyncResult {
    data class Success(val eventsCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}