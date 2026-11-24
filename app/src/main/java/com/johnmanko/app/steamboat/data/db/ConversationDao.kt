package com.johnmanko.app.steamboat.data.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observe(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun get(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE matchKey = :matchKey LIMIT 1")
    suspend fun findByMatchKey(matchKey: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun paging(): PagingSource<Int, ConversationEntity>

    @Query(
        """
        SELECT * FROM conversations
        WHERE title LIKE '%' || :query || '%' ESCAPE '\'
           OR address LIKE '%' || :query || '%' ESCAPE '\'
           OR snippet LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY updatedAt DESC
        """,
    )
    fun search(query: String): PagingSource<Int, ConversationEntity>

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query(
        """
        UPDATE conversations
        SET snippet = :snippet, updatedAt = :updatedAt
        WHERE id = :id AND updatedAt <= :updatedAt
        """,
    )
    suspend fun updateSnippetIfNewer(id: Long, snippet: String, updatedAt: Long)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = (
            SELECT COUNT(*) FROM messages
            WHERE messages.conversationId = conversations.id
              AND messages.outgoing = 0
              AND messages.seen = 0
        )
        WHERE id = :id
        """,
    )
    suspend fun refreshUnread(id: Long)

    @Query(
        """
        UPDATE conversations
        SET unreadCount = (
            SELECT COUNT(*) FROM messages
            WHERE messages.conversationId = conversations.id
              AND messages.outgoing = 0
              AND messages.seen = 0
        )
        """,
    )
    suspend fun refreshAllUnread()

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnread(id: Long)

    @Query(
        """
        UPDATE conversations
        SET contactLookupKey = :lookupKey,
            title = :title,
            photoUri = :photoUri,
            contactLinkedManually = 1
        WHERE id = :id
        """,
    )
    suspend fun linkManually(id: Long, lookupKey: String, title: String, photoUri: String?)

    @Query(
        """
        UPDATE conversations
        SET contactLookupKey = NULL,
            title = address,
            photoUri = NULL,
            contactLinkedManually = 1
        WHERE id = :id
        """,
    )
    suspend fun unlink(id: Long)

    @Query(
        """
        UPDATE conversations
        SET contactLookupKey = (
                SELECT lookupKey FROM contacts
                WHERE contacts.matchKey = conversations.matchKey
                ORDER BY displayName LIMIT 1
            ),
            title = (
                SELECT displayName FROM contacts
                WHERE contacts.matchKey = conversations.matchKey
                ORDER BY displayName LIMIT 1
            ),
            photoUri = (
                SELECT photoUri FROM contacts
                WHERE contacts.matchKey = conversations.matchKey
                ORDER BY displayName LIMIT 1
            )
        WHERE contactLinkedManually = 0
          AND EXISTS (
              SELECT 1 FROM contacts WHERE contacts.matchKey = conversations.matchKey
          )
        """,
    )
    suspend fun linkMatchingContacts()

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        UPDATE conversations
        SET address = :address,
            matchKey = :matchKey,
            title = :title,
            photoUri = NULL,
            contactLookupKey = NULL,
            contactLinkedManually = 0
        WHERE id = :id
        """,
    )
    suspend fun updateGroup(id: Long, address: String, matchKey: String, title: String)
}
