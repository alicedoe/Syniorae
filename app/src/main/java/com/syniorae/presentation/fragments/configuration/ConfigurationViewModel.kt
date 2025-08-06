package com.syniorae.presentation.fragments.configuration

import androidx.lifecycle.viewModelScope
import com.syniorae.data.repository.widgets.WidgetRepository
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.presentation.activities.CalendarConfigurationActivity
import com.syniorae.presentation.common.BaseViewModel
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la page de configuration des widgets (Page 2)
 * Version complète avec WidgetRepository
 */
class ConfigurationViewModel(
    private val widgetRepository: WidgetRepository
) : BaseViewModel() {

    // Récupérer le CalendarRepository via DI
    private val calendarRepository = com.syniorae.core.di.DependencyInjection.getCalendarRepository()

    // État de la vue
    private val _viewState = MutableStateFlow(ConfigurationViewState())
    val viewState = _viewState.asStateFlow()

    // Navigation
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Événement pour lancer le tunnel de configuration
    private val _configurationLaunchEvent = MutableSharedFlow<Unit>()
    val configurationLaunchEvent = _configurationLaunchEvent.asSharedFlow()

    init {
        loadInitialState()
        observeWidgetChanges()
    }

    /**
     * Charge l'état initial depuis le WidgetRepository
     */
    private fun loadInitialState() {
        executeWithLoading {
            val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
            updateUIFromWidget(calendarWidget)
        }
    }

    /**
     * Observe les changements de widgets (pour les mises à jour en temps réel)
     */
    private fun observeWidgetChanges() {
        viewModelScope.launch {
            widgetRepository.widgets.collect { widgets ->
                val calendarWidget = widgets.find { it.type == WidgetType.CALENDAR }
                updateUIFromWidget(calendarWidget)
            }
        }
    }

    /**
     * Met à jour l'UI selon l'état du widget
     */
    private fun updateUIFromWidget(calendarWidget: com.syniorae.domain.models.widgets.Widget?) {
        val calendarState = when {
            calendarWidget == null -> {
                // Widget non trouvé (cas anormal)
                CalendarWidgetState(
                    isEnabled = false,
                    lastSyncDisplay = "Widget non trouvé",
                    eventsCountDisplay = "0 événements",
                    syncStatusDisplay = "Erreur système",
                    hasError = true
                )
            }
            calendarWidget.isActive() -> {
                // Widget actif et configuré
                CalendarWidgetState(
                    isEnabled = true,
                    lastSyncDisplay = "Configuré - Prêt",
                    eventsCountDisplay = "Prêt à synchroniser",
                    syncStatusDisplay = "✓ Configuré",
                    hasError = false
                )
            }
            calendarWidget.status == com.syniorae.domain.models.widgets.WidgetStatus.CONFIGURING -> {
                // Widget en cours de configuration
                CalendarWidgetState(
                    isEnabled = true,
                    lastSyncDisplay = "Configuration...",
                    eventsCountDisplay = "En cours",
                    syncStatusDisplay = "🔧 Configuration en cours",
                    hasError = false
                )
            }
            calendarWidget.hasError() -> {
                // Widget en erreur
                CalendarWidgetState(
                    isEnabled = false,
                    lastSyncDisplay = "Erreur",
                    eventsCountDisplay = "0 événements",
                    syncStatusDisplay = "✗ ${calendarWidget.errorMessage ?: "Erreur"}",
                    hasError = true
                )
            }
            else -> {
                // Widget désactivé (état par défaut)
                CalendarWidgetState(
                    isEnabled = false,
                    lastSyncDisplay = "Jamais",
                    eventsCountDisplay = "0 événements",
                    syncStatusDisplay = "Widget désactivé",
                    hasError = false
                )
            }
        }

        _viewState.value = _viewState.value.copy(calendarWidget = calendarState)
    }

    /**
     * Active/désactive le widget calendrier
     */
    fun toggleCalendarWidget(isEnabled: Boolean) {
        executeWithLoading {
            if (isEnabled) {
                // Vérifier si déjà configuré
                val hasConfig = widgetRepository.hasConfiguration(WidgetType.CALENDAR)

                if (hasConfig) {
                    // Réactiver directement
                    val success = widgetRepository.enableWidget(WidgetType.CALENDAR)
                    if (success) {
                        showMessage("Widget réactivé")
                    } else {
                        showError("Impossible de réactiver le widget")
                    }
                } else {
                    // Marquer en mode configuration et lancer le tunnel
                    widgetRepository.setWidgetConfiguring(WidgetType.CALENDAR)
                    launchCalendarConfiguration()
                }
            } else {
                // Désactiver le widget
                val success = widgetRepository.disableWidget(WidgetType.CALENDAR)
                if (success) {
                    showMessage("Widget désactivé")
                } else {
                    showError("Impossible de désactiver le widget")
                }
            }
        }
    }

    /**
     * Lance le tunnel de configuration
     */
    private suspend fun launchCalendarConfiguration() {
        _configurationLaunchEvent.emit(Unit)
    }

    /**
     * Appelé quand la configuration est terminée avec succès
     */
    fun onConfigurationCompleted() {
        executeWithLoading {
            // Le WidgetRepository a déjà été mis à jour par CalendarConfigurationViewModel
            // On affiche juste un message de confirmation
            showMessage("Widget calendrier configuré avec succès !")

            // Forcer le rechargement de l'état
            loadInitialState()
        }
    }

    /**
     * Appelé quand la configuration est annulée
     */
    fun onConfigurationCancelled() {
        executeWithLoading {
            // Remettre le widget en OFF
            val success = widgetRepository.disableWidget(WidgetType.CALENDAR)
            if (success) {
                showMessage("Configuration annulée")
            } else {
                showError("Erreur lors de l'annulation")
            }
        }
    }

    /**
     * Navigue vers les paramètres détaillés du calendrier
     */
    fun navigateToCalendarSettings() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToSettings)
        }
    }

    /**
     * Lance la synchronisation du calendrier
     */
    fun syncCalendar() {
        val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)

        if (calendarWidget?.isActive() != true) {
            showError("Le widget doit être configuré avant la synchronisation")
            return
        }

        executeWithLoading {
            showMessage("Synchronisation en cours...")

            // Utiliser le CalendarRepository pour la synchronisation
            when (val result = calendarRepository.syncCalendar()) {
                is com.syniorae.data.repository.calendar.SyncResult.Success -> {
                    showMessage("Synchronisation terminée - ${result.eventsCount} événements récupérés")
                }
                is com.syniorae.data.repository.calendar.SyncResult.Error -> {
                    showError("Erreur de synchronisation: ${result.message}")
                }
            }
        }
    }

    /**
     * Supprime complètement la configuration du calendrier
     */
    fun deleteCalendarConfiguration() {
        executeWithLoading {
            val success = widgetRepository.deleteWidgetConfiguration(WidgetType.CALENDAR)
            if (success) {
                showMessage("Configuration supprimée")
            } else {
                showError("Erreur lors de la suppression")
            }
        }
    }

    /**
     * Restaure la configuration depuis les backups
     */
    fun restoreCalendarConfiguration() {
        executeWithLoading {
            val success = widgetRepository.restoreWidgetConfiguration(WidgetType.CALENDAR)
            if (success) {
                showMessage("Configuration restaurée depuis le backup")
            } else {
                showError("Aucun backup trouvé ou erreur lors de la restauration")
            }
        }
    }
}

/**
 * État de la vue de configuration
 */
data class ConfigurationViewState(
    val calendarWidget: CalendarWidgetState = CalendarWidgetState()
)

/**
 * État du widget calendrier
 */
data class CalendarWidgetState(
    val isEnabled: Boolean = false,
    val lastSyncDisplay: String = "Jamais",
    val eventsCountDisplay: String = "0 événements",
    val syncStatusDisplay: String = "Widget désactivé",
    val hasError: Boolean = false
)