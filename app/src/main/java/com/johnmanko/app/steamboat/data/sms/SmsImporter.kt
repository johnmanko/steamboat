package com.johnmanko.app.steamboat.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.data.repo.ImportedSms
import com.johnmanko.app.steamboat.domain.MessageStatus
import com.johnmanko.app.steamboat.domain.PhoneNumbers
import kotlinx.coroutines.yield

/**
 * Copies the device SMS store into Room one page at a time. The UI never reads
 * the telephony provider while scrolling.
 */
class SmsImporter(private val context: Context) {
    fun canRead(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS,
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun readAll(onPage: suspend (List<ImportedSms>) -> Unit) {
        if (!canRead()) return
        var beforeDate = Long.MAX_VALUE
        var beforeId = Long.MAX_VALUE
        var pages = 0
        while (pages < MAX_PAGES) {
            val page = readPage(beforeDate, beforeId)
            if (page.isEmpty()) return
            onPage(page.map { it.toImported() })
            val oldest = page.last()
            if (oldest.sentAt == beforeDate && oldest.providerId == beforeId) return
            beforeDate = oldest.sentAt
            beforeId = oldest.providerId
            pages += 1
            if (page.size < PAGE_SIZE) return
            yield()
        }
    }

    private fun readPage(beforeDate: Long, beforeId: Long): List<SmsRow> {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ,
            ),
            "(${Telephony.Sms.DATE} < ?) OR (${Telephony.Sms.DATE} = ? AND ${Telephony.Sms._ID} < ?)",
            arrayOf(beforeDate.toString(), beforeDate.toString(), beforeId.toString()),
            "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC",
        ) ?: return emptyList()

        val rows = ArrayList<SmsRow>(PAGE_SIZE)
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Sms.READ)
            while (it.moveToNext() && rows.size < PAGE_SIZE) {
                val type = it.getInt(typeIndex)
                if (type == Telephony.Sms.MESSAGE_TYPE_DRAFT) continue
                val address = it.getString(addressIndex) ?: continue
                if (PhoneNumbers.matchKey(address).isEmpty()) continue
                val providerId = it.getLong(idIndex)
                val outgoing = type != Telephony.Sms.MESSAGE_TYPE_INBOX
                val status = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageStatus.FAILED
                    Telephony.Sms.MESSAGE_TYPE_OUTBOX,
                    Telephony.Sms.MESSAGE_TYPE_QUEUED,
                    -> MessageStatus.SENDING
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageStatus.RECEIVED
                    else -> MessageStatus.SENT
                }
                rows += SmsRow(
                    providerId = providerId,
                    providerKey = "sms:$providerId",
                    address = address,
                    body = it.getString(bodyIndex).orEmpty(),
                    sentAt = it.getLong(dateIndex),
                    outgoing = outgoing,
                    seen = outgoing || it.getInt(readIndex) == 1,
                    status = status,
                )
            }
        }
        return rows
    }

    private data class SmsRow(
        val providerId: Long,
        val providerKey: String,
        val address: String,
        val body: String,
        val sentAt: Long,
        val outgoing: Boolean,
        val seen: Boolean,
        val status: String,
    ) {
        fun toImported() = ImportedSms(
            providerKey = providerKey,
            address = address,
            body = body,
            sentAt = sentAt,
            outgoing = outgoing,
            seen = seen,
            status = status,
        )
    }

    private companion object {
        const val PAGE_SIZE = 400
        const val MAX_PAGES = 10_000
    }
}
