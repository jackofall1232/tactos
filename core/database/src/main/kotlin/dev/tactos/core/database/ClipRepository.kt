package dev.tactos.core.database

import dev.tactos.core.model.ClipItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain-facing timeline API over [ClipDao]: maps between [ClipItem] and the
 * Room entity, collapses consecutive duplicates, and owns query hygiene
 * (lowercasing + LIKE escaping) so callers never touch SQL semantics.
 */
class ClipRepository(private val dao: ClipDao) {

    fun timeline(limit: Int = DEFAULT_TIMELINE_LIMIT): Flow<List<ClipItem>> =
        dao.timeline(limit).map { rows -> rows.map { it.toClipItem() } }

    /** Persist a capture, collapsing consecutive duplicates. Returns the row id. */
    suspend fun save(item: ClipItem): Long = dao.upsertDedup(item.toEntity())

    /** Case-insensitive (Unicode-aware) substring search; blank query = empty result. */
    suspend fun search(query: String): List<ClipItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return dao.search(escapeLike(trimmed.lowercase())).map { it.toClipItem() }
    }

    suspend fun byId(id: Long): ClipItem? = dao.byId(id)?.toClipItem()

    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun setCategory(id: Long, category: String?) = dao.setCategory(id, category)

    suspend fun delete(id: Long) = dao.delete(id)

    /** Age-based cleanup; pinned/favorite rows are always spared. Returns rows deleted. */
    suspend fun deleteOlderThan(cutoffEpochMillis: Long): Int = dao.deleteOlderThan(cutoffEpochMillis)

    /** Count-based cleanup; pinned/favorite rows are spared and don't count. Returns rows deleted. */
    suspend fun trimToNewest(keep: Int): Int {
        require(keep >= 0) { "keep must be >= 0" }
        return dao.trimToNewest(keep)
    }

    suspend fun count(): Int = dao.count()

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    companion object {
        /** Rows the live timeline exposes; full history stays searchable. */
        const val DEFAULT_TIMELINE_LIMIT: Int = 500
    }
}
