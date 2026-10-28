# TraducciÃ³n al espaÃ±ol â€” Status C17

AdaptaciÃ³n comunitaria no oficial de [SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar), versiÃ³n de referencia 1.8.3.

## Uso de inteligencia artificial

La traducciÃ³n al espaÃ±ol se preparÃ³ con ayuda de inteligencia artificial (IA) y revisiÃ³n parcial. Puede contener errores, expresiones poco naturales o elementos que sigan apareciendo en inglÃ©s o chino. No es oficial ni cuenta con el respaldo del autor original.

## Alcance y compatibilidad

La localizaciÃ³n busca traducir textos visibles de la interfaz, ajustes, mensajes, diagnÃ³sticos y documentaciÃ³n. Se conservan identificadores, claves JSON, nombres de recursos y marcadores de plantillas cuando traducirlos pudiera romper la compatibilidad.

**No se debe asumir que todos los archivos estÃ¡n traducidos, que la aplicaciÃ³n compila o que la adaptaciÃ³n fue probada en un telÃ©fono.** No se afirma que este fork proporcione un APK traducido verificado.

## Compilar localmente

Con JDK 17 y Android SDK configurados, abre una terminal en la raÃ­z del proyecto y ejecuta:

```bat
gradlew.bat assembleDebug
```

Si termina correctamente, el APK de depuraciÃ³n deberÃ­a estar en `app/build/outputs/apk/debug/app-debug.apk`. Revisa los registros y prueba con cuidado en un dispositivo compatible; no instales mÃ³dulos que alteran SystemUI sin disponer de una vÃ­a de recuperaciÃ³n.

## CrÃ©ditos y licencia

Se conservan los crÃ©ditos y la licencia declarada del proyecto original: **GPL-3.0-only**. Consulta [LICENSE](LICENSE), [COPYRIGHT.md](COPYRIGHT.md) y [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Los recursos de terceros estÃ¡n sujetos a sus respectivas licencias.
