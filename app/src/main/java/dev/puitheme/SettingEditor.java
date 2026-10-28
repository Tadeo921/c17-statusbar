// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.math.BigDecimal;
import java.util.Map;

/** Editor context from the real catalog; suggested slider bounds never become input limits. */
public final class SettingEditor {
    private SettingEditor(){ }
    public static boolean appPickerKey(String key) {
        return NotificationBigClockSettings.CLOCK_LAUNCH_APP.equals(key)||NotificationBigClockSettings.DATE_LAUNCH_APP.equals(key)
                ||BigClockWidgetsSettings.launchAppKey(key);
    }
    public static final class Info {
        public final String context,label,initial,valueSummary,description;
        public final boolean numeric,integer,signed,pattern,previewable;
        private Info(String context,String label,String initial,String valueSummary,String description,
                boolean numeric,boolean integer,boolean signed,boolean pattern,boolean previewable){
            this.context=context;this.label=label;this.initial=initial;this.valueSummary=valueSummary;
            this.description=description;this.numeric=numeric;this.integer=integer;this.signed=signed;this.pattern=pattern;
            this.previewable=previewable;
        }
    }
    /** Prefer the confirmed raw number over a rendering guard's bounded result. Viewing never writes it. */
    public static Object storedValue(SettingsCatalog.Item item,Map<String,?> values){
        String key=StatusBarLayoutSettings.editingKey(item.key,values);
        Object raw=values==null?null:values.get(key);
        if(SettingsCatalog.NUMERIC.equals(item.type)&&raw instanceof Number
                &&Float.isFinite(((Number)raw).floatValue()))return raw;
        return SettingsCatalog.value(values,key);
    }
    public static String number(float value){
        if(!Float.isFinite(value))return Float.toString(value);
        if(Math.abs(value)>=1e7f||value!=0f&&Math.abs(value)<.001f)return Float.toString(value);
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }
    public static String formatted(SettingsCatalog.Item item,Object value){
        String text=valueText(value);
        return text+(item.unit.isEmpty()?"":" "+item.unit);
    }
    private static String valueText(Object value){
        if(value==null)return "Sin configurar";
        return value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long
                ?value.toString():value instanceof Number?number(((Number)value).floatValue()):value.toString();
    }
    public static Info forItem(SettingsCatalog.Item item,Map<String,?> values){
        Object value=storedValue(item,values);
        boolean numeric=SettingsCatalog.NUMERIC.equals(item.type),integer=NativeStatusIcons.MAX.equals(item.key)||NotificationIconArea.MAX_COUNT.equals(item.key)
                ||NotificationBigClockSettings.VISIBLE_COUNT.equals(NotificationBigClockSettings.portraitKey(item.key))
                ||StatusBarLayoutSettings.orderKey(item.key);
        boolean pattern=item.key.endsWith("_pattern")||LockscreenControls.DATE_FORMAT.equals(item.key),signed=NumericPolicy.signed(item.key);
        String initial=value==null?"":numeric?valueText(value):value.toString();
        String summary=numeric?"Valor actual: "+formatted(item,value)+"\nValor predeterminado: "+formatted(item,item.defaultValue)
                +"\n"+(QsTileCorners.RADIUS.equals(item.key)?"Rango permitido: ":"Rango recomendado del control: ")
                +number(item.min)+"～"+number(item.max)+(item.unit.isEmpty()?"":" "+item.unit):"";
        boolean component=BigClockWidgetsSettings.textKey(item.key);
        String label=numeric?(item.unit.isEmpty()?"Grosor tipográfico (sin unidad)":item.unit.equals("unidades")?"Cantidad (elementos)":"Valor ("+item.unit+"）")
                :pattern?"Formato personalizado":component?"Contenido del componente":"Texto de una sola línea";
        if(numeric&&item.unit.isEmpty()&&!item.key.endsWith("_weight")&&!item.key.equals(StatusBarSettings.FONT_WEIGHT))label="Valor (sin unidad)";
        String description=numeric?numericDescription(item,values):textDescription(item);
        String context=context(item);
        if(!item.key.equals(StatusBarLayoutSettings.editingKey(item.key,values)))context+=" · compartido con Wi-Fi";
        return new Info(context,label,initial,summary,description,numeric,integer,signed,pattern,pattern||component);
    }
    public static String context(SettingsCatalog.Item item){
        SettingsCatalog.Group group=SettingsCatalog.group(item.groupId);
        String title=group==null?item.groupId:group.title;
        if("native_network_badge".equals(item.groupId)) {
            if(item.key.startsWith(NativeNetworkBadgeSettings.PART_ONE+"_"))title+=" · indicador 1";
            else if(item.key.startsWith(NativeNetworkBadgeSettings.PART_TWO+"_"))title+=" · indicador 2";
        }
        if("carrier".equals(item.groupId))title=item.key.startsWith(CarrierPanels.LOCKSCREEN)?"Texto del operador en la pantalla de bloqueo"
                :item.key.startsWith(CarrierPanels.CONTROL)?"Texto del operador en el centro de control":"Texto del operador en la barra de notificaciones";
        int separator=item.section.lastIndexOf(" · ");
        String section=separator>=0?item.section.substring(separator+3):item.section;
        return title+(section.isEmpty()?"":" · "+section);
    }
    private static String numericDescription(SettingsCatalog.Item item,Map<String,?> values){
        String key=NotificationBigClockSettings.portraitKey(item.key);
        if(StatusBarLayoutSettings.orderKey(key))return StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL,"order").equals(key)
                ?"El tipo de red sigue siempre la zona de señal móvil. 0 = seguir al sistema; 1 = antes de la señal; 2 = después de la señal. No fuerza la visualización de un tipo de red que el sistema haya ocultado."
                :"Orden físico de izquierda a derecha dentro de la misma zona. 0 = seguir al sistema; desde 1, ordenar por número; los elementos con el mismo número mantienen el orden nativo. Solo se admiten enteros no negativos. Guardar no activa automáticamente la distribución.";
        if(StatusBarLayoutSettings.marginKey(key))return (StatusBarSettings.bool(values,StatusBarLayoutSettings.WIFI_LABEL_SHARED)
                &&("wifi".equals(item.groupId)||"label".equals(item.groupId))?"Wi-Fi y el tipo de red comparten este espaciado; los cambios se sincronizan entre ambas páginas. Al desactivar el uso compartido, se restauran los ajustes independientes.\n":"")
                +"Ajusta el espacio en relación con los límites nativos del elemento; izquierda y derecha se refieren a los lados físicos de la pantalla. Los valores positivos amplían el espacio, los negativos lo reducen y 0 conserva el espacio nativo. El elemento mantiene las animaciones del sistema. El margen es independiente del orden y del interruptor de estilo.\nSe admiten decimales positivos y negativos y notación científica. El rango recomendado del control deslizante no limita la entrada manual. Los valores fuera del rango recomendado se prueban durante 20 segundos y se revierten si no se confirman. La distribución sigue protegida por los límites físicos. Se conserva el desplazamiento horizontal antiguo, pero no se suma cuando se aplica el margen.";
        if(QsTileCorners.RADIUS.equals(key))return "Ajusta de forma conjunta las esquinas redondeadas de mosaicos, tarjetas multimedia, brillo, volumen y dispositivos. 0 significa esquinas rectas; se permite de 0 a "+number(QsTileCorners.MAX_RADIUS)+" dp. Los decimales introducidos manualmente no se redondean al paso del control deslizante.\nLos valores dentro del rango recomendado se guardan directamente; la función no se activa automáticamente.";
        if(NotificationIconArea.MAX_COUNT.equals(key))return "Solo se aplica a los iconos de notificaciones nativos y debe ser un entero no negativo. 0 oculta los iconos, pero no elimina las notificaciones. Puede superar la cantidad recomendada por el control deslizante.\nLos valores dentro del rango recomendado se guardan directamente; los que queden fuera se prueban durante 20 segundos antes de confirmarlos.";
        if(NotificationBigClockSettings.SCREEN_PADDING.equals(key))return "Solo se aplica a la fuente nativa en vertical. Ajusta el margen del reloj respecto a los bordes seguros izquierdo y derecho de la pantalla, con el mismo valor en ambos lados. La altura de los caracteres cambia dentro del ancho restante, mientras que el ancho permanece estable; no cambia el tamaño de las tarjetas de notificación. 0 conserva los márgenes seguros de la pantalla.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NotificationIconArea.SPACING.equals(key))return "Aumenta o reduce el espacio entre los iconos de notificación del sistema; 0 conserva el espacio nativo. Solo cambia el hueco entre varios iconos, no su tamaño. Los símbolos, textos o imágenes individuales no se ven afectados. La cantidad visible sigue limitada por el espacio disponible en la barra de estado.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NotificationBigClockSettings.MAX_SIZE.equals(key)||NotificationBigClockSettings.COMPACT_MAX_SIZE.equals(key))return "Establece en dp la altura real del texto horario: cuanto mayor sea el valor, más altos serán los caracteres. En horizontal se usa una única altura. En vertical, solo la fuente nativa distingue entre la altura expandida y la contraída; las demás fuentes usan una sola altura. El ancho disponible limita la visualización para evitar que el texto salga de la pantalla.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NativeStatusIcons.MAX.equals(key))return "Solo cuenta los iconos de avisos del sistema, como Bluetooth, ubicación y alarma; no incluye notificaciones, red ni batería. Introduce un entero no negativo. 0 oculta los avisos y se conservan primero los de mayor prioridad. Si la cantidad supera los avisos existentes, solo se muestran los que haya.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NativeStatusIcons.X.equals(key)||NativeStatusIcons.Y.equals(key))return "Respecto a la posición predeterminada del sistema, los valores horizontales positivos mueven a la derecha y los negativos a la izquierda; los valores verticales positivos bajan y los negativos suben. Solo mueve los iconos de aviso y conserva sus animaciones nativas. La posición se limita al espacio disponible para evitar tapar la zona de red o quedar recortada por el contenedor.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NativeStatusIcons.SPACING.equals(key))return "Añade la distancia introducida al espaciado nativo entre iconos de aviso; 0 conserva el espaciado del sistema. La visualización está limitada por el espacio del contenedor y no cambia el espaciado de la señal móvil, la batería ni el Wi-Fi.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        if(NotificationBigClockSettings.VISIBLE_COUNT.equals(key))return "Define cuántas notificaciones se muestran completas en una pila. Introduce un entero entre 1 y 2147483647; se recomiendan de 1 a 5, aunque puedes usar valores mayores. Si el número supera las notificaciones existentes, se muestran todas con normalidad; no se generan notificaciones adicionales.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman.";
        String detail;
        if(key.endsWith("_weight")||StatusBarSettings.FONT_WEIGHT.equals(key))detail=weightDescription(item,values);
        else if(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY.equals(key))detail="Intensidad del tinte de las notificaciones normales en la pantalla de bloqueo y AOD respecto a las tarjetas de Fluid Cloud. 100 % las iguala por completo (usa la opacidad de relleno de esta página o la de la pantalla de bloqueo si está personalizada); los valores menores hacen que las notificaciones normales se vean más tenues.";
        else if(SpeedPosition.EDGE_RESERVE.equals(key))detail="Zona fija reservada para la señal y el Wi-Fi. Al activar el ajuste de posición de la velocidad de red o la distancia respecto al icono vecino, la velocidad se coloca en relación con esta zona. El Wi-Fi y los datos móviles no cambian de posición; el ajuste también funciona con un desplazamiento horizontal de 0. 0 no reserva espacio. Si el icono real es más ancho, la posición se mantiene; aumenta el valor si se superponen. El reloj se alinea con sus propios bordes y no usa esta reserva.";
        else if(SpeedPosition.SAFE_GAP.equals(key))detail="Distancia entre la velocidad de red y la zona reservada seleccionada o el borde del reloj: 0 dp significa pegado al borde, los valores negativos acercan y permiten superposición, y los positivos separan. Si el icono de señal se ensancha, la velocidad no se desplaza. Activa «Mantener distancia con el icono vecino»; al activarlo, el desplazamiento horizontal anterior deja de aplicarse y se restaura al desactivarlo.";
        else if(key.endsWith("offset_x"))detail=positionDescription(item,true);
        else if(key.endsWith("offset_y"))detail=positionDescription(item,false);
        else if("em".equals(item.unit))detail="Ajusta el espaciado entre caracteres según el tamaño de fuente: 1 em equivale al tamaño actual, los valores positivos separan y los negativos acercan; 0 no añade ningún ajuste.";
        else if(key.endsWith("_spacing"))detail="Añade o resta la distancia indicada al espaciado nativo. Los valores positivos separan los caracteres, los negativos los acercan y 0 no añade ningún ajuste.";
        else if(key.endsWith("_line_gap"))detail="Ajusta la distancia entre el número y la unidad. Se admiten valores positivos y negativos; 0 no añade espacio adicional.";
        else if("sp".equals(item.unit))detail="Define el tamaño de fuente en sp; se escala con el tamaño de texto del sistema.";
        else if("ms".equals(item.unit))detail="Introduce el intervalo de actualización de la velocidad de red en milisegundos (1 segundo = 1000 ms). Se recomiendan valores de 1 a 500; cuanto más corto sea el intervalo, más frecuentes serán el cálculo y la actualización. Los valores menores de 1 ms se tratan como 1 ms.";
        else if("segundos".equals(item.unit)||"s".equals(item.unit))detail="Introduce la duración en segundos; se admiten decimales. Por ejemplo, 0,25 equivale a 250 ms.";
        else if("°".equals(item.unit))detail="Introduce la dirección del degradado en grados, sin escribir el símbolo de grados.";
        else if("%".equals(item.unit))detail="Introduce el porcentaje sin escribir el símbolo %; por ejemplo, 100 significa 100 %.";
        else if("dp".equals(item.unit))detail="Se define en dp"+item.title+", y se convierte en distancia visible según la densidad de pantalla。";
        else detail=item.description;
        return detail+"\nSe admiten decimales y notación científica; el rango recomendado no limita la entrada manual. Los valores extremos pueden provocar desplazamientos, elementos invisibles, lentitud o fallos de la interfaz del sistema; el dibujo sigue sujeto a límites de seguridad.\nLos valores dentro del rango recomendado se guardan directamente; los valores fuera se prueban durante 20 segundos y se revierten si no se confirman. No se activa la función automáticamente.";
    }
    private static String positionDescription(SettingsCatalog.Item item,boolean horizontal){
        String base=item.groupId.startsWith("notification_big_clock")
                ?item.key.contains("_compact_offset_")?"Ajusta adicionalmente la posición contraída respecto al desplazamiento inicial": "Ajusta respecto a la posición de diseño predeterminada de este contenido"
                :"Ajusta respecto a la posición nativa";
        return base+"："+(horizontal?"Los valores positivos mueven a la derecha y los negativos a la izquierda":"Los valores positivos mueven hacia abajo y los negativos hacia arriba")+", 0 no añade desplazamiento.";
    }
    private static String weightDescription(SettingsCatalog.Item item,Map<String,?> values){
        String mode;
        if("battery".equals(item.groupId))mode="native";
        else if("native_network_badge".equals(item.groupId))mode=StatusBarSettings.string(values,
                item.key.startsWith(NativeNetworkBadgeSettings.PART_ONE+"_")?NativeNetworkBadgeSettings.key(1,"font"):
                item.key.startsWith(NativeNetworkBadgeSettings.PART_TWO+"_")?NativeNetworkBadgeSettings.key(2,"font"):NativeNetworkBadgeControls.FONT);
        else if(item.groupId.startsWith("notification_big_clock"))mode=StatusBarSettings.string(values,
                "notification_big_clock_landscape".equals(item.groupId)
                        ?NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.FONT):NotificationBigClockSettings.FONT);
        else mode=StatusBarSettings.bool(values,"font_enabled")?StatusBarSettings.string(values,StatusBarSettings.FONT_MODE):"system";
        if("global".equals(mode))mode=StatusBarSettings.bool(values,"font_enabled")?StatusBarSettings.string(values,StatusBarSettings.FONT_MODE):"system";
        String revision=StatusBarSettings.string(values,StatusBarSettings.FONT_REVISION);
        String scope=fontScope(item.groupId);
        if(scope!=null&&FeatureOptions.from(values).enabled("font"))try{
            String assigned=FontScopes.assignment(values,scope);
            if(assigned!=null){mode=FontScopes.mode(assigned);revision=FontScopes.revision(assigned);}
        }catch(IllegalArgumentException malformed){/* The editor mirrors the runtime's legacy fallback. */}
        String source;
        if("pingfang".equals(mode))source="PingFang; el eje real de grosor va de 100 a 900";
        else if("custom".equals(mode)){
            FontCatalog.Entry entry=FontCatalog.forRevision(revision);
            source=entry==null?"Fuente personalizada; el rango real depende del eje de grosor del archivo":entry.displayName+", el eje real de grosor es "+entry.minWeight+"～"+entry.maxWeight;
        }else source="native".equals(mode)?"Fuente nativa":"Fuente del sistema";
        return "Origen actual: "+source+"。Cuanto mayor sea el valor, más gruesa suele ser la fuente. Se guarda el valor decimal original, mientras que el dibujo lo transforma a un entero de 1 a 1000 y respeta el rango real del eje de la fuente seleccionada.";
    }
    private static String fontScope(String group){
        if("clock".equals(group))return FontScopes.STATUSBAR_CLOCK;
        if("shade_clock".equals(group)||"classic_clock".equals(group))return FontScopes.SHADE_CLOCK;
        if("label".equals(group)||"native_network_badge".equals(group))return FontScopes.NETWORK_TYPE;
        if("speed".equals(group))return FontScopes.NETWORK_SPEED;
        if(CarrierPanels.LOCKSCREEN.equals(group))return FontScopes.LOCKSCREEN_TEXT;
        if("carrier".equals(group)||CarrierPanels.isPanel(group))return FontScopes.SHADE_TEXT;
        if(group.startsWith("notification_big_clock"))return FontScopes.NOTIFICATION_CLOCK;
        return null;
    }
    private static String textDescription(SettingsCatalog.Item item){
        String key=NotificationBigClockSettings.portraitKey(item.key);
        if(NativeStatusIcons.PRIORITY.equals(item.key))return "Introduce los elementos por prioridad, por ejemplo: Bluetooth, ubicación, alarma. También puedes usar identificadores del sistema como bluetooth, location y alarm_clock. Los elementos al principio de la lista tienen prioridad si no hay espacio para todos; los no incluidos conservan el orden del sistema. Solo se aplica a avisos existentes y no bloqueados por el sistema; no crea iconos nuevos.";
        if(NotificationIconArea.TEXT.equals(item.key))return "Se usa cuando hay una notificación visible y el modo de visualización es texto personalizado. Puedes introducir texto en chino, inglés o emoji, hasta 12 caracteres Unicode, en una sola línea.";
        if(item.key.endsWith("_pattern")||LockscreenControls.DATE_FORMAT.equals(item.key))
            return patternHelp(NotificationBigClockSettings.FOOTER_PATTERN.equals(key));
        if(BigClockWidgetsSettings.textKey(item.key))return componentHelp(item.key.equals(BigClockWidgetsSettings.RIGHT_TEXT));
        return item.description+(item.description.isEmpty()?"":"\n")+"Mantén el texto en una sola línea; máximo "+TimeFormat.MAX_TEXT+" caracteres. Guardar el texto no activa automáticamente la función correspondiente.";
    }
    /** What a format may contain, so the field is never a guess. Every token here is accepted by TimeFormat. */
    static String patternHelp(boolean footer){
        return "Campos disponibles (se muestra una vista previa en tiempo real mientras escribes)\n"
                +"Año: yyyy 2026 · yy 26 · {年}\n"
                +"Mes: M 10 · MM 10 (añade cero inicial) · {月}; día: d 7 · dd 07 · {日}\n"
                +"Formato de 24 horas: H 8 · HH 08 · {时}; formato de 12 horas: h 8 · hh 08 · {12时}\n"
                +"Minutos: m 3 · mm 03 · {分}; segundos: s 9 · ss 09 · {秒}\n"
                +"AM/PM: a o {上午下午}\n"
                +"Franja horaria: {时段}; muestra madrugada, mañana, mediodía, tarde o noche según la hora\n"
                +"Día de la semana: {周} (miércoles) · {星期} (miércoles); también puedes usar E o EEEE\n"
                +"Calendario lunar: {农历} año/día lunar · {农历日期} fecha lunar · {干支} año sexagenario\n"
                +(footer?"Texto inferior: {text} o {文本}; inserta el texto inferior configurado arriba y no se interpreta como formato\n":"")
                +"Si se incluyen segundos (s, ss o {秒}), se actualiza una vez por segundo.\n"
                +"Escribe el texto fijo entre comillas simples emparejadas, por ejemplo 'C17' HH:mm; puedes introducir directamente símbolos y texto.\n"
                +"Ejemplo: HH:mm:ss → 08:03:09; M月d日 {周} {时段} HH:mm → fecha y hora local\n"
                +"Máximo "+TimeFormat.MAX_PATTERN+" caracteres, en una sola línea. La vista previa indicará los errores y se validará antes de guardar.";
    }
    /** What a left/right component may contain; every token listed is resolved by BigClockTextContent. */
    static String componentHelp(boolean right){
        return "Toca un ejemplo de arriba para insertarlo o escribe tu propio formato. El texto normal se muestra tal cual y puede combinarse con campos (se admiten paréntesis chinos y occidentales). La vista previa aparece mientras escribes; los iconos son ilustrativos.\n"
                +"\nFormato personalizado (categoría: plantilla)\n"
                +"El clima, la batería, la señal y la red se pueden personalizar igual que la hora: combina texto con {campos} en la plantilla y escribe tú mismo los símbolos.\n"
                +"{campo} obtiene un valor · {campo:parámetro} usa un parámetro · {campo?texto si se cumple} · {campo?texto si se cumple|texto si no} · {电量<=20?低|够} compara números (< <= > >= = !=)\n"
                +"Para mostrar los símbolos { } | ( ) :, ponlos entre comillas simples, por ejemplo '（受限）'; '' representa una comilla simple.\n"
                +"Clima: {描述} {描述:中文} {描述:英文} {温度} {温度:数字} {温度:单位} {图标} {图标:原色}; condiciones {夜} {昼} {有数据} {有温度}\n"
                +"Batería: {电量} {电量:000} (relleno con ceros) {状态} {状态:英文} {条:格数:实心:空心} {图标}; condiciones {充电} {满电} {未充电} {放电} {低电} {有数据}\n"
                +"Señal: {格数} {总格} {状态} {条:格数:实心:空心} {图标}; condiciones {飞行} {无卡} {无服务} {有信号} {有数据}\n"
                +"Red: {类型} {制式} {状态} {WiFi格数} {条:格数:实心:空心} {图标}; condiciones {联网} {WiFi} {蜂窝} {有线} {离线} {受限} {有数据}\n"
                +"Ejemplos: （电池:{电量}%{充电?⚡}）→ 85%⚡ · （电池:{条:5:█:░} {电量}%）→ ████░ 85% · （天气:{描述} {温度:数字}°C）→ clima 23°C\n"
                +"Si no hay datos, la plantilla completa (categoría:…) muestra «Sin datos». Para personalizar el aviso, usa {有数据?…|…}. Los campos incorrectos se indican en la vista previa e impiden guardar.\n"
                +"\nLos formatos anteriores siguen funcionando\n"
                +"Clima: （天气）（天气中文）（天气英文）（天气原色）（天气描述）（天气图标）（温度）; estado: （蜂窝信号）（网络状态）（电池）\n"
                +"Hora: （时间）（12小时）（上午下午）（时段）; fecha: （日期）（年月日）（年）（星期）（周）（农历）（农历日期）（干支）\n"
                +"Inglés: （日期英文）Oct 17 · （星期英文）Saturday · （星期英文简）Sat · （月份英文）October\n"
                +"Formato de hora personalizado: （格式:HH:mm {周}） usa la misma sintaxis que el formato horario; no admite segundos y no se sustituye si los contiene.\n"
                +"El clima procede del widget meteorológico de la pantalla de bloqueo; los modos chino e inglés fijan el texto, pero no traducen por Internet. Máximo "+BigClockWidgetsSettings.MAX_TEXT+" caracteres.";
    }
    /** Ready-made formats ("预设") for a time-class field, as {format, name} pairs the editor offers above the input.
     * Every time-class field has some: the field's own catalog list when it has one, else a general set. */
    public static String[][] presets(SettingsCatalog.Item item){
        if(item!=null&&BigClockWidgetsSettings.textKey(item.key)){
            String[][] result=new String[item.values.length][];
            for(int i=0;i<result.length;i++)result[i]=new String[]{item.values[i],i<item.labels.length?item.labels[i]:item.values[i]};
            return result;
        }
        if(item==null||!(item.key.endsWith("_pattern")||LockscreenControls.DATE_FORMAT.equals(item.key)))return new String[0][];
        String[] values=item.values,labels=item.labels;
        if(values.length==0){
            values=new String[]{"HH:mm","HH:mm:ss","hh:mm","M月d日 {周}","M月d日 {周} HH:mm","yyyy年M月d日 {星期}","{时段} HH:mm","MM/dd HH:mm"};
            labels=new String[]{"Hora y minutos · 24 horas","Hora, minutos y segundos","Hora · formato de 12 horas","Día, mes y día de la semana","Día, mes y hora","Fecha completa y día de la semana","Franja horaria y hora","Fecha corta y hora"};
        }
        String[][] result=new String[values.length][];
        for(int i=0;i<values.length;i++)result[i]=new String[]{values[i],i<labels.length?labels[i]:values[i]};
        return result;
    }
    /** Live result of a format for the editor, or the reason it cannot be saved. Empty when the field is not a format. */
    public static CharSequence preview(SettingsCatalog.Item item,String input){
        if(BigClockWidgetsSettings.textKey(item.key))return BigClockPresets.preview(input,System.currentTimeMillis());
        boolean pattern=item.key.endsWith("_pattern")||LockscreenControls.DATE_FORMAT.equals(item.key);
        if(!pattern)return "";
        try{
            long now=System.currentTimeMillis();
            String key=NotificationBigClockSettings.portraitKey(item.key);
            return NotificationBigClockSettings.FOOTER_PATTERN.equals(key)?TimeFormat.formatContent(input,"Texto de ejemplo",now):TimeFormat.format(input,now);
        }catch(IllegalArgumentException invalid){
            return "Formato no válido: "+invalid.getMessage();
        }
    }
    /** Captured before NumericTrial.begin(); includes both confirmed and proposed values. */
    public static String trialSummary(SettingsCatalog.Item item,Object confirmed,float proposed,Map<String,?> values){
        return context(item)+"\nÚltimo valor confirmado: "+formatted(item,confirmed)+"\nValor de prueba actual: "+formatted(item,proposed)
                +(proposed<item.min||proposed>item.max?"\nFuera del rango recomendado: "+number(item.min)+"～"+number(item.max)+(item.unit.isEmpty()?"":" "+item.unit):"")
                +"\n"+numericDescription(item,values)+"\nSi cancelas, se agota el tiempo o sales de la aplicación, se restaura el último valor confirmado.";
    }
}
