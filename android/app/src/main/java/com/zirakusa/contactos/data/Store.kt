package com.zirakusa.contactos.data

import androidx.core.content.edit

/** Where contacts live: Android's contacts in the app, a list in memory in the tests. */
interface ContactStore {
    /** Every contact; throws [NoAccess] without the permission. */
    fun list(): List<Contact>
    /** Adds ([Contact.id] 0) or replaces a contact; returns it with its id. */
    fun save(c: Contact): Contact
    fun delete(id: Long): Boolean
    fun star(id: Long, on: Boolean): Boolean
}

/** The user hasn't allowed Contactos to read the contacts. */
class NoAccess : Exception()

class MemoryStore(items: List<Contact> = emptyList()) : ContactStore {
    private val map = LinkedHashMap<Long, Contact>().apply { items.forEach { put(it.id, it) } }
    var allowed = true
    override fun list(): List<Contact> { if (!allowed) throw NoAccess(); return map.values.toList() }
    override fun save(c: Contact): Contact {
        val saved = if (c.id == 0L) c.copy(id = (map.keys.maxOrNull() ?: 0) + 1) else c
        map[saved.id] = saved; return saved
    }
    override fun delete(id: Long) = map.remove(id) != null
    override fun star(id: Long, on: Boolean) = map[id]?.let { map[id] = it.copy(starred = on); true } ?: false
}

/** Where the settings live: the phone's preferences in the app, a map in the tests. */
interface KeyValues {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

class MapValues(private val map: MutableMap<String, String> = mutableMapOf()) : KeyValues {
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String?) { if (value == null) map.remove(key) else map[key] = value }
}

/** «Ordenar por»: first name or last name. */
enum class Order { FIRST, LAST }

/** Every setting of Contactos, typed. */
class Settings(private val kv: KeyValues) {
    var order: Order get() = kv.get("order")?.let { runCatching { Order.valueOf(it) }.getOrNull() } ?: Order.FIRST; set(v) = kv.put("order", v.name)
    var theme: String? get() = kv.get("theme"); set(v) = kv.put("theme", v)
    var accent: String? get() = kv.get("accent"); set(v) = kv.put("accent", v)
}

/** The phone's preferences file «contactos» (only settings and tokens: the contacts stay in Android's own store). */
class SharedValues(context: android.content.Context) : KeyValues {
    private val prefs = context.applicationContext.getSharedPreferences("contactos", android.content.Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String?) {
        prefs.edit(commit = true) { if (value == null) remove(key) else putString(key, value) }
    }
}
