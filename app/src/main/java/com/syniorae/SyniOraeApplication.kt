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
 * ✅ Version sans fuite mémoire - pas de stockage statique avec Context
 */
class SyniOraeApplication : Application() {

    companion object {
        private const val TAG = "SyniOraeApp"
    }

    // Scope pour les opérations d'initialisation
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Application SyniOrae initialisée")

        // ✅ Initialiser le DependencyInjection avec le Context
        DependencyInjection.initialize(applicationContext)

        // ✅ Pas d'initialisation statique - juste initialiser les composants
        initializeComponents()
    }

    /**
     * Initialise les composants principaux de l'application
     * ✅ Utilise le Context seulement lors de l'initialisation
     */
    private fun initializeComponents() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Initialisation des composants...")

                // ✅ Récupérer les instances SANS paramètres
                val jsonFileManager = DependencyInjection.getJsonFileManager()
                val initialized = jsonFileManager.initialize()

                if (initialized) {
                    Log.d(TAG, "Système JSON initialisé")
                } else {
                    Log.w(TAG, "Échec de l'initialisation du système JSON")
                }

                // ✅ Récupérer le repository SANS paramètres
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

        // ✅ Nettoyer les ressources localement
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