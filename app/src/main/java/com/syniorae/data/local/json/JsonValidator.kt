package com.syniorae.data.local.json

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException

/**
 * Validateur pour les fichiers JSON
 * Vérifie la syntaxe et la structure des données
 */
class JsonValidator {

    /**
     * Vérifie si une chaîne est un JSON valide
     */
    fun isValidJson(jsonString: String): Boolean {
        return try {
            JsonParser.parseString(jsonString)
            true
        } catch (e: JsonSyntaxException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Valide la structure d'un fichier de configuration de widget
     */
    fun validateConfigStructure(jsonString: String): ValidationResult {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            // Vérifications communes pour tous les widgets
            val errors = mutableListOf<String>()

            if (!jsonObject.has("widgetType")) {
                errors.add("Champ 'widgetType' manquant")
            }

            if (!jsonObject.has("isConfigured")) {
                errors.add("Champ 'isConfigured' manquant")
            }

            if (!jsonObject.has("lastUpdate")) {
                errors.add("Champ 'lastUpdate' manquant")
            }

            ValidationResult(
                isValid = errors.isEmpty(),
                errors = errors
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf("Structure JSON invalide: ${e.message}")
            )
        }
    }

    /**
     * Valide la structure d'un fichier de données de calendrier
     */
    fun validateCalendarDataStructure(jsonString: String): ValidationResult {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val errors = mutableListOf<String>()

            if (!jsonObject.has("lastSync")) {
                errors.add("Champ 'lastSync' manquant")
            }

            if (!jsonObject.has("syncStatus")) {
                errors.add("Champ 'syncStatus' manquant")
            }

            if (!jsonObject.has("events")) {
                errors.add("Champ 'events' manquant")
            } else {
                val eventsArray = jsonObject.getAsJsonArray("events")
                eventsArray.forEachIndexed { index, eventElement ->
                    if (eventElement.isJsonObject) {
                        val event = eventElement.asJsonObject
                        if (!event.has("id")) {
                            errors.add("Événement $index: champ 'id' manquant")
                        }
                        if (!event.has("title")) {
                            errors.add("Événement $index: champ 'title' manquant")
                        }
                        if (!event.has("startDateTime")) {
                            errors.add("Événement $index: champ 'startDateTime' manquant")
                        }
                        if (!event.has("endDateTime")) {
                            errors.add("Événement $index: champ 'endDateTime' manquant")
                        }
                    }
                }
            }

            ValidationResult(
                isValid = errors.isEmpty(),
                errors = errors
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf("Structure JSON invalide: ${e.message}")
            )
        }
    }

    /**
     * Valide la structure d'un fichier d'associations d'icônes
     */
    fun validateIconsStructure(jsonString: String): ValidationResult {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val errors = mutableListOf<String>()

            if (!jsonObject.has("associations")) {
                errors.add("Champ 'associations' manquant")
            } else {
                val associations = jsonObject.getAsJsonArray("associations")
                associations.forEachIndexed { index, assocElement ->
                    if (assocElement.isJsonObject) {
                        val assoc = assocElement.asJsonObject
                        if (!assoc.has("keywords")) {
                            errors.add("Association $index: champ 'keywords' manquant")
                        }
                        if (!assoc.has("iconPath")) {
                            errors.add("Association $index: champ 'iconPath' manquant")
                        }
                    }
                }
            }

            ValidationResult(
                isValid = errors.isEmpty(),
                errors = errors
            )
        } catch (e: Exception) {
            ValidationResult(
                isValid = false,
                errors = listOf("Structure JSON invalide: ${e.message}")
            )
        }
    }

    /**
     * Vérifie si un JSON n'est pas vide
     */
    fun isNotEmpty(jsonString: String): Boolean {
        return try {
            val jsonElement = JsonParser.parseString(jsonString)
            !jsonElement.isJsonNull &&
                    !(jsonElement.isJsonObject && jsonElement.asJsonObject.size() == 0) &&
                    !(jsonElement.isJsonArray && jsonElement.asJsonArray.size() == 0)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Nettoie un JSON en supprimant les champs null ou vides
     */
    fun cleanJson(jsonString: String): String {
        return try {
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            // Supprime les champs null
            val keysToRemove = mutableListOf<String>()
            for (entry in jsonObject.entrySet()) {
                if (entry.value.isJsonNull) {
                    keysToRemove.add(entry.key)
                }
            }
            keysToRemove.forEach { jsonObject.remove(it) }

            jsonObject.toString()
        } catch (e: Exception) {
            jsonString // Retourne l'original si erreur
        }
    }
}

/**
 * Résultat de validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun getErrorMessage(): String {
        return errors.joinToString("; ")
    }
}