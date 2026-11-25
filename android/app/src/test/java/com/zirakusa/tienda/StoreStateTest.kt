package com.zirakusa.tienda

import com.zirakusa.tienda.data.Catalog
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

    @Test fun cancelStopsTheDownloadAndTheQueue() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val started = mutableListOf<String>()
        val slow = object : com.zirakusa.tienda.ui.Engine by FakeEngine() {
            override suspend fun install(app: com.zirakusa.tienda.data.CatalogApp, onJob: (com.zirakusa.tienda.ui.Phase, Float) -> Unit): Failure? {
                started += app.id; onJob(com.zirakusa.tienda.ui.Phase.DOWNLOADING, 0.3f); gate.await(); return null
            }
        }
        val state = StoreState(slow, scope, "com.zirakusa.tienda").apply { start() }
        state.updateAll()
        assertTrue(state.busy); assertEquals(2, state.queue.size)
        state.cancel()
        assertFalse(state.busy); assertNull(state.job); assertNull(state.failure)
        assertEquals(listOf("reloj"), started)                                        // nothing else started
        assertEquals(3, state.updates)                                                // nothing changed
    }

    @Test fun uninstallOnlyInstalledAppsAndNeverStoreItself() {
        val engine = FakeEngine()
        val state = StoreState(engine, scope, "com.zirakusa.tienda").apply { start() }
        state.rows.forEach { state.uninstall(it) }
        assertEquals(listOf("uninstall com.zirakusa.reloj", "uninstall com.zirakusa.notas", "uninstall com.zirakusa.clima"), engine.log)
    }

    @Test fun sizesAndPercent() {
        val es = java.util.Locale.forLanguageTag("es")
        assertEquals("", com.zirakusa.tienda.data.Formats.size(0, es))
        assertEquals("512 B", com.zirakusa.tienda.data.Formats.size(512, es))
        assertEquals("12,4 MB", com.zirakusa.tienda.data.Formats.size(13_000_000, es))
        assertEquals("64,1 MB", com.zirakusa.tienda.data.Formats.size(67_260_453, es))
        assertEquals("150 MB", com.zirakusa.tienda.data.Formats.size(157_286_400, es))
        assertEquals("1.3 GB", com.zirakusa.tienda.data.Formats.size(1_400_000_000, java.util.Locale.US))
        assertEquals(0, com.zirakusa.tienda.data.Formats.percent(-1f)); assertEquals(42, com.zirakusa.tienda.data.Formats.percent(0.42f))
        assertEquals(100, com.zirakusa.tienda.data.Formats.percent(3f))
    }

    /** Store left in the background: coming back reads the catalog again once it is older than five minutes. */
    @Test fun comingBackReadsAStaleCatalogAgain() {
        val engine = FakeEngine()
        var clock = 1_000_000L
        val state = StoreState(engine, scope, "com.zirakusa.tienda", now = { clock }).apply { start() }
        assertEquals(3, state.updates)
        // Clima gets a new version on GitHub meanwhile.
        engine.catalog = Catalog(engine.catalog.apps.map { if (it.id == "clima") it.copy(versionCode = 3, versionName = "0.2.0") else it })
        clock += 60_000; state.resumed()
        assertEquals(3, state.updates)                                    // a minute later: the same catalog
        clock += StoreState.STALE_MS; state.resumed()
        assertEquals(4, state.updates)                                    // later: read again, Clima can be updated
    }
}
