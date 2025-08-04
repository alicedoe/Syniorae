package com.syniorae.presentation.fragments.calendar.configuration

import androidx.lifecycle.viewModelScope
import com.syniorae.domain.models.widgets.calendar.CalendarConfig
import com.syniorae.domain.models.widgets.calendar.IconAssociation
import com.syniorae.presentation.common.BaseViewModel
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel partagé pour tout le tunnel de configuration du calendrier
 * Gère l'état et la navigation entre les 6 étapes
 */
class CalendarConfigurationViewModel : BaseViewModel() {

    // État de la configuration
    private val _configState = MutableStateFlow(CalendarConfigState())
    val configState = _configState.asStateFlow()

    // Navigation
    private val _navigationEvent = MutableSharedFlow<ConfigNavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Étape actuelle
    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    /**
     * Passe à l'étape suivante
     */
    fun nextStep() {
        val current = _currentStep.value
        if (current < 6) {
            _currentStep.value = current + 1
            viewModelScope.launch {
                _navigationEvent.emit(ConfigNavigationEvent.NavigateToStep(current + 1))
            }
        } else {
            // Fin du tunnel - sauvegarder et terminer
            finishConfiguration()
        }
    }

    /**
     * Revient à l'étape précédente
     */
    fun previousStep() {
        val current = _currentStep.value
        if (current > 1) {
            _currentStep.value = current - 1
            viewModelScope.launch {
                _navigationEvent.emit(ConfigNavigationEvent.NavigateToStep(current - 1))
            }
        } else {
            // Première étape - annuler la configuration
            cancelConfiguration()
        }
    }

    /**
     * ÉTAPE 1 : Configure la connexion Google
     */
    fun setGoogleAccount(email: String) {
        _configState.value = _configState.value.copy(
            googleAccountEmail = email,
            isGoogleConnected = true
        )
    }

    /**
     * ÉTAPE 2 : Sélectionne un calendrier
     */
    fun selectCalendar(calendarId: String, calendarName: String) {
        _configState.value = _configState.value.copy(
            selectedCalendarId = calendarId,
            selectedCalendarName = calendarName
        )
    }

    /**
     * ÉTAPE 3 : Configure le nombre de semaines
     */
    fun setWeeksAhead(weeks: Int) {
        _configState.value = _configState.value.copy(
            weeksAhead = weeks
        )
    }

    /**
     * ÉTAPE 4 : Configure le nombre d'événements max
     */
    fun setMaxEvents(maxEvents: Int) {
        _configState.value = _configState.value.copy(
            maxEvents = maxEvents
        )
    }

    /**
     * ÉTAPE 5 : Configure la fréquence de synchronisation
     */
    fun setSyncFrequency(hours: Int) {
        _configState.value = _configState.value.copy(
            syncFrequencyHours = hours
        )
    }

    /**
     * ÉTAPE 6 : Ajoute une association d'icône
     */
    fun addIconAssociation(keywords: List<String>, iconPath: String) {
        val currentAssociations = _configState.value.iconAssociations.toMutableList()
        currentAssociations.add(
            IconAssociation(
                keywords = keywords,
                iconPath = iconPath
            )
        )
        _configState.value = _configState.value.copy(
            iconAssociations = currentAssociations
        )
    }

    /**
     * ÉTAPE 6 : Supprime une association d'icône
     */
    fun removeIconAssociation(index: Int) {
        val currentAssociations = _configState.value.iconAssociations.toMutableList()
        if (index in currentAssociations.indices) {
            currentAssociations.removeAt(index)
            _configState.value = _configState.value.copy(
                iconAssociations = currentAssociations
            )
        }
    }

    /**
     * Termine la configuration
     */
    private fun finishConfiguration() {
        executeWithLoading {
            val state = _configState.value

            // TODO: Sauvegarder dans les fichiers JSON
            // TODO: Activer le widget
            // TODO: Lancer la première synchronisation

            showMessage("Configuration terminée avec succès !")

            viewModelScope.launch {
                _navigationEvent.emit(ConfigNavigationEvent.ConfigurationComplete)
            }
        }
    }

    /**
     * Annule la configuration
     */
    private fun cancelConfiguration() {
        viewModelScope.launch {
            _navigationEvent.emit(ConfigNavigationEvent.ConfigurationCancelled)
        }
    }

    /**
     * Vérifie si l'étape actuelle est valide
     */
    fun isCurrentStepValid(): Boolean {
        val state = _configState.value
        return when (_currentStep.value) {
            1 -> state.isGoogleConnected
            2 -> state.selectedCalendarId.isNotBlank()
            3 -> state.weeksAhead > 0
            4 -> state.maxEvents > 0
            5 -> state.syncFrequencyHours > 0
            6 -> true // Les associations sont optionnelles
            else -> false
        }
    }
}

/**
 * État de la configuration du calendrier
 */
data class CalendarConfigState(
    val googleAccountEmail: String = "",
    val isGoogleConnected: Boolean = false,
    val selectedCalendarId: String = "",
    val selectedCalendarName: String = "",
    val weeksAhead: Int = 4,
    val maxEvents: Int = 50,
    val syncFrequencyHours: Int = 4,
    val iconAssociations: List<IconAssociation> = emptyList()
) {
    /**
     * Convertit vers CalendarConfig pour sauvegarde
     */
    fun toCalendarConfig(): CalendarConfig {
        return CalendarConfig(
            selectedCalendarId = selectedCalendarId,
            selectedCalendarName = selectedCalendarName,
            weeksAhead = weeksAhead,
            maxEvents = maxEvents,
            syncFrequencyHours = syncFrequencyHours,
            googleAccountEmail = googleAccountEmail
        )
    }
}

/**
 * Événements de navigation du tunnel de configuration
 */
sealed class ConfigNavigationEvent {
    data class NavigateToStep(val step: Int) : ConfigNavigationEvent()
    object ConfigurationComplete : ConfigNavigationEvent()
    object ConfigurationCancelled : ConfigNavigationEvent()
}