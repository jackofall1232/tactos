package dev.tactos.core.images

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class ResizeSpecTest {

    @Test
    fun `explicit returns the exact size regardless of aspect`() {
        assertEquals(
            TargetSize(300, 100),
            ResizeSpec.Explicit(300, 100).resolveTargetSize(1000, 1000),
        )
    }

    @Test
    fun `fit preserves aspect ratio`() {
        // 4000x3000 into 1080x1080 -> scale by 1080/4000
        assertEquals(
            TargetSize(1080, 810),
            ResizeSpec.Fit(1080, 1080).resolveTargetSize(4000, 3000),
        )
        // Portrait source: height binds.
        assertEquals(
            TargetSize(810, 1080),
            ResizeSpec.Fit(1080, 1080).resolveTargetSize(3000, 4000),
        )
    }

    @Test
    fun `fit never upscales`() {
        assertEquals(
            TargetSize(640, 480),
            ResizeSpec.Fit(1920, 1080).resolveTargetSize(640, 480),
        )
    }

    @Test
    fun `percent scales both dimensions and floors at one pixel`() {
        assertEquals(
            TargetSize(500, 250),
            ResizeSpec.Percent(0.5).resolveTargetSize(1000, 500),
        )
        // Tiny source at a tiny fraction still yields a valid 1x1 target.
        assertEquals(
            TargetSize(1, 1),
            ResizeSpec.Percent(0.01).resolveTargetSize(3, 3),
        )
        // Upscaling is allowed when asked for explicitly.
        assertEquals(
            TargetSize(2000, 1000),
            ResizeSpec.Percent(2.0).resolveTargetSize(1000, 500),
        )
    }

    @Test
    fun `extreme aspect ratios keep both dimensions at least one pixel`() {
        assertEquals(
            TargetSize(1080, 1),
            ResizeSpec.Fit(1080, 1080).resolveTargetSize(20_000, 10),
        )
    }

    @Test
    fun `invalid constructions and sources are rejected`() {
        assertFailsWith<IllegalArgumentException> { ResizeSpec.Explicit(0, 100) }
        assertFailsWith<IllegalArgumentException> { ResizeSpec.Explicit(100, -1) }
        assertFailsWith<IllegalArgumentException> { ResizeSpec.Fit(0, 0) }
        assertFailsWith<IllegalArgumentException> { ResizeSpec.Percent(0.0) }
        assertFailsWith<IllegalArgumentException> { ResizeSpec.Percent(4.1) }
        assertFailsWith<IllegalArgumentException> {
            ResizeSpec.Fit(100, 100).resolveTargetSize(0, 100)
        }
    }
}
