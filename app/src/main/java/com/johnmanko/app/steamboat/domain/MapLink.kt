package com.johnmanko.app.steamboat.domain

import java.util.Locale

enum class MapProvider {
    OPENSTREETMAP,
    GOOGLE,
    ;

    companion object {
        fun fromStored(value: String?): MapProvider =
            entries.firstOrNull { it.name == value } ?: OPENSTREETMAP
    }
}

/** A browser link both Android and iPhone messages can open. */
fun mapLink(provider: MapProvider, latitude: Double, longitude: Double): String {
    val lat = String.format(Locale.US, "%.6f", latitude)
    val lon = String.format(Locale.US, "%.6f", longitude)
    return when (provider) {
        MapProvider.OPENSTREETMAP ->
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=16/$lat/$lon"
        MapProvider.GOOGLE ->
            "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
    }
}

/**
 * Puts [snippet] where the cursor is, or in place of the selected range.
 * The returned index is where the cursor should sit after the insert.
 */
fun insertAtSelection(text: String, start: Int, end: Int, snippet: String): Pair<String, Int> {
    val from = start.coerceIn(0, text.length)
    val to = end.coerceIn(from, text.length)
    return text.substring(0, from) + snippet + text.substring(to) to from + snippet.length
}
