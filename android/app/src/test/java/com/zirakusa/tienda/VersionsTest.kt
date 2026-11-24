package com.zirakusa.tienda

import com.zirakusa.tienda.data.AppRow
import com.zirakusa.tienda.data.Installed
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.data.UpdateAll
import com.zirakusa.tienda.data.Versions
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionsTest {
    @Test fun compareNamesNumberByNumber() {
        assertEquals(1, Versions.compareNames("0.3.10", "0.3.2"))
        assertEquals(-1, Versions.compareNames("0.3.1", "0.3.2"))
        assertEquals(0, Versions.compareNames("1.0", "1.0.0"))
        assertEquals(1, Versions.compareNames("1.7.26", "1.7.9"))
        assertEquals(-1, Versions.compareNames("0.0.24", "0.1"))
        assertEquals(-1, Versions.compareNames("1.0-beta", "1.0.1"))
        assertEquals(1, Versions.compareNames("2.0", "1.99.99"))
    }

    @Test fun rowStateComesFromTheVersionCode() {
        val reloj = app("reloj", "Reloj", "com.zirakusa.reloj", 5, "0.3.2")
        assertEquals(RowState.NOT_INSTALLED, Versions.state(reloj, null))
        assertEquals(RowState.UPDATE, Versions.state(reloj, Installed(4, "0.3.1")))
        assertEquals(RowState.UP_TO_DATE, Versions.state(reloj, Installed(5, "0.3.2")))
        // Newer on the phone than in the catalog (a test build): nothing to update, «Abrir».
        assertEquals(RowState.UP_TO_DATE, Versions.state(reloj, Installed(6, "0.3.3")))
    }

    @Test fun rowsFollowTheCatalog() {
        val engine = FakeEngine()
        val rows = Versions.rows(sampleCatalog, engine::installed)
        assertEquals(listOf("tienda", "reloj", "notas", "clima", "qr"), rows.map { it.app.id })
        assertEquals(listOf(RowState.UPDATE, RowState.UPDATE, RowState.UPDATE, RowState.UP_TO_DATE, RowState.NOT_INSTALLED),
            rows.map { it.state })
        assertEquals("0.3.1", rows[1].installed?.versionName)
    }

    @Test fun updateAllLeavesTiendaLastAndSkipsTheRest() {
        val rows = Versions.rows(sampleCatalog, FakeEngine()::installed)
        assertEquals(listOf("reloj", "notas", "tienda"), UpdateAll.order(rows, "com.zirakusa.tienda").map { it.id })
        val none = rows.map { AppRow(it.app, it.installed, RowState.UP_TO_DATE) }
        assertEquals(emptyList<String>(), UpdateAll.order(none, "com.zirakusa.tienda").map { it.id })
        val onlySelf = rows.filter { it.app.id == "tienda" || it.app.id == "qr" }
        assertEquals(listOf("tienda"), UpdateAll.order(onlySelf, "com.zirakusa.tienda").map { it.id })
    }
}
