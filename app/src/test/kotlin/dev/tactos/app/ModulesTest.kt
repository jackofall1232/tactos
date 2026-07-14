package dev.tactos.app

import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import dev.tactos.feature.clipboard.ClipboardToolbox
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [appModuleRegistry] is the app's actual wiring of the
 * [dev.tactos.core.model.ModuleRegistry] contract — the generic contract
 * behavior (sorting, duplicate detection, action aggregation) is already
 * exhaustively covered by core/model's ModuleRegistryTest against fakes.
 * These tests only check the app-specific wiring: that construction
 * succeeds and that the real ClipboardToolbox is actually registered and
 * reachable through it.
 */
class ModulesTest {

    @Test
    fun `appModuleRegistry constructs without throwing`() {
        appModuleRegistry()
    }

    @Test
    fun `ClipboardToolbox is registered under its stable id`() {
        val registry = appModuleRegistry()
        assertEquals(ClipboardToolbox, registry.byId("clipboard"))
    }

    @Test
    fun `registry exposes exactly the clipboard toolbox on the home grid`() {
        val registry = appModuleRegistry()
        assertEquals(listOf("clipboard"), registry.modules.map { it.id })
    }

    @Test
    fun `registry resolves ClipboardToolbox's URL actions for a URL clip`() {
        val registry = appModuleRegistry()
        val urlItem = ClipItem(text = "https://tactos.dev", type = ClipType.URL, createdAt = 0L)

        val actionIds = registry.actionsFor(urlItem).map { it.id }

        assertTrue(ClipboardToolbox.ACTION_URL_OPEN in actionIds)
        assertTrue(ClipboardToolbox.ACTION_URL_QR in actionIds)
        assertTrue(ClipboardToolbox.ACTION_COPY in actionIds)
        assertTrue(ClipboardToolbox.ACTION_JSON_VALIDATE !in actionIds)
    }
}
