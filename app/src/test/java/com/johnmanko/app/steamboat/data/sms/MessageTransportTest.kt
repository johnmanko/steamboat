package com.johnmanko.app.steamboat.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTransportTest {
    @Test
    fun textPrefersRcsWhenTheCarrierHasIt() {
        assertEquals(MessageTransport.RCS, MessageTransport.choose(rcsRegistered = true, hasMedia = false))
    }

    @Test
    fun textUsesSmsWhenRcsIsDown() {
        assertEquals(MessageTransport.SMS, MessageTransport.choose(rcsRegistered = false, hasMedia = false))
    }

    @Test
    fun mediaUsesMmsEvenWhenRcsIsUp() {
        assertEquals(MessageTransport.MMS, MessageTransport.choose(rcsRegistered = true, hasMedia = true))
    }

    @Test
    fun aGroupUsesMms() {
        assertEquals(
            MessageTransport.MMS,
            MessageTransport.choose(rcsRegistered = true, hasMedia = false, recipients = 3),
        )
    }

    @Test
    fun mmsPduCarriesThePicture() {
        val picture = byteArrayOf(1, 2, 3, 4)
        val pdu = MmsPdu.sendRequest(listOf("5550101001"), "Hi", picture, "image/jpeg")
        assertEquals(0x8C, pdu[0].toInt() and 0xFF)
        assertEquals(0x80, pdu[1].toInt() and 0xFF)
        val text = pdu.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("image/jpeg"))
        assertTrue(text.contains("5550101001"))
    }

    @Test
    fun groupPduListsEveryRecipient() {
        val pdu = MmsPdu.sendRequest(listOf("5550101001", "5550101002"), "Hi", null, null)
        val text = pdu.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("5550101001"))
        assertTrue(text.contains("5550101002"))
    }
}
