package com.syniorae.core.di

import android.content.Context
import com.syniorae.core.constants.StorageConstants
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.repository.widgets.WidgetRepository
import java.io.File

/**
 * Injection de dépendances simple
 * ✅ Version SANS fuite mémoire - ne stocke PAS le Context
 */
object DependencyInjection {

    // On stocke les CHEMINS au lieu du Context
    private var _dataDirectoryPath: String? = null
    private var _backupDirectoryPath: String? = null

    // Instances des singletons
    private var _jsonFileManager: JsonFileManager? = null
    private var _widgetRepository: WidgetRepository? = null
    private var _calendarRepository: com.syniorae.data.repository.calendar.CalendarRepository? = null

    /**
     * Initialise avec le contexte de l'application
     * Extrait les chemins puis libère la référence au Context
     */
    fun initialize(applicationContext: Context) {
        // Extraire les chemins nécessaires
        _dataDirectoryPath = File(applicationContext.filesDir, StorageConstants.JSON_DIR).absolutePath
        _backupDirectoryPath = File(applicationContext.filesDir, StorageConstants.BACKUP_DIR).absolutePath

        // Initialiser le WidgetRepository avec le Context puis libérer la référence
        if (_widgetRepository == null) {
            _widgetRepository = WidgetRepository(_dataDirectoryPath!!, getJsonFileManager())
            _widgetRepository!!.initializeWithContext(applicationContext)
        }
    }

    /**
     * Récupère le JsonFileManager (singleton)
     */
    fun getJsonFileManager(): JsonFileManager {
        if (_jsonFileManager == null) {
            val dataPath = _dataDirectoryPath ?: throw IllegalStateException("DependencyInjection not initialized")
            val backupPath = _backupDirectoryPath ?: throw IllegalStateException("DependencyInjection not initialized")
            _jsonFileManager = JsonFileManager(dataPath, backupPath)
        }
        return _jsonFileManager!!
    }

    /**
     * Récupère le WidgetRepository (singleton)
     */
    fun getWidgetRepository(): WidgetRepository {
        return _widgetRepository ?: throw IllegalStateException("DependencyInjection not initialized")
    }

    /**
     * Récupère le CalendarRepository (singleton)
     */
    fun getCalendarRepository(): com.syniorae.data.repository.calendar.CalendarRepository {
        if (_calendarRepository == null) {
            _calendarRepository = com.syniorae.data.repository.calendar.CalendarRepository(getJsonFileManager())
        }
        return _calendarRepository!!
    }

    /**
     * Récupère le GoogleAuthManager (créé à chaque fois car il a besoin du Context)
     */
    fun getGoogleAuthManager(context: Context): com.syniorae.data.remote.google.GoogleAuthManager {
        return com.syniorae.data.remote.google.GoogleAuthManager(context)
    }

    /**
     * Récupère le GoogleCalendarApi
     */
    fun getGoogleCalendarApi(context: Context): com.syniorae.data.remote.google.GoogleCalendarApi {
        return com.syniorae.data.remote.google.GoogleCalendarApi(getGoogleAuthManager(context))
    }

    /**
     * Nettoie les instances (pour les tests ou redémarrage)
     */
    fun clear() {
        _jsonFileManager = null
        _widgetRepository = null
        _calendarRepository = null
        _dataDirectoryPath = null
        _backupDirectoryPath = null
    }

    /**
     * Force la création d'une nouvelle instance du WidgetRepository
     */
    fun resetWidgetRepository() {
        _widgetRepository = null
    }

    /**
     * Force la création d'une nouvelle instance du JsonFileManager
     */
    fun resetJsonFileManager() {
        _jsonFileManager = null
        _widgetRepository = null
        _calendarRepository = null
    }

    /**
     * Force la création d'une nouvelle instance du CalendarRepository
     */
    fun resetCalendarRepository() {
        _calendarRepository = null
    }
}