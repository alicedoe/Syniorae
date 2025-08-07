package com.syniorae.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Planificateur pour la synchronisation automatique du calendrier
 * Utilise AlarmManager pour déclencher les syncs à intervalles réguliers
 */
class SyncScheduler {

    companion object {
        private const val TAG = "SyncScheduler"
        private const val SYNC_ACTION = "com.syniorae.SYNC_CALENDAR"
        private const val REQUEST_CODE = 1001

        /**
         * Planifie la synchronisation automatique
         */
        fun scheduleSyncAlarm(context: Context, intervalHours: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SyncReceiver::class.java).apply {
                action = SYNC_ACTION
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            val intervalMillis = intervalHours * 60 * 60 * 1000L
            val triggerAtMillis = System.currentTimeMillis() + intervalMillis

            try {
                // Utiliser setInexactRepeating pour économiser la batterie
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC,
                    triggerAtMillis,
                    intervalMillis,
                    pendingIntent
                )

                Log.d(TAG, "Synchronisation planifiée toutes les $intervalHours heures")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la planification de la synchronisation", e)
            }
        }

        /**
         * Annule la synchronisation automatique
         */
        fun cancelSyncAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SyncReceiver::class.java).apply {
                action = SYNC_ACTION
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Synchronisation automatique annulée")
        }
    }
}