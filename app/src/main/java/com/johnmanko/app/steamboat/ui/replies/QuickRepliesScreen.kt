package com.johnmanko.app.steamboat.ui.replies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import com.johnmanko.app.steamboat.data.prefs.QuickReplies
import com.johnmanko.app.steamboat.data.prefs.QuickReplyRules
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRepliesRoute(
    quickReplies: QuickReplies,
    onBack: () -> Unit,
) {
    var phrases by remember { mutableStateOf<List<String>?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        phrases = quickReplies.current()
    }
    fun update(next: List<String>) {
        phrases = next
        scope.launch { quickReplies.set(next) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_replies_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    val current = phrases
                    if (current != null && current.size < QuickReplyRules.MAX_SAVED) {
                        IconButton(onClick = { update(current + "") }) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.quick_reply_add),
                            )
                        }
                    }
                },
                colors = washTopBarColors(),
            )
        },
    ) { padding ->
        val current = phrases
        if (current == null) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.quick_replies_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(current, key = { index, _ -> index }) { index, phrase ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { value ->
                            val clipped = value.replace('\n', ' ').take(QuickReplyRules.MAX_PHRASE)
                            update(current.toMutableList().also { it[index] = clipped })
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.quick_reply_label, index + 1)) },
                        supportingText = {
                            Text("${phrase.trim().length}/${QuickReplyRules.MAX_PHRASE}")
                        },
                    )
                    IconButton(onClick = {
                        update(current.toMutableList().also { it.removeAt(index) })
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.quick_reply_remove),
                        )
                    }
                }
            }
            if (current.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.quick_replies_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }
            }
        }
    }
}
