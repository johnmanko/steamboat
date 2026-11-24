package com.johnmanko.app.steamboat.data.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import com.johnmanko.app.steamboat.notify.Notify
import com.johnmanko.app.steamboat.sms.SmsSentReceiver

class SmsSender(private val context: Context) {
    fun send(messageId: Long, address: String, body: String) {
        val smsManager = context.getSystemService(SmsManager::class.java)
        val sent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            Intent(context, SmsSentReceiver::class.java).apply {
                action = SmsSentReceiver.ACTION
                putExtra(Notify.EXTRA_MESSAGE_ID, messageId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val destination = PhoneNumberUtils.normalizeNumber(address).ifBlank { address }
        val parts = smsManager.divideMessage(body)
        if (parts.size <= 1) {
            smsManager.sendTextMessage(destination, null, body, sent, null)
        } else {
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            repeat(parts.size) { sentIntents += sent }
            smsManager.sendMultipartTextMessage(destination, null, parts, sentIntents, null)
        }
    }

    /**
     * The default SMS app is responsible for writing outgoing messages into the
     * shared SMS provider. Other apps keep their own copy in Room either way.
     */
    fun archiveSent(address: String, body: String, sentAt: Long): String? {
        if (!context.isDefaultSmsApp()) return null
        return insertProviderRow(
            uri = Telephony.Sms.Sent.CONTENT_URI,
            address = address,
            body = body,
            sentAt = sentAt,
            type = Telephony.Sms.MESSAGE_TYPE_SENT,
            read = 1,
        )
    }

    fun archiveIncoming(address: String, body: String, sentAt: Long): String? {
        if (!context.isDefaultSmsApp()) return null
        return insertProviderRow(
            uri = Telephony.Sms.Inbox.CONTENT_URI,
            address = address,
            body = body,
            sentAt = sentAt,
            type = Telephony.Sms.MESSAGE_TYPE_INBOX,
            read = 0,
        )
    }

    private fun insertProviderRow(
        uri: android.net.Uri,
        address: String,
        body: String,
        sentAt: Long,
        type: Int,
        read: Int,
    ): String? = runCatching {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, sentAt)
            put(Telephony.Sms.READ, read)
            put(Telephony.Sms.SEEN, read)
            put(Telephony.Sms.TYPE, type)
        }
        val inserted = context.contentResolver.insert(uri, values)
        inserted?.lastPathSegment?.let { "sms:$it" }
    }.getOrNull()
}

fun Context.isDefaultSmsApp(): Boolean {
    val roles = getSystemService(android.app.role.RoleManager::class.java) ?: return false
    return roles.isRoleAvailable(android.app.role.RoleManager.ROLE_SMS) &&
        roles.isRoleHeld(android.app.role.RoleManager.ROLE_SMS)
}
