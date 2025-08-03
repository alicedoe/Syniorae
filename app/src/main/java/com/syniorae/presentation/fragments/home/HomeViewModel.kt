package com.syniorae.presentation.fragments.home

import androidx.lifecycle.viewModelScope
import com.syniorae.domain.models.widgets.calendar.CalendarEvent
import com.syniorae.domain.usecases.navigation.HandleLongPressUseCase
import com.syniorae.domain.usecases.navigation.NavigationTarget
import com.syniorae.domain.usecases.navigation.NavigateToPageUseCase
import com.syniorae.domain.usecases.navigation.NavigationResult
import com.syniorae.domain.usecases.user.GetUserRoleUseCase
import com.syniorae.domain.usecases.widgets.common.GetAllWidgetsUseCase
import com.syniorae.presentation.common.BaseViewModel
import com.syniorae.presentation.common.NavigationEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ViewModel pour la page d'accueil (Page 1)
 */
class HomeViewModel(
    private val getUserRoleUseCase: GetUserRoleUseCase,
    private val getAllWidgetsUseCase: GetAllWidgetsUseCase,
    private val handleLongPressUseCase: HandleLongPressUseCase,
    private val navigateToPageUseCase: NavigateToPageUseCase
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
        observeUserChanges()
        observeWidgetChanges()
    }

    /**
     * Charge les données initiales
     */
    private fun loadInitialData() {
        execute {
            updateCurrentDate()
            loadWidgetsData()
        }
    }

    /**
     * Met à jour la date actuelle
     */
    private fun updateCurrentDate() {
        val now = LocalDateTime.now()
        val dayOfWeek = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
        val dayOfMonth = now.format(DateTimeFormatter.ofPattern("d"))
        val month = now.format(DateTimeFormatter.ofPattern("MMMM"))
        val year = now.format(DateTimeFormatter.ofPattern("yyyy"))

        _viewState.value = _viewState.value.copy(
            dayOfWeek = dayOfWeek.replaceFirstChar { it.uppercase() },
            dayOfMonth = dayOfMonth,
            monthYear = "$month $year"
        )
    }

    /**
     * Charge les données des widgets
     */
    private fun loadWidgetsData() {
        val activeWidgets = getAllWidgetsUseCase.getActiveWidgets()

        // Pour l'instant, on simule des événements vides
        // TODO: Récupérer les vrais événements depuis le widget calendrier
        val todayEvents = emptyList<CalendarEvent>()
        val futureEvents = emptyList<CalendarEvent>()

        _viewState.value = _viewState.value.copy(
            hasCalendarWidget = activeWidgets.any { it.type.name == "CALENDAR" },
            todayEvents = todayEvents,
            futureEvents = futureEvents
        )
    }

    /**
     * Observe les changements d'utilisateur
     */
    private fun observeUserChanges() {
        viewModelScope.launch {
            getUserRoleUseCase.observeUser().collect { user ->
                _viewState.value = _viewState.value.copy(
                    canAccessConfiguration = user.canAccessConfiguration()
                )
            }
        }
    }

    /**
     * Observe les changements de widgets
     */
    private fun observeWidgetChanges() {
        viewModelScope.launch {
            getAllWidgetsUseCase.observeWidgets().collect {
                loadWidgetsData()
            }
        }
    }

    /**
     * Gère le début de l'appui long sur l'icône paramètre
     */
    fun onSettingsIconPressed() {
        if (isLongPressing) return

        isLongPressing = true

        execute {
            val result = handleLongPressUseCase.execute(
                onProgress = { progress ->
                    _longPressProgress.value = progress
                },
                checkIfReleased = { !isLongPressing }
            )

            result.fold(
                onSuccess = { completed ->
                    if (completed) {
                        // Appui maintenu jusqu'à la fin
                        navigateToConfiguration()
                    }
                    // Reset la progression
                    _longPressProgress.value = 0f
                },
                onFailure = { exception ->
                    handleError(exception)
                    _longPressProgress.value = 0f
                }
            )
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
        val result = navigateToPageUseCase.execute(NavigationTarget.CONFIGURATION)
        when (result) {
            is NavigationResult.Success -> {
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.NavigateToConfiguration)
                }
            }
            is NavigationResult.Denied -> {
                showError(result.message)
            }
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
    val futureEvents: List<CalendarEvent> = emptyList(),
    val canAccessConfiguration: Boolean = false
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
}