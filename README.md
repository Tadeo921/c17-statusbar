# Status C17 · Adaptación comunitaria al español

> Aviso: Esta adaptacion no es oficial. La traduccion de la documentacion y de los textos de interfaz se prepara con ayuda de inteligencia artificial (IA) y revision parcial. Puede contener errores o cadenas sin traducir. No representa al desarrollador original.

Proyecto original: [SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar) · Autor original acreditado: aiingjie  

Versión de referencia: 1.8.3 · Licencia declarada: GPL-3.0-only

Status C17 es un módulo de personalización de SystemUI para ColorOS 17, adaptado principalmente tomando como referencia un OnePlus 13. Permite personalizar la barra de estado, las notificaciones, el Centro de control, Fluid Cloud y la pantalla de bloqueo. La aplicación de ajustes ofrece interfaces COUI y Miuix y utiliza LibXposed API 101.

## Estado de la traducción

Este fork conserva el historial del proyecto original y organiza una adaptación al español. La traducción del código fuente se está incorporando por etapas; no se afirma que todos los archivos estén traducidos ni que exista un APK traducido compilado y verificado por este fork. Los cambios necesitan revisión y pruebas en un dispositivo compatible.

Algunos identificadores internos, claves de configuración y marcadores de plantillas deben conservarse sin traducir para evitar incompatibilidades.

## Funciones del proyecto original

Personalización de la barra de estado, reloj, red, señal y batería.

Gestión de iconos, fuentes, colores, notificaciones y componentes de texto.

- Ajustes del Centro de control, reproductor multimedia, Fluid Cloud y cápsula de cámara.

- Opciones para pantalla de bloqueo, Always-On Display y fondos del panel desplegable.

Importación/exportación de configuraciones, diagnósticos y registros.

- Funciones auxiliares opcionales de optimización mediante root.

Algunas funciones son experimentales o dependen de la ROM. Haz una copia de seguridad antes de modificar componentes del sistema.

## Descargas e instalación

Las versiones publicadas por el proyecto original están en [GitHub Releases](https://github.com/SANWU5/c17-statusbar/releases). Esas versiones no son compilaciones de este fork y pueden no incluir traducción al español.

El módulo original se utiliza mediante un gestor compatible con LSPosed y requiere habilitar el módulo para SystemUI. La compatibilidad concreta depende del dispositivo y de la ROM.

## Compilación

El proyecto usa JDK 17, Gradle Wrapper y Android SDK. En Windows, abre una terminal en la raíz del código fuente y ejecuta:

```bat
gradlew.bat assembleDebug
```

Una compilación correcta no sustituye las pruebas en un dispositivo real.

## Créditos y licencia

Se conservan los créditos y la licencia del proyecto original. Consulta [LICENSE](LICENSE), [COPYRIGHT.md](COPYRIGHT.md) y [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Los recursos de terceros conservan sus propias licencias.

- Proyecto original: [SANWU5/c17-statusbar](https://github.com/SANWU5/c17-statusbar).

- La adaptación aquí descrita usa IA para apoyar la traducción y requiere revisión humana.

Documentación original en chino: [README.zh-CN.md](README.zh-CN.md)

Documentación original en inglés: [README.en.md](README.en.md)

- Notas de traducción: [TRADUCCION-ES.md](TRADUCCION-ES.md)
