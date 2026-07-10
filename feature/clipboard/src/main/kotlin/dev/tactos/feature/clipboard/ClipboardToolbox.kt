package dev.tactos.feature.clipboard

import dev.tactos.core.model.ClipAction
import dev.tactos.core.model.ClipType
import dev.tactos.core.model.ToolboxModule

/**
 * The clipboard toolbox — the first real [ToolboxModule]. From this unit on,
 * the ToolboxModule/ClipAction contracts have a consumer, so contract changes
 * are review-gated (CLAUDE.md section 6).
 *
 * The universal actions are declared as descriptors even though the timeline
 * UI currently binds copy/share/pin directly: stable action ids
 * ("clipboard.copy", "clipboard.share", "clipboard.pin") seed the app-side
 * executor registry that the contextual-actions unit — and later the V2 AI
 * workflow engine — will consume.
 */
object ClipboardToolbox : ToolboxModule {
    override val id: String = "clipboard"
    override val title: String = "Clipboard"
    override val description: String = "Smart clipboard timeline"
    override val emoji: String = "📋"
    override val order: Int = 0

    override fun clipActions(): List<ClipAction> = listOf(
        ClipAction("clipboard.copy", "Copy", id, ClipType.entries.toSet()),
        ClipAction("clipboard.share", "Share", id, ClipType.entries.toSet()),
        ClipAction("clipboard.pin", "Pin", id, ClipType.entries.toSet()),
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
