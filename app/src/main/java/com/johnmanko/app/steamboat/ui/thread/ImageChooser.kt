package com.johnmanko.app.steamboat.ui.thread

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.media.DeviceMedia
import com.johnmanko.app.steamboat.data.media.DeviceMediaList
import com.johnmanko.app.steamboat.data.sms.MmsBudget
import com.johnmanko.app.steamboat.domain.MessageAttachments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ImageChooser(
    usedBytes: Long,
    onDismiss: () -> Unit,
    onAdd: (List<Uri>) -> Unit,
) {
    val context = LocalContext.current
    var media by remember { mutableStateOf<List<DeviceMedia>?>(null) }
    var selected by remember { mutableStateOf<List<DeviceMedia>>(emptyList()) }
    val maxBytes = remember { MmsBudget(context).maxBytes() }
    val doesNotFit = stringResource(R.string.image_does_not_fit)
    LaunchedEffect(Unit) {
        media = withContext(Dispatchers.IO) { DeviceMediaList(context).recent() }
    }
    val selectedBytes = selected.sumOf { it.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_media))
                    }
                    Text(
                        text = stringResource(R.string.choose_images),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { onAdd(selected.map { it.uri }) },
                        enabled = selected.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.add_images))
                    }
                }
                when (val items = media) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    emptyList<DeviceMedia>() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_photos))
                    }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(items, key = { it.uri.toString() }) { item ->
                            val picked = selected.any { it.uri == item.uri }
                            val allowed = picked || MessageAttachments.fits(
                                usedBytes = usedBytes + selectedBytes,
                                nextBytes = item.size,
                                maxBytes = maxBytes,
                            )
                            MediaTile(
                                item = item,
                                picked = picked,
                                enabled = allowed,
                                onClick = {
                                    if (picked) {
                                        selected = selected.filterNot { it.uri == item.uri }
                                    } else if (allowed) {
                                        selected = selected + item
                                    } else {
                                        Toast.makeText(context, doesNotFit, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(
    item: DeviceMedia,
    picked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.uri) {
        bitmap = withContext(Dispatchers.IO) { decodeUri(context, item.uri, item.mime) }
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
    ) {
        bitmap?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = stringResource(if (item.mime.startsWith("video/")) R.string.video else R.string.photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (picked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun decodeUri(context: android.content.Context, uri: Uri, mime: String): Bitmap? {
    if (mime.startsWith("video/")) {
        return runCatching {
            android.media.MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0)
            }
        }.getOrNull()
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 240 || bounds.outHeight / sample > 240) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor, null, options)
    }
}
