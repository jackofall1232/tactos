package dev.tactos.core.database

import android.content.Context

/** Process-wide singleton access to the timeline database. */
object TactosDb {

    @Volatile
    private var instance: TactosDatabase? = null

    fun get(context: Context): TactosDatabase =
        instance ?: synchronized(this) {
            instance ?: TactosDatabase.create(context.applicationContext).also { instance = it }
        }
}
