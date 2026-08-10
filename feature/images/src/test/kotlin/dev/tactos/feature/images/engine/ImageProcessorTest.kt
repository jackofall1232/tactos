package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import dev.tactos.core.images.ImageFormat
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.ResizeSpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageProcessorTest {

    private fun bitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(200, 120, 40))
        }

    private fun decode(bytes: ByteArray): Bitmap =
        assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))

    @Test
    fun `resize fit produces aspect-preserved dimensions and decodable output`() {
        val out = ImageProcessor.process(
            bitmap(400, 200),
            ImageJob.Resize(ResizeSpec.Fit(100, 100), ImageFormat.PNG),
        )
        assertEquals(100, out.width)
        assertEquals(50, out.height)
        val decoded = decode(out.bytes)
        assertEquals(100, decoded.width)
        assertEquals(50, decoded.height)
    }

    @Test
    fun `convert to each format roundtrips through the platform decoder`() {
        for (format in ImageFormat.entries) {
            val out = ImageProcessor.process(bitmap(64, 48), ImageJob.Convert(format))
            val decoded = decode(out.bytes)
            assertEquals(64, decoded.width, "$format")
            assertEquals(48, decoded.height, "$format")
        }
    }

    @Test
    fun `quality-less formats report null quality`() {
        val png = ImageProcessor.process(bitmap(32, 32), ImageJob.Convert(ImageFormat.PNG))
        assertEquals(null, png.quality)
        val jpeg = ImageProcessor.process(
            bitmap(32, 32),
            ImageJob.Convert(ImageFormat.JPEG, quality = 77),
        )
        assertEquals(77, jpeg.quality)
    }

    @Test
    fun `compress to quality encodes at that quality`() {
        val out = ImageProcessor.process(
            bitmap(128, 128),
            ImageJob.Compress(ImageFormat.JPEG, quality = 40),
        )
        assertEquals(40, out.quality)
        assertTrue(out.fitsTarget)
        decode(out.bytes)
    }

    @Test
    fun `compress to target size stays within budget when feasible`() {
        val reference = ImageProcessor.process(
            bitmap(256, 256),
            ImageJob.Compress(ImageFormat.JPEG, quality = 100),
        ).bytes.size.toLong()
        val target = reference / 2
        val out = ImageProcessor.process(
            bitmap(256, 256),
            ImageJob.Compress(ImageFormat.JPEG, targetBytes = target),
        )
        assertTrue(out.fitsTarget)
        assertTrue(out.bytes.size <= target, "${out.bytes.size} > $target")
    }

    @Test
    fun `impossible target reports best effort instead of looping forever`() {
        val out = ImageProcessor.process(
            bitmap(256, 256),
            ImageJob.Compress(ImageFormat.JPEG, targetBytes = 1),
        )
        // Downscaling bottoms out at the minimum dimension and reports the
        // honest result.
        assertTrue(!out.fitsTarget)
        assertTrue(out.width < 256)
    }
}
