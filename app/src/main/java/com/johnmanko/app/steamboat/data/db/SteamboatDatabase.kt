package com.johnmanko.app.steamboat.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        ContactEntity::class,
        ConversationEntity::class,
        ConversationMemberEntity::class,
        MessageEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class SteamboatDatabase : RoomDatabase() {
    abstract fun contacts(): ContactDao

    abstract fun conversations(): ConversationDao

    abstract fun members(): ConversationMemberDao

    abstract fun messages(): MessageDao

    companion object {
        fun create(context: Context): SteamboatDatabase {
            val file = context.applicationContext.getDatabasePath("steamboat.db")
            file.parentFile?.mkdirs()
            return Room.databaseBuilder<SteamboatDatabase>(
                context = context.applicationContext,
                name = file.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare("ALTER TABLE messages ADD COLUMN mediaPath TEXT").use { it.step() }
                connection.prepare("ALTER TABLE messages ADD COLUMN mediaMime TEXT").use { it.step() }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare(
                    """
                    CREATE TABLE IF NOT EXISTS `conversation_members` (
                        `conversationId` INTEGER NOT NULL,
                        `matchKey` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        PRIMARY KEY(`conversationId`, `matchKey`),
                        FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                ).use { it.step() }
                connection.prepare(
                    "CREATE INDEX IF NOT EXISTS `index_conversation_members_matchKey` ON `conversation_members` (`matchKey`)",
                ).use { it.step() }
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare("ALTER TABLE messages ADD COLUMN senderMatchKey TEXT").use { it.step() }
            }
        }
    }
}
