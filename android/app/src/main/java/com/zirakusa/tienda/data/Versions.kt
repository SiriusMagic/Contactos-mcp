package com.zirakusa.tienda.data

/** What the phone has of an app: its versionCode and versionName. */
data class Installed(val versionCode: Long, val versionName: String)

/** The row of «Apps»: not on the phone («Instalar»), up to date («Abrir») or with a newer version («Actualizar»). */
enum class RowState { NOT_INSTALLED, UP_TO_DATE, UPDATE }

data class AppRow(val app: CatalogApp, val installed: Installed?, val state: RowState)

object Versions {
    /**
     * Compares two versionNames number by number («0.3.10» > «0.3.2», «1.0» == «1.0.0»); a part with letters compares as
     * text after the numbers. Only for showing; what decides an update is the versionCode, like Android.
     */
    fun compareNames(a: String, b: String): Int {
        val x = a.trim().split('.', '-', '+')
        val y = b.trim().split('.', '-', '+')
        for (i in 0 until maxOf(x.size, y.size)) {
            val p = x.getOrElse(i) { "0" }
            val q = y.getOrElse(i) { "0" }
            val pn = p.toLongOrNull()
            val qn = q.toLongOrNull()
            val c = when {
                pn != null && qn != null -> pn.compareTo(qn)
                pn != null -> 1
                qn != null -> -1
                else -> p.compareTo(q)
            }
            if (c != 0) return c.coerceIn(-1, 1)
        }
        return 0
    }

    /** Android installs over an app only with a higher versionCode, so that is what makes «Actualizar». */
    fun state(app: CatalogApp, installed: Installed?): RowState = when {
        installed == null -> RowState.NOT_INSTALLED
        app.versionCode > installed.versionCode -> RowState.UPDATE
        else -> RowState.UP_TO_DATE
    }

    fun rows(catalog: Catalog, installed: (String) -> Installed?): List<AppRow> =
        catalog.apps.map { app -> installed(app.packageName).let { AppRow(app, it, state(app, it)) } }
}

object UpdateAll {
    /**
     * «Actualizar todo»: every app with a newer version, in catalog order, one after another, and Tienda itself last
     * (installing it restarts the app, which would stop the rest).
     */
    fun order(rows: List<AppRow>, selfPackage: String): List<CatalogApp> {
        val updates = rows.filter { it.state == RowState.UPDATE }.map { it.app }
        return updates.filter { it.packageName != selfPackage } + updates.filter { it.packageName == selfPackage }
    }
}
