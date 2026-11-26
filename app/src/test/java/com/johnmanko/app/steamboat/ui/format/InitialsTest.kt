package com.johnmanko.app.steamboat.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun aPhoneNumberUsesItsLastTwoDigits() {
        assertEquals("88", initials("(555) 010-1888"))
        assertEquals("88", initials("5550101888"))
    }

    @Test
    fun aNameUsesItsInitials() {
        assertEquals("MC", initials("Maya Chen"))
    }
}
