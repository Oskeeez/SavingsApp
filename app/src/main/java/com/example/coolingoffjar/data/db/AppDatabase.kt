package com.example.coolingoffjar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WantEntity::class, JarEntity::class, OwnedItemEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wantDao(): WantDao
    abstract fun jarDao(): JarDao
    abstract fun shelfDao(): ShelfDao

    companion object {
        /** v2 adds the shelf: items the user has bought. Existing wants and jars are untouched. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `owned_items` (" +
                        "`itemId` TEXT NOT NULL, `tier` INTEGER NOT NULL, `slot` INTEGER NOT NULL, " +
                        "`purchasedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`))",
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cooling_off_jar.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
