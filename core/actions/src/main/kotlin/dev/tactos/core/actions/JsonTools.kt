package dev.tactos.core.actions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Outcome of a [JsonTools] operation: either the produced [Success.text] or a
 * concise, single-line, human-readable [Failure.message].
 */
sealed interface JsonToolResult {
    /** The operation succeeded; [text] is the produced output or verdict. */
    data class Success(val text: String) : JsonToolResult

    /** The input was not valid JSON; [message] is a single-line explanation. */
    data class Failure(val message: String) : JsonToolResult
}

/**
 * Offline JSON tooling for the clipboard's `JSON` contextual actions:
 * validate, beautify (2-space indent), and minify. Strict JSON only — no
 * lenient parsing — and key order is always preserved. Never throws.
 */
object JsonTools {
    @OptIn(ExperimentalSerializationApi::class)
    private val pretty = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Pretty-prints [input] with a 2-space indent, preserving key order. */
    fun beautify(input: String): JsonToolResult = withParsed(input) { element ->
        pretty.encodeToString(JsonElement.serializer(), element)
    }

    /** Re-encodes [input] compactly (no whitespace), preserving key order. */
    fun minify(input: String): JsonToolResult = withParsed(input) { element ->
        Json.encodeToString(JsonElement.serializer(), element)
    }

    /**
     * Checks [input] and returns a short verdict describing the top-level
     * value, e.g. `Valid JSON — object with 3 entries`.
     */
    fun validate(input: String): JsonToolResult = withParsed(input) { element ->
        "Valid JSON — ${describe(element)}"
    }

    private inline fun withParsed(
        input: String,
        transform: (JsonElement) -> String,
    ): JsonToolResult {
        if (input.isBlank()) return JsonToolResult.Failure("Empty input")
        val element = try {
            Json.parseToJsonElement(input)
        } catch (e: Exception) {
            return JsonToolResult.Failure(failureMessage(e))
        }
        findLooseLiteral(element)?.let { token ->
            return JsonToolResult.Failure("Unexpected token '$token'")
        }
        return JsonToolResult.Success(transform(element))
    }

    private val JSON_NUMBER = Regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")

    /**
     * kotlinx parses bare tokens like `nul` or `hello` as unquoted string
     * literals instead of failing. Strict JSON has no such value, so walk the
     * tree and surface the first non-string primitive that isn't a number,
     * boolean, or null; returns `null` when the document is strictly valid.
     */
    private fun findLooseLiteral(element: JsonElement): String? = when (element) {
        is JsonObject -> element.values.firstNotNullOfOrNull(::findLooseLiteral)
        is JsonArray -> element.firstNotNullOfOrNull(::findLooseLiteral)
        is JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> null
            element.content == "true" || element.content == "false" -> null
            JSON_NUMBER.matches(element.content) -> null
            else -> element.content
        }
    }

    private fun describe(element: JsonElement): String = when (element) {
        is JsonObject -> "object with ${element.size} ${plural(element.size, "entry", "entries")}"
        is JsonArray -> "array with ${element.size} ${plural(element.size, "element", "elements")}"
        is JsonNull -> "null"
        is JsonPrimitive -> when {
            element.isString -> "string"
            element.content == "true" || element.content == "false" -> "boolean"
            else -> "number"
        }
    }

    private fun plural(count: Int, singular: String, plural: String): String {
        return if (count == 1) singular else plural
    }

    /**
     * Reduces a parser exception to a concise single-line message: first line
     * only (which keeps kotlinx's offset/path info but drops the multi-line
     * "JSON input" dump), no exception class names.
     */
    private fun failureMessage(e: Exception): String {
        val firstLine = e.message.orEmpty().substringBefore('\n').trim()
        return firstLine.ifBlank { "Invalid JSON" }
    }
}
