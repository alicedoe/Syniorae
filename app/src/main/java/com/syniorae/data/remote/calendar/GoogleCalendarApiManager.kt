package com.syniorae.data.remote.calendar

import android.content.Context
import android.util.Log
import com.syniorae.data.remote.google.ApiResponse
import com.syniorae.data.remote.google.GoogleApiClient
import com.syniorae.data.remote.google.GoogleTokenManager
import com.syniorae.domain.exceptions.AuthenticationException
import com.syniorae.domain.exceptions.SyncException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * Manager pour l'API Google Calendar (version réelle)
 */
class GoogleCalendarApiManager(
    private val context: Context,
    private val tokenManager: GoogleTokenManager
) {

    private val googleApiClient by lazy {
        GoogleApiClient(context, tokenManager)
    }

    /**
     * Authentifie l'utilisateur avec Google (vérifie le token existant)
     */
    suspend fun authenticate(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connectivityOk = googleApiClient.testConnectivity()
            if (!connectivityOk) {
                return@withContext Result.failure(AuthenticationException.tokenExpired())
            }

            // Récupérer l'email utilisateur via /userinfo
            when (val resp = googleApiClient.get("/oauth2/v2/userinfo")) {
                is ApiResponse.Success -> {
                    val json = JSONObject(resp.body)
                    val email = json.optString("email", "")
                    if (email.isNotBlank()) {
                        Result.success(email)
                    } else {
                        Result.failure(AuthenticationException.googleConnectionFailed())
                    }
                }
                is ApiResponse.Error -> {
                    Result.failure(AuthenticationException.googleConnectionFailed(Exception(resp.message)))
                }
            }
        } catch (e: Exception) {
            Result.failure(AuthenticationException.googleConnectionFailed(e))
        }
    }

    /**
     * Récupère la liste des calendriers de l'utilisateur
     */
    suspend fun getCalendarList(): Result<List<CalendarInfo>> = withContext(Dispatchers.IO) {
        try {
            when (val resp = googleApiClient.get("/calendar/v3/users/me/calendarList")) {
                is ApiResponse.Success -> {
                    val json = JSONObject(resp.body)
                    val items = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())

                    val calendars = mutableListOf<CalendarInfo>()
                    for (i in 0 until items.length()) {
                        val obj = items.getJSONObject(i)
                        calendars.add(
                            CalendarInfo(
                                id = obj.getString("id"),
                                name = obj.optString("summary", ""),
                                description = obj.optString("description", ""),
                                isShared = obj.optBoolean("primary", false).not(),
                                colorId = obj.optString("colorId", "")
                            )
                        )
                    }
                    Result.success(calendars)
                }
                is ApiResponse.Error -> Result.failure(SyncException.networkError(Exception(resp.message)))
            }
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
    ): Result<List<EventInfo>> = withContext(Dispatchers.IO) {
        try {
            val timeMin = ZonedDateTime.now().toInstant().toString()
            val timeMax = ZonedDateTime.now().plusWeeks(weeksAhead.toLong()).toInstant().toString()

            val params = mapOf(
                "maxResults" to maxResults.toString(),
                "singleEvents" to "true",
                "orderBy" to "startTime",
                "timeMin" to timeMin,
                "timeMax" to timeMax
            )

            when (val resp = googleApiClient.get("/calendar/v3/calendars/$calendarId/events", params)) {
                is ApiResponse.Success -> {
                    val json = JSONObject(resp.body)
                    val items = json.optJSONArray("items") ?: return@withContext Result.success(emptyList())

                    val events = mutableListOf<EventInfo>()
                    for (i in 0 until items.length()) {
                        val obj = items.getJSONObject(i)
                        val startDateTime = parseDateTime(obj.getJSONObject("start"))
                        val endDateTime = parseDateTime(obj.getJSONObject("end"))

                        events.add(
                            EventInfo(
                                id = obj.getString("id"),
                                title = obj.optString("summary", ""),
                                startDateTime = startDateTime,
                                endDateTime = endDateTime,
                                isAllDay = !obj.getJSONObject("start").has("dateTime"),
                                isRecurring = obj.has("recurrence"),
                                location = obj.optString("location", "")
                            )
                        )
                    }
                    Result.success(events)
                }
                is ApiResponse.Error -> Result.failure(SyncException(resp.message))


            }
        } catch (e: Exception) {
            Result.failure(SyncException.networkError(e))
        }
    }

    /**
     * Déconnecte l'utilisateur
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            tokenManager.clearTokens()
            googleApiClient.cleanup()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse un objet JSON "start" ou "end" en LocalDateTime
     */
    private fun parseDateTime(obj: JSONObject): LocalDateTime {
        return if (obj.has("dateTime")) {
            OffsetDateTime.parse(obj.getString("dateTime")).toLocalDateTime()
        } else {
            LocalDate.parse(obj.getString("date")).atStartOfDay()
        }
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
    fun isCurrentlyRunning(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(startDateTime) && now.isBefore(endDateTime)
    }

    fun isMultiDay(): Boolean {
        return startDateTime.toLocalDate() != endDateTime.toLocalDate()
    }
}
