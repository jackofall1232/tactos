package dev.tactos.core.images

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/** Non-destructive edits; crop uses the orientation-corrected source coordinates. */
data class PhotoEditSpec(
    val crop: PhotoCrop = PhotoCrop(),
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.NONE,
    val watermark: PhotoWatermark? = null,
) {
    init {
        require(brightness.isFinite() && brightness in -1f..1f)
        require(contrast.isFinite() && contrast in 0f..2f)
        require(saturation.isFinite() && saturation in 0f..2f)
        require(warmth.isFinite() && warmth in -1f..1f)
    }

    val normalizedQuarterTurns: Int get() = ((quarterTurns % 4) + 4) % 4

    fun outputSize(width: Int, height: Int, maxPixels: Long = MAX_OUTPUT_PIXELS): Pair<Int, Int> {
        require(maxPixels > 0)
        val bounds = crop.pixelBounds(width, height)
        val swap = normalizedQuarterTurns % 2 == 1
        val w = if (swap) bounds.height else bounds.width
        val h = if (swap) bounds.width else bounds.height
        val scale = minOf(1.0, sqrt(maxPixels.toDouble() / (w.toLong() * h)))
        var outW = maxOf(1, floor(w * scale).toInt())
        var outH = maxOf(1, floor(h * scale).toInt())
        // Very narrow images can round one dimension up to 1, exceeding the budget.
        if (outW.toLong() * outH > maxPixels) {
            if (outW >= outH) outW = (maxPixels / outH).toInt().coerceAtLeast(1)
            else outH = (maxPixels / outW).toInt().coerceAtLeast(1)
        }
        return outW to outH
    }

    companion object {
        const val MAX_OUTPUT_PIXELS: Long = 16_000_000
    }
}

data class PhotoCrop(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() && it in 0f..1f })
        require(left < right && top < bottom) { "Crop must have positive area" }
    }

    /** Include every intersecting pixel, with at least one pixel on each axis. */
    fun pixelBounds(width: Int, height: Int): PhotoPixelBounds {
        require(width > 0 && height > 0)
        val x = floor(left.toDouble() * width).toInt().coerceIn(0, width - 1)
        val y = floor(top.toDouble() * height).toInt().coerceIn(0, height - 1)
        return PhotoPixelBounds(x, y,
            ceil(right.toDouble() * width).toInt().coerceIn(x + 1, width),
            ceil(bottom.toDouble() * height).toInt().coerceIn(y + 1, height))
    }
}

data class PhotoPixelBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

enum class PhotoFilter { NONE, GRAYSCALE, SEPIA, VIVID }
enum class WatermarkPosition { TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }

data class PhotoWatermark(
    val text: String,
    val opacity: Float = 0.7f,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
) {
    init {
        require(text.length <= 200) { "Watermark is limited to 200 characters" }
        require(opacity.isFinite() && opacity in 0f..1f)
    }
}
