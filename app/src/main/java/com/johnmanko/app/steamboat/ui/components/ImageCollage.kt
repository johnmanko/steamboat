package com.johnmanko.app.steamboat.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.johnmanko.app.steamboat.data.media.PhoneGallery
import com.johnmanko.app.steamboat.domain.PendingAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCollage(
    images: List<PendingAttachment>,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val shown = images.take(3)
    val overflow = images.size > 3
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(if (images.size == 2) 132.dp else 176.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onLongClick == null) {
                    Modifier.clickable { open = true }
                } else {
                    Modifier.combinedClickable(onClick = { open = true }, onLongClick = onLongClick)
                },
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (shown.size == 2) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CollageTile(shown[0], Modifier.weight(1f).fillMaxHeight())
                CollageTile(shown[1], Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CollageTile(shown[0], Modifier.weight(1f).fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    CollageTile(shown[1], Modifier.weight(1f).fillMaxWidth())
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        CollageTile(shown[2], Modifier.fillMaxSize())
                        if (overflow) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.collage_more),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (open) {
        ImageGallery(images = images, onDismiss = { open = false })
    }
}

@Composable
private fun ImageGallery(images: List<PendingAttachment>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.images_saved)
    val failed = stringResource(R.string.images_not_saved)
    fun save(chosen: List<PendingAttachment>) {
        scope.launch {
            val count = withContext(Dispatchers.IO) { PhoneGallery.save(context, chosen) }
            Toast.makeText(context, if (count > 0) saved else failed, Toast.LENGTH_SHORT).show()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(Modifier.fillMaxSize().background(Color.Black)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_media),
                        tint = Color.White,
                    )
                }
                TextButton(onClick = { save(images) }) {
                    Text(stringResource(R.string.save_all))
                }
                TextButton(
                    onClick = {
                        val image = images.find { it.path == selected } ?: return@TextButton
                        save(listOf(image))
                    },
                    enabled = selected != null,
                ) {
                    Text(stringResource(R.string.save_image))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                images.forEach { image ->
                    val picked = selected == image.path
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (picked) 3.dp else 0.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { selected = if (picked) null else image.path },
                    ) {
                        CollageTile(image, Modifier.fillMaxSize(), maxEdge = 1400, scale = ContentScale.Fit)
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageTile(
    image: PendingAttachment,
    modifier: Modifier = Modifier,
    maxEdge: Int = 480,
    scale: ContentScale = ContentScale.Crop,
) {
    var bitmap by remember(image.path) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(image.path, maxEdge) {
        bitmap = withContext(Dispatchers.IO) { decodeSampled(image.path, maxEdge) }
    }
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = stringResource(R.string.photo),
                contentScale = scale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun decodeSampled(path: String, maxEdge: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}
