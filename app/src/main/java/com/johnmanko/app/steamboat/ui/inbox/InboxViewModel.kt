package com.johnmanko.app.steamboat.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.paging.cachedIn
import com.johnmanko.app.steamboat.data.repo.MessagingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InboxViewModel(private val repository: MessagingRepository) : ViewModel() {
    val query = MutableStateFlow("")
    private val _searchOpen = MutableStateFlow(false)
    val searchOpen = _searchOpen.asStateFlow()
    val conversations = repository.conversations(query).cachedIn(viewModelScope)
    val hits = repository.messageHits(query).cachedIn(viewModelScope)
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    fun onQuery(value: String) {
        query.value = value
    }

    fun openSearch() {
        _searchOpen.value = true
    }

    fun closeSearch() {
        _searchOpen.value = false
        query.value = ""
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            try {
                repository.syncContacts()
                repository.importSms()
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun deleteConversations(ids: Collection<Long>) {
        viewModelScope.launch {
            ids.forEach { repository.deleteConversation(it) }
        }
    }
}

@Composable
fun inboxViewModel(repository: MessagingRepository): InboxViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = InboxViewModel(repository) as T
    },
)
