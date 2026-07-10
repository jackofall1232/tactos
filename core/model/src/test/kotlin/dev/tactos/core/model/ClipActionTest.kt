package dev.tactos.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipActionTest {

    private fun item(type: ClipType) = ClipItem(text = "t", type = type, createdAt = 0L)

    @Test
    fun `applies only to declared types`() {
        val action = ClipAction("m.qr", "Generate QR", "m", setOf(ClipType.URL))
        assertTrue(action.appliesTo(item(ClipType.URL)))
        assertFalse(action.appliesTo(item(ClipType.JSON)))
        assertFalse(action.appliesTo(item(ClipType.TEXT)))
    }

    @Test
    fun `multi-type action applies to each declared type`() {
        val action = ClipAction("m.copy", "Copy", "m", ClipType.entries.toSet())
        for (type in ClipType.entries) {
            assertTrue(action.appliesTo(item(type)), "expected to apply to $type")
        }
    }

    @Test
    fun `rejects blank id, blank label, and empty type set`() {
        assertFailsWith<IllegalArgumentException> { ClipAction(" ", "L", "m", setOf(ClipType.URL)) }
        assertFailsWith<IllegalArgumentException> { ClipAction("m.a", "", "m", setOf(ClipType.URL)) }
        assertFailsWith<IllegalArgumentException> { ClipAction("m.a", "L", "m", emptySet()) }
    }

    @Test
    fun `descriptor equality is structural`() {
        assertEquals(
            ClipAction("m.a", "L", "m", setOf(ClipType.URL)),
            ClipAction("m.a", "L", "m", setOf(ClipType.URL)),
        )
    }
}
