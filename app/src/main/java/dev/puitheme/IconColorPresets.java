// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.IOException;
import java.util.*;

/** Data-only named palettes. They cannot select sources, register hosts or change geometry. */
public final class IconColorPresets {
    public static final int MAX_PRESETS=16,MAX_APPEARANCES=256;
    public static final class Preset {
        public final String id,name;public final Map<String,IconManagementSettings.Appearance> appearances;
        private Preset(String id,String name,Map<String,IconManagementSettings.Appearance> appearances){this.id=id;this.name=name;this.appearances=Collections.unmodifiableMap(new LinkedHashMap<>(appearances));}
    }
    /** These local choices are available even when a legacy v3 package declares no palette. */
    public static Map<String,Preset> options(IconManagementManifest manifest){
        Map<String,Preset> result=new LinkedHashMap<>();
        for(String mode:new String[]{"system","artwork"}){Map<String,IconManagementSettings.Appearance> colors=new LinkedHashMap<>();for(String scope:IconRoleRegistry.SCOPES)colors.put("scope:"+scope,new IconManagementSettings.Appearance(mode,0xff000000,0xffffffff,null));String id="@"+mode;result.put(id,new Preset(id,"system".equals(mode)?"跟随系统颜色":"保留素材原色",colors));}
        result.putAll(manifest.colorPresets);return Collections.unmodifiableMap(result);
    }
    static Map<String,Preset> parse(Object raw)throws IOException{
        if(raw==null)return Collections.emptyMap();Map<String,Object> entries=IconManagementJson.object(raw);if(entries.size()>MAX_PRESETS)throw IconManagementJson.error("最多16个图库颜色预设");
        Map<String,Preset> result=new TreeMap<>();for(Map.Entry<String,Object> entry:entries.entrySet()){
            String id=entry.getKey();if(!id.matches("[a-z0-9][a-z0-9_-]{0,63}"))throw IconManagementJson.error("颜色预设标识无效");
            Map<String,Object> value=IconManagementJson.object(entry.getValue());IconManagementJson.only(value,"name","appearances");String name=IconManagementJson.text(value,"name","",64).trim();if(name.isEmpty()||name.matches("(?s).*\\p{Cntrl}.*"))throw IconManagementJson.error("颜色预设名称无效");
            Map<String,Object> items=IconManagementJson.object(value.get("appearances"));if(items.isEmpty()||items.size()>MAX_APPEARANCES)throw IconManagementJson.error("颜色预设须含1至256项外观");
            Map<String,IconManagementSettings.Appearance> colors=new TreeMap<>();for(Map.Entry<String,Object> item:items.entrySet()){IconManagementSettings.requireAppearanceKey(item.getKey());colors.put(item.getKey(),IconManagementSettings.parseAppearance(IconManagementJson.object(item.getValue()),false));}
            result.put(id,new Preset(id,name,colors));
        }return Collections.unmodifiableMap(result);
    }
    static Map<String,Object> values(Map<String,Preset> presets)throws IOException{
        Map<String,Object> result=new TreeMap<>();for(Preset preset:presets.values()){Map<String,Object> value=new LinkedHashMap<>(),colors=new TreeMap<>();value.put("name",preset.name);for(Map.Entry<String,IconManagementSettings.Appearance> entry:preset.appearances.entrySet())colors.put(entry.getKey(),IconManagementSettings.appearanceValue(entry.getValue()));value.put("appearances",colors);result.put(preset.id,value);}return result;
    }
    private IconColorPresets(){ }
}
