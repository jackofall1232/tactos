package dev.tactos.app

import dev.tactos.core.model.ModuleRegistry
import dev.tactos.feature.clipboard.ClipboardToolbox
import dev.tactos.feature.images.ImagesToolbox

/** The app's module registry. Later toolboxes and plugins register here. */
fun appModuleRegistry(): ModuleRegistry = ModuleRegistry(
    listOf(
        ClipboardToolbox,
        ImagesToolbox,
    ),
)
