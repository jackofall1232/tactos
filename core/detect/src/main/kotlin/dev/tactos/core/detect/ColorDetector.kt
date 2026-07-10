package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is a color literal, case-insensitively.
 *
 * Two forms are accepted:
 *  - hex: `#` followed by exactly 3, 4, 6, or 8 hex digits;
 *  - functional: `rgb(r,g,b)` or `rgba(r,g,b,a)`, where `r`, `g`, `b` are
 *    integers 0–255 (no leading `+`, leading zeros allowed) and `a` is a
 *    decimal in `0..1` (`0`, `1`, `0.x`, `.x`, `1.0`). Arbitrary spaces are
 *    allowed around the parentheses, commas, and values; only commas separate
 *    components.
 */
object ColorDetector : TypeDetector {
    override val type: ClipType = ClipType.COLOR

    private val ALPHA = Regex("^(\\d+(\\.\\d+)?|\\.\\d+)$")

    override fun matches(text: String): Boolean {
        return isHexColor(text) || isFunctionalColor(text)
    }

    private fun isHexColor(s: String): Boolean {
        if (!s.startsWith('#')) return false
        val hex = s.substring(1)
        if (hex.length != 3 && hex.length != 4 && hex.length != 6 && hex.length != 8) return false
        return hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun isFunctionalColor(s: String): Boolean {
        val lower = s.lowercase()
        val hasAlpha: Boolean
        val prefixLength: Int
        when {
            lower.startsWith("rgba") -> {
                hasAlpha = true
                prefixLength = 4
            }
            lower.startsWith("rgb") -> {
                hasAlpha = false
                prefixLength = 3
            }
            else -> return false
        }

        // The full string must end at the closing paren — outer whitespace is
        // the caller's problem (ContentDetector trims). Spaces are lenient
        // only inside the functional syntax.
        if (!s.endsWith(')')) return false
        val body = s.substring(prefixLength, s.length - 1).trimStart()
        if (!body.startsWith('(')) return false
        val inner = body.substring(1)

        val components = inner.split(',').map { it.trim() }
        val expected = if (hasAlpha) 4 else 3
        if (components.size != expected) return false

        for (i in 0 until 3) {
            if (!isByte(components[i])) return false
        }
        if (hasAlpha && !isAlpha(components[3])) return false
        return true
    }

    private fun isByte(v: String): Boolean {
        if (v.isEmpty()) return false
        if (!v.all { it in '0'..'9' }) return false // rejects signs and non-digits
        val n = v.toIntOrNull() ?: return false
        return n in 0..255
    }

    private fun isAlpha(v: String): Boolean {
        if (!ALPHA.matches(v)) return false
        val d = v.toDoubleOrNull() ?: return false
        return d in 0.0..1.0
    }
}
