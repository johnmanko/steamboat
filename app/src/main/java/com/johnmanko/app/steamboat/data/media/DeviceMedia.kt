package com.johnmanko.app.steamboat.data.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class DeviceMedia(
    val uri: Uri,
    val mime: String,
    val size: Long,
)

/** Photos and videos on the device, newest first, with the size used for the MMS limit. */
class DeviceMediaList(private val context: Context) {
    fun recent(): List<DeviceMedia> {
        val images = read(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videos = read(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        return (images + videos).sortedByDescending { it.addedAt }.take(MAX_ITEMS).map { it.media }
    }

    private fun read(collection: Uri): List<Dated> = runCatching { query(collection) }.getOrDefault(emptyList())

    private fun query(collection: Uri): List<Dated> {
        val rows = ArrayList<Dated>()
        val cursor = context.contentResolver.query(
            collection,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
            ),
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        ) ?: return rows
        cursor.use {
            val id = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val mime = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val size = it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val added = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (it.moveToNext() && rows.size < MAX_ITEMS) {
                val bytes = it.getLong(size)
                if (bytes <= 0L) continue
                rows += Dated(
                    addedAt = it.getLong(added),
                    media = DeviceMedia(
                        uri = Uri.withAppendedPath(collection, it.getLong(id).toString()),
                        mime = it.getString(mime) ?: "image/jpeg",
                        size = bytes,
                    ),
                )
            }
        }
        return rows
    }

    private data class Dated(val addedAt: Long, val media: DeviceMedia)

    private companion object {
        const val MAX_ITEMS = 180
    }
}
