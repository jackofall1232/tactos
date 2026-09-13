package dev.tactos.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tactos.core.design.components.ToolboxCard
import dev.tactos.core.model.ModuleRegistry
import dev.tactos.feature.images.ImagesToolbox

/** Searchable entry point. Only installed, working tools appear here. */
@Composable
fun HomeScreen(
    registry: ModuleRegistry,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPhotoTool: (String) -> Unit = { onOpenModule(ImagesToolbox.id) },
) {
    var query by rememberSaveable { mutableStateOf("") }
    val modules = registry.modules.filter { query.isBlank() || "${it.title} ${it.description}".contains(query.trim(), true) }
    val photoTools = ImagesToolbox.tools.filter { query.isBlank() || "${it.label} ${it.description}".contains(query.trim(), true) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A little more possible.", style = MaterialTheme.typography.headlineLarge)
                Text("Your everyday tools, together. Free local tools. No ads.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(query, { query = it }, label = { Text("Find a tool") }, placeholder = { Text("Try crop, metadata, or clipboard") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        if (query.isBlank()) item(span = { GridItemSpan(maxLineSpan) }) {
            Card(onClick = { onOpenPhotoTool(ImagesToolbox.TOOL_STUDIO) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PHOTO STUDIO · ON DEVICE", style = MaterialTheme.typography.labelMedium)
                    Text("Your photos. Your finish.", style = MaterialTheme.typography.headlineSmall)
                    Text("Crop, color, filters, and watermarks. Start editing →", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (modules.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("Your toolboxes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        items(modules, key = { "module-${it.id}" }) { module ->
            ToolboxCard(module.emoji, module.title, module.description, { onOpenModule(module.id) })
        }
        if (photoTools.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { Text("Photo tools", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
        items(photoTools, key = { it.id }) { tool ->
            ToolboxCard(tool.emoji, tool.label, tool.description, { onOpenPhotoTool(tool.id) })
        }
        if (modules.isEmpty() && photoTools.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Text("No tools match “$query”. Try another word.", modifier = Modifier.padding(16.dp))
        }
    }
}
