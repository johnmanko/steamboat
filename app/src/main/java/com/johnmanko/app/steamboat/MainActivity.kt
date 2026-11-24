package com.johnmanko.app.steamboat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johnmanko.app.steamboat.SteamboatApp
import com.johnmanko.app.steamboat.ui.nav.SteamboatNavHost
import com.johnmanko.app.steamboat.ui.theme.LocalThemeWash
import com.johnmanko.app.steamboat.ui.theme.SteamboatTheme
import com.johnmanko.app.steamboat.ui.theme.brush

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SteamboatApp
        app.handleIntent(intent)
        setContent {
            val themes = app.graph.themes
            val theme by themes.selection.collectAsStateWithLifecycle(themes.selection.value)
            SteamboatTheme(theme = theme, systemDark = isSystemInDarkTheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalThemeWash.current.brush()),
                ) {
                    SteamboatNavHost(app.graph)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        (application as SteamboatApp).handleIntent(intent)
    }
}
