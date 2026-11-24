package com.zirakusa.tienda

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.screens.AppsScreen
import com.zirakusa.tienda.ui.theme.StoreTheme

/** Creates the state with the real engine and paints «Apps». Coming back (Settings, an install) reads everything again. */
class MainActivity : ComponentActivity() {
    private lateinit var state: StoreState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val engine = AndroidEngine(applicationContext, start = ::launch)
        engine.cleanUp()
        state = StoreState(engine, lifecycleScope, packageName)
        state.start()
        setContent { StoreTheme { AppsScreen(state) } }
    }

    override fun onResume() {
        super.onResume()
        if (::state.isInitialized) state.recheck()
    }

    private fun launch(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }
}
