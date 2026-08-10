package dev.tactos.core.images

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class TargetSizeSearchTest {

    /** A monotonic fake encoder: size = quality * factor. */
    private fun linearProbe(factor: Long): (Int) -> Long = { q -> q * factor }

    @Test
    fun `finds the highest quality under the budget`() {
        // size = q * 100; target 5000 -> q=50 exactly.
        val result = TargetSizeSearch.search(targetBytes = 5_000, probe = linearProbe(100))
        assertEquals(50, result.quality)
        assertEquals(5_000, result.bytes)
        assertTrue(result.fits)
    }

    @Test
    fun `budget between steps picks the floor quality`() {
        val result = TargetSizeSearch.search(targetBytes = 5_050, probe = linearProbe(100))
        assertEquals(50, result.quality)
        assertTrue(result.fits)
    }

    @Test
    fun `everything fits returns max quality without extra probes`() {
        var probes = 0
        val result = TargetSizeSearch.search(targetBytes = 1_000_000) { q ->
            probes++
            q.toLong()
        }
        assertEquals(100, result.quality)
        assertTrue(result.fits)
        assertEquals(1, probes)
    }

    @Test
    fun `nothing fits reports overshoot at min quality`() {
        // Even q=1 produces 10_000 bytes > 1 byte target.
        val result = TargetSizeSearch.search(targetBytes = 1, probe = linearProbe(10_000))
        assertEquals(1, result.quality)
        assertEquals(10_000, result.bytes)
        assertFalse(result.fits)
    }

    @Test
    fun `probe count stays logarithmic`() {
        var probes = 0
        TargetSizeSearch.search(targetBytes = 4_242, probe = { q -> probes++; q * 100L })
        // 2 fast-path probes + ~log2(99) bisection steps.
        assertTrue(probes <= 10, "expected <= 10 probes, got $probes")
    }

    @Test
    fun `non-monotonic probes still return a truthful result`() {
        // A wobbly encoder: quality 60 encodes slightly smaller than 59.
        val result = TargetSizeSearch.search(targetBytes = 6_000) { q ->
            if (q == 60) 5_900L else q * 100L
        }
        // Whatever quality is chosen, the reported bytes must match its probe
        // and fit the budget.
        assertTrue(result.fits)
        assertTrue(result.bytes <= 6_000)
    }

    @Test
    fun `custom quality range is respected`() {
        val result = TargetSizeSearch.search(
            targetBytes = 5_000,
            minQuality = 40,
            maxQuality = 45,
            probe = linearProbe(1_000),
        )
        assertEquals(40, result.quality)
        assertFalse(result.fits) // 40 * 1000 = 40_000 > 5_000
    }

    @Test
    fun `invalid arguments are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            TargetSizeSearch.search(targetBytes = 0, probe = linearProbe(1))
        }
        assertFailsWith<IllegalArgumentException> {
            TargetSizeSearch.search(targetBytes = 10, minQuality = 0, probe = linearProbe(1))
        }
        assertFailsWith<IllegalArgumentException> {
            TargetSizeSearch.search(targetBytes = 10, minQuality = 80, maxQuality = 20, probe = linearProbe(1))
        }
    }
}
