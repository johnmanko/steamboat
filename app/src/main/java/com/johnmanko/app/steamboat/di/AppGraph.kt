package com.johnmanko.app.steamboat.di

import android.content.Context
import com.johnmanko.app.steamboat.data.contacts.ContactSync
import com.johnmanko.app.steamboat.data.db.SteamboatDatabase
import com.johnmanko.app.steamboat.data.prefs.LocationPreferences
import com.johnmanko.app.steamboat.data.prefs.NotificationPreferences
import com.johnmanko.app.steamboat.data.prefs.QuickReplies
import com.johnmanko.app.steamboat.data.prefs.ThemePreferences
import com.johnmanko.app.steamboat.data.themes.ThemeDatabase
import com.johnmanko.app.steamboat.data.themes.ThemeLibrary
import com.johnmanko.app.steamboat.data.repo.MessageStore
import com.johnmanko.app.steamboat.data.repo.MessagingRepository
import com.johnmanko.app.steamboat.data.sample.SampleInbox
import com.johnmanko.app.steamboat.data.sms.AttachmentStore
import com.johnmanko.app.steamboat.data.sms.MmsSender
import com.johnmanko.app.steamboat.data.sms.RcsAvailability
import com.johnmanko.app.steamboat.data.sms.SmsImporter
import com.johnmanko.app.steamboat.data.sms.SmsSender
import com.johnmanko.app.steamboat.notify.NotificationCenter

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    val themes = ThemeLibrary(appContext, ThemeDatabase.create(appContext), ThemePreferences(appContext))
    val quickReplies = QuickReplies(appContext)
    val location = LocationPreferences(appContext)
    val notificationSettings = NotificationPreferences(appContext)
    val navigator = OpenRequestBus()
    val notifications = NotificationCenter(appContext, notificationSettings)
    val repository: MessagingRepository

    init {
        val database = SteamboatDatabase.create(appContext)
        val store = MessageStore(database)
        repository = MessagingRepository(
            context = appContext,
            store = store,
            contactSync = ContactSync(appContext),
            smsImporter = SmsImporter(appContext),
            smsSender = SmsSender(appContext),
            mmsSender = MmsSender(appContext),
            attachments = AttachmentStore(appContext),
            rcs = RcsAvailability(appContext),
            notifications = notifications,
            quickReplies = quickReplies,
            sampleInbox = SampleInbox(appContext, store),
        )
    }
}
