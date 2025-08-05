package com.syniorae.presentation.fragments.configuration

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
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
 * Version mise à jour avec lancement du tunnel de configuration
 */
class ConfigurationViewModel : BaseViewModel() {

    // État de la vue
    private val _viewState = MutableStateFlow(ConfigurationViewState())
    val viewState = _viewState.asStateFlow()

    // Navigation
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Événement pour lancer une activité
    private val _activityLaunchEvent = MutableSharedFlow<Intent>()
    val activityLaunchEvent = _activityLaunchEvent.asSharedFlow()

    init {
        loadInitialState()
    }

    /**
     * Charge l'état initial
     */
    private fun loadInitialState() {
        // Pour l'instant, widget calendrier désactivé par défaut
        _viewState.value = ConfigurationViewState(
            calendarWidget = CalendarWidgetState(
                isEnabled = false,
                lastSyncDisplay = "Jamais",
                eventsCountDisplay = "0 événements",
                syncStatusDisplay = "✓ OK",
                hasError = false
            )
        )
    }

    /**
     * Active/désactive le widget calendrier
     */
    fun toggleCalendarWidget(isEnabled: Boolean) {
        executeWithLoading {
            if (isEnabled) {
                // Première activation → Lancer le tunnel de configuration
                // Note: Le contexte sera fourni par le Fragment lors de l'appel
                showMessage("Lancement de la configuration du calendrier...")
            } else {
                // Désactivation du widget
                _viewState.value = _viewState.value.copy(
                    calendarWidget = _viewState.value.calendarWidget.copy(
                        isEnabled = false
                    )
                )
                showMessage("Widget calendrier désactivé")
            }
        }
    }

    /**
     * Lance le tunnel de configuration du calendrier
     * Le contexte doit être fourni par le Fragment
     */
    fun launchCalendarConfiguration(context: Context) {
        viewModelScope.launch {
            val intent = CalendarConfigurationActivity.newIntent(context)
            _activityLaunchEvent.emit(intent)
        }
    }

    /**
     * Appelé quand la configuration est terminée avec succès
     */
    fun onConfigurationCompleted() {
        _viewState.value = _viewState.value.copy(
            calendarWidget = _viewState.value.calendarWidget.copy(
                isEnabled = true,
                lastSyncDisplay = "À l'instant",
                eventsCountDisplay = "Configuration terminée",
                syncStatusDisplay = "✓ Configuré"
            )
        )
        showMessage("Widget calendrier configuré avec succès !")
    }

    /**
     * Appelé quand la configuration est annulée
     */
    fun onConfigurationCancelled() {
        _viewState.value = _viewState.value.copy(
            calendarWidget = _viewState.value.calendarWidget.copy(
                isEnabled = false
            )
        )
        showMessage("Configuration annulée")
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
        executeWithLoading {
            showMessage("Synchronisation en cours...")

            // Simulation d'une synchronisation
            kotlinx.coroutines.delay(2000)

            // Mise à jour de l'état après sync
            _viewState.value = _viewState.value.copy(
                calendarWidget = _viewState.value.calendarWidget.copy(
                    lastSyncDisplay = "À l'instant",
                    eventsCountDisplay = "0 événements récupérés",
                    syncStatusDisplay = "✓ Synchronisé"
                )
            )

            showMessage("Synchronisation terminée")
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
    val syncStatusDisplay: String = "✓ OK",
    val hasError: Boolean = false
)