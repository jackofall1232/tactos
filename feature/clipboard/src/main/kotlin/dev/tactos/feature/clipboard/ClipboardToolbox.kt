package dev.tactos.feature.clipboard

import dev.tactos.core.model.ClipAction
import dev.tactos.core.model.ClipType
import dev.tactos.core.model.ToolboxModule

/**
 * The clipboard toolbox — the first real [ToolboxModule]. The
 * ToolboxModule/ClipAction contracts have a consumer, so contract changes
 * are review-gated (CLAUDE.md section 6).
 *
 * Every clip action is declared as a [ClipAction] descriptor and executed
 * through the id-keyed executor registry — never as an ad-hoc capability
 * callback. This is a binding design constraint (.l00prite/memory.md): the
 * same descriptors later become the V2 AI workflow engine's model-invocable
 * tool registry. Timeline metadata operations (favorite, delete,
 * set-category) intentionally stay UI-level in v1 — CLAUDE.md section 3
 * scopes the universal actions to copy/share/pin — and must gain
 * descriptors before the V2 registry consumes this seam. All ids below are
 * stable API.
 */
object ClipboardToolbox : ToolboxModule {
    override val id: String = "clipboard"
    override val title: String = "Clipboard"
    override val description: String = "Smart clipboard timeline"
    override val emoji: String = "📋"
    override val order: Int = 0

    // Universal actions (any clip type).
    const val ACTION_COPY = "clipboard.copy"
    const val ACTION_SHARE = "clipboard.share"
    const val ACTION_PIN = "clipboard.pin"

    // Type-specific contextual actions.
    const val ACTION_URL_OPEN = "clipboard.url.open"
    const val ACTION_URL_QR = "clipboard.url.qr"
    const val ACTION_COLOR_CONVERT = "clipboard.color.convert"
    const val ACTION_JSON_VALIDATE = "clipboard.json.validate"
    const val ACTION_JSON_BEAUTIFY = "clipboard.json.beautify"
    const val ACTION_JSON_MINIFY = "clipboard.json.minify"

    /** The universal ids, in the order the detail sheet presents them. */
    val universalActionIds: Set<String> = setOf(ACTION_COPY, ACTION_SHARE, ACTION_PIN)

    override fun clipActions(): List<ClipAction> = listOf(
        ClipAction(ACTION_COPY, "Copy", id, ClipType.entries.toSet()),
        ClipAction(ACTION_SHARE, "Share", id, ClipType.entries.toSet()),
        ClipAction(ACTION_PIN, "Pin", id, ClipType.entries.toSet()),
        ClipAction(ACTION_URL_OPEN, "Open", id, setOf(ClipType.URL)),
        ClipAction(ACTION_URL_QR, "QR code", id, setOf(ClipType.URL)),
        ClipAction(ACTION_COLOR_CONVERT, "Convert", id, setOf(ClipType.COLOR)),
        ClipAction(ACTION_JSON_VALIDATE, "Validate", id, setOf(ClipType.JSON)),
        ClipAction(ACTION_JSON_BEAUTIFY, "Beautify", id, setOf(ClipType.JSON)),
        ClipAction(ACTION_JSON_MINIFY, "Minify", id, setOf(ClipType.JSON)),
    )
}

/** Presentation glyph per clip type (feature-layer concern, not contract). */
val ClipType.emoji: String
    get() = when (this) {
        ClipType.URL -> "🔗"
        ClipType.IP_ADDRESS -> "🌐"
        ClipType.COLOR -> "🎨"
        ClipType.JSON -> "🧩"
        ClipType.EMAIL -> "✉️"
        ClipType.PHONE -> "📞"
        ClipType.TEXT -> "📝"
    }
