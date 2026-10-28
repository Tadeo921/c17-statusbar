// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Presentation only: destinations and folding never project, reset or save runtime settings. */
public final class SettingsOrganization {
    private static final Map<String, String> GUIDES;
    static {
        Map<String, String> guides = new LinkedHashMap<>();
        guides.put("statusbar_layout", "Solo gestiona las zonas y el orden de los elementos; el espaciado izquierdo y derecho se ajusta por separado en cada sección y no requiere activar el orden personalizado.");
        guides.put("speed", "El estilo, el tamaño, la posición y la velocidad de actualización se configuran por separado; el espaciado lateral es independiente del aspecto de la velocidad de red.");
        guides.put("data", "La forma y la disposición de la señal se configuran por separado. Ocultar las insignias de red no afecta al texto independiente del tipo de red; las flechas pequeñas se gestionan en la sección de insignias nativas.");
        guides.put("wifi", "La forma del icono, los indicadores nativos y la posición se configuran por separado. Compartir posición con el tipo de red no cambia sus reglas de visualización.");
        guides.put("label", "Ocultar tiene prioridad sobre reemplazar el texto. Elige entre un nombre común y contenido personalizado; los textos por tipo de red están en los ajustes avanzados.");
        guides.put("native_network_badge", "La insignia 1, la insignia 2 y la flecha pequeña de datos se configuran por separado. El texto del tipo de red pausa las insignias nativas, pero no la flecha.");
        guides.put("clock", "El reloj predeterminado y el reloj de dos líneas son excluyentes. La fuente monoespaciada, los dos puntos y el espaciado lateral de los ajustes generales se aplican a ambos modos.");
        guides.put("carrier", "Las notificaciones, el centro de control y la pantalla de bloqueo tienen interruptores independientes; el espaciado lateral compartido se guarda por separado.");
        guides.put("font", "En «Otros → Configuración de fuentes», selecciona primero la fuente, elige el ámbito y aplícala. Las áreas no seleccionadas conservan su fuente original; el tamaño, el grosor y el color se configuran en cada sección.");
        guides.put("battery", "El estilo, el texto interior, la disposición y los colores están en pestañas separadas; cada color tiene su propio interruptor. Si desactivas el porcentaje del sistema, se conserva el estilo elegido.");
        guides.put("notification_icons", "El modo de visualización determina los parámetros de imágenes, texto e iconos nativos; la cantidad de iconos y los reemplazos para aplicaciones concretas se administran por separado.");
        guides.put("status_hint_icons", "La cantidad visible y la prioridad de conservación se ajustan aquí. La posición izquierda/derecha y el orden individual están en la página de organización. No se fuerzan los iconos que el sistema oculta.");
        guides.put("notification_icon_overrides", "Las reglas por aplicación se guardan por separado; elegir texto o imagen para un elemento concreto pausa estas reglas.");
        guides.put("status_icon_library", "Primero elige una biblioteca de iconos y luego su ámbito de aplicación. La selección individual tiene prioridad; el color y la transparencia se configuran por separado. Al desactivar el interruptor general se pausan los diseños, colores y animaciones, pero se conservan la configuración y las bibliotecas; al reactivarlo se restauran. Los estados no compatibles mantienen el aspecto nativo.");
        guides.put("qs_appearance", "Los colores de fondo y degradados se configuran por separado para los modos claro y oscuro. Los bordes redondeados, el relleno completo y el tamaño de los iconos son funciones independientes.");
        guides.put("tile_2x1_fill", "Solo controla el relleno del fondo de los mosaicos 2×1; el color, las esquinas redondeadas y el comportamiento al tocar se configuran en sus respectivas secciones.");
        guides.put("tile_corners", "Los mosaicos, controles deslizantes, tarjetas multimedia y tarjetas de dispositivos comparten el radio de las esquinas, de 0 a 30 dp.");
        guides.put("tile_icon_size", "Solo cambia el tamaño de los mosaicos 1×1 y los iconos de dispositivos; conserva las áreas táctiles nativas y los anillos de batería.");
        guides.put("qs_entrances", "Los botones Editar y Ajustes se pueden ocultar por separado. Permitir que sigan siendo táctiles después de ocultarlos es una opción adicional independiente.");
        guides.put("shade_clock", "El centro de notificaciones y el centro de control comparten este reloj pequeño. Cuando se muestra el reloj grande de notificaciones, se pausa el reloj pequeño de esa página; el centro de control no se ve afectado.");
        String big = "Los ajustes se agrupan en hora, apariencia, fecha, componentes izquierdo/derecho, animación y parte inferior. La transparencia del fondo tiene prioridad sobre otros materiales; la intensidad de la máscara se guarda por separado para cada texto y solo la hora principal usa un borde con relieve.";
        guides.put("notification_big_clock", big);
        guides.put("notification_big_clock_landscape", "El modo horizontal tiene un interruptor y parámetros propios; " + big.replace("componentes izquierdo/derecho, ", ""));
        guides.put("notification_stack", "La pila de notificaciones de pantalla completa requiere el reloj grande en la orientación correspondiente; el modo horizontal también necesita permiso. La miniagrupación solo se oculta automáticamente cuando la pila exterior realmente cubre las notificaciones.");
        guides.put("notification_mini_stack", "La miniagrupación por aplicación es independiente del reloj grande y de la pila de pantalla completa; cada tipo tiene su propio interruptor. Solo se oculta automáticamente si la pila de pantalla completa la cubre.");
        guides.put("notification_clear", "El color y la animación de entrada en modo vertical siguen el interruptor de esta página y conservan la acción nativa de borrar. La posición y el color en horizontal se configuran por separado en su propia página.");
        guides.put("notification_clear_landscape", "La posición y el color en horizontal tienen interruptores independientes; desactivar la posición no desactiva el color guardado.");
        guides.put("qs_media", "El fondo de la tarjeta, el resplandor de la portada pequeña y la transición entre portadas se configuran por separado. El color personalizado tiene prioridad sobre el color principal, que a su vez tiene prioridad sobre la imagen de portada.");
        guides.put("c17_highlight_removal", "Eliminar los reflejos y unificar el color de las notificaciones son funciones independientes; desactivar la eliminación de reflejos no desactiva la unificación del color.");
        guides.put("classic_text", "El operador y la hora del panel clásico tienen interruptores independientes. Se pausan en el panel separado, pero se conservan los parámetros guardados.");
        guides.put("shade_wallpaper_portrait", "Función aún no terminada. La orientación vertical, cada panel y la zona del reloj grande se configuran por separado; no uses Restablecer como sustituto de desactivar.");
        guides.put("shade_wallpaper_landscape", "Función aún no terminada. La configuración horizontal es independiente de la vertical; se conserva la configuración de los tipos de panel que no estén activos.");
        guides.put("lockscreen_date", "Solo reemplaza la fecha de la pantalla de bloqueo; no cambia la fecha del panel de notificaciones ni del centro de control.");
        guides.put("lockscreen_lock_icon", "Solo oculta el pequeño candado; mantiene la huella digital, la interacción de desbloqueo y las funciones de seguridad.");
        guides.put("lockscreen_status_blur", "Solo desenfoca el fondo de pantalla detrás de la barra de estado. El área de desenfoque y la máscara de color se configuran por separado; el texto y los iconos permanecen nítidos.");
        guides.put("fluid_cloud_accent", "El color de énfasis multimedia, la máscara y las reglas de seguimiento se administran por separado. La opción de seguir el color solo durante la reproducción también se aplica a la pantalla de bloqueo y AOD.");
        guides.put("fluid_cloud_style", "Los materiales de notificación, la apariencia de Fluid Cloud y el ámbito de compatibilidad están en pestañas separadas. Elige las categorías de uso reconocidas para aplicar el estilo; las tarjetas no reconocidas se agrupan en «Otros y no reconocidos». Se aplica a las vistas flotantes expandidas, la pantalla de bloqueo y AOD; la escena de la barra de notificaciones está desactivada.");
        guides.put("fluid_cloud_capsule", "El color de fondo, el borde, la forma y el contenido están en pestañas separadas. La visualización persistente es independiente del interruptor de apariencia; desactivar la apariencia no desactiva la visualización persistente.");
        guides.put("lock_awake", "El degradado con pantalla encendida, la personalización y el seguimiento de notificaciones normales tienen interruptores separados. El estilo unificado usa el efecto de tarjeta propio de cada escena y no copia los parámetros de AOD.");
        guides.put("lock_aod", "La personalización de AOD y el vidrio blanco de las notificaciones se configuran por separado. Los ajustes de pantalla encendida y AOD se guardan por separado; las tarjetas flotantes del escritorio, la fecha y el desenfoque del fondo se administran de forma independiente.");
        GUIDES = Collections.unmodifiableMap(guides);
    }
    private SettingsOrganization() {}
    public static Map<String, String> guides() { return GUIDES; }
    public static String guide(String id) { String guide = GUIDES.get(id); return guide == null ? "" : guide; }
    /** A full block folds, never a lone slider. Ordinary controls remain visible by default. */
    public static boolean advanced(String group, String section) {
        String title = section.contains(" · ") ? section.substring(section.lastIndexOf(" · ") + 3) : section;
        if ("label".equals(group) && (section.contains("Contenido mostrado") || section.contains("Contenido personalizado"))) return true;
        if ("fluid_cloud_capsule".equals(group)) return Arrays.asList("Luz en movimiento", "Desvanecer hacia dentro", "Rebote de aparición", "Espectro en tiempo real").contains(section);
        if (group.startsWith("notification_big_clock")) return Arrays.asList("Textura de pantalla de bloqueo", "Borde de vidrio", "Borde con relieve de la hora principal", "Estilo contraído", "Posición contraída", "Efecto de rebote", "Borde superior de las notificaciones").contains(section);
        if (group.startsWith("shade_wallpaper")) return "Área del reloj grande".equals(section);
        if ("qs_media".equals(group)) return "Transición".equals(section) || title.equals("Resplandor exterior");
        return Arrays.asList("Color y transparencia", "Efecto de degradado", "Espacio junto al borde de la batería", "Estilo del texto", "Texto y espaciado", "Alineación de los números", "Velocidad de actualización", "Alternancia durante la carga").contains(title);
    }
}
