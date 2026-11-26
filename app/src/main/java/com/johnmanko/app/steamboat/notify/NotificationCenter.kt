package com.johnmanko.app.steamboat.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.MainActivity
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.db.ConversationEntity
import com.johnmanko.app.steamboat.data.db.MessageEntity
import com.johnmanko.app.steamboat.data.prefs.NotificationPreferences
import com.johnmanko.app.steamboat.data.prefs.QuickReplyRules
import com.johnmanko.app.steamboat.domain.AlertDelivery
import com.johnmanko.app.steamboat.domain.AlertPolicy
import com.johnmanko.app.steamboat.domain.alertDelivery
import java.time.LocalTime

class NotificationCenter(
    private val context: Context,
    private val preferences: NotificationPreferences,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        val system = system()
        if (system.getNotificationChannel(Notify.CHANNEL_ID) == null) {
            system.createNotificationChannel(
                NotificationChannel(
                    Notify.CHANNEL_ID,
                    context.getString(R.string.messages_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.messages_channel_description)
                    enableVibration(true)
                },
            )
        }
        ensure(QUIET_CHANNEL, context.getString(R.string.messages_channel_quiet), quietSpec())
    }

    suspend fun showIncoming(
        conversation: ConversationEntity,
        recent: List<MessageEntity>,
        quickReplies: List<String> = QuickReplyRules.defaults,
    ) {
        if (!canNotify()) return
        val resolved = preferences.resolve(conversation.id)
        val now = LocalTime.now()
        val delivery = alertDelivery(resolved.policy, now.hour * 60 + now.minute)
        val channelId = channelFor(conversation, resolved.custom, delivery, resolved.policy)
        val title = conversation.title.ifBlank { conversation.address }
        val sender = Person.Builder()
            .setName(title)
            .setKey(conversation.matchKey.ifBlank { conversation.address })
            .build()
        val self = Person.Builder()
            .setName(context.getString(R.string.you))
            .setKey("self")
            .build()
        publishShortcut(conversation, sender)

        val style = NotificationCompat.MessagingStyle(self)
        recent.asReversed().forEach { message ->
            style.addMessage(
                message.body.ifBlank { message.listSnippet() },
                message.sentAt,
                if (message.outgoing) self else sender,
            )
        }
        if (recent.isEmpty()) {
            style.addMessage(conversation.snippet, conversation.updatedAt, sender)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFC48B9F.toInt())
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(Notify.shortcutId(conversation.id))
            .addPerson(sender)
            .setContentIntent(contentIntent(conversation.id))
            .addAction(replyAction(conversation.id))
        quickReplies.take(QuickReplyRules.MAX_ON_NOTIFICATION).forEachIndexed { index, phrase ->
            builder.addAction(quickReplyAction(conversation.id, index, phrase))
        }
        builder.addAction(markReadAction(conversation.id))
        if (delivery == AlertDelivery.QUIET) builder.setSilent(true)
        val notification = builder
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(Notify.notificationId(conversation.id), notification)
    }

    fun cancel(conversationId: Long) {
        manager.cancel(Notify.notificationId(conversationId))
    }

    /**
     * A typed reply marks the notification as lifetime-extended, and the system
     * then ignores [cancel]. Posting a fresh notification drops that flag, so
     * the following cancel actually removes it.
     */
    fun syncGlobal(policy: AlertPolicy) {
        ensureChannel()
        val delivery = configuredDelivery(policy)
        if (delivery == AlertDelivery.QUIET || delivery == AlertDelivery.SYSTEM) return
        ensure(
            channelId(custom = false, conversationId = 0L, delivery),
            context.getString(channelName(delivery)),
            spec(delivery, policy),
        )
    }

    fun syncThread(conversationId: Long, title: String, policy: AlertPolicy) {
        val delivery = configuredDelivery(policy)
        if (delivery == AlertDelivery.QUIET) {
            ensureChannel()
            system().deleteNotificationChannel(threadChannel(conversationId))
            return
        }
        ensure(
            threadChannel(conversationId),
            title.ifBlank { context.getString(R.string.messages_channel) },
            spec(delivery, policy),
        )
    }

    suspend fun forgetThread(conversationId: Long) {
        preferences.clearThread(conversationId)
        system().deleteNotificationChannel(threadChannel(conversationId))
    }

    fun clearAfterDirectReply(conversationId: Long) {
        ensureChannel()
        val id = Notify.notificationId(conversationId)
        val replacement = NotificationCompat.Builder(context, QUIET_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setSilent(true)
            .setTimeoutAfter(400)
            .build()
        manager.notify(id, replacement)
        manager.cancel(id)
    }

    private fun publishShortcut(conversation: ConversationEntity, person: Person) {
        val title = conversation.title.ifBlank { conversation.address }
        val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(
            context,
            Notify.shortcutId(conversation.id),
        )
            .setShortLabel(title)
            .setLongLived(true)
            .setPerson(person)
            .setIntent(openIntent(conversation.id))
            .setCategories(setOf("android.shortcut.conversation"))
            .build()
        runCatching {
            androidx.core.content.pm.ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }
    }

    private fun replyAction(conversationId: Long): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(Notify.KEY_REPLY)
            .setLabel(context.getString(R.string.reply))
            .build()
        val intent = PendingIntent.getBroadcast(
            context,
            Notify.notificationId(conversationId),
            Intent(context, ReplyReceiver::class.java).apply {
                action = Notify.ACTION_REPLY
                putExtra(Notify.EXTRA_CONVERSATION_ID, conversationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            context.getString(R.string.reply),
            intent,
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()
    }

    private fun quickReplyAction(conversationId: Long, index: Int, phrase: String): NotificationCompat.Action {
        val intent = PendingIntent.getBroadcast(
            context,
            Notify.notificationId(conversationId) + QUICK_REPLY_OFFSET + index,
            Intent(context, ReplyReceiver::class.java).apply {
                action = Notify.ACTION_REPLY
                putExtra(Notify.EXTRA_CONVERSATION_ID, conversationId)
                putExtra(Notify.EXTRA_QUICK_REPLY, phrase)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            phrase,
            intent,
        ).build()
    }

    private fun markReadAction(conversationId: Long): NotificationCompat.Action {
        val intent = PendingIntent.getBroadcast(
            context,
            Notify.notificationId(conversationId) + MARK_READ_OFFSET,
            Intent(context, MarkReadReceiver::class.java).apply {
                action = Notify.ACTION_MARK_READ
                putExtra(Notify.EXTRA_CONVERSATION_ID, conversationId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            context.getString(R.string.mark_read),
            intent,
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .build()
    }

    private fun contentIntent(conversationId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            Notify.notificationId(conversationId),
            openIntent(conversationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openIntent(conversationId: Long): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(Notify.EXTRA_CONVERSATION_ID, conversationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            android.os.Build.VERSION.SDK_INT < 33

    private fun channelFor(
        conversation: ConversationEntity,
        custom: Boolean,
        delivery: AlertDelivery,
        policy: AlertPolicy,
    ): String {
        ensureChannel()
        if (delivery == AlertDelivery.QUIET) return QUIET_CHANNEL
        if (!custom && delivery == AlertDelivery.SYSTEM) return Notify.CHANNEL_ID
        val id = channelId(custom, conversation.id, delivery)
        val name = if (custom) {
            conversation.title.ifBlank { conversation.address }
        } else {
            context.getString(channelName(delivery))
        }
        ensure(id, name, spec(delivery, policy))
        return id
    }

    private fun channelId(custom: Boolean, conversationId: Long, delivery: AlertDelivery): String = when {
        delivery == AlertDelivery.QUIET -> QUIET_CHANNEL
        custom -> threadChannel(conversationId)
        delivery == AlertDelivery.SOUND -> SOUND_CHANNEL
        delivery == AlertDelivery.VIBRATE -> VIBRATE_CHANNEL
        delivery == AlertDelivery.BOTH -> BOTH_CHANNEL
        else -> Notify.CHANNEL_ID
    }

    private fun channelName(delivery: AlertDelivery): Int = when (delivery) {
        AlertDelivery.SOUND -> R.string.messages_channel_sound
        AlertDelivery.VIBRATE -> R.string.messages_channel_vibrate
        AlertDelivery.BOTH -> R.string.messages_channel_both
        else -> R.string.messages_channel
    }

    private fun configuredDelivery(policy: AlertPolicy): AlertDelivery = when {
        policy.silent -> AlertDelivery.QUIET
        policy.style == com.johnmanko.app.steamboat.domain.AlertStyle.SOUND -> AlertDelivery.SOUND
        policy.style == com.johnmanko.app.steamboat.domain.AlertStyle.VIBRATE -> AlertDelivery.VIBRATE
        policy.style == com.johnmanko.app.steamboat.domain.AlertStyle.BOTH -> AlertDelivery.BOTH
        else -> AlertDelivery.SYSTEM
    }

    private fun spec(delivery: AlertDelivery, policy: AlertPolicy): ChannelSpec {
        val chosen = policy.soundUri?.let(Uri::parse) ?: Settings.System.DEFAULT_NOTIFICATION_URI
        return when (delivery) {
            AlertDelivery.QUIET -> quietSpec()
            AlertDelivery.SYSTEM -> ChannelSpec(NotificationManager.IMPORTANCE_HIGH, Settings.System.DEFAULT_NOTIFICATION_URI, vibrate = true)
            AlertDelivery.SOUND -> ChannelSpec(NotificationManager.IMPORTANCE_HIGH, chosen, vibrate = false)
            AlertDelivery.VIBRATE -> ChannelSpec(NotificationManager.IMPORTANCE_HIGH, sound = null, vibrate = true)
            AlertDelivery.BOTH -> ChannelSpec(NotificationManager.IMPORTANCE_HIGH, chosen, vibrate = true)
        }
    }

    private fun quietSpec() = ChannelSpec(NotificationManager.IMPORTANCE_HIGH, sound = null, vibrate = false)

    private fun ensure(id: String, name: String, spec: ChannelSpec) {
        val system = system()
        val existing = system.getNotificationChannel(id)
        if (existing != null && matches(existing, spec)) return
        if (existing != null) system.deleteNotificationChannel(id)
        val channel = NotificationChannel(id, name, spec.importance).apply {
            description = context.getString(R.string.messages_channel_description)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(spec.sound, if (spec.sound == null) null else attributes)
            enableVibration(spec.vibrate)
            if (spec.vibrate) vibrationPattern = longArrayOf(0, 200, 120, 200)
        }
        system.createNotificationChannel(channel)
    }

    private fun matches(channel: NotificationChannel, spec: ChannelSpec): Boolean {
        val sound = channel.sound?.takeUnless { it == Uri.EMPTY }?.toString().orEmpty()
        val wanted = spec.sound?.toString().orEmpty()
        return channel.importance == spec.importance && sound == wanted && channel.shouldVibrate() == spec.vibrate
    }

    private fun system(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    private fun threadChannel(conversationId: Long) = "thread-$conversationId"

    private data class ChannelSpec(val importance: Int, val sound: Uri?, val vibrate: Boolean)

    private companion object {
        const val QUIET_CHANNEL = "messages-quiet"
        const val SOUND_CHANNEL = "messages-sound"
        const val VIBRATE_CHANNEL = "messages-vibrate"
        const val BOTH_CHANNEL = "messages-both"
        const val MARK_READ_OFFSET = 100_000
        const val QUICK_REPLY_OFFSET = 200_000
    }
}
