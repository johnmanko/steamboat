package com.johnmanko.app.steamboat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

internal fun GoghPalette.isDarkUi(): Boolean = background.luminance() < 0.5f

internal fun GoghPalette.materialScheme(): ColorScheme {
    val darkUi = isDarkUi()
    val accent = accentColor()
    val secondary = ansi.drop(1).take(6).firstOrNull { it != accent } ?: ansi[6]
    val tertiary = ansi[2]
    val lift: (Float) -> Color = { amount ->
        if (darkUi) background.mix(Color.White, amount) else background.mix(Color.Black, amount)
    }
    val primaryContainer = background.mix(accent, if (darkUi) 0.34f else 0.22f)
    val secondaryContainer = background.mix(secondary, if (darkUi) 0.30f else 0.18f)
    val tertiaryContainer = background.mix(tertiary, if (darkUi) 0.30f else 0.18f)
    val error = ansi[1]
    val errorContainer = background.mix(error, if (darkUi) 0.38f else 0.20f)
    val onSurfaceVariant = background.mix(foreground, 0.72f)
    return if (darkUi) {
        darkColorScheme(
            primary = accent,
            onPrimary = accent.onContent(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primaryContainer.onContent(),
            inversePrimary = accent,
            secondary = secondary,
            onSecondary = secondary.onContent(),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryContainer.onContent(),
            tertiary = tertiary,
            onTertiary = tertiary.onContent(),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiaryContainer.onContent(),
            background = background,
            onBackground = foreground,
            surface = lift(0.04f),
            onSurface = foreground,
            surfaceVariant = lift(0.12f),
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = accent,
            inverseSurface = foreground,
            inverseOnSurface = background,
            error = error,
            onError = error.onContent(),
            errorContainer = errorContainer,
            onErrorContainer = errorContainer.onContent(),
            outline = background.mix(foreground, 0.38f),
            outlineVariant = background.mix(foreground, 0.22f),
            scrim = Color.Black,
            surfaceBright = lift(0.16f),
            surfaceDim = background,
            surfaceContainerLowest = background,
            surfaceContainerLow = lift(0.04f),
            surfaceContainer = lift(0.07f),
            surfaceContainerHigh = lift(0.10f),
            surfaceContainerHighest = lift(0.14f),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = accent.onContent(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primaryContainer.onContent(),
            inversePrimary = accent,
            secondary = secondary,
            onSecondary = secondary.onContent(),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = secondaryContainer.onContent(),
            tertiary = tertiary,
            onTertiary = tertiary.onContent(),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tertiaryContainer.onContent(),
            background = background,
            onBackground = foreground,
            surface = lift(0.02f),
            onSurface = foreground,
            surfaceVariant = lift(0.07f),
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = accent,
            inverseSurface = foreground,
            inverseOnSurface = background,
            error = error,
            onError = error.onContent(),
            errorContainer = errorContainer,
            onErrorContainer = errorContainer.onContent(),
            outline = background.mix(foreground, 0.28f),
            outlineVariant = background.mix(foreground, 0.14f),
            scrim = Color.Black,
            surfaceBright = background,
            surfaceDim = lift(0.08f),
            surfaceContainerLowest = Color.White.mix(background, 0.4f),
            surfaceContainerLow = lift(0.02f),
            surfaceContainer = lift(0.045f),
            surfaceContainerHigh = lift(0.07f),
            surfaceContainerHighest = lift(0.10f),
        )
    }
}

internal fun GoghPalette.themeWash(): ThemeWash {
    val accent = accentColor()
    val sky = ansi.drop(1).take(6).firstOrNull { it != accent && it.chroma() > 0.08f } ?: cursor
    return if (isDarkUi()) {
        ThemeWash(
            top = background.mix(sky, 0.36f).mix(Color.White, 0.07f),
            middle = background,
            bottom = background.mix(Color.Black, 0.38f).mix(accent, 0.24f),
        )
    } else {
        ThemeWash(
            top = background.mix(Color.White, 0.7f).mix(sky, 0.18f),
            middle = background,
            bottom = background.mix(accent, 0.30f),
        )
    }
}

internal fun GoghPalette.textCandidates(): List<Color> {
    val rest = ansi.asSequence()
        .filter { it != foreground && it != cursor && it.chroma() >= 0.15f }
        .sortedByDescending { it.chroma() }
        .toList()
    return (listOf(foreground, cursor) + rest).distinct()
}

internal fun GoghPalette.contrastInk(wash: ThemeWash = themeWash()): Color =
    readableInk(textCandidates(), wash)

internal fun readableInk(candidates: List<Color>, wash: ThemeWash): Color {
    val surfaces = listOf(wash.top, wash.middle, wash.bottom)
    candidates.firstOrNull { it.chroma() >= 0.15f && it.readsOn(surfaces, wash.middle) }
        ?.let { return it }
    candidates.firstOrNull { it.readsOn(surfaces, wash.middle) }?.let { return it }
    return (candidates + LightInk + DarkInk).maxBy { candidate ->
        surfaces.minOf { contrastRatio(candidate, it) }
    }
}

internal fun ColorScheme.withContrastText(wash: ThemeWash, candidates: List<Color>): ColorScheme {
    val surfaces = listOf(wash.top, wash.middle, wash.bottom)
    val ink = readableInk(candidates, wash)
    return copy(
        onBackground = ink,
        onSurface = ink,
        onSurfaceVariant = ink.softOn(wash.middle, surfaces),
    )
}

private val LightInk = Color(0xFFF7F4EF)
private val DarkInk = Color(0xFF1C1917)

private fun Color.readsOn(surfaces: List<Color>, middle: Color): Boolean =
    contrastRatio(this, middle) >= 4.5f &&
        surfaces.minOf { contrastRatio(this, it) } >= 3f

private fun Color.softOn(middle: Color, surfaces: List<Color>): Color {
    val softened = mix(middle, 0.32f)
    val softenedContrast = surfaces.minOf { contrastRatio(softened, it) }
    return if (softenedContrast >= 3.2f) softened else this
}

internal fun GoghPalette.toPastel(): PastelTokens {
    val darkUi = isDarkUi()
    val accent = accentColor()
    val incomingTint = ansi.drop(1).take(6).firstOrNull { it != accent } ?: ansi[4]
    val outgoing = if (darkUi) background.mix(accent, 0.72f) else background.mix(accent, 0.78f)
    val incoming = if (darkUi) background.mix(incomingTint, 0.38f) else background.mix(incomingTint, 0.22f)
    return PastelTokens(
        outgoing = outgoing,
        onOutgoing = outgoing.onContent(),
        incoming = incoming,
        onIncoming = incoming.onContent(),
        avatars = ansi.drop(1).take(6).map { color ->
            background.mix(color, if (darkUi) 0.62f else 0.55f)
        },
    )
}

private fun GoghPalette.accentColor(): Color {
    return ansi.drop(1).take(6).maxBy { color ->
        val ratio = contrastRatio(color, background)
        color.chroma() * if (ratio >= 1.6f) 1f else 0.2f
    }
}

fun Color.onContent(): Color =
    if (contrastRatio(LightInk, this) >= contrastRatio(DarkInk, this)) LightInk else DarkInk

/** Push a theme color slightly off the page color so a name reads stronger. */
fun Color.strongerOn(field: Color, amount: Float = 0.2f): Color {
    val toward = if (field.luminance() < 0.5f) Color.White else Color.Black
    return mix(toward, amount)
}

/** Pull a theme color toward the page so an excerpt or time sits back. */
fun Color.quieterOn(field: Color, amount: Float = 0.4f): Color = mix(field, amount)

private fun Color.mix(other: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * t,
        green = green + (other.green - green) * t,
        blue = blue + (other.blue - blue) * t,
        alpha = 1f,
    )
}

private fun Color.chroma(): Float = max(red, max(green, blue)) - min(red, min(green, blue))

private fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance() + 0.05f
    val l2 = b.luminance() + 0.05f
    return if (l1 > l2) l1 / l2 else l2 / l1
}
