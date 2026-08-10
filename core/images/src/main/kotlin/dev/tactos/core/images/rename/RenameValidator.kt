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

        val literal = RenamePattern.TOKEN.replace(pattern, "")
        for (char in RenamePattern.ILLEGAL_CHARS) {
            if (char in literal) errors += RenameValidationError.IllegalCharacter(char)
        }

        if (batchSize > 1) {
            val names = tokens.map { RenamePattern.tokenName(it) }
            val differentiating = names.any { it == "n" || it == "name" || it == "rand" }
            if (!differentiating) errors += RenameValidationError.NotUniquePerFile
        }

        return errors
    }
}
