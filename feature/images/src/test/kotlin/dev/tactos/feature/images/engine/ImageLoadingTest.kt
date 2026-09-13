package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageLoadingTest {
    @Test fun `bounds-only null bitmap does not reject a valid image`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File.createTempFile("bounds", ".png", context.cacheDir)
        val bitmap = Bitmap.createBitmap(24, 12, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val uri = Uri.fromFile(file)
            val info = assertNotNull(ImageLoading.info(context, uri))
            assertEquals(24, info.width); assertEquals(12, info.height)
            val decoded = assertNotNull(ImageLoading.decode(context, uri))
            assertEquals(24, decoded.width); assertEquals(12, decoded.height)
            decoded.recycle()
        } finally { bitmap.recycle(); file.delete() }
    }
}
