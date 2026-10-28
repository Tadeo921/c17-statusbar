// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Explicit font assignments. Missing scopes keep the complete pre-upgrade selection. */
public final class FontScopes {
    public static final String KEY="font_scope_assignments";
    public static final String STATUSBAR_CLOCK="statusbar_clock", SHADE_CLOCK="shade_clock",
            NETWORK_TYPE="network_type", NETWORK_SPEED="network_speed", SHADE_TEXT="shade_text",
            LOCKSCREEN_TEXT="lockscreen_text", NOTIFICATION_CLOCK="notification_clock";
    public static final String[] SCOPES={STATUSBAR_CLOCK,SHADE_CLOCK,NETWORK_TYPE,NETWORK_SPEED,
            SHADE_TEXT,LOCKSCREEN_TEXT,NOTIFICATION_CLOCK};
    public static final String DEFAULT="{}";
    private FontScopes(){ }
    public static String title(String scope){
        if(STATUSBAR_CLOCK.equals(scope))return "Reloj de la barra de estado";
        if(SHADE_CLOCK.equals(scope))return "Reloj del panel desplegable";
        if(NETWORK_TYPE.equals(scope))return "Texto del tipo de red";
        if(NETWORK_SPEED.equals(scope))return "Velocidad de red";
        if(SHADE_TEXT.equals(scope))return "Texto personalizado del panel";
        if(LOCKSCREEN_TEXT.equals(scope))return "Texto personalizado de la pantalla de bloqueo";
        if(NOTIFICATION_CLOCK.equals(scope))return "Reloj grande y texto adicional de notificaciones";
        throw new IllegalArgumentException("Ámbito de fuente desconocido");
    }
    public static boolean validScope(String scope){for(String known:SCOPES)if(known.equals(scope))return true;return false;}
    public static boolean validRevision(String revision){return revision!=null&&revision.matches("[0-9a-f]{64}");}
    public static String source(String mode,String revision){
        if("system".equals(mode)||"pingfang".equals(mode))return mode;
        if("custom".equals(mode)&&validRevision(revision))return "custom:"+revision;
        throw new IllegalArgumentException("Fuente no válida; vuelve a seleccionarla.");
    }
    public static String mode(String source){
        if("system".equals(source)||"pingfang".equals(source))return source;
        if(source!=null&&source.startsWith("custom:")&&validRevision(source.substring(7)))return "custom";
        throw new IllegalArgumentException("Fuente no válida; vuelve a seleccionarla.");
    }
    public static String revision(String source){return "custom".equals(mode(source))?source.substring(7):"";}
    public static String assignment(Map<String,?> values,String scope){
        if(!validScope(scope))throw new IllegalArgumentException("Ámbito de fuente desconocido");
        Object raw=values==null?null:values.get(KEY);
        return parse(raw instanceof String?(String)raw:DEFAULT).get(scope);
    }
    /** Only seven ASCII names and sources are accepted; duplicate names and escaped aliases are rejected. */
    public static Map<String,String> parse(String text){
        if(text==null||text.length()>4096)throw new IllegalArgumentException("Formato del ámbito de la fuente no válido.");
        Reader reader=new Reader(text);Map<String,String> result=new LinkedHashMap<>();reader.expect('{');
        if(!reader.take('}')){
            do{
                String scope=reader.string();if(!validScope(scope)||result.containsKey(scope))
                    throw new IllegalArgumentException("El ámbito de la fuente es desconocido o está duplicado.");
                reader.expect(':');String selected=reader.string();mode(selected);result.put(scope,selected);
            }while(reader.take(','));
            reader.expect('}');
        }
        reader.end();return Collections.unmodifiableMap(result);
    }
    public static String encode(Map<String,String> assignments){
        if(assignments==null)throw new IllegalArgumentException("Falta el ámbito de la fuente.");
        for(Map.Entry<String,String> item:assignments.entrySet()){
            if(!validScope(item.getKey()))throw new IllegalArgumentException("Ámbito de fuente desconocido");mode(item.getValue());
        }
        StringBuilder result=new StringBuilder("{");boolean first=true;
        for(String scope:SCOPES)if(assignments.containsKey(scope)){
            if(!first)result.append(',');first=false;
            result.append('"').append(scope).append("\":\"").append(assignments.get(scope)).append('"');
        }
        return result.append('}').toString();
    }
    /** Applying a subset never rewrites assignments or legacy selections outside that subset. */
    public static String merge(String current,Collection<String> selected,String source){
        mode(source);if(selected==null||selected.isEmpty())throw new IllegalArgumentException("请至少选择一个作用范围");
        Map<String,String> result=new LinkedHashMap<>(parse(current));
        for(String scope:selected){if(!validScope(scope))throw new IllegalArgumentException("Ámbito de fuente desconocido");result.put(scope,source);}
        return encode(result);
    }
    /** A paused font feature starts only the newly selected scopes; active scopes preserve unselected choices. */
    public static final class Application{
        public final String assignments;public final boolean resetLegacySource;
        private Application(String assignments,boolean resetLegacySource){this.assignments=assignments;this.resetLegacySource=resetLegacySource;}
    }
    public static Application planApplication(String current,boolean wasEnabled,Collection<String> selected,String source){
        return new Application(merge(wasEnabled?current:DEFAULT,selected,source),!wasEnabled);
    }
    public static boolean references(String assignments,String hash){
        if(!validRevision(hash))return false;return parse(assignments).containsValue("custom:"+hash);
    }
    public static String removeFont(String assignments,String hash){
        Map<String,String> result=new LinkedHashMap<>(parse(assignments));
        if(validRevision(hash))for(Map.Entry<String,String> item:result.entrySet())
            if(item.getValue().equals("custom:"+hash))item.setValue("system");
        return encode(result);
    }
    /** Portable files carry references only; a different local font never satisfies a missing hash. */
    public static String fallbackMissing(String assignments,Predicate<String> available){
        Map<String,String> result=new LinkedHashMap<>(parse(assignments));
        for(Map.Entry<String,String> item:result.entrySet())if("custom".equals(mode(item.getValue()))
                &&!available.test(revision(item.getValue())))item.setValue("system");
        return encode(result);
    }
    private static final class Reader{
        final String text;int at;Reader(String text){this.text=text;}
        void whitespace(){while(at<text.length()&&(text.charAt(at)==' '||text.charAt(at)=='\n'||text.charAt(at)=='\r'||text.charAt(at)=='\t'))at++;}
        boolean take(char token){whitespace();if(at<text.length()&&text.charAt(at)==token){at++;return true;}return false;}
        void expect(char token){if(!take(token))throw new IllegalArgumentException("Formato del ámbito de la fuente no válido.");}
        String string(){expect('"');int start=at;while(at<text.length()&&text.charAt(at)!='"'){
            char value=text.charAt(at++);if(value<' '||value>'~'||value=='\\')throw new IllegalArgumentException("Formato del ámbito de la fuente no válido.");
        }if(at>=text.length())throw new IllegalArgumentException("Formato del ámbito de la fuente no válido.");String value=text.substring(start,at);at++;return value;}
        void end(){whitespace();if(at!=text.length())throw new IllegalArgumentException("Formato del ámbito de la fuente no válido.");}
    }
}
