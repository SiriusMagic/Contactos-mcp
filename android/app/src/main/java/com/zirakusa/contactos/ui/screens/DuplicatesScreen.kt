package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.contactos.R
import com.zirakusa.contactos.data.Phones
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors

/** «Contactos repetidos»: people saved twice (same number or same name). Tap one to edit or delete it. */
@Composable fun DuplicatesScreen(state: ContactosState) = Column(Modifier.fillMaxSize().background(ContactosColors.Background).systemBarsPadding()
    .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    PageTop(stringResource(R.string.duplicates)) { state.page = Page.SETTINGS }
    val groups = state.duplicates
    if (groups.isEmpty()) Text(stringResource(R.string.no_duplicates), Modifier.padding(16.dp), color = ContactosColors.Muted)
    groups.forEach { g ->
        Group { g.forEachIndexed { i, c ->
            if (i > 0) Divider()
            Row(Modifier.fillMaxWidth().clickable { state.open(c) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(c)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(c.shownName, fontSize = 17.sp)
                    Text(c.phones.joinToString(" · ") { Phones.pretty(it.number) }, color = ContactosColors.Muted, fontSize = 13.sp)
                }
            }
        } }
    }
}
