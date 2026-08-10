package dev.tactos.app.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.tactos.app.BuildConfig
import dev.tactos.app.settings.TactosSettings

private const val REPO_URL = "https://github.com/jackofall1232/tactos"

private val RETENTION_DAY_OPTIONS = listOf(
    0 to "Keep forever",
    1 to "1 day",
    7 to "7 days",
    30 to "30 days",
    90 to "90 days",
)

private val RETENTION_COUNT_OPTIONS = listOf(
    0 to "Unlimited",
    50 to "50 clips",
    100 to "100 clips",
    500 to "500 clips",
    1000 to "1000 clips",
)

/** DataStore-backed settings: capture, privacy, history retention, about. */
@Composable
fun SettingsScreen(
    settings: TactosSettings,
    onSetCaptureOnFocus: (Boolean) -> Unit,
    onSetRetentionDays: (Int) -> Unit,
    onSetRetentionMaxItems: (Int) -> Unit,
    onShowDisclosure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDaysDialog by remember { mutableStateOf(false) }
    var showCountDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SettingsHeader("Capture")
        SwitchRow(
            title = "Capture when tactos opens",
            subtitle = "Save whatever is on the clipboard whenever the app comes to the foreground",
            checked = settings.captureOnFocus,
            onCheckedChange = onSetCaptureOnFocus,
        )
        SettingsRow(
            title = "Share to tactos",
            subtitle = "Always available from any app's share menu",
        )
        SettingsRow(
            title = "Automatic background capture",
            subtitle = "Planned: an opt-in accessibility service, pending on-device " +
                "verification and review",
            enabled = false,
        )
        HorizontalDivider()

        SettingsHeader("Privacy")
        SettingsRow(
            title = "Clipboard capture disclosure",
            subtitle = "How capture works and what tactos never does",
            onClick = onShowDisclosure,
        )
        SettingsRow(
            title = "Sensitive clips",
            subtitle = "Clips other apps mark as sensitive are never saved",
        )
        HorizontalDivider()

        SettingsHeader("History")
        SettingsRow(
            title = "Delete clips older than",
            subtitle = RETENTION_DAY_OPTIONS.labelFor(settings.retentionDays),
            onClick = { showDaysDialog = true },
        )
        SettingsRow(
            title = "Keep at most",
            subtitle = RETENTION_COUNT_OPTIONS.labelFor(settings.retentionMaxItems),
            onClick = { showCountDialog = true },
        )
        Text(
            text = "Pinned and favorite clips are never auto-deleted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider()

        SettingsHeader("About")
        SettingsRow(
            title = "tactos ${BuildConfig.VERSION_NAME}",
            subtitle = "Open source (MIT) · no ads · no accounts · offline-first",
        )
        SettingsRow(
            title = "Source code",
            subtitle = REPO_URL.removePrefix("https://"),
            onClick = {
                // User-initiated hand-off to the browser — the app itself has
                // no INTERNET permission and never talks to the network.
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, REPO_URL.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    if (showDaysDialog) {
        SingleChoiceDialog(
            title = "Delete clips older than",
            options = RETENTION_DAY_OPTIONS,
            selected = settings.retentionDays,
            onSelect = {
                onSetRetentionDays(it)
                showDaysDialog = false
            },
            onDismiss = { showDaysDialog = false },
        )
    }
    if (showCountDialog) {
        SingleChoiceDialog(
            title = "Keep at most",
            options = RETENTION_COUNT_OPTIONS,
            selected = settings.retentionMaxItems,
            onSelect = {
                onSetRetentionMaxItems(it)
                showCountDialog = false
            },
            onDismiss = { showCountDialog = false },
        )
    }
}

private fun List<Pair<Int, String>>.labelFor(value: Int): String =
    firstOrNull { it.first == value }?.second ?: "$value"

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val base = Modifier.fillMaxWidth()
    val clickable = if (onClick != null && enabled) base.clickable(onClick = onClick) else base
    Column(modifier = clickable.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == selected,
                                onClick = { onSelect(value) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) },
                        )
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
