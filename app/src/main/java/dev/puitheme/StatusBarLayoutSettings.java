// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Bundle;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** App/runtime protocol for physical left/right gaps and native status-bar element placement. */
public final class StatusBarLayoutSettings {
    public static final String MASTER = "statusbar_layout_enabled", GROUP = "statusbar_layout";
    public static final String WIFI_LABEL_SHARED = "statusbar_wifi_label_shared_position";
    public static final String NATIVE = "native", LEFT = "left", RIGHT = "right";
    public static final String CLOCK = "clock", NOTIFICATION = "notification", CARRIER = "carrier", SPEED = "speed";
    public static final String WIFI = "wifi", MOBILE = "mobile", BATTERY = "battery", SYSTEM = "system", LABEL = "label";
    public static final List<String> TOKENS = Collections.unmodifiableList(Arrays.asList(
            CLOCK, NOTIFICATION, CARRIER, SPEED, WIFI, MOBILE, BATTERY, SYSTEM));
    public static final Map<String, Boolean> BOOLEANS;
    public static final Map<String, Float> NUMBERS;
    public static final Map<String, String> STRINGS;
    static {
        Map<String, Boolean> switches = new LinkedHashMap<>();
        switches.put(MASTER, false); switches.put(WIFI_LABEL_SHARED, false);
        BOOLEANS = Collections.unmodifiableMap(switches);
        Map<String, Float> numbers = new LinkedHashMap<>();
        Map<String, String> strings = new LinkedHashMap<>();
        for (String token : TOKENS) {
            strings.put(key(token, "side"), NATIVE);
            numbers.put(key(token, "order"), 0f);
            numbers.put(key(token, "margin_left"), 0f);
            numbers.put(key(token, "margin_right"), 0f);
        }
        numbers.put(key(LABEL, "order"), 0f);
        numbers.put(key(LABEL, "margin_left"), 0f);
        numbers.put(key(LABEL, "margin_right"), 0f);
        NUMBERS = Collections.unmodifiableMap(numbers);
        STRINGS = Collections.unmodifiableMap(strings);
    }
    private StatusBarLayoutSettings() { }

    /** label is an inner mobile-network element and deliberately has no side setting. */
    public static String key(String token, String suffix) {
        boolean outer = TOKENS.contains(token), inner = LABEL.equals(token);
        if ((!outer && !inner) || !("order".equals(suffix) || "margin_left".equals(suffix)
                || "margin_right".equals(suffix) || outer && "side".equals(suffix)))
            throw new IllegalArgumentException("Unknown status-bar layout field");
        return "statusbar_layout_" + token + "_" + suffix;
    }
    public static boolean owns(String key) {
        return BOOLEANS.containsKey(key) || NUMBERS.containsKey(key) || STRINGS.containsKey(key);
    }
    public static boolean orderKey(String key) { return NUMBERS.containsKey(key) && key.endsWith("_order"); }
    public static boolean marginKey(String key) { return NUMBERS.containsKey(key) && !orderKey(key); }
    public static boolean sideKey(String key) { return STRINGS.containsKey(key); }
    public static boolean validSide(Object value) { return NATIVE.equals(value) || LEFT.equals(value) || RIGHT.equals(value); }
    /** Editor alias only: the independently saved label gaps are never copied or overwritten. */
    public static String editingKey(String key, Map<String, ?> values) {
        if (values != null && Boolean.TRUE.equals(values.get(WIFI_LABEL_SHARED))) {
            if (key(LABEL, "margin_left").equals(key)) return key(WIFI, "margin_left");
            if (key(LABEL, "margin_right").equals(key)) return key(WIFI, "margin_right");
        }
        return key;
    }
    public static String label(String token) {
        if (CLOCK.equals(token)) return "Hora";
        if (NOTIFICATION.equals(token)) return "Iconos de notificación";
        if (CARRIER.equals(token)) return "Operador";
        if (SPEED.equals(token)) return "Velocidad de red";
        if (WIFI.equals(token)) return "Wi-Fi";
        if (MOBILE.equals(token)) return "Señal móvil";
        if (BATTERY.equals(token)) return "Batería";
        if (SYSTEM.equals(token)) return "Avisos del sistema";
        if (LABEL.equals(token)) return "Tipo de red";
        throw new IllegalArgumentException("Unknown status-bar element");
    }
    /** The saved master controls region/order only; margins have no additional switch. */
    public static boolean orderingEnabled(Bundle values) {
        return values != null && Boolean.TRUE.equals(values.get(MASTER))
                && !Boolean.TRUE.equals(values.get(StatusBarSettings.SAFE_MODE));
    }
    /** Any finite nonzero physical margin requests native flow, even with sorting off. */
    public static boolean enabled(Bundle values) {
        if (values == null || Boolean.TRUE.equals(values.get(StatusBarSettings.SAFE_MODE))) return false;
        if (orderingEnabled(values) || Boolean.TRUE.equals(values.get(WIFI_LABEL_SHARED))) return true;
        for (String key : NUMBERS.keySet()) if (marginKey(key) && margin(values.get(key)) != 0f) return true;
        return false;
    }
    /** Map equivalent used by the settings editor; never seeds missing values or writes preferences. */
    public static boolean enabledValues(Map<String, ?> values) {
        if (values == null || Boolean.TRUE.equals(values.get(StatusBarSettings.SAFE_MODE))) return false;
        if (Boolean.TRUE.equals(values.get(MASTER)) || Boolean.TRUE.equals(values.get(WIFI_LABEL_SHARED))) return true;
        for (String key : NUMBERS.keySet()) if (marginKey(key) && margin(values.get(key)) != 0f) return true;
        return false;
    }
    private static float margin(Object value) { return NumericPolicy.finite(value, 0f); }
    /** Suspend old outer drawing-X paths in an isolated effective copy; never alter persisted data. */
    public static Bundle runtime(Bundle raw) {
        Bundle result = raw == null ? new Bundle() : new Bundle(raw);
        if (!enabled(raw)) return result;
        for (String key : new String[]{StatusBarSettings.CLOCK_OFFSET_X, ClockLayoutSettings.DUAL_OFFSET_X,
                StatusBarSettings.CARRIER_OFFSET_X, StatusBarSettings.BATTERY_OFFSET_X,
                StatusBarSettings.SPEED_OFFSET_X, StatusBarSettings.WIFI_OFFSET_X,
                StatusBarSettings.DATA_OFFSET_X, StatusBarSettings.LABEL_OFFSET_X,
                NotificationIconArea.X, NativeStatusIcons.X}) result.putFloat(key, 0f);
        result.putBoolean(ClockLayoutSettings.MARGIN_ENABLED, false);
        result.putBoolean(SpeedPosition.SAFE_GAP_ENABLED, false);
        result.putBoolean(NetworkIconOrder.SWAP, false);
        result.putBoolean(DataBatterySpacing.ENABLED, false);
        // The flow reserves hint spacing as real width. The old target writer
        // must not apply it a second time after our native animation target.
        result.putFloat(NativeStatusIcons.SPACING, 0f);
        // The old speed placement state is gated inside SpeedPosition, keeping its Y switch alive.
        return result;
    }
    /** Immutable parsed values. Renderers still resolve the native host, visibility and density. */
    public static final class Snapshot {
        public final boolean enabled, orderingEnabled, wifiLabelShared;
        private final NativeStatusIconLayout.Snapshot hintLayout;
        private final String[] sides;
        private final int[] orders;
        private final float[] left, right;
        private Snapshot(Bundle values) {
            orderingEnabled = StatusBarLayoutSettings.orderingEnabled(values);
            Object hintSource=value(values,NativeStatusIcons.LAYOUT);
            hintLayout=NativeStatusIconLayout.parse(hintSource instanceof String?(String)hintSource:NativeStatusIconLayout.DEFAULT);
            boolean safe = values != null && Boolean.TRUE.equals(values.get(StatusBarSettings.SAFE_MODE));
            wifiLabelShared = !safe && values != null && Boolean.TRUE.equals(values.get(WIFI_LABEL_SHARED));
            boolean hasMargin = false;
            sides = new String[TOKENS.size()];
            orders = new int[TOKENS.size() + 1];
            left = new float[orders.length]; right = new float[orders.length];
            for (int index = 0; index < orders.length; index++) {
                String token = index == TOKENS.size() ? LABEL : TOKENS.get(index);
                if (index < sides.length) {
                    Object side = value(values, key(token, "side"));
                    sides[index] = validSide(side) ? (String) side : NATIVE;
                }
                Object rawOrder = value(values, key(token, "order"));
                double order = rawOrder instanceof Number ? ((Number) rawOrder).doubleValue() : 0d;
                orders[index] = Double.isFinite(order) && order >= 0d && order <= Integer.MAX_VALUE
                        && order == Math.floor(order) && (!LABEL.equals(token) || order <= 2d) ? (int) order : 0;
                left[index] = safe ? 0f : margin(value(values, key(token, "margin_left")));
                right[index] = safe ? 0f : margin(value(values, key(token, "margin_right")));
                hasMargin |= left[index] != 0f || right[index] != 0f;
            }
            enabled = orderingEnabled || hasMargin || wifiLabelShared;
        }
        private Snapshot(Snapshot source, boolean ordering) {
            orderingEnabled = ordering && source.orderingEnabled; wifiLabelShared = source.wifiLabelShared;
            hintLayout=source.hintLayout;
            sides = source.sides; orders = source.orders; left = source.left; right = source.right;
            boolean hasMargin = false;
            for (int index = 0; index < left.length; index++) hasMargin |= left[index] != 0f || right[index] != 0f;
            enabled = orderingEnabled || hasMargin || wifiLabelShared;
        }
        public static Snapshot parse(Bundle values) { return new Snapshot(values); }
        /** Margins (and the shared Wi-Fi slot) without region/order: used while a fluid-cloud
         * capsule occupies the bar, whose native hiding follows the native sides and order. */
        public Snapshot withoutOrdering() { return orderingEnabled ? new Snapshot(this, false) : this; }
        /** The scene adapter resolves one actual shared slot, rather than two equal coordinate values. */
        public String slotToken(String token) { return wifiLabelShared && (WIFI.equals(token) || LABEL.equals(token)) ? WIFI : token; }
        public String side(String token) {
            int index = TOKENS.indexOf(token);
            return !orderingEnabled || index < 0 ? NATIVE : sides[index];
        }
        public boolean hintLayoutActive(){return orderingEnabled&&!hintLayout.empty();}
        public boolean hintSpecified(String slot){return hintLayoutActive()&&hintLayout.specified(slot);}
        public String hintSide(String slot){return hintSpecified(slot)?hintLayout.side(slot):NATIVE;}
        public int hintOrder(String slot){return hintSpecified(slot)?hintLayout.order(slot):0;}
        public int order(String token) { int index = index(token); return !orderingEnabled || index < 0 ? 0 : orders[index]; }
        public float left(String token) { int index = index(token); return index < 0 ? 0f : left[index]; }
        public float right(String token) { int index = index(token); return index < 0 ? 0f : right[index]; }
        private int index(String token) { return LABEL.equals(token) ? TOKENS.size() : TOKENS.indexOf(token); }
        private static Object value(Bundle values, String key) { return values == null ? null : values.get(key); }
    }
}
