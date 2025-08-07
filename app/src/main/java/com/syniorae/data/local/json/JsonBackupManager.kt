package com.syniorae.data.local.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.syniorae.core.constants.StorageConstants
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * Manager pour la sauvegarde et restauration des fichiers JSON
 * ✅ Version sans fuite mémoire - ne stocke pas le Context
 */
class JsonBackupManager(private val dataDirectoryPath: String, private val backupDirectoryPath: String) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()

    private val dataDirectory: File
        get() = File(dataDirectoryPath)

    private val backupDirectory: File
        get() = File(backupDirectoryPath)

    /**
     * Initialise les répertoires de stockage
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureDirectoryExists()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * S'assure que les répertoires existent
     */
    private fun ensureDirectoryExists(): Boolean {
        val dataCreated = if (!dataDirectory.exists()) {
            dataDirectory.mkdirs()
        } else {
            true
        }

        val backupCreated = if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
        } else {
            true
        }

        return dataCreated && backupCreated
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
            val file = File(dataDirectory, fileName)

            if (file.exists()) {
                val jsonContent = file.readText(Charsets.UTF_8)
                if (isValidJson(jsonContent)) {
                    gson.fromJson(jsonContent, clazz)
                } else {
                    null
                }
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

            if (isValidJson(jsonContent)) {
                ensureDirectoryExists()

                // Backup simple avant écriture
                createSimpleBackup(fileName)

                // Écriture du fichier
                val file = File(dataDirectory, fileName)
                file.writeText(jsonContent, Charsets.UTF_8)
                true
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
            val configFile = getFileName(widgetType, JsonFileType.CONFIG)
            val dataFile = getFileName(widgetType, JsonFileType.DATA)
            val iconsFile = getFileName(widgetType, JsonFileType.ICONS)

            listOf(configFile, dataFile, iconsFile).all { fileName ->
                val file = File(dataDirectory, fileName)
                if (file.exists()) {
                    file.delete()
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un widget a ses fichiers de configuration
     */
    suspend fun hasConfigurationFiles(widgetType: WidgetType): Boolean = withContext(Dispatchers.IO) {
        try {
            val configFile = getFileName(widgetType, JsonFileType.CONFIG)
            val file = File(dataDirectory, configFile)
            file.exists() && file.isFile
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restaure les fichiers depuis le backup
     */
    suspend fun restoreFromBackup(widgetType: WidgetType): Boolean = withContext(Dispatchers.IO) {
        try {
            val configFile = getFileName(widgetType, JsonFileType.CONFIG)
            val dataFile = getFileName(widgetType, JsonFileType.DATA)
            val iconsFile = getFileName(widgetType, JsonFileType.ICONS)

            listOf(configFile, dataFile, iconsFile).all { fileName ->
                restoreSimpleBackup(fileName)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retourne la taille totale des fichiers JSON
     */
    suspend fun getTotalStorageSize(): Long = withContext(Dispatchers.IO) {
        try {
            dataDirectory.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Nettoie les fichiers temporaires
     */
    suspend fun cleanup(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Nettoyage simple des anciens backups
            cleanupOldBackups()
            true
        } catch (e: Exception) {
            false
        }
    }

    // === Méthodes utilitaires privées ===

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
     * Vérifie si une chaîne est un JSON valide
     */
    private fun isValidJson(jsonString: String): Boolean {
        return try {
            com.google.gson.JsonParser.parseString(jsonString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Crée un backup simple d'un fichier
     */
    fun createSimpleBackup(fileName: String) {
        try {
            val sourceFile = File(dataDirectory, fileName)
            if (sourceFile.exists()) {
                val backupFile = File(backupDirectory, "${fileName}.backup")
                sourceFile.copyTo(backupFile, overwrite = true)
            }
        } catch (e: Exception) {
            // Ignore backup errors
        }
    }

    /**
     * Restaure un fichier depuis son backup simple
     */
    private fun restoreSimpleBackup(fileName: String): Boolean {
        return try {
            val backupFile = File(backupDirectory, "${fileName}.backup")
            if (backupFile.exists()) {
                val targetFile = File(dataDirectory, fileName)
                backupFile.copyTo(targetFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Nettoie les anciens backups
     */
    private fun cleanupOldBackups() {
        try {
            if (backupDirectory.exists()) {
                backupDirectory.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".backup") && file.lastModified() < System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)) {
                        file.delete() // Supprime les backups de plus de 7 jours
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}