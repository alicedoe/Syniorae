package com.syniorae.core.di

import android.content.Context

/**
 * Injection de dépendances ultra-simple
 * Juste pour stocker le contexte de l'application
 */
object DependencyInjection {

    private var _context: Context? = null

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
}