package dev.tactos.core.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import androidx.test.core.app.ApplicationProvider
import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
class ClipboardCaptureTest {

    private lateinit var context: Context
    private lateinit var clipboard: ClipboardManager
    private val stored = mutableListOf<ClipItem>()
    private val capture = ClipboardCapture { item ->
        stored += item
        stored.size.toLong()
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clipboard = context.getSystemService(ClipboardManager::class.java)
        stored.clear()
    }

    // --- captureCurrent ---

    @Test
    fun `captures primary clip with detected type`() = runTest {
        clipboard.setPrimaryClip(ClipData.newPlainText("t", "https://tactos.dev"))
        val result = capture.captureCurrent(context, sourceHint = "focus")

        assertIs<ClipboardCapture.Result.Saved>(result)
        assertEquals(1, stored.size)
        assertEquals("https://tactos.dev", stored[0].text)
        assertEquals(ClipType.URL, stored[0].type)
        assertEquals("focus", stored[0].sourceApp)
    }

    @Test
    fun `trims captured text before storing`() = runTest {
        clipboard.setPrimaryClip(ClipData.newPlainText("t", "  padded  "))
        capture.captureCurrent(context)

        assertEquals("padded", stored.single().text)
    }

    @Test
    fun `skips sensitive clips entirely`() = runTest {
        val clip = ClipData.newPlainText("t", "hunter2")
        clip.description.extras = PersistableBundle().apply {
            putBoolean(SensitiveClips.EXTRA_IS_SENSITIVE, true)
        }
        clipboard.setPrimaryClip(clip)

        val result = capture.captureCurrent(context)

        assertEquals(ClipboardCapture.Result.SkippedSensitive, result)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `empty clipboard captures nothing`() = runTest {
        val result = capture.captureCurrent(context)

        assertEquals(ClipboardCapture.Result.NothingToCapture, result)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `blank clip text captures nothing`() = runTest {
        clipboard.setPrimaryClip(ClipData.newPlainText("t", "   "))
        val result = capture.captureCurrent(context)

        assertEquals(ClipboardCapture.Result.NothingToCapture, result)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `uri-only clip that coerces to its own uri string captures nothing`() = runTest {
        // No provider backs this URI, so coercion yields the raw
        // content:// string (or fails) — either way nothing is stored.
        // Built directly: ClipData.newUri would query the (absent) provider.
        val uri = Uri.parse("content://media/external/images/media/42")
        val clip = ClipData(
            ClipDescription("image", arrayOf("image/jpeg")),
            ClipData.Item(uri),
        )
        clipboard.setPrimaryClip(clip)

        val result = capture.captureCurrent(context)

        assertEquals(ClipboardCapture.Result.NothingToCapture, result)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `intent-only clip captures nothing`() = runTest {
        clipboard.setPrimaryClip(ClipData.newIntent("shortcut", Intent(Intent.ACTION_VIEW)))

        val result = capture.captureCurrent(context)

        assertEquals(ClipboardCapture.Result.NothingToCapture, result)
        assertTrue(stored.isEmpty())
    }

    // --- saveText ---

    @Test
    fun `saveText persists trimmed text with detected type`() = runTest {
        val result = capture.saveText(" {\"a\": 1} ", sourceHint = "shared")

        assertIs<ClipboardCapture.Result.Saved>(result)
        assertEquals("{\"a\": 1}", stored.single().text)
        assertEquals(ClipType.JSON, stored.single().type)
        assertEquals("shared", stored.single().sourceApp)
    }

    @Test
    fun `saveText rejects blank text`() = runTest {
        val result = capture.saveText("  \n ")

        assertEquals(ClipboardCapture.Result.NothingToCapture, result)
        assertTrue(stored.isEmpty())
    }

    @Test
    fun `saved id is passed through from the store`() = runTest {
        capture.saveText("first")
        val second = capture.saveText("second")

        assertEquals(ClipboardCapture.Result.Saved(2L), second)
    }
}
