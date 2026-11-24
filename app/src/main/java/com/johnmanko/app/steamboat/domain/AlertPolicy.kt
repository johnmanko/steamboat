package com.johnmanko.app.steamboat.domain

import java.net.URLDecoder
import java.net.URLEncoder

enum class AlertStyle {
    SYSTEM,
    SOUND,
    VIBRATE,
    BOTH,
    ;

    companion object {
        fun fromStored(value: String?): AlertStyle =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class AlertDelivery {
    QUIET,
    SYSTEM,
    SOUND,
    VIBRATE,
    BOTH,
}

data class AlertPolicy(
    val silent: Boolean = false,
    val scheduled: Boolean = false,
    val startMinute: Int = 8 * 60,
    val endMinute: Int = 22 * 60,
    val style: AlertStyle = AlertStyle.SYSTEM,
    val soundUri: String? = null,
)

/** Sound and vibration are on from [start] until [end]. A window that passes midnight wraps around. Equal ends mean all day. */
fun minuteIsActive(start: Int, end: Int, now: Int): Boolean {
    val from = start.mod(MINUTES_PER_DAY)
    val until = end.mod(MINUTES_PER_DAY)
    val minute = now.mod(MINUTES_PER_DAY)
    if (from == until) return true
    return if (from < until) minute in from until until else minute >= from || minute < until
}

fun alertDelivery(policy: AlertPolicy, minuteOfDay: Int): AlertDelivery {
    if (policy.silent) return AlertDelivery.QUIET
    if (policy.scheduled && !minuteIsActive(policy.startMinute, policy.endMinute, minuteOfDay)) {
        return AlertDelivery.QUIET
    }
    return when (policy.style) {
        AlertStyle.SYSTEM -> AlertDelivery.SYSTEM
        AlertStyle.SOUND -> AlertDelivery.SOUND
        AlertStyle.VIBRATE -> AlertDelivery.VIBRATE
        AlertStyle.BOTH -> AlertDelivery.BOTH
    }
}

fun encodeAlertPolicy(policy: AlertPolicy): String {
    val sound = URLEncoder.encode(policy.soundUri.orEmpty(), "UTF-8")
    return listOf(
        if (policy.silent) "1" else "0",
        if (policy.scheduled) "1" else "0",
        policy.startMinute.coerceIn(0, MINUTES_PER_DAY - 1).toString(),
        policy.endMinute.coerceIn(0, MINUTES_PER_DAY - 1).toString(),
        policy.style.name,
        sound,
    ).joinToString("|")
}

fun decodeAlertPolicy(raw: String): AlertPolicy? {
    val parts = raw.split('|')
    if (parts.size != 6) return null
    val silent = when (parts[0]) {
        "1" -> true
        "0" -> false
        else -> return null
    }
    val scheduled = when (parts[1]) {
        "1" -> true
        "0" -> false
        else -> return null
    }
    val start = parts[2].toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
    val end = parts[3].toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY } ?: return null
    val style = AlertStyle.entries.firstOrNull { it.name == parts[4] } ?: return null
    val sound = URLDecoder.decode(parts[5], "UTF-8").ifBlank { null }
    return AlertPolicy(silent, scheduled, start, end, style, sound)
}

private const val MINUTES_PER_DAY = 24 * 60
