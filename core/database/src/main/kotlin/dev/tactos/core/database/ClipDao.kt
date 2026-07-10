package dev.tactos.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {

    /** Pinned items first, then newest first. */
    @Query("SELECT * FROM clip_items ORDER BY pinned DESC, created_at DESC")
    fun timeline(): Flow<List<ClipItemEntity>>

    /**
     * Case-insensitive substring search over [ClipItemEntity.textLc].
     * [pattern] must be pre-lowercased and LIKE-escaped by the caller
     * (see [ClipRepository.search]).
     */
    @Query(
        "SELECT * FROM clip_items WHERE text_lc LIKE '%' || :pattern || '%' ESCAPE '\\' " +
            "ORDER BY pinned DESC, created_at DESC",
    )
    suspend fun search(pattern: String): List<ClipItemEntity>

    @Query("SELECT * FROM clip_items ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun newest(): ClipItemEntity?

    @Query("SELECT * FROM clip_items WHERE id = :id")
    suspend fun byId(id: Long): ClipItemEntity?

    @Insert
    suspend fun insert(entity: ClipItemEntity): Long

    @Query("UPDATE clip_items SET updated_at = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    /**
     * Collapse *consecutive* duplicates: if the newest row carries the same
     * content hash, bump its `updated_at` instead of inserting a copy.
     * Non-consecutive repeats insert normally — the timeline records that the
     * user came back to a value.
     */
    @Transaction
    suspend fun upsertDedup(entity: ClipItemEntity): Long {
        val newest = newest()
        if (newest != null && newest.contentHash == entity.contentHash) {
            touch(newest.id, entity.updatedAt)
            return newest.id
        }
        return insert(entity)
    }

    @Query("UPDATE clip_items SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE clip_items SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE clip_items SET category = :category WHERE id = :id")
    suspend fun setCategory(id: Long, category: String?)

    @Query("DELETE FROM clip_items WHERE id = :id")
    suspend fun delete(id: Long)

    /** Age-based retention. Pinned and favorite items are always spared. */
    @Query("DELETE FROM clip_items WHERE pinned = 0 AND favorite = 0 AND created_at < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    /**
     * Count-based retention: keep the [keep] newest unpinned/unfavorited rows,
     * delete the rest. Pinned and favorite items are always spared and do not
     * count toward [keep].
     */
    @Query(
        "DELETE FROM clip_items WHERE id IN (" +
            "SELECT id FROM clip_items WHERE pinned = 0 AND favorite = 0 " +
            "ORDER BY created_at DESC, id DESC LIMIT -1 OFFSET :keep)",
    )
    suspend fun trimToNewest(keep: Int): Int

    @Query("SELECT COUNT(*) FROM clip_items")
    suspend fun count(): Int
}
