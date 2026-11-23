# Arquitectura de Contactos

Carpeta = package (`android/app/src/main/java/com/zirakusa/contactos/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Contact` (`Label`, `Phone`, `Email`, `Birthday` con 29 de febrero, `Contact` con iniciales; `Contacts`: orden por nombre o apellido, letras con Ñ, grupos, búsqueda, repetidos, cumpleaños próximos; `Phones`: clave para comparar 0…/+593…, formato de Ecuador, limpiar); `VCard` (crear y leer 3.0); `Store` (`ContactStore`, `NoAccess`, `MemoryStore`, `Settings` con `Order`, `KeyValues`). |
| `ui/` | `ContactosState` (único dueño del estado: contactos, búsqueda, el abierto, el `Draft` que se edita y ajustes) y `ContactosApp` (`AppActions`). |
| `ui/screens/` | `ListScreen` (favoritos, cumpleaños, A–Z con barra de letras, «+»), `DetailScreen`, `EditScreen`, `DuplicatesScreen`, `SettingsScreen`, `Avatar`, `McpScreen`, `Parts`, `Chrome` y `SettingsParts`. |
| `ui/theme/` | `Theme`: `ThemeMode` y `Accent` (rosa de fábrica). |
| `mcp/` | Modo MCP, como las apps hermanas: `Tokens` (`con_…`), `ContactTools` (`contacts_search`, `contacts_get`, `contacts_favorites`, `contacts_birthdays`; con escritura `contacts_create`, `contacts_favorite`; a través de `ContactData`), `McpHandler`, `RelayLink`, `McpMode` (`LiveData`) y `HubLink` (módulo `contacts_`). |
| `AndroidStore.kt` | Los contactos de Android (`ContactsContract`): leer todo de una vez, guardar en lote, eliminar y favoritos. |
| `Summary.kt` | `SummaryLines` (pura) y `SummaryProvider`: cumpleaños de la semana para Inicio. |
| `MainActivity.kt` | Crea el estado, pide permiso, marca, escribe, abre el correo o el mapa, comparte la tarjeta y copia. |
