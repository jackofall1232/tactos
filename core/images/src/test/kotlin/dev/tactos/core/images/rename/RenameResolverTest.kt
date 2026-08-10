package dev.tactos.core.images.rename

import java.time.ZoneOffset
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class RenameResolverTest {

    // 2026-08-10T12:34:56Z
    private val now = 1_786_710_896_000L
    private fun resolver(seed: Long = 1L) =
        RenameResolver(nowMillis = now, zoneId = ZoneOffset.UTC, random = Random(seed))

    private val input = RenameInput(
        originalName = "IMG_0042",
        index = 4,
        takenAtMillis = null,
        width = 4000,
        height = 3000,
    )

    @Test
    fun `literal text passes through`() {
        assertEquals("holiday", resolver().resolve("holiday", input))
    }

    @Test
    fun `name, sequence, and dimensions substitute`() {
        assertEquals(
            "IMG_0042-5-4000x3000",
            resolver().resolve("{name}-{n}-{w}x{h}", input),
        )
    }

    @Test
    fun `sequence pads when asked`() {
        assertEquals("005", resolver().resolve("{n:3}", input))
        assertEquals("5", resolver().resolve("{n:0}", input))
        // Non-numeric pad arg falls back to no padding.
        assertEquals("5", resolver().resolve("{n:x}", input))
    }

    @Test
    fun `date uses takenAt when present and now otherwise`() {
        val taken = input.copy(takenAtMillis = 0L) // 1970-01-01T00:00:00Z
        assertEquals("19700101-000000", resolver().resolve("{date}", taken))
        assertEquals("2026", resolver().resolve("{date:yyyy}", input))
    }

    @Test
    fun `invalid date format falls back to the default`() {
        val resolved = resolver().resolve("{date:qqqq-invalid}", input)
        assertEquals(RenamePattern.DEFAULT_DATE_FORMAT.length, resolved.length)
    }

    @Test
    fun `rand is deterministic under an injected seed and honours width`() {
        val a = resolver(seed = 7).resolve("{rand:6}", input)
        val b = resolver(seed = 7).resolve("{rand:6}", input)
        assertEquals(a, b)
        assertEquals(6, a.length)
        assertTrue(a.all { it.isDigit() })
        assertEquals(RenamePattern.DEFAULT_RANDOM_DIGITS, resolver().resolve("{rand}", input).length)
    }

    @Test
    fun `unknown dimensions resolve to empty`() {
        val bare = RenameInput(originalName = "x", index = 0)
        assertEquals("-", resolver().resolve("{w}-{h}", bare).trim())
    }

    @Test
    fun `unknown token stays visible instead of vanishing`() {
        assertEquals("{foo}", resolver().resolve("{foo}", input))
    }
}
