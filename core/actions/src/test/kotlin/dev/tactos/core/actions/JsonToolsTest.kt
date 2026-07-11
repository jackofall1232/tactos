package dev.tactos.core.actions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Table-driven tests for [JsonTools]: beautify/minify/validate on nested,
 * unicode, and scalar documents, plus adversarial invalid inputs.
 */
class JsonToolsTest {

    @Test
    fun `beautify pretty-prints with two-space indent preserving key order`() {
        val input = """{"b":1,"a":[1,2,{"c":null}]}"""
        val expected = """
            {
              "b": 1,
              "a": [
                1,
                2,
                {
                  "c": null
                }
              ]
            }
        """.trimIndent()
        val result = assertIs<JsonToolResult.Success>(JsonTools.beautify(input))
        assertEquals(expected, result.text)
    }

    @Test
    fun `beautify keeps unicode and escaped characters intact`() {
        val input = "{\"greeting\":\"héllo wörld\",\"escaped\":\"line1\\nline2 \\\"quoted\\\"\"}"
        val result = assertIs<JsonToolResult.Success>(JsonTools.beautify(input))
        assertTrue("héllo wörld" in result.text, "unicode content lost: ${result.text}")
        assertTrue("\\n" in result.text, "newline escape lost: ${result.text}")
        assertTrue("\\\"quoted\\\"" in result.text, "quote escape lost: ${result.text}")
    }

    @Test
    fun `minify produces compact output preserving key order`() {
        val cases = mapOf(
            "{ \"z\" : 1 , \"a\" : 2 , \"m\" : 3 }" to """{"z":1,"a":2,"m":3}""",
            "[ 1 , 2 , 3 ]" to "[1,2,3]",
            "  42  " to "42",
            "\"hi\"" to "\"hi\"",
            "true" to "true",
            "null" to "null",
            """{ "nested" : { "deep" : [ { "x" : false } ] } }""" to
                """{"nested":{"deep":[{"x":false}]}}""",
        )
        for ((input, expected) in cases) {
            val result = assertIs<JsonToolResult.Success>(
                JsonTools.minify(input),
                "minify of <$input>",
            )
            assertEquals(expected, result.text, "minify of <$input>")
        }
    }

    @Test
    fun `minify of beautify equals minify`() {
        val inputs = listOf(
            """{"b":1,"a":[1,2,{"c":null}]}""",
            """[[1,2],[3,4],{"k":"v"}]""",
            "{\"greeting\":\"héllo\",\"escaped\":\"a\\tb\"}",
            "42",
            "-3.5e2",
            "\"scalar\"",
            "true",
            "false",
            "null",
            "{}",
            "[]",
        )
        for (input in inputs) {
            val beautified = assertIs<JsonToolResult.Success>(
                JsonTools.beautify(input),
                "beautify of <$input>",
            )
            val direct = assertIs<JsonToolResult.Success>(JsonTools.minify(input))
            val roundTripped = assertIs<JsonToolResult.Success>(JsonTools.minify(beautified.text))
            assertEquals(direct.text, roundTripped.text, "round-trip of <$input>")
        }
    }

    @Test
    fun `validate describes every top-level value shape`() {
        val cases = mapOf(
            """{"a":1,"b":2,"c":3}""" to "Valid JSON — object with 3 entries",
            """{"only":1}""" to "Valid JSON — object with 1 entry",
            "{}" to "Valid JSON — object with 0 entries",
            "[1,2,3,4,5]" to "Valid JSON — array with 5 elements",
            "[42]" to "Valid JSON — array with 1 element",
            "[]" to "Valid JSON — array with 0 elements",
            "\"hello\"" to "Valid JSON — string",
            "42" to "Valid JSON — number",
            "-3.5e2" to "Valid JSON — number",
            "true" to "Valid JSON — boolean",
            "false" to "Valid JSON — boolean",
            "null" to "Valid JSON — null",
        )
        for ((input, expected) in cases) {
            val result = assertIs<JsonToolResult.Success>(
                JsonTools.validate(input),
                "validate of <$input>",
            )
            assertEquals(expected, result.text, "validate of <$input>")
        }
    }

    @Test
    fun `all tools reject invalid json with single-line messages`() {
        val invalid = listOf(
            """{"a":}""",
            "[1,2,]",
            "{'a':1}",
            """{"a":1} trailing""",
            "{a:1}",
            "nul",
            "{",
            "[1",
            "\"unterminated",
            "[1 2]",
            "{\"dup\" 1}",
        )
        val tools = mapOf<String, (String) -> JsonToolResult>(
            "beautify" to JsonTools::beautify,
            "minify" to JsonTools::minify,
            "validate" to JsonTools::validate,
        )
        for (input in invalid) {
            for ((name, tool) in tools) {
                val result = assertIs<JsonToolResult.Failure>(
                    tool(input),
                    "$name should fail on <$input>",
                )
                assertTrue(
                    result.message.isNotBlank(),
                    "$name failure message blank for <$input>",
                )
                assertFalse(
                    '\n' in result.message,
                    "$name failure message multi-line for <$input>: <${result.message}>",
                )
            }
        }
    }

    @Test
    fun `blank input fails with Empty input`() {
        for (input in listOf("", "   ", "\n\t ")) {
            assertEquals(JsonToolResult.Failure("Empty input"), JsonTools.beautify(input))
            assertEquals(JsonToolResult.Failure("Empty input"), JsonTools.minify(input))
            assertEquals(JsonToolResult.Failure("Empty input"), JsonTools.validate(input))
        }
    }
}
