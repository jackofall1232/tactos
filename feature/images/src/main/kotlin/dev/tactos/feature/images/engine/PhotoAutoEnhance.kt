package dev.tactos.feature.images.engine

import android.graphics.Bitmap
import android.graphics.Color
import dev.tactos.core.images.PhotoEditSpec
import kotlin.math.roundToInt

/** Conservative, offline tonal suggestions. No reconstruction or generated detail.
 * The caller retains ownership of [Bitmap] inputs and should call this off the main thread.
 */
object PhotoAutoEnhance {
    fun suggest(source: Bitmap): PhotoEditSpec {
        require(!source.isRecycled) { "Source image has been released" }
        require(source.config != Bitmap.Config.HARDWARE) { "Use a software-decoded image" }
        val histogram = IntArray(256)
        var total = 0
        var sum = 0.0
        // A uniform grid bounds work at 4096 pixel reads, independent of image dimensions.
        val columns = minOf(source.width, 64)
        val rows = minOf(source.height, 64)
        for (row in 0 until rows) {
            val y = ((row.toLong() * source.height + source.height / 2) / rows).toInt().coerceAtMost(source.height - 1)
            for (column in 0 until columns) {
                val x = ((column.toLong() * source.width + source.width / 2) / columns).toInt().coerceAtMost(source.width - 1)
                val pixel = source.getPixel(x, y)
                // Transparent padding is not exposure information about the photograph.
                if (Color.alpha(pixel) < 128) continue
                val luminance = (.2126 * Color.red(pixel) + .7152 * Color.green(pixel) + .0722 * Color.blue(pixel))
                    .roundToInt().coerceIn(0, 255)
                histogram[luminance]++
                total++
                sum += luminance
            }
        }
        if (total == 0) return PhotoEditSpec()
        fun percentile(fraction: Float): Int {
            val target = (total * fraction).roundToInt().coerceAtLeast(1)
            var cumulative = 0
            for (value in histogram.indices) {
                cumulative += histogram[value]
                if (cumulative >= target) return value
            }
            return 255
        }
        val low = percentile(.05f)
        val high = percentile(.95f)
        val span = high - low
        // Flat artwork, black/white frames, and near-flat images have no reliable tonal range.
        if (span < 12) return PhotoEditSpec()
        val mean = (sum / total).toFloat()
        var contrast = (1f + (192f / span - 1f).coerceAtLeast(0f) * .25f).coerceIn(1f, 1.18f)
        // Keep the central 90% of luminances in bounds with at most +/- 4% brightness.
        if (low < 128) contrast = minOf(contrast, (128f + 10f) / (128f - low))
        if (high > 128) contrast = minOf(contrast, (127f + 10f) / (high - 128f))
        val lower = maxOf(-.04f, -(128f + (low - 128f) * contrast) / 255f)
        val upper = minOf(.04f, (255f - 128f - (high - 128f) * contrast) / 255f)
        val brightness = if (lower <= upper) {
            ((128f - mean) * .2f / 255f).coerceIn(lower, upper)
        } else 0f
        return PhotoEditSpec(brightness = brightness, contrast = contrast, saturation = 1.03f)
    }
}
