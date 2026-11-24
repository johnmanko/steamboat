package com.johnmanko.app.steamboat.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupsTest {
    private val maya = GroupPerson("5550101001", "(555) 010-1001", "Maya Chen")
    private val jonah = GroupPerson("5550101002", "(555) 010-1002", "Jonah Ellis")

    @Test
    fun keyIgnoresOrder() {
        assertEquals(Groups.key(listOf(jonah, maya)), Groups.key(listOf(maya, jonah)))
        assertEquals("g:5550101001,5550101002", Groups.key(listOf(jonah, maya)))
    }

    @Test
    fun titleUsesFirstNames() {
        assertEquals("Jonah, Maya", Groups.title(listOf(jonah, maya)))
    }
}
