package com.johnmanko.app.steamboat.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.johnmanko.app.steamboat.SteamboatApp
import kotlinx.coroutines.launch

/** Incoming SMS for the default messaging app, including texts that can be answered from the shade. */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pending = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val address = messages.firstOrNull()?.displayOriginatingAddress
            ?: messages.firstOrNull()?.originatingAddress
        val body = buildString {
            messages.forEach { part -> append(part.displayMessageBody ?: part.messageBody.orEmpty()) }
        }
        val sentAt = messages.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
        val app = context.applicationContext as SteamboatApp
        app.appScope.launch {
            try {
                if (!address.isNullOrBlank() && body.isNotEmpty()) {
                    app.graph.repository.acceptDeliveredSms(address, body, sentAt)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
