// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** The camera-area fluid cloud ("capsule"): the black pill drawn around the front camera while a live alert is
 * collapsed or expanding. The plugin's pill is one GradientDrawable background (fill) and one GradientDrawable foreground
 * (an outline whose stroke the plugin rewrites on every expansion step).
 *
 * Since code99 this class never edits the plugin's own fill: when any look is wanted (a colour, a gradient, the frosted glass or
 * other corners) the view's background is swapped for a {@link PillBackground} that draws the pill itself, and the plugin's
 * drawable is kept and put back when the look goes away. That makes the gradient, the corners and the colour exact whatever the
 * platform's own drawable does (code98 relied on GradientDrawable: its colours and corner radius could stay behind). The outline
 * is replaced by a ring drawable in the view's overlay when a border, the running light or other corners are wanted, and the
 * plugin's own thin stroke is taken off; leaving it, the plugin's expansion step rewrites its stroke. The music cover can have its
 * own visual scale, following the plugin's animation baseline. The capsule's layout, size, content and camera hole stay native.
 *
 * Contract (checked by structure, never by obfuscated names): the plugin's CapsuleView is a FrameLayout with exactly
 * one GradientDrawable field and exactly one (float)void method, the expansion step that sets the outline. */
public final class FluidCloudCapsule {
    public static final String MASTER = "fluid_cloud_capsule_enabled";
    public static final String FILL_ENABLED = "fluid_cloud_capsule_fill_enabled";
    public static final String FILL_COLOR = "fluid_cloud_capsule_fill_color";
    public static final String FILL_OPACITY = "fluid_cloud_capsule_fill_opacity";
    public static final String FOLLOW_MEDIA = "fluid_cloud_capsule_follow_media_enabled";
    public static final String BORDER_ENABLED = "fluid_cloud_capsule_border_enabled";
    public static final String BORDER_COLOR = "fluid_cloud_capsule_border_color";
    public static final String BORDER_WIDTH = "fluid_cloud_capsule_border_width";
    public static final String RADIUS_ENABLED = "fluid_cloud_capsule_radius_enabled";
    public static final String RADIUS = "fluid_cloud_capsule_radius";
    /** The album cover at the left of the music capsule gets its own corner radius (the plugin's is a circle). */
    public static final String COVER_RADIUS_ENABLED = "fluid_cloud_capsule_cover_radius_enabled";
    public static final String COVER_RADIUS = "fluid_cloud_capsule_cover_radius";
    /** The cover is about 20 dp: 10 dp is the round cover again, 0 a square one. */
    public static final float DEFAULT_COVER_RADIUS = 6f, MAX_COVER_RADIUS = 10f;
    public static final String COVER_SIZE_ENABLED = "fluid_cloud_capsule_cover_size_enabled";
    public static final String COVER_SCALE = "fluid_cloud_capsule_cover_scale";
    public static final float DEFAULT_COVER_SCALE = 100f, MIN_COVER_SCALE = 50f, MAX_COVER_SCALE = 150f;
    /** Fill gradient: the fill colour at the start of the direction, the gradient colour at its end. */
    public static final String GRADIENT_ENABLED = "fluid_cloud_capsule_gradient_enabled";
    public static final String GRADIENT_MODE = "fluid_cloud_capsule_gradient_mode";
    /** How much of the pill the change from one colour to the other takes (percent): 100 runs end to end, less keeps both ends solid. */
    public static final String GRADIENT_RANGE = "fluid_cloud_capsule_gradient_range";
    public static final float DEFAULT_GRADIENT_RANGE = 100f;
    public static final String GRADIENT_COLOR = "fluid_cloud_capsule_gradient_color";
    /** Frosted glass behind the capsule (the platform's own background blur of the window), with its own mask colour.
     * It takes the fill's place: the fill options do not apply while it is on. */
    public static final String BLUR_ENABLED = "fluid_cloud_capsule_blur_enabled";
    public static final String BLUR_RADIUS = "fluid_cloud_capsule_blur_radius";
    public static final String BLUR_MASK_COLOR = "fluid_cloud_capsule_blur_mask_color";
    /** Running light around the outline. */
    public static final String MARQUEE_ENABLED = "fluid_cloud_capsule_marquee_enabled";
    public static final String MARQUEE_PRESET = "fluid_cloud_capsule_marquee_preset";
    public static final String MARQUEE_COLOR_1 = "fluid_cloud_capsule_marquee_color_1";
    public static final String MARQUEE_COLOR_2 = "fluid_cloud_capsule_marquee_color_2";
    public static final String MARQUEE_COLOR_3 = "fluid_cloud_capsule_marquee_color_3";
    public static final String MARQUEE_COLOR_4 = "fluid_cloud_capsule_marquee_color_4";
    public static final String MARQUEE_COLOR_5 = "fluid_cloud_capsule_marquee_color_5";
    public static final String MARQUEE_COLOR_6 = "fluid_cloud_capsule_marquee_color_6";
    public static final String[] MARQUEE_COLOR_KEYS = {MARQUEE_COLOR_1, MARQUEE_COLOR_2, MARQUEE_COLOR_3,
            MARQUEE_COLOR_4, MARQUEE_COLOR_5, MARQUEE_COLOR_6};
    public static final String MARQUEE_COLOR_COUNT = "fluid_cloud_capsule_marquee_color_count";
    public static final String MARQUEE_OPACITY = "fluid_cloud_capsule_marquee_opacity";
    public static final String MARQUEE_PERIOD = "fluid_cloud_capsule_marquee_period";
    public static final String MARQUEE_REVERSE = "fluid_cloud_capsule_marquee_reverse_enabled";
    /** The running light also fades inwards from the outline, like a glow. */
    public static final String GLOW_ENABLED = "fluid_cloud_capsule_marquee_glow_enabled";
    public static final String GLOW_DEPTH = "fluid_cloud_capsule_marquee_glow_depth";
    /** The music spectrum in the capsule follows the real audio instead of the canned animation. */
    public static final String SPECTRUM_ENABLED = "fluid_cloud_capsule_spectrum_enabled";
    public static final String SPECTRUM_SENSITIVITY = "fluid_cloud_capsule_spectrum_sensitivity";
    public static final String SPECTRUM_COLOR = "fluid_cloud_capsule_spectrum_color";
    /** The bars take the media colour (the accent) instead of the colour chosen above. */
    public static final String SPECTRUM_FOLLOW = "fluid_cloud_capsule_spectrum_follow_enabled";
    public static final float DEFAULT_SPECTRUM_SENSITIVITY = 60f;
    /** The plugin's own bars: 80 % white. */
    public static final int DEFAULT_SPECTRUM_COLOR = 0xccffffff;
    public static final String TOP_BOTTOM = "top_bottom", BOTTOM_TOP = "bottom_top", CENTER = "center";
    public static final String VIEW = "com.oplus.systemui.plugins.seedling.capsule.ui.view.CapsuleView";
    /** The plugin's own values: black pill, a 1.3 dp outline of 25 % grey, 20 dp corners. */
    public static final int DEFAULT_FILL_COLOR = 0xff000000, DEFAULT_BORDER_COLOR = 0x40959595;
    /** The pill is about 30 dp tall, so any radius from 15 dp up is already the full pill; only smaller radii change its shape. */
    public static final float DEFAULT_FILL_OPACITY = 100f, DEFAULT_BORDER_WIDTH = 1.3f, DEFAULT_RADIUS = 15f;
    public static final float MAX_BORDER_WIDTH = 6f, MAX_RADIUS = 15f;
    /** The collapsed outline of the plugin: shown while the expansion progress is at most this. */
    static final float NATIVE_OUTLINE_PROGRESS = .13f;
    /** A pill at most this much wider than tall is the capsule shrunk onto the camera; from TALL_RATIO on it is a full capsule. */
    static final float ROUND_RATIO = 1.25f, FULL_RATIO = 2f;

    /** How far the pill is from its form on the punch-hole camera (0) to a full capsule (1), eased, from its real size. Shrunk onto the
     * camera, the pill and the album cover sit around the hole, so the corner settings never touch that form; they come in as it widens
     * into the capsule (the music capsule with its cover and bars is a full capsule) and leave as it shrinks back, without a jump.
     * code100 took this from the card-expansion progress instead, which is 0 for the ordinary music capsule: its cover stayed round. */
    static float shapeWeight(float width, float height) {
        if (!(width > 0f) || !(height > 0f)) return 0f;
        float t = Math.max(0f, Math.min(1f, (width / height - ROUND_RATIO) / (FULL_RATIO - ROUND_RATIO)));
        return t * t * (3f - 2f * t);
    }

    /** The pill's current size: the background's bounds (the plugin animates them), else the view's. */
    static float shapeWeight(View view) {
        Drawable background = view.getBackground();
        android.graphics.Rect pill = background == null ? null : background.getBounds();
        if (pill != null && pill.width() > 0 && pill.height() > 0) return shapeWeight(pill.width(), pill.height());
        return shapeWeight(view.getWidth(), view.getHeight());
    }
    public static final int DEFAULT_GRADIENT_COLOR = 0xff3a2f6b;
    public static final float DEFAULT_BLUR_RADIUS = 20f, MAX_BLUR_RADIUS = 40f;
    public static final int DEFAULT_BLUR_MASK_COLOR = 0x66000000;
    public static final float DEFAULT_MARQUEE_OPACITY = 100f, DEFAULT_MARQUEE_PERIOD = 3f, MIN_MARQUEE_PERIOD = 0.5f, MAX_MARQUEE_PERIOD = 12f;
    public static final int DEFAULT_MARQUEE_COLOR_1 = 0xff00e5ff, DEFAULT_MARQUEE_COLOR_2 = 0xffb388ff, DEFAULT_MARQUEE_COLOR_3 = 0xffff80ab,
            DEFAULT_MARQUEE_COLOR_4 = 0xffffb74d, DEFAULT_MARQUEE_COLOR_5 = 0xff69f0ae, DEFAULT_MARQUEE_COLOR_6 = 0xff40c4ff;
    public static final int[] DEFAULT_MARQUEE_COLORS = {DEFAULT_MARQUEE_COLOR_1, DEFAULT_MARQUEE_COLOR_2, DEFAULT_MARQUEE_COLOR_3,
            DEFAULT_MARQUEE_COLOR_4, DEFAULT_MARQUEE_COLOR_5, DEFAULT_MARQUEE_COLOR_6};
    public static final int MIN_MARQUEE_COLORS = 2, MAX_MARQUEE_COLORS = 6, DEFAULT_MARQUEE_COLOR_COUNT = 3;
    public static final float DEFAULT_GLOW_DEPTH = 8f, MIN_GLOW_DEPTH = 2f, MAX_GLOW_DEPTH = 20f;
    public static final String DEFAULT_MARQUEE_PRESET = "rainbow";
    /** Marquee presets, in the order they are offered. "comet" and "custom" take the user's colours. */
    public static final String[] MARQUEE_PRESETS = {"rainbow", "neon", "aurora", "flame", "glacier", "sunset", "candy", "ocean", "forest",
            "lava", "galaxy", "sakura", "mint", "cyber", "gold", "silver", "lemon", "coral", "rgb", "duo", "comet", "custom"};
    public static final String[] MARQUEE_PRESET_LABELS = {"彩虹", "霓虹", "极光", "火焰", "冰川", "日落", "糖果", "海洋", "森林",
            "熔岩", "星空", "樱花", "薄荷", "赛博朋克", "鎏金", "银河", "柠檬", "珊瑚", "三原色", "红蓝", "单色流光", "自定义"};
    /** One lap of each ready-made preset (the first colour closes the lap). The two last ones are the user's. */
    private static final int[][] PRESET_COLORS = {
            {0xffff3b30, 0xffff9500, 0xffffcc00, 0xff34c759, 0xff32ade6, 0xff5856d6, 0xffff3b30},
            {0xff00f5ff, 0xff7b2fff, 0xffff2fd6, 0xff00f5ff},
            {0xff00e5a0, 0xff00b3ff, 0xff7a5cff, 0xff00e5a0},
            {0xffff3d00, 0xffff9100, 0xffffea00, 0xffff3d00},
            {0xffa8e6ff, 0xff4fc3f7, 0xffe1f5ff, 0xffa8e6ff},
            {0xffff5e62, 0xffff9966, 0xffffd36e, 0xff8e44ad, 0xffff5e62},
            {0xffff8ad8, 0xffffd1f3, 0xff8ad8ff, 0xffc7a6ff, 0xffff8ad8},
            {0xff0077be, 0xff00b4d8, 0xff90e0ef, 0xff0077be},
            {0xff1b5e20, 0xff43a047, 0xffa5d6a7, 0xff1b5e20},
            {0xffff1744, 0xffff6d00, 0xffffab00, 0xffb71c1c, 0xffff1744},
            {0xff3f1d89, 0xff7c4dff, 0xff00e5ff, 0xffff4081, 0xff3f1d89},
            {0xffffc1d6, 0xffff8fb1, 0xffffe0ec, 0xffffc1d6},
            {0xff00bfa5, 0xff64ffda, 0xffb9f6ca, 0xff00bfa5},
            {0xfffcee0a, 0xff00f0ff, 0xffff003c, 0xfffcee0a},
            {0xffffd700, 0xfffff2a8, 0xffb8860b, 0xffffd700},
            {0xffffffff, 0xff9e9e9e, 0xffe0e0e0, 0xffffffff},
            {0xfffff176, 0xffc6ff00, 0xffffee58, 0xfffff176},
            {0xffff6f61, 0xffffb4a2, 0xffff8a65, 0xffff6f61},
            {0xffff0000, 0xff00ff00, 0xff0000ff, 0xffff0000},
            {0xffff1744, 0xff2979ff, 0xffff1744}};

    private final FluidCloudAccent accent;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<View, Binding> bindings = Collections.synchronizedMap(new WeakHashMap<>());
    private final ThreadLocal<Boolean> restoring = new ThreadLocal<>();
    private volatile Hooks hooks;
    private volatile boolean enabled, released, fillEnabled, followMedia, borderEnabled, radiusEnabled;
    private volatile boolean gradientEnabled, blurEnabled, marqueeEnabled, marqueeReverse, glowEnabled;
    private volatile String gradientMode = TOP_BOTTOM, marqueePreset = DEFAULT_MARQUEE_PRESET;
    private volatile int fillColor = DEFAULT_FILL_COLOR, borderColor = DEFAULT_BORDER_COLOR, gradientColor = DEFAULT_GRADIENT_COLOR;
    private volatile int blurMaskColor = DEFAULT_BLUR_MASK_COLOR, marqueeColorCount = DEFAULT_MARQUEE_COLOR_COUNT;
    private volatile int[] marqueeCustom = DEFAULT_MARQUEE_COLORS.clone();
    private volatile float fillOpacity = DEFAULT_FILL_OPACITY, borderWidth = DEFAULT_BORDER_WIDTH, radius = DEFAULT_RADIUS;
    private volatile float blurRadius = DEFAULT_BLUR_RADIUS, marqueeOpacity = DEFAULT_MARQUEE_OPACITY, marqueePeriod = DEFAULT_MARQUEE_PERIOD;
    private volatile float glowDepth = DEFAULT_GLOW_DEPTH, gradientRange = DEFAULT_GRADIENT_RANGE;
    private volatile boolean coverEnabled, coverSizeEnabled;
    private volatile float coverRadius = DEFAULT_COVER_RADIUS, coverScale = DEFAULT_COVER_SCALE;
    private final CapsuleCover cover = new CapsuleCover();
    private final CapsuleTextFade textFade = new CapsuleTextFade(capsule -> active() && capsule.getBackground() instanceof PillBackground);
    private volatile long revision;
    private volatile boolean blurUnavailable;

    public FluidCloudCapsule(FluidCloudAccent accent) { this.accent = accent; cover.setWantedFor(this::coverWantedFor); }

    /** One view and what the plugin had drawn before this class changed anything. */
    private static final class Binding {
        ColorStateList nativeFill;
        float nativeFillRadius = Float.NaN, progress, weight = -1f;
        boolean captured, lineChanged;
        long revision = -1, accentRevision = -1;
        MarqueeRing ring;
        BlurHolder blur;
        /** The plugin's own pill comes back when a colour has finished handing over to it. */
        Runnable revert;
    }

    /** The members this class hooks, resolved once per plugin class loader. */
    public static final class Hooks {
        public final Method onFinishInflate, onAttachedToWindow, setHidden, progress, draw;
        public Hooks(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> type = Class.forName(VIEW, false, loader);
            if (!FrameLayout.class.isAssignableFrom(type)) throw new NoSuchMethodException(VIEW + " is not a FrameLayout");
            onFinishInflate = type.getDeclaredMethod("onFinishInflate");
            onAttachedToWindow = type.getDeclaredMethod("onAttachedToWindow");
            setHidden = type.getDeclaredMethod("setHidden", boolean.class);
            draw = type.getDeclaredMethod("onDraw", Canvas.class);
            Method step = null;
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != float.class || method.getReturnType() != void.class) continue;
                if (step != null) throw new NoSuchMethodException(VIEW + " has several expansion steps");
                step = method;
            }
            if (step == null) throw new NoSuchMethodException(VIEW + " has no expansion step");
            int drawables = 0;
            for (Field field : type.getDeclaredFields()) if (field.getType() == GradientDrawable.class) drawables++;
            if (drawables != 1) throw new NoSuchMethodException(VIEW + " outline drawable changed");
            progress = step;
            for (Method method : new Method[]{onFinishInflate, onAttachedToWindow, setHidden, step, draw}) method.setAccessible(true);
        }
    }

    /** Resolve the structure; null when this ROM's capsule is not the audited one, so it stays native. */
    public Hooks resolve(ClassLoader loader) {
        if (loader == null || released) return null;
        try {
            Hooks found = new Hooks(loader);
            hooks = found;
            return found;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            ModuleDiagnostics.info("fluid", "Capsule contract unavailable; the camera-area fluid cloud stays native");
            return null;
        }
    }

    public boolean active() { return enabled && !released; }
    public long revision() { return revision; }
    public CapsuleTextFade.Hooks resolveTextFade(ClassLoader loader) { return textFade.resolve(loader); }
    public void textChanged(View view) { textFade.changed(view); }
    public void textDetached(View view) { textFade.detached(view); }

    public void configure(Bundle settings) {
        if (released) return;
        boolean nextEnabled = settings != null && settings.getBoolean(MASTER, false) && !SafetyMode.enabled(settings);
        boolean nextFill = settings != null && settings.getBoolean(FILL_ENABLED, false);
        boolean nextFollow = settings != null && settings.getBoolean(FOLLOW_MEDIA, false);
        boolean nextBorder = settings != null && settings.getBoolean(BORDER_ENABLED, false);
        boolean nextRadius = settings != null && settings.getBoolean(RADIUS_ENABLED, false);
        int nextFillColor = alphaColor(settings, FILL_COLOR, DEFAULT_FILL_COLOR);
        int nextBorderColor = alphaColor(settings, BORDER_COLOR, DEFAULT_BORDER_COLOR);
        float nextOpacity = number(settings, FILL_OPACITY, DEFAULT_FILL_OPACITY, 0f, 100f);
        float nextWidth = number(settings, BORDER_WIDTH, DEFAULT_BORDER_WIDTH, 0f, MAX_BORDER_WIDTH);
        float nextRadiusValue = number(settings, RADIUS, DEFAULT_RADIUS, 0f, MAX_RADIUS);
        boolean nextGradient = settings != null && settings.getBoolean(GRADIENT_ENABLED, false);
        String nextMode = mode(settings == null ? null : settings.get(GRADIENT_MODE));
        int nextGradientColor = alphaColor(settings, GRADIENT_COLOR, DEFAULT_GRADIENT_COLOR);
        boolean nextBlur = settings != null && settings.getBoolean(BLUR_ENABLED, false);
        float nextBlurRadius = number(settings, BLUR_RADIUS, DEFAULT_BLUR_RADIUS, 0f, MAX_BLUR_RADIUS);
        int nextMask = alphaColor(settings, BLUR_MASK_COLOR, DEFAULT_BLUR_MASK_COLOR);
        boolean nextMarquee = settings != null && settings.getBoolean(MARQUEE_ENABLED, false);
        String nextPreset = preset(settings == null ? null : settings.get(MARQUEE_PRESET));
        int[] nextCustom = new int[MARQUEE_COLOR_KEYS.length];
        for (int i = 0; i < nextCustom.length; i++)
            nextCustom[i] = color(settings == null ? null : settings.get(MARQUEE_COLOR_KEYS[i]), DEFAULT_MARQUEE_COLORS[i]) | 0xff000000;
        int nextCount = Math.round(number(settings, MARQUEE_COLOR_COUNT, DEFAULT_MARQUEE_COLOR_COUNT, MIN_MARQUEE_COLORS, MAX_MARQUEE_COLORS));
        float nextMarqueeOpacity = number(settings, MARQUEE_OPACITY, DEFAULT_MARQUEE_OPACITY, 0f, 100f);
        float nextPeriod = number(settings, MARQUEE_PERIOD, DEFAULT_MARQUEE_PERIOD, MIN_MARQUEE_PERIOD, MAX_MARQUEE_PERIOD);
        boolean nextReverse = settings != null && settings.getBoolean(MARQUEE_REVERSE, false);
        boolean nextGlow = settings != null && settings.getBoolean(GLOW_ENABLED, false);
        float nextGlowDepth = number(settings, GLOW_DEPTH, DEFAULT_GLOW_DEPTH, MIN_GLOW_DEPTH, MAX_GLOW_DEPTH);
        float nextRange = number(settings, GRADIENT_RANGE, DEFAULT_GRADIENT_RANGE, 5f, 100f);
        boolean nextCover = settings != null && settings.getBoolean(COVER_RADIUS_ENABLED, false);
        float nextCoverRadius = number(settings, COVER_RADIUS, DEFAULT_COVER_RADIUS, 0f, MAX_COVER_RADIUS);
        boolean nextCoverSize = settings != null && settings.getBoolean(COVER_SIZE_ENABLED, false);
        float nextCoverScale = number(settings, COVER_SCALE, DEFAULT_COVER_SCALE, MIN_COVER_SCALE, MAX_COVER_SCALE);
        if (enabled == nextEnabled && fillEnabled == nextFill && followMedia == nextFollow && borderEnabled == nextBorder
                && radiusEnabled == nextRadius && fillColor == nextFillColor && borderColor == nextBorderColor
                && fillOpacity == nextOpacity && borderWidth == nextWidth && radius == nextRadiusValue
                && gradientEnabled == nextGradient && gradientMode.equals(nextMode) && gradientColor == nextGradientColor
                && blurEnabled == nextBlur && blurRadius == nextBlurRadius && blurMaskColor == nextMask && marqueeEnabled == nextMarquee
                && marqueePreset.equals(nextPreset) && java.util.Arrays.equals(marqueeCustom, nextCustom) && marqueeColorCount == nextCount
                && marqueeOpacity == nextMarqueeOpacity && marqueePeriod == nextPeriod && marqueeReverse == nextReverse
                && glowEnabled == nextGlow && glowDepth == nextGlowDepth && gradientRange == nextRange
                && coverEnabled == nextCover && coverRadius == nextCoverRadius && coverSizeEnabled == nextCoverSize && coverScale == nextCoverScale) return;
        enabled = nextEnabled; fillEnabled = nextFill; followMedia = nextFollow; borderEnabled = nextBorder; radiusEnabled = nextRadius;
        fillColor = nextFillColor; borderColor = nextBorderColor; fillOpacity = nextOpacity; borderWidth = nextWidth; radius = nextRadiusValue;
        gradientEnabled = nextGradient; gradientMode = nextMode; gradientColor = nextGradientColor;
        blurEnabled = nextBlur; blurRadius = nextBlurRadius; blurMaskColor = nextMask; marqueeEnabled = nextMarquee; marqueePreset = nextPreset;
        marqueeCustom = nextCustom; marqueeColorCount = nextCount;
        marqueeOpacity = nextMarqueeOpacity; marqueePeriod = nextPeriod; marqueeReverse = nextReverse;
        glowEnabled = nextGlow; glowDepth = nextGlowDepth; gradientRange = nextRange;
        coverEnabled = nextCover; coverRadius = nextCoverRadius;
        coverSizeEnabled = nextCoverSize; coverScale = nextCoverScale;
        revision++;
        refreshAll();
    }

    /** A colour setting with its "own alpha" switch: without it the colour is opaque. */
    private static int alphaColor(Bundle settings, String key, int fallback) {
        int value = color(settings == null ? null : settings.get(key), fallback);
        Object custom = settings == null ? null : settings.get(StatusBarSettings.alphaKey(key));
        if (!(custom instanceof Boolean ? (Boolean) custom : (value >>> 24) != 255)) value |= 0xff000000;
        return value;
    }

    static String mode(Object value) { return BOTTOM_TOP.equals(value) || CENTER.equals(value) ? (String) value : TOP_BOTTOM; }
    static String preset(Object value) {
        for (String id : MARQUEE_PRESETS) if (id.equals(value)) return id;
        return DEFAULT_MARQUEE_PRESET;
    }

    /** The colours of one lap of the running light, for the settings page's previews as well as the ring. Closed: the last colour
     * is the first, so the lap has no seam. {@code custom} holds the user's six colours, {@code count} how many of them are used. */
    public static int[] presetColors(String preset, int[] custom, int count) {
        int[] mine = custom == null || custom.length == 0 ? DEFAULT_MARQUEE_COLORS : custom;
        int used = Math.max(1, Math.min(mine.length, count));
        if ("comet".equals(preset)) {
            int head = mine[0] | 0xff000000;
            return new int[]{head & 0x00ffffff, head & 0x00ffffff, head & 0x00ffffff, head};
        }
        if ("custom".equals(preset)) {
            int[] lap = new int[used + 1];
            for (int i = 0; i < used; i++) lap[i] = mine[i] | 0xff000000;
            lap[used] = lap[0];
            return lap;
        }
        for (int i = 0; i < PRESET_COLORS.length; i++) if (MARQUEE_PRESETS[i].equals(preset)) return PRESET_COLORS[i].clone();
        return PRESET_COLORS[0].clone();
    }

    /** Capsule views that are on screen pick a change up at once, not at their next draw. */
    private void refreshAll() {
        final ArrayList<View> views;
        synchronized (bindings) { views = new ArrayList<>(bindings.keySet()); }
        if (views.isEmpty()) return;
        Runnable apply = () -> {
            for (View view : views) if (view != null) { apply(view, Float.NaN); textFade.capsuleChanged(view); view.invalidate(); }
            textFade.refresh();
        };
        if (Looper.myLooper() == Looper.getMainLooper()) apply.run(); else main.post(apply);
    }

    /** The cover radius wanted for this capsule now (px), before the circle-to-radius blend; negative: the plugin's own. */
    private float wantedCover(View capsule, float weight) {
        if (!active() || !coverEnabled || !(weight > 0f)) return -1f;
        View known = cover.cover(capsule);
        // A timer or call capsule that reuses the cover view gives it back to the plugin.
        if (known == null || !cover.musicCapsuleCached(capsule)) return -1f;
        return CapsuleCover.shown(known, coverRadius * capsule.getResources().getDisplayMetrics().density, weight);
    }

    /** For the cover hooks: what a cover the plugin is just setting up should be, from its capsule. */
    double coverWantedFor(View coverView) {
        View capsule = CapsuleCover.capsuleOf(coverView, VIEW);
        if (capsule == null || !active() || !coverEnabled) return -1;
        float weight = shapeWeight(capsule);
        if (!(weight > 0f) || !CapsuleCover.musicCapsule(capsule)) return -1;
        return CapsuleCover.shown(coverView, coverRadius * capsule.getResources().getDisplayMetrics().density, weight);
    }
    Object coverShape(View coverView, Object shape) { return cover.shapeRequested(coverView, shape); }
    float coverRadius(View coverView, float radius) { return cover.radiusRequested(coverView, radius); }

    /** Called after the plugin inflates, attaches, hides/shows or steps the expansion of one capsule. */
    public void changed(View view, float progress) {
        if (released || view == null || Boolean.TRUE.equals(restoring.get())) return;
        cover.bindingChanged(view);
        apply(view, progress);
    }

    /** Called before the plugin draws: a changed setting or media colour reaches a capsule that already exists. */
    public void beforeDraw(View view) {
        if (released || view == null || Boolean.TRUE.equals(restoring.get())) return;
        textFade.refresh();
        Binding binding = bindings.get(view);
        long colours = followMedia && accent != null ? accent.colorRevision() : 0L;
        float weight = shapeWeight(view);
        if (binding != null && binding.revision == revision && binding.accentRevision == colours && Math.abs(weight - binding.weight) < .01f
                && cover.settled(view, wantedCover(view, weight))
                && cover.scaleSettled(view, active() && coverSizeEnabled ? coverScale : DEFAULT_COVER_SCALE, weight)) return;
        apply(view, Float.NaN);
    }

    private void apply(View view, float progress) {
        Drawable background = view.getBackground(), foreground = view.getForeground();
        GradientDrawable nativeFill = background instanceof PillBackground ? ((PillBackground) background).nativeDrawable
                : background instanceof GradientDrawable ? (GradientDrawable) background : null;
        if (nativeFill == null || !(foreground instanceof GradientDrawable)) return;
        GradientDrawable line = (GradientDrawable) foreground;
        Binding binding;
        synchronized (bindings) {
            binding = bindings.get(view);
            if (binding == null) { binding = new Binding(); bindings.put(view, binding); }
        }
        if (!Float.isNaN(progress)) binding.progress = progress;
        boolean discoverText = !binding.captured;
        if (!binding.captured) {
            binding.nativeFill = nativeFill.getColor();
            binding.nativeFillRadius = nativeFill.getCornerRadius();
            binding.captured = true;
        }
        binding.revision = revision;
        binding.accentRevision = followMedia && accent != null ? accent.colorRevision() : 0L;
        boolean on = active();
        float density = view.getResources().getDisplayMetrics().density;
        // Shrunk onto the camera the capsule keeps the plugin's own shape; the corner settings follow its real width.
        float weight = shapeWeight(view);
        binding.weight = weight;
        // The music capsule's album cover: our corner radius, or the plugin's own circle.
        cover.apply(view, on && coverEnabled ? coverRadius * density : -1f, weight);
        cover.applyScale(view, on && coverSizeEnabled ? coverScale : DEFAULT_COVER_SCALE, weight);
        float nativeRadius = Float.isNaN(binding.nativeFillRadius) ? DEFAULT_RADIUS * density : binding.nativeFillRadius;
        boolean customRadius = on && radiusEnabled;
        float cornerPx = customRadius ? nativeRadius + (radius * density - nativeRadius) * weight : nativeRadius;
        // The frosted glass takes the fill's place: a translucent pill over the window's own blur, tinted by the mask colour.
        boolean glass = false;
        if (on && blurEnabled) glass = attachBlur(view, binding, density, cornerPx); else detachBlur(view, binding);
        Integer colour = on && !glass ? fillColour() : null;
        boolean wantPill = glass || colour != null || customRadius;
        if (wantPill) {
            PillBackground pill = background instanceof PillBackground ? (PillBackground) background : new PillBackground(nativeFill);
            int base = binding.nativeFill == null ? DEFAULT_FILL_COLOR : binding.nativeFill.getDefaultColor();
            pill.look(glass, colour == null ? base : colour, colour != null && gradientEnabled ? gradientMode : null,
                    withOpacity(gradientColor, fillOpacity), customRadius ? cornerPx : Float.NaN, nativeRadius);
            pill.blur(glass ? binding.blur : null);
            pill.range(gradientRange / 100f);
            if (background != pill) { pill.setBounds(background.getBounds()); view.setBackground(pill); }
            else pill.invalidateSelf();
        } else if (background instanceof PillBackground) {
            PillBackground shown = (PillBackground) background;
            int plainFill = binding.nativeFill == null ? DEFAULT_FILL_COLOR : binding.nativeFill.getDefaultColor();
            if (on && shown.holds(plainFill)) {
                // The colour that followed the media is gone and nothing is chosen in its place: it fades to the plugin's own colour,
                // and the plugin's own pill comes back when it has.
                if (shown.fillColor() != plainFill) shown.look(false, plainFill, null, 0, Float.NaN, nativeRadius);
                scheduleRevert(view, binding);
            } else {
                shown.blur(null);
                nativeFill.setBounds(background.getBounds());
                view.setBackground(nativeFill);
            }
        }
        // The outline: the running light, a plain border, or the plugin's own collapsed outline redrawn on the custom corners all
        // come from one ring in the overlay; the plugin's own stroke is taken off while that ring is there.
        boolean running = on && marqueeEnabled, plain = on && !running && borderEnabled, nativeLike = on && !running && !plain && customRadius;
        if (running || plain || nativeLike) {
            line.setStroke(0, 0); binding.lineChanged = true;
            attachRing(view, binding, density, cornerPx, running, plain, binding.progress);
        } else {
            detachRing(view, binding);
            if (binding.lineChanged) {
                binding.lineChanged = false;
                Hooks current = hooks;
                if (current != null) {
                    Boolean previous = restoring.get();
                    restoring.set(Boolean.TRUE);
                    try { current.progress.invoke(view, binding.progress); }
                    catch (ReflectiveOperationException | RuntimeException unavailable) { ModuleDiagnostics.info("fluid", "Capsule outline could not be restored"); }
                    finally { if (previous == null) restoring.remove(); else restoring.set(previous); }
                }
            }
        }
        if (discoverText || view.getBackground() != background) textFade.capsuleChanged(view);
    }

    /** One look at the capsule when a hand-over to the plugin's own colour should be over. */
    private void scheduleRevert(View view, Binding binding) {
        if (binding.revert != null) return;
        binding.revert = () -> {
            binding.revert = null;
            if (!released && view.getBackground() instanceof PillBackground) apply(view, Float.NaN);
        };
        main.postDelayed(binding.revert, PillBackground.FADE_MS + 60L);
    }

    private void attachRing(View view, Binding binding, float density, float cornerPx, boolean running, boolean plain, float progress) {
        MarqueeRing ring = binding.ring;
        if (ring == null) {
            ring = new MarqueeRing(view);
            try { view.getOverlay().add(ring); } catch (RuntimeException unavailable) { return; }
            binding.ring = ring;
        }
        if (running) {
            // Without the custom border the running light keeps the plugin's own outline width; the width setting belongs to that border.
            float width = borderEnabled ? borderWidth : DEFAULT_BORDER_WIDTH;
            ring.animated(presetColors(marqueePreset, marqueeCustom, marqueeColorCount), marqueeOpacity,
                    Math.max(1, Math.round(width * density)), cornerPx, Math.round(marqueePeriod * 1000f), marqueeReverse,
                    glowEnabled ? glowDepth * density : 0f);
        } else if (plain) {
            ring.fixed(borderColor, Math.max(1, Math.round(borderWidth * density)), cornerPx, true);
        } else {
            ring.fixed(DEFAULT_BORDER_COLOR, Math.max(1, Math.round(1.3f * density)), cornerPx, progress <= NATIVE_OUTLINE_PROGRESS);
        }
        view.invalidate();
    }

    private void detachRing(View view, Binding binding) {
        MarqueeRing ring = binding.ring;
        if (ring == null) return;
        binding.ring = null;
        ring.stop();
        try { view.getOverlay().remove(ring); } catch (RuntimeException ignored) { }
        view.invalidate();
    }

    /** The frosted glass is a platform blur region drawn by the pill itself, as the very first thing, so the mask colour it carries
     * lies under the capsule's icons and text and never over them (an overlay would be drawn after the children). */
    private boolean attachBlur(View view, Binding binding, float density, float cornerPx) {
        if (blurUnavailable || !view.isAttachedToWindow()) return false;
        if (binding.blur == null) {
            BlurHolder holder = BlurHolder.create(view);
            if (holder == null) { blurUnavailable = true; ModuleDiagnostics.info("fluid", "Capsule background blur unavailable on this system"); return false; }
            binding.blur = holder;
        }
        binding.blur.configure(Math.round(blurRadius * density), cornerPx, blurMaskColor);
        view.invalidate();
        return true;
    }

    private void detachBlur(View view, Binding binding) {
        BlurHolder holder = binding.blur;
        if (holder == null) return;
        binding.blur = null;
        Drawable background = view.getBackground();
        if (background instanceof PillBackground) ((PillBackground) background).blur(null);
        holder.release();
        view.invalidate();
    }

    /** The pill itself: a solid colour or a gradient, in a rounded rectangle over the bounds the plugin gives it (they animate
     * with the capsule). It keeps the plugin's own drawable so it can be put back, and forwards nothing to it. Gradients are built
     * once in unit coordinates and scaled to the bounds with a matrix, so a growing pill allocates nothing per frame. */
    static final class PillBackground extends Drawable {
        static final int SOLID = 0, TOP_DOWN = 1, BOTTOM_UP = 2, RADIAL = 3;
        final GradientDrawable nativeDrawable;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Matrix matrix = new Matrix();
        /** A new colour of the same kind of fill fades in over this long instead of replacing the old one at once: when the media is
         * closed, the colour that followed it hands over to the chosen one smoothly (a new track's colour does the same). */
        static final long FADE_MS = 600L;
        private boolean transparent, seen;
        private int fill, start, end, mode = SOLID, alpha = 255;
        private float radiusPx = Float.NaN, nativeRadius;
        private Shader shader;
        private int shaderMode = -1, shaderStart, shaderEnd;
        private float range = 1f, shaderRange = Float.NaN;
        private BlurHolder blur;
        private int fromStart, fromEnd;
        private long fadeStart = -FADE_MS;

        PillBackground(GradientDrawable nativeDrawable) { this.nativeDrawable = nativeDrawable; }

        /** The platform blur region this pill draws in its own place (null: none). */
        void blur(BlurHolder holder) { if (blur != holder) { blur = holder; invalidateSelf(); } }
        BlurHolder blurHolder() { return blur; }

        /** {@code direction} is a gradient direction, or null for a solid fill; {@code end} is the gradient's last colour. */
        void look(boolean glass, int colour, String direction, int endColour, float customRadius, float nativeCorner) {
            int nextMode = direction == null ? SOLID : CENTER.equals(direction) ? RADIAL : BOTTOM_TOP.equals(direction) ? BOTTOM_UP : TOP_DOWN;
            long now = SystemClock.uptimeMillis();
            if (seen && !glass && !transparent && nextMode == mode) {
                // The same kind of fill in another colour: from what is on the screen at this instant to the new colour. The plugin
                // asks for the same look at every step of its expansion; that changes nothing and restarts nothing.
                if (colour != start || nextMode != SOLID && endColour != end) {
                    fromStart = shown(now, fromStart, start);
                    fromEnd = shown(now, fromEnd, end);
                    fadeStart = now;
                }
            } else {
                fromStart = colour; fromEnd = endColour; fadeStart = now - FADE_MS;
            }
            seen = true;
            transparent = glass; fill = colour; radiusPx = customRadius; nativeRadius = nativeCorner;
            mode = nextMode;
            start = colour; end = endColour;
            invalidateSelf();
        }

        /** The colour between two, {@code from} at the start of the fade and {@code to} at its end, easing in and out. */
        private int shown(long now, int from, int to) {
            long elapsed = now - fadeStart;
            if (elapsed >= FADE_MS) return to;
            if (elapsed <= 0L) return from;
            float t = elapsed / (float) FADE_MS;
            return blend(from, to, t * t * (3f - 2f * t));
        }

        static int blend(int from, int to, float t) {
            int result = 0;
            for (int shift = 24; shift >= 0; shift -= 8) {
                int a = (from >>> shift) & 0xff, b = (to >>> shift) & 0xff;
                result |= Math.max(0, Math.min(255, Math.round(a + (b - a) * t))) << shift;
            }
            return result;
        }

        /** True while this pill shows a plain colour other than {@code plain}, or is still fading from one. */
        boolean holds(int plain) {
            return !transparent && mode == SOLID && (fill != plain || SystemClock.uptimeMillis() - fadeStart < FADE_MS);
        }

        /** How much of the pill the gradient's change of colour takes (0.05..1); outside it the two colours stay solid. */
        void range(float fraction) {
            float next = Math.max(.05f, Math.min(1f, fraction));
            if (next != range) { range = next; invalidateSelf(); }
        }
        float range() { return range; }

        boolean glass() { return transparent; }
        int mode() { return mode; }
        int fillColor() { return fill; }
        int gradientEnd() { return end; }
        float cornerPx() { return radius(getBounds()); }

        float radius(Rect bounds) {
            float corner = Float.isNaN(radiusPx) ? nativeRadius : radiusPx;
            return Math.max(0f, Math.min(corner, Math.min(bounds.width(), bounds.height()) / 2f));
        }

        /** In unit coordinates: the change of colour sits in the middle of the run and takes {@code range} of it; CLAMP keeps the ends solid. */
        private Shader shader(int first, int last) {
            if (shader != null && shaderMode == mode && shaderStart == first && shaderEnd == last && shaderRange == range) return shader;
            float from = (1f - range) / 2f, to = (1f + range) / 2f;
            switch (mode) {
                case RADIAL: shader = new RadialGradient(.5f, .5f, .5f, new int[]{first, first, last, last}, new float[]{0f, from, to, 1f},
                        Shader.TileMode.CLAMP); break;
                case BOTTOM_UP: shader = new LinearGradient(0f, to, 0f, from, first, last, Shader.TileMode.CLAMP); break;
                default: shader = new LinearGradient(0f, from, 0f, to, first, last, Shader.TileMode.CLAMP);
            }
            shaderMode = mode; shaderStart = first; shaderEnd = last; shaderRange = range;
            return shader;
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) return;
            // Frosted glass: nothing of the pill's own, only the platform's blur region (with its mask colour) over the same bounds.
            if (transparent) { if (blur != null) blur.draw(canvas, bounds); return; }
            rect.set(bounds);
            float corner = radius(bounds);
            long now = SystemClock.uptimeMillis();
            int first = shown(now, fromStart, start), last = shown(now, fromEnd, end);
            if (mode == SOLID) {
                paint.setShader(null);
                paint.setColor(first);
                paint.setAlpha(Math.round((first >>> 24) * alpha / 255f));
            } else {
                Shader gradient = shader(first, last);
                matrix.setScale(bounds.width(), bounds.height());
                matrix.postTranslate(bounds.left, bounds.top);
                gradient.setLocalMatrix(matrix);
                paint.setShader(gradient);
                paint.setColor(0xff000000);
                paint.setAlpha(alpha);
            }
            canvas.drawRoundRect(rect, corner, corner, paint);
            if (now - fadeStart < FADE_MS) invalidateSelf();
        }

        @Override public void getOutline(Outline outline) {
            Rect bounds = getBounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) { outline.setEmpty(); return; }
            outline.setRoundRect(bounds, radius(bounds));
            outline.setAlpha(transparent ? 0f : mode == SOLID ? (fill >>> 24) / 255f * alpha / 255f : alpha / 255f);
        }

        @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
        @Override public int getAlpha() { return alpha; }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return -1; }
        @Override public int getIntrinsicHeight() { return -1; }
    }

    /** The outline ring in the view's overlay. Animated: one lap of colour running around the pill (a rotating sweep gradient
     * drawn as a stroked rounded rectangle, about thirty frames a second, only while it is drawn), optionally with a glow fading
     * inwards from the outline. Fixed: one stroke of one colour (a plain border, or the plugin's own collapsed outline). Both
     * follow the pill's current bounds. */
    static final class MarqueeRing extends Drawable {
        private static final long FRAME_MS = 32L;
        /** The fallback glow (no runtime shaders) is a run of thin rings that overlap, so they run into one smooth fade. */
        static int glowSteps(float glowPx) { return Math.max(4, Math.min(64, Math.round(glowPx / 1.25f))); }
        /** The glow in one draw: inside the pill, the running colour's alpha falls off with the distance from the edge (a rounded-box
         * distance), from the inner side of the ring over {@code depth} pixels, as (1 - t)^2. Continuous, so there are no steps, and one
         * fill instead of dozens of strokes per frame. */
        static final String GLOW_SHADER = "uniform shader sweep; uniform float4 box; uniform float radius; uniform float start;"
                + " uniform float depth; uniform float strength;"
                + "half4 main(float2 p){ float2 c=(box.xy+box.zw)*0.5; float2 h=(box.zw-box.xy)*0.5;"
                + " float2 q=abs(p-c)-h+float2(radius); float d=length(max(q,float2(0.0)))+min(max(q.x,q.y),0.0)-radius;"
                + " float inside=-d-start; if(inside<=0.0)return half4(0);"
                + " float t=clamp(inside/depth,0.0,1.0); float a=strength*(1.0-t)*(1.0-t);"
                + " return sweep.eval(p)*half(a); }";
        /** Peak strength of the glow next to the ring, relative to the ring's own opacity. */
        static final float GLOW_STRENGTH = .8f;
        private static volatile boolean glowShaderRefused;
        private android.graphics.RuntimeShader glow;
        private final View owner;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Matrix matrix = new Matrix();
        private int[] colours = new int[]{0, 0};
        private Shader shader;
        private float cx = Float.NaN, cy = Float.NaN, strokePx = 1f, radiusPx, glowPx;
        private long periodMs = 3000L;
        private boolean reverse, stopped, animated, shown = true;
        private int alpha = 255, fixedColour;

        MarqueeRing(View owner) { this.owner = owner; paint.setStyle(Paint.Style.STROKE); }

        void animated(int[] nextColours, float opacityPercent, float stroke, float radius, long period, boolean backwards, float glow) {
            if (!java.util.Arrays.equals(colours, nextColours)) { colours = nextColours; shader = null; }
            alpha = Math.round(255f * Math.max(0f, Math.min(100f, opacityPercent)) / 100f);
            strokePx = Math.max(1f, stroke); radiusPx = radius; periodMs = Math.max(200L, period); reverse = backwards;
            glowPx = Math.max(0f, glow); animated = true; shown = true; stopped = false;
        }

        void fixed(int colour, float stroke, float radius, boolean visible) {
            fixedColour = colour; strokePx = Math.max(1f, stroke); radiusPx = radius; shown = visible;
            animated = false; glowPx = 0f; stopped = false;
        }

        void stop() { stopped = true; }
        boolean isAnimated() { return animated; }
        boolean isShown() { return shown; }
        float glow() { return glowPx; }

        /** The angle of the light at this instant, in degrees. */
        float angle(long now) {
            float turn = (now % periodMs) / (float) periodMs * 360f;
            return reverse ? -turn : turn;
        }

        @Override public void draw(Canvas canvas) {
            if (stopped || !owner.isAttachedToWindow() || !owner.isShown()
                    || owner.getWindowVisibility() != View.VISIBLE || owner.getAlpha() <= 0f) return;
            Drawable background = owner.getBackground();
            Rect pill = background == null ? null : background.getBounds();
            // Setting, visibility and layout changes invalidate the owner themselves. Empty or transparent frames have no
            // pixels to animate, so they must not keep the 32 ms redraw loop alive while waiting for one of those changes.
            if (!shown || pill == null || pill.width() <= 0 || pill.height() <= 0 || animated && alpha == 0) return;
            if (!animated) {
                paint.setShader(null); paint.setColor(fixedColour); paint.setStrokeWidth(strokePx);
                ring(canvas, pill, strokePx / 2f);
                return;
            }
            float half = strokePx / 2f;
            float centreX = pill.exactCenterX(), centreY = pill.exactCenterY();
            if (shader == null || centreX != cx || centreY != cy) {
                cx = centreX; cy = centreY;
                shader = new SweepGradient(cx, cy, colours, null);
            }
            matrix.setRotate(angle(SystemClock.uptimeMillis()), cx, cy);
            shader.setLocalMatrix(matrix);
            paint.setShader(shader); paint.setStrokeWidth(strokePx); paint.setAlpha(alpha);
            ring(canvas, pill, half);
            // The glow: the colour of the running light bleeds inward from the ring and fades out smoothly, in the same running colours.
            // One shader fill where the platform has runtime shaders; otherwise rings that are each wider than the step to the next (no gap)
            // with an alpha that falls off continuously.
            if (glowPx > 0f && !drawGlow(canvas, pill)) {
                int steps = glowSteps(glowPx);
                float step = glowPx / steps;
                paint.setStrokeWidth(Math.max(1.5f, step * 1.6f));
                for (int i = 0; i < steps; i++) {
                    float depth = (i + .5f) / steps;
                    int ringAlpha = Math.round(alpha * .5f * (1f - depth) * (1f - depth));
                    if (ringAlpha < 2) break;
                    paint.setAlpha(ringAlpha);
                    ring(canvas, pill, strokePx + (i + .5f) * step);
                }
            }
            next();
        }

        /** The glow as one fill with the runtime shader; false when the platform cannot (it is then never tried again). */
        private boolean drawGlow(Canvas canvas, Rect pill) {
            if (glowShaderRefused || android.os.Build.VERSION.SDK_INT < 33) return false;
            float radius = Math.max(0f, Math.min(radiusPx, Math.min(pill.width(), pill.height()) / 2f));
            rect.set(pill.left + strokePx, pill.top + strokePx, pill.right - strokePx, pill.bottom - strokePx);
            if (rect.width() <= 0f || rect.height() <= 0f) return true;
            try {
                if (glow == null) glow = new android.graphics.RuntimeShader(GLOW_SHADER);
                glow.setFloatUniform("box", pill.left, pill.top, pill.right, pill.bottom);
                glow.setFloatUniform("radius", radius);
                glow.setFloatUniform("start", strokePx);
                glow.setFloatUniform("depth", Math.max(1f, glowPx));
                glow.setFloatUniform("strength", alpha / 255f * GLOW_STRENGTH);
                // Set again every frame: the sweep's rotation changed, and this makes the shader pick the new matrix up.
                glow.setInputShader("sweep", shader);
            } catch (RuntimeException | LinkageError refused) {
                glowShaderRefused = true; glow = null;
                ModuleDiagnostics.info("fluid", "Capsule glow shader unavailable; overlapping rings stand in");
                return false;
            }
            float corner = Math.max(0f, radius - strokePx);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(glow);
            paint.setAlpha(255);
            canvas.drawRoundRect(rect, corner, corner, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setShader(shader);
            return true;
        }

        private void ring(Canvas canvas, Rect pill, float inset) {
            rect.set(pill.left + inset, pill.top + inset, pill.right - inset, pill.bottom - inset);
            if (rect.width() <= 0f || rect.height() <= 0f) return;
            float radius = Math.max(0f, Math.min(radiusPx - inset, Math.min(rect.width(), rect.height()) / 2f));
            canvas.drawRoundRect(rect, radius, radius, paint);
        }

        private void next() { if (!stopped) owner.postInvalidateDelayed(FRAME_MS); }

        @Override public void setAlpha(int value) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** The platform's own background blur for a region of the window (ViewRootImpl.createBackgroundBlurDrawable), reached by
     * reflection, tinted by a mask colour. It blurs what is behind the window in the pill's bounds. The pill draws it. */
    static final class BlurHolder {
        private final Drawable blur;
        private final Method setRadius, setCorner, setMask;
        private boolean released;
        private float cornerPx;

        private BlurHolder(Drawable blur, Method setRadius, Method setCorner, Method setMask) {
            this.blur = blur; this.setRadius = setRadius; this.setCorner = setCorner; this.setMask = setMask;
        }

        static BlurHolder create(View view) {
            try {
                Object root = View.class.getMethod("getViewRootImpl").invoke(view);
                if (root == null) return null;
                Object made = root.getClass().getMethod("createBackgroundBlurDrawable").invoke(root);
                if (!(made instanceof Drawable)) return null;
                Method mask = null;
                try { mask = made.getClass().getMethod("setColor", int.class); } catch (NoSuchMethodException withoutMask) { }
                return new BlurHolder((Drawable) made, made.getClass().getMethod("setBlurRadius", int.class),
                        made.getClass().getMethod("setCornerRadius", float.class), mask);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return null; }
        }

        void configure(int radiusPx, float corner, int maskColour) {
            cornerPx = Math.max(0f, corner);
            try {
                setRadius.invoke(blur, Math.max(0, radiusPx));
                if (setMask != null) setMask.invoke(blur, maskColour);
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }

        void release() {
            released = true;
            try { setRadius.invoke(blur, 0); } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }

        /** Draw the region over the pill's current bounds; the corner is at most half the pill's height and follows it as it grows. */
        void draw(Canvas canvas, Rect pill) {
            if (released || pill == null || pill.isEmpty()) return;
            try { setCorner.invoke(blur, Math.min(cornerPx, Math.min(pill.width(), pill.height()) / 2f)); }
            catch (ReflectiveOperationException | RuntimeException ignored) { }
            blur.setBounds(pill);
            blur.draw(canvas);
        }
    }

    /** The colour the spectrum bars take when they follow the accent (RGB): the media colour of the moment, raised to a brightness that
     * shows on the capsule (the hue stays) and, when the pill itself is filled with about that colour, turned to a dark or a light one
     * so the bars never vanish into it. Null when there is no media colour to follow. */
    Integer spectrumAccent() {
        FluidCloudAccent owner = accent;
        Integer media = owner == null ? null : owner.nativeCurrentMediaColor(null);
        if (media == null) return null;
        int bars = CapsuleSpectrum.visible(media);
        Integer pill = fillColour();
        if (pill != null && (pill >>> 24) >= 0x80 && Math.abs(luma(bars) - luma(pill)) < 56) bars = luma(pill) > 140 ? 0x202020 : 0xffffff;
        return bars;
    }

    private static int luma(int argb) { return ((argb >> 16 & 0xff) * 299 + (argb >> 8 & 0xff) * 587 + (argb & 0xff) * 114) / 1000; }

    /** ARGB of the pill, or null when the plugin's own black stays. */
    private Integer fillColour() {
        FluidCloudAccent owner = accent;
        Integer base = null;
        if (followMedia && owner != null) {
            Integer media = owner.maskedMediaColor(owner.nativeCurrentMediaColor(null));
            if (media != null) base = media | 0xff000000;
        }
        if (base == null && fillEnabled) base = fillColor;
        if (base == null) return null;
        return withOpacity(base, fillOpacity);
    }

    /** The colour's own alpha scaled by the opacity percent. */
    static int withOpacity(int argb, float percent) {
        float alpha = (argb >>> 24) * Math.max(0f, Math.min(100f, percent)) / 100f;
        return Math.round(alpha) << 24 | argb & 0x00ffffff;
    }

    /** Back to the plugin's own capsule, and forget every view. The restore runs before the views are forgotten. */
    public void release() {
        if (released) return;
        enabled = false; released = true; revision++;
        Runnable restore = () -> {
            final ArrayList<View> views;
            synchronized (bindings) { views = new ArrayList<>(bindings.keySet()); }
            for (View view : views) if (view != null) { apply(view, Float.NaN); view.invalidate(); }
            for (Map.Entry<View, Binding> entry : new ArrayList<>(bindings.entrySet())) {
                View view = entry.getKey();
                if (view != null) { detachRing(view, entry.getValue()); detachBlur(view, entry.getValue()); }
            }
            textFade.release();
            bindings.clear(); hooks = null;
            cover.restoreAll();
        };
        if (Looper.myLooper() == Looper.getMainLooper()) restore.run(); else main.post(restore);
    }

    private static int color(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) try { return android.graphics.Color.parseColor((String) value); }
        catch (IllegalArgumentException ignored) { }
        return fallback;
    }

    private static float number(Bundle settings, String key, float fallback, float min, float max) {
        Object value = settings == null ? null : settings.get(key);
        float result = value instanceof Number ? ((Number) value).floatValue() : fallback;
        if (!Float.isFinite(result)) result = fallback;
        return Math.max(min, Math.min(max, result));
    }
}
