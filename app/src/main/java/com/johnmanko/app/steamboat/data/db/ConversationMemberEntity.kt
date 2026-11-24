package com.johnmanko.app.steamboat.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "conversation_members",
    primaryKeys = ["conversationId", "matchKey"],
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["matchKey"])],
)
data class ConversationMemberEntity(
    val conversationId: Long,
    val matchKey: String,
    val address: String,
    val title: String,
)
