package com.johnmanko.app.steamboat.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.johnmanko.app.steamboat.SteamboatApp
import kotlinx.coroutines.launch

/**
 * System quick reply. Android delivers [Intent.ACTION_RESPOND_VIA_MESSAGE] when the user
 * answers from a notification, the dialer, or another system surface.
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as SteamboatApp
        val scheme = intent?.data?.scheme
        if (scheme == "mms" || scheme == "mmsto") {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val recipients = SmsIntents.recipients(intent)
        val body = SmsIntents.body(intent).orEmpty()
        app.appScope.launch {
            try {
                if (body.isNotBlank()) {
                    recipients.forEach { address ->
                        val conversationId = app.graph.repository.openConversation(address)
                        app.graph.repository.send(conversationId, body)
                    }
                }
            } finally {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }
}
