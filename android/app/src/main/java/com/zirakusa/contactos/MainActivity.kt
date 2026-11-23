package com.zirakusa.contactos

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.zirakusa.contactos.data.Contact
import com.zirakusa.contactos.data.SharedValues
import com.zirakusa.contactos.data.VCard
import com.zirakusa.contactos.mcp.McpMode
import com.zirakusa.contactos.ui.AppActions
import com.zirakusa.contactos.ui.ContactosApp
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosColors
import com.zirakusa.contactos.ui.theme.ContactosTheme
import java.io.File

/** Creates the state, asks for the contacts, and does what the screens ask: dial, write, email, map, share and copy. */
class MainActivity : ComponentActivity() {
    private lateinit var state: ContactosState
    private val allow = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { state.load() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        state = ContactosState(SharedValues(this), AndroidStore(this))
        McpMode.onChanged = { state.load() }
        McpMode.load(this)
        val actions = AppActions(allow = ::ask, call = { open(Intent(Intent.ACTION_DIAL, "tel:${android.net.Uri.encode(it)}".toUri())) },
            sms = { open(Intent(Intent.ACTION_SENDTO, "smsto:${android.net.Uri.encode(it)}".toUri())) },
            email = { open(Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).putExtra(Intent.EXTRA_EMAIL, arrayOf(it))) },
            map = { open(Intent(Intent.ACTION_VIEW, "geo:0,0?q=${android.net.Uri.encode(it)}".toUri())) },
            share = ::share, copy = { copy("Contacto", it, R.string.copied) }, copyLink = { copy("Contactos MCP", it, R.string.copied) }, goToHub = ::goToHub)
        state.load()
        if (!state.allowed) ask()
        setContent {
            ContactosTheme(state.themeMode, state.accent) {
                val light = ContactosColors.isLight
                SideEffect { WindowCompat.getInsetsController(window, window.decorView).run { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light } }
                BackHandler(state.page != Page.LIST || state.query.isNotEmpty()) {
                    when (state.page) {
                        Page.LIST -> state.query = ""; Page.EDIT -> state.cancelEdit(); Page.MCP, Page.DUPLICATES -> state.page = Page.SETTINGS
                        else -> state.page = Page.LIST
                    }
                }
                ContactosApp(state, actions)
            }
        }
    }

    override fun onResume() { super.onResume(); if (state.page != Page.EDIT) state.load() }

    private fun ask() = allow.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))

    private fun open(intent: Intent) = try { startActivity(intent) } catch (_: ActivityNotFoundException) { Toast.makeText(this, R.string.no_app, Toast.LENGTH_SHORT).show() }

    /** The contact as a .vcf card, for any app the user picks. */
    private fun share(c: Contact) {
        val file = File(File(cacheDir, "share").apply { mkdirs() }, "contacto.vcf").apply { writeText(VCard.build(c)) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/x-vcard").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(c.shownName, uri)
        open(Intent.createChooser(send, getString(R.string.share)))
    }

    private fun copy(label: String, text: String, message: Int) {
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /** «Ir al Hub»: copies the hub's link (in case it must be pasted) and opens the Gateway Hub app. */
    private fun goToHub() {
        copy("Contactos › Gateway Hub", McpMode.hubLink(), R.string.copied)
        val hub = packageManager.getLaunchIntentForPackage("com.zirakusa.gatewayhub")
        if (hub != null) startActivity(hub) else Toast.makeText(this, R.string.mcp_paste_in_hub, Toast.LENGTH_LONG).show()
    }
}
