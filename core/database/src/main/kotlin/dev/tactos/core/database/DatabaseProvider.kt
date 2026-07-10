package dev.tactos.core.database

import android.content.Context

/**
 * Process-wide singleton access to the timeline storage. Consumers outside
 * this module use [repository] — it keeps Room types (`RoomDatabase`,
 * `ClipDao`) off their compile classpath, so Room stays an implementation
 * detail of core/database.
 */
object TactosDb {

    @Volatile
    private var instance: TactosDatabase? = null

    internal fun get(context: Context): TactosDatabase =
        instance ?: synchronized(this) {
            instance ?: TactosDatabase.create(context.applicationContext).also { instance = it }
        }

    fun repository(context: Context): ClipRepository =
        ClipRepository(get(context).clipDao())
}
