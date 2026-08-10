package dev.tactos.feature.images.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface

/**
 * Decodes user-picked documents. BitmapFactory (not ImageDecoder) keeps one
 * code path across minSdk 26..36; [decode] downsamples with inSampleSize so
 * a 100-megapixel pick cannot OOM the app.
 */
object ImageLoading {

    data class Info(
        val width: Int,
        val height: Int,
        val mimeType: String?,
        val sizeBytes: Long?,
        val displayName: String?,
    )

    /** Bounds-only decode plus provider metadata; null when [uri] isn't a decodable image. */
    fun info(context: Context, uri: Uri): Info? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull() ?: return null
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var sizeBytes: Long? = null
        var displayName: String? = null
        // Providers can throw (SecurityException on expired grants, provider
        // bugs) — metadata is best-effort, never a crash.
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIx >= 0 && !cursor.isNull(sizeIx)) sizeBytes = cursor.getLong(sizeIx)
                    val nameIx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIx >= 0) displayName = cursor.getString(nameIx)
                }
            }
        }
        return Info(
            width = options.outWidth,
            height = options.outHeight,
            mimeType = options.outMimeType,
            sizeBytes = sizeBytes,
            displayName = displayName,
        )
    }

    /**
     * Full decode, downsampled so neither dimension exceeds [maxDimension].
     * Null when the stream can't be opened or decoded.
     */
    fun decode(context: Context, uri: Uri, maxDimension: Int = MAX_DECODE_DIMENSION): Bitmap? {
        val info = info(context, uri) ?: return null
        var sample = 1
        while (info.width / (sample * 2) >= maxDimension || info.height / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }.getOrNull() ?: return null
        // Camera JPEGs often store pixels unrotated with orientation in EXIF;
        // re-encoding without applying it would save a sideways image.
        return applyOrientation(decoded, orientationOf(context, uri))
    }

    private fun orientationOf(context: Context, uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val oriented = runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrNull() ?: return bitmap
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    /** Base name (no extension) for rename patterns; empty when unknown. */
    fun baseName(info: Info?): String =
        info?.displayName?.substringBeforeLast('.').orEmpty()

    const val MAX_DECODE_DIMENSION: Int = 4096
}
