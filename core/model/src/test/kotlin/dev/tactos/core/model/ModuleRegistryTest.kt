package dev.tactos.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private class FakeModule(
    override val id: String,
    override val order: Int = ToolboxModule.DEFAULT_ORDER,
    private val actions: List<ClipAction> = emptyList(),
) : ToolboxModule {
    override val title: String = id
    override val description: String = "fake $id"
    override fun clipActions(): List<ClipAction> = actions
}

class ModuleRegistryTest {

    @Test
    fun `modules are sorted by order then id`() {
        val registry = ModuleRegistry(
            listOf(FakeModule("zeta", order = 1), FakeModule("beta"), FakeModule("alpha")),
        )
        assertEquals(listOf("zeta", "alpha", "beta"), registry.modules.map { it.id })
    }

    @Test
    fun `byId resolves registered modules and misses gracefully`() {
        val registry = ModuleRegistry(listOf(FakeModule("clipboard")))
        assertEquals("clipboard", registry.byId("clipboard")?.id)
        assertNull(registry.byId("missing"))
    }

    @Test
    fun `duplicate module ids fail fast`() {
        assertFailsWith<IllegalArgumentException> {
            ModuleRegistry(listOf(FakeModule("dup"), FakeModule("dup")))
        }
    }

    @Test
    fun `duplicate action ids across modules fail fast`() {
        val a = FakeModule("a", actions = listOf(ClipAction("shared.id", "A", "a", setOf(ClipType.URL))))
        val b = FakeModule("b", actions = listOf(ClipAction("shared.id", "B", "b", setOf(ClipType.JSON))))
        assertFailsWith<IllegalArgumentException> { ModuleRegistry(listOf(a, b)) }
    }

    @Test
    fun `actionsFor aggregates in module order and filters by type`() {
        val urlItem = ClipItem(text = "https://example.com", type = ClipType.URL, createdAt = 0L)
        val first = FakeModule(
            "first",
            order = 1,
            actions = listOf(
                ClipAction("first.open", "Open", "first", setOf(ClipType.URL)),
                ClipAction("first.beautify", "Beautify", "first", setOf(ClipType.JSON)),
            ),
        )
        val second = FakeModule(
            "second",
            order = 2,
            actions = listOf(ClipAction("second.qr", "QR", "second", setOf(ClipType.URL))),
        )
        val registry = ModuleRegistry(listOf(second, first))
        assertEquals(listOf("first.open", "second.qr"), registry.actionsFor(urlItem).map { it.id })
    }

    @Test
    fun `empty registry yields no modules and no actions`() {
        val registry = ModuleRegistry(emptyList())
        assertEquals(emptyList(), registry.modules)
        val item = ClipItem(text = "x", type = ClipType.TEXT, createdAt = 0L)
        assertEquals(emptyList(), registry.actionsFor(item))
    }
}
