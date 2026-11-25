package com.zirakusa.tienda

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.provider.Settings
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.zirakusa.tienda.data.ApkCheck
import com.zirakusa.tienda.data.ApkFacts
import com.zirakusa.tienda.data.Installed
import java.io.File

/** Everything Tienda asks of PackageManager: versions, signing certificates, the install permission, opening an app. */
class Platform(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager

    private fun info(packageName: String, flags: Int): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else @Suppress("DEPRECATION") pm.getPackageInfo(packageName, flags)
    } catch (_: PackageManager.NameNotFoundException) { null }

    fun installed(packageName: String): Installed? =
        info(packageName, 0)?.let { Installed(PackageInfoCompat.getLongVersionCode(it), it.versionName.orEmpty()) }

    /** Tienda installed this app (installer of record), so Android 12+ may update it without asking again. */
    fun installedByTienda(packageName: String): Boolean = Build.VERSION.SDK_INT >= 30 && runCatching {
        pm.getInstallSourceInfo(packageName).installingPackageName == context.packageName
    }.getOrDefault(false)

    /** SHA-256 of Tienda's own signing certificates: the only ones it accepts. */
    fun ownCertificates(): Set<String> = certificates(info(context.packageName, signingFlag()))

    /** Package, versionCode and certificates of a downloaded APK, or null when Android cannot read it. */
    fun archiveFacts(file: File): ApkFacts? {
        val path = file.absolutePath
        var info = archive(path, signingFlag())
        var certs = certificates(info)
        if (certs.isEmpty() && Build.VERSION.SDK_INT >= 28) {
            info = archive(path, legacyFlag()) ?: info
            certs = certificates(info)
        }
        info ?: return null
        return ApkFacts(info.packageName.orEmpty(), PackageInfoCompat.getLongVersionCode(info), certs)
    }

    private fun archive(path: String, flags: Int): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(flags.toLong()))
        else pm.getPackageArchiveInfo(path, flags)
    }.getOrNull()

    private fun signingFlag() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else legacyFlag()

    // The certificates are only ever compared in full against Tienda's own (ApkCheck.verify), never trusted on their own.
    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun legacyFlag() = PackageManager.GET_SIGNATURES

    private fun certificates(info: PackageInfo?): Set<String> {
        info ?: return emptySet()
        val signers: Array<Signature>? = if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        }
        return signers.orEmpty().map { ApkCheck.sha256(it.toByteArray()) }.toSet()
    }

    fun canInstall(): Boolean = pm.canRequestPackageInstalls()

    fun allowInstallsIntent() =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    /** Android's own uninstall dialog for [packageName]. */
    fun uninstallIntent(packageName: String) = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())

    /** The installed app's own icon at [px]×[px], or null if it is not installed. */
    fun icon(packageName: String, px: Int): android.graphics.Bitmap? = try {
        pm.getApplicationIcon(packageName).toBitmap(px, px)
    } catch (_: PackageManager.NameNotFoundException) { null }

    /** On Wi‑Fi or another network that does not spend mobile data (for «Solo con Wi‑Fi»). */
    fun onUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
