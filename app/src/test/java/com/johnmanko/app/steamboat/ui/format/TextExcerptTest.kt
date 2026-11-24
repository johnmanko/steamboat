package com.johnmanko.app.steamboat.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextExcerptTest {
    @Test
    fun keepsAShortMessageAndMarksEveryMatch() {
        val excerpt = textExcerpt("Just parked outside.", "parked")
        assertEquals("Just parked outside.", excerpt.text)
        assertEquals(listOf(5 until 11), excerpt.highlights)
    }

    @Test
    fun matchIgnoresCase() {
        val excerpt = textExcerpt("Just Parked outside.", "parked")
        assertEquals(listOf(5 until 11), excerpt.highlights)
        assertEquals("Parked", excerpt.text.substring(5, 11))
    }

    @Test
    fun windowCentersOnTheFirstMatch() {
        val body = "a".repeat(80) + "parked" + "b".repeat(80)
        val excerpt = textExcerpt(body, "parked", radius = 4)
        assertTrue(excerpt.text.startsWith("…"))
        assertTrue(excerpt.text.endsWith("…"))
        assertEquals("parked", excerpt.text.substring(excerpt.highlights.single()))
    }
}
