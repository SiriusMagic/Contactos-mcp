# Contactos (contactos MCP)

Contactos para Android en negro con acento rosa. Parte de la familia de Stone, Luma, Calculadora, Notas, Filem, Brújula,
Nivel, Reloj, Calendario, Inicio, QR, Clima y Redes: tu IA (Claude, ChatGPT…) puede **buscar tus contactos y crear
nuevos** por MCP.

## Estado (0.1.0)

- Los **contactos del teléfono** (Google, SIM o teléfono), donde ya estén: Contactos no hace copias propias.
- **Lista A–Z** con barra de letras para saltar (Ñ aparte, tildes ignoradas), **favoritos** arriba y **cumpleaños
  próximos** (14 días, con la edad que cumplen).
- **Buscar** por nombre (cualquier palabra, sin tildes), número (099… y +593… son lo mismo) o correo.
- **Ficha:** Llamar, Mensaje, Correo y Compartir (tarjeta .vcf); cada número (formato de Ecuador), correo, cumpleaños,
  dirección (abre el mapa) y nota; mantener pulsado copia. Favorito, Editar y Eliminar (con confirmación).
- **Nuevo / Editar:** nombre, varios números y correos, empresa, cumpleaños (año opcional), dirección y nota.
- **Ajustes:** ordenar por nombre o apellido, **contactos repetidos** (mismo número o mismo nombre), **Modo MCP**, Temas,
  privacidad y versión.
- **Modo MCP:** `contacts_search`, `contacts_get`, `contacts_favorites`, `contacts_birthdays` y, con token de lectura y
  escritura, `contacts_create` y `contacts_favorite`. La IA **nunca elimina** contactos ni llama o escribe. Tokens
  `con_…`; módulo `contacts_` para Gateway Hub.
- **Resumen para Inicio:** «Cumpleaños» de la semana en la pantalla «Hoy» (solo nombres y días, nunca números).
- Sin anuncios ni estadísticas; internet solo para el Modo MCP (apagado de fábrica).

APK: [`android/Contactos-0.1.0.apk`](android/Contactos-0.1.0.apk) (`com.zirakusa.contactos`, firmado como el resto de la
familia y Gateway Hub).

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

## Siguiente

Ver [`ROADMAP.md`](ROADMAP.md).
