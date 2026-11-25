package com.zirakusa.tienda.data

/** Where the settings live: a map in the tests, the app's private preferences on the phone (out of backups). */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String) { map[key] = value }
}

/** «Tema»: follow the phone, always light, or always black. */
enum class ThemeChoice { SYSTEM, LIGHT, BLACK }

/** Tienda's settings, typed. Unknown or broken values fall back to the defaults. */
class StoreSettings(private val kv: KeyValues) {
    var theme: ThemeChoice
        get() = kv.get("theme")?.let { runCatching { ThemeChoice.valueOf(it) }.getOrNull() } ?: ThemeChoice.SYSTEM
        set(v) = kv.put("theme", v.name)
    /** Downloads only on Wi‑Fi (or another unmetered network). Off by default. */
    var wifiOnly: Boolean get() = flag("wifi_only", false); set(v) = kv.put("wifi_only", v.toString())
    /** Shows the apps that are not installed yet. On by default. */
    var showNotInstalled: Boolean get() = flag("show_new", true); set(v) = kv.put("show_new", v.toString())
    /** Downloads the catalog every time Tienda opens. On by default; off, it uses the saved one until «Recargar». */
    var checkOnOpen: Boolean get() = flag("check_on_open", true); set(v) = kv.put("check_on_open", v.toString())

    private fun flag(key: String, default: Boolean) = kv.get(key)?.toBooleanStrictOrNull() ?: default
}

/** Rules for the settings screen. Pure, with tests. */
object SettingsRules {
    /** The list shown: everything, or (with «Mostrar no instaladas» off) only what is on the phone. */
    fun visible(rows: List<AppRow>, showNotInstalled: Boolean): List<AppRow> =
        if (showNotInstalled) rows else rows.filter { it.state != RowState.NOT_INSTALLED }

    /** A SHA-256 in hex as «34:B8:87:…», the way Android shows certificate fingerprints; null when it is not one. */
    fun fingerprint(hex: String?): String? {
        val clean = hex?.lowercase()?.takeIf { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } } ?: return null
        return clean.chunked(2).joinToString(":").uppercase()
    }
}
