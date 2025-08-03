package com.syniorae.domain.usecases.widgets.common

import com.syniorae.data.repository.widgets.WidgetRepository
import com.syniorae.domain.models.widgets.Widget
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.flow.Flow

/**
 * Use case pour récupérer tous les widgets
 */
class GetAllWidgetsUseCase(
    private val widgetRepository: WidgetRepository
) {

    /**
     * Récupère tous les widgets disponibles
     */
    fun execute(): List<Widget> {
        return widgetRepository.getAllWidgets()
    }

    /**
     * Observe les changements de widgets
     */
    fun observeWidgets(): Flow<List<Widget>> {
        return widgetRepository.widgets
    }

    /**
     * Récupère seulement les widgets actifs
     */
    fun getActiveWidgets(): List<Widget> {
        return widgetRepository.getActiveWidgets()
    }
}

/**
 * Use case pour basculer l'état d'un widget (ON/OFF)
 */
class ToggleWidgetUseCase(
    private val widgetRepository: WidgetRepository
) {

    /**
     * Active ou désactive un widget selon son état actuel
     */
    suspend fun execute(widgetType: WidgetType): Result<Boolean> {
        return try {
            val widget = widgetRepository.getWidget(widgetType)

            if (widget == null) {
                return Result.failure(Exception("Widget non trouvé"))
            }

            val success = if (widget.isActive()) {
                // Widget actif → le désactiver
                widgetRepository.disableWidget(widgetType)
            } else {
                // Widget inactif → vérifier s'il est configuré
                if (widget.isConfigured) {
                    // Déjà configuré → l'activer directement
                    widgetRepository.enableWidget(widgetType)
                } else {
                    // Pas configuré → lancer la configuration
                    widgetRepository.setWidgetConfiguring(widgetType)
                }
            }

            if (success) {
                Result.success(widget.isActive())
            } else {
                Result.failure(Exception("Impossible de changer l'état du widget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Active un widget spécifique
     */
    suspend fun enableWidget(widgetType: WidgetType): Result<Unit> {
        return try {
            val success = widgetRepository.enableWidget(widgetType)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossible d'activer le widget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Désactive un widget spécifique
     */
    suspend fun disableWidget(widgetType: WidgetType): Result<Unit> {
        return try {
            val success = widgetRepository.disableWidget(widgetType)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Impossible de désactiver le widget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case pour récupérer le statut d'un widget
 */
class GetWidgetStatusUseCase(
    private val widgetRepository: WidgetRepository
) {

    /**
     * Récupère un widget par son type
     */
    fun execute(widgetType: WidgetType): Widget? {
        return widgetRepository.getWidget(widgetType)
    }

    /**
     * Vérifie si un widget est actif
     */
    fun isWidgetActive(widgetType: WidgetType): Boolean {
        return widgetRepository.getWidget(widgetType)?.isActive() ?: false
    }

    /**
     * Vérifie si un widget est configuré
     */
    fun isWidgetConfigured(widgetType: WidgetType): Boolean {
        return widgetRepository.getWidget(widgetType)?.isConfigured ?: false
    }

    /**
     * Vérifie si un widget a une erreur
     */
    fun hasWidgetError(widgetType: WidgetType): Boolean {
        return widgetRepository.getWidget(widgetType)?.hasError() ?: false
    }

    /**
     * Récupère le message d'erreur d'un widget
     */
    fun getWidgetError(widgetType: WidgetType): String? {
        return widgetRepository.getWidget(widgetType)?.errorMessage
    }
}

/**
 * Use case pour valider la configuration d'un widget
 */
class ValidateWidgetConfigUseCase(
    private val widgetRepository: WidgetRepository
) {

    /**
     * Valide qu'un widget peut être activé
     */
    suspend fun canActivateWidget(widgetType: WidgetType): Result<Boolean> {
        return try {
            val widget = widgetRepository.getWidget(widgetType)

            if (widget == null) {
                return Result.failure(Exception("Widget non trouvé"))
            }

            // Vérifie si le widget a sa configuration
            val hasConfig = widgetRepository.hasConfiguration(widgetType)

            Result.success(hasConfig)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Valide qu'un widget peut être configuré
     */
    fun canConfigureWidget(widgetType: WidgetType): Boolean {
        val widget = widgetRepository.getWidget(widgetType)
        return widget != null && !widget.isActive()
    }

    /**
     * Retourne les étapes de configuration manquantes
     */
    suspend fun getMissingConfigurationSteps(widgetType: WidgetType): List<String> {
        val missingSteps = mutableListOf<String>()

        // Vérifications génériques
        val hasConfig = widgetRepository.hasConfiguration(widgetType)
        if (!hasConfig) {
            missingSteps.add("Configuration de base manquante")
        }

        // Vérifications spécifiques par type de widget
        when (widgetType) {
            WidgetType.CALENDAR -> {
                // TODO: Ajouter vérifications spécifiques au calendrier
                if (!hasConfig) {
                    missingSteps.addAll(listOf(
                        "Connexion Google manquante",
                        "Calendrier non sélectionné",
                        "Paramètres de synchronisation non définis"
                    ))
                }
            }
            else -> {
                // Pour les autres widgets (futurs)
            }
        }

        return missingSteps
    }
}