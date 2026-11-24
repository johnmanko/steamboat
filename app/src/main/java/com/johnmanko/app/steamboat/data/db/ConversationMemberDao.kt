package com.johnmanko.app.steamboat.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationMemberDao {
    @Query("SELECT * FROM conversation_members WHERE conversationId = :conversationId ORDER BY title")
    fun observe(conversationId: Long): Flow<List<ConversationMemberEntity>>

    @Query("SELECT * FROM conversation_members WHERE conversationId = :conversationId ORDER BY title")
    suspend fun list(conversationId: Long): List<ConversationMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ConversationMemberEntity>)

    @Query("DELETE FROM conversation_members WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)
}
