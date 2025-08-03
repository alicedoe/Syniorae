package com.syniorae.data.local.json

import android.content.Context
import com.syniorae.core.constants.StorageConstants
import java.io.File
import java.io.IOException

/**
 * Manager des opérations I/O sur les fichiers JSON
 * Gère l'écriture, lecture et suppression des fichiers
 */
class JsonStorageManager(private val context: Context) {

    private val dataDirectory: File
        get() = File(context.filesDir, StorageConstants.JSON_DIR)

    /**
     * S'assure que le répertoire de données existe
     */
    fun ensureDirectoryExists(): Boolean {
        return if (!dataDirectory.exists()) {
            dataDirectory.mkdirs()
        } else {
            true
        }
    }

    /**
     * Lit le contenu d'un fichier JSON
     */
    fun readFile(fileName: String): String? {
        return try {
            val file = File(dataDirectory, fileName)
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Écrit le contenu dans un fichier JSON
     */
    fun writeFile(fileName: String, content: String): Boolean {
        return try {
            ensureDirectoryExists()
            val file = File(dataDirectory, fileName)
            file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Supprime un fichier JSON
     */
    fun deleteFile(fileName: String): Boolean {
        return try {
            val file = File(dataDirectory, fileName)
            if (file.exists()) {
                file.delete()
            } else {
                true // Considéré comme succès si le fichier n'existe pas
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Vérifie si un fichier existe
     */
    fun fileExists(fileName: String): Boolean {
        val file = File(dataDirectory, fileName)
        return file.exists() && file.isFile
    }

    /**
     * Retourne la taille d'un fichier en octets
     */
    fun getFileSize(fileName: String): Long {
        return try {
            val file = File(dataDirectory, fileName)
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Retourne la liste de tous les fichiers JSON
     */
    fun listJsonFiles(): List<String> {
        return try {
            dataDirectory.listFiles { _, name ->
                name.endsWith(".json")
            }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Copie un fichier vers un autre emplacement
     */
    fun copyFile(sourceFileName: String, destFileName: String): Boolean {
        return try {
            val sourceFile = File(dataDirectory, sourceFileName)
            val destFile = File(dataDirectory, destFileName)

            if (sourceFile.exists()) {
                sourceFile.copyTo(destFile, overwrite = true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retourne la taille totale du répertoire en octets
     */
    fun getDirectorySize(): Long {
        return try {
            dataDirectory.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Vide complètement le répertoire de données
     */
    fun clearAllData(): Boolean {
        return try {
            dataDirectory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retourne le chemin absolu d'un fichier
     */
    fun getFilePath(fileName: String): String {
        return File(dataDirectory, fileName).absolutePath
    }

    /**
     * Vérifie si le répertoire de données est accessible en écriture
     */
    fun isWritable(): Boolean {
        return try {
            ensureDirectoryExists()
            dataDirectory.canWrite()
        } catch (e: Exception) {
            false
        }
    }
}