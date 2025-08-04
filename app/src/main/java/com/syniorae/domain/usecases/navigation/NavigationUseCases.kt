package com.syniorae.domain.usecases.navigation

import com.syniorae.core.constants.AppConstants
import kotlinx.coroutines.delay

/**
 * Use case pour gérer l'appui long sur l'icône paramètre
 */
class HandleLongPressUseCase {

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
 * Use case pour naviguer vers une page spécifique
 * Plus de validation de rôles, toutes les pages sont accessibles
 */
class NavigateToPageUseCase {

    /**
     * Exécute la navigation (toujours autorisée)
     */
    fun execute(target: NavigationTarget): NavigationResult {
        return NavigationResult.Success(target)
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