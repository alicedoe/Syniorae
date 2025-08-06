package com.syniorae.core.utils

import android.util.Patterns
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * Utilitaires de validation pour l'application SyniOrae
 * Valide les données utilisateur selon le cahier des charges
 */
object ValidationUtils {

    /**
     * Valide un email Google
     */
    fun isValidGoogleEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false

        return Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                (email.endsWith("@gmail.com") || email.endsWith("@googlemail.com") ||
                        email.contains("@") && isValidEmailDomain(email))
    }

    /**
     * Vérifie si le domaine email est valide (pour les comptes professionnels Google Workspace)
     */
    private fun isValidEmailDomain(email: String): Boolean {
        val domain = email.substringAfter("@")
        return domain.isNotBlank() && domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".")
    }

    /**
     * Valide un ID de calendrier Google
     */
    fun isValidCalendarId(calendarId: String?): Boolean {
        if (calendarId.isNullOrBlank()) return false

        // Les IDs de calendrier Google peuvent être :
        // - Une adresse email (calendrier principal)
        // - Un UUID (calendriers secondaires)
        // - "primary" pour le calendrier principal
        return when {
            calendarId == "primary" -> true
            calendarId.contains("@") -> isValidGoogleEmail(calendarId)
            calendarId.length >= 10 -> true // Probablement un UUID
            else -> false
        }
    }

    /**
     * Valide le nombre de semaines de récupération
     */
    fun isValidWeeksAhead(weeks: Int): Boolean {
        return weeks in 1..12 // Selon le cahier des charges
    }

    /**
     * Valide le nombre maximum d'événements
     */
    fun isValidMaxEvents(maxEvents: Int): Boolean {
        return maxEvents in 10..200 // Selon les paramètres du SeekBar
    }

    /**
     * Valide la fréquence de synchronisation
     */
    fun isValidSyncFrequency(hours: Int): Boolean {
        return hours in listOf(1, 2, 4, 8, 24) // Fréquences autorisées
    }

    /**
     * Valide les mots-clés pour les associations d'icônes
     */
    fun isValidKeywords(keywords: List<String>): ValidationResult {
        val errors = mutableListOf<String>()

        if (keywords.isEmpty()) {
            errors.add("Au moins un mot-clé est requis")
            return ValidationResult(false, errors)
        }

        keywords.forEachIndexed { index, keyword ->
            val trimmed = keyword.trim()
            when {
                trimmed.isBlank() -> errors.add("Le mot-clé ${index + 1} est vide")
                trimmed.length < 2 -> errors.add("Le mot-clé '$trimmed' est trop court (minimum 2 caractères)")
                trimmed.length > 50 -> errors.add("Le mot-clé '$trimmed' est trop long (maximum 50 caractères)")
                !trimmed.matches(Regex("^[a-zA-ZÀ-ÿ0-9\\s-']+$")) ->
                    errors.add("Le mot-clé '$trimmed' contient des caractères non autorisés")
            }
        }

        // Vérifier les doublons
        val duplicates = keywords.groupBy { it.lowercase().trim() }
            .filter { it.value.size > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            errors.add("Mots-clés en double : ${duplicates.joinToString(", ")}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide une icône ou emoji
     */
    fun isValidIcon(icon: String?): ValidationResult {
        val errors = mutableListOf<String>()

        when {
            icon.isNullOrBlank() -> errors.add("Une icône est requise")
            icon.length > 10 -> errors.add("L'icône est trop longue (maximum 10 caractères)")
            icon.trim() != icon -> errors.add("L'icône ne peut pas commencer ou finir par des espaces")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide qu'une date est dans une plage acceptable
     */
    fun isValidEventDate(dateTime: LocalDateTime?): Boolean {
        if (dateTime == null) return false

        val now = LocalDateTime.now()
        val maxFuture = now.plusYears(2) // Maximum 2 ans dans le futur
        val maxPast = now.minusYears(1)   // Maximum 1 an dans le passé

        return dateTime.isAfter(maxPast) && dateTime.isBefore(maxFuture)
    }

    /**
     * Valide qu'une heure de fin est après l'heure de début
     */
    fun isValidEventTimeRange(startTime: LocalDateTime?, endTime: LocalDateTime?): ValidationResult {
        val errors = mutableListOf<String>()

        when {
            startTime == null -> errors.add("Heure de début manquante")
            endTime == null -> errors.add("Heure de fin manquante")
            startTime != null && endTime != null -> {
                if (!startTime.isBefore(endTime)) {
                    errors.add("L'heure de fin doit être après l'heure de début")
                }

                // Vérifier la durée maximum (24 heures)
                if (java.time.Duration.between(startTime, endTime).toHours() > 24) {
                    errors.add("La durée de l'événement ne peut pas dépasser 24 heures")
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide un titre d'événement
     */
    fun isValidEventTitle(title: String?): ValidationResult {
        val errors = mutableListOf<String>()

        when {
            title.isNullOrBlank() -> errors.add("Le titre est requis")
            title.trim().length < 2 -> errors.add("Le titre est trop court (minimum 2 caractères)")
            title.length > 200 -> errors.add("Le titre est trop long (maximum 200 caractères)")
            title.trim() != title -> errors.add("Le titre ne peut pas commencer ou finir par des espaces")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide un nom de calendrier
     */
    fun isValidCalendarName(name: String?): ValidationResult {
        val errors = mutableListOf<String>()

        when {
            name.isNullOrBlank() -> errors.add("Le nom du calendrier est requis")
            name.trim().length < 2 -> errors.add("Le nom est trop court (minimum 2 caractères)")
            name.length > 100 -> errors.add("Le nom est trop long (maximum 100 caractères)")
            name.trim() != name -> errors.add("Le nom ne peut pas commencer ou finir par des espaces")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide une configuration complète de calendrier
     */
    fun validateCalendarConfig(
        googleEmail: String?,
        calendarId: String?,
        calendarName: String?,
        weeksAhead: Int,
        maxEvents: Int,
        syncFrequency: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validation email Google
        if (!isValidGoogleEmail(googleEmail)) {
            errors.add("Adresse Gmail invalide")
        }

        // Validation calendrier
        if (!isValidCalendarId(calendarId)) {
            errors.add("ID de calendrier invalide")
        }

        val nameValidation = isValidCalendarName(calendarName)
        if (!nameValidation.isValid) {
            errors.addAll(nameValidation.errors)
        }

        // Validation paramètres
        if (!isValidWeeksAhead(weeksAhead)) {
            errors.add("Nombre de semaines invalide (1-12)")
        }

        if (!isValidMaxEvents(maxEvents)) {
            errors.add("Nombre d'événements invalide (10-200)")
        }

        if (!isValidSyncFrequency(syncFrequency)) {
            errors.add("Fréquence de synchronisation invalide")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide qu'une chaîne peut être parsée en LocalDateTime
     */
    fun isValidDateTimeString(dateTimeString: String?): Boolean {
        if (dateTimeString.isNullOrBlank()) return false

        return try {
            LocalDateTime.parse(dateTimeString)
            true
        } catch (e: DateTimeParseException) {
            false
        }
    }

    /**
     * Nettoie et valide une liste de mots-clés depuis une chaîne
     */
    fun cleanAndValidateKeywords(keywordsString: String?): ValidationResult {
        if (keywordsString.isNullOrBlank()) {
            return ValidationResult(false, listOf("Aucun mot-clé fourni"))
        }

        val keywords = keywordsString.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return isValidKeywords(keywords)
    }

    /**
     * Valide qu'un fichier JSON a une structure minimale correcte
     */
    fun isValidJsonStructure(jsonString: String?): Boolean {
        if (jsonString.isNullOrBlank()) return false

        return try {
            // Utiliser le JsonValidator existant
            com.syniorae.data.local.json.JsonValidator().isValidJson(jsonString)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Classe pour encapsuler les résultats de validation
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    ) {
        fun getErrorMessage(): String {
            return errors.joinToString("\n")
        }

        fun getFirstError(): String? {
            return errors.firstOrNull()
        }

        fun hasErrors(): Boolean = errors.isNotEmpty()
    }
}