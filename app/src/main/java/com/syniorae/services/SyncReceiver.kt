package com.syniorae.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import kotlinx.coroutines.*

/**
 * Récepteur pour les alarmes de synchronisation
 * Version corrigée sans fuites mémoire
 */
class SyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SyncReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != "com.syniorae.SYNC_CALENDAR") {
            return
        }

        Log.d(TAG, "Alarme de synchronisation reçue")

        // Utiliser goAsync() pour prolonger la durée de vie du BroadcastReceiver
        val pendingResult = goAsync()

        // Créer un scope limité pour cette opération uniquement
        val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        syncScope.launch {
            try {
                // Créer les repositories avec le nouveau système DI (pas de fuites mémoire)
                val widgetRepository = DependencyInjection.getWidgetRepository(context.applicationContext)
                val calendarRepository = DependencyInjection.getCalendarRepository(context.applicationContext)

                // Vérifier si le widget calendrier est actif
                val calendarWidget = widgetRepository.getWidget(
                    com.syniorae.domain.models.widgets.WidgetType.CALENDAR
                )

                if (calendarWidget?.isActive() == true) {
                    Log.d(TAG, "Lancement de la synchronisation automatique")

                    // Utiliser withTimeout pour éviter que la synchronisation traîne
                    withTimeout(30_000) { // 30 secondes maximum
                        when (val result = calendarRepository.syncCalendar()) {
                            is com.syniorae.data.repository.calendar.SyncResult.Success -> {
                                Log.d(TAG, "Synchronisation automatique réussie - ${result.eventsCount} événements")
                            }
                            is com.syniorae.data.repository.calendar.SyncResult.Error -> {
                                Log.w(TAG, "Erreur de synchronisation automatique: ${result.message}")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Widget calendrier inactif, synchronisation annulée")
                }

            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Timeout lors de la synchronisation automatique")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation automatique", e)
            } finally {
                // IMPORTANT: Nettoyer le scope et terminer l'opération async
                syncScope.cancel("Synchronisation terminée")
                pendingResult.finish()
            }
        }
    }
}