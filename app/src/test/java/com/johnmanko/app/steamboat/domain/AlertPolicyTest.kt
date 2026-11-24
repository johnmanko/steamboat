package com.johnmanko.app.steamboat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPolicyTest {
    @Test
    fun daytimeWindowIncludesTheStartAndStopsAtTheEnd() {
        assertTrue(minuteIsActive(8 * 60, 22 * 60, 8 * 60))
        assertTrue(minuteIsActive(8 * 60, 22 * 60, 21 * 60 + 59))
        assertFalse(minuteIsActive(8 * 60, 22 * 60, 22 * 60))
        assertFalse(minuteIsActive(8 * 60, 22 * 60, 7 * 60))
    }

    @Test
    fun overnightWindowWrapsPastMidnight() {
        assertTrue(minuteIsActive(22 * 60, 7 * 60, 23 * 60))
        assertTrue(minuteIsActive(22 * 60, 7 * 60, 6 * 60))
        assertFalse(minuteIsActive(22 * 60, 7 * 60, 12 * 60))
    }

    @Test
    fun equalEndsStayActiveAllDay() {
        assertTrue(minuteIsActive(9 * 60, 9 * 60, 0))
        assertTrue(minuteIsActive(9 * 60, 9 * 60, 15 * 60))
    }

    @Test
    fun silentAndTheClosedHoursStayQuiet() {
        val policy = AlertPolicy(scheduled = true, startMinute = 8 * 60, endMinute = 22 * 60, style = AlertStyle.BOTH)
        assertEquals(AlertDelivery.QUIET, alertDelivery(policy.copy(silent = true), 12 * 60))
        assertEquals(AlertDelivery.QUIET, alertDelivery(policy, 23 * 60))
        assertEquals(AlertDelivery.BOTH, alertDelivery(policy, 12 * 60))
    }

    @Test
    fun policyRoundTripsIncludingTheSound() {
        val policy = AlertPolicy(
            silent = false,
            scheduled = true,
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            style = AlertStyle.SOUND,
            soundUri = "content://media/internal/audio/media/12|x",
        )
        assertEquals(policy, decodeAlertPolicy(encodeAlertPolicy(policy)))
    }

    @Test
    fun aBrokenPolicyIsDropped() {
        assertNull(decodeAlertPolicy("nope"))
        assertNull(decodeAlertPolicy("1|0|8|22|NOISE|"))
    }
}
