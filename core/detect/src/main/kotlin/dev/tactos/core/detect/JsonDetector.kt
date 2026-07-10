package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is a syntactically valid JSON object or
 * array. Bare primitives (`42`, `true`, a lone quoted string) do not match.
 *
 * Validation is a strict, dependency-free recursive-descent parse over the
 * complete JSON grammar: objects, arrays, strings with the standard escapes
 * (`\" \\ \/ \b \f \n \r \t \uXXXX`), numbers (optional minus, no leading
 * zeros, optional fraction and exponent), the literals `true`/`false`/`null`,
 * and JSON whitespace between tokens. Trailing content, trailing commas,
 * single quotes, unquoted keys, raw control characters inside strings, and
 * nesting deeper than 512 are all rejected.
 */
object JsonDetector : TypeDetector {
    override val type: ClipType = ClipType.JSON

    private const val MAX_DEPTH = 512

    override fun matches(text: String): Boolean {
        return Parser(text).parseDocument()
    }

    /** Single-use recursive-descent parser with an index cursor into [s]. */
    private class Parser(private val s: String) {
        private var i = 0

        /** Parses a whole document: a top-level object or array, then EOF. */
        fun parseDocument(): Boolean {
            skipWhitespace()
            if (i >= s.length) return false
            val c = s[i]
            if (c != '{' && c != '[') return false // primitives never match
            if (!parseValue(0)) return false
            skipWhitespace()
            return i == s.length // reject trailing content
        }

        private fun parseValue(depth: Int): Boolean {
            skipWhitespace()
            if (i >= s.length) return false
            return when (s[i]) {
                '{' -> parseObject(depth)
                '[' -> parseArray(depth)
                '"' -> parseString()
                't' -> parseLiteral("true")
                'f' -> parseLiteral("false")
                'n' -> parseLiteral("null")
                else -> parseNumber()
            }
        }

        private fun parseObject(depth: Int): Boolean {
            if (depth + 1 > MAX_DEPTH) return false
            i++ // consume '{'
            skipWhitespace()
            if (i < s.length && s[i] == '}') {
                i++
                return true
            }
            while (true) {
                skipWhitespace()
                if (i >= s.length || s[i] != '"') return false // key must be a string
                if (!parseString()) return false
                skipWhitespace()
                if (i >= s.length || s[i] != ':') return false
                i++ // consume ':'
                if (!parseValue(depth + 1)) return false
                skipWhitespace()
                if (i >= s.length) return false
                when (s[i]) {
                    ',' -> i++ // another member must follow (no trailing comma)
                    '}' -> {
                        i++
                        return true
                    }
                    else -> return false
                }
            }
        }

        private fun parseArray(depth: Int): Boolean {
            if (depth + 1 > MAX_DEPTH) return false
            i++ // consume '['
            skipWhitespace()
            if (i < s.length && s[i] == ']') {
                i++
                return true
            }
            while (true) {
                if (!parseValue(depth + 1)) return false
                skipWhitespace()
                if (i >= s.length) return false
                when (s[i]) {
                    ',' -> i++ // another element must follow (no trailing comma)
                    ']' -> {
                        i++
                        return true
                    }
                    else -> return false
                }
            }
        }

        private fun parseString(): Boolean {
            // Precondition: s[i] == '"'.
            i++ // consume opening quote
            while (i < s.length) {
                val c = s[i]
                when {
                    c == '"' -> {
                        i++
                        return true
                    }
                    c == '\\' -> {
                        i++
                        if (i >= s.length) return false
                        when (s[i]) {
                            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> i++
                            'u' -> {
                                i++
                                if (i + 4 > s.length) return false
                                for (k in 0 until 4) {
                                    if (!isHexDigit(s[i + k])) return false
                                }
                                i += 4
                            }
                            else -> return false
                        }
                    }
                    c.code < 0x20 -> return false // raw control character
                    else -> i++
                }
            }
            return false // unterminated string
        }

        private fun parseLiteral(literal: String): Boolean {
            if (i + literal.length > s.length) return false
            for (k in literal.indices) {
                if (s[i + k] != literal[k]) return false
            }
            i += literal.length
            return true
        }

        private fun parseNumber(): Boolean {
            val start = i
            if (i < s.length && s[i] == '-') i++

            // Integer part: a single '0', or a non-zero digit run.
            if (i >= s.length) return false
            when {
                s[i] == '0' -> i++ // leading zero must stand alone
                s[i] in '1'..'9' -> {
                    i++
                    while (i < s.length && s[i] in '0'..'9') i++
                }
                else -> return false
            }

            // Optional fraction.
            if (i < s.length && s[i] == '.') {
                i++
                if (i >= s.length || s[i] !in '0'..'9') return false
                while (i < s.length && s[i] in '0'..'9') i++
            }

            // Optional exponent.
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i >= s.length || s[i] !in '0'..'9') return false
                while (i < s.length && s[i] in '0'..'9') i++
            }

            return i > start
        }

        private fun skipWhitespace() {
            while (i < s.length) {
                val c = s[i]
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++ else break
            }
        }

        private fun isHexDigit(c: Char): Boolean {
            return c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
        }
    }
}
