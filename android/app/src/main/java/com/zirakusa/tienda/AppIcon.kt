package com.zirakusa.tienda

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Store's launcher icons, «Estrella» by default. Each is an `activity-alias` in the manifest; exactly one is enabled.
 * Alias names never change (Android keeps the enabled state by name).
 */
enum class AppIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    SPARK("Home", R.string.icon_spark, R.mipmap.ic_launcher_spark_foreground, 0xFF0A92F2),
    ASTRONAUT("HomeAstronauta", R.string.icon_astronaut, R.mipmap.ic_launcher_astronaut_foreground, 0xFF0381CB),
    DISC("HomeDisco", R.string.icon_disc, R.mipmap.ic_launcher_disc_foreground, 0xFF021B56),
    SATURN("HomeSaturno", R.string.icon_saturn, R.mipmap.ic_launcher_saturn_foreground, 0xFF1388D9),
    BEAR("HomeOsa", R.string.icon_bear, R.mipmap.ic_launcher_bear_foreground, 0xFF042969);

    private fun component(context: Context) = ComponentName(context.packageName, "com.zirakusa.tienda.$alias")
    fun isEnabled(context: Context): Boolean = when (context.packageManager.getComponentEnabledSetting(component(context))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> this == DEFAULT              // as the manifest says
        else -> false
    }

    companion object {
        val DEFAULT = SPARK
        var current by mutableStateOf(DEFAULT); private set

        /** Reads the enabled one; if none is (an odd update), the default comes back so Store never loses its icon. */
        fun load(context: Context) { val on = entries.firstOrNull { it.isEnabled(context) }; if (on == null) apply(context, DEFAULT) else current = on }

        /** Enables [icon] first and then disables the others, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
        }
    }
}
