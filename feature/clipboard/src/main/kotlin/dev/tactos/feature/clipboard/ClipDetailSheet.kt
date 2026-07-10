package dev.tactos.feature.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tactos.core.model.ClipItem
import java.text.DateFormat
import java.util.Date

/** Full clip detail: selectable text, type + timestamps, category editor, actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipDetailSheet(
    item: ClipItem,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetCategory: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var categoryDraft by rememberSaveable(item.id) { mutableStateOf(item.category.orEmpty()) }
    val timeFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${item.type.emoji} ${item.type.label()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (item.pinned) Text("📌", style = MaterialTheme.typography.titleMedium)
                if (item.favorite) Text("⭐", style = MaterialTheme.typography.titleMedium)
            }

            SelectionContainer {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = buildString {
                    append("Copied ").append(timeFormat.format(Date(item.createdAt)))
                    if (item.updatedAt != item.createdAt) {
                        append(" · seen again ").append(timeFormat.format(Date(item.updatedAt)))
                    }
                    item.sourceApp?.let { append(" · from ").append(it) }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = categoryDraft,
                onValueChange = { categoryDraft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category") },
                singleLine = true,
                trailingIcon = {
                    if (categoryDraft != item.category.orEmpty()) {
                        TextButton(onClick = { onSetCategory(categoryDraft.trim()) }) {
                            Text("Save")
                        }
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(ClipData.newPlainText("tactos", item.text))
                }) { Text("📄 Copy") }
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, item.text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share clip"))
                }) { Text("↗ Share") }
                TextButton(onClick = onTogglePin) {
                    Text(if (item.pinned) "📌 Unpin" else "📌 Pin")
                }
                TextButton(onClick = onToggleFavorite) {
                    Text(if (item.favorite) "⭐ Unfavorite" else "☆ Favorite")
                }
            }
            TextButton(onClick = onDelete) {
                Text("🗑 Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
