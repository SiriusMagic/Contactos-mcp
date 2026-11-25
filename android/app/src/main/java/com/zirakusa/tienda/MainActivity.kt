package com.zirakusa.tienda

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.edit
import com.zirakusa.tienda.data.KeyValues
import com.zirakusa.tienda.data.StoreSettings
import com.zirakusa.tienda.data.ThemeChoice
import com.zirakusa.tienda.ui.Page
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.screens.AppsScreen
import com.zirakusa.tienda.ui.screens.IconCard
import com.zirakusa.tienda.ui.screens.SettingsScreen
import com.zirakusa.tienda.ui.theme.StoreTheme

/** Creates the state with the real engine and its settings, and paints «Apps» or «Ajustes». Coming back (Settings, an install) reads everything again. */
class MainActivity : ComponentActivity() {
    private lateinit var state: StoreState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val engine = AndroidEngine(applicationContext, start = ::launch)
        engine.cleanUp()
        val prefs = getSharedPreferences("ajustes", MODE_PRIVATE)
        val values = object : KeyValues {
            override fun get(key: String) = prefs.getString(key, null)
            override fun put(key: String, value: String) = prefs.edit { putString(key, value) }
        }
        state = StoreState(engine, lifecycleScope, packageName, StoreSettings(values))
        state.start()
        AppIcon.load(this)
        setContent {
            val dark = when (state.theme) { ThemeChoice.SYSTEM -> isSystemInDarkTheme(); ThemeChoice.LIGHT -> false; ThemeChoice.BLACK -> true }
            StoreTheme(dark) {
                BackHandler(state.page == Page.SETTINGS) { state.page = Page.APPS }
                if (state.page == Page.SETTINGS) SettingsScreen(state) { IconCard(AppIcon.current) { AppIcon.apply(this, it) } } else AppsScreen(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::state.isInitialized) state.recheck()
    }

    private fun launch(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }
}
