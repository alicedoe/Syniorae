package com.syniorae.core.di

import android.content.Context
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.repository.widgets.WidgetRepository

/**
 * Injection de dépendances simple
 * Gère les instances des classes principales
 */
object DependencyInjection {

    private var _context: Context? = null
    private var _jsonFileManager: JsonFileManager? = null
    private var _widgetRepository: WidgetRepository? = null
    private var _calendarRepository: com.syniorae.data.repository.calendar.CalendarRepository? = null

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
    fun getCalendarRepository(): com.syniorae.data.repository.calendar.CalendarRepository {
        if (_calendarRepository == null) {
            _calendarRepository = com.syniorae.data.repository.calendar.CalendarRepository(getJsonFileManager())
        }
        return _calendarRepository!!
    }

    /**
     * Nettoie les instances (pour les tests ou redémarrage)
     */
    fun clear() {
        _jsonFileManager = null
        _widgetRepository = null
        _calendarRepository = null
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
}