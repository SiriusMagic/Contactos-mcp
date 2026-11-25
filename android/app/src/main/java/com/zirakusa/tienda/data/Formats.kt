package com.zirakusa.tienda.data

import java.util.Locale

/** How sizes and download progress read on screen («32,3 MB», «45 % de 32,3 MB»). Pure, with tests. */
object Formats {
    /** Bytes as KB/MB/GB with one decimal under 100 (comma in Spanish); empty when unknown. */
    fun size(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return ""
        val units = listOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble(); var unit = 0
        while (value >= 1000 && unit < units.lastIndex) { value /= 1024; unit++ }
        val text = if (unit == 0 || value >= 100) "%.0f".format(locale, value) else "%.1f".format(locale, value)
        return "$text ${units[unit]}"
    }

    /** 0..100 from a 0..1 progress (unknown or out of range → 0..100). */
    fun percent(progress: Float): Int = (progress * 100).toInt().coerceIn(0, 100)
}
