package com.johnmanko.app.steamboat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenQuickReplies: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
                SettingsRow(
                    title = stringResource(R.string.quick_replies_title),
                    icon = {
                        Icon(
                            Icons.Filled.Quickreply,
                            contentDescription = null,
                        )
                    },
                    onClick = onOpenQuickReplies,
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.location_title),
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_map_pin),
                            contentDescription = null,
                        )
                    },
                    onClick = onOpenLocation,
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.notifications_title),
                    icon = {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                        )
                    },
                    onClick = onOpenNotifications,
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.themes_title),
                    icon = {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = null,
                        )
                    },
                    onClick = onOpenThemes,
                )
            }
            item {
                SettingsRow(
                    title = stringResource(R.string.about_title),
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                        )
                    },
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}
