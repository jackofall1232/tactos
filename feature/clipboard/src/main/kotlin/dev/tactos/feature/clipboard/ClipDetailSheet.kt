package dev.tactos.feature.clipboard

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tactos.core.actions.ColorValue
import dev.tactos.core.model.ClipItem
import dev.tactos.feature.clipboard.actions.ActionEffect
import dev.tactos.feature.clipboard.actions.ClipActionExecutors
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

/**
 * Full clip detail: selectable text, type + timestamps, category editor, and
 * the clip's actions. Actions are rendered from the [ClipboardToolbox]
 * descriptors and dispatched through the [ClipActionExecutors] registry; this
 * sheet only interprets the returned [ActionEffect]s.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ClipDetailSheet(
    item: ClipItem,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetCategory: (String) -> Unit,
    onSaveAsClip: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var categoryDraft by rememberSaveable(item.id) { mutableStateOf(item.category.orEmpty()) }
    val timeFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    var dialogEffect by remember(item.id) { mutableStateOf<ActionEffect?>(null) }

    fun handle(effect: ActionEffect) {
        when (effect) {
            is ActionEffect.CopyText -> copyToClipboard(context, effect.text)
            is ActionEffect.ShareText -> {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, effect.text)
                }
                context.startActivity(Intent.createChooser(send, "Share clip"))
            }
            is ActionEffect.OpenUrl -> try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
            } catch (e: ActivityNotFoundException) {
                dialogEffect = ActionEffect.ShowTextResult(
                    title = "Open link",
                    body = "No app on this device can open the link.",
                    isError = true,
                )
            }
            ActionEffect.TogglePin -> onTogglePin()
            is ActionEffect.ShowQr,
            is ActionEffect.ShowColor,
            is ActionEffect.ShowTextResult,
            -> dialogEffect = effect
        }
    }

    fun run(actionId: String) {
        ClipActionExecutors.forId(actionId)?.invoke(item)?.let(::handle)
    }

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${item.type.emoji} ${item.type.label()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Inline swatch for color clips.
                ColorValue.parse(item.text)?.let { color ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Color(
                                    red = color.red,
                                    green = color.green,
                                    blue = color.blue,
                                    alpha = (color.alpha * 255).roundToInt(),
                                ),
                            ),
                    )
                }
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

            // Type-specific contextual actions, straight from the registry.
            val typeActions = remember(item.type) {
                ClipboardToolbox.clipActions().filter {
                    item.type in it.appliesTo && it.id !in ClipboardToolbox.universalActionIds
                }
            }
            if (typeActions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    typeActions.forEach { action ->
                        AssistChip(
                            onClick = { run(action.id) },
                            label = { Text(action.label) },
                        )
                    }
                }
            }

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
                TextButton(onClick = { run(ClipboardToolbox.ACTION_COPY) }) { Text("📄 Copy") }
                TextButton(onClick = { run(ClipboardToolbox.ACTION_SHARE) }) { Text("↗ Share") }
                TextButton(onClick = { run(ClipboardToolbox.ACTION_PIN) }) {
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

    when (val effect = dialogEffect) {
        is ActionEffect.ShowQr -> QrDialog(
            text = effect.text,
            onDismiss = { dialogEffect = null },
        )
        is ActionEffect.ShowColor -> ColorDialog(
            color = effect.color,
            onCopy = { copyToClipboard(context, it) },
            onDismiss = { dialogEffect = null },
        )
        is ActionEffect.ShowTextResult -> TextResultDialog(
            title = effect.title,
            body = effect.body,
            isError = effect.isError,
            onCopy = if (effect.isError) null else { text -> copyToClipboard(context, text) },
            onSaveAsClip = if (effect.savable) onSaveAsClip else null,
            onDismiss = { dialogEffect = null },
        )
        else -> Unit
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("tactos", text))
}
