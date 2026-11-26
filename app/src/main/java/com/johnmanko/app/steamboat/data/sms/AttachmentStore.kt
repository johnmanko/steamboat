package com.johnmanko.app.steamboat.data.sms

import android.content.Context
import android.net.Uri
import com.johnmanko.app.steamboat.domain.PendingAttachment
import java.io.File
import java.util.UUID

class AttachmentStore(private val context: Context) {
    fun stage(uri: Uri): PendingAttachment {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.${extension(mime)}")
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Could not read the selected file" }
            file.outputStream().use { input.copyTo(it) }
        }
        return PendingAttachment(path = file.absolutePath, mime = mime)
    }

    fun save(bytes: ByteArray, mime: String?): String {
        val type = mime ?: "image/jpeg"
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.${extension(type)}")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun discard(attachment: PendingAttachment) {
        File(attachment.path).delete()
    }

    private fun extension(mime: String): String = when {
        mime.startsWith("video/") -> "mp4"
        mime == "image/png" -> "png"
        mime == "image/gif" -> "gif"
        mime == "image/webp" -> "webp"
        else -> "jpg"
    }
}
