package com.zirakusa.tienda

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import com.zirakusa.tienda.data.ApkCheck
import com.zirakusa.tienda.data.Catalog
import com.zirakusa.tienda.data.CatalogApp
import com.zirakusa.tienda.data.CatalogException
import com.zirakusa.tienda.data.CatalogParser
import com.zirakusa.tienda.data.Installed
import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.data.Sources
import com.zirakusa.tienda.ui.Engine
import com.zirakusa.tienda.ui.Failure
import com.zirakusa.tienda.ui.Phase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The real [Engine]: catalog and APKs through [Net], the checks of [ApkCheck] with [Platform], and [Installer].
 * Every downloaded APK is deleted afterwards, installed or not.
 */
class AndroidEngine(
    private val context: Context,
    private val start: (Intent) -> Unit,
    private val net: Net = Net(),
) : Engine {
    private val platform = Platform(context)
    private val prefs = context.getSharedPreferences("tienda", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val apks get() = File(context.cacheDir, "apks").apply { mkdirs() }

    /** Leftovers of an install that was cut short (Tienda updating itself restarts the app). */
    fun cleanUp() { apks.listFiles()?.forEach { it.delete() } }

    override fun cachedCatalog(): Catalog? =
        prefs.getString("catalog", null)?.let { runCatching { CatalogParser.parse(it) }.getOrNull() }

    override suspend fun catalog(): Catalog = withContext(Dispatchers.IO) {
        var last: Exception = IOException("no catalog")
        for (url in Sources.catalogUrls) {
            try {
                val text = net.text(url, CatalogParser.MAX_BYTES)
                val catalog = CatalogParser.parse(text)
                prefs.edit { putString("catalog", text) }
                return@withContext catalog
            } catch (e: CatalogException) { last = e } catch (e: IOException) { if (last !is CatalogException) last = e }
        }
        throw last
    }

    override fun installed(packageName: String): Installed? = platform.installed(packageName)
    override fun canInstall() = platform.canInstall()
    override fun allowInstalls() = start(platform.allowInstallsIntent())
    override fun open(packageName: String) { platform.launchIntent(packageName)?.let(start) }

    override suspend fun install(app: CatalogApp, onJob: (Phase, Float) -> Unit): Failure? {
        if (!platform.canInstall()) return Failure(Problem.NOT_ALLOWED)
        val file = File(apks, "${app.id}.apk")
        try {
            download(app, file) { p -> onJob(Phase.DOWNLOADING, p) }?.let { return it }
            post { onJob(Phase.VERIFYING, 1f) }
            val problem = withContext(Dispatchers.IO) {
                ApkCheck.verify(platform.archiveFacts(file), app, platform.ownCertificates())
            }
            if (problem != null) return Failure(problem)
            post { onJob(Phase.INSTALLING, 1f) }
            return Installer(context).install(file, app.packageName, platform.installedByTienda(app.packageName), start)
        } finally {
            file.delete()
        }
    }

    private suspend fun download(app: CatalogApp, file: File, progress: (Float) -> Unit): Failure? =
        withContext(Dispatchers.IO) {
            var shown = -1
            var failure = Failure(Problem.NETWORK)
            for (url in Sources.candidates(app.apk)) {
                try {
                    net.download(url, file, ApkCheck.MAX_BYTES) { p ->
                        val percent = (p * 100).toInt()
                        if (percent != shown) { shown = percent; post { progress(p.coerceAtLeast(0f)) } }
                    }
                    return@withContext null
                } catch (_: BlockedHost) { return@withContext Failure(Problem.BAD_HOST)
                } catch (_: TooBig) { return@withContext Failure(Problem.TOO_BIG)
                } catch (_: IOException) { failure = Failure(Problem.NETWORK) }
            }
            failure
        }

    private fun post(block: () -> Unit) { main.post(block) }
}
