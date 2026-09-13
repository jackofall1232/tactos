package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import dev.tactos.core.images.PhotoEditSpec
import dev.tactos.core.images.PhotoFilter
import dev.tactos.core.images.PhotoWatermark
import dev.tactos.core.images.WatermarkPosition

/** Offline rendering. Run off the main thread; the caller owns both source and result. */
object PhotoEditor {
    /** Allocates only the output (at most 64 MB). Never mutates or recycles [source]. */
    fun render(source: Bitmap, spec: PhotoEditSpec): Bitmap {
        require(!source.isRecycled) { "Source image has been released" }
        require(source.config != Bitmap.Config.HARDWARE) { "Use a software-decoded image" }
        val crop = spec.crop.pixelBounds(source.width, source.height)
        val (width, height) = spec.outputSize(source.width, source.height)
        val rotated = spec.normalizedQuarterTurns % 2 == 1
        val naturalWidth = if (rotated) crop.height else crop.width
        val naturalHeight = if (rotated) crop.width else crop.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(effects(spec))
            }
            canvas.save()
            canvas.translate(width / 2f, height / 2f)
            canvas.scale(width.toFloat() / naturalWidth, height.toFloat() / naturalHeight)
            canvas.scale(if (spec.flipHorizontal) -1f else 1f, if (spec.flipVertical) -1f else 1f)
            canvas.rotate(spec.normalizedQuarterTurns * 90f)
            canvas.drawBitmap(source, Rect(crop.left, crop.top, crop.right, crop.bottom),
                RectF(-crop.width / 2f, -crop.height / 2f, crop.width / 2f, crop.height / 2f), paint)
            canvas.restore()
            spec.watermark?.takeIf { it.text.isNotBlank() && it.opacity > 0f }?.let {
                drawWatermark(canvas, width, height, it)
            }
            return output
        } catch (error: Throwable) {
            output.recycle()
            throw error
        }
    }

    private fun effects(spec: PhotoEditSpec): ColorMatrix {
        val contrast = spec.contrast
        val offset = 128f * (1f - contrast) + 255f * spec.brightness
        val warmth = 40f * spec.warmth
        val matrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, offset + warmth,
            0f, contrast, 0f, 0f, offset,
            0f, 0f, contrast, 0f, offset - warmth,
            0f, 0f, 0f, 1f, 0f,
        ))
        matrix.postConcat(ColorMatrix().apply { setSaturation(spec.saturation) })
        when (spec.filter) {
            PhotoFilter.NONE -> Unit
            PhotoFilter.GRAYSCALE -> matrix.postConcat(ColorMatrix().apply { setSaturation(0f) })
            PhotoFilter.VIVID -> matrix.postConcat(ColorMatrix().apply { setSaturation(1.35f) })
            PhotoFilter.SEPIA -> matrix.postConcat(ColorMatrix(floatArrayOf(
                .393f, .769f, .189f, 0f, 0f,
                .349f, .686f, .168f, 0f, 0f,
                .272f, .534f, .131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
        return matrix
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int, mark: PhotoWatermark) {
        val text = mark.text.replace('\n', ' ').replace('\r', ' ').trim()
        val shortSide = minOf(width, height).toFloat()
        val padding = shortSide * .035f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (mark.opacity * 255).toInt()
            textSize = shortSide * .055f
            isFakeBoldText = true
        }
        val available = (width - 2 * padding).coerceAtLeast(.1f)
        val measured = paint.measureText(text)
        if (measured > available) paint.textSize *= available / measured
        val textWidth = paint.measureText(text)
        val metrics = paint.fontMetrics
        val x = when (mark.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> padding
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> width - padding - textWidth
            WatermarkPosition.CENTER -> (width - textWidth) / 2f
        }
        val y = when (mark.position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> padding - metrics.ascent
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - padding - metrics.descent
            WatermarkPosition.CENTER -> (height - metrics.ascent - metrics.descent) / 2f
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = paint.textSize * .09f
        paint.color = Color.BLACK
        paint.alpha = (mark.opacity * 180).toInt()
        canvas.drawText(text, x, y, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = (mark.opacity * 255).toInt()
        canvas.drawText(text, x, y, paint)
    }
}
