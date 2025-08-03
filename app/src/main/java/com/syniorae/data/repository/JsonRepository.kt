package com.syniorae.data.repository

import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.domain.models.widgets.WidgetType

/**
 * Repository centralisé pour l'accès aux fichiers JSON
 * Fournit une interface unifiée pour tous les widgets
 */
class JsonRepository(
    private val jsonFileManager: JsonFileManager
) {

    /**
     * Initialise le système de fichiers JSON
     */
    suspend fun initialize(): Boolean {
        return jsonFileManager.initialize()
    }

    /**
     * Lit un fichier de configuration
     */
    suspend fun <T> readConfig(widgetType: WidgetType, clazz: Class<T>): T? {
        return jsonFileManager.readJsonFile(widgetType, JsonFileType.CONFIG, clazz)
    }

    /**
     * Lit un fichier de données
     */
    suspend fun <T> readData(widgetType: WidgetType, clazz: Class<T>): T? {
        return jsonFileManager.readJsonFile(widgetType, JsonFileType.DATA, clazz)
    }

    /**
     * Lit un fichier d'icônes
     */
    suspend fun <T> readIcons(widgetType: WidgetType, clazz: Class<T>): T? {
        return jsonFileManager.readJsonFile(widgetType, JsonFileType.ICONS, clazz)
    }

    /**
     * Écrit un fichier de configuration
     */
    suspend fun <T> writeConfig(widgetType: WidgetType, data: T): Boolean {
        return jsonFileManager.writeJsonFile(widgetType, JsonFileType.CONFIG, data)
    }

    /**
     * Écrit un fichier de données
     */
    suspend fun <T> writeData(widgetType: WidgetType, data: T): Boolean {
        return jsonFileManager.writeJsonFile(widgetType, JsonFileType.DATA, data)
    }

    /**
     * Écrit un fichier d'icônes
     */
    suspend fun <T> writeIcons(widgetType: WidgetType, data: T): Boolean {
        return jsonFileManager.writeJsonFile(widgetType, JsonFileType.ICONS, data)
    }

    /**
     * Supprime tous les fichiers d'un widget
     */
    suspend fun deleteWidgetFiles(widgetType: WidgetType): Boolean {
        return jsonFileManager.deleteWidgetFiles(widgetType)
    }

    /**
     * Vérifie si un widget a ses fichiers de configuration
     */
    suspend fun hasConfiguration(widgetType: WidgetType): Boolean {
        return jsonFileManager.hasConfigurationFiles(widgetType)
    }

    /**
     * Restaure les fichiers depuis le backup
     */
    suspend fun restoreFromBackup(widgetType: WidgetType): Boolean {
        return jsonFileManager.restoreFromBackup(widgetType)
    }

    /**
     * Retourne la taille totale des fichiers JSON
     */
    suspend fun getTotalStorageSize(): Long {
        return jsonFileManager.getTotalStorageSize()
    }

    /**
     * Nettoie les fichiers temporaires
     */
    suspend fun cleanup(): Boolean {
        return jsonFileManager.cleanup()
    }
}