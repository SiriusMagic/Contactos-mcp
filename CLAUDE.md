# Contactos (contactos MCP): reglas para agentes

- Lee `ARQUITECTURA.md` (y el contrato común `FAMILIA.md` del repo de Inicio) y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos.
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Contactos-<versión>.apk` (borra el anterior)
  y anótalo en `CHANGELOG.md`.
- Reglas puras (orden, letras, búsqueda, números de Ecuador, repetidos, cumpleaños, vCard) en `data/`, sin Android y con
  pruebas. Las pantallas solo pintan y avisan. Todo acceso a los contactos pasa por `ContactStore` (`AndroidStore` en la
  app, `MemoryStore` en pruebas).
- Consultas al proveedor de contactos siempre con argumentos `?`; nunca texto pegado en la consulta.
- Privacidad: los contactos se quedan en la cuenta donde están; la app no guarda copias. Inicio solo recibe nombres y días.
  Nada sale salvo por el Modo MCP que el usuario active; internet es solo para eso.
- MCP: la IA puede leer, crear y marcar favoritos; **nunca eliminar** ni llamar o escribir. Cada herramienta nueva lleva
  su prueba en `McpTest`: lista de herramientas, llamada correcta, argumentos malos y «no encontrado». Las que cambian algo
  exigen token de lectura y escritura (`READ_ONLY` si no).
- Prohibido: `eval`, motores de scripts, SQL concatenado.
- Firma: la de depuración del entorno, igual que el resto de la familia y Gateway Hub.
