package com.johnmanko.app.steamboat.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.notify.Notify
import kotlinx.coroutines.launch

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val pending = goAsync()
        val messageId = intent.getLongExtra(Notify.EXTRA_MESSAGE_ID, -1L)
        // Handsets report Activity.RESULT_OK. The emulator radio reports
        // SmsManager.RESULT_ERROR_NONE (0), which is the same value as RESULT_CANCELED.
        val sent = resultCode == Activity.RESULT_OK || resultCode == SmsManager.RESULT_ERROR_NONE
        if (!sent) {
            Log.w(TAG, "SMS send failed result=$resultCode error=${intent.getIntExtra("errorCode", -1)}")
        }
        val app = context.applicationContext as SteamboatApp
        app.appScope.launch {
            try {
                if (messageId > 0) app.graph.repository.markDelivery(messageId, sent)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.johnmanko.app.steamboat.action.SMS_SENT"
        private const val TAG = "Steamboat"
    }
}
