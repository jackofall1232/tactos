package dev.tactos.feature.images.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.exif.ExifRemovalPreset
import dev.tactos.core.images.exif.MetadataTag
import dev.tactos.feature.images.engine.ExifStripper
import dev.tactos.feature.images.engine.ImageLoading
import dev.tactos.feature.images.engine.SafWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MetadataInspectorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by rememberSaveable { mutableStateOf<Uri?>(null) }
    var info by remember { mutableStateOf<ImageLoading.Info?>(null) }
    var fields by remember { mutableStateOf<Map<MetadataTag, String>>(emptyMap()) }
    var preset by rememberSaveable { mutableStateOf(ExifRemovalPreset.Privacy) }
    var inspecting by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val busy = inspecting || saving
    var readable by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(source) {
        fields = emptyMap()
        info = null
        readable = false
        val uri = source ?: return@LaunchedEffect
        inspecting = true
        try {
            val inspection = withContext(Dispatchers.IO) {
                ImageLoading.info(context, uri) to inspectPhotoMetadata(context, uri)
            }
            info = inspection.first
            fields = inspection.second
            readable = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            message = "Couldn't inspect this photo. The format or provider may not support EXIF reading."
        } finally {
            inspecting = false
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            source = uri
            message = null
        }
    }
    val mime = info?.mimeType ?: "image/jpeg"
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mime)) { destination ->
        val input = source
        if (destination != null) {
            val selectedPreset = preset
            scope.launch {
                saving = true
                message = null
                var written = false
                try {
                    check(input != null) { "Choose the photo again before saving." }
                    check(destination != input) { "Choose a new destination separate from the original photo." }
                    val report = withContext(Dispatchers.IO) {
                        val result = ExifStripper(context).strip(input, ImageJob.ExifStrip(selectedPreset))
                            ?: error("This format couldn't be cleaned without changing pixels. No copy was saved.")
                        check(SafWriter.write(context, destination, result.bytes)) { "Couldn't write the cleaned copy." }
                        // Reopen the actual destination, not the in-memory bytes, to verify selected fields.
                        val remaining = inspectPhotoMetadata(context, destination)
                        val selected = if (selectedPreset == ExifRemovalPreset.AllMetadata) MetadataTag.entries else selectedPreset.tags
                        val stillPresent = remaining.keys.filter { it in selected }
                        if (stillPresent.isEmpty()) {
                            "Saved and verified: ${result.removedKeys.size} selected fields removed; none of the selected supported fields remain."
                        } else {
                            "Saved, but ${stillPresent.size} selected fields remain: ${stillPresent.joinToString { it.key }}."
                        }
                    }
                    written = true
                    message = report
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    message = error.message ?: "Couldn't save and verify the photo."
                } finally {
                    if (!written && destination != input) withContext(NonCancellable + Dispatchers.IO) { SafWriter.delete(context, destination) }
                    saving = false
                }
            }
        }
    }
    val cleanable = readable && mime in listOf("image/jpeg", "image/png", "image/webp") &&
        info?.sizeBytes?.let { it in 1..(32L * 1024 * 1024) } == true
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Inspect & clean metadata", style = MaterialTheme.typography.headlineSmall)
        Text("Inspect supported EXIF fields locally, then save and verify a cleaned copy.")
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
            Text(if (source == null) "Choose photo" else "Change photo")
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        info?.let {
            Text(it.displayName ?: "Selected photo", style = MaterialTheme.typography.titleMedium)
            Text("${it.width} × ${it.height} · ${it.mimeType ?: "Unknown format"}")
        }
        if (readable) {
            Text("${fields.size} supported fields found", style = MaterialTheme.typography.titleMedium)
            if (fields.isEmpty()) Text("No supported EXIF fields were found. Other metadata may still exist.")
            fields.forEach { (tag, value) ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tag.key, style = MaterialTheme.typography.labelLarge)
                        Text(value.take(500) + if (value.length > 500) "…" else "", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text("Remove", style = MaterialTheme.typography.titleMedium)
            listOf(
                ExifRemovalPreset.LocationOnly to "Location fields",
                ExifRemovalPreset.Privacy to "Personal information",
                ExifRemovalPreset.AllMetadata to "All supported EXIF fields",
            ).forEach { (option, label) ->
                FilterChip(selected = preset == option, onClick = { preset = option }, enabled = !busy, label = { Text(label) })
            }
            Button(onClick = {
                val extension = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
                save.launch("clean-photo.$extension")
            }, enabled = cleanable && !busy, modifier = Modifier.fillMaxWidth()) { Text("Save cleaned copy & verify") }
            if (!cleanable) Text("Cleaning supports JPEG, PNG and WebP with a known file size up to 32 MB.", style = MaterialTheme.typography.bodySmall)
        }
        message?.let { Text(it) }
        Text("Originals stay untouched. Orientation and color information are preserved. This checks the app's supported EXIF fields, not every metadata family, embedded preview, or visible detail. It does not guarantee anonymity.", style = MaterialTheme.typography.bodySmall)
    }
}

private fun inspectPhotoMetadata(context: Context, uri: Uri): Map<MetadataTag, String> =
    context.contentResolver.openInputStream(uri)?.use { input ->
        val exif = ExifInterface(input)
        MetadataTag.entries.mapNotNull { tag -> exif.getAttribute(tag.key)?.let { tag to it } }.toMap()
    } ?: error("Couldn't open the photo for metadata inspection.")
