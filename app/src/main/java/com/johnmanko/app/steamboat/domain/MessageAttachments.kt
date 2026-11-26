package com.johnmanko.app.steamboat.domain

/**
 * Several images can share one message until their bytes reach the carrier MMS limit.
 * The first image is kept even when it is already over that limit, matching a single attachment.
 */
object MessageAttachments {
    const val DEFAULT_MAX_BYTES = 300 * 1024

    fun encode(parts: List<PendingAttachment>): String =
        parts.joinToString("\n") { "${it.mime}\t${it.path}" }

    fun decode(stored: String?): List<PendingAttachment> =
        stored.orEmpty().lineSequence().mapNotNull { line ->
            val split = line.indexOf('\t')
            if (split <= 0 || split == line.lastIndex) null
            else PendingAttachment(path = line.substring(split + 1), mime = line.substring(0, split))
        }.toList()

    fun fits(usedBytes: Long, nextBytes: Long, maxBytes: Int): Boolean {
        if (nextBytes <= 0L) return false
        if (usedBytes == 0L) return true
        return usedBytes + nextBytes <= maxBytes
    }
}
