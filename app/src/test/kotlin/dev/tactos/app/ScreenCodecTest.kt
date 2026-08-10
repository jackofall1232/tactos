package dev.tactos.app

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

/**
 * The screen codec backs rememberSaveable: every screen must roundtrip, and
 * unknown/stale input must decode to Home rather than crash or strand.
 */
class ScreenCodecTest {

    @Test
    fun `every screen roundtrips through the codec`() {
        val screens = listOf(
            Screen.Home,
            Screen.Settings,
            Screen.Disclosure,
            Screen.Module("clipboard"),
            Screen.Module("images"),
        )
        for (screen in screens) {
            assertEquals(screen, Screen.decode(Screen.encode(screen)), "roundtrip $screen")
        }
    }

    @Test
    fun `unknown or stale input decodes to Home`() {
        assertEquals(Screen.Home, Screen.decode(""))
        assertEquals(Screen.Home, Screen.decode("garbage"))
        assertEquals(Screen.Home, Screen.decode("HOME"))
    }

    @Test
    fun `module id survives colons in the id`() {
        val screen = Screen.Module("a:b:c")
        assertEquals(screen, Screen.decode(Screen.encode(screen)))
    }

    @Test
    fun `back targets mirror the up-arrow hierarchy`() {
        assertNull(Screen.backTarget(Screen.Home))
        assertEquals(Screen.Home, Screen.backTarget(Screen.Settings))
        assertEquals(Screen.Settings, Screen.backTarget(Screen.Disclosure))
        assertEquals(Screen.Home, Screen.backTarget(Screen.Module("clipboard")))
    }
}
