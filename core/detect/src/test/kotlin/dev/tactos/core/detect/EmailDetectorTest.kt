package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [EmailDetector], derived strictly from the
 * core/detect specification (regex plus the extra dot/hyphen structural rules).
 */
class EmailDetectorTest {

    @Test
    fun `declares EMAIL type`() {
        assertEquals(ClipType.EMAIL, EmailDetector.type)
    }

    @Test
    fun `accepts valid emails`() {
        val accepted = listOf(
            "user@example.com",
            "user.name@example.com",
            "user_name@example.com",           // underscore is in the local char set
            "user+tag@example.com",
            "user%test@example.com",
            "user-name@example.com",
            "u@example.com",                   // single-char local part
            "user@sub.example.com",
            "user@example.co.uk",
            "123@example.com",                 // numeric local part
            "a.b.c@example.com",               // interior dots, none consecutive
            "user@ex-ample.com",               // hyphen mid domain-label
            "USER@EXAMPLE.COM",
            "user@example.museum",             // long TLD
            "first.last+tag%x-y@sub.example.co",
        )
        for (case in accepted) {
            assertTrue(EmailDetector.matches(case), "expected EMAIL to match: <$case>")
        }
    }

    @Test
    fun `rejects non-emails`() {
        val rejected = listOf(
            "user@example",                    // no TLD dot
            "user@example.c",                  // TLD too short (<2)
            "user@example.co1",                // TLD must be letters only
            "user@example.",                   // nothing after final dot
            "userexample.com",                 // no @
            "@example.com",                    // empty local part
            "user@",                           // empty domain
            "user@.com",                       // domain starts with dot
            "user@example..com",               // ".." in domain
            "user@-example.com",               // domain label leading hyphen
            "user@example-.com",               // domain label trailing hyphen
            ".user@example.com",               // local starts with '.'
            "user.@example.com",               // local ends with '.'
            "a..b@example.com",                // ".." in local
            "user..name@example.com",
            "user@@example.com",               // double @
            "user @example.com",               // whitespace
            "user@exam ple.com",
            "user@example.com ",               // trailing whitespace
            " user@example.com",               // leading whitespace
            "user@example.com.",               // trailing dot after TLD
            "us\u00e9r@example.com",           // non-ASCII in local part
            "user@localhost",                  // no TLD
            "plainaddress",
            "user#test@example.com",           // '#' not a valid local char
            "user@ex_ample.com",               // underscore not a valid domain char
            "",
        )
        for (case in rejected) {
            assertFalse(EmailDetector.matches(case), "expected NON-EMAIL: <$case>")
        }
    }
}
