package com.zirakusa.tienda

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.zirakusa.tienda.data.Problem
import com.zirakusa.tienda.ui.Engine
import com.zirakusa.tienda.ui.Failure
import com.zirakusa.tienda.ui.FailedApp
import com.zirakusa.tienda.ui.Phase
import com.zirakusa.tienda.ui.StoreState
import com.zirakusa.tienda.ui.screens.AppsScreen
import com.zirakusa.tienda.ui.theme.StoreTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String) { compose.waitForIdle(); onView(isRoot()).captureRoboImage("build/screenshots/tienda-$name.png") }
    private fun show(state: StoreState, dark: Boolean = true) = compose.setContent {
        StoreTheme(dark) { if (state.page == com.zirakusa.tienda.ui.Page.SETTINGS) com.zirakusa.tienda.ui.screens.SettingsScreen(state) { com.zirakusa.tienda.ui.screens.IconCard(AppIcon.SPARK) {} } else AppsScreen(state) }
    }

    @Test fun settings() {
        val state = StoreState(FakeEngine(), CoroutineScope(Dispatchers.Unconfined), "com.zirakusa.tienda").apply { start() }
        show(state)
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNodeWithText("Solo con Wi‑Fi").assertExists()
        compose.onNodeWithText("Store 0.1.0").assertExists()
        compose.onNodeWithText("Osa Mayor").assertExists()
        shot("settings")
        compose.onNodeWithText("Mostrar no instaladas").performClick()
        assertEquals(false, state.showNotInstalled)
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("Instalar").assertDoesNotExist()
        shot("apps-installed-only")
    }

    @Test fun apps() {
        val engine = FakeEngine()
        val state = StoreState(engine, CoroutineScope(Dispatchers.Unconfined), "com.zirakusa.tienda").apply { start() }
        show(state)
        compose.onNodeWithText("Actualizaciones disponibles").assertExists()
        compose.onNodeWithText("Herramientas · 0.3.1 → 0.3.2").assertExists()
        compose.onNodeWithText("12,4 MB").assertExists()
        assertEquals(2, compose.onAllNodes(androidx.compose.ui.test.hasText("Versión 0.1.1")).fetchSemanticsNodes().size)   // Clima and QR
        compose.onNodeWithText("Esta app · 0.1.0").assertDoesNotExist()
        compose.onNodeWithText("Abrir").assertExists()
        compose.onNodeWithText("Instalar").assertExists()
        compose.onNodeWithContentDescription("Recargar").assertExists()
        shot("apps")
        compose.onNodeWithText("Reloj").performClick()
        compose.onNodeWithText("Novedades de 0.3.2").assertExists()
        compose.onNodeWithText("Desinstalar").assertExists()
        compose.onNodeWithText("Conectar todas, Lucía, Icono de la app").assertExists()
        shot("notes")
        state.failure = FailedApp("Notas", Failure(Problem.WRONG_SIGNATURE))
        compose.onNodeWithText("Notas no se instaló: no está firmado por el mismo desarrollador que Store. Por seguridad, nunca se instala.")
            .assertExists()
        shot("refused")
        compose.onNodeWithText("Cerrar").performClick()
        compose.onNodeWithText("Abrir").performClick()
        assertEquals(listOf("open com.zirakusa.clima"), engine.log)
    }

    /** «Actualizar todo» in progress: Reloj downloading, the rest waiting; then light theme. */
    @Test fun updatingAllAndLight() {
        val gate = CompletableDeferred<Unit>()
        val slow = object : Engine by FakeEngine() {
            override suspend fun install(app: com.zirakusa.tienda.data.CatalogApp, onJob: (Phase, Float) -> Unit): Failure? {
                onJob(Phase.DOWNLOADING, 0.42f); gate.await(); return null
            }
        }
        val state = StoreState(slow, CoroutineScope(Dispatchers.Unconfined), "com.zirakusa.tienda").apply { start() }
        show(state)
        compose.onNodeWithText("Actualizar todo").performClick()
        compose.onNodeWithText("42 % de 12,4 MB").assertExists()
        assertEquals(3, compose.onAllNodes(androidx.compose.ui.test.hasText("Cancelar")).fetchSemanticsNodes().size)
        assertEquals(2, compose.onAllNodes(androidx.compose.ui.test.hasText("En cola")).fetchSemanticsNodes().size)
        shot("updating")
        gate.complete(Unit)
    }

    @Test fun permissionAndOffline() {
        val state = StoreState(FakeEngine(allowed = false, offline = true), CoroutineScope(Dispatchers.Unconfined),
            "com.zirakusa.tienda").apply { start() }
        show(state, dark = false)
        compose.onNodeWithText("Permite «Instalar apps desconocidas»").assertExists()
        compose.onNodeWithText("No se pudo cargar el catálogo. Revisa la conexión y toca Recargar.").assertExists()
        shot("permission-offline-light")
    }

    @Test fun lightList() {
        val state = StoreState(FakeEngine(), CoroutineScope(Dispatchers.Unconfined), "com.zirakusa.tienda").apply { start() }
        show(state, dark = false)
        compose.onNodeWithText("0.1.0 → 0.1.1").assertExists()
        shot("apps-light")
    }
}
