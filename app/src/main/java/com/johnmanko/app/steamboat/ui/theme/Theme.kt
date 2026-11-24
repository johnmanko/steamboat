package com.johnmanko.app.steamboat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.johnmanko.app.steamboat.data.themes.ActiveTheme

val LocalPastel = staticCompositionLocalOf { PastelLight }

val LocalThemeWash = staticCompositionLocalOf { SystemLightWash }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun washTopBarColors(): TopAppBarColors {
    val ink = MaterialTheme.colorScheme.onBackground
    return TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent,
        titleContentColor = ink,
        navigationIconContentColor = ink,
        actionIconContentColor = ink,
    )
}

@Composable
fun washContentColor(): Color = MaterialTheme.colorScheme.onBackground

private val LightScheme = lightColorScheme(
    primary = Rose,
    onPrimary = Color(0xFFFFF8F9),
    primaryContainer = RoseWash,
    onPrimaryContainer = RoseDeep,
    inversePrimary = RoseWash,
    secondary = Sea,
    onSecondary = Color(0xFFF7FBFF),
    secondaryContainer = Mist,
    onSecondaryContainer = SeaInk,
    tertiary = Sage,
    onTertiary = Color(0xFFF4FFFB),
    tertiaryContainer = Mint,
    onTertiaryContainer = SageInk,
    background = Color(0xFFFBF7F8),
    onBackground = Ink,
    surface = Color(0xFFFFF9FB),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3E7EC),
    onSurfaceVariant = Color(0xFF655860),
    surfaceTint = Rose,
    inverseSurface = Color(0xFF32282E),
    inverseOnSurface = Color(0xFFFBF7F8),
    error = Color(0xFFB85C6A),
    onError = Color(0xFFFFF7F8),
    errorContainer = Color(0xFFFFD9DF),
    onErrorContainer = Color(0xFF4A222B),
    outline = Color(0xFFE4D3DA),
    outlineVariant = Color(0xFFF0E2E8),
    scrim = Color(0xFF2C2428),
    surfaceBright = Color(0xFFFFF9FB),
    surfaceDim = Color(0xFFE7DCE1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF4F6),
    surfaceContainer = Color(0xFFF6EEF1),
    surfaceContainerHigh = Color(0xFFF1E6EB),
    surfaceContainerHighest = Color(0xFFEBDEE4),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF0B7C9),
    onPrimary = Color(0xFF45212F),
    primaryContainer = Color(0xFF5C3A4A),
    onPrimaryContainer = Color(0xFFFBE3EB),
    inversePrimary = Rose,
    secondary = Color(0xFFC5D9EE),
    onSecondary = Color(0xFF1B3144),
    secondaryContainer = Color(0xFF314556),
    onSecondaryContainer = Color(0xFFE7F1FA),
    tertiary = Color(0xFFB7E6D4),
    onTertiary = SageInk,
    tertiaryContainer = Color(0xFF2C5346),
    onTertiaryContainer = Color(0xFFDDF6EC),
    background = Color(0xFF1A161C),
    onBackground = Color(0xFFF6EEF2),
    surface = Color(0xFF221E26),
    onSurface = Color(0xFFF6EEF2),
    surfaceVariant = Color(0xFF3A3038),
    onSurfaceVariant = Color(0xFFE4D5DC),
    surfaceTint = Color(0xFFF0B7C9),
    inverseSurface = Color(0xFFF6EEF2),
    inverseOnSurface = Ink,
    error = Color(0xFFF0B4BE),
    onError = Color(0xFF4A222B),
    errorContainer = Color(0xFF6A303C),
    onErrorContainer = Color(0xFFFFD9DF),
    outline = Color(0xFF6E5C66),
    outlineVariant = Color(0xFF463842),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3340),
    surfaceDim = Color(0xFF141116),
    surfaceContainerLowest = Color(0xFF141116),
    surfaceContainerLow = Color(0xFF221C24),
    surfaceContainer = Color(0xFF2C2630),
    surfaceContainerHigh = Color(0xFF372F3A),
    surfaceContainerHighest = Color(0xFF423844),
)

@Composable
fun SteamboatTheme(
    theme: ActiveTheme,
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = (theme as? ActiveTheme.Palette)?.theme?.toPalette()
    val wash = palette?.themeWash() ?: if (systemDark) SystemDarkWash else SystemLightWash
    val inkCandidates = palette?.textCandidates()
        ?: listOf(if (systemDark) DarkScheme.onBackground else LightScheme.onBackground)
    val scheme: ColorScheme = (palette?.materialScheme() ?: if (systemDark) DarkScheme else LightScheme)
        .withContrastText(wash, inkCandidates)
    val pastel = palette?.toPastel() ?: if (systemDark) PastelDark else PastelLight
    val lightBars = palette?.let { !it.isDarkUi() } ?: !systemDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = lightBars
            controller.isAppearanceLightNavigationBars = lightBars
        }
    }
    CompositionLocalProvider(LocalPastel provides pastel, LocalThemeWash provides wash) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SteamboatTypography,
            content = content,
        )
    }
}
