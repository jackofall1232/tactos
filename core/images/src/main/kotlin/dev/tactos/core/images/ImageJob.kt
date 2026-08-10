package dev.tactos.core.images

import dev.tactos.core.images.exif.ExifRemovalPreset

/**
 * What to do to each picked image — pure data, no Android types. The engine
 * in feature/images interprets one job against a batch of inputs. Modeling
 * work as declarative specs (not UI callbacks) is the binding V2 constraint
 * (.l00prite/memory.md): these specs are the future model-invocable surface.
 */
sealed interface ImageJob {

    /** Resize, optionally converting; [quality] applies when the format supports it. */
    data class Resize(
        val spec: ResizeSpec,
        val format: ImageFormat,
        val quality: Int = DEFAULT_QUALITY,
    ) : ImageJob {
        init {
            require(quality in 1..100) { "quality must be in 1..100" }
        }
    }

    /** Re-encode to hit [targetBytes] (via [TargetSizeSearch]) or at [quality]. */
    data class Compress(
        val format: ImageFormat,
        val quality: Int? = null,
        val targetBytes: Long? = null,
    ) : ImageJob {
        init {
            require((quality != null) xor (targetBytes != null)) {
                "exactly one of quality or targetBytes must be set"
            }
            quality?.let { require(it in 1..100) { "quality must be in 1..100" } }
            targetBytes?.let { require(it > 0) { "targetBytes must be positive" } }
            require(format.supportsQuality) { "compress needs a quality-capable format" }
        }
    }

    /** Convert to [format] without resizing. */
    data class Convert(
        val format: ImageFormat,
        val quality: Int = DEFAULT_QUALITY,
    ) : ImageJob {
        init {
            require(quality in 1..100) { "quality must be in 1..100" }
        }
    }

    /** Strip EXIF per [preset] ([customTags] only when preset is Custom), losslessly when possible. */
    data class ExifStrip(
        val preset: ExifRemovalPreset,
        val customTags: List<dev.tactos.core.images.exif.MetadataTag> = emptyList(),
    ) : ImageJob {
        init {
            require(preset != ExifRemovalPreset.Custom || customTags.isNotEmpty()) {
                "Custom preset needs an explicit tag list"
            }
        }
    }

    companion object {
        const val DEFAULT_QUALITY: Int = 90
    }
}
