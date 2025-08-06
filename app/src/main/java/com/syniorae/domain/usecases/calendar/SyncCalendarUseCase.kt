package com.syniorae.domain.usecases.calendar

import com.syniorae.data.repository.calendar.CalendarRepository
import com.syniorae.data.repository.calendar.SyncResult
import com.syniorae.data.repository.widgets.WidgetRepository
import com.syniorae.domain.models.widgets.WidgetType

/**
 * Use case pour synchroniser le calendrier Google
 */
class SyncCalendarUseCase(
    private val calendarRepository: CalendarRepository,
    private val widgetRepository: WidgetRepository
) {

    /**
     * Synchronise le calendrier si nécessaire
     * @param forceSync Force la synchronisation même si récente
     * @return Résultat de la synchronisation
     */
    suspend fun execute(forceSync: Boolean = false): SyncResult {
        return try {
            // Vérifier que le widget est actif
            val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
            if (calendarWidget?.isActive() != true) {
                return SyncResult.Error("Widget calendrier non configuré")
            }

            // Vérifier si une synchronisation récente existe
            if (!forceSync && calendarRepository.hasRecentSync()) {
                return SyncResult.Success(0) // Pas de sync nécessaire
            }

            // Lancer la synchronisation
            calendarRepository.syncCalendar()

        } catch (e: Exception) {
            SyncResult.Error("Erreur inattendue: ${e.message}")
        }
    }

    /**
     * Vérifie si une synchronisation est nécessaire
     */
    suspend fun isSyncNeeded(): Boolean {
        val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
        if (calendarWidget?.isActive() != true) {
            return false
        }

        return !calendarRepository.hasRecentSync()
    }

    /**
     * Synchronisation avec gestion d'erreurs et retry
     */
    suspend fun syncWithRetry(maxRetries: Int = 3): SyncResult {
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            when (val result = execute(forceSync = true)) {
                is SyncResult.Success -> return result
                is SyncResult.Error -> {
                    lastError = result.message
                    if (attempt < maxRetries - 1) {
                        // Attendre avant de réessayer (backoff exponentiel)
                        kotlinx.coroutines.delay((1000 * (attempt + 1)).toLong())
                    }
                }
            }
        }

        return SyncResult.Error("Échec après $maxRetries tentatives. Dernière erreur: $lastError")
    }
}