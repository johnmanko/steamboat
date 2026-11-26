package com.johnmanko.app.steamboat.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.FileProvider
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.data.sms.MmsNotice
import com.johnmanko.app.steamboat.data.sms.MmsPdu
import java.io.File
import java.util.UUID

/** Asks the carrier to fetch a multimedia message, then stores the downloaded PDU. */
internal class MmsDownload(private val context: Context) {
    fun enqueue(notice: MmsNotice) {
        val location = Uri.parse(notice.location)
        if (location.scheme != "http" && location.scheme != "https") {
            Log.w(TAG, "MMS content location was not an http URL")
            return
        }
        val dir = File(context.cacheDir, "mms").apply { mkdirs() }
        val file = File(dir, "in-${UUID.randomUUID()}.mms")
        file.writeBytes(byteArrayOf())
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        WRITERS.forEach { packageName ->
            runCatching {
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        val downloaded = PendingIntent.getBroadcast(
            context,
            file.name.hashCode(),
            Intent(context, MmsDownloadedReceiver::class.java).apply {
                action = MmsDownloadedReceiver.ACTION
                putExtra(MmsDownloadedReceiver.EXTRA_FILE, file.absolutePath)
                putExtra(MmsDownloadedReceiver.EXTRA_FROM, notice.from)
                putExtra(MmsDownloadedReceiver.EXTRA_TRANSACTION, notice.transactionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        try {
            context.getSystemService(SmsManager::class.java)
                .downloadMultimediaMessage(context, notice.location, uri, null, downloaded)
        } catch (error: Exception) {
            Log.w(TAG, "MMS download failed to start", error)
            file.delete()
        }
    }

    suspend fun finish(app: SteamboatApp, filePath: String, fallbackFrom: String?, transactionId: String?, resultCode: Int) {
        val file = File(filePath)
        try {
            if (resultCode != Activity.RESULT_OK) {
                Log.w(TAG, "MMS download failed result=$resultCode")
                return
            }
            if (!file.isFile || file.length() <= 0L || file.length() > MAX_BYTES) {
                Log.w(TAG, "MMS download was empty")
                return
            }
            val retrieved = MmsPdu.parseRetrieved(file.readBytes())
            if (retrieved == null) {
                Log.w(TAG, "MMS download could not be read")
                return
            }
            app.graph.repository.acceptDeliveredMms(
                address = retrieved.from ?: fallbackFrom.orEmpty(),
                body = retrieved.text,
                sentAt = retrieved.sentAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                media = retrieved.media,
                mediaMime = retrieved.mediaMime,
                transactionId = transactionId,
            )
        } finally {
            file.delete()
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    private companion object {
        const val TAG = "Steamboat"
        const val MAX_BYTES = 12_000_000L
        val WRITERS = listOf(
            "com.android.phone",
            "com.android.mms.service",
            "com.android.providers.telephony",
        )
    }
}
