package com.yangyx.adbhelper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yangyx.adbhelper.data.dao.CommandDao
import com.yangyx.adbhelper.data.dao.DeviceDao
import com.yangyx.adbhelper.data.entity.CommandEntity
import com.yangyx.adbhelper.data.entity.DeviceEntity

@Database(
    entities = [DeviceEntity::class, CommandEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun commandDao(): CommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            typeAndDefault: String
        ) {
            try {
                var exists = false
                db.query("PRAGMA table_info(`$table`)").use { cursor ->
                    val nameColIdx = cursor.getColumnIndex("name")
                    if (nameColIdx >= 0) {
                        while (cursor.moveToNext()) {
                            val colName = cursor.getString(nameColIdx)
                            if (colName.equals(column, ignoreCase = true)) {
                                exists = true
                                break
                            }
                        }
                    }
                }
                if (!exists) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $typeAndDefault")
                }
            } catch (_: Exception) {}
        }

        private fun migrateDevicesTable(db: SupportSQLiteDatabase) {
            addColumnIfNotExists(db, "devices", "serialNo", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "devices", "aliasName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfNotExists(db, "devices", "sortOrder", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "devices", "isFavorite", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfNotExists(db, "devices", "lastUsedBitrate", "INTEGER NOT NULL DEFAULT 4000000")
            addColumnIfNotExists(db, "devices", "lastUsedResolution", "INTEGER NOT NULL DEFAULT 1080")
            addColumnIfNotExists(db, "devices", "iconType", "TEXT NOT NULL DEFAULT 'phone'")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDevicesTable(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDevicesTable(db)
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateDevicesTable(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adb_helper_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

