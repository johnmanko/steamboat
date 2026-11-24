package com.johnmanko.app.steamboat.ui.notify

import android.text.format.DateFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.domain.AlertPolicy
import com.johnmanko.app.steamboat.domain.AlertStyle
import java.util.Calendar

fun alertIcon(policy: AlertPolicy): ImageVector = when {
    policy.silent -> Icons.Filled.NotificationsOff
    policy.style == AlertStyle.SOUND -> Icons.AutoMirrored.Filled.VolumeUp
    policy.style == AlertStyle.VIBRATE -> Icons.Filled.Vibration
    policy.style == AlertStyle.BOTH -> Icons.Filled.NotificationsActive
    else -> Icons.Filled.PhonelinkRing
}

@Composable
fun alertClueLabel(policy: AlertPolicy): String {
    val kind = when {
        policy.silent -> stringResource(R.string.notification_silent)
        policy.style == AlertStyle.SOUND -> stringResource(R.string.notification_sound)
        policy.style == AlertStyle.VIBRATE -> stringResource(R.string.notification_vibrate)
        policy.style == AlertStyle.BOTH -> stringResource(R.string.notification_both)
        else -> stringResource(R.string.notification_system)
    }
    if (policy.silent || !policy.scheduled) return kind
    return stringResource(
        R.string.notification_clue_scheduled,
        kind,
        formatMinute(policy.startMinute),
        formatMinute(policy.endMinute),
    )
}

@Composable
fun formatMinute(minute: Int): String {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minute.coerceIn(0, 23 * 60 + 59) / 60)
        set(Calendar.MINUTE, minute % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}
