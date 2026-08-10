package dev.tactos.feature.images.engine

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.exif.ExifRemovalPreset
import dev.tactos.core.images.exif.MetadataTag
import java.io.File

/**
 * Stream-level EXIF removal: copy the source document to a private cache
 * file, null the selected tags with androidx [ExifInterface] (lossless — no
 * re-encode, pixels untouched), and hand the cleaned bytes back. The source
 * document is never modified.
 */
class ExifStripper(private val context: Context) {

    class Result(
        val bytes: ByteArray,
        /** Tag keys that were present before stripping (for the UI's "removed N"). */
        val removedKeys: List<String>,
    )

    /**
     * Null when the source can't be read or the format can't carry EXIF that
     * [ExifInterface] can rewrite (callers fall back to re-encoding, which
     * drops metadata by construction).
     */
    fun strip(uri: Uri, job: ImageJob.ExifStrip): Result? {
        val temp = File.createTempFile("exif-strip", null, context.cacheDir)
        try {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
            }.getOrNull() ?: return null

            val tags = tagsFor(job)
            // ExifInterface throws on containers it can't parse (GIF, BMP...);
            // null routes the caller to the re-encode fallback.
            val exif = runCatching { ExifInterface(temp) }.getOrElse { return null }
            val present = tags.filter { runCatching { exif.getAttribute(it.key) }.getOrNull() != null }
            if (present.isNotEmpty()) {
                present.forEach { exif.setAttribute(it.key, null) }
                val saved = runCatching { exif.saveAttributes() }.isSuccess
                if (!saved) return null
            }
            return Result(bytes = temp.readBytes(), removedKeys = present.map { it.key })
        } finally {
            temp.delete()
        }
    }

    private fun tagsFor(job: ImageJob.ExifStrip): List<MetadataTag> = when (job.preset) {
        // Empty tags list means "everything removable" by preset contract.
        ExifRemovalPreset.AllMetadata -> MetadataTag.entries
        ExifRemovalPreset.Custom -> job.customTags
        else -> job.preset.tags
    }
}
