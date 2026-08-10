package dev.tactos.feature.images

import dev.tactos.core.model.ModuleRegistry
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ImagesToolboxTest {

    @Test
    fun `registers cleanly alongside a clipboard-like module`() {
        // The registry fails fast on duplicate module or action ids; images
        // must compose with any other module.
        val registry = ModuleRegistry(listOf(ImagesToolbox))
        assertEquals(ImagesToolbox, registry.byId("images"))
    }

    @Test
    fun `tool ids are unique and namespaced`() {
        val ids = ImagesToolbox.tools.map { it.id }
        assertEquals(ids, ids.distinct())
        assertTrue(ids.all { it.startsWith("images.") })
    }

    @Test
    fun `toolById resolves every declared tool and rejects unknowns`() {
        for (tool in ImagesToolbox.tools) {
            assertEquals(tool, ImagesToolbox.toolById(tool.id))
        }
        assertNull(ImagesToolbox.toolById("images.nope"))
    }

    @Test
    fun `v1 tools are all low risk because originals are never modified`() {
        assertTrue(ImagesToolbox.tools.all { it.risk == ImageTool.Risk.LOW })
    }

    @Test
    fun `no clip actions until ClipType IMAGE exists`() {
        assertTrue(ImagesToolbox.clipActions().isEmpty())
    }
}
