package com.johnmanko.app.steamboat.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.johnmanko.app.steamboat.domain.PendingAttachment
import com.johnmanko.app.steamboat.notify.Notify
import com.johnmanko.app.steamboat.sms.SmsSentReceiver
import java.io.File
import java.io.IOException

class MmsSender(private val context: Context) {
    fun send(
        messageId: Long,
        addresses: List<String>,
        text: String,
        media: List<PendingAttachment>,
    ) {
        val parts = media.map { item ->
            val file = File(item.path)
            if (!file.isFile) throw IOException("Missing attachment")
            MmsPart(item.mime, file.readBytes())
        }
        val destinations = addresses.map { address ->
            PhoneNumberUtils.normalizeNumber(address).ifBlank { address }
        }
        val pdu = MmsPdu.sendRequest(
            addresses = destinations,
            text = text,
            media = parts,
        )
        val dir = File(context.cacheDir, "mms").apply { mkdirs() }
        val file = File(dir, "$messageId.mms")
        file.writeBytes(pdu)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.grantUriPermission(
            "com.android.phone",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val sent = PendingIntent.getBroadcast(
            context,
            messageId.toInt(),
            Intent(context, SmsSentReceiver::class.java).apply {
                action = SmsSentReceiver.ACTION
                putExtra(Notify.EXTRA_MESSAGE_ID, messageId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        context.getSystemService(SmsManager::class.java)
            .sendMultimediaMessage(context, uri, null, null, sent, messageId)
    }
}
