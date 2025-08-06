package com.syniorae.presentation.fragments.home

import androidx.lifecycle.viewModelScope
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.EventsJsonModel
import com.syniorae.data.repository.widgets.WidgetRepository
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import com.syniorae.presentation.common.BaseViewModel
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ViewModel pour la page d'accueil (Page 1)
 * Version complète avec lecture des données JSON
 */
class HomeViewModel(
    private val widgetRepository: WidgetRepository,
    private val jsonFileManager: JsonFileManager
) : BaseViewModel() {

    // État de la vue
    private val _viewState = MutableStateFlow(HomeViewState())
    val viewState = _viewState.asStateFlow()

    // Navigation
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Progression de l'appui long
    private val _longPressProgress = MutableStateFlow(0f)
    val longPressProgress = _longPressProgress.asStateFlow()

    private var isLongPressing = false

    init {
        loadInitialData()
        observeWidgetChanges()
    }

    /**
     * Charge les données initiales
     */
    private fun loadInitialData() {
        updateCurrentDate()
        loadCalendarEvents()
    }

    /**
     * Observe les changements de widgets pour recharger les données
     */
    private fun observeWidgetChanges() {
        viewModelScope.launch {
            widgetRepository.widgets.collect { widgets ->
                val calendarWidget = widgets.find { it.type == WidgetType.CALENDAR }
                val hasCalendarWidget = calendarWidget?.isActive() == true

                if (hasCalendarWidget != _viewState.value.hasCalendarWidget) {
                    loadCalendarEvents()
                }
            }
        }
    }

    /**
     * Charge les événements du calendrier depuis les fichiers JSON
     */
    private fun loadCalendarEvents() {
        executeWithLoading {
            try {
                // Vérifier si le widget calendrier est actif
                val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)
                val hasActiveCalendar = calendarWidget?.isActive() == true

                if (!hasActiveCalendar) {
                    // Pas de widget calendrier actif
                    _viewState.value = _viewState.value.copy(
                        hasCalendarWidget = false,
                        todayEvents = emptyList(),
                        futureEvents = emptyList()
                    )
                    return@executeWithLoading
                }

                // Lire le fichier d'événements JSON
                val eventsData = jsonFileManager.readJsonFile(
                    WidgetType.CALENDAR,
                    JsonFileType.DATA,
                    EventsJsonModel::class.java
                )

                if (eventsData == null) {
                    // Fichier d'événements pas trouvé ou vide
                    _viewState.value = _viewState.value.copy(
                        hasCalendarWidget = true,
                        todayEvents = emptyList(),
                        futureEvents = emptyList()
                    )
                    return@executeWithLoading
                }

                // Convertir les événements JSON en CalendarEvent
                val calendarEvents = eventsData.evenements.map { eventJson ->
                    CalendarEvent(
                        id = eventJson.id,
                        title = eventJson.titre,
                        startDateTime = eventJson.date_debut,
                        endDateTime = eventJson.date_fin,
                        isAllDay = eventJson.toute_journee,
                        isMultiDay = eventJson.multi_jours,
                        isCurrentlyRunning = eventJson.isCurrentlyRunning()
                    )
                }

                // Séparer les événements d'aujourd'hui et du futur
                val now = LocalDateTime.now()
                val today = now.toLocalDate()

                val todayEvents = calendarEvents.filter { event ->
                    event.startDateTime.toLocalDate() == today
                }.sortedBy { it.startDateTime }

                val futureEvents = calendarEvents.filter { event ->
                    event.startDateTime.toLocalDate().isAfter(today)
                }.sortedBy { it.startDateTime }

                // Mettre à jour l'état
                _viewState.value = _viewState.value.copy(
                    hasCalendarWidget = true,
                    todayEvents = todayEvents,
                    futureEvents = futureEvents
                )

            } catch (e: Exception) {
                // Erreur lors du chargement des événements
                _viewState.value = _viewState.value.copy(
                    hasCalendarWidget = true,
                    todayEvents = emptyList(),
                    futureEvents = emptyList()
                )
                showError("Erreur lors du chargement des événements: ${e.message}")
            }
        }
    }

    /**
     * Rafraîchit les données (pull to refresh)
     */
    fun refreshData() {
        loadCalendarEvents()
    }

    /**
     * Met à jour la date actuelle
     */
    private fun updateCurrentDate() {
        val now = LocalDateTime.now()
        val locale = Locale.getDefault()

        val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE", locale))
        val dayOfMonth = now.format(DateTimeFormatter.ofPattern("d"))
        val month = now.format(DateTimeFormatter.ofPattern("MMMM", locale))
        val year = now.format(DateTimeFormatter.ofPattern("yyyy"))

        _viewState.value = _viewState.value.copy(
            dayOfWeek = dayOfWeek.replaceFirstChar { it.uppercase() },
            dayOfMonth = dayOfMonth,
            monthYear = "$month $year"
        )
    }

    /**
     * Gère le début de l'appui long sur l'icône paramètre
     */
    fun onSettingsIconPressed() {
        if (isLongPressing) return

        isLongPressing = true

        execute {
            // Simulation de l'appui long avec progression
            val totalDuration = 1000L // 1 seconde
            val updateInterval = 50L // Mise à jour toutes les 50ms
            val steps = totalDuration / updateInterval

            repeat(steps.toInt()) { step ->
                // Vérifie si l'utilisateur a relâché
                if (!isLongPressing) {
                    _longPressProgress.value = 0f
                    return@execute
                }

                // Met à jour la progression
                val progress = (step + 1).toFloat() / steps
                _longPressProgress.value = progress

                delay(updateInterval)
            }

            // Appui maintenu jusqu'à la fin - navigation vers configuration
            if (isLongPressing) {
                navigateToConfiguration()
            }

            // Reset la progression
            _longPressProgress.value = 0f
        }
    }

    /**
     * Gère le relâchement de l'appui long
     */
    fun onSettingsIconReleased() {
        isLongPressing = false
    }

    /**
     * Navigue vers la page de configuration
     */
    private fun navigateToConfiguration() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToConfiguration)
        }
    }

    /**
     * Force une synchronisation manuelle depuis la page d'accueil
     */
    fun forceSyncCalendar() {
        executeWithLoading {
            val calendarWidget = widgetRepository.getWidget(WidgetType.CALENDAR)

            if (calendarWidget?.isActive() != true) {
                showError("Le widget calendrier n'est pas configuré")
                return@executeWithLoading
            }

            showMessage("Synchronisation en cours...")

            // TODO: Implémenter la vraie synchronisation ici
            delay(2000)

            // Recharger les événements après la sync
            loadCalendarEvents()

            showMessage("Synchronisation terminée")
        }
    }
}

/**
 * État de la vue d'accueil
 */
data class HomeViewState(
    val dayOfWeek: String = "",
    val dayOfMonth: String = "",
    val monthYear: String = "",
    val hasCalendarWidget: Boolean = false,
    val todayEvents: List<CalendarEvent> = emptyList(),
    val futureEvents: List<CalendarEvent> = emptyList()
) {

    /**
     * Retourne le message à afficher dans la colonne droite
     */
    fun getRightColumnMessage(): String {
        return when {
            !hasCalendarWidget -> ""
            todayEvents.isEmpty() -> "Aucun événement de prévu"
            else -> ""
        }
    }

    /**
     * Vérifie s'il y a des événements aujourd'hui
     */
    fun hasTodayEvents(): Boolean {
        return todayEvents.isNotEmpty()
    }

    /**
     * Vérifie s'il y a des événements futurs
     */
    fun hasFutureEvents(): Boolean {
        return futureEvents.isNotEmpty()
    }

    /**
     * Retourne le nombre total d'événements
     */
    fun getTotalEventsCount(): Int {
        return todayEvents.size + futureEvents.size
    }

    /**
     * Vérifie s'il y a des événements en cours maintenant
     */
    fun hasCurrentlyRunningEvents(): Boolean {
        return todayEvents.any { it.isCurrentlyRunning }
    }
}