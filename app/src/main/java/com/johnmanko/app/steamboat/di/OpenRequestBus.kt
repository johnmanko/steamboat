package com.johnmanko.app.steamboat.di

import com.johnmanko.app.steamboat.domain.PendingAttachment
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class ComposerDraft(
    val text: String = "",
    val media: List<PendingAttachment> = emptyList(),
)

class OpenRequestBus {
    private val drafts = ConcurrentHashMap<Long, ComposerDraft>()
    private val _conversationId = MutableStateFlow<Long?>(null)
    val conversationId: StateFlow<Long?> = _conversationId.asStateFlow()
    private val _draftReady = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val draftReady: SharedFlow<Long> = _draftReady.asSharedFlow()
    private var forward: ComposerDraft? = null

    fun open(id: Long, draft: String?) {
        if (!draft.isNullOrBlank()) drafts[id] = ComposerDraft(text = draft)
        _conversationId.value = null
        _conversationId.value = id
    }

    fun stageForward(text: String, media: List<PendingAttachment>) {
        forward = ComposerDraft(text = text, media = media)
    }

    fun takeForward(): ComposerDraft {
        val current = forward ?: ComposerDraft()
        forward = null
        return current
    }

    /** Puts a forwarded message on a conversation that is about to open. */
    fun stageDraft(id: Long, draft: ComposerDraft) {
        if (draft.text.isBlank() && draft.media.isEmpty()) return
        drafts[id] = draft
        _draftReady.tryEmit(id)
    }

    fun consume() {
        _conversationId.value = null
    }

    fun takeDraft(id: Long): ComposerDraft? = drafts.remove(id)
}
