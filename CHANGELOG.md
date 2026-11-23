# Contactos 0.2.0

- Lista con título grande y el número de contactos, aviso «Se encontraron contactos repetidos» con **Combinar** (✕ lo
  oculta hasta que haya repetidos nuevos), **Mi perfil** arriba y el **abecedario completo** a la derecha (A–Z, Ñ, #):
  toca o desliza; una letra sin nadie salta a la siguiente.
- Marca en la foto de los contactos guardados en la **SIM** (con su número de ranura) o en **Google**.
- **Combinar** contactos repetidos (uno a uno o todos) con la unión de Android: no se borra nada. **Desvincular** los
  separa de nuevo.
- **Nuevo contacto › Guardar en:** teléfono, SIM o Google. Si una persona está en varias cuentas, **Editar** pregunta
  cuál editar y solo cambia esa.
- **Compartir contacto:** tarjeta con su **código QR** (vCard) para añadirlo desde otro teléfono; **Guardar** la imagen
  en Imágenes › Contactos o **Compartir** como tarjeta .vcf, imagen o texto.
- Menú ⋮ de la ficha: Favoritos, Compartir, **Tono de llamada**, **Bloquear contacto** (abre la lista de bloqueados del
  teléfono si Contactos no puede bloquear), Desvincular y Eliminar contacto (con confirmación). La ficha dice dónde está
  guardado.
- **Ajustes:** Contactos para mostrar (todas o una cuenta), Almacenamiento predeterminado, Ocultar los contactos
  repetidos, Ordenar por, **Formato del nombre** (nombre o apellido primero), **Importar/Exportar contactos (.vcf)**
  (también los .vcf de Android con tildes) y Números bloqueados.
- Modo MCP: `contacts_get` dice en qué tipo de cuenta está (sin nombres de cuenta) y `contacts_create` guarda en el
  almacenamiento predeterminado. La IA sigue sin poder eliminar, combinar, llamar ni escribir.
- 15 pruebas.

# Contactos 0.1.0

Primera versión: los contactos del teléfono en negro con acento rosa.
- Lista A–Z con barra de letras, favoritos y cumpleaños próximos; búsqueda por nombre, número o correo.
- Ficha con llamar, mensaje, correo y compartir (.vcf); crear, editar, favorito y eliminar.
- Orden por nombre o apellido y contactos repetidos.
- Modo MCP (`con_`, módulo `contacts_`): `contacts_search`, `contacts_get`, `contacts_favorites`, `contacts_birthdays`,
  `contacts_create`, `contacts_favorite`. La IA nunca elimina.
- Resumen «Cumpleaños» para la pantalla «Hoy» de Inicio.
- 11 pruebas.
