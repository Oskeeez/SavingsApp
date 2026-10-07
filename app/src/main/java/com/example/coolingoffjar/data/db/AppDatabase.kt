package com.example.coolingoffjar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [WantEntity::class, JarEntity::class, OwnedItemEntity::class, DecorChoiceEntity::class], version = 4, exportSchema = true)
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

        /** v3: each want can wear an icon. Existing wants get the default sprig. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wants` ADD COLUMN `iconKey` TEXT NOT NULL DEFAULT 'sprig'")
            }
        }

        /** v4: things can be moved anywhere (free wall positions for notes) and the room's look can be chosen. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `owned_items` ADD COLUMN `posX` REAL")
                db.execSQL("ALTER TABLE `owned_items` ADD COLUMN `posY` REAL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `decor_choice` (`kind` TEXT NOT NULL, `itemId` TEXT NOT NULL, PRIMARY KEY(`kind`))")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "cooling_off_jar.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
