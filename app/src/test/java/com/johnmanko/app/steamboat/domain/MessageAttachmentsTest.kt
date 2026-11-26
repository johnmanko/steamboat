package com.johnmanko.app.steamboat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageAttachmentsTest {
    @Test
    fun roundTripKeepsEveryImage() {
        val parts = listOf(
            PendingAttachment("/tmp/one.jpg", "image/jpeg"),
            PendingAttachment("/tmp/two.png", "image/png"),
        )
        assertEquals(parts, MessageAttachments.decode(MessageAttachments.encode(parts)))
    }

    @Test
    fun aThirdImagePastTheLimitCannotBeSelected() {
        val max = 1_000
        assertTrue(MessageAttachments.fits(usedBytes = 0, nextBytes = 400, maxBytes = max))
        assertTrue(MessageAttachments.fits(usedBytes = 400, nextBytes = 400, maxBytes = max))
        assertFalse(MessageAttachments.fits(usedBytes = 800, nextBytes = 400, maxBytes = max))
    }

    @Test
    fun theFirstImageIsKeptAndLaterOnesStopAtTheLimit() {
        val max = 1_000
        assertTrue(MessageAttachments.fits(usedBytes = 0, nextBytes = 2_000, maxBytes = max))
        assertTrue(MessageAttachments.fits(usedBytes = 400, nextBytes = 600, maxBytes = max))
        assertFalse(MessageAttachments.fits(usedBytes = 400, nextBytes = 601, maxBytes = max))
    }
}
