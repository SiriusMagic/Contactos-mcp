package com.zirakusa.contactos.ui

import androidx.compose.runtime.Composable
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.ui.screens.DetailScreen
import com.zirakusa.contactos.ui.screens.DuplicatesScreen
import com.zirakusa.contactos.ui.screens.EditScreen
import com.zirakusa.contactos.ui.screens.ListScreen
import com.zirakusa.contactos.ui.screens.McpScreen
import com.zirakusa.contactos.ui.screens.SettingsScreen

/** What the screens ask of the phone (the activity gives the real ones; tests leave the defaults). Each only after a tap. */
class AppActions(
    val allow: () -> Unit = {},
    /** Opens the dialer with the number (the call starts only when the user presses call there or in Teléfono). */
    val call: (String) -> Unit = {},
    val sms: (String) -> Unit = {},
    val email: (String) -> Unit = {},
    val map: (String) -> Unit = {},
    val share: (Contact) -> Unit = {},
    val copy: (String) -> Unit = {},
    val copyLink: (String) -> Unit = {},
    val goToHub: () -> Unit = {},
)

@Composable fun ContactosApp(state: ContactosState, actions: AppActions = AppActions()) = when (state.page) {
    Page.LIST -> ListScreen(state, actions)
    Page.DETAIL -> state.shown?.let { DetailScreen(state, it, actions) } ?: ListScreen(state, actions)
    Page.EDIT -> EditScreen(state)
    Page.DUPLICATES -> DuplicatesScreen(state)
    Page.SETTINGS -> SettingsScreen(state)
    Page.MCP -> McpScreen(actions.copyLink, actions.goToHub) { state.page = Page.SETTINGS }
}
