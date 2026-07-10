package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [PhoneDetector]. After stripping spaces,
 * hyphens, dots, and parens the remainder must be an optional '+' plus 7-15
 * digits, AND the original must carry a '+' or at least one separator — so a
 * bare digit run stays TEXT.
 */
class PhoneDetectorTest {

    @Test
    fun `declares PHONE type`() {
        assertEquals(ClipType.PHONE, PhoneDetector.type)
    }

    @Test
    fun `accepts valid phone numbers`() {
        val accepted = listOf(
            "+15551234567",
            "(555) 123-4567",
            "555-123-4567",
            "555.123.4567",
            "+1 555 123 4567",
            "123 4567",                        // 7 digits, has a space separator
            "+1234567",                        // '+' plus 7 digits
            "+123456789012345",                // '+' plus 15 digits (max)
            "(123) 456-7890",
            "1-2-3-4-5-6-7",
            "555 1234",
            "+1-555-123-4567",
            "+44 20 7946 0958",
            "1.234.567",                       // 7 digits, has '.' separator
            "12.34.56.78",                     // 8 digits, has '.' separator
            "192.168.0.1",                     // 8 digits + '.' -> matches PHONE in isolation
        )
        for (case in accepted) {
            assertTrue(PhoneDetector.matches(case), "expected PHONE to match: <$case>")
        }
    }

    @Test
    fun `rejects non-phone-numbers`() {
        val rejected = listOf(
            "12345678",                        // bare digits, no '+' or separator
            "1234567",                         // bare 7 digits
            "123-456",                         // only 6 digits
            "+123456",                         // '+' but 6 digits
            "+1234567890123456",               // '+' plus 16 digits (too many)
            "1234567890123456",                // bare 16 digits
            "+1 (555) 123-45678901234",        // 18 digits total (too many)
            "123456789012345678",              // 18 bare digits
            "phone",                           // no digits
            "555-CALL-NOW",                    // letters remain after strip
            "++15551234567",                   // two '+'
            "15551234567+",                    // '+' not leading
            "+1555+1234567",                   // interior '+'
            "123-456-789a",                    // trailing letter
            "+",                               // no digits
            "()",                              // no digits
            "1.2.3.4",                         // only 4 digits after strip
            "999999",                          // 6 bare digits
            "",
            "   ",                             // whitespace only
        )
        for (case in rejected) {
            assertFalse(PhoneDetector.matches(case), "expected NON-PHONE: <$case>")
        }
    }
}
