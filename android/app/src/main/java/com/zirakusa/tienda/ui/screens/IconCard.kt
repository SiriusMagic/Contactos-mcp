package com.zirakusa.tienda.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.tienda.AppIcon
import com.zirakusa.tienda.R
import com.zirakusa.tienda.ui.theme.StoreColors

/** Ajustes › «Icono de la app»: the five icons; a tap changes the home-screen icon. [choose] does it (Android in the app). */
@Composable fun IconCard(current: AppIcon, choose: (AppIcon) -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.icon_title), fontSize = 17.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppIcon.entries.forEach { icon ->
                val on = icon == current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = name) { choose(icon) }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(icon.background))
                        .border(if (on) 3.dp else 0.dp, if (on) StoreColors.Accent else Color.Transparent, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center) {
                        Image(painterResource(icon.preview), null, Modifier.requiredSize(72.dp))   // the 108dp layer, cropped like a launcher
                    }
                    Text(name, color = if (on) StoreColors.AccentText else StoreColors.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Text(stringResource(R.string.icon_hint), color = StoreColors.Muted, fontSize = 13.sp)
    }
}
