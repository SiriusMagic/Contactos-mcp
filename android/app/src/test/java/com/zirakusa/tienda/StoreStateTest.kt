package com.zirakusa.tienda

import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.ui.CatalogError
import com.zirakusa.tienda.ui.Failure
import com.zirakusa.tienda.ui.StoreState
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
}
