package com.johnmanko.app.steamboat.data.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeBundleTest {
    @Test
    fun bundledAssetInstallsTheShippedPalettes() {
        val bundle = ThemeBundleJson.parse(bundledAsset())
        assertEquals(ThemeBundles.BUNDLED_ID, bundle.id)
        assertEquals(1, bundle.version)
        assertEquals(ThemeFramework.VERSION, bundle.framework)
        assertEquals("Steamboat", bundle.name)
        assertNull(bundle.signature)
        assertEquals(36, bundle.themes.size)
        assertEquals(bundle.themes.map { it.id }.distinct().size, bundle.themes.size)
        val steamboat = bundle.themes.first { it.id == "Steamboat" }
        assertEquals("Steamboat", steamboat.name)
        assertEquals(1, steamboat.version)
        assertEquals("#08144B", steamboat.background)
        assertEquals("#D2D4DB", steamboat.foreground)
        assertEquals("#FF0BC4", steamboat.cursor)
        assertEquals("#030E37", steamboat.colors.first())
        assertEquals(16, steamboat.colors.size)
        assertEquals("Hello", steamboat.previewOutgoing)
        assertEquals("Hi", steamboat.previewIncoming)
        assertTrue(BundledThemeVerifier.verify(bundledAsset().encodeToByteArray(), bundle))
    }

    @Test
    fun legacySelectionStillNamesTheBundledTheme() {
        assertEquals(
            ThemeKey(ThemeBundles.BUNDLED_ID, "Steamboat"),
            ThemeKey.parse("Steamboat"),
        )
        assertEquals(
            "com.johnmanko.app.steamboat.themes.purchased/Harbor",
            ThemeKey("com.johnmanko.app.steamboat.themes.purchased", "Harbor").stored(),
        )
    }

    @Test
    fun bundledVerifierRejectsASignedPack() {
        val json = pack(signature = "\"signature\": \"abc\",")
        val bundle = ThemeBundleJson.parse(json)
        assertFalse(BundledThemeVerifier.verify(json.encodeToByteArray(), bundle))
    }

    @Test
    fun aThemeNeedsSixteenColors() {
        val broken = pack().replace(sampleColors(), """["#08144B"]""")
        assertThrows(IllegalArgumentException::class.java) {
            ThemeBundleJson.parse(broken)
        }
    }

    @Test
    fun firstInstallSelectsSteamboatAndUpgradesKeepASavedTheme() {
        val available = setOf(ThemeKey(ThemeBundles.BUNDLED_ID, "Steamboat"), ThemeKey(ThemeBundles.BUNDLED_ID, "Dracula"))
        val fallback = ThemeKey.defaultStored()
        assertEquals(fallback, resolvedThemeId(null, available, fallback))
        assertEquals("System", resolvedThemeId("System", available, fallback))
        assertEquals("Steamboat", resolvedThemeId("Steamboat", available, fallback))
        assertEquals(
            "com.johnmanko.app.steamboat.themes.bundled/Dracula",
            resolvedThemeId("com.johnmanko.app.steamboat.themes.bundled/Dracula", available, fallback),
        )
        assertEquals(fallback, resolvedThemeId("com.example.themes/Retired", available, fallback))
    }

    @Test
    fun installFollowsThePackVersion() {
        assertTrue(shouldInstall(ThemeSource.APP, null, 1))
        assertFalse(shouldInstall(ThemeSource.APP, 1, 1))
        assertTrue(shouldInstall(ThemeSource.APP, 1, 2))
        assertTrue(shouldInstall(ThemeSource.APP, 2, 1))
        assertFalse(shouldInstall(ThemeSource.PAID, 2, 1))
        assertTrue(shouldInstall(ThemeSource.PAID, 1, 2))
    }

    @Test
    fun includedThemesDropWhateverTheAssetNoLongerLists() {
        val sameVersion = appThemePlan(
            installedVersion = 1,
            bundleVersion = 1,
            installedIds = setOf("Steamboat", "Dracula"),
            assetIds = setOf("Steamboat"),
        )
        assertEquals(setOf("Dracula"), sameVersion.remove)
        assertTrue(sameVersion.upsert.isEmpty())
        val changed = appThemePlan(1, 2, setOf("Steamboat"), setOf("Steamboat", "Dracula"))
        assertEquals(setOf("Steamboat", "Dracula"), changed.upsert)
        assertFalse(ThemeFramework.accepts(2))
    }

    private fun bundledAsset(): String {
        val file = listOf(
            File("src/main/assets/themes/bundled.json"),
            File("app/src/main/assets/themes/bundled.json"),
        ).first { it.exists() }
        return file.readText()
    }

    private fun pack(signature: String = ""): String = """
        {
          "id": "com.johnmanko.app.steamboat.themes.bundled",
          "version": 1,
          "framework": 1,
          "name": "Steamboat",
          $signature
          "themes": [
            {
              "id": "Steamboat",
              "version": 1,
              "name": "Steamboat",
              "background": "#08144B",
              "foreground": "#D2D4DB",
              "cursor": "#FF0BC4",
              "colors": ${sampleColors()},
              "previewOutgoing": "Hello",
              "previewIncoming": "Hi"
            }
          ]
        }
    """.trimIndent()

    private fun sampleColors(): String =
        """["#030E37", "#D30CA8", "#0556A9", "#E048DE", "#0F5FCE", "#FF0BC4", "#0A99E9", "#9BA4BE", "#2A155B", "#F80CBB", "#112D95", "#D92DCF", "#5E1592", "#A10E96", "#0A1A69", "#D2D4DB"]"""
}
