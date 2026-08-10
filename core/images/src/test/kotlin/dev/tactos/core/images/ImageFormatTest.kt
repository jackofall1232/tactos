package dev.tactos.core.images

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ImageFormatTest {

    @Test
    fun `mime type lookup is case-insensitive and trims`() {
        val cases = mapOf(
            "image/png" to ImageFormat.PNG,
            "IMAGE/PNG" to ImageFormat.PNG,
            " image/jpeg " to ImageFormat.JPEG,
            "image/webp" to ImageFormat.WEBP,
        )
        for ((mime, expected) in cases) {
            assertEquals(expected, ImageFormat.fromMimeType(mime), mime)
        }
    }

    @Test
    fun `unsupported or null mime types return null`() {
        for (mime in listOf(null, "", "image/gif", "image/avif", "text/plain", "png")) {
            assertNull(ImageFormat.fromMimeType(mime), "mime=$mime")
        }
    }

    @Test
    fun `extension lookup handles jpeg alias, dots, and case`() {
        val cases = mapOf(
            "png" to ImageFormat.PNG,
            "jpg" to ImageFormat.JPEG,
            "jpeg" to ImageFormat.JPEG,
            "JPEG" to ImageFormat.JPEG,
            ".webp" to ImageFormat.WEBP,
        )
        for ((ext, expected) in cases) {
            assertEquals(expected, ImageFormat.fromExtension(ext), ext)
        }
        assertNull(ImageFormat.fromExtension("gif"))
        assertNull(ImageFormat.fromExtension(null))
    }

    @Test
    fun `only png is quality-less`() {
        assertEquals(false, ImageFormat.PNG.supportsQuality)
        assertEquals(true, ImageFormat.JPEG.supportsQuality)
        assertEquals(true, ImageFormat.WEBP.supportsQuality)
    }
}
