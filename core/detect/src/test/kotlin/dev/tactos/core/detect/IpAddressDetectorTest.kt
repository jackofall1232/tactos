package dev.tactos.core.detect

import dev.tactos.core.model.ClipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Adversarial, table-driven tests for [IpAddressDetector] covering IPv4 and the
 * standard IPv6 textual forms (compression, embedded IPv4 tail, group counts).
 */
class IpAddressDetectorTest {

    @Test
    fun `declares IP_ADDRESS type`() {
        assertEquals(ClipType.IP_ADDRESS, IpAddressDetector.type)
    }

    @Test
    fun `accepts valid IPv4 and IPv6`() {
        val accepted = listOf(
            // IPv4
            "0.0.0.0",
            "255.255.255.255",
            "192.168.0.1",
            "8.8.8.8",
            "127.0.0.1",
            "1.2.3.4",
            "10.0.0.255",
            "0.1.2.3",                         // single-zero octets are allowed
            "100.64.0.1",
            "12.34.56.78",
            // IPv6
            "::1",
            "::",
            "fe80::",                          // trailing compression
            "2001:db8::1",
            "2001:0db8:0000:0000:0000:0000:0000:0001",
            "2001:db8:0:0:0:0:0:1",
            "fe80::1",
            "::ffff:192.168.0.1",              // embedded IPv4 tail
            "2001:db8::192.168.0.1",
            "0:0:0:0:0:0:0:0",
            "abcd:ef01:2345:6789:abcd:ef01:2345:6789",
            "1::",
            "1::8",
            "1:2:3:4:5:6:7:8",
            "::ffff:0:0",
            "fe80::abc:def",
            "2001:DB8::1",                     // uppercase hex
            "64:ff9b::192.0.2.33",
            "1:2:3:4:5:6:1.2.3.4",             // 6 hex + embedded IPv4 = 8 groups
            "2001:0db8:85a3::8a2e:0370:7334",
        )
        for (case in accepted) {
            assertTrue(IpAddressDetector.matches(case), "expected IP to match: <$case>")
        }
    }

    @Test
    fun `rejects invalid IPv4`() {
        val rejected = listOf(
            "999.1.1.1",                       // octet > 255
            "256.1.1.1",
            "01.2.3.4",                        // leading zero
            "1.02.3.4",
            "1.2.3.04",
            "1.2.3",                           // too few octets
            "1.2.3.4.5",                       // too many octets
            "1.2.3.",                          // trailing empty octet
            ".1.2.3.4",                        // leading empty octet
            "1..2.3",                          // empty octet
            "1.2.3.256",
            "1.2.3.4 ",                        // trailing whitespace
            " 1.2.3.4",                        // leading whitespace
            "1.2.3.4a",                        // trailing garbage
            "-1.2.3.4",                        // negative
            "1.2.3.4/24",                      // CIDR not allowed
            "1.2.3.4:80",                      // port not allowed
            "300.300.300.300",
            "12.34.56.789",
            "0x1.2.3.4",                       // hex
        )
        for (case in rejected) {
            assertFalse(IpAddressDetector.matches(case), "expected NON-IP: <$case>")
        }
    }

    @Test
    fun `rejects invalid IPv6`() {
        val rejected = listOf(
            "2001:db8::1::2",                  // two "::"
            ":::",                             // triple colon
            "12345::",                         // group longer than 4 hex digits
            "fe80::1%eth0",                    // zone index
            "1:2:3:4:5:6:7",                   // 7 groups, no "::"
            "1:2:3:4:5:6:7:8:9",               // 9 groups
            "1:2:3:4:5:6:7:8:",                // trailing colon
            ":1:2:3:4:5:6:7:8",                // leading single colon
            "1::2::3",                         // two "::"
            "gggg::1",                         // non-hex group
            "1:2:3:4:5:6:7:8::",               // 8 explicit groups then "::"
            "::ffff:999.1.1.1",                // invalid embedded IPv4
            "::ffff:192.168.0.256",
            "::ffff:1.2.3",                    // incomplete embedded IPv4
            "::ffff:01.2.3.4",                 // embedded IPv4 leading zero
            "::12345",                         // 5-hex group after "::"
            "2001:db8:::1",                    // triple colon
            "[::1]",                           // brackets not allowed
            "::1/64",                          // CIDR not allowed
            "fe80::1 ",                        // trailing whitespace
            " ::1",                            // leading whitespace
            "1:2:3:4:5:1.2.3.4",               // 5 hex + embedded IPv4 = 7 groups
            "1:2:3:4:5:6:7:1.2.3.4",           // 7 hex + embedded IPv4 = 9 groups
            "::1:2:3:4:5:6:7:8",               // "::" then 8 explicit groups
            "1234:5678",                       // only 2 groups, no "::"
            "12345:1::",                       // 5-hex first group
            "",
        )
        for (case in rejected) {
            assertFalse(IpAddressDetector.matches(case), "expected NON-IP: <$case>")
        }
    }
}
