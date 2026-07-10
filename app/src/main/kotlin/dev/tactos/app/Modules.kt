package dev.tactos.app

import dev.tactos.core.model.ModuleRegistry
import dev.tactos.feature.clipboard.ClipboardToolbox

/** The app's module registry. Later toolboxes and plugins register here. */
fun appModuleRegistry(): ModuleRegistry = ModuleRegistry(
    listOf(
        ClipboardToolbox,
    ),
)
