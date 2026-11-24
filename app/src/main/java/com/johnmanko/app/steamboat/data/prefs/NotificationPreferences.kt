package com.johnmanko.app.steamboat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.johnmanko.app.steamboat.domain.AlertPolicy
import com.johnmanko.app.steamboat.domain.AlertStyle
import com.johnmanko.app.steamboat.domain.decodeAlertPolicy
import com.johnmanko.app.steamboat.domain.encodeAlertPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationStore by preferencesDataStore(name = "notifications")

data class ResolvedAlert(
    val policy: AlertPolicy,
    val custom: Boolean,
)

class NotificationPreferences(private val context: Context) {
    val global: Flow<AlertPolicy> = context.notificationStore.data.map { prefs ->
        AlertPolicy(
            silent = prefs[Silent] ?: false,
            scheduled = prefs[Scheduled] ?: false,
            startMinute = prefs[Start] ?: AlertPolicy().startMinute,
            endMinute = prefs[End] ?: AlertPolicy().endMinute,
            style = AlertStyle.fromStored(prefs[Style]),
            soundUri = prefs[Sound]?.ifBlank { null },
        )
    }

    val overrides: Flow<Map<Long, AlertPolicy>> = context.notificationStore.data.map { prefs ->
        prefs.asMap().mapNotNull { (key, value) ->
            val name = key.name
            if (!name.startsWith(THREAD_PREFIX)) return@mapNotNull null
            val id = name.removePrefix(THREAD_PREFIX).toLongOrNull() ?: return@mapNotNull null
            val policy = (value as? String)?.let(::decodeAlertPolicy) ?: return@mapNotNull null
            id to policy
        }.toMap()
    }

    suspend fun setGlobal(policy: AlertPolicy) {
        context.notificationStore.edit { prefs ->
            prefs[Silent] = policy.silent
            prefs[Scheduled] = policy.scheduled
            prefs[Start] = policy.startMinute
            prefs[End] = policy.endMinute
            prefs[Style] = policy.style.name
            if (policy.soundUri.isNullOrBlank()) prefs.remove(Sound) else prefs[Sound] = policy.soundUri
        }
    }

    suspend fun setThread(conversationId: Long, policy: AlertPolicy) {
        context.notificationStore.edit { prefs ->
            prefs[threadKey(conversationId)] = encodeAlertPolicy(policy)
        }
    }

    suspend fun clearThread(conversationId: Long) {
        context.notificationStore.edit { prefs ->
            prefs.remove(threadKey(conversationId))
        }
    }

    suspend fun resolve(conversationId: Long): ResolvedAlert {
        val prefs = context.notificationStore.data.first()
        val custom = prefs[threadKey(conversationId)]?.let(::decodeAlertPolicy)
        if (custom != null) return ResolvedAlert(custom, custom = true)
        return ResolvedAlert(
            policy = AlertPolicy(
                silent = prefs[Silent] ?: false,
                scheduled = prefs[Scheduled] ?: false,
                startMinute = prefs[Start] ?: AlertPolicy().startMinute,
                endMinute = prefs[End] ?: AlertPolicy().endMinute,
                style = AlertStyle.fromStored(prefs[Style]),
                soundUri = prefs[Sound]?.ifBlank { null },
            ),
            custom = false,
        )
    }

    private fun threadKey(conversationId: Long) = stringPreferencesKey("$THREAD_PREFIX$conversationId")

    private companion object {
        const val THREAD_PREFIX = "t:"
        val Silent = booleanPreferencesKey("silent")
        val Scheduled = booleanPreferencesKey("scheduled")
        val Start = intPreferencesKey("start")
        val End = intPreferencesKey("end")
        val Style = stringPreferencesKey("style")
        val Sound = stringPreferencesKey("sound")
    }
}
