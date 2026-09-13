package dev.tactos.feature.images.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tactos.feature.images.engine.ImageLoading
import dev.tactos.feature.images.engine.PhotoPdfExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImagesPdfScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photos by rememberSaveable(
        stateSaver = listSaver<List<Uri>, String>(
            save = { values -> values.map(Uri::toString) },
            restore = { values -> values.map(Uri::parse) },
        ),
    ) { mutableStateOf<List<Uri>>(emptyList()) }
    var names by remember { mutableStateOf<Map<Uri, String>>(emptyMap()) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var task by remember { mutableStateOf<Job?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(photos) {
        names = withContext(Dispatchers.IO) {
            photos.associateWith { ImageLoading.info(context, it)?.displayName ?: "Photo" }
        }
    }
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PhotoPdfExporter.MAX_PHOTOS),
    ) { selected ->
        if (selected.isNotEmpty()) {
            photos = selected.distinct().take(PhotoPdfExporter.MAX_PHOTOS)
            message = null
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val sources = photos.toList()
            val useLandscape = landscape
            task = scope.launch {
                running = true
                message = null
                try {
                    PhotoPdfExporter.export(context, sources, uri, useLandscape)
                    message = "Saved ${sources.size} pages as a new PDF."
                } catch (cancelled: CancellationException) {
                    message = "Export cancelled."
                    throw cancelled
                } catch (error: Exception) {
                    message = error.message ?: "Couldn't save the PDF. Try another destination."
                } finally {
                    running = false
                }
            }
        }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Photos to PDF", style = MaterialTheme.typography.headlineSmall)
        Text("One photo per page, fitted without cropping. Everything stays on this device.")
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !running) {
            Text(if (photos.isEmpty()) "Choose photos" else "Replace photos")
        }
        Text("Up to ${PhotoPdfExporter.MAX_PHOTOS} photos per PDF. Images are reduced to at most 1600 pixels per side to keep exports manageable.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !landscape, onClick = { landscape = false }, enabled = !running, label = { Text("A4 portrait") })
            FilterChip(selected = landscape, onClick = { landscape = true }, enabled = !running, label = { Text("A4 landscape") })
        }
        photos.forEachIndexed { index, uri ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${index + 1}. ${names[uri] ?: "Photo"}", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            photos = photos.toMutableList().apply { add(index - 1, removeAt(index)) }
                        }, enabled = !running && index > 0) { Text("Move up") }
                        TextButton(onClick = {
                            photos = photos.toMutableList().apply { add(index + 1, removeAt(index)) }
                        }, enabled = !running && index < photos.lastIndex) { Text("Move down") }
                        TextButton(onClick = { photos = photos.filterNot { it == uri } }, enabled = !running) { Text("Remove") }
                    }
                }
            }
        }
        if (running) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { task?.cancel() }) { Text("Cancel export") }
        }
        message?.let { Text(it) }
        Button(onClick = { save.launch("tactos-photos.pdf") }, enabled = photos.isNotEmpty() && !running, modifier = Modifier.fillMaxWidth()) {
            Text("Save PDF (${photos.size} pages)")
        }
        Text("Original photos are preserved. Leaving or rotating this screen cancels an active export. Removal of incomplete output is attempted.", style = MaterialTheme.typography.bodySmall)
    }
}
