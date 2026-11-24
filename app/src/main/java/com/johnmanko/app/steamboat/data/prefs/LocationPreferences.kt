package com.johnmanko.app.steamboat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.johnmanko.app.steamboat.domain.MapProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.locationStore by preferencesDataStore(name = "location")

data class LocationSettings(
    val enabled: Boolean = true,
    val provider: MapProvider = MapProvider.OPENSTREETMAP,
)

class LocationPreferences(private val context: Context) {
    val settings: Flow<LocationSettings> = context.locationStore.data.map { prefs ->
        LocationSettings(
            enabled = prefs[Enabled] ?: true,
            provider = MapProvider.fromStored(prefs[Provider]),
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.locationStore.edit { prefs -> prefs[Enabled] = enabled }
    }

    suspend fun setProvider(provider: MapProvider) {
        context.locationStore.edit { prefs -> prefs[Provider] = provider.name }
    }

    private companion object {
        val Enabled = booleanPreferencesKey("enabled")
        val Provider = stringPreferencesKey("provider")
    }
}
