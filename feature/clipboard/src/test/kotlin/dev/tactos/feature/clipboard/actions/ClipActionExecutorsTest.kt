package dev.tactos.feature.clipboard.actions

import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import dev.tactos.feature.clipboard.ClipboardToolbox
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipActionExecutorsTest {

    private fun clip(text: String, type: ClipType) =
        ClipItem(text = text, type = type, createdAt = 1_000L)

    private fun effectOf(actionId: String, item: ClipItem): ActionEffect {
        val executor = assertNotNull(ClipActionExecutors.forId(actionId), actionId)
        return executor(item)
    }

    // --- registry totality (the declarative-actions constraint) ---

    @Test
    fun `every declared action has an executor`() {
        for (action in ClipboardToolbox.clipActions()) {
            assertNotNull(
                ClipActionExecutors.forId(action.id),
                "descriptor ${action.id} has no executor",
            )
        }
    }

    @Test
    fun `unknown action id has no executor`() {
        assertNull(ClipActionExecutors.forId("clipboard.does-not-exist"))
    }

    // --- universal actions ---

    @Test
    fun `copy carries the clip text`() {
        val effect = effectOf(ClipboardToolbox.ACTION_COPY, clip("hello", ClipType.TEXT))
        assertEquals(ActionEffect.CopyText("hello"), effect)
    }

    @Test
    fun `share carries the clip text`() {
        val effect = effectOf(ClipboardToolbox.ACTION_SHARE, clip("hello", ClipType.TEXT))
        assertEquals(ActionEffect.ShareText("hello"), effect)
    }

    @Test
    fun `pin toggles`() {
        val effect = effectOf(ClipboardToolbox.ACTION_PIN, clip("hello", ClipType.TEXT))
        assertEquals(ActionEffect.TogglePin, effect)
    }

    // --- URL actions ---

    @Test
    fun `url open trims the text`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_URL_OPEN,
            clip(" https://tactos.dev ", ClipType.URL),
        )
        assertEquals(ActionEffect.OpenUrl("https://tactos.dev"), effect)
    }

    @Test
    fun `url qr trims the text`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_URL_QR,
            clip(" https://tactos.dev ", ClipType.URL),
        )
        assertEquals(ActionEffect.ShowQr("https://tactos.dev"), effect)
    }

    // --- color actions ---

    @Test
    fun `color convert parses the clip`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_COLOR_CONVERT,
            clip("#00696D", ClipType.COLOR),
        )
        val show = assertIs<ActionEffect.ShowColor>(effect)
        assertEquals(0x00, show.color.red)
        assertEquals(0x69, show.color.green)
        assertEquals(0x6D, show.color.blue)
    }

    @Test
    fun `color convert on unparseable text is an error result`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_COLOR_CONVERT,
            clip("not a color", ClipType.COLOR),
        )
        val result = assertIs<ActionEffect.ShowTextResult>(effect)
        assertTrue(result.isError)
        assertFalse(result.savable)
    }

    // --- JSON actions ---

    @Test
    fun `json validate reports the verdict and is not savable`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_JSON_VALIDATE,
            clip("""{"a":1}""", ClipType.JSON),
        )
        val result = assertIs<ActionEffect.ShowTextResult>(effect)
        assertEquals("Valid JSON — object with 1 entry", result.body)
        assertFalse(result.isError)
        assertFalse(result.savable)
    }

    @Test
    fun `json beautify is savable and indented`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_JSON_BEAUTIFY,
            clip("""{"a":1,"b":[2,3]}""", ClipType.JSON),
        )
        val result = assertIs<ActionEffect.ShowTextResult>(effect)
        assertTrue(result.savable)
        assertFalse(result.isError)
        assertTrue("\n  \"a\": 1" in result.body, result.body)
    }

    @Test
    fun `json minify is savable and compact`() {
        val effect = effectOf(
            ClipboardToolbox.ACTION_JSON_MINIFY,
            clip("{ \"a\" : 1 }", ClipType.JSON),
        )
        val result = assertIs<ActionEffect.ShowTextResult>(effect)
        assertTrue(result.savable)
        assertEquals("""{"a":1}""", result.body)
    }

    @Test
    fun `json tools surface parse failures as errors`() {
        for (actionId in listOf(
            ClipboardToolbox.ACTION_JSON_VALIDATE,
            ClipboardToolbox.ACTION_JSON_BEAUTIFY,
            ClipboardToolbox.ACTION_JSON_MINIFY,
        )) {
            val effect = effectOf(actionId, clip("""{"a":}""", ClipType.JSON))
            val result = assertIs<ActionEffect.ShowTextResult>(effect, actionId)
            assertTrue(result.isError, actionId)
            assertFalse(result.savable, actionId)
            assertTrue(result.body.isNotBlank(), actionId)
        }
    }
}
