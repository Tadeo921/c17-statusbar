// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.util.*;

/** Fixed data identities. A package can supply artwork but can never register native lookup rules. */
public final class IconRoleRegistry {
    public static final String[] SCOPES={"hint","wifi","cellular","battery","tile","slider","media","device","footer"};
    public static final class Target{
        public final String id,title,scope,reason;public final List<String> roles;public final Map<String,String> stateLabels;
        public final boolean supported,dynamicSupported;
        private Target(String id,String title,String scope,boolean supported,String reason,boolean dynamic,String... states){
            this.id=id;this.title=title;this.scope=scope;this.supported=supported;this.reason=reason;dynamicSupported=dynamic;
            Map<String,String> labels=new LinkedHashMap<>();List<String> roles=new ArrayList<>();
            for(int i=0;i<states.length;i+=2){roles.add(id+'.'+states[i]);labels.put(states[i],states[i+1]);}
            this.roles=Collections.unmodifiableList(roles);stateLabels=Collections.unmodifiableMap(labels);
        }
    }
    public static final class Role{
        public final String id,targetId,state,title,scope;public final boolean supported;
        private Role(Target target,String state){id=target.id+'.'+state;targetId=target.id;this.state=state;title=target.title+" · "+target.stateLabels.get(state);scope=target.scope;supported=target.supported;}
    }
    private static final Map<String,Target> TARGETS=new LinkedHashMap<>();private static final Map<String,Role> ROLES=new LinkedHashMap<>();
    private static final Map<String,String> TILE_SPECS=new LinkedHashMap<>();
    static{
        for(String slot:NativeStatusIcons.PRIORITY_SLOTS){String title=NativeStatusIcons.priorityLabel(slot);
            if("bluetooth".equals(slot))add(new Target("hint."+slot,title,"hint",true,"",false,"on","Conectado","off","Desconectado"));
            else add(new Target("hint."+slot,title,"hint",true,"",false,"default","Estado mostrado por el sistema"));
        }
        add(new Target("wifi","Wi-Fi","wifi",true,"",false,"none","Desconectado","0","Sin barras","1","Una barra","2","Dos barras","3","Tres barras","4","Señal completa"));
        add(new Target("cellular","Señal móvil","cellular",true,"",false,"none","Desconectado","0","Sin barras","1","Una barra","2","Dos barras","3","Tres barras","4","Señal completa"));
        for(String row:new String[]{"primary","secondary"})add(new Target("cellular.stacked_"+row,
                "primary".equals(row)?"Señal de doble fila · SIM principal arriba":"Señal de doble fila · SIM secundaria abajo","cellular",true,
                "Mantiene la posición superior/inferior en el lienzo compartido; no amplía cada fila por separado",false,"none","Sin servicio","0","Sin barras","1","Una barra","2","Dos barras","3","Tres barras","4","Señal completa"));
        for(String id:new String[]{"battery","battery.charging"}){String[] levels=new String[22];for(int i=0;i<=10;i++){levels[i*2]=Integer.toString(i*10);levels[i*2+1]=(i*10)+"%";}add(new Target(id,id.equals("battery")?"Contorno de batería":"Contorno de batería en carga","battery",true,"",false,levels));}
        // Exact stock specs and state contracts: ICON-V3-QS-STATE-CONTRACT.zh-CN.md.
        for(String[] tile:new String[][]{{"wifi","wifi","Wi-Fi"},{"bluetooth","bt","Bluetooth"},{"mobile_data","cell","Datos móviles"},{"hotspot","hotspot","Punto de acceso"},{"airplane","airplane","Modo avión"},{"nfc","nfc","NFC"},{"flashlight","flashlight","Linterna"},{"rotation","rotation","Rotación de pantalla"},{"dark","darkmode","Modo oscuro"},{"location","location","Ubicación"},{"camera","cameratoggle","Permiso de cámara"},{"microphone","mictoggle","Permiso de micrófono"}}){
            String off="Desactivado",on="Activado";if(tile[0].equals("camera")||tile[0].equals("microphone")){off="Bloquear acceso";on="Permitir acceso";}else if(tile[0].equals("rotation")){off="Rotación bloqueada";on="Rotación automática";}else if(tile[0].equals("dark")){off="Modo claro";on="Modo oscuro";}
            tile(tile[0],tile[1],tile[2],true,true,"off",off,"on",on,"unavailable","No disponible");
        }
        add(new Target("tile.cast","Transmitir pantalla","tile",false,"La fábrica de recursos stock no tiene esta especificación; se conserva el diseño del sistema",false,"off","Desactivado","on","Activado","unavailable","No disponible"));
        for(String[] tile:new String[][]{{"auto_brightness","autobrightness","Brillo automático"},{"auto_color_temperature","autoscreentemperature","Temperatura de color automática"},{"data_saver","saver","Ahorro de datos"},{"dolby","dobly","Audio Dolby"},{"breathing_light","breathinglight","Luz de notificación"},{"work","work","Perfil de trabajo"},{"osie","osie","Mejora de imagen OSIE"},{"iris","lris","Mejora de vídeo"},{"vpn","vpn","Estado de VPN"},{"otg","otg","OTG"},{"dnd","dnd","No molestar"},{"no_disturb","nodisturb","No molestar nativo"},{"battery_saver","battery","Ahorro de batería"},{"phone_anti_theft","phoneantitheft","Alarma antirrobo"},{"super_power_saver","superpowersave","Ahorro de batería extremo"},{"vibration","vibration","Vibración del modo de timbre actual"},{"small_screen","smallscreen","Modo de pantalla pequeña con una mano"},{"envelope","envelope","Asistente de sobres rojos"}}){
            boolean unavailable=Arrays.asList("iris","super_power_saver","small_screen").contains(tile[0]);
            tile(tile[0],tile[1],tile[2],true,false,unavailable?new String[]{"off","Desactivado","on","Activado","unavailable","No disponible"}:new String[]{"off","Desactivado","on","Activado"});
        }
        for(String[] tile:new String[][]{{"alipay","alipay","Pago de Alipay"},{"ali_scan","aliscan","Escanear con Alipay"},{"wechat_pay","weixinpay","Pago de WeChat"},{"wechat_scan","weixinscan","Escanear con WeChat"},{"calculator","calculator","Calculadora"},{"clean","clear","Limpiar"},{"google_now","googlenow","Acceso al asistente"},{"lock_screen","lockscreen","Pantalla de bloqueo"},{"input_method","inputmethod","Selección de teclado"},{"shutdown","shutdown","Opciones de energía"},{"qr_scanner","qr_code_scanner","Escáner QR del sistema"},{"multi_system","multisystem","Cambio de sistema"},{"screenshot","screenshot","Captura de pantalla"}})
            tile(tile[0],tile[1],tile[2],true,false,"default","Acceso a la acción");
        tile("ringer_mode","ringermode","Modo de timbre",true,false,"silent","Silencio","vibrate","Vibración","ring","Sonido");
        tile("screen_record","screenrecord","Grabación de pantalla",true,false,"unavailable","No disponible","idle","Listo para grabar la pantalla","recording","Grabación en curso");
        tile("controls","controls","Controles del dispositivo",false,false,"default","Acceso a servicios del sistema");
        tile("wallet","wallet","Billetera",false,false,"default","Tarjetas y servicios del sistema");
        add(new Target("slider.volume.normal","Volumen · altavoz del teléfono","slider",true,"",false,"mute","Silencio","low","Volumen bajo","medium","Volumen medio","high","Volumen alto"));
        add(new Target("slider.volume.wired","Volumen · dispositivo con cable","slider",true,"",false,"mute","Silencio","unmute","Con volumen"));
        add(new Target("slider.volume.bluetooth","Volumen · dispositivo Bluetooth","slider",true,"",false,"mute","Silencio","unmute","Con volumen"));
        add(new Target("slider.brightness","Icono principal de brillo","slider",true,"",false,"default","Brillo actual"));
        add(new Target("slider.brightness_auto","Indicador de brillo automático","slider",true,"Solo aparece cuando el sistema usa realmente el brillo automático",false,"default","Modo automático"));
        for(String[] footer:new String[][]{{"settings","Ajustes"},{"more","Más"}})add(new Target("footer."+footer[0],footer[1],"footer",true,"",false,"default","Acceso"));
        add(new Target("media.play_pause","Controles de reproducción multimedia","media",true,"Solo representa el estado estático de controles estándar; conserva las animaciones multimedia del sistema",false,"play","Reproducir","pause","Pausar"));
        for(String[] media:new String[][]{{"previous","Pista anterior"},{"next","Pista siguiente"},{"output","Salida de audio"}})add(new Target("media."+media[0],media[1],"media",true,"Solo incluye acciones estándar y salidas predeterminadas verificadas; conserva las acciones personalizadas y los iconos de dispositivos",false,"default","Acción estándar del sistema"));
        add(new Target("device.entrance","Acceso a los controles del dispositivo","device",true,"Solo para el acceso a dispositivos conectados al sistema",false,"default","Acceso"));
        for(String[] device:new String[][]{{"phone","Teléfono"},{"pad","Tableta"},{"computer","Ordenador"},{"headset","Auriculares"},{"tv","Televisor"},{"watch","Reloj inteligente"},{"wristband","Pulsera inteligente"}})add(new Target("device."+device[0],device[1],"device",true,"Solo usa iconos genéricos cuando coinciden el tipo, la ausencia de una foto y el recurso nativo predeterminado; conserva los iconos de marca",false,"default","Icono genérico del sistema"));
    }
    private static void add(Target target){TARGETS.put(target.id,target);for(String state:target.stateLabels.keySet()){Role role=new Role(target,state);ROLES.put(role.id,role);}}
    private static void tile(String id,String spec,String title,boolean supported,boolean dynamic,String... states){String target="tile."+id;add(new Target(target,title,"tile",supported,supported?"":"Se conserva el icono nativo para servicios complejos y accesos a tarjetas",dynamic,states));TILE_SPECS.put(spec,target);}
    /** Only the audited OEM factory identities; aliases, labels and third party custom specs never match. */
    public static String tileTarget(String spec){return TILE_SPECS.get(spec);}
    public static List<Target> targets(String scope){List<Target> result=new ArrayList<>();for(Target target:TARGETS.values())if(scope==null||target.scope.equals(scope))result.add(target);return Collections.unmodifiableList(result);}
    public static Target target(String id){return TARGETS.get(id);}
    public static Role role(String id){return ROLES.get(id);}
    /** These two native layers share one viewport; their transparent space separates the rows. */
    public static boolean sharedSignalCanvas(String role){Role found=ROLES.get(role);return found!=null&&
            (found.targetId.equals("cellular.stacked_primary")||found.targetId.equals("cellular.stacked_secondary"));}
    public static boolean validScope(String scope){return Arrays.asList(SCOPES).contains(scope);}
    public static String scopeTitle(String scope){String[] titles={"Indicaciones del sistema","Wi-Fi","Señal móvil","Batería","Mosaicos del centro de control","Control deslizante","Controles multimedia","Iconos de dispositivos","Acceso del pie de página"};for(int i=0;i<SCOPES.length;i++)if(SCOPES[i].equals(scope))return titles[i];throw new IllegalArgumentException("Ámbito de iconos desconocido");}
    public static String scopeOfRole(String role){Target target=targetForRole(role);if(target!=null)return target.scope;int end=role==null?-1:role.indexOf('.');return end>0&&validScope(role.substring(0,end))?role.substring(0,end):null;}
    public static Target targetForRole(String role){
        if(role==null)return null;Target longest=null;for(Target target:TARGETS.values())if(role.startsWith(target.id+'.')&&(longest==null||target.id.length()>longest.id.length()))longest=target;
        if(longest==null&&role.startsWith("hint."))longest=TARGETS.get(role);return longest;
    }
    public static boolean validResourceRole(String role){return role!=null&&role.length()<=128&&role.matches("(?:hint|wifi|cellular|battery|tile|slider|media|device|footer)(?:\\.[a-z0-9][a-z0-9_]{0,47}){1,5}");}
    public static boolean knownResourceRole(String role){return ROLES.containsKey(role)||role!=null&&role.startsWith("hint.")&&TARGETS.containsKey(role);}
    public static boolean validKnownState(String role){Target target=targetForRole(role);return target==null||ROLES.containsKey(role)||target.id.equals(role)&&"hint".equals(target.scope);}
    public static List<String> roles(String scope){List<String> result=new ArrayList<>();for(Target target:targets(scope))result.addAll(target.roles);return Collections.unmodifiableList(result);}
    private IconRoleRegistry(){ }
}
