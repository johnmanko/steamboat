package com.johnmanko.app.steamboat.domain

data class GroupPerson(
    val matchKey: String,
    val address: String,
    val title: String,
)

object Groups {
    fun key(people: Collection<GroupPerson>): String {
        val keys = people.map { it.matchKey.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        require(keys.size >= 2) { "A group needs two people" }
        return "g:" + keys.joinToString(",")
    }

    fun title(people: Collection<GroupPerson>): String {
        val names = people.map { it.title.trim() }.filter { it.isNotEmpty() }
        val firsts = names.map { name -> name.substringBefore(' ').ifBlank { name } }
        val labels = if (firsts.distinct().size == firsts.size) firsts else names
        return labels.joinToString(", ")
    }

    fun address(people: Collection<GroupPerson>): String =
        people.map { it.address.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
}
