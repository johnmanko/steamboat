package com.johnmanko.app.steamboat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.db.MessageEntity
import com.johnmanko.app.steamboat.domain.MessageStatus
import com.johnmanko.app.steamboat.ui.format.formatMessageTime
import com.johnmanko.app.steamboat.ui.format.textLinks
import com.johnmanko.app.steamboat.ui.theme.LocalPastel

private val OutgoingShape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
private val IncomingShape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    onRetry: (Long) -> Unit,
    onLongPress: () -> Unit,
    senderName: String? = null,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pastel = LocalPastel.current
    val yesterday = stringResource(R.string.yesterday)
    val failed = message.outgoing && message.status == MessageStatus.FAILED
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
    ) {
        val shape = if (message.outgoing) OutgoingShape else IncomingShape
        val ink = if (message.outgoing) pastel.onOutgoing else pastel.onIncoming
        Surface(
            modifier = if (highlighted) {
                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
            } else {
                Modifier
            },
            shape = shape,
            color = if (message.outgoing) pastel.outgoing else pastel.incoming,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .pointerInput(onLongPress) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!senderName.isNullOrBlank()) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = ink,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                val media = message.mediaParts()
                val images = media.filter { !it.isVideo }
                media.filter { it.isVideo }.forEach { part ->
                    MessageMedia(path = part.path, mime = part.mime, onLongClick = onLongPress)
                }
                if (images.size >= 2) {
                    ImageCollage(images, onLongClick = onLongPress)
                } else {
                    images.forEach { part ->
                        MessageMedia(path = part.path, mime = part.mime, onLongClick = onLongPress)
                    }
                }
                if (message.body.isNotBlank()) {
                    MessageBody(message.body, ink)
                }
                Text(
                    text = statusLine(message, yesterday),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.72f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.End)
                        .then(
                            if (failed) {
                                Modifier.combinedClickable(
                                    onClick = { onRetry(message.id) },
                                    onLongClick = onLongPress,
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun MessageBody(body: String, color: Color) {
    val annotated = remember(body, color) {
        val links = textLinks(body)
        buildAnnotatedString {
            if (links.isEmpty()) {
                append(body)
                return@buildAnnotatedString
            }
            val linkStyle = TextLinkStyles(
                style = SpanStyle(color = color, textDecoration = TextDecoration.Underline),
            )
            var cursor = 0
            for (link in links) {
                append(body.substring(cursor, link.start))
                pushLink(LinkAnnotation.Url(url = link.url, styles = linkStyle))
                append(body.substring(link.start, link.endExclusive))
                pop()
                cursor = link.endExclusive
            }
            if (cursor < body.length) append(body.substring(cursor))
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun statusLine(message: MessageEntity, yesterday: String): String {
    val time = formatMessageTime(message.sentAt, yesterday)
    return when {
        message.outgoing && message.status == MessageStatus.SENDING ->
            stringResource(R.string.sending)
        message.outgoing && message.status == MessageStatus.FAILED ->
            stringResource(R.string.tap_retry)
        else -> time
    }
}
