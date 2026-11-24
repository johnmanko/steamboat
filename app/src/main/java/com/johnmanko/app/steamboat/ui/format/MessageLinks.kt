package com.johnmanko.app.steamboat.ui.format

internal data class TextLink(
    val start: Int,
    val endExclusive: Int,
    val url: String,
)

private val LinkPattern = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"']+""")
private val Trailing = setOf('.', ',', ';', ':', '!', '?', '"', '\'', ']')

internal fun textLinks(body: String): List<TextLink> =
    LinkPattern.findAll(body).mapNotNull { match ->
        var raw = match.value
        var end = match.range.last + 1
        while (raw.isNotEmpty()) {
            val last = raw.last()
            val extraParen = last == ')' && raw.count { it == '(' } < raw.count { it == ')' }
            if (last !in Trailing && !extraParen) break
            raw = raw.dropLast(1)
            end--
        }
        if (raw.isEmpty()) return@mapNotNull null
        val url = when {
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            else -> "https://$raw"
        }
        TextLink(match.range.first, end, url)
    }.toList()
