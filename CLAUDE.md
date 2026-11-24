# Tienda (la tienda de la familia): reglas para agentes

- Lee `ARQUITECTURA.md` (y el contrato común `FAMILIA.md` del repo de Inicio) y abre solo los archivos que necesites.
- Pruebas: `cd android && ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` (debe pasar todo).
  Nunca desactives ni saltes una prueba. Lint sin avisos nuevos. Las pruebas no usan internet (`FakeEngine`).
- Cada versión: sube `versionCode` y `versionName`, copia el APK a `android/Tienda-<versión>.apk` (borra el anterior),
  anótalo en `CHANGELOG.md` y ejecuta `python3 tools/update_catalog.py` para que `catalog.json` la anuncie.
- Tras la versión de cualquier otra app de la familia: `python3 tools/update_catalog.py` (lee los repos locales, no los
  toca; con `--check` comprueba que cada APK ya está en GitHub) y sube `catalog.json`. `CatalogTest` lo valida.
- Reglas puras (catálogo, versiones y estado de cada fila, direcciones permitidas, orden de «Actualizar todo», firma)
  en `data/`, sin Android y con pruebas. Las pantallas solo pintan y avisan. Todo lo de Android vive en `Platform`,
  `Installer`, `Net` y `AndroidEngine`.
- Internet: solo `Net`, solo https y solo `raw.githubusercontent.com` (`AllowedHost`, también en cada redirección).
  Nunca otra dirección, aunque venga en el catálogo. La dirección base es una sola constante (`Sources.BASE`).
- Firma obligatoria: antes de instalar, `ApkCheck.verify` (mismo paquete, mismo versionCode y exactamente la misma firma
  que Tienda). **Nunca** se instala algo con otra firma, ni se añade una forma de saltarse la comprobación.
- Tienda nunca instala sola: siempre tras un toque del usuario («Instalar», «Actualizar», «Actualizar todo»), con el
  instalador de Android. El APK descargado se borra siempre. Tienda se actualiza la última.
- Privacidad: sin anuncios ni estadísticas; solo guarda el último catálogo (fuera de copias). No envía nada.
- Prohibido: `eval`, motores de scripts, `QUERY_ALL_PACKAGES`, SQL concatenado.
- Firma: la de depuración del entorno, igual que el resto de la familia y Gateway Hub.
