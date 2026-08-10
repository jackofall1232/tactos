package dev.tactos.core.images

import dev.tactos.core.images.exif.ExifRemovalPreset
import dev.tactos.core.images.exif.MetadataTag
import kotlin.test.assertFailsWith
import org.junit.Test

class ImageJobTest {

    @Test
    fun `valid jobs construct`() {
        ImageJob.Resize(ResizeSpec.Fit(1080, 1080), ImageFormat.JPEG, quality = 85)
        ImageJob.Compress(ImageFormat.JPEG, quality = 70)
        ImageJob.Compress(ImageFormat.WEBP, targetBytes = 500_000)
        ImageJob.Convert(ImageFormat.PNG)
        ImageJob.ExifStrip(ExifRemovalPreset.Privacy)
        ImageJob.ExifStrip(ExifRemovalPreset.Custom, customTags = listOf(MetadataTag.GpsLatitude))
    }

    @Test
    fun `compress needs exactly one of quality or target`() {
        assertFailsWith<IllegalArgumentException> {
            ImageJob.Compress(ImageFormat.JPEG)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageJob.Compress(ImageFormat.JPEG, quality = 50, targetBytes = 1_000)
        }
    }

    @Test
    fun `compress rejects quality-less formats`() {
        assertFailsWith<IllegalArgumentException> {
            ImageJob.Compress(ImageFormat.PNG, quality = 50)
        }
    }

    @Test
    fun `quality bounds are enforced`() {
        assertFailsWith<IllegalArgumentException> {
            ImageJob.Convert(ImageFormat.JPEG, quality = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            ImageJob.Resize(ResizeSpec.Percent(0.5), ImageFormat.WEBP, quality = 101)
        }
    }

    @Test
    fun `custom exif strip needs explicit tags`() {
        assertFailsWith<IllegalArgumentException> {
            ImageJob.ExifStrip(ExifRemovalPreset.Custom)
        }
    }
}
