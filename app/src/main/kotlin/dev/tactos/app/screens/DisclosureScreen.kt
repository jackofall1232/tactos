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
 * DRAFT for that review, shipped read-only. The onboarding flow will present
 * it before any capture option can be enabled.
 */
@Composable
fun DisclosureScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Paragraph(
            "tactos can keep a history of what you copy, so you can find it again later. " +
                "Because of Android's privacy rules, automatic capture in the background " +
                "only works if you turn on the tactos accessibility service.",
        )
        Heading("What the accessibility service does")
        Paragraph(
            "It watches for clipboard changes and saves new copies into your on-device " +
                "timeline. That is all it does.",
        )
        Heading("What tactos never does")
        Paragraph(
            "• Never sends your clipboard (or anything else) off this device.\n" +
                "• Never shows ads, never tracks you, never asks for an account.\n" +
                "• Never stores clips that apps mark as sensitive (like passwords) in " +
                "plain view.\n" +
                "• Never requires the accessibility service — you can decline it and add " +
                "clips by sharing to tactos or opening the app.",
        )
        Heading("You stay in control")
        Paragraph(
            "You can turn capture off at any time in Settings, delete any item, or clear " +
                "the whole history. Auto-cleanup can trim old items for you.",
        )
        Paragraph(
            "This wording is a draft pending maintainer review.",
            emphasized = true,
        )
    }
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
