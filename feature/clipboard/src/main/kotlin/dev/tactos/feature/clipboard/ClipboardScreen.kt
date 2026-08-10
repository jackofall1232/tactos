package dev.tactos.feature.clipboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tactos.core.database.ClipRepository
import dev.tactos.core.detect.ContentDetector
import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The clipboard timeline: search, type/category filter chips, pinned-first
 * list, manual add, and a detail sheet with copy/share/pin/favorite/delete.
 */
@Composable
fun ClipboardScreen(
    repository: ClipRepository,
    modifier: Modifier = Modifier,
    /** Invoked after any save so the host can enforce retention rules. */
    afterSave: suspend () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<ClipType?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    // Bumped after each mutation so an active search re-queries the database.
    var refresh by remember { mutableStateOf(0) }

    val timeline by repository.timeline().collectAsState(initial = emptyList())
    var searchResults by remember { mutableStateOf<List<ClipItem>?>(null) }
    LaunchedEffect(query, refresh, timeline) {
        if (query.isBlank()) {
            searchResults = null
        } else {
            // Debounce keystrokes; effect restart cancels the pending query.
            delay(300)
            searchResults = repository.search(query)
        }
    }

    val base = searchResults ?: timeline
    val shown = base.applyFilter(TimelineFilter(typeFilter, categoryFilter))
    val categories = timeline.distinctCategories()
    val typesPresent = timeline.distinctTypes()
    // Resolved from the database, not the limit-capped timeline list, so a
    // search hit older than the cap still opens; re-keyed on refresh/timeline
    // so the open sheet reflects pin/favorite/category mutations.
    var detailItem by remember { mutableStateOf<ClipItem?>(null) }
    LaunchedEffect(detailId, refresh, timeline) {
        detailItem = detailId?.let { repository.byId(it) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search history") },
                singleLine = true,
            )

            AnimatedVisibility(visible = typesPresent.size > 1) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = typeFilter == null,
                            onClick = { typeFilter = null },
                            label = { Text("All") },
                        )
                    }
                    items(typesPresent) { type ->
                        FilterChip(
                            selected = typeFilter == type,
                            onClick = { typeFilter = if (typeFilter == type) null else type },
                            label = { Text("${type.emoji} ${type.label()}") },
                        )
                    }
                }
            }

            AnimatedVisibility(visible = categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = categoryFilter == category,
                            onClick = {
                                categoryFilter = if (categoryFilter == category) null else category
                            },
                            label = { Text("🏷 $category") },
                        )
                    }
                }
            }

            Crossfade(targetState = shown.isEmpty(), label = "timeline-empty") { empty ->
                if (empty) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (timeline.isEmpty()) {
                                "Nothing here yet.\nCopy something, share text to tactos, or tap + to add a clip."
                            } else {
                                "No clips match."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        // Extra bottom padding so the last clip scrolls clear of the FAB.
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(shown, key = { it.id }) { item ->
                            ClipRow(
                                item = item,
                                onClick = { detailId = item.id },
                                onToggleFavorite = {
                                    scope.launch {
                                        repository.setFavorite(item.id, !item.favorite)
                                        refresh++
                                    }
                                },
                                // Animate reorders (pin), inserts (capture), and removals.
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add clip")
        }
    }

    if (showAdd) {
        AddClipDialog(
            onDismiss = { showAdd = false },
            onAdd = { text ->
                scope.launch {
                    repository.save(
                        ClipItem(
                            text = text,
                            type = ContentDetector.detect(text),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    afterSave()
                    refresh++
                }
                showAdd = false
            },
        )
    }

    detailItem?.let { item ->
        ClipDetailSheet(
            item = item,
            onDismiss = { detailId = null },
            onTogglePin = {
                scope.launch {
                    repository.setPinned(item.id, !item.pinned)
                    refresh++
                }
            },
            onToggleFavorite = {
                scope.launch {
                    repository.setFavorite(item.id, !item.favorite)
                    refresh++
                }
            },
            onSetCategory = { category ->
                scope.launch {
                    repository.setCategory(item.id, category.ifBlank { null })
                    refresh++
                }
            },
            onSaveAsClip = { text ->
                scope.launch {
                    repository.save(
                        ClipItem(
                            text = text,
                            type = ContentDetector.detect(text),
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                    afterSave()
                    refresh++
                }
            },
            onDelete = {
                scope.launch {
                    repository.delete(item.id)
                    refresh++
                }
                detailId = null
            },
        )
    }
}

internal fun ClipType.label(): String = when (this) {
    ClipType.URL -> "URL"
    ClipType.IP_ADDRESS -> "IP"
    ClipType.COLOR -> "Color"
    ClipType.JSON -> "JSON"
    ClipType.EMAIL -> "Email"
    ClipType.PHONE -> "Phone"
    ClipType.TEXT -> "Text"
}

@Composable
private fun ClipRow(
    item: ClipItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.type.emoji,
                style = MaterialTheme.typography.titleLarge,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.type.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (item.pinned) Text("📌", style = MaterialTheme.typography.labelSmall)
                    item.category?.let {
                        Text(
                            text = "🏷 $it",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = onToggleFavorite) {
                Text(if (item.favorite) "⭐" else "☆")
            }
        }
    }
}
