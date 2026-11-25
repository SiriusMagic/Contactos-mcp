package com.zirakusa.tienda

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Store's five launcher icons («Estrella» by default): one home-screen entry at a time, and never none. */
@RunWith(RobolectricTestRunner::class)
class AppIconTest {
    private val context = RuntimeEnvironment.getApplication()
    private fun entries() = context.packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName), 0)
        .map { it.activityInfo.name.substringAfterLast('.') }

    @Test fun freshInstallShowsTheStar() {
        assertEquals(listOf("Home"), entries())
        AppIcon.load(context); assertEquals(AppIcon.SPARK, AppIcon.current)
    }

    @Test fun oneIconAtATimeAndNeverNone() {
        AppIcon.entries.forEach { AppIcon.apply(context, it); assertEquals(listOf(it.alias), entries()); assertEquals(it, AppIcon.current) }
        AppIcon.entries.forEach { context.packageManager.setComponentEnabledSetting(ComponentName(context, "com.zirakusa.tienda.${it.alias}"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
        AppIcon.load(context)
        assertEquals(listOf("Home"), entries())
    }
}
