package com.johnmanko.app.steamboat.data.db

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface MessageDao {
    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY sentAt DESC, id DESC
        """,
    )
    fun paging(conversationId: Long): PagingSource<Int, MessageEntity>

    @Query(
        """
        SELECT messages.id AS id,
               messages.conversationId AS conversationId,
               messages.body AS body,
               messages.sentAt AS sentAt,
               conversations.title AS title,
               conversations.address AS address,
               conversations.matchKey AS matchKey
        FROM messages
        INNER JOIN conversations ON conversations.id = messages.conversationId
        WHERE messages.body LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY messages.sentAt DESC, messages.id DESC
        """,
    )
    fun search(query: String): PagingSource<Int, MessageSearchHit>

    @Query(
        """
        SELECT * FROM messages
        WHERE conversationId = :conversationId
        ORDER BY sentAt DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun latest(conversationId: Long, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE messages SET providerKey = :providerKey WHERE id = :id AND providerKey IS NULL")
    suspend fun updateProviderKey(id: Long, providerKey: String)

    @Query("UPDATE messages SET seen = 1 WHERE conversationId = :conversationId AND seen = 0")
    suspend fun markSeen(conversationId: Long)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun get(id: Long): MessageEntity?

    @Query("SELECT id FROM messages WHERE providerKey = :providerKey LIMIT 1")
    suspend fun findByProviderKey(providerKey: String): Long?

    @Query(
        """
        SELECT providerKey FROM messages
        WHERE conversationId = :conversationId AND providerKey IS NOT NULL
        """,
    )
    suspend fun providerKeys(conversationId: Long): List<String>

    @Query(
        """
        SELECT id FROM messages
        WHERE conversationId = :conversationId
          AND outgoing = 1
          AND body = :body
          AND sentAt BETWEEN :start AND :end
          AND providerKey IS NULL
        LIMIT 1
        """,
    )
    suspend fun findOutgoingDuplicate(
        conversationId: Long,
        body: String,
        start: Long,
        end: Long,
    ): Long?
}
