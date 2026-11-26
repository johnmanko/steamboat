package com.johnmanko.app.steamboat.data.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.paging.Pager
import androidx.paging.PagingData
import com.johnmanko.app.steamboat.BuildConfig
import com.johnmanko.app.steamboat.data.contacts.ContactSync
import com.johnmanko.app.steamboat.data.prefs.QuickReplies
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.data.sample.SampleInbox
import com.johnmanko.app.steamboat.data.db.ConversationEntity
import com.johnmanko.app.steamboat.data.db.MessageEntity
import com.johnmanko.app.steamboat.data.sms.AttachmentStore
import com.johnmanko.app.steamboat.data.sms.MessageTransport
import com.johnmanko.app.steamboat.data.sms.MmsArchive
import com.johnmanko.app.steamboat.data.sms.MmsBudget
import com.johnmanko.app.steamboat.data.sms.MmsSender
import com.johnmanko.app.steamboat.data.sms.RcsAvailability
import com.johnmanko.app.steamboat.data.sms.SmsImporter
import com.johnmanko.app.steamboat.data.sms.SmsSender
import com.johnmanko.app.steamboat.domain.GroupPerson
import com.johnmanko.app.steamboat.domain.MessageAttachments
import com.johnmanko.app.steamboat.domain.MessageStatus
import com.johnmanko.app.steamboat.domain.PendingAttachment
import com.johnmanko.app.steamboat.domain.PhoneNumbers
import com.johnmanko.app.steamboat.notify.ActiveConversation
import com.johnmanko.app.steamboat.notify.NotificationCenter
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.johnmanko.app.steamboat.data.db.MessageSearchHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MessagingRepository(
    private val context: Context,
    private val store: MessageStore,
    private val contactSync: ContactSync,
    private val smsImporter: SmsImporter,
    private val smsSender: SmsSender,
    private val mmsSender: MmsSender,
    private val attachments: AttachmentStore,
    private val rcs: RcsAvailability,
    private val notifications: NotificationCenter,
    private val quickReplies: QuickReplies,
    private val sampleInbox: SampleInbox,
) {
    private val contactMutex = Mutex()
    private val importMutex = Mutex()
    private val mmsArchive = MmsArchive(context)
    private val mmsBudget = MmsBudget(context)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun conversations(query: Flow<String>): Flow<PagingData<ConversationEntity>> =
        query.flatMapLatest { raw ->
            Pager(PagingConfigs.lists) {
                val pattern = PhoneNumbers.likePattern(raw)
                if (pattern.isEmpty()) store.conversationPaging() else store.searchConversations(pattern)
            }.flow
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun contacts(query: Flow<String>): Flow<PagingData<ContactEntity>> =
        query.flatMapLatest { raw ->
            Pager(PagingConfigs.lists) {
                val pattern = PhoneNumbers.likePattern(raw)
                if (pattern.isEmpty()) {
                    store.contactPaging()
                } else {
                    store.searchContacts(pattern, PhoneNumbers.digits(raw))
                }
            }.flow
        }

    fun messages(conversationId: Long): Flow<PagingData<MessageEntity>> =
        Pager(PagingConfigs.messages) { store.messagePaging(conversationId) }.flow

    @OptIn(ExperimentalCoroutinesApi::class)
    fun messageHits(query: Flow<String>): Flow<PagingData<MessageSearchHit>> =
        query.flatMapLatest { raw ->
            val pattern = PhoneNumbers.likePattern(raw)
            if (pattern.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                Pager(PagingConfigs.lists) { store.searchMessages(pattern) }.flow
            }
        }

    fun observeConversation(id: Long) = store.observeConversation(id)

    fun observeMembers(id: Long) = store.observeMembers(id)

    suspend fun installSampleInbox(): Boolean = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) return@withContext false
        sampleInbox.install()
    }

    suspend fun syncContacts() = withContext(Dispatchers.IO) {
        contactMutex.withLock {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) return@withLock
            store.replaceContacts(contactSync.read())
        }
    }

    suspend fun importSms() = withContext(Dispatchers.IO) {
        importMutex.withLock {
            if (!smsImporter.canRead()) return@withLock
            smsImporter.readAll { page -> store.importMessages(page) }
            store.refreshAllUnread()
            store.relinkContacts()
        }
    }

    suspend fun openConversation(contact: ContactEntity): Long = withContext(Dispatchers.IO) {
        store.openConversation(
            address = contact.phoneNumber,
            title = contact.displayName,
            photoUri = contact.photoUri,
            lookupKey = contact.lookupKey,
            manual = true,
        )
    }

    suspend fun openGroup(contacts: List<ContactEntity>): Long = withContext(Dispatchers.IO) {
        store.openGroup(contacts.map { it.toPerson() })
    }

    suspend fun addPerson(conversationId: Long, contact: ContactEntity): Long = withContext(Dispatchers.IO) {
        store.addPerson(conversationId, contact.toPerson())
    }

    suspend fun removePerson(conversationId: Long, matchKey: String) = withContext(Dispatchers.IO) {
        store.removePerson(conversationId, matchKey)
    }

    suspend fun addUnlistedNumber(raw: String): Long = withContext(Dispatchers.IO) {
        val display = PhoneNumbers.display(raw)
        contactSync.insert(display)
        syncContacts()
        val saved = store.contactByMatchKey(PhoneNumbers.matchKey(raw))
        if (saved != null) {
            store.openConversation(
                address = saved.phoneNumber,
                title = saved.displayName,
                photoUri = saved.photoUri,
                lookupKey = saved.lookupKey,
                manual = true,
            )
        } else {
            store.openConversation(
                address = display,
                title = display,
                photoUri = null,
                lookupKey = null,
                manual = false,
            )
        }
    }

    suspend fun openConversation(address: String): Long = withContext(Dispatchers.IO) {
        store.openConversation(
            address = address,
            title = address,
            photoUri = null,
            lookupKey = null,
            manual = false,
        )
    }

    suspend fun linkContact(conversationId: Long, contact: ContactEntity) = withContext(Dispatchers.IO) {
        store.linkManually(
            conversationId = conversationId,
            lookupKey = contact.lookupKey,
            title = contact.displayName,
            photoUri = contact.photoUri,
        )
    }

    suspend fun unlinkContact(conversationId: Long) = withContext(Dispatchers.IO) {
        store.unlink(conversationId)
    }

    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        store.deleteConversation(conversationId)
        notifications.cancel(conversationId)
        notifications.forgetThread(conversationId)
    }

    suspend fun markRead(conversationId: Long) = withContext(Dispatchers.IO) {
        store.markRead(conversationId)
        notifications.cancel(conversationId)
    }

    suspend fun stageAttachment(uri: Uri): PendingAttachment = withContext(Dispatchers.IO) {
        attachments.stage(uri)
    }

    suspend fun addAttachments(existing: List<PendingAttachment>, uris: List<Uri>): AttachmentBatch =
        withContext(Dispatchers.IO) {
            val max = mmsBudget.maxBytes()
            val kept = existing.toMutableList()
            var used = existing.sumOf { File(it.path).length() }
            var skipped = 0
            uris.forEach { uri ->
                val staged = runCatching { attachments.stage(uri) }.getOrNull()
                if (staged == null) {
                    skipped += 1
                    return@forEach
                }
                val size = File(staged.path).length()
                if (MessageAttachments.fits(used, size, max)) {
                    kept += staged
                    used += size
                } else {
                    attachments.discard(staged)
                    skipped += 1
                }
            }
            AttachmentBatch(kept, skipped)
        }

    suspend fun discardAttachment(attachment: PendingAttachment) = withContext(Dispatchers.IO) {
        attachments.discard(attachment)
    }

    suspend fun send(
        conversationId: Long,
        rawBody: String,
        media: List<PendingAttachment> = emptyList(),
    ) = withContext(Dispatchers.IO) {
        val body = rawBody.trim()
        if ((body.isEmpty() && media.isEmpty()) || body.length > MAX_BODY) return@withContext
        val conversation = store.getConversation(conversationId) ?: return@withContext
        val sentAt = System.currentTimeMillis()
        val first = media.firstOrNull()
        val messageId = store.addMessage(
            message = MessageEntity(
                conversationId = conversationId,
                body = body,
                sentAt = sentAt,
                outgoing = true,
                status = MessageStatus.SENDING,
                seen = true,
                providerKey = null,
                mediaPath = first?.path,
                mediaMime = first?.mime,
                attachments = MessageAttachments.encode(media).ifBlank { null },
            ),
            countUnread = false,
        )
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            store.updateStatus(messageId, MessageStatus.FAILED)
            return@withContext
        }
        dispatch(messageId, destinations(conversation), body, media)
    }

    suspend fun retry(messageId: Long) = withContext(Dispatchers.IO) {
        val message = store.getMessage(messageId) ?: return@withContext
        if (!message.outgoing || message.status != MessageStatus.FAILED) return@withContext
        val conversation = store.getConversation(message.conversationId) ?: return@withContext
        store.updateStatus(messageId, MessageStatus.SENDING)
        dispatch(messageId, destinations(conversation), message.body, message.mediaParts())
    }

    private suspend fun destinations(conversation: ConversationEntity): List<String> {
        val members = store.memberAddresses(conversation.id)
        return members.ifEmpty { listOf(conversation.address) }
    }

    private suspend fun dispatch(
        messageId: Long,
        addresses: List<String>,
        body: String,
        media: List<PendingAttachment>,
    ) {
        if (addresses.isEmpty()) {
            store.updateStatus(messageId, MessageStatus.FAILED)
            return
        }
        val registered = rcs.isRegistered()
        val transport = MessageTransport.choose(registered, media.isNotEmpty(), addresses.size)
        try {
            when (transport) {
                MessageTransport.MMS -> mmsSender.send(
                    messageId,
                    addresses,
                    body,
                    media,
                )
                MessageTransport.RCS -> {
                    Log.i(TAG, "RCS is registered; no app send API, falling back to SMS")
                    smsSender.send(messageId, addresses.first(), body)
                }
                else -> smsSender.send(messageId, addresses.first(), body)
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "Message send blocked", error)
            store.updateStatus(messageId, MessageStatus.FAILED)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Message send rejected", error)
            store.updateStatus(messageId, MessageStatus.FAILED)
        } catch (error: IOException) {
            Log.w(TAG, "Message send could not read the attachment", error)
            store.updateStatus(messageId, MessageStatus.FAILED)
        }
    }

    suspend fun markDelivery(messageId: Long, sent: Boolean) = withContext(Dispatchers.IO) {
        val message = store.getMessage(messageId) ?: return@withContext
        store.updateStatus(messageId, if (sent) MessageStatus.SENT else MessageStatus.FAILED)
        if (sent && message.providerKey == null) {
            val conversation = store.getConversation(message.conversationId) ?: return@withContext
            val grouped = store.memberAddresses(conversation.id).size > 1
            if (!grouped) {
                val key = smsSender.archiveSent(conversation.address, message.body, message.sentAt)
                if (key != null) store.attachProviderKey(messageId, key)
            }
        }
    }

    suspend fun acceptDeliveredSms(address: String, body: String, sentAt: Long) {
        val providerKey = withContext(Dispatchers.IO) {
            smsSender.archiveIncoming(address, body, sentAt)
        }
        ingestIncoming(address, body, sentAt, providerKey)
    }

    suspend fun acceptDeliveredMms(
        address: String,
        body: String,
        sentAt: Long,
        media: ByteArray?,
        mediaMime: String?,
        transactionId: String?,
    ) = withContext(Dispatchers.IO) {
        if (body.isBlank() && (media == null || media.isEmpty())) return@withContext
        val stable = transactionId?.takeIf { it.isNotBlank() }?.let { "mms:$it" }
        if (stable != null && store.hasProviderKey(stable)) return@withContext
        val providerKey = stable ?: mmsArchive.insertIncoming(address, body, sentAt, media, mediaMime)
        val mediaPath = if (media != null && media.isNotEmpty()) attachments.save(media, mediaMime) else null
        ingestIncoming(address, body, sentAt, providerKey, mediaPath, mediaMime)
    }

    suspend fun ingestIncoming(
        address: String,
        body: String,
        sentAt: Long,
        providerKey: String?,
        mediaPath: String? = null,
        mediaMime: String? = null,
    ) =
        withContext(Dispatchers.IO) {
            if (PhoneNumbers.matchKey(address).isEmpty()) return@withContext
            val conversationId = store.openConversation(
                address = address,
                title = address,
                photoUri = null,
                lookupKey = null,
                manual = false,
            )
            val active = ActiveConversation.id.value == conversationId
            val multimedia = mediaPath != null || providerKey?.startsWith("mms:") == true
            if (multimedia) {
                if (!providerKey.isNullOrBlank() && store.hasProviderKey(providerKey)) return@withContext
                store.addMessage(
                    message = MessageEntity(
                        conversationId = conversationId,
                        body = body,
                        sentAt = sentAt,
                        outgoing = false,
                        status = MessageStatus.RECEIVED,
                        seen = active,
                        providerKey = providerKey,
                        mediaPath = mediaPath,
                        mediaMime = mediaMime,
                    ),
                    countUnread = true,
                )
            } else if (!providerKey.isNullOrBlank()) {
                store.importMessages(
                    listOf(
                        ImportedSms(
                            providerKey = providerKey,
                            address = address,
                            body = body,
                            sentAt = sentAt,
                            outgoing = false,
                            seen = active,
                            status = MessageStatus.RECEIVED,
                        ),
                    ),
                )
            } else {
                store.addMessage(
                    message = MessageEntity(
                        conversationId = conversationId,
                        body = body,
                        sentAt = sentAt,
                        outgoing = false,
                        status = MessageStatus.RECEIVED,
                        seen = active,
                        providerKey = null,
                    ),
                    countUnread = true,
                )
            }
            store.refreshAllUnread()
            store.relinkContacts()
            val conversation = store.getConversation(conversationId) ?: return@withContext
            if (ActiveConversation.id.value == conversationId) {
                notifications.cancel(conversationId)
            } else {
                notifications.showIncoming(
                    conversation = conversation,
                    recent = store.latestMessages(conversationId, RECENT_FOR_NOTIFICATION),
                    quickReplies = quickReplies.current(),
                )
            }
        }

    private fun ContactEntity.toPerson() = GroupPerson(
        matchKey = matchKey,
        address = phoneNumber,
        title = displayName.ifBlank { phoneNumber },
    )

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    data class AttachmentBatch(val attachments: List<PendingAttachment>, val skipped: Int)

    private companion object {
        const val TAG = "Steamboat"
        const val MAX_BODY = 5_000
        const val RECENT_FOR_NOTIFICATION = 8
    }
}
