package com.syniorae.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de base avec gestion des erreurs et états de chargement
 */
abstract class BaseViewModel : ViewModel() {

    // État de chargement
    protected val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Gestion des erreurs
    protected val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    // Messages d'information
    protected val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    // Handler pour les exceptions
    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleError(exception)
    }

    /**
     * Execute une action avec gestion automatique du loading et des erreurs
     */
    protected fun executeWithLoading(action: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            _isLoading.value = true
            try {
                action()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Execute une action sans loading automatique
     */
    protected fun execute(action: suspend () -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            action()
        }
    }

    /**
     * Gère les erreurs de manière centralisée
     */
    protected open fun handleError(exception: Throwable) {
        val errorMessage = when (exception) {
            is IllegalArgumentException -> "Paramètre invalide"
            is IllegalStateException -> "État invalide de l'application"
            else -> exception.message ?: "Erreur inconnue"
        }

        viewModelScope.launch {
            _error.emit(errorMessage)
            _isLoading.value = false
        }
    }

    /**
     * Affiche un message d'information
     */
    protected fun showMessage(message: String) {
        viewModelScope.launch {
            _message.emit(message)
        }
    }

    /**
     * Affiche un message d'erreur
     */
    protected fun showError(error: String) {
        viewModelScope.launch {
            _error.emit(error)
        }
    }
}

/**
 * États de vue communs
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * États de navigation
 */
sealed class NavigationEvent {
    object NavigateToConfiguration : NavigationEvent()
    object NavigateToSettings : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class NavigateToWidgetConfig(val widgetType: String) : NavigationEvent()
}

/**
 * Événements spéciaux de configuration
 */
sealed class ConfigurationSpecialEvent {
    object LaunchConfigurationTunnel : ConfigurationSpecialEvent()
}