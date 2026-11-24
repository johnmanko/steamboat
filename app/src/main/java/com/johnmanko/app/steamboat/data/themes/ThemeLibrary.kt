package com.johnmanko.app.steamboat.data.themes

import android.content.Context
import androidx.room3.withWriteTransaction
import com.johnmanko.app.steamboat.data.prefs.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/**
 * Themes the app can show. The pack in assets is installed when this process
 * starts, which covers a fresh install and an update. A purchased pack uses
 * [install] after its signature has been checked for the buying account.
 */
class ThemeLibrary(
    private val context: Context,
    private val database: ThemeDatabase,
    private val preferences: ThemePreferences,
) {
    private val themes = MutableStateFlow<List<InstalledTheme>>(emptyList())
    private val active = MutableStateFlow<ActiveTheme>(ActiveTheme.System)

    val catalog: StateFlow<List<InstalledTheme>> = themes
    val selection: StateFlow<ActiveTheme> = active

    suspend fun ensureBundledInstalled() {
        val bytes = context.assets.open(ThemeBundles.ASSET).use { it.readBytes() }
        install(bytes, ThemeSource.APP, account = null, verifier = BundledThemeVerifier)
        val rows = database.themes().listAll()
        val available = rows.map { ThemeKey(it.bundleId, it.themeId) }.toSet()
        val stored = preferences.stored()
        val keep = resolvedThemeId(stored, available, ThemeKey.defaultStored())
        if (keep != stored) preferences.setTheme(keep)
        publish(rows, keep)
    }

    suspend fun watch() {
        combine(preferences.themeId, database.themes().observeAll()) { id, rows ->
            id to rows
        }.collect { (id, rows) ->
            publish(rows, id)
        }
    }

    suspend fun select(theme: InstalledTheme) {
        preferences.setTheme(theme.key)
    }

    suspend fun selectSystem() {
        preferences.setTheme(ThemeKey.SYSTEM)
    }

    suspend fun install(
        bytes: ByteArray,
        source: ThemeSource,
        account: String?,
        verifier: ThemeBundleVerifier,
    ) {
        val bundle = ThemeBundleJson.parse(bytes.decodeToString())
        require(verifier.verify(bytes, bundle)) { "Theme bundle ${bundle.id} was rejected" }
        when (source) {
            ThemeSource.APP -> {
                require(account == null) { "An included theme pack has no account" }
                require(bundle.id == ThemeBundles.BUNDLED_ID) { "The included pack id is reserved" }
            }
            ThemeSource.PAID -> {
                require(!account.isNullOrBlank()) { "A paid theme pack needs the buying account" }
                require(bundle.id != ThemeBundles.BUNDLED_ID) { "The included pack id is reserved" }
                require(!bundle.signature.isNullOrBlank()) { "A paid theme pack needs a signature" }
            }
        }
        database.withWriteTransaction {
            val installedVersion = database.themes().bundleVersion(bundle.id)
            val assetIds = bundle.themes.map { it.id }
            if (source == ThemeSource.APP) {
                if (!ThemeFramework.accepts(bundle.framework)) return@withWriteTransaction
                val installedIds = database.themes().themeIds(bundle.id, source.name).toSet()
                val plan = appThemePlan(installedVersion, bundle.version, installedIds, assetIds.toSet())
                if (plan.upsert.isNotEmpty()) {
                    database.themes().upsertBundle(bundle.toEntity(source, account))
                    database.themes().upsertThemes(
                        bundle.themes.filter { it.id in plan.upsert }.map { it.toEntity(bundle, source) },
                    )
                }
                if (assetIds.isEmpty()) {
                    database.themes().deleteSource(bundle.id, source.name)
                } else {
                    database.themes().deleteSourceExcept(bundle.id, source.name, assetIds)
                }
            } else if (shouldInstall(source, installedVersion, bundle.version)) {
                database.themes().upsertBundle(bundle.toEntity(source, account))
                if (assetIds.isEmpty()) {
                    database.themes().deleteSource(bundle.id, source.name)
                } else {
                    database.themes().deleteSourceExcept(bundle.id, source.name, assetIds)
                }
                database.themes().upsertThemes(bundle.themes.map { it.toEntity(bundle, source) })
            }
        }
    }

    private fun publish(rows: List<ThemeEntity>, stored: String) {
        themes.value = rows.filter { ThemeFramework.accepts(it.framework) }.map { it.toInstalled() }
        active.value = resolve(stored, rows)
    }

    private fun resolve(stored: String, rows: List<ThemeEntity>): ActiveTheme {
        if (stored != ThemeKey.SYSTEM) {
            val key = ThemeKey.parse(stored)
            val row = rows.firstOrNull { it.bundleId == key.bundleId && it.themeId == key.themeId }
            if (row != null && ThemeFramework.accepts(row.framework)) {
                return ActiveTheme.Palette(row.toInstalled())
            }
        }
        val fallback = rows.firstOrNull {
            it.bundleId == ThemeBundles.BUNDLED_ID &&
                it.themeId == ThemeKey.STEAMBOAT &&
                ThemeFramework.accepts(it.framework)
        }
        return if (stored == ThemeKey.SYSTEM || fallback == null) {
            ActiveTheme.System
        } else {
            ActiveTheme.Palette(fallback.toInstalled())
        }
    }

    private fun ThemeBundle.toEntity(source: ThemeSource, account: String?) = ThemeBundleEntity(
        id = id,
        version = version,
        framework = framework,
        name = name,
        origin = source.name,
        account = account,
        installedAt = System.currentTimeMillis(),
    )

    private fun ThemeSpec.toEntity(bundle: ThemeBundle, source: ThemeSource) = ThemeEntity(
        bundleId = bundle.id,
        themeId = id,
        version = version,
        name = name,
        background = background,
        foreground = foreground,
        cursor = cursor,
        colors = colors.joinToString(" "),
        previewOutgoing = previewOutgoing,
        previewIncoming = previewIncoming,
        source = source.name,
        framework = bundle.framework,
    )
}
