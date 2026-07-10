package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * A deterministic, offline content-type detector for a single [ClipType].
 *
 * Each detector receives the already-trimmed full clipboard string and decides
 * whether that whole string is an instance of its [type]. Detectors never scan
 * for or extract substrings — a value either is the type in its entirety or it
 * is not. Detection is pure: no I/O, no randomness, no shared state.
 */
interface TypeDetector {
    /** The content type this detector recognizes. */
    val type: ClipType

    /**
     * Returns true iff [text] — the trimmed full string — is a value of [type].
     *
     * @param text the trimmed clipboard content; decided on in full.
     */
    fun matches(text: String): Boolean
}
