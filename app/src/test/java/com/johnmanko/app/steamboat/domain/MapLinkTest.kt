package com.johnmanko.app.steamboat.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MapLinkTest {
    @Test
    fun linkUsesADotDecimalAndAMarker() {
        assertEquals(
            "https://www.openstreetmap.org/?mlat=40.748400&mlon=-73.985700#map=16/40.748400/-73.985700",
            mapLink(MapProvider.OPENSTREETMAP, 40.7484, -73.9857),
        )
    }

    @Test
    fun googleLinkUsesTheSearchQuery() {
        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=40.748400,-73.985700",
            mapLink(MapProvider.GOOGLE, 40.7484, -73.9857),
        )
    }

    @Test
    fun insertReplacesTheSelectionAndParksTheCursorAfterIt() {
        val (text, cursor) = insertAtSelection("see you there", 4, 7, "https://www.openstreetmap.org/")
        assertEquals("see https://www.openstreetmap.org/ there", text)
        assertEquals(4 + "https://www.openstreetmap.org/".length, cursor)
    }

    @Test
    fun insertAppendsWhenTheCursorIsPastTheEnd() {
        val (text, cursor) = insertAtSelection("hi", 20, 20, " map")
        assertEquals("hi map", text)
        assertEquals(6, cursor)
    }
}
