package dev.tactos.feature.clipboard.actions

import dev.tactos.core.actions.ColorValue

/**
 * What executing a clip action asks the host UI to do. Executors are pure
 * (clip in, effect out); every side effect — intents, clipboard writes,
 * dialogs — is interpreted by the UI layer. This keeps the action registry
 * data-driven and testable, per the V2 constraint in .l00prite/memory.md.
 */
sealed interface ActionEffect {
    /** Put [text] on the system clipboard. */
    data class CopyText(val text: String) : ActionEffect

    /** Open the system share sheet for [text]. */
    data class ShareText(val text: String) : ActionEffect

    /** Hand [url] to the browser (user-initiated — the app has no network). */
    data class OpenUrl(val url: String) : ActionEffect

    /** Toggle the clip's pinned state. */
    data object TogglePin : ActionEffect

    /** Show an offline QR code for [text]. */
    data class ShowQr(val text: String) : ActionEffect

    /** Show the color conversion dialog for [color]. */
    data class ShowColor(val color: ColorValue) : ActionEffect

    /** Show a text result (JSON output, verdicts, errors). */
    data class ShowTextResult(
        val title: String,
        val body: String,
        val isError: Boolean = false,
        /** Whether the UI should offer saving [body] as a new clip. */
        val savable: Boolean = false,
    ) : ActionEffect
}
