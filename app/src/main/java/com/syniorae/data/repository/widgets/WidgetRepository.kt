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
import java.io.File

/**
 * Repository principal pour la gestion des widgets
 * ✅ Version sans fuite mémoire - ne stocke pas le Context
 */
class WidgetRepository(
    private val dataDirectoryPath: String,
    private val jsonFileManager: JsonFileManager
) {

    // Stocker le chemin des SharedPreferences au lieu du Context
    private val sharedPrefsFile: File = File(dataDirectoryPath).parentFile?.let {
        File(it, "shared_prefs/${AppConstants.SHARED_PREFS_NAME}_widgets.xml")
    } ?: throw IllegalStateException("Impossible de déterminer le chemin des SharedPreferences")

    // État des widgets en mémoire
    private val _widgets = MutableStateFlow(loadWidgetsFromPrefs())
    val widgets: Flow<List<Widget>> = _widgets.asStateFlow()

    /**
     * Initialise avec le contexte (appelé une seule fois)
     */
    fun initializeWithContext(context: Context) {
        // Charger les widgets depuis les SharedPreferences
        val sharedPrefs = context.getSharedPreferences(
            "${AppConstants.SHARED_PREFS_NAME}_widgets",
            Context.MODE_PRIVATE
        )
        _widgets.value = loadWidgetsFromSharedPrefs(sharedPrefs)
    }

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
        val hasConfig = hasConfiguration(type)

        return if (hasConfig) {
            updateWidgetStatus(type, WidgetStatus.ON, isConfigured = true)
        } else {
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
            val filesDeleted = jsonFileManager.deleteWidgetFiles(type)

            if (filesDeleted) {
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
                // TODO: Sauvegarder dans SharedPreferences si nécessaire
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
        return Widget.getAvailableWidgets()
    }

    /**
     * Charge les widgets depuis SharedPreferences
     */
    private fun loadWidgetsFromSharedPrefs(sharedPrefs: SharedPreferences): List<Widget> {
        return try {
            val availableWidgets = Widget.getAvailableWidgets().toMutableList()

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