package com.johnmanko.app.steamboat.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

internal val Rose = Color(0xFFA85D78)
internal val RoseDeep = Color(0xFF4A2434)
internal val RoseWash = Color(0xFFF6D5E2)
internal val Ink = Color(0xFF2C2428)
internal val BlueWash = Color(0xFFE3EEF8)
internal val BlueInk = Color(0xFF24303A)
internal val Mist = Color(0xFFD7E8F7)
internal val Sea = Color(0xFF5E86AB)
internal val SeaInk = Color(0xFF1C3348)
internal val Mint = Color(0xFFD4F3E7)
internal val Sage = Color(0xFF4F917C)
internal val SageInk = Color(0xFF14382E)

data class ThemeWash(
    val top: Color,
    val middle: Color,
    val bottom: Color,
)

fun ThemeWash.brush(): Brush = Brush.linearGradient(listOf(top, middle, bottom))

val SystemLightWash = ThemeWash(
    top = Color(0xFFE4EEF8),
    middle = Color(0xFFFBF7F8),
    bottom = Color(0xFFF4C9DA),
)

val SystemDarkWash = ThemeWash(
    top = Color(0xFF243044),
    middle = Color(0xFF1A161C),
    bottom = Color(0xFF3C2230),
)

data class PastelTokens(
    val outgoing: Color,
    val onOutgoing: Color,
    val incoming: Color,
    val onIncoming: Color,
    val avatars: List<Color>,
)

val PastelLight = PastelTokens(
    outgoing = RoseWash,
    onOutgoing = Color(0xFF3A2430),
    incoming = BlueWash,
    onIncoming = BlueInk,
    avatars = listOf(
        Color(0xFFF6C1D0),
        Color(0xFFC9D7F2),
        Color(0xFFC9EBD9),
        Color(0xFFF8E1B8),
        Color(0xFFE3D4F5),
        Color(0xFFF7D2C4),
    ),
)

val PastelDark = PastelTokens(
    outgoing = Color(0xFF5C3A4A),
    onOutgoing = Color(0xFFFBEFF3),
    incoming = Color(0xFF314556),
    onIncoming = Color(0xFFEAF3FB),
    avatars = listOf(
        Color(0xFF6E4A5C),
        Color(0xFF3E5674),
        Color(0xFF3E6558),
        Color(0xFF6E5A3E),
        Color(0xFF56466E),
        Color(0xFF6E4E42),
    ),
)
