// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;

/** User-facing descriptors shared by the Compose configuration pages. No View or Activity state. */
public final class SettingsCatalog {
    public static final String STATUSBAR = "statusbar", NOTIFICATION = "notification", CONTROL_CENTER = "control_center", LOCK_SCREEN = "lockscreen", OTHER = "other";
    public static final String BOOLEAN = "boolean", NUMERIC = "numeric", STRING = "string";
    public static final String COLOR = "color", FONT = "font", IMAGE = "image", OPTIONS = "options";
    private static final String[] CLOCK_VALUES = {"HH:mm", "HH:mm:ss", "{上午下午} hh:mm", "{时段} HH:mm", "M月d日 {周} HH:mm", "yyyy年MM月dd日 {星期} HH:mm", "MM/dd HH:mm"};
    private static final String[] CLOCK_LABELS = {"Hora y minutos · 24 horas", "Hora, minutos y segundos · 24 horas", "AM/PM · 12 horas", "Franja horaria y hora", "Día, mes y hora", "Fecha completa, día de la semana y hora", "Fecha corta y hora"};
    private static final String[] DUAL_VALUES = {"HH:mm", "HH:mm:ss", "M月d日 {周}", "M月d日 EEEE", "{周}", "{时段}", "yyyy年M月d日", "{农历日期}", "MM/dd"};
    private static final String[] DUAL_LABELS = {"Hora y minutos", "Hora, minutos y segundos", "Día, mes y día de la semana", "Día, mes y nombre completo del día", "Día de la semana", "Franja horaria", "Fecha completa", "Fecha lunar", "Fecha corta"};
    private static final String[] DATE_VALUES = {NotificationBigClockSettings.DATE_DEFAULT, "M月d日 EEEE", "yyyy年M月d日 {星期}", "M月d日 {周} {农历}", "{干支} {农历日期}"};
    private static final String[] DATE_LABELS = {"Día, mes, día de la semana, ciclo sexagenario y calendario lunar", "Día, mes y día de la semana", "Fecha completa y día de la semana", "Día, mes, día de la semana y calendario lunar", "Año del ciclo sexagenario y fecha lunar"};
    private static final String[] FOOTER_VALUES = {"{text}", "HH:mm {text}", "yyyy年M月d日 {周} {text}", "{农历} {text}", "'C17' HH:mm {text}", "✨ {text}"};
    private static final String[] FOOTER_LABELS = {"Solo texto", "Hora y texto", "Fecha, día de la semana y texto", "Fecha lunar y texto", "C17, hora y texto", "Brillo de estrellas y texto"};
    private static final List<Group> GROUPS;
    private static final Map<String, Group> GROUP_BY_ID;
    private static final Map<String, Item> ITEM_BY_KEY;
    private static final Set<String> HIDDEN;
    /** One wording for every "过渡范围": a gradient between two colours, and a colour that fades out. */
    static final String RANGE_BETWEEN = "Longitud de la transición entre dos colores: 100 % crea una transición uniforme de un extremo al otro (valor original); cuanto menor sea el valor, más concentrada queda en el centro y más sólidos se mantienen los extremos.";
    static final String RANGE_FADE = "El color se desvanece desde el inicio hasta esta posición: 100 % lo desvanece por completo a lo largo de todo el recorrido (valor original); cuanto menor sea el valor, más corta será la transición y más concentrado quedará el color al principio.";
    /** The same for a fade measured in dp (a glow, an edge): the distance over which it goes from full to nothing. */
    static final String RANGE_DISTANCE = "Distancia desde el inicio hasta que el efecto se desvanece por completo: cuanto mayor sea, más larga y suave será la transición; cuanto menor, más concentrada quedará al principio.";

    public static final class Group {
        public final String id, key, title, description, category, masterKey;
        /** Several independent opt-ins share a page without a page-wide switch. */
        public final boolean independentSwitches;
        public final List<Item> items;
        private Group(Builder builder) {
            id = key = builder.id;
            title = builder.title; description = builder.description;
            category = builder.category; masterKey = builder.master;
            independentSwitches = builder.independentSwitches;
            items = Collections.unmodifiableList(new ArrayList<>(builder.items));
        }
    }

    /** A short, flat list of destinations belonging to the same screen or task. */
    public static final class NavigationSection {
        public final String title;
        public final List<Group> groups;
        private NavigationSection(String title, String... ids) {
            this.title = title;
            List<Group> result = new ArrayList<>();
            for (String id : ids) result.add(group(id));
            groups = Collections.unmodifiableList(result);
        }
    }

    /** Scene tabs only partition long forms; they never create or change setting keys. */
    public static final class DetailPage {
        public final String id, title;
        public final List<Item> items;
        private DetailPage(String id, String title, List<Item> items) {
            this.id = id; this.title = title;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }
    }

    public static final class Item {
        public final String key, title, description, type, section, unit, groupId;
        /** Suggested slider interval; persisted values are not clipped to these suggestions. */
        public final float min, max, step;
        /** Optional presets for string fields, or the complete values for an options field. */
        public final String[] values, labels;
        /** For colors, the matching other theme, theme name, and separately persisted alpha mode. */
        public final String pairedKey, scene, alphaKey;
        public final boolean master, allowsCustomValue;
        public final Object defaultValue;
        private Item(Builder group, String key, String title, String description, String type,
                String section, String unit, float min, float max, float step,
                String[] values, String[] labels, String pairedKey, String scene) {
            this.key = key; this.title = title; this.description = description; this.type = type;
            this.section = organizedSection(group.id, key, section); this.unit = unit; this.min = min; this.max = max; this.step = step;
            this.values = values == null ? new String[0] : values.clone();
            this.labels = labels == null ? new String[0] : labels.clone();
            if (this.values.length != this.labels.length) throw new IllegalArgumentException("Preset labels mismatch: " + key);
            this.pairedKey = pairedKey == null ? "" : pairedKey;
            this.scene = scene == null ? "" : scene;
            alphaKey = COLOR.equals(type) ? StatusBarSettings.alphaKey(key) : "";
            groupId = group.id; master = !group.independentSwitches && key.equals(group.master);
            allowsCustomValue = STRING.equals(type) || NUMERIC.equals(type);
            defaultValue = SettingsCatalog.defaultValue(key);
        }
    }

    /** What a time-class field means, in one line. Landscape copies share the portrait wording. */
    static String fieldNote(String key) {
        String k = NotificationBigClockSettings.portraitKey(key);
        String big = "notification_big_clock_";
        if (k.startsWith(big)) {
            switch (k.substring(big.length())) {
                case "max_size": return "Altura del texto de la hora cuando está expandida; con la fuente nativa se reduce al deslizar hacia arriba.";
                case "compact_max_size": return "Altura del texto de la hora después de contraerlo al deslizar hacia arriba.";
                case "screen_padding": return "Margen a cada lado cuando la fuente nativa ocupa todo el ancho de la pantalla.";
                case "letter_spacing": return "Espaciado de los caracteres de la hora: los valores positivos separan y los negativos juntan.";
                case "alignment": return "Alineación de la hora en la pantalla.";
                case "glass_border_enabled": return "Añade un borde fino con aspecto de vidrio a los números, la fecha y el texto adicional.";
                case "glass_border_width": return "Grosor del borde de vidrio.";
                case "entry_travel": return "Distancia que recorren la hora y la fecha desde arriba al desplegar el panel.";
                case "entry_blur_radius": return "Desenfoque al aparecer; se vuelve más nítido a medida que se despliega.";
                case "entry_fade_strength": return "Transparencia inicial; cuanto mayor sea el valor, más transparente se verá.";
                case "entry_completion": return "Progreso del despliegue a partir del cual la hora y la fecha se ven completamente nítidas.";
                case "date_enabled": return "Muestra la fecha en el área del reloj grande; en vertical puede intercambiarse de fila con los componentes laterales.";
                case "date_size": return "Tamaño de fuente de la fecha.";
                case "date_gap": return "Separación entre la fecha y la hora.";
                case "date_alignment": return "Alineación de la fecha en la pantalla.";
                case "notification_gap_enabled": return "Deja automáticamente espacio entre el reloj y la primera notificación.";
                case "notification_gap": return "Distancia entre la parte inferior de la hora o la fecha y la primera notificación.";
                case "notification_edge_safe_distance": return "Distancia que se conserva antes de empezar a desvanecer una notificación al deslizarla hacia el reloj.";
                case "notification_edge_range": return "Zona en la que las notificaciones se desvanecen gradualmente debajo del reloj.";
                case "notification_edge_blur_radius": return "Desenfoque de la zona de desvanecimiento.";
                case "footer_enabled": return "Muestra una línea de contenido personalizado al final de la lista de notificaciones.";
                case "footer_size": return "Tamaño de fuente del contenido inferior.";
                case "footer_margin": return "Distancia de seguridad entre el contenido inferior y el borde de la pantalla.";
                case "footer_alignment": return "Alineación del contenido inferior.";
                default: return "";
            }
        }
        boolean clockLike = k.startsWith("clock_") || k.startsWith("shade_clock_") || k.startsWith("classic_clock_");
        if (clockLike || k.startsWith("carrier_")) {
            if (k.endsWith("_digit_spacing")) return "Ancho adicional de cada celda numérica: los valores positivos separan y los negativos juntan.";
            if (k.endsWith("_dual_upper_scale") || k.endsWith("_dual_lower_scale")) return "Tamaño de fuente de esta fila, como porcentaje del reloj nativo.";
            if (k.endsWith("_dual_line_gap")) return "Separación adicional entre las dos filas; puede ser negativa.";
            if (k.endsWith("_margin_start")) return "Espacio adicional a la izquierda del reloj; los iconos adyacentes se desplazan para dejar sitio.";
            if (k.endsWith("_margin_end")) return "Espacio adicional a la derecha del reloj; los iconos adyacentes se desplazan para dejar sitio.";
            if (k.endsWith("_spacing")) return "Espaciado entre caracteres: los valores positivos separan y los negativos juntan.";
            if (k.endsWith("_scale")) return "Tamaño del texto respecto al original del sistema; 100 conserva el tamaño original.";
        }
        return "";
    }

    private static final class Builder {
        final String id, title, description, category, master;
        boolean independentSwitches;
        final List<Item> items = new ArrayList<>();
        Builder(String id, String title, String description, String category, String master) {
            this(id, title, description, category, master, false, "Activar" + title, description);
        }
        /** labelled: the page's own switch is a named, searchable row instead of the unlabelled top-bar switch. */
        Builder(String id, String title, String description, String category, String master,
                boolean labelled, String masterTitle, String masterDescription) {
            this.id = id; this.title = title; this.description = description;
            this.category = category; this.master = master; independentSwitches = labelled;
            if (master != null) toggle(master, masterTitle, masterDescription, "Interruptor de la función");
        }
        void toggle(String key, String title, String description, String section) {
            add(key, title, description.isEmpty() ? fieldNote(key) : description, BOOLEAN, section, "", 0, 1, 1, null, null, null, null);
        }
        void number(String key, String title, String section, String unit, float min, float max, float step) {
            String description = key.contains("offset_") ? "Respecto a la posición predeterminada del sistema" : key.endsWith("_weight") || key.equals(StatusBarSettings.FONT_WEIGHT) ? "100 es el grosor más fino; 900, el más grueso." : fieldNote(key);
            add(key, title, description, NUMERIC, section, unit, min, max, step, null, null, null, null);
        }
        void choice(String key, String title, String description, String section, String[] values, String[] labels) {
            add(key, title, description.isEmpty() ? fieldNote(key) : description, OPTIONS, section, "", 0, 0, 0, values, labels, null, null);
        }
        void text(String key, String title, String description, String section, String[] values, String[] labels) {
            add(key, title, description, STRING, section, "", 0, 0, 0, values, labels, null, null);
        }
        void colorPair(String light, String dark, String title, String section) {
            add(light, title + " · Fondo claro", "Se usa sobre fondos claros; el color y la transparencia se pueden configurar por separado.", COLOR, section, "", 0, 0, 0, null, null, dark, "light");
            add(dark, title + " · Fondo oscuro", "Se usa sobre fondos oscuros; el color y la transparencia se pueden configurar por separado.", COLOR, section, "", 0, 0, 0, null, null, light, "dark");
        }
        void add(String key, String title, String description, String type, String section, String unit,
                float min, float max, float step, String[] values, String[] labels, String pairedKey, String scene) {
            items.add(new Item(this, key, title, description, type, section, unit, min, max, step, values, labels, pairedKey, scene));
        }
    }

    static {
        Set<String> hidden = new LinkedHashSet<>(Arrays.asList(
                StatusBarSettings.DATA_ACTIVITY_HIDDEN, StatusBarSettings.SAFE_MODE,
                ClockBackdropSettings.NOTIFICATION_FOLLOW, ClockBackdropSettings.NOTIFICATION_RELIEF,
                StatusBarSettings.DIAGNOSTICS_ENABLED, StatusBarSettings.FONT_REVISION, NotificationIconArea.IMAGE_REVISION,
                ResidentCache.INSTALLED, ResidentCache.RUNNING,
                StatusBarSettings.FONT_NAME, FontScopes.KEY,
                IconPackRepository.MASTER, IconPackRepository.LAYERS, IconPackAssignments.ASSIGNMENTS,
                NotificationBigClockSettings.STATUS_ICONS_FIXED, NotificationBigClockSettings.COMPACT_WEIGHT,
                NotificationBigClockSettings.STACK_GAP, NotificationBigClockSettings.STACK_INSET,
                StatusBarSettings.TILES_FADE_RANGE,
                "carrier_position_enabled", "carrier_size_enabled", "carrier_color_enabled",
                "carrier_text_style_enabled", "carrier_replace_enabled",
                StatusBarSettings.CARRIER_MODE, StatusBarSettings.CARRIER_PATTERN, StatusBarSettings.CARRIER_TEXT,
                StatusBarSettings.CARRIER_OFFSET_X, StatusBarSettings.CARRIER_OFFSET_Y,
                StatusBarSettings.CARRIER_SCALE, StatusBarSettings.CARRIER_WEIGHT, StatusBarSettings.CARRIER_SPACING,
                "carrier_color_light", "carrier_color_dark"));
        hidden.add(StatusBarShadeIconSettings.MASTER);
        hidden.add(NativeClockMeasurement.SCALE_BASIS_VERSION);
        hidden.add(NativeClockMeasurement.LEGACY_SCALE_FACTOR);
        hidden.add(NotificationBigClockSettings.INTERACTIVE_STACK);
        hidden.add(NotificationGroupStack.MASTER);
        hidden.add(RadioState.WIFI_MERGED);
        hidden.add(RadioState.ALWAYS_SHOWN);
        hidden.add(RadioState.BOTH_VISIBLE);
        hidden.add(FluidCloudStyle.FOLLOW_FOREGROUND);
        // Retired (code100): the notification-shade scene of the fluid-cloud style; always off.
        hidden.add(FluidCloudStyle.SCOPE_SHADE);
        // Retired: highlight removal belongs to the C17 feature; the media accent is no longer lock-screen-only.
        hidden.add(FluidCloudStyle.DESKTOP_HIGHLIGHTS);
        hidden.add(FluidCloudAccent.LOCKSCREEN_ENABLED);
        hidden.add(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED);
        hidden.add("data_single_enabled");
        hidden.addAll(Arrays.asList(NativeNetworkBadgeControls.X,NativeNetworkBadgeControls.Y,
                NativeNetworkBadgeControls.SCALE,NativeNetworkBadgeControls.WEIGHT,NativeNetworkBadgeControls.FONT));
        hidden.add(NetworkSpeedControls.INTERVAL_SECONDS);
        hidden.add(NetworkSpeedControls.STYLE_ENABLED);
        hidden.add(NetworkSpeedControls.DISPLAY_STYLE);
        // The code160 area de-dimming was withdrawn in code161 (the see-through belongs to the digits only).
        for (String key : new String[]{NotificationBigClockSettings.REVEAL_ENABLED, NotificationBigClockSettings.REVEAL_AMOUNT,
                NotificationBigClockSettings.REVEAL_FEATHER}) { hidden.add(key); hidden.add(NotificationBigClockSettings.landscapeKey(key)); }
        // code169 replaces background sampling with a colour mask; the old reveal strength stays portable only.
        hidden.add(NotificationBigClockSettings.DIGIT_REVEAL_AMOUNT);
        hidden.add(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_AMOUNT));
        // code169 text components replace the AppWidget editor; historical provider/height data remains portable.
        hidden.addAll(Arrays.asList(BigClockWidgetsSettings.LEFT_PROVIDER,BigClockWidgetsSettings.RIGHT_PROVIDER,
                BigClockWidgetsSettings.LEFT_HEIGHT,BigClockWidgetsSettings.RIGHT_HEIGHT));
        // The page-turn effect was withdrawn in code161: its saved values stay importable, never shown.
        hidden.add("tiles_enabled");
        for (Item item : tiles().items) hidden.add(item.key);
        hidden.add(NotificationBigClockSettings.SCALE);
        hidden.add(NotificationBigClockSettings.COMPACT_SCALE);
        for (String key : new String[]{NotificationBigClockSettings.COMPACT_SCALE,
                NotificationBigClockSettings.COMPACT_MAX_SIZE, NotificationBigClockSettings.COMPACT_OFFSET_X,
                NotificationBigClockSettings.COMPACT_OFFSET_Y}) hidden.add(NotificationBigClockSettings.landscapeKey(key));
        hidden.add(NotificationBigClockSettings.TAIL_WIDTH_1);
        hidden.add(NotificationBigClockSettings.TAIL_WIDTH_2);
        hidden.add(NotificationBigClockSettings.TAIL_WIDTH_3);
        for (String key : new String[]{NotificationBigClockSettings.COMPACT_WEIGHT,
                NotificationBigClockSettings.STACK_ENABLED, NotificationBigClockSettings.VISIBLE_COUNT, NotificationBigClockSettings.TAIL_WIDTH_1,
                NotificationBigClockSettings.TAIL_WIDTH_2, NotificationBigClockSettings.TAIL_WIDTH_3}) {
            String landscape = NotificationBigClockSettings.landscapeKey(key);
            if (StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(landscape)
                    || StatusBarSettings.NUMERIC_DEFAULTS.containsKey(landscape)) hidden.add(landscape);
        }
        // Newer network settings remain saved for compatibility; 1.8.2 does not expose or use them (the per-generation texts are back).
        hidden.addAll(Arrays.asList(NetworkSlotGeometry.POSITION, NetworkSlotGeometry.OFFSET_X,
                NetworkSlotGeometry.OFFSET_Y, NetworkSlotGeometry.MARGIN_LEFT, NetworkSlotGeometry.MARGIN_RIGHT));
        hidden.add("data_roaming_hidden");
        // Drawing offsets remain portable legacy data; the layout editor uses measured gaps instead.
        hidden.addAll(Arrays.asList(StatusBarSettings.CLOCK_OFFSET_X, ClockLayoutSettings.DUAL_OFFSET_X,
                StatusBarSettings.SPEED_OFFSET_X, StatusBarSettings.DATA_OFFSET_X, StatusBarSettings.WIFI_OFFSET_X,
                StatusBarSettings.LABEL_OFFSET_X, StatusBarSettings.BATTERY_OFFSET_X, NotificationIconArea.X,
                NativeStatusIcons.X, SpeedPosition.POSITION, SpeedPosition.SAFE_GAP_ENABLED, SpeedPosition.SAFE_GAP,
                SpeedPosition.EDGE_RESERVE, ClockLayoutSettings.MARGIN_ENABLED, ClockLayoutSettings.MARGIN_START,
                ClockLayoutSettings.MARGIN_END, NetworkIconOrder.SWAP));
        // Retired code175 parents keep their stored protocol keys, with no editor or effect ownership.
        hidden.addAll(Arrays.asList("statusbar_layout_adjustments_enabled", "notification_stack_effects_enabled",
                "notification_clear_controls_enabled", "lock_notification_effects_enabled"));
        HIDDEN = Collections.unmodifiableSet(hidden);
        List<Group> groups = new ArrayList<>();
        groups.add(statusBarLayout());
        groups.add(speed()); groups.add(data()); groups.add(wifi()); groups.add(label()); groups.add(nativeNetworkBadge());
        groups.add(clock("clock", STATUSBAR)); groups.add(carrier()); groups.add(font()); groups.add(battery()); groups.add(notificationIcons());
        groups.add(nativeStatusIcons());
        groups.add(notificationOverrides()); groups.add(iconLibrary());
        groups.add(qs()); groups.add(tileFullFill()); groups.add(tileCorners()); groups.add(tileIconSize()); groups.add(qsEntrances());
        groups.add(clock("shade_clock", NOTIFICATION));
        groups.add(bigClock()); groups.add(landscapeClock()); groups.add(wholeStack()); groups.add(miniStack()); groups.add(clear()); groups.add(landscapeClear()); groups.add(media());
        groups.add(highlightRemoval());
        groups.add(classicText()); groups.add(shadeWallpaperPage(false)); groups.add(shadeWallpaperPage(true));
        groups.add(lockscreenDate()); groups.add(lockscreenLock()); groups.add(lockscreenBlur()); groups.add(fluidAccent()); groups.add(fluidStyle()); groups.add(fluidCapsule());
        groups.add(lockAwake()); groups.add(lockAod());
        Map<String, Group> byId = new LinkedHashMap<>();
        Map<String, Item> byKey = new LinkedHashMap<>();
        for (Group group : groups) {
            if (byId.put(group.id, group) != null) throw new IllegalStateException("Duplicate settings group: " + group.id);
            for (Item item : group.items) {
                if (hidden.contains(item.key)) throw new IllegalStateException("Hidden setting exposed: " + item.key);
                if (byKey.put(item.key, item) != null) throw new IllegalStateException("Duplicate settings item: " + item.key);
            }
        }
        GROUPS = Collections.unmodifiableList(groups);
        GROUP_BY_ID = Collections.unmodifiableMap(byId);
        ITEM_BY_KEY = Collections.unmodifiableMap(byKey);
    }

    private static Builder builder(String id, String title, String description, String category, String master) {
        return new Builder(id, title, description, category, master);
    }
    private static String organizedSection(String groupId, String key, String section) {
        // The camera-area capsule names its own sections; the key-suffix rules below pulled "调整音乐封面大小" out into a lone "大小" section.
        if ("fluid_cloud_capsule".equals(groupId)) return section;
        if (StatusBarLayoutSettings.GROUP.equals(groupId)) return section;
        if ("battery".equals(groupId) && (BatteryTextStyle.MASTER.equals(key) || BatteryTextStyle.NUMBERS.containsKey(key))) return "Texto dentro de la batería";
        String theme = key.contains("_light_") ? "Interfaz clara · " : key.contains("_dark_") ? "Interfaz oscura · " : "";
        if (!theme.isEmpty() && "qs_appearance".equals(groupId)) return theme + (key.contains("gradient") ? "Degradado" : "Color de fondo");
        if (!theme.isEmpty() && "qs_media".equals(groupId)) return theme + (key.contains("background") ? "Fondo" : "Resplandor exterior");
        if (groupId.startsWith("notification_big_clock")) {
            key = NotificationBigClockSettings.portraitKey(key);
            if (ClockBackdropSettings.DATE_MASK_OPACITY.equals(key)) return "Estilo del fondo de la fecha";
            if (ClockBackdropSettings.FOOTER_MASK_OPACITY.equals(key)) return "Estilo del fondo del contenido inferior";
            if (ClockBackdropSettings.EDGE_ENABLED.equals(key) || ClockBackdropSettings.EDGE_WIDTH.equals(key)
                    || ClockBackdropSettings.EDGE_COLOR.equals(key) || ClockBackdropSettings.EDGE_OPACITY.equals(key)
                    || ClockBackdropSettings.HIGHLIGHT_ANGLE.equals(key) || ClockBackdropSettings.HIGHLIGHT_STRENGTH.equals(key)
                    || ClockBackdropSettings.SHADE_STRENGTH.equals(key) || ClockBackdropSettings.DEPTH_WIDTH.equals(key)) return "Borde con relieve de la hora principal";
            if (NotificationBigClockSettings.COMPACT_MAX_SIZE.equals(key)) return "Estilo contraído";
            if (NotificationBigClockSettings.COMPACT_OFFSET_X.equals(key) || NotificationBigClockSettings.COMPACT_OFFSET_Y.equals(key)) return "Posición contraída";
            if (NotificationBigClockSettings.DATE_OFFSET_X.equals(key) || NotificationBigClockSettings.DATE_OFFSET_Y.equals(key)
                    || NotificationBigClockSettings.DATE_GAP.equals(key) || NotificationBigClockSettings.DATE_ALIGNMENT.equals(key)) return "Posición de la fecha";
            if (NotificationBigClockSettings.FOOTER_OFFSET_X.equals(key) || NotificationBigClockSettings.FOOTER_OFFSET_Y.equals(key)
                    || NotificationBigClockSettings.FOOTER_MARGIN.equals(key) || NotificationBigClockSettings.FOOTER_ALIGNMENT.equals(key)) return "Posición inferior";
            // 时间页：内容与字体 → 大小与字重 → 位置与对齐 → 点击操作。
            if (NotificationBigClockSettings.PATTERN.equals(key) || NotificationBigClockSettings.FONT.equals(key)) return "Contenido y fuente";
            if (NotificationBigClockSettings.MAX_SIZE.equals(key) || NotificationBigClockSettings.COMPACT_MAX_SIZE.equals(key)
                    || NotificationBigClockSettings.WEIGHT.equals(key) || NotificationBigClockSettings.LETTER_SPACING.equals(key)
                    || NotificationBigClockSettings.PROPORTIONAL_DIGITS.equals(key)) return "Tamaño y grosor";
            if (NotificationBigClockSettings.ALIGNMENT.equals(key) || NotificationBigClockSettings.SCREEN_PADDING.equals(key)
                    || NotificationBigClockSettings.OFFSET_X.equals(key) || NotificationBigClockSettings.OFFSET_Y.equals(key)
                    || NotificationBigClockSettings.COMPACT_OFFSET_X.equals(key) || NotificationBigClockSettings.COMPACT_OFFSET_Y.equals(key)) return "Posición y alineación";
        }
        String scene = section.contains(" · ") ? section.substring(0, section.indexOf(" · ")) + " · " : "";
        if (Arrays.asList("clock", "shade_clock", "carrier", "classic_text").contains(groupId)) {
            if (key.endsWith("_size_enabled") || key.endsWith("_scale")) return scene + "Tamaño";
            // Numeric digit alignment remains a common setting, separate from typographic fine tuning.
            if (!key.endsWith("_digit_spacing") && (key.endsWith("_weight") || key.endsWith("_spacing")
                    || key.endsWith("_text_style_enabled"))) return scene + "Estilo del texto";
        }
        if (key.endsWith("_position_enabled")) return scene + "Posición";
        if (key.endsWith("_size_enabled")) return scene + "Tamaño";
        if (key.endsWith("_color_enabled") && (section.endsWith("Interruptor independiente") || "Interruptor independiente".equals(section))) return scene + "Color y transparencia";
        if (key.endsWith("_text_style_enabled")) return scene + ("speed".equals(groupId) || "label".equals(groupId) ? "Texto y espaciado" : "Estilo del texto");
        if ("speed_lines_enabled".equals(key)) return "Texto y espaciado";
        if ("Interruptor independiente".equals(section)) return "Estilo de visualización";
        return section;
    }
    private static void controls(Builder b, String prefix, String section, boolean text) {
        boolean outer = outerPosition(prefix);
        b.toggle(prefix + "_position_enabled", outer ? "Ajustar posición vertical" : "Ajustar posición",
                outer ? "Ajusta la posición vertical; el espaciado horizontal se configura por separado en esta página." : "Aplica las posiciones horizontal y vertical configuradas.", section);
        b.toggle(prefix + "_size_enabled", "Ajustar tamaño", "Aplica la escala configurada.", section);
        b.toggle(prefix + "_color_enabled", "Ajustar color", "Permite configurar por separado los fondos claros y oscuros.", section);
        if (text) b.toggle(prefix + "_text_style_enabled", "Grosor y espaciado del texto", "Aplica el estilo de texto configurado.", section);
    }
    private static void position(Builder b, String prefix, String section) {
        if (!outerPosition(prefix)) b.number(prefix + "_offset_x", "Posición horizontal", section, "dp", -80, 80, .01f);
        b.number(prefix + "_offset_y", "Posición vertical", section, "dp", -24, 24, .01f);
    }
    private static boolean outerPosition(String prefix) {
        return Arrays.asList("clock", "speed", "data", "wifi", "label", "battery").contains(prefix);
    }
    private static Group statusBarLayout() {
        Builder b = builder(StatusBarLayoutSettings.GROUP, "Orden de la barra de estado (experimental)",
                "Elige la zona y el orden de cada elemento. Si se desactiva, se usa la zona y el orden del sistema; el margen lateral de cada página sigue aplicándose por separado. Los elementos ocultos no se mostrarán a la fuerza.",
                STATUSBAR, StatusBarLayoutSettings.MASTER);
        for (String token : StatusBarLayoutSettings.TOKENS) {
            String name = StatusBarLayoutSettings.label(token);
            b.choice(StatusBarLayoutSettings.key(token, "side"), "Zona",
                    StatusBarLayoutSettings.CARRIER.equals(token) ? "El operador solo aparece en la parte superior del panel desplegado y en la pantalla de bloqueo; no aparece en la barra de estado normal. No funciona si el sistema o el módulo lo ocultan y no crea texto nuevo."
                            : "Zona izquierda, derecha o la definida por el sistema; sigue al contenedor actual.", name,
                    new String[]{StatusBarLayoutSettings.NATIVE, StatusBarLayoutSettings.LEFT, StatusBarLayoutSettings.RIGHT},
                    new String[]{"Seguir al sistema", "Izquierda", "Derecha"});
            b.add(StatusBarLayoutSettings.key(token, "order"), "Orden", "0 sigue al sistema; desde 1 se ordena físicamente de izquierda a derecha. Los elementos con el mismo número conservan su orden nativo. También puedes organizarlos desde la lista de orden.",
                    NUMERIC, name, "", 0, 16, 1, null, null, null, null);
        }
        String label = "Tipo de red · seguir a la red móvil";
        b.add(StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order"), "Orden del tipo de red y la señal",
                "Sigue la zona de la red móvil; 0 sigue al sistema, 1 delante de la señal y 2 detrás. La visibilidad de Wi-Fi y la ocultación del tipo de red siguen dependiendo del sistema y de los ajustes existentes.",
                NUMERIC, label, "", 0, 2, 1, null, null, null, null);
        b.text(NativeStatusIcons.LAYOUT,"Ordenar los iconos de estado del sistema individualmente","Elige en qué lado aparecen los iconos de Bluetooth, ubicación, alarma y otros avisos, y ajusta el orden dentro de cada lado. La posición del grupo sigue anclada al sistema.","Iconos de aviso del sistema",null,null);
        return new Group(b);
    }
    private static void layoutMargins(Builder b, String token, String section) {
        String scope = StatusBarLayoutSettings.LABEL.equals(token) ? "El tipo de red sigue a la red móvil." : "";
        b.add(StatusBarLayoutSettings.key(token, "margin_left"), "Margen izquierdo", scope + "Ajusta el espacio izquierdo respecto al límite nativo: un valor positivo lo aumenta, uno negativo lo reduce y 0 conserva el margen original. Si el elemento está a la derecha y es el más cercano al centro (por ejemplo, la velocidad de red), este margen lo mueve directamente a la derecha o a la izquierda. Se aplica de forma independiente, sin activar el orden de la barra de estado.",
                NUMERIC, section, "dp", -40, 40, .1f, null, null, null, null);
        b.add(StatusBarLayoutSettings.key(token, "margin_right"), "Margen derecho", scope + "Ajusta el espacio derecho respecto al límite nativo: un valor positivo lo aumenta, uno negativo lo reduce y 0 conserva el margen original. Si el elemento está a la izquierda y es el más cercano al centro, este margen lo mueve directamente a la izquierda o a la derecha. Se aplica de forma independiente, sin activar el orden de la barra de estado.",
                NUMERIC, section, "dp", -40, 40, .1f, null, null, null, null);
    }
    private static void scale(Builder b, String key, String title, String section) {
        b.number(key, title, section, "%", 25, 250, .01f);
    }
    private static void textSize(Builder b, String prefix, String section) {
        scale(b, prefix + "_scale", "Tamaño del texto", section);
        b.number(prefix + "_weight", "Grosor del texto", section, "", 100, 900, 1);
        b.number(prefix + "_spacing", "Ajuste del espaciado", section, "dp", -2, 8, .01f);
    }
    private static void normalColors(Builder b, String prefix, String title) {
        b.colorPair(prefix + "_color_light", prefix + "_color_dark", title, "Color y transparencia");
    }
    private static void alignment(Builder b, String key, String title, String section) {
        b.choice(key, title, "", section, new String[]{"left", "center", "right"}, new String[]{"Alinear a la izquierda", "Centrar", "Alinear a la derecha"});
    }
    private static Group speed() {
        Builder b = builder("speed", "Velocidad de red en tiempo real", "Conserva el valor en tiempo real del sistema y la unidad KB/s.", STATUSBAR, "speed_enabled");
        controls(b, "speed", "Interruptor independiente", true);
        b.toggle(NetworkSpeedControls.INTERVAL_ENABLED, "Ajustar intervalo de actualización", "Si está desactivado, sigue al sistema; un intervalo más corto actualiza más rápido.", "Velocidad de actualización");
        b.add(NetworkSpeedControls.INTERVAL_MILLIS, "Intervalo de actualización", "La unidad son milisegundos; se recomiendan 1–500 ms. Cuanto menor sea el intervalo, más frecuentes serán el muestreo y las actualizaciones de la interfaz. Ajústalo según tus necesidades.", NUMERIC,
                "Velocidad de actualización", "ms", 1f, 500f, 1f, null, null, null, null);
        b.toggle("speed_lines_enabled", "Espacio entre el número y la unidad", "Ajusta por separado el espacio entre el número y la unidad.", "Interruptor independiente");
        position(b, "speed", "Posición");
        layoutMargins(b, StatusBarLayoutSettings.SPEED, "Espaciado lateral");
        scale(b, StatusBarSettings.SPEED_SCALE, "Tamaño general", "Tamaño");
        scale(b, StatusBarSettings.SPEED_NUMBER_SCALE, "Tamaño de los números", "Tamaño");
        scale(b, StatusBarSettings.SPEED_UNIT_SCALE, "Tamaño de la unidad", "Tamaño");
        b.number(StatusBarSettings.SPEED_LINE_GAP, "Espacio entre el número y la unidad", "Texto y espaciado", "dp", -8, 8, .01f);
        b.number(StatusBarSettings.SPEED_WEIGHT, "Grosor del texto", "Texto y espaciado", "", 100, 900, 1);
        normalColors(b, "speed", "Texto de velocidad de red");
        return new Group(b);
    }
    private static Group data() {
        Builder b = builder("data", "Señal móvil", "Las barras reflejan la señal real del sistema. La flecha de actividad de datos está oculta de forma predeterminada; puedes activarla y ajustarla por separado en los ajustes de los indicadores nativos.", STATUSBAR, "data_enabled");
        controls(b, "data", "Interruptor independiente", false);
        b.toggle("data_icon_enabled", "Estilo de señal similar a iOS", "Conservar la intensidad real de la señal", "Interruptor independiente");
        b.toggle("data_badge_hidden", "Ocultar los indicadores de red del sistema", "Reducir los indicadores de red duplicados", "Interruptor independiente");
        b.toggle(DataBatterySpacing.ENABLED, "Espacio sin indicador de red", "Se aplica cuando el sistema confirma que no se muestran ni el icono Wi-Fi ni el texto del tipo de red, y la señal móvil queda junto a la batería. Si el estado es desconocido, se conserva el espaciado nativo.", "Espacio junto al borde de la batería");
        b.add(DataBatterySpacing.SPACING, "Ajuste del espacio entre la red móvil y la batería", "Aumenta o reduce el espacio respecto al límite nativo; 0 conserva el original. Solo cambia el espacio entre la señal móvil y la batería, sin alterar los demás iconos.", NUMERIC,
                "Espacio junto al borde de la batería", "dp", -12, 24, .01f, null, null, null, null);
        b.choice(StatusBarSettings.SIGNAL_LAYOUT, "Distribución de la señal", "La señal secundaria de la segunda SIM sigue siendo administrada por el sistema.", "Distribución de la señal", new String[]{"system", "single"}, new String[]{"Filas de señal según el sistema", "Mostrar solo la señal principal en una fila"});
        position(b, "data", "Posición"); layoutMargins(b, StatusBarLayoutSettings.MOBILE, "Espaciado lateral");
        scale(b, StatusBarSettings.DATA_ICON_SCALE, "Tamaño del icono", "Tamaño");
        normalColors(b, "data", "Señal móvil");
        return new Group(b);
    }
    private static Group nativeNetworkBadge() {
        Builder b=builder("native_network_badge", "Ajustes de indicadores nativos",
                "Los indicadores 1 y 2 permiten configurar por separado la posición, el tamaño, la fuente, el grosor y la ocultación. En el diseño de señal independiente corresponden a la SIM principal y a la secundaria; en el diseño de dos filas se conservan los segmentos anterior y posterior nativos. Si solo se muestra una fila, el indicador de la SIM secundaria se oculta automáticamente. Primero debes desactivar el texto del tipo de red. La flecha de datos se configura por separado y está oculta de forma predeterminada.",
                STATUSBAR,NativeNetworkBadgeControls.MASTER);
        for(int part=1;part<=2;part++) {
            String p=NativeNetworkBadgeSettings.group(part),s="Indicador"+part;
            b.toggle(p+"_enabled","Ajustar"+s,part==1?"Indicador de la SIM principal; en el diseño de dos filas corresponde al texto anterior.":"Indicador de la SIM secundaria; en el diseño de dos filas corresponde al texto posterior.",s+" · Interruptor");
            b.toggle(p+"_hidden","Ocultar"+s,"Oculta solo este segmento de texto; el otro conserva su configuración.",s+" · Interruptor");
            b.number(p+"_offset_x","Posición horizontal",s+" · Posición","dp",-32,32,.01f);
            b.number(p+"_offset_y","Posición vertical",s+" · Posición","dp",-16,16,.01f);
            scale(b,p+"_scale","Tamaño del indicador",s+" · Tamaño");
            b.number(p+"_weight","Grosor de fuente",s+" · Texto","",100,900,1);
            b.choice(p+"_font","Fuente del indicador","Importa fuentes personalizadas en «Otros → Configuración de fuentes».",s+" · Texto",
                    new String[]{"native","global","system","pingfang","custom"},new String[]{"Fuente nativa del indicador","Usar la fuente del texto","Fuente del sistema","PingFang","Fuente personalizada"});
        }
        b.toggle(NativeDataActivity.MASTER,"Mostrar la flecha de actividad de datos","Al activarla, vuelve a mostrar la indicación nativa de actividad de subida y descarga móvil, independientemente del interruptor principal de los indicadores de esta página.","Flecha de datos");
        b.number(NativeDataActivity.X,"Posición horizontal de la flecha","Flecha de datos","dp",-32,32,.01f);
        b.number(NativeDataActivity.Y,"Posición vertical de la flecha","Flecha de datos","dp",-16,16,.01f);
        scale(b,NativeDataActivity.SCALE,"Tamaño de la flecha","Flecha de datos");
        b.toggle(NativeDataActivity.COLOR_ENABLED,"Color personalizado de la flecha","Si está desactivado, se usa el color nativo, que cambia según el fondo del sistema.","Flecha de datos");
        b.colorPair(NativeDataActivity.COLOR_LIGHT,NativeDataActivity.COLOR_DARK,"Color de la flecha","Flecha de datos");
        return new Group(b);
    }

    private static Group wifi() {
        Builder b = builder("wifi", "Icono de Wi-Fi", "Configura por separado la posición, el tamaño y el color; la transparencia sigue al estado nativo.", STATUSBAR, "wifi_enabled");
        controls(b, "wifi", "Interruptor independiente", false);
        b.toggle("wifi_icon_enabled", "Estilo de Wi-Fi similar a iOS", "Conservar la intensidad real de la señal Wi-Fi", "Interruptor independiente");
        b.toggle("wifi_activity_hidden", "Ocultar las flechas de subida y descarga de Wi-Fi", "Conservar el icono Wi-Fi y el estado de conexión", "Interruptor independiente");
        b.toggle("wifi_badge_hidden", "Ocultar el indicador de Wi-Fi", "Reducir los indicadores de red duplicados", "Interruptor independiente");
        b.toggle(StatusBarLayoutSettings.WIFI_LABEL_SHARED, "Compartir posición entre Wi-Fi y el tipo de red",
                "Wi-Fi y el tipo de red usan la misma posición relativa, orden y márgenes laterales.", "Posición compartida");
        position(b, "wifi", "Posición"); layoutMargins(b, StatusBarLayoutSettings.WIFI, "Espaciado lateral");
        scale(b, StatusBarSettings.WIFI_ICON_SCALE, "Tamaño del icono", "Tamaño");
        normalColors(b, "wifi", "Icono de Wi-Fi");
        return new Group(b);
    }
    private static Group label() {
        Builder b = builder("label", "Texto del tipo de red", "Sigue a la SIM principal. Elige entre «Uniforme» y «Personalizado»: el modo uniforme muestra 5G para las variantes de 5G y 4G para las variantes de 4G; el personalizado muestra el texto que escribas para cada tipo. Se oculta cuando hay Wi-Fi conectado y también cuando tanto el Wi-Fi como los datos móviles están desactivados.", STATUSBAR, "label_enabled");
        controls(b, "label", "Interruptor independiente", true);
        b.toggle(StatusBarSettings.LABEL_HIDDEN, "Ocultar el texto del tipo de red", "Ocultar las etiquetas de texto como 5G y 4G", "Interruptor independiente");
        b.toggle("label_normalize_enabled", "Unificar el nombre del tipo de red", "5G, 5G+ y 5GA se muestran como 5G; 4G, 4G+ y 4.5G como 4G; H y H+ como 3G. Es incompatible con «Contenido personalizado del tipo de red»: al activar esta opción se desactiva la personalización.", "Interruptor independiente");
        b.toggle(NetworkLabelFormats.ENABLED, "Contenido personalizado del tipo de red", "Configura un texto o emoji para cada tipo de red; por ejemplo, mostrar «XX» para 5G y «5Ga» para 5GA. Déjalo vacío para ocultar ese tipo. Las etiquetas del operador que no se reconozcan conservan el texto original del sistema. Es incompatible con «Unificar el nombre del tipo de red»: al activarlo, se desactiva el modo uniforme.", "Contenido mostrado");
        for (NetworkLabel.Level level : NetworkLabel.Level.values())
            b.text(NetworkLabelFormats.key(level), level.text + " Contenido mostrado",
                    "Predeterminado " + level.text + "; solo sustituye el texto visible, no cambia la red real. Déjalo vacío para ocultar este tipo de red.",
                    level.family + " · Contenido personalizado", null, null);
        position(b, "label", "Posición"); layoutMargins(b, StatusBarLayoutSettings.LABEL, "Espaciado lateral");
        scale(b, StatusBarSettings.LABEL_SCALE, "Tamaño del texto", "Tamaño");
        b.number(StatusBarSettings.SLOT_WIDTH, "Ancho reservado", "Texto y espaciado", "dp", 0, 80, .01f);
        b.number(StatusBarSettings.FONT_WEIGHT, "Grosor del texto", "Texto y espaciado", "", 100, 900, 1);
        normalColors(b, "label", "Texto del tipo de red");
        return new Group(b);
    }
    private static Group clock(String id, String category) {
        boolean shade = "shade_clock".equals(id);
        Builder b = builder(id, shade ? "Reloj del panel de notificaciones y del centro de control" : "Hora de la barra de estado", shade ? "Configura el reloj del panel desplegado por separado; si se desactiva, sigue el formato horario y el estilo de texto de la barra de estado. El reloj grande de la página de notificaciones en vertical tiene prioridad." : "El reloj estándar y el reloj de dos filas son excluyentes; sus interruptores están en la parte superior de cada pestaña. El ancho uniforme de los números, su espaciado, la alineación de los dos puntos y los márgenes se ajustan en «Ajustes generales» y se comparten entre ambos modos.", category, id + "_controls_enabled");
        controls(b, id, "Interruptor independiente", true);
        b.toggle(id + "_enabled", "Formato de hora personalizado", "Admite horas, minutos, segundos, fecha, día de la semana y franjas horarias en chino.", "Formato de hora");
        b.text(id + "_pattern", "Formato de hora", "Contenido de la hora; puedes elegir un formato habitual o crear uno personalizado.", "Formato de hora", CLOCK_VALUES, CLOCK_LABELS);
        position(b, id, "Posición"); textSize(b, id, "Estilo del texto"); normalColors(b, id, "Texto de la hora");
        digitCells(b, id, "Alineación de los números");
        if (!shade) { layoutMargins(b, StatusBarLayoutSettings.CLOCK, "Espaciado lateral"); clockLayout(b); }
        return new Group(b);
    }
    /** Fixed-width digit cells stop a seconds display from moving as proportional digits change. */
    private static void digitCells(Builder b, String group, String section) {
        b.toggle(ClockLayoutSettings.digitKey(group), "Números de ancho uniforme (evita saltos al cambiar los segundos)",
                "Todos los números ocupan el mismo ancho, por lo que el texto no salta lateralmente cuando cambian los segundos. Solo funciona con formatos que incluyen segundos. Al activarlo, el espaciado se ajusta por separado abajo.", section);
        b.number(ClockLayoutSettings.digitSpacingKey(group), "Espaciado entre números", section, "dp", -2, 8, .01f);
        b.toggle(ClockLayoutSettings.colonKey(group), "Centrar los dos puntos",
                "Dibuja los dos puntos en el centro visual de los números para evitar que queden demasiado bajos. Se aplica a todos los dos puntos del formato, con o sin segundos, y no depende del ancho uniforme.", section);
    }
    /** The two-row clock is a separate mode of the status-bar time with its own master switch (shown on its own tab). */
    private static void clockLayout(Builder b) {
        b.toggle(ClockLayoutSettings.DUAL_ENABLED, "Reloj de dos filas",
                "Divide la hora en dos filas. Este modo es independiente del reloj estándar: no hace falta activar su interruptor; al habilitarlo, sustituye la hora de la barra de estado. El contenido, tamaño, posición y color se configuran aquí.", "Contenido");
        b.text(ClockLayoutSettings.DUAL_UPPER_PATTERN, "Contenido de la fila superior", "Contenido de la primera fila; puedes elegir un formato habitual o uno personalizado.", "Contenido", DUAL_VALUES, DUAL_LABELS);
        b.text(ClockLayoutSettings.DUAL_LOWER_PATTERN, "Contenido de la fila inferior", "Contenido de la segunda fila; usa las mismas reglas de formato que la superior.", "Contenido", DUAL_VALUES, DUAL_LABELS);
        b.number(ClockLayoutSettings.DUAL_UPPER_SCALE, "Tamaño de la fila superior", "Tamaño", "%", 25, 250, .01f);
        b.number(ClockLayoutSettings.DUAL_LOWER_SCALE, "Tamaño de la fila inferior", "Tamaño", "%", 25, 250, .01f);
        b.number(ClockLayoutSettings.DUAL_UPPER_WEIGHT, "Grosor de texto de la fila superior", "Estilo del texto", "", 100, 900, 1);
        b.number(ClockLayoutSettings.DUAL_LOWER_WEIGHT, "Grosor de texto de la fila inferior", "Estilo del texto", "", 100, 900, 1);
        b.number(ClockLayoutSettings.DUAL_SPACING, "Ajuste del espaciado", "Estilo del texto", "dp", -2, 8, .01f);
        b.number(ClockLayoutSettings.DUAL_OFFSET_Y, "Posición vertical", "Posición", "dp", -24, 24, .01f);
        b.number(ClockLayoutSettings.DUAL_LINE_GAP, "Separación entre filas", "Posición", "dp", -8, 16, .01f);
        b.choice(ClockLayoutSettings.DUAL_ALIGN, "Alineación de las dos filas", "Tomar como referencia la fila más ancha", "Posición",
                new String[]{ClockLayoutSettings.ALIGN_CENTER, ClockLayoutSettings.ALIGN_LEFT, ClockLayoutSettings.ALIGN_RIGHT},
                new String[]{"Centrar", "Alinear a la izquierda", "Alinear a la derecha"});
        b.toggle(ClockLayoutSettings.DUAL_COLOR_ENABLED, "Color personalizado", "Si está desactivado, las dos filas usan el color nativo que cambia según el fondo; al activarlo, puedes definir colores distintos para fondos claros y oscuros.", "Color y transparencia");
        b.colorPair(ClockLayoutSettings.DUAL_COLOR_LIGHT, ClockLayoutSettings.DUAL_COLOR_DARK, "Texto de la hora", "Color y transparencia");
    }
    private static Group notificationIcons() {
        Builder b = builder("notification_icons", "Área de iconos de notificación", "Ajusta los iconos de notificación de la izquierda de la barra de estado. Puedes usar los iconos nativos, un corazón, texto o una imagen.", STATUSBAR, NotificationIconArea.MASTER);
        b.choice(NotificationIconArea.MODE, "Modo de visualización", "Mostrar solo cuando haya notificaciones visibles", "Estilo de visualización",
                new String[]{"native", "heart", "text", "image"},
                new String[]{"Iconos de notificación nativos", "Un solo corazón", "Símbolo o texto personalizado", "Imagen personalizada"});
        b.toggle(NotificationIconArea.FOLLOW_LAUNCHER, "Usar el paquete de iconos actual del lanzador", "Sustituye los iconos de las aplicaciones en la barra de estado y en las tarjetas de mensajes de notificación; solo afecta al modo nativo. Las sustituciones específicas por aplicación tienen prioridad. Los avatares y las imágenes grandes de los mensajes no cambian; si no se pueden leer, se conserva el icono del sistema.", "Estilo de visualización");
        b.text(NotificationIconArea.TEXT, "Símbolo o texto", "Hasta 12 caracteres; se admiten símbolos o textos cortos.", "Estilo de visualización", null, null);
        b.add(NotificationIconArea.IMAGE_NAME, "Imagen personalizada", "Selecciona PNG, JPEG o WebP de hasta 2 MB.", IMAGE,
                "Estilo de visualización", "", 0, 0, 0, null, null, null, null);
        b.toggle(NotificationIconArea.POSITION, "Ajustar posición vertical", "Ajusta la posición vertical; el espaciado horizontal se configura por separado en esta página.", "Posición");
        b.number(NotificationIconArea.Y, "Posición vertical", "Posición", "dp", -160, 160, .01f);
        layoutMargins(b, StatusBarLayoutSettings.NOTIFICATION, "Espaciado lateral");
        b.toggle(NotificationIconArea.SPACING_ENABLED,"Ajustar el espaciado de los iconos","Se aplica a varios iconos de notificación; no afecta a un solo símbolo, texto o imagen.","Espaciado de iconos");
        b.number(NotificationIconArea.SPACING,"Ajuste del espaciado entre iconos","Espaciado de iconos","dp",-10,20,.1f);
        b.toggle(NotificationIconArea.SIZE_ENABLED, "Ajustar tamaño", "Tamaño del icono de la aplicación o del contenido", "Tamaño");
        b.number(NotificationIconArea.SIZE, "Tamaño visible", "Tamaño", "dp", 8, 40, .01f);
        b.toggle("notification_icons_count_enabled", "Limitar la cantidad de iconos nativos", "Solo afecta al modo de visualización nativo; todas las notificaciones reales se conservan.", "Cantidad de iconos");
        b.number("notification_icons_max_count", "Cantidad máxima de iconos visibles", "Cantidad de iconos", "unidades", 0, 10, 1);
        b.toggle(NotificationIconArea.COLOR_ENABLED, "Ajustar color", "Si está desactivado, se usa el color de iconos del sistema.", "Color y transparencia");
        b.colorPair("notification_icons_color_light", "notification_icons_color_dark", "Color de iconos", "Color y transparencia");
        return new Group(b);
    }
    private static Group notificationOverrides() {
        Builder b = builder("notification_icon_overrides", "Icono de notificación por aplicación", "Elige una aplicación del sistema o de terceros y sustituye su icono de notificación en la barra de estado por texto, emoji o el icono de otra aplicación. Cada aplicación se configura por separado y el contenido de la notificación no se modifica. Si la aplicación de origen no está disponible, se conserva el icono original.", STATUSBAR, NotificationIconOverrides.MASTER);
        b.text(NotificationIconOverrides.RULES, "Gestionar sustituciones por aplicación", "Elige la aplicación y el contenido sustituto; puedes activarlo, editarlo o eliminarlo por separado.", "Reglas de aplicaciones", null, null);
        return new Group(b);
    }
    private static Group iconLibrary() {
        Builder b = builder("status_icon_library", "Gestión de iconos", "Importa una biblioteca de iconos y elige los conjuntos a los que aplicarla, o configura iconos, estados, colores y transparencia por función. Al desactivar el interruptor principal se pausan todos los cambios, pero se conservan la configuración y las bibliotecas. Al volver a activarlo, se restauran.", OTHER, IconManagementSettings.ENABLED);
        b.text(IconManagementSettings.KEY, "Gestionar iconos", "Bibliotecas de iconos, aplicación de conjuntos, iconos por función y colores.", "Gestión de iconos", null, null);
        return new Group(b);
    }
    private static Group carrier() {
        Builder b = builder("carrier", "Personalización del texto del operador", "Configura por separado el operador, la hora o el texto simple en el panel de notificaciones, el centro de control y la pantalla de bloqueo.", NOTIFICATION, "carrier_enabled");
        layoutMargins(b, StatusBarLayoutSettings.CARRIER, "Espaciado lateral");
        String[] titles = {"Panel de notificaciones", "Centro de control", "Pantalla de bloqueo"};
        for (int i = 0; i < CarrierPanels.GROUPS.length; i++) {
            String p = CarrierPanels.GROUPS[i], s = titles[i];
            b.toggle(p + "_enabled", "Activar" + s + "Configuración independiente", i == 2 ? "Al desactivarlo, restaura el texto del operador del sistema." : "Solo se aplica a esta interfaz", s);
            controls(b, p, s + " · Interruptor independiente", true);
            b.toggle(p + "_replace_enabled", "Sustitución del operador", "Aplica el contenido mostrado seleccionado abajo.", s + " · Contenido mostrado");
            b.choice(p + "_mode", "Contenido mostrado", "Qué se muestra en la posición del operador: el operador del sistema, una hora personalizada o texto simple.", s + " · Contenido mostrado", new String[]{"original", "time", "text"}, new String[]{"Operador", "Hora personalizada", "Solo texto"});
            b.text(p + "_pattern", "Formato de hora", "Contenido que se muestra cuando «Contenido mostrado» está configurado como hora personalizada.", s + " · Contenido mostrado", CLOCK_VALUES, CLOCK_LABELS);
            b.text(p + "_text", "Texto personalizado", "Texto que se muestra cuando «Contenido mostrado» está configurado como texto simple; admite chino, inglés y emoji.", s + " · Contenido mostrado", null, null);
            position(b, p, s + " · Posición"); textSize(b, p, s + " · Estilo del texto");
            b.colorPair(p + "_color_light", p + "_color_dark", "Texto del operador", s + " · Color y transparencia");
        }
        return new Group(b);
    }
    private static Group classicText() {
        String carrier = CarrierPanels.CLASSIC, clock = ClassicTextSettings.CLOCK;
        Builder b = builder("classic_text", "Personalización de texto en modo clásico",
                "Solo se aplica al panel desplegable clásico del sistema. El operador y la fecha/hora se configuran por separado, y los interruptores de cada pestaña controlan sus respectivas funciones. El formato, la posición, el tamaño, el color y la transparencia se guardan de forma independiente. Al cambiar al modo separado, esta función queda en pausa y se conserva la configuración.",
                NOTIFICATION, carrier + "_enabled");
        controls(b, carrier, "Operador · Interruptor independiente", true);
        b.toggle(carrier + "_replace_enabled", "Sustitución del operador", "Elige el operador nativo, fecha y hora personalizadas o texto simple.", "Operador · Contenido mostrado");
        b.choice(carrier + "_mode", "Contenido mostrado", "Qué se muestra en la posición del operador: el operador del sistema, fecha y hora personalizadas o texto simple.", "Operador · Contenido mostrado", new String[]{"original", "time", "text"}, new String[]{"Operador", "Fecha y hora personalizadas", "Solo texto"});
        b.text(carrier + "_pattern", "Formato de fecha y hora", "Contenido que se muestra cuando «Contenido mostrado» está configurado como fecha y hora personalizadas.", "Operador · Contenido mostrado", CLOCK_VALUES, CLOCK_LABELS);
        b.text(carrier + "_text", "Texto personalizado del operador", "Texto que se muestra cuando «Contenido mostrado» está configurado como texto simple; admite chino, inglés y emoji.", "Operador · Contenido mostrado", null, null);
        position(b, carrier, "Operador · Posición"); textSize(b, carrier, "Operador · Estilo del texto");
        b.colorPair(carrier + "_color_light", carrier + "_color_dark", "Color del operador", "Operador · Color y transparencia");
        b.toggle(ClassicTextSettings.CLOCK_MASTER, "Ajustar por separado la hora del modo clásico", "Es independiente del interruptor del operador de esta página; si se desactiva, se restaura la hora nativa del panel clásico.", "Hora · Interruptor de función");
        controls(b, clock, "Hora · Interruptor independiente", true);
        b.toggle(clock + "_enabled", "Formato de hora personalizado", "Admite hora, fecha y día de la semana.", "Hora · Formato de hora");
        b.text(clock + "_pattern", "Formato de fecha y hora", "Contenido de la hora del panel clásico; puedes elegir un formato habitual o uno personalizado.", "Hora · Formato de hora", CLOCK_VALUES, CLOCK_LABELS);
        position(b, clock, "Hora · Posición"); textSize(b, clock, "Hora · Estilo del texto");
        b.colorPair(clock + "_color_light", clock + "_color_dark", "Color de la hora", "Hora · Color y transparencia");
        digitCells(b, clock, "Hora · Alineación de los números");
        return new Group(b);
    }
    /**
     * One orientation, one entrance: the page's own switch sits in the top bar and inside there is one tab per panel (notification centre,
     * control centre, and for the system's classic pull-down the single panel). Each tab: the picture (a card with the album picker, the
     * cropper and the rest), whether it is used, its brightness and blur, and for the notification centre the clock zone.
     */
    private static Group shadeWallpaperPage(boolean landscape) {
        String name = landscape ? "Horizontal" : "Vertical";
        Builder b = builder(landscape ? "shade_wallpaper_landscape" : "shade_wallpaper_portrait", name + "Fondo de pantalla del panel desplegable",
                "Función aún no terminada." + name + "Fondo de pantalla del panel desplegable. El centro de notificaciones y el centro de control tienen páginas independientes: elige una imagen de la galería, recórtala y ajusta el brillo y el desenfoque. En el centro de notificaciones también puedes configurar por separado el área del reloj grande."
                        + "El fondo aparece según el progreso del despliegue y se mezcla al cambiar de página lateralmente. Si solo configuras un panel, el otro usa la misma imagen. La textura de bloqueo del reloj grande deja ver este fondo.",
                OTHER, ShadeWallpaperSettings.masterKey(landscape));
        b.toggle(ShadeWallpaperSettings.blurWithPullKey(landscape), "Desenfoque gradual al desplegar",
                "El fondo empieza nítido y se desenfoca progresivamente al desplegarse, igual que el desenfoque nativo del sistema. Si se desactiva, empieza directamente con el nivel de desenfoque configurado. Se comparte entre ambos paneles.", "Progreso del despliegue");
        b.toggle(ShadeWallpaperSettings.swipeSlideKey(landscape), "Desplazar el fondo al cambiar de página lateralmente",
                "Al deslizar horizontalmente entre el centro de notificaciones y el centro de control, desactivado hace que los dos fondos aparezcan y desaparezcan gradualmente sin moverse; activado hace que se desplacen junto con las páginas. Solo se nota si las imágenes son distintas; si son iguales, la imagen permanece fija.", "Progreso del despliegue");
        String[] scenes = {landscape ? ShadeWallpaperSettings.NOTIFICATION_LANDSCAPE : ShadeWallpaperSettings.NOTIFICATION_PORTRAIT,
                landscape ? ShadeWallpaperSettings.CONTROL_LANDSCAPE : ShadeWallpaperSettings.CONTROL_PORTRAIT,
                landscape ? ShadeWallpaperSettings.CLASSIC_LANDSCAPE : ShadeWallpaperSettings.CLASSIC_PORTRAIT};
        String[] panels = {"Centro de notificaciones", "Centro de control", "Panel desplegable clásico"};
        for (int i = 0; i < scenes.length; i++) {
            String scene = scenes[i], panel = panels[i];
            b.text(ShadeWallpaperSettings.revisionKey(scene), "Imagen de fondo", "Selecciona una imagen de la galería y recórtala con zoom, desplazamiento y rotación. Si no hay imagen, se usa el fondo nativo.", "Imagen de fondo", null, null);
            b.toggle(ShadeWallpaperSettings.enabledKey(scene), "Usar" + panel + "Fondo de pantalla",
                    i == 2 ? "Esta imagen se usa cuando el sistema tiene el panel desplegable clásico (notificaciones y centro de control en el mismo panel). No se usa en el modo separado."
                            : "Al activarlo, el panel de" + name + " desplegado usa" + panel + "esta imagen; si no hay imagen o está desactivado, se usa el fondo nativo.", "Mostrar");
            b.number(ShadeWallpaperSettings.brightnessKey(scene), "Brillo de la imagen", "Mostrar", "%", 0, 200, 1f);
            b.number(ShadeWallpaperSettings.blurKey(scene), "Desenfoque del fondo de pantalla", "Mostrar", "dp", 0, ShadeWallpaperSettings.MAX_BLUR_DP, .5f);
            if (!ShadeWallpaperSettings.hasZone(scene)) continue;
            b.toggle(ShadeWallpaperSettings.zoneEnabledKey(scene), "Configuración independiente del área del reloj grande", "La zona superior del reloj grande tiene sus propios ajustes: desenfoque del fondo bajo el texto, brillo, saturación y color superpuesto. Se funde suavemente con los ajustes de abajo. La textura de vidrio del reloj grande deja ver la imagen de esta zona.", "Área del reloj grande");
            b.number(ShadeWallpaperSettings.zoneBlurKey(scene), "Desenfoque del fondo bajo el texto", "Área del reloj grande", "dp", 0, ShadeWallpaperSettings.MAX_BLUR_DP, .5f);
            b.add(ShadeWallpaperSettings.zoneLiftKey(scene), "Aclarar el fondo bajo el texto", "Los valores positivos aclaran hacia el blanco, los negativos oscurecen hacia el negro y 0 no cambia nada. Solo afecta al fondo del área del reloj grande.", NUMERIC, "Área del reloj grande", "%", -100, 100, 1f, null, null, null, null);
            b.add(ShadeWallpaperSettings.zoneSaturationKey(scene), "Saturación del fondo bajo el texto", "100 % conserva los colores originales, 0 % los convierte en gris y 200 % duplica la intensidad.", NUMERIC, "Área del reloj grande", "%", 0, 200, 1f, null, null, null, null);
            b.add(ShadeWallpaperSettings.zoneTintKey(scene), "Color superpuesto al fondo bajo el texto", "Capa de color sobre el fondo del área del reloj grande; su intensidad se define con «Intensidad de superposición».", COLOR, "Área del reloj grande", "", 0, 0, 0, null, null, null, null);
            b.add(ShadeWallpaperSettings.zoneTintStrengthKey(scene), "Intensidad de superposición", "0 % no aplica superposición; 100 % usa por completo el color superpuesto.", NUMERIC, "Área del reloj grande", "%", 0, 100, 1f, null, null, null, null);
            b.toggle(ShadeWallpaperSettings.zoneFollowKey(scene), "Altura del área según el reloj grande", "El borde inferior del área sigue al borde inferior del reloj grande, incluida la fecha, y cambia con el tamaño y la posición del texto. Si se desactiva, se usa la altura fija indicada abajo.", "Área del reloj grande");
            b.number(ShadeWallpaperSettings.zoneHeightKey(scene), "Altura del área", "Área del reloj grande", "dp", 0, ShadeWallpaperSettings.MAX_ZONE_HEIGHT_DP, 1f);
            b.number(ShadeWallpaperSettings.zoneFeatherKey(scene), "Altura de la transición", "Área del reloj grande", "dp", 0, ShadeWallpaperSettings.MAX_ZONE_FEATHER_DP, 1f);
        }
        return new Group(b);
    }
    private static Group font() {
        Builder b = builder("font", "Configuración de fuentes", "Primero elige la fuente y después su ámbito de aplicación; el tamaño, grosor y color siguen configurándose en cada función.", OTHER, "font_enabled");
        b.choice(StatusBarSettings.FONT_MODE, "Origen del texto", "Elige de forma unificada fuentes del sistema, PingFang, fuentes importadas o descargables. Puedes importarlas, cambiarles el nombre y eliminarlas.", "Origen del texto", new String[]{"system", "pingfang", "custom"}, new String[]{"Seguir al sistema", "PingFang", "Fuente personalizada"});
        return new Group(b);
    }
    private static Group battery() {
        Builder b = builder("battery", "Batería", "Sigue la carga real. La carcasa de la batería, los números y el rayo se configuran por separado.", STATUSBAR, "battery_enabled");
        controls(b, "battery", "Interruptor independiente", false);
        b.toggle("battery_style_enabled", "Aplicar el estilo de batería seleccionado", "Si se desactiva, se restaura el estilo nativo del sistema.", "Estilo de batería");
        b.choice(StatusBarSettings.BATTERY_STYLE, "Estilo de batería", "PUI sigue los ajustes de porcentaje de batería del sistema: si el porcentaje aparece dentro de la batería, muestra el número; si aparece fuera, conserva la barra de carga. El sistema debe tener activado el porcentaje (dentro o fuera). Al desactivarlo, se restaura el estilo nativo; los colores de batería baja, ahorro de energía y carga siguen al sistema.", "Estilo de batería", new String[]{"pui", "native"}, new String[]{"Estilo PUI predeterminado", "Estilo nativo del sistema"});
        b.toggle(BatteryControls.HIDE_CHARGE, "Ocultar el icono de carga", "Oculta el rayo de carga dentro y fuera de la batería. Tiene prioridad sobre la alternancia entre números y rayo, y no cambia el estado de carga ni el color del relleno durante la carga.", "Indicador de carga");
        b.toggle(StatusBarSettings.BATTERY_CHARGE_INSIDE, "Alternar entre el número y el rayo dentro de la batería", "Alterna suavemente mientras carga y muestra el nivel de batería al dejar de cargar.", "Alternancia durante la carga");
        b.number(StatusBarSettings.BATTERY_HOLD, "Tiempo visible de cada elemento", "Alternancia durante la carga", "segundos", 1, 10, .01f);
        b.number(StatusBarSettings.BATTERY_FADE, "Duración del fundido de entrada y salida", "Alternancia durante la carga", "segundos", .15f, 3, .01f);
        position(b, "battery", "Posición");
        layoutMargins(b, StatusBarLayoutSettings.BATTERY, "Espaciado lateral");
        scale(b, StatusBarSettings.BATTERY_SCALE, "Tamaño general", "Tamaño");
        scale(b, StatusBarSettings.BATTERY_WIDTH_SCALE, "Ajuste fino del ancho", "Tamaño");
        scale(b, StatusBarSettings.BATTERY_HEIGHT_SCALE, "Ajuste fino de la altura", "Tamaño");
        b.toggle(BatteryTextStyle.MASTER, "Ajustes del texto dentro de la batería", "Ajusta de forma independiente los números del nivel de batería; al desactivar, se restaura el texto nativo.", "Texto dentro de la batería");
        b.number(BatteryTextStyle.X, "Posición horizontal de los números", "Texto dentro de la batería", "dp", -20, 20, .01f);
        b.number(BatteryTextStyle.Y, "Posición vertical de los números", "Texto dentro de la batería", "dp", -20, 20, .01f);
        b.number(BatteryTextStyle.SIZE, "Tamaño de los números", "Texto dentro de la batería", "sp", 4, 24, .01f);
        b.number(BatteryTextStyle.WEIGHT, "Grosor de los números", "Texto dentro de la batería", "", 100, 900, 1);
        b.number(BatteryTextStyle.SPACING, "Espaciado de los números", "Texto dentro de la batería", "dp", -2, 4, .01f);
        normalColors(b, "battery", "Carcasa de la batería y relleno normal");
        String[] parts = {"text", "bolt", "charge", "alert"};
        String[] titles = {"Número del nivel de batería", "Rayo de carga", "Relleno durante la carga", "Relleno de batería baja o ahorro de energía"};
        for (int i = 0; i < parts.length; i++) {
            String p = "battery_" + parts[i];
            b.toggle(p + "_color_enabled", "Independiente" + titles[i] + "Color", "Si está desactivado, sigue el color del sistema.", titles[i]);
            b.colorPair(p + "_color_light", p + "_color_dark", titles[i], titles[i]);
        }
        return new Group(b);
    }
    private static Group tiles() {
        Builder b = builder("tiles", "Animación al pasar páginas", "Al cambiar de página entre las tarjetas del centro de control, ambas páginas tienen animaciones que siguen continuamente el movimiento del dedo y vuelven con el rebote del sistema al soltarlo. Solo se dibujan durante el cambio de página, sin coste adicional cuando está quieto.", CONTROL_CENTER, "tiles_enabled");
        b.choice(TilePageEffects.STYLE, "Estilo de animación", "iOS: ambas páginas se encogen ligeramente y se desvanecen; 3D: giran como dos caras de un cubo alrededor de la unión; zoom: solo se encogen; fundido: solo cambia la transparencia; borde clásico: conserva el desvanecimiento y desenfoque de los bordes originales (los ajustes de «Borde clásico» solo se aplican a este estilo).", "Estilo",
                TilePageEffects.STYLES, new String[]{"Zoom y fundido estilo iOS", "Giro 3D", "Zoom", "Solo fundido", "Fundido del borde clásico"});
        b.add(StatusBarSettings.TILES_STRENGTH, "Intensidad del efecto", "Se comparte entre todos los estilos: 0 % no aplica efectos y 100 % usa el efecto completo.", NUMERIC, "Estilo", "%", 0, 100, .01f, null, null, null, null);
        b.toggle("tiles_portrait_enabled", "Activar en vertical", "", "Interruptor independiente");
        b.toggle("tiles_landscape_enabled", "Activar en horizontal", "", "Interruptor independiente");
        b.toggle("tiles_fade_enabled", "Fundido de los bordes durante el deslizamiento", "Solo para el estilo «Borde clásico»", "Borde clásico");
        b.toggle("tiles_blur_enabled", "Desenfoque avanzado", "Solo para el estilo «Borde clásico». Desactivado de forma predeterminada; desenfoca gradualmente los bordes durante el deslizamiento.", "Borde clásico");
        b.number(StatusBarSettings.TILES_LEFT_RANGE, "Rango de fundido del lado izquierdo", "Borde clásico · Rango y posición", "dp", 0, 192, .01f);
        b.number(StatusBarSettings.TILES_RIGHT_RANGE, "Rango de fundido del lado derecho", "Borde clásico · Rango y posición", "dp", 0, 192, .01f);
        b.number(StatusBarSettings.TILES_LEFT_OFFSET_X, "Posición horizontal izquierda", "Borde clásico · Rango y posición", "dp", -160, 160, .01f);
        b.number(StatusBarSettings.TILES_RIGHT_OFFSET_X, "Posición horizontal derecha", "Borde clásico · Rango y posición", "dp", -160, 160, .01f);
        b.number(StatusBarSettings.TILES_OFFSET_Y, "Posición vertical del área de fundido", "Borde clásico · Rango y posición", "dp", -240, 240, .01f);
        b.number(StatusBarSettings.TILES_REGION_HEIGHT, "Altura del área de fundido", "Borde clásico · Rango y posición", "dp", 0, 1000, .01f);
        b.number(StatusBarSettings.TILES_VERTICAL_FEATHER, "Rango de transición suave vertical", "Borde clásico · Rango y posición", "dp", 0, 192, .01f);
        b.number(StatusBarSettings.TILES_BLUR_RADIUS, "Radio de desenfoque", "Borde clásico", "dp", 0, 48, .01f);
        return new Group(b);
    }
    private static Group tileFullFill() {
        return new Group(builder("tile_2x1_fill", "Expandir las tarjetas 2×1 para llenar el área",
                "Las tarjetas 2×1 activadas ocupan toda la tarjeta, como la tarjeta de auriculares conectados. Las tarjetas desactivadas conservan el vidrio del sistema; en ambos estados se elimina el pequeño círculo detrás del icono. Puedes ajustar los colores en «Color de las tarjetas». Al desactivar esta función se restaura el aspecto original.",
                CONTROL_CENTER, QsTileFullFill.ENABLED));
    }

    private static Group tileCorners() {
        Builder b = builder("tile_corners", "Radio de esquina uniforme", "Ajusta conjuntamente las esquinas de las tarjetas, los controles deslizantes de brillo y volumen y las tarjetas de música y dispositivos. Al desactivarlo, se restauran las esquinas del sistema. Para evitar errores de dibujo nativo, el radio solo admite valores de 0 a" + (int) QsTileCorners.MAX_RADIUS + " dp。", CONTROL_CENTER, "qs_tile_corners_enabled");
        b.number(QsTileCorners.RADIUS, "Radio de esquina", "Esquinas redondeadas", "dp", 0, QsTileCorners.MAX_RADIUS, .5f);
        return new Group(b);
    }
    private static Group tileIconSize() {
        Builder b = builder("tile_icon_size", "Iconos de tarjetas y dispositivos 1×1", "Ajusta el tamaño de los iconos dentro de las tarjetas 1×1 y de las tarjetas de auriculares y espacio para dispositivos, conservando el centrado, la forma, el anillo de batería y las zonas táctiles nativos.", CONTROL_CENTER, QsTileIconSize.MASTER);
        b.number(QsTileIconSize.SIZE, "Tamaño del icono", "Tamaño del icono", "dp", 0, 64, .01f);
        return new Group(b);
    }
    private static Group qsEntrances() {
        Builder b = builder(QsEntrances.GROUP, "Botones superiores", "Botones «Editar» (arriba a la izquierda) y «Ajustes» (arriba a la derecha) del centro de control. Al ocultarlos no se ven ni se pueden tocar; si activas «Permitir tocar elementos ocultos», quedan invisibles pero se puede tocar su posición original. Los demás botones y tarjetas conservan su posición nativa. Desactivar esta página o el interruptor restaura el comportamiento.", CONTROL_CENTER, QsEntrances.MASTER);
        b.toggle(QsEntrances.EDIT_HIDDEN, "Ocultar «Editar»", "Oculta el botón de la esquina superior izquierda que permite editar las tarjetas, incluido su punto rojo. Después podrás ajustar las tarjetas desde los ajustes del sistema o desactivar temporalmente esta opción para editarlas.", "Botón");
        b.toggle(QsEntrances.SETTINGS_HIDDEN, "Ocultar «Ajustes»", "Oculta el engranaje de la esquina superior derecha del centro de control que abre los ajustes del sistema.", "Botón");
        b.toggle(QsEntrances.HIDDEN_CLICKABLE, "Permitir tocar elementos ocultos", "Los botones «Editar» y «Ajustes» ocultos permanecen en su posición original, pero son totalmente transparentes; al tocar esa zona se abre la edición o los ajustes del sistema. Si está desactivado, los botones ocultos no ocupan espacio ni se pueden tocar. Solo afecta a los botones cuya ocultación esté activada arriba.", "Botón");
        return new Group(b);
    }
    private static Group shadeIcons() {
        Builder b = builder("shade_status_icons", "Iconos superiores al cambiar de página", "Si se activa este interruptor o el reloj grande, durante el despliegue se ocultan la hora pequeña, los iconos de notificación y los iconos de la derecha de la barra de estado; también se ocultan la hora pequeña y los iconos derechos de la página de notificaciones. Se restauran cuando ambos paneles se cierran. Permanecen ocultos durante el cambio lateral, sin dibujar copias ni mover iconos. Los iconos derechos del centro de control siguen siendo nativos; este interruptor controla la hora pequeña y las notificaciones de la izquierda con los márgenes del sistema.", CONTROL_CENTER, StatusBarShadeIconSettings.MASTER);
        return new Group(b);
    }
    private static Group qs() {
        Builder b = builder("qs_appearance", "Color de las tarjetas", "Color de fondo, degradado y transparencia de las tarjetas activas; conserva el vidrio y los reflejos nativos. Los degradados pueden consumir más recursos; actívalos solo si los necesitas.", CONTROL_CENTER, QsTileAppearance.MASTER);
        b.colorPair("qs_global_light_color", "qs_global_dark_color", "Color del relleno activo", "Color de relleno");
        b.colorPair("qs_global_light_gradient_color", "qs_global_dark_gradient_color", "Color final del degradado", "Color del degradado");
        for (String scene : new String[]{"light", "dark"}) {
            String s = "light".equals(scene) ? "Fondo claro" : "Fondo oscuro", p = QsTileAppearance.prefix(scene);
            b.toggle(p + "_gradient_enabled", "Activar degradado", "El degradado puede consumir más recursos; la transparencia del relleno se multiplica por la transparencia del color.", s + " · Efecto de relleno");
            b.number(p + "_opacity", "Transparencia del relleno", s + " · Efecto de relleno", "%", 0, 100, .01f);
            b.number(p + "_gradient_angle", "Dirección del degradado", s + " · Efecto de relleno", "°", 0, 360, .01f);
            b.add(p + "_gradient_range", "Rango de transición", RANGE_BETWEEN, NUMERIC, s + " · Efecto de relleno", "%", 5, 100, 1f, null, null, null, null);
        }
        return new Group(b);
    }
    private static Group bigClock() {
        Builder b = builder("notification_big_clock", "Reloj grande en vertical", "Solo ajusta la página de notificaciones en vertical; la horizontal se configura por separado. La hora se alinea con los bordes de las notificaciones, cuyo tamaño de texto, iconos y tarjetas conserva el aspecto nativo. Al deslizar hacia arriba, la hora superior y los bordes de las notificaciones se desvanecen y desenfocan; el texto personalizado inferior permanece nítido. Desactivado de forma predeterminada; la configuración vertical existente se conserva.", NOTIFICATION, NotificationBigClockSettings.MASTER);
        b.text(NotificationBigClockSettings.PATTERN, "Formato de hora", "Contenido del reloj grande de la página de notificaciones; puedes elegir un formato habitual o uno personalizado.", "Hora", CLOCK_VALUES, CLOCK_LABELS);
        b.choice(NotificationBigClockSettings.FONT, "Origen de la fuente", "Hay tres fuentes nativas: seguir la fuente actual de la pantalla de bloqueo o elegir OPPO Sans, OPPO Digit u OPPO Serif. En vertical puedes definir por separado la altura expandida y contraída; las demás fuentes usan una sola altura. Las fuentes personalizadas se importan en «Otros → Configuración de fuentes».", "Hora", NotificationBigClockSettings.FONT_VALUES, NotificationBigClockSettings.FONT_LABELS);
        b.number(NotificationBigClockSettings.MAX_SIZE, "Ajustar altura de la fuente", "Hora", "dp", 48, 240, .01f);
        b.number(NotificationBigClockSettings.COMPACT_MAX_SIZE, "Altura de la fuente contraída", "Hora", "dp", 12, 96, .01f);
        b.number(NotificationBigClockSettings.SCREEN_PADDING, "Ajuste del espaciado dentro de la pantalla", "Hora", "dp", 0, 80, .1f);
        b.number(NotificationBigClockSettings.WEIGHT, "Grosor de la fuente de la hora", "Hora", "", 100, 900, 1);
        b.number(NotificationBigClockSettings.LETTER_SPACING, "Espaciado de la hora", "Hora", "em", -.15f, .5f, .01f);
        b.toggle(NotificationBigClockSettings.PROPORTIONAL_DIGITS, "Desbloquear el límite de espaciado de ancho uniforme",
                "Los números se distribuyen según el ancho de cada glifo y pueden ajustarse con el espaciado de la hora; el ancho real depende de la fuente seleccionada.", "Hora");
        b.toggle(NotificationBigClockSettings.TIME_ANIMATION_DISABLED, "Desactivar la animación al cambiar la hora",
                "Actualiza los números directamente cuando cambia la hora, conservando las animaciones de entrada, salida, despliegue y rebote del reloj grande.", "Cambio de hora");
        b.toggle(NotificationBigClockSettings.LOCK_STYLE, "Textura de pantalla de bloqueo",
                "Da al reloj grande de las notificaciones un aspecto similar al reloj de la pantalla de bloqueo: contornos redondeados, texto blanco cremoso más intenso arriba y más tenue abajo, un borde iluminado a un lado, sombra al otro y un leve resplandor exterior. Es una simulación, no una copia exacta píxel a píxel (la pantalla de bloqueo usa la imagen del fondo; aquí solo se usan los píxeles del reloj). Al activarlo, «Texto de vidrio líquido» y «Borde de vidrio» quedan en pausa, pero sus ajustes se conservan. Requiere Android 13 o posterior.", "Textura de pantalla de bloqueo");
        b.toggle(NotificationBigClockSettings.LOCK_STYLE_NATIVE, "Usar el vidrio nativo de la pantalla de bloqueo",
                "Cuando el reloj ya se haya mostrado en la pantalla de bloqueo, utiliza directamente su efecto de vidrio nativo (muestra el fondo, refracción, resplandor y borde iluminado), la opción más parecida al original. En ese caso solo actúa «Intensidad del cuerpo del texto» (intensidad del vidrio). Si el reloj de bloqueo aún no se ha mostrado o el sistema no permite reutilizarlo, se emplea automáticamente la simulación aproximada; los demás controles solo afectan a esa simulación.", "Textura de pantalla de bloqueo");
        b.add(NotificationBigClockSettings.LOCK_STYLE_BODY, "Intensidad del cuerpo del texto", "Intensidad del vidrio nativo y opacidad del texto en la simulación: 100 % reproduce el aspecto de la pantalla de bloqueo; cuanto menor sea el valor, más transparente será.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_GRADIENT, "Más intenso arriba, más tenue abajo", "Grado de transición del texto de intenso a tenue de arriba abajo: 0 % mantiene la misma intensidad; cuanto mayor sea el valor, más diferencia habrá.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_RIM, "Intensidad del borde iluminado", "Reflejo en el borde del lado iluminado: 0 % sin reflejo; cuanto mayor sea el valor, más brillante.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_RIM_DEPTH, "Ancho del borde iluminado", "Distancia hacia el interior del texto que ocupan el reflejo y la sombra: cuanto mayor sea, más anchos y suaves; cuanto menor, más pegados al borde.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_ROUND, "Redondez", "Redondez del contorno de los caracteres: 0 % mantiene las esquinas nítidas; cuanto mayor sea el valor, más redondeado y suave será el aspecto.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_SHADE, "Intensidad de la sombra", "Sombra del borde en el lado opuesto a la luz: 0 % sin sombra; cuanto mayor sea el valor, más oscura y tridimensional se verá la letra.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_GLOW, "Resplandor exterior", "Un halo tenue alrededor de los caracteres: 0 % desactivado; cuanto mayor sea el valor, más visible.", NUMERIC, "Textura de pantalla de bloqueo", "%", 0, NotificationBigClockSettings.LOCK_STYLE_STRENGTH_MAX, 1f, null, null, null, null);
        b.add(NotificationBigClockSettings.LOCK_STYLE_ANGLE, "Dirección de la luz", "Dirección de la luz: 0° desde la derecha, 90° desde arriba, 180° desde la izquierda y 270° desde abajo. El valor predeterminado es 300° (desde abajo a la derecha).", NUMERIC, "Textura de pantalla de bloqueo", "°", 0, 360, 1f, null, null, null, null);
        b.toggle(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED, "Mostrar el fondo a través de los caracteres",
                "Muestra dentro de los caracteres de la hora el fondo desenfocado existente del sistema, elimina su oscurecimiento negro y superpone una máscara de color ajustable. Al activarlo, usa un material de fondo específico; se conservan los ajustes de color del texto, vidrio líquido y textura de bloqueo y se restauran al desactivarlo. La fuente, tamaño, grosor, espaciado, posición y animaciones siguen siendo configurables.", "Transparencia del fondo y máscara");
        b.toggle(NotificationBigClockSettings.FOLLOW_STYLE, "Sincronizar el estilo de hora y fecha",
                "Desactivado de forma predeterminada. Solo está disponible cuando se activa la transparencia de fondo dentro de los caracteres: la fecha y el texto adicional utilizan el mismo efecto y color de máscara que la hora, con intensidades ajustables por separado y un aspecto plano. El borde 3D solo se aplica a los números del reloj grande. Si se desactiva, se conservan los colores y estilos independientes.", "Transparencia del fondo y máscara");
        b.add(NotificationBigClockSettings.REVEAL_MASK_COLOR, "Color de la máscara", "Color superpuesto al fondo desenfocado dentro de los caracteres; el blanco es el predeterminado. Se guarda por separado para vertical y horizontal. La transparencia del color y la intensidad de la máscara de cada texto determinan el resultado.", COLOR, "Transparencia del fondo y máscara", "", 0, 0, 0, null, null, null, null);
        b.add(NotificationBigClockSettings.REVEAL_MASK_OPACITY, "Intensidad de la máscara de la hora principal", "Solo ajusta los números del reloj grande. 0 % no superpone color y deja visible el fondo desenfocado sin oscurecimiento negro; 100 % cubre el fondo con el color elegido y su transparencia. El valor predeterminado es blanco al 20 %.", NUMERIC, "Transparencia del fondo y máscara", "%", 0, 100, 1f, null, null, null, null);
        b.add(ClockBackdropSettings.DATE_MASK_OPACITY,"Intensidad de la máscara de la fecha","Cuando se sincroniza el estilo de hora y fecha, ajusta solo la intensidad de la máscara de color de la fecha; 0 % conserva el fondo visible dentro de los caracteres.",NUMERIC,"Transparencia del fondo y máscara","%",0,100,1f,null,null,null,null);
        b.add(ClockBackdropSettings.FOOTER_MASK_OPACITY,"Intensidad de la máscara del texto adicional","Cuando se sincroniza el estilo de hora y fecha, ajusta solo la máscara de color del texto inferior adicional; 0 % conserva el fondo visible dentro de los caracteres.",NUMERIC,"Transparencia del fondo y máscara","%",0,100,1f,null,null,null,null);
        b.toggle(ClockBackdropSettings.EDGE_ENABLED,"Contorno de los bordes","Añade un contorno alrededor de los números del reloj grande para mejorar la legibilidad sobre fondos complejos; está activado de forma predeterminada. La fecha, el texto adicional y los componentes laterales conservan el aspecto plano.","Transparencia del fondo y máscara");
        b.add(ClockBackdropSettings.EDGE_WIDTH,"Ancho del contorno","Ancho del contorno de los caracteres; el valor predeterminado es 0,6 dp.",NUMERIC,"Transparencia del fondo y máscara","dp",0,4,.01f,null,null,null,null);
        b.add(ClockBackdropSettings.EDGE_COLOR,"Color del contorno","El blanco es el valor predeterminado. Admite transparencia propia del color, combinada con la opacidad del contorno.",COLOR,"Transparencia del fondo y máscara","",0,0,0,null,null,null,null);
        b.add(ClockBackdropSettings.EDGE_OPACITY,"Opacidad del contorno","Intensidad del contorno; 60 % es el valor predeterminado.",NUMERIC,"Transparencia del fondo y máscara","%",0,100,1f,null,null,null,null);
        b.add(ClockBackdropSettings.HIGHLIGHT_ANGLE,"Ángulo de la luz","Dirección de la luz de los bordes tridimensionales; 315° es el valor predeterminado.",NUMERIC,"Transparencia del fondo y máscara","°",0,360,1f,null,null,null,null);
        b.add(ClockBackdropSettings.HIGHLIGHT_STRENGTH,"Brillo del reflejo","Intensidad del borde iluminado; 40 % es el valor predeterminado. 0 % desactiva el reflejo.",NUMERIC,"Transparencia del fondo y máscara","%",0,100,1f,null,null,null,null);
        b.add(ClockBackdropSettings.SHADE_STRENGTH,"Intensidad de la sombra","Intensidad del borde en sombra: 0 % de forma predeterminada, para conservar el texto sin oscurecimiento negro. Solo añade sombra si aumentas el valor.",NUMERIC,"Transparencia del fondo y máscara","%",0,100,1f,null,null,null,null);
        b.add(ClockBackdropSettings.DEPTH_WIDTH,"Ancho del borde tridimensional","Distancia hacia el interior de los caracteres que ocupan el reflejo y la sombra; 1,5 dp es el valor predeterminado.",NUMERIC,"Transparencia del fondo y máscara","dp",0,8,.01f,null,null,null,null);
        b.toggle(NotificationBigClockSettings.GLASS, "Texto de vidrio líquido", "Ajusta el material del texto de la hora. Si se activa la transparencia de fondo dentro de los caracteres, queda temporalmente deshabilitado y se conserva la configuración; se restaura al desactivar dicha transparencia.", "Vidrio líquido");
        b.colorPair(NotificationBigClockSettings.COLOR_LIGHT, NotificationBigClockSettings.COLOR_DARK, "Color del texto de la hora", "Color del texto");
        b.toggle(NotificationBigClockSettings.GLASS_BORDER_ENABLED, "Mostrar borde de vidrio", "", "Borde de vidrio");
        b.number(NotificationBigClockSettings.GLASS_BORDER_WIDTH, "Ancho del borde de vidrio", "Borde de vidrio", "dp", 0, 4, .01f);
        b.colorPair(NotificationBigClockSettings.GLASS_BORDER_COLOR_LIGHT, NotificationBigClockSettings.GLASS_BORDER_COLOR_DARK, "Color del borde", "Borde de vidrio");
        b.toggle(NotificationBigClockSettings.ENTRY_EFFECT_ENABLED, "Mover, desenfocar y mostrar gradualmente al deslizar el panel", "Usar la misma transición al expandir y contraer", "Efecto de entrada");
        b.number(NotificationBigClockSettings.ENTRY_TRAVEL, "Distancia de desplazamiento de entrada", "Efecto de entrada", "dp", 0, 120, .01f);
        b.number(NotificationBigClockSettings.ENTRY_BLUR_RADIUS, "Radio de desenfoque de entrada", "Efecto de entrada", "dp", 0, 32, .01f);
        b.number(NotificationBigClockSettings.ENTRY_FADE_STRENGTH, "Intensidad del fundido de entrada", "Efecto de entrada", "%", 0, 100, .01f);
        b.number(NotificationBigClockSettings.ENTRY_COMPLETION, "Progreso de despliegue para mostrar el texto nítido", "Efecto de entrada", "%", 1, 100, .01f);
        b.toggle(NotificationBigClockSettings.REBOUND_BLUR_ENABLED, "Desenfoque al rebotar", "Cuando la lista de notificaciones se desliza más allá del límite superior o inferior y rebota con la animación nativa, el reloj se desenfoca más cuanto mayor sea el desplazamiento y vuelve a verse nítido al regresar a su lugar. Solo afecta al reloj grande en vertical.", "Efecto de rebote");
        b.number(NotificationBigClockSettings.REBOUND_BLUR_RADIUS, "Radio de desenfoque durante el rebote", "Efecto de rebote", "dp", 0, 32, .1f);
        b.number(NotificationBigClockSettings.REBOUND_FADE, "Desvanecimiento durante el rebote", "Efecto de rebote", "%", 0, 100, 1f);
        b.toggle(NotificationBigClockSettings.REBOUND_LOCK, "Desactivar el efecto de rebote", "Cuando la lista de notificaciones se desliza más allá del límite y rebota, el reloj deja de desplazarse: permanece fijo tanto al desplegar rápidamente como al seguir deslizando hacia arriba al final de la lista. Se conservan el fundido y el desenfoque durante el despliegue, así como el desvanecimiento al deslizar hacia arriba; solo se elimina la interacción de rebote. Es incompatible con «Desenfoque al rebotar»: al activarlo, la otra opción queda desactivada. Solo afecta al reloj grande en vertical.", "Efecto de rebote");
        b.toggle(NotificationBigClockSettings.CLOCK_CLICK_ENABLED, "Tocar la hora para abrir el reloj", "Solo afecta al reloj grande en vertical; activado de forma predeterminada.", "Acción al tocar");
        b.text(NotificationBigClockSettings.CLOCK_LAUNCH_APP, "Aplicación que se abre al tocar la hora", "Usa el Reloj del sistema por defecto, o elige otra aplicación.", "Acción al tocar", null, null);
        b.toggle(NotificationBigClockSettings.DATE_CLICK_ENABLED, "Tocar la fecha para abrir el calendario", "Funciona cuando se muestra la fecha; está activado de forma predeterminada.", "Fecha · Acción al tocar");
        b.text(NotificationBigClockSettings.DATE_LAUNCH_APP, "Aplicación que se abre al tocar la fecha", "Usa el Calendario del sistema por defecto, o elige otra aplicación.", "Fecha · Acción al tocar", null, null);
        b.toggle(NotificationBigClockSettings.DATE_ENABLED, "Mostrar fecha", "", "Fecha");
        b.text(NotificationBigClockSettings.DATE_PATTERN, "Formato de fecha", "Contenido de la fila de fecha; admite día de la semana, calendario lunar y ciclo sexagenario.", "Fecha", DATE_VALUES, DATE_LABELS);
        b.number(NotificationBigClockSettings.DATE_SIZE, "Tamaño de la fecha", "Fecha", "dp", 8, 40, .01f);
        b.number(NotificationBigClockSettings.DATE_WEIGHT, "Grosor de fuente de la fecha", "Fecha", "", 100, 900, 1);
        b.number(NotificationBigClockSettings.DATE_OFFSET_X, "Posición horizontal de la fecha", "Fecha", "dp", -120, 120, .01f);
        b.number(NotificationBigClockSettings.DATE_OFFSET_Y, "Posición vertical de la fecha", "Fecha", "dp", -120, 120, .01f);
        b.number(NotificationBigClockSettings.DATE_GAP, "Espacio entre fecha y hora", "Fecha", "dp", 0, 80, .01f);
        alignment(b, NotificationBigClockSettings.DATE_ALIGNMENT, "Alineación de la fecha", "Fecha");
        b.colorPair(NotificationBigClockSettings.DATE_COLOR_LIGHT, NotificationBigClockSettings.DATE_COLOR_DARK, "Texto de la fecha", "Color de la fecha");
        b.toggle(BigClockWidgetsSettings.SWAP_WITH_DATE,"Intercambiar posición con la fecha","En vertical, intercambia toda la fila de la fecha con la de los componentes laterales; la hora principal permanece en su lugar. Si la fecha o los componentes no están disponibles, no hay espacio suficiente arriba o la fecha se solapa con la hora, se usa automáticamente la disposición original sin cambiar el interruptor guardado.","Distribución de los componentes");
        bigClockTextComponent(b,BigClockWidgetsSettings.LEFT);
        bigClockTextComponent(b,BigClockWidgetsSettings.RIGHT);
        alignment(b, NotificationBigClockSettings.ALIGNMENT, "Alineación de la hora", "Posición y alineación");
        b.number(NotificationBigClockSettings.OFFSET_X, "Posición horizontal inicial", "Posición y alineación", "dp", -160, 160, .01f);
        b.number(NotificationBigClockSettings.OFFSET_Y, "Posición vertical inicial", "Posición y alineación", "dp", -120, 200, .01f);
        b.number(NotificationBigClockSettings.COMPACT_OFFSET_X, "Posición horizontal contraída", "Posición y alineación", "dp", -160, 160, .01f);
        b.number(NotificationBigClockSettings.COMPACT_OFFSET_Y, "Posición vertical contraída", "Posición y alineación", "dp", -120, 120, .01f);
        b.toggle(NotificationBigClockSettings.NOTIFICATION_GAP_ENABLED, "Mantener automáticamente la distancia entre el reloj y la primera notificación", "", "Espacio entre notificaciones");
        b.number(NotificationBigClockSettings.NOTIFICATION_GAP, "Separación entre la hora y las notificaciones", "Espacio entre notificaciones", "dp", 0, 100, .01f);
        b.toggle(NotificationBigClockSettings.NOTIFICATION_EDGE_ENABLED, "Desenfocar y desvanecer las notificaciones al acercarse al borde superior", "El efecto sigue la posición real de las notificaciones.", "Borde superior de las notificaciones");
        b.number(NotificationBigClockSettings.NOTIFICATION_EDGE_SAFE_DISTANCE, "Distancia de seguridad superior", "Borde superior de las notificaciones", "dp", 0, 100, .01f);
        b.add(NotificationBigClockSettings.NOTIFICATION_EDGE_RANGE, "Rango de transición", "Las notificaciones empiezan a desenfocarse y desvanecerse a partir de la distancia de seguridad superior." + RANGE_DISTANCE,
                NUMERIC, "Borde superior de las notificaciones", "dp", 0, 120, .01f, null, null, null, null);
        b.number(NotificationBigClockSettings.NOTIFICATION_EDGE_BLUR_RADIUS, "Radio de desenfoque del borde", "Borde superior de las notificaciones", "dp", 0, 32, .01f);
        b.toggle(NotificationBigClockSettings.FOOTER_ENABLED, "Mostrar contenido inferior", "", "Contenido inferior");
        b.text(NotificationBigClockSettings.FOOTER_PATTERN, "Formato del contenido", "Formato de la línea inferior; {text} representa el texto configurado abajo.", "Contenido inferior", FOOTER_VALUES, FOOTER_LABELS);
        b.text(NotificationBigClockSettings.FOOTER_TEXT, "Editar texto simple", "Texto que se muestra abajo; admite chino, inglés y emoji.", "Contenido inferior", null, null);
        b.number(NotificationBigClockSettings.FOOTER_SIZE, "Tamaño de fuente inferior", "Contenido inferior", "dp", 8, 36, .01f);
        b.number(NotificationBigClockSettings.FOOTER_WEIGHT, "Grosor de fuente inferior", "Contenido inferior", "", 100, 900, 1);
        b.number(NotificationBigClockSettings.FOOTER_OFFSET_X, "Posición horizontal inferior", "Contenido inferior", "dp", -160, 160, .01f);
        b.number(NotificationBigClockSettings.FOOTER_OFFSET_Y, "Posición vertical inferior", "Contenido inferior", "dp", -120, 120, .01f);
        b.number(NotificationBigClockSettings.FOOTER_MARGIN, "Margen de seguridad inferior", "Contenido inferior", "dp", 0, 120, .01f);
        alignment(b, NotificationBigClockSettings.FOOTER_ALIGNMENT, "Alineación del contenido inferior", "Contenido inferior");
        b.colorPair(NotificationBigClockSettings.FOOTER_COLOR_LIGHT, NotificationBigClockSettings.FOOTER_COLOR_DARK, "Texto inferior", "Color inferior");
        return new Group(b);
    }
    private static void bigClockTextComponent(Builder b,int side) {
        String name=side==BigClockWidgetsSettings.LEFT?"Izquierda":"Derecha",section=name+"Componentes";
        boolean right=side==BigClockWidgetsSettings.RIGHT;
        b.toggle(BigClockWidgetsSettings.enabledKey(side),"Mostrar"+section,"Mostrar en el área del reloj grande vertical"+name+"Componente de texto que puede intercambiar toda su fila con la fecha. Desactivado de forma predeterminada.",section);
        b.text(BigClockWidgetsSettings.textKey(side),name+"Contenido del componente","Puedes elegir un valor predefinido o introducir texto libre y combinarlo con texto normal."
                        +"El tiempo, la batería, la señal y la red pueden personalizarse como el formato horario; introduce los símbolos manualmente. Ejemplos de sintaxis: （电池:{电量}%{充电?⚡}）、（天气:{描述} {温度:数字}°C）、（信号:{条:4:●:○}）、（网络:{类型}）。"
                        +"Los marcadores existentes （天气）（天气原色）（蜂窝信号）（网络状态）（电池） siguen funcionando y muestran iconos con el estilo de la barra de estado y el estado en tiempo real."
                        +"Fecha y hora: （时间）（日期）（星期）（周）（年月日）（12小时）（时段）（农历）（农历日期）（干支）（日期英文）（星期英文）；（格式:HH:mm {周}） permite personalizar el formato horario."
                        +"También se admiten paréntesis de ancho medio.",section,
                BigClockPresets.values(right),BigClockPresets.labels(right));
        b.add(BigClockWidgetsSettings.sizeKey(side),name+"Tamaño de fuente del componente","Solo cambia el tamaño del texto de este componente lateral.",NUMERIC,section,"dp",BigClockWidgetsSettings.MIN_SIZE,BigClockWidgetsSettings.MAX_SIZE,1f,null,null,null,null);
        b.add(BigClockWidgetsSettings.weightKey(side),name+"Grosor de fuente del componente","Solo cambia el grosor del texto de este lado; 400 es el valor predeterminado. Los iconos conservan la forma elegida para la barra de estado.",NUMERIC,section,"",BigClockWidgetsSettings.MIN_WEIGHT,BigClockWidgetsSettings.MAX_WEIGHT,1f,null,null,null,null);
        b.add(BigClockWidgetsSettings.offsetXKey(side),name+"Posición horizontal del componente","Desplazamiento horizontal respecto a la posición predeterminada de este lado; negativo mueve a la izquierda y positivo a la derecha.",NUMERIC,section,"dp",-BigClockWidgetsSettings.MAX_OFFSET,BigClockWidgetsSettings.MAX_OFFSET,1f,null,null,null,null);
        b.add(BigClockWidgetsSettings.offsetYKey(side),name+"Posición vertical del componente","Desplazamiento vertical respecto a la posición predeterminada debajo del reloj grande; negativo mueve hacia arriba y positivo hacia abajo.",NUMERIC,section,"dp",-BigClockWidgetsSettings.MAX_OFFSET,BigClockWidgetsSettings.MAX_OFFSET,1f,null,null,null,null);
        b.toggle(BigClockWidgetsSettings.followTimeColorKey(side),name+"Color del componente según la hora principal","Activado de forma predeterminada; sigue el color de la hora principal. Si esta muestra el fondo a través de los caracteres, también sigue el color de su máscara, con intensidad ajustable por separado y aspecto plano. Al desactivarlo, puedes configurar el color del componente por separado.",section);
        b.add(BigClockWidgetsSettings.maskOpacityKey(side),name+"Intensidad de la máscara del componente","Se aplica cuando este componente sigue a la hora principal y la hora tiene activada la transparencia de fondo dentro de los caracteres. Solo ajusta la intensidad de la máscara de color de este lado; 0 % conserva el fondo visible dentro de los caracteres.",NUMERIC,section,"%",0,100,1f,null,null,null,null);
        b.colorPair(BigClockWidgetsSettings.colorKey(side,false),BigClockWidgetsSettings.colorKey(side,true),name+"Color del componente",section);
        b.toggle(BigClockWidgetsSettings.clickEnabledKey(side),name+"Tocar el componente para abrir una aplicación","Desactivado de forma predeterminada. Al activarlo, tocar el texto de este lado abre la aplicación seleccionada.",section);
        b.text(BigClockWidgetsSettings.launchAppKey(side),name+"Aplicación que se abre al tocar el componente","Selecciona una aplicación para abrirla al tocar este componente; si está vacío, no se inicia ninguna aplicación.",section,null,null);
    }
    private static Group landscapeClock() {
        Group portrait = bigClock();
        Builder b = builder("notification_big_clock_landscape", "Reloj grande horizontal",
                "Solo ajusta la página de notificaciones en horizontal: reloj arriba, notificaciones abajo y botón de borrar a la derecha. Las notificaciones conservan su tamaño nativo; la hora se alinea con sus bordes. La parte superior puede desvanecerse y desenfocarse al deslizar hacia arriba, mientras que el texto inferior permanece nítido. Se guarda y activa de forma independiente del reloj grande vertical; desactivado de forma predeterminada.",
                NOTIFICATION, NotificationBigClockSettings.LANDSCAPE_MASTER);
        for (Item item : portrait.items) if (!item.master && !NotificationBigClockSettings.SCREEN_PADDING.equals(item.key)
                && !bigClockWidget(item.key)
                && !NotificationBigClockSettings.CLOCK_CLICK_ENABLED.equals(item.key)
                && !NotificationBigClockSettings.DATE_CLICK_ENABLED.equals(item.key)
                && !NotificationBigClockSettings.CLOCK_LAUNCH_APP.equals(item.key)
                && !NotificationBigClockSettings.DATE_LAUNCH_APP.equals(item.key)
                && !NotificationBigClockSettings.REBOUND_BLUR_ENABLED.equals(item.key)
                && !NotificationBigClockSettings.REBOUND_BLUR_RADIUS.equals(item.key)
                && !NotificationBigClockSettings.REBOUND_FADE.equals(item.key)
                && !NotificationBigClockSettings.REBOUND_LOCK.equals(item.key)
                && !HIDDEN.contains(NotificationBigClockSettings.landscapeKey(item.key))) b.add(
                NotificationBigClockSettings.landscapeKey(item.key), item.title, item.description, item.type,
                item.section, item.unit, item.min, item.max, item.step, item.values, item.labels,
                NotificationBigClockSettings.landscapeKey(item.pairedKey), item.scene);
        b.toggle(NotificationBigClockSettings.LANDSCAPE_NOTIFICATION_WIDTH_ENABLED,"Ajustar la longitud de las notificaciones","Solo cambia el ancho de las tarjetas de notificación en horizontal; el texto y los iconos conservan su tamaño nativo.","Distribución de las notificaciones");
        b.number(NotificationBigClockSettings.LANDSCAPE_NOTIFICATION_WIDTH,"Longitud de las notificaciones","Distribución de las notificaciones","%",50,150,1);
        return new Group(b);
    }
    private static Group clear() {
        Builder b = builder("notification_clear", "Posición del botón de borrar en vertical", "La posición, la animación de entrada, la distancia de seguridad de las notificaciones y el color del botón solo se aplican en vertical y conservan la acción nativa de borrar. Configura por separado la posición y el color horizontal en «Posición del botón de borrar en horizontal».", NOTIFICATION, NotificationClearAppearance.MASTER);
        b.toggle("notification_clear_motion_enabled", "Entrada y fundido independientes", "Ajusta solo la entrada y el cierre del botón de borrar; no depende del reloj grande.", "Animación y distancia de seguridad");
        b.number("notification_clear_safe_distance", "Con la distancia de seguridad de las notificaciones", "Animación y distancia de seguridad", "dp", 0, 160, .1f);
        b.number("notification_clear_entry_travel", "Desplazamiento de entrada", "Animación y distancia de seguridad", "dp", 0, 160, .1f);
        b.number("notification_clear_offset_y", "Posición vertical", "Animación y distancia de seguridad", "dp", -160, 160, .1f);
        b.colorPair(NotificationClearAppearance.COLOR_LIGHT, NotificationClearAppearance.COLOR_DARK, "Color de fondo del botón", "Color de fondo del botón");
        b.toggle(NotificationClearAppearance.GRADIENT_ENABLED, "Activar degradado", "", "Efecto de degradado");
        b.colorPair(NotificationClearAppearance.GRADIENT_COLOR_LIGHT, NotificationClearAppearance.GRADIENT_COLOR_DARK, "Color final del degradado", "Efecto de degradado");
        b.number(NotificationClearAppearance.OPACITY, "Transparencia del fondo", "Color de fondo del botón", "%", 0, 100, .01f);
        b.number(NotificationClearAppearance.GRADIENT_ANGLE, "Dirección del degradado", "Efecto de degradado", "°", 0, 360, .01f);
        b.add(NotificationClearAppearance.GRADIENT_RANGE, "Rango de transición", RANGE_BETWEEN, NUMERIC, "Efecto de degradado", "%", 5, 100, 1f, null, null, null, null);
        return new Group(b);
    }
    private static Group landscapeClear() {
        Builder b = builder("notification_clear_landscape", "Posición del botón de borrar en horizontal",
                "La posición y el color se guardan por separado de los ajustes verticales; el interruptor de posición no afecta al interruptor de color inferior. Sigue el movimiento, desvanecimiento y acción de borrado nativos de la página de notificaciones. Un desplazamiento excesivo puede solaparse con las notificaciones o salir de la pantalla. En la primera actualización se hereda el color que antes se compartía; después cada orientación se configura por separado.",
                NOTIFICATION, NotificationClearMotion.LANDSCAPE_MASTER);
        b.number(NotificationClearMotion.LANDSCAPE_OFFSET_X, "Posición horizontal", "Posición", "dp", -160, 160, .1f);
        b.number(NotificationClearMotion.LANDSCAPE_OFFSET_Y, "Posición vertical", "Posición", "dp", -160, 160, .1f);
        b.toggle(NotificationClearAppearance.LANDSCAPE_MASTER,"Color personalizado del botón","Solo funciona en horizontal y no depende del interruptor de posición; al desactivarlo se restaura el material nativo horizontal.","Color de fondo del botón");
        b.colorPair(NotificationClearAppearance.LANDSCAPE_COLOR_LIGHT,NotificationClearAppearance.LANDSCAPE_COLOR_DARK,"Color de fondo del botón","Color de fondo del botón");
        b.number(NotificationClearAppearance.LANDSCAPE_OPACITY,"Transparencia del fondo","Color de fondo del botón","%",0,100,.01f);
        b.toggle(NotificationClearAppearance.LANDSCAPE_GRADIENT_ENABLED,"Activar degradado","Solo funciona si está activado el color personalizado horizontal.","Efecto de degradado");
        b.colorPair(NotificationClearAppearance.LANDSCAPE_GRADIENT_COLOR_LIGHT,NotificationClearAppearance.LANDSCAPE_GRADIENT_COLOR_DARK,"Color final del degradado","Efecto de degradado");
        b.number(NotificationClearAppearance.LANDSCAPE_GRADIENT_ANGLE,"Dirección del degradado","Efecto de degradado","°",0,360,.01f);
        b.add(NotificationClearAppearance.LANDSCAPE_GRADIENT_RANGE,"Rango de transición",RANGE_BETWEEN,NUMERIC,"Efecto de degradado","%",5,100,1f,null,null,null,null);
        return new Group(b);
    }
    private static Group lockscreenDate() {
        Builder b=builder("lockscreen_date", "Personalizar la fecha de la pantalla de bloqueo",
                "Personaliza el texto de año, mes, día y semana de la pantalla de bloqueo, conservando la fuente, el tamaño y la frecuencia de actualización nativos. Solo afecta a la fecha de la pantalla de bloqueo, no a la hora del panel de notificaciones ni del centro de control. Desactivado de forma predeterminada.",
                LOCK_SCREEN, LockscreenControls.DATE_ENABLED);
        b.text(LockscreenControls.DATE_FORMAT,"Formato de fecha","Contenido de la fecha de la pantalla de bloqueo; por ejemplo, yyyy年M月d日 {星期}.","Contenido mostrado",
                new String[]{"M月d日 {周}","yyyy年M月d日 {星期}","yyyy/MM/dd EEEE","M d"},
                new String[]{"Día, mes y día de la semana","Año, mes, día y nombre completo del día","Fecha numérica y día de la semana","Mostrar solo el mes y el día"});
        return new Group(b);
    }
    private static Group lockscreenLock() {
        return new Group(builder("lockscreen_lock_icon", "Ocultar el candado pequeño de la pantalla de bloqueo",
                "Oculta el candado pequeño de la pantalla de bloqueo y el texto de carga que aparece junto a él. Solo modifica el dibujo: conserva la zona táctil de desbloqueo, el estado del sistema y la interacción con la huella. Desactivado de forma predeterminada.",
                LOCK_SCREEN, LockscreenControls.HIDE_LOCK));
    }
    private static Group lockscreenBlur() {
        Builder b=builder("lockscreen_status_blur", "Desenfoque de la zona de la barra de estado",
                "Desenfoca solo el fondo de pantalla de la parte superior de la pantalla de bloqueo y recupera gradualmente la nitidez hacia abajo. La hora, las notificaciones y los iconos de estado permanecen nítidos; el fondo desenfocado se almacena en caché y se desvanece durante el desbloqueo nativo y el apagado de la pantalla.",
                LOCK_SCREEN, LockscreenStatusBlur.ENABLED);
        b.number(LockscreenStatusBlur.RADIUS,"Intensidad del desenfoque","Zona de desenfoque","dp",0,40,.1f);
        b.number(LockscreenStatusBlur.RANGE,"Altura principal del desenfoque","Zona de desenfoque","dp",0,200,.1f);
        b.number(LockscreenStatusBlur.TRANSITION,"Altura de transición de desenfocado a nítido","Zona de desenfoque","dp",0,120,.1f);
        b.add(LockscreenStatusBlur.MASK_COLOR,"Máscara de color","Se desvanece hacia abajo y solo cubre el fondo de pantalla; si la transparencia del color es 0, no se muestra la máscara.",COLOR,"Máscara de color","",0,0,0,null,null,null,null);
        return new Group(b);
    }
    private static Group fluidAccent() {
        Builder b=builder("fluid_cloud_accent", "Color de énfasis multimedia de Fluid Cloud",
                "La cápsula multimedia de Fluid Cloud usa el color de énfasis detectado por el sistema; otras tarjetas pueden seguir el color del medio actual. Al desactivar el interruptor principal, se pausa el color multimedia de esta página. El degradado, la opacidad, el aclarado y el seguimiento de notificaciones en la pantalla de bloqueo y AOD se configuran por separado en «Pantalla de bloqueo → Bloqueo con pantalla encendida / AOD». Aquí solo se define el origen del color. Se aplica a las tarjetas flotantes expandidas, la pantalla de bloqueo y AOD, no al panel de notificaciones; esta última escena está desactivada.",
                OTHER, FluidCloudAccent.ENABLED);
        b.toggle(FluidCloudAccent.MASK_ENABLED,"Máscara del color de énfasis","Superpone un color al color multimedia; el color de énfasis y el color seguido comparten esta máscara.","Máscara");
        b.add(FluidCloudAccent.MASK_COLOR,"Color de la máscara","",COLOR,"Máscara","",0,0,0,null,null,null,null);
        b.add(FluidCloudAccent.MASK_OPACITY,"Opacidad de la máscara","Intensidad de la máscara superpuesta al color multimedia: 100 % opaca, 0 % transparente.",NUMERIC,"Máscara","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudAccent.OPACITY,"Opacidad del relleno","Intensidad del color de énfasis o del color seguido sobre la tarjeta: 100 % opaco y 0 % transparente. Si está activado «Personalización de bloqueo», usa los valores de pantalla de bloqueo de abajo.",NUMERIC,"Relleno de color","%",0,100,.1f,null,null,null,null);
        b.toggle(FluidCloudStyle.FOLLOW_MEDIA,"Seguir el color de énfasis multimedia","Las tarjetas que no tienen un color de énfasis propio usan el color del medio actual, con la máscara y opacidad de esta página; si no hay contenido multimedia, usan el color personalizado de «Estilo de Fluid Cloud». Primero debes activar allí el color personalizado de fondo. Si lo desactivas, también se desactiva el seguimiento. Los cambios de color (al cambiar de canción, detener la reproducción o volver al color del sistema) se funden suavemente, sin saltos repentinos.","Seguir el color multimedia");
        b.toggle(FluidCloudStyle.FOLLOW_ONLY_PLAYING,"Seguir solo durante la reproducción","Al pausar o detener el medio, deja de seguir el color y vuelve al fondo personalizado.","Seguir el color multimedia");
        return new Group(b);
    }
    /**
     * Lock screen with the screen on: how the fluid-cloud cards (music, stopwatch, countdown) and the ordinary notifications are coloured,
     * and the "unify style" switch that makes AOD and the notifications follow it. Where the colour comes from stays on the media-accent page.
     */
    private static Group lockAwake() {
        Builder b = new Builder("lock_awake", "Pantalla de bloqueo encendida",
                "Aspecto de la pantalla de bloqueo mientras la pantalla está encendida: cómo se colorean, degradan, aclaran y enmascaran las tarjetas de Fluid Cloud (música, cronómetro, temporizador, etc.), además de la pequeña cápsula inferior y de si las notificaciones normales (WeChat, SMS, etc.) siguen el color principal de la música. AOD se configura por separado en la página «AOD»; también puedes activar aquí «Estilo unificado» para igualar ambos. Esta página funciona de forma independiente y no requiere activar «Estilo de Fluid Cloud» ni «Color de énfasis multimedia de Fluid Cloud», que controlan otros escenarios, como las tarjetas flotantes del escritorio. Al activar «Degradado con pantalla encendida» o «Personalización con pantalla encendida», las tarjetas de la pantalla de bloqueo se tiñen con el color principal del medio. Para las notificaciones normales, usa «Seguir color multimedia en notificaciones» o «Estilo unificado».",
                LOCK_SCREEN, FluidCloudAccent.LOCK_GRADIENT, true, "Estilo de degradado con pantalla encendida", "La tarjeta de música usa un degradado desde el color principal superior hasta la transparencia en la pantalla de bloqueo encendida (el punto en que desaparece por completo se define con «Rango de transición»). Desactivado de forma predeterminada: toda la tarjeta usa un único relleno, igual que el cronómetro y las demás tarjetas.");
        b.toggle(LockNotificationSettings.UNIFY_AOD,"Estilo unificado","Al activarlo, las notificaciones normales de la pantalla de bloqueo y AOD se colorean como las tarjetas de Fluid Cloud: el color, el degradado y la intensidad coinciden por completo (en la pantalla encendida se usa el estilo de esta página; en AOD, el de la página «AOD»). Dejan de utilizarse el interruptor y la intensidad de seguimiento independientes. Si no hay música reproduciéndose, las notificaciones y las tarjetas de Fluid Cloud en AOD utilizan el fondo de vidrio neutro de las tarjetas de la pantalla encendida y la misma capa de color. Los estilos de la pantalla encendida y AOD se siguen configurando por separado y no se afectan entre sí. Al desactivarlo, las notificaciones usan sus propios ajustes de seguimiento. Desactivado de forma predeterminada.","Estilo unificado");
        b.add(FluidCloudAccent.GRADIENT_RANGE,"Rango de transición","El degradado de la tarjeta de música y de la cápsula inferior de la pantalla de bloqueo empieza desde arriba."+RANGE_FADE,
                NUMERIC,"Estilo con pantalla encendida","%",5,100,1f,null,null,null,null);
        b.choice(FluidCloudStyle.COLOR_STYLE,"Estilo de relleno de color","Define cómo se aplica el color a la tarjeta de música en la pantalla de bloqueo: «Color sólido» usa un único color en toda la tarjeta; «Degradado nativo» cambia los colores del degradado integrado y sustituye el degradado negro original (si está activado «Estilo de degradado con pantalla encendida», ya se utiliza un degradado). La cápsula inferior de la pantalla de bloqueo también sigue este ajuste. No afecta al cronómetro, a las tarjetas de comida a domicilio ni a las notificaciones normales.","Estilo con pantalla encendida",
                new String[]{"solid","gradient"},
                new String[]{"Color sólido","Degradado nativo"});
        b.toggle(FluidCloudStyle.LOCK_CAPSULE,"Colorear la cápsula inferior de la pantalla de bloqueo","Pequeña cápsula entre la parte inferior de la pantalla de bloqueo o AOD y los botones de linterna y cámara. Desactivada de forma predeterminada para conservar el aspecto original; al activarla, aplica los colores en este orden: color de énfasis → color multimedia seguido → fondo personalizado.","Estilo con pantalla encendida");
        b.toggle(FluidCloudAccent.LOCK_CUSTOM,"Personalización con pantalla encendida","Colorea la pantalla de bloqueo encendida mediante la opacidad y la máscara de abajo (si existe «Color de énfasis multimedia de Fluid Cloud», sustituye los ajustes compartidos de esa página). Al activarlo, las tarjetas usan el color principal del medio actual sin necesidad de activar primero esa función.","Personalización con pantalla encendida");
        b.add(FluidCloudAccent.LOCK_OPACITY,"Opacidad del relleno con pantalla encendida","Intensidad del color aplicado a las tarjetas con la pantalla encendida: 100 % opaco, 0 % transparente. Solo controla la transparencia; el tono se ajusta con «Aclarar el fondo con pantalla encendida».",NUMERIC,"Personalización con pantalla encendida","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudAccent.LOCK_MASK_COLOR,"Color de la máscara con pantalla encendida","",COLOR,"Personalización con pantalla encendida","",0,0,0,null,null,null,null);
        b.add(FluidCloudAccent.LOCK_MASK_OPACITY,"Opacidad de la máscara con pantalla encendida","Intensidad de la máscara sobre la pantalla de bloqueo encendida: 100 % opaca, 0 % transparente.",NUMERIC,"Personalización con pantalla encendida","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudAccent.LOCK_LIGHT,"Aclarar el fondo con pantalla encendida","El fondo de la pantalla de bloqueo es oscuro y las capas semitransparentes pueden volver el color grisáceo, muy distinto del vidrio claro al desbloquear o desplegar el panel. Aclarar hace que el color parezca colocado sobre blanco: 0 no aclara y 100 es el tono más claro. Solo cambia el tono, no la transparencia, que depende de la opacidad del relleno.",
                NUMERIC,"Personalización con pantalla encendida","%",0,100,1f,null,null,null,null);
        b.toggle(FluidCloudStyle.NOTIFICATION_FOLLOW,"Seguir el color multimedia en las notificaciones","Hace que las notificaciones normales de la pantalla de bloqueo y AOD sigan el color principal de la música, con el mismo degradado, transición de transparencia y brillo que las tarjetas de Fluid Cloud. Este interruptor controla las notificaciones normales por separado; la intensidad se ajusta abajo. Si se activa «Estilo unificado», las notificaciones adoptan el aspecto de las tarjetas y este interruptor deja de utilizarse.","Notificaciones");
        b.number(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY,"Intensidad de seguimiento","Notificaciones","%",0,100,.1f);
        return new Group(b);
    }
    /** Lock screen in AOD: the same look as the lit lock screen, set on its own; "unify style" on the lit page turns this page off. */
    private static Group lockAod() {
        Builder b = new Builder("lock_aod", "Pantalla siempre encendida (AOD)",
                "Aspecto de las tarjetas de Fluid Cloud y de las notificaciones normales en AOD, configurado por separado de la pantalla de bloqueo encendida. Al principio usa los mismos valores que la pantalla encendida, pero después cada modo puede ajustarse por separado. «Estilo unificado» en la página de pantalla encendida solo controla si las notificaciones normales adoptan el aspecto de Fluid Cloud; no afecta al resto de ajustes de AOD. Esta página funciona por sí sola y no requiere activar «Estilo de Fluid Cloud» ni «Color de énfasis multimedia de Fluid Cloud». Al activar «Estilo de degradado AOD» o «Personalización AOD», las tarjetas de AOD usan el color principal del medio actual. Las notificaciones normales dependen de «Seguir el color multimedia en las notificaciones» o «Estilo unificado» de la página de pantalla encendida; la intensidad se ajusta aquí.",
                LOCK_SCREEN, FluidCloudAccent.AOD_GRADIENT, true, "Estilo de degradado AOD", "En AOD, la tarjeta de música usa un degradado desde el color principal superior hasta la transparencia. Como el fondo de AOD es negro puro, la parte inferior del degradado deja ver el fondo negro; por eso está desactivado de forma predeterminada y toda la tarjeta usa un único color.");
        b.toggle(AodNotificationBackground.ENABLED,"Fondo de vidrio blanco para notificaciones AOD","Solo en AOD, añade un efecto de vidrio blanco suave al fondo de las notificaciones normales compatibles, las notificaciones de Fluid Cloud y las tarjetas multimedia. Conserva el desenfoque, los colores y detalles del fondo de pantalla, la transparencia, las esquinas redondeadas y las transiciones, y permite seguir viendo el fondo. El texto, los iconos y el contenido mantienen los colores del sistema. Funciona de forma independiente y no afecta a la pantalla de bloqueo encendida; al desactivarlo, se restaura el fondo original. Desactivado de forma predeterminada.","Fondo de las notificaciones");
        b.toggle(FluidCloudAccent.AOD_CUSTOM,"Personalización AOD","AOD utiliza la opacidad y la máscara de abajo para aplicar color (si se usa «Color de énfasis multimedia de Fluid Cloud», sustituye los ajustes compartidos de aquella página). Al activarlo, las tarjetas de AOD usan el color principal de la música sin necesidad de activar esa función.","Personalización AOD");
        b.add(FluidCloudAccent.AOD_OPACITY,"Opacidad del relleno AOD","Intensidad del color aplicado a las tarjetas AOD: 100 % opaco, 0 % transparente. Solo controla la transparencia; el tono se ajusta con «Aclarar el fondo AOD».",NUMERIC,"Personalización AOD","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudAccent.AOD_MASK_COLOR,"Color de la máscara AOD","",COLOR,"Personalización AOD","",0,0,0,null,null,null,null);
        b.add(FluidCloudAccent.AOD_MASK_OPACITY,"Opacidad de la máscara AOD","Intensidad de la máscara sobre AOD: 100 % opaca, 0 % transparente.",NUMERIC,"Personalización AOD","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudAccent.AOD_LIGHT,"Aclarar el fondo AOD","Aclara el color para que parezca colocado sobre blanco: 0 no aclara y 100 es el tono más claro. Solo cambia el tono, no la transparencia.",
                NUMERIC,"Personalización AOD","%",0,100,1f,null,null,null,null);
        b.number(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY,"Intensidad de seguimiento de notificaciones AOD","Notificaciones","%",0,100,.1f);
        return new Group(b);
    }
    /** Camera-area appearance plus an independent persistence switch shared by its tabs. */
    private static Group fluidCapsule() {
        Builder b=builder("fluid_cloud_capsule", "Fluid Cloud alrededor de la cámara",
                "Cápsula de Fluid Cloud alrededor de la cámara (la cápsula negra que aparece expandida o contraída para música, temporizadores, llamadas y otras actividades). Tiene tres páginas: fondo (color, degradado y desenfoque), borde (borde, luces en movimiento y desvanecimiento hacia dentro) y forma y contenido (esquinas redondeadas, portada del álbum, rebote de entrada y espectro en tiempo real). Al desactivar el interruptor general se restaura el aspecto original; la opción de mantener Fluid Cloud visible controla por separado el cierre automático.",
                OTHER, FluidCloudCapsule.MASTER);
        b.toggle(CapsulePersistence.ENABLED,"Mantener Fluid Cloud visible","Al activarlo, la cápsula de Fluid Cloud alrededor de la cámara no se contrae automáticamente tras un tiempo de inactividad, aunque puedes cerrarla manualmente. El sistema la elimina normalmente al finalizar la música o la tarea. Es independiente del interruptor general de apariencia de esta página.","Reglas de visualización");
        b.toggle(FluidCloudCapsule.FILL_ENABLED,"Color de fondo personalizado","La cápsula utiliza el color y la transparencia de abajo en lugar del negro nativo. Si el fondo es demasiado claro, el texto blanco de la cápsula puede resultar difícil de leer.","Color de fondo");
        b.add(FluidCloudCapsule.FILL_COLOR,"Color de fondo","",COLOR,"Color de fondo","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.FILL_OPACITY,"Opacidad del fondo","100 % completamente opaco; 0 % completamente transparente.",NUMERIC,"Color de fondo","%",0,100,.1f,null,null,null,null);
        b.toggle(FluidCloudCapsule.FOLLOW_MEDIA,"Seguir el color de énfasis multimedia","Cuando hay contenido multimedia, la cápsula usa el color del medio actual y aplica la máscara de «Color de énfasis multimedia de Fluid Cloud»; la intensidad se define con la opacidad del fondo de esta página. Si no hay contenido multimedia, usa el fondo personalizado o el negro nativo. El cambio de color se desvanece gradualmente en unos 0,6 segundos.","Color de fondo");
        b.toggle(FluidCloudCapsule.GRADIENT_ENABLED,"Degradado de fondo","El fondo pasa del color superior (o del color multimedia) al color de degradado inferior. Primero debes activar el fondo personalizado o el seguimiento del color de énfasis multimedia.","Degradado");
        b.choice(FluidCloudCapsule.GRADIENT_MODE,"Dirección del degradado","De arriba abajo, de abajo arriba o desde el centro de la cápsula hacia los lados.","Degradado",
                new String[]{FluidCloudCapsule.TOP_BOTTOM,FluidCloudCapsule.BOTTOM_TOP,FluidCloudCapsule.CENTER},
                new String[]{"De arriba abajo","De abajo arriba","Del centro hacia los lados"});
        b.add(FluidCloudCapsule.GRADIENT_COLOR,"Color del degradado","",COLOR,"Degradado","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.GRADIENT_RANGE,"Rango de transición",RANGE_BETWEEN,
                NUMERIC,"Degradado","%",5,100,1f,null,null,null,null);
        b.toggle(FluidCloudCapsule.BLUR_ENABLED,"Desenfoque del fondo","Convierte la cápsula en vidrio esmerilado: desenfoca lo que hay detrás y superpone una capa de «Color de máscara». Al activarlo, el color de fondo lo determina la máscara y las opciones de fondo personalizado, degradado y seguimiento del color multimedia quedan deshabilitadas. El borde, la luz en movimiento y las esquinas redondeadas siguen funcionando. Depende del desenfoque de fondo de ventanas del sistema; si no es compatible o está desactivado, se conserva el fondo original.","Desenfoque");
        b.number(FluidCloudCapsule.BLUR_RADIUS,"Intensidad del desenfoque","Desenfoque","dp",0,FluidCloudCapsule.MAX_BLUR_RADIUS,.5f);
        b.add(FluidCloudCapsule.BLUR_MASK_COLOR,"Color de la máscara","Color que se superpone al fondo desenfocado; la transparencia propia del color determina la intensidad. Cuando el desenfoque está activado, el fondo personalizado, el degradado y el seguimiento del color multimedia dejan de aplicarse.",COLOR,"Desenfoque","",0,0,0,null,null,null,null);
        b.toggle(FluidCloudCapsule.BORDER_ENABLED,"Borde personalizado","La cápsula muestra siempre este borde (el sistema nativo solo tiene un borde muy tenue cuando está contraída).","Borde");
        b.add(FluidCloudCapsule.BORDER_COLOR,"Color del borde","",COLOR,"Borde","",0,0,0,null,null,null,null);
        b.number(FluidCloudCapsule.BORDER_WIDTH,"Grosor del borde","Borde","dp",0,FluidCloudCapsule.MAX_BORDER_WIDTH,.1f);
        b.toggle(FluidCloudCapsule.MARQUEE_ENABLED,"Luz en movimiento del borde","Sustituye el borde normal por una luz multicolor que gira alrededor de la cápsula. Si el borde personalizado está activado, usa su grosor; de lo contrario, usa el valor predeterminado del sistema de 1,3 dp. Solo se anima mientras la cápsula está visible, a unos 30 fotogramas por segundo.","Luz en movimiento");
        b.choice(FluidCloudCapsule.MARQUEE_PRESET,"Preajuste","Hay veinte preajustes con colores incluidos. «Flujo monocromático» usa el color 1 para crear una estela que se desvanece; «Personalizado» utiliza los colores de abajo (de 2 a 6 colores).","Luz en movimiento",
                FluidCloudCapsule.MARQUEE_PRESETS,
                FluidCloudCapsule.MARQUEE_PRESET_LABELS);
        b.number(FluidCloudCapsule.MARQUEE_COLOR_COUNT,"Cantidad de colores personalizados","Luz en movimiento","unidades",FluidCloudCapsule.MIN_MARQUEE_COLORS,FluidCloudCapsule.MAX_MARQUEE_COLORS,1f);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_1,"Color 1","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_2,"Color 2","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_3,"Color 3","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_4,"Color 4","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_5,"Color 5","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_COLOR_6,"Color 6","",COLOR,"Luz en movimiento","",0,0,0,null,null,null,null);
        b.add(FluidCloudCapsule.MARQUEE_OPACITY,"Opacidad","100 % completamente opaco; 0 % completamente transparente.",NUMERIC,"Luz en movimiento","%",0,100,.1f,null,null,null,null);
        b.number(FluidCloudCapsule.MARQUEE_PERIOD,"Duración de una vuelta","Luz en movimiento","segundos",FluidCloudCapsule.MIN_MARQUEE_PERIOD,FluidCloudCapsule.MAX_MARQUEE_PERIOD,.1f);
        b.toggle(FluidCloudCapsule.MARQUEE_REVERSE,"Girar en sentido contrario","Por defecto gira en sentido horario; al activarlo, gira en sentido antihorario.","Luz en movimiento");
        b.toggle(FluidCloudCapsule.GLOW_ENABLED,"Desvanecer hacia dentro","El halo se desvanece gradualmente hacia el interior de la cápsula, creando un resplandor suave cuyo color gira junto con la luz del borde.","Desvanecer hacia dentro");
        b.add(FluidCloudCapsule.GLOW_DEPTH,"Rango de transición","El resplandor empieza en el borde y se desvanece hacia dentro."+RANGE_DISTANCE,
                NUMERIC,"Desvanecer hacia dentro","dp",FluidCloudCapsule.MIN_GLOW_DEPTH,FluidCloudCapsule.MAX_GLOW_DEPTH,.5f,null,null,null,null);
        b.toggle(FluidCloudCapsule.RADIUS_ENABLED,"Esquinas redondeadas personalizadas","Cuando la cápsula está expandida, el fondo, el borde y la luz en movimiento usan el mismo radio de esquina. La cápsula mide unos 30 dp de alto; un radio de 15 dp o más produce una forma completamente ovalada, igual que la nativa. Los radios menores crean esquinas más cuadradas. Al contraerse alrededor del recorte de la cámara, conserva la forma nativa y vuelve suavemente al radio elegido durante la expansión.","Forma");
        b.number(FluidCloudCapsule.RADIUS,"Radio de esquina","Forma","dp",0,FluidCloudCapsule.MAX_RADIUS,.5f);
        b.toggle(FluidCloudCapsule.COVER_RADIUS_ENABLED,"Esquinas personalizadas para la portada","La portada del álbum, a la izquierda de la cápsula musical, es circular de forma nativa. Al activar esta opción, la cápsula expandida usa el radio de esquina de abajo. Al contraerse, la portada queda junto al recorte de la cámara y conserva su forma circular; al expandirse, cambia suavemente. Solo afecta a la cápsula musical; los iconos de temporizadores, llamadas y otras cápsulas no cambian. Al desactivarlo, se restaura el círculo.","Portada del álbum");
        b.add(FluidCloudCapsule.COVER_RADIUS,"Radio de esquina de la portada","0 crea esquinas cuadradas; 10 dp se aproxima al círculo nativo.",NUMERIC,"Portada del álbum","dp",0,FluidCloudCapsule.MAX_COVER_RADIUS,.5f,null,null,null,null);
        b.toggle(FluidCloudCapsule.COVER_SIZE_ENABLED,"Ajustar el tamaño de la portada musical","Solo cambia la portada de música cuando la cápsula está expandida. El tamaño de la cápsula, el texto y los demás iconos no cambian; al contraerse, se restaura el tamaño del sistema.","Portada del álbum");
        b.add(FluidCloudCapsule.COVER_SCALE,"Tamaño de la portada musical","100 % equivale al tamaño del sistema; al expandirse y contraerse, sigue la animación nativa.",NUMERIC,"Portada del álbum","%",FluidCloudCapsule.MIN_COVER_SCALE,FluidCloudCapsule.MAX_COVER_SCALE,1f,null,null,null,null);
        b.toggle(CapsuleEffects.BOUNCE_ENABLED,"Rebote de aparición personalizado","El efecto nativo amplía brevemente la cápsula unos 16 dp de ancho y 3 dp de alto cuando aparece, antes de volver a su tamaño normal. Al activar esta opción, se utiliza la intensidad de abajo; al desactivarla, se restaura el comportamiento original. Se aplica la próxima vez que aparezca la cápsula y no cambia su posición alrededor de la cámara.","Rebote de aparición");
        b.add(CapsuleEffects.BOUNCE,"Intensidad del rebote","0 % sin rebote; 100 % reproduce el efecto original del sistema; máximo 200 %.",NUMERIC,"Rebote de aparición","%",0,CapsuleEffects.MAX_BOUNCE,1f,null,null,null,null);
        b.toggle(FluidCloudCapsule.SPECTRUM_ENABLED,"Espectro en tiempo real","Sustituye la animación fija del espectro musical de la cápsula por una visualización que reacciona en tiempo real a la música. Solo funciona cuando la cápsula está visible y se reproduce música; no consume energía el resto del tiempo. Depende de la visualización de audio del sistema; si no es compatible, conserva la animación original.","Espectro en tiempo real");
        b.add(FluidCloudCapsule.SPECTRUM_SENSITIVITY,"Sensibilidad del espectro","Ajusta la sensibilidad a tu gusto: en 0, las barras apenas se mueven y solo suben con golpes de batería claros, hasta algo más de un tercio de la altura; en 100, son más altas y activas y pueden llegar al borde superior. La altura sigue el rango de volumen reciente de cada banda, por lo que no se mantienen siempre al máximo. Con volumen bajo, el conjunto también se verá más bajo.",
                NUMERIC,"Espectro en tiempo real","%",0,100,1f,null,null,null,null);
        b.toggle(FluidCloudCapsule.SPECTRUM_FOLLOW,"El espectro sigue el color de énfasis","Las barras del espectro usan el color del medio actual (el color principal de la portada musical, aclarado si es demasiado oscuro para verse). Si coincide con el fondo de la cápsula, se usa un color oscuro o blanco. Deja de usar el color fijo de abajo; si el color multimedia no está disponible temporalmente, se conserva el color inferior.","Espectro en tiempo real");
        b.add(FluidCloudCapsule.SPECTRUM_COLOR,"Color del espectro","",COLOR,"Espectro en tiempo real","",0,0,0,null,null,null,null);
        return new Group(b);
    }
    private static Group fluidStyle() {
        Builder b=builder("fluid_cloud_style", "Estilo de Fluid Cloud",
                "Estilo del sistema para las notificaciones y fondo de Fluid Cloud en tarjetas flotantes expandidas, pantalla de bloqueo y AOD. Selecciona las categorías de actividad compatibles; solo controla el estilo de esta página. Las tarjetas no identificadas se incluyen en «Otros o no identificados». El color de énfasis y el seguimiento del color multimedia se configuran por separado y tienen prioridad sobre este fondo.",
                OTHER, FluidCloudStyle.MASTER);
        b.toggle(FluidCloudStyle.FOLLOW_NOTIFICATION,"Seguir el estilo del sistema para las notificaciones","El panel de notificaciones sigue las tarjetas de notificación, las notificaciones flotantes siguen el material de las ventanas flotantes y la pantalla de bloqueo conserva el material de los widgets del sistema.","Notificaciones");
        b.toggle(FluidCloudStyle.SCOPE_FLOAT,"Vista flotante expandida","Tarjetas de Fluid Cloud que flotan sobre el escritorio.","Escenario de aplicación");
        b.toggle(FluidCloudStyle.SCOPE_LOCK,"Pantalla de bloqueo y AOD","La pantalla de bloqueo y AOD mantienen el mismo color, sin cambios bruscos al entrar o salir de AOD.","Escenario de aplicación");
        b.toggle(FluidCloudStyle.DESKTOP_BACKGROUND,"Color de fondo personalizado","Solo cambia el color y la transparencia; conserva el desenfoque y las animaciones nativas.","Color de fondo");
        b.choice(FluidCloudStyle.BACKGROUND_MODE,"Modo de fondo","El degradado nativo conserva el desenfoque, el material, la forma y la animación del sistema, y permite ajustar el rango de transición. El fondo sólido personalizado usa el color seleccionado. El color de énfasis multimedia y el seguimiento del color multimedia tienen prioridad.","Color de fondo",
                new String[]{"native","custom"},
                new String[]{"Degradado nativo","Fondo sólido personalizado"});
        b.add(FluidCloudStyle.BACKGROUND_COLOR,"Color personalizado","",COLOR,"Color de fondo","",0,0,0,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_OPACITY,"Opacidad del relleno","100 % completamente opaco; 0 % completamente transparente.",NUMERIC,"Color de fondo","%",0,100,.1f,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_GRADIENT_START,"Inicio del degradado","Distancia desde la parte superior de la tarjeta hasta el inicio del cambio de color; antes de ese punto se mantiene el color inicial. 0 % empieza arriba del todo. Solo afecta al degradado nativo de las tarjetas flotantes expandidas; la pantalla de bloqueo y AOD usan sus propios ajustes.",NUMERIC,"Degradado","%",0,98,.1f,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE,"Color final del degradado","Distancia hasta el final del cambio de color; después se mantiene el color final. 100 % conserva la longitud del degradado del sistema; los valores menores hacen que termine antes y más rápido.",NUMERIC,"Degradado","%",1,100,.1f,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_GRADIENT_MID,"Punto medio del degradado","Posición entre el inicio y el final donde el color ha cambiado a la mitad. 50 % produce una transición uniforme; un valor menor adelanta el cambio y uno mayor lo retrasa.",NUMERIC,"Degradado","%",1,99,.1f,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_GRADIENT_STRENGTH,"Intensidad del degradado","Diferencia entre el color inicial y el final: 100 % reproduce el sistema; los valores menores aplanan el degradado, 0 % deja toda la tarjeta con el color inicial y sin degradado, y los valores mayores aumentan el contraste y aclaran el color final.",NUMERIC,"Degradado","%",0,200,.1f,null,null,null,null);
        b.add(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,"Transparencia del degradado","Solo vuelve transparente la capa del degradado; no afecta al fondo ni al desenfoque. 100 % reproduce el sistema. A diferencia de «Opacidad del relleno», esta opción no afecta a todo el fondo.",NUMERIC,"Degradado","%",0,100,.1f,null,null,null,null);
        b.toggle(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,"Conservar los reflejos de las tarjetas multimedia","Conserva los reflejos nativos de la tarjeta musical; primero debes activar «Eliminar reflejos de C17».","Reflejos");
        String[] scopeIds={"media","ride_hailing","food_delivery","parcel_pickup","food_pickup","timer","alarm","navigation","travel","battery","earphones","smart_suggestions","other"};
        String[] scopeTitles={"Contenido multimedia","Transporte por aplicación de viajes","Comida a domicilio","Códigos de recogida y paquetería","Código de pedido y recogida en tienda","Cuenta atrás","Alarma","Navegación","Transporte","Batería baja y carga","Auriculares","Sugerencias contextuales","Otros o no identificados"};
        for(int index=0;index<scopeIds.length;index++)b.toggle("fluid_cloud_scope_"+scopeIds[index]+"_enabled",scopeTitles[index],
                "other".equals(scopeIds[index])?"Fluid Cloud para otras tareas y categorías que aún no se identifican. Al desactivarlo, se deja de ajustar el estilo de estas tarjetas."
                        :"smart_suggestions".equals(scopeIds[index])?"Sugerencias del sistema como activar Wi-Fi al llegar a casa o al trabajo, silenciar durante una película o apagar la linterna. Al desactivarlo, se deja de ajustar el estilo de estas tarjetas."
                        :"battery".equals(scopeIds[index])?"Avisos de batería baja, ahorro de energía y carga en Fluid Cloud. Al desactivarlo, se deja de ajustar el estilo de estas tarjetas."
                        :"earphones".equals(scopeIds[index])?"Fluid Cloud cuando se conectan los auriculares. Al desactivarlo, se deja de ajustar el estilo de estas tarjetas."
                        :"Al desactivarlo, se deja de ajustar el estilo de estas tarjetas de Fluid Cloud; las tarjetas no identificadas se agrupan en «Otros o no identificados».","Ámbito de compatibilidad");
        return new Group(b);
    }
    private static Group wholeStack() {
        Builder b=builder("notification_stack", "Notificaciones apiladas", "Usa el apilado nativo del sistema, que se expande y escala automáticamente al desplazarse por la lista. En vertical debes activar el reloj grande vertical; en horizontal, el reloj grande horizontal y «Aplicar también en horizontal». Si se desactiva el reloj grande correspondiente, esa orientación vuelve automáticamente a las notificaciones nativas y conserva los ajustes guardados. Los grupos de aplicaciones siguen usando las acciones táctiles del sistema.", NOTIFICATION, NotificationBigClockSettings.STACK_ENABLED);
        b.toggle(FeatureOptions.STACK_LANDSCAPE_ENABLED,"Aplicar también en horizontal","Después de activar las notificaciones apiladas, también debes activar el reloj grande horizontal para permitir el apilado en horizontal. Desactivado de forma predeterminada.","Reglas de visualización");
        b.toggle(NotificationMiniStack.AUTO_HIDE_IN_STACK,"Ocultar automáticamente el apilado pequeño","Cuando el apilado de toda la columna cubre las capas del apilado pequeño de la misma aplicación, oculta las capas posteriores y las restaura al expandirse. Requiere activar tanto las notificaciones apiladas como el apilado pequeño por aplicación. Se pausa si cualquiera de los modos se desactiva, pero se conserva esta opción.","Reglas de visualización");
        b.number(NotificationBigClockSettings.VISIBLE_COUNT,"Cantidad de notificaciones completas","Reglas de visualización","unidades",1,5,1);
        return new Group(b);
    }
    /** Same-app mini stack: an independent page, not tied to either big clock, the whole-list stack or the fluid cloud. */
    private static Group miniStack() {
        Builder b = new Builder("notification_mini_stack", "Apilado pequeño por aplicación", NotificationMiniStackSource.SHORT
                + ". Cuando el sistema agrupa las notificaciones de una misma aplicación, el estado contraído muestra completa la primera notificación y deja ver hasta dos capas de notificaciones reales detrás. Al tocar la primera, se expande todo el grupo,"
                + "El sistema sigue controlando la expansión, contracción, deslizamiento lateral y eliminación. Desactivado de forma predeterminada; tras activar o desactivar la función, reinicia la interfaz del sistema desde la página principal para aplicar todos los cambios.",
                NOTIFICATION, NotificationMiniStack.ENABLED, true, NotificationMiniStackSource.FEATURE,
                "Solo afecta a los grupos de notificaciones nativos (dos o más notificaciones de una misma aplicación). Las notificaciones individuales o no agrupadas no cambian. La primera vez que se activa, se muestra un aviso sobre el origen de la función.");
        b.toggle(NotificationMiniStack.HIDE_BADGE, "Ocultar la cantidad de indicadores nativos", "Oculta el contador del apilado pequeño y los restos de su animación al contraerse; al desactivarlo, restaura el contador. Los grupos expandidos conservan el comportamiento del sistema.", "Reglas de visualización");
        return new Group(b);
    }
    private static Group nativeStatusIcons() {
        Builder b=builder("status_hint_icons", "Iconos de aviso del sistema", "Ajusta por separado la cantidad, el espaciado y la prioridad de los iconos de aviso nativos, como Bluetooth, ubicación y alarmas. La posición izquierda/derecha y el orden de cada icono se configuran en la página de orden de la barra de estado. Esta página no controla los iconos de notificación, Wi-Fi, señal móvil, batería ni velocidad de red.", STATUSBAR, NativeStatusIcons.MASTER);
        b.number(NativeStatusIcons.Y,"Posición vertical","Posición","dp",-16,16,.1f);
        layoutMargins(b, StatusBarLayoutSettings.SYSTEM, "Espaciado lateral");
        b.number(NativeStatusIcons.MAX,"Cantidad máxima visible","Reglas de visualización","unidades",0,14,1);
        b.add(NativeStatusIcons.SPACING,"Espaciado adicional","0 conserva el espaciado del sistema; los valores positivos lo aumentan. Solo mueve los iconos de aviso, no la red ni la batería. Un espaciado o desplazamiento demasiado grande puede sacarlos de la pantalla.",NUMERIC,"Posición","dp",0,12,.1f,null,null,null,null);
        b.text(NativeStatusIcons.PRIORITY,"Prioridad de visualización","Mantén pulsado y arrastra los nombres de los iconos para ordenarlos; los primeros tienen prioridad para conservarse. Solo se muestran los iconos que el sistema permite mostrar en ese momento.","Reglas de visualización",
                new String[]{"","bluetooth,location,alarm_clock","location,microphone,camera,bluetooth","zen,alarm_clock,bluetooth"},
                new String[]{"Seguir al sistema","Prioridad de Bluetooth","Prioridad de ubicación y privacidad","Prioridad de No molestar y alarmas"});
        return new Group(b);
    }
    private static Group media() {
        Builder b = builder("qs_media", "Tarjeta musical", "Configura por separado para interfaces claras y oscuras el fondo de la portada, el desenfoque y el resplandor exterior de la miniatura. Al cambiar la portada, muestra inmediatamente un color sólido muestreado de la nueva imagen. Cuando no haya nuevas actualizaciones durante aproximadamente un segundo, calcula el fondo desenfocado en segundo plano y lo hace aparecer suavemente.", CONTROL_CENTER, QsMediaAppearance.MASTER);
        b.toggle(QsMediaAppearance.BACKGROUND, "Usar la portada como fondo de la tarjeta", "Recorte centrado; conserva el título y los controles de reproducción nativos.", "Efecto visual");
        b.toggle(QsMediaAppearance.GLOW, "Resplandor exterior de la miniatura", "Conservar el vidrio y los reflejos del sistema", "Efecto visual");
        b.number(QsMediaAppearance.TRANSITION, "Duración de la transición de la portada", "Transición", "ms", 0, QsMediaAppearance.MAX_TRANSITION_MS, 10f);
        b.number(QsMediaAppearance.SETTLE, "Tiempo de espera tras cambiar la portada", "Transición", "ms", 0, QsMediaAppearance.MAX_SETTLE_MS, 50f);
        for (String scene : new String[]{"light", "dark"}) {
            String s = "light".equals(scene) ? "Interfaz clara" : "Interfaz oscura", p = QsMediaAppearance.prefix(scene);
            b.number(p + "_background_opacity", "Transparencia del fondo", s + " · Fondo", "%", 0, 100, .01f);
            b.number(p + "_background_blur", "Intensidad del desenfoque del fondo", s + " · Fondo", "dp", 0, 48, .01f);
            b.toggle(p + "_background_solid_enabled", "Usar el color principal de la portada como fondo", "No muestra la imagen de la portada; usa solo un color sólido muestreado de ella. Sigue sujeto a la transparencia del fondo.", s + " · Fondo");
            b.toggle(p + "_background_custom_enabled", "Usar un color de fondo personalizado", "Tiene prioridad sobre el color principal de la portada; si no hay portada, conserva el aspecto original del sistema.", s + " · Fondo");
            b.number(p + "_background_tint_opacity", "Intensidad del color superpuesto al fondo", s + " · Fondo", "%", 0, 100, .01f);
            b.toggle(p + "_glow_inverse_enabled", "Usar el color invertido de la portada para el resplandor", "Al desactivarlo, se usa el color de resplandor personalizado.", s + " · Resplandor exterior");
            b.number(p + "_glow_opacity", "Transparencia del resplandor", s + " · Resplandor exterior", "%", 0, 100, .01f);
            b.number(p + "_glow_radius", "Radio de desenfoque del resplandor", s + " · Resplandor exterior", "dp", 0, 48, .01f);
            b.number(p + "_glow_spread", "Distancia de expansión del resplandor", s + " · Resplandor exterior", "dp", 0, 24, .01f);
        }
        b.colorPair("qs_media_light_glow_color", "qs_media_dark_glow_color", "Color de resplandor personalizado", "Color del resplandor");
        b.colorPair("qs_media_light_background_color", "qs_media_dark_background_color", "Color de fondo personalizado", "Color de fondo");
        b.colorPair("qs_media_light_background_tint_color", "qs_media_dark_background_tint_color", "Color superpuesto al fondo", "Color superpuesto al fondo");
        return new Group(b);
    }

    private static Group highlightRemoval() {
        Builder b = builder("c17_highlight_removal", "Eliminar reflejos de C17",
                "Elimina los reflejos de borde, el brillo interior y los destellos de los materiales nativos de las notificaciones y del centro de control. Conserva el desenfoque, las esquinas redondeadas y la interacción nativa para dar a la superficie un aspecto más acrílico. Se pueden configurar fondos claros y oscuros por separado. Está desactivado de forma predeterminada.",
                OTHER, C17HighlightRemoval.ENABLED);
        b.toggle(C17HighlightRemoval.NOTIFICATION_ENABLED, "Eliminar reflejos del panel de notificaciones", "Solo afecta a los reflejos de la página y las tarjetas de notificación. Al desactivarlo, se restaura el aspecto nativo.", "Ámbito de aplicación");
        b.toggle(C17HighlightRemoval.HEADS_UP_ENABLED, "Eliminar reflejos de las notificaciones flotantes", "Elimina los reflejos y bordes del aviso flotante que aparece en la parte superior de la pantalla. Conserva el desenfoque, el contenido y las acciones táctiles. Es independiente del ámbito del panel desplegable.", "Ámbito de aplicación");
        b.toggle(C17HighlightRemoval.CONTROL_ENABLED, "Eliminar reflejos del panel de la barra de estado", "Afecta al centro de control de la derecha, los mosaicos y la tarjeta de música. Al desactivarlo, se restaura el aspecto nativo.", "Ámbito de aplicación");
        b.toggle(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, "Unificar el color de las tarjetas de notificación", "Se puede usar de forma independiente. Configura el fondo claro u oscuro que aparece abajo; conserva el desenfoque y el contenido nativos. Añade una composición de fondo adicional, así que actívalo solo si lo necesitas.", "Color de las notificaciones");
        b.toggle(C17HighlightRemoval.BACKGROUND_ENABLED, "Fondo acrílico personalizado",
                "Aplica un fondo uniforme a los materiales comunes. Los colores personalizados de los mosaicos y el fondo de la portada musical conservan sus propios ajustes. Al desactivarlo, solo se eliminan los reflejos.", "Color de fondo");
        b.colorPair(C17HighlightRemoval.LIGHT_BACKGROUND, C17HighlightRemoval.DARK_BACKGROUND,
                "Fondo acrílico", "Color y transparencia");
        return new Group(b);
    }

    public static List<Group> groups() { return GROUPS; }
    public static List<Group> groups(String category) {
        List<Group> result = new ArrayList<>();
        for (NavigationSection section : navigationSections(category)) result.addAll(section.groups);
        return Collections.unmodifiableList(result);
    }
    public static List<NavigationSection> navigationSections(String category) {
        // Each page appears once, where its effect is seen; network entries follow their on-screen order.
        // The per-surface carrier text and the shared pull-down clock are the only deliberate repeats.
        if (STATUSBAR.equals(category)) return Arrays.asList(
                new NavigationSection("Orden", StatusBarLayoutSettings.GROUP),
                new NavigationSection("Hora y batería", "clock", "battery"),
                new NavigationSection("Señal de red", "speed", "data", "wifi", "label", "native_network_badge"),
                new NavigationSection("Iconos de notificación", "notification_icons", "notification_icon_overrides"),
                new NavigationSection("Iconos del sistema", "status_hint_icons"));
        if (CONTROL_CENTER.equals(category)) return Arrays.asList(
                new NavigationSection("Mosaicos y controles deslizantes", "tile_2x1_fill", "tile_corners", "tile_icon_size", "qs_appearance"),
                new NavigationSection("Tarjeta musical", "qs_media"),
                new NavigationSection("Hora y texto superiores", "shade_clock", "carrier"),
                new NavigationSection("Botones superiores", QsEntrances.GROUP));
        if (NOTIFICATION.equals(category)) return Arrays.asList(
                new NavigationSection("Reloj y fecha", "notification_big_clock", "notification_big_clock_landscape", "shade_clock"),
                new NavigationSection("Apilado de notificaciones", "notification_stack", "notification_mini_stack"),
                new NavigationSection("Botón de borrar", "notification_clear", "notification_clear_landscape"),
                new NavigationSection("Texto de la interfaz", "carrier", "classic_text"));
        if (OTHER.equals(category)) return Arrays.asList(
                new NavigationSection("Iconos", "status_icon_library"),
                new NavigationSection("Fuentes", "font"),
                new NavigationSection("Fluid Cloud", "fluid_cloud_accent", "fluid_cloud_style", "fluid_cloud_capsule"),
                new NavigationSection("Fondo del panel desplegable", "shade_wallpaper_portrait", "shade_wallpaper_landscape"),
                new NavigationSection("Apariencia del panel", "c17_highlight_removal"));
        if (LOCK_SCREEN.equals(category)) return Arrays.asList(new NavigationSection("Fecha y texto", "lockscreen_date", "carrier"),
                new NavigationSection("Iconos de aviso", "lockscreen_lock_icon"),
                new NavigationSection("Fondo de pantalla y desenfoque", "lockscreen_status_blur"),
                new NavigationSection("Pantalla encendida y AOD", "lock_awake", "lock_aod"));
        return Collections.emptyList();
    }
    public static List<DetailPage> detailPages(Group group) {
        if (group == null) return Collections.emptyList();
        LinkedHashMap<String, List<Item>> buckets = new LinkedHashMap<>();
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        if (group.id.startsWith("notification_big_clock")) {
            labels.put("time", "Hora"); labels.put("look", "Apariencia"); labels.put("date", "Fecha");
            labels.put("widgets", "Componentes izquierdo y derecho");
            labels.put("motion", "Animaciones y notificaciones"); labels.put("footer", "Parte inferior");
        } else if ("carrier".equals(group.id)) {
            labels.put("notification", "Panel de notificaciones"); labels.put("control", "Centro de control"); labels.put("lockscreen", "Pantalla de bloqueo");
        } else if ("classic_text".equals(group.id)) {
            labels.put("carrier", "Operador"); labels.put("clock", "Hora y fecha");
        } else if ("native_network_badge".equals(group.id)) {
            labels.put("part1", "Indicador 1"); labels.put("part2", "Indicador 2"); labels.put("activity", "Flecha de datos");
        } else if ("battery".equals(group.id)) {
            labels.put("style", "Estilo"); labels.put("text", "Texto interior"); labels.put("layout", "Diseño"); labels.put("colors", "Color");
        } else if ("qs_media".equals(group.id) || "qs_appearance".equals(group.id)) {
            labels.put("common", "General"); labels.put("light", "Interfaz clara"); labels.put("dark", "Interfaz oscura");
        } else if ("clock".equals(group.id)) {
            labels.put("default", "Reloj predeterminado"); labels.put("dual", "Reloj de dos filas"); labels.put("common", "Ajustes generales");
        } else if ("fluid_cloud_capsule".equals(group.id)) {
            labels.put("fill","Color de fondo"); labels.put("border","Borde"); labels.put("content","Forma y contenido");
        } else if ("fluid_cloud_style".equals(group.id)) {
            labels.put("notification","Notificaciones");labels.put("appearance","Apariencia de Fluid Cloud");labels.put("scope","Ámbito de compatibilidad");
        } else if (group.id.startsWith("shade_wallpaper")) {
            labels.put("notification", "Centro de notificaciones"); labels.put("control", "Centro de control"); labels.put("classic", "Panel desplegable clásico");
        } else labels.put("all", "Todo");
        for (String id : labels.keySet()) buckets.put(id, new ArrayList<>());
        Set<String> shared = new HashSet<>();
        for (Item item : commonDetailItems(group)) shared.add(item.key);
        for (Item item : group.items) if (!item.master && !shared.contains(item.key))
            buckets.get(detailPageId(group, item)).add(item);
        List<DetailPage> pages = new ArrayList<>();
        boolean wallpaper = group.id.startsWith("shade_wallpaper");
        for (List<Item> items : buckets.values()) items.sort((left, right) -> wallpaper
                ? Integer.compare(wallpaperSectionPriority(left.section), wallpaperSectionPriority(right.section))
                : Integer.compare(sectionPriority(left.section), sectionPriority(right.section)));
        for (Map.Entry<String, List<Item>> entry : buckets.entrySet()) if (!entry.getValue().isEmpty())
            pages.add(new DetailPage(entry.getKey(), labels.get(entry.getKey()), entry.getValue()));
        return Collections.unmodifiableList(pages);
    }
    /** A search result may temporarily reveal another saved panel scene without changing the system mode. */
    public static List<DetailPage> editorDetailPages(Group group, String panelMode, String requestedKey) {
        List<DetailPage> pages = detailPages(group);
        if (group == null || !group.id.startsWith("shade_wallpaper")) return pages;
        List<DetailPage> result = new ArrayList<>();
        for (DetailPage page : pages) {
            boolean requested = false;
            for (Item item : page.items) if (item.key.equals(requestedKey)) { requested = true; break; }
            if (requested || "classic".equals(page.id) == PanelMode.CLASSIC.equals(panelMode)) result.add(page);
        }
        return Collections.unmodifiableList(result);
    }
    public static String detailModeReason(Group group, DetailPage page, String panelMode) {
        if (group == null || page == null || !group.id.startsWith("shade_wallpaper")) return "";
        if (!PanelMode.CLASSIC.equals(panelMode) && !PanelMode.SEPARATE.equals(panelMode))
            return "No se ha confirmado el modo del panel del sistema. Aquí se muestra la configuración guardada; se aplicará según el modo real.";
        if ("classic".equals(page.id) == PanelMode.CLASSIC.equals(panelMode)) return "";
        return PanelMode.CLASSIC.equals(panelMode)
                ? "El modo actual es clásico. Los fondos independientes del centro de notificaciones y del centro de control están pausados; se restaurarán con los valores guardados al volver al modo separado."
                : "El modo actual es separado. El fondo desplegable clásico está pausado; se restaurará con los valores guardados al volver al modo clásico.";
    }
    /** The old shared carrier switch remains portable but has no editor; three scene switches replace it. */
    public static List<Item> searchItems(Group group) {
        if (group == null) return Collections.emptyList();
        if (!"carrier".equals(group.id)) return group.items;
        List<Item> result = new ArrayList<>();
        for (Item item : group.items) if (!item.key.equals(group.masterKey)) result.add(item);
        return Collections.unmodifiableList(result);
    }
    public static List<Item> commonDetailItems(Group group) {
        if (group != null && ("wifi".equals(group.id) || "label".equals(group.id))) {
            Item shared = ITEM_BY_KEY.get(StatusBarLayoutSettings.WIFI_LABEL_SHARED);
            return shared == null ? Collections.emptyList() : Collections.singletonList(shared);
        }
        if (group != null && "carrier".equals(group.id)) {
            String token = StatusBarLayoutSettings.CARRIER;
            List<Item> shared = new ArrayList<>();
            Item left = ITEM_BY_KEY.get(StatusBarLayoutSettings.key(token, "margin_left"));
            Item right = ITEM_BY_KEY.get(StatusBarLayoutSettings.key(token, "margin_right"));
            if (left != null) shared.add(left);
            if (right != null) shared.add(right);
            return Collections.unmodifiableList(shared);
        }
        if (group != null && "fluid_cloud_capsule".equals(group.id)) {
            Item persistent = ITEM_BY_KEY.get(CapsulePersistence.ENABLED);
            return persistent == null ? Collections.emptyList() : Collections.singletonList(persistent);
        }
        if (group != null && group.id.startsWith("shade_wallpaper")) {
            boolean landscape = "shade_wallpaper_landscape".equals(group.id);
            List<Item> shared = new ArrayList<>();
            Item pull = ITEM_BY_KEY.get(ShadeWallpaperSettings.blurWithPullKey(landscape)), slide = ITEM_BY_KEY.get(ShadeWallpaperSettings.swipeSlideKey(landscape));
            if (pull != null) shared.add(pull);
            if (slide != null) shared.add(slide);
            return Collections.unmodifiableList(shared);
        }
        return Collections.emptyList();
    }
    /** One panel's tab reads top to bottom: the picture, how it is shown, the clock zone. */
    private static int wallpaperSectionPriority(String section) {
        if ("Imagen de fondo".equals(section)) return 0;
        if ("Mostrar".equals(section)) return 10;
        if ("Área del reloj grande".equals(section)) return 20;
        return 50;
    }
    private static int sectionPriority(String section) {
        String title = section.contains(" · ") ? section.substring(section.indexOf(" · ") + 3) : section;
        if ("Transparencia del fondo y máscara".equals(title)) return -10;
        if ("Borde con relieve de la hora principal".equals(title)) return 0;
        if ("Fecha".equals(title) || "Contenido inferior".equals(title)) return 0;
        if ("Posición de la fecha".equals(title) || "Posición inferior".equals(title)) return 10;
        if ("Estilo del fondo de la fecha".equals(title) || "Estilo del fondo del contenido inferior".equals(title)) return 30;
        if ("Posición inicial".equals(title)) return -1;
        if ("Textura de pantalla de bloqueo".equals(title)) return 5;
        if ("Vidrio líquido".equals(title)) return 15;
        if ("Borde de vidrio".equals(title)) return 25;
        if ("Contenido y fuente".equals(title)) return 0;
        if ("Tamaño y grosor".equals(title)) return 20;
        if ("Posición y alineación".equals(title)) return 30;
        if (Arrays.asList("Estilo de visualización", "Formato de hora", "Origen de la fuente", "Efecto visual", "Distribución de la señal", "Estilo de batería", "Panel de notificaciones", "Centro de control", "Pantalla de bloqueo", "Color de fondo", "Contenido").contains(title)) return 0;
        if ("Posición".equals(title)) return 10;
        if ("Espaciado lateral".equals(title)) return 11;
        if ("Tamaño".equals(title) || "Altura de la fuente".equals(title)) return 20;
        if ("Estilo del texto".equals(title) || "Texto y espaciado".equals(title)) return 30;
        if (title.contains("Color") || "Color y transparencia".equals(title)) return 50;
        if ("Estilo contraído".equals(title) || "Posición contraída".equals(title)) return 60;
        return 40;
    }
    /** The panel a wallpaper setting belongs to: its tab. */
    private static String wallpaperPanel(String key) {
        if (key.contains("_control_")) return "control";
        if (key.contains("_classic_")) return "classic";
        return "notification";
    }
    private static String detailPageId(Group group, Item item) {
        if (group.id.startsWith("shade_wallpaper")) return wallpaperPanel(item.key);
        String section = item.section;
        if("fluid_cloud_style".equals(group.id))return FluidCloudScope.isScopeKey(item.key)?"scope"
                :FluidCloudStyle.FOLLOW_NOTIFICATION.equals(item.key)?"notification":"appearance";
        if("fluid_cloud_capsule".equals(group.id))
            return Arrays.asList("Color de fondo","Degradado","Desenfoque").contains(section)?"fill":Arrays.asList("Borde","Luz en movimiento","Desvanecer hacia dentro").contains(section)?"border":"content";
        if ("clock".equals(group.id) && (ClockLayoutSettings.digitKey("clock").equals(item.key)
                || ClockLayoutSettings.digitSpacingKey("clock").equals(item.key) || ClockLayoutSettings.colonKey("clock").equals(item.key)
                || StatusBarLayoutSettings.key(StatusBarLayoutSettings.CLOCK,"margin_left").equals(item.key)
                || StatusBarLayoutSettings.key(StatusBarLayoutSettings.CLOCK,"margin_right").equals(item.key))) return "common";
        if ("clock".equals(group.id)) return item.key.startsWith("clock_dual_") || ClockLayoutSettings.MARGIN_ENABLED.equals(item.key)
                || ClockLayoutSettings.MARGIN_START.equals(item.key) || ClockLayoutSettings.MARGIN_END.equals(item.key) ? "dual" : "default";
        if (group.id.startsWith("notification_big_clock")) {
            if (bigClockWidget(item.key)) return "widgets";
            if (section.startsWith("Fecha")) return "date";
            if (section.startsWith("Notificaciones") || "Efecto de entrada".equals(section) || "Efecto de rebote".equals(section)) return "motion";
            if (section.startsWith("Parte inferior")) return "footer";
            if (section.startsWith("Vidrio") || "Textura de pantalla de bloqueo".equals(section) || "Vidrio líquido".equals(section) || "Color del texto".equals(section)
                    || "Transparencia del fondo y máscara".equals(section) || "Borde con relieve de la hora principal".equals(section)) return "look";
            return "time";
        }
        if ("carrier".equals(group.id)) {
            if (item.key.startsWith(CarrierPanels.CONTROL + "_")) return "control";
            if (item.key.startsWith(CarrierPanels.LOCKSCREEN + "_")) return "lockscreen";
            return "notification";
        }
        if ("classic_text".equals(group.id)) return item.key.startsWith(ClassicTextSettings.CLOCK + "_") ? "clock" : "carrier";
        if ("native_network_badge".equals(group.id)) return item.key.startsWith(NativeNetworkBadgeSettings.PART_ONE+"_") ? "part1"
                : item.key.startsWith(NativeNetworkBadgeSettings.PART_TWO+"_") ? "part2" : "activity";
        if ("battery".equals(group.id)) {
            if ("Texto dentro de la batería".equals(section)) return "text";
            if (COLOR.equals(item.type) || item.key.endsWith("_color_enabled")) return "colors";
            if ("Posición".equals(section) || "Espaciado lateral".equals(section) || "Tamaño".equals(section)) return "layout";
            return "style";
        }
        if ("qs_media".equals(group.id) || "qs_appearance".equals(group.id)) {
            if (!item.scene.isEmpty()) return item.scene;
            if (section.startsWith("Claro")) return "light";
            if (section.startsWith("Oscuro")) return "dark";
            return "common";
        }
        return "all";
    }
    public static Group group(String id) { return GROUP_BY_ID.get(id); }
    public static List<Item> items(String groupId) {
        Group group = group(groupId);
        return group == null ? Collections.emptyList() : group.items;
    }
    public static Item item(String key) { return ITEM_BY_KEY.get(key); }
    /** Unavailable controls are disabled by the editor; experimental controls remain opt-in. */
    public static String unavailableReason(String key) {
        return ShadeWallpaperSettings.unavailableReason(key);
    }
    /** Saved conflicting values are retained; their effects resume only when the conflict ends. */
    /** The value a greyed numeric item shows: the one that is actually in force while it is unavailable (null: the stored value). */
    public static Float inactiveNumber(String key, Map<String,?> values) {
        if(FluidCloudCapsule.BORDER_WIDTH.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.BORDER_ENABLED))
            return FluidCloudCapsule.DEFAULT_BORDER_WIDTH;
        return null;
    }



    private static boolean lockStyleStrength(String portraitKey) {
        return NotificationBigClockSettings.LOCK_STYLE_NATIVE.equals(portraitKey)||NotificationBigClockSettings.LOCK_STYLE_BODY.equals(portraitKey)||NotificationBigClockSettings.LOCK_STYLE_GRADIENT.equals(portraitKey)
                ||NotificationBigClockSettings.LOCK_STYLE_RIM.equals(portraitKey)||NotificationBigClockSettings.LOCK_STYLE_RIM_DEPTH.equals(portraitKey)
                ||NotificationBigClockSettings.LOCK_STYLE_ROUND.equals(portraitKey)||NotificationBigClockSettings.LOCK_STYLE_SHADE.equals(portraitKey)
                ||NotificationBigClockSettings.LOCK_STYLE_GLOW.equals(portraitKey)||NotificationBigClockSettings.LOCK_STYLE_ANGLE.equals(portraitKey);
    }
    /** The plugin's own glass material and its border, replaced while the lock-screen style is on. */
    private static boolean plainGlass(String portraitKey) {
        return NotificationBigClockSettings.GLASS.equals(portraitKey)||NotificationBigClockSettings.GLASS_BORDER_ENABLED.equals(portraitKey)
                ||NotificationBigClockSettings.GLASS_BORDER_WIDTH.equals(portraitKey)||NotificationBigClockSettings.GLASS_BORDER_COLOR_LIGHT.equals(portraitKey)
                ||NotificationBigClockSettings.GLASS_BORDER_COLOR_DARK.equals(portraitKey);
    }
    private static boolean backdropMaterialControl(String key) {
        String base=key!=null&&key.endsWith("_custom_alpha")?key.substring(0,key.length()-"_custom_alpha".length()):key;
        String clock=NotificationBigClockSettings.portraitKey(base);
        return plainGlass(clock)||NotificationBigClockSettings.LOCK_STYLE.equals(clock)||lockStyleStrength(clock)
                ||NotificationBigClockSettings.COLOR_LIGHT.equals(clock)||NotificationBigClockSettings.COLOR_DARK.equals(clock);
    }
    public static String unavailableReason(String key, Map<String,?> values) {
        String shelved = ShadeWallpaperSettings.unavailableReason(key);
        if (!shelved.isEmpty()) return shelved;
        if(BigClockWidgetsSettings.SWAP_WITH_DATE.equals(key)) {
            if(!StatusBarSettings.bool(values,NotificationBigClockSettings.DATE_ENABLED)
                    ||!(StatusBarSettings.settingNumber(values,NotificationBigClockSettings.DATE_SIZE,15f)>0f))return "Primero, activa una fecha válida.";
            if(!StatusBarSettings.bool(values,BigClockWidgetsSettings.LEFT_ENABLED)
                    &&!StatusBarSettings.bool(values,BigClockWidgetsSettings.RIGHT_ENABLED))return "Primero, activa al menos un componente lateral.";
        }
        if(bigClockWidget(key)) {
            int side=BigClockWidgetsSettings.textSide(key);
            String parent=side>=0&&!BigClockWidgetsSettings.enabledKey(side).equals(key)?BigClockWidgetsSettings.enabledKey(side):null;
            if(parent!=null&&!StatusBarSettings.bool(values,parent))return BigClockWidgetsSettings.LEFT_ENABLED.equals(parent)
                    ?"Primero, activa la visualización del componente izquierdo.":"Primero, activa la visualización del componente derecho.";
        }
        if (StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order").equals(key)
                && StatusBarSettings.bool(values, StatusBarLayoutSettings.WIFI_LABEL_SHARED))
            return "Comparte el orden con Wi-Fi. Ajusta la zona y el orden desde la configuración de Wi-Fi.";
        if ((StatusBarLayoutSettings.sideKey(key) || StatusBarLayoutSettings.orderKey(key))
                && !StatusBarSettings.bool(values, StatusBarLayoutSettings.MASTER)) return "Primero, activa el orden de la barra de estado.";
        if ((DataBatterySpacing.ENABLED.equals(key) || DataBatterySpacing.SPACING.equals(key))
                && StatusBarLayoutSettings.enabledValues(values))
            return "El orden de la barra de estado o los márgenes laterales controlan ahora la distribución. Los valores anteriores se conservan; para restaurarlos, desactiva el orden y devuelve todos los márgenes laterales a 0.";
        if (StatusBarSettings.SPEED_OFFSET_X.equals(key)
                && StatusBarSettings.bool(values, SpeedPosition.SAFE_GAP_ENABLED))
            return "El modo de distancia controla ahora la posición horizontal. Desactiva «Mantener distancia con los iconos adyacentes» para ajustar la posición. Se conservan los valores horizontales anteriores.";
        String modeReason = PanelMode.freezeReason(key);
        if (modeReason != null && !modeReason.isEmpty()) return modeReason;
        if(NetworkLabelFormats.owns(key)&&!StatusBarSettings.bool(values,NetworkLabelFormats.ENABLED))
            return "Primero, activa el contenido personalizado del tipo de red.";
        String clockKey=NotificationBigClockSettings.portraitKey(key);
        String backdropSwitch=key!=null&&key.startsWith(NotificationBigClockSettings.LANDSCAPE_PREFIX)
                ?NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED):NotificationBigClockSettings.DIGIT_REVEAL_ENABLED;
        if(ClockBackdropSettings.owns(key)&&!StatusBarSettings.bool(values,backdropSwitch))return "Primero, activa el fondo visible a través del texto.";
        if((ClockBackdropSettings.DATE_MASK_OPACITY.equals(clockKey)||ClockBackdropSettings.FOOTER_MASK_OPACITY.equals(clockKey))
                &&!StatusBarSettings.bool(values,clockKey.equals(key)?NotificationBigClockSettings.FOLLOW_STYLE:NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.FOLLOW_STYLE)))
            return "Primero, activa la opción para que la hora y la fecha sigan el mismo estilo.";
        if(BigClockWidgetsSettings.maskOpacityKey(key)) {
            if(!StatusBarSettings.bool(values,BigClockWidgetsSettings.followTimeColorKey(BigClockWidgetsSettings.textSide(key))))return "Primero, activa la opción para que el color del componente siga el de la hora principal.";
            if(!StatusBarSettings.bool(values,NotificationBigClockSettings.DIGIT_REVEAL_ENABLED))return "Primero, activa el fondo visible a través del texto en orientación vertical.";
        }
        if(ClockBackdropSettings.NOTIFICATION_FOLLOW.equals(key)||ClockBackdropSettings.NOTIFICATION_RELIEF.equals(key))
            return "Se ha eliminado el material de fondo del texto de las notificaciones. Los ajustes anteriores se conservan solo por compatibilidad.";
        if(BigClockWidgetsSettings.launchAppKey(key)&&!StatusBarSettings.bool(values,BigClockWidgetsSettings.clickEnabledKey(BigClockWidgetsSettings.textSide(key))))
            return "Primero, activa la opción para abrir una aplicación al tocar el componente.";
        if(backdropMaterialControl(key)&&StatusBarSettings.bool(values,backdropSwitch))
            return "Está activado el fondo visible a través del texto y se usa un material de fondo específico. Al desactivarlo, se restaura el aspecto anterior.";
        if ((NotificationBigClockSettings.FOLLOW_STYLE.equals(clockKey)
                ||NotificationBigClockSettings.REVEAL_MASK_COLOR.equals(clockKey)
                ||NotificationBigClockSettings.REVEAL_MASK_OPACITY.equals(clockKey))
                &&!StatusBarSettings.bool(values,clockKey.equals(key)?NotificationBigClockSettings.DIGIT_REVEAL_ENABLED
                :NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED)))
            return "Primero, activa el fondo visible a través del texto.";
        String styleSwitch=clockKey.equals(key)?NotificationBigClockSettings.LOCK_STYLE
                :NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.LOCK_STYLE);
        if(lockStyleStrength(clockKey)&&!StatusBarSettings.bool(values,styleSwitch))return "Primero, activa el efecto de textura de la pantalla de bloqueo.";
        if(plainGlass(clockKey)&&StatusBarSettings.bool(values,styleSwitch))return "Esta opción ha sido sustituida por el efecto de textura de la pantalla de bloqueo. Estará disponible cuando se desactive ese efecto.";
        if(NotificationBigClockSettings.CLOCK_LAUNCH_APP.equals(key)
                &&!StatusBarSettings.bool(values,NotificationBigClockSettings.CLOCK_CLICK_ENABLED))
            return "Primero, activa la opción para abrir el reloj al tocar la hora.";
        if(NotificationBigClockSettings.DATE_LAUNCH_APP.equals(key)
                &&!StatusBarSettings.bool(values,NotificationBigClockSettings.DATE_CLICK_ENABLED))
            return "Primero, activa la opción para abrir el calendario al tocar la fecha.";
        if((NotificationBigClockSettings.REBOUND_BLUR_ENABLED.equals(key)||NotificationBigClockSettings.REBOUND_BLUR_RADIUS.equals(key)
                ||NotificationBigClockSettings.REBOUND_FADE.equals(key))
                &&StatusBarSettings.bool(values,NotificationBigClockSettings.REBOUND_LOCK))
            return "Está activada la opción «Desactivar el efecto de rebote». Sin rebote no hay desenfoque durante el rebote; ambas opciones son incompatibles. Desactiva primero esta opción.";
        if((NotificationBigClockSettings.REBOUND_BLUR_RADIUS.equals(key)||NotificationBigClockSettings.REBOUND_FADE.equals(key))
                &&!StatusBarSettings.bool(values,NotificationBigClockSettings.REBOUND_BLUR_ENABLED))
            return "Primero, activa el desenfoque durante el rebote.";
        boolean unified=StatusBarSettings.bool(values,LockNotificationSettings.UNIFY_AOD);
        if(unified&&(FluidCloudStyle.NOTIFICATION_FOLLOW.equals(key)||FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY.equals(key)
                ||LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY.equals(key)))
            return "Está activado el estilo unificado: las notificaciones adoptan los colores de las tarjetas de Fluid Cloud, por lo que se ignoran este interruptor y su intensidad.";
        if((FluidCloudAccent.AOD_OPACITY.equals(key)||FluidCloudAccent.AOD_MASK_COLOR.equals(key)||FluidCloudAccent.AOD_MASK_OPACITY.equals(key))
                &&!StatusBarSettings.bool(values,FluidCloudAccent.AOD_CUSTOM))
            return "Primero, activa la personalización de AOD.";
        String wallpaper=wallpaperReason(key,values);
        if(!wallpaper.isEmpty())return wallpaper;
        if(NotificationIconArea.FOLLOW_LAUNCHER.equals(key)
                &&!"native".equals(StatusBarSettings.string(values,NotificationIconArea.MODE)))
            return "Solo se aplica al modo de visualización de iconos de notificación nativos.";
        if(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT.equals(key)
                &&!StatusBarSettings.bool(values,C17HighlightRemoval.ENABLED))
            return "Primero, activa la eliminación de reflejos en «Materiales de la interfaz → Eliminar reflejos de C17». Esta opción solo determina si se conservan en la tarjeta de música.";
        // The media-accent page has one master. Everything on it, following included, is greyed while it is off.
        if(fluidAccentChild(key)&&!StatusBarSettings.bool(values,FluidCloudAccent.ENABLED))
            return "Primero, activa el color de énfasis multimedia de Fluid Cloud.";
        if((FluidCloudAccent.MASK_COLOR.equals(key)||FluidCloudAccent.MASK_OPACITY.equals(key))
                &&!StatusBarSettings.bool(values,FluidCloudAccent.MASK_ENABLED))
            return "Primero, activa la máscara del color de énfasis.";
        boolean customBase=StatusBarSettings.bool(values,FluidCloudStyle.MASTER)&&StatusBarSettings.bool(values,FluidCloudStyle.DESKTOP_BACKGROUND);
        if(FluidCloudStyle.FOLLOW_MEDIA.equals(key)&&!customBase)
            return "Primero, activa el fondo personalizado en «Estilo de Fluid Cloud».";
        // "Only while playing" also governs the lock screen's and AOD's own following, which needs neither master.
        if(FluidCloudStyle.FOLLOW_ONLY_PLAYING.equals(key)&&!lockOwnFollowing(values)
                &&!(StatusBarSettings.bool(values,FluidCloudStyle.FOLLOW_MEDIA)&&customBase))
            return "Primero, activa el seguimiento del color de énfasis multimedia o una de estas opciones de la pantalla de bloqueo/AOD: notificaciones que siguen el color, estilo unificado, estilo degradado o personalización.";
        if(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY.equals(key)
                &&!StatusBarSettings.bool(values,FluidCloudStyle.NOTIFICATION_FOLLOW))
            return "Primero, activa la opción para que las notificaciones sigan el color multimedia.";
        if((FluidCloudAccent.LOCK_OPACITY.equals(key)||FluidCloudAccent.LOCK_MASK_COLOR.equals(key)
                ||FluidCloudAccent.LOCK_MASK_OPACITY.equals(key))
                &&!StatusBarSettings.bool(values,FluidCloudAccent.LOCK_CUSTOM))
            return "Primero, activa la personalización de la pantalla de bloqueo.";
        if(fluidStyleChild(key)&&!StatusBarSettings.bool(values,FluidCloudStyle.MASTER))
            return "Primero, activa el estilo de Fluid Cloud.";
        if(capsuleChild(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.MASTER))
            return "Primero, activa Fluid Cloud en el área de la cámara.";
        if(glassOverridesFill(key)&&StatusBarSettings.bool(values,FluidCloudCapsule.BLUR_ENABLED))
            return "Cuando se activa el desenfoque del fondo, el color de fondo lo determina la máscara; esta opción deja de tener efecto.";
        if(FluidCloudCapsule.BLUR_MASK_COLOR.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.BLUR_ENABLED))
            return "Primero, activa el desenfoque del fondo.";
        if(FluidCloudCapsule.GLOW_ENABLED.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.MARQUEE_ENABLED))
            return "Primero, activa el borde luminoso animado.";
        if(FluidCloudCapsule.GLOW_DEPTH.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.GLOW_ENABLED))
            return "Primero, activa el desvanecimiento hacia dentro.";
        if((FluidCloudCapsule.SPECTRUM_COLOR.equals(key)||FluidCloudCapsule.SPECTRUM_FOLLOW.equals(key))
                &&!StatusBarSettings.bool(values,FluidCloudCapsule.SPECTRUM_ENABLED))
            return "Primero, activa el espectro en tiempo real.";
        if(FluidCloudCapsule.MARQUEE_COLOR_COUNT.equals(key)&&!"custom".equals(StatusBarSettings.string(values,FluidCloudCapsule.MARQUEE_PRESET)))
            return "Solo se utiliza con el ajuste preestablecido «Personalizado».";
        for(int i=3;i<FluidCloudCapsule.MARQUEE_COLOR_KEYS.length;i++)
            if(FluidCloudCapsule.MARQUEE_COLOR_KEYS[i].equals(key)&&"custom".equals(StatusBarSettings.string(values,FluidCloudCapsule.MARQUEE_PRESET))
                    &&i>=Math.round(StatusBarSettings.settingNumber(values,FluidCloudCapsule.MARQUEE_COLOR_COUNT,FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_COUNT)))
                return "La cantidad de colores personalizados es de solo "+Math.round(StatusBarSettings.settingNumber(values,FluidCloudCapsule.MARQUEE_COLOR_COUNT,FluidCloudCapsule.DEFAULT_MARQUEE_COLOR_COUNT))+" colores";
        if(FluidCloudCapsule.FILL_COLOR.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.FILL_ENABLED))
            return "Primero, activa el fondo personalizado.";
        if(FluidCloudCapsule.FILL_OPACITY.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.FILL_ENABLED)
                &&!StatusBarSettings.bool(values,FluidCloudCapsule.FOLLOW_MEDIA))
            return "Primero, activa el fondo personalizado o el seguimiento del color de énfasis multimedia.";
        if(FluidCloudCapsule.BORDER_COLOR.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.BORDER_ENABLED))
            return "Primero, activa el borde personalizado.";
        // The running light alone draws at the plugin's own outline width; only the custom border makes the width adjustable.
        if(FluidCloudCapsule.BORDER_WIDTH.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.BORDER_ENABLED))
            return StatusBarSettings.bool(values,FluidCloudCapsule.MARQUEE_ENABLED)
                    ?"Sin un borde personalizado, el efecto luminoso usa el grosor predeterminado del sistema de 1,3 dp. Activa el borde personalizado para ajustarlo."
                    :"Primero, activa el borde personalizado.";
        if(FluidCloudCapsule.GRADIENT_ENABLED.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.FILL_ENABLED)
                &&!StatusBarSettings.bool(values,FluidCloudCapsule.FOLLOW_MEDIA))
            return "Primero, activa el fondo personalizado o el seguimiento del color de énfasis multimedia.";
        if((FluidCloudCapsule.GRADIENT_MODE.equals(key)||FluidCloudCapsule.GRADIENT_COLOR.equals(key))
                &&!StatusBarSettings.bool(values,FluidCloudCapsule.GRADIENT_ENABLED))
            return "Primero, activa el fondo degradado.";
        if(FluidCloudCapsule.BLUR_RADIUS.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.BLUR_ENABLED))
            return "Primero, activa el desenfoque del fondo.";
        if(marqueeChild(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.MARQUEE_ENABLED))
            return "Primero, activa el borde luminoso animado.";
        if(FluidCloudCapsule.MARQUEE_COLOR_1.equals(key)&&!"comet".equals(StatusBarSettings.string(values,FluidCloudCapsule.MARQUEE_PRESET))
                &&!"custom".equals(StatusBarSettings.string(values,FluidCloudCapsule.MARQUEE_PRESET)))
            return "El ajuste preestablecido actual ya incluye colores. Selecciona «Flujo monocromático» o «Personalizado» para modificarlos.";
        if((FluidCloudCapsule.MARQUEE_COLOR_2.equals(key)||FluidCloudCapsule.MARQUEE_COLOR_3.equals(key))
                &&!"custom".equals(StatusBarSettings.string(values,FluidCloudCapsule.MARQUEE_PRESET)))
            return "Este color solo se utiliza con el ajuste preestablecido «Personalizado».";
        if(FluidCloudCapsule.SPECTRUM_SENSITIVITY.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.SPECTRUM_ENABLED))
            return "Primero, activa el espectro en tiempo real.";
        if(FluidCloudCapsule.RADIUS.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.RADIUS_ENABLED))
            return "Primero, activa las esquinas redondeadas personalizadas.";
        if(FluidCloudCapsule.COVER_RADIUS.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.COVER_RADIUS_ENABLED))
            return "Primero, activa las esquinas redondeadas personalizadas de la portada.";
        if(FluidCloudCapsule.COVER_SCALE.equals(key)&&!StatusBarSettings.bool(values,FluidCloudCapsule.COVER_SIZE_ENABLED))
            return "Primero, activa el ajuste del tamaño de la portada musical.";
        if(CapsuleEffects.BOUNCE.equals(key)&&!StatusBarSettings.bool(values,CapsuleEffects.BOUNCE_ENABLED))
            return "Primero, activa el rebote de entrada personalizado.";
        boolean gradientShape=FluidCloudStyle.BACKGROUND_GRADIENT_RANGE.equals(key)||FluidCloudStyle.BACKGROUND_GRADIENT_START.equals(key)
                ||FluidCloudStyle.BACKGROUND_GRADIENT_MID.equals(key)||FluidCloudStyle.BACKGROUND_GRADIENT_STRENGTH.equals(key)
                ||FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY.equals(key);
        if(FluidCloudStyle.BACKGROUND_MODE.equals(key)||FluidCloudStyle.BACKGROUND_COLOR.equals(key)
                ||FluidCloudStyle.BACKGROUND_OPACITY.equals(key)||gradientShape) {
            if(!StatusBarSettings.bool(values,FluidCloudStyle.MASTER))return "Primero, activa el estilo de Fluid Cloud.";
            if(!StatusBarSettings.bool(values,FluidCloudStyle.DESKTOP_BACKGROUND))return "Primero, activa el fondo personalizado.";
            if(gradientShape
                    &&!"native".equals(StatusBarSettings.string(values,FluidCloudStyle.BACKGROUND_MODE)))
                return "Selecciona el modo de fondo degradado nativo.";
            if(FluidCloudStyle.BACKGROUND_COLOR.equals(key)
                    &&"notification".equals(StatusBarSettings.string(values,FluidCloudStyle.BACKGROUND_MODE)))
                return "Cuando se siguen las notificaciones del sistema, se usa el color del sistema. Selecciona «Predeterminado del sistema» o «Color personalizado» para editarlo.";
        }
        if(ClockLayoutSettings.STRINGS.containsKey(key)||ClockLayoutSettings.NUMBERS.containsKey(key)||ClockLayoutSettings.COLORS.containsKey(key)) {
            for(String group:ClockLayoutSettings.DIGIT_GROUPS)
                if(ClockLayoutSettings.digitSpacingKey(group).equals(key)
                        &&!StatusBarSettings.bool(values,ClockLayoutSettings.digitKey(group)))return "Primero, activa los números monoespaciados.";
            if(key.startsWith("clock_dual_")&&!StatusBarSettings.bool(values,ClockLayoutSettings.DUAL_ENABLED))
                return "Primero, activa el reloj de dos filas.";
            if((ClockLayoutSettings.MARGIN_START.equals(key)||ClockLayoutSettings.MARGIN_END.equals(key))
                    &&!StatusBarSettings.bool(values,ClockLayoutSettings.MARGIN_ENABLED))return "Primero, activa el margen general.";
        }
        if(NotificationBigClockSettings.STACK_ENABLED.equals(key)
                &&!StatusBarSettings.bool(values,NotificationBigClockSettings.MASTER)
                &&!(StatusBarSettings.bool(values,NotificationBigClockSettings.LANDSCAPE_MASTER)
                    &&StatusBarSettings.bool(values,FeatureOptions.STACK_LANDSCAPE_ENABLED)))
            return "Primero, activa el reloj grande para la orientación correspondiente, para evitar problemas al desplazar las notificaciones.";
        if(FeatureOptions.STACK_LANDSCAPE_ENABLED.equals(key)
                &&!StatusBarSettings.bool(values,NotificationBigClockSettings.LANDSCAPE_MASTER))
            return "Primero, activa el reloj grande para la orientación correspondiente, para evitar problemas al desplazar las notificaciones.";
        if(NotificationMiniStack.HIDE_BADGE.equals(key)&&!StatusBarSettings.bool(values,NotificationMiniStack.ENABLED))
            return "Primero, activa el apilado compacto (misma aplicación).";
        if(NotificationMiniStack.AUTO_HIDE_IN_STACK.equals(key)) {
            if(!StatusBarSettings.bool(values,NotificationBigClockSettings.STACK_ENABLED))return "Primero, activa el apilado de notificaciones.";
            if(!StatusBarSettings.bool(values,NotificationMiniStack.ENABLED))return "Primero, activa el apilado compacto (misma aplicación).";
        }
        Item item=item(key);
        if(item!=null && "native_network_badge".equals(item.groupId)
                && !key.startsWith("native_data_activity_")
                && StatusBarSettings.bool(values,"label_enabled"))
            return "Desactiva primero el texto del tipo de red y después activa el ajuste de los indicadores nativos.";
        return unavailableReason(key);
    }

    /** One of the clock zone's own settings (not its switch). */
    private static boolean zoneKey(String scene,String key) {
        return key.equals(ShadeWallpaperSettings.zoneBlurKey(scene))||key.equals(ShadeWallpaperSettings.zoneFollowKey(scene))
                ||key.equals(ShadeWallpaperSettings.zoneHeightKey(scene))||key.equals(ShadeWallpaperSettings.zoneFeatherKey(scene))
                ||key.equals(ShadeWallpaperSettings.zoneLiftKey(scene))||key.equals(ShadeWallpaperSettings.zoneSaturationKey(scene))
                ||key.equals(ShadeWallpaperSettings.zoneTintKey(scene))||key.equals(ShadeWallpaperSettings.zoneTintStrengthKey(scene));
    }
    /** The pull-down wallpaper's switches: everything waits for its page; a scene's switch waits for its picture; the zone's numbers wait for the zone. */
    private static String wallpaperReason(String key,Map<String,?> values) {
        if(key==null||!key.startsWith("shade_wallpaper_")||ShadeWallpaperSettings.MASTER.equals(key)
                ||ShadeWallpaperSettings.PORTRAIT_MASTER.equals(key)||ShadeWallpaperSettings.LANDSCAPE_MASTER.equals(key)
                ||ShadeWallpaperSettings.LEGACY_MANAGE.equals(key)||ShadeWallpaperSettings.sceneOfRevisionKey(key)!=null)return "";
        for(boolean landscape:new boolean[]{false,true})
            if((key.equals(ShadeWallpaperSettings.blurWithPullKey(landscape))||key.equals(ShadeWallpaperSettings.swipeSlideKey(landscape)))
                    &&!StatusBarSettings.bool(values,ShadeWallpaperSettings.masterKey(landscape)))
                return landscape?"Primero, activa el fondo del panel desplegable en orientación horizontal.":"Primero, activa el fondo del panel desplegable en orientación vertical.";
        for(String scene:ShadeWallpaperSettings.SCENES) {
            boolean ofScene=key.equals(ShadeWallpaperSettings.enabledKey(scene))||key.equals(ShadeWallpaperSettings.brightnessKey(scene))||key.equals(ShadeWallpaperSettings.blurKey(scene))
                    ||(ShadeWallpaperSettings.hasZone(scene)&&(zoneKey(scene,key)||key.equals(ShadeWallpaperSettings.zoneEnabledKey(scene))));
            if(ofScene&&!StatusBarSettings.bool(values,ShadeWallpaperSettings.masterKey(scene)))
                return ShadeWallpaperSettings.landscape(scene)?"Primero, activa el fondo del panel desplegable en orientación horizontal.":"Primero, activa el fondo del panel desplegable en orientación vertical.";
            if(key.equals(ShadeWallpaperSettings.enabledKey(scene))
                    &&!ShadeWallpaperSettings.validRevision(String.valueOf(values==null?null:values.get(ShadeWallpaperSettings.revisionKey(scene)))))
                return "Primero, selecciona la imagen para este panel en «Imágenes de fondo».";
            if(!ShadeWallpaperSettings.hasZone(scene))continue;
            if(zoneKey(scene,key)&&!StatusBarSettings.bool(values,ShadeWallpaperSettings.zoneEnabledKey(scene)))return "Primero, activa los ajustes independientes del área del reloj grande.";
            if(key.equals(ShadeWallpaperSettings.zoneHeightKey(scene))&&StatusBarSettings.bool(values,ShadeWallpaperSettings.zoneFollowKey(scene)))
                return "La altura del área sigue actualmente la del reloj grande. Desactiva «La altura del área sigue al reloj grande» para usar una altura fija.";
        }
        return "";
    }
    /**
     * What waits for the media-accent master. The lock screen and AOD pages are not in it: they are set on their own and take effect without
     * the media-accent and fluid-cloud style masters (those govern the other scenes).
     */
    private static boolean fluidAccentChild(String key) {
        return FluidCloudAccent.MASK_ENABLED.equals(key)||FluidCloudAccent.MASK_COLOR.equals(key)
                ||FluidCloudAccent.MASK_OPACITY.equals(key)||FluidCloudAccent.OPACITY.equals(key)
                ||FluidCloudStyle.FOLLOW_MEDIA.equals(key);
    }
    /** The lock screen or AOD follows the media colour on its own pages' switches. */
    private static boolean lockOwnFollowing(Map<String,?> values) {
        return StatusBarSettings.bool(values,FluidCloudStyle.NOTIFICATION_FOLLOW)||StatusBarSettings.bool(values,LockNotificationSettings.UNIFY_AOD)
                ||StatusBarSettings.bool(values,FluidCloudStyle.LOCK_CAPSULE)||StatusBarSettings.bool(values,FluidCloudAccent.LOCK_GRADIENT)
                ||StatusBarSettings.bool(values,FluidCloudAccent.LOCK_CUSTOM)||StatusBarSettings.bool(values,FluidCloudAccent.AOD_GRADIENT)
                ||StatusBarSettings.bool(values,FluidCloudAccent.AOD_CUSTOM);
    }
    private static boolean marqueeChild(String key) {
        for(String colour:FluidCloudCapsule.MARQUEE_COLOR_KEYS)if(colour.equals(key))return true;
        return FluidCloudCapsule.MARQUEE_PRESET.equals(key)||FluidCloudCapsule.MARQUEE_COLOR_COUNT.equals(key)
                ||FluidCloudCapsule.MARQUEE_OPACITY.equals(key)||FluidCloudCapsule.MARQUEE_PERIOD.equals(key)
                ||FluidCloudCapsule.MARQUEE_REVERSE.equals(key);
    }
    /** What the frosted glass replaces: the fill, its gradient and following the media colour. */
    private static boolean glassOverridesFill(String key) {
        return FluidCloudCapsule.FILL_ENABLED.equals(key)||FluidCloudCapsule.FILL_COLOR.equals(key)||FluidCloudCapsule.FILL_OPACITY.equals(key)
                ||FluidCloudCapsule.FOLLOW_MEDIA.equals(key)||FluidCloudCapsule.GRADIENT_ENABLED.equals(key)
                ||FluidCloudCapsule.GRADIENT_MODE.equals(key)||FluidCloudCapsule.GRADIENT_COLOR.equals(key);
    }
    private static boolean capsuleChild(String key) {
        return FluidCloudCapsule.FILL_ENABLED.equals(key)||FluidCloudCapsule.FILL_COLOR.equals(key)
                ||FluidCloudCapsule.FILL_OPACITY.equals(key)||FluidCloudCapsule.FOLLOW_MEDIA.equals(key)
                ||FluidCloudCapsule.BORDER_ENABLED.equals(key)||FluidCloudCapsule.BORDER_COLOR.equals(key)
                ||FluidCloudCapsule.BORDER_WIDTH.equals(key)||FluidCloudCapsule.RADIUS_ENABLED.equals(key)
                ||FluidCloudCapsule.RADIUS.equals(key)||FluidCloudCapsule.GRADIENT_ENABLED.equals(key)
                ||FluidCloudCapsule.GRADIENT_MODE.equals(key)||FluidCloudCapsule.GRADIENT_COLOR.equals(key)
                ||FluidCloudCapsule.BLUR_ENABLED.equals(key)||FluidCloudCapsule.BLUR_RADIUS.equals(key)||FluidCloudCapsule.BLUR_MASK_COLOR.equals(key)
                ||FluidCloudCapsule.GLOW_ENABLED.equals(key)||FluidCloudCapsule.GLOW_DEPTH.equals(key)||FluidCloudCapsule.SPECTRUM_COLOR.equals(key)
                ||FluidCloudCapsule.MARQUEE_ENABLED.equals(key)||marqueeChild(key)
                ||FluidCloudCapsule.SPECTRUM_ENABLED.equals(key)||FluidCloudCapsule.SPECTRUM_SENSITIVITY.equals(key)
                ||FluidCloudCapsule.SPECTRUM_FOLLOW.equals(key)
                ||FluidCloudCapsule.COVER_RADIUS_ENABLED.equals(key)||FluidCloudCapsule.COVER_RADIUS.equals(key)
                ||FluidCloudCapsule.COVER_SIZE_ENABLED.equals(key)||FluidCloudCapsule.COVER_SCALE.equals(key)
                ||CapsuleEffects.BOUNCE_ENABLED.equals(key)||CapsuleEffects.BOUNCE.equals(key);
    }
    /** The style page's own master governs what only that page does; keeping a music card's highlight does not belong to it. */
    private static boolean fluidStyleChild(String key) {
        return FluidCloudStyle.FOLLOW_NOTIFICATION.equals(key)||FluidCloudStyle.SCOPE_FLOAT.equals(key)
                ||FluidCloudStyle.SCOPE_SHADE.equals(key)||FluidCloudStyle.SCOPE_LOCK.equals(key)||FluidCloudStyle.DESKTOP_BACKGROUND.equals(key)
                ||FluidCloudScope.isScopeKey(key);
    }

    /** Actual precedence and mutual exclusion, shown at the top of each affected feature. */
    public static String conflictDescription(String groupId, String category) {
        if ("clock".equals(groupId)) return "El reloj predeterminado y el reloj de dos filas son excluyentes: al activar uno se desactiva el otro. Cada interruptor está en la parte superior de su pestaña. Si activas el reloj grande en la página de notificaciones vertical u horizontal, este sustituye a la hora pequeña del panel desplegado; la barra de estado seguirá usando los ajustes de esta página. El reloj de dos filas solo controla la hora de la barra de estado; los relojes del panel desplegado y de otras ubicaciones siguen usando la configuración predeterminada.";
        if ("shade_clock".equals(groupId)) return "Incompatibilidad: comparte el área de la hora de la página de notificaciones con el reloj grande. Si cambias las condiciones que activan esta opción, se desactiva el reloj grande. Si el reloj grande no está activo, la página de notificaciones conserva el reloj desplegable y el centro de control sigue usando esta configuración.";
        if ("carrier".equals(groupId) && NOTIFICATION.equals(category)) return "Incompatibilidad: comparte el área superior con el reloj grande de la página de notificaciones. Activar o editar esta opción desactiva el reloj grande. Apagar el interruptor de esta página no vuelve a activar automáticamente el reloj grande.";
        if (groupId.startsWith("notification_big_clock")) return "Prioridad: solo sustituye la hora pequeña de la página de notificaciones en la orientación correspondiente y conserva los iconos nativos del centro de control. Editar el reloj del panel desplegable o el texto del operador en la barra de notificaciones desactiva el reloj grande en ambas orientaciones. El apilado de notificaciones se configura por separado en «Panel de notificaciones → Tarjetas de notificación».";
        if ("font".equals(groupId)) return "Prioridad: el tamaño y el grosor de cada función siguen siendo independientes. Si el reloj grande utiliza la fuente nativa, emplea sus propios caracteres; el archivo importado aquí solo se usa al seleccionar una fuente personalizada.";
        if ("notification_icons".equals(groupId)) return "Opciones excluyentes: elige solo una entre icono nativo, corazón, texto e imagen. La cantidad máxima de iconos solo se aplica al modo nativo; las reglas para ocultarlos en la página de notificaciones tienen prioridad.";
        if ("notification_icon_overrides".equals(groupId)) return "Prioridad: cuando el área de iconos de notificación usa un corazón, texto o imagen únicos, se suspenden las sustituciones individuales por aplicación. Se reanudan al volver al modo de visualización nativo. El recuento de notificaciones y las reglas para ocultarlas siguen controlados por el sistema y la configuración correspondiente.";
        if ("status_icon_library".equals(groupId)) return "Importa primero una biblioteca de iconos y después aplícala al ámbito seleccionado. La selección individual tiene prioridad sobre el paquete completo. Por defecto, el color sigue al sistema; los recursos originales pueden conservar su color propio. El color y la transparencia se heredan por separado. Los estados desactivado, no disponible y animado dependen del comportamiento nativo real.";
        if ("notification_mini_stack".equals(groupId)) return "Independiente: solo muestra los grupos de notificaciones que el sistema ya ha creado. No vuelve a agrupar ni ordenar, y no añade gestos de deslizamiento hacia abajo. La expansión al deslizar hacia arriba conserva el comportamiento original."
                + "No depende del «Apilado de notificaciones» de columna completa y no requiere el reloj grande ni Fluid Cloud. El color, los degradados y los reflejos de las notificaciones siguen gestionándose por separado. El contenido privado, las notificaciones flotantes y las notificaciones que abren el menú de ajustes conservan el comportamiento nativo."
                + NotificationMiniStackSource.DECLARATION + NotificationMiniStackSource.TECHNICAL_ORIGIN;
        if ("notification_stack".equals(groupId)) return "Requisito: primero activa el reloj grande en la orientación correspondiente para evitar problemas al desplazar las notificaciones. En vertical se necesita el reloj grande vertical; en horizontal también deben activarse «Aplicar también en horizontal» y el reloj grande horizontal. Solo se pliega la lista exterior; los grupos dentro de cada aplicación conservan la lógica nativa.";
        if ("shade_status_icons".equals(groupId)) return "Prioridad: si está activado este interruptor o el reloj grande, se ocultan continuamente la hora pequeña nativa superior, los iconos de notificación y los iconos de la derecha, así como la hora pequeña y los iconos derechos de la página de notificaciones durante el despliegue y los cambios entre páginas. Se restauran al cerrar ambos paneles o desactivar los dos interruptores. Los iconos derechos del centro de control siempre se mantienen nativos. Este interruptor controla por sí solo la hora pequeña y los iconos de notificación del lado izquierdo. El estilo y la cantidad de iconos siguen configurándose en «Área de iconos de notificación».";
        if ("label".equals(groupId)) return "Prioridad: si se oculta el tipo de red, el Wi-Fi está conectado o ambos interruptores de red están desactivados, ocultar tiene prioridad sobre los ajustes de nombre, posición, fuente y color.";
        if ("native_network_badge".equals(groupId)) return "Opciones incompatibles: cuando está activado el texto del tipo de red, se suspende el ajuste de esta página. Al desactivarlo, se aplica el interruptor de ocultación de los indicadores nativos; los parámetros de esta página se conservan. Ocultar el indicador de red del sistema tiene prioridad sobre su posición, tamaño y grosor.";
        if ("battery".equals(groupId)) return "Prioridad: los ajustes del texto dentro de la batería solo afectan a los números. Si los números de carga y el rayo alternan, la fase de los números usa los ajustes de texto. Al cambiar el estilo de batería, se vuelve a aplicar la forma de los caracteres nativa de ese estilo.";
        if ("qs_media".equals(groupId)) return "Opciones incompatibles: si el resplandor exterior usa el color invertido de la portada, se ignora el color de resplandor personalizado. Prioridad del fondo: color personalizado > color principal de la portada > imagen de la portada. El color superpuesto solo afecta a la imagen o al color principal de la portada. El fondo de la portada y las esquinas uniformes se pueden usar a la vez.";
        if ("c17_highlight_removal".equals(groupId)) return "Compatibilidad: la eliminación de reflejos puede activarse junto con los colores de los mosaicos y el fondo de la portada musical. Esos fondos personalizados se conservan; el fondo acrílico de esta página se aplica a los demás materiales nativos.";
        if ("fluid_cloud_style".equals(groupId)) return "Prioridad del color: color de énfasis multimedia > seguir el color multimedia > fondo personalizado de esta página. El color de énfasis y el color seguido usan la máscara y la transparencia de la página «Color de énfasis multimedia de Fluid Cloud»; en los demás casos se usa el fondo de esta página. El estilo que sigue a las notificaciones del sistema solo aporta el material y se puede combinar con un color. El color solo afecta a las notificaciones flotantes expandidas, la pantalla de bloqueo y AOD (AOD sigue a la pantalla de bloqueo); no modifica el panel de notificaciones. La eliminación de reflejos se controla desde «Eliminar reflejos de C17».";
        if ("fluid_cloud_accent".equals(groupId)) return "Prioridad 1: cuando el interruptor principal está activado, la tarjeta de música siempre utiliza el color de énfasis, incluso en notificaciones flotantes, pantalla de bloqueo y AOD. Las otras tarjetas pueden seguir el color multimedia actual (prioridad 2, por encima del fondo personalizado). El color de énfasis y el color seguido comparten la máscara y la transparencia de relleno de esta página; la pantalla de bloqueo y AOD se pueden personalizar por separado en «Personalización de pantalla de bloqueo». Si se desactiva el interruptor principal, todos los interruptores de esta página quedan inactivos. Si no hay contenido multimedia o no se está reproduciendo (cuando está activado «Seguir solo durante la reproducción»), se vuelve al fondo personalizado de «Estilo de Fluid Cloud».";
        if ("data".equals(groupId)) return "Prioridad: ocultar el indicador de red no oculta el texto independiente del tipo de red; este se configura en «Texto del tipo de red». La flecha pequeña de datos móviles se activa por separado en «Ajuste de indicadores nativos» y no depende del estilo de datos móviles ni del interruptor del texto de red.";
        return "";
    }
    public static List<Item> allItems() { return Collections.unmodifiableList(new ArrayList<>(ITEM_BY_KEY.values())); }
    public static Set<String> hiddenKeys() { return HIDDEN; }
    public static boolean isVisible(String key) { return ITEM_BY_KEY.containsKey(key); }
    /** Scene-specific editors never change the saved compatibility keys. */
    public static boolean applicable(Item item,Map<String,?> saved) {
        if(item==null)return false;
        int widgetSide=BigClockWidgetsSettings.textSide(item.key);
        if(widgetSide>=0&&(BigClockWidgetsSettings.colorKey(widgetSide,false).equals(item.key)||BigClockWidgetsSettings.colorKey(widgetSide,true).equals(item.key)))
            return !StatusBarSettings.bool(saved,BigClockWidgetsSettings.followTimeColorKey(widgetSide));
        String portrait=NotificationBigClockSettings.portraitKey(item.key);
        // Per-line colours return whenever the optional follow policy is paused.
        if(NotificationBigClockSettings.DATE_COLOR_LIGHT.equals(portrait)||NotificationBigClockSettings.DATE_COLOR_DARK.equals(portrait)
                ||NotificationBigClockSettings.FOOTER_COLOR_LIGHT.equals(portrait)||NotificationBigClockSettings.FOOTER_COLOR_DARK.equals(portrait))
            return !NotificationBigClockSettings.effectiveFollowStyle(saved,item.key.startsWith(NotificationBigClockSettings.LANDSCAPE_PREFIX));
        if(NotificationBigClockSettings.SCREEN_PADDING.equals(item.key)
                ||NotificationBigClockSettings.COMPACT_MAX_SIZE.equals(NotificationBigClockSettings.portraitKey(item.key)))
            return "notification_big_clock".equals(item.groupId)
                    && NotificationBigClockSettings.nativeHeightFont(StatusBarSettings.string(saved,NotificationBigClockSettings.FONT));
        return true;
    }
    /** Search may reveal an inactive mode's saved setting; explain it without changing that mode. */
    public static String inactivePresentationReason(Item item, Map<String,?> saved) {
        if (item == null || applicable(item, saved)) return "";
        if (BigClockWidgetsSettings.textSide(item.key) >= 0) return "El componente sigue actualmente el color de la hora principal. Al desactivar el seguimiento, se usa el color independiente guardado aquí.";
        String key = NotificationBigClockSettings.portraitKey(item.key);
        if (NotificationBigClockSettings.DATE_COLOR_LIGHT.equals(key) || NotificationBigClockSettings.DATE_COLOR_DARK.equals(key)
                || NotificationBigClockSettings.FOOTER_COLOR_LIGHT.equals(key) || NotificationBigClockSettings.FOOTER_COLOR_DARK.equals(key))
            return "Actualmente sigue el estilo del fondo. Al desactivar el seguimiento, se restaura el color independiente guardado aquí.";
        return "Solo se aplica a la fuente nativa en orientación vertical. La fuente o la orientación actuales no utilizan este parámetro.";
    }
    public static String displayTitle(Item item,Map<String,?> saved) {
        if(item==null)return "";
        if (StatusBarLayoutSettings.marginKey(item.key)
                && ("wifi".equals(item.groupId) || "label".equals(item.groupId))
                && StatusBarSettings.bool(saved, StatusBarLayoutSettings.WIFI_LABEL_SHARED)) return item.title + " · Compartido";
        if(NotificationBigClockSettings.MAX_SIZE.equals(item.key)
                && NotificationBigClockSettings.nativeHeightFont(StatusBarSettings.string(saved,NotificationBigClockSettings.FONT)))return "Altura de la fuente expandida";
        return item.title;
    }
    private static boolean bigClockWidget(String key) {
        return BigClockWidgetsSettings.SWAP_WITH_DATE.equals(key)||BigClockWidgetsSettings.textSide(key)>=0
                ||BigClockWidgetsSettings.LEFT_PROVIDER.equals(key)||BigClockWidgetsSettings.RIGHT_PROVIDER.equals(key)
                ||BigClockWidgetsSettings.LEFT_HEIGHT.equals(key)||BigClockWidgetsSettings.RIGHT_HEIGHT.equals(key);
    }
    public static Object defaultValue(String key) {
        if (StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key)) return StatusBarSettings.BOOLEAN_DEFAULTS.get(key);
        if (StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)) return StatusBarSettings.NUMERIC_DEFAULTS.get(key);
        if (StatusBarSettings.COLOR_DEFAULTS.containsKey(key)) return StatusBarSettings.COLOR_DEFAULTS.get(key);
        return StatusBarSettings.STRING_DEFAULTS.get(key);
    }
    /** Read through the existing legacy fallback rules; never rewrite stored values just by viewing. */
    public static Object value(Map<String, ?> saved, String key) {
        if (StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key)) return StatusBarSettings.bool(saved, key);
        if (StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)) return StatusBarSettings.settingNumber(saved, key, StatusBarSettings.NUMERIC_DEFAULTS.get(key));
        if (StatusBarSettings.COLOR_DEFAULTS.containsKey(key)) return StatusBarSettings.color(saved, key);
        if (StatusBarSettings.STRING_DEFAULTS.containsKey(key)) return StatusBarSettings.string(saved, key);
        return saved == null ? null : saved.get(key);
    }
    /** Check domain restrictions before float conversion can erase a fractional input. */
    static String numericInputError(String key, BigDecimal raw) {
        if(BigClockWidgetsSettings.maskOpacityKey(key)&&(raw.signum()<0||raw.compareTo(BigDecimal.valueOf(100))>0))return "La intensidad de la máscara del componente debe estar entre 0 y 100 %.";
        if(BigClockWidgetsSettings.weightKey(key)&&(raw.compareTo(BigDecimal.valueOf(BigClockWidgetsSettings.MIN_WEIGHT))<0
                ||raw.compareTo(BigDecimal.valueOf(BigClockWidgetsSettings.MAX_WEIGHT))>0))return "El grosor de la fuente del componente debe estar entre 100 y 1000.";
        if(BigClockWidgetsSettings.sizeKey(key)&&(raw.compareTo(BigDecimal.valueOf(BigClockWidgetsSettings.MIN_SIZE))<0
                ||raw.compareTo(BigDecimal.valueOf(BigClockWidgetsSettings.MAX_SIZE))>0))return "El tamaño de fuente del componente debe estar entre 8 y 64 dp.";
        if(BigClockWidgetsSettings.offsetKey(key)&&(raw.compareTo(BigDecimal.valueOf(-BigClockWidgetsSettings.MAX_OFFSET))<0
                ||raw.compareTo(BigDecimal.valueOf(BigClockWidgetsSettings.MAX_OFFSET))>0))return "La posición del componente debe estar entre -1000 y 1000 dp.";
        if(NotificationBigClockSettings.REVEAL_MASK_OPACITY.equals(NotificationBigClockSettings.portraitKey(key))
                &&(raw.signum()<0||raw.compareTo(BigDecimal.valueOf(100))>0))return "La opacidad de la máscara debe estar entre 0 y 100 %.";
        String backdropNumberError=ClockBackdropSettings.numericInputError(key,raw);
        if(backdropNumberError!=null)return backdropNumberError;
        if (StatusBarLayoutSettings.orderKey(key) && (raw.signum() < 0 || raw.stripTrailingZeros().scale() > 0
                || raw.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0)) return "El orden debe ser un número entero entre 0 y 2147483647.";
        if (StatusBarLayoutSettings.orderKey(key) && (double) raw.floatValue() > Integer.MAX_VALUE)
            return "El número de orden es demasiado grande; utiliza un valor menor.";
        if (StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order").equals(key)
                && raw.compareTo(BigDecimal.valueOf(2)) > 0) return "El orden del tipo de red solo puede ser 0 (seguir al sistema), 1 (antes de la señal) o 2 (después de la señal).";
        if (NetworkSpeedControls.INTERVAL_MILLIS.equals(key) && raw.signum() <= 0)
            return "El intervalo de actualización debe ser superior a 0 ms.";
        if (QsTileCorners.RADIUS.equals(key) && (raw.signum() < 0 || raw.compareTo(BigDecimal.valueOf(QsTileCorners.MAX_RADIUS)) > 0))
            return "El radio de las esquinas debe estar entre 0 y " + (int) QsTileCorners.MAX_RADIUS + " dp";
        if ((NativeStatusIcons.MAX.equals(key)||NotificationIconArea.MAX_COUNT.equals(key)) && (raw.signum() < 0 || raw.stripTrailingZeros().scale() > 0))
            return "La cantidad de iconos debe ser un entero no negativo; 0 significa no mostrar ninguno.";
        if (NotificationBigClockSettings.VISIBLE_COUNT.equals(NotificationBigClockSettings.portraitKey(key)) && (raw.compareTo(BigDecimal.ONE) < 0
                || raw.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0 || raw.stripTrailingZeros().scale() > 0))
            return "La cantidad total de notificaciones debe ser un entero entre 1 y 2147483647.";
        return null;
    }
    /** Suggested bounds control trial confirmation, not valid manual-entry limits. */
    public static boolean requiresNumericTrial(Item item, Number value) {
        if (item == null || value == null || !NUMERIC.equals(item.type)) return false;
        double number = value.doubleValue();
        return Double.isFinite(number) && (number < item.min || number > item.max);
    }
    public static float customNumber(Item item, String input) {
        BigDecimal raw = NumericInput.decimal(input);
        String error = numericInputError(item.key, raw);
        if (error != null) throw new IllegalArgumentException(error);
        return raw.floatValue();
    }
    /** Options are closed sets. Pattern/string presets intentionally remain user-editable. */
    public static String validationError(Item item, Object value) {
        if (item == null) return "No se encontró este ajuste.";
        if (!unavailableReason(item.key).isEmpty()) return unavailableReason(item.key);
        if (BOOLEAN.equals(item.type)) return value instanceof Boolean ? null : "Selecciona activar o desactivar.";
        if (NUMERIC.equals(item.type)) {
            if (!(value instanceof Number)) return "Introduce un número válido.";
            float number = ((Number) value).floatValue();
            if (Float.isNaN(number) || Float.isInfinite(number)) return "Introduce un número finito.";
            String widgetNumberError=BigClockWidgetsSettings.numericValidationError(item.key,number);
            if(widgetNumberError!=null)return widgetNumberError;
            String backdropNumberError=ClockBackdropSettings.numericValidationError(item.key,number);
            if(backdropNumberError!=null)return backdropNumberError;
            if(NotificationBigClockSettings.REVEAL_MASK_OPACITY.equals(NotificationBigClockSettings.portraitKey(item.key))
                    &&(number<0f||number>100f))return "La opacidad de la máscara debe estar entre 0 y 100 %.";
            if (StatusBarLayoutSettings.orderKey(item.key) && (number < 0 || number != Math.floor(number)
                    || (double) number > Integer.MAX_VALUE)) return "El orden debe ser un entero no negativo.";
            if (StatusBarLayoutSettings.key(StatusBarLayoutSettings.LABEL, "order").equals(item.key) && number > 2)
                return "El orden del tipo de red solo puede ser 0, 1 o 2.";
            if (NetworkSpeedControls.INTERVAL_MILLIS.equals(item.key) && number <= 0) return "El intervalo de actualización debe ser superior a 0 ms.";
            if (QsTileCorners.RADIUS.equals(item.key) && (number < 0 || number > QsTileCorners.MAX_RADIUS)) return "El radio de las esquinas debe estar entre 0 y " + (int) QsTileCorners.MAX_RADIUS + " dp";
            if ((NativeStatusIcons.MAX.equals(item.key)||NotificationIconArea.MAX_COUNT.equals(item.key)) && (number < 0 || number != Math.floor(number))) return "La cantidad de iconos debe ser un entero no negativo; 0 significa no mostrar ninguno.";
            if (NotificationBigClockSettings.VISIBLE_COUNT.equals(NotificationBigClockSettings.portraitKey(item.key)) && (number < 1
                    || number > (float)Integer.MAX_VALUE || number != Math.floor(number))) return "La cantidad total de notificaciones debe ser un entero entre 1 y 2147483647.";
            return null;
        }
        if (COLOR.equals(item.type)) return value instanceof Number ? null : "Introduce un color válido.";
        if (!(value instanceof String)) return "Introduce texto.";
        String text = (String) value;
        if (IconManagementSettings.KEY.equals(item.key)) {
            try { IconManagementSettings.parse(text); return null; }
            catch (java.io.IOException invalid) { return invalid.getMessage(); }
        }
        if(BigClockWidgetsSettings.launchAppKey(item.key))return BigClockWidgetsSettings.launchAppValidationError(text);
        if(BigClockWidgetsSettings.textKey(item.key))return BigClockWidgetsSettings.textValidationError(text);
        if (NotificationIconOverrides.RULES.equals(item.key)) return NotificationIconOverrides.validationError(text);
        if (IconPackAssignments.ASSIGNMENTS.equals(item.key)) return IconPackAssignments.validationError(text);
        if (IconPackRepository.LAYERS.equals(item.key)) {
            try { IconPackRepository.layers(text); return null; }
            catch (java.io.IOException invalid) { return invalid.getMessage(); }
        }
        if (LockscreenControls.DATE_FORMAT.equals(item.key)) return TimeFormat.validationError(text);
        if (NotificationIconArea.TEXT.equals(item.key) && text.codePointCount(0, text.length()) > 12) return "El símbolo o texto puede contener como máximo 12 caracteres.";
        if (OPTIONS.equals(item.type)) {
            for (String option : item.values) if (option.equals(text)) return null;
            return "Selecciona una opción de la lista.";
        }
        if (item.key.endsWith("_pattern")) return NotificationBigClockSettings.FOOTER_PATTERN.equals(NotificationBigClockSettings.portraitKey(item.key)) ? TimeFormat.contentValidationError(text) : TimeFormat.validationError(text);
        if (STRING.equals(item.type) && (text.length() > TimeFormat.MAX_TEXT || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0)) return "Introduce un texto de una sola línea de hasta 120 caracteres.";
        return null;
    }
    private SettingsCatalog() { }
}
