package com.johnmanko.app.steamboat.data.sms

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Writes a received multimedia message into the shared MMS provider.
 * The thread keeps its own copy either way.
 */
class MmsArchive(private val context: Context) {
    fun insertIncoming(
        address: String,
        body: String,
        sentAt: Long,
        media: ByteArray?,
        mediaMime: String?,
    ): String? {
        if (!context.isDefaultSmsApp()) return null
        return runCatching { insert(address, body, sentAt, media, mediaMime) }
            .onFailure { error -> Log.w(TAG, "MMS archive failed", error) }
            .getOrNull()
    }

    private fun insert(
        address: String,
        body: String,
        sentAt: Long,
        media: ByteArray?,
        mediaMime: String?,
    ): String? {
        val values = ContentValues().apply {
            put(Telephony.Mms.DATE, sentAt / 1000L)
            put(Telephony.Mms.DATE_SENT, sentAt / 1000L)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_INBOX)
            put(Telephony.Mms.READ, 0)
            put(Telephony.Mms.SEEN, 0)
            put(Telephony.Mms.MESSAGE_TYPE, RETRIEVE_CONF)
            put(Telephony.Mms.MESSAGE_CLASS, "personal")
            put(Telephony.Mms.TEXT_ONLY, if (media == null) 1 else 0)
            put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related")
        }
        val message = context.contentResolver.insert(Telephony.Mms.Inbox.CONTENT_URI, values) ?: return null
        val id = message.lastPathSegment ?: return null
        insertAddress(id, address)
        if (body.isNotBlank()) insertText(id, body)
        if (media != null && !mediaMime.isNullOrBlank()) insertMedia(id, media, mediaMime)
        return "mms:$id"
    }

    private fun insertAddress(id: String, address: String) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Addr.MSG_ID, id)
            put(Telephony.Mms.Addr.ADDRESS, address)
            put(Telephony.Mms.Addr.CHARSET, UTF_8)
            put(Telephony.Mms.Addr.TYPE, FROM)
        }
        context.contentResolver.insert(Uri.parse("content://mms/$id/addr"), values)
    }

    private fun insertText(id: String, body: String) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.MSG_ID, id)
            put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain")
            put(Telephony.Mms.Part.CHARSET, UTF_8)
            put(Telephony.Mms.Part.TEXT, body)
            put(Telephony.Mms.Part.SEQ, 0)
        }
        context.contentResolver.insert(Uri.parse("content://mms/$id/part"), values)
    }

    private fun insertMedia(id: String, media: ByteArray, mime: String) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.MSG_ID, id)
            put(Telephony.Mms.Part.CONTENT_TYPE, mime)
            put(Telephony.Mms.Part.SEQ, 1)
            put(Telephony.Mms.Part.NAME, "media")
        }
        val part = context.contentResolver.insert(Uri.parse("content://mms/$id/part"), values) ?: return
        context.contentResolver.openOutputStream(part)?.use { it.write(media) }
    }

    private companion object {
        const val TAG = "Steamboat"
        const val RETRIEVE_CONF = 132
        const val FROM = 137
        const val UTF_8 = 106
    }
}
