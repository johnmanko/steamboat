package com.johnmanko.app.steamboat.data.themes

/**
 * A versioned package of themes. The copy shipped in the app and a theme pack
 * downloaded later use this same shape. [signature] is empty for the bundled
 * pack. A purchased pack carries a signature over the file bytes for the
 * account that bought it.
 */
data class ThemeBundle(
    val id: String,
    val version: Int,
    val framework: Int,
    val name: String,
    val signature: String?,
    val themes: List<ThemeSpec>,
)

data class ThemeSpec(
    val id: String,
    val version: Int,
    val name: String,
    val background: String,
    val foreground: String,
    val cursor: String,
    val colors: List<String>,
    val previewOutgoing: String,
    val previewIncoming: String,
)

enum class ThemeSource {
    APP,
    PAID,
}

object ThemeFramework {
    const val VERSION = 1

    fun accepts(framework: Int): Boolean = framework == VERSION
}

data class AppThemePlan(
    val remove: Set<String>,
    val upsert: Set<String>,
)

internal fun appThemePlan(
    installedVersion: Int?,
    bundleVersion: Int,
    installedIds: Set<String>,
    assetIds: Set<String>,
): AppThemePlan {
    val refresh = installedVersion != bundleVersion
    return AppThemePlan(
        remove = installedIds - assetIds,
        upsert = if (refresh) assetIds else assetIds - installedIds,
    )
}

object ThemeBundles {
    const val BUNDLED_ID = "com.johnmanko.app.steamboat.themes.bundled"
    const val ASSET = "themes/bundled.json"
}

data class ThemeKey(val bundleId: String, val themeId: String) {
    fun stored(): String = "$bundleId/$themeId"

    companion object {
        const val SYSTEM = "System"
        const val STEAMBOAT = "Steamboat"

        fun defaultStored(): String = ThemeKey(ThemeBundles.BUNDLED_ID, STEAMBOAT).stored()

        fun parse(stored: String): ThemeKey {
            val slash = stored.indexOf('/')
            if (slash <= 0 || slash == stored.lastIndex) {
                return ThemeKey(ThemeBundles.BUNDLED_ID, stored)
            }
            return ThemeKey(stored.substring(0, slash), stored.substring(slash + 1))
        }
    }
}

fun interface ThemeBundleVerifier {
    fun verify(bytes: ByteArray, bundle: ThemeBundle): Boolean
}

object BundledThemeVerifier : ThemeBundleVerifier {
    override fun verify(bytes: ByteArray, bundle: ThemeBundle): Boolean {
        return bytes.isNotEmpty() && bundle.id == ThemeBundles.BUNDLED_ID && bundle.signature == null
    }
}

internal fun resolvedThemeId(stored: String?, available: Set<ThemeKey>, defaultId: String): String {
    if (stored == null) return defaultId
    if (stored == ThemeKey.SYSTEM) return stored
    return if (ThemeKey.parse(stored) in available) stored else defaultId
}

internal fun shouldInstall(source: ThemeSource, installedVersion: Int?, bundleVersion: Int): Boolean =
    when (source) {
        ThemeSource.APP -> installedVersion != bundleVersion
        ThemeSource.PAID -> installedVersion == null || bundleVersion > installedVersion
    }
