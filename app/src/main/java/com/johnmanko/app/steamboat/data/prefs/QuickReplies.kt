package com.johnmanko.app.steamboat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.quickReplyStore by preferencesDataStore(name = "quick_replies")

object QuickReplyRules {
    const val MAX_PHRASE = 15
    const val MAX_MANUAL = 143
    const val MAX_SAVED = 8
    const val MAX_ON_NOTIFICATION = 2

    val defaults = listOf("On my way", "OK", "Thanks!")

    fun sanitize(raw: List<String>): List<String> =
        raw.map { it.trim().replace('\n', ' ').take(MAX_PHRASE) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_SAVED)

    fun manual(raw: String): String = raw.trim().take(MAX_MANUAL)
}

class QuickReplies(private val context: Context) {
    val phrases: Flow<List<String>> = context.quickReplyStore.data.map { prefs ->
        prefs[Key]?.let(::decode) ?: QuickReplyRules.defaults
    }

    suspend fun current(): List<String> = phrases.first()

    suspend fun set(phrases: List<String>) {
        val stored = QuickReplyRules.sanitize(phrases)
        context.quickReplyStore.edit { prefs ->
            prefs[Key] = stored.joinToString("\n")
        }
    }

    private fun decode(stored: String): List<String> = QuickReplyRules.sanitize(stored.split('\n'))

    private companion object {
        val Key = stringPreferencesKey("phrases")
    }
}
