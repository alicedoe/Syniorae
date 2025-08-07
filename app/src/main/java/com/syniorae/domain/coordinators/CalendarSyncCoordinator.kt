package com.syniorae.domain.coordinators

import android.content.Context
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.repository.calendar.SyncResult
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.services.CalendarSyncService
import com.syniorae.services.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * Coordinateur pour orchestrer la synchronisation du calendrier
 * Gère les services, alarmes et états de synchronisation
 */
class CalendarSyncCoordinator(private val context: Context) {

    companion object {
        private const val TAG = "CalendarSyncCoordinator"
    }

    private val coordinatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val widgetRepository by lazy { DependencyInjection.getWidgetRepository() }
    private val calendarRepository by lazy { DependencyInjection.getCalendarRepository() }
    private val preferencesManager by lazy {
        com.syniorae.core.utils.PreferencesManager.getInstance(context)
    }

    /**
     * Active la synchronisation automatique du calendrier
     */
    fun enableAutoSync() {
        coordinatorScope.launch {
            try {
                val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)

                if (calendarWidget?.isActive() == true) {
                    // Démarrer le service de synchronisation
                    CalendarSyncService.start(context)

                    // Programmer les alarmes de synchronisation
                    val syncFrequency = preferencesManager.syncFrequencyHours
                    SyncScheduler.scheduleSyncAlarm(context, syncFrequency)

                    Log.d(TAG, "Synchronisation automatique activée (${syncFrequency}h)")
                } else {
                    Log.w(TAG, "Impossible d'activer la sync : widget calendrier inactif")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'activation de la synchronisation", e)
            }
        }
    }

    /**
     * Désactive la synchronisation automatique
     */
    fun disableAutoSync() {
        try {
            // Arrêter le service
            CalendarSyncService.stop(context)

            // Annuler les alarmes
            SyncScheduler.cancelSyncAlarm(context)

            Log.d(TAG, "Synchronisation automatique désactivée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la désactivation de la synchronisation", e)
        }
    }

    /**
     * Force une synchronisation immédiate
     */
    fun forceSyncNow(onResult: ((SyncResult) -> Unit)? = null) {
        coordinatorScope.launch {
            try {
                Log.d(TAG, "Synchronisation forcée demandée")

                val result = calendarRepository.forceSyncWithRetry()

                when (result) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Synchronisation forcée réussie - ${result.eventsCount} événements")
                        preferencesManager.recordSuccessfulSync()
                    }
                    is SyncResult.Error -> {
                        Log.w(TAG, "Échec de la synchronisation forcée: ${result.message}")
                    }
                }

                onResult?.invoke(result)
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation forcée", e)
                onResult?.invoke(SyncResult.Error("Erreur inattendue: ${e.message}"))
            }
        }
    }

    /**
     * Met à jour la fréquence de synchronisation
     */
    fun updateSyncFrequency(newFrequencyHours: Int) {
        coordinatorScope.launch {
            try {
                // Mettre à jour les préférences
                preferencesManager.syncFrequencyHours = newFrequencyHours

                // Reprogrammer les alarmes avec la nouvelle fréquence
                val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
                if (calendarWidget?.isActive() == true) {
                    SyncScheduler.cancelSyncAlarm(context)
                    SyncScheduler.scheduleSyncAlarm(context, newFrequencyHours)

                    Log.d(TAG, "Fréquence de synchronisation mise à jour : ${newFrequencyHours}h")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la mise à jour de la fréquence", e)
            }
        }
    }

    /**
     * Vérifie l'état de la synchronisation
     */
    suspend fun getSyncStatus(): SyncStatus {
        return try {
            val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)

            if (calendarWidget?.isActive() != true) {
                return SyncStatus.Disabled
            }

            val hasRecentSync = calendarRepository.hasRecentSync()
            val timeSinceLastSync = preferencesManager.getTimeSinceLastSync()

            when {
                hasRecentSync -> SyncStatus.UpToDate(timeSinceLastSync)
                preferencesManager.lastSyncTime == 0L -> SyncStatus.NeverSynced
                else -> SyncStatus.OutOfDate(timeSinceLastSync)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification du statut", e)
            SyncStatus.Error(e.message ?: "Erreur inconnue")
        }
    }

    /**
     * Teste la connectivité avec l'API Google Calendar
     */
    suspend fun testApiConnection(): Boolean {
        return try {
            calendarRepository.testApiConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du test de connexion", e)
            false
        }
    }

    /**
     * Nettoie les ressources du coordinateur
     */
    fun cleanup() {
        coordinatorScope.cancel()
    }
}

/**
 * États possibles de la synchronisation
 */
sealed class SyncStatus {
    object Disabled : SyncStatus()
    object NeverSynced : SyncStatus()
    data class UpToDate(val lastSyncTime: String) : SyncStatus()
    data class OutOfDate(val lastSyncTime: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()

    fun getDisplayMessage(): String {
        return when (this) {
            is Disabled -> "Synchronisation désactivée"
            is NeverSynced -> "Jamais synchronisé"
            is UpToDate -> "À jour ($lastSyncTime)"
            is OutOfDate -> "Mise à jour nécessaire ($lastSyncTime)"
            is Error -> "Erreur : $message"
        }
    }
}