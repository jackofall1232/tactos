package dev.tactos.feature.images.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tactos.core.images.*
import dev.tactos.feature.images.engine.*
import kotlinx.coroutines.*

private val EditSaver = listSaver<PhotoEditSpec, Any>(
    save = { listOf(it.crop.left, it.crop.top, it.crop.right, it.crop.bottom,
        it.quarterTurns, it.flipHorizontal, it.flipVertical, it.brightness, it.contrast,
        it.saturation, it.warmth, it.filter.name, it.watermark?.text.orEmpty()) },
    restore = { PhotoEditSpec(crop = PhotoCrop(it[0] as Float, it[1] as Float, it[2] as Float, it[3] as Float),
        quarterTurns = it[4] as Int, flipHorizontal = it[5] as Boolean, flipVertical = it[6] as Boolean,
        brightness = it[7] as Float, contrast = it[8] as Float, saturation = it[9] as Float,
        warmth = it[10] as Float, filter = PhotoFilter.valueOf(it[11] as String),
        watermark = (it[12] as String).takeIf(String::isNotBlank)?.let { text -> PhotoWatermark(text) }) },
)

/** Non-destructive, local photo editor. Preview and export use the same rendering pipeline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoStudioScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uriString by rememberSaveable { mutableStateOf<String?>(null) }
    var edit by rememberSaveable(stateSaver = EditSaver) { mutableStateOf(PhotoEditSpec()) }
    var tab by rememberSaveable { mutableStateOf("Adjust") }
    var format by rememberSaveable { mutableStateOf("JPEG") }
    var before by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var rendering by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val undo = remember { mutableStateListOf<PhotoEditSpec>() }
    val redo = remember { mutableStateListOf<PhotoEditSpec>() }
    var sliderStart by remember { mutableStateOf<PhotoEditSpec?>(null) }
    fun beginSlider() { sliderStart = edit }
    fun finishSlider() {
        val initial = sliderStart
        sliderStart = null
        if (initial != null && initial != edit) {
            if (undo.size >= 50) undo.removeAt(0)
            undo.add(initial)
            redo.clear()
        }
    }
    fun update(value: PhotoEditSpec) {
        if (value != edit) {
            if (undo.size >= 50) undo.removeAt(0)
            undo.add(edit); redo.clear(); edit = value
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uriString = uri.toString(); edit = PhotoEditSpec(); undo.clear(); redo.clear(); message = null
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        val exportPath = pending
        pending = null
        val exportFile = exportPath?.let { java.io.File(context.cacheDir, it) }
        if (result.resultCode == android.app.Activity.RESULT_OK && uri != null) {
            if (uri.toString() == uriString) {
                exportFile?.delete()
                message = "Choose a new file. Your original will not be overwritten."
            } else scope.launch(start = CoroutineStart.UNDISPATCHED) {
                busy = true
                var written = false
                try {
                    check(exportFile != null) { "Export expired" }
                    written = withContext(Dispatchers.IO) { SafWriter.write(context, uri, exportFile.readBytes()) }
                    message = if (written) "Edited copy saved. Your original is unchanged." else "Could not save. Please choose another location."
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { message = "Export expired. Please save the photo again." }
                finally {
                    withContext(NonCancellable + Dispatchers.IO) {
                        if (!written) SafWriter.delete(context, uri)
                        exportFile?.delete()
                    }
                    busy = false
                }
            }
        } else { exportFile?.delete() }
    }
    LaunchedEffect(uriString) {
        source = null; preview = null
        val uri = uriString ?: return@LaunchedEffect
        busy = true
        try {
            source = withContext(Dispatchers.IO) { ImageLoading.decode(context, Uri.parse(uri), 1024) }
            if (source == null) message = "This photo could not be opened. Choose it again or try another file."
        } finally { busy = false }
    }
    LaunchedEffect(source, edit) {
        val bitmap = source ?: return@LaunchedEffect
        rendering = true
        try {
            delay(100)
            preview = withContext(Dispatchers.Default) { PhotoEditor.render(bitmap, edit) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { message = "Could not render these adjustments. Try resetting them." }
        finally { rendering = false }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Photo studio", style = MaterialTheme.typography.headlineMedium)
        Text("Make it yours. Every edit stays on your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (source == null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Start with a photo", style = MaterialTheme.typography.titleLarge)
                    Text("Crop, rotate, adjust light and color, apply a look, or add your watermark.")
                    Button(enabled = !busy, onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose photo") }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                val shown = if (before) source else preview ?: source
                shown?.let { Image(it.asImageBitmap(), if (before) "Original photo" else "Edited photo preview", Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp), contentScale = ContentScale.Fit) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = before, onClick = { before = !before }, label = { Text(if (before) "Showing original" else "Compare original") })
                TextButton(enabled = !busy && undo.isNotEmpty(), onClick = { redo.add(edit); edit = undo.removeAt(undo.lastIndex) }) { Text("Undo") }
                TextButton(enabled = !busy && redo.isNotEmpty(), onClick = { undo.add(edit); edit = redo.removeAt(redo.lastIndex) }) { Text("Redo") }
                TextButton(enabled = !busy, onClick = { update(PhotoEditSpec()) }) { Text("Reset") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Adjust", "Crop", "Looks", "Watermark").forEach { name -> FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name) }) }
            }
            when (tab) {
                "Adjust" -> {
                    OutlinedButton(enabled = !busy, onClick = {
                        val bitmap = source ?: return@OutlinedButton
                        val suggestion = PhotoAutoEnhance.suggest(bitmap)
                        update(edit.copy(brightness = suggestion.brightness, contrast = suggestion.contrast, saturation = suggestion.saturation))
                    }) { Text("Auto enhance · local") }
                    StudioSlider("Brightness", edit.brightness, -1f..1f, !busy, ::beginSlider, ::finishSlider) { edit = edit.copy(brightness = it) }
                    StudioSlider("Contrast", edit.contrast, 0f..2f, !busy, ::beginSlider, ::finishSlider) { edit = edit.copy(contrast = it) }
                    StudioSlider("Saturation", edit.saturation, 0f..2f, !busy, ::beginSlider, ::finishSlider) { edit = edit.copy(saturation = it) }
                    StudioSlider("Warmth", edit.warmth, -1f..1f, !busy, ::beginSlider, ::finishSlider) { edit = edit.copy(warmth = it) }
                }
                "Crop" -> {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mapOf("Original" to 0f, "Square" to 1f, "4:3" to 4f/3, "3:2" to 1.5f, "16:9" to 16f/9, "9:16" to 9f/16).forEach { (name, ratio) ->
                            OutlinedButton(enabled = !busy, onClick = {
                                val s = source ?: return@OutlinedButton
                                val originalRatio = s.width.toFloat()/s.height
                                val sourceRatio = if (ratio > 0f && edit.quarterTurns % 2 != 0) 1f / ratio else ratio
                                val w = if (sourceRatio == 0f || sourceRatio >= originalRatio) 1f else sourceRatio/originalRatio
                                val h = if (sourceRatio == 0f || sourceRatio <= originalRatio) 1f else originalRatio/sourceRatio
                                update(edit.copy(crop = PhotoCrop((1-w)/2, (1-h)/2, (1+w)/2, (1+h)/2)))
                            }) { Text(name) }
                        }
                    }
                    val crop = edit.crop
                    val w = crop.right - crop.left; val h = crop.bottom - crop.top
                    if (w < .999f || h < .999f) Text("Crop position follows the original photo, before rotation and flips.", style = MaterialTheme.typography.bodySmall)
                    if (w < .999f) StudioSlider("Source horizontal position", crop.left / (1-w), 0f..1f, !busy, ::beginSlider, ::finishSlider) {
                        val left = it*(1-w); edit = edit.copy(crop = crop.copy(left = left, right = left+w))
                    }
                    if (h < .999f) StudioSlider("Source vertical position", crop.top / (1-h), 0f..1f, !busy, ::beginSlider, ::finishSlider) {
                        val top = it*(1-h); edit = edit.copy(crop = crop.copy(top = top, bottom = top+h))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { update(edit.copy(quarterTurns = (edit.quarterTurns+1)%4)) }) { Text("Rotate 90°") }
                        FilterChip(enabled = !busy, selected = edit.flipHorizontal, onClick = { update(edit.copy(flipHorizontal = !edit.flipHorizontal)) }, label = { Text("Flip horizontal") })
                        FilterChip(enabled = !busy, selected = edit.flipVertical, onClick = { update(edit.copy(flipVertical = !edit.flipVertical)) }, label = { Text("Flip vertical") })
                    }
                }
                "Looks" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoFilter.entries.forEach { filter -> FilterChip(enabled = !busy, selected = edit.filter == filter, onClick = { update(edit.copy(filter = filter)) }, label = { Text(filter.name.lowercase().replaceFirstChar(Char::titlecase)) }) }
                }
                "Watermark" -> OutlinedTextField(value = edit.watermark?.text.orEmpty(), onValueChange = { text ->
                    if (text.length <= 120) update(edit.copy(watermark = text.takeIf(String::isNotBlank)?.let { PhotoWatermark(it) }))
                }, enabled = !busy, label = { Text("Watermark text") }, supportingText = { Text("Placed at the bottom right of the exported photo.") }, modifier = Modifier.fillMaxWidth())
            }
            HorizontalDivider()
            Text("Save a new copy", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageFormat.entries.forEach { type -> FilterChip(enabled = !busy, selected = format == type.name, onClick = { format = type.name }, label = { Text(type.name) }) }
            }
            Text("Export processes photos up to 4096 px on the long side, with a lower limit on devices with less memory. Original EXIF fields are not copied. JPEG uses a white background for transparency.", style = MaterialTheme.typography.bodySmall)
            Button(modifier = Modifier.fillMaxWidth(), enabled = !busy && pending == null, onClick = {
                val selected = uriString ?: return@Button
                val snapshot = edit
                val outputFormat = ImageFormat.valueOf(format)
                scope.launch {
                    busy = true; message = null
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            val maxDimension = PhotoExportBudget.maxDimension(Runtime.getRuntime().maxMemory())
                            val original = ImageLoading.decode(context, Uri.parse(selected), maxDimension) ?: error("Photo is no longer available")
                            try {
                                val rendered = PhotoEditor.render(original, snapshot)
                                try { ImageEncoders.encodeToBytes(rendered, outputFormat, 95) } finally { rendered.recycle() }
                            } finally { original.recycle() }
                        }
                        pending = withContext(Dispatchers.IO) {
                            java.io.File.createTempFile("studio-export-", ".tmp", context.cacheDir).apply { writeBytes(bytes) }.name
                        }
                        save.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE); type = outputFormat.mimeType
                            putExtra(Intent.EXTRA_TITLE, "tactos-edit.${outputFormat.extension}")
                        })
                    } catch (e: CancellationException) { throw e }
                    catch (_: OutOfMemoryError) { pending = null; message = "There is not enough memory to export this photo. Close other tools or choose a smaller image and try again." }
                    catch (_: Exception) { pending = null; message = "Could not export this photo. Try a smaller image or choose it again." }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Preparing…" else "Save edited photo") }
            TextButton(enabled = !busy && pending == null, onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Choose another photo") }
        }
        if (busy || rendering) LinearProgressIndicator(Modifier.fillMaxWidth())
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun StudioSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onChange: (Float) -> Unit,
) {
    var changing by remember { mutableStateOf(false) }
    Column {
        Text("$label · ${"%.2f".format(value)}", style = MaterialTheme.typography.labelLarge)
        Slider(value = value.coerceIn(range), onValueChange = {
            if (!changing) { changing = true; onStart() }
            onChange(it)
        }, onValueChangeFinished = {
            changing = false
            onFinish()
        }, valueRange = range, enabled = enabled)
    }
}
