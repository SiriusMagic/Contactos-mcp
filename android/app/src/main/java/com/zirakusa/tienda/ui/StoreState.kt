package com.zirakusa.tienda.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.tienda.data.AppRow
import com.zirakusa.tienda.data.Catalog
import com.zirakusa.tienda.data.CatalogApp
import com.zirakusa.tienda.data.CatalogException
import com.zirakusa.tienda.data.Installed
import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.data.RowState
import com.zirakusa.tienda.data.SettingsRules
import com.zirakusa.tienda.data.StoreSettings
import com.zirakusa.tienda.data.MapValues
import com.zirakusa.tienda.data.ThemeChoice
import com.zirakusa.tienda.data.UpdateAll
import com.zirakusa.tienda.data.Versions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What an install is doing right now. [progress] is 0..1 while downloading. */
enum class Phase { DOWNLOADING, VERIFYING, INSTALLING }

data class Job(val packageName: String, val phase: Phase, val progress: Float = 0f)

/** An install that did not happen: which app, why, and Android's own words when it refused. */
data class Failure(val problem: Problem, val detail: String? = null)

data class FailedApp(val name: String, val failure: Failure)

enum class CatalogError { NETWORK, INVALID }

enum class Page { APPS, SETTINGS }

/** Everything Tienda does outside the screen: the network, PackageManager and PackageInstaller (fakes in the tests). */
interface Engine {
    /** The last catalog that loaded, so the list shows at once (also without internet). */
    fun cachedCatalog(): Catalog?
    /** Downloads catalog.json; throws [CatalogException] when it is not valid, any other exception when offline. */
    suspend fun catalog(): Catalog
    fun installed(packageName: String): Installed?
    /** «Instalar apps desconocidas» is allowed for Tienda. */
    fun canInstall(): Boolean
    /** Opens Android's «Instalar apps desconocidas» page for Tienda. */
    fun allowInstalls()
    /** Downloads, verifies and installs [app]; null when it was installed. [onJob] is called on the main thread. */
    suspend fun install(app: CatalogApp, onJob: (Phase, Float) -> Unit): Failure?
    /** Opens an installed app (only ever after a tap of the user). */
    fun open(packageName: String)
    /** The phone is on Wi‑Fi or another network that does not spend mobile data. */
    fun onUnmeteredNetwork(): Boolean
    /** SHA-256 of Tienda's own signing certificate, in hex (for Ajustes › Acerca de). */
    fun ownFingerprint(): String?
    /** The app's icon: the installed one, else the catalog's PNG (kept in the cache); null = show the initial. */
    suspend fun icon(app: CatalogApp): android.graphics.Bitmap?
    /** Android's «¿Desinstalar?» for [packageName] (only ever after a tap; the person confirms in Android's dialog). */
    fun uninstall(packageName: String)
}

/** The only owner of the screen's state. The screens paint it and call these functions; nothing else changes it. */
class StoreState(private val engine: Engine, private val scope: CoroutineScope, val selfPackage: String,
    private val settings: StoreSettings = StoreSettings(MapValues())) {
    var page by mutableStateOf(Page.APPS)
    var theme by mutableStateOf(settings.theme); private set
    var wifiOnly by mutableStateOf(settings.wifiOnly); private set
    var showNotInstalled by mutableStateOf(settings.showNotInstalled); private set
    var checkOnOpen by mutableStateOf(settings.checkOnOpen); private set
    /** An install was asked for with «Solo con Wi‑Fi» on and no Wi‑Fi: the notice says so. */
    var wifiNeeded by mutableStateOf(false)
    var rows by mutableStateOf<List<AppRow>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var catalogError by mutableStateOf<CatalogError?>(null); private set
    var job by mutableStateOf<Job?>(null); private set
    /** Packages still waiting in «Actualizar todo». */
    var queue by mutableStateOf<List<String>>(emptyList()); private set
    var failure by mutableStateOf<FailedApp?>(null)
    /** The package whose notes are open (tap on a row). */
    var expanded by mutableStateOf<String?>(null)
    var canInstall by mutableStateOf(true); private set
    private var catalog: Catalog? = null
    /** The «Instalar / Actualizar todo» running now (Cancelar stops it while it downloads). */
    private var running: kotlinx.coroutines.Job? = null

    /** The rows the list shows (Ajustes › «Mostrar no instaladas»). */
    val visibleRows get() = SettingsRules.visible(rows, showNotInstalled)
    val busy get() = job != null || queue.isNotEmpty()
    val updates get() = rows.count { it.state == RowState.UPDATE }

    fun start() {
        engine.cachedCatalog()?.let { catalog = it }
        recheck()
        if (checkOnOpen || catalog == null) refresh()
    }

    /** After coming back to the app (an install, Settings): reads again what is installed and the permission. */
    fun recheck() {
        canInstall = engine.canInstall()
        catalog?.let { c -> rows = Versions.rows(c, engine::installed) }
    }

    fun refresh() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                catalog = engine.catalog()
                catalogError = null
                recheck()
            } catch (_: CatalogException) {
                catalogError = CatalogError.INVALID
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                catalogError = CatalogError.NETWORK
            } finally {
                loading = false
            }
        }
    }

    fun toggleNotes(row: AppRow) { expanded = if (expanded == row.app.packageName) null else row.app.packageName }

    /** The row's button: «Instalar» / «Actualizar» install, «Abrir» opens. */
    fun act(row: AppRow) = when (row.state) {
        RowState.UP_TO_DATE -> engine.open(row.app.packageName)
        else -> install(listOf(row.app))
    }

    fun updateAll() = install(UpdateAll.order(rows, selfPackage))

    fun changeTheme(next: ThemeChoice) { theme = next; settings.theme = next }
    fun changeWifiOnly(on: Boolean) { wifiOnly = on; settings.wifiOnly = on; if (!on) wifiNeeded = false }
    fun changeShowNotInstalled(on: Boolean) { showNotInstalled = on; settings.showNotInstalled = on }
    fun changeCheckOnOpen(on: Boolean) { checkOnOpen = on; settings.checkOnOpen = on }

    /** Ajustes › Acerca de: Tienda's version and its signature, as «34:B8:…». */
    val version: String? get() = engine.installed(selfPackage)?.versionName
    val fingerprint: String? get() = SettingsRules.fingerprint(engine.ownFingerprint())

    fun allowInstalls() = engine.allowInstalls()

    suspend fun icon(app: CatalogApp) = engine.icon(app)

    /** «Desinstalar» in a row's details (never Store itself, never while installing). */
    fun uninstall(row: AppRow) { if (!busy && row.installed != null && row.app.packageName != selfPackage) engine.uninstall(row.app.packageName) }

    /** «Cancelar»: stops the download and the apps still waiting; an install already handed to Android is not stopped. */
    fun cancel() { if (job?.phase == Phase.DOWNLOADING || queue.isNotEmpty()) running?.cancel() }

    private fun install(apps: List<CatalogApp>) {
        if (busy || apps.isEmpty()) return
        if (!engine.canInstall()) { canInstall = false; return }
        if (wifiOnly && !engine.onUnmeteredNetwork()) { wifiNeeded = true; return }
        wifiNeeded = false
        failure = null
        queue = apps.map { it.packageName }
        running = scope.launch {
            try {
                for (app in apps) {
                    queue = queue - app.packageName
                    job = Job(app.packageName, Phase.DOWNLOADING)
                    val failed = engine.install(app) { phase, p -> job = Job(app.packageName, phase, p) }
                    if (failed != null) {
                        if (failed.problem == Problem.NOT_ALLOWED) { canInstall = false; queue = emptyList(); break }
                        failure = FailedApp(app.name, failed)
                    }
                    recheck()
                }
            } finally {
                job = null
                queue = emptyList()
                running = null
            }
        }
    }
}
