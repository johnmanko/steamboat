package com.johnmanko.app.steamboat.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.johnmanko.app.steamboat.SteamboatApp
import kotlinx.coroutines.launch

class MarkReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as SteamboatApp
        val conversationId = intent.getLongExtra(Notify.EXTRA_CONVERSATION_ID, -1L)
        app.appScope.launch {
            try {
                if (conversationId > 0) app.graph.repository.markRead(conversationId)
            } finally {
                pending.finish()
            }
        }
    }
}
