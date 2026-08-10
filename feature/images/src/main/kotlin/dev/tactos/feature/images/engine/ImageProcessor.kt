package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.TargetSizeSearch
import kotlin.math.max

/**
 * Executes bitmap-level [ImageJob]s (resize/convert/compress). EXIF
 * stripping is stream-level and lives in [ExifStripper]. Pure in/out —
 * callers own threading (Dispatchers.Default) and recycling.
 */
object ImageProcessor {

    class Output(
        val bytes: ByteArray,
        val width: Int,
        val height: Int,
        /** Quality the encoder actually used; null for quality-less formats. */
        val quality: Int?,
        /** False only for target-size jobs whose budget couldn't be met. */
        val fitsTarget: Boolean = true,
    )

    fun process(source: Bitmap, job: ImageJob): Output = when (job) {
        is ImageJob.Resize -> {
            val target = job.spec.resolveTargetSize(source.width, source.height)
            val scaled = scale(source, target.width, target.height)
            val quality = if (job.format.supportsQuality) job.quality else 100
            val bytes = ImageEncoders.encodeToBytes(scaled, job.format, quality)
            val (width, height) = scaled.width to scaled.height
            // Free intermediate pixel memory promptly (batch runs).
            if (scaled !== source) scaled.recycle()
            Output(
                bytes = bytes,
                width = width,
                height = height,
                quality = quality.takeIf { job.format.supportsQuality },
            )
        }

        is ImageJob.Convert -> {
            val quality = if (job.format.supportsQuality) job.quality else 100
            Output(
                bytes = ImageEncoders.encodeToBytes(source, job.format, quality),
                width = source.width,
                height = source.height,
                quality = quality.takeIf { job.format.supportsQuality },
            )
        }

        is ImageJob.Compress -> {
            val quality = job.quality
            if (quality != null) {
                Output(
                    bytes = ImageEncoders.encodeToBytes(source, job.format, quality),
                    width = source.width,
                    height = source.height,
                    quality = quality,
                )
            } else {
                compressToTarget(source, job)
            }
        }

        is ImageJob.ExifStrip -> error("ExifStrip is stream-level; use ExifStripper")
    }

    /**
     * Quality search first; when even quality-1 overshoots, halve the pixel
     * count and try again (the outer loop ImageToolbox's weight-resize uses),
     * bottoming out at [MIN_DIMENSION].
     */
    private fun compressToTarget(source: Bitmap, job: ImageJob.Compress): Output {
        val targetBytes = checkNotNull(job.targetBytes)
        var bitmap = source
        while (true) {
            val result = TargetSizeSearch.search(targetBytes) { quality ->
                ImageEncoders.encodeToBytes(bitmap, job.format, quality).size.toLong()
            }
            val canDownscale =
                max(bitmap.width, bitmap.height) / SCALE_STEP_DIVISOR >= MIN_DIMENSION
            if (result.fits || !canDownscale) {
                val bytes = ImageEncoders.encodeToBytes(bitmap, job.format, result.quality)
                val (width, height) = bitmap.width to bitmap.height
                if (bitmap !== source) bitmap.recycle()
                return Output(
                    bytes = bytes,
                    width = width,
                    height = height,
                    quality = result.quality,
                    fitsTarget = result.fits,
                )
            }
            val scaled = scale(
                bitmap,
                max(1, bitmap.width / SCALE_STEP_DIVISOR),
                max(1, bitmap.height / SCALE_STEP_DIVISOR),
            )
            // Each loop step abandons the previous intermediate — free it.
            if (bitmap !== source) bitmap.recycle()
            bitmap = scaled
        }
    }

    private fun scale(source: Bitmap, width: Int, height: Int): Bitmap =
        if (source.width == width && source.height == height) source
        else Bitmap.createScaledBitmap(source, width, height, /* filter = */ true)

    private const val SCALE_STEP_DIVISOR = 2
    private const val MIN_DIMENSION = 64
}
