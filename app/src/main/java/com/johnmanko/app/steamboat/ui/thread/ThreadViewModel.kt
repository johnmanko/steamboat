package com.johnmanko.app.steamboat.ui.thread

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import com.johnmanko.app.steamboat.data.repo.MessagingRepository
import com.johnmanko.app.steamboat.di.OpenRequestBus
import com.johnmanko.app.steamboat.domain.PendingAttachment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreadViewModel(
    private val conversationId: Long,
    private val repository: MessagingRepository,
    private val navigator: OpenRequestBus,
) : ViewModel() {
    val conversation = repository.observeConversation(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val members = repository.observeMembers(conversationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val messages = repository.messages(conversationId).cachedIn(viewModelScope)
    var composer by mutableStateOf("")
        private set
    var attachments by mutableStateOf<List<PendingAttachment>>(emptyList())
        private set
    var imagesLeftOff by mutableStateOf(0)
        private set

    init {
        navigator.takeDraft(conversationId)?.let { applyDraft(it) }
        viewModelScope.launch {
            navigator.conversationId.collect { requested ->
                if (requested == conversationId) {
                    navigator.takeDraft(conversationId)?.let { applyDraft(it) }
                }
            }
        }
        viewModelScope.launch { repository.markRead(conversationId) }
    }

    fun onComposerChange(value: String) {
        composer = value
    }

    fun useReply(phrase: String) {
        composer = phrase
    }

    fun applyDraft(text: String) {
        if (composer.isBlank()) composer = text
    }

    fun clearImagesLeftOff() {
        imagesLeftOff = 0
    }

    fun attach(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val batch = repository.addAttachments(attachments, uris)
            attachments = batch.attachments
            if (batch.skipped > 0) imagesLeftOff += 1
        }
    }

    fun removeAttachment(path: String) {
        val current = attachments.find { it.path == path } ?: return
        attachments = attachments.filterNot { it.path == path }
        viewModelScope.launch { repository.discardAttachment(current) }
    }

    fun send() {
        val body = composer
        val pending = attachments
        if (body.isBlank() && pending.isEmpty()) return
        composer = ""
        attachments = emptyList()
        viewModelScope.launch { repository.send(conversationId, body, pending) }
    }

    fun retry(messageId: Long) {
        viewModelScope.launch { repository.retry(messageId) }
    }

    fun unlink() {
        viewModelScope.launch { repository.unlinkContact(conversationId) }
    }

    fun removePerson(matchKey: String) {
        viewModelScope.launch { repository.removePerson(conversationId, matchKey) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            onDeleted()
        }
    }
}

@Composable
fun threadViewModel(
    conversationId: Long,
    repository: MessagingRepository,
    navigator: OpenRequestBus,
): ThreadViewModel = viewModel(
    key = "thread-$conversationId",
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ThreadViewModel(conversationId, repository, navigator) as T
    },
)
