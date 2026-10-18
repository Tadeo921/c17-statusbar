// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Schema 46 adds explicit font scopes; the empty map preserves every legacy font choice. */
final class Upgrade186 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> defaults=new LinkedHashMap<>();defaults.put(FontScopes.KEY,FontScopes.DEFAULT);
        DEFAULTS=Collections.unmodifiableMap(defaults);
    }
    static void migrate(SharedPreferences preferences){
        Map<String,?> old=preferences.getAll();if(old.isEmpty()||old.containsKey(FontScopes.KEY))return;
        Map<String,Object> missing=new LinkedHashMap<>(DEFAULTS);
        SharedPreferences.Editor edit=preferences.edit().putString(FontScopes.KEY,FontScopes.DEFAULT);
        PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade186(){ }
}
