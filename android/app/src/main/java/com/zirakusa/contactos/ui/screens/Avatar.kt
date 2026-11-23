package com.zirakusa.contactos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.contactos.data.Contact

private val tints = listOf(0xFFF06292, 0xFF4FC3F7, 0xFF4CE08A, 0xFFFF9A1F, 0xFFB57BEE, 0xFFF5C518, 0xFF26C6DA, 0xFFEF5350).map { Color(it) }

/** A round picture with the contact's initials, always the same colour for the same name. */
@Composable fun Avatar(c: Contact, size: Dp = 44.dp) {
    val tint = tints[Math.floorMod(c.shownName.hashCode(), tints.size)]
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
        Text(c.initials, color = tint, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.SemiBold)
    }
}
