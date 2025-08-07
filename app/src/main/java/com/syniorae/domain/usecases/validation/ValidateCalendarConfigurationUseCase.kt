package com.syniorae.domain.usecases.validation

import com.syniorae.core.utils.ValidationUtils
import com.syniorae.data.local.json.JsonFileManager
import com.syniorae.data.local.json.JsonFileType
import com.syniorae.data.local.json.models.ConfigurationJsonModel
import com.syniorae.domain.models.widgets.WidgetType
import com.syniorae.domain.models.widgets.calendar.CalendarConfig
import com.syniorae.domain.models.widgets.calendar.IconAssociation

/**
 * Use case pour valider la configuration du calendrier
 */
class ValidateCalendarConfigurationUseCase(
    private val jsonFileManager: JsonFileManager
) {

    /**
     * Valide une configuration complète de calendrier
     */
    suspend fun execute(config: CalendarConfig): ValidationResult {
        val errors = mutableListOf<String>()

        // Validation des champs requis
        if (config.selectedCalendarId.isBlank()) {
            errors.add("ID de calendrier requis")
        } else if (!ValidationUtils.isValidCalendarId(config.selectedCalendarId)) {
            errors.add("ID de calendrier invalide")
        }

        if (config.selectedCalendarName.isBlank()) {
            errors.add("Nom de calendrier requis")
        } else {
            val nameValidation = ValidationUtils.isValidCalendarName(config.selectedCalendarName)
            if (!nameValidation.isValid) {
                errors.addAll(nameValidation.errors)
            }
        }

        // Validation de l'email Google
        if (config.googleAccountEmail != null) {
            if (!ValidationUtils.isValidGoogleEmail(config.googleAccountEmail)) {
                errors.add("Adresse email Google invalide")
            }
        }

        // Validation des paramètres numériques
        if (!ValidationUtils.isValidWeeksAhead(config.weeksAhead)) {
            errors.add("Nombre de semaines invalide (doit être entre 1 et 12)")
        }

        if (!ValidationUtils.isValidMaxEvents(config.maxEvents)) {
            errors.add("Nombre d'événements max invalide (doit être entre 10 et 200)")
        }

        if (!ValidationUtils.isValidSyncFrequency(config.syncFrequencyHours)) {
            errors.add("Fréquence de synchronisation invalide (1, 2, 4, 8 ou 24 heures)")
        }

        // Validation de cohérence
        val coherenceErrors = validateConfigCoherence(config)
        errors.addAll(coherenceErrors)

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide la cohérence interne de la configuration
     */
    private fun validateConfigCoherence(config: CalendarConfig): List<String> {
        val errors = mutableListOf<String>()

        // Vérifier que les paramètres sont cohérents
        if (config.weeksAhead > 12 && config.maxEvents < 50) {
            errors.add("Nombre d'événements probablement insuffisant pour ${config.weeksAhead} semaines")
        }

        if (config.syncFrequencyHours > 24 && config.weeksAhead < 4) {
            errors.add("Fréquence de sync trop faible pour une période courte")
        }

        return errors
    }

    /**
     * Valide qu'une configuration existe et est complète
     */
    suspend fun validateExistingConfiguration(widgetType: WidgetType): ValidationResult {
        return try {
            val config = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.CONFIG,
                ConfigurationJsonModel::class.java
            )

            if (config == null) {
                ValidationResult(false, listOf("Aucune configuration trouvée"))
            } else if (!config.isValid()) {
                ValidationResult(false, listOf("Configuration incomplète ou invalide"))
            } else {
                ValidationResult(true, emptyList())
            }
        } catch (e: Exception) {
            ValidationResult(false, listOf("Erreur lors de la validation: ${e.message}"))
        }
    }
}

/**
 * Use case pour valider les associations d'icônes
 */
class ValidateIconAssociationsUseCase {

    /**
     * Valide une liste d'associations d'icônes
     */
    fun execute(associations: List<IconAssociation>): ValidationResult {
        val errors = mutableListOf<String>()
        val allKeywords = mutableSetOf<String>()

        associations.forEachIndexed { index, association ->
            // Valider les mots-clés
            val keywordsValidation = ValidationUtils.isValidKeywords(association.keywords)
            if (!keywordsValidation.isValid) {
                errors.addAll(keywordsValidation.errors.map { "Association ${index + 1}: $it" })
            }

            // Valider l'icône
            val iconValidation = ValidationUtils.isValidIcon(association.iconPath)
            if (!iconValidation.isValid) {
                errors.addAll(iconValidation.errors.map { "Association ${index + 1}: $it" })
            }

            // Vérifier les doublons de mots-clés entre associations
            association.keywords.forEach { keyword ->
                val normalizedKeyword = keyword.lowercase().trim()
                if (normalizedKeyword in allKeywords) {
                    errors.add("Mot-clé en double: '$keyword'")
                } else {
                    allKeywords.add(normalizedKeyword)
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide une association d'icône individuelle
     */
    fun validateSingle(association: IconAssociation): ValidationResult {
        val errors = mutableListOf<String>()

        // Valider les mots-clés
        val keywordsValidation = ValidationUtils.isValidKeywords(association.keywords)
        if (!keywordsValidation.isValid) {
            errors.addAll(keywordsValidation.errors)
        }

        // Valider l'icône
        val iconValidation = ValidationUtils.isValidIcon(association.iconPath)
        if (!iconValidation.isValid) {
            errors.addAll(iconValidation.errors)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Suggère des améliorations pour les associations
     */
    fun suggestImprovements(associations: List<IconAssociation>): List<String> {
        val suggestions = mutableListOf<String>()

        // Vérifier la couverture des mots-clés communs
        val commonEventTypes = listOf("médecin", "travail", "famille", "anniversaire", "réunion", "rendez-vous")
        val coveredTypes = associations.flatMap { it.keywords.map { keyword -> keyword.lowercase() } }

        commonEventTypes.forEach { eventType ->
            if (eventType !in coveredTypes) {
                suggestions.add("Considérer ajouter une association pour '$eventType'")
            }
        }

        // Vérifier la longueur des mots-clés
        associations.forEach { association ->
            if (association.keywords.size == 1) {
                suggestions.add("Ajouter des synonymes pour '${association.keywords.first()}' pour une meilleure détection")
            }
        }

        return suggestions
    }
}

/**
 * Use case pour valider les données d'événements
 */
class ValidateEventsDataUseCase {

    /**
     * Valide une liste d'événements
     */
    fun execute(events: List<com.syniorae.domain.models.widgets.calendar.CalendarEvent>): ValidationResult {
        val errors = mutableListOf<String>()

        events.forEachIndexed { index, event ->
            // Valider le titre
            val titleValidation = ValidationUtils.isValidEventTitle(event.title)
            if (!titleValidation.isValid) {
                errors.addAll(titleValidation.errors.map { "Événement ${index + 1}: $it" })
            }

            // Valider les dates
            if (!ValidationUtils.isValidEventDate(event.startDateTime)) {
                errors.add("Événement ${index + 1}: Date de début invalide")
            }

            if (!ValidationUtils.isValidEventDate(event.endDateTime)) {
                errors.add("Événement ${index + 1}: Date de fin invalide")
            }

            // Valider la plage de temps
            val timeRangeValidation = ValidationUtils.isValidEventTimeRange(event.startDateTime, event.endDateTime)
            if (!timeRangeValidation.isValid) {
                errors.addAll(timeRangeValidation.errors.map { "Événement ${index + 1}: $it" })
            }

            // Vérifications de cohérence
            if (event.isAllDay && !event.startDateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
                errors.add("Événement ${index + 1}: Événement 'toute la journée' doit commencer à minuit")
            }

            if (event.isMultiDay && event.startDateTime.toLocalDate() == event.endDateTime.toLocalDate()) {
                errors.add("Événement ${index + 1}: Événement marqué 'multi-jours' mais dates identiques")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Détecte les conflits potentiels entre événements
     */
    fun detectConflicts(events: List<com.syniorae.domain.models.widgets.calendar.CalendarEvent>): List<EventConflict> {
        val conflicts = mutableListOf<EventConflict>()

        for (i in events.indices) {
            for (j in i + 1 until events.size) {
                val event1 = events[i]
                val event2 = events[j]

                if (eventsOverlap(event1, event2)) {
                    conflicts.add(
                        EventConflict(
                            event1 = event1,
                            event2 = event2,
                            type = ConflictType.TIME_OVERLAP
                        )
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * Vérifie si deux événements se chevauchent
     */
    private fun eventsOverlap(event1: com.syniorae.domain.models.widgets.calendar.CalendarEvent, event2: com.syniorae.domain.models.widgets.calendar.CalendarEvent): Boolean {
        return event1.startDateTime.isBefore(event2.endDateTime) && event2.startDateTime.isBefore(event1.endDateTime)
    }
}

/**
 * Use case pour valider les fichiers JSON
 */
class ValidateJsonIntegrityUseCase(
    private val jsonFileManager: JsonFileManager
) {

    /**
     * Valide l'intégrité de tous les fichiers JSON d'un widget
     */
    suspend fun execute(widgetType: WidgetType): ValidationResult {
        val errors = mutableListOf<String>()

        // Vérifier le fichier de configuration
        val configValidation = validateConfigFile(widgetType)
        if (!configValidation.isValid) {
            errors.addAll(configValidation.errors.map { "Config: $it" })
        }

        // Vérifier le fichier de données
        val dataValidation = validateDataFile(widgetType)
        if (!dataValidation.isValid) {
            errors.addAll(dataValidation.errors.map { "Data: $it" })
        }

        // Vérifier le fichier d'icônes
        val iconsValidation = validateIconsFile(widgetType)
        if (!iconsValidation.isValid) {
            errors.addAll(iconsValidation.errors.map { "Icons: $it" })
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Valide le fichier de configuration
     */
    private suspend fun validateConfigFile(widgetType: WidgetType): ValidationResult {
        return try {
            val config = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.CONFIG,
                ConfigurationJsonModel::class.java
            )

            if (config == null) {
                ValidationResult(false, listOf("Fichier de configuration manquant"))
            } else if (!config.isValid()) {
                ValidationResult(false, listOf("Configuration invalide"))
            } else {
                ValidationResult(true, emptyList())
            }
        } catch (e: Exception) {
            ValidationResult(false, listOf("Erreur lecture configuration: ${e.message}"))
        }
    }

    /**
     * Valide le fichier de données
     */
    private suspend fun validateDataFile(widgetType: WidgetType): ValidationResult {
        return try {
            val eventsData = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.DATA,
                com.syniorae.data.local.json.models.EventsJsonModel::class.java
            )

            if (eventsData == null) {
                ValidationResult(false, listOf("Fichier de données manquant"))
            } else {
                val errors = mutableListOf<String>()

                if (eventsData.derniere_synchro == null && eventsData.evenements.isNotEmpty()) {
                    errors.add("Date de synchronisation manquante")
                }

                if (eventsData.statut !in listOf("success", "error")) {
                    errors.add("Statut invalide: ${eventsData.statut}")
                }

                ValidationResult(errors.isEmpty(), errors)
            }
        } catch (e: Exception) {
            ValidationResult(false, listOf("Erreur lecture données: ${e.message}"))
        }
    }

    /**
     * Valide le fichier d'icônes
     */
    private suspend fun validateIconsFile(widgetType: WidgetType): ValidationResult {
        return try {
            val iconsData = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.ICONS,
                com.syniorae.data.local.json.models.IconsJsonModel::class.java
            )

            if (iconsData == null) {
                ValidationResult(true, emptyList()) // Fichier d'icônes optionnel
            } else {
                val errors = mutableListOf<String>()

                iconsData.associations.forEachIndexed { index, association ->
                    if (!association.isValid()) {
                        errors.add("Association $index invalide")
                    }
                }

                ValidationResult(errors.isEmpty(), errors)
            }
        } catch (e: Exception) {
            ValidationResult(false, listOf("Erreur lecture icônes: ${e.message}"))
        }
    }

    /**
     * Répare automatiquement les erreurs mineures
     */
    suspend fun repairMinorIssues(widgetType: WidgetType): Boolean {
        return try {
            // Créer fichier d'événements vide si manquant
            val eventsData = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.DATA,
                com.syniorae.data.local.json.models.EventsJsonModel::class.java
            )

            if (eventsData == null) {
                val emptyEventsData = com.syniorae.data.local.json.models.EventsJsonModel.createEmpty()
                jsonFileManager.writeJsonFile(widgetType, JsonFileType.DATA, emptyEventsData)
            }

            // Créer fichier d'icônes vide si manquant
            val iconsData = jsonFileManager.readJsonFile(
                widgetType,
                JsonFileType.ICONS,
                com.syniorae.data.local.json.models.IconsJsonModel::class.java
            )

            if (iconsData == null) {
                val emptyIconsData = com.syniorae.data.local.json.models.IconsJsonModel.createEmpty()
                jsonFileManager.writeJsonFile(widgetType, JsonFileType.ICONS, emptyIconsData)
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Résultat de validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList()
) {
    fun getFirstError(): String? = errors.firstOrNull()
    fun getErrorMessage(): String = errors.joinToString("\n")
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    fun hasSuggestions(): Boolean = suggestions.isNotEmpty()
}

/**
 * Conflit entre événements
 */
data class EventConflict(
    val event1: com.syniorae.domain.models.widgets.calendar.CalendarEvent,
    val event2: com.syniorae.domain.models.widgets.calendar.CalendarEvent,
    val type: ConflictType
)

/**
 * Types de conflits possibles
 */
enum class ConflictType {
    TIME_OVERLAP,       // Chevauchement horaire
    DUPLICATE_TITLE,    // Titre identique
    SUSPICIOUS_DURATION // Durée suspecte
}