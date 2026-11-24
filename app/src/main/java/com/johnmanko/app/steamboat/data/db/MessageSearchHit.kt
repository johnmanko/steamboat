package com.johnmanko.app.steamboat.data.db

data class MessageSearchHit(
    val id: Long,
    val conversationId: Long,
    val body: String,
    val sentAt: Long,
    val title: String,
    val address: String,
    val matchKey: String,
)
