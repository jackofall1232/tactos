package dev.tactos.core.actions

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Table-driven tests for [ColorValue]: HEX/RGB/HSL/HSV conversions, the
 * ColorDetector-mirroring [ColorValue.parse] grammar, and adversarial rejects.
 */
class ColorValueTest {

    @Test
    fun `constructor rejects out-of-range components`() {
        assertFailsWith<IllegalArgumentException> { ColorValue(-1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ColorValue(256, 0, 0) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, -1, 0) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, 256, 0) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, 0, -1) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, 0, 256) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, 0, 0, -0.01f) }
        assertFailsWith<IllegalArgumentException> { ColorValue(0, 0, 0, 1.01f) }
    }

    @Test
    fun `known colors convert to expected hsl and hsv strings`() {
        val cases = listOf(
            Triple("#FF0000", "hsl(0, 100%, 50%)", "hsv(0, 100%, 100%)"),
            Triple("#00FF00", "hsl(120, 100%, 50%)", "hsv(120, 100%, 100%)"),
            Triple("#0000FF", "hsl(240, 100%, 50%)", "hsv(240, 100%, 100%)"),
            Triple("#00696D", "hsl(182, 100%, 21%)", "hsv(182, 100%, 43%)"),
            Triple("#808080", "hsl(0, 0%, 50%)", "hsv(0, 0%, 50%)"),
            Triple("#FFFFFF", "hsl(0, 0%, 100%)", "hsv(0, 0%, 100%)"),
            Triple("#000000", "hsl(0, 0%, 0%)", "hsv(0, 0%, 0%)"),
            Triple("#FFFF00", "hsl(60, 100%, 50%)", "hsv(60, 100%, 100%)"),
            Triple("#00FFFF", "hsl(180, 100%, 50%)", "hsv(180, 100%, 100%)"),
            Triple("#FF00FF", "hsl(300, 100%, 50%)", "hsv(300, 100%, 100%)"),
        )
        for ((hex, hsl, hsv) in cases) {
            val color = assertNotNull(ColorValue.parse(hex), "expected parseable: <$hex>")
            assertEquals(hsl, color.toHslString(), "hsl of <$hex>")
            assertEquals(hsv, color.toHsvString(), "hsv of <$hex>")
        }
    }

    @Test
    fun `toHexString formats opaque and translucent colors`() {
        assertEquals("#FF0000", ColorValue(255, 0, 0).toHexString())
        assertEquals("#00696D", ColorValue(0, 105, 109).toHexString())
        assertEquals("#000000", ColorValue(0, 0, 0).toHexString())
        assertEquals("#FFFFFF", ColorValue(255, 255, 255).toHexString())
        // alpha byte = (alpha * 255) rounded: 0.5 * 255 = 127.5 -> 128 = 0x80.
        assertEquals("#FF000080", ColorValue(255, 0, 0, 0.5f).toHexString())
        assertEquals("#00696D00", ColorValue(0, 105, 109, 0f).toHexString())
        assertEquals("#01020340", ColorValue(1, 2, 3, 0.25f).toHexString())
    }

    @Test
    fun `toRgbString trims alpha to at most two decimals`() {
        assertEquals("rgb(0, 105, 109)", ColorValue(0, 105, 109).toRgbString())
        assertEquals("rgb(255, 255, 255)", ColorValue(255, 255, 255).toRgbString())
        assertEquals("rgba(0, 105, 109, 0.5)", ColorValue(0, 105, 109, 0.5f).toRgbString())
        assertEquals("rgba(0, 0, 0, 0.25)", ColorValue(0, 0, 0, 0.25f).toRgbString())
        assertEquals("rgba(1, 2, 3, 0)", ColorValue(1, 2, 3, 0f).toRgbString())
        assertEquals("rgba(0, 0, 0, 0.05)", ColorValue(0, 0, 0, 0.05f).toRgbString())
        // 128/255 = 0.50196... rounds to two decimals then trims to 0.5.
        assertEquals("rgba(0, 0, 0, 0.5)", ColorValue(0, 0, 0, 128 / 255f).toRgbString())
        assertEquals("rgba(0, 0, 0, 0.7)", ColorValue(0, 0, 0, 0.7f).toRgbString())
    }

    @Test
    fun `hsl gains alpha component but hsv never does`() {
        val translucent = ColorValue(0, 105, 109, 0.5f)
        assertEquals("hsla(182, 100%, 21%, 0.5)", translucent.toHslString())
        assertEquals("hsv(182, 100%, 43%)", translucent.toHsvString())
        val opaque = ColorValue(0, 105, 109)
        assertEquals("hsl(182, 100%, 21%)", opaque.toHslString())
        assertEquals("hsv(182, 100%, 43%)", opaque.toHsvString())
    }

    @Test
    fun `parse accepts hex forms including short expansion`() {
        val cases = mapOf(
            "#abc" to ColorValue(0xAA, 0xBB, 0xCC),
            "#ABC" to ColorValue(0xAA, 0xBB, 0xCC),
            "#ABCD" to ColorValue(0xAA, 0xBB, 0xCC, 0xDD / 255f),
            "#aBcD" to ColorValue(0xAA, 0xBB, 0xCC, 0xDD / 255f),
            "#00696d" to ColorValue(0, 105, 109),
            "#00696D" to ColorValue(0, 105, 109),
            "#12345678" to ColorValue(0x12, 0x34, 0x56, 0x78 / 255f),
            "#ffffffff" to ColorValue(255, 255, 255, 1f),
            "#000" to ColorValue(0, 0, 0),
            "#fff" to ColorValue(255, 255, 255),
        )
        for ((input, expected) in cases) {
            assertEquals(expected, ColorValue.parse(input), "parse of <$input>")
        }
    }

    @Test
    fun `parse accepts functional rgb and rgba forms`() {
        val cases = mapOf(
            "rgb(0,0,0)" to ColorValue(0, 0, 0),
            "rgb(255, 255, 255)" to ColorValue(255, 255, 255),
            "rgb( 0 , 105 , 109 )" to ColorValue(0, 105, 109),
            "RGB(1,2,3)" to ColorValue(1, 2, 3),
            "rgb(00,01,02)" to ColorValue(0, 1, 2),
            "rgb(010,020,030)" to ColorValue(10, 20, 30),
            "rgba(0,0,0,0)" to ColorValue(0, 0, 0, 0f),
            "rgba(0,0,0,1)" to ColorValue(0, 0, 0),
            "rgba(0,0,0,1.0)" to ColorValue(0, 0, 0),
            "rgba(0,0,0,0.5)" to ColorValue(0, 0, 0, 0.5f),
            "rgba(0,0,0,.5)" to ColorValue(0, 0, 0, 0.5f),
            "RGBA(0,0,0,0.5)" to ColorValue(0, 0, 0, 0.5f),
            "rgba( 255 , 128 , 0 , 0.25 )" to ColorValue(255, 128, 0, 0.25f),
        )
        for ((input, expected) in cases) {
            assertEquals(expected, ColorValue.parse(input), "parse of <$input>")
        }
    }

    @Test
    fun `parse trims outer whitespace`() {
        assertEquals(ColorValue(255, 255, 255), ColorValue.parse("  #fff  "))
        assertEquals(ColorValue(0, 0, 0), ColorValue.parse("\trgb(0,0,0)\n"))
        assertEquals(ColorValue(0, 105, 109), ColorValue.parse(" #00696D "))
    }

    @Test
    fun `parse rejects invalid inputs`() {
        val rejected = listOf(
            // hex
            "#ggg",
            "#12345",
            "#",
            "#ff",
            "#fffffff",
            "#fffffffff",
            "# fff",
            "fff",
            "#ff88gg",
            // functional
            "rgb(256,0,0)",
            "rgb(0,0,256)",
            "rgb(-1,0,0)",
            "rgb(+1,0,0)",
            "rgb(0,0)",
            "rgb(0,0,0,0)",
            "rgba(0,0,0)",
            "rgba(0,0,0,1.5)",
            "rgba(0,0,0,-0.5)",
            "rgba(0,0,0,0.)",
            "rgba(0,0,0,)",
            "rgb(0.5,0,0)",
            "rgb(0 0 0)",
            "rgb 0,0,0",
            "rgb(0,0,0",
            "rgb(0,0,0)x",
            "rgb()",
            "rgb(,0,0)",
            "hsl(0, 0%, 0%)",
            // not colors at all
            "red",
            "",
            "  ",
            "color",
        )
        for (input in rejected) {
            assertNull(ColorValue.parse(input), "expected parse to reject: <$input>")
        }
    }

    @Test
    fun `parse of toHexString round-trips exactly for byte-aligned alphas`() {
        val colors = listOf(
            ColorValue(0, 0, 0),
            ColorValue(255, 255, 255),
            ColorValue(0, 105, 109),
            ColorValue(170, 187, 204, 221 / 255f),
            ColorValue(1, 2, 3, 128 / 255f),
            ColorValue(255, 0, 128, 0f),
        )
        for (color in colors) {
            assertEquals(color, ColorValue.parse(color.toHexString()), "round-trip of $color")
        }
    }

    @Test
    fun `functional parse round-trips through hex within one alpha step`() {
        val parsed = assertNotNull(ColorValue.parse("rgba(10, 20, 30, 0.5)"))
        val reparsed = assertNotNull(ColorValue.parse(parsed.toHexString()))
        assertEquals(parsed.red, reparsed.red)
        assertEquals(parsed.green, reparsed.green)
        assertEquals(parsed.blue, reparsed.blue)
        assertTrue(
            abs(parsed.alpha - reparsed.alpha) <= 1 / 255f,
            "alpha drifted more than one byte step: ${parsed.alpha} vs ${reparsed.alpha}",
        )
    }
}
