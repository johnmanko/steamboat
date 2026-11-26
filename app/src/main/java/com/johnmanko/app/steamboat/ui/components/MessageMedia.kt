package com.johnmanko.app.steamboat.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.johnmanko.app.steamboat.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MessageMedia(
    path: String,
    mime: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val video = mime?.startsWith("video/") == true
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var open by remember(path) { mutableStateOf(false) }
    LaunchedEffect(path, video) {
        bitmap = withContext(Dispatchers.IO) {
            if (video) videoFrame(path) else decodeSampled(path, 900)
        }
    }
    val frame = if (compact) Modifier.size(88.dp) else Modifier.fillMaxWidth().height(180.dp)
    Box(
        modifier = modifier
            .then(frame)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable { open = true },
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let { frame ->
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = stringResource(if (video) R.string.video else R.string.photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (video) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.45f)) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.play_video),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
    if (open) {
        MediaViewer(path = path, video = video, onDismiss = { open = false })
    }
}

@Composable
private fun MediaViewer(path: String, video: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (video) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.fromFile(File(path)))
                            setOnPreparedListener { it.start() }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                )
            } else {
                var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(path) {
                    bitmap = withContext(Dispatchers.IO) { decodeSampled(path, 1600) }
                }
                bitmap?.let { frame ->
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = stringResource(R.string.photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close_media),
                    tint = Color.White,
                )
            }
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

private fun videoFrame(path: String): Bitmap? = runCatching {
    MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(path)
        retriever.getFrameAtTime(0)
    }
}.getOrNull()
