package com.johnmanko.app.steamboat.domain

/**
 * Normalizes phone numbers so a thread and a contact refer to the same person
 * even when one side includes a country code or formatting.
 */
object PhoneNumbers {
    fun digits(raw: String): String = buildString(raw.length) {
        for (char in raw) {
            when (char.uppercaseChar()) {
                in '0'..'9' -> append(char)
                'A', 'B', 'C' -> append('2')
                'D', 'E', 'F' -> append('3')
                'G', 'H', 'I' -> append('4')
                'J', 'K', 'L' -> append('5')
                'M', 'N', 'O' -> append('6')
                'P', 'Q', 'R', 'S' -> append('7')
                'T', 'U', 'V' -> append('8')
                'W', 'X', 'Y', 'Z' -> append('9')
            }
        }
    }

    fun matchKey(raw: String): String {
        val digits = digits(raw).trimStart('0')
        if (digits.isEmpty()) return ""
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun same(left: String, right: String): Boolean {
        val key = matchKey(left)
        return key.isNotEmpty() && key == matchKey(right)
    }

    fun likePattern(raw: String): String = raw.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    /**
     * One display for every stored shape of the same number.
     * A 10-digit number and the same number with a leading 1 both render as
     * `(555) 123-1234`.
     */
    fun display(raw: String): String {
        val digits = digits(raw)
        val national = when {
            digits.length == 10 -> digits
            digits.length == 11 && digits.startsWith("1") -> digits.drop(1)
            digits.length == 7 -> return "${digits.take(3)}-${digits.drop(3)}"
            else -> return if (raw.trim().startsWith("+")) "+$digits" else digits.ifEmpty { raw.trim() }
        }
        return "(${national.take(3)}) ${national.substring(3, 6)}-${national.substring(6)}"
    }

    /** A phone-only query long enough to start a conversation, or null. */
    fun newContactNumber(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.any { it.isLetter() }) return null
        if (digits(trimmed).length < 3) return null
        return trimmed
    }
}
