package com.johnmanko.app.steamboat

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.StrictMode
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.di.AppGraph
import com.johnmanko.app.steamboat.notify.Notify
import com.johnmanko.app.steamboat.sms.SmsIntents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Pastel SMS client. Threads are stored in Room and paged, so opening a long
 * conversation does not load every message. Contacts are matched to threads by
 * phone number. Notification replies and the system RESPOND_VIA_MESSAGE action
 * both write through [com.johnmanko.app.steamboat.data.repo.MessagingRepository].
 */
class SteamboatApp : Application() {
    lateinit var graph: AppGraph
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
                    .build(),
            )
        }
        graph = AppGraph(this)
        runBlocking(Dispatchers.IO) {
            graph.themes.ensureBundledInstalled()
        }
        appScope.launch(Dispatchers.IO) {
            graph.themes.watch()
        }
        graph.notifications.ensureChannel()
        appScope.launch {
            val wroteSampleContacts = graph.repository.installSampleInbox()
            val canReadContacts = ContextCompat.checkSelfPermission(
                this@SteamboatApp,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED
            if (wroteSampleContacts || canReadContacts) {
                graph.repository.syncContacts()
            }
            graph.repository.importSms()
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val requested = intent.getLongExtra(Notify.EXTRA_CONVERSATION_ID, -1L)
        if (requested > 0L) {
            graph.navigator.open(requested, null)
            return
        }
        val address = SmsIntents.recipient(intent) ?: return
        val draft = SmsIntents.body(intent)
        appScope.launch {
            val id = graph.repository.openConversation(address)
            graph.navigator.open(id, draft)
        }
    }
}
