package dev.tactos.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tactos.core.model.ToolboxModule

/**
 * Shown for a registered module whose feature UI hasn't landed yet. Replaced
 * per-module as feature modules arrive (feature/clipboard first).
 */
@Composable
fun ModulePlaceholderScreen(
    module: ToolboxModule?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = module?.emoji ?: "🧰",
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = module?.title ?: "Unknown module",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "This toolbox is under construction.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
