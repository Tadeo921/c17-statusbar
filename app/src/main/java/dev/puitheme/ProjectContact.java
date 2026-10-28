// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/** Public contact information; copying occurs only after the user taps the about-page row. */
public final class ProjectContact {
    public static final String AUTHOR = "aiingjie";
    public static final String GITHUB = "sanwu5";
    public static final String COOLAPK = "konwo";
    /** The author's Coolapk profile; the Coolapk app opens it when installed, otherwise the browser does. */
    public static final String COOLAPK_URL = "https://www.coolapk.com/u/745949";
    /** Shown with the donation entries. */
    public static final String DONATION_NOTE = "No hay un importe mínimo ni máximo para donar. Después de donar, puedes contactar al autor y solicitar acceso al grupo de pruebas. Las actualizaciones se prueban allí antes de publicarse cuando son estables.";
    public static final String QQ = "2726344450";
    private ProjectContact() { }
    public static boolean copyQq(Context context) {
        return copy(context, "QQ para colaboraciones y donaciones", QQ);
    }
    public static boolean copyCoolapk(Context context) {
        return copy(context, "Nombre de usuario en Coolapk", COOLAPK);
    }
    private static boolean copy(Context context, String label, String value) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) return false;
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
            return true;
        } catch (RuntimeException unavailable) { return false; }
    }
}
