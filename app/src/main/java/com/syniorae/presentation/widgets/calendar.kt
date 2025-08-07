package com.syniorae.presentation.widgets.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.syniorae.MainActivity
import com.syniorae.R
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.EventsJsonModel
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Provider pour le widget Android du calendrier
 * Affiche les événements sur l'écran d'accueil Android
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "CalendarWidget"
        const val ACTION_REFRESH = "com.syniorae.widget.REFRESH"
        const val ACTION_OPEN_APP = "com.syniorae.widget.OPEN_APP"
    }

    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate appelé pour ${appWidgetIds.size} widgets")

        // Mettre à jour chaque widget
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                Log.d(TAG, "Refresh demandé")
                refreshAllWidgets(context)
            }
            ACTION_OPEN_APP -> {
                Log.d(TAG, "Ouverture app demandée")
                openMainApp(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Premier widget activé")

        // Initialiser le système si nécessaire
        widgetScope.launch {
            DependencyInjection.initialize(context.applicationContext)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Dernier widget désactivé")
    }

    /**
     * Met à jour un widget spécifique
     */
    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        widgetScope.launch {
            try {
                DependencyInjection.initialize(context.applicationContext)

                val views = RemoteViews(context.packageName, R.layout.widget_calendar_layout)
                val events = loadTodayEvents(context)

                populateWidgetViews(context, views, events)
                setupWidgetActions(context, views)

                appWidgetManager.updateAppWidget(widgetId, views)
                Log.d(TAG, "Widget $widgetId mis à jour avec ${events.size} événements")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur mise à jour widget $widgetId", e)
                showErrorWidget(context, appWidgetManager, widgetId, e.message)
            }
        }
    }

    /**
     * Charge les événements d'aujourd'hui
     */
    private suspend fun loadTodayEvents(context: Context): List<TodayEvent> {
        return try {
            val jsonFileManager = DependencyInjection.getJsonFileManager()
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return emptyList()

            val today = LocalDateTime.now().toLocalDate()

            eventsData.evenements
                .filter { event -> event.date_debut.toLocalDate() == today }
                .sortedBy { it.date_debut }
                .take(5) // Limiter à 5 événements pour le widget
                .map { event ->
                    TodayEvent(
                        title = event.titre,
                        time = if (event.toute_journee) {
                            "Toute la journée"
                        } else {
                            event.date_debut.format(DateTimeFormatter.ofPattern("HH'h'mm"))
                        },
                        isCurrentlyRunning = event.isCurrentlyRunning(),
                        isAllDay = event.toute_journee
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement événements", e)
            emptyList()
        }
    }

    /**
     * Remplit les vues du widget avec les données
     */
    private fun populateWidgetViews(context: Context, views: RemoteViews, events: List<TodayEvent>) {
        // Date du jour
        val now = LocalDateTime.now()
        val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
        val dayOfMonth = now.format(DateTimeFormatter.ofPattern("d"))
        val monthYear = now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

        views.setTextViewText(R.id.widget_day_of_week, dayOfWeek.replaceFirstChar { it.uppercase() })
        views.setTextViewText(R.id.widget_day_of_month, dayOfMonth)
        views.setTextViewText(R.id.widget_month_year, monthYear)

        // Événements
        if (events.isEmpty()) {
            views.setTextViewText(R.id.widget_events_text, "Aucun événement")
            views.setViewVisibility(R.id.widget_events_list, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_events_text, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_events_text, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_events_list, android.view.View.VISIBLE)

            // Construire la liste des événements
            val eventsText = events.take(3).joinToString("\n") { event ->
                val indicator = if (event.isCurrentlyRunning) "🔴 " else ""
                "${indicator}${event.time} ${event.title}"
            }

            views.setTextViewText(R.id.widget_events_list, eventsText)

            // Afficher le nombre total si plus de 3
            if (events.size > 3) {
                views.setTextViewText(R.id.widget_more_events, "+${events.size - 3} autres")
                views.setViewVisibility(R.id.widget_more_events, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_more_events, android.view.View.GONE)
            }
        }

        // Dernière mise à jour
        val lastUpdate = now.format(DateTimeFormatter.ofPattern("HH:mm"))
        views.setTextViewText(R.id.widget_last_update, "Maj: $lastUpdate")
    }

    /**
     * Configure les actions du widget
     */
    private fun setupWidgetActions(context: Context, views: RemoteViews) {
        // Action d'ouverture de l'app
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_main_container, openAppPendingIntent)

        // Action de refresh
        val refreshIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
    }

    /**
     * Affiche un widget d'erreur
     */
    private fun showErrorWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, errorMessage: String?) {
        val views = RemoteViews(context.packageName, R.layout.widget_calendar_error_layout)

        views.setTextViewText(R.id.widget_error_message,
            errorMessage ?: "Erreur de chargement")

        // Action pour ouvrir l'app
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_error_container, openAppPendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /**
     * Rafraîchit tous les widgets
     */
    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, CalendarWidgetProvider::class.java)
        )

        widgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    /**
     * Ouvre l'application principale
     */
    private fun openMainApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

/**
 * Modèle pour un événement du jour dans le widget
 */
data class TodayEvent(
    val title: String,
    val time: String,
    val isCurrentlyRunning: Boolean = false,
    val isAllDay: Boolean = false
)

/**
 * Classe utilitaire pour la gestion des widgets
 */
object CalendarWidgetUtils {

    /**
     * Met à jour tous les widgets calendrier
     */
    fun updateAllWidgets(context: Context) {
        val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, CalendarWidgetProvider::class.java)
        )

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        context.sendBroadcast(intent)
    }

    /**
     * Vérifie si des widgets sont actifs
     */
    fun hasActiveWidgets(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, CalendarWidgetProvider::class.java)
        )
        return widgetIds.isNotEmpty()
    }

    /**
     * Obtient le nombre de widgets actifs
     */
    fun getActiveWidgetCount(context: Context): Int {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, CalendarWidgetProvider::class.java)
        )
        return widgetIds.size
    }
}