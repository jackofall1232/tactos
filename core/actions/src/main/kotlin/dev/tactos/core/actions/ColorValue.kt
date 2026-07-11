package dev.tactos.core.actions

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * An immutable RGBA color with integer channels in `0..255` and a float
 * [alpha] in `0..1`, providing the HEX ⇄ RGB ⇄ HSL/HSV conversions behind the
 * clipboard color actions.
 *
 * [parse] accepts exactly the grammar the `ColorDetector` in `core/detect`
 * classifies as a color, so every clip typed `COLOR` is guaranteed to parse.
 */
data class ColorValue(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float = 1f,
) {
    init {
        require(red in 0..255) { "red must be in 0..255, was $red" }
        require(green in 0..255) { "green must be in 0..255, was $green" }
        require(blue in 0..255) { "blue must be in 0..255, was $blue" }
        require(alpha in 0f..1f) { "alpha must be in 0..1, was $alpha" }
    }

    /**
     * Formats as `#RRGGBB` (uppercase hex), or `#RRGGBBAA` when [alpha] is
     * less than `1` — the alpha byte is `(alpha * 255)` rounded to nearest.
     */
    fun toHexString(): String {
        val rgb = "#${red.toHexByte()}${green.toHexByte()}${blue.toHexByte()}"
        return if (alpha < 1f) rgb + (alpha * 255).roundToInt().toHexByte() else rgb
    }

    /**
     * Formats as `rgb(r, g, b)`, or `rgba(r, g, b, a)` when [alpha] is less
     * than `1` — alpha rendered with at most two decimals, no trailing zeros.
     */
    fun toRgbString(): String {
        return if (alpha < 1f) {
            "rgba($red, $green, $blue, ${formatAlpha()})"
        } else {
            "rgb($red, $green, $blue)"
        }
    }

    /**
     * Formats as `hsl(h, s%, l%)` with the hue as the nearest integer degree
     * in `0..359` and saturation/lightness as nearest integer percents, or
     * `hsla(h, s%, l%, a)` when [alpha] is less than `1`.
     */
    fun toHslString(): String {
        val max = maxOf(r01, g01, b01)
        val min = minOf(r01, g01, b01)
        val delta = max - min
        val lightness = (max + min) / 2.0
        val saturation = if (delta == 0.0) 0.0 else delta / (1.0 - abs(2.0 * lightness - 1.0))
        val h = hueDegrees(max, delta)
        val s = (saturation * 100).roundToInt()
        val l = (lightness * 100).roundToInt()
        return if (alpha < 1f) "hsla($h, $s%, $l%, ${formatAlpha()})" else "hsl($h, $s%, $l%)"
    }

    /**
     * Formats as `hsv(h, s%, v%)` with the same rounding as [toHslString].
     * HSV output always stays three-component; [alpha] is never included.
     */
    fun toHsvString(): String {
        val max = maxOf(r01, g01, b01)
        val min = minOf(r01, g01, b01)
        val delta = max - min
        val saturation = if (max == 0.0) 0.0 else delta / max
        val h = hueDegrees(max, delta)
        val s = (saturation * 100).roundToInt()
        val v = (max * 100).roundToInt()
        return "hsv($h, $s%, $v%)"
    }

    private val r01: Double get() = red / 255.0
    private val g01: Double get() = green / 255.0
    private val b01: Double get() = blue / 255.0

    /** Hue as the nearest integer degree in `0..359`; `0` for achromatic. */
    private fun hueDegrees(max: Double, delta: Double): Int {
        if (delta == 0.0) return 0
        val segment = when (max) {
            r01 -> ((g01 - b01) / delta).mod(6.0)
            g01 -> (b01 - r01) / delta + 2.0
            else -> (r01 - g01) / delta + 4.0
        }
        return (segment * 60.0).roundToInt().mod(360)
    }

    /** Alpha with at most two decimals and no trailing zeros, e.g. `0.5`. */
    private fun formatAlpha(): String {
        val hundredths = (alpha * 100).roundToInt()
        return when {
            hundredths % 100 == 0 -> (hundredths / 100).toString()
            hundredths % 10 == 0 -> "${hundredths / 100}.${hundredths / 10 % 10}"
            else -> "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
        }
    }

    companion object {
        /** Same alpha grammar as ColorDetector: `0`, `1`, `0.x`, `.x`, `1.0`. */
        private val ALPHA = Regex("^(\\d+(\\.\\d+)?|\\.\\d+)$")

        /**
         * Parses [text] (trimmed first) as a color literal, mirroring the
         * grammar of `ColorDetector`: `#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA`
         * (case-insensitive hex, short forms expand each digit), or
         * `rgb(r, g, b)` / `rgba(r, g, b, a)` with integer channels `0..255`
         * (digits only, leading zeros allowed, no signs), a decimal alpha in
         * `0..1`, lenient inner whitespace, and a case-insensitive prefix.
         *
         * Returns `null` for anything else; never throws.
         */
        fun parse(text: String): ColorValue? {
            val s = text.trim()
            if (s.isEmpty()) return null
            return parseHex(s) ?: parseFunctional(s)
        }

        private fun parseHex(s: String): ColorValue? {
            if (!s.startsWith('#')) return null
            val hex = s.substring(1)
            if (hex.length != 3 && hex.length != 4 && hex.length != 6 && hex.length != 8) {
                return null
            }
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
            val expanded = if (hex.length <= 4) {
                hex.asSequence().joinToString("") { "$it$it" }
            } else {
                hex
            }
            return ColorValue(
                red = expanded.substring(0, 2).toInt(16),
                green = expanded.substring(2, 4).toInt(16),
                blue = expanded.substring(4, 6).toInt(16),
                alpha = if (expanded.length == 8) expanded.substring(6, 8).toInt(16) / 255f else 1f,
            )
        }

        private fun parseFunctional(s: String): ColorValue? {
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
                else -> return null
            }

            // The (trimmed) string must end at the closing paren; spaces are
            // lenient only inside the functional syntax — as in ColorDetector.
            if (!s.endsWith(')')) return null
            val body = s.substring(prefixLength, s.length - 1).trimStart()
            if (!body.startsWith('(')) return null
            val inner = body.substring(1)

            val components = inner.split(',').map { it.trim() }
            val expected = if (hasAlpha) 4 else 3
            if (components.size != expected) return null

            val red = parseByte(components[0]) ?: return null
            val green = parseByte(components[1]) ?: return null
            val blue = parseByte(components[2]) ?: return null
            val alpha = if (hasAlpha) parseAlpha(components[3]) ?: return null else 1f
            return ColorValue(red, green, blue, alpha)
        }

        private fun parseByte(v: String): Int? {
            if (v.isEmpty()) return null
            if (!v.all { it in '0'..'9' }) return null // rejects signs and non-digits
            val n = v.toIntOrNull() ?: return null
            return n.takeIf { it in 0..255 }
        }

        private fun parseAlpha(v: String): Float? {
            if (!ALPHA.matches(v)) return null
            val d = v.toDoubleOrNull() ?: return null
            return d.takeIf { it in 0.0..1.0 }?.toFloat()
        }

        private fun Int.toHexByte(): String = toString(16).uppercase().padStart(2, '0')
    }
}
