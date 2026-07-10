package dev.tactos.feature.clipboard

import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals

class TimelineFilterTest {

    private fun item(text: String, type: ClipType, category: String? = null) =
        ClipItem(text = text, type = type, createdAt = 0L, category = category)

    private val items = listOf(
        item("https://a.dev", ClipType.URL, category = "work"),
        item("#ff8800", ClipType.COLOR),
        item("note one", ClipType.TEXT, category = "work"),
        item("note two", ClipType.TEXT, category = "personal"),
    )

    @Test
    fun `no filter returns everything in order`() {
        assertEquals(items, items.applyFilter(TimelineFilter()))
    }

    @Test
    fun `type filter keeps only that type`() {
        assertEquals(
            listOf("note one", "note two"),
            items.applyFilter(TimelineFilter(type = ClipType.TEXT)).map { it.text },
        )
    }

    @Test
    fun `category filter keeps only that category`() {
        assertEquals(
            listOf("https://a.dev", "note one"),
            items.applyFilter(TimelineFilter(category = "work")).map { it.text },
        )
    }

    @Test
    fun `type and category filters combine`() {
        assertEquals(
            listOf("note one"),
            items.applyFilter(TimelineFilter(type = ClipType.TEXT, category = "work")).map { it.text },
        )
    }

    @Test
    fun `distinctCategories is sorted and skips null`() {
        assertEquals(listOf("personal", "work"), items.distinctCategories())
    }

    @Test
    fun `distinctTypes follows enum order`() {
        assertEquals(listOf(ClipType.URL, ClipType.COLOR, ClipType.TEXT), items.distinctTypes())
    }

    @Test
    fun `every clip type has an emoji and a label`() {
        for (type in ClipType.entries) {
            assertEquals(false, type.emoji.isBlank(), "emoji for $type")
            assertEquals(false, type.label().isBlank(), "label for $type")
        }
    }
}
