package com.johnmanko.app.steamboat.domain

data class PendingAttachment(
    val path: String,
    val mime: String,
) {
    val isVideo: Boolean get() = mime.startsWith("video/")
}
