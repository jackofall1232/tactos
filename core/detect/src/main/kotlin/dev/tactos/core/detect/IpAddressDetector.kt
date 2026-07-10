package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Detects whether the full string is an IPv4 or IPv6 address, with no port,
 * CIDR suffix, brackets, or zone index.
 *
 * IPv4 is exactly four dot-separated decimal octets 0–255 with leading zeros
 * forbidden (`0` alone is allowed, `01` is not). IPv6 accepts the standard
 * textual forms: eight colon-separated groups of 1–4 hex digits, at most one
 * `::` compression (which may stand for one or more zero groups, including at
 * the start or end), and an optional embedded IPv4 dotted-quad as the final
 * 32 bits (validated by the IPv4 rules above).
 */
object IpAddressDetector : TypeDetector {
    override val type: ClipType = ClipType.IP_ADDRESS

    override fun matches(text: String): Boolean {
        return isIpv4(text) || isIpv6(text)
    }

    private fun isIpv4(s: String): Boolean {
        val parts = s.split('.')
        if (parts.size != 4) return false
        return parts.all { isOctet(it) }
    }

    private fun isOctet(p: String): Boolean {
        if (p.isEmpty() || p.length > 3) return false
        if (!p.all { it in '0'..'9' }) return false
        if (p.length > 1 && p[0] == '0') return false // leading zero forbidden
        val n = p.toIntOrNull() ?: return false
        return n in 0..255
    }

    private fun isIpv6(s: String): Boolean {
        if (s.isEmpty()) return false
        if (s.contains('%')) return false // zone indexes are rejected
        if (s.contains(":::")) return false

        val first = s.indexOf("::")
        if (first >= 0) {
            // Reject a second "::" occurrence.
            if (s.indexOf("::", first + 1) >= 0) return false

            val left = s.substring(0, first)
            val right = s.substring(first + 2)
            val leftGroups = if (left.isEmpty()) emptyList() else left.split(':')
            val rightGroups = if (right.isEmpty()) emptyList() else right.split(':')

            // An embedded IPv4 tail can only appear at the very end (in the
            // right half); it is never valid before the "::" compression.
            val leftValues = countValues(leftGroups, allowV4Tail = false)
            if (leftValues < 0) return false
            val rightValues = countValues(rightGroups, allowV4Tail = true)
            if (rightValues < 0) return false

            // "::" fills at least one zero group, so the explicit groups must
            // leave room: their combined value count is at most 7.
            return leftValues + rightValues <= 7
        }

        // No compression: exactly eight 16-bit values, where an embedded IPv4
        // tail counts as the final two.
        val groups = s.split(':')
        val values = countValues(groups, allowV4Tail = true)
        return values == 8
    }

    /**
     * Validates a run of colon-separated IPv6 tokens and returns the number of
     * 16-bit values they represent (hex group = 1, embedded IPv4 tail = 2), or
     * -1 if any token is malformed. An IPv4 tail is only honored as the final
     * token and only when [allowV4Tail] is true.
     */
    private fun countValues(tokens: List<String>, allowV4Tail: Boolean): Int {
        var count = 0
        for ((index, token) in tokens.withIndex()) {
            val isLast = index == tokens.size - 1
            if (allowV4Tail && isLast && token.contains('.')) {
                if (!isIpv4(token)) return -1
                count += 2
            } else {
                if (!isHexGroup(token)) return -1
                count += 1
            }
        }
        return count
    }

    private fun isHexGroup(t: String): Boolean {
        if (t.isEmpty() || t.length > 4) return false
        return t.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }
}
