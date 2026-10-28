// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.util.Objects;

/** Event-fed, non-identifying text and icon evidence for the big clock's custom components.
 * Inputs come from existing SystemUI callbacks. This class owns no Android
 * service, listener, timer, provider, View or subscription object. */
final class BigClockSystemText {
    static final String SIGNAL_UNAVAILABLE = "Señal móvil sin datos";
    static final String NETWORK_UNAVAILABLE = "Red sin datos";
    static final String BATTERY_UNAVAILABLE = "Batería sin datos";

    enum Transport { UNKNOWN, NONE, WIFI, CELLULAR, ETHERNET, OTHER }
    enum Charging { UNKNOWN, CHARGING, DISCHARGING, NOT_CHARGING, FULL }

    static final class Snapshot {
        final String cellularSignal, networkStatus, battery;
        final long revision, cellularRevision, networkRevision, batteryRevision;
        final long subscriptionEpoch;
        final int subscription, signalLevel, batteryPercent;
        final int wifiResourceId, wifiLevel;
        final int cellularResourceId,secondaryCellularResourceId;
        final String cellularResourceName,secondaryCellularResourceName;
        final Boolean airplaneMode, simAbsent, inService, validated;
        final Boolean wifiVisible;
        final String dataGeneration, wifiResourceName;
        final Transport transport;
        final Charging charging;

        private Snapshot(String signal, String network, String battery, long revision,
                long cellularRevision, long networkRevision, long batteryRevision) {
            cellularSignal = signal; networkStatus = network; this.battery = battery;
            this.revision = revision; this.cellularRevision = cellularRevision;
            this.networkRevision = networkRevision; this.batteryRevision = batteryRevision;
            subscriptionEpoch = epoch; subscription = selectedSubscription;
            signalLevel = level; batteryPercent = percent; airplaneMode = airplane;
            simAbsent = absent;
            inService = service; validated = networkValidated; dataGeneration = generation;
            transport = route; charging = charge;
            wifiResourceId = wifiRes; wifiResourceName = wifiName;
            wifiLevel = wifiBars; wifiVisible = wifiShown;
            cellularResourceId=cellRes;secondaryCellularResourceId=secondaryRes;
            cellularResourceName=cellName;secondaryCellularResourceName=secondaryName;
        }

        /** Detached example values: no revision, no native resource, no subscription. */
        private Snapshot(String signal, String network, String battery, int level, int percent, Charging charge, Transport route,
                Boolean validated, String generation, int wifiBars, Boolean airplane, Boolean absent, Boolean service) {
            cellularSignal = signal; networkStatus = network; this.battery = battery;
            revision = cellularRevision = networkRevision = batteryRevision = 0L; subscriptionEpoch = 0L; subscription = -1;
            signalLevel = level; batteryPercent = percent; airplaneMode = airplane; simAbsent = absent;
            inService = service; this.validated = validated; dataGeneration = generation;
            transport = route; this.charging = charge;
            wifiResourceId = 0; wifiResourceName = ""; wifiLevel = wifiBars; wifiVisible = null;
            cellularResourceId = secondaryCellularResourceId = 0; cellularResourceName = secondaryCellularResourceName = "";
        }
    }

    private static long epoch;
    private static int selectedSubscription = -1, level = -1, percent = -1;
    private static Boolean airplane, absent, service, networkValidated;
    private static String generation = "";
    private static int wifiRes, wifiBars = -1;
    private static String wifiName = "";
    private static Boolean wifiShown;
    private static int cellRes,secondaryRes;
    private static String cellName="",secondaryName="";
    private static Transport route = Transport.UNKNOWN;
    private static Charging charge = Charging.UNKNOWN;
    private static volatile Snapshot current = new Snapshot(SIGNAL_UNAVAILABLE, NETWORK_UNAVAILABLE,
            BATTERY_UNAVAILABLE, 0L, 0L, 0L, 0L);
    private BigClockSystemText() { }

    static Snapshot snapshot() { return current; }
    static long revision() { return current.revision; }

    /** Return a token for this active-data observer lifecycle. Selecting a
     * different SIM clears only its cellular evidence, never Wi-Fi or battery. */
    static synchronized long selectSubscription(int subscription) {
        int next = subscription < 0 ? -1 : subscription;
        if (next == selectedSubscription && !(next >= 0 && Boolean.TRUE.equals(absent))) return epoch;
        selectedSubscription = next; epoch++;
        if (next >= 0) absent = false;
        service = null; level = -1; generation = "";
        publish(); return epoch;
    }

    static synchronized boolean service(long token, int subscription, Boolean inService) {
        if (!accepts(token, subscription) || Objects.equals(service, inService)) return false;
        service = inService;
        if (!Boolean.TRUE.equals(inService)) { level = -1; generation = ""; }
        return publish();
    }

    static synchronized boolean signal(long token, int subscription, Integer signalLevel) {
        if (!accepts(token, subscription) || Boolean.TRUE.equals(airplane) || Boolean.FALSE.equals(service)) return false;
        int next = signalLevel != null && signalLevel >= 0 && signalLevel <= 4 ? signalLevel : -1;
        if (next == level) return false;
        level = next; return publish();
    }

    /** Accept only known generation words; arbitrary native/user text cannot
     * smuggle an SSID, network identifier, number or address into this snapshot. */
    static synchronized boolean dataNetwork(long token, int subscription, String nativeGeneration) {
        if (!accepts(token, subscription) || Boolean.TRUE.equals(airplane) || Boolean.FALSE.equals(service)) return false;
        NetworkLabel.Level known = NetworkLabel.level(nativeGeneration);
        String next = known == null ? "" : known.text;
        if (next.equals(generation)) return false;
        generation = next; return publish();
    }

    static synchronized boolean airplane(Boolean enabled) {
        if (Objects.equals(airplane, enabled)) return false;
        airplane = enabled;
        // Old RF evidence must not reappear immediately when flight mode ends.
        if (Boolean.TRUE.equals(enabled)) { service = null; level = -1; generation = ""; }
        return publish();
    }

    /** Only explicit native absence evidence belongs here. A temporarily
     * missing active-data subscription is not proof that every SIM is absent. */
    static synchronized boolean simAbsent(Boolean value) {
        if (Objects.equals(absent, value)) return false;
        absent = value;
        if (Boolean.TRUE.equals(value)) { service = null; level = -1; generation = ""; }
        return publish();
    }

    /** validated is null until real default-network capabilities are known.
     * NONE is an explicit absent default route; UNKNOWN is not proof of offline. */
    static synchronized boolean network(Transport transport, Boolean validated) {
        Transport next = transport == null ? Transport.UNKNOWN : transport;
        Boolean confirmed = next == Transport.NONE || next == Transport.UNKNOWN ? null : validated;
        boolean retireWifi = next == Transport.NONE || next == Transport.UNKNOWN;
        Boolean nextWifiShown = next == Transport.NONE ? Boolean.FALSE : null;
        boolean wifiRetired = wifiRes == 0 && wifiBars == -1 && wifiName.isEmpty() && Objects.equals(wifiShown, nextWifiShown);
        if (next == route && Objects.equals(networkValidated, confirmed) && (!retireWifi || wifiRetired)) return false;
        route = next; networkValidated = confirmed;
        if (retireWifi) { wifiRes = 0; wifiName = ""; wifiBars = -1; wifiShown = nextWifiShown; }
        return publish();
    }

    /** The existing native Visible.getRes event supplies only its resource ID
     * and entry name. Never infer bars from a connection, SSID or an unknown
     * icon family; hidden/unknown observations discard previous icon evidence. */
    static synchronized boolean wifiIcon(Integer resourceId, String resourceName, Boolean visible) {
        int bars = wifiLevel(resourceName);
        int res = Boolean.TRUE.equals(visible) && resourceId != null && resourceId > 0 && bars >= 0
                ? resourceId : 0;
        String name = res == 0 ? "" : resourceName;
        if (res == 0) bars = -1;
        if (wifiRes == res && wifiBars == bars && wifiName.equals(name) && Objects.equals(wifiShown, visible)) return false;
        wifiRes = res; wifiName = name; wifiBars = bars; wifiShown = visible;
        return publish();
    }

    /** These IDs are the native binder's resolved single or complete stacked
     * resources. Telephony bars cannot reproduce soft-SIM/IMS/no-voice choices. */
    static synchronized boolean cellularIcons(int primary,String primaryName,int secondary,String secondName) {
        int first=primary>0&&primaryName!=null&&!primaryName.isEmpty()?primary:0;
        int second=first>0&&secondary>0&&secondName!=null&&!secondName.isEmpty()?secondary:0;
        String firstName=first==0?"":primaryName,nextSecondName=second==0?"":secondName;
        if(first==cellRes&&second==secondaryRes&&firstName.equals(cellName)&&nextSecondName.equals(secondaryName))return false;
        cellRes=first;secondaryRes=second;cellName=firstName;secondaryName=nextSecondName;return publish();
    }

    private static int wifiLevel(String resourceName) {
        if (resourceName == null) return -1;
        final String prefix = "stat_signal_wifi_signal_";
        int position = prefix.length();
        if (!resourceName.startsWith(prefix) || resourceName.length() <= position) return -1;
        char bar = resourceName.charAt(position);
        if (bar < '0' || bar > '4') return -1;
        return resourceName.length() == position + 1 || resourceName.length() == position + 6 && resourceName.endsWith("_os17")
                ? bar - '0' : -1;
    }

    /** Android BatteryManager status values: unknown=1, charging=2,
     * discharging=3, not-charging=4, full=5. Missing values remain unknown. */
    static synchronized boolean batteryRaw(int nativeLevel, int nativeScale, int nativeStatus) {
        int next = nativeScale > 0 && nativeLevel >= 0 && nativeLevel <= nativeScale
                ? (int) Math.round(nativeLevel * 100d / nativeScale) : -1;
        Charging charging;
        switch (nativeStatus) {
            case 2: charging = Charging.CHARGING; break;
            case 3: charging = Charging.DISCHARGING; break;
            case 4: charging = Charging.NOT_CHARGING; break;
            case 5: charging = Charging.FULL; break;
            default: charging = Charging.UNKNOWN; break;
        }
        if (percent == next && charge == charging) return false;
        percent = next; charge = charging; return publish();
    }

    /** Invalidate old callbacks even when the displayed values were empty. */
    static synchronized boolean clear() {
        epoch++; selectedSubscription = -1; level = percent = -1;
        airplane = absent = service = networkValidated = null;
        generation = ""; route = Transport.UNKNOWN; charge = Charging.UNKNOWN;
        wifiRes = 0; wifiName = ""; wifiBars = -1; wifiShown = null;
        cellRes=secondaryRes=0;cellName=secondaryName="";
        return publish();
    }

    private static boolean accepts(long token, int subscription) {
        return token == epoch && subscription >= 0 && subscription == selectedSubscription && !Boolean.TRUE.equals(absent);
    }

    private static boolean publish() {
        Snapshot previous = current;
        String signal = signalText(), network = networkText(), battery = batteryText();
        boolean cellularIconChanged=cellRes!=previous.cellularResourceId||secondaryRes!=previous.secondaryCellularResourceId
                ||!cellName.equals(previous.cellularResourceName)||!secondaryName.equals(previous.secondaryCellularResourceName);
        boolean cellularChanged = !signal.equals(previous.cellularSignal)||cellularIconChanged;
        boolean networkChanged = !network.equals(previous.networkStatus)
                || wifiRes != previous.wifiResourceId || wifiBars != previous.wifiLevel
                || !wifiName.equals(previous.wifiResourceName) || !Objects.equals(wifiShown, previous.wifiVisible)
                ||route==Transport.CELLULAR&&cellularIconChanged;
        boolean batteryChanged = !battery.equals(previous.battery);
        boolean changed = cellularChanged || networkChanged || batteryChanged;
        if (!changed && previous.subscriptionEpoch == epoch && previous.subscription == selectedSubscription
                && previous.signalLevel == level && previous.batteryPercent == percent
                && Objects.equals(previous.airplaneMode, airplane) && Objects.equals(previous.simAbsent, absent)
                && Objects.equals(previous.inService, service) && Objects.equals(previous.validated, networkValidated)
                && previous.dataGeneration.equals(generation) && previous.transport == route && previous.charging == charge) return false;
        current = new Snapshot(signal, network, battery, previous.revision + (changed ? 1L : 0L),
                previous.cellularRevision + (cellularChanged ? 1L : 0L),
                previous.networkRevision + (networkChanged ? 1L : 0L),
                previous.batteryRevision + (batteryChanged ? 1L : 0L));
        return changed;
    }

    private static String signalText() {
        return signalText(airplane, absent, selectedSubscription, service, level);
    }

    private static String signalText(Boolean airplaneMode, Boolean simAbsent, int subscription, Boolean inService, int bars) {
        if (Boolean.TRUE.equals(airplaneMode)) return "Modo avión";
        if (Boolean.TRUE.equals(simAbsent)) return "Sin tarjeta SIM";
        if (subscription < 0) return SIGNAL_UNAVAILABLE;
        if (Boolean.FALSE.equals(inService)) return "Sin servicio";
        if (!Boolean.TRUE.equals(inService) || bars < 0) return SIGNAL_UNAVAILABLE;
        return bars + "/4 barras";
    }

    private static String networkText() {
        return networkText(route, generation, networkValidated);
    }

    private static String networkText(Transport transport, String dataGeneration, Boolean validated) {
        String text;
        switch (transport) {
            case NONE: return "Sin conexión";
            case WIFI: text = "Wi-Fi"; break;
            case CELLULAR: text = dataGeneration.isEmpty() ? "Red móvil" : dataGeneration; break;
            case ETHERNET: text = "Ethernet"; break;
            case OTHER: text = "Otra red"; break;
            default: return NETWORK_UNAVAILABLE;
        }
        return Boolean.TRUE.equals(validated) ? text
                : Boolean.FALSE.equals(validated) ? text + " (conectividad limitada)" : text + " (estado de conexión desconocido)";
    }

    private static String batteryText() {
        return batteryText(percent, charge);
    }

    /** A detached example for the settings preview and for desktop checks. It is never published as live state. */
    static Snapshot sample(int batteryPercent, Charging charging, int signalLevel, Transport transport, Boolean validated,
            int wifiBars, String dataGeneration, Boolean airplaneMode, Boolean simAbsent, Boolean inService) {
        String generationText = dataGeneration == null ? "" : dataGeneration;
        return new Snapshot(signalText(airplaneMode, simAbsent, signalLevel >= 0 || inService != null ? 0 : -1, inService, signalLevel),
                networkText(transport, generationText, validated), batteryText(batteryPercent, charging),
                signalLevel, batteryPercent, charging, transport, validated, generationText, wifiBars, airplaneMode, simAbsent, inService);
    }

    private static String batteryText(int batteryPercent, Charging charging) {
        if (batteryPercent < 0) return BATTERY_UNAVAILABLE;
        String text = batteryPercent + "%";
        switch (charging) {
            case CHARGING: return text + " Cargando";
            case FULL: return text + " Carga completa";
            case NOT_CHARGING: return text + " Sin cargar";
            case UNKNOWN: return text + " Estado de carga desconocido";
            default: return text;
        }
    }
}
