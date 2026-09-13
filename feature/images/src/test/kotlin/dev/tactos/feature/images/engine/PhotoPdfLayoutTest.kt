package dev.tactos.feature.images.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PhotoPdfLayoutTest {
    @Test fun `portrait photo fits margins and keeps ratio`() {
        val fit = PhotoPdfLayout.fit(1000, 2000, false)
        assertEquals(24f, fit.top)
        assertEquals(818f, fit.bottom)
        assertEquals(0.5f, (fit.right - fit.left) / (fit.bottom - fit.top))
        assertEquals(595f, fit.left + fit.right)
    }

    @Test fun `landscape panorama fits width without cropping`() {
        val fit = PhotoPdfLayout.fit(4000, 1000, true)
        assertEquals(24f, fit.left)
        assertEquals(818f, fit.right)
        assertEquals(4f, (fit.right - fit.left) / (fit.bottom - fit.top))
        assertTrue(fit.top >= 24f && fit.bottom <= 571f)
        assertEquals(595f, fit.top + fit.bottom)
    }

    @Test fun `invalid dimensions and margins rejected`() {
        assertFailsWith<IllegalArgumentException> { PhotoPdfLayout.fit(0, 10, false) }
        assertFailsWith<IllegalArgumentException> { PhotoPdfLayout.fit(10, -1, false) }
        assertFailsWith<IllegalArgumentException> { PhotoPdfLayout.fit(10, 10, false, 300f) }
    }
}
