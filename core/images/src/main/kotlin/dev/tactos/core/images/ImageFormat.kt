package dev.tactos.core.images

/**
 * Output formats the image toolbox can encode. v1 sticks to what the Android
 * platform encodes natively — AVIF/JXL would each be a new native dependency
 * and are deferred (see .l00prite/todos.md Phase 4 remainder).
 */
enum class ImageFormat(
    val mimeType: String,
    val extension: String,
    /** Whether the encoder honours a quality setting (PNG is lossless-only). */
    val supportsQuality: Boolean,
) {
    PNG("image/png", "png", supportsQuality = false),
    JPEG("image/jpeg", "jpg", supportsQuality = true),
    WEBP("image/webp", "webp", supportsQuality = true),
    ;

    companion object {
        /** Case-insensitive match on MIME type; null for anything unsupported. */
        fun fromMimeType(mimeType: String?): ImageFormat? =
            entries.firstOrNull { it.mimeType.equals(mimeType?.trim(), ignoreCase = true) }

        /** Match on a bare file extension ("jpg", "jpeg", "png"...); null if unsupported. */
        fun fromExtension(extension: String?): ImageFormat? {
            val cleaned = extension?.trim()?.removePrefix(".")?.lowercase() ?: return null
            if (cleaned == "jpeg") return JPEG
            return entries.firstOrNull { it.extension == cleaned }
        }
    }
}
