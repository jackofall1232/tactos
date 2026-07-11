package dev.tactos.feature.clipboard.actions

import dev.tactos.core.actions.ColorValue
import dev.tactos.core.actions.JsonToolResult
import dev.tactos.core.actions.JsonTools
import dev.tactos.core.model.ClipItem
import dev.tactos.feature.clipboard.ClipboardToolbox

/** Maps a clip to the [ActionEffect] its action produces. Pure — no side effects. */
typealias ClipActionExecutor = (ClipItem) -> ActionEffect

/**
 * The app-side executor registry, keyed by [ClipAction] id. Every descriptor
 * [ClipboardToolbox.clipActions] declares has exactly one executor here —
 * [ClipActionExecutorsTest] enforces the totality.
 */
object ClipActionExecutors {

    /** Mirrors UrlDetector's scheme list in core/detect. */
    private val WEB_SCHEMES = listOf("http://", "https://", "ftp://", "ftps://")

    fun forId(actionId: String): ClipActionExecutor? = executors[actionId]

    private val executors: Map<String, ClipActionExecutor> = mapOf(
        ClipboardToolbox.ACTION_COPY to { item -> ActionEffect.CopyText(item.text) },
        ClipboardToolbox.ACTION_SHARE to { item -> ActionEffect.ShareText(item.text) },
        ClipboardToolbox.ACTION_PIN to { _ -> ActionEffect.TogglePin },
        ClipboardToolbox.ACTION_URL_OPEN to { item ->
            // UrlDetector accepts scheme-less "www." URLs, but ACTION_VIEW
            // resolves nothing without a scheme — default to https.
            val text = item.text.trim()
            val url = if (WEB_SCHEMES.any { text.startsWith(it, ignoreCase = true) }) {
                text
            } else {
                "https://$text"
            }
            ActionEffect.OpenUrl(url)
        },
        ClipboardToolbox.ACTION_URL_QR to { item -> ActionEffect.ShowQr(item.text.trim()) },
        ClipboardToolbox.ACTION_COLOR_CONVERT to { item ->
            val color = ColorValue.parse(item.text)
            if (color != null) {
                ActionEffect.ShowColor(color)
            } else {
                ActionEffect.ShowTextResult(
                    title = "Convert color",
                    body = "Couldn't parse this clip as a color.",
                    isError = true,
                )
            }
        },
        ClipboardToolbox.ACTION_JSON_VALIDATE to { item ->
            jsonEffect("Validate JSON", JsonTools.validate(item.text), savable = false)
        },
        ClipboardToolbox.ACTION_JSON_BEAUTIFY to { item ->
            jsonEffect("Beautified JSON", JsonTools.beautify(item.text), savable = true)
        },
        ClipboardToolbox.ACTION_JSON_MINIFY to { item ->
            jsonEffect("Minified JSON", JsonTools.minify(item.text), savable = true)
        },
    )

    private fun jsonEffect(
        title: String,
        result: JsonToolResult,
        savable: Boolean,
    ): ActionEffect = when (result) {
        is JsonToolResult.Success -> ActionEffect.ShowTextResult(
            title = title,
            body = result.text,
            savable = savable,
        )
        is JsonToolResult.Failure -> ActionEffect.ShowTextResult(
            title = title,
            body = result.message,
            isError = true,
        )
    }
}
