package com.johnmanko.app.steamboat.data.repo

import androidx.room3.withWriteTransaction
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.data.db.ConversationEntity
import com.johnmanko.app.steamboat.data.db.ConversationMemberEntity
import com.johnmanko.app.steamboat.data.db.MessageEntity
import com.johnmanko.app.steamboat.data.db.SteamboatDatabase
import com.johnmanko.app.steamboat.domain.GroupPerson
import com.johnmanko.app.steamboat.domain.Groups
import com.johnmanko.app.steamboat.domain.PhoneNumbers

data class ImportedSms(
    val providerKey: String,
    val address: String,
    val body: String,
    val sentAt: Long,
    val outgoing: Boolean,
    val seen: Boolean,
    val status: String,
    val mediaPath: String? = null,
    val mediaMime: String? = null,
)

/**
 * All message and thread writes go through here so snippet, unread, and contact
 * columns stay consistent. Reads that feed the UI are paged.
 */
class MessageStore(private val database: SteamboatDatabase) {
    private val contacts get() = database.contacts()
    private val conversations get() = database.conversations()
    private val members get() = database.members()
    private val messages get() = database.messages()

    fun observeConversation(id: Long) = conversations.observe(id)

    fun observeMembers(conversationId: Long) = members.observe(conversationId)

    suspend fun memberAddresses(conversationId: Long): List<String> =
        members.list(conversationId).map { it.address }

    fun conversationPaging() = conversations.paging()

    fun searchConversations(query: String) = conversations.search(query)

    fun contactPaging() = contacts.paging()

    fun searchContacts(query: String, digits: String) = contacts.search(query, digits)

    suspend fun contactByMatchKey(matchKey: String) = contacts.findByMatchKey(matchKey)

    fun messagePaging(conversationId: Long) = messages.paging(conversationId)

    fun searchMessages(query: String) = messages.search(query)

    suspend fun getConversation(id: Long) = conversations.get(id)

    suspend fun findByMatchKey(matchKey: String) = conversations.findByMatchKey(matchKey)

    suspend fun getMessage(id: Long) = messages.get(id)

    suspend fun latestMessages(conversationId: Long, limit: Int) = messages.latest(conversationId, limit)

    suspend fun replaceContacts(rows: List<ContactEntity>) {
        database.withWriteTransaction {
            contacts.replaceAll(rows)
            conversations.linkMatchingContacts()
        }
    }

    suspend fun upsertContacts(rows: List<ContactEntity>) {
        if (rows.isEmpty()) return
        database.withWriteTransaction {
            contacts.upsertAll(rows)
            conversations.linkMatchingContacts()
        }
    }

    suspend fun relinkContacts() {
        conversations.linkMatchingContacts()
    }

    suspend fun linkManually(conversationId: Long, lookupKey: String, title: String, photoUri: String?) {
        conversations.linkManually(conversationId, lookupKey, title, photoUri)
    }

    suspend fun unlink(conversationId: Long) {
        conversations.unlink(conversationId)
    }

    suspend fun deleteConversation(id: Long) {
        conversations.delete(id)
    }

    suspend fun markRead(conversationId: Long) {
        database.withWriteTransaction {
            messages.markSeen(conversationId)
            conversations.clearUnread(conversationId)
        }
    }

    suspend fun updateStatus(messageId: Long, status: String) {
        messages.updateStatus(messageId, status)
    }

    suspend fun attachProviderKey(messageId: Long, providerKey: String) {
        messages.updateProviderKey(messageId, providerKey)
    }

    suspend fun refreshAllUnread() {
        conversations.refreshAllUnread()
    }

    suspend fun openConversation(
        address: String,
        title: String,
        photoUri: String?,
        lookupKey: String?,
        manual: Boolean,
    ): Long {
        val key = PhoneNumbers.matchKey(address)
        require(key.isNotEmpty())
        val existing = conversations.findByMatchKey(key)
        if (existing != null) {
            if (manual && lookupKey != null) {
                conversations.linkManually(existing.id, lookupKey, title, photoUri)
            }
            return existing.id
        }
        return conversations.insert(
            ConversationEntity(
                address = address,
                matchKey = key,
                title = if (manual && title.isNotBlank()) title else formatAddress(address),
                photoUri = if (manual) photoUri else null,
                contactLookupKey = if (manual) lookupKey else null,
                contactLinkedManually = manual && lookupKey != null,
                snippet = "",
                updatedAt = 0,
                unreadCount = 0,
            ),
        )
    }

    suspend fun openGroup(people: List<GroupPerson>): Long = database.withWriteTransaction {
        insertGroup(people)
    }

    private suspend fun insertGroup(people: List<GroupPerson>): Long {
        val distinct = people.distinctBy { it.matchKey }
        val key = Groups.key(distinct)
        conversations.findByMatchKey(key)?.let { return it.id }
        val id = conversations.insert(
            ConversationEntity(
                address = Groups.address(distinct),
                matchKey = key,
                title = Groups.title(distinct),
                photoUri = null,
                contactLookupKey = null,
                contactLinkedManually = false,
                snippet = "",
                updatedAt = 0,
                unreadCount = 0,
            ),
        )
        members.insertAll(distinct.map { it.toMember(id) })
        return id
    }

    suspend fun addPerson(conversationId: Long, person: GroupPerson): Long = database.withWriteTransaction {
        val conversation = conversations.get(conversationId) ?: return@withWriteTransaction conversationId
        val current = members.list(conversationId)
        val base = if (current.size >= 2) {
            current.map { GroupPerson(it.matchKey, it.address, it.title) }
        } else {
            listOf(GroupPerson(conversation.matchKey, conversation.address, conversation.title))
        }
        val next = (base + person).distinctBy { it.matchKey }
        if (next.size < 2) return@withWriteTransaction conversationId
        val key = Groups.key(next)
        conversations.findByMatchKey(key)?.let { existing ->
            return@withWriteTransaction existing.id
        }
        if (current.size >= 2) {
            members.deleteForConversation(conversationId)
            members.insertAll(next.map { it.toMember(conversationId) })
            conversations.updateGroup(
                id = conversationId,
                address = Groups.address(next),
                matchKey = key,
                title = Groups.title(next),
            )
            conversationId
        } else {
            insertGroup(next)
        }
    }

    suspend fun removePerson(conversationId: Long, matchKey: String) = database.withWriteTransaction {
        val current = members.list(conversationId)
        if (current.size <= 2) return@withWriteTransaction
        val next = current.filterNot { it.matchKey == matchKey }.map {
            GroupPerson(it.matchKey, it.address, it.title)
        }
        val key = Groups.key(next)
        val other = conversations.findByMatchKey(key)
        if (other != null && other.id != conversationId) return@withWriteTransaction
        members.deleteForConversation(conversationId)
        members.insertAll(next.map { it.toMember(conversationId) })
        conversations.updateGroup(
            id = conversationId,
            address = Groups.address(next),
            matchKey = key,
            title = Groups.title(next),
        )
    }

    suspend fun addMessage(message: MessageEntity, countUnread: Boolean): Long = database.withWriteTransaction {
        val id = messages.insert(message)
        conversations.updateSnippetIfNewer(message.conversationId, message.listSnippet(), message.sentAt)
        if (countUnread) conversations.refreshUnread(message.conversationId)
        id
    }

    suspend fun importMessages(rows: List<ImportedSms>) {
        if (rows.isEmpty()) return
        database.withWriteTransaction {
            val cache = HashMap<String, ConversationEntity>()
            for (row in rows) {
                if (messages.findByProviderKey(row.providerKey) != null) continue
                val conversation = ensureConversation(cache, row.address, row.sentAt, row.body)
                if (row.outgoing) {
                    val duplicate = messages.findOutgoingDuplicate(
                        conversationId = conversation.id,
                        body = row.body,
                        start = row.sentAt - DUPLICATE_WINDOW_MS,
                        end = row.sentAt + DUPLICATE_WINDOW_MS,
                    )
                    if (duplicate != null) {
                        messages.updateProviderKey(duplicate, row.providerKey)
                        continue
                    }
                }
                messages.insert(
                    MessageEntity(
                        conversationId = conversation.id,
                        body = row.body,
                        sentAt = row.sentAt,
                        outgoing = row.outgoing,
                        status = row.status,
                        seen = row.seen,
                        providerKey = row.providerKey,
                        mediaPath = row.mediaPath,
                        mediaMime = row.mediaMime,
                    ),
                )
                val snippet = if (row.body.isNotBlank()) {
                    row.body
                } else if (row.mediaMime?.startsWith("video/") == true) {
                    "Video"
                } else if (row.mediaPath != null) {
                    "Photo"
                } else {
                    ""
                }
                conversations.updateSnippetIfNewer(conversation.id, snippet, row.sentAt)
            }
        }
    }

    private suspend fun ensureConversation(
        cache: MutableMap<String, ConversationEntity>,
        address: String,
        sentAt: Long,
        snippet: String,
    ): ConversationEntity {
        val key = PhoneNumbers.matchKey(address)
        cache[key]?.let { return it }
        conversations.findByMatchKey(key)?.let { existing ->
            cache[key] = existing
            return existing
        }
        val id = conversations.insert(
            ConversationEntity(
                address = address,
                matchKey = key,
                title = formatAddress(address),
                photoUri = null,
                contactLookupKey = null,
                contactLinkedManually = false,
                snippet = snippet,
                updatedAt = sentAt,
                unreadCount = 0,
            ),
        )
        val created = conversations.get(id) ?: error("Conversation $id was not stored")
        cache[key] = created
        return created
    }

    private fun GroupPerson.toMember(conversationId: Long) = ConversationMemberEntity(
        conversationId = conversationId,
        matchKey = matchKey,
        address = address,
        title = title,
    )

    private fun formatAddress(address: String): String = PhoneNumbers.display(address)

    private companion object {
        const val DUPLICATE_WINDOW_MS = 15_000L
    }
}
