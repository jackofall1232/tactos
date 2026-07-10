package dev.tactos.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Settings skeleton. The entries shown as "coming soon" are wired up when
 * DataStore-backed preferences land (dependency review gate) — nothing here
 * pretends to persist anything yet.
 */
@Composable
fun SettingsScreen(
    onShowDisclosure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SettingsHeader("Privacy")
        SettingsRow(
            title = "Clipboard capture disclosure",
            subtitle = "How automatic capture works and what tactos never does",
            onClick = onShowDisclosure,
        )
        HorizontalDivider()
        SettingsHeader("History")
        SettingsRow(
            title = "Retention",
            subtitle = "Auto-cleanup rules — coming soon",
            enabled = false,
        )
        SettingsRow(
            title = "Sensitive clips",
            subtitle = "Handling of password-manager copies — coming soon",
            enabled = false,
        )
        HorizontalDivider()
        SettingsHeader("About")
        SettingsRow(
            title = "tactos",
            subtitle = "Open source (MIT) · no ads · no accounts · offline-first",
            enabled = false,
        )
    }
}

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
