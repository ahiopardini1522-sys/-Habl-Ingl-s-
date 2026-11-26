# Hablá Inglés

App Android para aprender inglés desde el español, estilo Duolingo, con Pico el hornero.

- 25 unidades (de A1 a B1): lecciones, historias, cofres y nivel Leyenda.
- Palabras nuevas con dibujo, ejercicios con imágenes, armar frases, dictados, hablar y conversaciones con personajes.
- Prueba de nivel y dificultad que se adapta sola.
- Voz nativa de Android en inglés y en español, y micrófono para practicar pronunciación.
- Curso de portugués (14 unidades) además del de inglés, con voz y micrófono en portugués.
- Widget para la pantalla de inicio con la racha y la palabra del día.
- Recordatorio diario con notificación y modo oscuro que sigue al celular.
- Rachas, misiones diarias, liga semanal, logros, tienda y Pico personalizable.

## Descargar
El APK está en **Releases** → `HablaIngles.apk`. Cada cambio en `main` compila uno nuevo automáticamente.

## Se actualiza sola
- La app entera vive en `app/src/main/assets/index.html`. Cada vez que se abre, la app instalada descarga la última versión de ese archivo desde este repositorio y muestra «ACTUALIZAR». No hace falta reinstalar.
- Para que el celular note el cambio hay que subir el número de `<meta name="app-version">` dentro de `index.html`.
- Si cambia el código Android (`MainActivity.java`, el manifiesto, etc.), subir el número de `native-version.txt` y `NATIVE_VERSION` en `MainActivity.java`: la app avisa que hay un APK nuevo para descargar e instalar encima, sin perder el progreso.
