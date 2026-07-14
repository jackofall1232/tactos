package dev.tactos.app.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.tactos.core.database.ClipRepository
import dev.tactos.core.database.TactosDatabase
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

/**
 * [RetentionCleanup.run] just forwards [TactosSettings] into
 * [ClipRepository]'s already-tested retention queries (see
 * core/database's ClipRepositoryTest for the age/count cleanup semantics
 * themselves) — these tests verify the wiring: which repository calls fire,
 * with which cutoffs, for which settings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetentionCleanupTest {

    private lateinit var db: TactosDatabase
    private lateinit var repo: ClipRepository

    private val dayMs = 24L * 60 * 60 * 1000

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

    private fun item(text: String, createdAt: Long) = ClipItem(
        text = text,
        type = ClipType.TEXT,
        createdAt = createdAt,
    )

    @Test
    fun `both retention rules disabled deletes nothing regardless of age or count`() = runTest {
        repo.save(item("ancient", createdAt = System.currentTimeMillis() - 365 * dayMs))
        repo.save(item("also-ancient", createdAt = System.currentTimeMillis() - 365 * dayMs))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 0, retentionMaxItems = 0))

        assertEquals(2, repo.count())
    }

    @Test
    fun `positive retentionDays deletes items older than the cutoff only`() = runTest {
        val now = System.currentTimeMillis()
        repo.save(item("old", createdAt = now - 10 * dayMs))
        repo.save(item("fresh", createdAt = now))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 5, retentionMaxItems = 0))

        assertEquals(listOf("fresh"), repo.timeline().first().map { it.text })
    }

    @Test
    fun `zero retentionDays does not invoke age-based cleanup even for very old items`() = runTest {
        repo.save(item("old", createdAt = System.currentTimeMillis() - 1000 * dayMs))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 0, retentionMaxItems = 0))

        assertEquals(1, repo.count())
    }

    @Test
    fun `positive retentionMaxItems trims down to the newest N`() = runTest {
        val now = System.currentTimeMillis()
        repo.save(item("one", createdAt = now - 3 * dayMs))
        repo.save(item("two", createdAt = now - 2 * dayMs))
        repo.save(item("three", createdAt = now - 1 * dayMs))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 0, retentionMaxItems = 2))

        assertEquals(
            setOf("two", "three"),
            repo.timeline().first().map { it.text }.toSet(),
        )
    }

    @Test
    fun `zero retentionMaxItems does not invoke count-based cleanup even with many items`() = runTest {
        val now = System.currentTimeMillis()
        repeat(5) { repo.save(item("item-$it", createdAt = now - it * dayMs)) }

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 0, retentionMaxItems = 0))

        assertEquals(5, repo.count())
    }

    @Test
    fun `both rules positive apply age cleanup then count cleanup`() = runTest {
        val now = System.currentTimeMillis()
        repo.save(item("ancient", createdAt = now - 100 * dayMs))
        repo.save(item("one", createdAt = now - 3 * dayMs))
        repo.save(item("two", createdAt = now - 2 * dayMs))
        repo.save(item("three", createdAt = now - 1 * dayMs))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 10, retentionMaxItems = 2))

        assertEquals(
            setOf("two", "three"),
            repo.timeline().first().map { it.text }.toSet(),
        )
    }

    @Test
    fun `pinned and favorite items survive both cleanup rules`() = runTest {
        val now = System.currentTimeMillis()
        val pinnedId = repo.save(item("pinned-ancient", createdAt = now - 100 * dayMs))
        repo.setPinned(pinnedId, true)
        val favId = repo.save(item("favorite-ancient", createdAt = now - 100 * dayMs))
        repo.setFavorite(favId, true)
        repo.save(item("fresh", createdAt = now))

        RetentionCleanup.run(repo, TactosSettings(retentionDays = 1, retentionMaxItems = 1))

        assertEquals(
            setOf("pinned-ancient", "favorite-ancient", "fresh"),
            repo.timeline().first().map { it.text }.toSet(),
        )
    }
}
