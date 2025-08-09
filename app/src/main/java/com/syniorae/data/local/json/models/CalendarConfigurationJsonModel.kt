package com.syniorae.data.local.json.models

import java.time.LocalDateTime

/**
 * Modèle pour le fichier config.json du calendrier
 * Structure selon le cahier des charges
 */
data class CalendarConfigurationJsonModel(
    val calendrier_id: String,
    val calendrier_name: String = "",
    val nb_semaines_max: Int,
    val nb_evenements_max: Int,
    val frequence_synchro: Int, // en heures
    val google_account_email: String? = null,
    val widget_type: String = "calendar",
    val is_configured: Boolean = true,
    val last_update: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun createDefault() = CalendarConfigurationJsonModel(
            calendrier_id = "",
            nb_semaines_max = 4,
            nb_evenements_max = 50,
            frequence_synchro = 4
        )
    }

    fun isValid(): Boolean {
        return calendrier_id.isNotBlank() &&
                nb_semaines_max > 0 &&
                nb_evenements_max > 0 &&
                frequence_synchro > 0
    }
}

/**
 * Modèle pour le fichier events.json
 * Structure selon le cahier des charges
 */
data class EventsJsonModel(
    val derniere_synchro: LocalDateTime?,
    val statut: String, // "success" ou "error"
    val error_message: String? = null,
    val nb_evenements_recuperes: Int = 0,
    val evenements: List<EventJsonModel>,
    val source_api: String = "google_calendar",
    val calendar_id: String = ""
) {
    companion object {
        fun createEmpty() = EventsJsonModel(
            derniere_synchro = null,
            statut = "success",
            evenements = emptyList()
        )

        fun createError(errorMessage: String) = EventsJsonModel(
            derniere_synchro = LocalDateTime.now(),
            statut = "error",
            error_message = errorMessage,
            evenements = emptyList()
        )
    }

    fun isSuccess(): Boolean = statut == "success"
    fun hasEvents(): Boolean = evenements.isNotEmpty()
}

/**
 * Modèle pour un événement individuel dans events.json
 */
data class EventJsonModel(
    val id: String,
    val titre: String,
    val date_debut: LocalDateTime,
    val date_fin: LocalDateTime,
    val en_cours: Boolean = false,
    val toute_journee: Boolean = false,
    val multi_jours: Boolean = false,
    val calendrier_source: String = ""
) {
    fun isToday(): Boolean {
        val today = LocalDateTime.now().toLocalDate()
        return date_debut.toLocalDate() == today
    }

    fun isFuture(): Boolean {
        return date_debut.isAfter(LocalDateTime.now())
    }

    fun isCurrentlyRunning(): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(date_debut) && now.isBefore(date_fin)
    }
}

/**
 * Modèle pour le fichier icons.json
 * Structure selon le cahier des charges
 */
data class IconsJsonModel(
    val associations: List<IconAssociationJsonModel>
) {
    companion object {
        fun createEmpty() = IconsJsonModel(associations = emptyList())

        fun createDefault() = IconsJsonModel(
            associations = listOf(
                IconAssociationJsonModel(
                    mots_cles = listOf("médecin", "docteur", "rdv"),
                    icone = "ic_medical",
                    display_name = "Médical"
                ),
                IconAssociationJsonModel(
                    mots_cles = listOf("famille", "anniversaire"),
                    icone = "ic_family",
                    display_name = "Famille"
                ),
                IconAssociationJsonModel(
                    mots_cles = listOf("travail", "réunion", "bureau"),
                    icone = "ic_work",
                    display_name = "Travail"
                )
            )
        )
    }

    fun findMatchingIcon(eventTitle: String): String? {
        val titleLower = eventTitle.lowercase()
        return associations.firstOrNull { association ->
            association.mots_cles.any { keyword ->
                titleLower.contains(keyword.lowercase())
            }
        }?.icone
    }
}

/**
 * Association d'icône selon le cahier des charges
 */
data class IconAssociationJsonModel(
    val mots_cles: List<String>,
    val icone: String,
    val display_name: String = ""
) {
    fun isValid(): Boolean {
        return mots_cles.isNotEmpty() &&
                mots_cles.all { it.isNotBlank() } &&
                icone.isNotBlank()
    }
}