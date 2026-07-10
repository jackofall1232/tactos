package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [JsonDetector]. Only syntactically valid
 * top-level objects/arrays match; primitives, trailing content, bad escapes,
 * raw control characters, and over-deep nesting are rejected.
 *
 * Object/array literals use Kotlin raw strings so their inner quotes and JSON
 * escape sequences stay byte-exact; raw control characters and primitives use
 * ordinary escaped strings.
 */
class JsonDetectorTest {

    private val deepOk = "[".repeat(200) + "]".repeat(200)     // depth 200 <= 512
    private val deepBad = "[".repeat(600) + "]".repeat(600)    // depth 600 > 512

    @Test
    fun `declares JSON type`() {
        assertEquals(ClipType.JSON, JsonDetector.type)
    }

    @Test
    fun `accepts valid JSON objects and arrays`() {
        val accepted = listOf(
            """{}""",
            """[]""",
            """[1,2,3]""",
            """{"a":1}""",
            """{"a":1,"b":2}""",
            """{"a":"b"}""",
            """{"nested":{"x":[1,2,{"y":true}]}}""",
            """[true,false,null]""",
            """[1.5,-2,3e10,4.2e-3]""",
            """{"s":"a\nb"}""",                 // valid \n escape (raw string keeps backslash-n)
            """{"u":"\u00e9"}""",               // \uXXXX escape
            """{"e":"a\"b\\c\/d\bf\fg\nh\ri\tj"}""", // every string escape
            """["a","b","c"]""",
            """{"a":[]}""",
            """[{}]""",
            """{"num":-0}""",
            """{"n":0}""",
            """{"n":0.5}""",
            """{"n":1e10}""",
            """{"n":1E10}""",
            """{"n":1.5e+3}""",
            """{"n":-1.5E-3}""",
            """[ 1 , 2 , 3 ]""",                // JSON whitespace between tokens
            """{ "a" : 1 }""",
            """{"deep":{"a":{"b":{"c":1}}}}""",
            """[[[[]]]]""",
            """{"empty":""}""",
            """[null]""",
            """{"bool":true}""",
            """{"k e y":"v a l"}""",
            deepOk,
        )
        for (case in accepted) {
            assertTrue(JsonDetector.matches(case), "expected JSON to match: <$case>")
        }
    }

    @Test
    fun `rejects primitives and malformed JSON`() {
        val rejected = listOf(
            // top-level primitives are not objects/arrays
            "42",
            "true",
            "false",
            "null",
            "\"quoted\"",
            "3.14",
            "'single'",
            "{",
            "}",
            "[",
            "]",
            "",
            "   ",
            // structural errors (raw strings)
            """{"a":}""",                       // missing value
            """{"a":1,}""",                     // trailing comma (object)
            """[1,2,3,]""",                     // trailing comma (array)
            """{'a':1}""",                      // single-quoted key
            """{a:1}""",                        // unquoted key
            """{"a":1""",                       // unterminated object
            """[1,2""",                         // unterminated array
            """{}extra""",                      // trailing content
            """[] x""",                         // trailing content
            """{"a":1}{"b":2}""",               // two top-level values
            """{"a" 1}""",                      // missing colon
            """{"a":1 "b":2}""",                // missing comma
            """{"a":1,,"b":2}""",               // double comma
            """[,]""",                          // leading comma
            """{"a":+1}""",                     // leading plus in number
            """{"a":.5}""",                     // missing integer part
            """{"a":1.}""",                     // missing fraction digits
            """{"a":1e}""",                     // missing exponent digits
            """{"a":01}""",                     // leading zero
            """{"a":00}""",
            """[01]""",
            """{"a":tru}""",                    // bad literal
            """{"a":True}""",                   // capitalized literal
            """{"a":NaN}""",
            """{"a":Infinity}""",
            """{"key":"unterminated}""",        // unterminated string
            """{"a":"bad\escape"}""",           // invalid escape \e
            """{"a":"\u12"}""",                 // incomplete \u
            """{"a":"\uXYZW"}""",               // non-hex \u
            """["\x41"]""",                     // \x is not a JSON escape
            // raw control characters inside a string (ordinary escaped strings)
            "{\"s\":\"a\nb\"}",                 // raw newline (U+000A)
            "{\"s\":\"a\tb\"}",                 // raw tab (U+0009)
            deepBad,                            // nesting depth > 512
        )
        for (case in rejected) {
            assertFalse(JsonDetector.matches(case), "expected NON-JSON: <$case>")
        }
    }
}
