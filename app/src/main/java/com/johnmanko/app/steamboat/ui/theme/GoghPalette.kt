package com.johnmanko.app.steamboat.ui.theme

import androidx.compose.ui.graphics.Color
import com.johnmanko.app.steamboat.data.themes.InstalledTheme

internal data class GoghPalette(
    val id: String,
    val name: String,
    val background: Color,
    val foreground: Color,
    val cursor: Color,
    val ansi: List<Color>,
    val previewOutgoing: String,
    val previewIncoming: String,
)

internal fun InstalledTheme.toPalette(): GoghPalette = GoghPalette(
    id = key,
    name = name,
    background = themeColor(background),
    foreground = themeColor(foreground),
    cursor = themeColor(cursor),
    ansi = colors.map(::themeColor),
    previewOutgoing = previewOutgoing,
    previewIncoming = previewIncoming,
)

internal fun themeColor(hex: String): Color {
    val digits = hex.removePrefix("#")
    val rgb = digits.toLong(16)
    val argb = if (digits.length == 6) rgb or 0xFF000000 else rgb
    return Color(argb.toInt())
}
