package com.johnmanko.app.steamboat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun matchKeyStripsFormattingAndCountryCode() {
        assertEquals("4155550100", PhoneNumbers.matchKey("+1 (415) 555-0100"))
        assertEquals("4155550100", PhoneNumbers.matchKey("415.555.0100"))
    }

    @Test
    fun shortCodesStayIntact() {
        assertEquals("45678", PhoneNumbers.matchKey("45678"))
    }

    @Test
    fun vanityNumbersUseThePhoneKeypad() {
        assertEquals("8003569377", PhoneNumbers.matchKey("1-800-FLOWERS"))
    }

    @Test
    fun sameNumberMatchesAcrossFormats() {
        assertTrue(PhoneNumbers.same("+1 415-555-0100", "(415) 555-0100"))
        assertFalse(PhoneNumbers.same("4155550100", "4155550199"))
        assertFalse(PhoneNumbers.same("", "4155550100"))
    }

    @Test
    fun likePatternEscapesWildcards() {
        assertEquals("100\\%\\_off", PhoneNumbers.likePattern("100%_off"))
    }
}
