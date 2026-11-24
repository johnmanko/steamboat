package com.johnmanko.app.steamboat.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class OpenRequestBus {
    private val drafts = ConcurrentHashMap<Long, String>()
    private val _conversationId = MutableStateFlow<Long?>(null)
    val conversationId: StateFlow<Long?> = _conversationId.asStateFlow()

    fun open(id: Long, draft: String?) {
        if (!draft.isNullOrBlank()) drafts[id] = draft
        _conversationId.value = null
        _conversationId.value = id
    }

    fun consume() {
        _conversationId.value = null
    }

    fun takeDraft(id: Long): String? = drafts.remove(id)
}
