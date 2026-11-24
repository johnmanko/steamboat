package com.johnmanko.app.steamboat.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageLinksTest {
    @Test
    fun findsHttpLinksAndLeavesTheRest() {
        val body = "Look: https://www.pexels.com/search/free%20wallpaper/ today"
        val links = textLinks(body)
        assertEquals(1, links.size)
        assertEquals("https://www.pexels.com/search/free%20wallpaper/", links[0].url)
        assertEquals(body.indexOf("https"), links[0].start)
    }

    @Test
    fun addsASchemeToWwwAndDropsTrailingPunctuation() {
        val links = textLinks("See www.example.com.")
        assertEquals(listOf("https://www.example.com"), links.map { it.url })
        assertEquals("www.example.com".length, links[0].endExclusive - links[0].start)
    }

    @Test
    fun keepsAClosingParenThatBelongsToTheUrl() {
        val url = "https://en.wikipedia.org/wiki/SMS_(disambiguation)"
        val links = textLinks("($url).")
        assertEquals(url, links.single().url)
    }

    @Test
    fun plainTextHasNoLinks() {
        assertEquals(emptyList<TextLink>(), textLinks("On my way"))
    }
}
