package com.johnmanko.app.steamboat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.johnmanko.app.steamboat.ui.format.initials
import com.johnmanko.app.steamboat.ui.theme.LocalPastel
import com.johnmanko.app.steamboat.ui.theme.onContent

@Composable
fun PastelAvatar(
    title: String,
    key: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val palette = LocalPastel.current.avatars
    val color = palette[(key.hashCode().toUInt() % palette.size.toUInt()).toInt()]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials(title),
            style = MaterialTheme.typography.titleMedium,
            color = color.onContent(),
        )
    }
}
