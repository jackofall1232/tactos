package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [ContentDetector]: it trims first, treats blank/empty as TEXT, then
 * applies detectors in the exact order URL, EMAIL, IP_ADDRESS, COLOR, JSON,
 * PHONE, falling back to TEXT. The priority-collision cases (an IPv4 address
 * also strips to a PHONE-shaped digit run) are the point of this suite.
 */
class ContentDetectorTest {

    @Test
    fun `blank and whitespace-only input is TEXT`() {
        val blanks = listOf("", " ", "   ", "\t", "\n", "\t\n  ", "  \r\n\t ")
        for (case in blanks) {
            assertEquals(ClipType.TEXT, ContentDetector.detect(case), "blank -> TEXT: <$case>")
        }
    }

    @Test
    fun `detects each type and resolves ordering collisions`() {
        val cases: List<Pair<String, ClipType>> = listOf(
            // URL wins first
            "http://example.com" to ClipType.URL,
            "  http://example.com  " to ClipType.URL,           // trimmed before detection
            "\nhttps://example.com/path\t" to ClipType.URL,
            "www.example.com" to ClipType.URL,
            // EMAIL (an email is not a URL)
            "user@example.com" to ClipType.EMAIL,
            "  user.name@example.co.uk " to ClipType.EMAIL,
            // IP_ADDRESS beats PHONE for dotted-decimal digit runs
            "192.168.0.1" to ClipType.IP_ADDRESS,              // would also strip to a phone
            "255.255.255.255" to ClipType.IP_ADDRESS,          // 12 digits after strip
            "12.34.56.78" to ClipType.IP_ADDRESS,
            "0.0.0.0" to ClipType.IP_ADDRESS,
            "1.2.3.4" to ClipType.IP_ADDRESS,
            "::1" to ClipType.IP_ADDRESS,
            "2001:db8::1" to ClipType.IP_ADDRESS,
            // COLOR
            "#ff8800" to ClipType.COLOR,
            "#abc" to ClipType.COLOR,
            "rgb(0,0,0)" to ClipType.COLOR,
            "rgba(0,0,0,0.5)" to ClipType.COLOR,
            "  #FFFFFF  " to ClipType.COLOR,
            // JSON
            """{"a":1}""" to ClipType.JSON,
            "[1,2,3]" to ClipType.JSON,
            "  {}  " to ClipType.JSON,
            "  [true, false, null]  " to ClipType.JSON,
            // PHONE (only reached when nothing above matches)
            "+15551234567" to ClipType.PHONE,
            "(555) 123-4567" to ClipType.PHONE,
            "  555-123-4567 " to ClipType.PHONE,
            // TEXT fallback
            "hello world" to ClipType.TEXT,
            "42" to ClipType.TEXT,                             // JSON rejects primitives
            "true" to ClipType.TEXT,
            "3.14" to ClipType.TEXT,
            "\"quoted\"" to ClipType.TEXT,
            "user@localhost" to ClipType.TEXT,                 // no TLD -> not EMAIL
            "12345678" to ClipType.TEXT,                       // bare digits -> not PHONE
            "mailto:x@y.com" to ClipType.TEXT,                 // other scheme -> not URL
            "localhost" to ClipType.TEXT,                      // bare host
            "not a url http://x" to ClipType.TEXT,             // whitespace kills the URL
            "999.1.1.1" to ClipType.TEXT,                      // invalid IPv4, digits < 7
            "#ggg" to ClipType.TEXT,                           // invalid color
            "rgb(256,0,0)" to ClipType.TEXT,                   // invalid color
            """{"a":}""" to ClipType.TEXT,                     // invalid JSON
        )
        for ((input, expected) in cases) {
            assertEquals(expected, ContentDetector.detect(input), "detect(<$input>)")
        }
    }
}
