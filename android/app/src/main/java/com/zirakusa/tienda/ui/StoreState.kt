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
}

/** The only owner of the screen's state. The screens paint it and call these functions; nothing else changes it. */
class StoreState(private val engine: Engine, private val scope: CoroutineScope, val selfPackage: String) {
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

    val busy get() = job != null || queue.isNotEmpty()
    val updates get() = rows.count { it.state == RowState.UPDATE }

    fun start() {
        engine.cachedCatalog()?.let { catalog = it }
        recheck()
        refresh()
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

    fun allowInstalls() = engine.allowInstalls()

    private fun install(apps: List<CatalogApp>) {
        if (busy || apps.isEmpty()) return
        if (!engine.canInstall()) { canInstall = false; return }
        failure = null
        queue = apps.map { it.packageName }
        scope.launch {
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
            }
        }
    }
}
