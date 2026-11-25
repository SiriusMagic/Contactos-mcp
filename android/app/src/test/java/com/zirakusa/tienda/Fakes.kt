package com.zirakusa.tienda

import com.zirakusa.tienda.data.Catalog
import com.zirakusa.tienda.data.CatalogApp
import com.zirakusa.tienda.data.Installed
import com.zirakusa.tienda.ui.Engine
import com.zirakusa.tienda.ui.Failure
import com.zirakusa.tienda.ui.Phase

fun app(id: String, name: String, pkg: String, code: Long, version: String, color: Int = 0x3DDC97, notes: String = "",
    size: Long = 0, category: String = "") =
    CatalogApp(id, name, pkg, code, version, "https://raw.githubusercontent.com/SiriusMagic/$name/main/android/$name-$version.apk",
        notes, color, size, category)

/** A small family for the tests: Reloj and Notas need an update, Clima is up to date, QR is not installed. */
val sampleCatalog = Catalog(listOf(
    app("tienda", "Tienda", "com.zirakusa.tienda", 2, "0.1.1", 0x3DDC97, "Actualizar todo y notas de cada versión."),
    app("reloj", "Reloj", "com.zirakusa.reloj", 5, "0.3.2", 0xFF9A1F, "Conectar todas, Lucía, Icono de la app", 13_000_000, "Herramientas"),
    app("notas", "Notas", "com.zirakusa.notas", 8, "0.6.2", 0xF5C518, "Etiquetas de colores"),
    app("clima", "Clima", "com.zirakusa.clima", 2, "0.1.1", 0x2F80ED),
    app("qr", "QR", "com.zirakusa.qr", 2, "0.1.1", 0x26A69A, "Crear códigos de Wi-Fi"),
))

class FakeEngine(
    var catalog: Catalog = sampleCatalog,
    val installed: MutableMap<String, Installed> = mutableMapOf(
        "com.zirakusa.tienda" to Installed(1, "0.1.0"),
        "com.zirakusa.reloj" to Installed(4, "0.3.1"),
        "com.zirakusa.notas" to Installed(7, "0.6.1"),
        "com.zirakusa.clima" to Installed(2, "0.1.1"),
    ),
    var allowed: Boolean = true,
    var failWith: Map<String, Failure> = emptyMap(),
    var offline: Boolean = false,
    var wifi: Boolean = true,
) : Engine {
    val log = mutableListOf<String>()
    override fun cachedCatalog(): Catalog? = null
    override suspend fun catalog(): Catalog { if (offline) throw java.io.IOException("offline"); return catalog }
    override fun installed(packageName: String) = installed[packageName]
    override fun canInstall() = allowed
    override fun allowInstalls() { log += "allow" }
    override suspend fun install(app: CatalogApp, onJob: (Phase, Float) -> Unit): Failure? {
        log += "install ${app.id}"
        onJob(Phase.DOWNLOADING, 0.5f)
        failWith[app.packageName]?.let { return it }
        installed[app.packageName] = Installed(app.versionCode, app.versionName)
        return null
    }
    override fun open(packageName: String) { log += "open $packageName" }
    override fun onUnmeteredNetwork() = wifi
    override fun ownFingerprint() = "34b88716ba31a8ca170624c1c009c11dd23e182148731f0889c98404b34182d4"
    override suspend fun icon(app: CatalogApp): android.graphics.Bitmap? = null
    override fun uninstall(packageName: String) { log += "uninstall $packageName" }
}
