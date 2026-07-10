package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [UrlDetector], derived strictly from the
 * core/detect specification. Each case names itself in the assertion message.
 */
class UrlDetectorTest {

    @Test
    fun `declares URL type`() {
        assertEquals(ClipType.URL, UrlDetector.type)
    }

    @Test
    fun `accepts valid URLs`() {
        val accepted = listOf(
            "http://example.com",
            "https://example.com",
            "ftp://example.com",
            "ftps://example.com",
            "HTTP://example.com",              // scheme is case-insensitive
            "HtTpS://Example.COM",
            "http://localhost",                // localhost + scheme needs no dot
            "http://localhost:8080",
            "http://localhost/path",
            "https://localhost:3000/a?b=c#d",
            "http://example.com:8080",
            "http://example.com:1",            // min port
            "http://example.com:65535",        // max port
            "http://example.com/",
            "http://example.com/path",
            "http://example.com/a/b/c",
            "http://example.com/path?query=1",
            "http://example.com/path?query=1#frag",
            "http://example.com/#top",
            "https://sub.domain.example.co.uk",
            "http://ex-ample.com",             // hyphen mid-label
            "http://123.example.com",          // numeric label
            "http://example.com/p?a=1&b=2#s",
            "http://EXAMPLE.COM",
            "www.example.com",                 // www. branch, no scheme
            "www.example.com/path",
            "www.example.co.uk",
            "ftp://files.example.org/file.txt",
        )
        for (case in accepted) {
            assertTrue(UrlDetector.matches(case), "expected URL to match: <$case>")
        }
    }

    @Test
    fun `rejects non-URLs`() {
        val rejected = listOf(
            "http://",                         // empty host
            "https://",
            "ftp://",
            "ftps://",
            "HTTP://",                         // empty host, any case
            "http:// example.com",             // whitespace inside
            "http://exa mple.com",
            "http://example.com/pa th",        // whitespace in path
            " http://example.com",             // leading whitespace
            "http://example.com ",             // trailing whitespace
            "example.com",                     // bare host, no scheme or www.
            "localhost",                       // bare, no scheme
            "mailto:x@y.com",                  // other scheme is not a URL here
            "httpx://example.com",             // unknown scheme
            "http:/example.com",               // single slash
            "http:example.com",
            "//example.com",
            "www.",                            // empty host remainder
            "www .example.com",                // whitespace
            "http://-example.com",             // label leading hyphen
            "http://example-.com",             // label trailing hyphen
            "http://example",                  // no dot, not localhost
            "http://example..com",             // empty label
            "http://.example.com",             // leading dot
            "http://example.com.",             // trailing dot
            "http://example.com:0",            // port 0
            "http://example.com:65536",        // port out of range
            "http://example.com:99999",
            "http://example.com:abc",          // non-numeric port
            "http://user@example.com",         // '@' not a valid host char
            "",
            "just some text",
        )
        for (case in rejected) {
            assertFalse(UrlDetector.matches(case), "expected NON-URL: <$case>")
        }
    }
}
