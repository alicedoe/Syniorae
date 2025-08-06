package com.syniorae.data.repository.widgets

import android.content.Context
import android.content.SharedPreferences
import com.syniorae.core.constants.AppConstants
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.domain.models.widgets.Widget
import com.syniorae.domain.models.widgets.WidgetStatus
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository principal pour la gestion des widgets
 * Version connectée avec JsonFileManager - CORRIGÉE
 */
class WidgetRepository(
    private val context: Context,
    private val jsonFileManager: JsonFileManager
) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "${AppConstants.SHARED_PREFS_NAME}_widgets",
        Context.MODE_PRIVATE
    )

    // État des widgets en mémoire
    private val _widgets = MutableStateFlow(loadWidgetsFromPrefs())
    val widgets: Flow<List<Widget>> = _widgets.asStateFlow()

    /**
     * Récupère tous les widgets disponibles
     */
    fun getAllWidgets(): List<Widget> {
        return _widgets.value
    }

    /**
     * Récupère tous les widgets actifs
     */
    fun getActiveWidgets(): List<Widget> {
        return _widgets.value.filter { it.isActive() }
    }

    /**
     * Récupère un widget par son type
     */
    fun getWidget(type: WidgetType): Widget? {
        return _widgets.value.find { it.type == type }
    }

    /**
     * Vérifie si un widget a ses fichiers de configuration JSON
     */
    suspend fun hasConfiguration(type: WidgetType): Boolean {
        return jsonFileManager.hasConfigurationFiles(type)
    }

    /**
     * Active un widget
     */
    suspend fun enableWidget(type: WidgetType): Boolean {
        // Vérifier si le widget a sa configuration JSON
        val hasConfig = hasConfiguration(type)

        return if (hasConfig) {
            // Configuration existante → Activer directement
            updateWidgetStatus(type, WidgetStatus.ON, isConfigured = true)
        } else {
            // Pas de configuration → Mode configuration
            updateWidgetStatus(type, WidgetStatus.CONFIGURING, isConfigured = false)
        }
    }

    /**
     * Désactive un widget
     */
    suspend fun disableWidget(type: WidgetType): Boolean {
        return updateWidgetStatus(type, WidgetStatus.OFF, isConfigured = false)
    }

    /**
     * Met le widget en mode configuration
     */
    suspend fun setWidgetConfiguring(type: WidgetType): Boolean {
        return updateWidgetStatus(type, WidgetStatus.CONFIGURING, isConfigured = false)
    }

    /**
     * Marque un widget comme configuré et l'active
     */
    suspend fun markWidgetConfigured(type: WidgetType): Boolean {
        // Vérifier que les fichiers JSON existent bien
        val hasConfig = hasConfiguration(type)

        return if (hasConfig) {
            updateWidgetStatus(type, WidgetStatus.ON, isConfigured = true)
        } else {
            updateWidgetStatus(
                type,
                WidgetStatus.ERROR,
                isConfigured = false,
                errorMessage = "Fichiers de configuration manquants"
            )
        }
    }

    /**
     * Met un widget en erreur
     */
    suspend fun setWidgetError(type: WidgetType, errorMessage: String): Boolean {
        return updateWidgetStatus(type, WidgetStatus.ERROR, errorMessage = errorMessage)
    }

    /**
     * Supprime complètement la configuration d'un widget
     */
    suspend fun deleteWidgetConfiguration(type: WidgetType): Boolean {
        return try {
            // Supprimer les fichiers JSON
            val filesDeleted = jsonFileManager.deleteWidgetFiles(type)

            if (filesDeleted) {
                // Remettre le widget à OFF
                updateWidgetStatus(type, WidgetStatus.OFF, isConfigured = false)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restaure la configuration depuis les backups
     */
    suspend fun restoreWidgetConfiguration(type: WidgetType): Boolean {
        return try {
            val restored = jsonFileManager.restoreFromBackup(type)

            if (restored) {
                // Vérifier que la restauration a fonctionné
                val hasConfig = hasConfiguration(type)
                if (hasConfig) {
                    updateWidgetStatus(type, WidgetStatus.ON, isConfigured = true)
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Met à jour le statut d'un widget
     */
    private fun updateWidgetStatus(
        type: WidgetType,
        status: WidgetStatus,
        isConfigured: Boolean? = null,
        errorMessage: String? = null
    ): Boolean {
        return try {
            val widgets = _widgets.value.toMutableList()
            val index = widgets.indexOfFirst { it.type == type }

            if (index != -1) {
                val currentWidget = widgets[index]
                widgets[index] = currentWidget.copy(
                    status = status,
                    isConfigured = isConfigured ?: currentWidget.isConfigured,
                    errorMessage = errorMessage,
                    lastUpdate = System.currentTimeMillis()
                )

                _widgets.value = widgets
                saveWidgetsToPrefs(widgets)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Charge les widgets depuis les préférences
     */
    private fun loadWidgetsFromPrefs(): List<Widget> {
        return try {
            val availableWidgets = Widget.getAvailableWidgets().toMutableList()

            // Charge les états sauvegardés
            availableWidgets.forEachIndexed { index, widget ->
                val keyPrefix = "widget_${widget.type.name.lowercase()}"
                val statusName = sharedPrefs.getString("${keyPrefix}_status", WidgetStatus.OFF.name)
                val isConfigured = sharedPrefs.getBoolean("${keyPrefix}_configured", false)
                val lastUpdate = sharedPrefs.getLong("${keyPrefix}_last_update", 0L)
                val errorMessage = sharedPrefs.getString("${keyPrefix}_error", null)

                try {
                    val status = WidgetStatus.valueOf(statusName ?: WidgetStatus.OFF.name)
                    availableWidgets[index] = widget.copy(
                        status = status,
                        isConfigured = isConfigured,
                        lastUpdate = lastUpdate,
                        errorMessage = errorMessage
                    )
                } catch (e: Exception) {
                    // Si erreur, garde les valeurs par défaut
                }
            }

            availableWidgets
        } catch (e: Exception) {
            Widget.getAvailableWidgets()
        }
    }

    /**
     * Sauvegarde les widgets dans les préférences
     */
    private fun saveWidgetsToPrefs(widgets: List<Widget>): Boolean {
        return try {
            with(sharedPrefs.edit()) {
                widgets.forEach { widget ->
                    val keyPrefix = "widget_${widget.type.name.lowercase()}"
                    putString("${keyPrefix}_status", widget.status.name)
                    putBoolean("${keyPrefix}_configured", widget.isConfigured)
                    putLong("${keyPrefix}_last_update", widget.lastUpdate)
                    putString("${keyPrefix}_error", widget.errorMessage)
                }
                apply()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Nettoie les anciens fichiers et optimise le stockage
     */
    suspend fun cleanup(): Boolean {
        return try {
            jsonFileManager.cleanup()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retourne la taille totale du stockage JSON
     */
    suspend fun getStorageSize(): Long {
        return try {
            jsonFileManager.getTotalStorageSize()
        } catch (e: Exception) {
            0L
        }
    }
}