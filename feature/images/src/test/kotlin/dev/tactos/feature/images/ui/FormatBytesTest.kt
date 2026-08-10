package dev.tactos.feature.images.ui

import kotlin.test.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun `bytes render in the right unit`() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("500 KB", formatBytes(500 * 1024))
        assertEquals("2.5 MB", formatBytes((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `boundaries pick the larger unit`() {
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1.0 MB", formatBytes(1024 * 1024))
    }
}
