package com.syniorae.presentation.fragments.calendar.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.syniorae.core.di.DependencyInjection
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.CalendarConfigurationJsonModel
import com.syniorae.data.local.json.models.IconsJsonModel
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel pour la page des paramètres détaillés du calendrier (Page 3)
 */
class CalendarSettingsViewModel : ViewModel() {

    private val jsonFileManager = DependencyInjection.getJsonFileManager()
    private val widgetRepository = DependencyInjection.getWidgetRepository()
    private val googleAuthManager = DependencyInjection.getGoogleAuthManager()

    private val _uiState = MutableStateFlow(CalendarSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CalendarSettingsEvent>()
    val events = _events.asSharedFlow()

    /**
     * Charge les paramètres actuels depuis les fichiers de configuration
     */
    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Charger la configuration principale
                val config = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )

                // Charger les associations d'icônes
                val iconsData = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.ICONS,
                    IconsJsonModel::class.java
                )

                // Récupérer l'email du compte Google connecté
                val googleEmail = googleAuthManager.getSignedInAccountEmail() ?: ""

                if (config != null) {
                    _uiState.value = CalendarSettingsUiState(
                        isLoading = false,
                        googleAccountEmail = googleEmail,
                        selectedCalendarName = config.calendrier_name,
                        weeksAhead = config.nb_semaines_max,
                        maxEvents = config.nb_evenements_max,
                        syncFrequencyHours = config.frequence_synchro,
                        iconAssociationsCount = iconsData?.associations?.size ?: 0
                    )
                } else {
                    // Configuration non trouvée
                    _uiState.value = CalendarSettingsUiState(
                        isLoading = false,
                        googleAccountEmail = googleEmail
                    )
                    _events.emit(CalendarSettingsEvent.ShowError("Configuration non trouvée"))
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors du chargement: ${e.message}"))
            }
        }
    }

    /**
     * Ouvre l'étape 2 du tunnel (sélection de calendrier)
     */
    fun editCalendarSelection() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.NavigateToStep(2))
        }
    }

    /**
     * Ouvre l'étape 3 du tunnel (période de récupération)
     */
    fun editPeriod() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.NavigateToStep(3))
        }
    }

    /**
     * Ouvre l'étape 4 du tunnel (nombre d'événements)
     */
    fun editEventsCount() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.NavigateToStep(4))
        }
    }

    /**
     * Ouvre l'étape 5 du tunnel (fréquence de synchronisation)
     */
    fun editSyncFrequency() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.NavigateToStep(5))
        }
    }

    /**
     * Ouvre l'étape 6 du tunnel (associations d'icônes)
     */
    fun editIconAssociations() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.NavigateToStep(6))
        }
    }

    /**
     * Affiche la confirmation de déconnexion
     */
    fun showDisconnectConfirmation() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowDisconnectDialog)
        }
    }

    /**
     * Déconnecte le compte Google et supprime toute la configuration
     */
    fun disconnectGoogle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // 1. Déconnecter Google
                val disconnected = googleAuthManager.signOut()

                if (disconnected) {
                    // 2. Supprimer les fichiers JSON
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)

                    // 3. Désactiver le widget
                    widgetRepository.disableWidget(WidgetType.CALENDAR)

                    _events.emit(CalendarSettingsEvent.ShowMessage("Déconnexion réussie"))
                    _events.emit(CalendarSettingsEvent.NavigateBack)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la déconnexion"))
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la déconnexion: ${e.message}"))
            }
        }
    }
}

/**
 * État de l'interface utilisateur
 */
data class CalendarSettingsUiState(
    val isLoading: Boolean = false,
    val googleAccountEmail: String = "",
    val selectedCalendarName: String = "",
    val weeksAhead: Int = 0,
    val maxEvents: Int = 0,
    val syncFrequencyHours: Int = 0,
    val iconAssociationsCount: Int = 0
)

/**
 * Événements de navigation et d'interface
 */
sealed class CalendarSettingsEvent {
    object ShowDisconnectDialog : CalendarSettingsEvent()
    data class NavigateToStep(val step: Int) : CalendarSettingsEvent()
    object NavigateBack : CalendarSettingsEvent()
    data class ShowMessage(val message: String) : CalendarSettingsEvent()
    data class ShowError(val message: String) : CalendarSettingsEvent()
}

/**
 * Factory pour créer le ViewModel
 */
class CalendarSettingsViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarSettingsViewModel::class.java)) {
            return CalendarSettingsViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}