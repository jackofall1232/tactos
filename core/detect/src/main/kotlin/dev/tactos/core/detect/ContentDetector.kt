package dev.tactos.core.detect

import dev.tactos.core.model.ClipType

/**
 * Classifies a clipboard string into a single [ClipType].
 *
 * The input is trimmed first; a blank or empty string is [ClipType.TEXT].
 * Otherwise the trimmed string is offered to each detector in a fixed priority
 * order — URL, EMAIL, IP_ADDRESS, COLOR, JSON, PHONE — and the first match
 * wins. When no detector matches, the result is [ClipType.TEXT].
 */
object ContentDetector {

    /** Detectors in the exact priority order the spec mandates. */
    private val detectors: List<TypeDetector> = listOf(
        UrlDetector,
        EmailDetector,
        IpAddressDetector,
        ColorDetector,
        JsonDetector,
        PhoneDetector,
    )

    /**
     * Detects the type of [text], trimming it first and falling back to
     * [ClipType.TEXT] for blank input or when no detector matches.
     */
    fun detect(text: String): ClipType {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ClipType.TEXT
        for (detector in detectors) {
            if (detector.matches(trimmed)) return detector.type
        }
        return ClipType.TEXT
    }
}
