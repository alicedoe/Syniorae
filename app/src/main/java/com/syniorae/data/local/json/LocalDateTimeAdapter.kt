package com.syniorae.data.local.json

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Adaptateur Gson pour sérialiser/désérialiser LocalDateTime
 */
class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return if (src != null) {
            JsonPrimitive(src.format(formatter))
        } else {
            JsonNull.INSTANCE
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime? {
        return try {
            if (json?.isJsonNull == false) {
                LocalDateTime.parse(json.asString, formatter)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}