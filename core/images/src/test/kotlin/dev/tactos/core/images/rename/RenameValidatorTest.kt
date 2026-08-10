package dev.tactos.core.images.rename

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class RenameValidatorTest {

    @Test
    fun `valid patterns produce no errors`() {
        val patterns = listOf(
            "{name}",
            "trip-{n:3}",
            "{date:yyyy-MM-dd} {name}",
            "{rand:8}",
            "photo {n} of many",
        )
        for (pattern in patterns) {
            assertEquals(emptyList(), RenameValidator.validate(pattern, batchSize = 5), pattern)
        }
    }

    @Test
    fun `blank pattern is rejected`() {
        assertEquals(listOf(RenameValidationError.BlankPattern), RenameValidator.validate("  "))
    }

    @Test
    fun `unknown tokens are reported by name`() {
        val errors = RenameValidator.validate("{name}-{foo}-{bar:3}")
        assertTrue(RenameValidationError.UnknownToken("foo") in errors)
        assertTrue(RenameValidationError.UnknownToken("bar") in errors)
    }

    @Test
    fun `illegal filename characters in literals are reported`() {
        val errors = RenameValidator.validate("a/b:c")
        assertTrue(RenameValidationError.IllegalCharacter('/') in errors)
        assertTrue(RenameValidationError.IllegalCharacter(':') in errors)
    }

    @Test
    fun `colon inside a token is not an illegal character`() {
        assertEquals(emptyList(), RenameValidator.validate("{n:3}"))
    }

    @Test
    fun `illegal characters smuggled through a date format are caught`() {
        val errors = RenameValidator.validate("{date:yyyy/MM/dd}-{n}")
        assertTrue(RenameValidationError.IllegalCharacter('/') in errors, "$errors")
        // A clean custom format still passes.
        assertEquals(emptyList(), RenameValidator.validate("{date:yyyy-MM-dd}-{n}"))
    }

    @Test
    fun `bad date formats are reported`() {
        val errors = RenameValidator.validate("{date:qqqq-invalid}")
        assertTrue(errors.any { it is RenameValidationError.BadDateFormat })
    }

    @Test
    fun `non-differentiating pattern fails only for multi-file batches`() {
        val pattern = "{date:yyyy}" // same value for every file in a batch
        assertEquals(emptyList(), RenameValidator.validate(pattern, batchSize = 1))
        assertTrue(
            RenameValidationError.NotUniquePerFile in RenameValidator.validate(pattern, batchSize = 2),
        )
        // {n} differentiates.
        assertEquals(emptyList(), RenameValidator.validate("{date:yyyy}-{n}", batchSize = 2))
    }

    @Test
    fun `errors accumulate`() {
        val errors = RenameValidator.validate("x|{nope}", batchSize = 3)
        assertTrue(errors.size >= 3, "$errors")
    }
}
