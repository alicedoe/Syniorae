package com.syniorae.data.remote.calendar

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.CalendarList
import com.google.api.services.calendar.model.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar as JavaCalendar
import java.util.Date

/**
 * Manager pour l'API Google Calendar
 * Gère les appels à l'API Google Calendar
 *
 * NOUVEAU FICHIER - Créer dans : app/src/main/java/com/syniorae/data/remote/calendar/GoogleCalendarApiManager.kt
 */
class GoogleCalendarApiManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleCalendarApi"
    }

    private var calendarService: Calendar? = null

    /**
     * Initialise le service Calendar API
     */
    fun initializeService(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)

        if (account == null) {
            Log.w(TAG, "Aucun compte Google connecté")
            return false
        }

        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(CalendarScopes.CALENDAR_READONLY, CalendarScopes.CALENDAR_EVENTS_READONLY)
            )
            credential.selectedAccount = account.account

            calendarService = Calendar.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                credential
            )
                .setApplicationName("SyniOrae")
                .build()

            Log.d(TAG, "Service Google Calendar initialisé avec succès")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation du service Google Calendar", e)
            return false
        }
    }

    /**
     * Récupère la liste des calendriers
     */
    suspend fun getCalendarList(): Result<List<CalendarItem>> = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: return@withContext Result.failure(
                Exception("Service non initialisé")
            )

            Log.d(TAG, "Récupération de la liste des calendriers...")

            val calendarList: CalendarList = service.calendarList().list().execute()

            val calendars = calendarList.items?.mapNotNull { calendar ->
                try {
                    CalendarItem(
                        id = calendar.id ?: return@mapNotNull null,
                        name = calendar.summary ?: "Sans nom",
                        description = calendar.description ?: "Aucune description",
                        isShared = calendar.accessRole != "owner"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur lors du traitement du calendrier ${calendar.id}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "Récupéré ${calendars.size} calendriers")
            Result.success(calendars)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des calendriers", e)
            Result.failure(e)
        }
    }

    /**
     * Récupère les événements d'un calendrier
     */
    suspend fun getCalendarEvents(
        calendarId: String,
        maxResults: Int,
        weeksAhead: Int
    ): Result<List<CalendarEventRemote>> = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: return@withContext Result.failure(
                Exception("Service non initialisé")
            )

            Log.d(TAG, "Récupération des événements pour le calendrier: $calendarId")

            // Définir la période de récupération
            val now = JavaCalendar.getInstance()
            val timeMin = com.google.api.client.util.DateTime(now.timeInMillis)

            val futureTime = JavaCalendar.getInstance().apply {
                add(JavaCalendar.WEEK_OF_YEAR, weeksAhead)
            }
            val timeMax = com.google.api.client.util.DateTime(futureTime.timeInMillis)

            // Récupérer les événements
            val events: Events = service.events()
                .list(calendarId)
                .setMaxResults(maxResults)
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true) // Développer les événements récurrents
                .execute()

            val calendarEvents = events.items?.mapNotNull { event ->
                try {
                    // Traiter la date/heure de début
                    val startDateTime = event.start?.dateTime?.let { dateTime ->
                        LocalDateTime.ofInstant(
                            Date(dateTime.value).toInstant(),
                            ZoneId.systemDefault()
                        )
                    } ?: event.start?.date?.let { date ->
                        // Événement toute la journée
                        LocalDateTime.ofInstant(
                            Date(date.value).toInstant(),
                            ZoneId.systemDefault()
                        ).withHour(0).withMinute(0)
                    }

                    // Traiter la date/heure de fin
                    val endDateTime = event.end?.dateTime?.let { dateTime ->
                        LocalDateTime.ofInstant(
                            Date(dateTime.value).toInstant(),
                            ZoneId.systemDefault()
                        )
                    } ?: event.end?.date?.let { date ->
                        // Événement toute la journée
                        LocalDateTime.ofInstant(
                            Date(date.value).toInstant(),
                            ZoneId.systemDefault()
                        ).withHour(23).withMinute(59)
                    }

                    if (startDateTime != null && endDateTime != null) {
                        CalendarEventRemote(
                            id = event.id ?: "no-id-${System.currentTimeMillis()}",
                            title = event.summary ?: "Sans titre",
                            startDateTime = startDateTime,
                            endDateTime = endDateTime,
                            isAllDay = event.start?.date != null, // Si date seulement = toute la journée
                            isMultiDay = startDateTime.toLocalDate() != endDateTime.toLocalDate(),
                            location = event.location,
                            description = event.description
                        )
                    } else {
                        Log.w(TAG, "Événement ignoré car dates invalides: ${event.summary}")
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur lors du traitement de l'événement ${event.summary}", e)
                    null
                }
            } ?: emptyList()

            Log.d(TAG, "Récupéré ${calendarEvents.size} événements")
            Result.success(calendarEvents)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des événements", e)
            Result.failure(e)
        }
    }

    /**
     * Test de connexion à l'API
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = calendarService ?: return@withContext false

            // Essayer de récupérer la liste des calendriers (limit 1)
            val calendarList = service.calendarList().list()
                .setMaxResults(1)
                .execute()

            Log.d(TAG, "Test de connexion réussi")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Test de connexion échoué", e)
            false
        }
    }
}

/**
 * Modèle pour un calendrier Google
 */
data class CalendarItem(
    val id: String,
    val name: String,
    val description: String,
    val isShared: Boolean
)

/**
 * Modèle pour un événement Google Calendar
 */
data class CalendarEventRemote(
    val id: String,
    val title: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val isAllDay: Boolean,
    val isMultiDay: Boolean,
    val location: String? = null,
    val description: String? = null
)