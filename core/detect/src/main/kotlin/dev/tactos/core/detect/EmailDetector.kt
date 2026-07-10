package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is a single email address.
 *
 * The string must match
 * `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` and additionally: the
 * local part may not start or end with `.`, the string may contain no `..`
 * sequence, and no domain label may start or end with `-`.
 */
object EmailDetector : TypeDetector {
    override val type: ClipType = ClipType.EMAIL

    private val PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    override fun matches(text: String): Boolean {
        if (!PATTERN.matches(text)) return false
        if (text.contains("..")) return false

        val at = text.indexOf('@')
        val local = text.substring(0, at)
        val domain = text.substring(at + 1)

        if (local.startsWith('.') || local.endsWith('.')) return false

        for (label in domain.split('.')) {
            if (label.isEmpty()) return false
            if (label.first() == '-' || label.last() == '-') return false
        }
        return true
    }
}
