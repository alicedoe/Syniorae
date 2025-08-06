package com.syniorae.core.utils

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Utilitaire de formatage des dates optimisé pour les seniors
 * Formats clairs et lisibles selon le cahier des charges
 */
object DateFormatter {

    // Formatters selon la locale
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
    private val dayOfMonthFormatter = DateTimeFormatter.ofPattern("d")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH'h'mm")
    private val shortTimeFormatter = DateTimeFormatter.ofPattern("HH'h'mm")

    /**
     * Formate la date pour l'affichage principal (colonne gauche)
     * Format : "Dimanche" / "3" / "août 2025"
     */
    fun formatMainDate(date: LocalDateTime): Triple<String, String, String> {
        val dayOfWeek = date.format(dayOfWeekFormatter).replaceFirstChar { it.uppercase() }
        val dayOfMonth = date.format(dayOfMonthFormatter)
        val monthYear = date.format(monthYearFormatter)

        return Triple(dayOfWeek, dayOfMonth, monthYear)
    }

    /**
     * Formate la date complète pour les en-têtes d'événements
     * Format : "Lundi 4 août 2025"
     */
    fun formatEventHeaderDate(date: LocalDate): String {
        return date.format(fullDateFormatter).replaceFirstChar { it.uppercase() }
    }

    /**
     * Formate l'heure pour les événements
     * Format : "14h30" ou "Toute la journée"
     */
    fun formatEventTime(startTime: LocalDateTime, isAllDay: Boolean): String {
        return if (isAllDay) {
            "Toute la journée"
        } else {
            startTime.format(timeFormatter)
        }
    }

    /**
     * Formate la durée d'un événement
     * Format : "14h30 - 16h00" ou "2h30"
     */
    fun formatEventDuration(startTime: LocalDateTime, endTime: LocalDateTime): String {
        val start = startTime.format(timeFormatter)
        val end = endTime.format(timeFormatter)
        return "$start - $end"
    }

    /**
     * Formate une durée simple
     * Format : "2h30", "45min", "3 jours"
     */
    fun formatDuration(startTime: LocalDateTime, endTime: LocalDateTime): String {
        val minutes = ChronoUnit.MINUTES.between(startTime, endTime)
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        val days = hours / 24
        val remainingHours = hours % 24

        return when {
            days > 0 -> if (days == 1L) "1 jour" else "$days jours"
            hours > 0 && remainingMinutes > 0 -> "${hours}h${remainingMinutes.toString().padStart(2, '0')}"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}min"
            else -> "0min"
        }
    }

    /**
     * Formate un timestamp relatif pour les synchronisations
     * Format : "il y a 5 min", "il y a 2h", "hier", "jamais"
     */
    fun formatRelativeTime(dateTime: LocalDateTime?): String {
        if (dateTime == null) return "Jamais"

        val now = LocalDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "il y a ${minutes}min"
            hours < 24 -> "il y a ${hours}h"
            days == 1L -> "Hier"
            days < 7 -> "il y a ${days} jour${if (days > 1) "s" else ""}"
            days < 30 -> "il y a ${days / 7} semaine${if (days / 7 > 1) "s" else ""}"
            else -> "il y a plus d'un mois"
        }
    }

    /**
     * Formate une date pour le statut de synchronisation
     * Format : "03/08 à 14h30"
     */
    fun formatSyncDate(dateTime: LocalDateTime): String {
        val dayMonth = dateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
        val time = dateTime.format(timeFormatter)
        return "$dayMonth à $time"
    }

    /**
     * Détermine si un événement est "aujourd'hui", "demain", etc.
     */
    fun getRelativeDay(eventDate: LocalDate): String? {
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(today, eventDate)

        return when (daysDiff) {
            0L -> "Aujourd'hui"
            1L -> "Demain"
            -1L -> "Hier"
            in 2..6 -> null // Utiliser le nom du jour
            else -> null // Utiliser la date complète
        }
    }

    /**
     * Formate une date d'événement avec contexte
     * Utilise "Aujourd'hui", "Demain" ou le nom du jour si proche
     */
    fun formatEventDateWithContext(eventDate: LocalDate): String {
        val relativeDay = getRelativeDay(eventDate)

        return if (relativeDay != null) {
            relativeDay
        } else {
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(today, eventDate)

            when {
                daysDiff in 2..6 -> {
                    // Cette semaine - juste le nom du jour
                    eventDate.format(dayOfWeekFormatter).replaceFirstChar { it.uppercase() }
                }
                else -> {
                    // Date complète
                    formatEventHeaderDate(eventDate)
                }
            }
        }
    }

    /**
     * Vérifie si deux dates sont le même jour
     */
    fun isSameDay(date1: LocalDateTime, date2: LocalDateTime): Boolean {
        return date1.toLocalDate() == date2.toLocalDate()
    }

    /**
     * Vérifie si une date est aujourd'hui
     */
    fun isToday(date: LocalDateTime): Boolean {
        return date.toLocalDate() == LocalDate.now()
    }

    /**
     * Vérifie si une date est dans le futur
     */
    fun isFuture(date: LocalDateTime): Boolean {
        return date.isAfter(LocalDateTime.now())
    }

    /**
     * Vérifie si un événement est en cours maintenant
     */
    fun isCurrentlyRunning(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(startTime) && now.isBefore(endTime)
    }

    /**
     * Formate un nombre d'événements pour l'affichage
     */
    fun formatEventsCount(count: Int): String {
        return when (count) {
            0 -> "Aucun événement"
            1 -> "1 événement"
            else -> "$count événements"
        }
    }

    /**
     * Formate le statut de synchronisation avec icône
     */
    fun formatSyncStatus(isSuccess: Boolean, errorMessage: String? = null): String {
        return if (isSuccess) {
            "✓ Synchronisé"
        } else {
            "✗ ${errorMessage ?: "Erreur"}"
        }
    }

    /**
     * Formate une fréquence de synchronisation en texte lisible
     */
    fun formatSyncFrequency(hours: Int): String {
        return when (hours) {
            1 -> "Toutes les heures"
            24 -> "Une fois par jour"
            else -> "Toutes les $hours heures"
        }
    }

    /**
     * Formate un nombre de semaines pour les paramètres
     */
    fun formatWeeksAhead(weeks: Int): String {
        return when (weeks) {
            1 -> "1 semaine"
            else -> "$weeks semaines"
        }
    }

    /**
     * Formate un nombre d'événements max pour les paramètres
     */
    fun formatMaxEvents(count: Int): String {
        return "$count événements maximum"
    }

    /**
     * Formate une durée en millisecondes en texte lisible
     */
    fun formatDurationFromMillis(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}j ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}min"
            minutes > 0 -> "${minutes}min"
            else -> "${seconds}s"
        }
    }
}