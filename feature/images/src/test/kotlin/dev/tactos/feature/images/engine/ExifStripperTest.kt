package dev.tactos.feature.images.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.exif.ExifRemovalPreset
import dev.tactos.core.images.exif.MetadataTag
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExifStripperTest {

    private lateinit var context: Context
    private lateinit var jpeg: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        jpeg = File(context.cacheDir, "exif-source.jpg")
        jpeg.outputStream().use { out ->
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(Color.GREEN) }
                .compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        // Write EXIF the way a camera app would.
        ExifInterface(jpeg).apply {
            setAttribute(MetadataTag.Make.key, "TestCam")
            setAttribute(MetadataTag.Model.key, "X100")
            setAttribute(MetadataTag.DatetimeOriginal.key, "2026:08:10 12:00:00")
            setLatLong(52.5, 13.4)
            saveAttributes()
        }
    }

    private fun sourceUri(): Uri = Uri.fromFile(jpeg)

    @Test
    fun `privacy preset removes gps, dates, and device identity`() {
        val result = ExifStripper(context).strip(
            sourceUri(),
            ImageJob.ExifStrip(ExifRemovalPreset.Privacy),
        )
        assertNotNull(result)
        assertTrue(result.removedKeys.isNotEmpty())

        val cleaned = File(context.cacheDir, "cleaned.jpg")
        cleaned.writeBytes(result.bytes)
        val exif = ExifInterface(cleaned)
        assertNull(exif.latLong)
        assertNull(exif.getAttribute(MetadataTag.Make.key))
        assertNull(exif.getAttribute(MetadataTag.Model.key))
        assertNull(exif.getAttribute(MetadataTag.DatetimeOriginal.key))
    }

    @Test
    fun `location-only preset keeps device identity`() {
        val result = assertNotNull(
            ExifStripper(context).strip(
                sourceUri(),
                ImageJob.ExifStrip(ExifRemovalPreset.LocationOnly),
            ),
        )
        val cleaned = File(context.cacheDir, "cleaned-location.jpg")
        cleaned.writeBytes(result.bytes)
        val exif = ExifInterface(cleaned)
        assertNull(exif.latLong)
        assertEquals("TestCam", exif.getAttribute(MetadataTag.Make.key))
    }

    @Test
    fun `stripping reports which keys were actually present`() {
        val result = assertNotNull(
            ExifStripper(context).strip(
                sourceUri(),
                ImageJob.ExifStrip(ExifRemovalPreset.Privacy),
            ),
        )
        assertTrue(MetadataTag.Make.key in result.removedKeys)
        // A tag that was never present must not be reported as removed.
        assertTrue(MetadataTag.LensSerialNumber.key !in result.removedKeys)
    }

    @Test
    fun `source document is never modified`() {
        val before = jpeg.readBytes()
        ExifStripper(context).strip(sourceUri(), ImageJob.ExifStrip(ExifRemovalPreset.Privacy))
        assertTrue(before.contentEquals(jpeg.readBytes()))
    }
}
