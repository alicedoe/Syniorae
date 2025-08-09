package com.syniorae.data.local.json.models

import java.time.LocalDateTime

/**
 * Modèle générique pour le fichier config.json
 * Pour les widgets autres que le calendrier
 */
data class ConfigurationJsonModel(
    val widget_type: String,
    val is_configured: Boolean = false,
    val last_update: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun createDefault(widgetType: String) = ConfigurationJsonModel(
            widget_type = widgetType
        )
    }

    fun isValid(): Boolean {
        return widget_type.isNotBlank()
    }
}