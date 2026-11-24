package com.johnmanko.app.steamboat.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.data.prefs.QuickReplyRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Direct reply from a Steamboat message notification. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as SteamboatApp
        val conversationId = intent.getLongExtra(Notify.EXTRA_CONVERSATION_ID, -1L)
        val typed = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(Notify.KEY_REPLY)
            ?.toString()
        val body = if (typed != null) {
            QuickReplyRules.manual(typed)
        } else {
            QuickReplyRules.manual(intent.getStringExtra(Notify.EXTRA_QUICK_REPLY).orEmpty())
                .take(QuickReplyRules.MAX_PHRASE)
        }
        app.appScope.launch {
            val sent = conversationId > 0 && body.isNotBlank()
            try {
                if (sent) {
                    app.graph.repository.send(conversationId, body)
                    app.graph.repository.markRead(conversationId)
                }
            } finally {
                pending.finish()
            }
            if (sent && typed != null) {
                // Cancel races the system update, so replace the extended
                // notification and dismiss the replacement once it is posted.
                app.graph.notifications.clearAfterDirectReply(conversationId)
                delay(300)
                app.graph.notifications.cancel(conversationId)
            }
        }
    }
}
