package dev.tactos.core.images.rename

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/**
 * Batch-rename pattern engine. Idea borrowed from ImageToolbox's
 * batch-rename tool (ADR-0004 tier 2); the token syntax is tactos's own,
 * chosen to be typeable and self-explanatory:
 *
 * - `{name}` — original file name (without extension)
 * - `{n}` — 1-based sequence number; `{n:3}` zero-pads to 3 digits
 * - `{date}` — capture date (falls back to "now") as yyyyMMdd-HHmmss;
 *   `{date:yyyy-MM-dd}` uses a custom java.time pattern
 * - `{w}` / `{h}` — pixel dimensions, empty when unknown
 * - `{rand}` — 4 random digits; `{rand:6}` — 6 digits
 *
 * Anything else in braces is an error surfaced by [RenameValidator]; text
 * outside braces is kept literally. The resolved value is a base name —
 * the save flow appends the extension for the chosen output format.
 */
object RenamePattern {
    val TOKEN = Regex("""\{([^{}]*)\}""")

    const val DEFAULT_DATE_FORMAT = "yyyyMMdd-HHmmss"
    const val DEFAULT_RANDOM_DIGITS = 4
    const val MAX_RANDOM_DIGITS = 64
    const val MAX_SEQUENCE_PAD = 10

    /** Characters that can never appear in a resolved file name. */
    val ILLEGAL_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    val KNOWN_TOKENS = setOf("name", "n", "date", "w", "h", "rand")

    internal fun tokenName(body: String): String = body.substringBefore(':').trim()

    internal fun tokenArg(body: String): String? =
        if (':' in body) body.substringAfter(':') else null
}

/** Everything the resolver may substitute for one file in a batch. */
data class RenameInput(
    /** Original file name without extension ("" when unknown). */
    val originalName: String,
    /** 0-based position in the batch. */
    val index: Int,
    /** Capture/modified time, or null to fall back to the resolver's clock. */
    val takenAtMillis: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
)

/**
 * Resolves a validated pattern for each file of a batch. Deterministic by
 * construction: the clock and random source are injected.
 */
class RenameResolver(
    private val nowMillis: Long,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val random: Random = Random.Default,
) {

    fun resolve(pattern: String, input: RenameInput): String =
        RenamePattern.TOKEN.replace(pattern) { match ->
            substitute(RenamePattern.tokenName(match.groupValues[1]),
                RenamePattern.tokenArg(match.groupValues[1]), input)
        }.trim()

    private fun substitute(name: String, arg: String?, input: RenameInput): String = when (name) {
        "name" -> input.originalName
        "n" -> {
            val pad = arg?.toIntOrNull()?.coerceIn(0, RenamePattern.MAX_SEQUENCE_PAD) ?: 0
            (input.index + 1).toString().padStart(pad, '0')
        }
        "date" -> formatDate(arg, input.takenAtMillis ?: nowMillis)
        "w" -> input.width?.toString().orEmpty()
        "h" -> input.height?.toString().orEmpty()
        "rand" -> {
            val digits = arg?.toIntOrNull()
                ?.coerceIn(1, RenamePattern.MAX_RANDOM_DIGITS)
                ?: RenamePattern.DEFAULT_RANDOM_DIGITS
            buildString { repeat(digits) { append(random.nextInt(10)) } }
        }
        // Validation rejects unknown tokens; if one slips through, keep it
        // visible rather than silently dropping user input.
        else -> "{$name}"
    }

    private fun formatDate(pattern: String?, millis: Long): String {
        val format = pattern?.takeIf { it.isNotBlank() } ?: RenamePattern.DEFAULT_DATE_FORMAT
        val formatter = runCatching { DateTimeFormatter.ofPattern(format, Locale.US) }
            .getOrElse { DateTimeFormatter.ofPattern(RenamePattern.DEFAULT_DATE_FORMAT, Locale.US) }
        return formatter.format(Instant.ofEpochMilli(millis).atZone(zoneId))
    }
}
