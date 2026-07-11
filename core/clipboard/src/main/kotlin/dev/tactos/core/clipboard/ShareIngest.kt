package dev.tactos.core.clipboard

import android.content.Intent

/**
 * The share-to-tactos rung of the capture ladder: extracts shareable text
 * from an [Intent.ACTION_SEND] delivery. Pure inspection — persisting the
 * result is [ClipboardCapture]'s job.
 */
object ShareIngest {

    /**
     * Returns the shared text, trimmed, or null when [intent] is not a
     * non-blank `text/*` [Intent.ACTION_SEND] share.
     */
    fun textFrom(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val type = intent.type ?: return null
        if (!type.startsWith("text/")) return null
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: return null
        return text.trim().takeIf { it.isNotEmpty() }
    }
}
