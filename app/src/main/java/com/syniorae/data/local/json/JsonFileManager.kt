package com.syniorae.data.local.json

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.syniorae.core.constants.StorageConstants
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * Manager principal pour la gestion des fichiers JSON
 * Coordonne les opérations sur tous les widgets
 * ✅ Version sans fuite mémoire
 */
class JsonFileManager(
    private val dataDirectoryPath: String,
    private val backupDirectoryPath: String
) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    private val storageManager = JsonStorageManager(dataDirectoryPath)
    private val validator = JsonValidator()
    private val backupManager = JsonBackupManager(dataDirectoryPath, backupDirectoryPath)

    /**
     * Initialise le répertoire de stockage
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            storageManager.ensureDirectoryExists() && backupManager.initialize()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Lit un fichier JSON pour un widget spécifique
     */
    suspend fun <T> readJsonFile(
        widgetType: WidgetType,
        fileType: JsonFileType,
        clazz: Class<T>
    ): T? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(widgetType, fileType)
            val jsonContent = storageManager.readFile(fileName)

            if (jsonContent != null && validator.isValidJson(jsonContent)) {
                gson.fromJson(jsonContent, clazz)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Écrit un fichier JSON pour un widget spécifique
     */
    suspend fun <T> writeJsonFile(
        widgetType: WidgetType,
        fileType: JsonFileType,
        data: T
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(widgetType, fileType)
            val jsonContent = gson.toJson(data)

            if (validator.isValidJson(jsonContent)) {
                // Backup avant écriture
                backupManager.createSimpleBackup(fileName)
                storageManager.writeFile(fileName, jsonContent)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Supprime tous les fichiers d'un widget
     */
    suspend fun deleteWidgetFiles(widgetType: WidgetType): Boolean = withContext(Dispatchers.IO) {
        try {
            backupManager.deleteWidgetFiles(widgetType)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un widget a ses fichiers de configuration
     */
    suspend fun hasConfigurationFiles(widgetType: WidgetType): Boolean = withContext(Dispatchers.IO) {
        try {
            backupManager.hasConfigurationFiles(widgetType)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restaure les fichiers depuis le backup
     */
    suspend fun restoreFromBackup(widgetType: WidgetType): Boolean = withContext(Dispatchers.IO) {
        try {
            backupManager.restoreFromBackup(widgetType)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Génère le nom de fichier selon le type de widget et le type de fichier
     */
    private fun getFileName(widgetType: WidgetType, fileType: JsonFileType): String {
        val baseName = widgetType.name.lowercase()
        return when (fileType) {
            JsonFileType.CONFIG -> "${baseName}_config.json"
            JsonFileType.DATA -> "${baseName}_data.json"
            JsonFileType.ICONS -> "${baseName}_icons.json"
        }
    }

    /**
     * Retourne la taille totale des fichiers JSON
     */
    suspend fun getTotalStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            backupManager.getTotalStorageSize()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Nettoie les fichiers temporaires et anciens backups
     */
    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        try {
            backupManager.cleanup()
        } catch (e: Exception) {
            false
        }
    }
    suspend fun deleteJsonFile(
        widgetType: WidgetType,
        fileType: JsonFileType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(widgetType, fileType)
            storageManager.deleteFile(fileName)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fileExists(
        widgetType: WidgetType,
        fileType: JsonFileType
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(widgetType, fileType)
            storageManager.fileExists(fileName)
        } catch (e: Exception) {
            false
        }
    }
}