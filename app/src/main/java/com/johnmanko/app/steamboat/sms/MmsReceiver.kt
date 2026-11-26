package com.johnmanko.app.steamboat.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.data.sms.MmsReceivePdu
import java.io.File
import kotlinx.coroutines.launch

/**
 * Receives MMS download notifications and asks the carrier to retrieve the PDU.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Mms.Intents.WAP_PUSH_DELIVER_ACTION) return
        val raw = intent.getByteArrayExtra("data") ?: intent.getByteArrayExtra(Intent.EXTRA_DATA_REMOVED)
        val contentLocation = MmsReceivePdu.contentLocation(raw)
            ?: intent.data?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (contentLocation.isNullOrBlank()) return

        val pending = goAsync()
        val dir = File(context.cacheDir, "mms-download").apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}-${System.nanoTime()}.mms")
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.grantUriPermission(
            "com.android.phone",
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        context.getSystemService(SmsManager::class.java).downloadMultimediaMessage(
            context,
            contentLocation,
            uri,
            null,
            object : SmsManager.DownloadMultimediaMessageCallback() {
                override fun onSuccess() {
                    val app = context.applicationContext as SteamboatApp
                    app.appScope.launch {
                        try {
                            app.graph.repository.importSms()
                        } finally {
                            file.delete()
                            pending.finish()
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    file.delete()
                    pending.finish()
                }
            },
        )
    }
}
