package com.zirakusa.tienda

import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.ui.CatalogError
import com.zirakusa.tienda.ui.Failure
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.data.MapValues
import com.zirakusa.tienda.data.SettingsRules
import com.zirakusa.tienda.data.StoreSettings
import com.zirakusa.tienda.data.ThemeChoice
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StoreStateTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test fun updateAllGoesOneByOneWithTiendaLast() {
        val engine = FakeEngine()
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        assertEquals(3, state.updates)
        state.updateAll()
        assertEquals(listOf("install reloj", "install notas", "install tienda"), engine.log)
        assertEquals(0, state.updates)
        assertFalse(state.busy)
        assertNull(state.failure)
    }

    @Test fun aFailureIsShownAndTheRestStillUpdates() {
        val engine = FakeEngine(failWith = mapOf("com.zirakusa.reloj" to Failure(Problem.WRONG_SIGNATURE)))
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        state.updateAll()
        assertEquals(listOf("install reloj", "install notas", "install tienda"), engine.log)
        assertEquals("Reloj", state.failure?.name)
        assertEquals(Problem.WRONG_SIGNATURE, state.failure?.failure?.problem)
        assertEquals(RowState.UPDATE, state.rows.first { it.app.id == "reloj" }.state)
    }

    @Test fun buttonsInstallUpdateOrOpen() {
        val engine = FakeEngine()
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        state.act(state.rows.first { it.app.id == "clima" })
        state.act(state.rows.first { it.app.id == "qr" })
        assertEquals(listOf("open com.zirakusa.clima", "install qr"), engine.log)
        assertEquals(RowState.UP_TO_DATE, state.rows.first { it.app.id == "qr" }.state)
        state.toggleNotes(state.rows[1]); assertEquals("com.zirakusa.reloj", state.expanded)
        state.toggleNotes(state.rows[1]); assertNull(state.expanded)
    }

    @Test fun withoutThePermissionNothingInstalls() {
        val engine = FakeEngine(allowed = false)
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        assertFalse(state.canInstall)
        state.updateAll()
        assertEquals(emptyList<String>(), engine.log)
        state.allowInstalls()
        assertEquals(listOf("allow"), engine.log)
    }

    @Test fun offlineShowsTheCatalogError() {
        val state = StoreState(FakeEngine(offline = true), scope, "com.zirakusa.tienda").apply { start() }
        assertEquals(CatalogError.NETWORK, state.catalogError)
        assertEquals(0, state.rows.size)
        assertFalse(state.loading)
    }

    @Test fun settingsAreSavedAndFallBackToDefaults() {
        val values = MapValues()
        val fresh = StoreSettings(values)
        assertEquals(ThemeChoice.SYSTEM, fresh.theme); assertFalse(fresh.wifiOnly); assertTrue(fresh.showNotInstalled); assertTrue(fresh.checkOnOpen)
        val state = StoreState(FakeEngine(), scope, "com.zirakusa.tienda", fresh)
        state.changeTheme(ThemeChoice.BLACK); state.changeWifiOnly(true); state.changeShowNotInstalled(false); state.changeCheckOnOpen(false)
        val again = StoreSettings(values)
        assertEquals(ThemeChoice.BLACK, again.theme); assertTrue(again.wifiOnly); assertFalse(again.showNotInstalled); assertFalse(again.checkOnOpen)
        values.put("theme", "PURPLE"); values.put("wifi_only", "maybe")
        assertEquals(ThemeChoice.SYSTEM, StoreSettings(values).theme); assertFalse(StoreSettings(values).wifiOnly)
    }

    @Test fun wifiOnlyStopsDownloadsOnMobileData() {
        val engine = FakeEngine(wifi = false)
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        state.changeWifiOnly(true)
        state.updateAll()
        assertTrue(state.wifiNeeded); assertEquals(emptyList<String>(), engine.log); assertEquals(3, state.updates)
        engine.wifi = true
        state.updateAll()
        assertFalse(state.wifiNeeded); assertEquals(0, state.updates)
    }

    @Test fun hidingNotInstalledAndCheckingOnOpen() {
        val engine = FakeEngine()
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        val all = state.visibleRows.size
        state.changeShowNotInstalled(false)
        assertTrue(state.visibleRows.size < all)
        assertTrue(state.visibleRows.none { it.state == RowState.NOT_INSTALLED })
        assertEquals(state.rows.filter { it.state != RowState.NOT_INSTALLED }, SettingsRules.visible(state.rows, false))
        // «Buscar novedades al abrir» off: with nothing saved it still loads once, so the list is never empty for no reason.
        val off = StoreSettings(MapValues()).apply { checkOnOpen = false }
        assertEquals(state.rows.size, StoreState(FakeEngine(), scope, "com.zirakusa.tienda", off).apply { start() }.rows.size)
    }

    @Test fun aboutShowsVersionAndFingerprint() {
        val state = StoreState(FakeEngine(), scope, "com.zirakusa.tienda")
        assertEquals("0.1.0", state.version)
        assertEquals("34:B8:87:16:BA:31:A8:CA:17:06:24:C1:C0:09:C1:1D:D2:3E:18:21:48:73:1F:08:89:C9:84:04:B3:41:82:D4", state.fingerprint)
        assertNull(SettingsRules.fingerprint("not hex")); assertNull(SettingsRules.fingerprint(null)); assertNull(SettingsRules.fingerprint("ab"))
    }
}
