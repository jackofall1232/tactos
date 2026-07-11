package dev.tactos.core.clipboard

import android.content.Intent
import android.text.SpannableString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareIngestTest {

    private fun sendIntent(
        text: CharSequence? = "hello",
        action: String = Intent.ACTION_SEND,
        type: String? = "text/plain",
    ): Intent = Intent(action).apply {
        this.type = type
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
    }

    @Test
    fun `plain text send is ingested trimmed`() {
        assertEquals("hello", ShareIngest.textFrom(sendIntent(text = "  hello \n")))
    }

    @Test
    fun `spannable text is ingested as plain string`() {
        assertEquals("styled", ShareIngest.textFrom(sendIntent(text = SpannableString("styled"))))
    }

    @Test
    fun `any text subtype is accepted`() {
        assertEquals("a", ShareIngest.textFrom(sendIntent(text = "a", type = "text/x-url")))
    }

    @Test
    fun `null intent is rejected`() {
        assertNull(ShareIngest.textFrom(null))
    }

    @Test
    fun `wrong action is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(action = Intent.ACTION_VIEW)))
    }

    @Test
    fun `send multiple is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(action = Intent.ACTION_SEND_MULTIPLE)))
    }

    @Test
    fun `missing mime type is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(type = null)))
    }

    @Test
    fun `non-text mime type is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(type = "image/png")))
    }

    @Test
    fun `missing extra text is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(text = null)))
    }

    @Test
    fun `blank extra text is rejected`() {
        assertNull(ShareIngest.textFrom(sendIntent(text = "   \n\t")))
    }
}
