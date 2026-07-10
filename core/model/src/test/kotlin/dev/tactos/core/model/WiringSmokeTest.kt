package dev.tactos.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** Proves the JVM test toolchain is wired; replaced by real contract tests. */
class WiringSmokeTest {
    @Test
    fun `test runner executes kotlin tests`() {
        assertEquals(4, 2 + 2)
    }
}
