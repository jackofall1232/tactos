package dev.tactos.feature.images.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tactos.feature.images.ImageTool
import dev.tactos.feature.images.ImagesToolbox

/**
 * Image toolbox root: the tool list, and one tool at a time. Internal
 * navigation follows the shell idiom — plain saveable state plus a nested
 * BackHandler (inner handlers win, so back closes the tool before the shell
 * navigates Home) and the same directional AnimatedContent transition.
 */
@Composable
fun ImagesScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    var toolId by rememberSaveable { mutableStateOf<String?>(null) }
    // Saved as strings so the selection survives rotation. Picker grants are
    // per-process, so after process death stale uris simply fail to decode
    // and the user re-picks — never a crash.
    var pickedStrings by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val picked = remember(pickedStrings) { pickedStrings.map(Uri::parse) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PICK),
    ) { uris -> if (uris.isNotEmpty()) pickedStrings = uris.map(Uri::toString) }

    val tool = toolId?.let { ImagesToolbox.toolById(it) }
    BackHandler(enabled = tool != null) { toolId = null }

    AnimatedContent(
        targetState = tool,
        transitionSpec = {
            val forward = targetState != null
            val enter = fadeIn() + slideInHorizontally { if (forward) it / 8 else -it / 8 }
            val exit = fadeOut() + slideOutHorizontally { if (forward) -it / 8 else it / 8 }
            enter togetherWith exit
        },
        label = "images-tool",
        modifier = modifier,
    ) { current ->
        if (current == null) {
            ToolList(onOpen = { toolId = it })
        } else {
            ToolScreen(
                tool = current,
                picked = picked,
                onPickImages = {
                    pickImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onClearPicked = { pickedStrings = emptyList() },
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

@Composable
private fun ToolList(onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ImagesToolbox.tools, key = { it.id }) { tool ->
            ToolRow(tool = tool, onClick = { onOpen(tool.id) })
        }
        item {
            Text(
                text = "Everything runs on this device. Originals are never modified — " +
                    "results are always saved as new files where you choose. " +
                    "Very large photos are processed at up to 4096 px on the long side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ToolRow(tool: ImageTool, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = tool.emoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = tool.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val MAX_PICK = 30
