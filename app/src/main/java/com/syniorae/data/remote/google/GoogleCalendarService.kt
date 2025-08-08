package com.syniorae.data.remote.google

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service pour interagir avec l'API Google Calendar réelle
 * Utilise OkHttp pour les appels HTTP vers Google Calendar API
 */
class GoogleCalendarService(
    private val context: Context,
    private val authManager: GoogleAuthManager
) {

    companion object {
        private const val TAG = "GoogleCalendarService"
        private const val CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3"
    }

    // Client HTTP configuré
    private val httpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Effectue un appel GET vers l'API Google Calendar
     */
    private suspend fun makeApiCall(endpoint: String, params: Map<String, String> = emptyMap()): String? {
        return withContext(Dispatchers.IO) {
            try {
                val accessToken = authManager.getAccessToken()
                if (accessToken.isNullOrBlank()) {
                    Log.e(TAG, "Token d'accès manquant")
                    return@withContext null
                }

                // Construire l'URL avec les paramètres
                val urlBuilder = StringBuilder("$CALENDAR_API_URL$endpoint")
                if (params.isNotEmpty()) {
                    urlBuilder.append("?")
                    params.entries.forEachIndexed { index, (key, value) ->
                        if (index > 0) urlBuilder.append("&")
                        urlBuilder.append("$key=$value")
                    }
                }

                // Pour l'instant, simulation car nous n'avons pas encore les vraies clés API
                Log.d(TAG, "Simulation d'appel API vers: $urlBuilder")
                kotlinx.coroutines.delay(1500)

                // Simuler une réponse JSON selon l'endpoint
                when {
                    endpoint.contains("calendarList") -> {
                        generateCalendarListResponse()
                    }
                    endpoint.contains("events") -> {
                        val calendarId = endpoint.substringAfter("calendars/").substringBefore("/events")
                        generateEventsJsonResponse(calendarId)
                    }
                    else -> {
                        """{"error": "Endpoint non supporté: $endpoint"}"""
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "Erreur réseau lors de l'appel API", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'appel API", e)
                null
            }
        }
    }

    /**
     * Génère une réponse simulée pour la liste des calendriers
     */
    private fun generateCalendarListResponse(): String {
        return """
        {
          "kind": "calendar#calendarList",
          "items": [
            {
              "kind": "calendar#calendarListEntry",
              "id": "primary",
              "summary": "Principal",
              "description": "Mon calendrier principal",
              "backgroundColor": "#1976d2",
              "accessRole": "owner"
            },
            {
              "kind": "calendar#calendarListEntry", 
              "id": "work_calendar_123@group.calendar.google.com",
              "summary": "Travail",
              "description": "Calendrier professionnel",
              "backgroundColor": "#388e3c",
              "accessRole": "owner"
            },
            {
              "kind": "calendar#calendarListEntry",
              "id": "family_shared_456@group.calendar.google.com", 
              "summary": "Famille",
              "description": "Événements familiaux",
              "backgroundColor": "#f57c00",
              "accessRole": "reader"
            },
            {
              "kind": "calendar#calendarListEntry",
              "id": "addressbook#contacts@group.v.calendar.google.com",
              "summary": "Anniversaires",
              "description": "Dates d'anniversaire de vos contacts",
              "backgroundColor": "#e91e63",
              "accessRole": "reader"
            }
          ]
        }
        """.trimIndent()
    }

    /**
     * Génère une réponse JSON simulée pour les événements
     */
    private fun generateEventsJsonResponse(calendarId: String): String {
        val now = System.currentTimeMillis()

        return when {
            calendarId == "primary" -> """
            {
              "kind": "calendar#events",
              "items": [
                {
                  "kind": "calendar#event",
                  "id": "event1_primary",
                  "summary": "Rendez-vous médecin",
                  "description": "Consultation de routine",
                  "start": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 3600000)}"
                  },
                  "end": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 7200000)}"
                  }
                },
                {
                  "kind": "calendar#event",
                  "id": "event2_primary", 
                  "summary": "Courses au supermarché",
                  "start": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 86400000)}"
                  },
                  "end": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 90000000)}"
                  }
                }
              ]
            }
            """.trimIndent()

            calendarId.contains("work") -> """
            {
              "kind": "calendar#events",
              "items": [
                {
                  "kind": "calendar#event",
                  "id": "work1",
                  "summary": "Réunion équipe",
                  "start": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 86400000)}"
                  },
                  "end": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 90000000)}"
                  }
                },
                {
                  "kind": "calendar#event",
                  "id": "work2",
                  "summary": "Présentation client",
                  "start": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 172800000)}"
                  },
                  "end": {
                    "dateTime": "${java.time.Instant.ofEpochMilli(now + 180000000)}"
                  }
                }
              ]
            }
            """.trimIndent()

            calendarId.contains("family") -> """
            {
              "kind": "calendar#events", 
              "items": [
                {
                  "kind": "calendar#event",
                  "id": "family1",
                  "summary": "Anniversaire Sophie",
                  "start": {
                    "date": "${java.time.LocalDate.now().plusDays(3)}"
                  },
                  "end": {
                    "date": "${java.time.LocalDate.now().plusDays(4)}"
                  }
                },
                {
                  "kind": "calendar#event",
                  "id": "family2", 
                  "summary": "Vacances en famille",
                  "start": {
                    "date": "${java.time.LocalDate.now().plusDays(10)}"
                  },
                  "end": {
                    "date": "${java.time.LocalDate.now().plusDays(17)}"
                  }
                }
              ]
            }
            """.trimIndent()

            else -> """
            {
              "kind": "calendar#events",
              "items": []
            }
            """.trimIndent()
        }
    }

    /**
     * Récupère la liste des calendriers
     */
    suspend fun fetchCalendarList(): String? {
        return makeApiCall("/users/me/calendarList")
    }

    /**
     * Récupère les événements d'un calendrier
     */
    suspend fun fetchCalendarEvents(
        calendarId: String,
        timeMin: String? = null,
        timeMax: String? = null,
        maxResults: Int = 50
    ): String? {
        val params = mutableMapOf<String, String>().apply {
            if (timeMin != null) put("timeMin", timeMin)
            if (timeMax != null) put("timeMax", timeMax)
            put("maxResults", maxResults.toString())
            put("singleEvents", "true")
            put("orderBy", "startTime")
        }

        return makeApiCall("/calendars/${calendarId}/events", params)
    }

    /**
     * Teste la connectivité avec l'API
     */
    suspend fun testConnection(): Boolean {
        return try {
            val response = makeApiCall("/users/me/calendarList")
            !response.isNullOrBlank()
        } catch (e: Exception) {
            Log.e(TAG, "Échec du test de connexion", e)
            false
        }
    }
}