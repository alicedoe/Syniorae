package com.syniorae.core.di

import android.content.Context
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.core.utils.PreferencesManager
import com.syniorae.data.repository.widgets.WidgetRepository

/**
 * Container d'injection de dépendances simplifié
 * Version corrigée compatible avec votre architecture existante
 */
object DependencyInjection {

    // ApplicationContext (safe contre les memory leaks)
    private var applicationContext: Context? = null

    // Instances singleton
    private var _jsonFileManager: JsonFileManager? = null
    private var _widgetRepository: WidgetRepository? = null
    private var _calendarRepository: com.syniorae.data.repository.calendar.CalendarRepository? = null
    private var _preferencesManager: PreferencesManager? = null
    private var _googleAuthManager: com.syniorae.data.remote.google.GoogleAuthManager? = null
    private var _googleCalendarApi: com.syniorae.data.remote.google.GoogleCalendarApi? = null
    private var _googleTokenManager: com.syniorae.data.remote.google.GoogleTokenManager? = null
    private var _googleCalendarService: com.syniorae.data.remote.google.GoogleCalendarServiceBackup? = null

    // Chemins calculés
    private var _dataDirectoryPath: String? = null
    private var _backupDirectoryPath: String? = null

    /**
     * Initialise le container avec l'ApplicationContext
     * DOIT être appelé depuis Application.onCreate()
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext

        // Calculer les chemins
        _dataDirectoryPath = context.filesDir.absolutePath
        _backupDirectoryPath = context.getExternalFilesDir("backups")?.absolutePath
            ?: context.filesDir.absolutePath + "/backups"
    }

    /**
     * Vérifie que le contexte est disponible
     */
    private fun requireContext(): Context {
        return applicationContext ?: throw IllegalStateException(
            "DependencyInjection not initialized. Call initialize() first."
        )
    }

    /**
     * Vérifie si l'injection de dépendances est initialisée
     */
    fun isInitialized(): Boolean {
        return applicationContext != null && _dataDirectoryPath != null && _backupDirectoryPath != null
    }

    /**
     * Récupère le chemin du répertoire de données
     */
    fun getDataDirectoryPath(): String {
        return _dataDirectoryPath ?: throw IllegalStateException("DependencyInjection not initialized")
    }

    /**
     * Récupère le chemin du répertoire de sauvegarde
     */
    fun getBackupDirectoryPath(): String {
        return _backupDirectoryPath ?: throw IllegalStateException("DependencyInjection not initialized")
    }

    /**
     * Récupère le JsonFileManager (singleton)
     * Compatible avec le constructeur (String, String)
     */
    fun getJsonFileManager(): JsonFileManager {
        if (_jsonFileManager == null) {
            _jsonFileManager = JsonFileManager(
                dataDirectoryPath = getDataDirectoryPath(),
                backupDirectoryPath = getBackupDirectoryPath()
            )
        }
        return _jsonFileManager!!
    }

    /**
     * Récupère le PreferencesManager (singleton)
     */
    fun getPreferencesManager(): PreferencesManager {
        if (_preferencesManager == null) {
            _preferencesManager = PreferencesManager.getInstance(requireContext())
        }
        return _preferencesManager!!
    }

    /**
     * Récupère le WidgetRepository (singleton)
     * Compatible avec le constructeur (String, JsonFileManager)
     */
    fun getWidgetRepository(): WidgetRepository {
        if (_widgetRepository == null) {
            _widgetRepository = WidgetRepository(
                dataDirectoryPath = getDataDirectoryPath(),
                jsonFileManager = getJsonFileManager()
            )
            // Initialiser avec le contexte une seule fois
            _widgetRepository!!.initializeWithContext(requireContext())
        }
        return _widgetRepository!!
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
     * Récupère le GoogleAuthManager (singleton)
     */
    fun getGoogleAuthManager(): com.syniorae.data.remote.google.GoogleAuthManager {
        if (_googleAuthManager == null) {
            _googleAuthManager = com.syniorae.data.remote.google.GoogleAuthManager()
        }
        return _googleAuthManager!!
    }

    /**
     * Récupère le GoogleTokenManager (singleton)
     */
    fun getGoogleTokenManager(): com.syniorae.data.remote.google.GoogleTokenManager {
        if (_googleTokenManager == null) {
            _googleTokenManager = com.syniorae.data.remote.google.GoogleTokenManager(requireContext())
        }
        return _googleTokenManager!!
    }

    /**
     * Récupère le GoogleCalendarApi (singleton)
     */
    fun getGoogleCalendarApi(): com.syniorae.data.remote.google.GoogleCalendarApi {
        if (_googleCalendarApi == null) {
            _googleCalendarApi = com.syniorae.data.remote.google.GoogleCalendarApi(getGoogleAuthManager())
        }
        return _googleCalendarApi!!
    }

    /**
     * Récupère le GoogleCalendarService (singleton)
     */
    fun getGoogleCalendarService(): String {
        return "Test - GoogleCalendarService disabled temporarily"
    }

    /**
     * Nettoie les instances (pour les tests ou redémarrage)
     */
    fun clear() {
        _jsonFileManager = null
        _widgetRepository = null
        _calendarRepository = null
        _preferencesManager = null
        _googleAuthManager = null
        _googleCalendarApi = null
        _googleTokenManager = null
        _googleCalendarService = null
        _dataDirectoryPath = null
        _backupDirectoryPath = null
        // Ne pas nettoyer applicationContext
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

    /**
     * Force la création d'une nouvelle instance du GoogleAuthManager
     */
    fun resetGoogleAuthManager() {
        _googleAuthManager = null
        _googleCalendarApi = null
        _googleCalendarService = null
    }

    /**
     * Force la création d'une nouvelle instance du GoogleTokenManager
     */
    fun resetGoogleTokenManager() {
        _googleTokenManager = null
    }

    /**
     * Force la création d'une nouvelle instance du GoogleCalendarService
     */
    fun resetGoogleCalendarService() {
        _googleCalendarService = null
    }
}