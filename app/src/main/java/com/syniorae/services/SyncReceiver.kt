package com.syniorae.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Récepteur pour les alarmes de synchronisation
 */
class SyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncReceiver"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != "com.syniorae.SYNC_CALENDAR") {
            return
        }

        Log.d(TAG, "Alarme de synchronisation reçue")

        receiverScope.launch {
            try {
                // Initialiser les dépendances si nécessaire
                DependencyInjection.initialize(context.applicationContext)

                val widgetRepository = DependencyInjection.getWidgetRepository()
                val calendarRepository = DependencyInjection.getCalendarRepository()

                // Vérifier si le widget calendrier est actif
                val calendarWidget = widgetRepository.getWidget(
                    com.syniorae.domain.models.widgets.WidgetType.CALENDAR
                )

                if (calendarWidget?.isActive() == true) {
                    Log.d(TAG, "Lancement de la synchronisation automatique")

                    when (val result = calendarRepository.syncCalendar()) {
                        is com.syniorae.data.repository.calendar.SyncResult.Success -> {
                            Log.d(TAG, "Synchronisation automatique réussie - ${result.eventsCount} événements")
                        }
                        is com.syniorae.data.repository.calendar.SyncResult.Error -> {
                            Log.w(TAG, "Erreur de synchronisation automatique: ${result.message}")
                        }
                    }
                } else {
                    Log.d(TAG, "Widget calendrier inactif, synchronisation annulée")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation automatique", e)
            }
        }
    }
}