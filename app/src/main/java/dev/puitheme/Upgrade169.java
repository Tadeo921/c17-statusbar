// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** Schema 37: left/right text components; the retired AppWidget keys remain compatible. */
final class Upgrade169 {
    static final Map<String,Object> DEFAULTS;
    static {
        Map<String,Object> values=new LinkedHashMap<>();
        for(int side:new int[]{BigClockWidgetsSettings.LEFT,BigClockWidgetsSettings.RIGHT}) {
            values.put(BigClockWidgetsSettings.textKey(side),side==0?BigClockWidgetsSettings.DEFAULT_LEFT_TEXT:BigClockWidgetsSettings.DEFAULT_RIGHT_TEXT);
            values.put(BigClockWidgetsSettings.sizeKey(side),BigClockWidgetsSettings.DEFAULT_SIZE);
            values.put(BigClockWidgetsSettings.offsetXKey(side),0f);
            values.put(BigClockWidgetsSettings.offsetYKey(side),0f);
            values.put(BigClockWidgetsSettings.followTimeColorKey(side),true);
            for(boolean dark:new boolean[]{false,true}) {
                String key=BigClockWidgetsSettings.colorKey(side,dark);
                values.put(key,dark?0xffffffff:0xff000000);
                values.put(StatusBarSettings.alphaKey(key),false);
            }
        }
        DEFAULTS=Collections.unmodifiableMap(values);
    }
    static void migrate(SharedPreferences preferences) {
        Map<String,?> old=preferences.getAll();if(old.isEmpty())return;
        Map<String,Object> missing=new LinkedHashMap<>();SharedPreferences.Editor edit=preferences.edit();
        for(Map.Entry<String,Object> item:DEFAULTS.entrySet())if(!old.containsKey(item.getKey())) {
            missing.put(item.getKey(),item.getValue());StatusBarSettings.copyPreference(edit,item.getKey(),item.getValue());
        }
        if(!missing.isEmpty())PreferenceWrites.commit(preferences,edit,missing,false);
    }
    private Upgrade169(){}
}
