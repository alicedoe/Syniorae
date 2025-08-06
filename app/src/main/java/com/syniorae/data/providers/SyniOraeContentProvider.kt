package com.syniorae.data.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.EventsJsonModel
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.runBlocking

/**
 * ContentProvider pour exposer les données SyniOrae à d'autres applications
 * Permet l'accès en lecture seule aux événements de calendrier (futur)
 * Désactivé par défaut pour des raisons de sécurité
 */
class SyniOraeContentProvider : ContentProvider() {

    companion object {
        private const val TAG = "SyniOraeProvider"
        private const val AUTHORITY = "com.syniorae.provider"

        // URIs supportés
        private const val EVENTS = 1
        private const val EVENT_BY_ID = 2
        private const val TODAY_EVENTS = 3
        private const val FUTURE_EVENTS = 4

        private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "events", EVENTS)
            addURI(AUTHORITY, "events/#", EVENT_BY_ID)
            addURI(AUTHORITY, "events/today", TODAY_EVENTS)
            addURI(AUTHORITY, "events/future", FUTURE_EVENTS)
        }

        // Colonnes exposées
        private val EVENTS_COLUMNS = arrayOf(
            "id",
            "title",
            "start_time",
            "end_time",
            "is_all_day",
            "is_multi_day",
            "is_currently_running"
        )

        // URIs publics
        val CONTENT_URI_EVENTS: Uri = Uri.parse("content://$AUTHORITY/events")
        val CONTENT_URI_TODAY: Uri = Uri.parse("content://$AUTHORITY/events/today")
        val CONTENT_URI_FUTURE: Uri = Uri.parse("content://$AUTHORITY/events/future")
    }

    override fun onCreate(): Boolean {
        return try {
            context?.let {
                DependencyInjection.initialize(it.applicationContext)
                Log.d(TAG, "ContentProvider initialisé")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation du ContentProvider", e)
            false
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "Query: $uri")

        return try {
            when (URI_MATCHER.match(uri)) {
                EVENTS -> getAllEvents(projection, selection, selectionArgs, sortOrder)
                EVENT_BY_ID -> getEventById(uri.lastPathSegment, projection)
                TODAY_EVENTS -> getTodayEvents(projection, sortOrder)
                FUTURE_EVENTS -> getFutureEvents(projection, sortOrder)
                else -> {
                    Log.w(TAG, "URI non supporté: $uri")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la requête", e)
            null
        }
    }

    /**
     * Récupère tous les événements
     */
    private fun getAllEvents(
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = runBlocking {
        try {
            val jsonFileManager = DependencyInjection.getJsonFileManager()
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return@runBlocking createEmptyCursor(projection)

            val cursor = MatrixCursor(projection ?: EVENTS_COLUMNS)

            eventsData.evenements.forEach { event ->
                cursor.addRow(arrayOf(
                    event.id,
                    event.titre,
                    event.date_debut.toString(),
                    event.date_fin.toString(),
                    if (event.toute_journee) 1 else 0,
                    if (event.multi_jours) 1 else 0,
                    if (event.isCurrentlyRunning()) 1 else 0
                ))
            }

            Log.d(TAG, "Retourné ${cursor.count} événements")
            cursor
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des événements", e)
            createEmptyCursor(projection)
        }
    }

    /**
     * Récupère un événement par ID
     */
    private fun getEventById(eventId: String?, projection: Array<String>?): Cursor? = runBlocking {
        if (eventId == null) return@runBlocking createEmptyCursor(projection)

        try {
            val jsonFileManager = DependencyInjection.getJsonFileManager()
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return@runBlocking createEmptyCursor(projection)

            val event = eventsData.evenements.find { it.id == eventId }
                ?: return@runBlocking createEmptyCursor(projection)

            val cursor = MatrixCursor(projection ?: EVENTS_COLUMNS)
            cursor.addRow(arrayOf(
                event.id,
                event.titre,
                event.date_debut.toString(),
                event.date_fin.toString(),
                if (event.toute_journee) 1 else 0,
                if (event.multi_jours) 1 else 0,
                if (event.isCurrentlyRunning()) 1 else 0
            ))

            cursor
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération de l'événement $eventId", e)
            createEmptyCursor(projection)
        }
    }

    /**
     * Récupère les événements d'aujourd'hui
     */
    private fun getTodayEvents(projection: Array<String>?, sortOrder: String?): Cursor? = runBlocking {
        try {
            val jsonFileManager = DependencyInjection.getJsonFileManager()
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return@runBlocking createEmptyCursor(projection)

            val cursor = MatrixCursor(projection ?: EVENTS_COLUMNS)

            eventsData.evenements
                .filter { it.isToday() }
                .sortedBy { it.date_debut }
                .forEach { event ->
                    cursor.addRow(arrayOf(
                        event.id,
                        event.titre,
                        event.date_debut.toString(),
                        event.date_fin.toString(),
                        if (event.toute_journee) 1 else 0,
                        if (event.multi_jours) 1 else 0,
                        if (event.isCurrentlyRunning()) 1 else 0
                    ))
                }

            Log.d(TAG, "Retourné ${cursor.count} événements d'aujourd'hui")
            cursor
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des événements d'aujourd'hui", e)
            createEmptyCursor(projection)
        }
    }

    /**
     * Récupère les événements futurs
     */
    private fun getFutureEvents(projection: Array<String>?, sortOrder: String?): Cursor? = runBlocking {
        try {
            val jsonFileManager = DependencyInjection.getJsonFileManager()
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return@runBlocking createEmptyCursor(projection)

            val cursor = MatrixCursor(projection ?: EVENTS_COLUMNS)

            eventsData.evenements
                .filter { it.isFuture() }
                .sortedBy { it.date_debut }
                .forEach { event ->
                    cursor.addRow(arrayOf(
                        event.id,
                        event.titre,
                        event.date_debut.toString(),
                        event.date_fin.toString(),
                        if (event.toute_journee) 1 else 0,
                        if (event.multi_jours) 1 else 0,
                        if (event.isCurrentlyRunning()) 1 else 0
                    ))
                }

            Log.d(TAG, "Retourné ${cursor.count} événements futurs")
            cursor
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des événements futurs", e)
            createEmptyCursor(projection)
        }
    }

    /**
     * Crée un curseur vide
     */
    private fun createEmptyCursor(projection: Array<String>?): MatrixCursor {
        return MatrixCursor(projection ?: EVENTS_COLUMNS)
    }

    override fun getType(uri: Uri): String? {
        return when (URI_MATCHER.match(uri)) {
            EVENTS, TODAY_EVENTS, FUTURE_EVENTS -> "vnd.android.cursor.dir/vnd.syniorae.events"
            EVENT_BY_ID -> "vnd.android.cursor.item/vnd.syniorae.event"
            else -> null
        }
    }

    // Opérations d'écriture non supportées (lecture seule)
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.w(TAG, "Insert non supporté")
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        Log.w(TAG, "Delete non supporté")
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        Log.w(TAG, "Update non supporté")
        return 0
    }
}