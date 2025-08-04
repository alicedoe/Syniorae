package com.syniorae.presentation.fragments.home

import androidx.lifecycle.viewModelScope
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
 * Version simplifiée sans dépendances externes
 */
class HomeViewModel : BaseViewModel() {

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
    }

    /**
     * Charge les données initiales
     */
    private fun loadInitialData() {
        updateCurrentDate()
        // Pour l'instant, pas de widget calendrier activé
        _viewState.value = _viewState.value.copy(
            hasCalendarWidget = false,
            todayEvents = emptyList(),
            futureEvents = emptyList()
        )
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
}