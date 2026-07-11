package dev.tactos.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * First-run flow: what tactos is, the capture disclosure, and the initial
 * capture choice. Everything works if the user leaves capture off — the
 * share target and manual add don't depend on it.
 */
@Composable
fun OnboardingScreen(
    onFinish: (captureOnFocus: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var captureOnFocus by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = "📋", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Welcome to tactos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "A smarter clipboard, and a toolbox around it. " +
                "No ads, no accounts, nothing leaves this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        )

        DisclosureBody()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture when I open tactos",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Save the current clipboard whenever the app comes to the foreground",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = captureOnFocus,
                onCheckedChange = { captureOnFocus = it },
            )
        }

        Button(
            onClick = { onFinish(captureOnFocus) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
        ) {
            Text("Get started")
        }
    }
}
