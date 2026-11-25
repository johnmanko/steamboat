package com.johnmanko.app.steamboat.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.ui.components.PastelAvatar
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import com.johnmanko.app.steamboat.domain.PhoneNumbers
import com.johnmanko.app.steamboat.ui.format.formatPhone
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsRoute(
    mode: ContactsMode,
    conversationId: Long?,
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onOpenThread: (Long) -> Unit,
    onLinked: () -> Unit,
) {
    val contacts = viewModel.contacts.collectAsLazyPagingItems()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val granted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }
    var pendingNumber by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        tick += 1
        viewModel.refresh()
    }
    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        val number = pendingNumber
        pendingNumber = null
        if (number != null) viewModel.addNumber(number, onOpenThread)
    }
    fun addTypedNumber(number: String) {
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (writeGranted) {
            viewModel.addNumber(number, onOpenThread)
        } else {
            pendingNumber = number
            writeLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }
    LaunchedEffect(granted) {
        if (granted) viewModel.refresh()
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                colors = washTopBarColors(),
                title = {
                    Text(
                        stringResource(
                            when (mode) {
                                ContactsMode.Link -> R.string.link_title
                                ContactsMode.Add -> R.string.add_person
                                ContactsMode.NewMessage -> R.string.contacts_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!granted) {
                Column(Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.contacts_permission_body))
                    Button(
                        onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text(stringResource(R.string.allow_contacts)) }
                }
                return@Column
            }
            TextField(
                value = query,
                onValueChange = viewModel::onQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
            )
            when {
                contacts.loadState.refresh is LoadState.Loading && contacts.itemCount == 0 -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                contacts.itemCount == 0 && contacts.loadState.refresh is LoadState.NotLoading -> {
                    Column(Modifier.padding(24.dp).weight(1f)) {
                        Text(stringResource(if (query.isBlank()) R.string.no_contacts else R.string.no_matches))
                        val typed = PhoneNumbers.newContactNumber(query)
                        if (mode == ContactsMode.NewMessage && typed != null) {
                            Button(
                                onClick = { addTypedNumber(typed) },
                                modifier = Modifier.padding(top = 12.dp),
                            ) { Text(stringResource(R.string.add_number, formatPhone(typed))) }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(
                            count = contacts.itemCount,
                            key = contacts.itemKey { "${it.lookupKey}:${it.matchKey}" },
                            contentType = contacts.itemContentType { "contact" },
                        ) { index ->
                            val contact = contacts[index] ?: return@items
                            val picked = viewModel.selected.any { it.matchKey == contact.matchKey }
                            ContactRow(contact, selected = picked && mode == ContactsMode.NewMessage) {
                                when (mode) {
                                    ContactsMode.NewMessage -> viewModel.toggle(contact)
                                    ContactsMode.Link -> {
                                        val id = conversationId ?: return@ContactRow
                                        viewModel.link(id, contact, onLinked)
                                    }
                                    ContactsMode.Add -> {
                                        val id = conversationId ?: return@ContactRow
                                        viewModel.add(id, contact, onOpenThread)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (mode == ContactsMode.NewMessage && viewModel.selected.isNotEmpty()) {
                Button(
                    onClick = { viewModel.startSelected(onOpenThread) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text(stringResource(R.string.message_selected)) }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: ContactEntity, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PastelAvatar(title = contact.displayName, key = contact.matchKey, size = 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatPhone(contact.phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp).size(22.dp),
            )
        }
    }
}
