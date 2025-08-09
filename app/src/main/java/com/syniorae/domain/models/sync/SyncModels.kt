package com.syniorae.domain.models.sync

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * État détaillé d'une synchronisation
 */
data class SyncState(
    val status: SyncStatus,
    val lastSyncTime: LocalDateTime?,
    val nextSyncTime: LocalDateTime?,
    val eventsCount: Int = 0,
    val errorMessage: String? = null,
    val syncDurationMs: Long = 0L,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
) {

    /**
     * Vérifie si la synchronisation est en cours
     */
    fun isInProgress(): Boolean = status == SyncStatus.IN_PROGRESS

    /**
     * Vérifie si la synchronisation a échoué
     */
    fun hasFailed(): Boolean = status == SyncStatus.ERROR

    /**
     * Vérifie si la synchronisation est réussie
     */
    fun isSuccessful(): Boolean = status == SyncStatus.SUCCESS

    /**
     * Vérifie si des tentatives de retry sont encore possibles
     */
    fun canRetry(): Boolean = hasFailed() && retryCount < maxRetries

    /**
     * Calcule le délai depuis la dernière synchronisation
     */
    fun getTimeSinceLastSync(): String {
        if (lastSyncTime == null) return "Jamais"

        val now = LocalDateTime.now()
        val diffMinutes = java.time.Duration.between(lastSyncTime, now).toMinutes()

        return when {
            diffMinutes < 1 -> "À l'instant"
            diffMinutes < 60 -> "Il y a ${diffMinutes}min"
            diffMinutes < 1440 -> "Il y a ${diffMinutes / 60}h"
            else -> "Il y a ${diffMinutes / 1440}j"
        }
    }

    /**
     * Retourne le temps estimé jusqu'à la prochaine synchronisation
     */
    fun getTimeUntilNextSync(): String? {
        if (nextSyncTime == null) return null

        val now = LocalDateTime.now()
        if (nextSyncTime.isBefore(now)) return "En retard"

        val diffMinutes = java.time.Duration.between(now, nextSyncTime).toMinutes()

        return when {
            diffMinutes < 1 -> "Maintenant"
            diffMinutes < 60 -> "Dans ${diffMinutes}min"
            diffMinutes < 1440 -> "Dans ${diffMinutes / 60}h"
            else -> "Dans ${diffMinutes / 1440}j"
        }
    }

    /**
     * Formate la durée de synchronisation
     */
    fun getFormattedDuration(): String {
        return when {
            syncDurationMs < 1000 -> "${syncDurationMs}ms"
            syncDurationMs < 60000 -> "${syncDurationMs / 1000}s"
            else -> "${syncDurationMs / 60000}min ${(syncDurationMs % 60000) / 1000}s"
        }
    }

    /**
     * Retourne un message d'état pour l'affichage
     */
    fun getDisplayMessage(): String {
        return when (status) {
            SyncStatus.NEVER_SYNCED -> "Première synchronisation nécessaire"
            SyncStatus.SUCCESS -> "✓ Synchronisé • ${getTimeSinceLastSync()}"
            SyncStatus.IN_PROGRESS -> "🔄 Synchronisation en cours..."
            SyncStatus.ERROR -> "✗ Erreur • ${errorMessage ?: "Échec"}"
            SyncStatus.CANCELLED -> "Synchronisation annulée"
            SyncStatus.SCHEDULED -> "Programmée ${getTimeUntilNextSync() ?: ""}"
        }
    }

    /**
     * Retourne les détails techniques pour le debug
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("Statut: $status")
            appendLine("Dernière sync: ${lastSyncTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "Jamais"}")
            appendLine("Prochaine sync: ${nextSyncTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "Non programmée"}")
            appendLine("Événements: $eventsCount")
            appendLine("Durée: ${getFormattedDuration()}")
            appendLine("Tentatives: $retryCount/$maxRetries")
            if (errorMessage != null) appendLine("Erreur: $errorMessage")
        }
    }
}

/**
 * États possibles de synchronisation
 */
enum class SyncStatus {
    NEVER_SYNCED,   // Jamais synchronisé
    SUCCESS,        // Dernière sync réussie
    IN_PROGRESS,    // Synchronisation en cours
    ERROR,          // Erreur lors de la dernière sync
    CANCELLED,      // Synchronisation annulée
    SCHEDULED       // Synchronisation programmée
}

/**
 * Configuration de synchronisation pour un widget
 */
data class SyncConfiguration(
    val widgetType: String,
    val isEnabled: Boolean = true,
    val frequencyHours: Int = 4,
    val autoRetryEnabled: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMinutes: Int = 5,
    val syncOnlyOnWifi: Boolean = false,
    val syncOnlyWhenCharging: Boolean = false,
    val quietHoursStart: Int? = null,  // Heure de début des heures silencieuses (0-23)
    val quietHoursEnd: Int? = null     // Heure de fin des heures silencieuses (0-23)
) {

    /**
     * Vérifie si on est actuellement dans les heures silencieuses
     */
    fun isInQuietHours(): Boolean {
        if (quietHoursStart == null || quietHoursEnd == null) return false

        val currentHour = LocalDateTime.now().hour

        return if (quietHoursStart <= quietHoursEnd) {
            // Heures silencieuses dans la même journée (ex: 22h-6h)
            currentHour in quietHoursStart..quietHoursEnd
        } else {
            // Heures silencieuses à cheval sur deux jours (ex: 23h-7h)
            currentHour >= quietHoursStart || currentHour <= quietHoursEnd
        }
    }

    /**
     * Calcule le délai avant la prochaine synchronisation
     */
    fun getNextSyncDelay(): Long {
        return frequencyHours * 60 * 60 * 1000L // en millisecondes
    }

    /**
     * Calcule le délai de retry en cas d'échec
     */
    fun getRetryDelay(attemptNumber: Int): Long {
        // Backoff exponentiel : 5min, 10min, 20min...
        val baseDelayMs = retryDelayMinutes * 60 * 1000L
        return baseDelayMs * (1 shl (attemptNumber - 1)) // 2^(attempt-1)
    }

    /**
     * Valide la configuration
     */
    fun isValid(): Boolean {
        return frequencyHours > 0 &&
                maxRetries >= 0 &&
                retryDelayMinutes > 0 &&
                (quietHoursStart == null || quietHoursStart in 0..23) &&
                (quietHoursEnd == null || quietHoursEnd in 0..23)
    }
}



/**
 * Statistiques de synchronisation sur une période
 */
data class SyncStatistics(
    val totalSyncs: Int = 0,
    val successfulSyncs: Int = 0,
    val failedSyncs: Int = 0,
    val totalEventsRetrieved: Int = 0,
    val averageDurationMs: Long = 0L,
    val lastWeekSyncs: Int = 0,
    val longestSuccessStreak: Int = 0,
    val currentSuccessStreak: Int = 0
) {

    /**
     * Calcule le taux de réussite
     */
    fun getSuccessRate(): Double {
        return if (totalSyncs > 0) {
            (successfulSyncs.toDouble() / totalSyncs) * 100
        } else {
            0.0
        }
    }

    /**
     * Calcule la moyenne d'événements par synchronisation
     */
    fun getAverageEventsPerSync(): Double {
        return if (successfulSyncs > 0) {
            totalEventsRetrieved.toDouble() / successfulSyncs
        } else {
            0.0
        }
    }

    /**
     * Retourne un résumé des statistiques
     */
    fun getSummary(): String {
        return buildString {
            appendLine("Total synchronisations: $totalSyncs")
            appendLine("Taux de réussite: ${"%.1f".format(getSuccessRate())}%")
            appendLine("Événements récupérés: $totalEventsRetrieved")
            appendLine("Durée moyenne: ${averageDurationMs / 1000}s")
            appendLine("Série de succès actuelle: $currentSuccessStreak")
        }
    }
}