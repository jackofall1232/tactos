package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.graphics.Color
import dev.tactos.core.images.PhotoCrop
import dev.tactos.core.images.PhotoEditSpec
import dev.tactos.core.images.PhotoFilter
import dev.tactos.core.images.PhotoWatermark
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoEditorTest {
    private fun stripes(): Bitmap = Bitmap.createBitmap(
        intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA),
        3, 2, Bitmap.Config.ARGB_8888,
    )

    @Test fun `identity returns independent pixels and never recycles source`() {
        val source = stripes()
        val out = PhotoEditor.render(source, PhotoEditSpec())
        assertNotSame(source, out)
        assertFalse(source.isRecycled)
        for (y in 0 until 2) for (x in 0 until 3) assertEquals(source.getPixel(x, y), out.getPixel(x, y))
        out.eraseColor(Color.BLACK)
        assertEquals(Color.RED, source.getPixel(0, 0))
    }

    @Test fun `clockwise rotation followed by displayed horizontal flip has correct pixels`() {
        val source = stripes()
        val rotated = PhotoEditor.render(source, PhotoEditSpec(quarterTurns = 1))
        assertEquals(2, rotated.width)
        assertEquals(3, rotated.height)
        assertEquals(Color.YELLOW, rotated.getPixel(0, 0))
        assertEquals(Color.RED, rotated.getPixel(1, 0))
        val flipped = PhotoEditor.render(source, PhotoEditSpec(quarterTurns = 1, flipHorizontal = true))
        assertEquals(Color.RED, flipped.getPixel(0, 0))
        assertEquals(Color.YELLOW, flipped.getPixel(1, 0))
    }

    @Test fun `crop applies before transform`() {
        val source = stripes()
        val out = PhotoEditor.render(source, PhotoEditSpec(crop = PhotoCrop(0f, 0f, 1f, .5f), quarterTurns = 1))
        assertEquals(1, out.width)
        assertEquals(3, out.height)
        assertEquals(Color.RED, out.getPixel(0, 0))
        assertEquals(Color.BLUE, out.getPixel(0, 2))
    }

    @Test fun `grayscale changes RGB and preserves alpha`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.argb(128, 180, 80, 20))
        }
        val out = PhotoEditor.render(source, PhotoEditSpec(filter = PhotoFilter.GRAYSCALE))
        val pixel = out.getPixel(0, 0)
        assertEquals(128, Color.alpha(pixel))
        assertEquals(Color.red(pixel), Color.green(pixel))
        assertEquals(Color.green(pixel), Color.blue(pixel))
    }

    @Test fun `brightness and warmth have predictable direction`() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(100, 100, 100)) }
        val bright = PhotoEditor.render(source, PhotoEditSpec(brightness = .2f)).getPixel(0, 0)
        assertTrue(Color.red(bright) in 150..152)
        val warm = PhotoEditor.render(source, PhotoEditSpec(warmth = 1f)).getPixel(0, 0)
        assertTrue(Color.red(warm) > Color.blue(warm))
    }

    @Test fun `watermark leaves visible pixels while zero opacity is invisible`() {
        val source = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
        val out = PhotoEditor.render(source, PhotoEditSpec(watermark = PhotoWatermark("tactos")))
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        assertTrue(pixels.any { Color.alpha(it) > 0 })
        val invisible = PhotoEditor.render(source, PhotoEditSpec(watermark = PhotoWatermark("tactos", opacity = 0f)))
        invisible.getPixels(pixels, 0, invisible.width, 0, 0, invisible.width, invisible.height)
        assertTrue(pixels.all { it == Color.TRANSPARENT })
    }
}
