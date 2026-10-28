# Status C17 Â· adaptaciÃ³n comunitaria al espaÃ±ol

> **Aviso:** Esta adaptaciÃ³n no es oficial. La traducciÃ³n de la documentaciÃ³n y de los textos de interfaz se preparÃ³ con ayuda de inteligencia artificial (IA) y revisiÃ³n parcial. Puede contener errores o cadenas sin traducir. No representa al desarrollador original.

**Proyecto original:** [SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar) Â· **Autor original acreditado:** aiingjie  
**VersiÃ³n de referencia:** 1.8.3 Â· **Licencia declarada:** GPL-3.0-only

Status C17 es un mÃ³dulo de personalizaciÃ³n de SystemUI para ColorOS 17, adaptado principalmente tomando como referencia un OnePlus 13. Permite personalizar la barra de estado, las notificaciones, el Centro de control, Fluid Cloud y la pantalla de bloqueo. La aplicaciÃ³n de ajustes ofrece interfaces COUI y Miuix y utiliza LibXposed API 101.

## Estado de la traducciÃ³n

Este fork conserva el historial del proyecto original y organiza una adaptaciÃ³n al espaÃ±ol. La traducciÃ³n del cÃ³digo fuente se estÃ¡ incorporando por etapas; **no se afirma que todos los archivos estÃ©n traducidos ni que exista un APK traducido compilado y verificado por este fork**. Los cambios necesitan revisiÃ³n y pruebas en un dispositivo compatible.

Algunos identificadores internos, claves de configuraciÃ³n y marcadores de plantillas deben conservarse sin traducir para evitar incompatibilidades.

## Funciones del proyecto original

- PersonalizaciÃ³n de la barra de estado, reloj, red, seÃ±al y baterÃ­a.
- GestiÃ³n de iconos, fuentes, colores, notificaciones y componentes de texto.
- Ajustes del Centro de control, reproductor multimedia, Fluid Cloud y cÃ¡psula de cÃ¡mara.
- Opciones para pantalla de bloqueo, Always-On Display y fondos del panel desplegable.
- ImportaciÃ³n/exportaciÃ³n de configuraciones, diagnÃ³sticos y registros.
- Funciones auxiliares opcionales de optimizaciÃ³n mediante Root.

Algunas funciones son experimentales o dependen de la ROM. Haz una copia de seguridad antes de modificar componentes del sistema.

## Descargas e instalaciÃ³n

Las versiones publicadas por el proyecto original estÃ¡n en [GitHub Releases](https://github.com/SANWU5/c17-statusbar/releases). Esas versiones **no son compilaciones de este fork** y pueden no incluir traducciÃ³n al espaÃ±ol.

El mÃ³dulo original se utiliza mediante un gestor compatible con LSPosed y requiere habilitar el mÃ³dulo para SystemUI. La compatibilidad concreta depende del dispositivo y de la ROM.

## CompilaciÃ³n

El proyecto usa JDK 17, Gradle Wrapper y Android SDK. En Windows, abre una terminal en la raÃ­z del cÃ³digo fuente y ejecuta:

```bat
gradlew.bat assembleDebug
```

Una compilaciÃ³n correcta no sustituye las pruebas en un dispositivo real.

## CrÃ©ditos y licencia

Se conservan los crÃ©ditos y la licencia del proyecto original. Consulta [LICENSE](LICENSE), [COPYRIGHT.md](COPYRIGHT.md) y [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Los recursos de terceros conservan sus propias licencias.

- Proyecto original: [SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar).
- La adaptaciÃ³n aquÃ­ descrita usa IA para apoyar la traducciÃ³n y requiere revisiÃ³n humana.
- DocumentaciÃ³n original en chino: [README.zh-CN.md](README.zh-CN.md).
- DocumentaciÃ³n original en inglÃ©s: [README.en.md](README.en.md).
- Notas de traducciÃ³n: [TRADUCCION-ES.md](TRADUCCION-ES.md).
