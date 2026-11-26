package com.johnmanko.app.steamboat.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MmsReceivePduTest {
    @Test
    fun extractsHttpContentLocation() {
        val pdu = byteArrayOf(0x8C.toByte(), 0x80.toByte()) +
            "http://mms.example/message/123".toByteArray(Charsets.US_ASCII) +
            byteArrayOf(0)

        assertEquals(
            "http://mms.example/message/123",
            MmsReceivePdu.contentLocation(pdu),
        )
    }

    @Test
    fun ignoresPduWithoutLocation() {
        assertNull(MmsReceivePdu.contentLocation(byteArrayOf(0x8C.toByte(), 0x80.toByte())))
    }
}
