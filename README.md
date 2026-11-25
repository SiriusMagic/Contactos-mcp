# Tienda (la tienda de la familia)

Instala y actualiza las apps de la familia (Stone, Stone Video, Gateway Hub, Teléfono, SMS, Reloj, Calendario, Notas,
Clima, Cámara, QR, Redes, Brújula, Nivel, Calculadora, Filem, Luma, Inicio y la propia Tienda) desde sus repos de GitHub,
sin copiar APK a mano.

## Estado (0.1.0)

- **Apps:** una fila por app con su color, «Instalada 0.3.1 · Nueva 0.3.2» y el botón **Instalar**, **Actualizar** o
  **Abrir**. Al tocar una fila, sus novedades.
- **Actualizar todo:** una app tras otra (Tienda, la última). **Recargar** con el botón o deslizando hacia abajo.
- **Seguro:** solo descarga de `raw.githubusercontent.com` por https. Antes de instalar comprueba que el APK es la app y
  la versión del catálogo y que está **firmado igual que Tienda**; si no, no se instala y lo explica. El APK descargado
  se borra siempre.
- Sin anuncios ni estadísticas.

APK: [`android/Tienda-0.1.0.apk`](android/Tienda-0.1.0.apk) (`com.zirakusa.tienda`, firmado como el resto de la familia).

## Primeros pasos

1. Este repo ya se llama `tienda-de-app` en GitHub (antes `Contactos-mcp`). La app prueba primero el nombre nuevo y
   después el viejo, así que funcionan las dos.
2. **Instala Tienda a mano una vez**: descarga `android/Tienda-0.1.0.apk` en el teléfono y ábrelo.
3. Al pulsar Instalar o Actualizar por primera vez, Tienda te lleva a Ajustes: **permite «Instalar apps desconocidas»**
   para Tienda. Desde ahí, Tienda se actualiza sola (con un toque) como las demás.

Las apps de la familia instaladas antes a mano se actualizan igual (misma firma); Android puede pedir confirmar cada una.

## Catálogo

`catalog.json` (en la raíz) dice qué versión hay de cada app. Tras publicar una versión de cualquier app:

```bash
python3 tools/update_catalog.py          # lee los APK y CHANGELOG de las carpetas locales
python3 tools/update_catalog.py --check  # además comprueba que cada APK ya está en GitHub
```

## Compilar y probar

```bash
cd android
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

El código anterior de este repo (Contactos) vive ahora dentro de Teléfono; queda en el historial de git.
