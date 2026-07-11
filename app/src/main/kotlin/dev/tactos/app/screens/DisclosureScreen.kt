package dev.tactos.app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The clipboard-capture disclosure. This is a trust document: the final
 * wording is a human review gate (CLAUDE.md section 8) — the text below is a
 * DRAFT for that review, shipped read-only. Onboarding presents the same
 * body before any capture option can be enabled.
 */
@Composable
fun DisclosureScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        DisclosureBody()
    }
}

/** The disclosure text itself, shared by [DisclosureScreen] and onboarding. */
@Composable
fun DisclosureBody() {
    Paragraph(
        "tactos can keep a history of what you copy, so you can find it again later. " +
            "Everything stays on this device, in tactos's private storage.",
    )
    Heading("How capture works today")
    Paragraph(
        "• When you open tactos, whatever is on the clipboard is saved to your " +
            "timeline (you can turn this off in Settings).\n" +
            "• You can share text to tactos from any app's share menu.\n" +
            "• You can add clips by hand with the + button.",
    )
    Heading("Automatic background capture")
    Paragraph(
        "Because of Android's privacy rules, capturing copies made while tactos is " +
            "closed would need the tactos accessibility service. That service is not " +
            "part of the app yet — when it arrives it will be strictly opt-in, behind " +
            "this disclosure, and will do nothing except watch for clipboard changes.",
    )
    Heading("What tactos never does")
    Paragraph(
        "• Never sends your clipboard (or anything else) off this device — the app " +
            "doesn't even hold the permission to use the internet.\n" +
            "• Never shows ads, never tracks you, never asks for an account.\n" +
            "• Never stores clips that other apps mark as sensitive, like passwords " +
            "copied from a password manager.",
    )
    Heading("You stay in control")
    Paragraph(
        "You can turn capture off at any time in Settings, delete any item, or let " +
            "auto-cleanup trim old items for you. Pinned and favorite clips are " +
            "always kept.",
    )
    Paragraph(
        "This wording is a draft pending maintainer review.",
        emphasized = true,
    )
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun Paragraph(text: String, emphasized: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (emphasized) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
