# Arquitectura de Tienda

Tienda descarga `catalog.json` (raíz de este repo) y, cuando el usuario lo pide, el APK de cada app de la familia; lo
comprueba y lo instala con el instalador de Android.

Carpeta = package (`android/app/src/main/java/com/zirakusa/tienda/`):

| Carpeta | Qué hay |
|---|---|
| `data/` | Lógica pura, con pruebas. `Sources` (`AllowedHost`: solo https a `raw.githubusercontent.com`; `Sources.BASE` y el repo con su nombre nuevo `Tienda-mcp` antes que el viejo `Contactos-mcp`, para el catálogo y el APK de Tienda). `Catalog` (`CatalogParser`: un archivo roto es error; una app con un campo malo, repetida o con un APK fuera de `AllowedHost` se deja fuera). `Versions` (`compareNames`, `RowState` por versionCode: No instalada, Al día, Actualizar; `UpdateAll.order`: Tienda la última). `ApkCheck` (`verify`: paquete, versionCode y firma igual a la de Tienda; `Problem`). |
| `ui/` | `StoreState` (único dueño del estado: filas, carga, error del catálogo, instalación en curso y cola, fallo, notas abiertas, permiso) y la interfaz `Engine` (lo que la app hace fuera de la pantalla; `FakeEngine` en pruebas). |
| `ui/screens/` | `AppsScreen` (cabecera con «Actualizar todo» y «Recargar», avisos y lista que se recarga deslizando), `AppRowView` (punto de color con la inicial, versiones, botón, progreso y novedades) y `Notices` (permiso, mensajes de error claros por cada `Problem`). Solo pintan y avisan. |
| `ui/theme/` | `StoreColors` (sigue al teléfono: negro de noche, claro de día; acento verde). |
| `Net.kt` | OkHttp con un guardián (`AllowedHost`) en cada petición y cada redirección; texto con tope y descarga con progreso. |
| `Platform.kt` | PackageManager: versión instalada, firmas (propias y del APK: `GET_SIGNING_CERTIFICATES` en 28+, `GET_SIGNATURES` antes), «Instalar apps desconocidas», abrir una app. |
| `Installer.kt` | Sesión de `PackageInstaller`, `InstallReceiver` (no exportado) e `Installs` (respuestas por sesión). En 31+, `USER_ACTION_NOT_REQUIRED` para apps que Tienda ya instaló. |
| `AndroidEngine.kt` | El `Engine` real: catálogo (y su copia para abrir sin internet), descarga a `cache/apks`, `ApkCheck.verify`, instalación y borrado del APK. |
| `MainActivity.kt` | Crea el estado y vuelve a leer todo al volver a la app (tras Ajustes o una instalación). |

`catalog.json`: `{"format": 1, "apps": [{id, name, package, versionCode, versionName, apk, notes, color}]}`; `apk` es la
dirección completa en `raw.githubusercontent.com`, `color` es `#RRGGBB`. Lo genera `tools/update_catalog.py` leyendo
cada APK (`aapt2 dump badging`) y su `CHANGELOG.md` en las carpetas locales.

Reglas:
- Texto visible en `res/values/strings.xml` (español) y `values-en`.
- Colores siempre de `StoreColors`.
- Archivos de pantalla de unas 150 líneas como mucho; si crece, se divide.
- `ScreenshotsTest` usa un catálogo falso (`sampleCatalog`) y `FakeEngine`; nada sale a internet.
