package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.os.Build
import dev.tactos.core.images.ImageFormat
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Per-format encoding behind one seam — the ImageCompressorBackend factory
 * idea from ImageToolbox (ADR-0004 tier 2), collapsed to the three formats
 * the platform encodes natively.
 */
object ImageEncoders {

    /** Encode [bitmap] to [out]. [quality] is ignored by quality-less formats. */
    fun encode(bitmap: Bitmap, format: ImageFormat, quality: Int, out: OutputStream) {
        require(quality in 1..100) { "quality must be in 1..100" }
        val compressFormat = when (format) {
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.WEBP ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Quality 100 = lossless on the modern API, matching how
                    // the platform documents WEBP_LOSSY/WEBP_LOSSLESS.
                    if (quality >= 100) Bitmap.CompressFormat.WEBP_LOSSLESS
                    else Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
        }
        check(bitmap.compress(compressFormat, quality, out)) {
            "Bitmap.compress failed for $format"
        }
    }

    fun encodeToBytes(bitmap: Bitmap, format: ImageFormat, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        encode(bitmap, format, quality, out)
        return out.toByteArray()
    }
}
