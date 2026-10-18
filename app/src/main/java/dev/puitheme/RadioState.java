// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

/** Radio decisions from the same Wi-Fi model used by SystemUI, with a default-route fallback. */
public final class RadioState {
    private RadioState() { }

    // Post-1.8.2 configuration keys remain readable for existing backups/UI only.
    // They do not change the public 1.8.2 radio or label decisions below.
    public static final String ALWAYS_SHOWN = "label_always_shown";
    public static final String WIFI_MERGED = "label_wifi_merged";
    public static final String BOTH_VISIBLE = "network_both_visible";

    /**
     * nativeWifiVisible is WifiIcon.Visible, not a View's visibility or animation alpha.
     * null means the native model is unavailable. defaultWifi describes only the current
     * default network (including its VPN underlying transport), never all known networks.
     */
    public static boolean wifiInUse(boolean wifiEnabled, Boolean nativeWifiVisible, boolean defaultWifi) {
        if (!wifiEnabled) return false;
        return nativeWifiVisible != null ? nativeWifiVisible : defaultWifi;
    }

    /**
     * The active-data fallback must belong to the current data subscription; callers must
     * clear that source on SIM/radio changes rather than reusing the last displayed label.
     */
    public static String cellularLabel(boolean enabled, boolean wifiInUse, boolean airplaneMode,
                                       String nativeLabel, String activeDataFallback, boolean normalize) {
        return cellularLabel(enabled,wifiInUse,airplaneMode,nativeLabel,activeDataFallback,normalize,null,null);
    }

    public static String cellularLabel(boolean enabled, boolean wifiInUse, boolean airplaneMode,
                                       String nativeLabel, String activeDataFallback, boolean normalize,
                                       Boolean wifiEnabled,Boolean mobileDataEnabled) {
        if (!enabled || wifiInUse || airplaneMode || NetworkLabel.radiosDisabled(wifiEnabled,mobileDataEnabled)) return "";
        String result = label(nativeLabel, normalize);
        return result.isEmpty() ? label(activeDataFallback, normalize) : result;
    }

    private static String label(String value, boolean normalize) {
        String normalized = NetworkLabel.normalize(value);
        if (normalized.isEmpty()) return "";
        return normalize ? normalized : value.trim();
    }
}
