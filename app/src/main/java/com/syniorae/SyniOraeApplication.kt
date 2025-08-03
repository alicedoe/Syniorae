package com.syniorae

import android.app.Application
import android.util.Log

/**
 * Classe Application principale de SyniOrae
 * Point d'entrée de l'application pour l'initialisation globale
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

        // TODO: Initialisation Hilt (plus tard)
        // TODO: Initialisation des préférences
        // TODO: Initialisation des utilitaires globaux
    }
}