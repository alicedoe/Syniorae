package com.syniorae.data.local.json

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Adaptateur Gson pour LocalDateTime
 * Permet la sérialisation/désérialisation JSON des dates
 */
class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src == null) {
            JsonNull.INSTANCE
        } else {
            JsonPrimitive(src.format(FORMATTER))
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        return try {
            if (json?.isJsonNull != false) {
                null
            } else {
                LocalDateTime.parse(json.asString, FORMATTER)
            }
        } catch (e: DateTimeParseException) {
            throw JsonParseException("Erreur parsing LocalDateTime: ${json?.asString}", e)
        }
    }
}