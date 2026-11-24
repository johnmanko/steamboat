package com.johnmanko.app.steamboat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnmanko.app.steamboat.BuildConfig
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors

private const val SourceUrl = "https://github.com/johnmanko/steamboat"
private const val CreatorUrl = "https://x.com/johnmanko"
private const val LicenseUrl = "https://www.gnu.org/licenses/gpl-3.0.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutRoute(onBack: () -> Unit) {
    val links = LocalUriHandler.current
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = washTopBarColors(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_steamboat),
                        contentDescription = null,
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                AboutLink(
                    label = stringResource(R.string.about_creator),
                    value = stringResource(R.string.about_creator_name),
                    onClick = { links.openUri(CreatorUrl) },
                )
            }
            item {
                AboutLink(
                    label = stringResource(R.string.about_source),
                    value = stringResource(R.string.about_source_value),
                    onClick = { links.openUri(SourceUrl) },
                )
            }
            item {
                AboutLink(
                    label = stringResource(R.string.about_license),
                    value = stringResource(R.string.about_license_value),
                    onClick = { links.openUri(LicenseUrl) },
                )
            }
        }
    }
}

@Composable
private fun AboutLink(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
