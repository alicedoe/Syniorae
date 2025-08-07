package com.syniorae.data.remote.google

import com.syniorae.data.local.json.models.EventJsonModel
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parser pour les réponses de l'API Google Calendar
 * Convertit les JSON Google en modèles internes de l'application
 */
object GoogleCalendarResponseParser {

    /**
     * Parse une réponse de liste de calendriers
     */
    fun parseCalendarList(jsonResponse: String): List<GoogleCalendarInfo> {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            val items = jsonObject.optJSONArray("items") ?: JSONArray()
            val calendars = mutableListOf<GoogleCalendarInfo>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                calendars.add(parseCalendarItem(item))
            }

            calendars
        } catch (e: Exception) {
            android.util.Log.e("CalendarParser", "Erreur parsing calendriers", e)
            emptyList()
        }
    }

    /**
     * Parse un élément de calendrier individuel
     */
    private fun parseCalendarItem(item: JSONObject): GoogleCalendarInfo {
        return GoogleCalendarInfo(
            id = item.optString("id", ""),
            name = item.optString("summary", "Calendrier sans nom"),
            description = item.optString("description", ""),
            isShared = item.optString("accessRole", "owner") != "owner",
            backgroundColor = item.optString("backgroundColor", "#1976d2")
        )
    }

    /**
     * Parse une réponse d'événements de calendrier
     */
    fun parseEventsList(jsonResponse: String): List<EventJsonModel> {
        return try {
            val jsonObject = JSONObject(jsonResponse)
            val items = jsonObject.optJSONArray("items") ?: JSONArray()
            val events = mutableListOf<EventJsonModel>()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                parseEventItem(item)?.let { event ->
                    events.add(event)
                }
            }

            // Trier par date de début
            events.sortedBy { it.date_debut }
        } catch (e: Exception) {
            android.util.Log.e("CalendarParser", "Erreur parsing événements", e)
            emptyList()
        }
    }

    /**
     * Parse un événement individuel
     */
    private fun parseEventItem(item: JSONObject): EventJsonModel? {
        return try {
            val id = item.optString("id", "")
            val title = item.optString("summary", "Événement sans titre")

            // Parser les dates de début et fin
            val startObject = item.optJSONObject("start")
            val endObject = item.optJSONObject("end")

            if (startObject == null || endObject == null) {
                return null
            }

            val (startDateTime, isAllDay) = parseDateTimeObject(startObject)
            val (endDateTime, _) = parseDateTimeObject(endObject)

            if (startDateTime == null || endDateTime == null) {
                return null
            }

            // Déterminer si c'est multi-jours
            val isMultiDay = startDateTime.toLocalDate() != endDateTime.toLocalDate()

            EventJsonModel(
                id = id,
                titre = title,
                date_debut = startDateTime,
                date_fin = endDateTime,
                toute_journee = isAllDay,
                multi_jours = isMultiDay,
                calendrier_source = item.optString("organizer.displayName", "")
            )
        } catch (e: Exception) {
            android.util.Log.e("CalendarParser", "Erreur parsing événement", e)
            null
        }
    }

    /**
     * Parse un objet date/time de Google Calendar
     * Retourne (LocalDateTime, isAllDay)
     */
    private fun parseDateTimeObject(dateObject: JSONObject): Pair<LocalDateTime?, Boolean> {
        return try {
            // Vérifier si c'est une date entière (toute la journée)
            val dateString = dateObject.optString("date", null)
            if (dateString != null) {
                // Événement toute la journée
                val date = java.time.LocalDate.parse(dateString)
                return Pair(date.atStartOfDay(), true)
            }

            // Sinon, c'est une dateTime avec heure
            val dateTimeString = dateObject.optString("dateTime", null)
            if (dateTimeString != null) {
                val dateTime = parseGoogleDateTime(dateTimeString)
                return Pair(dateTime, false)
            }

            Pair(null, false)
        } catch (e: Exception) {
            android.util.Log.e("CalendarParser", "Erreur parsing date", e)
            Pair(null, false)
        }
    }

    /**
     * Parse une chaîne dateTime de Google Calendar en LocalDateTime
     * Formats supportés : ISO 8601 avec timezone
     */
    private fun parseGoogleDateTime(dateTimeString: String): LocalDateTime? {
        return try {
            // Google utilise le format ISO 8601 : "2023-08-03T14:30:00+02:00"
            // On va d'abord essayer de parser avec différents formatters

            val formatters = listOf(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                DateTimeFormatter.ISO_ZONED_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            )

            // Nettoyer la chaîne (supprimer timezone pour LocalDateTime)
            var cleanDateTime = dateTimeString
            if (cleanDateTime.contains("+") || cleanDateTime.endsWith("Z")) {
                // Supprimer la partie timezone pour obtenir LocalDateTime
                cleanDateTime = cleanDateTime.substringBefore("+").substringBefore("Z")
            }

            for (formatter in formatters) {
                try {
                    return LocalDateTime.parse(cleanDateTime, formatter)
                } catch (e: DateTimeParseException) {
                    continue
                }
            }

            // Si rien ne fonctionne, essayer de parser avec java.time.Instant
            if (dateTimeString.contains("T")) {
                val instant = java.time.Instant.parse(dateTimeString)
                return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            }

            null
        } catch (e: Exception) {
            android.util.Log.e("CalendarParser", "Erreur parsing dateTime: $dateTimeString", e)
            null
        }
    }

    /**
     * Valide qu'une réponse JSON est valide
     */
    fun isValidResponse(jsonResponse: String?): Boolean {
        if (jsonResponse.isNullOrBlank()) return false

        return try {
            val json = JSONObject(jsonResponse)
            // Vérifier que ce n'est pas une réponse d'erreur
            !json.has("error")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extrait un message d'erreur d'une réponse JSON
     */
    fun parseErrorMessage(jsonResponse: String?): String? {
        if (jsonResponse.isNullOrBlank()) return null

        return try {
            val json = JSONObject(jsonResponse)
            val error = json.optJSONObject("error")
            error?.optString("message") ?: "Erreur inconnue"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse les métadonnées d'une réponse d'événements
     */
    fun parseEventsMetadata(jsonResponse: String): EventsMetadata? {
        return try {
            val json = JSONObject(jsonResponse)
            EventsMetadata(
                kind = json.optString("kind", ""),
                nextPageToken = json.optString("nextPageToken"),
                timeZone = json.optString("timeZone", "UTC"),
                updated = json.optString("updated"),
                accessRole = json.optString("accessRole", "reader")
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Métadonnées d'une réponse d'événements
 */
data class EventsMetadata(
    val kind: String,
    val nextPageToken: String?,
    val timeZone: String,
    val updated: String,
    val accessRole: String
)