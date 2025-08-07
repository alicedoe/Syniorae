package com.syniorae.domain.coordinators

import android.content.Context
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.*
import com.syniorae.data.remote.google.GoogleCalendarResponseParser
import com.syniorae.domain.models.sync.*
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

/**
 * Coordinateur principal pour la synchronisation du calendrier
 * Orchestre toutes les opérations de sync entre Google Calendar et les fichiers JSON
 */
class CalendarSyncCoordinator(private val context: Context) {

    companion object {
        private const val TAG = "CalendarSyncCoordinator"
    }

    // Composants injectés
    private val jsonFileManager = DependencyInjection.getJsonFileManager()
    private val googleAuthManager = DependencyInjection.getGoogleAuthManager()
    private val googleCalendarApi = DependencyInjection.getGoogleCalendarApi()

    // État de synchronisation en temps réel
    private val _syncState = MutableStateFlow(
        SyncState(
            status = SyncStatus.NEVER_SYNCED,
            lastSyncTime = null,
            nextSyncTime = null
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Lance une synchronisation complète
     */
    suspend fun performFullSync(): SyncResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Début de synchronisation complète")

        // Mettre à jour l'état
        _syncState.value = _syncState.value.copy(
            status = SyncStatus.IN_PROGRESS,
            retryCount = 0
        )

        return try {
            // 1. Vérifier la configuration
            val config = loadConfiguration()
                ?: return createErrorResult("Configuration manquante", startTime)

            // 2. Vérifier l'authentification Google
            if (!googleAuthManager.isSignedIn()) {
                return createErrorResult("Utilisateur non connecté", startTime)
            }

            // 3. Récupérer les événements depuis Google Calendar
            val events = fetchEventsFromGoogle(config)
                ?: return createErrorResult("Impossible de récupérer les événements", startTime)

            // 4. Traiter et enrichir les événements
            val processedEvents = processEvents(events)

            // 5. Sauvegarder dans le fichier JSON
            val saveSuccess = saveEventsToJson(processedEvents, config)
            if (!saveSuccess) {
                return createErrorResult("Échec de la sauvegarde", startTime)
            }

            // 6. Mettre à jour l'état et programmer la prochaine sync
            val duration = System.currentTimeMillis() - startTime
            updateSyncSuccess(processedEvents.size, duration, config.frequence_synchro)

            Log.d(TAG, "Synchronisation réussie - ${processedEvents.size} événements")
            SyncResult.success(processedEvents.size, duration)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la synchronisation", e)
            val duration = System.currentTimeMillis() - startTime
            updateSyncError(e.message ?: "Erreur inconnue")
            SyncResult.error(e.message ?: "Erreur inconnue", null, duration)
        }
    }

    /**
     * Synchronisation avec retry automatique
     */
    suspend fun performSyncWithRetry(maxRetries: Int = 3): SyncResult {
        var lastResult: SyncResult? = null

        for (attempt in 1..maxRetries) {
            Log.d(TAG, "Tentative de synchronisation $attempt/$maxRetries")

            _syncState.value = _syncState.value.copy(retryCount = attempt - 1)

            lastResult = performFullSync()

            if (lastResult.isSuccess) {
                return lastResult
            }

            // Attendre avant la prochaine tentative (sauf pour la dernière)
            if (attempt < maxRetries) {
                val delayMs = calculateRetryDelay(attempt)
                Log.d(TAG, "Attente de ${delayMs}ms avant retry")
                kotlinx.coroutines.delay(delayMs)
            }
        }

        return lastResult ?: SyncResult.error("Toutes les tentatives ont échoué")
    }

    /**
     * Charge la configuration du calendrier
     */
    private suspend fun loadConfiguration(): ConfigurationJsonModel? {
        return try {
            jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.CONFIG,
                ConfigurationJsonModel::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement configuration", e)
            null
        }
    }

    /**
     * Récupère les événements depuis Google Calendar
     */
    private suspend fun fetchEventsFromGoogle(config: ConfigurationJsonModel): List<EventJsonModel>? {
        return try {
            Log.d(TAG, "Récupération événements pour calendrier ${config.calendrier_id}")

            googleCalendarApi.getCalendarEvents(
                calendarId = config.calendrier_id,
                maxResults = config.nb_evenements_max,
                weeksAhead = config.nb_semaines_max
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur récupération événements Google", e)
            null
        }
    }

    /**
     * Traite et enrichit les événements récupérés
     */
    private fun processEvents(events: List<EventJsonModel>): List<EventJsonModel> {
        val now = LocalDateTime.now()

        return events.map { event ->
            event.copy(
                // Marquer les événements en cours
                en_cours = event.isCurrentlyRunning(),
                // Nettoyer et normaliser les titres
                titre = event.titre.trim().takeIf { it.isNotBlank() } ?: "Événement sans titre"
            )
        }.filter { event ->
            // Filtrer les événements trop anciens (plus de 1 jour dans le passé)
            event.date_fin.isAfter(now.minusDays(1))
        }.sortedBy { it.date_debut }
    }

    /**
     * Sauvegarde les événements dans le fichier JSON
     */
    private suspend fun saveEventsToJson(
        events: List<EventJsonModel>,
        config: ConfigurationJsonModel
    ): Boolean {
        return try {
            val eventsModel = EventsJsonModel(
                derniere_synchro = LocalDateTime.now(),
                statut = "success",
                nb_evenements_recuperes = events.size,
                evenements = events
            )

            jsonFileManager.writeJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                eventsModel
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur sauvegarde événements", e)
            false
        }
    }

    /**
     * Met à jour l'état après un succès
     */
    private fun updateSyncSuccess(eventsCount: Int, durationMs: Long, frequencyHours: Int) {
        val now = LocalDateTime.now()
        _syncState.value = SyncState(
            status = SyncStatus.SUCCESS,
            lastSyncTime = now,
            nextSyncTime = now.plusHours(frequencyHours.toLong()),
            eventsCount = eventsCount,
            syncDurationMs = durationMs,
            retryCount = 0
        )
    }

    /**
     * Met à jour l'état après une erreur
     */
    private fun updateSyncError(errorMessage: String) {
        _syncState.value = _syncState.value.copy(
            status = SyncStatus.ERROR,
            errorMessage = errorMessage,
            retryCount = _syncState.value.retryCount + 1
        )
    }

    /**
     * Calcule le délai de retry avec backoff exponentiel
     */
    private fun calculateRetryDelay(attemptNumber: Int): Long {
        val baseDelayMs = 5 * 60 * 1000L // 5 minutes
        return baseDelayMs * (1 shl (attemptNumber - 1)) // 2^(attempt-1)
    }

    /**
     * Crée un résultat d'erreur
     */
    private fun createErrorResult(message: String, startTime: Long): SyncResult {
        val duration = System.currentTimeMillis() - startTime
        updateSyncError(message)
        return SyncResult.error(message, null, duration)
    }

    /**
     * Vérifie si une synchronisation récente existe
     */
    suspend fun hasRecentSync(maxAgeHours: Int = 4): Boolean {
        return try {
            val eventsData = jsonFileManager.readJsonFile(
                WidgetType.CALENDAR,
                JsonFileType.DATA,
                EventsJsonModel::class.java
            ) ?: return false

            val lastSync = eventsData.derniere_synchro ?: return false
            val cutoff = LocalDateTime.now().minusHours(maxAgeHours.toLong())

            lastSync.isAfter(cutoff)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur vérification sync récente", e)
            false
        }
    }

    /**
     * Force l'annulation d'une synchronisation en cours
     */
    fun cancelSync() {
        if (_syncState.value.isInProgress()) {
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.CANCELLED
            )
            Log.d(TAG, "Synchronisation annulée")
        }
    }

    /**
     * Programme la prochaine synchronisation
     */
    fun scheduleNextSync(delayHours: Int) {
        val nextSync = LocalDateTime.now().plusHours(delayHours.toLong())
        _syncState.value = _syncState.value.copy(
            status = SyncStatus.SCHEDULED,
            nextSyncTime = nextSync
        )

        // Programmer l'alarme système
        com.syniorae.services.SyncScheduler.scheduleSyncAlarm(context, delayHours)
        Log.d(TAG, "Prochaine synchronisation programmée: $nextSync")
    }

    /**
     * Annule la synchronisation programmée
     */
    fun cancelScheduledSync() {
        com.syniorae.services.SyncScheduler.cancelSyncAlarm(context)
        _syncState.value = _syncState.value.copy(
            status = SyncStatus.SUCCESS,
            nextSyncTime = null
        )
        Log.d(TAG, "Synchronisation programmée annulée")
    }

    /**
     * Nettoie les anciennes données de synchronisation
     */
    suspend fun cleanup(): Boolean {
        return try {
            jsonFileManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du nettoyage", e)
            false
        }
    }

    /**
     * Exporte les statistiques de synchronisation
     */
    suspend fun exportSyncStatistics(): SyncStatistics {
        return try {
            // TODO: Implémenter la collecte de statistiques depuis les logs ou fichiers
            SyncStatistics(
                totalSyncs = 0,
                successfulSyncs = 0,
                failedSyncs = 0,
                totalEventsRetrieved = _syncState.value.eventsCount,
                averageDurationMs = _syncState.value.syncDurationMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur export statistiques", e)
            SyncStatistics()
        }
    }

    /**
     * Teste la connectivité avec Google Calendar
     */
    suspend fun testConnectivity(): Boolean {
        return try {
            if (!googleAuthManager.isSignedIn()) {
                Log.d(TAG, "Utilisateur non connecté")
                return false
            }

            googleCalendarApi.checkApiAccess()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur test connectivité", e)
            false
        }
    }
}