package com.johnmanko.app.steamboat.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickReplyRulesTest {
    @Test
    fun phrasesStayShortAndUnique() {
        val saved = QuickReplyRules.sanitize(
            listOf("  On my way  ", "OK", "OK", "This phrase is far too long", "   ", "Thanks!"),
        )
        assertEquals(listOf("On my way", "OK", "This phrase is ", "Thanks!"), saved)
    }

    @Test
    fun manualReplyStopsBefore144() {
        val typed = "a".repeat(200)
        assertEquals(143, QuickReplyRules.manual(typed).length)
    }
}
