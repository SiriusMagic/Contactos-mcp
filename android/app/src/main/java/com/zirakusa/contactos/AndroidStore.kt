package com.zirakusa.contactos

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds as K
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import com.zirakusa.contactos.data.Birthday
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.ContactStore
import com.zirakusa.contactos.data.Email
import com.zirakusa.contactos.data.Label
import com.zirakusa.contactos.data.NoAccess
import com.zirakusa.contactos.data.Phone

/**
 * The phone's own contacts (Google, SIM, phone…), read and written through Android's contacts provider. Always with
 * `?` arguments, never text glued into a query. Edits replace only the kinds of data Contactos shows (name, phones,
 * emails, company, note, birthday, address) in the contact's first account; the rest stays untouched.
 */
class AndroidStore(context: Context) : ContactStore {
    private val cr: ContentResolver = context.applicationContext.contentResolver
    private val kinds = listOf(K.StructuredName.CONTENT_ITEM_TYPE, K.Phone.CONTENT_ITEM_TYPE, K.Email.CONTENT_ITEM_TYPE,
        K.Organization.CONTENT_ITEM_TYPE, K.Note.CONTENT_ITEM_TYPE, K.Event.CONTENT_ITEM_TYPE, K.StructuredPostal.CONTENT_ITEM_TYPE)

    override fun list(): List<Contact> {
        val byId = LinkedHashMap<Long, Contact>()
        val cols = arrayOf(Data.CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3, Data.DISPLAY_NAME_PRIMARY, Data.STARRED, Data.PHOTO_THUMBNAIL_URI)
        val where = Data.MIMETYPE + " IN (" + kinds.joinToString(",") { "?" } + ")"
        val cursor = try { cr.query(Data.CONTENT_URI, cols, where, kinds.toTypedArray(), null) } catch (_: SecurityException) { throw NoAccess() } ?: return emptyList()
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                var x = byId[id] ?: Contact(id, c.getString(5).orEmpty(), starred = c.getInt(6) == 1, photo = c.getString(7))
                val d1 = c.getString(2).orEmpty(); val type = c.getInt(3)
                x = when (c.getString(1)) {
                    K.Phone.CONTENT_ITEM_TYPE -> if (d1.isBlank() || x.phones.any { it.number == d1 }) x else x.copy(phones = x.phones + Phone(d1, phoneLabel(type)))
                    K.Email.CONTENT_ITEM_TYPE -> if (d1.isBlank() || x.emails.any { it.address == d1 }) x else x.copy(emails = x.emails + Email(d1, emailLabel(type)))
                    K.Organization.CONTENT_ITEM_TYPE -> x.copy(org = x.org.ifBlank { d1 })
                    K.Note.CONTENT_ITEM_TYPE -> x.copy(note = x.note.ifBlank { d1 })
                    K.Event.CONTENT_ITEM_TYPE -> if (type == K.Event.TYPE_BIRTHDAY) x.copy(birthday = x.birthday ?: Birthday.parse(d1)) else x
                    K.StructuredPostal.CONTENT_ITEM_TYPE -> x.copy(address = x.address.ifBlank { d1.replace('\n', ' ') })
                    else -> x
                }
                byId[id] = x
            }
        }
        return byId.values.toList()
    }

    override fun save(c: Contact): Contact {
        val ops = ArrayList<ContentProviderOperation>()
        val raw: Long?
        if (c.id == 0L) {
            raw = null
            ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null).build()
        } else {
            raw = rawOf(c.id) ?: return c
            ops += ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection(Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + " IN (" + kinds.joinToString(",") { "?" } + ")",
                    arrayOf(raw.toString()) + kinds).build()
        }
        fun row(kind: String, vararg values: Pair<String, Any?>) {
            val b = ContentProviderOperation.newInsert(Data.CONTENT_URI).withValue(Data.MIMETYPE, kind)
            if (raw == null) b.withValueBackReference(Data.RAW_CONTACT_ID, 0) else b.withValue(Data.RAW_CONTACT_ID, raw)
            values.forEach { (k, v) -> b.withValue(k, v) }
            ops += b.build()
        }
        row(K.StructuredName.CONTENT_ITEM_TYPE, K.StructuredName.DISPLAY_NAME to c.name.trim())
        c.phones.filter { it.number.isNotBlank() }.forEach { row(K.Phone.CONTENT_ITEM_TYPE, K.Phone.NUMBER to it.number, K.Phone.TYPE to phoneType(it.label)) }
        c.emails.filter { it.address.isNotBlank() }.forEach { row(K.Email.CONTENT_ITEM_TYPE, K.Email.ADDRESS to it.address, K.Email.TYPE to emailType(it.label)) }
        if (c.org.isNotBlank()) row(K.Organization.CONTENT_ITEM_TYPE, K.Organization.COMPANY to c.org)
        if (c.note.isNotBlank()) row(K.Note.CONTENT_ITEM_TYPE, K.Note.NOTE to c.note)
        c.birthday?.let { row(K.Event.CONTENT_ITEM_TYPE, K.Event.START_DATE to it.iso(), K.Event.TYPE to K.Event.TYPE_BIRTHDAY) }
        if (c.address.isNotBlank()) row(K.StructuredPostal.CONTENT_ITEM_TYPE, K.StructuredPostal.FORMATTED_ADDRESS to c.address, K.StructuredPostal.TYPE to K.StructuredPostal.TYPE_HOME)
        val results = try { cr.applyBatch(ContactsContract.AUTHORITY, ops) } catch (_: SecurityException) { throw NoAccess() }
        val id = if (c.id != 0L) c.id else contactOf(results[0].uri?.lastPathSegment?.toLongOrNull()) ?: 0L
        if (id != 0L && c.starred) star(id, true)
        return c.copy(id = id)
    }

    override fun delete(id: Long): Boolean = try {
        cr.delete(ContactsContract.Contacts.CONTENT_URI, ContactsContract.Contacts._ID + "=?", arrayOf(id.toString())) > 0
    } catch (_: SecurityException) { throw NoAccess() }

    override fun star(id: Long, on: Boolean): Boolean = try {
        cr.update(ContactsContract.Contacts.CONTENT_URI, ContentValues().apply { put(ContactsContract.Contacts.STARRED, if (on) 1 else 0) },
            ContactsContract.Contacts._ID + "=?", arrayOf(id.toString())) > 0
    } catch (_: SecurityException) { throw NoAccess() }

    private fun rawOf(contact: Long): Long? = cr.query(RawContacts.CONTENT_URI, arrayOf(RawContacts._ID), RawContacts.CONTACT_ID + "=? AND " + RawContacts.DELETED + "=0",
        arrayOf(contact.toString()), RawContacts._ID)?.use { if (it.moveToFirst()) it.getLong(0) else null }

    private fun contactOf(raw: Long?): Long? = raw?.let { r ->
        cr.query(RawContacts.CONTENT_URI, arrayOf(RawContacts.CONTACT_ID), RawContacts._ID + "=?", arrayOf(r.toString()), null)?.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    private fun phoneLabel(t: Int) = when (t) { K.Phone.TYPE_MOBILE -> Label.MOBILE; K.Phone.TYPE_HOME -> Label.HOME; K.Phone.TYPE_WORK, K.Phone.TYPE_WORK_MOBILE -> Label.WORK; else -> Label.OTHER }
    private fun emailLabel(t: Int) = when (t) { K.Email.TYPE_HOME -> Label.HOME; K.Email.TYPE_WORK -> Label.WORK; K.Email.TYPE_MOBILE -> Label.MOBILE; else -> Label.OTHER }
    private fun phoneType(l: Label) = when (l) { Label.MOBILE -> K.Phone.TYPE_MOBILE; Label.HOME -> K.Phone.TYPE_HOME; Label.WORK -> K.Phone.TYPE_WORK; Label.OTHER -> K.Phone.TYPE_OTHER }
    private fun emailType(l: Label) = when (l) { Label.MOBILE -> K.Email.TYPE_MOBILE; Label.HOME -> K.Email.TYPE_HOME; Label.WORK -> K.Email.TYPE_WORK; Label.OTHER -> K.Email.TYPE_OTHER }
}
