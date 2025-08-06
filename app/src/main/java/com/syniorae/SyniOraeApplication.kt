package com.syniorae

import android.app.Application
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Classe Application principale de SyniOrae
 * Version complète avec initialisation des composants
 */
class SyniOraeApplication : Application() {

    companion object {
        private const val TAG = "SyniOraeApp"
        lateinit var instance: SyniOraeApplication
            private set
    }

    // Scope pour les opérations d'initialisation
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d(TAG, "Application SyniOrae initialisée")

        // Initialiser l'injection de dépendances
        DependencyInjection.initialize(applicationContext)

        // Initialiser les composants de manière asynchrone
        initializeComponents()
    }

    /**
     * Initialise les composants principaux de l'application
     */
    private fun initializeComponents() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Initialisation des composants...")

                // Initialiser le système JSON
                val jsonFileManager = DependencyInjection.getJsonFileManager()
                val initialized = jsonFileManager.initialize()

                if (initialized) {
                    Log.d(TAG, "Système JSON initialisé")
                } else {
                    Log.w(TAG, "Échec de l'initialisation du système JSON")
                }

                // Initialiser le repository des widgets
                val widgetRepository = DependencyInjection.getWidgetRepository()
                Log.d(TAG, "WidgetRepository initialisé")

                // Nettoyer les anciens fichiers si nécessaire
                val cleaned = widgetRepository.cleanup()
                if (cleaned) {
                    Log.d(TAG, "Nettoyage des fichiers terminé")
                }

                // Démarrer le service de synchronisation si des widgets sont actifs
                val activeWidgets = widgetRepository.getActiveWidgets()
                if (activeWidgets.isNotEmpty()) {
                    com.syniorae.services.CalendarSyncService.start(applicationContext)
                    Log.d(TAG, "Service de synchronisation démarré")
                }

                Log.d(TAG, "Initialisation des composants terminée")

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'initialisation des composants", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        // Nettoyer les ressources
        applicationScope.launch {
            try {
                val widgetRepository = DependencyInjection.getWidgetRepository()
                widgetRepository.cleanup()
                Log.d(TAG, "Nettoyage final terminé")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du nettoyage final", e)
            }
        }
    }
}