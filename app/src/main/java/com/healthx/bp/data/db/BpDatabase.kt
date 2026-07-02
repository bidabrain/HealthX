package com.healthx.bp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BpRecord::class], version = 2, exportSchema = false)
abstract class BpDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao

    companion object {
        /**
         * v1 → v2: add sync columns and backfill a UUID + updatedAt for existing
         * rows, then create the unique index on uid (name must match what Room
         * generates for @Index(unique=true) on "uid").
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bp_record ADD COLUMN uid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE bp_record ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bp_record ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE bp_record SET uid = lower(hex(randomblob(16))), updatedAt = timestamp WHERE uid = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bp_record_uid ON bp_record(uid)")
            }
        }

        @Volatile private var instance: BpDatabase? = null

        fun get(context: Context): BpDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BpDatabase::class.java,
                    "healthx.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
