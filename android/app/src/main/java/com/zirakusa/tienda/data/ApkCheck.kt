package com.zirakusa.tienda.data

import java.security.MessageDigest

/** What Android read from a downloaded APK: package, versionCode and the SHA-256 of each signing certificate. */
data class ApkFacts(val packageName: String, val versionCode: Long, val certificates: Set<String>)

/** Why an APK may not be installed. Each one has its own clear message on screen. */
enum class Problem {
    /** No connection or the server failed. */
    NETWORK,
    /** A URL outside [AllowedHost] (also a redirect). */
    BAD_HOST,
    /** Bigger than [ApkCheck.MAX_BYTES]. */
    TOO_BIG,
    /** Android could not read the file as an APK, or its signature. */
    UNREADABLE,
    WRONG_PACKAGE,
    WRONG_VERSION,
    /** Signed by someone else: never installed. */
    WRONG_SIGNATURE,
    /** «Instalar apps desconocidas» is off for Tienda. */
    NOT_ALLOWED,
    /** The user said no in Android's dialog. */
    CANCELLED,
    /** Android refused (no space, incompatible…); its message is shown too. */
    INSTALL_FAILED,
}

object ApkCheck {
    const val MAX_BYTES = 300L * 1024 * 1024

    /**
     * The APK must be exactly the catalog's [app] (same package and versionCode) and be signed with exactly Tienda's own
     * certificates [own]. Returns null when everything matches, or the first [Problem].
     */
    fun verify(facts: ApkFacts?, app: CatalogApp, own: Set<String>): Problem? = when {
        facts == null -> Problem.UNREADABLE
        facts.packageName != app.packageName -> Problem.WRONG_PACKAGE
        facts.versionCode != app.versionCode -> Problem.WRONG_VERSION
        own.isEmpty() || facts.certificates.isEmpty() -> Problem.UNREADABLE
        facts.certificates != own -> Problem.WRONG_SIGNATURE
        else -> null
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
