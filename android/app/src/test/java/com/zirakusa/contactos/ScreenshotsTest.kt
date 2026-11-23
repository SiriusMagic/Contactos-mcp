package com.zirakusa.contactos

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.contactos.data.MapValues
import com.zirakusa.contactos.data.MemoryStore
import com.zirakusa.contactos.ui.ContactosApp
import com.zirakusa.contactos.ui.ContactosState
import com.zirakusa.contactos.ui.Page
import com.zirakusa.contactos.ui.theme.ContactosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Every screen, painted: the list, a search, a contact, the editor, duplicates, Ajustes and the permission card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()
    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/contactos-$name.png") }

    @Test fun allScreens() {
        val store = MemoryStore(People.all)
        val state = ContactosState(MapValues(), store) { LocalDate.of(2026, 11, 21) }
        state.load()
        compose.setContent { ContactosTheme(state.themeMode, state.accent) { ContactosApp(state) } }
        compose.onNodeWithText("Cumpleaños próximos").assertExists()
        shot("list")
        state.query = "099"
        compose.onNodeWithText("099 123 4567").assertExists()
        shot("search")
        state.query = ""
        compose.onNodeWithText("Carlos Mora").performClick()
        state.open(People.ana)
        compose.onNodeWithText("Zirakusa").assertExists(); compose.onNodeWithText("Cumpleaños · cumple 35").assertExists()
        shot("detail")
        compose.onNodeWithText("Editar").performClick()
        compose.onNodeWithText("Guardar").assertExists()
        shot("edit")
        state.page = Page.SETTINGS
        compose.onNodeWithText("Contactos repetidos").performClick()
        compose.onNodeWithText("Anita P.").assertExists()
        shot("duplicates")
        store.allowed = false; state.load(); state.page = Page.LIST
        compose.onNodeWithText("Permitir").assertExists()
        shot("permission")
    }
}
