package com.syniorae.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.repository.calendar.SyncResult
import com.syniorae.domain.usecases.calendar.SyncCalendarUseCase
import kotlinx.coroutines.*

/**
 * Service pour la synchronisation automatique du calendrier en arrière-plan
 */
class CalendarSyncService : Service() {

    companion object {
        private const val TAG = "CalendarSyncService"

        fun start(context: Context) {
            val intent = Intent(context, CalendarSyncService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CalendarSyncService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    private lateinit var syncCalendarUseCase: SyncCalendarUseCase

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service créé")

        // Initialiser le use case
        syncCalendarUseCase = SyncCalendarUseCase(
            calendarRepository = DependencyInjection.getCalendarRepository(),
            widgetRepository = DependencyInjection.getWidgetRepository()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service démarré")

        startPeriodicSync()

        // Le service redémarre automatiquement s'il est tué
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Pas de binding nécessaire
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service détruit")

        syncJob?.cancel()
        serviceScope.cancel()
    }

    /**
     * Démarre la synchronisation périodique
     */
    private fun startPeriodicSync() {
        syncJob?.cancel()

        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Vérifier si une sync est nécessaire
                    if (syncCalendarUseCase.isSyncNeeded()) {
                        Log.d(TAG, "Synchronisation nécessaire, lancement...")

                        when (val result = syncCalendarUseCase.execute()) {
                            is SyncResult.Success -> {
                                Log.d(TAG, "Synchronisation réussie - ${result.eventsCount} événements")
                            }
                            is SyncResult.Error -> {
                                Log.w(TAG, "Erreur de synchronisation: ${result.message}")
                            }
                        }
                    } else {
                        Log.d(TAG, "Synchronisation récente trouvée, pas de sync nécessaire")
                    }

                    // Attendre avant la prochaine vérification (15 minutes)
                    delay(15 * 60 * 1000L)

                } catch (e: Exception) {
                    Log.e(TAG, "Erreur dans la boucle de synchronisation", e)
                    delay(5 * 60 * 1000L) // Attendre 5 minutes en cas d'erreur
                }
            }
        }
    }

    /**
     * Force une synchronisation immédiate
     */
    fun forceSyncNow() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Synchronisation forcée demandée")

                when (val result = syncCalendarUseCase.syncWithRetry()) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Synchronisation forcée réussie - ${result.eventsCount} événements")
                    }
                    is SyncResult.Error -> {
                        Log.w(TAG, "Échec de la synchronisation forcée: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la synchronisation forcée", e)
            }
        }
    }
}