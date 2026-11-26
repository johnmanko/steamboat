package com.johnmanko.app.steamboat.ui.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.data.repo.MessagingRepository
import com.johnmanko.app.steamboat.domain.PhoneNumbers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class ContactsMode { NewMessage, Link, Add, Forward }

class ContactsViewModel(private val repository: MessagingRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val contacts = repository.contacts(query).cachedIn(viewModelScope)
    var selected by mutableStateOf(emptyList<ContactEntity>())
        private set
    var messageNumber by mutableStateOf<String?>(null)
        private set

    fun onQuery(value: String) {
        query.value = value
        messageNumber = PhoneNumbers.newContactNumber(value)
    }

    fun messageTypedNumber(number: String, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            onReady(repository.openConversation(PhoneNumbers.display(number)))
        }
    }

    fun toggle(contact: ContactEntity) {
        selected = if (selected.any { it.matchKey == contact.matchKey }) {
            selected.filterNot { it.matchKey == contact.matchKey }
        } else {
            selected + contact
        }
    }

    fun startSelected(onReady: (Long) -> Unit) {
        val people = selected
        if (people.isEmpty()) return
        viewModelScope.launch {
            val id = if (people.size == 1) {
                repository.openConversation(people.first())
            } else {
                repository.openGroup(people)
            }
            onReady(id)
        }
    }

    fun add(conversationId: Long, contact: ContactEntity, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            onReady(repository.addPerson(conversationId, contact))
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.syncContacts()
            messageNumber = PhoneNumbers.newContactNumber(query.value)
        }
    }

    fun start(contact: ContactEntity, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            onReady(repository.openConversation(contact))
        }
    }

    fun link(conversationId: Long, contact: ContactEntity, onReady: () -> Unit) {
        viewModelScope.launch {
            repository.linkContact(conversationId, contact)
            onReady()
        }
    }
}

@Composable
fun contactsViewModel(repository: MessagingRepository): ContactsViewModel = viewModel(
    factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ContactsViewModel(repository) as T
    },
)
