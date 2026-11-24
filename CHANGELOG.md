# Tienda 0.1.0

Primera versión: la tienda propia de la familia.
- **Apps**: una fila por app (Stone, Stone Video, Gateway Hub, Teléfono, SMS, Reloj, Calendario, Notas, Clima, Cámara,
  QR, Redes, Brújula, Nivel, Calculadora, Filem, Luma, Inicio y la propia Tienda) con su color, la versión instalada y la
  nueva («Instalada 0.3.1 · Nueva 0.3.2») y el botón Instalar, Actualizar o Abrir. Al tocar una fila, sus novedades.
- **Actualizar todo**: una app tras otra; Tienda, la última. Recargar con el botón o deslizando hacia abajo.
- **Seguro**: el catálogo (`catalog.json` de este repo) y los APK solo de raw.githubusercontent.com por https. Antes de
  instalar comprueba que el APK es la app y la versión del catálogo y que está firmado igual que Tienda; si no, no se
  instala. Instala con el instalador de Android y borra el archivo descargado.
- `tools/update_catalog.py` regenera el catálogo desde las carpetas de las apps.
- Sin anuncios ni estadísticas. 26 pruebas (catálogo, versiones, direcciones, firma, estado y capturas).
