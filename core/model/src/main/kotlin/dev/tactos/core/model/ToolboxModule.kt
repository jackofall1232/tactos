package dev.tactos.core.model

/**
 * The contract every tactos toolbox implements — and, later, the seed of the
 * third-party plugin API (treat changes as semi-public once consumed; see the
 * human review gates in CLAUDE.md section 6).
 *
 * Deliberately UI-free: modules describe identity and contributed actions
 * here; their screens are registered against [id] in the app layer, so this
 * module never depends on Android or Compose.
 */
interface ToolboxModule {
    /** Stable unique id, lowercase, e.g. "clipboard", "developer". */
    val id: String

    /** Display name shown on the home grid. */
    val title: String

    /** One-line description shown on the home grid. */
    val description: String

    /**
     * Emoji glyph for the home grid tile. A string (not a drawable) keeps the
     * contract UI-toolkit-neutral for future plugins.
     */
    val emoji: String
        get() = DEFAULT_EMOJI

    /** Sort weight on the home grid; lower comes first, ties break by [id]. */
    val order: Int
        get() = DEFAULT_ORDER

    /** Contextual actions this module contributes to the clipboard timeline. */
    fun clipActions(): List<ClipAction> = emptyList()

    companion object {
        const val DEFAULT_ORDER: Int = 100
        const val DEFAULT_EMOJI: String = "🧰"
    }
}
