// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The text shown for each network generation, as the last step before drawing: the radio state, the Wi-Fi gate and the
 * choice of source never see it. Custom contents and the unify switch exclude each other: the one that is on is the only
 * one that acts, and a word that is not a known generation (an unknown carrier word) keeps the system text.
 */
final class NetworkLabelFormats {
    static final String ENABLED="label_custom_content_enabled",PREFIX="label_content_",UNIFY="label_normalize_enabled";
    static final Map<String,String> STRINGS;
    static {
        Map<String,String> values=new LinkedHashMap<>();
        for(NetworkLabel.Level level:NetworkLabel.Level.values())values.put(key(level),level.text);
        STRINGS=Collections.unmodifiableMap(values);
    }
    private volatile Map<String,String> formats=STRINGS;
    static String key(NetworkLabel.Level level){return PREFIX+level.id;}
    static boolean owns(String key){return STRINGS.containsKey(key);}
    /** The switch that one must be turned off when this one is turned on, or null. */
    static String turnsOff(String key){
        if(ENABLED.equals(key))return UNIFY;
        if(UNIFY.equals(key))return ENABLED;
        return null;
    }
    void configure(Bundle settings) {
        Map<String,String> values=new LinkedHashMap<>();
        for(Map.Entry<String,String> item:STRINGS.entrySet()) {
            String value=settings.getString(item.getKey(),item.getValue());
            values.put(item.getKey(),valid(value)?value:item.getValue());
        }
        formats=Collections.unmodifiableMap(values);
    }
    private static boolean valid(String value){
        if(value==null||value.length()>TimeFormat.MAX_TEXT)return false;
        for(int i=0;i<value.length();i++) {
            char ch=value.charAt(i);if(Character.isISOControl(ch))return false;
            if(Character.isHighSurrogate(ch)){if(++i>=value.length()||!Character.isLowSurrogate(value.charAt(i)))return false;}
            else if(Character.isLowSurrogate(ch))return false;
        }
        return true;
    }
    /**
     * canonical is the recognised label (see NetworkLabel.canonical). With custom contents on, a known generation shows its own
     * text (empty hides it) and the unify switch is ignored even if a restored backup left both on; otherwise unify groups the
     * generations, and with both off the system text is kept.
     */
    String display(String canonical,boolean unified,boolean custom) {
        if(canonical==null||canonical.isEmpty())return "";
        if(custom) {
            NetworkLabel.Level level=NetworkLabel.level(canonical);
            return level==null?canonical:formats.get(key(level));
        }
        return unified?NetworkLabel.normalize(canonical):canonical;
    }
}
