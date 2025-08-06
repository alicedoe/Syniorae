package com.syniorae.data.remote.calendar

import android.content.Context
import com.syniorae.domain.exceptions.AuthenticationException
import com.syniorae.domain.exceptions.SyncException
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/**
 * Manager pour l'API Google Calendar
 * Version de simulation sans dépendances Google (pour éviter les erreurs de build)
 * TODO: Remplacer par une vraie intégration Google Calendar API plus tard
 */
class GoogleCalendarApiManager(private val context: Context) {

    private var isAuthenticated = false
    private var currentToken: String? = null

    /**
     * Authentifie l'utilisateur avec Google
     */
    suspend fun authenticate(): Result<String> {
        return try {
            // Simulation de l'authentification Google
            delay(1000)

            // Pour la démo, on simule une authentification réussie
            isAuthenticated = true
            currentToken = "fake_token_${System.currentTimeMillis()}"

            Result.success("utilisateur@gmail.com")
        } catch (e: Exception) {
            Result.failure(AuthenticationException.googleConnectionFailed(e))
        }
    }

    /**
     * Récupère la liste des calendriers de l'utilisateur
     */
    suspend fun getCalendarList(): Result<List<CalendarInfo>> {
        return try {
            if (!isAuthenticated) {
                return Result.failure(AuthenticationException.tokenExpired())
            }

            // Simulation de récupération des calendriers
            delay(1500)

            val calendars = listOf(
                CalendarInfo(
                    id = "primary",
                    name = "Principal",
                    description = "Mon calendrier principal",
                    isShared = false,
                    colorId = "1"
                ),
                CalendarInfo(
                    id = "work_calendar",
                    name = "Travail",
                    description = "Calendrier professionnel",
                    isShared = false,
                    colorId = "2"
                ),
                CalendarInfo(
                    id = "family_calendar",
                    name = "Famille",
                    description = "Événements familiaux",
                    isShared = true,
                    colorId = "3"
                ),
                CalendarInfo(
                    id = "shared_birthdays",
                    name = "Anniversaires",
                    description = "Calendrier partagé des anniversaires",
                    isShared = true,
                    colorId = "4"
                )
            )

            Result.success(calendars)
        } catch (e: Exception) {
            Result.failure(SyncException.networkError(e))
        }
    }

    /**
     * Récupère les événements d'un calendrier spécifique
     */
    suspend fun getEvents(
        calendarId: String,
        maxResults: Int = 50,
        weeksAhead: Int = 4
    ): Result<List<EventInfo>> {
        return try {
            if (!isAuthenticated) {
                return Result.failure(AuthenticationException.tokenExpired())
            }

            // Simulation de récupération des événements
            delay(2000)

            val events = generateTestEvents(maxResults, weeksAhead)
            Result.success(events)

        } catch (e: Exception) {
            Result.failure(SyncException.networkError(e))
        }
    }

    /**
     * Vérifie si le token d'authentification est valide
     */
    fun isTokenValid(): Boolean {
        return isAuthenticated && currentToken != null
    }

    /**
     * Déconnecte l'utilisateur
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            isAuthenticated = false
            currentToken = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Génère des événements de test pour la démonstration
     */
    private fun generateTestEvents(maxResults: Int, weeksAhead: Int): List<EventInfo> {
        val events = mutableListOf<EventInfo>()
        val now = LocalDateTime.now()

        // Événement aujourd'hui
        events.add(
            EventInfo(
                id = "event_today_1",
                title = "Rendez-vous médecin",
                startDateTime = now.withHour(14).withMinute(30),
                endDateTime = now.withHour(15).withMinute(30),
                isAllDay = false,
                isRecurring = false,
                location = "Cabinet Dr. Martin"
            )
        )

        // Événement en cours aujourd'hui
        events.add(
            EventInfo(
                id = "event_today_2",
                title = "Réunion famille",
                startDateTime = now.minusHours(1),
                endDateTime = now.plusHours(1),
                isAllDay = false,
                isRecurring = false,
                location = "Maison"
            )
        )

        // Événement demain
        events.add(
            EventInfo(
                id = "event_tomorrow_1",
                title = "Kinésithérapeute",
                startDateTime = now.plusDays(1).withHour(9).withMinute(0),
                endDateTime = now.plusDays(1).withHour(10).withMinute(0),
                isAllDay = false,
                isRecurring = false,
                location = "Centre de rééducation"
            )
        )

        // Événement toute la journée
        events.add(
            EventInfo(
                id = "event_birthday",
                title = "Anniversaire Sophie",
                startDateTime = now.plusDays(2).withHour(0).withMinute(0),
                endDateTime = now.plusDays(2).withHour(23).withMinute(59),
                isAllDay = true,
                isRecurring = true,
                location = ""
            )
        )

        // Événement multi-jours
        events.add(
            EventInfo(
                id = "event_vacation",
                title = "Vacances en famille",
                startDateTime = now.plusDays(7).withHour(0).withMinute(0),
                endDateTime = now.plusDays(14).withHour(23).withMinute(59),
                isAllDay = true,
                isRecurring = false,
                location = "Côte d'Azur"
            )
        )

        // Ajouter plus d'événements pour remplir selon maxResults
        val daysToGenerate = minOf(weeksAhead * 7, 30)
        for (day in 3..daysToGenerate) {
            if (events.size >= maxResults) break

            // Ajouter des événements aléatoires
            if (day % 3 == 0) { // Événement tous les 3 jours environ
                events.add(
                    EventInfo(
                        id = "event_generated_$day",
                        title = getRandomEventTitle(),
                        startDateTime = now.plusDays(day.toLong()).withHour(10 + (day % 8)).withMinute(0),
                        endDateTime = now.plusDays(day.toLong()).withHour(11 + (day % 8)).withMinute(0),
                        isAllDay = false,
                        isRecurring = false,
                        location = ""
                    )
                )
            }
        }

        return events.take(maxResults)
    }

    /**
     * Génère des titres d'événements aléatoires pour la démo
     */
    private fun getRandomEventTitle(): String {
        val titles = listOf(
            "Rendez-vous dentiste",
            "Courses au marché",
            "Appel téléphonique important",
            "Visite chez le coiffeur",
            "Déjeuner avec amis",
            "Promenade au parc",
            "Lecture du journal",
            "Jardinage",
            "Préparation du dîner",
            "Émission TV préférée"
        )
        return titles.random()
    }
}

/**
 * Informations d'un calendrier
 */
data class CalendarInfo(
    val id: String,
    val name: String,
    val description: String,
    val isShared: Boolean,
    val colorId: String
)

/**
 * Informations d'un événement
 */
data class EventInfo(
    val id: String,
    val title: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val isAllDay: Boolean,
    val isRecurring: Boolean,
    val location: String = ""
) {
    /**
     * Vérifie si l'événement est actuellement en cours
     */
    fun isCurrentlyRunning(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(startDateTime) && now.isBefore(endDateTime)
    }

    /**
     * Vérifie si l'événement s'étend sur plusieurs jours
     */
    fun isMultiDay(): Boolean {
        return startDateTime.toLocalDate() != endDateTime.toLocalDate()
    }
}