package com.johnmanko.app.steamboat.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId", "sentAt"]),
        Index(value = ["providerKey"], unique = true),
        Index(value = ["conversationId", "seen"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val body: String,
    val sentAt: Long,
    val outgoing: Boolean,
    val status: String,
    val seen: Boolean,
    val providerKey: String?,
    val mediaPath: String? = null,
    val mediaMime: String? = null,
    val senderMatchKey: String? = null,
) {
    fun listSnippet(): String = when {
        body.isNotBlank() -> body
        mediaMime?.startsWith("video/") == true -> "Video"
        !mediaPath.isNullOrBlank() -> "Photo"
        else -> ""
    }
}
