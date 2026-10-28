// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import java.util.Collections;

/** App-only acknowledgement. It is never part of the hook settings, mirror or JSON export. */
public final class FreeNotice {
    public static final String PHRASE = "Este módulo es gratuito para siempre. Las donaciones son voluntarias; no pagues por comprarlo.";
    public static final String DESCRIPTION = "Este módulo es un proyecto gratuito y de código abierto, no un producto de pago. Todas las funciones son y serán gratuitas.\n\n"
            + "Donar es totalmente voluntario y sirve para apoyar al autor. Puedes usar todas las funciones sin donar." + ProjectContact.DONATION_NOTE + "\n\n"
            + "No pagues a nadie por este módulo, por claves de activación ni por supuestas versiones de pago. Desconfía de las ventas, activaciones cobradas o donaciones obligatorias.";
    private static final String PREFERENCES = "app_free_notice";
    private static final String ACCEPTED_VERSION = "accepted_version";
    private static final int VERSION = 1;
    private FreeNotice() { }
    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }
    public static boolean accepted(Context context) {
        return Integer.valueOf(VERSION).equals(PreferenceWrites.values(preferences(context)).get(ACCEPTED_VERSION));
    }
    public static boolean matches(String text) { return PHRASE.equals(text); }
    public static boolean accept(Context context, String text) {
        if (!matches(text)) return false;
        SharedPreferences raw = preferences(context);
        return PreferenceWrites.commit(raw, raw.edit().putInt(ACCEPTED_VERSION, VERSION),
                Collections.<String, Object>singletonMap(ACCEPTED_VERSION, VERSION), false);
    }
    public static boolean blocksContextMenuAction(int action) {
        return action == android.R.id.paste || action == android.R.id.pasteAsPlainText
                || action == android.R.id.copy || action == android.R.id.cut
                || action == android.R.id.shareText;
    }
    public static boolean blocksClipboardShortcut(int key, int modifiers) {
        boolean control = (modifiers & KeyEvent.META_CTRL_ON) != 0;
        boolean shift = (modifiers & KeyEvent.META_SHIFT_ON) != 0;
        return control && (key == KeyEvent.KEYCODE_V || key == KeyEvent.KEYCODE_C
                || key == KeyEvent.KEYCODE_X || key == KeyEvent.KEYCODE_INSERT)
                || shift && (key == KeyEvent.KEYCODE_INSERT || key == KeyEvent.KEYCODE_DEL);
    }
}
