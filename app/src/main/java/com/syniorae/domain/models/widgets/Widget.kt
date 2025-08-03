package com.syniorae.domain.models.widgets

/**
 * Types de widgets disponibles
 */
enum class WidgetType {
    CALENDAR,       // Widget Calendrier Google
    WEATHER,        // Widget Météo (futur)
    CONTACTS,       // Widget Contacts (futur)
    REMINDERS,      // Widget Rappels (futur)
    NEWS           // Widget Actualités (futur)
}

/**
 * États possibles d'un widget
 */
enum class WidgetStatus {
    OFF,           // Widget désactivé
    ON,            // Widget activé et configuré
    CONFIGURING,   // Widget en cours de configuration
    ERROR          // Widget en erreur
}

/**
 * Modèle de base représentant un widget
 */
data class Widget(
    val type: WidgetType,
    val status: WidgetStatus = WidgetStatus.OFF,
    val isConfigured: Boolean = false,
    val lastUpdate: Long = 0L,
    val errorMessage: String? = null,
    val displayOrder: Int = 0
) {

    /**
     * Vérifie si le widget est actif et fonctionnel
     */
    fun isActive(): Boolean {
        return status == WidgetStatus.ON && isConfigured
    }

    /**
     * Vérifie si le widget est en erreur
     */
    fun hasError(): Boolean {
        return status == WidgetStatus.ERROR || errorMessage != null
    }

    /**
     * Retourne le nom d'affichage du widget
     */
    fun getDisplayName(): String {
        return when (type) {
            WidgetType.CALENDAR -> "Calendrier Google"
            WidgetType.WEATHER -> "Météo"
            WidgetType.CONTACTS -> "Contacts"
            WidgetType.REMINDERS -> "Rappels"
            WidgetType.NEWS -> "Actualités"
        }
    }

    /**
     * Retourne les noms des fichiers JSON associés
     */
    fun getJsonFileNames(): Triple<String, String, String> {
        val baseName = type.name.lowercase()
        return Triple(
            "${baseName}_config.json",
            "${baseName}_data.json",
            "${baseName}_icons.json"
        )
    }

    companion object {
        /**
         * Crée un widget par défaut (désactivé)
         */
        fun createDefault(type: WidgetType, order: Int = 0) = Widget(
            type = type,
            status = WidgetStatus.OFF,
            isConfigured = false,
            displayOrder = order
        )

        /**
         * Liste des widgets disponibles dans l'ordre d'affichage
         */
        fun getAvailableWidgets(): List<Widget> {
            return listOf(
                createDefault(WidgetType.CALENDAR, 0)
                // Les autres widgets seront ajoutés plus tard
            )
        }
    }
}