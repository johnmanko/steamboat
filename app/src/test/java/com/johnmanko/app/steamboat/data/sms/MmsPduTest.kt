package com.johnmanko.app.steamboat.data.sms

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MmsPduTest {
    @Test
    fun notificationCarriesTheDownloadLocation() {
        val pdu = pdu {
            write(0x8C)
            write(0x82)
            text(0x98, "t1")
            write(0x8D)
            write(0x92)
            from("15550101001/TYPE=PLMN")
            write(0x8E)
            write(0x04)
            write(0x00)
            write(0x00)
            write(0x10)
            write(0x00)
            text(0x83, "https://mmsc.example/m")
        }
        val notice = MmsPdu.parseNotification(pdu)
        assertEquals("https://mmsc.example/m", notice?.location)
        assertEquals("15550101001", notice?.from)
        assertEquals("t1", notice?.transactionId)
        assertNull(MmsPdu.parseRetrieved(pdu))
    }

    @Test
    fun retrieveConfKeepsTheTextAndThePicture() {
        val picture = byteArrayOf(9, 8, 7)
        val pdu = pdu {
            write(0x8C)
            write(0x84)
            text(0x98, "t1")
            write(0x8D)
            write(0x92)
            write(0x85)
            write(0x04)
            write(0x00)
            write(0x00)
            write(0x00)
            write(0x64)
            from("+15550101001/TYPE=PLMN")
            write(0x84)
            write(0xA3)
            write(2)
            write(1)
            write(2)
            write(0x83)
            write('H'.code)
            write('i'.code)
            write(1)
            write(picture.size)
            write(0x9E)
            write(picture)
        }
        val message = MmsPdu.parseRetrieved(pdu)
        assertEquals("+15550101001", message?.from)
        assertEquals("Hi", message?.text)
        assertEquals(100_000L, message?.sentAt)
        assertArrayEquals(picture, message?.media)
        assertEquals("image/jpeg", message?.mediaMime)
        assertNull(MmsPdu.parseNotification(pdu))
    }

    @Test
    fun aSendRequestCarriesEveryImage() {
        val pdu = MmsPdu.sendRequest(
            addresses = listOf("5550101001"),
            text = "Hi",
            media = listOf(
                MmsPart("image/jpeg", byteArrayOf(1, 2)),
                MmsPart("image/png", byteArrayOf(3, 4, 5)),
            ),
        )
        val text = pdu.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("image/jpeg"))
        assertTrue(text.contains("image/png"))
        assertTrue(text.contains("Hi"))
    }

    @Test
    fun aSendRequestIsNotAnIncomingMessage() {
        val pdu = MmsPdu.sendRequest(listOf("5550101001"), "Hi", byteArrayOf(1, 2, 3, 4), "image/jpeg")
        assertNull(MmsPdu.parseNotification(pdu))
        assertNull(MmsPdu.parseRetrieved(pdu))
    }

    @Test
    fun partsWrittenByTheSenderCanBeReadBack() {
        val picture = byteArrayOf(1, 2, 3, 4)
        val pdu = MmsPdu.sendRequest(listOf("5550101001"), "Hi", picture, "image/jpeg")
        pdu[1] = 0x84.toByte()
        val message = MmsPdu.parseRetrieved(pdu)
        assertEquals("Hi", message?.text)
        assertArrayEquals(picture, message?.media)
        assertEquals("image/jpeg", message?.mediaMime)
    }

    private fun pdu(block: ByteArrayOutputStream.() -> Unit): ByteArray =
        ByteArrayOutputStream().apply(block).toByteArray()

    private fun ByteArrayOutputStream.text(field: Int, value: String) {
        write(field)
        write(value.toByteArray(Charsets.US_ASCII))
        write(0)
    }

    private fun ByteArrayOutputStream.from(address: String) {
        val encoded = address.toByteArray(Charsets.US_ASCII)
        write(0x89)
        write(encoded.size + 2)
        write(0x80)
        write(encoded)
        write(0)
    }
}
