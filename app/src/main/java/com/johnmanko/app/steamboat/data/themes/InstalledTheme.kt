package com.johnmanko.app.steamboat.data.themes

data class InstalledTheme(
    val bundleId: String,
    val themeId: String,
    val version: Int,
    val name: String,
    val background: String,
    val foreground: String,
    val cursor: String,
    val colors: List<String>,
    val previewOutgoing: String,
    val previewIncoming: String,
    val source: String,
    val framework: Int,
) {
    val key: String get() = ThemeKey(bundleId, themeId).stored()
}

sealed interface ActiveTheme {
    data object System : ActiveTheme

    data class Palette(val theme: InstalledTheme) : ActiveTheme
}
