package dev.tactos.app

import dev.tactos.core.model.ModuleRegistry
import dev.tactos.core.model.ToolboxModule

/**
 * Placeholder registration for the clipboard toolbox so the home grid runs
 * through the real [ModuleRegistry] seam from day one. The `feature/clipboard`
 * module replaces this object when the timeline UI lands; its stable id
 * ("clipboard") must not change.
 */
object ClipboardToolboxPlaceholder : ToolboxModule {
    override val id: String = "clipboard"
    override val title: String = "Clipboard"
    override val description: String = "Smart clipboard timeline"
    override val emoji: String = "📋"
    override val order: Int = 0
}

/** The app's module registry. Later toolboxes and plugins register here. */
fun appModuleRegistry(): ModuleRegistry = ModuleRegistry(
    listOf(
        ClipboardToolboxPlaceholder,
    ),
)
