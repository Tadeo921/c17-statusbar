// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.ArrayList;
import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Tracks native text and styling per view; one visible-view scheduler serves all time locations. */
public final class TextControls {
    public static final int NONE = 0, CLOCK = 1, CARRIER = 2;
    private final Handler handler;
    private final Map<TextView,Entry> views = Collections.synchronizedMap(new WeakHashMap<>());
    private final Object classificationLock = new Object();
    private final Map<TextView,Classification> classifications = new WeakHashMap<>();
    private final Map<View,Map<TextView,Boolean>> classificationDependents = new WeakHashMap<>();
    private final ThreadLocal<Integer> internal = ThreadLocal.withInitial(() -> 0);
    private Context context;
    private PowerManager power;
    // Written by the main-thread tick and by whichever thread applies a new configuration.
    private volatile boolean interactive = true, pending;
    /** Uptime at which the queued tick is due; a tick far past it was lost and must not block the scheduler. */
    private volatile long scheduledAt, lastPowerCheck, lastHealPost;
    private volatile boolean healPosted;
    private int healNotes, failureNotes;
    private static final long STALL_GRACE_MS = 3000L, POWER_CHECK_MS = 500L, HEAL_POST_MS = 1000L;
    private volatile Map<String, ClockStyle> clockStyles = clockStyles(new Bundle(), FeatureOptions.from(Collections.emptyMap()));
    private volatile Map<String, CarrierStyle> carrierStyles = carrierStyles(new Bundle());
    private volatile Map<String,Integer> colors = StatusBarSettings.COLOR_DEFAULTS;
    private volatile Map<String,Boolean> alpha = Collections.emptyMap();
    private volatile FeatureOptions features = FeatureOptions.from(Collections.emptyMap());
    private BooleanSupplier additionalClockVisible, additionalClockSeconds;
    private Runnable additionalClockUpdate;
    private Consumer<View> layoutAnchorObserver;

    /** Existing native clock bindings also locate roots created before optional layout hooks. */
    public void observeLayoutAnchors(Consumer<View> observer) {
        layoutAnchorObserver = observer;
        if (observer == null) return;
        ArrayList<TextView> tracked;
        synchronized (views) { tracked = new ArrayList<>(views.keySet()); }
        for (TextView view : tracked) if (view != null) observer.accept(view);
    }
    // refresh() re-arms itself, but returns early inside a native-style scope and can fail on one view;
    // the finally keeps the seconds display from stopping until the user touches something.
    private final Runnable tick = () -> {
        pending = false;
        try { refresh(false); } finally { schedule(); }
    };
    /** Digit cells and two-row spans live in the text, not its characters; any new configuration rebuilds them. */
    private volatile long layoutRevision;

    /** Values never retain their TextView key or any strong parent/ancestor reference. */
    private static final class Classification {
        final int id;
        final WeakReference<ViewParent> parent;
        final ArrayList<WeakReference<View>> ancestors = new ArrayList<>(8);
        int kind;
        boolean dirty, ancestry, shadeClock;
        String carrierGroup = "";
        Classification(TextView view) { id = view.getId(); parent = new WeakReference<>(view.getParent()); }
    }

    private static final class Entry {
        int kind, nativeTint;
        float nativeSize, nativeSpacing;
        boolean carrierPaddingOwned, carrierNativePadding;
        final Paint.FontMetrics carrierMetrics = new Paint.FontMetrics();
        final boolean statClock, legacyNativeMeasureMissing;
        final float initialNativeSize;
        final NativeClockMeasurement.AutoSize autoSize = new NativeClockMeasurement.AutoSize();
        final NativeClockMeasurement.Ellipsize ellipsize = new NativeClockMeasurement.Ellipsize();
        float resourceNativeSize = Float.NaN, resourceDensity = Float.NaN, resourceScaledDensity = Float.NaN;
        int measuredWidth = -1, availableWidth = -1, desiredWidth = -1;
        int incomingWidthMode = -1, incomingWidth = -1, nativeMeasuredWidth = -1, nativeActualWidth = -1, outgoingWidth = -1;
        boolean measureChanged;
        boolean layoutCacheRebuilt;
        float diagnosticNativeSize = Float.NaN, diagnosticAppliedSize = Float.NaN, diagnosticDensity = Float.NaN;
        Typeface nativeFace;
        String nativeAxes;
        CharSequence nativeText;
        CharSequence nativeDescription;
        long nativeTextVersion, appliedNativeTextVersion = -1;
        boolean textApplied, appliedReplacement, managed;
        boolean carrierSizeApplied, carrierSizePending, carrierFaceApplied, carrierSpacingApplied, carrierTintApplied;
        float carrierAppliedSize, carrierAppliedSpacing;
        int carrierAppliedTint;
        Typeface carrierAppliedFace;
        String carrierAppliedAxes;
        TextPaint carrierWidthPaint;
        CharSequence carrierWidthText;
        Typeface carrierWidthFace;
        String carrierWidthAxes;
        long carrierWidthRevision = -1;
        float carrierWidthSize, carrierWidthMinimum, carrierWidthAvailable, carrierWidthSpacing, carrierWidthPixelSpacing, carrierWidthResult;
        String group;
        // Two-row status clock: the native view draws the upper row, this entry owns the lower row.
        CharSequence lowerText;
        TextPaint lowerPaint;
        StaticLayout lowerLayout;
        CharSequence lowerPreparedText;
        ClockStyle lowerStyle;
        long lowerFontRevision=-1;
        float lowerPreparedSize,lowerPreparedSpacing,lowerPreparedDensity;
        Typeface lowerNativeFace;
        String lowerNativeAxes;
        float lowerWidth;
        final Paint.FontMetrics upperMetrics=new Paint.FontMetrics(),lowerMetrics=new Paint.FontMetrics();
        final ClockDual.Placement dualPlacement=new ClockDual.Placement();
        long appliedFontLayoutRevision=-1,appliedFontSourceRevision=-1;
        String fontGroup;
        boolean fontDual;
        Typeface fontNativeFace,fontAppliedFace;
        String fontNativeAxes,fontAppliedAxes;
        boolean dualPlaced;
        float upperDx, upperDy, lowerX, lowerBaseline;
        long appliedLayoutRevision = -1;
        View.OnLayoutChangeListener fitListener;
        String placeReason = "none", factKey;
        Entry(TextView view, int kind) {
            this.kind = kind; statClock = NativeClockMeasurement.statClock(view);
            legacyNativeMeasureMissing = statClock && NativeClockMeasurement.widthField(view.getClass()) == null;
            initialNativeSize = view.getTextSize(); capture(view); refreshClockBaseline(view, true);
        }
        void refreshClockBaseline(TextView view, boolean force) {
            if (!statClock) return;
            float density = view.getContext().getResources().getDisplayMetrics().density;
            float scaled = view.getContext().getResources().getDisplayMetrics().scaledDensity;
            if (!force && density == resourceDensity && scaled == resourceScaledDensity) return;
            resourceDensity = density; resourceScaledDensity = scaled;
            float source = NativeClockMeasurement.resourcePixels(view);
            if (Float.isFinite(source) && source > 0f) { resourceNativeSize = source; nativeSize = source; }
        }
        void captureStyle(TextView view) {
            float size = view.getTextSize();
            if (size > 0f && !Float.isNaN(size) && !Float.isInfinite(size)) nativeSize = size;
            refreshClockBaseline(view, true);
            nativeTint = view.getCurrentTextColor(); nativeFace = view.getTypeface();
            nativeAxes = view.getFontVariationSettings(); nativeSpacing = view.getLetterSpacing();
        }
        void capture(TextView view) {
            nativeTint = view.getCurrentTextColor();
            nativeSize = view.getTextSize(); nativeFace = view.getTypeface();
            nativeAxes = view.getFontVariationSettings();
            nativeSpacing = view.getLetterSpacing(); nativeText = view.getText();
            nativeDescription = view.getContentDescription();
            textApplied = appliedReplacement = managed = false;
            appliedNativeTextVersion = -1;
        }
    }

    private static final class CarrierStyle {
        final String mode, pattern, text, colorGroup;
        final boolean seconds;
        final float x, y, scale, spacing;
        final int weight;
        CarrierStyle(Bundle settings, String group) {
            Object storedMode = panelValue(settings, group, "mode");
            String selected = storedMode instanceof String ? (String) storedMode : "original";
            mode = selected.equals("time") || selected.equals("text") ? selected : "original";
            Object storedPattern = panelValue(settings, group, "pattern");
            pattern = validPattern(storedPattern instanceof String ? (String) storedPattern : null, TimeFormat.CARRIER_DEFAULT);
            seconds = TimeFormat.hasSeconds(pattern);
            Object storedText = panelValue(settings, group, "text");
            String input = storedText instanceof String ? ((String) storedText).replace('\n', ' ').replace('\r', ' ') : "";
            text = input.length() > TimeFormat.MAX_TEXT ? input.substring(0, TimeFormat.MAX_TEXT) : input;
            x = panelNumber(settings, group, "offset_x", 0, -80, 80);
            y = panelNumber(settings, group, "offset_y", 0, -24, 24);
            scale = panelNumber(settings, group, "scale", 100, 25, 250);
            weight = Math.round(panelNumber(settings, group, "weight", 600, 1, 1000));
            spacing = panelNumber(settings, group, "spacing", 0, -2, 8);
            // Direct callers supplying only old keys keep their old palette. The provider sends
            // explicit panel keys (including migrated colors), so independently saved colors win.
            colorGroup = CarrierPanels.isPanel(group) && CarrierPanels.legacyKey(CarrierPanels.key(group, "mode")) != null
                    && !hasPanelConfiguration(settings, group) ? "carrier" : group;
        }
    }

    private static final class ClockStyle {
        final boolean enabled, seconds;
        final String pattern;
        final float x, y, scale, spacing;
        final int weight;
        final int scaleBasisVersion;
        final float legacyScaleFactor;
        // Digit cells, the two-row layout and the outer margins (status-bar clock only for the last two).
        final boolean digitFixed, colonCentered, dual, margins, dualColor;
        final float digitSpacing, upperScale, lowerScale, lineGap, marginStart, marginEnd, alignFraction;
        final float dualX, dualY, dualSpacing;
        final int upperWeight, lowerWeight;
        final String upperPattern, lowerPattern;
        ClockStyle(Bundle settings, String group, FeatureOptions features) {
            boolean status = ClockLayoutSettings.STATUS.equals(group);
            // The two-row clock needs only its own switch; it never depends on the default clock's.
            dual = status && features.dualClock();
            margins = dual && features.isEnabled(ClockLayoutSettings.MARGIN_ENABLED);
            dualColor = dual && features.isEnabled(ClockLayoutSettings.DUAL_COLOR_ENABLED);
            enabled = features.effective(group, group + "_enabled");
            pattern = validPattern(settings.getString(group + "_pattern"), TimeFormat.CLOCK_DEFAULT);
            upperPattern = validPattern(settings.getString(ClockLayoutSettings.DUAL_UPPER_PATTERN), ClockLayoutSettings.DEFAULT_UPPER_PATTERN);
            lowerPattern = validPattern(settings.getString(ClockLayoutSettings.DUAL_LOWER_PATTERN), ClockLayoutSettings.DEFAULT_LOWER_PATTERN);
            seconds = TimeFormat.hasSeconds(pattern)
                    || dual && (TimeFormat.hasSeconds(upperPattern) || TimeFormat.hasSeconds(lowerPattern));
            // Fixed digit cells only matter when something changes every second.
            digitFixed = seconds && (features.effective(group, ClockLayoutSettings.digitKey(group))
                    || dual && features.isEnabled(ClockLayoutSettings.digitKey(group)));
            dualX = value(settings, ClockLayoutSettings.DUAL_OFFSET_X, 0, -80, 80);
            dualY = value(settings, ClockLayoutSettings.DUAL_OFFSET_Y, 0, -24, 24);
            dualSpacing = value(settings, ClockLayoutSettings.DUAL_SPACING, 0, -2, 8);
            colonCentered = features.effective(group, ClockLayoutSettings.colonKey(group))
                    || dual && features.isEnabled(ClockLayoutSettings.colonKey(group));
            digitSpacing = value(settings, ClockLayoutSettings.digitSpacingKey(group), 0, -2, 8);
            upperScale = Math.max(1f, value(settings, ClockLayoutSettings.DUAL_UPPER_SCALE, ClockLayoutSettings.DEFAULT_ROW_SCALE, 25, 250));
            lowerScale = Math.max(1f, value(settings, ClockLayoutSettings.DUAL_LOWER_SCALE, ClockLayoutSettings.DEFAULT_ROW_SCALE, 25, 250));
            upperWeight = Math.round(value(settings, ClockLayoutSettings.DUAL_UPPER_WEIGHT, 600, 1, 1000));
            lowerWeight = Math.round(value(settings, ClockLayoutSettings.DUAL_LOWER_WEIGHT, 500, 1, 1000));
            lineGap = value(settings, ClockLayoutSettings.DUAL_LINE_GAP, 0, -8, 16);
            marginStart = value(settings, ClockLayoutSettings.MARGIN_START, 0, 0, 40);
            marginEnd = value(settings, ClockLayoutSettings.MARGIN_END, 0, 0, 40);
            Object align = settings.get(ClockLayoutSettings.DUAL_ALIGN);
            alignFraction = ClockLayoutSettings.alignFraction(ClockLayoutSettings.validAlign(align) ? (String) align : ClockLayoutSettings.ALIGN_CENTER);
            x = value(settings, group + "_offset_x", 0, -80, 80);
            y = value(settings, group + "_offset_y", 0, -24, 24);
            scale = value(settings, group + "_scale", 100, 25, 250);
            scaleBasisVersion = "clock".equals(group) ? Math.round(NumericPolicy.finite(settings.get(NativeClockMeasurement.SCALE_BASIS_VERSION), 1f)) : 2;
            legacyScaleFactor = "clock".equals(group) ? NumericPolicy.finite(settings.get(NativeClockMeasurement.LEGACY_SCALE_FACTOR), Float.NaN) : Float.NaN;
            weight = Math.round(value(settings, group + "_weight", 600, 1, 1000));
            spacing = value(settings, group + "_spacing", 0, -2, 8);
        }
    }

    private static Map<String, ClockStyle> clockStyles(Bundle settings, FeatureOptions features) {
        Map<String, ClockStyle> result = new LinkedHashMap<>();
        for (String group : new String[]{"clock", "shade_clock", PanelMode.CLOCK}) result.put(group, new ClockStyle(settings, group, features));
        return Collections.unmodifiableMap(result);
    }

    private ClockStyle clockStyle(String group) {
        ClockStyle style = clockStyles.get(group);
        return style == null ? clockStyles.get("clock") : style;
    }

    private static Map<String, CarrierStyle> carrierStyles(Bundle settings) {
        Map<String, CarrierStyle> result = new LinkedHashMap<>();
        result.put("carrier", new CarrierStyle(settings, "carrier"));
        for (String group : CarrierPanels.ALL_GROUPS) result.put(group, new CarrierStyle(settings, group));
        return Collections.unmodifiableMap(result);
    }

    private CarrierStyle carrierStyle(String group) {
        CarrierStyle style = carrierStyles.get(group);
        return style == null ? carrierStyles.get("carrier") : style;
    }

    private static Object panelValue(Bundle settings, String group, String suffix) {
        String key = CarrierPanels.isPanel(group) ? CarrierPanels.key(group, suffix) : "carrier_" + suffix;
        Object value = settings.get(key);
        if (value == null && CarrierPanels.isPanel(group)) {
            String legacy = CarrierPanels.legacyKey(key);
            if (legacy != null) value = settings.get(legacy);
        }
        return value;
    }

    private static float panelNumber(Bundle settings, String group, String suffix, float fallback, float min, float max) {
        Object stored = panelValue(settings, group, suffix);
        String key = CarrierPanels.isPanel(group) ? CarrierPanels.key(group, suffix) : "carrier_" + suffix;
        return NumericPolicy.setting(key, stored, fallback);
    }

    private static boolean hasPanelConfiguration(Bundle settings, String group) {
        for (String suffix : new String[]{"mode", "pattern", "text", "enabled", "replace_enabled", "position_enabled",
                "size_enabled", "color_enabled", "text_style_enabled", "offset_x", "offset_y", "scale", "weight", "spacing",
                "color_light", "color_dark", "color_light_custom_alpha", "color_dark_custom_alpha"}) {
            if (settings.get(CarrierPanels.key(group, suffix)) != null) return true;
        }
        return false;
    }

    public TextControls(Handler handler) { this.handler = handler; }
    /**
     * Set only on the thread running the StatClock measure hook. Its style swap around OEM code then writes Paint
     * directly: the TextView setters would request a layout on every measure and keep the status bar traversing.
     * prepareClockMeasure drops the stale Layout once, so the final measure still sees the applied style.
     */
    private final ThreadLocal<Boolean> paintOnly = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private void writeSize(TextView view, float pixels) {
        if (!paintOnly.get()) { view.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixels); return; }
        Paint paint = view.getPaint();
        if (paint.getTextSize() != pixels) paint.setTextSize(pixels);
    }

    private void writeSpacing(TextView view, float spacing) {
        if (!paintOnly.get()) { view.setLetterSpacing(spacing); return; }
        Paint paint = view.getPaint();
        if (paint.getLetterSpacing() != spacing) paint.setLetterSpacing(spacing);
    }

    private void restoreFace(TextView view, Typeface face, String axes) {
        if (paintOnly.get()) FontWeight.restore(view.getPaint(), face, axes);
        else FontWeight.restore(view, face, axes);
    }

    private long writeFace(TextView view, Typeface face, int weight, String axes, String scope) {
        return paintOnly.get() ? FontRepository.applyRevision(view.getPaint(), face, weight, axes, scope)
                : FontRepository.applyRevision(view, face, weight, axes, scope);
    }

    /** Font locations follow the native host even when shade controls follow the status clock. */
    private String fontScope(TextView view, Entry entry) {
        if (entry.kind == CLOCK) return classification(view).shadeClock ? FontScopes.SHADE_CLOCK : FontScopes.STATUSBAR_CLOCK;
        return CarrierPanels.LOCKSCREEN.equals(entry.group) ? FontScopes.LOCKSCREEN_TEXT : FontScopes.SHADE_TEXT;
    }

    public boolean isInternal() { return internal.get() != 0; }
    public void enter() { internal.set(internal.get() + 1); }
    public void exit() { internal.set(Math.max(0, internal.get() - 1)); }

    public static int namedKind(String className, String resourceName, String parentClass) {
        if (className.equals("com.oplus.systemui.statusbar.widget.StatClock")) return CLOCK;
        if (isShadeClockClass(className)) return CLOCK;
        if (isShadeClockResource(resourceName) && isShadeParent(parentClass)) return CLOCK;
        if (className.equals("com.android.systemui.statusbar.policy.Clock")) {
            String parent = parentClass.toLowerCase(Locale.ROOT);
            if (parent.contains("qs") || parent.contains("shade") || parent.contains("keyguard")) return NONE;
            return oneOf(resourceName, "clock", "clock_left", "clock_right", "clock_center", "status_bar_clock") ? CLOCK : NONE;
        }
        if (className.equals("com.oplus.systemui.qs.widget.OplusSecondCarrierText")) return CARRIER;
        // OplusQSCarrierText is the data-usage field, despite its misleading class name.
        if (className.equals("com.oplus.systemui.qs.widget.OplusQSCarrierText")) return NONE;
        if (className.equals("com.oplus.systemui.statusbar.widget.OplusStatCarrierText")
                || className.equals("com.android.keyguard.CarrierText")) return CARRIER;
        if (isShadeCarrierResource(resourceName) && isShadeParent(parentClass)) return CARRIER;
        if (isLockscreenCarrierResource(resourceName) && isLockscreenParent(parentClass)) return CARRIER;
        if (parentClass.equals("com.android.systemui.shade.carrier.ShadeCarrier")
                && (resourceName.equals("carrier_text") || resourceName.equals("carrier_name"))) return CARRIER;
        return NONE;
    }

    private static boolean isShadeClockClass(String className) {
        return className.equals("com.oplus.systemui.qs.widget.OplusQSClock")
                || className.equals("com.oplus.systemui.qs.widget.SimpleQsClock")
                || className.equals("com.oplus.systemui.qs.fake.view.QsClock");
    }

    private static boolean isShadeClockResource(String resourceName) {
        return resourceName.equals("qs_footer_clock") || resourceName.equals("oplus_qs_clock");
    }
    private static boolean isShadeCarrierResource(String resourceName) {
        return resourceName.equals("qs_carrier_text") || resourceName.equals("qs_header_carrier_text");
    }
    private static boolean oneOf(String value, String... choices) {
        for (String choice : choices) if (value.equals(choice)) return true;
        return false;
    }

    private static boolean isShadeClock(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
            if (isShadeClockClass(type.getName())) return true;
        return false;
    }

    private static boolean isShadeParent(String className) {
        String name = className.toLowerCase(Locale.ROOT);
        return className.equals("com.oplus.systemui.separate.OplusQSSimpleHeader")
                || name.contains(".qs.") || name.contains(".shade.") || name.endsWith("qsheader")
                || name.contains("quickstatusbarheader") || name.contains("shadestatusbar");
    }

    private static boolean isShadeResource(String resourceName) {
        return oneOf(resourceName, "qs_clock_container", "qs_header", "header_container", "simple_qs_container", "simple_qs_footer",
                "qs_container_area_layout", "qs_status_bar_container_layout", "qs_panel", "quick_qs_panel");
    }

    private static boolean isLockscreenParent(String className) {
        return className.equals("com.android.systemui.statusbar.phone.KeyguardStatusBarView")
                || className.equals("com.oplus.systemui.statusbar.widget.FixedKeyguardStatusBarView")
                || className.equals("com.oplus.systemui.statusbar.widget.KeyguardStartSideContentLayout")
                || className.equals("com.android.keyguard.KeyguardStatusView");
    }

    private static boolean isLockscreenParent(Class<?> source) {
        for (Class<?> type = source; type != null; type = type.getSuperclass())
            if (isLockscreenParent(type.getName())) return true;
        return false;
    }

    private static boolean isLockscreenResource(String resourceName) {
        return oneOf(resourceName, "keyguard_header", "keyguard_status_bar_contents", "keyguard_status_view", "keyguard_status_area");
    }

    private static boolean isLockscreenCarrierResource(String resourceName) {
        return oneOf(resourceName, "keyguard_carrier_text", "oplus_keyguard_carrier_text", "lockscreen_carrier_text", "carrier_text", "carrier_name");
    }

    private static boolean isLockscreenCarrier(View view) {
        return namedClass(view.getClass(), "com.android.keyguard.CarrierText")
                || namedClass(view.getClass(), "com.oplus.systemui.statusbar.widget.OplusStatCarrierText");
    }

    public int kind(View view) {
        if (!(view instanceof TextView)) return NONE;
        return classification((TextView) view).kind;
    }

    /** Call only after native ID/parent or real header lifecycle mutations, never from draw. */
    public void classificationChanged(View view) {
        if (view == null) return;
        synchronized (classificationLock) {
            Classification own = classifications.get(view);
            if (own != null) own.dirty = true;
            Map<TextView,Boolean> dependents = classificationDependents.get(view);
            if (dependents != null) for (TextView text : dependents.keySet()) {
                Classification value = classifications.get(text);
                if (value != null) value.dirty = true;
            }
        }
    }

    private Classification classification(TextView view) {
        Classification resolved;
        synchronized (classificationLock) {
            Classification previous = classifications.get(view);
            if (previous != null && !previous.dirty && previous.id == view.getId()
                    && previous.parent.get() == view.getParent()) return previous;
            if (previous != null) for (WeakReference<View> reference : previous.ancestors) {
                View ancestor = reference.get();
                Map<TextView,Boolean> dependents = ancestor == null ? null : classificationDependents.get(ancestor);
                if (dependents != null) dependents.remove(view);
            }
            resolved = new Classification(view);
            resolved.kind = resolveKind(view, resolved);
            if (resolved.kind == CLOCK) resolved.shadeClock = isShadeClockLocation(view);
            if (resolved.kind == CARRIER) resolved.carrierGroup = resolveCarrierGroup(view);
            if (resolved.ancestry) {
                ViewParent parent = view.getParent();
                for (int depth = 0; parent instanceof View && depth < 16; depth++) {
                    View ancestor = (View) parent;
                    resolved.ancestors.add(new WeakReference<>(ancestor));
                    Map<TextView,Boolean> dependents = classificationDependents.get(ancestor);
                    if (dependents == null) {
                        dependents = new WeakHashMap<>(); classificationDependents.put(ancestor, dependents);
                    }
                    dependents.put(view, Boolean.TRUE); parent = ancestor.getParent();
                }
            }
            classifications.put(view, resolved);
        }
        // No inverse lock ordering: the dependency index lock is released before native restoration.
        synchronized (views) {
            Entry entry = views.get(view);
            if (entry != null) {
                if (entry.kind == NONE && resolved.kind != NONE) entry.capture(view);
                entry.kind = resolved.kind; entry.group = selectedGroup(resolved);
                if (entry.managed) apply(view, entry, System.currentTimeMillis(), true);
            }
        }
        return resolved;
    }

    private int resolveKind(View view, Classification classification) {
        String name = view.getClass().getName();
        if (namedClass(view.getClass(), "com.oplus.systemui.qs.widget.OplusQSCarrierText")) return NONE;
        if (view instanceof TextView && NativeClockMeasurement.statClock((TextView)view)) return CLOCK;
        // Real and animation shade clocks belong to one clock family; group() selects
        // status-following or independent shade settings. Carrier replacements stay separate.
        if (isShadeClock(view)) return CLOCK;
        if (namedClass(view.getClass(), "com.oplus.systemui.qs.widget.OplusSecondCarrierText")) { classification.ancestry = true; return CARRIER; }
        if (isLockscreenCarrier(view)) { classification.ancestry = true; return CARRIER; }
        String parent = view.getParent() == null ? "" : view.getParent().getClass().getName();
        String resource = "";
        String resourcePackage = "";
        try { if (view.getId() != View.NO_ID) {
            resource = view.getResources().getResourceEntryName(view.getId());
            resourcePackage = view.getResources().getResourcePackageName(view.getId());
        } }
        catch (Exception ignored) { }
        int result = namedKind(name, resource, parent);
        boolean shadeClock = isShadeClockResource(resource);
        boolean shadeCarrier = isShadeCarrierResource(resource);
        boolean lockCarrier = isLockscreenCarrierResource(resource);
        classification.ancestry = name.equals("com.android.systemui.statusbar.policy.Clock")
                || "com.android.systemui".equals(resourcePackage) && (shadeClock || shadeCarrier || lockCarrier);
        if (result != NONE && ((result == CLOCK && !shadeClock) || "com.android.systemui".equals(resourcePackage))) return result;
        // Both separated notification and settings headers can inflate ordinary TextViews.
        // Match the SystemUI ID and its header ancestry rather than every carrier-looking text.
        if (!"com.android.systemui".equals(resourcePackage) || (!shadeClock && !shadeCarrier && !lockCarrier)) return NONE;
        ViewParent ancestor = view.getParent();
        for (int depth = 0; ancestor != null && depth < 16; depth++) {
            if (shadeClock && isShadeParent(ancestor.getClass().getName())) return CLOCK;
            if (shadeCarrier && isShadeParent(ancestor.getClass().getName())) return CARRIER;
            if (lockCarrier && isLockscreenParent(ancestor.getClass())) return CARRIER;
            if (!(ancestor instanceof View)) break;
            View owner = (View) ancestor;
            try {
                if (owner.getId() != View.NO_ID
                        && "com.android.systemui".equals(owner.getResources().getResourcePackageName(owner.getId()))) {
                    String ownerId = owner.getResources().getResourceEntryName(owner.getId());
                    if (shadeClock && (isShadeResource(ownerId)
                            || oneOf(ownerId, "qs_fake_clock_container", "oplus_fake_clock_container", "separateqs_fake_status_layout"))) return CLOCK;
                    if ((shadeCarrier && isShadeResource(ownerId)) || (lockCarrier && isLockscreenResource(ownerId))) return CARRIER;
                }
            } catch (Exception ignored) { }
            ancestor = owner.getParent();
        }
        return NONE;
    }

    /** Current owner, resolved again after header recreation or view reparenting. */
    public String group(View view) {
        return view instanceof TextView ? selectedGroup(classification((TextView) view)) : "";
    }
    private String selectedGroup(Classification classification) {
        if (PanelMode.classic()) {
            if (classification.kind == CLOCK && classification.shadeClock) return PanelMode.CLOCK;
            if (classification.kind == CARRIER && (CarrierPanels.NOTIFICATION.equals(classification.carrierGroup)
                    || CarrierPanels.CONTROL.equals(classification.carrierGroup))) return CarrierPanels.CLASSIC;
        }
        if (classification.kind == CLOCK) return features.enabled("shade_clock") && classification.shadeClock ? "shade_clock" : "clock";
        return classification.kind == CARRIER ? classification.carrierGroup : "";
    }
    private static String resolveCarrierGroup(View view) {
        boolean control = false, lockscreen = isLockscreenCarrier(view);
        ViewParent ancestor = view.getParent();
        for (int depth = 0; ancestor != null && depth < 16; depth++) {
            if (namedClass(ancestor.getClass(), "com.oplus.systemui.separate.OplusQSSimpleHeader")) return CarrierPanels.NOTIFICATION;
            if (namedClass(ancestor.getClass(), "com.oplus.systemui.qs.OplusQuickStatusBarHeader")) control = true;
            if (isLockscreenParent(ancestor.getClass())) lockscreen = true;
            if (!(ancestor instanceof View)) break;
            View owner = (View) ancestor;
            try {
                if (owner.getId() != View.NO_ID && "com.android.systemui".equals(owner.getResources().getResourcePackageName(owner.getId()))) {
                    String id = owner.getResources().getResourceEntryName(owner.getId());
                    if (id.equals("simple_qs_container") || id.equals("simple_qs_footer")) return CarrierPanels.NOTIFICATION;
                    if (id.equals("qs_status_bar_container_layout") || id.equals("qs_container_area_layout")) control = true;
                    if (isLockscreenResource(id)) lockscreen = true;
                }
            } catch (Exception ignored) { }
            ancestor = owner.getParent();
        }
        // A notification header contains a QuickStatusBarHeader too: the outer SimpleHeader wins.
        return control ? CarrierPanels.CONTROL : lockscreen ? CarrierPanels.LOCKSCREEN : "carrier";
    }

    private static boolean isShadeClockLocation(View view) {
        if (isShadeClock(view)) return true;
        try {
            return view.getId() != View.NO_ID
                    && "com.android.systemui".equals(view.getResources().getResourcePackageName(view.getId()))
                    && isShadeClockResource(view.getResources().getResourceEntryName(view.getId()));
        } catch (Exception ignored) { return false; }
    }

    /** Measures follow the selected owner even when the status-bar clock group is disabled. */
    public boolean clockControlsEnabled(TextView view) {
        if (kind(view) != CLOCK) return false;
        if (features.enabled(group(view))) return true;
        Entry entry = entry(view);
        return entry != null && dualActive(entry);
    }

    /** True when this module owns the entry's text style: its group is on, or it is the two-row status clock. */
    private boolean managed(Entry entry) {
        return features.enabled(group(entry)) || dualActive(entry);
    }

    private static boolean namedClass(Class<?> source, String name) {
        for (Class<?> type = source; type != null; type = type.getSuperclass()) if (name.equals(type.getName())) return true;
        return false;
    }

    private Entry entry(TextView view) {
        Classification classification = classification(view);
        if (classification.kind == NONE) return null;
        synchronized (views) {
            Entry entry = views.get(view);
            if (entry == null) {
                entry = new Entry(view, classification.kind); views.put(view, entry);
            }
            entry.group = selectedGroup(classification);
            return entry;
        }
    }

    public void attach(TextView view) {
        if (context == null) {
            context = view.getContext().getApplicationContext();
            if (context == null) context = view.getContext();
            power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power != null) interactive = power.isInteractive();
        }
        Entry entry = entry(view);
        if (entry != null && layoutAnchorObserver != null) layoutAnchorObserver.accept(view);
        if (entry != null && !isInternal()) { apply(view, entry, System.currentTimeMillis()); schedule(); }
    }

    /** Recreated header copies may use ordinary TextViews instead of the original widget class. */
    public void attachTree(View header) { classificationChanged(header); attachTree(header, 0); }

    private void attachTree(View view, int depth) {
        if (view == null || depth > 8) return;
        if (view instanceof TextView && kind(view) != NONE) attach((TextView) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) attachTree(group.getChildAt(i), depth + 1);
        }
    }

    public void detach(TextView view) {
        classificationChanged(view);
        // Weak entries survive reattachment, preserving the last real carrier/clock text.
        restart();
    }

    public boolean replaces(TextView view) {
        int kind = kind(view);
        String group = group(view);
        if (kind == CLOCK) {
            if (clockStyle(group).enabled) return true;
            Entry entry = entry(view);
            return entry != null && dualActive(entry);
        }
        return kind == CARRIER && carrierReplacement(group) && !carrierStyle(group).mode.equals("original");
    }

    private boolean carrierReplacement(String group) { return features.effective(group, group + "_replace_enabled"); }
    private static String group(Entry entry) { return entry.group; }

    public Typeface nativeFamily() {
        synchronized (views) {
            for (Map.Entry<TextView,Entry> item : views.entrySet()) {
                if (item.getValue().kind == CLOCK && item.getKey().isAttachedToWindow()
                        && item.getKey().isShown() && item.getValue().nativeFace != null) return item.getValue().nativeFace;
            }
        }
        return Typeface.DEFAULT;
    }

    public float styledSize(TextView view) {
        Entry entry = entry(view);
        return entry == null ? view.getTextSize() : fitted(view, entry, textSize(entry));
    }

    /** A native setter keeps its original unit and value when this location's size is off. */
    public boolean sizeControlsEnabled(TextView view) {
        if (kind(view) == NONE) return false;
        Entry entry = entry(view);
        return features.size(group(view)) || entry != null && dualActive(entry);
    }

    /** Two rows are only drawn for the OEM status clock; every other clock keeps its single row. */
    private boolean dualActive(Entry entry) {
        return entry.kind == CLOCK && entry.statClock && ClockLayoutSettings.STATUS.equals(entry.group)
                && clockStyle(entry.group).dual;
    }

    private float textSize(Entry entry) {
        // The upper row of a two-row clock has its own size and replaces the single-row scale.
        if (dualActive(entry)) return ClockLayoutSettings.rowPixels(entry.nativeSize, clockStyle(entry.group).upperScale);
        // Inactive size controls pass native pixels through, including a native
        // zero-size transition. Legacy compensation belongs only to user scaling.
        if (!features.size(group(entry))) return entry.nativeSize;
        double scale = (entry.kind == CLOCK ? clockStyle(entry.group).scale : carrierStyle(entry.group).scale) / 100d;
        if (entry.statClock && "clock".equals(entry.group)) scale *= scaleFactor(entry);
        return NumericPolicy.textPixels(entry.nativeSize * scale);
    }

    /** The status bar is a fixed-height strip, so a single-row clock whose line box is taller than the strip is cut off at the
     * bottom. The requested size is kept while its line fits; otherwise the largest size whose line fits is used. The strip's
     * height is only known after the first layout, hence the layout listener that asks for one more measure. */
    static float fitHeight(TextView view, float size, int viewHeight) {
        if (!(size > 0f) || viewHeight <= 0) return size;
        float current = view.getPaint().getTextSize();
        if (!(current > 0f)) return size;
        android.graphics.Paint.FontMetrics metrics = new android.graphics.Paint.FontMetrics();
        view.getPaint().getFontMetrics(metrics);
        float line = view.getIncludeFontPadding() ? metrics.bottom - metrics.top : metrics.descent - metrics.ascent;
        float available = viewHeight - view.getCompoundPaddingTop() - view.getCompoundPaddingBottom();
        if (!(line > 0f) || !(available > 0f)) return size;
        float limit = available * current / line;
        return size > limit ? Math.max(1f, limit) : size;
    }
    /**
     * A carrier is one line of glyphs. A variable font's line box (top to bottom, with its font padding) can be close to three
     * times the font size, which would cap every enlargement at the native size; only the ink has to fit the fixed row.
     */
    static float fitInkHeight(TextView view, float size, int viewHeight) {
        if (!(size > 0f) || viewHeight <= 0) return size;
        CharSequence text = view.getText();
        float available = viewHeight - view.getCompoundPaddingTop() - view.getCompoundPaddingBottom();
        if (text == null || text.length() == 0 || !(available > 0f)) return size;
        TextPaint paint = new TextPaint();
        paint.set(view.getPaint()); paint.setTextSize(size);
        android.graphics.Rect ink = new android.graphics.Rect(); String value = text.toString();
        paint.getTextBounds(value, 0, value.length(), ink);
        float height = ink.height();
        return height > available ? Math.max(1f, size * available / height) : size;
    }
    private float fitted(TextView view, Entry entry, float size) {
        if (entry.kind == CARRIER) return fittedCarrier(view, entry, size);
        if (!entry.statClock || dualActive(entry)) return size;
        if (entry.fitListener == null) {
            entry.fitListener = (changed, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (bottom - top == oldBottom - oldTop || isInternal() || !(changed instanceof TextView)) return;
                TextView clock = (TextView) changed;
                Entry owner = entry(clock);
                if (owner == null || !managed(owner)) return;
                float wanted = fitHeight(clock, textSize(owner), bottom - top);
                if (Math.abs(clock.getPaint().getTextSize() - wanted) > .5f) clock.requestLayout();
            };
            view.addOnLayoutChangeListener(entry.fitListener);
        }
        return fitHeight(view, size, view.getHeight() > 0 ? view.getHeight() : view.getMeasuredHeight());
    }

    /** The native carrier size is the 100% baseline; a replacement font never makes that baseline smaller. */
    private float fittedCarrier(TextView view, Entry entry, float size) {
        if (size <= entry.nativeSize + .001f) return size;
        if (!features.enabled(entry.group) || !(features.size(entry.group) || features.textStyle(entry.group)
                || features.enabled("font") && !FontRepository.systemMode(fontScope(view, entry)))) return size;
        if (!CarrierPanels.NOTIFICATION.equals(entry.group) && !CarrierPanels.CONTROL.equals(entry.group)
                && !CarrierPanels.CLASSIC.equals(entry.group)) return size;
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null || params.height == ViewGroup.LayoutParams.WRAP_CONTENT) return size;
        if (entry.fitListener == null) {
            entry.fitListener = (changed, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (bottom - top == oldBottom - oldTop && right - left == oldRight - oldLeft
                        || isInternal() || !(changed instanceof TextView)) return;
                TextView carrier = (TextView) changed; Entry owner = entry(carrier);
                if (owner != null) applyCarrierStyle(carrier, owner);
            };
            view.addOnLayoutChangeListener(entry.fitListener);
        }
        int height = view.getHeight() > 0 ? view.getHeight() : view.getMeasuredHeight();
        // A larger request may use the fixed row's remaining height, but can never end up smaller than 100%.
        float fitted = Math.max(entry.nativeSize, fitInkHeight(view, size, height));
        return fittedCarrierWidth(view, entry, fitted);
    }

    /** Fit only the enlarged portion of a replacement into its real fixed QS slot; 100% stays native. */
    private float fittedCarrierWidth(TextView view, Entry entry, float size) {
        if (size <= entry.nativeSize + .001f || !carrierReplacement(entry.group)
                || carrierStyle(entry.group).mode.equals("original")) return size;
        int width = view.getWidth() > 0 ? view.getWidth() : view.getMeasuredWidth();
        float x = features.position(entry.group) ? NumericPolicy.pixels(carrierStyle(entry.group).x, density(view)) : 0f;
        // A positive local X consumes the right edge in LTR, a negative X the left edge in RTL.
        float consumed = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? Math.max(0f, -x) : Math.max(0f, x);
        float available = width - view.getCompoundPaddingLeft() - view.getCompoundPaddingRight() - consumed;
        CharSequence text = view.getText();
        if (width <= 0 || !Float.isFinite(available) || available <= 0f || text == null || text.length() == 0) return size;
        float spacing = NumericPolicy.pixels(features.textStyle(entry.group) ? carrierStyle(entry.group).spacing : 0f, density(view));
        Typeface face = view.getTypeface(); String axes = view.getFontVariationSettings();
        if (entry.carrierWidthRevision == layoutRevision && entry.carrierWidthFace == face
                && sameAxes(entry.carrierWidthAxes, axes) && sameCharacters(entry.carrierWidthText, text)
                && entry.carrierWidthSize == size && entry.carrierWidthMinimum == entry.nativeSize
                && entry.carrierWidthAvailable == available && entry.carrierWidthSpacing == entry.nativeSpacing
                && entry.carrierWidthPixelSpacing == spacing)
            return entry.carrierWidthResult;
        if (entry.carrierWidthPaint == null) entry.carrierWidthPaint = new TextPaint();
        TextPaint paint = entry.carrierWidthPaint; paint.set(view.getPaint());
        float result = size;
        if (carrierWidth(text, paint, size, entry.nativeSpacing, spacing) > available) {
            float lower = entry.nativeSize, upper = size;
            if (carrierWidth(text, paint, lower, entry.nativeSpacing, spacing) <= available) {
                for (int i = 0; i < 20; i++) {
                    float middle = (lower + upper) * .5f;
                    if (carrierWidth(text, paint, middle, entry.nativeSpacing, spacing) <= available) lower = middle;
                    else upper = middle;
                }
            }
            result = lower;
        }
        entry.carrierWidthRevision = layoutRevision; entry.carrierWidthFace = face; entry.carrierWidthAxes = axes;
        entry.carrierWidthText = text.toString(); entry.carrierWidthSize = size; entry.carrierWidthMinimum = entry.nativeSize;
        entry.carrierWidthAvailable = available; entry.carrierWidthSpacing = entry.nativeSpacing; entry.carrierWidthPixelSpacing = spacing;
        return entry.carrierWidthResult = result;
    }

    private static float carrierWidth(CharSequence text, TextPaint paint, float size, float nativeSpacing, float pixels) {
        paint.setTextSize(size); paint.setLetterSpacing(nativeSpacing + pixels / Math.max(1f, size));
        return Layout.getDesiredWidth(text, 0, text.length(), paint);
    }

    private float scaleFactor(Entry entry) {
        ClockStyle clock = clockStyle(entry.group);
        return NativeClockMeasurement.legacyFactor(clock.scaleBasisVersion, clock.legacyScaleFactor,
                entry.legacyNativeMeasureMissing, entry.initialNativeSize, entry.resourceNativeSize, clock.scale);
    }

    public void beforeMeasure(TextView view) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null) { entry.refreshClockBaseline(view, false); applyStyle(view, entry); }
    }

    /** Native onMeasure must run first, even on ROMs without the optional actualWidth field. */
    public NativeStyleScope beginClockMeasure(TextView view) {
        if (!NativeClockMeasurement.statClock(view)) return null;
        return beginNativeStyle(view, true);
    }

    /** Called after native measure + scope.close(), before one real TextView super measure. */
    public int prepareClockMeasure(TextView view, int originalWidthSpec) {
        if (isInternal() || !clockControlsEnabled(view) || !NativeClockMeasurement.statClock(view)) return originalWidthSpec;
        boolean outer = paintOnly.get();
        paintOnly.set(Boolean.TRUE);
        try { beforeMeasure(view); } finally { paintOnly.set(outer); }
        Entry entry = entry(view);
        int available = NativeClockMeasurement.availableWidth(view);
        CharSequence text = view.getText();
        double textWidth = ClockDigits.width(text, view.getPaint());
        double horizontalPadding = view.getCompoundPaddingLeft() + view.getCompoundPaddingRight();
        float density = view.getResources().getDisplayMetrics().density;
        if (dualActive(entry)) {
            prepareLower(view, entry, density);
            if (entry.lowerLayout != null) textWidth = Math.max(textWidth, entry.lowerWidth);
        }
        ClockStyle clock = clockStyle(entry.group);
        boolean margins = clock.margins && dualActive(entry);
        double marginStart = margins ? NumericPolicy.pixels(clock.marginStart, density) : 0d;
        double marginEnd = margins ? NumericPolicy.pixels(clock.marginEnd, density) : 0d;
        entry.availableWidth = available;
        int desiredPixels = NumericPolicy.layoutPixels(Math.ceil(ClockDual.viewWidth(textWidth, horizontalPadding, marginStart, marginEnd)));
        if (entry.desiredWidth != desiredPixels) entry.measureChanged = true;
        entry.desiredWidth = desiredPixels;
        if (entry.outgoingWidth != desiredPixels) entry.measureChanged = true;
        entry.outgoingWidth = desiredPixels;
        // OEM onMeasure writes Paint directly; a subsequent same-size setter leaves
        // its old Layout/BoringLayout alive. Rebuild only on this measure path, never
        // in draw or the visible-time scheduler. Parent width is diagnostic only:
        // an enabled clock keeps its full requested size and complete time format.
        entry.layoutCacheRebuilt = NativeClockMeasurement.clearTextLayout(view);
        return View.MeasureSpec.makeMeasureSpec(desiredPixels, View.MeasureSpec.EXACTLY);
    }

    /** Capture OEM measurement before the module's super measure overwrites actualWidth. */
    public void onNativeClockMeasure(TextView view, int incomingWidthSpec, int nativeMeasuredWidth, int actualWidthOrNegative) {
        if (!NativeClockMeasurement.statClock(view)) return;
        Entry entry = entry(view); if (entry == null) return;
        int mode = View.MeasureSpec.getMode(incomingWidthSpec), width = View.MeasureSpec.getSize(incomingWidthSpec);
        if (entry.incomingWidthMode != mode || entry.incomingWidth != width
                || entry.nativeMeasuredWidth != nativeMeasuredWidth || entry.nativeActualWidth != actualWidthOrNegative) entry.measureChanged = true;
        entry.incomingWidthMode = mode; entry.incomingWidth = width;
        entry.nativeMeasuredWidth = nativeMeasuredWidth; entry.nativeActualWidth = actualWidthOrNegative;
    }

    public void afterClockMeasure(TextView view) {
        if (!NativeClockMeasurement.statClock(view)) return;
        Entry entry = entry(view); if (entry == null) return;
        int width = view.getMeasuredWidth();
        if ((entry.measureChanged || entry.measuredWidth != width) && ModuleDiagnostics.enabled()) ModuleDiagnostics.info("font",
                "StatClock nativePx=" + entry.nativeSize + "; appliedPx=" + view.getPaint().getTextSize()
                        + "; measuredWidth=" + width + "; availableWidth=" + entry.availableWidth
                        + "; incomingMode=" + entry.incomingWidthMode + "; incomingWidth=" + entry.incomingWidth
                        + "; nativeMeasuredWidth=" + entry.nativeMeasuredWidth + "; nativeActualWidth=" + entry.nativeActualWidth
                        + "; desiredWidth=" + entry.desiredWidth + "; outgoingWidth=" + entry.outgoingWidth
                        + "; layoutCacheRebuilt=" + entry.layoutCacheRebuilt + "; widthPolicy=natural"
                        + "; legacyFactor=" + scaleFactor(entry));
        entry.measuredWidth = width; entry.measureChanged = false;
    }

    /** Evidence for a single guarded preference migration; 0 means no proved legacy conversion. */
    public float clockLegacyScaleFactor(TextView view) {
        Entry entry = entry(view);
        if (entry == null || !entry.statClock || !"clock".equals(entry.group)) return 0f;
        float factor = scaleFactor(entry);
        return factor > 0f && factor < 1f ? factor : 0f;
    }

    public String summary() {
        StringBuilder result = new StringBuilder();
        synchronized (views) {
            for (Map.Entry<TextView,Entry> item : views.entrySet()) {
                Entry entry = item.getValue(); TextView view = item.getKey();
                if (entry.kind == CARRIER && view != null) {
                    if (result.length() > 0) result.append(" | ");
                    result.append("Carrier group=").append(entry.group).append(",nativePx=").append(entry.nativeSize)
                            .append(",capturedPx=").append(entry.initialNativeSize).append(",appliedPx=").append(view.getTextSize())
                            .append(",sizeOwned=").append(entry.carrierSizeApplied).append(",fontOwned=").append(entry.carrierFaceApplied)
                            .append(",sizeEnabled=").append(features.size(entry.group));
                    continue;
                }
                if (!entry.statClock || view == null) continue;
                if (result.length() > 0) result.append(" | ");
                result.append("StatClock nativePx=").append(entry.nativeSize).append(",resourcePx=").append(entry.resourceNativeSize)
                        .append(",capturedPx=").append(entry.initialNativeSize).append(",appliedPx=").append(view.getPaint().getTextSize())
                        .append(",measuredWidth=").append(view.getMeasuredWidth()).append(",availableWidth=").append(entry.availableWidth)
                        .append(",desiredWidth=").append(entry.desiredWidth)
                        .append(",layoutCacheRebuilt=").append(entry.layoutCacheRebuilt).append(",widthPolicy=natural")
                        .append(",incomingMode=").append(entry.incomingWidthMode).append(",incomingWidth=").append(entry.incomingWidth)
                        .append(",nativeMeasuredWidth=").append(entry.nativeMeasuredWidth).append(",nativeActualWidth=").append(entry.nativeActualWidth)
                        .append(",outgoingWidth=").append(entry.outgoingWidth)
                        .append(",legacyMeasureMissing=").append(entry.legacyNativeMeasureMissing).append(",legacyFactor=").append(scaleFactor(entry));
            }
        }
        return result.toString();
    }

    /** Vendor width probes write the native clock size into Paint; restore style and width together. */
    public float restoreClockWidth(TextView view, float nativeWidth, float measuredSize) {
        if (isInternal()) return nativeWidth;
        if (!clockControlsEnabled(view)) return nativeWidth;
        beforeMeasure(view);
        double ratio = measuredSize > 0f && !Float.isNaN(measuredSize) && !Float.isInfinite(measuredSize)
                ? (double) styledSize(view) / measuredSize : 1d;
        return Math.max(0f, NumericPolicy.drawPixels((double) nativeWidth * ratio));
    }

    public void setInteractive(boolean value) { interactive = value; if (value) refresh(); else cancel(); }

    public void configure(Bundle settings, Map<String,Integer> colors, Map<String,Boolean> alpha) {
        this.colors = colors; this.alpha = alpha;
        features = FeatureOptions.from(settings);
        clockStyles = clockStyles(settings, features);
        carrierStyles = carrierStyles(settings);
        layoutRevision++;
        refresh();
    }

    private static String validPattern(String pattern, String fallback) {
        return pattern != null && TimeFormat.validationError(pattern) == null ? pattern : fallback;
    }

    private static float value(Bundle settings, String key, float fallback, float min, float max) {
        return NumericPolicy.setting(key, settings.get(key), fallback);
    }

    public CharSequence nativeText(TextView view, CharSequence text) {
        if (isInternal()) return text;
        Entry entry = entry(view);
        if (entry == null) return text;
        entry.nativeText = text;
        entry.nativeTextVersion++;
        // The OEM updates its own text at least once a minute: a free chance to notice a stopped seconds tick.
        if (entry.kind == CLOCK) schedule();
        return replacement(entry, System.currentTimeMillis(), density(view));
    }

    /** A replacement setText may trigger native auto-sizing; that side effect is not a new OEM size baseline. */
    public CarrierTextScope beginCarrierText(TextView view) {
        if (isInternal() || kind(view) != CARRIER || !replaces(view)) return null;
        return new CarrierTextScope(view);
    }

    public final class CarrierTextScope implements AutoCloseable {
        private final TextView view;
        private final float pixels;
        private boolean closed;
        private CarrierTextScope(TextView view) { this.view = view; pixels = view.getTextSize(); }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (!Float.isFinite(pixels) || pixels < 0f || Math.abs(view.getTextSize() - pixels) <= .001f) return;
            enter();
            try { view.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixels); }
            finally { exit(); }
        }
    }

    public int nativeColor(TextView view, int color) {
        if (isInternal()) return color;
        Entry entry = entry(view);
        if (entry == null) return color;
        entry.nativeTint = color;
        boolean dual = dualActive(entry);
        String colorGroup = dual ? ClockLayoutSettings.DUAL_COLOR_GROUP : entry.kind == CLOCK ? entry.group : carrierStyle(entry.group).colorGroup;
        boolean recolor = dual ? clockStyle(entry.group).dualColor : features.color(group(entry));
        int result = recolor ? IconAppearance.color(colorGroup, color, colors, alpha) : color;
        if (entry.kind == CARRIER) {
            entry.carrierTintApplied = features.color(entry.group); entry.carrierAppliedTint = result;
        }
        return result;
    }

    public CharSequence nativeDescription(TextView view, CharSequence description) {
        if (isInternal()) return description;
        Entry entry = entry(view);
        if (entry == null) return description;
        entry.nativeDescription = description;
        return replaces(view) ? description(replacement(entry, System.currentTimeMillis(), density(view)), entry) : description;
    }

    public void nativeSize(TextView view, float pixels) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null && pixels >= 0 && !Float.isInfinite(pixels) && !Float.isNaN(pixels)) {
            entry.refreshClockBaseline(view, false);
            // OEM StatClock defines its baseline in the actual SystemUI dimen, not early XML/default Paint.
            entry.nativeSize = entry.statClock && Float.isFinite(entry.resourceNativeSize) ? entry.resourceNativeSize : pixels;
            if (entry.kind == CARRIER) {
                entry.carrierSizePending = true;
                entry.carrierSizeApplied = features.size(entry.group);
                entry.carrierAppliedSize = textSize(entry);
            }
        }
    }

    public void nativeTypeface(TextView view, Typeface typeface) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null) entry.nativeFace = typeface;
    }

    /** Called after a real TextView variation setter, with its nested typeface writes scoped out. */
    public void nativeFontVariation(TextView view) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null) { entry.nativeFace = view.getTypeface(); entry.nativeAxes = view.getFontVariationSettings(); }
    }

    public void nativeSpacing(TextView view, float spacing) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null && !Float.isNaN(spacing) && !Float.isInfinite(spacing)) entry.nativeSpacing = spacing;
    }

    /** OEM config/auto-fit code must read native pixels, never our already scaled Paint. */
    public NativeStyleScope beginNativeStyle(TextView view) {
        return beginNativeStyle(view, false);
    }

    /** {@code quiet} keeps the swap on Paint until the scope closes (the StatClock measure hook only). */
    private NativeStyleScope beginNativeStyle(TextView view, boolean quiet) {
        if (isInternal()) return null;
        Entry entry = entry(view);
        if (entry == null || !entry.managed && !managed(entry)) return null;
        if (entry.kind == CARRIER) syncCarrierStyle(view, entry);
        boolean outer = paintOnly.get();
        if (quiet) paintOnly.set(Boolean.TRUE);
        enter();
        try {
            if (entry.kind != CARRIER || entry.carrierSizeApplied) writeSize(view, entry.nativeSize);
            if (entry.kind != CARRIER || entry.carrierFaceApplied)
                restoreFace(view, entry.nativeFace, entry.nativeAxes);
            if (entry.kind != CARRIER || entry.carrierSpacingApplied) writeSpacing(view, entry.nativeSpacing);
            if (entry.kind != CARRIER || entry.carrierTintApplied) view.setTextColor(entry.nativeTint);
            if (entry.kind == CARRIER) clearCarrierOwnership(entry);
            return new NativeStyleScope(view, entry, quiet, outer);
        } catch (Throwable failure) {
            paintOnly.set(outer); exit(); throw failure;
        }
    }

    public final class NativeStyleScope implements AutoCloseable {
        private final TextView view;
        private final Entry entry;
        private final CharSequence previousText, previousDescription;
        private float carrierPixels = Float.NaN;
        private CharSequence carrierText;
        private boolean carrierTextProvided;
        private boolean closed;
        private final boolean quiet, outerPaintOnly;
        private NativeStyleScope(TextView view, Entry entry, boolean quiet, boolean outerPaintOnly) {
            this.view = view; this.entry = entry; this.quiet = quiet; this.outerPaintOnly = outerPaintOnly;
            previousText = view.getText(); previousDescription = view.getContentDescription();
        }
        /** A verified OEM callback can retain its applied target without writing Paint on a cache hit. */
        public void carrierPixels(float pixels) {
            if (!closed && entry.kind == CARRIER && Float.isFinite(pixels) && pixels > 0f) carrierPixels = pixels;
        }
        /** The real carrier callback may update its source text before a delayed native fade assigns TextView text. */
        public void carrierText(CharSequence text) {
            if (!closed && entry.kind == CARRIER) { carrierText = text; carrierTextProvided = true; }
        }
        @Override public void close() {
            if (closed) return;
            closed = true;
            try {
                entry.captureStyle(view);
                if (Float.isFinite(carrierPixels)) {
                    entry.nativeSize = carrierPixels;
                    // The OEM target was applied before our scope, or accepted by its native callback.
                    // Reconcile a stale Paint even when only replacement text owns this location.
                    entry.carrierSizeApplied = true;
                    entry.carrierAppliedSize = view.getTextSize();
                }
                if (carrierTextProvided) {
                    entry.nativeText = carrierText; entry.nativeTextVersion++;
                } else if (!sameCharacters(previousText, view.getText())) {
                    entry.nativeText = view.getText(); entry.nativeTextVersion++;
                }
                if (!sameCharacters(previousDescription, view.getContentDescription()))
                    entry.nativeDescription = view.getContentDescription();
            }
            finally { exit(); }
            try { apply(view, entry, System.currentTimeMillis(), false); }
            finally { if (quiet) paintOnly.set(outerPaintOnly); }
            if (ModuleDiagnostics.enabled()) ModuleDiagnostics.info("font",
                    "Native text style restored before OEM configuration; applied once after callback");
        }
    }

    private static float density(TextView view) {
        return view.getResources().getDisplayMetrics().density;
    }

    /** Spoken text for a two-row clock includes both rows; the displayed spans never reach accessibility. */
    private static CharSequence description(CharSequence text, Entry entry) {
        CharSequence plain = text == null ? "" : text.toString();
        return entry.lowerText == null ? plain : plain + " " + entry.lowerText;
    }

    private CharSequence replacement(Entry entry, long now, float density) {
        ClockStyle clock = clockStyle(entry.group);
        boolean dual = dualActive(entry);
        if (entry.kind == CLOCK && (clock.enabled || dual)) {
            float cell = NumericPolicy.pixels(clock.digitSpacing, density);
            CharSequence upper = TimeFormat.format(dual ? clock.upperPattern : clock.pattern, now);
            if (dual) {
                CharSequence lower = TimeFormat.format(clock.lowerPattern, now);
                entry.lowerText = ClockDigits.align(lower, cell, clock.digitFixed, clock.colonCentered);
            } else entry.lowerText = null;
            return ClockDigits.align(upper, cell, clock.digitFixed, clock.colonCentered);
        }
        if (entry.kind == CARRIER && carrierReplacement(entry.group)) {
            CarrierStyle style = carrierStyle(entry.group);
            if (style.mode.equals("time")) return TimeFormat.format(style.pattern, now);
            if (style.mode.equals("text")) return style.text;
        }
        return entry.nativeText;
    }

    /** Read-only outer Canvas origin in pixels, for a conservative budget using the whole native source width.
     * Two-row margins/alignment are already inside that width; upperDx moves only the upper row and must not be added again. */
    public float clockDrawOffsetX(View source) {
        if (!(source instanceof TextView)) return 0f;
        TextView view = (TextView) source;
        Entry entry = views.get(view);
        if (entry == null || entry.kind != CLOCK) return 0f;
        float density = view.getResources().getDisplayMetrics().density;
        if (dualActive(entry)) return NumericPolicy.pixels(clockStyle(entry.group).dualX, density);
        Classification known;
        synchronized (classificationLock) { known = classifications.get(view); }
        String positionGroup = known != null && known.shadeClock && !PanelMode.classic() ? "shade_clock" : entry.group;
        return features.position(positionGroup) ? NumericPolicy.pixels(clockStyle(positionGroup).x, density) : 0f;
    }

    public void beforeDraw(TextView view, Canvas canvas) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry == null) return;
        if (entry.kind == CLOCK) keepTicking(view, entry);
        applyStyle(view, entry);
        float density = view.getResources().getDisplayMetrics().density;
        CarrierStyle style = carrierStyle(entry.group);
        // Following the status clock's text/style does not give a shade header the status
        // bar's coordinates. Its native anchor is different (and moves with the panel).
        // Both the real header and its animation copies use only the independent shade
        // position switch, so toggling that master cannot introduce a status-only offset.
        String positionGroup = entry.kind == CLOCK && classification(view).shadeClock && !PanelMode.classic() ? "shade_clock" : entry.group;
        ClockStyle clock = clockStyle(positionGroup);
        ClockStyle status = clockStyle(entry.group);
        if (entry.kind == CARRIER) { float shift = carrierBaselineShift(view, entry); if (shift != 0f) canvas.translate(0f, shift); }
        if (dualActive(entry)) canvas.translate(NumericPolicy.pixels(status.dualX, density), NumericPolicy.pixels(status.dualY, density));
        else if (features.position(positionGroup)) canvas.translate(StatusBarFlowLayout.owns(view) ? 0f : NumericPolicy.pixels(entry.kind == CLOCK ? clock.x : style.x, density),
                NumericPolicy.pixels(entry.kind == CLOCK ? clock.y : style.y, density));
        placeClock(view, entry, canvas, density);
    }

    /** Outer margin and two-row placement for the OEM status clock; every other view returns untouched. */
    private void placeClock(TextView view, Entry entry, Canvas canvas, float density) {
        entry.dualPlaced = false;
        if (!dualActive(entry)) { entry.placeReason = "inactive"; return; }
        ClockStyle clock = clockStyle(entry.group);
        float start = clock.margins ? NumericPolicy.pixels(clock.marginStart, density) : 0f;
        Layout upper = view.getLayout();
        if (upper == null) { entry.placeReason = "layout_null"; return; }
        prepareLower(view, entry, density);
        if (entry.lowerLayout == null) { entry.placeReason = "lower_null"; return; }
        entry.placeReason = "ok";
        Paint.FontMetrics upperMetrics=entry.upperMetrics,lowerMetrics=entry.lowerMetrics;
        view.getPaint().getFontMetrics(upperMetrics);entry.lowerPaint.getFontMetrics(lowerMetrics);
        ClockDual.Placement place = ClockDual.place(entry.dualPlacement,view.getCompoundPaddingLeft(), view.getExtendedPaddingTop(),
                view.getExtendedPaddingBottom(), view.getHeight(), view.getBaseline(),
                view.getCompoundPaddingLeft() + upper.getLineLeft(0),
                upper.getLineWidth(0), -upperMetrics.ascent, upperMetrics.descent - upperMetrics.ascent,
                entry.lowerWidth, -lowerMetrics.ascent, lowerMetrics.descent - lowerMetrics.ascent,
                NumericPolicy.pixels(clock.lineGap, density), start, clock.alignFraction);
        entry.upperDx = place.upperDx; entry.upperDy = place.upperDy;
        entry.lowerX = place.lowerX; entry.lowerBaseline = place.lowerBaseline;
        canvas.translate(place.upperDx, place.upperDy);
        entry.dualPlaced = true;
    }

    /** Called after the native draw, on the same canvas state beforeDraw left: draws the lower row. */
    public void afterDraw(TextView view, Canvas canvas) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry == null || !entry.dualPlaced || entry.lowerLayout == null || entry.lowerPaint == null) return;
        entry.dualPlaced = false;
        // Back to the position-shifted origin; the native row alone carried the upper-row translation.
        canvas.translate(-entry.upperDx, -entry.upperDy);
        entry.lowerPaint.setColor(view.getCurrentTextColor());
        int saved = canvas.save();
        canvas.translate(entry.lowerX, entry.lowerBaseline - entry.lowerLayout.getLineBaseline(0));
        entry.lowerLayout.draw(canvas);
        canvas.restoreToCount(saved);
    }

    /** Rebuild the lower row only when its text or paint inputs changed. Returns true when anything changed. */
    private boolean prepareLower(TextView view, Entry entry, float density) {
        ClockStyle clock = clockStyle(entry.group);
        CharSequence text = entry.lowerText;
        if (text == null || !dualActive(entry)) {
            if (entry.lowerLayout == null) return false;
            entry.lowerLayout = null; entry.lowerPreparedText = null; entry.lowerStyle=null;entry.lowerWidth = 0f; return true;
        }
        float size = ClockLayoutSettings.rowPixels(entry.nativeSize, clock.lowerScale);
        float spacingPixels = NumericPolicy.pixels(clock.dualSpacing, density);
        float spacing = entry.nativeSpacing + spacingPixels / Math.max(1f, size);
        long fontRevision=FontRepository.styleRevision();
        if(entry.lowerLayout!=null&&entry.lowerStyle==clock&&entry.lowerFontRevision==fontRevision
                &&entry.lowerPreparedSize==size&&entry.lowerPreparedSpacing==spacing&&entry.lowerPreparedDensity==density
                &&entry.lowerNativeFace==entry.nativeFace&&sameAxes(entry.lowerNativeAxes,entry.nativeAxes)
                &&sameCharacters(entry.lowerPreparedText,text)&&!FontRepository.retryDue(fontScope(view, entry))){
            entry.lowerPreparedText=text;return false;
        }
        if (entry.lowerPaint == null) entry.lowerPaint = new TextPaint(view.getPaint());
        else entry.lowerPaint.set(view.getPaint());
        TextPaint paint = entry.lowerPaint;
        paint.setTextSize(size);
        fontRevision=FontRepository.applyRevision(paint, entry.nativeFace, clock.lowerWeight, entry.nativeAxes, fontScope(view, entry));
        paint.setLetterSpacing(spacing);
        paint.setColor(view.getCurrentTextColor());
        entry.lowerLayout = ClockDual.layout(text, paint);
        entry.lowerWidth = (float) ClockDigits.width(text, paint);
        entry.lowerPreparedText=text;entry.lowerStyle=clock;entry.lowerFontRevision=fontRevision;
        entry.lowerPreparedSize=size;entry.lowerPreparedSpacing=spacing;entry.lowerPreparedDensity=density;
        entry.lowerNativeFace=entry.nativeFace;entry.lowerNativeAxes=entry.nativeAxes;
        return true;
    }

    /** One structured record per change of the status clock's state; never per frame or per second. */
    private void emitClockFacts(TextView view, Entry entry) {
        if (entry.kind != CLOCK || !entry.statClock || !ModuleDiagnostics.enabled()) return;
        boolean requested = features.isEnabled(ClockLayoutSettings.DUAL_ENABLED), active = dualActive(entry);
        String reason = active ? entry.placeReason : !requested ? "switch_off" : features.safeMode() ? "safe_mode"
                : !ClockLayoutSettings.STATUS.equals(entry.group) ? "group_" + entry.group : "inactive";
        Layout layout = view.getLayout();
        float density = view.getResources().getDisplayMetrics().density;
        String key = entry.group + "|" + requested + "|" + active + "|" + reason + "|" + (layout != null) + "|" + (entry.lowerLayout != null)
                + "|" + view.getWidth() + "|" + view.getHeight() + "|" + Math.round(entry.lowerWidth) + "|" + view.getTextSize();
        if (key.equals(entry.factKey)) return;
        entry.factKey = key;
        ClockStyle clock = clockStyle(entry.group);
        ModuleDiagnostics.fact("clock", "clock", "状态栏时钟状态",
                "group", entry.group, "stat_clock", true, "dual_enabled", requested, "dual_active", active,
                "master_enabled", features.enabled(entry.group), "layout_ready", layout != null,
                "lower_ready", entry.lowerLayout != null, "view_width", view.getWidth(), "view_height", view.getHeight(),
                "upper_width", layout == null ? 0f : layout.getLineWidth(0), "lower_width", entry.lowerWidth,
                "density", density, "text_size", view.getTextSize(),
                "lower_size", entry.lowerPaint == null ? 0f : entry.lowerPaint.getTextSize(),
                "digit_fixed", clock.digitFixed, "seconds", clock.seconds,
                "margin_start_px", clock.margins ? NumericPolicy.pixels(clock.marginStart, density) : 0f,
                "margin_end_px", clock.margins ? NumericPolicy.pixels(clock.marginEnd, density) : 0f,
                "reason", reason);
    }

    private void applyStyle(TextView view, Entry entry) {
        if (isInternal()) return;
        if (entry.kind == CARRIER) { applyCarrierStyle(view, entry); return; }
        if (!managed(entry) && !entry.managed) return;
        if (managed(entry)) entry.managed = true;
        enter();
        try {
            String group = group(entry);
            CarrierStyle carrier = carrierStyle(group);
            ClockStyle clock = clockStyle(group);
            float size = fitted(view, entry, textSize(entry));
            entry.autoSize.configure(view, entry.statClock ? managed(entry) : features.size(group));
            if (entry.statClock) entry.ellipsize.configure(view, managed(entry));
            if (Math.abs(view.getTextSize() - size) > .001f || Math.abs(view.getPaint().getTextSize() - size) > .001f)
                writeSize(view, size);
            int nativeWeight = entry.nativeFace == null ? 400 : Build.VERSION.SDK_INT >= 28
                    ? entry.nativeFace.getWeight() : entry.nativeFace.isBold() ? 700 : 400;
            boolean textStyle = features.textStyle(group);
            boolean dual = dualActive(entry);
            long fontRevision=FontRepository.styleRevision();
            if(entry.appliedFontLayoutRevision!=layoutRevision||entry.appliedFontSourceRevision!=fontRevision
                    ||!group.equals(entry.fontGroup)||entry.fontDual!=dual
                    ||entry.fontNativeFace!=entry.nativeFace||!sameAxes(entry.fontNativeAxes,entry.nativeAxes)
                    ||entry.fontAppliedFace!=view.getTypeface()||!sameAxes(entry.fontAppliedAxes,view.getFontVariationSettings())||FontRepository.retryDue(fontScope(view, entry))){
                boolean sourceChanged=features.enabled("font")&&!FontRepository.systemMode(fontScope(view, entry));
                if (managed(entry) && (sourceChanged || textStyle || dual))
                    fontRevision=writeFace(view, entry.nativeFace, dual ? clock.upperWeight : textStyle
                            ? (entry.kind == CLOCK ? clock.weight : carrier.weight) : nativeWeight, entry.nativeAxes, fontScope(view, entry));
                else restoreFace(view, entry.nativeFace, entry.nativeAxes);
                entry.appliedFontLayoutRevision=layoutRevision;entry.appliedFontSourceRevision=fontRevision;
                entry.fontGroup=group;entry.fontDual=dual;
                entry.fontNativeFace=entry.nativeFace;entry.fontNativeAxes=entry.nativeAxes;
                entry.fontAppliedFace=view.getTypeface();entry.fontAppliedAxes=view.getFontVariationSettings();
            }
            float spacing = entry.nativeSpacing + NumericPolicy.pixels(dual ? clock.dualSpacing : features.textStyle(group)
                    ? (entry.kind == CLOCK ? clock.spacing : carrier.spacing) : 0,
                    view.getResources().getDisplayMetrics().density) / Math.max(1f, size);
            if (Math.abs(view.getLetterSpacing() - spacing) > .0001f) writeSpacing(view, spacing);
            boolean recolor = dual ? clock.dualColor : features.color(group);
            String colorGroup = dual ? ClockLayoutSettings.DUAL_COLOR_GROUP : entry.kind == CLOCK ? group : carrier.colorGroup;
            int tint = recolor ? IconAppearance.color(colorGroup, entry.nativeTint, colors, alpha) : entry.nativeTint;
            if (view.getCurrentTextColor() != tint) view.setTextColor(tint);
            if (ModuleDiagnostics.enabled()) {
                float density = view.getResources().getDisplayMetrics().density;
                if (entry.diagnosticNativeSize != entry.nativeSize || entry.diagnosticAppliedSize != size
                        || entry.diagnosticDensity != density) {
                    entry.diagnosticNativeSize = entry.nativeSize; entry.diagnosticAppliedSize = size;
                    entry.diagnosticDensity = density;
                    ModuleDiagnostics.info("font", "Text metrics " + group + "; nativePx=" + entry.nativeSize
                            + "; appliedPx=" + size + "; measuredWidth=" + view.getMeasuredWidth() + "; density " + density
                            + "; scaled density " + view.getResources().getDisplayMetrics().scaledDensity);
                }
            }
        } finally { exit(); }
    }

    private static boolean sameAxes(String first, String second) {
        return first == second || first != null && first.equals(second);
    }

    /** Observe unowned properties and newer OEM writes before considering restoration. */
    private static void syncCarrierStyle(TextView view, Entry entry) {
        float size = view.getTextSize();
        // The OEM replacement-size hook supplies a real native px baseline before applyStyle,
        // whereas the ordinary setter supplies it before writing the transformed pixels.
        if (entry.carrierSizePending) entry.carrierSizePending = false;
        else if (!entry.carrierSizeApplied || Math.abs(size - entry.carrierAppliedSize) > .001f) {
            if (size >= 0f && Float.isFinite(size)) entry.nativeSize = size;
            entry.carrierSizeApplied = false;
        }
        if (!entry.carrierFaceApplied || view.getTypeface() != entry.carrierAppliedFace
                || !sameAxes(view.getFontVariationSettings(), entry.carrierAppliedAxes)) {
            entry.nativeFace = view.getTypeface(); entry.nativeAxes = view.getFontVariationSettings();
            entry.carrierFaceApplied = false;
        }
        float spacing = view.getLetterSpacing();
        if (!entry.carrierSpacingApplied || Math.abs(spacing - entry.carrierAppliedSpacing) > .0001f) {
            if (Float.isFinite(spacing)) entry.nativeSpacing = spacing;
            entry.carrierSpacingApplied = false;
        }
        if (!entry.carrierTintApplied || view.getCurrentTextColor() != entry.carrierAppliedTint) {
            entry.nativeTint = view.getCurrentTextColor(); entry.carrierTintApplied = false;
        }
    }

    private static void clearCarrierOwnership(Entry entry) {
        entry.carrierSizeApplied = entry.carrierFaceApplied = entry.carrierSpacingApplied = entry.carrierTintApplied = false;
        entry.carrierSizePending = false;
    }

    /** Carrier controls own individual properties; text/position changes never claim native size. */
    /**
     * A fixed row shorter than the font's line box (a variable font's top/bottom span nearly three em) top-aligns the text, so the
     * baseline sits 1.8 em below the row top: any enlargement pushes it below the row and the native clip cuts the glyphs off.
     * While size control is on, take the font padding away and give the baseline back its place at 100% (carrierBaselineShift).
     */
    private void syncCarrierPadding(TextView view, Entry entry, boolean wanted) {
        if (wanted && !entry.carrierPaddingOwned) {
            int height = view.getHeight() > 0 ? view.getHeight() : view.getMeasuredHeight();
            if (height <= 0) return;
            view.getPaint().getFontMetrics(entry.carrierMetrics);
            float line = entry.carrierMetrics.bottom - entry.carrierMetrics.top;
            if (!(line > height - view.getCompoundPaddingTop() - view.getCompoundPaddingBottom())) return;
            entry.carrierNativePadding = view.getIncludeFontPadding();
            if (entry.carrierNativePadding) view.setIncludeFontPadding(false);
            entry.carrierPaddingOwned = true;
        } else if (!wanted && entry.carrierPaddingOwned) {
            view.setIncludeFontPadding(entry.carrierNativePadding);
            entry.carrierPaddingOwned = false;
        }
    }

    /** Baseline at the native size with the native padding, minus the baseline now: the draw moves by the difference. */
    private float carrierBaselineShift(TextView view, Entry entry) {
        float size = view.getPaint().getTextSize();
        if (!entry.carrierPaddingOwned || !entry.carrierNativePadding || !(size > 0f) || !(entry.nativeSize > 0f)) return 0f;
        view.getPaint().getFontMetrics(entry.carrierMetrics);
        float shift = -entry.carrierMetrics.top / size * entry.nativeSize + entry.carrierMetrics.ascent;
        return Float.isFinite(shift) ? shift : 0f;
    }

    private void applyCarrierStyle(TextView view, Entry entry) {
        syncCarrierStyle(view, entry);
        String group = entry.group;
        boolean enabled = features.enabled(group);
        if (!enabled && !entry.managed) return;
        if (enabled) entry.managed = true;
        CarrierStyle style = carrierStyle(group);
        boolean sizeEnabled = features.size(group), textStyle = features.textStyle(group);
        boolean faceEnabled = enabled && (textStyle || features.enabled("font") && !FontRepository.systemMode(fontScope(view, entry)));
        boolean colorEnabled = features.color(group);
        enter();
        try {
            entry.autoSize.configure(view, sizeEnabled);
            if (faceEnabled) {
                int nativeWeight = entry.nativeFace == null ? 400 : Build.VERSION.SDK_INT >= 28
                        ? entry.nativeFace.getWeight() : entry.nativeFace.isBold() ? 700 : 400;
                FontRepository.apply(view, entry.nativeFace, textStyle ? style.weight : nativeWeight, entry.nativeAxes, fontScope(view, entry));
                entry.carrierAppliedFace = view.getTypeface(); entry.carrierAppliedAxes = view.getFontVariationSettings();
                entry.carrierFaceApplied = true;
            } else if (entry.carrierFaceApplied) {
                FontWeight.restore(view, entry.nativeFace, entry.nativeAxes); entry.carrierFaceApplied = false;
            }
            if (sizeEnabled || faceEnabled || entry.carrierSizeApplied) {
                // Fit after the final Typeface is applied. Keep nativeSize unchanged so a fitted size never becomes
                // the baseline of the next tick, OEM configuration callback or feature-off restoration.
                float size = fittedCarrier(view, entry, textSize(entry));
                if (Math.abs(view.getTextSize() - size) > .001f)
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                entry.carrierAppliedSize = view.getTextSize();
                entry.carrierSizeApplied = sizeEnabled || Math.abs(size - entry.nativeSize) > .001f;
            }
            syncCarrierPadding(view, entry, enabled && sizeEnabled);
            if (textStyle || entry.carrierSpacingApplied) {
                float spacing = entry.nativeSpacing + NumericPolicy.pixels(textStyle ? style.spacing : 0f,
                        view.getResources().getDisplayMetrics().density) / Math.max(1f, view.getTextSize());
                if (Math.abs(view.getLetterSpacing() - spacing) > .0001f) view.setLetterSpacing(spacing);
                entry.carrierAppliedSpacing = view.getLetterSpacing(); entry.carrierSpacingApplied = textStyle;
            }
            if (colorEnabled || entry.carrierTintApplied) {
                int tint = colorEnabled ? IconAppearance.color(style.colorGroup, entry.nativeTint, colors, alpha) : entry.nativeTint;
                if (view.getCurrentTextColor() != tint) view.setTextColor(tint);
                entry.carrierAppliedTint = view.getCurrentTextColor(); entry.carrierTintApplied = colorEnabled;
            }
            if (ModuleDiagnostics.enabled() && (entry.diagnosticNativeSize != entry.nativeSize
                    || entry.diagnosticAppliedSize != view.getTextSize())) {
                entry.diagnosticNativeSize = entry.nativeSize; entry.diagnosticAppliedSize = view.getTextSize();
                ModuleDiagnostics.info("font", "Carrier metrics " + group + "; nativePx=" + entry.nativeSize
                        + "; appliedPx=" + view.getTextSize() + "; sizeOwned=" + entry.carrierSizeApplied
                        + "; fontOwned=" + entry.carrierFaceApplied);
            }
        } finally { exit(); }
    }

    private void apply(TextView view, Entry entry, long now) {
        apply(view, entry, now, true);
    }

    private void apply(TextView view, Entry entry, long now, boolean redrawUnchanged) {
        entry.group = group(view);
        // A disabled location starts as a native pass-through, including spans, font and alpha.
        // Once enabled, one restoration pass is needed when it is disabled or moved elsewhere.
        if (!managed(entry) && !entry.managed) return;
        applyStyle(view, entry);
        CharSequence text = replacement(entry, now, density(view));
        if (text == null) text = "";
        boolean replaced = replaces(view);
        float lowerBefore = entry.lowerWidth;
        boolean lowerChanged = prepareLower(view, entry, density(view));
        boolean lowerWidthChanged = Math.abs(entry.lowerWidth - lowerBefore) > .5f;
        // TextView can wrap a String or Spannable in a different CharSequence implementation.
        // Compare characters during stable refreshes; preserve native spans once on restoration.
        boolean force = !entry.textApplied || entry.appliedReplacement != replaced
                || entry.appliedLayoutRevision != layoutRevision
                || (!replaced && entry.appliedNativeTextVersion != entry.nativeTextVersion);
        enter();
        try {
            if (force || !sameCharacters(view.getText(), text)) {
                if (entry.kind == CARRIER) {
                    try (CarrierTextScope scope = new CarrierTextScope(view)) { view.setText(text); }
                    // Replacement width differs from the previous native text; evaluate it before the first measure/draw.
                    if (replaced) applyCarrierStyle(view, entry);
                } else view.setText(text);
            }
            entry.textApplied = true;
            entry.appliedLayoutRevision = layoutRevision;
            entry.appliedReplacement = replaced;
            if (!replaced) entry.appliedNativeTextVersion = entry.nativeTextVersion;
            CharSequence description = replaced ? description(text, entry) : entry.nativeDescription;
            if (redrawUnchanged || force || !sameCharacters(view.getContentDescription(), description))
                view.setContentDescription(description);
            // Text/style setters invalidate actual changes. Explicit refreshes also redraw
            // unchanged text because position settings are applied only inside beforeDraw.
            if (redrawUnchanged || lowerChanged) view.invalidate();
            // The lower row can be the wider one; the measure path reads its width again.
            if (lowerWidthChanged && dualActive(entry)) view.requestLayout();
            if (!managed(entry)) entry.managed = false;
            emitClockFacts(view, entry);
        } finally { exit(); }
    }

    private static boolean sameCharacters(CharSequence first, CharSequence second) {
        if (first == second) return true;
        if (first == null || second == null || first.length() != second.length()) return false;
        for (int i = 0; i < first.length(); i++) if (first.charAt(i) != second.charAt(i)) return false;
        return true;
    }

    public void refresh() {
        refresh(true);
    }

    private void refresh(boolean redrawUnchanged) {
        if (isInternal()) return;
        cancel();
        long now = System.currentTimeMillis();
        try {
            synchronized (views) {
                for (Map.Entry<TextView,Entry> item : views.entrySet()) {
                    TextView view = item.getKey();
                    if (view == null || !view.isAttachedToWindow()) continue;
                    // The per-second tick only touches what can be seen; a hidden clock is brought up to date
                    // when it becomes visible. Hidden text changes would otherwise request a layout every second.
                    if (!redrawUnchanged && !view.isShown()) continue;
                    // One failing location must not stop the time of every other one.
                    try { apply(view, item.getValue(), now, redrawUnchanged); }
                    catch (RuntimeException failure) { applyFailed(failure); }
                }
            }
            if (additionalClockVisible != null && additionalClockVisible.getAsBoolean() && additionalClockUpdate != null) {
                try { additionalClockUpdate.run(); } catch (RuntimeException failure) { applyFailed(failure); }
            }
        } catch (RuntimeException failure) {
            applyFailed(failure);
        } finally {
            // Always re-armed: a lost tick shows up as digits that stay put until something is touched.
            schedule();
        }
    }

    private void applyFailed(RuntimeException failure) {
        if (failureNotes++ < 3 && ModuleDiagnostics.enabled()) ModuleDiagnostics.error("clock", "Clock refresh skipped one view", failure);
    }

    private void healed(String reason, long gap) {
        if (healNotes >= 12 || !ModuleDiagnostics.enabled()) return;
        healNotes++;
        ModuleDiagnostics.fact("clock", "clock", "时钟刷新自恢复", "reason", reason, "gap_ms", gap,
                "interactive", interactive, "pending", pending);
    }

    /** The screen-on broadcast is the usual signal; PowerManager is the truth when one was missed. Throttled: it is a binder call. */
    private boolean powerInteractive(long uptime) {
        PowerManager manager = power;
        if (manager == null || lastPowerCheck != 0L && uptime - lastPowerCheck < POWER_CHECK_MS) return false;
        lastPowerCheck = uptime;
        try { return manager.isInteractive(); } catch (RuntimeException unavailable) { return false; }
    }

    /** A clock that is drawn while its tick is gone gets it back at once instead of waiting for a touch. */
    private void keepTicking(TextView view, Entry entry) {
        long uptime = SystemClock.uptimeMillis();
        if (pending && uptime - scheduledAt <= STALL_GRACE_MS) return;
        if (healPosted || lastHealPost != 0L && uptime - lastHealPost < HEAL_POST_MS) return;
        if (!(clockStyle(entry.group).enabled || dualActive(entry)) || !view.isShown()) return;
        if (!interactive && !powerInteractive(uptime)) return;
        healPosted = true; lastHealPost = uptime;
        handler.post(() -> { healPosted = false; healed("draw_without_tick", 0L); refresh(false); });
    }

    /** Shares the existing visible-time scheduler with overlays; no extra timer is created. */
    public void setAdditionalClock(BooleanSupplier visible, BooleanSupplier seconds, Runnable update) {
        additionalClockVisible = visible; additionalClockSeconds = seconds; additionalClockUpdate = update;
        restart();
    }

    // Unconditional: a tick can be queued while the flag says otherwise, and two chains must never coexist.
    private void cancel() { handler.removeCallbacks(tick); pending = false; }
    private void restart() { cancel(); schedule(); }
    public void visibilityChanged() { restart(); }

    private volatile boolean restartQueued;
    private final Runnable restartTask = () -> { restartQueued = false; restart(); };

    /** Hook entry for one text view whose visibility changed. Only that view is brought up to date and the timer
     * restarts once for the whole burst; a panel animation used to refresh every clock once per changed view. */
    public void viewVisibilityChanged(TextView view) {
        if (isInternal()) return;
        Entry entry = entry(view);
        if (entry != null && view.isAttachedToWindow()) {
            try { apply(view, entry, System.currentTimeMillis(), false); } catch (RuntimeException failure) { applyFailed(failure); }
        }
        if (!restartQueued) { restartQueued = true; handler.post(restartTask); }
    }

    private void schedule() {
        long uptime = SystemClock.uptimeMillis();
        if (pending) {
            if (uptime - scheduledAt <= STALL_GRACE_MS) return;
            // The queued tick never ran. Clearing it is what a touch used to do by accident.
            handler.removeCallbacks(tick); pending = false;
            healed("pending_lost", uptime - scheduledAt);
        }
        if (!interactive) {
            if (!powerInteractive(uptime)) return;
            interactive = true; healed("screen_on_missed", 0L);
        }
        boolean active = additionalClockVisible != null && additionalClockVisible.getAsBoolean();
        boolean seconds = active && additionalClockSeconds != null && additionalClockSeconds.getAsBoolean();
        synchronized (views) {
            for (Map.Entry<TextView,Entry> item : views.entrySet()) {
                TextView view = item.getKey(); Entry entry = item.getValue();
                if (!view.isAttachedToWindow() || !view.isShown()) continue;
                entry.group = group(view);
                ClockStyle clock = clockStyle(entry.group);
                if (entry.kind == CLOCK && (clock.enabled || dualActive(entry))) { active = true; seconds |= clock.seconds; }
                CarrierStyle style = carrierStyle(entry.group);
                if (entry.kind == CARRIER && carrierReplacement(entry.group) && style.mode.equals("time")) { active = true; seconds |= style.seconds; }
            }
        }
        if (active) {
            long delay = TimeFormat.nextDelay(System.currentTimeMillis(), seconds);
            scheduledAt = uptime + delay; pending = true;
            handler.postDelayed(tick, delay);
        }
    }
}
