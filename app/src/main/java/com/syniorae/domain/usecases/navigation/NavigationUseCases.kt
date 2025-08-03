package com.syniorae.domain.usecases.navigation

import com.syniorae.core.constants.AppConstants
import com.syniorae.data.repository.UserRepository
import kotlinx.coroutines.delay

/**
 * Use case pour gérer l'appui long sur l'icône paramètre
 */
class HandleLongPressUseCase(
    private val userRepository: UserRepository
) {

    /**
     * Gère l'appui long avec progression
     * @param onProgress Callback pour mettre à jour la progression (0.0 à 1.0)
     * @return true si l'appui est maintenu jusqu'à la fin, false sinon
     */
    suspend fun execute(
        onProgress: (Float) -> Unit,
        checkIfReleased: () -> Boolean
    ): Result<Boolean> {
        return try {
            val totalDuration = AppConstants.LONG_PRESS_DURATION
            val updateInterval = 50L // Mise à jour toutes les 50ms pour un effet fluide
            val steps = totalDuration / updateInterval

            repeat(steps.toInt()) { step ->
                // Vérifie si l'utilisateur a relâché
                if (checkIfReleased()) {
                    onProgress(0f) // Reset la progression
                    return Result.success(false)
                }

                // Met à jour la progression
                val progress = (step + 1).toFloat() / steps
                onProgress(progress)

                delay(updateInterval)
            }

            // Appui maintenu jusqu'à la fin
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case pour valider la navigation vers les pages de configuration
 */
class ValidateNavigationUseCase(
    private val userRepository: UserRepository
) {

    /**
     * Valide si l'utilisateur peut naviguer vers la page de configuration
     */
    fun canNavigateToConfiguration(): Boolean {
        return userRepository.canAccessConfiguration()
    }

    /**
     * Valide si l'utilisateur peut naviguer vers les paramètres détaillés
     */
    fun canNavigateToSettings(): Boolean {
        return userRepository.canAccessConfiguration()
    }

    /**
     * Retourne le message d'erreur approprié si la navigation est refusée
     */
    fun getNavigationDeniedMessage(targetPage: NavigationTarget): String {
        return when (targetPage) {
            NavigationTarget.CONFIGURATION ->
                "Accès refusé. Maintenez l'icône paramètre pour accéder à la configuration."
            NavigationTarget.SETTINGS ->
                "Accès refusé. Mode configurateur requis."
            NavigationTarget.HOME ->
                "" // Toujours accessible
        }
    }
}

/**
 * Use case pour naviguer vers une page spécifique
 */
class NavigateToPageUseCase(
    private val validateNavigationUseCase: ValidateNavigationUseCase
) {

    /**
     * Valide et exécute la navigation
     */
    fun execute(target: NavigationTarget): NavigationResult {
        return when (target) {
            NavigationTarget.HOME -> NavigationResult.Success(target)

            NavigationTarget.CONFIGURATION -> {
                if (validateNavigationUseCase.canNavigateToConfiguration()) {
                    NavigationResult.Success(target)
                } else {
                    NavigationResult.Denied(
                        validateNavigationUseCase.getNavigationDeniedMessage(target)
                    )
                }
            }

            NavigationTarget.SETTINGS -> {
                if (validateNavigationUseCase.canNavigateToSettings()) {
                    NavigationResult.Success(target)
                } else {
                    NavigationResult.Denied(
                        validateNavigationUseCase.getNavigationDeniedMessage(target)
                    )
                }
            }
        }
    }
}

/**
 * Cibles de navigation possibles
 */
enum class NavigationTarget {
    HOME,
    CONFIGURATION,
    SETTINGS
}

/**
 * Résultat d'une tentative de navigation
 */
sealed class NavigationResult {
    data class Success(val target: NavigationTarget) : NavigationResult()
    data class Denied(val message: String) : NavigationResult()
}