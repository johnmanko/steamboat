package com.johnmanko.app.steamboat.data.themes

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [ThemeBundleEntity::class, ThemeEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ThemeDatabase : RoomDatabase() {
    abstract fun themes(): ThemeDao

    companion object {
        fun create(context: Context): ThemeDatabase {
            val file = context.applicationContext.getDatabasePath("themes.db")
            file.parentFile?.mkdirs()
            return Room.databaseBuilder<ThemeDatabase>(
                context = context.applicationContext,
                name = file.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    "ALTER TABLE theme_bundles ADD COLUMN framework INTEGER NOT NULL DEFAULT 1",
                ).use { it.step() }
                connection.prepare(
                    "ALTER TABLE themes ADD COLUMN source TEXT NOT NULL DEFAULT 'APP'",
                ).use { it.step() }
                connection.prepare(
                    "ALTER TABLE themes ADD COLUMN framework INTEGER NOT NULL DEFAULT 1",
                ).use { it.step() }
                connection.prepare(
                    "UPDATE theme_bundles SET origin = 'APP' WHERE origin = 'BUNDLED'",
                ).use { it.step() }
                connection.prepare(
                    "UPDATE theme_bundles SET origin = 'PAID' WHERE origin = 'PURCHASED'",
                ).use { it.step() }
            }
        }
    }
}
