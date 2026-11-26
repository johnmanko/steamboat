package com.johnmanko.app.steamboat.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.johnmanko.app.steamboat.SteamboatApp
import kotlinx.coroutines.launch

/** Completes an MMS download started from [MmsReceiver]. */
class MmsDownloadedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val file = intent.getStringExtra(EXTRA_FILE) ?: return
        val pending = goAsync()
        val app = context.applicationContext as SteamboatApp
        val result = resultCode
        app.appScope.launch {
            try {
                MmsDownload(context.applicationContext).finish(
                    app = app,
                    filePath = file,
                    fallbackFrom = intent.getStringExtra(EXTRA_FROM),
                    transactionId = intent.getStringExtra(EXTRA_TRANSACTION),
                    resultCode = result,
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.johnmanko.app.steamboat.action.MMS_DOWNLOADED"
        const val EXTRA_FILE = "file"
        const val EXTRA_FROM = "from"
        const val EXTRA_TRANSACTION = "transaction"
    }
}
