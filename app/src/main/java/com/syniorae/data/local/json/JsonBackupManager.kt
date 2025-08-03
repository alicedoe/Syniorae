package com.syniorae.data.local.json

import android.content.Context
import com.syniorae.core.constants.StorageConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager pour la sauvegarde et restauration des fichiers JSON
 */
class JsonBackupManager(private val context: Context) {

    private val backupDirectory: File
        get() = File(context.filesDir, StorageConstants.BACKUP_DIR)

    private val dataDirectory: File
        get() = File(context.filesDir, StorageConstants.JSON_DIR)

    /**
     * Crée un backup d'un fichier avant modification
     */
    fun backupFile(fileName: String): Boolean {
        return try {
            ensureBackupDirectoryExists()

            val sourceFile = File(dataDirectory, fileName)
            if (sourceFile.exists()) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFileName = "${fileName.substringBeforeLast(".")}_backup_$timestamp.${fileName.substringAfterLast(".")}"
                val backupFile = File(backupDirectory, backupFileName)

                sourceFile.copyTo(backupFile, overwrite = true)
                true
            } else {
                true // Pas de fichier à sauvegarder = succès
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restaure un fichier depuis le backup le plus récent
     */
    fun restoreFile(fileName: String): Boolean {
        return try {
            val baseName = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".")

            // Trouve le backup le plus récent
            val backupFiles = backupDirectory.listFiles { _, name ->
                name.startsWith("${baseName}_backup_") && name.endsWith(".$extension")
            }?.sortedByDescending { it.lastModified() }

            if (backupFiles?.isNotEmpty() == true) {
                val latestBackup = backupFiles.first()
                val targetFile = File(dataDirectory, fileName)

                latestBackup.copyTo(targetFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Nettoie les anciens backups (garde les 5 plus récents)
     */
    fun cleanOldBackups(): Boolean {
        return try {
            if (!backupDirectory.exists()) {
                return true
            }

            val backupFiles = backupDirectory.listFiles()?.toList() ?: emptyList()

            // Groupe par nom de base de fichier
            val groupedBackups = backupFiles.groupBy { file ->
                val name = file.name
                // Extrait le nom de base (avant _backup_)
                name.substringBefore("_backup_")
            }

            // Pour chaque groupe, garde les 5 plus récents
            groupedBackups.forEach { (_, files) ->
                val sortedFiles = files.sortedByDescending { it.lastModified() }
                if (sortedFiles.size > 5) {
                    sortedFiles.drop(5).forEach { it.delete() }
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retourne la liste des backups disponibles pour un fichier
     */
    fun getBackupsForFile(fileName: String): List<BackupInfo> {
        return try {
            val baseName = fileName.substringBeforeLast(".")
            val extension = fileName.substringAfterLast(".")

            backupDirectory.listFiles { _, name ->
                name.startsWith("${baseName}_backup_") && name.endsWith(".$extension")
            }?.map { file ->
                BackupInfo(
                    fileName = file.name,
                    originalFileName = fileName,
                    timestamp = file.lastModified(),
                    size = file.length()
                )
            }?.sortedByDescending { it.timestamp } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Supprime tous les backups
     */
    fun clearAllBackups(): Boolean {
        return try {
            if (backupDirectory.exists()) {
                backupDirectory.listFiles()?.forEach { it.delete() }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun ensureBackupDirectoryExists(): Boolean {
        return if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
        } else {
            true
        }
    }
}

/**
 * Informations sur un backup
 */
data class BackupInfo(
    val fileName: String,
    val originalFileName: String,
    val timestamp: Long,
    val size: Long
) {
    fun getFormattedDate(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}