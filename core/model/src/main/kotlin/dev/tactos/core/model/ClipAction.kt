package dev.tactos.core.model

/**
 * A contextual action a toolbox module contributes for clipboard items of
 * particular [ClipType]s (e.g. "Generate QR" for URL, "Beautify" for JSON).
 *
 * This is a *descriptor*: identity, label, and applicability. Execution is
 * platform work (open a browser, share, render a QR) and is bound to the
 * descriptor's [id] by the app layer. Keeping the descriptor pure keeps this
 * contract usable by any future plugin, on or off the JVM UI thread.
 */
data class ClipAction(
    /** Stable unique id, namespaced by module, e.g. "clipboard.copy". */
    val id: String,
    /** Human-readable label shown in the timeline UI. */
    val label: String,
    /** The [ToolboxModule.id] of the contributing module. */
    val moduleId: String,
    /** The clip types this action applies to. */
    val appliesTo: Set<ClipType>,
) {
    init {
        require(id.isNotBlank()) { "ClipAction id must not be blank" }
        require(label.isNotBlank()) { "ClipAction label must not be blank" }
        require(appliesTo.isNotEmpty()) { "ClipAction must apply to at least one ClipType" }
    }

    fun appliesTo(item: ClipItem): Boolean = item.type in appliesTo
}
