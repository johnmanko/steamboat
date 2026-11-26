package com.johnmanko.app.steamboat.data.sms

/**
 * Extracts the carrier-provided location from a WAP MMS notification PDU.
 */
internal object MmsReceivePdu {
    fun contentLocation(pdu: ByteArray?): String? {
        if (pdu.isNullOrEmpty()) return null
        val http = findUrl(pdu)
        if (http != null) return http

        val field = pdu.indexOf(0x8E)
        if (field < 0) return null
        var index = field + 1
        if (index < pdu.size && pdu[index].toInt() and 0xFF == 0x80) index++
        if (index < pdu.size && pdu[index].toInt() and 0xFF == 0x1F) index++
        return readNullTerminated(pdu, index)
    }

    private fun findUrl(pdu: ByteArray): String? {
        val ascii = pdu.toString(Charsets.ISO_8859_1)
        val start = sequenceOf(
            ascii.indexOf("http://"),
            ascii.indexOf("https://"),
        ).filter { it >= 0 }.minOrNull() ?: return null
        val end = ascii.indexOf('\u0000', start).takeIf { it >= 0 } ?: ascii.length
        return ascii.substring(start, end).trim().takeIf { it.isNotEmpty() }
    }

    private fun readNullTerminated(pdu: ByteArray, start: Int): String? {
        var index = start
        while (index < pdu.size && pdu[index].toInt() and 0xFF in 0x80..0xFF) index++
        val end = pdu.indexOf(0, index)
        if (end <= index) return null
        return pdu.copyOfRange(index, end)
            .toString(Charsets.ISO_8859_1)
            .trim()
            .takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun ByteArray.indexOf(value: Int): Int {
        val target = value.toByte()
        for (index in indices) if (this[index] == target) return index
        return -1
    }
}
