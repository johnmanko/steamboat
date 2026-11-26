package com.johnmanko.app.steamboat.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.data.sms.MmsPdu
import kotlinx.coroutines.launch

/** Incoming multimedia message for the default messaging app. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        val mime = intent.type
        if (mime != null && !mime.equals(MMS_MIME, ignoreCase = true)) return
        val pdu = intent.getByteArrayExtra("data") ?: return
        val pending = goAsync()
        val app = context.applicationContext as SteamboatApp
        app.appScope.launch {
            try {
                val retrieved = MmsPdu.parseRetrieved(pdu)
                if (retrieved != null && !retrieved.from.isNullOrBlank()) {
                    app.graph.repository.acceptDeliveredMms(
                        address = retrieved.from,
                        body = retrieved.text,
                        sentAt = retrieved.sentAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                        media = retrieved.media,
                        mediaMime = retrieved.mediaMime,
                        transactionId = null,
                    )
                } else {
                    val notice = MmsPdu.parseNotification(pdu)
                    if (notice == null) {
                        Log.w(TAG, "Unrecognized MMS push (${pdu.size} bytes)")
                    } else {
                        MmsDownload(context.applicationContext).enqueue(notice)
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "MMS receive failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "Steamboat"
        const val MMS_MIME = "application/vnd.wap.mms-message"
    }
}
