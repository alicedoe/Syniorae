package com.syniorae.data.remote.google

import android.util.Log
import com.syniorae.data.local.json.models.EventJsonModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Interface avec l'API Google Calendar
 * Gère les appels REST vers Google Calendar avec de vraies données
 */
class GoogleCalendarApi(
    private val authManager: GoogleAuthManager
) {

    companion object {
        private const val TAG = "GoogleCalendarApi"
        private const val CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3"
    }

    // Client HTTP configuré
    private val httpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Logging pour debug
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    /**
     * Récupère la liste RÉELLE des calendriers de l'utilisateur
     */
    suspend fun getCalendarList(): List<GoogleCalendarInfo> {
        return withContext(Dispatchers.IO) {
            try {
                if (!authManager.isSignedIn()) {
                    throw Exception("Utilisateur non connecté")
                }

                val accessToken = authManager.getAccessToken()
                    ?: throw Exception("Token d'accès non disponible")

                Log.d(TAG, "Récupération des calendriers avec token: ${accessToken.take(20)}...")

                // Si c'est un token temporaire de développement, utiliser des données simulées
                if (accessToken.startsWith("gsi_token_")) {
                    Log.i(TAG, "Token de développement détecté, utilisation de calendriers simulés")
                    delay(1500) // Simuler délai réseau
                    return@withContext listOf(
                        GoogleCalendarInfo(
                            id = "primary",
                            name = "Principal",
                            description = "Mon calendrier principal",
                            isShared = false,
                            backgroundColor = "#1976d2"
                        ),
                        GoogleCalendarInfo(
                            id = "work_calendar_123",
                            name = "Travail",
                            description = "Calendrier professionnel",
                            isShared = false,
                            backgroundColor = "#388e3c"
                        ),
                        GoogleCalendarInfo(
                            id = "family_shared_456",
                            name = "Famille",
                            description = "Événements familiaux",
                            isShared = true,
                            backgroundColor = "#f57c00"
                        ),
                        GoogleCalendarInfo(
                            id = "birthdays_789",
                            name = "Anniversaires",
                            description = "Calendrier des anniversaires",
                            isShared = true,
                            backgroundColor = "#e91e63"
                        )
                    )
                }

                // Appel RÉEL à l'API Google Calendar
                val url = "$CALENDAR_API_BASE_URL/users/me/calendarList?maxResults=50"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Erreur inconnue"
                    Log.e(TAG, "Erreur API: ${response.code} - $errorBody")

                    if (response.code == 401) {
                        throw Exception("Token expiré ou invalide")
                    }
                    throw Exception("Erreur API: ${response.code}")
                }

                val responseBody = response.body?.string()
                    ?: throw Exception("Réponse vide de l'API")

                Log.d(TAG, "Réponse brute des calendriers: $responseBody")

                parseCalendarList(responseBody)

            } catch (e: IOException) {
                Log.e(TAG, "Erreur réseau lors de la récupération des calendriers", e)
                throw Exception("Erreur réseau: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des calendriers", e)
                throw e
            }
        }
    }

    /**
     * Récupère les événements RÉELS d'un calendrier spécifique
     */
    suspend fun getCalendarEvents(
        calendarId: String,
        maxResults: Int = 50,
        weeksAhead: Int = 4
    ): List<EventJsonModel> {
        return withContext(Dispatchers.IO) {
            try {
                if (!authManager.isSignedIn()) {
                    throw Exception("Utilisateur non connecté")
                }

                val accessToken = authManager.getAccessToken()
                    ?: throw Exception("Token d'accès non disponible")

                Log.d(TAG, "Récupération des événements pour calendrier: $calendarId")

                // Si c'est un token temporaire de développement, utiliser des données simulées
                if (accessToken.startsWith("gsi_token_")) {
                    Log.i(TAG, "Token de développement détecté, utilisation d'événements simulés")
                    delay(2000) // Simuler délai réseau
                    return@withContext generateSimulatedEvents(calendarId)
                }

                // Préparer les dates ISO pour l'API
                val timeMin = LocalDateTime.now()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_INSTANT)

                val timeMax = LocalDateTime.now().plusWeeks(weeksAhead.toLong())
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_INSTANT)

                // URL encodée pour éviter les problèmes avec les caractères spéciaux
                val encodedCalendarId = java.net.URLEncoder.encode(calendarId, "UTF-8")
                val url = buildString {
                    append("$CALENDAR_API_BASE_URL/calendars/$encodedCalendarId/events")
                    append("?maxResults=$maxResults")
                    append("&timeMin=$timeMin")
                    append("&timeMax=$timeMax")
                    append("&singleEvents=true")
                    append("&orderBy=startTime")
                }

                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Erreur inconnue"
                    Log.e(TAG, "Erreur API événements: ${response.code} - $errorBody")

                    if (response.code == 401) {
                        throw Exception("Token expiré ou invalide")
                    }
                    throw Exception("Erreur API: ${response.code}")
                }

                val responseBody = response.body?.string()
                    ?: throw Exception("Réponse vide de l'API")

                Log.d(TAG, "Réponse brute des événements: ${responseBody.take(500)}...")

                parseEventsList(responseBody)

            } catch (e: IOException) {
                Log.e(TAG, "Erreur réseau lors de la récupération des événements", e)
                throw Exception("Erreur réseau: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la récupération des événements", e)
                throw e
            }
        }
    }

    /**
     * Génère des événements simulés pour les tests
     */
    private fun generateSimulatedEvents(calendarId: String): List<EventJsonModel> {
        val now = LocalDateTime.now()
        val events = mutableListOf<EventJsonModel>()

        // Événements basés sur le calendrier sélectionné
        when (calendarId) {
            "primary" -> {
                events.addAll(listOf(
                    EventJsonModel(
                        id = "event_1_$calendarId",
                        titre = "Rendez-vous médecin",
                        date_debut = now.plusHours(3),
                        date_fin = now.plusHours(4),
                        toute_journee = false,
                        multi_jours = false
                    ),
                    EventJsonModel(
                        id = "event_2_$calendarId",
                        titre = "Courses au supermarché",
                        date_debut = now.plusDays(1).withHour(10).withMinute(0),
                        date_fin = now.plusDays(1).withHour(11).withMinute(30),
                        toute_journee = false,
                        multi_jours = false
                    ),
                    EventJsonModel(
                        id = "event_3_$calendarId",
                        titre = "Anniversaire maman",
                        date_debut = now.plusDays(3).withHour(0).withMinute(0),
                        date_fin = now.plusDays(3).withHour(23).withMinute(59),
                        toute_journee = true,
                        multi_jours = false
                    )
                ))
            }
            "work_calendar_123" -> {
                events.addAll(listOf(
                    EventJsonModel(
                        id = "work_1_$calendarId",
                        titre = "Réunion équipe",
                        date_debut = now.plusDays(1).withHour(9).withMinute(0),
                        date_fin = now.plusDays(1).withHour(10).withMinute(30),
                        toute_journee = false,
                        multi_jours = false
                    ),
                    EventJsonModel(
                        id = "work_2_$calendarId",
                        titre = "Présentation client",
                        date_debut = now.plusDays(2).withHour(14).withMinute(0),
                        date_fin = now.plusDays(2).withHour(16).withMinute(0),
                        toute_journee = false,
                        multi_jours = false
                    )
                ))
            }
            "family_shared_456" -> {
                events.addAll(listOf(
                    EventJsonModel(
                        id = "family_1_$calendarId",
                        titre = "Dîner en famille",
                        date_debut = now.plusDays(1).withHour(19).withMinute(30),
                        date_fin = now.plusDays(1).withHour(22).withMinute(0),
                        toute_journee = false,
                        multi_jours = false
                    )
                ))
            }
            else -> {
                events.addAll(listOf(
                    EventJsonModel(
                        id = "generic_1_$calendarId",
                        titre = "Événement générique",
                        date_debut = now.plusDays(2).withHour(12).withMinute(0),
                        date_fin = now.plusDays(2).withHour(13).withMinute(0),
                        toute_journee = false,
                        multi_jours = false
                    )
                ))
            }
        }

        return events.sortedBy { it.date_debut }
    }

    /**
     * Parse la réponse JSON de la liste des calendriers
     */
    private fun parseCalendarList(jsonResponse: String): List<GoogleCalendarInfo> {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val itemsArray = jsonObject.optJSONArray("items") ?: return emptyList()

            val calendars = mutableListOf<GoogleCalendarInfo>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)

                val calendar = GoogleCalendarInfo(
                    id = item.getString("id"),
                    name = item.optString("summary", "Calendrier sans nom"),
                    description = item.optString("description", ""),
                    isShared = item.optString("accessRole", "") != "owner",
                    backgroundColor = item.optString("backgroundColor", "#1976d2")
                )

                calendars.add(calendar)
                Log.d(TAG, "Calendrier trouvé: ${calendar.name} (${calendar.id})")
            }

            Log.i(TAG, "Total calendriers récupérés: ${calendars.size}")
            return calendars

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing des calendriers", e)
            throw Exception("Erreur de parsing: ${e.message}")
        }
    }

    /**
     * Parse la réponse JSON de la liste des événements
     */
    private fun parseEventsList(jsonResponse: String): List<EventJsonModel> {
        try {
            val jsonObject = JSONObject(jsonResponse)
            val itemsArray = jsonObject.optJSONArray("items") ?: return emptyList()

            val events = mutableListOf<EventJsonModel>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)

                try {
                    val event = parseEventItem(item)
                    if (event != null) {
                        events.add(event)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur parsing événement $i: ${e.message}")
                    continue
                }
            }

            Log.i(TAG, "Total événements récupérés: ${events.size}")
            return events.sortedBy { it.date_debut }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du parsing des événements", e)
            throw Exception("Erreur de parsing: ${e.message}")
        }
    }

    /**
     * Parse un événement individuel du JSON Google Calendar
     */
    private fun parseEventItem(item: JSONObject): EventJsonModel? {
        try {
            val id = item.getString("id")
            val title = item.optString("summary", "Événement sans titre")

            // Récupérer les dates de début et fin
            val start = item.getJSONObject("start")
            val end = item.getJSONObject("end")

            // Vérifier si c'est un événement "toute la journée"
            val isAllDay = start.has("date") // Si "date" au lieu de "dateTime"

            val startDateTime = if (isAllDay) {
                parseAllDayDate(start.getString("date"))
            } else {
                parseDateTime(start.getString("dateTime"))
            }

            val endDateTime = if (isAllDay) {
                parseAllDayDate(end.getString("date"))
            } else {
                parseDateTime(end.getString("dateTime"))
            }

            // Vérifier si c'est un événement multi-jours
            val isMultiDay = startDateTime.toLocalDate() != endDateTime.toLocalDate()

            return EventJsonModel(
                id = id,
                titre = title,
                date_debut = startDateTime,
                date_fin = endDateTime,
                toute_journee = isAllDay,
                multi_jours = isMultiDay
            )

        } catch (e: Exception) {
            Log.w(TAG, "Impossible de parser l'événement: ${e.message}")
            return null
        }
    }

    /**
     * Parse une date "toute la journée" (format: "2024-01-15")
     */
    private fun parseAllDayDate(dateString: String): LocalDateTime {
        return try {
            val date = java.time.LocalDate.parse(dateString)
            date.atStartOfDay() // 00:00:00
        } catch (e: Exception) {
            Log.w(TAG, "Erreur parsing date toute la journée: $dateString")
            LocalDateTime.now()
        }
    }

    /**
     * Parse une dateTime avec timezone (format: "2024-01-15T14:30:00+01:00")
     */
    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            // Parser avec timezone puis convertir en heure locale
            val zonedDateTime = java.time.ZonedDateTime.parse(dateTimeString)
            zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        } catch (e: Exception) {
            Log.w(TAG, "Erreur parsing dateTime: $dateTimeString")
            LocalDateTime.now()
        }
    }

    /**
     * Vérifie si l'API est accessible
     */
    suspend fun checkApiAccess(): Boolean {
        return try {
            if (!authManager.isSignedIn()) {
                false
            } else {
                // Test avec un appel simple
                val calendars = getCalendarList()
                calendars.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test d'accès API échoué", e)
            false
        }
    }

    /**
     * Teste la connectivité vers Google Calendar API avec informations détaillées
     */
    suspend fun testConnectivity(): ApiTestResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!authManager.isSignedIn()) {
                    return@withContext ApiTestResult(
                        isSuccess = false,
                        message = "Utilisateur non connecté",
                        details = "Authentification Google requise"
                    )
                }

                val accessToken = authManager.getAccessToken()
                if (accessToken.isNullOrBlank()) {
                    return@withContext ApiTestResult(
                        isSuccess = false,
                        message = "Token d'accès manquant",
                        details = "Reconnexion nécessaire"
                    )
                }

                // Si c'est un token de test, retourner un succès simulé
                if (accessToken.startsWith("gsi_token_")) {
                    return@withContext ApiTestResult(
                        isSuccess = true,
                        message = "Mode simulation actif",
                        details = "Token de développement - données simulées"
                    )
                }

                // Test de connectivité simple
                val url = "$CALENDAR_API_BASE_URL/users/me/calendarList?maxResults=1"
                val request = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    ApiTestResult(
                        isSuccess = true,
                        message = "Connexion réussie",
                        details = "API Google Calendar accessible"
                    )
                } else {
                    ApiTestResult(
                        isSuccess = false,
                        message = "Erreur API: ${response.code}",
                        details = response.body?.string() ?: "Détails non disponibles"
                    )
                }

            } catch (e: Exception) {
                ApiTestResult(
                    isSuccess = false,
                    message = "Erreur de connectivité",
                    details = e.message ?: "Erreur inconnue"
                )
            }
        }
    }
}

/**
 * Informations sur un calendrier Google RÉEL
 */
data class GoogleCalendarInfo(
    val id: String,
    val name: String,
    val description: String,
    val isShared: Boolean = false,
    val backgroundColor: String = "#1976d2"
)

/**
 * Résultat du test de connectivité API
 */
data class ApiTestResult(
    val isSuccess: Boolean,
    val message: String,
    val details: String
)