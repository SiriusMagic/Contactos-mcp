package com.zirakusa.tienda.data

import java.net.URI

/**
 * The only place Tienda may talk to: [HOST], over https. Every URL (catalog, APKs, redirects) passes through [allowed]
 * before a request goes out; anything else is refused.
 */
object AllowedHost {
    const val HOST = "raw.githubusercontent.com"

    fun allowed(url: String): Boolean {
        if (url.length > 2048 || url.any { it.isWhitespace() || it.code < 0x20 }) return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.isAbsolute && !uri.isOpaque &&
            uri.scheme.equals("https", ignoreCase = true) &&
            uri.rawUserInfo == null &&
            uri.host?.equals(HOST, ignoreCase = true) == true &&
            (uri.port == -1 || uri.port == 443) &&
            uri.rawPath.orEmpty().startsWith("/") && !uri.rawPath.contains("/../") && uri.rawPath.length > 1
    }
}

/**
 * Where the catalog lives. [BASE] is the one constant: the family's GitHub account on [AllowedHost.HOST]. The repo is
 * being renamed from Contactos-mcp to Tienda-mcp, so the new name is tried first and the old one second.
 */
object Sources {
    const val BASE = "https://raw.githubusercontent.com/SiriusMagic/"
    /** Tienda's own repo: its new name first, then the old one. */
    val REPOS = listOf("Tienda-mcp", "Contactos-mcp")
    const val BRANCH = "main"
    const val CATALOG = "catalog.json"

    val catalogUrls: List<String> get() = REPOS.map { "$BASE$it/$BRANCH/$CATALOG" }

    /**
     * The URLs to try for [url], in order. A file of Tienda's own repo (under either name) is tried under the new name
     * first, then the old one; any other URL is tried as it is.
     */
    fun candidates(url: String): List<String> {
        val repo = REPOS.firstOrNull { url.startsWith("$BASE$it/", ignoreCase = true) } ?: return listOf(url)
        val rest = url.substring(BASE.length + repo.length)
        return REPOS.map { "$BASE$it$rest" }
    }
}
