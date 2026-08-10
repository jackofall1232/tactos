package dev.tactos.feature.images

import dev.tactos.core.model.ClipAction
import dev.tactos.core.model.ToolboxModule

/**
 * The image toolbox — the second [ToolboxModule], and the proof that the
 * module seam generalizes beyond the clipboard.
 *
 * It contributes no [ClipAction]s yet: the timeline is text-only until
 * `ClipType.IMAGE` exists, and adding that is a review-gated core/model
 * contract change (documented in .l00prite/todos.md). Its capabilities are
 * still declared as data — [ImageTool] descriptors drive the tool list UI
 * and, later, the V2 workflow registry (binding constraint,
 * .l00prite/memory.md).
 */
object ImagesToolbox : ToolboxModule {
    override val id: String = "images"
    override val title: String = "Images"
    override val description: String = "Resize, convert, compress, clean"
    override val emoji: String = "🖼️"
    override val order: Int = 10

    const val TOOL_RESIZE = "images.resize"
    const val TOOL_COMPRESS = "images.compress"
    const val TOOL_CONVERT = "images.convert"
    const val TOOL_EXIF = "images.exif_strip"

    /**
     * The tool catalog, in presentation order. Batch rename is not a tool of
     * its own in v1 — output naming (a rename pattern) is part of every
     * tool's save step, where SAF actually assigns names.
     */
    val tools: List<ImageTool> = listOf(
        ImageTool(
            id = TOOL_RESIZE,
            label = "Resize",
            description = "Fit within bounds, exact size, or scale by percent",
            emoji = "📐",
        ),
        ImageTool(
            id = TOOL_COMPRESS,
            label = "Compress",
            description = "Shrink to a quality level or a target file size",
            emoji = "🗜️",
        ),
        ImageTool(
            id = TOOL_CONVERT,
            label = "Convert",
            description = "PNG, JPEG, or WebP",
            emoji = "🔄",
        ),
        ImageTool(
            id = TOOL_EXIF,
            label = "Remove metadata",
            description = "Strip location, dates, and device info (EXIF)",
            emoji = "🧹",
        ),
    )

    fun toolById(id: String): ImageTool? = tools.firstOrNull { it.id == id }

    override fun clipActions(): List<ClipAction> = emptyList()
}

/**
 * A declarative image-tool descriptor. [risk] feeds the V2 engine's
 * auto-run policy: every v1 tool is [Risk.LOW] because outputs are written
 * to user-chosen new documents — originals are never modified in place.
 */
data class ImageTool(
    val id: String,
    val label: String,
    val description: String,
    val emoji: String,
    val risk: Risk = Risk.LOW,
) {
    enum class Risk { LOW, ALWAYS_CONFIRM }

    init {
        require(id.startsWith("images.")) { "ImageTool ids are namespaced under images." }
        require(label.isNotBlank() && description.isNotBlank())
    }
}
