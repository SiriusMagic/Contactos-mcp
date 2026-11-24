package com.zirakusa.tienda

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.IntentCompat
import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.ui.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Hands PackageInstaller's answers to the install waiting for them, by session id. */
object Installs {
    const val ACTION = "com.zirakusa.tienda.action.INSTALL_RESULT"
    private val waiting = ConcurrentHashMap<Int, Channel<Intent>>()

    fun register(session: Int): Channel<Intent> = Channel<Intent>(Channel.UNLIMITED).also { waiting[session] = it }
    fun unregister(session: Int) { waiting.remove(session)?.close() }
    fun deliver(session: Int, intent: Intent) { waiting[session]?.trySend(intent) }
}

/** PackageInstaller's answers come here (not exported: only our own PendingIntent reaches it). */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Installs.ACTION) return
        val session = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (session >= 0) Installs.deliver(session, intent)
    }
}

/** Installs one verified APK with a PackageInstaller session and waits for Android's answer. */
class Installer(private val context: Context) {
    /**
     * [skipConfirm]: an update of an app Tienda installed, so on Android 12+ it asks not to show the dialog again
     * (Android may still ask). [confirm] shows Android's dialog when it asks the user.
     */
    suspend fun install(apk: File, packageName: String, skipConfirm: Boolean, confirm: (Intent) -> Unit): Failure? {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31 && skipConfirm) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = withContext(Dispatchers.IO) { installer.createSession(params) }
        val answers = Installs.register(id)
        try {
            withContext(Dispatchers.IO) {
                installer.openSession(id).use { session ->
                    apk.inputStream().use { input ->
                        session.openWrite("base.apk", 0, apk.length()).use { out -> input.copyTo(out); session.fsync(out) }
                    }
                    session.commit(resultSender(id))
                }
            }
            while (true) {
                val answer = withTimeoutOrNull(WAIT_MS) { answers.receive() } ?: return Failure(Problem.CANCELLED)
                when (val status = answer.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val dialog = IntentCompat.getParcelableExtra(answer, Intent.EXTRA_INTENT, Intent::class.java)
                            ?: return Failure(Problem.INSTALL_FAILED)
                        withContext(Dispatchers.Main) { confirm(dialog) }
                    }
                    PackageInstaller.STATUS_SUCCESS -> return null
                    PackageInstaller.STATUS_FAILURE_ABORTED -> return Failure(Problem.CANCELLED)
                    else -> return Failure(Problem.INSTALL_FAILED,
                        answer.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            runCatching { installer.abandonSession(id) }
            return Failure(Problem.INSTALL_FAILED, e.message)
        } finally {
            Installs.unregister(id)
        }
    }

    private fun resultSender(session: Int) = PendingIntent.getBroadcast(context, session,
        Intent(context, InstallReceiver::class.java).setAction(Installs.ACTION).setPackage(context.packageName),
        // Mutable: PackageInstaller adds the status to it. Explicit (our receiver only), so nobody else can fill it.
        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)).intentSender

    private companion object { const val WAIT_MS = 15 * 60 * 1000L }
}
