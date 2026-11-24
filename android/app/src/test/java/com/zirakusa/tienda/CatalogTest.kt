package com.zirakusa.tienda

import com.zirakusa.tienda.data.AllowedHost
import com.zirakusa.tienda.data.CatalogException
import com.zirakusa.tienda.data.CatalogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogTest {
    private val good = """{"id":"reloj","name":"Reloj","package":"com.zirakusa.reloj","versionCode":4,"versionName":"0.3.1",
        "apk":"https://raw.githubusercontent.com/SiriusMagic/Reloj-mcp/main/android/Reloj-0.3.1.apk",
        "notes":"Conectar todas","color":"#FF9A1F"}"""

    @Test fun readsAGoodEntry() {
        val c = CatalogParser.parse("""{"format":1,"apps":[$good]}""")
        assertEquals(1, c.apps.size); assertEquals(emptyList<String>(), c.skipped)
        val a = c.apps[0]
        assertEquals("reloj", a.id); assertEquals("Reloj", a.name); assertEquals("com.zirakusa.reloj", a.packageName)
        assertEquals(4L, a.versionCode); assertEquals("0.3.1", a.versionName); assertEquals("Conectar todas", a.notes)
        assertEquals(0xFF9A1F, a.color)
    }

    @Test fun notesAreOptional() {
        val c = CatalogParser.parse("""{"apps":[${good.replace(Regex("\"notes\":\"[^\"]*\","), "")}]}""")
        assertEquals("", c.apps.single().notes)
    }

    @Test fun badOrMissingFieldsLeaveOnlyThatAppOut() {
        val bad = listOf(
            good.replace("\"id\":\"reloj\",", ""),
            good.replace("\"id\":\"reloj\"", "\"id\":\"Reloj Mcp!\""),
            good.replace("\"name\":\"Reloj\"", "\"name\":\"  \""),
            good.replace("\"package\":\"com.zirakusa.reloj\"", "\"package\":\"reloj\""),
            good.replace("\"versionCode\":4", "\"versionCode\":\"4\""),
            good.replace("\"versionCode\":4", "\"versionCode\":0"),
            good.replace("\"versionCode\":4", "\"versionCode\":4.5"),
            good.replace("\"versionName\":\"0.3.1\"", "\"versionName\":\"\""),
            good.replace("https://raw.githubusercontent.com", "https://evil.com"),
            good.replace("https://raw.githubusercontent.com", "http://raw.githubusercontent.com"),
            good.replace("Reloj-0.3.1.apk\"", "Reloj-0.3.1.zip\""),
            good.replace("\"color\":\"#FF9A1F\"", "\"color\":\"orange\""),
            good.replace(",\"color\":\"#FF9A1F\"", ""),
            "42",
        )
        val other = good.replace("reloj", "notas").replace("Reloj", "Notas")
        val c = CatalogParser.parse("""{"apps":[${(bad + other).joinToString(",")}]}""")
        assertEquals(listOf("notas"), c.apps.map { it.id })
        assertEquals(bad.size, c.skipped.size)
    }

    @Test fun repeatedIdOrPackageKeepsTheFirst() {
        val samePackage = good.replace("\"id\":\"reloj\"", "\"id\":\"reloj2\"")
        val c = CatalogParser.parse("""{"apps":[$good,$good,$samePackage]}""")
        assertEquals(1, c.apps.size); assertEquals(2, c.skipped.size)
    }

    @Test fun aBrokenFileIsAnError() {
        listOf("", "not json", "[]", "{}", """{"apps":{}}""", """{"format":2,"apps":[]}""").forEach {
            assertThrows(it, CatalogException::class.java) { CatalogParser.parse(it) }
        }
        assertThrows(CatalogException::class.java) { CatalogParser.parse(" ".repeat(CatalogParser.MAX_BYTES + 1)) }
        assertEquals(0, CatalogParser.parse("""{"apps":[]}""").apps.size)
    }

    /** The catalog.json of this repo (the one the phones download) is valid and holds the whole family. */
    @Test fun theRepoCatalogIsValid() {
        val file = listOf(File("../../catalog.json"), File("../catalog.json"), File("catalog.json")).first { it.exists() }
        val c = CatalogParser.parse(file.readText())
        assertEquals(emptyList<String>(), c.skipped)
        assertEquals(20, c.apps.size)
        assertTrue(c.apps.all { AllowedHost.allowed(it.apk) && it.apk.startsWith("https://raw.githubusercontent.com/SiriusMagic/") })
        assertTrue(c.apps.all { it.apk.endsWith("-${it.versionName}.apk") || it.apk.endsWith("-${it.versionName}-optimized.apk") })
        assertEquals("com.zirakusa.tienda", c.apps.last().packageName)
        listOf("com.zirakusa.stone.kotlin.preview", "com.zirakusa.gatewayhub", "app.luma.gallery", "com.zirakusa.launcher", "com.zirakusa.grabadora")
            .forEach { pkg -> assertTrue(pkg, c.apps.any { it.packageName == pkg }) }
    }
}
