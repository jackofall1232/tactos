package dev.tactos.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType

/**
 * Room row for one timeline entry. [type] is stored as the [ClipType] name
 * string (not an ordinal) so rows survive enum reordering; unknown values map
 * back to [ClipType.TEXT]. [textLc] is the Kotlin-lowercased copy of [text] —
 * Kotlin lowercasing is Unicode-aware where SQLite's `lower()` is ASCII-only,
 * so case-insensitive search queries this column.
 */
@Entity(
    tableName = "clip_items",
    indices = [
        Index("content_hash"),
        Index("created_at"),
        Index("pinned"),
        Index("favorite"),
    ],
)
data class ClipItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    @ColumnInfo(name = "text_lc") val textLc: String,
    val type: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val pinned: Boolean,
    val favorite: Boolean,
    val category: String?,
    @ColumnInfo(name = "source_app") val sourceApp: String?,
    @ColumnInfo(name = "content_hash") val contentHash: String,
)

fun ClipItem.toEntity(): ClipItemEntity = ClipItemEntity(
    id = id,
    text = text,
    textLc = text.lowercase(),
    type = type.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    favorite = favorite,
    category = category,
    sourceApp = sourceApp,
    contentHash = contentHash,
)

fun ClipItemEntity.toClipItem(): ClipItem = ClipItem(
    id = id,
    text = text,
    type = ClipType.entries.firstOrNull { it.name == type } ?: ClipType.TEXT,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pinned = pinned,
    favorite = favorite,
    category = category,
    sourceApp = sourceApp,
    contentHash = contentHash,
)
