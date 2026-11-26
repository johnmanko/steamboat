package com.johnmanko.app.steamboat.data.sms

import java.io.ByteArrayOutputStream

internal data class MmsNotice(
    val location: String,
    val from: String?,
    val transactionId: String?,
)

internal data class MmsPart(val mime: String, val data: ByteArray)

internal data class MmsRetrieved(
    val from: String?,
    val text: String,
    val sentAt: Long,
    val media: ByteArray?,
    val mediaMime: String?,
)

/**
 * MMS PDUs. Outgoing messages are a send-request with text and one or more media parts.
 * Incoming messages arrive as a notification, then a retrieve-confirmation.
 */
internal object MmsPdu {
    fun sendRequest(addresses: List<String>, text: String, media: ByteArray?, mediaMime: String?): ByteArray =
        sendRequest(
            addresses = addresses,
            text = text,
            media = if (media != null && !mediaMime.isNullOrBlank()) listOf(MmsPart(mediaMime, media)) else emptyList(),
        )

    fun sendRequest(addresses: List<String>, text: String, media: List<MmsPart>): ByteArray {
        val parts = ArrayList<ByteArray>(media.size + 1)
        if (text.isNotBlank()) parts += part("text/plain", text.toByteArray(Charsets.UTF_8))
        media.forEach { item ->
            if (item.data.isNotEmpty() && item.mime.isNotBlank()) parts += part(item.mime, item.data)
        }
        if (parts.isEmpty()) parts += part("text/plain", byteArrayOf())

        val body = ByteArrayOutputStream()
        writeUintvar(body, parts.size.toLong())
        parts.forEach { body.write(it) }

        val headers = ByteArrayOutputStream()
        headers.write(0x8C)
        headers.write(0x80)
        writeTextHeader(headers, 0x98, "sb${System.currentTimeMillis().toString(16)}")
        headers.write(0x8D)
        headers.write(0x92)
        headers.write(0x89)
        headers.write(0x01)
        headers.write(0x81)
        addresses.forEach { address ->
            val digits = address.filter { it.isDigit() || it == '+' }
            if (digits.isNotEmpty()) writeTextHeader(headers, 0x97, "$digits/TYPE=PLMN")
        }
        writeTextHeader(headers, 0x84, "application/vnd.wap.multipart.mixed")
        headers.write(body.toByteArray())
        return headers.toByteArray()
    }

    fun parseNotification(pdu: ByteArray): MmsNotice? {
        val parsed = parse(pdu) ?: return null
        val location = parsed.location?.takeIf { it.isNotBlank() } ?: return null
        if (parsed.type != MESSAGE_NOTIFICATION) return null
        return MmsNotice(location = location, from = parsed.from, transactionId = parsed.transactionId)
    }

    fun parseRetrieved(pdu: ByteArray): MmsRetrieved? {
        val parsed = parse(pdu) ?: return null
        if (parsed.type != MESSAGE_RETRIEVE) return null
        if (parsed.text.isBlank() && parsed.media == null) return null
        return MmsRetrieved(
            from = parsed.from,
            text = parsed.text,
            sentAt = parsed.sentAt,
            media = parsed.media,
            mediaMime = parsed.mediaMime,
        )
    }

    private data class Parsed(
        val type: Int,
        val location: String?,
        val from: String?,
        val transactionId: String?,
        val text: String,
        val sentAt: Long,
        val media: ByteArray?,
        val mediaMime: String?,
    )

    private fun parse(pdu: ByteArray): Parsed? {
        if (pdu.isEmpty()) return null
        return runCatching { Cursor(pdu).parse() }.getOrNull()
    }

    private class Cursor(private val bytes: ByteArray) {
        private var index = 0

        fun parse(): Parsed? {
            if (!seekMessage()) return null
            var type = -1
            var location: String? = null
            var from: String? = null
            var transactionId: String? = null
            var subject = ""
            var sentAt = 0L
            var contentType: String? = null
            while (remaining() > 0) {
                val field = read()
                if (field < 0) break
                when (field) {
                    0x8C -> type = read() and 0x7F
                    0x98, 0x8B -> {
                        val text = readText()
                        if (field == 0x98) transactionId = text
                    }
                    0x83 -> location = readTextish()
                    0x89 -> from = readFrom()
                    0x96 -> subject = readEncodedString()
                    0x85 -> sentAt = epochMillis(readLong())
                    0x84 -> {
                        contentType = readContentType()
                        break
                    }
                    else -> if (!skipValue()) return null
                }
            }
            if (type < 0) return null
            val parts = when {
                contentType == null -> Parts()
                contentType.isMultipart() -> readParts()
                contentType.isMedia() -> Parts(media = readRemaining(), mediaMime = contentType)
                contentType.startsWith("text/") -> Parts(text = readRemaining().toString(Charsets.UTF_8))
                else -> Parts()
            }
            val text = parts.text.ifBlank { subject }
            return Parsed(
                type = type,
                location = location,
                from = from,
                transactionId = transactionId,
                text = text,
                sentAt = sentAt,
                media = parts.media,
                mediaMime = parts.mediaMime,
            )
        }

        private fun seekMessage(): Boolean {
            val limit = minOf(bytes.size - 1, index + 80)
            while (index < limit) {
                val field = bytes[index].toInt() and 0xFF
                val next = bytes[index + 1].toInt() and 0xFF
                if (field == 0x8C && next in 0x80..0x86) return true
                index += 1
            }
            return false
        }

        private fun readParts(): Parts {
            if (remaining() <= 0) return Parts()
            val count = readUintvar().toInt()
            if (count < 0 || count > 32) return Parts()
            val text = StringBuilder()
            var media: ByteArray? = null
            var mediaMime: String? = null
            repeat(count) {
                val headerLen = readUintvar().toInt()
                val dataLen = readUintvar().toInt()
                if (headerLen < 0 || dataLen < 0 || headerLen > remaining() || dataLen > remaining() - headerLen) {
                    return Parts(text.toString().trim(), media, mediaMime)
                }
                val headerEnd = index + headerLen
                val mime = if (headerLen > 0) readContentType() else ""
                index = headerEnd
                val data = readBytes(dataLen)
                when {
                    mime.startsWith("text/plain") -> {
                        if (text.isNotEmpty()) text.append('\n')
                        text.append(data.toString(Charsets.UTF_8))
                    }
                    media == null && mime.isMedia() -> {
                        media = data
                        mediaMime = mime
                    }
                }
            }
            return Parts(text.toString().trim(), media, mediaMime)
        }

        private fun readFrom(): String? {
            val length = readValueLength()
            val end = (index + length).coerceAtMost(bytes.size)
            if (index >= end) {
                index = end
                return null
            }
            val token = read()
            val address = if (token == 0x80 && index < end) readEncodedString() else null
            index = end
            return address?.let(::phone)
        }

        private fun readContentType(): String {
            if (remaining() <= 0) return ""
            val first = peek()
            val raw = when {
                first >= 128 -> wellKnown(read() and 0x7F)
                first <= 31 -> {
                    val length = readValueLength()
                    val end = (index + length).coerceAtMost(bytes.size)
                    val mime = readMediaType()
                    index = end
                    mime
                }
                else -> readText()
            }
            return raw.substringBefore(';').trim().lowercase()
        }

        private fun readMediaType(): String {
            if (remaining() <= 0) return ""
            val first = peek()
            return if (first >= 128) wellKnown(read() and 0x7F) else readText()
        }

        private fun readEncodedString(): String {
            if (remaining() <= 0) return ""
            val first = peek()
            if (first <= 31) {
                val length = readValueLength()
                val end = (index + length).coerceAtMost(bytes.size)
                if (index < end && peek() >= 128) read()
                val text = readText()
                index = end
                return text
            }
            return readText()
        }

        private fun readTextish(): String {
            if (remaining() <= 0) return ""
            return if (peek() <= 31) readEncodedString() else readText()
        }

        private fun readText(): String {
            if (remaining() <= 0) return ""
            if (peek() == 127) read()
            val start = index
            while (index < bytes.size && bytes[index] != 0.toByte()) index += 1
            val text = bytes.copyOfRange(start, index).toString(Charsets.UTF_8)
            if (index < bytes.size) index += 1
            return text
        }

        private fun readLong(): Long {
            val count = read()
            if (count <= 0 || count > 8) return 0
            var value = 0L
            repeat(count) { value = (value shl 8) or read().toLong().and(0xFF) }
            return value
        }

        private fun readValueLength(): Int {
            val first = read()
            if (first < 31) return first
            if (first == 31) return readUintvar().toInt()
            return 0
        }

        private fun readUintvar(): Long {
            var value = 0L
            repeat(5) {
                if (remaining() <= 0) return value
                val byte = read()
                value = (value shl 7) or (byte and 0x7F).toLong()
                if (byte and 0x80 == 0) return value
            }
            return value
        }

        private fun skipValue(): Boolean {
            if (remaining() <= 0) return false
            val first = peek()
            when {
                first >= 128 -> read()
                first == 31 -> {
                    read()
                    skip(readUintvar().toInt())
                }
                first < 31 -> skip(read())
                else -> readText()
            }
            return index <= bytes.size
        }

        private fun readRemaining(): ByteArray = readBytes(remaining())

        private fun readBytes(count: Int): ByteArray {
            if (count <= 0 || index >= bytes.size) return byteArrayOf()
            val end = (index + count).coerceAtMost(bytes.size)
            val slice = bytes.copyOfRange(index, end)
            index = end
            return slice
        }

        private fun skip(count: Int) {
            if (count <= 0) return
            index = (index + count).coerceAtMost(bytes.size)
        }

        private fun read(): Int {
            if (index >= bytes.size) return -1
            return bytes[index++].toInt() and 0xFF
        }

        private fun peek(): Int = if (index >= bytes.size) -1 else bytes[index].toInt() and 0xFF

        private fun remaining() = bytes.size - index
    }

    private data class Parts(
        val text: String = "",
        val media: ByteArray? = null,
        val mediaMime: String? = null,
    )

    private fun wellKnown(code: Int): String = when (code) {
        0x03 -> "text/plain"
        0x1D -> "image/gif"
        0x1E -> "image/jpeg"
        0x20 -> "image/png"
        0x21 -> "image/vnd.wap.wbmp"
        0x23 -> "application/vnd.wap.multipart.mixed"
        0x33 -> "application/vnd.wap.multipart.related"
        else -> "application/octet-stream"
    }

    private fun String.isMultipart(): Boolean =
        startsWith("application/vnd.wap.multipart") || this == "multipart/mixed" || this == "multipart/related"

    private fun String.isMedia(): Boolean = startsWith("image/") || startsWith("video/")

    private fun phone(raw: String): String? {
        val head = raw.substringBefore('/').trim()
        if (head.isEmpty()) return null
        val number = head.filter { it.isDigit() || it == '+' }
        return number.ifBlank { head }
    }

    private fun epochMillis(value: Long): Long = when {
        value <= 0L -> 0L
        value < 10_000_000_000L -> value * 1000L
        else -> value
    }

    private fun part(mime: String, data: ByteArray): ByteArray {
        val contentType = mime.toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        val out = ByteArrayOutputStream()
        writeUintvar(out, contentType.size.toLong())
        writeUintvar(out, data.size.toLong())
        out.write(contentType)
        out.write(data)
        return out.toByteArray()
    }

    private fun writeTextHeader(out: ByteArrayOutputStream, field: Int, value: String) {
        out.write(field)
        val encoded = value.toByteArray(Charsets.US_ASCII)
        if (encoded.isNotEmpty() && encoded[0].toInt() and 0xFF > 127) out.write(127)
        out.write(encoded)
        out.write(0)
    }

    private fun writeUintvar(out: ByteArrayOutputStream, value: Long) {
        val stack = ArrayList<Int>(5)
        var remaining = value
        stack += (remaining and 0x7F).toInt()
        remaining = remaining ushr 7
        while (remaining > 0) {
            stack += ((remaining and 0x7F) or 0x80).toInt()
            remaining = remaining ushr 7
        }
        for (index in stack.lastIndex downTo 0) out.write(stack[index])
    }

    private const val MESSAGE_NOTIFICATION = 0x02
    private const val MESSAGE_RETRIEVE = 0x04
}
