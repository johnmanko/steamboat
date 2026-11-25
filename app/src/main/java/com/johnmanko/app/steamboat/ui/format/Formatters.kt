package com.johnmanko.app.steamboat.ui.format

import com.johnmanko.app.steamboat.domain.PhoneNumbers
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

fun formatPhone(raw: String): String = PhoneNumbers.display(raw)

fun formatConversationTime(epochMillis: Long, yesterdayLabel: String): String {
    if (epochMillis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    return when (val date = time.toLocalDate()) {
        today -> timeFormatter.format(time)
        today.minusDays(1) -> yesterdayLabel
        else -> dayFormatter.format(date)
    }
}

fun formatMessageTime(epochMillis: Long, yesterdayLabel: String): String {
    if (epochMillis <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = java.time.LocalDate.now(zone)
    val clock = timeFormatter.format(time)
    return when (val date = time.toLocalDate()) {
        today -> clock
        today.minusDays(1) -> "$yesterdayLabel $clock"
        else -> "${dayFormatter.format(date)} $clock"
    }
}

fun initials(title: String): String {
    val parts = title.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.getDefault())
        else -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
    }
}
