package com.johnmanko.app.steamboat.ui.format

data class TextExcerpt(
    val text: String,
    val highlights: List<IntRange>,
)

fun textExcerpt(body: String, query: String, radius: Int = 42): TextExcerpt {
    val needle = query.trim()
    if (needle.isEmpty()) return TextExcerpt(body, emptyList())
    val matches = matchRanges(body, needle)
    if (matches.isEmpty()) return TextExcerpt(body, emptyList())
    val focus = matches.first()
    val from = (focus.first - radius).coerceAtLeast(0)
    val to = (focus.last + radius).coerceAtMost(body.length)
    val prefix = if (from > 0) "…" else ""
    val suffix = if (to < body.length) "…" else ""
    val highlights = matches.mapNotNull { range ->
        val start = range.first.coerceAtLeast(from)
        val end = (range.last + 1).coerceAtMost(to)
        if (start >= end) null else (start - from + prefix.length) until (end - from + prefix.length)
    }
    return TextExcerpt(prefix + body.substring(from, to) + suffix, highlights)
}

private fun matchRanges(body: String, needle: String): List<IntRange> {
    val found = mutableListOf<IntRange>()
    var start = 0
    while (start < body.length) {
        val index = body.indexOf(needle, start, ignoreCase = true)
        if (index < 0) break
        val end = index + needle.length
        found += index until end
        start = end.coerceAtLeast(index + 1)
    }
    return found
}
