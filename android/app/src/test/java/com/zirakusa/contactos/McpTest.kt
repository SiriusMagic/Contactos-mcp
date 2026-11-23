package com.zirakusa.contactos

import com.zirakusa.contactos.data.Account
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.mcp.Access
import com.zirakusa.contactos.mcp.ContactData
import com.zirakusa.contactos.mcp.HubLink
import com.zirakusa.contactos.mcp.Kind
import com.zirakusa.contactos.mcp.McpHandler
import com.zirakusa.contactos.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Contactos' MCP: tools list, good calls, bad arguments, not found, no permission, read-only tokens, wrong tokens, hub link. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class McpTest {
    private val google = Account("com.google", "ana@gmail.com")
    private val memory = MemoryStore(People.all)
    private val data = object : ContactData {
        override val store = memory
        var changes = 0
        override fun today(): LocalDate = LocalDate.of(2026, 11, 21)
        override fun changed() { changes++ }
        override fun account() = google
    }
    private val writer = Token("w", "Claude", "con_" + "w".repeat(40), Access.WRITE, Kind.HUB, "Contactos")
    private val reader = Token("r", "Lector", "con_" + "r".repeat(40), Access.READ)
    private val paused = Token("p", "Viejo", "con_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(writer, reader, paused) }, data, "0.1.0")

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(name: String, args: JSONObject, token: Token = writer) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(name: String, args: JSONObject = JSONObject()) =
        tool(name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(name: String, args: JSONObject = JSONObject(), token: Token = writer) = tool(name, args, token).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")
    private fun names(token: Token) = rpc(token, "tools/list").getJSONArray("tools").let { t -> (0 until t.length()).map { t.getJSONObject(it).getString("name") } }

    @Test fun listsTheTools() {
        assertEquals(listOf("contacts_search", "contacts_get", "contacts_favorites", "contacts_birthdays", "contacts_create", "contacts_favorite"), names(writer))
        assertEquals(listOf("contacts_search", "contacts_get", "contacts_favorites", "contacts_birthdays"), names(reader))
        assertEquals("contactos", rpc(writer, "initialize").getJSONObject("serverInfo").getString("name"))
    }

    @Test fun readsAndWrites() {
        val found = ok("contacts_search", JSONObject().put("query", "+593 99 123 4567"))
        assertEquals(2, found.getInt("total")); assertEquals("Ana Pérez", found.getJSONArray("contacts").getJSONObject(0).getString("name"))
        val ana = ok("contacts_get", JSONObject().put("id", 1)).getJSONObject("contact")
        assertEquals("ana@correo.ec", ana.getJSONArray("emails").getJSONObject(0).getString("address")); assertEquals("1991-11-23", ana.getString("birthday"))
        assertEquals("phone", ana.getJSONArray("accounts").getString(0))                     // kinds only, never account names
        assertEquals(1, ok("contacts_favorites").getJSONArray("contacts").length())
        val b = ok("contacts_birthdays", JSONObject().put("days", 7)).getJSONArray("birthdays")
        assertEquals(2, b.length()); assertEquals(0, b.getJSONObject(0).getInt("days")); assertEquals(35, b.getJSONObject(1).getInt("turns"))
        val made = ok("contacts_create", JSONObject().put("name", "Luis Mora").put("phone", "099 111 2222").put("birthday", "05-14")).getJSONObject("contact")
        assertEquals("0991112222", made.getString("phone")); assertEquals("--05-14", made.getString("birthday"))
        assertTrue(ok("contacts_favorite", JSONObject().put("id", made.getLong("id")).put("favorite", true)).getBoolean("favorite"))
        assertEquals(2, data.changes); assertEquals(7, memory.list().size)
        assertEquals(google, memory.list().first { it.id == made.getLong("id") }.raws.single().account)   // «Almacenamiento predeterminado»
        assertEquals("google", ok("contacts_get", JSONObject().put("id", made.getLong("id"))).getJSONObject("contact").getJSONArray("accounts").getString(0))
    }

    @Test fun badArgumentsAreRefused() {
        assertEquals("BAD_ARGUMENT", code("contacts_search", JSONObject().put("query", " ")))
        assertEquals("BAD_ARGUMENT", code("contacts_search", JSONObject().put("query", "ana").put("limit", 99)))
        assertEquals("BAD_ARGUMENT", code("contacts_get", JSONObject().put("id", "1")))
        assertEquals("BAD_ARGUMENT", code("contacts_birthdays", JSONObject().put("days", 0)))
        assertEquals("BAD_ARGUMENT", code("contacts_create", JSONObject().put("name", "X").put("phone", "abc")))
        assertEquals("BAD_ARGUMENT", code("contacts_create", JSONObject().put("name", "X").put("email", "nada")))
        assertEquals("BAD_ARGUMENT", code("contacts_create", JSONObject().put("name", "X").put("birthday", "ayer")))
        assertEquals("BAD_ARGUMENT", code("contacts_create", JSONObject().put("name", "X").put("delete_all", true)))
        assertEquals("BAD_ARGUMENT", code("contacts_favorite", JSONObject().put("id", 1).put("favorite", "sí")))
        assertEquals(6, memory.list().size)
    }

    @Test fun notFoundNoPermissionAndReadOnly() {
        assertEquals("NOT_FOUND", code("contacts_get", JSONObject().put("id", 99)))
        assertEquals("NOT_FOUND", code("contacts_favorite", JSONObject().put("id", 99).put("favorite", true)))
        assertEquals("UNKNOWN_TOOL", code("contacts_delete", JSONObject().put("id", 1)))
        assertEquals("READ_ONLY", code("contacts_create", JSONObject().put("name", "X"), reader))
        memory.allowed = false
        assertEquals("NO_PERMISSION", code("contacts_search", JSONObject().put("query", "ana")))
    }

    @Test fun wrongAndPausedTokensAndHubLink() = runBlocking {
        val body = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), body).status)
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer con_wrong"), emptyMap(), body).status)
        assertEquals(200, handler.handle("POST", "/mcp", emptyMap(), mapOf("token" to reader.secret), body).status)
        assertTrue(Token.secret().startsWith("con_"))
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }
}
