package dev.tactos.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.tactos.core.model.ClipItem
import dev.tactos.core.model.ClipType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClipRepositoryTest {

    private lateinit var db: TactosDatabase
    private lateinit var repo: ClipRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TactosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ClipRepository(db.clipDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun item(
        text: String,
        type: ClipType = ClipType.TEXT,
        createdAt: Long = 1_000L,
        pinned: Boolean = false,
        favorite: Boolean = false,
        category: String? = null,
        sourceApp: String? = null,
    ) = ClipItem(
        text = text,
        type = type,
        createdAt = createdAt,
        pinned = pinned,
        favorite = favorite,
        category = category,
        sourceApp = sourceApp,
    )

    // --- roundtrip ---

    @Test
    fun `save and read back preserves every field`() = runTest {
        val saved = item(
            text = "https://example.com",
            type = ClipType.URL,
            createdAt = 42L,
            pinned = true,
            favorite = true,
            category = "links",
            sourceApp = "com.android.chrome",
        )
        val id = repo.save(saved)
        val loaded = repo.byId(id)!!
        assertEquals(saved.text, loaded.text)
        assertEquals(ClipType.URL, loaded.type)
        assertEquals(42L, loaded.createdAt)
        assertEquals(42L, loaded.updatedAt)
        assertTrue(loaded.pinned)
        assertTrue(loaded.favorite)
        assertEquals("links", loaded.category)
        assertEquals("com.android.chrome", loaded.sourceApp)
        assertEquals(saved.contentHash, loaded.contentHash)
    }

    @Test
    fun `unknown persisted type maps back to TEXT`() = runTest {
        val dao = db.clipDao()
        val id = dao.insert(
            item("mystery").toEntity().copy(type = "SOME_FUTURE_TYPE"),
        )
        assertEquals(ClipType.TEXT, repo.byId(id)!!.type)
    }

    // --- consecutive dedup ---

    @Test
    fun `consecutive duplicate bumps updatedAt instead of inserting`() = runTest {
        val first = repo.save(item("same", createdAt = 100L))
        val second = repo.save(item("same", createdAt = 200L))
        assertEquals(first, second)
        assertEquals(1, repo.count())
        val row = repo.byId(first)!!
        assertEquals(100L, row.createdAt)
        assertEquals(200L, row.updatedAt)
    }

    @Test
    fun `non-consecutive repeat inserts a new row`() = runTest {
        repo.save(item("alpha", createdAt = 100L))
        repo.save(item("beta", createdAt = 200L))
        repo.save(item("alpha", createdAt = 300L))
        assertEquals(3, repo.count())
    }

    // --- search ---

    @Test
    fun `search is case-insensitive including non-ascii`() = runTest {
        repo.save(item("Visit the Café tomorrow", createdAt = 1L))
        repo.save(item("unrelated", createdAt = 2L))
        assertEquals(1, repo.search("CAFÉ").size)
        assertEquals(1, repo.search("café").size)
        assertEquals(1, repo.search("visit the c").size)
        assertEquals(0, repo.search("cafe").size) // é != e — no folding beyond case
    }

    @Test
    fun `search treats percent and underscore as literals`() = runTest {
        repo.save(item("progress: 100% done", createdAt = 1L))
        repo.save(item("progress: 100x done", createdAt = 2L))
        repo.save(item("snake_case_name", createdAt = 3L))
        repo.save(item("snakeXcaseXname", createdAt = 4L))
        assertEquals(1, repo.search("100%").size)
        assertEquals(1, repo.search("snake_case").size)
    }

    @Test
    fun `blank search returns nothing`() = runTest {
        repo.save(item("anything", createdAt = 1L))
        assertEquals(0, repo.search("   ").size)
    }

    // --- ordering ---

    @Test
    fun `timeline lists pinned first then newest first`() = runTest {
        repo.save(item("old", createdAt = 1L))
        repo.save(item("new", createdAt = 3L))
        val pinnedId = repo.save(item("pinned-old", createdAt = 2L))
        repo.setPinned(pinnedId, true)
        val texts = repo.timeline().first().map { it.text }
        assertEquals(listOf("pinned-old", "new", "old"), texts)
    }

    // --- flags and delete ---

    @Test
    fun `pin favorite category toggles persist`() = runTest {
        val id = repo.save(item("flags", createdAt = 1L))
        repo.setPinned(id, true)
        repo.setFavorite(id, true)
        repo.setCategory(id, "work")
        val row = repo.byId(id)!!
        assertTrue(row.pinned)
        assertTrue(row.favorite)
        assertEquals("work", row.category)
        repo.setCategory(id, null)
        assertNull(repo.byId(id)!!.category)
    }

    @Test
    fun `delete removes the row`() = runTest {
        val id = repo.save(item("gone", createdAt = 1L))
        repo.delete(id)
        assertNull(repo.byId(id))
        assertEquals(0, repo.count())
    }

    // --- retention ---

    @Test
    fun `deleteOlderThan spares pinned and favorite rows`() = runTest {
        repo.save(item("ancient", createdAt = 10L))
        val pinnedId = repo.save(item("ancient-pinned", createdAt = 11L))
        repo.setPinned(pinnedId, true)
        val favId = repo.save(item("ancient-fav", createdAt = 12L))
        repo.setFavorite(favId, true)
        repo.save(item("fresh", createdAt = 100L))

        val deleted = repo.deleteOlderThan(50L)

        assertEquals(1, deleted)
        val remaining = repo.timeline().first().map { it.text }.toSet()
        assertEquals(setOf("ancient-pinned", "ancient-fav", "fresh"), remaining)
    }

    @Test
    fun `trimToNewest keeps N newest and spares pinned and favorite`() = runTest {
        repo.save(item("one", createdAt = 1L))
        repo.save(item("two", createdAt = 2L))
        repo.save(item("three", createdAt = 3L))
        val pinnedId = repo.save(item("pinned-oldest", createdAt = 0L))
        repo.setPinned(pinnedId, true)

        val deleted = repo.trimToNewest(2)

        assertEquals(1, deleted)
        val remaining = repo.timeline().first().map { it.text }.toSet()
        assertEquals(setOf("pinned-oldest", "two", "three"), remaining)
    }

    @Test
    fun `trimToNewest zero keeps only pinned and favorite`() = runTest {
        repo.save(item("a", createdAt = 1L))
        repo.save(item("b", createdAt = 2L))
        val favId = repo.save(item("fav", createdAt = 3L))
        repo.setFavorite(favId, true)

        val deleted = repo.trimToNewest(0)

        assertEquals(2, deleted)
        assertEquals(listOf("fav"), repo.timeline().first().map { it.text })
    }
}
