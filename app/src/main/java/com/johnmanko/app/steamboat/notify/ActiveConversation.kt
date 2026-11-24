package com.johnmanko.app.steamboat.notify

import kotlinx.coroutines.flow.MutableStateFlow

/** The open thread, so a new text does not raise a notification over a chat the user is reading. */
object ActiveConversation {
    val id = MutableStateFlow<Long?>(null)
}
