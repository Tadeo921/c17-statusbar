// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Version 3 only. Legacy manifests remain in their separate historical codec and storage domain. */
public final class IconManagementManifest {
    public static final String FORMAT="c17-statusbar-icons";public static final int VERSION=3,MAX_BYTES=256*1024,MAX_ICONS=512;
    public final String id,name,author,license;public final int version=3;
    public final Map<String,Asset> icons;public final Map<String,Transition> transitions;public final Set<String> unsupportedRoles;
    public final Map<String,IconColorPresets.Preset> colorPresets;
    public static final class Asset{
        public final String source,render,format,sha256;
        private Asset(String source,String render,String sha256){this.source=source;this.render=render;this.sha256=sha256;format=source.endsWith(".png")?"png":"vector";}
    }
    public static final class Transition{
        public final String source,from,to,sha256;public final boolean reversible;
        private Transition(String source,String from,String to,boolean reversible,String sha256){this.source=source;this.from=from;this.to=to;this.reversible=reversible;this.sha256=sha256;}
    }
    private IconManagementManifest(String id,String name,String author,String license,Map<String,Asset> icons,Map<String,Transition> transitions,Set<String> unsupported,Map<String,IconColorPresets.Preset> presets){this.id=id;this.name=name;this.author=author;this.license=license;this.icons=Collections.unmodifiableMap(new LinkedHashMap<>(icons));this.transitions=Collections.unmodifiableMap(new LinkedHashMap<>(transitions));unsupportedRoles=Collections.unmodifiableSet(new LinkedHashSet<>(unsupported));colorPresets=presets;}
    public static boolean validAsset(String path){return path!=null&&path.matches("assets/[A-Za-z0-9][A-Za-z0-9_-]{0,95}\\.(?:png|xml|json)");}
    public static boolean validStatic(String path){return validAsset(path)&&!path.endsWith(".json");}
    private static String label(Map<String,Object> root,String key,int max,boolean required)throws IOException{String value=IconManagementJson.text(root,key,"",max).trim();if(required&&value.isEmpty()||value.matches("(?s).*\\p{Cntrl}.*"))throw IconManagementJson.error("El texto del paquete de iconos no es válido: "+key);return value;}
    private static String digest(Map<String,Object> object)throws IOException{String digest=IconManagementJson.text(object,"sha256","",64);if(!digest.isEmpty()&&!IconManagementSettings.validPackId(digest))throw IconManagementJson.error("El resumen de recursos no es válido.");return digest;}
    public static IconManagementManifest parse(byte[] bytes)throws IOException{
        Map<String,Object> root=IconManagementJson.parse(bytes,MAX_BYTES);IconManagementJson.only(root,"format","version","id","name","author","license","fallback","icons","transitions","colorPresets");
        Object version=root.get("version");if(!FORMAT.equals(root.get("format"))||!(version instanceof Number)||new java.math.BigDecimal(version.toString()).compareTo(java.math.BigDecimal.valueOf(3))!=0)throw IconManagementJson.error("Se necesita un paquete de iconos v3; los paquetes v1/v2 ya no se aplican.");
        if(!"native".equals(root.get("fallback")))throw IconManagementJson.error("Los paquetes v3 deben declarar «fallback» como «native».");
        String id=label(root,"id",64,true);if(!id.matches("[a-z0-9][a-z0-9_-]{0,63}"))throw IconManagementJson.error("El ID del paquete de iconos no es válido.");
        String name=label(root,"name",64,true),author=label(root,"author",128,false),license=label(root,"license",128,true);
        Map<String,Object> rawIcons=IconManagementJson.object(root.get("icons"));if(rawIcons.isEmpty()||rawIcons.size()>MAX_ICONS)throw IconManagementJson.error("El paquete de iconos debe contener entre 1 y 512 diseños.");
        Map<String,Asset> icons=new TreeMap<>();Set<String> unsupported=new LinkedHashSet<>();
        for(Map.Entry<String,Object> row:rawIcons.entrySet()){
            String role=row.getKey();if(!IconRoleRegistry.validResourceRole(role)||!IconRoleRegistry.validKnownState(role))throw IconManagementJson.error("Rol de icono o estado conocido no válido: "+role);
            Map<String,Object> value=IconManagementJson.object(row.getValue());IconManagementJson.only(value,"source","render","sha256");String source=IconManagementJson.text(value,"source",null,128),render=IconManagementJson.text(value,"render","mask",8);
            if(!validStatic(source)||!Arrays.asList("mask","color").contains(render))throw IconManagementJson.error("Solo se admiten PNG o VectorDrawable restringidos; la ruta del recurso o el render no son válidos.");
            icons.put(role,new Asset(source,render,digest(value)));if(!IconRoleRegistry.knownResourceRole(role))unsupported.add(role);
        }
        Map<String,Transition> transitions=new TreeMap<>();Map<String,Object> rawTransitions=root.containsKey("transitions")?IconManagementJson.object(root.get("transitions")):Collections.emptyMap();if(rawTransitions.size()>128)throw IconManagementJson.error("Hay demasiados diseños animados.");
        for(Map.Entry<String,Object> row:rawTransitions.entrySet()){
            String key=row.getKey();int split=key.lastIndexOf('.');if(split<0)throw IconManagementJson.error("El destino de la animación no es válido.");String target=key.substring(0,split),kind=key.substring(split+1);
            if(!target.startsWith("tile.")||!IconRoleRegistry.validResourceRole(target+".on")||!Arrays.asList("off_to_on","on_to_off","transient").contains(kind))throw IconManagementJson.error("La animación solo admite cambios de mosaicos declarados y estados transitorios.");
            Map<String,Object> value=IconManagementJson.object(row.getValue());IconManagementJson.only(value,"source","from","to","reversible","sha256");String source=IconManagementJson.text(value,"source",null,128),from=IconManagementJson.text(value,"from",null,16),to=IconManagementJson.text(value,"to",null,16);boolean reversible=value.containsKey("reversible")&&IconManagementJson.bool(value.get("reversible"));
            if(!validAsset(source)||!source.endsWith(".json")||!Arrays.asList("on","off").contains(to))throw IconManagementJson.error("El recurso de animación o el estado final no es válido.");
            if(kind.equals("transient")){if(!"transient".equals(from)||reversible)throw IconManagementJson.error("El estado transitorio no se puede reproducir al revés; «from» debe ser «transient».");}
            else if(!kind.equals(from+"_to_"+to)||from.equals(to))throw IconManagementJson.error("Los campos «from/to» no coinciden con el nombre de la animación.");
            if(!icons.containsKey(target+'.'+to)||!kind.equals("transient")&&!icons.containsKey(target+'.'+from))throw IconManagementJson.error("Falta el estado final estático correspondiente a la animación.");
            transitions.put(key,new Transition(source,from,to,reversible,digest(value)));
        }
        return new IconManagementManifest(id,name,author,license,icons,transitions,unsupported,root.containsKey("colorPresets")?IconColorPresets.parse(IconManagementJson.object(root.get("colorPresets"))):Collections.emptyMap());
    }
    public IconManagementManifest withHashes(Map<String,String> hashes)throws IOException{
        Map<String,Asset> icons=new TreeMap<>();Map<String,Transition> transitions=new TreeMap<>();
        for(Map.Entry<String,Asset> row:this.icons.entrySet()){Asset a=row.getValue();String hash=hashes.get(a.source);requireHash(a.sha256,hash);icons.put(row.getKey(),new Asset(a.source,a.render,hash));}
        for(Map.Entry<String,Transition> row:this.transitions.entrySet()){Transition a=row.getValue();String hash=hashes.get(a.source);requireHash(a.sha256,hash);transitions.put(row.getKey(),new Transition(a.source,a.from,a.to,a.reversible,hash));}
        return new IconManagementManifest(id,name,author,license,icons,transitions,unsupportedRoles,colorPresets);
    }
    private static void requireHash(String declared,String actual)throws IOException{if(!IconManagementSettings.validPackId(actual)||!declared.isEmpty()&&!declared.equals(actual))throw IconManagementJson.error("Falló la verificación del resumen de recursos.");}
    public byte[] bytes()throws IOException{
        Map<String,Object> root=new LinkedHashMap<>(),icons=new TreeMap<>(),transitions=new TreeMap<>();root.put("format",FORMAT);root.put("version",3);root.put("id",id);root.put("name",name);root.put("author",author);root.put("license",license);root.put("fallback","native");
        for(Map.Entry<String,Asset> row:this.icons.entrySet()){Asset a=row.getValue();Map<String,Object> value=new LinkedHashMap<>();value.put("source",a.source);value.put("render",a.render);if(!a.sha256.isEmpty())value.put("sha256",a.sha256);icons.put(row.getKey(),value);}
        for(Map.Entry<String,Transition> row:this.transitions.entrySet()){Transition a=row.getValue();Map<String,Object> value=new LinkedHashMap<>();value.put("source",a.source);value.put("from",a.from);value.put("to",a.to);value.put("reversible",a.reversible);if(!a.sha256.isEmpty())value.put("sha256",a.sha256);transitions.put(row.getKey(),value);}
        root.put("icons",icons);root.put("transitions",transitions);if(!colorPresets.isEmpty())root.put("colorPresets",IconColorPresets.values(colorPresets));byte[] bytes=IconManagementJson.encode(root).getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw IconManagementJson.error("El manifiesto normalizado supera el límite.");return bytes;
    }
    public Set<String> sources(){Set<String> result=new TreeSet<>();for(Asset asset:icons.values())result.add(asset.source);for(Transition transition:transitions.values())result.add(transition.source);return Collections.unmodifiableSet(result);}
    public void requireRuntimeHashes()throws IOException{for(Asset asset:icons.values())if(!IconManagementSettings.validPackId(asset.sha256))throw IconManagementJson.error("Falta el resumen de un diseño en tiempo de ejecución.");for(Transition transition:transitions.values())if(!IconManagementSettings.validPackId(transition.sha256))throw IconManagementJson.error("Falta el resumen de una animación en tiempo de ejecución.");}
}
