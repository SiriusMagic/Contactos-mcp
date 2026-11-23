# Contactos (contactos MCP)

Contactos para Android en negro con acento rosa. Parte de la familia de Stone, Luma, Calculadora, Notas, Filem, Brújula,
Nivel, Reloj, Calendario, Inicio, QR, Clima y Redes: tu IA (Claude, ChatGPT…) puede **buscar tus contactos y crear
nuevos** por MCP.

## Estado (0.2.0)

- Los **contactos del teléfono** (Google, SIM o teléfono), donde ya estén: Contactos no hace copias propias.
- **Lista A–Z** con el número de contactos y el abecedario completo a la derecha para saltar (Ñ aparte, tildes
  ignoradas), **Mi perfil**, **favoritos** y **cumpleaños próximos** (14 días, con la edad que cumplen). Marca de SIM o
  Google en la foto. Aviso de **repetidos** con «Combinar» (unión de Android: nada se borra; «Desvincular» la deshace).
- **Buscar** por nombre (cualquier palabra, sin tildes), número (099… y +593… son lo mismo) o correo.
- **Ficha:** Llamar, Mensaje, Correo y Compartir; cada número (formato de Ecuador), correo, cumpleaños, dirección (abre
  el mapa), nota y dónde está guardado; mantener pulsado copia. Menú ⋮: Favoritos, Compartir, Tono de llamada, Bloquear,
  Desvincular y Eliminar (con confirmación).
- **Compartir:** tarjeta con código QR (vCard); Guardar la imagen o compartir .vcf, imagen o texto.
- **Nuevo / Editar:** «Guardar en» (teléfono, SIM o Google), nombre, varios números y correos, empresa, cumpleaños (año
  opcional), dirección y nota. Si la persona está en varias cuentas, se elige cuál editar.
- **Ajustes:** contactos para mostrar, almacenamiento predeterminado, **contactos repetidos** (combinar u ocultar),
  ordenar y formato del nombre, importar/exportar .vcf, números bloqueados, **Modo MCP**, Temas, privacidad y versión.
- **Modo MCP:** `contacts_search`, `contacts_get`, `contacts_favorites`, `contacts_birthdays` y, con token de lectura y
  escritura, `contacts_create` y `contacts_favorite`. La IA **nunca elimina** contactos ni llama o escribe. Tokens
  `con_…`; módulo `contacts_` para Gateway Hub.
- **Resumen para Inicio:** «Cumpleaños» de la semana en la pantalla «Hoy» (solo nombres y días, nunca números).
- Sin anuncios ni estadísticas; internet solo para el Modo MCP (apagado de fábrica).

APK: [`android/Contactos-0.2.0.apk`](android/Contactos-0.2.0.apk) (`com.zirakusa.contactos`, firmado como el resto de la
familia y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).
