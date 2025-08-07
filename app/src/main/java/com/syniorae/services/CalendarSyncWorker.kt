package com.syniorae.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.repository.calendar.SyncResult
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.usecases.calendar.SyncCalendarUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker Android pour la synchronisation automatique du calendrier en arrière-plan
 * Utilise WorkManager pour une exécution fiable même si l'app est fermée
 */
class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CalendarSyncWorker"
        private const val WORK_NAME = "calendar_sync_work"
        private const val SYNC_FREQUENCY_KEY = "sync_frequency_hours"
        private const val FORCE_SYNC_KEY = "force_sync"
        private const val MAX_RETRY_ATTEMPTS = 3

        /**
         * Programme la synchronisation périodique
         */
        fun schedulePeriodicSync(context: Context, frequencyHours: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                frequencyHours.toLong(), TimeUnit.HOURS,
                30, TimeUnit.MINUTES // Fenêtre de flexibilité de 30 minutes
            )
                .setConstraints(constraints)
                .setInputData(
                    Data.Builder()
                        .putInt(SYNC_FREQUENCY_KEY, frequencyHours)
                        .putBoolean(FORCE_SYNC_KEY, false)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES // Retry après 15 min en cas d'échec
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    syncRequest
                )

            Log.d(TAG, "Synchronisation périodique programmée: ${frequencyHours}h")
        }

        /**
         * Lance une synchronisation immédiate (one-time)
         */
        fun scheduleImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                .setConstraints(constraints)
                .setInputData(
                    Data.Builder()
                        .putBoolean(FORCE_SYNC_KEY, true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "calendar_sync_immediate",
                    ExistingWorkPolicy.REPLACE,
                    immediateRequest
                )

            Log.d(TAG, "Synchronisation immédiate programmée")
        }

        /**
         * Annule toutes les synchronisations programmées
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("calendar_sync_immediate")
            Log.d(TAG, "Toutes les synchronisations annulées")
        }

        /**
         * Vérifie l'état des synchronisations programmées
         */
        suspend fun getSyncWorkState(context: Context): List<WorkInfo> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .await()
        }
    }

    private lateinit var syncCalendarUseCase: SyncCalendarUseCase

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Début de la synchronisation en arrière-plan")

                // Initialiser les dépendances
                if (!initializeDependencies()) {
                    Log.e(TAG, "Échec de l'initialisation des dépendances")
                    return@withContext Result.failure(createFailureData("Dépendances non disponibles"))
                }

                // Vérifier si le widget calendrier est actif
                if (!isCalendarWidgetActive()) {
                    Log.d(TAG, "Widget calendrier inactif, arrêt de la synchronisation")
                    return@withContext Result.success(createSuccessData("Widget inactif"))
                }

                // Déterminer si c'est un sync forcé
                val forceSync = inputData.getBoolean(FORCE_SYNC_KEY, false)

                // Effectuer la synchronisation
                val syncResult = if (forceSync) {
                    syncCalendarUseCase.syncWithRetry(MAX_RETRY_ATTEMPTS)
                } else {
                    syncCalendarUseCase.execute()
                }

                // Traiter le résultat
                when (syncResult) {
                    is SyncResult.Success -> {
                        Log.d(TAG, "Synchronisation réussie: ${syncResult.eventsCount} événements")
                        updateLastSyncTime()
                        Result.success(createSuccessData("${syncResult.eventsCount} événements synchronisés"))
                    }
                    is SyncResult.Error -> {
                        Log.w(TAG, "Échec de la synchronisation: ${syncResult.message}")

                        // Décider si c'est un échec temporaire ou permanent
                        if (isRetryableError(syncResult.message)) {
                            Result.retry()
                        } else {
                            Result.failure(createFailureData(syncResult.message))
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de la synchronisation", e)
                Result.failure(createFailureData("Exception: ${e.message}"))
            }
        }
    }

    /**
     * Initialise les dépendances nécessaires
     */
    private fun initializeDependencies(): Boolean {
        return try {
            // S'assurer que l'injection de dépendances est initialisée
            if (!DependencyInjection.isInitialized()) {
                DependencyInjection.initialize(applicationContext)
            }

            syncCalendarUseCase = SyncCalendarUseCase(
                calendarRepository = DependencyInjection.getCalendarRepository(),
                widgetRepository = DependencyInjection.getWidgetRepository()
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation des dépendances", e)
            false
        }
    }

    /**
     * Vérifie si le widget calendrier est actif
     */
    private suspend fun isCalendarWidgetActive(): Boolean {
        return try {
            val widgetRepository = DependencyInjection.getWidgetRepository()
            val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
            calendarWidget?.isActive() == true
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification du widget", e)
            false
        }
    }

    /**
     * Met à jour le timestamp de dernière synchronisation
     */
    private fun updateLastSyncTime() {
        try {
            val prefs = applicationContext.getSharedPreferences("syniorae_sync", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour du timestamp", e)
        }
    }

    /**
     * Détermine si une erreur peut être retentée
     */
    private fun isRetryableError(errorMessage: String): Boolean {
        val retryableKeywords = listOf(
            "network", "timeout", "connection", "server", "unavailable",
            "rate limit", "quota", "temporary", "503", "502", "500"
        )

        return retryableKeywords.any { keyword ->
            errorMessage.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * Crée des données de résultat de succès
     */
    private fun createSuccessData(message: String): Data {
        return Data.Builder()
            .putString("result", "success")
            .putString("message", message)
            .putLong("sync_time", System.currentTimeMillis())
            .build()
    }

    /**
     * Crée des données de résultat d'échec
     */
    private fun createFailureData(error: String): Data {
        return Data.Builder()
            .putString("result", "failure")
            .putString("error", error)
            .putLong("failure_time", System.currentTimeMillis())
            .build()
    }
}

/**
 * Utilitaires pour gérer les workers de synchronisation
 */
object SyncWorkerUtils {

    private const val TAG = "SyncWorkerUtils"

    /**
     * Démarre la synchronisation automatique avec WorkManager
     */
    fun startAutoSync(context: Context, frequencyHours: Int) {
        try {
            CalendarSyncWorker.schedulePeriodicSync(context, frequencyHours)
            Log.d(TAG, "Synchronisation automatique démarrée (${frequencyHours}h)")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du démarrage de la sync auto", e)
        }
    }

    /**
     * Arrête la synchronisation automatique
     */
    fun stopAutoSync(context: Context) {
        try {
            CalendarSyncWorker.cancelAllSync(context)
            Log.d(TAG, "Synchronisation automatique arrêtée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'arrêt de la sync auto", e)
        }
    }

    /**
     * Force une synchronisation immédiate
     */
    fun forceSyncNow(context: Context) {
        try {
            CalendarSyncWorker.scheduleImmediateSync(context)
            Log.d(TAG, "Synchronisation immédiate lancée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sync immédiate", e)
        }
    }

    /**
     * Met à jour la fréquence de synchronisation
     */
    fun updateSyncFrequency(context: Context, newFrequencyHours: Int) {
        try {
            // Annuler l'ancien planning
            CalendarSyncWorker.cancelAllSync(context)

            // Programmer avec la nouvelle fréquence
            CalendarSyncWorker.schedulePeriodicSync(context, newFrequencyHours)

            Log.d(TAG, "Fréquence de sync mise à jour: ${newFrequencyHours}h")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour de fréquence", e)
        }
    }

    /**
     * Obtient des statistiques sur les synchronisations
     */
    suspend fun getSyncStats(context: Context): SyncStats {
        return try {
            val workInfos = CalendarSyncWorker.getSyncWorkState(context)
            val prefs = context.getSharedPreferences("syniorae_sync", Context.MODE_PRIVATE)

            val lastSyncTime = prefs.getLong("last_sync_time", 0L)
            val totalSyncs = prefs.getInt("total_syncs", 0)
            val lastFailureTime = prefs.getLong("last_failure_time", 0L)

            val isScheduled = workInfos.any { it.state != WorkInfo.State.CANCELLED }
            val isRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }

            SyncStats(
                isScheduled = isScheduled,
                isCurrentlyRunning = isRunning,
                lastSyncTime = lastSyncTime,
                totalSyncs = totalSyncs,
                lastFailureTime = lastFailureTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des stats", e)
            SyncStats()
        }
    }

    /**
     * Nettoie les anciens logs de WorkManager
     */
    fun cleanupOldWorkLogs(context: Context) {
        try {
            WorkManager.getInstance(context).pruneWork()
            Log.d(TAG, "Nettoyage des anciens logs WorkManager effectué")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du nettoyage", e)
        }
    }
}

/**
 * Statistiques de synchronisation
 */
data class SyncStats(
    val isScheduled: Boolean = false,
    val isCurrentlyRunning: Boolean = false,
    val lastSyncTime: Long = 0L,
    val totalSyncs: Int = 0,
    val lastFailureTime: Long = 0L
) {
    fun getTimeSinceLastSync(): String {
        if (lastSyncTime == 0L) return "Jamais synchronisé"

        val diffMs = System.currentTimeMillis() - lastSyncTime
        val diffHours = diffMs / (1000 * 60 * 60)

        return when {
            diffHours < 1 -> "Il y a moins d'une heure"
            diffHours < 24 -> "Il y a ${diffHours}h"
            else -> "Il y a ${diffHours / 24}j"
        }
    }

    fun hasRecentFailure(): Boolean {
        if (lastFailureTime == 0L) return false
        val diffHours = (System.currentTimeMillis() - lastFailureTime) / (1000 * 60 * 60)
        return diffHours < 24 // Échec dans les dernières 24h
    }
}