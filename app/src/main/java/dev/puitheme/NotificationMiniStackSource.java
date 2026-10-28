// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;

/** Source declaration of the same-app mini stack and its app-only acknowledgement. The record is independent of the
 * free notice and of the feature switch, and is never part of the hook settings, mirror or JSON export: an imported
 * "on" configuration therefore cannot stand in for the user having seen the declaration. */
public final class NotificationMiniStackSource {
    public static final String PROJECT = "OplusX", AUTHOR = "xxxxawl", FEATURE = "Activar miniagrupación (misma aplicación)";
    public static final String URL = "https://www.coolapk.com/u/18956155";
    public static final String TITLE = "Aviso sobre el origen de esta función";
    /** The full declaration: first use, inside the page, and the About credit. */
    public static final String DECLARATION = "Esta función toma como referencia «Activar miniagrupación (misma aplicación)» de OplusX. Gracias a xxxxawl, autor de OplusX, por la referencia de implementación. Toca para ver su perfil en Coolapk.";
    /** Next to the 通知栏 entry. */
    public static final String SHORT = "Miniagrupación de notificaciones por aplicación · basada en OplusX / xxxxawl";
    /** The page's permanent source line. */
    public static final String INSIDE = "Referencia de la función: OplusX · autor xxxxawl";
    public static final String LINK = "Ver perfil del autor en Coolapk";
    /** The reference's own stack code builds on this; credited separately so its origin is not attributed to one author. */
    public static final String TECHNICAL_ORIGIN = "La base de agrupación de notificaciones de OplusX procede de VivoGroupDeck test21.";
    public static final String NOTICE = DECLARATION + "\n\nEsta función solo muestra grupos de notificaciones que el sistema ya creó: al contraerse, muestra completa la primera y deja ver hasta dos capas de notificaciones reales."
            + "No vuelve a agrupar ni ordenar las notificaciones y no añade gestos de deslizamiento. Confirmar solo registra que has leído este aviso; no reinicia automáticamente SystemUI.";
    private static final String PREFERENCES = "app_mini_stack_source";
    private static final String ACKNOWLEDGED_VERSION = "acknowledged_version";
    private static final int VERSION = 1;
    private NotificationMiniStackSource() { }
    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
    public static boolean acknowledged(Context context) {
        return Integer.valueOf(VERSION).equals(PreferenceWrites.values(preferences(context)).get(ACKNOWLEDGED_VERSION));
    }
    public static boolean acknowledge(Context context) {
        SharedPreferences raw = preferences(context);
        return PreferenceWrites.commit(raw, raw.edit().putInt(ACKNOWLEDGED_VERSION, VERSION),
                Collections.<String, Object>singletonMap(ACKNOWLEDGED_VERSION, VERSION), false);
    }
    /** Turning the feature on, from a switch or an import, first needs the declaration to have been acknowledged. */
    public static boolean requiresDeclaration(String key, Object value) {
        return NotificationMiniStack.ENABLED.equals(key) && Boolean.TRUE.equals(value);
    }
    /** An import that would switch the feature on cannot count as having seen the declaration. */
    public static boolean importNeedsDeclaration(java.util.Map<String, ?> imported, boolean acknowledged) {
        return !acknowledged && imported != null && requiresDeclaration(NotificationMiniStack.ENABLED, imported.get(NotificationMiniStack.ENABLED));
    }
}
