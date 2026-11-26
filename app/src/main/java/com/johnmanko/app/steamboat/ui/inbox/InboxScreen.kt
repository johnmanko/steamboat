package com.johnmanko.app.steamboat.ui.inbox

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.db.ConversationEntity
import com.johnmanko.app.steamboat.data.prefs.NotificationPreferences
import com.johnmanko.app.steamboat.domain.AlertPolicy
import com.johnmanko.app.steamboat.ui.notify.alertClueLabel
import com.johnmanko.app.steamboat.ui.notify.alertIcon
import com.johnmanko.app.steamboat.data.db.MessageSearchHit
import com.johnmanko.app.steamboat.data.sms.isDefaultSmsApp
import com.johnmanko.app.steamboat.ui.components.PastelAvatar
import com.johnmanko.app.steamboat.ui.theme.LocalThemeWash
import com.johnmanko.app.steamboat.ui.theme.quieterOn
import com.johnmanko.app.steamboat.ui.theme.strongerOn
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import com.johnmanko.app.steamboat.ui.format.formatConversationTime
import com.johnmanko.app.steamboat.ui.format.formatPhone
import com.johnmanko.app.steamboat.ui.format.textExcerpt
import androidx.compose.foundation.lazy.LazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxRoute(
    viewModel: InboxViewModel,
    notificationSettings: NotificationPreferences,
    onOpenThread: (Long) -> Unit,
    onOpenMessage: (Long, Long) -> Unit,
    onNewMessage: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val conversations = viewModel.conversations.collectAsLazyPagingItems()
    val hits = viewModel.hits.collectAsLazyPagingItems()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchOpen by viewModel.searchOpen.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val alerts by notificationSettings.overrides.collectAsStateWithLifecycle(emptyMap())
    val globalAlert by notificationSettings.global.collectAsStateWithLifecycle(AlertPolicy())
    val searchFocus = remember { FocusRequester() }
    var focusSearch by remember { mutableStateOf(false) }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFocus.requestFocus()
            focusSearch = false
        }
    }
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val missing = remember(tick) { missingPermissions(context) }
    val defaultSms = remember(tick) { context.isDefaultSmsApp() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        tick += 1
        viewModel.refresh()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        tick += 1
        viewModel.refresh()
    }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selecting = selectedIds.isNotEmpty()
    BackHandler(enabled = selecting && !confirmDelete) {
        selectedIds = emptySet()
    }
    fun toggleSelected(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            if (selecting) {
                SelectionBar(
                    count = selectedIds.size,
                    onExit = { selectedIds = emptySet() },
                    onDelete = { confirmDelete = true },
                )
            } else if (searchOpen) {
                SearchField(
                    query = query,
                    focusRequester = searchFocus,
                    onQuery = viewModel::onQuery,
                    onClose = viewModel::closeSearch,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_steamboat),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(R.string.inbox_title))
                            Icon(
                                imageVector = alertIcon(globalAlert),
                                contentDescription = alertClueLabel(globalAlert),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.openSearch()
                            focusSearch = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    },
                    colors = washTopBarColors(),
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
                FloatingActionButton(onClick = onNewMessage) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.new_message))
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (missing.isNotEmpty() || !defaultSms) {
                    SetupBanner(
                        needsPermissions = missing.isNotEmpty(),
                        needsDefault = !defaultSms,
                        onGrant = { permissionLauncher.launch(missing.toTypedArray()) },
                        onMakeDefault = {
                            val roles = context.getSystemService(RoleManager::class.java)
                            roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_SMS))
                        },
                    )
                }
                if (query.isBlank()) {
                    ConversationList(
                        conversations = conversations,
                        alerts = alerts,
                        query = query,
                        selecting = selecting,
                        selectedIds = selectedIds,
                        onOpenThread = onOpenThread,
                        onToggle = ::toggleSelected,
                        onStartSelection = { selectedIds = selectedIds + it },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    MessageHitList(
                        hits = hits,
                        query = query,
                        onOpenMessage = onOpenMessage,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        val many = selectedIds.size > 1
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    stringResource(
                        if (many) R.string.delete_conversations_title else R.string.delete_conversation_title,
                    ),
                )
            },
            text = {
                Text(
                    stringResource(
                        if (many) R.string.delete_conversations_body else R.string.delete_conversation_body,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds
                    confirmDelete = false
                    selectedIds = emptySet()
                    viewModel.deleteConversations(ids)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    focusRequester: FocusRequester,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.search_messages)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close_search),
                    )
                }
            },
            trailingIcon = if (query.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear_search),
                        )
                    }
                }
            },
        )
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings),
            )
        }
    }
}

@Composable
private fun SetupBanner(
    needsPermissions: Boolean,
    needsDefault: Boolean,
    onGrant: () -> Unit,
    onMakeDefault: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (needsPermissions) {
                Text(stringResource(R.string.permissions_body), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onGrant) { Text(stringResource(R.string.grant_permissions)) }
            }
            if (needsDefault) {
                Text(stringResource(R.string.default_sms_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onMakeDefault) { Text(stringResource(R.string.make_default)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
    count: Int,
    onExit: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.exit_selection),
                )
            }
        },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        },
        colors = washTopBarColors(),
    )
}

@Composable
private fun ConversationList(
    conversations: LazyPagingItems<ConversationEntity>,
    alerts: Map<Long, AlertPolicy>,
    query: String,
    selecting: Boolean,
    selectedIds: Set<Long>,
    onOpenThread: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onStartSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh = conversations.loadState.refresh
    when {
        refresh is LoadState.Loading && conversations.itemCount == 0 -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        refresh is LoadState.NotLoading && conversations.itemCount == 0 -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(if (query.isBlank()) R.string.no_messages else R.string.no_matches),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (query.isBlank()) {
                    Text(
                        text = stringResource(R.string.no_messages_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(
                    count = conversations.itemCount,
                    key = conversations.itemKey { it.id },
                    contentType = conversations.itemContentType { "conversation" },
                ) { index ->
                    val conversation = conversations[index] ?: return@items
                    val picked = conversation.id in selectedIds
                    ConversationRow(
                        conversation,
                        alert = alerts[conversation.id],
                        selected = picked,
                        onClick = {
                            if (selecting) onToggle(conversation.id) else onOpenThread(conversation.id)
                        },
                        onLongClick = { onStartSelection(conversation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageHitList(
    hits: LazyPagingItems<MessageSearchHit>,
    query: String,
    onOpenMessage: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh = hits.loadState.refresh
    when {
        refresh is LoadState.Loading && hits.itemCount == 0 -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        refresh is LoadState.NotLoading && hits.itemCount == 0 -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_matches),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(
                    count = hits.itemCount,
                    key = hits.itemKey { it.id },
                    contentType = hits.itemContentType { "hit" },
                ) { index ->
                    val hit = hits[index] ?: return@items
                    MessageHitRow(hit, query, onClick = { onOpenMessage(hit.conversationId, hit.id) })
                }
            }
        }
    }
}

@Composable
private fun MessageHitRow(hit: MessageSearchHit, query: String, onClick: () -> Unit) {
    val yesterday = stringResource(R.string.yesterday)
    val ink = MaterialTheme.colorScheme.onSurface
    val field = LocalThemeWash.current.middle
    val nameColor = ink.strongerOn(field)
    val quiet = ink.quieterOn(field)
    val excerpt = remember(hit.body, query) { textExcerpt(hit.body, query) }
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PastelAvatar(
            title = hit.title,
            key = hit.matchKey.ifBlank { hit.address },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = hit.title.ifBlank { formatPhone(hit.address) },
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildAnnotatedString {
                    append(excerpt.text)
                    excerpt.highlights.forEach { range ->
                        addStyle(
                            SpanStyle(background = highlight, color = nameColor, fontWeight = FontWeight.SemiBold),
                            range.first,
                            range.last + 1,
                        )
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = quiet,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatConversationTime(hit.sentAt, yesterday),
            style = MaterialTheme.typography.labelMedium,
            color = quiet,
        )
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    alert: AlertPolicy?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val yesterday = stringResource(R.string.yesterday)
    val unreadLabel = stringResource(R.string.unread_count, conversation.unreadCount)
    val ink = MaterialTheme.colorScheme.onSurface
    val field = LocalThemeWash.current.middle
    val nameColor = ink.strongerOn(field)
    val quiet = ink.quieterOn(field)
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) highlight else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.selected_mark),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        PastelAvatar(
            title = conversation.title,
            key = conversation.matchKey.ifBlank { conversation.address },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = conversation.title.ifBlank { formatPhone(conversation.address) },
                style = MaterialTheme.typography.titleMedium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = conversation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = quiet,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (alert != null) {
                    Icon(
                        imageVector = alertIcon(alert),
                        contentDescription = alertClueLabel(alert),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = formatConversationTime(conversation.updatedAt, yesterday),
                    style = MaterialTheme.typography.labelMedium,
                    color = quiet,
                )
            }
            if (conversation.unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .semantics { contentDescription = unreadLabel },
                ) {
                    Text(
                        text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

private fun missingPermissions(context: android.content.Context): List<String> {
    val wanted = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.WRITE_CONTACTS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return wanted.filter {
        ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
