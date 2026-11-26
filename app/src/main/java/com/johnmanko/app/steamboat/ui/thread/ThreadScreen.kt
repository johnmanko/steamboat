package com.johnmanko.app.steamboat.ui.thread

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.db.ConversationMemberEntity
import com.johnmanko.app.steamboat.data.db.MessageEntity
import com.johnmanko.app.steamboat.data.prefs.LocationPreferences
import com.johnmanko.app.steamboat.data.prefs.LocationSettings
import com.johnmanko.app.steamboat.data.prefs.NotificationPreferences
import com.johnmanko.app.steamboat.data.prefs.QuickReplies
import com.johnmanko.app.steamboat.notify.NotificationCenter
import com.johnmanko.app.steamboat.ui.notify.alertClueLabel
import com.johnmanko.app.steamboat.ui.notify.alertIcon
import com.johnmanko.app.steamboat.ui.settings.ThreadNotificationDialog
import com.johnmanko.app.steamboat.domain.insertAtSelection
import com.johnmanko.app.steamboat.domain.mapLink
import com.johnmanko.app.steamboat.location.currentCoordinates
import com.johnmanko.app.steamboat.notify.ActiveConversation
import com.johnmanko.app.steamboat.domain.PendingAttachment
import com.johnmanko.app.steamboat.ui.components.MessageBubble
import com.johnmanko.app.steamboat.ui.components.MessageMedia
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import com.johnmanko.app.steamboat.ui.components.PastelAvatar
import com.johnmanko.app.steamboat.ui.format.formatPhone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadRoute(
    conversationId: Long,
    focusMessageId: Long = -1L,
    viewModel: ThreadViewModel,
    quickReplies: QuickReplies,
    location: LocationPreferences,
    notificationSettings: NotificationPreferences,
    notifications: NotificationCenter,
    onBack: () -> Unit,
    onLinkContact: (Long) -> Unit,
    onAddPerson: () -> Unit,
) {
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val phrases by quickReplies.phrases.collectAsStateWithLifecycle(emptyList())
    val locationSettings by location.settings.collectAsStateWithLifecycle(LocationSettings())
    val alerts by notificationSettings.overrides.collectAsStateWithLifecycle(emptyMap())
    val threadAlert = alerts[conversationId]
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var peopleOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    val group = members.size >= 2

    DisposableEffect(conversationId) {
        ActiveConversation.id.value = conversationId
        onDispose {
            if (ActiveConversation.id.value == conversationId) ActiveConversation.id.value = null
        }
    }
    LaunchedEffect(messages.itemCount) {
        if (focusMessageId > 0L) return@LaunchedEffect
        if (messages.itemCount > 0 && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }
    var placedFocus by remember(focusMessageId) { mutableStateOf(false) }
    LaunchedEffect(focusMessageId, messages.itemCount, messages.loadState.append) {
        if (focusMessageId <= 0L || placedFocus) return@LaunchedEffect
        val count = messages.itemCount
        val index = (0 until count).firstOrNull { messages.peek(it)?.id == focusMessageId }
        val append = messages.loadState.append
        when {
            index != null -> {
                listState.animateScrollToItem(index)
                placedFocus = true
            }
            count > 0 && append is LoadState.NotLoading && !append.endOfPaginationReached ->
                listState.scrollToItem(count - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                colors = washTopBarColors(),
                title = {
                    val title = conversation?.title?.ifBlank { null }
                        ?: conversation?.address?.let(::formatPhone)
                        ?: ""
                    val address = conversation?.address?.let(::formatPhone).orEmpty()
                    val subtitle = if (group) stringResource(R.string.people_count, members.size) else address
                    Row(
                        modifier = Modifier.clickable { peopleOpen = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (conversation != null) {
                            PastelAvatar(
                                title = title,
                                key = conversation?.matchKey ?: address,
                                size = 36.dp,
                            )
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (group || (subtitle.isNotBlank() && subtitle != title)) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (threadAlert != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = alertIcon(threadAlert),
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = alertClueLabel(threadAlert),
                                        modifier = Modifier.padding(start = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        val linked = conversation?.contactLookupKey != null
                        if (!group) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (linked) R.string.change_contact else R.string.link_contact,
                                        ),
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onLinkContact(conversationId)
                                },
                            )
                            if (linked || conversation?.contactLinkedManually == true) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.unlink_contact)) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.unlink()
                                    },
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.notifications_title)) },
                            onClick = {
                                menuOpen = false
                                notificationsOpen = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_conversation)) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            when {
                messages.loadState.refresh is LoadState.Loading && messages.itemCount == 0 -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            count = messages.itemCount,
                            key = messages.itemKey { it.id },
                            contentType = messages.itemContentType { "message" },
                        ) { index ->
                            val message = messages[index] ?: return@items
                            MessageBubble(
                                message = message,
                                onRetry = viewModel::retry,
                                senderName = senderName(message, members),
                                highlighted = message.id == focusMessageId,
                            )
                        }
                        if (messages.loadState.append is LoadState.Loading) {
                            item(key = "older") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
            val context = LocalContext.current
            LaunchedEffect(viewModel.imagesLeftOff) {
                if (viewModel.imagesLeftOff > 0) {
                    Toast.makeText(context, R.string.images_left_off, Toast.LENGTH_LONG).show()
                    viewModel.clearImagesLeftOff()
                }
            }
            var chooserOpen by remember { mutableStateOf(false) }
            val photoAccess = rememberLauncherForActivityResult(RequestMultiplePermissions()) { grants ->
                if (grants.values.any { it }) {
                    chooserOpen = true
                } else {
                    Toast.makeText(context, R.string.photo_access_needed, Toast.LENGTH_LONG).show()
                }
            }
            Composer(
                text = viewModel.composer,
                attachments = viewModel.attachments,
                phrases = phrases,
                location = locationSettings,
                onText = viewModel::onComposerChange,
                onAttach = {
                    if (canReadPhotos(context)) chooserOpen = true
                    else photoAccess.launch(photoPermissions())
                },
                onPickReply = viewModel::useReply,
                onRemoveAttachment = viewModel::removeAttachment,
                onSend = viewModel::send,
            )
            if (chooserOpen) {
                ImageChooser(
                    usedBytes = viewModel.attachments.sumOf { java.io.File(it.path).length() },
                    onDismiss = { chooserOpen = false },
                    onAdd = { uris ->
                        chooserOpen = false
                        viewModel.attach(uris)
                    },
                )
            }
        }
    }

    if (notificationsOpen) {
        ThreadNotificationDialog(
            conversationId = conversationId,
            title = conversation?.title?.ifBlank { null } ?: conversation?.address.orEmpty(),
            preferences = notificationSettings,
            notifications = notifications,
            onDismiss = { notificationsOpen = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_conversation_title)) },
            text = { Text(stringResource(R.string.delete_conversation_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (peopleOpen) {
        val shown = if (group) {
            members
        } else {
            listOf(
                ConversationMemberEntity(
                    conversationId = conversationId,
                    matchKey = conversation?.matchKey.orEmpty(),
                    address = conversation?.address.orEmpty(),
                    title = conversation?.title?.ifBlank { null }
                        ?: conversation?.address?.let(::formatPhone).orEmpty(),
                ),
            )
        }
        PeopleDialog(
            people = shown,
            canRemove = group && members.size > 2,
            onDismiss = { peopleOpen = false },
            onAdd = {
                peopleOpen = false
                onAddPerson()
            },
            onRemove = viewModel::removePerson,
        )
    }
}

private fun senderName(
    message: MessageEntity,
    members: List<ConversationMemberEntity>,
): String? {
    if (message.outgoing || members.size < 2) return null
    val key = message.senderMatchKey ?: return null
    val title = members.firstOrNull { it.matchKey == key }?.title ?: return null
    return title.substringBefore(' ').ifBlank { title }
}

@Composable
private fun PeopleDialog(
    people: List<ConversationMemberEntity>,
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conversation_people)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                people.forEach { person ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = person.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (canRemove) {
                            IconButton(onClick = { onRemove(person.matchKey) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.remove_person),
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onAdd) { Text(stringResource(R.string.add_person)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun Composer(
    text: String,
    attachments: List<PendingAttachment>,
    phrases: List<String>,
    location: LocationSettings,
    onText: (String) -> Unit,
    onAttach: () -> Unit,
    onPickReply: (String) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSend: () -> Unit,
) {
    var repliesOpen by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue(text)) }
    if (field.text != text) field = TextFieldValue(text, TextRange(text.length))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun insert(snippet: String) {
        val (next, cursor) = insertAtSelection(
            field.text,
            field.selection.min,
            field.selection.max,
            snippet,
        )
        field = TextFieldValue(next, TextRange(cursor))
        onText(next)
    }
    fun placeLocation() {
        scope.launch {
            val coordinates = currentCoordinates(context)
            if (coordinates == null) {
                Toast.makeText(context, R.string.location_unavailable, Toast.LENGTH_SHORT).show()
            } else {
                insert(mapLink(location.provider, coordinates.first, coordinates.second))
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(RequestMultiplePermissions()) {
        if (locationGranted(context)) placeLocation()
        else Toast.makeText(context, R.string.location_permission_needed, Toast.LENGTH_SHORT).show()
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (attachments.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attachments, key = { it.path }) { item ->
                    Box {
                        MessageMedia(path = item.path, mime = item.mime, compact = true)
                        IconButton(
                            onClick = { onRemoveAttachment(item.path) },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.remove_attachment),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttach) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.attach_media),
                )
            }
            Box {
                IconButton(onClick = { repliesOpen = true }) {
                    Icon(
                        Icons.Filled.Quickreply,
                        contentDescription = stringResource(R.string.quick_replies),
                    )
                }
                DropdownMenu(expanded = repliesOpen, onDismissRequest = { repliesOpen = false }) {
                    if (phrases.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quick_replies_empty)) },
                            onClick = { repliesOpen = false },
                            enabled = false,
                        )
                    } else {
                        phrases.forEach { phrase ->
                            DropdownMenuItem(
                                text = { Text(phrase) },
                                onClick = {
                                    repliesOpen = false
                                    onPickReply(phrase)
                                },
                            )
                        }
                    }
                }
            }
            if (location.enabled) {
                IconButton(onClick = {
                    if (locationGranted(context)) placeLocation()
                    else locationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_map_pin),
                        contentDescription = stringResource(R.string.share_location),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = field,
                onValueChange = { updated ->
                    field = updated
                    onText(updated.text)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.message_hint)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
            )
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() || attachments.isNotEmpty(),
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }
}

private fun canReadPhotos(context: Context): Boolean {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return permissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun photoPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun locationGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}
