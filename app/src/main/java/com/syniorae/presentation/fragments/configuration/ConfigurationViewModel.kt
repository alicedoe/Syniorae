package com.syniorae.presentation.fragments.configuration

import androidx.lifecycle.viewModelScope
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.usecases.navigation.NavigationTarget
import com.syniorae.domain.usecases.navigation.NavigateToPageUseCase
import com.syniorae.domain.usecases.navigation.NavigationResult
import com.syniorae.domain.usecases.widgets.common.GetAllWidgetsUseCase
import com.syniorae.domain.usecases.widgets.common.ToggleWidgetUseCase
import com.syniorae.presentation.common.BaseViewModel
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la page de configuration des widgets (Page 2)
 */
class ConfigurationViewModel(
    private val getAllWidgetsUseCase: GetAllWidgetsUseCase,
    private val toggleWidgetUseCase: ToggleWidgetUseCase,
    private val navigateToPageUseCase: NavigateToPageUseCase
) : BaseViewModel() {

    // État de la vue
    private val _viewState = MutableStateFlow(ConfigurationViewState())
    val viewState = _viewState.asStateFlow()

    // Navigation
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadWidgets()
        observeWidgetChanges()
    }

    /**
     * Charge les widgets disponibles
     */
    private fun loadWidgets() {
        execute {
            val widgets = getAllWidgetsUseCase.execute()
            val calendarWidget = widgets.find { it.type == WidgetType.CALENDAR }

            val calendarWidgetState = if (calendarWidget != null) {
                CalendarWidgetState(
                    isEnabled = calendarWidget.isActive(),
                    lastSyncDisplay = formatLastUpdate(calendarWidget.lastUpdate),
                    eventsCountDisplay = "0 événements", // TODO: Récupérer le vrai nombre
                    syncStatusDisplay = if (calendarWidget.hasError()) "✗ Erreur" else "✓ OK",
                    hasError = calendarWidget.hasError()
                )
            } else {
                CalendarWidgetState()
            }

            _viewState.value = _viewState.value.copy(
                calendarWidget = calendarWidgetState
            )
        }
    }

    /**
     * Observe les changements de widgets
     */
    private fun observeWidgetChanges() {
        viewModelScope.launch {
            getAllWidgetsUseCase.observeWidgets().collect {
                loadWidgets()
            }
        }
    }

    /**
     * Active/désactive le widget calendrier
     */
    fun toggleCalendarWidget(isEnabled: Boolean) {
        executeWithLoading {
            if (isEnabled) {
                // Tentative d'activation du widget
                val result = toggleWidgetUseCase.enableWidget(WidgetType.CALENDAR)
                result.fold(
                    onSuccess = {
                        showMessage("Widget calendrier activé")
                    },
                    onFailure = { exception ->
                        // Si l'activation échoue, c'est probablement que le widget n'est pas configuré
                        // On doit lancer la configuration
                        showMessage("Configuration du widget calendrier requise")
                        // TODO: Lancer le tunnel de configuration
                    }
                )
            } else {
                // Désactivation du widget
                val result = toggleWidgetUseCase.disableWidget(WidgetType.CALENDAR)
                result.fold(
                    onSuccess = {
                        showMessage("Widget calendrier désactivé")
                    },
                    onFailure = { exception ->
                        handleError(exception)
                    }
                )
            }
        }
    }

    /**
     * Navigue vers les paramètres détaillés du calendrier
     */
    fun navigateToCalendarSettings() {
        val result = navigateToPageUseCase.execute(NavigationTarget.SETTINGS)
        when (result) {
            is NavigationResult.Success -> {
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.NavigateToSettings)
                }
            }
            is NavigationResult.Denied -> {
                showError(result.message)
            }
        }
    }

    /**
     * Lance la synchronisation du calendrier
     */
    fun syncCalendar() {
        executeWithLoading {
            // TODO: Implémenter la synchronisation réelle
            showMessage("Synchronisation en cours...")

            // Simulation d'une synchronisation
            kotlinx.coroutines.delay(2000)

            showMessage("Synchronisation terminée")
            loadWidgets() // Recharger les données
        }
    }

    /**
     * Formate la date de dernière mise à jour
     */
    private fun formatLastUpdate(timestamp: Long): String {
        if (timestamp == 0L) return "Jamais"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "À l'instant"
            diff < 3600_000 -> "${diff / 60_000} min"
            diff < 86400_000 -> "${diff / 3600_000}h"
            else -> "${diff / 86400_000}j"
        }
    }
}

/**
 * État de la vue de configuration
 */
data class ConfigurationViewState(
    val calendarWidget: CalendarWidgetState = CalendarWidgetState(),
    val isLoading: Boolean = false
)

/**
 * État du widget calendrier
 */
data class CalendarWidgetState(
    val isEnabled: Boolean = false,
    val lastSyncDisplay: String = "Jamais",
    val eventsCountDisplay: String = "0 événements",
    val syncStatusDisplay: String = "✓ OK",
    val hasError: Boolean = false
)