package dev.tactos.core.actions

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrCodeTest {

    // --- happy path ---

    @Test
    fun `encodes a url`() {
        val matrix = QrCode.encode("https://tactos.dev")
        assertNotNull(matrix)
    }

    @Test
    fun `module count is a valid qr version plus margin`() {
        val margin = 2
        val matrix = assertNotNull(QrCode.encode("https://tactos.dev", margin = margin))
        val modules = matrix.size - 2 * margin
        // QR symbol sizes are 21 + 4k, k in 0..39.
        assertTrue(modules >= 21, "expected at least version 1, got $modules modules")
        assertEquals(1, modules % 4, "QR module count must be 21 + 4k, got $modules")
    }

    @Test
    fun `margin ring is entirely light`() {
        val margin = 2
        val matrix = assertNotNull(QrCode.encode("margin test", margin = margin))
        for (i in 0 until matrix.size) {
            for (ring in 0 until margin) {
                assertTrue(!matrix[i, ring] && !matrix[ring, i], "top/left margin dark at $i")
                assertTrue(
                    !matrix[i, matrix.size - 1 - ring] && !matrix[matrix.size - 1 - ring, i],
                    "bottom/right margin dark at $i",
                )
            }
        }
    }

    @Test
    fun `finder pattern corners are dark`() {
        val margin = 2
        val matrix = assertNotNull(QrCode.encode("finder", margin = margin))
        val last = matrix.size - 1 - margin
        // Outer corner module of each of the three finder patterns, plus their
        // 7x7 centers, are always dark in any QR symbol.
        assertTrue(matrix[margin, margin])
        assertTrue(matrix[last, margin])
        assertTrue(matrix[margin, last])
        assertTrue(matrix[margin + 3, margin + 3])
        assertTrue(matrix[last - 3, margin + 3])
        assertTrue(matrix[margin + 3, last - 3])
    }

    @Test
    fun `margin zero produces bare symbol`() {
        val matrix = assertNotNull(QrCode.encode("bare", margin = 0))
        assertEquals(1, matrix.size % 4)
        assertTrue(matrix[0, 0], "finder corner should sit at the origin without margin")
    }

    @Test
    fun `unicode content encodes`() {
        assertNotNull(QrCode.encode("héllo wörld 🌍"))
    }

    @Test
    fun `max length input encodes`() {
        assertNotNull(QrCode.encode("a".repeat(QrCode.MAX_INPUT_LENGTH)))
    }

    // --- rejects ---

    @Test
    fun `empty text is rejected`() {
        assertNull(QrCode.encode(""))
    }

    @Test
    fun `blank text is rejected`() {
        assertNull(QrCode.encode("   \n"))
    }

    @Test
    fun `over-long text is rejected`() {
        assertNull(QrCode.encode("a".repeat(QrCode.MAX_INPUT_LENGTH + 1)))
    }

    @Test
    fun `negative margin is rejected`() {
        assertNull(QrCode.encode("x", margin = -1))
    }

    // --- matrix bounds ---

    @Test
    fun `out of bounds access throws`() {
        val matrix = assertNotNull(QrCode.encode("bounds"))
        assertFailsWith<IndexOutOfBoundsException> { matrix[-1, 0] }
        assertFailsWith<IndexOutOfBoundsException> { matrix[0, -1] }
        assertFailsWith<IndexOutOfBoundsException> { matrix[matrix.size, 0] }
        assertFailsWith<IndexOutOfBoundsException> { matrix[0, matrix.size] }
    }
}
