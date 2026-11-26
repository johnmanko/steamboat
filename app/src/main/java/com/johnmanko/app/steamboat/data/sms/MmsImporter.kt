package com.johnmanko.app.steamboat.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.data.repo.ImportedSms
import com.johnmanko.app.steamboat.domain.MessageStatus
import com.johnmanko.app.steamboat.domain.PhoneNumbers
import java.io.File
import java.util.UUID

class MmsImporter(private val context: Context) {
    fun canRead(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS,
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun readAll(onPage: suspend (List<ImportedSms>) -> Unit) {
        if (!canRead()) return
        val cursor = context.contentResolver.query(
            Telephony.Mms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Mms._ID,
                Telephony.Mms.DATE,
                Telephony.Mms.READ,
                Telephony.Mms.SEEN,
            ),
            null,
            null,
            "${Telephony.Mms.DATE} DESC",
        ) ?: return

        val rows = ArrayList<ImportedSms>()
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val readIndex = it.getColumnIndexOrThrow(Telephony.Mms.READ)
            val seenIndex = it.getColumnIndexOrThrow(Telephony.Mms.SEEN)
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val providerKey = "mms:$id"
                val address = senderAddress(id) ?: continue
                if (PhoneNumbers.matchKey(address).isEmpty()) continue
                val date = it.getLong(dateIndex) * 1000L
                val seen = it.getInt(seenIndex) == 1 || it.getInt(readIndex) == 1
                val parts = readParts(id)
                rows += ImportedSms(
                    providerKey = providerKey,
                    address = address,
                    body = parts.text,
                    sentAt = date,
                    outgoing = false,
                    seen = seen,
                    status = MessageStatus.RECEIVED,
                    mediaPath = parts.mediaPath,
                    mediaMime = parts.mediaMime,
                )
            }
        }
        onPage(rows)
    }

    private fun senderAddress(id: Long): String? {
        val cursor = context.contentResolver.query(
            Uri.parse("content://mms/$id/addr"),
            arrayOf("address", "type"),
            null,
            null,
            null,
        ) ?: return null
        cursor.use {
            val addressIndex = it.getColumnIndex("address")
            val typeIndex = it.getColumnIndex("type")
            if (addressIndex < 0 || typeIndex < 0) return null
            while (it.moveToNext()) {
                if (it.getInt(typeIndex) == 137) return it.getString(addressIndex)
            }
        }
        return null
    }

    private data class MmsParts(
        val text: String,
        val mediaPath: String?,
        val mediaMime: String?,
    )

    private fun readParts(id: Long): MmsParts {
        val cursor = context.contentResolver.query(
            Uri.parse("content://mms/$id/part"),
            arrayOf("_id", "ct", "text", "_data"),
            null,
            null,
            null,
        ) ?: return MmsParts("", null, null)

        val text = StringBuilder()
        var mediaPath: String? = null
        var mediaMime: String? = null
        cursor.use {
            val idIndex = it.getColumnIndex("_id")
            val mimeIndex = it.getColumnIndex("ct")
            val textIndex = it.getColumnIndex("text")
            val dataIndex = it.getColumnIndex("_data")
            while (it.moveToNext()) {
                val mime = if (mimeIndex >= 0) it.getString(mimeIndex) else null
                val partText = if (textIndex >= 0) it.getString(textIndex) else null
                if (mime == "text/plain" && !partText.isNullOrEmpty()) {
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(partText)
                } else if (mime != null && !mime.startsWith("application/vnd.wap.multipart")) {
                    val partUri = if (idIndex >= 0) {
                        Uri.parse("content://mms/part/${it.getLong(idIndex)}")
                    } else null
                    val path = if (partUri != null) copyPart(partUri, mime) else null
                    if (path != null) {
                        mediaPath = path
                        mediaMime = mime
                    }
                }
            }
        }
        return MmsParts(text.toString(), mediaPath, mediaMime)
    }

    private fun copyPart(uri: Uri, mime: String): String? = runCatching {
        val ext = when {
            mime.startsWith("video/") -> "mp4"
            mime == "image/png" -> "png"
            mime == "image/gif" -> "gif"
            mime == "image/webp" -> "webp"
            mime.startsWith("image/") -> "jpg"
            else -> "bin"
        }
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file.absolutePath
    }.getOrNull()
}
