package com.johnmanko.app.steamboat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.johnmanko.app.steamboat.data.themes.ThemeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeStore by preferencesDataStore(name = "theme")

class ThemePreferences(private val context: Context) {
    val themeId: Flow<String> = context.themeStore.data.map { prefs ->
        prefs[Key]?.takeIf { it.isNotBlank() } ?: ThemeKey.SYSTEM
    }

    suspend fun stored(): String? = context.themeStore.data
        .map { prefs -> prefs[Key]?.takeIf { it.isNotBlank() } }
        .first()

    suspend fun current(): String = themeId.first()

    suspend fun setTheme(id: String) {
        context.themeStore.edit { prefs -> prefs[Key] = id }
    }

    private companion object {
        val Key = stringPreferencesKey("mode")
    }
}
