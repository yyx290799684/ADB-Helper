package com.yangyx.adbhelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yangyx.adbhelper.data.dao.CommandDao
import com.yangyx.adbhelper.data.dao.DeviceDao
import com.yangyx.adbhelper.data.entity.CommandEntity
import com.yangyx.adbhelper.data.entity.DeviceEntity

@Database(
    entities = [DeviceEntity::class, CommandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun commandDao(): CommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adb_helper_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
