package dev.tactos.feature.clipboard

import dev.tactos.core.model.ClipType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipboardToolboxTest {

    @Test
    fun `action ids are unique`() {
        val ids = ClipboardToolbox.clipActions().map { it.id }
        assertEquals(ids.size, ids.distinct().size, "duplicate action ids: $ids")
    }

    @Test
    fun `stable ids never change`() {
        // These ids are public API for the future plugin SDK and the V2
        // intent registry — changing any of them is a breaking change.
        val expected = setOf(
            "clipboard.copy",
            "clipboard.share",
            "clipboard.pin",
            "clipboard.url.open",
            "clipboard.url.qr",
            "clipboard.color.convert",
            "clipboard.json.validate",
            "clipboard.json.beautify",
            "clipboard.json.minify",
        )
        assertEquals(expected, ClipboardToolbox.clipActions().map { it.id }.toSet())
    }

    @Test
    fun `universal actions apply to every clip type`() {
        val actions = ClipboardToolbox.clipActions().associateBy { it.id }
        for (id in ClipboardToolbox.universalActionIds) {
            assertEquals(
                ClipType.entries.toSet(),
                actions.getValue(id).appliesTo,
                "universal action $id must apply to all types",
            )
        }
    }

    @Test
    fun `type-specific actions are scoped to their type`() {
        val actions = ClipboardToolbox.clipActions().associateBy { it.id }
        assertEquals(setOf(ClipType.URL), actions.getValue(ClipboardToolbox.ACTION_URL_OPEN).appliesTo)
        assertEquals(setOf(ClipType.URL), actions.getValue(ClipboardToolbox.ACTION_URL_QR).appliesTo)
        assertEquals(
            setOf(ClipType.COLOR),
            actions.getValue(ClipboardToolbox.ACTION_COLOR_CONVERT).appliesTo,
        )
        for (id in listOf(
            ClipboardToolbox.ACTION_JSON_VALIDATE,
            ClipboardToolbox.ACTION_JSON_BEAUTIFY,
            ClipboardToolbox.ACTION_JSON_MINIFY,
        )) {
            assertEquals(setOf(ClipType.JSON), actions.getValue(id).appliesTo, id)
        }
    }

    @Test
    fun `every action is namespaced by the module id`() {
        for (action in ClipboardToolbox.clipActions()) {
            assertTrue(
                action.id.startsWith("${ClipboardToolbox.id}."),
                "${action.id} is not namespaced",
            )
            assertEquals(ClipboardToolbox.id, action.moduleId)
        }
    }
}
