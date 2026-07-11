package dev.tactos.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.tactos.app.screens.DisclosureScreen
import dev.tactos.app.screens.HomeScreen
import dev.tactos.app.screens.ModulePlaceholderScreen
import dev.tactos.app.screens.OnboardingScreen
import dev.tactos.app.screens.SettingsScreen
import dev.tactos.app.settings.RetentionCleanup
import dev.tactos.app.settings.SettingsRepository
import dev.tactos.core.database.TactosDb
import dev.tactos.feature.clipboard.ClipboardScreen
import dev.tactos.feature.clipboard.ClipboardToolbox
import kotlinx.coroutines.launch

/**
 * In-app destinations. Navigation is deliberately plain Compose state for
 * now — introducing navigation-compose is a dependency decision deferred to
 * a review gate (see .l00prite/todos.md).
 */
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Disclosure : Screen
    data class Module(val moduleId: String) : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TactosApp(
    settingsRepository: SettingsRepository,
    sharedClipTick: Int = 0,
) {
    val registry = remember { appModuleRegistry() }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val appContext = LocalContext.current.applicationContext
    val clipRepository = remember { TactosDb.repository(appContext) }
    val scope = rememberCoroutineScope()

    val settingsState by settingsRepository.settings.collectAsState(initial = null)
    // Hold rendering for the first frames until DataStore emits, so the
    // onboarding gate doesn't flash for already-onboarded users.
    val settings = settingsState ?: return

    // A share just landed: jump to the timeline (tick 0 = no share yet).
    LaunchedEffect(sharedClipTick) {
        if (sharedClipTick > 0) screen = Screen.Module(ClipboardToolbox.id)
    }

    // Apply retention at launch and whenever the rules change. No background
    // scheduler in v1 — see RetentionCleanup.
    LaunchedEffect(settings.retentionDays, settings.retentionMaxItems) {
        RetentionCleanup.run(clipRepository, settings)
    }

    if (!settings.onboardingComplete) {
        OnboardingScreen(
            onFinish = { captureOnFocus ->
                scope.launch {
                    settingsRepository.setCaptureOnFocus(captureOnFocus)
                    settingsRepository.setOnboardingComplete(true)
                }
            },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val s = screen) {
                            Screen.Home -> "tactos"
                            Screen.Settings -> "Settings"
                            Screen.Disclosure -> "Clipboard capture"
                            is Screen.Module -> registry.byId(s.moduleId)?.title ?: "tactos"
                        },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (screen != Screen.Home) {
                        IconButton(onClick = {
                            screen = if (screen == Screen.Disclosure) Screen.Settings else Screen.Home
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (screen == Screen.Home) {
                        IconButton(onClick = { screen = Screen.Settings }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                registry = registry,
                onOpenModule = { screen = Screen.Module(it) },
                modifier = contentModifier,
            )
            Screen.Settings -> SettingsScreen(
                settings = settings,
                onSetCaptureOnFocus = { scope.launch { settingsRepository.setCaptureOnFocus(it) } },
                onSetRetentionDays = { scope.launch { settingsRepository.setRetentionDays(it) } },
                onSetRetentionMaxItems = {
                    scope.launch { settingsRepository.setRetentionMaxItems(it) }
                },
                onShowDisclosure = { screen = Screen.Disclosure },
                modifier = contentModifier,
            )
            Screen.Disclosure -> DisclosureScreen(modifier = contentModifier)
            is Screen.Module -> when (s.moduleId) {
                ClipboardToolbox.id -> ClipboardScreen(
                    repository = clipRepository,
                    modifier = contentModifier,
                )
                else -> ModulePlaceholderScreen(
                    module = registry.byId(s.moduleId),
                    modifier = contentModifier,
                )
            }
        }
    }
}
