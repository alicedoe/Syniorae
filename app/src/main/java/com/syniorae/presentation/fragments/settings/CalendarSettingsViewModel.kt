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
     * Déconnecte le compte Google et supprime toute la configuration
     * Selon cahier des charges: suppression des 3 fichiers JSON + connexion Google + retour page 2 avec widget OFF
     */
    fun disconnectGoogle(context: android.content.Context? = null) {
        println("🔥 CalendarSettingsViewModel.disconnectGoogle() - DÉBUT")

        viewModelScope.launch {
            println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Dans viewModelScope.launch")
            _uiState.value = _uiState.value.copy(isLoading = true)
            println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Loading activé")

            try {
                // 1. Supprimer les 3 fichiers JSON AVANT la déconnexion
                try {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Suppression fichiers JSON...")

                    // Vérifier AVANT suppression avec JsonStorageManager directement
                    val configFileName = "calendar_config.json"
                    val dataFileName = "calendar_data.json"
                    val iconsFileName = "calendar_icons.json"

                    // Utiliser le storageManager interne pour vérifier l'existence
                    println("🔥 AVANT suppression - vérification des fichiers...")

                    val result1 = jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - CONFIG supprimé: $result1")

                    val result2 = jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - DATA supprimé: $result2")

                    val result3 = jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - ICONS supprimé: $result3")

                    println("🔥 APRÈS suppression - tous les résultats: $result1, $result2, $result3")

                } catch (e: Exception) {
                    // Continuer même si erreur de suppression des fichiers
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur suppression fichiers JSON: ${e.message}")
                }

                // 2. Désactiver le widget (toggle OFF sur page 2)
                try {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Désactivation widget...")
                    widgetRepository.disableWidget(WidgetType.CALENDAR)
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Widget désactivé")
                } catch (e: Exception) {
                    // Continuer même si erreur de désactivation
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur désactivation widget: ${e.message}")
                }

                // 2.5. ARRÊTER tous les services de synchronisation automatique
                try {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Arrêt des services automatiques...")

                    if (context != null) {
                        // Arrêter WorkManager (synchronisation en arrière-plan)
                        androidx.work.WorkManager.getInstance(context)
                            .cancelUniqueWork("calendar_sync_work")
                        androidx.work.WorkManager.getInstance(context)
                            .cancelUniqueWork("calendar_sync_immediate")

                        println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Services WorkManager arrêtés")
                    } else {
                        println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Context null, impossible d'arrêter WorkManager")
                    }
                } catch (e: Exception) {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur arrêt services: ${e.message}")
                }

                // 3. Déconnecter Google et supprimer les autorisations
                val disconnected = try {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Déconnexion Google...")
                    googleAuthManager.signOut()
                } catch (e: Exception) {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur déconnexion Google: ${e.message}")
                    true // Continuer quand même
                }
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Google déconnecté: $disconnected")

                // 4. Vider les données en mémoire (UI state)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Vidage de l'UI state...")
                _uiState.value = CalendarSettingsUiState(
                    isLoading = false,
                    googleAccountEmail = "",
                    selectedCalendarName = "",
                    weeksAhead = 0,
                    maxEvents = 0,
                    syncFrequencyHours = 0,
                    iconAssociationsCount = 0
                )
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - UI state vidé")

                // 5. Réinitialiser les repositories pour vider leurs caches
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Réinitialisation des repositories...")
                try {
                    // Force la réinitialisation des singletons pour vider les caches
                    com.syniorae.core.di.DependencyInjection.resetWidgetRepository()
                    com.syniorae.core.di.DependencyInjection.resetCalendarRepository()
                    com.syniorae.core.di.DependencyInjection.resetGoogleAuthManager()
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Repositories réinitialisés")
                } catch (e: Exception) {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur réinitialisation repositories: ${e.message}")
                }

                // 6. Toujours considérer comme succès (même si erreurs partielles)
                _uiState.value = _uiState.value.copy(isLoading = false)
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Loading désactivé")

                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Émission ShowMessage...")
                _events.emit(CalendarSettingsEvent.ShowMessage("Déconnexion réussie"))

                // 5. Retour page 2 avec widget OFF
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Émission NavigateBack...")
                _events.emit(CalendarSettingsEvent.NavigateBack)

                // 6. Suppression DÉFINITIVE après navigation (avec délai)
                kotlinx.coroutines.delay(500) // Attendre que la navigation soit terminée
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Suppression DÉFINITIVE post-navigation...")

                try {
                    // Re-suppression pour être sûr + vider les caches
                    println("🔥 Suppression DÉFINITIVE - Étape 1: Suppression fichiers")
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
                    jsonFileManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)

                    println("🔥 Suppression DÉFINITIVE - Étape 2: Force disable widget")
                    widgetRepository.disableWidget(WidgetType.CALENDAR)

                    println("🔥 Suppression DÉFINITIVE - Étape 3: Clear WorkManager")
                    if (context != null) {
                        androidx.work.WorkManager.getInstance(context).cancelAllWork()
                    }

                    println("🔥 Suppression DÉFINITIVE - Étape 4: Reset repositories")
                    com.syniorae.core.di.DependencyInjection.resetCalendarRepository()
                    com.syniorae.core.di.DependencyInjection.resetWidgetRepository()

                    // Délai supplémentaire pour s'assurer qu'aucun service ne recrée les fichiers
                    kotlinx.coroutines.delay(1000)

                    println("🔥 Suppression DÉFINITIVE - Étape 5: Suppression finale")
                    val freshJsonManager = com.syniorae.core.di.DependencyInjection.getJsonFileManager()
                    freshJsonManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.CONFIG)
                    freshJsonManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.DATA)
                    freshJsonManager.deleteJsonFile(WidgetType.CALENDAR, JsonFileType.ICONS)

                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Suppression DÉFINITIVE terminée")
                } catch (e: Exception) {
                    println("🔥 CalendarSettingsViewModel.disconnectGoogle() - Erreur suppression définitive: ${e.message}")
                }

                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - FIN")

            } catch (e: Exception) {
                println("🔥 CalendarSettingsViewModel.disconnectGoogle() - ERREUR GLOBALE: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                // Même en cas d'erreur, essayer de revenir en arrière
                _events.emit(CalendarSettingsEvent.ShowMessage("Déconnexion effectuée (certaines données peuvent persister)"))
                _events.emit(CalendarSettingsEvent.NavigateBack)
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