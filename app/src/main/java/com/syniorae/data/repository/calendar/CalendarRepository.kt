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
 * Combine les fichiers JSON et les appels API (futurs)
 */
class CalendarRepository(
    private val jsonFileManager: JsonFileManager
) {

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
     * Synchronise le calendrier avec l'API Google (simulation pour l'instant)
     */
    suspend fun syncCalendar(): SyncResult {
        return try {
            // Récupérer la configuration
            val config = getCalendarConfiguration()
                ?: return SyncResult.Error("Configuration manquante")

            // Simulation d'appel API Google Calendar
            delay(2000)

            // Créer des événements de test
            val testEvents = createTestEvents()

            // Sauvegarder les événements
            val eventsModel = EventsJsonModel(
                derniere_synchro = LocalDateTime.now(),
                statut = "success",
                nb_evenements_recuperes = testEvents.size,
                evenements = testEvents
            )

            val saved = jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                eventsModel
            )

            if (saved) {
                SyncResult.Success(testEvents.size)
            } else {
                SyncResult.Error("Échec de la sauvegarde")
            }

        } catch (e: Exception) {
            SyncResult.Error("Erreur de synchronisation: ${e.message}")
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
            // Événement aujourd'hui en cours
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
            )
        )
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
}

/**
 * Résultat d'une synchronisation
 */
sealed class SyncResult {
    data class Success(val eventsCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}