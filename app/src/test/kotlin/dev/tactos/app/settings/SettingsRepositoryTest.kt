package dev.tactos.app.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Robolectric shares the Application context across test methods in this
 * class, and DataStore persists to a real file under that context — without
 * teardown, settings written by one test leak into the next and make
 * results depend on execution order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repo = SettingsRepository(context)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "datastore").deleteRecursively()
    }

    // --- defaults ---

    @Test
    fun `defaults are the privacy-safe out-of-box experience`() = runTest {
        val settings = repo.settings.first()
        assertFalse(settings.onboardingComplete)
        assertFalse(settings.captureOnFocus)
        assertEquals(0, settings.retentionDays)
        assertEquals(0, settings.retentionMaxItems)
    }

    // --- round trip ---

    @Test
    fun `onboardingComplete round trips through DataStore`() = runTest {
        repo.setOnboardingComplete(true)
        assertTrue(repo.settings.first().onboardingComplete)

        repo.setOnboardingComplete(false)
        assertFalse(repo.settings.first().onboardingComplete)
    }

    @Test
    fun `captureOnFocus round trips through DataStore`() = runTest {
        repo.setCaptureOnFocus(true)
        assertTrue(repo.settings.first().captureOnFocus)

        repo.setCaptureOnFocus(false)
        assertFalse(repo.settings.first().captureOnFocus)
    }

    @Test
    fun `retentionDays round trips through DataStore`() = runTest {
        repo.setRetentionDays(30)
        assertEquals(30, repo.settings.first().retentionDays)

        repo.setRetentionDays(0)
        assertEquals(0, repo.settings.first().retentionDays)
    }

    @Test
    fun `retentionMaxItems round trips through DataStore`() = runTest {
        repo.setRetentionMaxItems(500)
        assertEquals(500, repo.settings.first().retentionMaxItems)

        repo.setRetentionMaxItems(0)
        assertEquals(0, repo.settings.first().retentionMaxItems)
    }

    @Test
    fun `each field persists independently of the others`() = runTest {
        repo.setOnboardingComplete(true)
        repo.setCaptureOnFocus(true)
        repo.setRetentionDays(14)
        repo.setRetentionMaxItems(200)

        val settings = repo.settings.first()
        assertTrue(settings.onboardingComplete)
        assertTrue(settings.captureOnFocus)
        assertEquals(14, settings.retentionDays)
        assertEquals(200, settings.retentionMaxItems)
    }

    // --- validation ---

    @Test
    fun `negative retentionDays throws and does not persist`() = runTest {
        assertFailsWith<IllegalArgumentException> { repo.setRetentionDays(-1) }
        assertEquals(0, repo.settings.first().retentionDays)
    }

    @Test
    fun `negative retentionMaxItems throws and does not persist`() = runTest {
        assertFailsWith<IllegalArgumentException> { repo.setRetentionMaxItems(-1) }
        assertEquals(0, repo.settings.first().retentionMaxItems)
    }
}
