package dev.tactos.app.settings

import dev.tactos.core.database.ClipRepository

/**
 * Applies the user's retention rules to the timeline. Runs at launch and
 * whenever the rules change — deliberately no background scheduler in v1
 * (WorkManager would be a new dependency, which is a review gate). The DAO
 * queries behind [ClipRepository] always spare pinned and favorite clips.
 */
object RetentionCleanup {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    suspend fun run(repository: ClipRepository, settings: TactosSettings) {
        if (settings.retentionDays > 0) {
            repository.deleteOlderThan(System.currentTimeMillis() - settings.retentionDays * DAY_MS)
        }
        if (settings.retentionMaxItems > 0) {
            repository.trimToNewest(settings.retentionMaxItems)
        }
    }
}
