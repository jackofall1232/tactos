package dev.tactos.core.images

/** A resolved target size in pixels. */
data class TargetSize(val width: Int, val height: Int)

/**
 * How to resize an image. Pure math — the Android engine feeds the resolved
 * size to Bitmap scaling. Adapted in shape (Explicit/Fit/Percent) from
 * ImageToolbox's ResizeType, reduced to what tactos v1 ships.
 */
sealed interface ResizeSpec {

    /** Exact output size, aspect ratio not preserved. */
    data class Explicit(val width: Int, val height: Int) : ResizeSpec {
        init {
            require(width > 0 && height > 0) { "Explicit size must be positive" }
        }
    }

    /**
     * Largest aspect-preserving size that fits in [maxWidth] x [maxHeight].
     * Never upscales: a source already inside the box keeps its size.
     */
    data class Fit(val maxWidth: Int, val maxHeight: Int) : ResizeSpec {
        init {
            require(maxWidth > 0 && maxHeight > 0) { "Fit bounds must be positive" }
        }
    }

    /** Scale both dimensions by [fraction] (0 exclusive to 4 inclusive). */
    data class Percent(val fraction: Double) : ResizeSpec {
        init {
            require(fraction > 0.0 && fraction <= 4.0) { "fraction must be in (0, 4]" }
        }
    }

    fun resolveTargetSize(srcWidth: Int, srcHeight: Int): TargetSize {
        require(srcWidth > 0 && srcHeight > 0) { "source size must be positive" }
        return when (this) {
            is Explicit -> TargetSize(width, height)
            is Fit -> {
                if (srcWidth <= maxWidth && srcHeight <= maxHeight) {
                    TargetSize(srcWidth, srcHeight)
                } else {
                    val scale = minOf(
                        maxWidth.toDouble() / srcWidth,
                        maxHeight.toDouble() / srcHeight,
                    )
                    TargetSize(
                        width = (srcWidth * scale).toInt().coerceAtLeast(1),
                        height = (srcHeight * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
            is Percent -> TargetSize(
                width = (srcWidth * fraction).toInt().coerceAtLeast(1),
                height = (srcHeight * fraction).toInt().coerceAtLeast(1),
            )
        }
    }
}
