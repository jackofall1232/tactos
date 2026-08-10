package dev.tactos.feature.images.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.tactos.core.images.ImageFormat
import dev.tactos.core.images.ImageJob
import dev.tactos.core.images.ResizeSpec
import dev.tactos.core.images.exif.ExifRemovalPreset
import dev.tactos.feature.images.ImageTool

// One composable per tool: each owns its saveable control state, builds the
// declarative ImageJob, and delegates layout/saving to ToolBody.

@Composable
internal fun ConvertTool(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
) {
    var formatName by rememberSaveable { mutableStateOf(ImageFormat.JPEG.name) }
    var quality by rememberSaveable { mutableIntStateOf(90) }
    val format = ImageFormat.valueOf(formatName)

    ToolBody(
        tool = tool,
        picked = picked,
        onPickImages = onPickImages,
        onClearPicked = onClearPicked,
        snackbarHostState = snackbarHostState,
        job = ImageJob.Convert(format, quality),
        outputFormat = format,
    ) { _ ->
        Text("Output format", style = MaterialTheme.typography.titleSmall)
        FormatChips(selected = format, onSelect = { formatName = it.name })
        if (format.supportsQuality) {
            QualitySlider(quality = quality, onChange = { quality = it }, format = format)
        }
    }
}

@Composable
internal fun ResizeTool(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
) {
    var mode by rememberSaveable { mutableStateOf(ResizeMode.FIT.name) }
    var boundText by rememberSaveable { mutableStateOf("1080") }
    var widthText by rememberSaveable { mutableStateOf("") }
    var heightText by rememberSaveable { mutableStateOf("") }
    var percentText by rememberSaveable { mutableStateOf("50") }
    var formatName by rememberSaveable { mutableStateOf(ImageFormat.JPEG.name) }
    var quality by rememberSaveable { mutableIntStateOf(90) }
    val format = ImageFormat.valueOf(formatName)

    val spec: ResizeSpec? = when (ResizeMode.valueOf(mode)) {
        ResizeMode.FIT -> boundText.toIntOrNull()?.takeIf { it > 0 }
            ?.let { ResizeSpec.Fit(it, it) }
        ResizeMode.EXACT -> {
            val w = widthText.toIntOrNull()
            val h = heightText.toIntOrNull()
            if (w != null && w > 0 && h != null && h > 0) ResizeSpec.Explicit(w, h) else null
        }
        ResizeMode.PERCENT -> percentText.toIntOrNull()
            ?.takeIf { it in 1..400 }
            ?.let { ResizeSpec.Percent(it / 100.0) }
    }

    ToolBody(
        tool = tool,
        picked = picked,
        onPickImages = onPickImages,
        onClearPicked = onClearPicked,
        snackbarHostState = snackbarHostState,
        job = spec?.let { ImageJob.Resize(it, format, quality) },
        outputFormat = format,
    ) { firstImage ->
        Text("Mode", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (candidate in ResizeMode.entries) {
                FilterChip(
                    selected = mode == candidate.name,
                    onClick = { mode = candidate.name },
                    label = { Text(candidate.label) },
                )
            }
        }
        when (ResizeMode.valueOf(mode)) {
            ResizeMode.FIT -> OutlinedTextField(
                value = boundText,
                onValueChange = { boundText = it.filter(Char::isDigit) },
                label = { Text("Longest side (px)") },
                supportingText = { Text("Keeps aspect ratio, never upscales") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ResizeMode.EXACT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it.filter(Char::isDigit) },
                    label = { Text("Width") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter(Char::isDigit) },
                    label = { Text("Height") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            ResizeMode.PERCENT -> OutlinedTextField(
                value = percentText,
                onValueChange = { percentText = it.filter(Char::isDigit) },
                label = { Text("Scale (%)") },
                supportingText = { Text("1–400") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // Live preview from the first picked image's real dimensions.
        if (spec != null && firstImage != null) {
            val target = spec.resolveTargetSize(firstImage.width, firstImage.height)
            Text(
                text = "${firstImage.width}×${firstImage.height} → ${target.width}×${target.height}" +
                    if (picked.size > 1) " (first image)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text("Output format", style = MaterialTheme.typography.titleSmall)
        FormatChips(selected = format, onSelect = { formatName = it.name })
        if (format.supportsQuality) {
            QualitySlider(quality = quality, onChange = { quality = it }, format = format)
        }
    }
}

@Composable
internal fun CompressTool(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
) {
    var byTarget by rememberSaveable { mutableStateOf(false) }
    var quality by rememberSaveable { mutableIntStateOf(70) }
    var targetKbText by rememberSaveable { mutableStateOf("500") }
    var formatName by rememberSaveable { mutableStateOf(ImageFormat.JPEG.name) }
    val format = ImageFormat.valueOf(formatName)

    val job: ImageJob? = if (byTarget) {
        targetKbText.toLongOrNull()?.takeIf { it > 0 }
            ?.let { ImageJob.Compress(format, targetBytes = it * 1024) }
    } else {
        ImageJob.Compress(format, quality = quality)
    }

    ToolBody(
        tool = tool,
        picked = picked,
        onPickImages = onPickImages,
        onClearPicked = onClearPicked,
        snackbarHostState = snackbarHostState,
        job = job,
        outputFormat = format,
    ) { firstImage ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !byTarget,
                onClick = { byTarget = false },
                label = { Text("By quality") },
            )
            FilterChip(
                selected = byTarget,
                onClick = { byTarget = true },
                label = { Text("To file size") },
            )
        }
        if (byTarget) {
            OutlinedTextField(
                value = targetKbText,
                onValueChange = { targetKbText = it.filter(Char::isDigit) },
                label = { Text("Target size (KB)") },
                supportingText = {
                    Text("Lowers quality first, then dimensions, until it fits")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            QualitySlider(quality = quality, onChange = { quality = it }, format = format)
        }
        firstImage?.sizeBytes?.let { bytes ->
            Text(
                text = "Original: ${formatBytes(bytes)}" +
                    if (picked.size > 1) " (first image)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Format", style = MaterialTheme.typography.titleSmall)
        FormatChips(
            selected = format,
            onSelect = { formatName = it.name },
            // PNG can't compress by quality; keep the choice honest.
            options = ImageFormat.entries.filter { it.supportsQuality },
        )
    }
}

@Composable
internal fun ExifTool(
    tool: ImageTool,
    picked: List<Uri>,
    onPickImages: () -> Unit,
    onClearPicked: () -> Unit,
    snackbarHostState: SnackbarHostState?,
) {
    var presetName by rememberSaveable { mutableStateOf(ExifRemovalPreset.Privacy.name) }
    val preset = ExifRemovalPreset.valueOf(presetName)

    ToolBody(
        tool = tool,
        picked = picked,
        onPickImages = onPickImages,
        onClearPicked = onClearPicked,
        snackbarHostState = snackbarHostState,
        job = ImageJob.ExifStrip(preset),
        // Keep each source's own format: lossless strip when possible.
        outputFormat = null,
    ) { _ ->
        Text("What to remove", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((candidate, label) in presetLabels) {
                FilterChip(
                    selected = preset == candidate,
                    onClick = { presetName = candidate.name },
                    label = { Text(label) },
                )
            }
        }
        Text(
            text = when (preset) {
                ExifRemovalPreset.AllMetadata ->
                    "Every removable tag is cleared."
                ExifRemovalPreset.Privacy ->
                    "Location, dates, device identity, and authorship — " +
                        "${preset.tags.size} tags."
                ExifRemovalPreset.LocationOnly ->
                    "GPS tags only — ${preset.tags.size} tags. Everything else stays."
                ExifRemovalPreset.KeepDateAndCopyright ->
                    "Everything except dates and copyright — ${preset.tags.size} tags."
                ExifRemovalPreset.Custom -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Pixels are untouched when the format allows it; otherwise the " +
                "image is re-encoded (which also drops all metadata).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class ResizeMode(val label: String) {
    FIT("Fit within"),
    EXACT("Exact size"),
    PERCENT("Percent"),
}

private val presetLabels = listOf(
    ExifRemovalPreset.Privacy to "Privacy",
    ExifRemovalPreset.AllMetadata to "Everything",
    ExifRemovalPreset.LocationOnly to "Location only",
    ExifRemovalPreset.KeepDateAndCopyright to "Keep date/©",
)

@Composable
private fun FormatChips(
    selected: ImageFormat,
    onSelect: (ImageFormat) -> Unit,
    options: List<ImageFormat> = ImageFormat.entries,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (format in options) {
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(format.name) },
            )
        }
    }
}

@Composable
private fun QualitySlider(quality: Int, onChange: (Int) -> Unit, format: ImageFormat) {
    Text(
        text = "Quality: $quality" +
            if (format == ImageFormat.WEBP && quality >= 100) " (lossless)" else "",
        style = MaterialTheme.typography.bodyMedium,
    )
    Slider(
        value = quality.toFloat(),
        onValueChange = { onChange(it.toInt().coerceIn(1, 100)) },
        valueRange = 1f..100f,
    )
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
