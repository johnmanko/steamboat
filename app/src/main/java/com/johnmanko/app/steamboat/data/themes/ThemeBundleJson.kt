package com.johnmanko.app.steamboat.data.themes

internal object ThemeBundleJson {
    fun parse(text: String): ThemeBundle {
        val root = JsonValues.parse(text) as? Map<*, *> ?: invalid("Theme bundle must be an object")
        val themes = root.array("themes").map { item ->
            val theme = item as? Map<*, *> ?: invalid("A theme must be an object")
            ThemeSpec(
                id = theme.text("id"),
                version = theme.version("version"),
                name = theme.text("name"),
                background = theme.color("background"),
                foreground = theme.color("foreground"),
                cursor = theme.color("cursor"),
                colors = theme.colors("colors"),
                previewOutgoing = theme.text("previewOutgoing"),
                previewIncoming = theme.text("previewIncoming"),
            )
        }
        require(themes.isNotEmpty()) { "Theme bundle has no themes" }
        require(themes.map { it.id }.distinct().size == themes.size) { "Theme bundle repeats a theme id" }
        return ThemeBundle(
            id = root.text("id"),
            version = root.version("version"),
            framework = root.version("framework"),
            name = root.text("name"),
            signature = root.optionalText("signature"),
            themes = themes,
        )
    }

    private fun Map<*, *>.text(key: String): String {
        val value = this[key] ?: invalid("Theme bundle is missing $key")
        val text = value as? String ?: invalid("Theme bundle $key must be text")
        require(text.isNotBlank()) { "Theme bundle $key is blank" }
        return text
    }

    private fun Map<*, *>.optionalText(key: String): String? {
        val value = this[key] ?: return null
        if (value == JsonValues.Null) return null
        return value as? String ?: invalid("Theme bundle $key must be text")
    }

    private fun Map<*, *>.version(key: String): Int {
        val value = this[key] ?: invalid("Theme bundle is missing $key")
        val number = value as? Int ?: invalid("Theme bundle $key must be a whole number")
        require(number >= 1) { "Theme bundle $key must be at least 1" }
        return number
    }

    private fun Map<*, *>.color(key: String): String {
        val text = text(key)
        require(HEX.matches(text)) { "Theme bundle $key is not a color" }
        return text
    }

    private fun Map<*, *>.colors(key: String): List<String> {
        val colors = array(key).map { item ->
            val text = item as? String ?: invalid("Theme bundle $key must be colors")
            require(HEX.matches(text)) { "Theme bundle $key has a value that is not a color" }
            text
        }
        require(colors.size == COLOR_COUNT) { "Theme bundle $key must list $COLOR_COUNT colors" }
        return colors
    }

    private fun Map<*, *>.array(key: String): List<*> {
        val value = this[key] ?: invalid("Theme bundle is missing $key")
        return value as? List<*> ?: invalid("Theme bundle $key must be a list")
    }

    private fun invalid(message: String): Nothing = throw IllegalArgumentException(message)

    private val HEX = Regex("#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})")
    private const val COLOR_COUNT = 16
}

private object JsonValues {
    val Null = object {}

    fun parse(text: String): Any? = Reader(text).parseValue()

    private class Reader(private val text: String) {
        private var index = 0

        fun parseValue(): Any? {
            skip()
            val value = when (val char = peek()) {
                '{' -> objectValue()
                '[' -> arrayValue()
                '"' -> stringValue()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", Null)
                else -> {
                    if (char == '-' || char.isDigit()) numberValue() else invalid()
                }
            }
            skip()
            return value
        }

        private fun objectValue(): Map<String, Any?> {
            expect('{')
            val fields = linkedMapOf<String, Any?>()
            skip()
            if (peek() == '}') {
                index++
                return fields
            }
            while (true) {
                skip()
                val key = stringValue()
                skip()
                expect(':')
                fields[key] = parseValue()
                skip()
                when (peek()) {
                    ',' -> index++
                    '}' -> {
                        index++
                        return fields
                    }
                    else -> invalid()
                }
            }
        }

        private fun arrayValue(): List<Any?> {
            expect('[')
            val items = mutableListOf<Any?>()
            skip()
            if (peek() == ']') {
                index++
                return items
            }
            while (true) {
                items += parseValue()
                skip()
                when (peek()) {
                    ',' -> index++
                    ']' -> {
                        index++
                        return items
                    }
                    else -> invalid()
                }
            }
        }

        private fun stringValue(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                val char = take()
                when (char) {
                    '"' -> return out.toString()
                    '\\' -> out.append(escape())
                    else -> out.append(char)
                }
            }
        }

        private fun escape(): Char {
            return when (val char = take()) {
                '"', '\\', '/' -> char
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> {
                    var code = 0
                    repeat(4) { code = code * 16 + hex(take()) }
                    code.toChar()
                }
                else -> invalid()
            }
        }

        private fun numberValue(): Int {
            val start = index
            if (peek() == '-') index++
            if (!peek().isDigit()) invalid()
            while (index < text.length && peek().isDigit()) index++
            if (index < text.length && peek() in ".eE") invalid()
            return text.substring(start, index).toIntOrNull() ?: invalid()
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!text.startsWith(word, index)) invalid()
            index += word.length
            return value
        }

        private fun skip() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        private fun peek(): Char = if (index < text.length) text[index] else invalid()

        private fun take(): Char = if (index < text.length) text[index++] else invalid()

        private fun expect(char: Char) {
            if (peek() != char) invalid()
            index++
        }

        private fun hex(char: Char): Int = char.digitToIntOrNull(16) ?: invalid()

        private fun invalid(): Nothing = throw IllegalArgumentException("Theme bundle is not valid JSON")
    }
}
