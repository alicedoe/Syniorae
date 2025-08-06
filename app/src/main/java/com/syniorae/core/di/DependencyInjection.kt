package com.syniorae.core.di

import android.content.Context
import com.syniorae.data.auth.GoogleAuthManager
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.remote.calendar.GoogleCalendarApiManager
import com.syniorae.data.repository.calendar.CalendarRepository
import com.syniorae.data.repository.widgets.WidgetRepository

/**
 * Injection de dépendances simple
 * MODIFICATION COMPLÈTE - Ajoute les managers Google
 */
object DependencyInjection {

    private var _context: Context? = null
    private var _jsonFileManager: JsonFileManager? = null
    private var _widgetRepository: WidgetRepository? = null
    private var _calendarRepository: CalendarRepository? = null
    private var _googleAuthManager: GoogleAuthManager? = null
    private var _googleCalendarApiManager: GoogleCalendarApiManager? = null

    /**
     * Initialise avec le contexte de l'application
     */
    fun initialize(applicationContext: Context) {
        _context = applicationContext
    }

    /**
     * Récupère le contexte
     */
    fun getContext(): Context {
        return _context ?: throw IllegalStateException("DependencyInjection not initialized")
    }

    /**
     * Récupère le JsonFileManager (singleton)
     */
    fun getJsonFileManager(): JsonFileManager {
        if (_jsonFileManager == null) {
            _jsonFileManager = JsonFileManager(getContext())
        }
        return _jsonFileManager!!
    }

    /**
     * Récupère le WidgetRepository (singleton)
     */
    fun getWidgetRepository(): WidgetRepository {
        if (_widgetRepository == null) {
            _widgetRepository = WidgetRepository(getContext(), getJsonFileManager())
        }
        return _widgetRepository!!
    }

    /**
     * Récupère le CalendarRepository (singleton)
     */
    fun getCalendarRepository(): CalendarRepository {
        if (_calendarRepository == null) {
            _calendarRepository = CalendarRepository(getJsonFileManager()).apply {
                initializeGoogleApi(getContext())
            }
        }
        return _calendarRepository!!
    }

    /**
     * Récupère le GoogleAuthManager (singleton)
     */
    fun getGoogleAuthManager(): GoogleAuthManager {
        if (_googleAuthManager == null) {
            _googleAuthManager = GoogleAuthManager(getContext())
        }
        return _googleAuthManager!!
    }

    /**
     * Récupère le GoogleCalendarApiManager (singleton)
     */
    fun getGoogleCalendarApiManager(): GoogleCalendarApiManager {
        if (_googleCalendarApiManager == null) {
            _googleCalendarApiManager = GoogleCalendarApiManager(getContext())
        }
        return _googleCalendarApiManager!!
    }

    /**
     * Nettoie les instances (pour les tests ou redémarrage)
     */
    fun clear() {
        _jsonFileManager = null
        _widgetRepository = null
        _calendarRepository = null
        _googleAuthManager = null
        _googleCalendarApiManager = null
    }

    /**
     * Force la création d'une nouvelle instance du WidgetRepository
     * Utile après des changements importants dans les données
     */
    fun resetWidgetRepository() {
        _widgetRepository = null
    }

    /**
     * Force la création d'une nouvelle instance du JsonFileManager
     * Utile après des changements de configuration
     */
    fun resetJsonFileManager() {
        _jsonFileManager = null
        _widgetRepository = null // Le repository dépend du JsonFileManager
        _calendarRepository = null // Le CalendarRepository dépend aussi du JsonFileManager
    }

    /**
     * Force la création d'une nouvelle instance du CalendarRepository
     */
    fun resetCalendarRepository() {
        _calendarRepository = null
    }

    /**
     * Force la création d'une nouvelle instance des managers Google
     * Utile après déconnexion/reconnexion Google
     */
    fun resetGoogleManagers() {
        _googleAuthManager = null
        _googleCalendarApiManager = null
        _calendarRepository = null // Dépend des managers Google
    }

    /**
     * Vérifie si toutes les dépendances sont initialisées
     */
    fun isFullyInitialized(): Boolean {
        return _context != null &&
                _jsonFileManager != null &&
                _widgetRepository != null &&
                _calendarRepository != null &&
                _googleAuthManager != null &&
                _googleCalendarApiManager != null
    }

    /**
     * Initialise toutes les dépendances de manière proactive
     */
    fun initializeAllDependencies() {
        getJsonFileManager()
        getWidgetRepository()
        getCalendarRepository()
        getGoogleAuthManager()
        getGoogleCalendarApiManager()
    }
}