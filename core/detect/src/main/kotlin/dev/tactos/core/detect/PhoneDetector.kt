package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is a phone number.
 *
 * After removing every space, `-`, `.`, `(`, and `)`, the remainder must be an
 * optional leading `+` followed by 7–15 digits and nothing else. In addition,
 * the original text must contain a `+` or at least one of the separator
 * characters — so a bare digit run such as `12345678`, with no `+` and no
 * separator, is deliberately not a phone number (it stays plain text).
 * Separators need not be balanced or positioned meaningfully.
 */
object PhoneDetector : TypeDetector {
    override val type: ClipType = ClipType.PHONE

    private val SEPARATORS = setOf(' ', '-', '.', '(', ')')

    override fun matches(text: String): Boolean {
        val stripped = buildString {
            for (c in text) {
                if (c !in SEPARATORS) append(c)
            }
        }
        if (stripped.isEmpty()) return false

        val digitsStart = if (stripped[0] == '+') 1 else 0
        val digits = stripped.substring(digitsStart)
        if (digits.isEmpty()) return false
        if (!digits.all { it in '0'..'9' }) return false
        if (digits.length < 7 || digits.length > 15) return false

        val hasPlus = text.contains('+')
        val hasSeparator = text.any { it in SEPARATORS }
        return hasPlus || hasSeparator
    }
}
