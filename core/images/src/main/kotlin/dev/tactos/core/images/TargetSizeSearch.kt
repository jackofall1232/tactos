package dev.tactos.core.images

/**
 * Find the highest encoder quality whose output fits a byte budget, by
 * binary search over the quality range. The probe is injected (quality ->
 * encoded size in bytes) so the search stays pure and JVM-testable; the
 * Android engine probes by encoding to a counting stream.
 *
 * Pattern-inspired by ImageToolbox's weight-resize tool (compress to target
 * file size); the downscale-when-even-minimum-quality-overshoots loop lives
 * in the engine, because downscaling changes the probe itself.
 */
object TargetSizeSearch {

    /**
     * @property quality best quality found (highest that fits, or [minQuality] on overshoot)
     * @property bytes probed size at [quality]
     * @property fits whether [bytes] is within the target budget
     */
    data class Result(val quality: Int, val bytes: Long, val fits: Boolean)

    fun search(
        targetBytes: Long,
        minQuality: Int = 1,
        maxQuality: Int = 100,
        probe: (quality: Int) -> Long,
    ): Result {
        require(targetBytes > 0) { "targetBytes must be positive" }
        require(minQuality in 1..100 && maxQuality in 1..100 && minQuality <= maxQuality) {
            "quality range must satisfy 1 <= min <= max <= 100"
        }

        // Fast paths: everything fits, or nothing does.
        val atMax = probe(maxQuality)
        if (atMax <= targetBytes) return Result(maxQuality, atMax, fits = true)
        val atMin = probe(minQuality)
        if (atMin > targetBytes) return Result(minQuality, atMin, fits = false)

        // Invariant: low fits, high does not. Encoded size is treated as
        // monotonic in quality; real encoders wobble slightly, which is fine —
        // the result is re-probed, so `bytes`/`fits` are always truthful.
        var low = minQuality
        var lowBytes = atMin
        var high = maxQuality
        while (low + 1 < high) {
            val mid = (low + high) / 2
            val size = probe(mid)
            if (size <= targetBytes) {
                low = mid
                lowBytes = size
            } else {
                high = mid
            }
        }
        return Result(low, lowBytes, fits = true)
    }
}
