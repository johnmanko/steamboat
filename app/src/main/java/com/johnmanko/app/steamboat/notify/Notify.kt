package com.johnmanko.app.steamboat.notify

object Notify {
    const val CHANNEL_ID = "messages"
    const val KEY_REPLY = "key_text_reply"
    const val EXTRA_CONVERSATION_ID = "conversation_id"
    const val EXTRA_MESSAGE_ID = "message_id"
    const val EXTRA_QUICK_REPLY = "quick_reply"
    const val ACTION_REPLY = "com.johnmanko.app.steamboat.action.REPLY"
    const val ACTION_MARK_READ = "com.johnmanko.app.steamboat.action.MARK_READ"

    fun notificationId(conversationId: Long): Int = (conversationId and Int.MAX_VALUE.toLong()).toInt()

    fun shortcutId(conversationId: Long): String = "thread:$conversationId"
}
