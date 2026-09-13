package dev.tactos.core.images

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class PhotoEditSpecTest {
    @Test fun `crop and rotation determine exported geometry`() {
        val spec = PhotoEditSpec(crop = PhotoCrop(.25f, 0f, .75f, .5f), quarterTurns = -1)
        assertEquals(3, spec.normalizedQuarterTurns)
        assertEquals(100 to 200, spec.outputSize(400, 200))
    }

    @Test fun `subpixel crops still contain a pixel`() {
        assertEquals(PhotoPixelBounds(0, 0, 1, 1), PhotoCrop(.01f, .01f, .02f, .02f).pixelBounds(2, 2))
        assertEquals(PhotoPixelBounds(9, 9, 10, 10), PhotoCrop(.999f, .999f, 1f, 1f).pixelBounds(10, 10))
    }

    @Test fun `large exports respect budget without enlarging small photos`() {
        assertEquals(4000 to 4000, PhotoEditSpec().outputSize(10000, 10000))
        assertEquals(20 to 10, PhotoEditSpec().outputSize(20, 10))
        val size = PhotoEditSpec().outputSize(Int.MAX_VALUE, 1, 100)
        assertTrue(size.first.toLong() * size.second <= 100)
    }

    @Test fun `reject empty crops and unsafe effect values`() {
        assertFailsWith<IllegalArgumentException> { PhotoCrop(left = 1f) }
        assertFailsWith<IllegalArgumentException> { PhotoCrop(top = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { PhotoEditSpec(brightness = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { PhotoEditSpec(contrast = 3f) }
        assertFailsWith<IllegalArgumentException> { PhotoWatermark("x", opacity = -1f) }
        assertFailsWith<IllegalArgumentException> { PhotoWatermark("x".repeat(201)) }
    }
}
