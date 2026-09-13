package dev.tactos.feature.images.engine

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class PhotoExportBudgetTest {
    @Test fun `128 MiB heaps reserve space for simultaneous export buffers`() {
        val heap = 128L * 1024 * 1024
        val dimension = PhotoExportBudget.maxDimension(heap)
        assertTrue(dimension in 1600..1700)
        assertTrue(dimension.toLong() * dimension * 48 <= heap)
    }

    @Test fun `decode budget scales with heap and stays within supported dimensions`() {
        assertTrue(PhotoExportBudget.maxDimension(256L * 1024 * 1024) > PhotoExportBudget.maxDimension(128L * 1024 * 1024))
        assertEquals(4096, PhotoExportBudget.maxDimension(2L * 1024 * 1024 * 1024))
        assertEquals(512, PhotoExportBudget.maxDimension(8L * 1024 * 1024))
        assertFailsWith<IllegalArgumentException> { PhotoExportBudget.maxDimension(0) }
    }
}
