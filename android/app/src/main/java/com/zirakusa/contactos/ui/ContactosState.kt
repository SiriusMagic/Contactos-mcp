package com.zirakusa.contactos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.ContactStore
import com.zirakusa.contactos.data.Contacts
import com.zirakusa.contactos.data.Email
import com.zirakusa.contactos.data.KeyValues
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.NoAccess
import com.zirakusa.contactos.data.Order
import com.zirakusa.contactos.data.Phone
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.data.Settings
import com.zirakusa.contactos.ui.theme.Accent
import com.zirakusa.contactos.ui.theme.ThemeMode
import java.time.LocalDate

/** The list, or a page over it. */
enum class Page { LIST, DETAIL, EDIT, DUPLICATES, SETTINGS, MCP }

/** What «Editar» holds while typing (phones and emails as lines, the birthday as text). */
data class Draft(val id: Long = 0, val name: String = "", val phones: List<String> = listOf(""), val emails: List<String> = listOf(""),
    val org: String = "", val birthday: String = "", val address: String = "", val note: String = "", val starred: Boolean = false) {
    fun toContact() = Contact(id, name.trim(), phones.map(Phones::clean).filter { it.any(Char::isDigit) }.map { Phone(it, Label.MOBILE) },
        emails.map { it.trim() }.filter { it.isNotEmpty() }.map { Email(it, Label.HOME) }, org.trim(), note.trim(), Birthday.parse(birthday), address.trim(), starred)
    /** Why it can't be saved yet: no name nor number; a birthday that isn't a date. */
    val problem get() = when {
        name.isBlank() && phones.none { it.any(Char::isDigit) } -> Problem.EMPTY
        birthday.isNotBlank() && Birthday.parse(birthday) == null -> Problem.BIRTHDAY
        else -> null
    }
    enum class Problem { EMPTY, BIRTHDAY }

    companion object {
        fun of(c: Contact) = Draft(c.id, c.name, c.phones.map { it.number }.ifEmpty { listOf("") }, c.emails.map { it.address }.ifEmpty { listOf("") },
            c.org, c.birthday?.iso()?.removePrefix("--").orEmpty(), c.address, c.note, c.starred)
    }
}

/** The only owner of Contactos' state: the contacts, search, the one open, the one being edited and the settings. Screens only paint. */
class ContactosState(kv: KeyValues, private val store: ContactStore, private val today: () -> LocalDate = LocalDate::now) {
    private val settings = Settings(kv)

    var page by mutableStateOf(Page.LIST)
    var items by mutableStateOf(emptyList<Contact>()); private set
    /** Android lets Contactos read the contacts. */
    var allowed by mutableStateOf(true); private set
    var query by mutableStateOf("")
    var shown by mutableStateOf<Contact?>(null); private set
    var draft by mutableStateOf(Draft())
    var order by mutableStateOf(settings.order); private set
    var themeMode by mutableStateOf(settings.theme?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.BLACK); private set
    var accent by mutableStateOf(settings.accent?.let { runCatching { Accent.valueOf(it) }.getOrNull() } ?: Accent.ROSA); private set

    val groups get() = Contacts.groups(items, order)
    val results get() = Contacts.search(items, query)
    val favorites get() = Contacts.sorted(items.filter { it.starred }, order)
    val birthdays get() = Contacts.upcoming(items, today(), 14)
    val duplicates get() = Contacts.duplicates(items)
    fun today() = today.invoke()

    fun load() {
        try { items = store.list(); allowed = true } catch (_: NoAccess) { items = emptyList(); allowed = false }
        shown = shown?.let { s -> items.firstOrNull { it.id == s.id } }
        if (shown == null && page == Page.DETAIL) page = Page.LIST
    }

    fun open(c: Contact) { shown = c; page = Page.DETAIL }
    fun edit(c: Contact? = null) { draft = c?.let(Draft::of) ?: Draft(name = query.takeIf { q -> q.none(Char::isDigit) }.orEmpty(),
        phones = listOf(query.takeIf { q -> q.isNotBlank() && q.all { it.isDigit() || it in "+ " } }.orEmpty())); page = Page.EDIT }

    /** «Guardar»: saves and shows it; null while there is a [Draft.problem]. */
    fun save(): Contact? {
        if (draft.problem != null) return null
        val saved = try { store.save(draft.toContact()) } catch (_: NoAccess) { allowed = false; return null }
        load(); shown = items.firstOrNull { it.id == saved.id } ?: saved; page = Page.DETAIL; query = ""
        return saved
    }

    fun cancelEdit() { page = if (draft.id != 0L && shown != null) Page.DETAIL else Page.LIST }
    fun star(c: Contact) { runCatching { store.star(c.id, !c.starred) }; load() }
    fun delete(c: Contact) { runCatching { store.delete(c.id) }; shown = null; page = Page.LIST; load() }

    fun changeOrder(o: Order) { order = o; settings.order = o }
    fun changeTheme(mode: ThemeMode) { themeMode = mode; settings.theme = mode.name }
    fun changeAccent(next: Accent) { accent = next; settings.accent = next.name }
}
