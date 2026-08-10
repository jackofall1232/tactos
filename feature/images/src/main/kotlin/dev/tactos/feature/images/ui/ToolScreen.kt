package dev.tactos.feature.images.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tactos.core.images.ImageFormat
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.rename.RenameInput
import dev.tactos.core.images.rename.RenameResolver
import dev.tactos.core.images.rename.RenameValidator
import dev.tactos.feature.images.ImageTool
import dev.tactos.feature.images.ImagesToolbox
import dev.tactos.feature.images.engine.ExifStripper
import dev.tactos.feature.images.engine.ImageLoading
import dev.tactos.feature.images.engine.ImageProcessor
import dev.tactos.feature.images.engine.SafWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dispatch to the tool's screen. Each tool owns its control state. */
@Composable
fun ToolScreen(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
) {
    when (tool.id) {
        ImagesToolbox.TOOL_CONVERT -> ConvertTool(tool, picked, onPickImages, onClearPicked, snackbarHostState)
        ImagesToolbox.TOOL_RESIZE -> ResizeTool(tool, picked, onPickImages, onClearPicked, snackbarHostState)
        ImagesToolbox.TOOL_COMPRESS -> CompressTool(tool, picked, onPickImages, onClearPicked, snackbarHostState)
        ImagesToolbox.TOOL_EXIF -> ExifTool(tool, picked, onPickImages, onClearPicked, snackbarHostState)
    }
}

/**
 * Shared tool layout (the slot idea behind ImageToolbox's
 * AdaptiveLayoutScreen, lean): picked strip -> controls -> naming -> save.
 * [job] null disables saving (controls incomplete); [outputFormat] null
 * means "keep each source's own format" (EXIF strip).
 */
@Composable
internal fun ToolBody(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
    job: ImageJob?,
    outputFormat: ImageFormat?,
    controls: @Composable ColumnScope.(firstImage: ImageLoading.Info?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var infos by remember(picked) { mutableStateOf<Map<Uri, ImageLoading.Info?>>(emptyMap()) }
    androidx.compose.runtime.LaunchedEffect(picked) {
        infos = withContext(Dispatchers.IO) {
            picked.associateWith { ImageLoading.info(context, it) }
        }
    }

    var namePattern by rememberSaveable(tool.id) { mutableStateOf("{name}") }
    val patternErrors = RenameValidator.validate(namePattern, batchSize = picked.size)

    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var runJob by remember { mutableStateOf<Job?>(null) }

    suspend fun announce(message: String) {
        snackbarHostState?.showSnackbar(message)
    }

    fun formatFor(info: ImageLoading.Info?): ImageFormat =
        outputFormat
            ?: ImageFormat.fromMimeType(info?.mimeType)
            ?: ImageFormat.JPEG

    val singleSave = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            formatFor(infos.values.firstOrNull()).mimeType,
        ),
    ) { uri ->
        val source = picked.firstOrNull() ?: return@rememberLauncherForActivityResult
        val activeJob = job ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        runJob = scope.launch {
            progress = 0 to 1
            val bytes = withContext(Dispatchers.Default) {
                processOne(context, source, activeJob, formatFor(infos[source]))
            }
            val ok = bytes != null && SafWriter.write(context, uri, bytes)
            progress = null
            announce(if (ok) "Saved" else "Couldn't process this image")
        }
    }

    val batchSave = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        val activeJob = job
        if (treeUri == null || activeJob == null) return@rememberLauncherForActivityResult
        runJob = scope.launch {
            val resolver = RenameResolver(nowMillis = System.currentTimeMillis())
            var failures = 0
            progress = 0 to picked.size
            for ((index, source) in picked.withIndex()) {
                val info = infos[source]
                val format = formatFor(info)
                val bytes = withContext(Dispatchers.Default) {
                    processOne(context, source, activeJob, format)
                }
                val name = resolver.resolve(
                    namePattern,
                    RenameInput(
                        originalName = ImageLoading.baseName(info).ifEmpty { "image" },
                        index = index,
                        takenAtMillis = null,
                        width = info?.width,
                        height = info?.height,
                    ),
                ).ifBlank { "image-${index + 1}" }
                val written = bytes != null && SafWriter.createInTree(
                    context, treeUri, "$name.${format.extension}", format.mimeType, bytes,
                ) != null
                if (!written) failures++
                progress = (index + 1) to picked.size
            }
            progress = null
            announce(
                if (failures == 0) "Saved ${picked.size} images"
                else "Saved ${picked.size - failures} of ${picked.size} — $failures failed",
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (picked.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = tool.emoji, style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                    Button(onClick = onPickImages) { Text("Pick images") }
                }
            }
        } else {
            PickedImageStrip(picked = picked, infos = infos)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPickImages) { Text("Change") }
                TextButton(onClick = onClearPicked) { Text("Clear") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    controls(infos.values.firstOrNull())
                }
            }

            if (picked.size > 1) {
                OutlinedTextField(
                    value = namePattern,
                    onValueChange = { namePattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("File name pattern") },
                    supportingText = {
                        Text(
                            patternErrors.firstOrNull()?.let(::describeError)
                                ?: "Tokens: {name} {n} {n:3} {date} {date:yyyy-MM-dd} {w} {h} {rand}",
                        )
                    },
                    isError = patternErrors.isNotEmpty(),
                    singleLine = true,
                )
            }

            val running = progress != null
            if (running) {
                val (done, total) = progress!!
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Processing $done / $total",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { runJob?.cancel(); progress = null }) {
                            Text("Cancel")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (picked.size == 1) {
                        val info = infos[picked.first()]
                        val format = formatFor(info)
                        val base = ImageLoading.baseName(info).ifEmpty { "image" }
                        singleSave.launch("$base.${format.extension}")
                    } else {
                        batchSave.launch(null)
                    }
                },
                enabled = job != null && !running && (picked.size == 1 || patternErrors.isEmpty()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (picked.size == 1) "Save" else "Save all (${picked.size})")
            }
            Text(
                text = "Saved as new files — originals stay untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Decode -> process for bitmap jobs; lossless strip (with re-encode fallback) for EXIF. */
internal fun processOne(
    context: Context,
    source: Uri,
    job: ImageJob,
    sourceFormat: ImageFormat,
): ByteArray? = when (job) {
    is ImageJob.ExifStrip -> {
        ExifStripper(context).strip(source, job)?.bytes
            ?: ImageLoading.decode(context, source)?.let { bitmap ->
                try {
                    // Fallback: re-encoding drops all metadata by construction.
                    ImageProcessor.process(
                        bitmap,
                        ImageJob.Convert(sourceFormat, quality = REENCODE_QUALITY),
                    ).bytes
                } finally {
                    bitmap.recycle()
                }
            }
    }
    else -> ImageLoading.decode(context, source)?.let { bitmap ->
        try {
            ImageProcessor.process(bitmap, job).bytes
        } finally {
            bitmap.recycle()
        }
    }
}

internal fun describeError(error: dev.tactos.core.images.rename.RenameValidationError): String =
    when (error) {
        is dev.tactos.core.images.rename.RenameValidationError.BlankPattern ->
            "Pattern can't be empty"
        is dev.tactos.core.images.rename.RenameValidationError.UnknownToken ->
            "Unknown token {${error.token}}"
        is dev.tactos.core.images.rename.RenameValidationError.IllegalCharacter ->
            "'${error.char}' can't be used in file names"
        is dev.tactos.core.images.rename.RenameValidationError.BadDateFormat ->
            "Bad date format: ${error.format}"
        is dev.tactos.core.images.rename.RenameValidationError.NotUniquePerFile ->
            "Add {n}, {name}, or {rand} so each file gets a unique name"
    }

private const val REENCODE_QUALITY = 95
