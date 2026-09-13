package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.graphics.Color
import dev.tactos.core.images.PhotoEditSpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoAutoEnhanceTest {
    private fun ramp(from: Int, to: Int): Bitmap = Bitmap.createBitmap(
        IntArray(64 * 64) { index ->
            val level = from + (to - from) * (index % 64) / 63
            Color.rgb(level, level, level)
        }, 64, 64, Bitmap.Config.ARGB_8888,
    )

    @Test fun `flat black white gray and transparent inputs remain unchanged`() {
        for (color in listOf(Color.BLACK, Color.WHITE, Color.GRAY, Color.TRANSPARENT)) {
            val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            try {
                assertEquals(PhotoEditSpec(), PhotoAutoEnhance.suggest(source))
                assertFalse(source.isRecycled)
                assertEquals(color, source.getPixel(0, 0))
            } finally { source.recycle() }
        }
    }

    @Test fun `low contrast gets modest stretch with no warmth shift`() {
        val source = ramp(85, 145)
        try {
            val suggested = PhotoAutoEnhance.suggest(source)
            assertTrue(suggested.contrast > 1f && suggested.contrast <= 1.18f)
            assertTrue(suggested.brightness > 0f && suggested.brightness <= .04f)
            assertEquals(0f, suggested.warmth)
            assertTrue(suggested.saturation in 1f..1.05f)
        } finally { source.recycle() }
    }

    @Test fun `extreme exposures remain finite and effect caps hold`() {
        for ((from, to) in listOf(0 to 40, 210 to 255, 0 to 255, 110 to 115)) {
            val source = ramp(from, to)
            try {
                val spec = PhotoAutoEnhance.suggest(source)
                assertTrue(spec.brightness.isFinite() && spec.brightness in -0.04f..0.04f)
                assertTrue(spec.contrast.isFinite() && spec.contrast in 1f..1.18f)
                assertTrue(spec.saturation.isFinite() && spec.saturation in 1f..1.05f)
                if (to - from < 12) assertEquals(PhotoEditSpec(), spec)
            } finally { source.recycle() }
        }
    }

    @Test fun `dark and bright image corrections move in appropriate directions`() {
        val dark = ramp(20, 110)
        val bright = ramp(160, 240)
        try {
            assertTrue(PhotoAutoEnhance.suggest(dark).brightness > 0)
            assertTrue(PhotoAutoEnhance.suggest(bright).brightness < 0)
        } finally { dark.recycle(); bright.recycle() }
    }
}
