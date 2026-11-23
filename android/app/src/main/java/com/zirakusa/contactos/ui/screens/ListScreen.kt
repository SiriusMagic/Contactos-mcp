package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.contactos.R
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

/** «Contactos»: search, favourites, birthdays soon, and everyone A–Z with a letter bar to jump. «+» adds one. */
@Composable fun ListScreen(state: ContactosState, actions: AppActions) = Box(Modifier.fillMaxSize().background(ContactosColors.Background)) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), Modifier.weight(1f).padding(start = 4.dp), fontSize = 26.sp)
            RoundButton(Icons.Outlined.Settings, stringResource(R.string.settings)) { state.page = Page.SETTINGS }
        }
        if (!state.allowed) return@Column Group(Modifier.padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Contacts, null, Modifier.size(48.dp), tint = ContactosColors.AccentText)
                Text(stringResource(R.string.allow_text), color = ContactosColors.Muted, textAlign = TextAlign.Center)
                MainButton(stringResource(R.string.allow), onClick = actions.allow)
            }
        }
        OutlinedTextField(state.query, { state.query = it.take(100) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), singleLine = true, shape = CircleShape,
            leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text(pluralStringResource(R.plurals.search_n, state.items.size, state.items.size)) })
        if (state.query.isNotBlank()) {
            val found = state.results
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp)) {
                if (found.isEmpty()) item { Text(stringResource(R.string.nothing_found), Modifier.padding(16.dp), color = ContactosColors.Muted) }
                items(found, key = { it.id }) { ContactRow(it, state) }
            }
            return@Column
        }
        Box(Modifier.weight(1f)) {
            val list = rememberLazyListState()
            val groups = state.groups
            val favorites = state.favorites
            val birthdays = state.birthdays
            val head = (if (favorites.isNotEmpty()) 1 else 0) + (if (birthdays.isNotEmpty()) 1 else 0)
            LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 36.dp, bottom = 100.dp)) {
                if (favorites.isNotEmpty()) item {
                    Column {
                        Caption(stringResource(R.string.favorites))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                            items(favorites, key = { it.id }) { c ->
                                Column(Modifier.width(64.dp).clickable { state.open(c) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Avatar(c, 56.dp)
                                    Text(c.shownName.substringBefore(' '), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                if (birthdays.isNotEmpty()) item {
                    Group(Modifier.padding(vertical = 8.dp)) {
                        Caption(stringResource(R.string.birthdays))
                        birthdays.take(3).forEach { (c, d) ->
                            val days = ChronoUnit.DAYS.between(state.today(), d).toInt()
                            Item(c.shownName, when (days) { 0 -> stringResource(R.string.today); 1 -> stringResource(R.string.tomorrow)
                                else -> pluralStringResource(R.plurals.in_days, days, days) } + (c.birthday?.turns(state.today())?.let { " · " + stringResource(R.string.turns, it) } ?: ""),
                                onClick = { state.open(c) })
                        }
                    }
                }
                groups.forEach { (letter, people) ->
                    item(key = "h$letter") { Text("$letter", Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp), color = ContactosColors.AccentText, fontSize = 16.sp) }
                    items(people, key = { it.id }) { ContactRow(it, state) }
                }
                if (groups.isEmpty()) item { Text(stringResource(R.string.empty), Modifier.padding(24.dp), color = ContactosColors.Muted) }
            }
            val scope = rememberCoroutineScope()
            Column(Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                var index = head
                val starts = groups.map { (l, p) -> (l to index).also { index += 1 + p.size } }
                starts.forEach { (l, at) ->
                    Text("$l", Modifier.clickable(onClickLabel = "$l") { scope.launch { list.scrollToItem(at) } }.padding(horizontal = 6.dp, vertical = 1.dp),
                        color = ContactosColors.Muted, fontSize = 11.sp)
                }
            }
        }
    }
    if (state.allowed) Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).size(60.dp).clip(RoundedCornerShape(20.dp))
        .background(ContactosColors.Accent).clickable(onClickLabel = stringResource(R.string.new_contact)) { state.edit() }, contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Add, stringResource(R.string.new_contact), tint = ContactosColors.OnAccent)
    }
}

@Composable private fun ContactRow(c: Contact, state: ContactosState) = Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { state.open(c) }
    .padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Avatar(c)
    Column(Modifier.padding(start = 14.dp)) {
        Text(c.shownName, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.query.isNotBlank()) c.phones.firstOrNull()?.let { Text(Phones.pretty(it.number), color = ContactosColors.Muted, fontSize = 13.sp) }
    }
}
