package com.zirakusa.contactos.data

import java.text.Collator
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Kinds of phone and email, as Android names them. [key] is what MCP uses. */
enum class Label(val key: String) { MOBILE("mobile"), HOME("home"), WORK("work"), OTHER("other") }

data class Phone(val number: String, val label: Label = Label.MOBILE)
data class Email(val address: String, val label: Label = Label.HOME)

/** A birthday: day and month, and the year when known. */
data class Birthday(val month: Int, val day: Int, val year: Int? = null) {
    init { require(month in 1..12 && day in 1..31) }
    fun next(today: LocalDate): LocalDate {
        fun on(y: Int) = if (month == 2 && day == 29 && !java.time.Year.isLeap(y.toLong())) LocalDate.of(y, 3, 1)
            else LocalDate.of(y, month, day.coerceAtMost(java.time.YearMonth.of(y, month).lengthOfMonth()))
        val t = on(today.year)
        return if (t.isBefore(today)) on(today.year + 1) else t
    }
    /** The age they turn on [next], when the year is known. */
    fun turns(today: LocalDate) = year?.let { next(today).year - it }
    /** «1990-12-02» or «--12-02» (vCard without year). */
    fun iso() = if (year != null) "%04d-%02d-%02d".format(year, month, day) else "--%02d-%02d".format(month, day)

    companion object {
        /** Reads «1990-12-02», «1990/12/02», «--12-02» or «12/02». */
        fun parse(text: String?): Birthday? {
            val t = text?.trim().orEmpty()
            Regex("""^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$""").find(t)?.destructured?.let { (y, m, d) -> return runCatching { Birthday(m.toInt(), d.toInt(), y.toInt()) }.getOrNull() }
            Regex("""^(?:--)?(\d{1,2})[-/.](\d{1,2})$""").find(t)?.destructured?.let { (m, d) -> return runCatching { Birthday(m.toInt(), d.toInt()) }.getOrNull() }
            return null
        }
    }
}

/** One contact as the app shows it (Android may keep it in several accounts; [id] is its contact id). */
data class Contact(val id: Long = 0, val name: String = "", val phones: List<Phone> = emptyList(), val emails: List<Email> = emptyList(),
    val org: String = "", val note: String = "", val birthday: Birthday? = null, val address: String = "", val starred: Boolean = false,
    val photo: String? = null) {
    val shownName get() = name.ifBlank { phones.firstOrNull()?.number ?: emails.firstOrNull()?.address ?: "" }
    /** Up to two letters for the round picture («AP» for Ana Pérez). */
    val initials get() = shownName.split(' ', '-').filter { it.isNotBlank() && it[0].isLetterOrDigit() }.take(2).joinToString("") { it.take(1).uppercase() }
        .ifEmpty { shownName.take(1).uppercase() }

    companion object { const val NAME_MAX = 100 }
}

/** Pure rules of the list: order, letters, search, duplicates and birthdays. */
object Contacts {
    private val collator = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }

    /** What a contact is sorted by: the name, or «Pérez Ana» when sorting by last name. */
    fun key(c: Contact, order: Order = Order.FIRST): String {
        val words = c.name.trim().split(Regex("\\s+"))
        return if (order == Order.LAST && words.size > 1) (words.drop(1) + words.first()).joinToString(" ") else c.shownName
    }

    /** Letters then «#» for names that start with something else. */
    fun sorted(items: List<Contact>, order: Order = Order.FIRST): List<Contact> =
        items.sortedWith(compareBy<Contact> { letter(it, order) == '#' }.thenComparator { a, b -> collator.compare(key(a, order), key(b, order)) })

    /** The letter a name is listed under; Ñ stays Ñ, accents go (Álvaro → A). */
    fun letter(c: Contact, order: Order = Order.FIRST): Char {
        val first = key(c, order).trim().firstOrNull() ?: return '#'
        if (first.uppercaseChar() == 'Ñ') return 'Ñ'
        val plain = plain(first.toString()).firstOrNull()?.uppercaseChar() ?: return '#'
        return if (plain in 'A'..'Z') plain else '#'
    }

    fun groups(items: List<Contact>, order: Order = Order.FIRST): List<Pair<Char, List<Contact>>> =
        sorted(items, order).groupBy { letter(it, order) }.toList()

    /** Names (any word's start, accents ignored), numbers (digits anywhere, Ecuador's 0/+593 both) and emails. */
    fun search(items: List<Contact>, query: String): List<Contact> {
        val q = plain(query.trim())
        if (q.isEmpty()) return emptyList()
        val digits = query.filter { it.isDigit() }
        val key = if (digits.length >= 6) Phones.key(digits) else null
        val found = items.filter { c ->
            val words = plain(c.shownName + " " + c.org).split(' ', '-', '.')
            words.any { it.startsWith(q) } || plain(c.shownName).startsWith(q) ||
                (digits.length >= 3 && c.phones.any { p -> p.number.filter(Char::isDigit).contains(digits) || (key != null && Phones.key(p.number).contains(key)) }) ||
                c.emails.any { plain(it.address).contains(q) }
        }
        return sorted(found).sortedBy { if (plain(it.shownName).startsWith(q)) 0 else 1 }
    }

    /** Contacts that look like the same person: the same number, or the same name. Groups of two or more. */
    fun duplicates(items: List<Contact>): List<List<Contact>> {
        val parent = items.indices.toMutableList()
        fun find(i: Int): Int { var x = i; while (parent[x] != x) x = parent[x]; return x }
        val seen = HashMap<String, Int>()
        items.forEachIndexed { i, c ->
            val keys = c.phones.map { "p:" + Phones.key(it.number) }.filter { it.length > 8 } + listOfNotNull(plain(c.name).takeIf { it.length > 2 }?.let { "n:$it" })
            keys.forEach { k -> seen[k]?.let { j -> parent[find(i)] = find(j) } ?: run { seen[k] = i } }
        }
        return items.indices.groupBy(::find).values.filter { it.size > 1 }.map { g -> g.map { items[it] } }
    }

    /** Birthdays in the next [days] days (today first). */
    fun upcoming(items: List<Contact>, today: LocalDate, days: Int): List<Pair<Contact, LocalDate>> =
        items.mapNotNull { c -> c.birthday?.next(today)?.let { c to it } }.filter { ChronoUnit.DAYS.between(today, it.second) in 0..days.toLong() }
            .sortedWith(compareBy({ it.second }, { it.first.shownName }))

    fun plain(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
}

/** Phone numbers the Ecuadorian way: the same person with «099 123 4567» and «+593 99 123 4567». */
object Phones {
    /** A key to compare numbers: the digits without the country or the leading 0 (the last 9 digits at most). */
    fun key(number: String): String {
        var d = number.filter { it.isDigit() }
        if (d.startsWith("593") && d.length >= 11) d = d.drop(3)
        d = d.trimStart('0')
        return d.takeLast(9)
    }

    /** «099 123 4567» for Ecuadorian mobiles, «02 234 5678» for landlines; others as typed. */
    fun pretty(number: String): String {
        val plus = number.trim().startsWith("+")
        val d = number.filter { it.isDigit() }
        return when {
            !plus && d.length == 10 && d.startsWith("09") -> "${d.take(3)} ${d.substring(3, 6)} ${d.drop(6)}"
            !plus && d.length == 9 && d.startsWith("0") -> "${d.take(2)} ${d.substring(2, 5)} ${d.drop(5)}"
            plus && d.startsWith("5939") && d.length == 12 -> "+593 ${d.substring(3, 5)} ${d.substring(5, 8)} ${d.drop(8)}"
            else -> number.trim()
        }
    }

    /** What a phone keeps: digits, «+» first, and * # for codes. */
    fun clean(number: String) = number.trim().let { t -> (if (t.startsWith("+")) "+" else "") + t.filter { it.isDigit() || it in "*#" } }
}
