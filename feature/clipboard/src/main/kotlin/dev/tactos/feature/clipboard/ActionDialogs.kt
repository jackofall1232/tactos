package dev.tactos.feature.clipboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.tactos.core.actions.ColorValue
import dev.tactos.core.actions.QrCode
import kotlin.math.roundToInt

/** Offline QR code for [text], rendered straight from the module matrix. */
@Composable
internal fun QrDialog(text: String, onDismiss: () -> Unit) {
    val matrix = remember(text) { QrCode.encode(text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR code") },
        text = {
            if (matrix == null) {
                Text("Couldn't generate a QR code for this clip.")
            } else {
                Column {
                    // The matrix includes its quiet zone; the white canvas
                    // keeps it scannable on dark themes.
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                    ) {
                        val cell = size.minDimension / matrix.size
                        for (y in 0 until matrix.size) {
                            for (x in 0 until matrix.size) {
                                if (matrix[x, y]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x * cell, y * cell),
                                        size = Size(cell, cell),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/** Swatch preview plus HEX/RGB/HSL/HSV representations with tap-to-copy. */
@Composable
internal fun ColorDialog(
    color: ColorValue,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Color(
                                red = color.red,
                                green = color.green,
                                blue = color.blue,
                                alpha = (color.alpha * 255).roundToInt(),
                            ),
                        ),
                )
                listOf(
                    "HEX" to color.toHexString(),
                    "RGB" to color.toRgbString(),
                    "HSL" to color.toHslString(),
                    "HSV" to color.toHsvString(),
                ).forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(44.dp),
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onCopy(value) }) { Text("Copy") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

/** Scrollable text output (JSON results, verdicts, action errors). */
@Composable
internal fun TextResultDialog(
    title: String,
    body: String,
    isError: Boolean,
    onCopy: ((String) -> Unit)?,
    onSaveAsClip: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = if (isError) null else FontFamily.Monospace,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            Row {
                if (onCopy != null) {
                    TextButton(onClick = { onCopy(body) }) { Text("Copy") }
                }
                if (onSaveAsClip != null) {
                    TextButton(onClick = {
                        onSaveAsClip(body)
                        onDismiss()
                    }) { Text("Save as clip") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
