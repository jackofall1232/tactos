package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [ColorDetector]: hex (3/4/6/8 digits) and
 * functional rgb()/rgba() with the spec's integer/alpha/whitespace rules.
 */
class ColorDetectorTest {

    @Test
    fun `declares COLOR type`() {
        assertEquals(ClipType.COLOR, ColorDetector.type)
    }

    @Test
    fun `accepts valid colors`() {
        val accepted = listOf(
            // hex
            "#fff",                            // 3
            "#ffff",                           // 4
            "#ffffff",                         // 6
            "#ffffffff",                       // 8
            "#FFF",
            "#AbCdEf",
            "#000",
            "#12345678",
            "#abc",
            "#1a2b3c",
            "#ff8800",
            "#abcd",
            "#ff88",
            // functional
            "rgb(0,0,0)",
            "rgb(255,255,255)",
            "rgb(128, 64, 32)",
            "rgb( 0 , 0 , 0 )",                // arbitrary spaces
            "rgba(0,0,0,0)",                   // alpha 0
            "rgba(0,0,0,1)",                   // alpha 1
            "rgba(0,0,0,0.5)",
            "rgba(0,0,0,.5)",                  // .x alpha form
            "rgba(0,0,0,1.0)",                 // 1.0 alpha form
            "RGB(255,255,255)",                // case-insensitive
            "RGBA(0,0,0,0.5)",
            "rgb(00,01,02)",                   // leading zeros allowed on channels
            "rgb(010,020,030)",
            "rgba( 255 , 128 , 0 , 0.25 )",
            "rgb(0, 128, 255)",
            "rgba(1,2,3,0)",
            "rgba(0,0,0,0.0)",
            "rgba(0,0,0,.0)",
        )
        for (case in accepted) {
            assertTrue(ColorDetector.matches(case), "expected COLOR to match: <$case>")
        }
    }

    @Test
    fun `rejects non-colors`() {
        val rejected = listOf(
            // hex
            "#ff",                             // 2 digits
            "#fffff",                          // 5
            "#fffffff",                        // 7
            "#fffffffff",                      // 9
            "#",                               // 0
            "#ggg",                            // non-hex
            "#gggggg",
            "fff",                             // missing '#'
            "#12",
            "#1234567",                        // 7
            "# fff",                           // space after '#'
            "#fff ",                           // trailing whitespace
            " #fff",                           // leading whitespace
            "#xyz",
            "#ff880",                          // 5
            "#ff88gg",                         // 6 with non-hex
            // functional
            "rgb(256,0,0)",                    // channel > 255
            "rgb(0,0,256)",
            "rgb(-1,0,0)",                     // negative channel
            "rgb(+1,0,0)",                     // leading '+' forbidden
            "rgba(0,0,0,1.1)",                 // alpha > 1
            "rgba(0,0,0,2)",
            "rgba(0,0,0,-0.5)",                // negative alpha
            "rgb(0,0)",                        // too few channels
            "rgb(0,0,0,0)",                    // four channels in rgb()
            "rgba(0,0,0)",                     // three channels in rgba()
            "rgb(0 0 0)",                      // space-separated (CSS4) not allowed
            "rgb(0,0,0",                       // missing close paren
            "rgb 0,0,0)",                      // missing open paren
            "rgb(0,0,0)x",                     // trailing garbage
            "rgb(0.5,0,0)",                    // non-integer channel
            "rgb(255,255,255) ",               // trailing whitespace
            "hsl(0,0,0)",                      // wrong function
            "rgb()",                           // empty
            "rgba(0,0,0,)",                    // empty alpha
            "rgb(,0,0)",                       // empty channel
            "rgba(0,0,0,0.)",                  // "0." is not a valid alpha form
            "rgb(0xff,0,0)",                   // hex channel
            "rgba(0,0,0,1.5)",
            "",
        )
        for (case in rejected) {
            assertFalse(ColorDetector.matches(case), "expected NON-COLOR: <$case>")
        }
    }
}
