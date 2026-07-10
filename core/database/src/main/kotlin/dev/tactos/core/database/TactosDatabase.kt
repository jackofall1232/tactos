package dev.tactos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ClipItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TactosDatabase : RoomDatabase() {

    abstract fun clipDao(): ClipDao

    companion object {
        const val NAME: String = "tactos.db"

        fun create(context: Context): TactosDatabase =
            Room.databaseBuilder(context, TactosDatabase::class.java, NAME).build()
    }
}
