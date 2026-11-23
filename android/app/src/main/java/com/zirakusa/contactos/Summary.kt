package com.zirakusa.contactos

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Contacts
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The lines of «Hoy» for the family summary: birthdays of the next 7 days («Hoy · Ana Pérez (35)»). Pure. */
object SummaryLines {
    const val MAX = 5
    fun lines(items: List<Contact>, today: LocalDate, day: (Int) -> String): List<String> =
        Contacts.upcoming(items, today, 7).take(MAX).map { (c, d) ->
            "${day(ChronoUnit.DAYS.between(today, d).toInt())} · ${c.shownName}" + (c.birthday?.turns(today)?.let { " ($it)" } ?: "")
        }
}

/**
 * The family summary (see Inicio's FAMILIA.md): upcoming birthdays for Inicio's «Hoy». Only apps signed by the same
 * developer may ask; only names and days leave, never numbers.
 */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val items = runCatching { AndroidStore(context).list() }.getOrDefault(emptyList())
        val r = context.resources
        val lines = SummaryLines.lines(items, LocalDate.now()) { d ->
            when (d) { 0 -> r.getString(R.string.today); 1 -> r.getString(R.string.tomorrow); else -> r.getQuantityString(R.plurals.in_days, d, d) }
        }
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(R.string.summary_empty))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
