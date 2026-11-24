package com.johnmanko.app.steamboat.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.prefs.NotificationPreferences
import com.johnmanko.app.steamboat.domain.AlertPolicy
import com.johnmanko.app.steamboat.domain.AlertStyle
import com.johnmanko.app.steamboat.notify.NotificationCenter
import com.johnmanko.app.steamboat.ui.notify.formatMinute
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsRoute(
    preferences: NotificationPreferences,
    notifications: NotificationCenter,
    onBack: () -> Unit,
) {
    val policy by preferences.global.collectAsStateWithLifecycle(AlertPolicy())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val picked = pickedRingtone(result.data)
        scope.launch {
            val next = policy.copy(soundUri = picked)
            preferences.setGlobal(next)
            notifications.syncGlobal(next)
        }
    }
    fun update(next: AlertPolicy) {
        scope.launch {
            preferences.setGlobal(next)
            notifications.syncGlobal(next)
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            NotificationEditor(
                policy = policy,
                soundName = notificationSoundName(context, policy.soundUri),
                onChange = ::update,
                onPickSound = { soundPicker.launch(ringtoneIntent(context, policy.soundUri)) },
                onPickTime = { start ->
                    pickTime(context, if (start) policy.startMinute else policy.endMinute) { minute ->
                        update(if (start) policy.copy(startMinute = minute) else policy.copy(endMinute = minute))
                    }
                },
            )
        }
    }
}

@Composable
fun ThreadNotificationDialog(
    conversationId: Long,
    title: String,
    preferences: NotificationPreferences,
    notifications: NotificationCenter,
    onDismiss: () -> Unit,
) {
    val global by preferences.global.collectAsStateWithLifecycle(AlertPolicy())
    val overrides by preferences.overrides.collectAsStateWithLifecycle(emptyMap())
    val custom = overrides[conversationId]
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val editing = custom ?: global
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val current = overrides[conversationId] ?: return@rememberLauncherForActivityResult
        val next = current.copy(soundUri = pickedRingtone(result.data))
        scope.launch {
            preferences.setThread(conversationId, next)
            notifications.syncThread(conversationId, title, next)
        }
    }
    fun save(next: AlertPolicy?) {
        scope.launch {
            if (next == null) {
                notifications.forgetThread(conversationId)
            } else {
                preferences.setThread(conversationId, next)
                notifications.syncThread(conversationId, title, next)
            }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.notifications_title),
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { save(if (custom == null) global else null) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notification_thread_custom),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = custom != null,
                        onCheckedChange = { enabled -> save(if (enabled) global else null) },
                    )
                }
                if (custom == null) {
                    Text(
                        text = stringResource(R.string.notification_thread_following),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    NotificationEditor(
                        policy = editing,
                        soundName = notificationSoundName(context, editing.soundUri),
                        onChange = { save(it) },
                        onPickSound = { soundPicker.launch(ringtoneIntent(context, editing.soundUri)) },
                        onPickTime = { start ->
                            pickTime(context, if (start) editing.startMinute else editing.endMinute) { minute ->
                                save(if (start) editing.copy(startMinute = minute) else editing.copy(endMinute = minute))
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.notification_done)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationEditor(
    policy: AlertPolicy,
    soundName: String,
    onChange: (AlertPolicy) -> Unit,
    onPickSound: () -> Unit,
    onPickTime: (start: Boolean) -> Unit,
) {
    Column(Modifier.padding(bottom = 12.dp)) {
        SwitchRow(
            title = stringResource(R.string.notification_silent),
            body = stringResource(R.string.notification_silent_body),
            checked = policy.silent,
            onChecked = { onChange(policy.copy(silent = it)) },
        )
        SwitchRow(
            title = stringResource(R.string.notification_schedule),
            body = stringResource(R.string.notification_schedule_body),
            checked = policy.scheduled,
            onChecked = { onChange(policy.copy(scheduled = it)) },
        )
        if (policy.scheduled) {
            TimeRow(stringResource(R.string.notification_from), formatMinute(policy.startMinute)) {
                onPickTime(true)
            }
            TimeRow(stringResource(R.string.notification_until), formatMinute(policy.endMinute)) {
                onPickTime(false)
            }
        }
        Text(
            text = stringResource(R.string.notification_alert),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        AlertStyle.entries.forEach { style ->
            AlertChoice(
                label = stringResource(styleLabel(style)),
                selected = policy.style == style,
                onSelect = { onChange(policy.copy(style = style)) },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickSound)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.notification_sound_choice),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = soundName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun TimeRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AlertChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun styleLabel(style: AlertStyle): Int = when (style) {
    AlertStyle.SYSTEM -> R.string.notification_system
    AlertStyle.SOUND -> R.string.notification_sound
    AlertStyle.VIBRATE -> R.string.notification_vibrate
    AlertStyle.BOTH -> R.string.notification_both
}

private fun ringtoneIntent(context: Context, current: String?): Intent {
    val existing = current?.let(Uri::parse) ?: Settings.System.DEFAULT_NOTIFICATION_URI
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
    }
}

private fun pickedRingtone(data: Intent?): String? {
    val uri = if (Build.VERSION.SDK_INT >= 33) {
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
    if (uri == null || uri == Settings.System.DEFAULT_NOTIFICATION_URI) return null
    return uri.toString()
}

private fun notificationSoundName(context: Context, uri: String?): String {
    if (uri.isNullOrBlank()) return context.getString(R.string.notification_sound_default)
    val title = RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
    return title?.ifBlank { null } ?: context.getString(R.string.notification_sound_default)
}

private fun pickTime(context: Context, minute: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, mins -> onPicked(hour * 60 + mins) },
        minute / 60,
        minute % 60,
        DateFormat.is24HourFormat(context),
    ).show()
}
