package dev.tactos.core.clipboard

import android.content.ClipboardManager
import android.content.Context
import dev.tactos.core.detect.ContentDetector
import dev.tactos.core.model.ClipItem

/**
 * The capture rungs that need no special access: reading the system
 * clipboard while tactos itself is focused (Android 10+ allows clipboard
 * reads only for the focused app), and persisting text handed over
 * explicitly (share target, manual add).
 *
 * Storage is injected as a plain suspend function so this module depends
 * only on `core/model`/`core/detect` — the app wires it to
 * `ClipRepository::save`, whose consecutive-duplicate dedup makes repeated
 * captures of the same content idempotent.
 */
class ClipboardCapture(private val store: suspend (ClipItem) -> Long) {

    sealed interface Result {
        /** The clip was persisted (new row, or the collapsed duplicate's id). */
        data class Saved(val id: Long) : Result

        /** The source app flagged the clip sensitive; nothing was stored. */
        data object SkippedSensitive : Result

        /** Empty clipboard, non-text clip, or blank text; nothing was stored. */
        data object NothingToCapture : Result
    }

    /**
     * Reads the current primary clip and persists its text. Call only while
     * the app holds window focus — earlier, the system returns no clip.
     */
    suspend fun captureCurrent(context: Context, sourceHint: String? = null): Result {
        val manager = context.getSystemService(ClipboardManager::class.java)
            ?: return Result.NothingToCapture
        val clip = manager.primaryClip ?: return Result.NothingToCapture
        if (SensitiveClips.isSensitive(clip.description)) return Result.SkippedSensitive
        if (clip.itemCount == 0) return Result.NothingToCapture
        val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
        if (text.isNullOrEmpty()) return Result.NothingToCapture
        return persist(text, sourceHint)
    }

    /** Persists explicitly provided text (share target, manual add). */
    suspend fun saveText(text: String, sourceHint: String? = null): Result {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.NothingToCapture
        return persist(trimmed, sourceHint)
    }

    private suspend fun persist(text: String, sourceHint: String?): Result {
        val id = store(
            ClipItem(
                text = text,
                type = ContentDetector.detect(text),
                createdAt = System.currentTimeMillis(),
                sourceApp = sourceHint,
            ),
        )
        return Result.Saved(id)
    }
}
