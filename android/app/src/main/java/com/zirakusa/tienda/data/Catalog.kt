package com.zirakusa.tienda.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * One app of catalog.json. [color] is 0xRRGGBB (behind the initial while there is no icon). Optional: [size] of the APK in
 * bytes (0 = unknown), [category] («Herramientas»…) and [icon], a PNG on [AllowedHost] (null = the initial).
 */
data class CatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val apk: String,
    val notes: String,
    val color: Int,
    val size: Long = 0,
    val category: String = "",
    val icon: String? = null,
)

/** The apps that passed every check, and what was wrong with the ones left out (for the log of an agent, not the user). */
data class Catalog(val apps: List<CatalogApp>, val skipped: List<String> = emptyList())

class CatalogException(message: String) : Exception(message)

/**
 * Reads catalog.json. A file that is not a JSON object with an `apps` list is an error; a single bad entry (missing or
 * wrong field, an APK outside [AllowedHost], a repeated id or package) is left out and the rest still shows.
 */
object CatalogParser {
    const val FORMAT = 1
    private val ID = Regex("^[a-z0-9][a-z0-9-]{0,39}$")
    private val PACKAGE = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    private val COLOR = Regex("^#[0-9A-Fa-f]{6}$")
    private val VERSION = Regex("^[0-9A-Za-z][0-9A-Za-z.+\\-]{0,31}$")
    const val MAX_BYTES = 512 * 1024
    private const val MAX_APPS = 200

    fun parse(text: String): Catalog {
        if (text.length > MAX_BYTES) throw CatalogException("catalog too big")
        val root = try { JSONObject(text) } catch (_: JSONException) { throw CatalogException("not a JSON object") }
        val format = root.optInt("format", FORMAT)
        if (format > FORMAT) throw CatalogException("format $format is newer than $FORMAT")
        val list: JSONArray = root.optJSONArray("apps") ?: throw CatalogException("no apps list")
        val apps = mutableListOf<CatalogApp>()
        val skipped = mutableListOf<String>()
        for (i in 0 until minOf(list.length(), MAX_APPS)) {
            val item = list.optJSONObject(i)
            if (item == null) { skipped += "#$i: not an object"; continue }
            val app = runCatching { entry(item) }.getOrElse { skipped += "#$i: ${it.message}"; continue }
            when {
                apps.any { it.id == app.id } -> skipped += "#$i: repeated id ${app.id}"
                apps.any { it.packageName == app.packageName } -> skipped += "#$i: repeated package ${app.packageName}"
                else -> apps += app
            }
        }
        return Catalog(apps, skipped)
    }

    private fun entry(o: JSONObject): CatalogApp {
        fun text(key: String): String {
            val v = o.opt(key)
            require(v is String && v.isNotBlank()) { "missing $key" }
            return v.trim()
        }
        val id = text("id").also { require(ID.matches(it)) { "bad id" } }
        val name = text("name").also { require(it.length <= 40) { "name too long" } }
        val pkg = text("package").also { require(PACKAGE.matches(it) && it.length <= 120) { "bad package" } }
        val codeValue = o.opt("versionCode")
        require(codeValue is Number && codeValue.toDouble() == codeValue.toLong().toDouble()) { "bad versionCode" }
        val code = codeValue.toLong().also { require(it > 0) { "bad versionCode" } }
        val versionName = text("versionName").also { require(VERSION.matches(it)) { "bad versionName" } }
        val apk = text("apk").also {
            require(AllowedHost.allowed(it)) { "apk not on ${AllowedHost.HOST}" }
            require(it.lowercase().endsWith(".apk")) { "apk is not an .apk" }
        }
        val notes = (o.opt("notes") as? String).orEmpty().trim().take(500)
        val color = text("color").also { require(COLOR.matches(it)) { "bad color" } }.substring(1).toInt(16)
        // Optional: a wrong one is ignored (the app still shows), never trusted.
        val size = (o.opt("size") as? Number)?.toLong()?.takeIf { it in 1..ApkCheck.MAX_BYTES } ?: 0L
        val category = (o.opt("category") as? String).orEmpty().trim().take(30)
        val icon = (o.opt("icon") as? String)?.trim()?.takeIf { AllowedHost.allowed(it) && it.lowercase().endsWith(".png") }
        return CatalogApp(id, name, pkg, code, versionName, apk, notes, color, size, category, icon)
    }
}
