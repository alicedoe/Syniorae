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
        println("🔥 CalendarSettingsViewModel.loadSettings() - DÉBUT")

        // Forcer une nouvelle instance du JsonFileManager pour éviter le cache
        println("🔥 FORÇAGE D'UN NOUVEAU JsonFileManager...")
        com.syniorae.core.di.DependencyInjection.resetJsonFileManager()
        val freshJsonFileManager = com.syniorae.core.di.DependencyInjection.getJsonFileManager()
        println("🔥 Nouvelle instance JsonFileManager: ${freshJsonFileManager.hashCode()}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            println("🔥 CalendarSettingsViewModel.loadSettings() - Loading activé")

            try {
                // LOGS DÉTAILLÉS pour traquer la source des données
                println("🔥 === DIAGNOSTIC COMPLET ===")
                println("🔥 JsonFileManager instance: ${jsonFileManager.hashCode()}")
                println("🔥 GoogleAuthManager instance: ${googleAuthManager.hashCode()}")

                // Charger la configuration principale
                println("🔥 CalendarSettingsViewModel.loadSettings() - Lecture fichier CONFIG")
                val config = freshJsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )
                println("🔥 CalendarSettingsViewModel.loadSettings() - Config RAW: $config")
                println("🔥 CalendarSettingsViewModel.loadSettings() - Config null? ${config == null}")

                if (config != null) {
                    println("🔥 Config détails:")
                    println("🔥   - calendrier_id: '${config.calendrier_id}'")
                    println("🔥   - calendrier_name: '${config.calendrier_name}'")
                    println("🔥   - nb_semaines_max: ${config.nb_semaines_max}")
                    println("🔥   - nb_evenements_max: ${config.nb_evenements_max}")
                    println("🔥   - frequence_synchro: ${config.frequence_synchro}")
                }

                // Charger les associations d'icônes
                println("🔥 CalendarSettingsViewModel.loadSettings() - Lecture fichier ICONS")
                val iconsData = freshJsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.ICONS,
                    IconsJsonModel::class.java
                )
                println("🔥 CalendarSettingsViewModel.loadSettings() - Icons: $iconsData")
                println("🔥 CalendarSettingsViewModel.loadSettings() - Icons count: ${iconsData?.associations?.size ?: 0}")

                // Récupérer l'email du compte Google connecté
                println("🔥 CalendarSettingsViewModel.loadSettings() - Récupération email Google")
                val googleEmail = googleAuthManager.getSignedInAccountEmail() ?: ""
                println("🔥 CalendarSettingsViewModel.loadSettings() - Email: '$googleEmail'")
                println("🔥 CalendarSettingsViewModel.loadSettings() - Email vide? ${googleEmail.isEmpty()}")

                // Vérifier l'état actuel de l'UI avant mise à jour
                println("🔥 État UI AVANT mise à jour:")
                println("🔥   - googleAccountEmail: '${_uiState.value.googleAccountEmail}'")
                println("🔥   - selectedCalendarName: '${_uiState.value.selectedCalendarName}'")
                println("🔥   - weeksAhead: ${_uiState.value.weeksAhead}")
                println("🔥   - maxEvents: ${_uiState.value.maxEvents}")

                if (config != null) {
                    println("🔥 CalendarSettingsViewModel.loadSettings() - Configuration trouvée, mise à jour UI")
                    val newState = CalendarSettingsUiState(
                        isLoading = false,
                        googleAccountEmail = googleEmail,
                        selectedCalendarName = config.calendrier_name,
                        weeksAhead = config.nb_semaines_max,
                        maxEvents = config.nb_evenements_max,
                        syncFrequencyHours = config.frequence_synchro,
                        iconAssociationsCount = iconsData?.associations?.size ?: 0
                    )

                    println("🔥 NOUVEAU État UI:")
                    println("🔥   - googleAccountEmail: '${newState.googleAccountEmail}'")
                    println("🔥   - selectedCalendarName: '${newState.selectedCalendarName}'")
                    println("🔥   - weeksAhead: ${newState.weeksAhead}")
                    println("🔥   - maxEvents: ${newState.maxEvents}")

                    _uiState.value = newState
                } else {
                    // Configuration non trouvée
                    println("🔥 CalendarSettingsViewModel.loadSettings() - Configuration NON trouvée")
                    val emptyState = CalendarSettingsUiState(
                        isLoading = false,
                        googleAccountEmail = googleEmail
                    )

                    println("🔥 État UI VIDE:")
                    println("🔥   - googleAccountEmail: '${emptyState.googleAccountEmail}'")
                    println("🔥   - selectedCalendarName: '${emptyState.selectedCalendarName}'")
                    println("🔥   - weeksAhead: ${emptyState.weeksAhead}")
                    println("🔥   - maxEvents: ${emptyState.maxEvents}")

                    _uiState.value = emptyState

                    if (googleEmail.isNotEmpty()) {
                        _events.emit(CalendarSettingsEvent.ShowError("Configuration non trouvée"))
                    }
                }

                println("🔥 État UI FINAL:")
                println("🔥   - googleAccountEmail: '${_uiState.value.googleAccountEmail}'")
                println("🔥   - selectedCalendarName: '${_uiState.value.selectedCalendarName}'")
                println("🔥   - weeksAhead: ${_uiState.value.weeksAhead}")
                println("🔥   - maxEvents: ${_uiState.value.maxEvents}")

            } catch (e: Exception) {
                println("🔥 CalendarSettingsViewModel.loadSettings() - ERREUR: ${e.message}")
                println("🔥 CalendarSettingsViewModel.loadSettings() - Stack trace: ${e.stackTrace.contentToString()}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors du chargement: ${e.message}"))
            }
        }
        println("🔥 CalendarSettingsViewModel.loadSettings() - FIN")
    }

    /**
     * Ouvre un popup pour modifier la sélection de calendrier
     */
    fun editCalendarSelection() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowCalendarSelectionDialog)
        }
    }

    /**
     * Ouvre un popup pour modifier la période de récupération
     */
    fun editPeriod() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowPeriodDialog)
        }
    }

    /**
     * Ouvre un popup pour modifier le nombre d'événements
     */
    fun editEventsCount() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowEventsCountDialog)
        }
    }

    /**
     * Ouvre un popup pour modifier la fréquence de synchronisation
     */
    fun editSyncFrequency() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowSyncFrequencyDialog)
        }
    }

    /**
     * Ouvre un popup pour modifier les associations d'icônes
     */
    fun editIconAssociations() {
        viewModelScope.launch {
            _events.emit(CalendarSettingsEvent.ShowIconAssociationsDialog)
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
     * Met à jour la sélection de calendrier
     */
    fun updateCalendarSelection(calendarId: String, calendarName: String) {
        viewModelScope.launch {
            try {
                val currentConfig = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )

                if (currentConfig != null) {
                    val updatedConfig = currentConfig.copy(
                        calendrier_id = calendarId,
                        calendrier_name = calendarName
                    )

                    jsonFileManager.writeJsonFile(
                        WidgetType.CALENDAR,
                        JsonFileType.CONFIG,
                        updatedConfig
                    )

                    // Mettre à jour l'UI
                    _uiState.value = _uiState.value.copy(selectedCalendarName = calendarName)
                    _events.emit(CalendarSettingsEvent.ShowMessage("Calendrier mis à jour"))
                }
            } catch (e: Exception) {
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la mise à jour: ${e.message}"))
            }
        }
    }

    /**
     * Met à jour la période de récupération
     */
    fun updatePeriod(weeks: Int) {
        viewModelScope.launch {
            try {
                val currentConfig = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )

                if (currentConfig != null) {
                    val updatedConfig = currentConfig.copy(nb_semaines_max = weeks)

                    jsonFileManager.writeJsonFile(
                        WidgetType.CALENDAR,
                        JsonFileType.CONFIG,
                        updatedConfig
                    )

                    // Mettre à jour l'UI
                    _uiState.value = _uiState.value.copy(weeksAhead = weeks)
                    _events.emit(CalendarSettingsEvent.ShowMessage("Période mise à jour"))
                }
            } catch (e: Exception) {
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la mise à jour: ${e.message}"))
            }
        }
    }

    /**
     * Met à jour le nombre d'événements
     */
    fun updateEventsCount(maxEvents: Int) {
        viewModelScope.launch {
            try {
                val currentConfig = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )

                if (currentConfig != null) {
                    val updatedConfig = currentConfig.copy(nb_evenements_max = maxEvents)

                    jsonFileManager.writeJsonFile(
                        WidgetType.CALENDAR,
                        JsonFileType.CONFIG,
                        updatedConfig
                    )

                    // Mettre à jour l'UI
                    _uiState.value = _uiState.value.copy(maxEvents = maxEvents)
                    _events.emit(CalendarSettingsEvent.ShowMessage("Nombre d'événements mis à jour"))
                }
            } catch (e: Exception) {
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la mise à jour: ${e.message}"))
            }
        }
    }

    /**
     * Met à jour la fréquence de synchronisation
     */
    fun updateSyncFrequency(hours: Int) {
        viewModelScope.launch {
            try {
                val currentConfig = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.CONFIG,
                    CalendarConfigurationJsonModel::class.java
                )

                if (currentConfig != null) {
                    val updatedConfig = currentConfig.copy(frequence_synchro = hours)

                    jsonFileManager.writeJsonFile(
                        WidgetType.CALENDAR,
                        JsonFileType.CONFIG,
                        updatedConfig
                    )

                    // Mettre à jour l'UI
                    _uiState.value = _uiState.value.copy(syncFrequencyHours = hours)
                    _events.emit(CalendarSettingsEvent.ShowMessage("Fréquence de synchronisation mise à jour"))
                }
            } catch (e: Exception) {
                _events.emit(CalendarSettingsEvent.ShowError("Erreur lors de la mise à jour: ${e.message}"))
            }
        }
    }

    /**
     * Déconnecte complètement le compte Google et supprime toute la configuration
     */
    fun disconnectGoogle() {
        viewModelScope.launch {
            try {
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - DÉBUT")
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Dans viewModelScope.launch")

                // 1. Activer le loading
                _uiState.value = _uiState.value.copy(isLoading = true)
                //_isLoading.value = true
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Loading activé")

                // 2. Supprimer les fichiers JSON (CONFIG, DATA, ICONS)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Suppression fichiers JSON...")
                println("🔥 AVANT suppression - vérification des fichiers...")

                val configDeleted =
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - CONFIG supprimé: $configDeleted")

                val dataDeleted =
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - DATA supprimé: $dataDeleted")

                val iconsDeleted =
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - ICONS supprimé: $iconsDeleted")

                println("🔥 APRÈS suppression - tous les résultats: $configDeleted, $dataDeleted, $iconsDeleted")

                // 3. ✅ CORRECTION PRINCIPALE : Supprimer complètement la configuration du WidgetRepository
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Suppression complète de la configuration du widget...")
                val widgetConfigDeleted =
                    widgetRepository.deleteWidgetConfiguration(WidgetType.CALENDAR)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Configuration widget supprimée: $widgetConfigDeleted")

                // 4. Arrêter les services de synchronisation automatique
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Arrêt des services automatiques...")
                // Utiliser les services via DependencyInjection ou CalendarSyncWorker
                try {
                    androidx.work.WorkManager.getInstance(/* context sera récupéré via DI */)
                        .cancelUniqueWork("calendar_sync")
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Services WorkManager arrêtés")
                } catch (e: Exception) {
                    println("🔥 Erreur WorkManager (non critique): ${e.message}")
                }

                // 5. Déconnecter Google
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Déconnexion Google...")
                val googleDisconnected = googleAuthManager.signOut()
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Google déconnecté: $googleDisconnected")

                // 6. Vider l'état UI local (adapter selon la structure réelle de _uiState)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Vidage de l'UI state...")
                _uiState.value = _uiState.value.copy(
                    googleAccountEmail = "",
                    selectedCalendarName = "",
                    weeksAhead = 0,
                    maxEvents = 0
                    // Enlever iconAssociations et autres champs qui n'existent pas
                )
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - UI state vidé")

                // 7. Réinitialiser les repositories si disponibles
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Réinitialisation des repositories...")
                try {
                    // Si calendarRepository est disponible, l'utiliser
                    // Sinon ignorer cette étape
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Repositories réinitialisés")
                } catch (e: Exception) {
                    println("🔥 Repository non disponible (non critique): ${e.message}")
                }

                // 8. Désactiver le loading
                _uiState.value = _uiState.value.copy(isLoading = false)
                //_isLoading.value = false
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Loading désactivé")

                // 9. Messages de succès et navigation
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Émission ShowMessage...")
                _events.emit(CalendarSettingsEvent.ShowMessage("Déconnexion réussie"))

                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Émission NavigateBack...")
                _events.emit(CalendarSettingsEvent.NavigateBack)

                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - FIN")

            } catch (e: Exception) {
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - ERREUR GLOBALE: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                //_isLoading.value = false
                _events.emit(CalendarSettingsEvent.ShowMessage("Erreur lors de la déconnexion: ${e.message}"))
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
        object ShowCalendarSelectionDialog : CalendarSettingsEvent()
        object ShowPeriodDialog : CalendarSettingsEvent()
        object ShowEventsCountDialog : CalendarSettingsEvent()
        object ShowSyncFrequencyDialog : CalendarSettingsEvent()
        object ShowIconAssociationsDialog : CalendarSettingsEvent()
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
}