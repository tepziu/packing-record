package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.domain.model.PackingSession

@Database(
    entities = [PackingSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PackingDatabase : RoomDatabase() {
    abstract fun packingSessionDao(): PackingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: PackingDatabase? = null

        fun getDatabase(context: Context): PackingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PackingDatabase::class.java,
                    "packing_recorder.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
