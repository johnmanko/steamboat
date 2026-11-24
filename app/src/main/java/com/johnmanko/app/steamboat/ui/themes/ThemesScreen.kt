package com.johnmanko.app.steamboat.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johnmanko.app.steamboat.R
import com.johnmanko.app.steamboat.data.themes.ActiveTheme
import com.johnmanko.app.steamboat.data.themes.InstalledTheme
import com.johnmanko.app.steamboat.data.themes.ThemeLibrary
import com.johnmanko.app.steamboat.ui.theme.GoghPalette
import com.johnmanko.app.steamboat.ui.theme.PastelDark
import com.johnmanko.app.steamboat.ui.theme.PastelLight
import com.johnmanko.app.steamboat.ui.theme.SystemDarkWash
import com.johnmanko.app.steamboat.ui.theme.SystemLightWash
import com.johnmanko.app.steamboat.ui.theme.ThemeWash
import com.johnmanko.app.steamboat.ui.theme.brush
import com.johnmanko.app.steamboat.ui.theme.contrastInk
import com.johnmanko.app.steamboat.ui.theme.themeWash
import com.johnmanko.app.steamboat.ui.theme.toPalette
import com.johnmanko.app.steamboat.ui.theme.toPastel
import com.johnmanko.app.steamboat.ui.theme.washContentColor
import com.johnmanko.app.steamboat.ui.theme.washTopBarColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesRoute(
    themes: ThemeLibrary,
    onBack: () -> Unit,
) {
    val selected by themes.selection.collectAsStateWithLifecycle()
    val catalog by themes.catalog.collectAsStateWithLifecycle()
    val systemLabel = stringResource(R.string.theme_system)
    val cards = remember(catalog, systemLabel) { themeCards(catalog, systemLabel) }
    val scope = rememberCoroutineScope()
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = washContentColor(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.themes_title)) },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cards, key = { it.key }) { card ->
                when (card) {
                    ThemeCard.System -> SystemThemeCard(
                        label = systemLabel,
                        selected = selected is ActiveTheme.System,
                        onClick = { scope.launch { themes.selectSystem() } },
                    )
                    is ThemeCard.Installed -> {
                        val palette = card.theme.toPalette()
                        PaletteThemeCard(
                            palette = palette,
                            selected = (selected as? ActiveTheme.Palette)?.theme?.key == card.theme.key,
                            onClick = { scope.launch { themes.select(card.theme) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemThemeCard(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(
            label = label,
            detail = stringResource(R.string.theme_system_body),
            color = MaterialTheme.colorScheme.onSurface,
            selected = selected,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniPane(
                wash = SystemLightWash,
                foreground = Color(0xFF2C2428),
                swatches = PastelLight.avatars,
                outgoing = PastelLight.outgoing,
                onOutgoing = PastelLight.onOutgoing,
                incoming = PastelLight.incoming,
                onIncoming = PastelLight.onIncoming,
                modifier = Modifier.weight(1f),
            )
            MiniPane(
                wash = SystemDarkWash,
                foreground = Color(0xFFF6EEF2),
                swatches = PastelDark.avatars,
                outgoing = PastelDark.outgoing,
                onOutgoing = PastelDark.onOutgoing,
                incoming = PastelDark.incoming,
                onIncoming = PastelDark.onIncoming,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PaletteThemeCard(
    palette: GoghPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val pastel = palette.toPastel()
    val ink = palette.contrastInk()
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = ink.copy(alpha = if (selected) 0.9f else 0.28f),
                shape = shape,
            )
            .background(palette.themeWash().brush())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TitleRow(
            label = palette.name,
            detail = null,
            color = ink,
            selected = selected,
        )
        Swatches(palette.ansi.drop(1).take(8), ink)
        BubbleRow(
            outgoing = pastel.outgoing,
            onOutgoing = pastel.onOutgoing,
            outgoingText = palette.previewOutgoing,
            incoming = pastel.incoming,
            onIncoming = pastel.onIncoming,
            incomingText = palette.previewIncoming,
        )
    }
}

@Composable
private fun TitleRow(label: String, detail: String?, color: Color, selected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.copy(alpha = 0.72f),
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.theme_selected),
                tint = color,
            )
        }
    }
}

@Composable
private fun Swatches(colors: List<Color>, ring: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, ring.copy(alpha = 0.25f), CircleShape),
            )
        }
    }
}

@Composable
private fun BubbleRow(
    outgoing: Color,
    onOutgoing: Color,
    outgoingText: String,
    incoming: Color,
    onIncoming: Color,
    incomingText: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SampleBubble(outgoingText, outgoing, onOutgoing)
        SampleBubble(incomingText, incoming, onIncoming)
    }
}

private sealed interface ThemeCard {
    val key: String

    data object System : ThemeCard {
        override val key: String = "system"
    }

    data class Installed(val theme: InstalledTheme) : ThemeCard {
        override val key: String = theme.key
    }
}

private fun themeCards(catalog: List<InstalledTheme>, systemLabel: String): List<ThemeCard> {
    val installed = catalog.map { ThemeCard.Installed(it) }
    return (installed + ThemeCard.System).sortedBy { card ->
        when (card) {
            ThemeCard.System -> systemLabel
            is ThemeCard.Installed -> card.theme.name
        }.lowercase()
    }
}

@Composable
private fun SampleBubble(text: String, background: Color, content: Color) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun MiniPane(
    wash: ThemeWash,
    foreground: Color,
    swatches: List<Color>,
    outgoing: Color,
    onOutgoing: Color,
    incoming: Color,
    onIncoming: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(wash.brush())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Swatches(swatches.take(6), foreground)
        SampleBubble("Hi", outgoing, onOutgoing)
        SampleBubble("Hello", incoming, onIncoming)
    }
}
