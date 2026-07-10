package dev.tactos.core.detect

import kotlin.test.Test
import kotlin.test.assertTrue

/** Proves the JVM test toolchain is wired; replaced by real detector tests. */
class WiringSmokeTest {
    @Test
    fun `test runner executes kotlin tests`() {
        assertTrue("tactos".isNotEmpty())
    }
}
