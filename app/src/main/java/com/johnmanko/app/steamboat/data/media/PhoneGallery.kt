package com.johnmanko.app.steamboat.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.johnmanko.app.steamboat.domain.PendingAttachment
import java.io.File

/** Copies message images into the phone's Pictures library. */
object PhoneGallery {
    fun save(context: Context, images: List<PendingAttachment>): Int {
        var saved = 0
        images.forEachIndexed { index, image ->
            if (!image.mime.startsWith("image/")) return@forEachIndexed
            if (write(context, image, index)) saved += 1
        }
        return saved
    }

    private fun write(context: Context, image: PendingAttachment, index: Int): Boolean {
        val file = File(image.path)
        if (!file.isFile) return false
        val name = "Steamboat-${System.currentTimeMillis()}-$index.${extension(image.mime)}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, image.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Steamboat")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val copied = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            context.contentResolver.delete(uri, null, null)
            return false
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        return true
    }

    private fun extension(mime: String): String = when (mime) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
