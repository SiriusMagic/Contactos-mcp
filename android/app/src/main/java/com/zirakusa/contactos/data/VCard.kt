package com.zirakusa.contactos.data

/** vCard 3.0, to share a contact with any app (and to read one shared with us). Pure. */
object VCard {
    fun build(c: Contact): String = buildString {
        append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
        append("FN:").append(esc(c.shownName)).append("\r\n")
        val parts = c.name.trim().split(Regex("\\s+"))
        append("N:").append(esc(if (parts.size > 1) parts.drop(1).joinToString(" ") else "")).append(';').append(esc(parts.first())).append(";;;\r\n")
        c.phones.forEach { append("TEL;TYPE=").append(type(it.label)).append(':').append(esc(it.number)).append("\r\n") }
        c.emails.forEach { append("EMAIL;TYPE=").append(type(it.label)).append(':').append(esc(it.address)).append("\r\n") }
        if (c.org.isNotBlank()) append("ORG:").append(esc(c.org)).append("\r\n")
        c.birthday?.let { append("BDAY:").append(it.iso()).append("\r\n") }
        if (c.address.isNotBlank()) append("ADR;TYPE=HOME:;;").append(esc(c.address)).append(";;;;\r\n")
        if (c.note.isNotBlank()) append("NOTE:").append(esc(c.note)).append("\r\n")
        append("END:VCARD\r\n")
    }

    fun parse(text: String): Contact? {
        if (!text.trimStart().startsWith("BEGIN:VCARD", true)) return null
        // Folded lines (a space at the start) belong to the line before.
        val lines = text.replace("\r\n ", "").replace("\n ", "").lines().map { it.trim() }.filter { it.isNotEmpty() }
        var c = Contact()
        var n = ""
        for (line in lines) {
            val head = line.substringBefore(':'); val value = unesc(line.substringAfter(':', ""))
            val key = head.substringBefore(';').uppercase()
            val label = labelOf(head)
            c = when (key) {
                "FN" -> c.copy(name = value)
                "N" -> { n = value.split(';').let { p -> listOfNotNull(p.getOrNull(1), p.getOrNull(0)).filter { it.isNotBlank() }.joinToString(" ") }; c }
                "TEL" -> c.copy(phones = c.phones + Phone(value, label))
                "EMAIL" -> c.copy(emails = c.emails + Email(value, if (label == Label.MOBILE) Label.HOME else label))
                "ORG" -> c.copy(org = value.replace(';', ' ').trim())
                "BDAY" -> c.copy(birthday = Birthday.parse(value))
                "NOTE" -> c.copy(note = value)
                "ADR" -> c.copy(address = value.split(';').filter { it.isNotBlank() }.joinToString(", "))
                else -> c
            }
        }
        if (c.name.isBlank()) c = c.copy(name = n)
        return c.takeIf { it.shownName.isNotBlank() }
    }

    private fun type(l: Label) = when (l) { Label.MOBILE -> "CELL"; Label.HOME -> "HOME"; Label.WORK -> "WORK"; Label.OTHER -> "OTHER" }
    private fun labelOf(head: String) = head.uppercase().let { h ->
        when { "CELL" in h -> Label.MOBILE; "WORK" in h -> Label.WORK; "HOME" in h -> Label.HOME; "TEL" in h -> Label.MOBILE; else -> Label.OTHER }
    }
    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")
    private fun unesc(s: String) = s.replace("\\n", "\n").replace("\\N", "\n").replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")
}
