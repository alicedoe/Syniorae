package com.syniorae.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.syniorae.core.di.DependencyInjection
import com.syniorae.domain.models.widgets.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Récepteur pour redémarrer les services après un reboot du système
 * Assure la continuité de la synchronisation automatique
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val action = intent?.action
        Log.d(TAG, "Action reçue: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                restartServices(context)
            }
        }
    }

    /**
     * Redémarre les services nécessaires après un reboot
     */
    private fun restartServices(context: Context) {
        receiverScope.launch {
            try {
                Log.d(TAG, "Redémarrage des services après reboot")

                // Initialiser l'injection de dépendances
                DependencyInjection.initialize(context.applicationContext)

                val widgetRepository = DependencyInjection.getWidgetRepository()
                val preferencesManager = com.syniorae.core.utils.PreferencesManager.getInstance(context)

                // Vérifier si des widgets sont actifs
                val activeWidgets = widgetRepository.getActiveWidgets()
                Log.d(TAG, "Widgets actifs trouvés: ${activeWidgets.size}")

                if (activeWidgets.isNotEmpty()) {
                    // Redémarrer le service de synchronisation
                    CalendarSyncService.start(context)
                    Log.d(TAG, "Service de synchronisation redémarré")

                    // Reprogrammer les alarmes de synchronisation pour chaque widget actif
                    activeWidgets.forEach { widget ->
                        when (widget.type) {
                            WidgetType.CALENDAR -> {
                                val syncFrequency = preferencesManager.syncFrequencyHours
                                SyncScheduler.scheduleSyncAlarm(context, syncFrequency)
                                Log.d(TAG, "Alarme de synchronisation calendrier reprogrammée (${syncFrequency}h)")
                            }
                            else -> {
                                // Futurs widgets
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Aucun widget actif, services non redémarrés")
                }

                // Nettoyer les anciens fichiers si nécessaire
                widgetRepository.cleanup()

            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du redémarrage des services", e)
            }
        }
    }
}