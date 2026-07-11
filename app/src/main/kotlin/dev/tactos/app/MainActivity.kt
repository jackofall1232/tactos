package dev.tactos.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.lifecycleScope
import dev.tactos.app.settings.RetentionCleanup
import dev.tactos.app.settings.SettingsRepository
import dev.tactos.core.clipboard.ClipboardCapture
import dev.tactos.core.clipboard.ShareIngest
import dev.tactos.core.database.TactosDb
import dev.tactos.core.design.TactosTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var capture: ClipboardCapture

    /** Bumped when a share lands, so the UI jumps to the timeline. */
    private val sharedClipTick = mutableIntStateOf(0)

    /** A share grants us focus too — don't also capture the stale clipboard. */
    private var suppressNextFocusCapture = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)
        val repository = TactosDb.repository(applicationContext)
        capture = ClipboardCapture { item ->
            val id = repository.save(item)
            // Keep the retention promise honest on every automatic save.
            RetentionCleanup.run(repository, settingsRepository.settings.first())
            id
        }
        // Recreation redelivers the original intent — only ingest it once.
        if (savedInstanceState == null) ingestShare(intent)
        setContent {
            TactosTheme {
                TactosApp(
                    settingsRepository = settingsRepository,
                    sharedClipTick = sharedClipTick.intValue,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Keep getIntent() pointing at the latest delivery, not the launch intent.
        setIntent(intent)
        ingestShare(intent)
    }

    /**
     * Foreground-refresh rung of the capture ladder: the system only lets the
     * focused app read the clipboard (Android 10+), so window focus is the
     * earliest reliable moment. Off until onboarding consent, and gated by
     * the user's capture toggle.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        if (suppressNextFocusCapture) {
            suppressNextFocusCapture = false
            return
        }
        lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.onboardingComplete && settings.captureOnFocus) {
                capture.captureCurrent(this@MainActivity)
            }
        }
    }

    /** Share-to-tactos rung: persist text arriving via ACTION_SEND. */
    private fun ingestShare(intent: Intent?) {
        val text = ShareIngest.textFrom(intent) ?: return
        suppressNextFocusCapture = true
        lifecycleScope.launch {
            if (capture.saveText(text, sourceHint = "share") is ClipboardCapture.Result.Saved) {
                Toast.makeText(this@MainActivity, "Saved to tactos", Toast.LENGTH_SHORT).show()
                sharedClipTick.intValue++
            }
        }
    }
}
