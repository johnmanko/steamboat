package com.johnmanko.app.steamboat.data.sms

import java.io.ByteArrayOutputStream

/**
 * A small MMS send-request PDU: optional text plus one image or video part.
 * Carriers that cannot accept it fail the send, and the thread still keeps the file.
 */
internal object MmsPdu {
    fun sendRequest(addresses: List<String>, text: String, media: ByteArray?, mediaMime: String?): ByteArray {
        val parts = ArrayList<ByteArray>(2)
        if (text.isNotBlank()) parts += part("text/plain", text.toByteArray(Charsets.UTF_8))
        if (media != null && !mediaMime.isNullOrBlank()) parts += part(mediaMime, media)
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
}
