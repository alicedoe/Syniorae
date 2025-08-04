package com.syniorae

import android.app.Application
import android.util.Log
import com.syniorae.core.di.DependencyInjection

/**
 * Classe Application principale de SyniOrae
 * Version simple
 */
class SyniOraeApplication : Application() {

    companion object {
        private const val TAG = "SyniOraeApp"
        lateinit var instance: SyniOraeApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Application SyniOrae initialisée")

        // Initialiser l'injection de dépendances simple
        DependencyInjection.initialize(applicationContext)
    }
}