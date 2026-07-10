package dev.tactos.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tactos.core.design.components.ToolboxCard
import dev.tactos.core.model.ModuleRegistry

/** Home grid of toolboxes, driven entirely by the [ModuleRegistry]. */
@Composable
fun HomeScreen(
    registry: ModuleRegistry,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(registry.modules, key = { it.id }) { module ->
            ToolboxCard(
                emoji = module.emoji,
                title = module.title,
                description = module.description,
                onClick = { onOpenModule(module.id) },
            )
        }
    }
}
