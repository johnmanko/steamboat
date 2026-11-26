package com.johnmanko.app.steamboat.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.domain.PhoneNumbers

/**
 * SMS and MMS rows stored by Android for one phone number.
 * Steamboat can read them with READ_SMS. Only the default SMS app can delete them.
 */
class PhoneMessages(private val context: Context) {
    data class Rows(val smsIds: List<Long>, val mmsIds: List<Long>) {
        fun providerKeys(): List<String> =
            smsIds.map { "sms:$it" } + mmsIds.map { "mms:$it" }

        companion object {
            val EMPTY = Rows(emptyList(), emptyList())
        }
    }

    fun find(address: String): Rows {
        if (!canRead() || PhoneNumbers.matchKey(address).isEmpty()) return Rows.EMPTY
        val sms = smsMatches(address)
        val threadIds = sms.mapNotNull { it.second }.toSet()
        val mms = (mmsIdsForThreads(threadIds) + mmsIdsForAddress(address)).toList()
        return Rows(sms.map { it.first }, mms)
    }

    fun delete(rows: Rows) {
        if (!context.isDefaultSmsApp()) return
        deleteIds(Telephony.Sms.CONTENT_URI, rows.smsIds)
        deleteIds(Telephony.Mms.CONTENT_URI, rows.mmsIds)
    }

    private fun smsMatches(address: String): List<Pair<Long, Long?>> {
        val matches = mutableListOf<Pair<Long, Long?>>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.THREAD_ID),
            null,
            null,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val threadCol = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                val rowAddress = cursor.getString(addressCol) ?: continue
                if (!PhoneNumbers.same(rowAddress, address)) continue
                val threadId = if (cursor.isNull(threadCol)) null else cursor.getLong(threadCol)
                matches += cursor.getLong(idCol) to threadId
            }
        }
        return matches
    }

    private fun mmsIdsForThreads(threadIds: Set<Long>): Set<Long> {
        if (threadIds.isEmpty()) return emptySet()
        val ids = mutableSetOf<Long>()
        threadIds.chunked(CHUNK).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID),
                "${Telephony.Mms.THREAD_ID} IN ($placeholders)",
                chunk.map { it.toString() }.toTypedArray(),
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
                while (cursor.moveToNext()) ids += cursor.getLong(idCol)
            }
        }
        return ids
    }

    private fun mmsIdsForAddress(address: String): Set<Long> {
        val ids = mutableSetOf<Long>()
        runCatching {
            context.contentResolver.query(
                Uri.parse("content://mms/addr"),
                arrayOf(Telephony.Mms.Addr.MSG_ID, Telephony.Mms.Addr.ADDRESS),
                null,
                null,
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.MSG_ID)
                val addressCol = cursor.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS)
                while (cursor.moveToNext()) {
                    val rowAddress = cursor.getString(addressCol) ?: continue
                    if (PhoneNumbers.same(rowAddress, address)) ids += cursor.getLong(idCol)
                }
            }
        }
        return ids
    }

    private fun deleteIds(uri: Uri, ids: List<Long>) {
        ids.distinct().chunked(CHUNK).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            runCatching {
                context.contentResolver.delete(
                    uri,
                    "_id IN ($placeholders)",
                    chunk.map { it.toString() }.toTypedArray(),
                )
            }
        }
    }

    private fun canRead(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHUNK = 200
    }
}
