# TuneTV by GeoL

**Quita la publicidad y las apps basura de tu Android TV o Google TV desde el teléfono, en español y en pocos pasos.**

> 🍴 **TuneTV es una bifurcación (fork) de [CleanTV by VGS](https://github.com/vgscity/vgs-cleantv), de AlexVGS (Vietnam Gaming Setup), que a su vez se basa en [CleanYourTV](https://github.com/Gathin23/cleanyourtv) de Gathin.** Todo el mérito de la app es de ellos: la conexión con la TV, las listas de cada modelo, los respaldos y el inicio limpio son su trabajo. TuneTV solo la traduce al español y cambia algunos detalles.

## Quién la hizo

No soy programador, solo alguien con tiempo libre que quería que más gente pudiera limpiar su TV sin complicarse. La traducción y los cambios los hice conversando con **Claude** (la IA de Anthropic).

## Qué cambia respecto a CleanTV

- 🇪🇸 **Toda la app en español** (solo español).
- 🔕 **Sin conteos anónimos:** la app no envía estadísticas por su cuenta.
- ⏸️ **Sin actualización automática** desde el servidor del autor original, para que no se reemplace por la versión en inglés.
- 🌎 Ciudades de Latinoamérica y España para el clima.
- 🏷️ Nombre propio (TuneTV) y paquete propio, así que se puede instalar al lado de CleanTV.
- 🤖 **Se compila sola en GitHub** cada vez que hay un cambio.

## Descargar

Ve a **[Releases → Última versión](../../releases/tag/ultima)** y descarga el archivo `.apk`. Ábrelo en tu teléfono Android (o en la TV) y acepta instalar apps de origen desconocido si te lo pide.

## Cómo limpiar tu TV

1. **En la TV:** *Ajustes → Sistema → Acerca de* y pulsa OK **7 veces** sobre *Compilación*. Luego, en *Opciones de desarrollador*, activa **Depuración USB**. La app trae una guía con imágenes.
2. **Teléfono y TV en el mismo Wi‑Fi.** Abre TuneTV y pulsa **Limpiar una TV**.
3. **Elige tu TV** de la lista (o escribe su IP). En la TV, acepta el aviso "¿Permitir depuración?" marcando **Permitir siempre**.
4. **Revisa la lista** de apps a desactivar. Las seguras ya vienen marcadas; desmarca las que uses.
5. Pulsa **Respaldar y limpiar**. Al final, **apaga la depuración USB** en la TV.

Nada se desinstala: todo se puede deshacer con **Restaurar**.

## Funciona en

Android TV y Google TV (Xiaomi, TCL, Hisense, Sony y la mayoría). **No** sirve en Samsung (Tizen), LG (webOS) ni Fire TV (en Fire TV solo limpia, no cambia el inicio).

## Compilar

GitHub la compila sola con [`.github/workflows/compilar.yml`](.github/workflows/compilar.yml). Para compilarla tú: JDK 17 y Android SDK (compileSdk 35), luego `./gradlew :app:assembleRelease`.

La APK se firma con una llave pública incluida en `firma/`, para que cada versión se instale encima de la anterior. Como es pública, úsala para ti y tu familia; si vas a repartirla a mucha gente, cámbiala por una llave privada.

## Privacidad

TuneTV no envía nada por su cuenta. Solo se conecta a internet para el clima (Open-Meteo) y cuando tú pulsas un botón que abre una página o envía una lista.

## Créditos y licencia

- **[CleanTV by VGS](https://github.com/vgscity/vgs-cleantv)**, de AlexVGS / Vietnam Gaming Setup. Licencia MIT. Ver [NOTICE](NOTICE) y [README-original.md](README-original.md).
- **[CleanYourTV](https://github.com/Gathin23/cleanyourtv)**, de Gathin y colaboradores: el inicio limpio y las listas de TV. Licencia MIT.
- Traducción y cambios: **[GeoL21](https://github.com/GeoL21)** con ayuda de **Claude (Anthropic)**.
- Licencia MIT, igual que el original ([LICENSE](LICENSE)).
