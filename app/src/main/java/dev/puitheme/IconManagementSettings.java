// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.IOException;
import java.util.*;

/** One immutable v3 settings domain. Applying a draft never reads or resurrects legacy icon preferences. */
public final class IconManagementSettings {
    /** Pause rendering without discarding the independent saved records or private icon libraries. */
    public static final String ENABLED="icon_management_v3_enabled";
    public static final String KEY="icon_management_v3_records";
    public static final String DEFAULT="{\"version\":3,\"scopePacks\":{},\"overrides\":{},\"appearances\":{},\"animations\":{}}";
    public static final int MAX_BYTES=128*1024;
    public static final Set<String> LEGACY_KEYS=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("status_icon_pack_enabled","status_icon_pack_layers","status_icon_pack_assignments")));
    public static final class Override{
        public final String mode,packId,sourceRole;
        public Override(String mode,String packId,String sourceRole){this.mode=mode;this.packId=packId==null?"":packId;this.sourceRole=sourceRole==null?"":sourceRole;}
    }
    public static final class Appearance{
        public final String mode;public final int lightColor,darkColor;public final Float opacity,sizePercent;
        public Appearance(String mode,int lightColor,int darkColor,Float opacity){this(mode,lightColor,darkColor,opacity,null);}
        public Appearance(String mode,int lightColor,int darkColor,Float opacity,Float sizePercent){this.mode=mode;this.lightColor=lightColor;this.darkColor=darkColor;this.opacity=opacity;this.sizePercent=sizePercent;}
    }
    public static final class State{
        public final Map<String,String> scopePacks;public final Map<String,Override> overrides;public final Map<String,Appearance> appearances;public final Map<String,Boolean> animations;
        private State(Map<String,String> packs,Map<String,Override> overrides,Map<String,Appearance> appearances,Map<String,Boolean> animations){
            scopePacks=Collections.unmodifiableMap(new LinkedHashMap<>(packs));this.overrides=Collections.unmodifiableMap(new LinkedHashMap<>(overrides));this.appearances=Collections.unmodifiableMap(new LinkedHashMap<>(appearances));this.animations=Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        }
    }
    public static boolean validPackId(String id){return id!=null&&id.matches("[0-9a-f]{64}");}
    private static Map<String,Object> optional(Map<String,Object> map,String key)throws IOException{return map.containsKey(key)?IconManagementJson.object(map.get(key)):Collections.emptyMap();}
    public static State parse(String records)throws IOException{
        Map<String,Object> root=IconManagementJson.parse(records,MAX_BYTES);IconManagementJson.only(root,"version","scopePacks","overrides","appearances","animations");if(IconManagementJson.integer(root.get("version"),3,3)!=3)throw IconManagementJson.error("La versión de configuración no es válida");
        Map<String,String> packs=new LinkedHashMap<>();Map<String,Override> overrides=new LinkedHashMap<>();Map<String,Appearance> appearances=new LinkedHashMap<>();Map<String,Boolean> animations=new LinkedHashMap<>();
        for(Map.Entry<String,Object> row:optional(root,"scopePacks").entrySet()){if(!IconRoleRegistry.validScope(row.getKey())||!(row.getValue() instanceof String)||!validPackId((String)row.getValue()))throw IconManagementJson.error("El alcance del paquete o su origen no es válido");packs.put(row.getKey(),(String)row.getValue());}
        Map<String,Object> rawOverrides=optional(root,"overrides"),rawAppearances=optional(root,"appearances");if(rawOverrides.size()>512||rawAppearances.size()>1024)throw IconManagementJson.error("Hay demasiadas asignaciones de iconos individuales");
        for(Map.Entry<String,Object> row:rawOverrides.entrySet()){requireRole(row.getKey());Map<String,Object> value=IconManagementJson.object(row.getValue());IconManagementJson.only(value,"mode","packId","sourceRole");
            Override selected=new Override(IconManagementJson.text(value,"mode",null,16),IconManagementJson.text(value,"packId","",64),IconManagementJson.text(value,"sourceRole","",128));validate(selected);overrides.put(row.getKey(),selected);}
        for(Map.Entry<String,Object> row:rawAppearances.entrySet()){requireAppearanceKey(row.getKey());appearances.put(row.getKey(),parseAppearance(IconManagementJson.object(row.getValue()),true));}
        for(Map.Entry<String,Object> row:optional(root,"animations").entrySet()){if(IconRoleRegistry.target(row.getKey())==null)throw IconManagementJson.error("Destino de icono animado desconocido");animations.put(row.getKey(),IconManagementJson.bool(row.getValue()));}
        return new State(packs,overrides,appearances,animations);
    }
    static Appearance parseAppearance(Map<String,Object> value,boolean allowSize)throws IOException{
            if(allowSize)IconManagementJson.only(value,"mode","lightColor","darkColor","opacity","sizePercent");else IconManagementJson.only(value,"mode","lightColor","darkColor","opacity");
            String mode=IconManagementJson.text(value,"mode","inherit",16);int light=0xff000000,dark=0xffffffff;
            if(value.containsKey("lightColor"))light=color(value.get("lightColor"));if(value.containsKey("darkColor"))dark=color(value.get("darkColor"));
            if("custom".equals(mode)&&(!value.containsKey("lightColor")||!value.containsKey("darkColor")))throw IconManagementJson.error("El color personalizado debe incluir un color para el fondo claro y otro para el oscuro");
            Float opacity=value.get("opacity")==null?null:(float)IconManagementJson.number(value.get("opacity"));
            Float size=value.get("sizePercent")==null?null:(float)IconManagementJson.number(value.get("sizePercent"));Appearance selected=new Appearance(mode,light,dark,opacity,size);validate(selected);return selected;
    }
    private static int color(Object value)throws IOException{int color=IconManagementJson.integer(value,Integer.MIN_VALUE,Integer.MAX_VALUE);if((color>>>24)!=255)throw IconManagementJson.error("La paleta solo admite RGB; configura la opacidad por separado");return color;}
    private static void validate(Override value)throws IOException{if(value==null)throw IconManagementJson.error("Falta el diseño del icono individual");if("system".equals(value.mode)){if(!value.packId.isEmpty()||!value.sourceRole.isEmpty())throw IconManagementJson.error("Los iconos del sistema no pueden incluir un origen");}
        else if(!"pack".equals(value.mode)||!validPackId(value.packId)||!IconRoleRegistry.validResourceRole(value.sourceRole)||!IconRoleRegistry.validKnownState(value.sourceRole))throw IconManagementJson.error("El origen del diseño individual no es válido");}
    private static void validate(Appearance value)throws IOException{if(value==null||!Arrays.asList("inherit","system","custom","artwork").contains(value.mode)||value.opacity!=null&&(!Float.isFinite(value.opacity)||value.opacity<0f||value.opacity>100f))throw IconManagementJson.error("El modo de color o la opacidad no son válidos");if(value.sizePercent!=null&&(!Float.isFinite(value.sizePercent)||value.sizePercent<25f||value.sizePercent>200f))throw IconManagementJson.error("El tamaño del icono debe estar entre el 25 % y el 200 %");if("custom".equals(value.mode)&&((value.lightColor>>>24)!=255||(value.darkColor>>>24)!=255))throw IconManagementJson.error("El color personalizado debe ser RGB opaco");}
    private static void requireRole(String role)throws IOException{if(IconRoleRegistry.role(role)==null)throw IconManagementJson.error("Rol de icono desconocido: "+role);}
    static void requireAppearanceKey(String key)throws IOException{if(key.startsWith("scope:")&&IconRoleRegistry.validScope(key.substring(6)))return;if(key.startsWith("target:")&&IconRoleRegistry.target(key.substring(7))!=null)return;if(key.startsWith("role:")&&IconRoleRegistry.role(key.substring(5))!=null)return;throw IconManagementJson.error("Alcance de apariencia del icono desconocido");}
    static String appearanceScope(String key)throws IOException{requireAppearanceKey(key);return key.startsWith("scope:")?key.substring(6):key.startsWith("target:")?IconRoleRegistry.target(key.substring(7)).scope:IconRoleRegistry.role(key.substring(5)).scope;}
    private static Map<String,Object> overrideValue(Override value)throws IOException{validate(value);Map<String,Object> out=new LinkedHashMap<>();out.put("mode",value.mode);if("pack".equals(value.mode)){out.put("packId",value.packId);out.put("sourceRole",value.sourceRole);}return out;}
    static Map<String,Object> appearanceValue(Appearance value)throws IOException{validate(value);Map<String,Object> out=new LinkedHashMap<>();out.put("mode",value.mode);if("custom".equals(value.mode)){out.put("lightColor",value.lightColor);out.put("darkColor",value.darkColor);}if(value.opacity!=null)out.put("opacity",value.opacity);if(value.sizePercent!=null)out.put("sizePercent",value.sizePercent);return out;}
    public static String encode(State state)throws IOException{
        Map<String,Object> root=new LinkedHashMap<>(),overrides=new LinkedHashMap<>(),appearances=new LinkedHashMap<>();root.put("version",3);root.put("scopePacks",state.scopePacks);
        for(Map.Entry<String,Override> row:state.overrides.entrySet())overrides.put(row.getKey(),overrideValue(row.getValue()));for(Map.Entry<String,Appearance> row:state.appearances.entrySet())appearances.put(row.getKey(),appearanceValue(row.getValue()));
        root.put("overrides",overrides);root.put("appearances",appearances);root.put("animations",state.animations);String encoded=IconManagementJson.encode(root);parse(encoded);return encoded;
    }
    public static String applyPack(String records,String packId,Collection<String> scopes,boolean clearOverrides)throws IOException{
        if(!validPackId(packId)||scopes==null||scopes.isEmpty())throw IconManagementJson.error("Selecciona un paquete y su alcance");State old=parse(records);Map<String,String> packs=new LinkedHashMap<>(old.scopePacks);Map<String,Override> overrides=new LinkedHashMap<>(old.overrides);
        for(String scope:scopes){if(!IconRoleRegistry.validScope(scope))throw IconManagementJson.error("Alcance de paquete desconocido");packs.put(scope,packId);}if(clearOverrides)overrides.entrySet().removeIf(row->scopes.contains(IconRoleRegistry.scopeOfRole(row.getKey())));
        return encode(new State(packs,overrides,old.appearances,old.animations));
    }
    public static String applyTarget(String records,String targetId,Map<String,Override> roleOverrides,Appearance appearance,Map<String,Appearance> roleAppearances,boolean animationEnabled)throws IOException{
        IconRoleRegistry.Target target=IconRoleRegistry.target(targetId);if(target==null||roleOverrides==null||roleAppearances==null)throw IconManagementJson.error("El borrador del destino no es válido");State old=parse(records);Map<String,Override> overrides=new LinkedHashMap<>(old.overrides);Map<String,Appearance> appearances=new LinkedHashMap<>(old.appearances);Map<String,Boolean> animations=new LinkedHashMap<>(old.animations);
        for(String role:target.roles){overrides.remove(role);appearances.remove("role:"+role);}appearances.remove("target:"+targetId);
        for(Map.Entry<String,Override> row:roleOverrides.entrySet()){if(!target.roles.contains(row.getKey()))throw IconManagementJson.error("El borrador contiene diseños para otros destinos");validate(row.getValue());overrides.put(row.getKey(),row.getValue());}
        if(appearance!=null){validate(appearance);appearances.put("target:"+targetId,appearance);}for(Map.Entry<String,Appearance> row:roleAppearances.entrySet()){if(!target.roles.contains(row.getKey()))throw IconManagementJson.error("El borrador contiene apariencias para otros destinos");validate(row.getValue());appearances.put("role:"+row.getKey(),row.getValue());}
        if(animationEnabled)animations.remove(targetId);else animations.put(targetId,false);return encode(new State(old.scopePacks,overrides,appearances,animations));
    }
    public static String applyAppearance(String records,String key,Appearance appearance)throws IOException{requireAppearanceKey(key);State old=parse(records);Map<String,Appearance> values=new LinkedHashMap<>(old.appearances);if(appearance==null)values.remove(key);else{validate(appearance);values.put(key,appearance);}return encode(new State(old.scopePacks,old.overrides,values,old.animations));}
    /** A scope/target palette replaces shadowing child colors, preserving independent size and alpha. */
    public static String applyColorPreset(String records,IconColorPresets.Preset preset,Collection<String> scopes)throws IOException{
        if(preset==null||scopes==null||scopes.isEmpty())throw IconManagementJson.error("Selecciona un preajuste de color y el alcance de aplicación");
        for(String scope:scopes)if(!IconRoleRegistry.validScope(scope))throw IconManagementJson.error("Alcance de aplicación del preajuste desconocido");
        State old=parse(records);Map<String,Appearance> values=new LinkedHashMap<>(old.appearances);
        for(Map.Entry<String,Appearance> row:old.appearances.entrySet()){
            String key=row.getKey(),scope=appearanceScope(key);if(!scopes.contains(scope))continue;
            boolean shadowed=!key.startsWith("scope:")&&preset.appearances.containsKey("scope:"+scope);
            if(key.startsWith("role:")){IconRoleRegistry.Role role=IconRoleRegistry.role(key.substring(5));shadowed|=preset.appearances.containsKey("target:"+role.targetId);}
            if(shadowed){Appearance previous=row.getValue();
                if(previous.opacity==null&&previous.sizePercent==null)values.remove(key);
                else values.put(key,new Appearance("inherit",previous.lightColor,previous.darkColor,previous.opacity,previous.sizePercent));
            }
        }
        for(Map.Entry<String,Appearance> row:preset.appearances.entrySet())if(scopes.contains(appearanceScope(row.getKey()))){Appearance color=row.getValue(),previous=values.get(row.getKey());
            values.put(row.getKey(),new Appearance(color.mode,color.lightColor,color.darkColor,color.opacity!=null?color.opacity:previous==null?null:previous.opacity,previous==null?null:previous.sizePercent));}
        return encode(new State(old.scopePacks,old.overrides,values,old.animations));
    }
    public static String removePack(String records,String id)throws IOException{State old=parse(records);Map<String,String> packs=new LinkedHashMap<>(old.scopePacks);Map<String,Override> overrides=new LinkedHashMap<>(old.overrides);packs.values().removeIf(value->value.equals(id));overrides.values().removeIf(value->id.equals(value.packId));return encode(new State(packs,overrides,old.appearances,old.animations));}
    public static String clearScopePack(String records,String scope)throws IOException{if(!IconRoleRegistry.validScope(scope))throw IconManagementJson.error("Alcance de paquete desconocido");State old=parse(records);Map<String,String> packs=new LinkedHashMap<>(old.scopePacks);packs.remove(scope);return encode(new State(packs,old.overrides,old.appearances,old.animations));}
    public static int dependencies(String records,String id)throws IOException{State state=parse(records);int count=0;for(String pack:state.scopePacks.values())if(id.equals(pack))count++;for(Override value:state.overrides.values())if(id.equals(value.packId))count++;return count;}
    public static String reset(){return DEFAULT;}
    private IconManagementSettings(){ }
}
