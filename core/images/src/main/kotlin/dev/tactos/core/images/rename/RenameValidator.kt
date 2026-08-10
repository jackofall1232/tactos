package dev.tactos.core.images.rename

import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface RenameValidationError {
    /** Pattern is blank — every file would get an empty name. */
    data object BlankPattern : RenameValidationError

    /** `{token}` isn't one of the supported tokens. */
    data class UnknownToken(val token: String) : RenameValidationError

    /** A literal part contains a character illegal in file names. */
    data class IllegalCharacter(val char: Char) : RenameValidationError

    /** `{date:...}` argument isn't a valid java.time pattern. */
    data class BadDateFormat(val format: String) : RenameValidationError

    /**
     * The pattern resolves identically for every file of a multi-file batch
     * (no {n}/{name}/{rand} and no time-varying token) — outputs would collide.
     */
    data object NotUniquePerFile : RenameValidationError
}

object RenameValidator {

    /**
     * Validate [pattern] for a batch of [batchSize] files. Empty result =
     * valid. Errors are cumulative so the UI can show them all at once.
     */
    fun validate(pattern: String, batchSize: Int = 1): List<RenameValidationError> {
        if (pattern.isBlank()) return listOf(RenameValidationError.BlankPattern)

        val errors = mutableListOf<RenameValidationError>()
        val tokens = RenamePattern.TOKEN.findAll(pattern).map { it.groupValues[1] }.toList()

        for (body in tokens) {
            val name = RenamePattern.tokenName(body)
            if (name !in RenamePattern.KNOWN_TOKENS) {
                errors += RenameValidationError.UnknownToken(name)
                continue
            }
            if (name == "date") {
                val arg = RenamePattern.tokenArg(body)
                if (arg != null && arg.isNotBlank() &&
                    runCatching { DateTimeFormatter.ofPattern(arg, Locale.US) }.isFailure
                ) {
                    errors += RenameValidationError.BadDateFormat(arg)
                }
            }
        }

        // Check what the pattern actually resolves to, not just its literal
        // text — a {date:yyyy/MM/dd} format smuggles '/' into the file name.
        val probe = RenameResolver(
            nowMillis = PROBE_MILLIS,
            zoneId = java.time.ZoneOffset.UTC,
            random = kotlin.random.Random(0),
        ).resolve(pattern, RenameInput(originalName = "sample", index = 0, width = 100, height = 100))
        for (char in RenamePattern.ILLEGAL_CHARS) {
            if (char in probe) errors += RenameValidationError.IllegalCharacter(char)
        }

        if (batchSize > 1) {
            val names = tokens.map { RenamePattern.tokenName(it) }
            val differentiating = names.any { it == "n" || it == "name" || it == "rand" }
            if (!differentiating) errors += RenameValidationError.NotUniquePerFile
        }

        return errors
    }

    // Fixed probe instant (2026-01-15T12:30:45Z): every date field is
    // two-or-more digits, so format separators surface in the probe output.
    private const val PROBE_MILLIS = 1_768_480_245_000L
}
