package dev.tactos.core.model

import java.security.MessageDigest

/**
 * One entry in the clipboard timeline. Pure data — persistence mapping lives
 * in `core/database`, capture in `core/clipboard`.
 *
 * [contentHash] is the dedup key: consecutive captures with the same hash are
 * collapsed rather than stored twice. Always produce it with [contentHashOf]
 * so every layer agrees on the algorithm.
 */
data class ClipItem(
    val id: Long = 0L,
    val text: String,
    val type: ClipType,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val category: String? = null,
    val sourceApp: String? = null,
    val contentHash: String = contentHashOf(text),
)

/** SHA-256 of the item text, lowercase hex. Stable across platforms. */
fun contentHashOf(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
