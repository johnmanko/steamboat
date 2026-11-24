package com.johnmanko.app.steamboat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.prefs.LocationPreferences
import com.johnmanko.app.steamboat.data.prefs.LocationSettings
import com.johnmanko.app.steamboat.domain.MapProvider
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationRoute(
    preferences: LocationPreferences,
    onBack: () -> Unit,
) {
    val settings by preferences.settings.collectAsStateWithLifecycle(LocationSettings())
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = washTopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { preferences.setEnabled(!settings.enabled) } }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.location_share),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.location_share_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            scope.launch { preferences.setEnabled(enabled) }
                        },
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.location_map),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                MapChoice(
                    label = stringResource(R.string.location_openstreetmap),
                    selected = settings.provider == MapProvider.OPENSTREETMAP,
                    onSelect = { scope.launch { preferences.setProvider(MapProvider.OPENSTREETMAP) } },
                )
            }
            item {
                MapChoice(
                    label = stringResource(R.string.location_google),
                    selected = settings.provider == MapProvider.GOOGLE,
                    onSelect = { scope.launch { preferences.setProvider(MapProvider.GOOGLE) } },
                )
            }
        }
    }
}

@Composable
private fun MapChoice(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
