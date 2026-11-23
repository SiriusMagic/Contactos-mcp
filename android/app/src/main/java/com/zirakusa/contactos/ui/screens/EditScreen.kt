package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zirakusa.contactos.R
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Draft
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Nuevo contacto» / «Editar»: name, numbers and emails (a new empty line appears as you fill the last), company, birthday, address, note. */
@Composable fun EditScreen(state: ContactosState) = Sheet(stringResource(if (state.draft.id == 0L) R.string.new_contact else R.string.edit), onClose = state::cancelEdit) {
    val d = state.draft
    fun set(next: Draft) { state.draft = next }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Field(R.string.name, d.name) { set(d.copy(name = it.take(100))) }
        d.phones.forEachIndexed { i, p -> Field(R.string.phone, p, KeyboardType.Phone) { v -> set(d.copy(phones = grow(d.phones, i, v.take(40)))) } }
        d.emails.forEachIndexed { i, e -> Field(R.string.email, e, KeyboardType.Email) { v -> set(d.copy(emails = grow(d.emails, i, v.take(100)))) } }
        Field(R.string.company, d.org) { set(d.copy(org = it.take(100))) }
        Field(R.string.birthday_hint, d.birthday, KeyboardType.Number) { set(d.copy(birthday = it.take(10))) }
        Field(R.string.address, d.address) { set(d.copy(address = it.take(200))) }
        Field(R.string.note, d.note, lines = 3) { set(d.copy(note = it.take(500))) }
        when (d.problem) {
            Draft.Problem.EMPTY -> Text(stringResource(R.string.need_name), color = ContactosColors.Muted)
            Draft.Problem.BIRTHDAY -> Text(stringResource(R.string.bad_birthday), color = ContactosColors.Danger)
            null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MainButton(stringResource(R.string.save)) { state.save() } }
        }
        TextButton(state::cancelEdit, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.cancel), color = ContactosColors.Text) }
    }
}

/** Replaces line [i]; when the last line gets text, an empty one is added under it. */
fun grow(lines: List<String>, i: Int, value: String): List<String> {
    val next = lines.toMutableList().also { it[i] = value }
    return if (next.last().isNotBlank() && next.size < 6) next + "" else next
}

@Composable private fun Field(label: Int, value: String, keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(stringResource(label)) }, singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard))
