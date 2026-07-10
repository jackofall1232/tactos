package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is a single web URL.
 *
 * A match is either:
 *  - a scheme (`http://`, `https://`, `ftp://`, `ftps://`, case-insensitive)
 *    followed by a non-empty valid host, or
 *  - a string that starts with `www.` followed by a valid host remainder.
 *
 * Hosts are dot-joined labels of `[A-Za-z0-9-]` (no leading/trailing hyphen)
 * and must contain at least one dot, unless the host is `localhost` and a
 * scheme is present. An optional `:port` (1–65535) and an optional
 * `/path?query#fragment` may follow. Whitespace anywhere disqualifies the
 * string, and non-web schemes such as `mailto:` are not URLs here.
 */
object UrlDetector : TypeDetector {
    override val type: ClipType = ClipType.URL

    private val SCHEMES = listOf("http://", "https://", "ftp://", "ftps://")

    override fun matches(text: String): Boolean {
        if (text.isEmpty()) return false
        if (text.any { it.isWhitespace() }) return false

        val lower = text.lowercase()
        val scheme = SCHEMES.firstOrNull { lower.startsWith(it) }

        val hasScheme: Boolean
        val rest: String
        when {
            scheme != null -> {
                hasScheme = true
                rest = text.substring(scheme.length)
            }
            lower.startsWith("www.") -> {
                hasScheme = false
                rest = text
            }
            else -> return false
        }
        if (rest.isEmpty()) return false

        // Split the authority from any /path, ?query, or #fragment tail.
        var end = rest.length
        for (k in rest.indices) {
            val c = rest[k]
            if (c == '/' || c == '?' || c == '#') {
                end = k
                break
            }
        }
        val authority = rest.substring(0, end)
        // The path/query/fragment tail (rest.substring(end)) is already known to
        // be whitespace-free, which is the only constraint the spec places on it.
        if (authority.isEmpty()) return false

        val colon = authority.indexOf(':')
        val host: String
        if (colon >= 0) {
            host = authority.substring(0, colon)
            val portStr = authority.substring(colon + 1)
            if (portStr.contains(':')) return false
            if (!isValidPort(portStr)) return false
        } else {
            host = authority
        }
        return isValidHost(host, hasScheme)
    }

    private fun isValidHost(host: String, hasScheme: Boolean): Boolean {
        if (host.isEmpty()) return false
        if (hasScheme && host.equals("localhost", ignoreCase = true)) return true
        val labels = host.split('.')
        if (labels.size < 2) return false // must contain at least one dot
        return labels.all { isValidLabel(it) }
    }

    private fun isValidLabel(label: String): Boolean {
        if (label.isEmpty()) return false
        if (label.first() == '-' || label.last() == '-') return false
        return label.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '-' }
    }

    private fun isValidPort(port: String): Boolean {
        if (port.isEmpty()) return false
        if (!port.all { it in '0'..'9' }) return false
        val n = port.toIntOrNull() ?: return false
        return n in 1..65535
    }
}
