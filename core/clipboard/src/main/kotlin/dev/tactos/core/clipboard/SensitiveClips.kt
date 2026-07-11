package dev.tactos.core.clipboard

import android.content.ClipDescription

/**
 * Recognizes clips the source app has flagged as sensitive (password
 * managers, one-time codes). Flagged clips are never stored — the privacy
 * spec in CLAUDE.md section 2 allows skip-or-mask, and tactos v1 chooses
 * skip: nothing sensitive ever touches the history database.
 */
object SensitiveClips {

    /**
     * The extras key Android 13 exposes as [ClipDescription.EXTRA_IS_SENSITIVE].
     * Using the literal key keeps the check working on older API levels, where
     * well-behaved apps already set the same extra.
     */
    const val EXTRA_IS_SENSITIVE: String = "android.content.extra.IS_SENSITIVE"

    fun isSensitive(description: ClipDescription?): Boolean =
        description?.extras?.getBoolean(EXTRA_IS_SENSITIVE, false) ?: false
}
