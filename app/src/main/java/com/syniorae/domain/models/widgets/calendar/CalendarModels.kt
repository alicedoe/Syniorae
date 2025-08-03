package com.syniorae.domain.models.widgets.calendar

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Représente un événement de calendrier
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val isMultiDay: Boolean = false,
    val isCurrentlyRunning: Boolean = false
) {

    /**
     * Formate l'heure d'affichage
     */
    fun getDisplayTime(): String {
        return if (isAllDay) {
            "Toute la journée"
        } else {
            startDateTime.format(DateTimeFormatter.ofPattern("HH'h'mm"))
        }
    }

    /**
     * Formate la date d'affichage
     */
    fun getDisplayDate(): String {
        return startDateTime.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
    }

    /**
     * Vérifie si l'événement est aujourd'hui
     */
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return startDateTime.toLocalDate() == today
    }

    /**
     * Vérifie si l'événement est dans le futur
     */
    fun isFuture(): Boolean {
        return startDateTime.isAfter(LocalDateTime.now())
    }
}

/**
 * Configuration du widget calendrier
 */
data class CalendarConfig(
    val selectedCalendarId: String,
    val selectedCalendarName: String,
    val weeksAhead: Int = 4,
    val maxEvents: Int = 50,
    val syncFrequencyHours: Int = 4,
    val googleAccountEmail: String? = null
) {

    /**
     * Valide la configuration
     */
    fun isValid(): Boolean {
        return selectedCalendarId.isNotBlank() &&
                selectedCalendarName.isNotBlank() &&
                weeksAhead > 0 &&
                maxEvents > 0 &&
                syncFrequencyHours > 0
    }

    /**
     * Retourne la description de la fréquence de sync
     */
    fun getSyncFrequencyDescription(): String {
        return when (syncFrequencyHours) {
            1 -> "Toutes les heures"
            24 -> "Une fois par jour"
            else -> "Toutes les $syncFrequencyHours heures"
        }
    }
}

/**
 * Association mot-clé => icône
 */
data class IconAssociation(
    val keywords: List<String>,
    val iconPath: String,
    val displayName: String = ""
) {

    /**
     * Vérifie si un titre d'événement correspond aux mots-clés
     */
    fun matchesEvent(eventTitle: String): Boolean {
        val titleLower = eventTitle.lowercase()
        return keywords.any { keyword ->
            titleLower.contains(keyword.lowercase())
        }
    }

    /**
     * Retourne la liste des mots-clés formatée
     */
    fun getKeywordsDisplay(): String {
        return keywords.joinToString(", ")
    }
}

/**
 * Statut de synchronisation
 */
data class SyncStatus(
    val isSuccess: Boolean,
    val lastSyncTime: LocalDateTime?,
    val eventsCount: Int = 0,
    val errorMessage: String? = null
) {

    /**
     * Retourne le texte d'affichage du statut
     */
    fun getDisplayStatus(): String {
        return if (isSuccess) "✓ Synchronisé" else "✗ Erreur"
    }

    /**
     * Retourne l'heure de dernière sync formatée
     */
    fun getLastSyncDisplay(): String {
        return lastSyncTime?.format(DateTimeFormatter.ofPattern("dd/MM à HH'h'mm")) ?: "Jamais"
    }

    /**
     * Retourne le texte du nombre d'événements
     */
    fun getEventsCountDisplay(): String {
        return when (eventsCount) {
            0 -> "Aucun événement"
            1 -> "1 événement récupéré"
            else -> "$eventsCount événements récupérés"
        }
    }
}