package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.R
import com.zirakusa.tienda.data.ThemeChoice
import com.zirakusa.tienda.ui.Page
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.theme.StoreColors

/** «Ajustes»: theme, downloads only on Wi‑Fi, which apps the list shows, checking on open, the app icon and «Acerca de». */
@Composable fun SettingsScreen(state: StoreState, iconCard: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(StoreColors.Background).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { state.page = Page.APPS }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.settings), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Group {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.theme), fontSize = 17.sp)
                    Row(Modifier.fillMaxWidth().clip(CircleShape).background(StoreColors.Chip).padding(4.dp)) {
                        listOf(ThemeChoice.SYSTEM to R.string.theme_system, ThemeChoice.LIGHT to R.string.theme_light, ThemeChoice.BLACK to R.string.theme_black)
                            .forEach { (choice, label) ->
                                val on = state.theme == choice
                                Text(stringResource(label), Modifier.weight(1f).clip(CircleShape).background(if (on) StoreColors.Accent else StoreColors.Chip)
                                    .clickable(role = Role.RadioButton) { state.changeTheme(choice) }.padding(vertical = 10.dp),
                                    color = if (on) StoreColors.OnAccent else StoreColors.Text, fontSize = 15.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                            }
                    }
                }
            }
            Group {
                SwitchRow(R.string.wifi_only, R.string.wifi_only_hint, state.wifiOnly, state::changeWifiOnly)
                SwitchRow(R.string.show_new, R.string.show_new_hint, state.showNotInstalled, state::changeShowNotInstalled)
                SwitchRow(R.string.check_on_open, R.string.check_on_open_hint, state.checkOnOpen, state::changeCheckOnOpen)
            }
            Group { iconCard() }
            Group {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.about), fontSize = 17.sp)
                    Text(stringResource(R.string.about_version, state.version ?: "—"), color = StoreColors.Muted, fontSize = 14.sp)
                    Text(stringResource(R.string.about_signature), color = StoreColors.Muted, fontSize = 14.sp)
                    Text(state.fingerprint ?: "—", color = StoreColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                    Text(stringResource(R.string.about_privacy), color = StoreColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable private fun Group(content: @Composable () -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StoreColors.Card)) { content() }

@Composable private fun SwitchRow(title: Int, hint: Int, on: Boolean, change: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { change(!on) }.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), fontSize = 17.sp)
            Text(stringResource(hint), color = StoreColors.Muted, fontSize = 14.sp, lineHeight = 19.sp)
        }
        Switch(on, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = StoreColors.Accent, checkedThumbColor = StoreColors.OnAccent))
    }
