package de.montagezeit.app.diagnostics.debug

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAccessor
import org.json.JSONArray
import org.json.JSONObject

internal object DebugDiagnosticsJson {
    fun encode(value: Any?): String = encodeValue(value).toString()

    fun prettyPrint(rawJson: String): String {
        return runCatching {
            when {
                rawJson.trim().startsWith("{") -> JSONObject(rawJson).toString(2)
                rawJson.trim().startsWith("[") -> JSONArray(rawJson).toString(2)
                else -> rawJson
            }
        }.getOrDefault(rawJson)
    }

    private fun encodeValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is JSONObject -> value
            is JSONArray -> value
            is Map<*, *> -> JSONObject().apply {
                value.entries
                    .sortedBy { it.key.toString() }
                    .forEach { (key, entryValue) ->
                        put(key.toString(), encodeValue(entryValue))
                    }
            }
            is Iterable<*> -> JSONArray().apply {
                value.forEach { put(encodeValue(it)) }
            }
            is Array<*> -> JSONArray().apply {
                value.forEach { put(encodeValue(it)) }
            }
            is Boolean,
            is Number,
            is String -> value
            is Enum<*> -> value.name
            is LocalDate,
            is LocalTime,
            is LocalDateTime,
            is TemporalAccessor -> value.toString()
            else -> value.toString()
        }
    }
}
