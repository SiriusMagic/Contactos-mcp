package com.zirakusa.tienda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** The app's colours for one mode. [accentText] is the accent for text and thin lines (darker on light screens). */
data class Palette(val background: Color, val card: Color, val chip: Color, val text: Color, val muted: Color,
    val accentText: Color, val light: Boolean)

private val Black = Palette(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFFF2F2F2), Color(0xFF8E8E92),
    Color(0xFF3DDC97), light = false)
private val Light = Palette(Color(0xFFF2F2F5), Color(0xFFFFFFFF), Color(0xFFE3E3E8), Color(0xFF121A26), Color(0xFF66748A),
    Color(0xFF13804F), light = true)

/** The global colours: they follow the phone (pure black at night, light by day), with Tienda's green accent. */
object StoreColors {
    var palette by mutableStateOf(Black); private set

    val Background get() = palette.background
    val Card get() = palette.card
    val Chip get() = palette.chip
    val Text get() = palette.text
    val Muted get() = palette.muted
    val AccentText get() = palette.accentText
    val Accent = Color(0xFF3DDC97)
    val OnAccent = Color(0xFF06170F)
    /** Errors: the bright red at night, a darker one on light screens (easier to read). */
    val Danger get() = if (palette.light) Color(0xFFC62828) else Color(0xFFFF6B6B)
    val Warning = Color(0xFFF5C518)
    val isLight get() = palette.light

    fun use(dark: Boolean) {
        val next = if (dark) Black else Light
        if (palette != next) palette = next
    }
}

@Composable fun StoreTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    StoreColors.use(dark)
    val c = StoreColors
    val scheme = if (!c.isLight) darkColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background,
        surface = c.Card, surfaceContainer = c.Card, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted,
        error = c.Danger)
    else lightColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background, surface = c.Card,
        surfaceContainer = c.Card, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted, error = c.Danger)
    MaterialTheme(scheme) { CompositionLocalProvider(LocalContentColor provides c.Text, content = content) }
}
