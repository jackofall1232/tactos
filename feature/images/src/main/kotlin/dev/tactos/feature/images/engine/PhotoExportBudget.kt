package dev.tactos.feature.images.engine

import kotlin.math.sqrt

/** Conservative decode size allowing source, edited pixels, JPEG flattening and encoded bytes to coexist. */
object PhotoExportBudget {
    fun maxDimension(maxHeapBytes: Long): Int {
        require(maxHeapBytes > 0)
        // Forty-eight bytes per source pixel reserves headroom for the app and previews,
        // in addition to the simultaneous export buffers. Decoder sampling may reduce further.
        return sqrt(maxHeapBytes / 48.0).toInt().coerceIn(512, 4096)
    }
}
