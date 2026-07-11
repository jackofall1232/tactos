package dev.tactos.core.clipboard

import android.content.ClipData
import android.os.PersistableBundle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SensitiveClipsTest {

    private fun description(extras: PersistableBundle? = null) =
        ClipData.newPlainText("label", "text").description.apply {
            if (extras != null) setExtras(extras)
        }

    @Test
    fun `null description is not sensitive`() {
        assertFalse(SensitiveClips.isSensitive(null))
    }

    @Test
    fun `description without extras is not sensitive`() {
        assertFalse(SensitiveClips.isSensitive(description()))
    }

    @Test
    fun `flagged clip is sensitive`() {
        val extras = PersistableBundle().apply {
            putBoolean(SensitiveClips.EXTRA_IS_SENSITIVE, true)
        }
        assertTrue(SensitiveClips.isSensitive(description(extras)))
    }

    @Test
    fun `explicitly unflagged clip is not sensitive`() {
        val extras = PersistableBundle().apply {
            putBoolean(SensitiveClips.EXTRA_IS_SENSITIVE, false)
        }
        assertFalse(SensitiveClips.isSensitive(description(extras)))
    }

    @Test
    fun `unrelated extras are not sensitive`() {
        val extras = PersistableBundle().apply {
            putBoolean("some.other.flag", true)
        }
        assertFalse(SensitiveClips.isSensitive(description(extras)))
    }
}
