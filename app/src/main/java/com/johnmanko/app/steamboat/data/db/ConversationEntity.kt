package com.johnmanko.app.steamboat.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["matchKey"], unique = true),
        Index(value = ["updatedAt"]),
    ],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val matchKey: String,
    val title: String,
    val photoUri: String?,
    val contactLookupKey: String?,
    val contactLinkedManually: Boolean,
    val snippet: String,
    val updatedAt: Long,
    val unreadCount: Int,
)
