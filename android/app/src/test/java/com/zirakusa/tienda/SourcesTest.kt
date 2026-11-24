package com.zirakusa.tienda

import com.zirakusa.tienda.data.AllowedHost
import com.zirakusa.tienda.data.Sources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SourcesTest {
    @Test fun onlyRawGithubOverHttps() {
        assertTrue(AllowedHost.allowed("https://raw.githubusercontent.com/SiriusMagic/Reloj-mcp/main/android/Reloj-0.3.1.apk"))
        assertTrue(AllowedHost.allowed("https://RAW.githubusercontent.com:443/SiriusMagic/Notas.mcp/main/catalog.json"))
        listOf(
            "http://raw.githubusercontent.com/SiriusMagic/Reloj-mcp/main/a.apk",
            "https://github.com/SiriusMagic/Reloj-mcp/raw/main/a.apk",
            "https://raw.githubusercontent.com.evil.com/a.apk",
            "https://evil.com/raw.githubusercontent.com/a.apk",
            "https://raw.githubusercontent.com@evil.com/a.apk",
            "https://user:pw@raw.githubusercontent.com/a.apk",
            "https://raw.githubusercontent.com:8443/a.apk",
            "https://xraw.githubusercontent.com/a.apk",
            "https://objects.githubusercontent.com/a.apk",
            "ftp://raw.githubusercontent.com/a.apk",
            "file:///sdcard/a.apk",
            "content://downloads/a.apk",
            "javascript:alert(1)",
            "raw.githubusercontent.com/a.apk",
            "https://raw.githubusercontent.com/",
            "https://raw.githubusercontent.com/a/../../b.apk",
            "https://raw.githubusercontent.com/a b.apk",
            "",
        ).forEach { assertFalse(it, AllowedHost.allowed(it)) }
    }

    @Test fun catalogTriesTheNewRepoNameFirst() {
        assertEquals(listOf(
            "https://raw.githubusercontent.com/SiriusMagic/Tienda-mcp/main/catalog.json",
            "https://raw.githubusercontent.com/SiriusMagic/Contactos-mcp/main/catalog.json",
        ), Sources.catalogUrls)
        Sources.catalogUrls.forEach { assertTrue(AllowedHost.allowed(it)) }
    }

    @Test fun tiendaApkUnderBothNames() {
        val old = "https://raw.githubusercontent.com/SiriusMagic/Contactos-mcp/main/android/Tienda-0.1.0.apk"
        val new = "https://raw.githubusercontent.com/SiriusMagic/Tienda-mcp/main/android/Tienda-0.1.0.apk"
        assertEquals(listOf(new, old), Sources.candidates(old))
        assertEquals(listOf(new, old), Sources.candidates(new))
        val reloj = "https://raw.githubusercontent.com/SiriusMagic/Reloj-mcp/main/android/Reloj-0.3.1.apk"
        assertEquals(listOf(reloj), Sources.candidates(reloj))
    }

    /** The guard refuses before connecting: no request ever leaves for another host (and the test needs no network). */
    @Test fun netRefusesOtherHosts() {
        val net = Net()
        listOf("https://example.com/catalog.json", "http://raw.githubusercontent.com/a/catalog.json").forEach {
            assertTrue(runCatching { net.text(it, 1000) }.exceptionOrNull() is BlockedHost)
        }
        val file = File.createTempFile("tienda", ".apk")
        try {
            assertTrue(runCatching { net.download("https://evil.com/a.apk", file, 1000) {} }.exceptionOrNull() is BlockedHost)
            assertEquals(0L, file.length())
        } finally { file.delete() }
    }
}
