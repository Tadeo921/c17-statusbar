package dev.puitheme;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import com.oplus.systemui.plugins.seedling.capsule.ui.view.CapsuleView;
import com.oplus.systemui.plugins.shared.template.section.media.K0;
import com.oplus.systemui.plugins.shared.template.section.media.s1;

/** The camera-area capsule: fill, outline and corners follow the settings, the plugin's own values are restored. */
public final class FluidCloudCapsuleCheck {
    private static int checks;
    private static void check(boolean okay, String why) { checks++; if (!okay) throw new AssertionError(why); }
    private static void same(int expected, int actual, String why) {
        checks++; if (expected != actual) throw new AssertionError(why + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual));
    }

    private static String reason(String key, java.util.Map<String, Object> values) {
        String result = SettingsCatalog.unavailableReason(key, values);
        return result == null ? "" : result;
    }

    private static CapsuleView capsule() {
        CapsuleView view = new CapsuleView(new Context());
        view.getResources().getDisplayMetrics().density = 2f;
        return view;
    }
    /** The drawable that draws the pill (ours once a look is wanted, the plugin's own before and after). */
    private static FluidCloudCapsule.PillBackground look(CapsuleView view) {
        return view.getBackground() instanceof FluidCloudCapsule.PillBackground ? (FluidCloudCapsule.PillBackground) view.getBackground() : null;
    }
    private static GradientDrawable outline(CapsuleView view) { return (GradientDrawable) view.getForeground(); }

    private static Bundle on() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FluidCloudCapsule.MASTER, true);
        return bundle;
    }

    private static void defaultsAndCatalog() {
        check(!StatusBarSettings.BOOLEAN_DEFAULTS.get(FluidCloudCapsule.MASTER), "master default off");
        for (String key : new String[]{FluidCloudCapsule.FILL_ENABLED, FluidCloudCapsule.FOLLOW_MEDIA, FluidCloudCapsule.BORDER_ENABLED,
                FluidCloudCapsule.RADIUS_ENABLED})
            check(Boolean.FALSE.equals(StatusBarSettings.BOOLEAN_DEFAULTS.get(key)), key + " default off");
        check(StatusBarSettings.NUMERIC_DEFAULTS.get(FluidCloudCapsule.FILL_OPACITY) == 100f, "opacity default");
        check(StatusBarSettings.NUMERIC_DEFAULTS.get(FluidCloudCapsule.BORDER_WIDTH) == 1.3f, "border default is the plugin's 1.3 dp");
        // The pill is about 30 dp tall: 15 dp and up is the full pill (the plugin's own 20 dp is too), so 15 is the largest that matters.
        check(StatusBarSettings.NUMERIC_DEFAULTS.get(FluidCloudCapsule.RADIUS) == 15f, "radius default is the full pill (15 dp)");
        check(FluidCloudCapsule.MAX_RADIUS == 15f, "a radius above the pill's half height changes nothing, so it is not offered");
        for (String key : new String[]{FluidCloudCapsule.GRADIENT_ENABLED, FluidCloudCapsule.BLUR_ENABLED, FluidCloudCapsule.MARQUEE_ENABLED,
                FluidCloudCapsule.MARQUEE_REVERSE, FluidCloudCapsule.SPECTRUM_ENABLED, FluidCloudCapsule.SPECTRUM_FOLLOW, FluidCloudStyle.LOCK_CAPSULE})
            check(Boolean.FALSE.equals(StatusBarSettings.BOOLEAN_DEFAULTS.get(key)), key + " default off");
        check(FluidCloudCapsule.TOP_BOTTOM.equals(StatusBarSettings.STRING_DEFAULTS.get(FluidCloudCapsule.GRADIENT_MODE)), "gradient runs top to bottom by default");
        check(FluidCloudCapsule.DEFAULT_MARQUEE_PRESET.equals(StatusBarSettings.STRING_DEFAULTS.get(FluidCloudCapsule.MARQUEE_PRESET)), "marquee preset default");
        check(FluidCloudStyle.SOLID.equals(StatusBarSettings.STRING_DEFAULTS.get(FluidCloudStyle.COLOR_STYLE)), "the colour style is solid by default");
        same(0xff000000, StatusBarSettings.COLOR_DEFAULTS.get(FluidCloudCapsule.FILL_COLOR), "fill colour default");
        same(0x40959595, StatusBarSettings.COLOR_DEFAULTS.get(FluidCloudCapsule.BORDER_COLOR), "border colour default");
        check("fluid_cloud_capsule".equals(FeatureOptions.GROUP_BY_KEY.get(FluidCloudCapsule.MASTER)), "master belongs to the capsule group");
        check(FeatureOptions.masterKey("fluid_cloud_capsule").equals(FluidCloudCapsule.MASTER), "the group's master key");
        for (String key : new String[]{FluidCloudCapsule.MASTER, FluidCloudCapsule.FILL_ENABLED, FluidCloudCapsule.FILL_COLOR,
                FluidCloudCapsule.FILL_OPACITY, FluidCloudCapsule.FOLLOW_MEDIA, FluidCloudCapsule.BORDER_ENABLED,
                FluidCloudCapsule.BORDER_COLOR, FluidCloudCapsule.BORDER_WIDTH, FluidCloudCapsule.RADIUS_ENABLED, FluidCloudCapsule.RADIUS,
                FluidCloudCapsule.GRADIENT_ENABLED, FluidCloudCapsule.GRADIENT_MODE, FluidCloudCapsule.GRADIENT_COLOR,
                FluidCloudCapsule.BLUR_ENABLED, FluidCloudCapsule.BLUR_RADIUS, FluidCloudCapsule.MARQUEE_ENABLED,
                FluidCloudCapsule.MARQUEE_PRESET, FluidCloudCapsule.MARQUEE_COLOR_1, FluidCloudCapsule.MARQUEE_COLOR_2,
                FluidCloudCapsule.MARQUEE_COLOR_3, FluidCloudCapsule.MARQUEE_OPACITY, FluidCloudCapsule.MARQUEE_PERIOD,
                FluidCloudCapsule.MARQUEE_REVERSE, FluidCloudCapsule.SPECTRUM_ENABLED, FluidCloudCapsule.SPECTRUM_SENSITIVITY,
                FluidCloudCapsule.BLUR_MASK_COLOR, FluidCloudCapsule.MARQUEE_COLOR_4, FluidCloudCapsule.MARQUEE_COLOR_5,
                FluidCloudCapsule.MARQUEE_COLOR_6, FluidCloudCapsule.MARQUEE_COLOR_COUNT, FluidCloudCapsule.GLOW_ENABLED,
                FluidCloudCapsule.GLOW_DEPTH, FluidCloudCapsule.SPECTRUM_COLOR, FluidCloudCapsule.SPECTRUM_FOLLOW}) {
            SettingsCatalog.Item item = SettingsCatalog.item(key);
            check(item != null && "fluid_cloud_capsule".equals(item.groupId), key + " is on the capsule page");
        }
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        check(reason(FluidCloudCapsule.FILL_ENABLED, values).contains("摄像头区域流体云"), "children wait for the master");
        values.put(FluidCloudCapsule.MASTER, true);
        check(reason(FluidCloudCapsule.FILL_ENABLED, values).isEmpty(), "master on frees the switches");
        check(!reason(FluidCloudCapsule.FILL_COLOR, values).isEmpty(), "colour needs the fill switch");
        check(!reason(FluidCloudCapsule.FILL_OPACITY, values).isEmpty(), "opacity needs fill or follow");
        values.put(FluidCloudCapsule.FOLLOW_MEDIA, true);
        check(reason(FluidCloudCapsule.FILL_OPACITY, values).isEmpty(), "following the media frees the opacity");
        check(!reason(FluidCloudCapsule.BORDER_WIDTH, values).isEmpty(), "border width needs the border switch");
        check(!reason(FluidCloudCapsule.RADIUS, values).isEmpty(), "radius needs the radius switch");
        values.put(FluidCloudCapsule.BORDER_ENABLED, true);
        values.put(FluidCloudCapsule.RADIUS_ENABLED, true);
        check(reason(FluidCloudCapsule.BORDER_WIDTH, values).isEmpty()
                && reason(FluidCloudCapsule.RADIUS, values).isEmpty(), "switches free their values");
        for (String key : Upgrade96.DEFAULTS.keySet()) {
            boolean known = StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key) || StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)
                    || StatusBarSettings.COLOR_DEFAULTS.containsKey(key) || key.endsWith("_custom_alpha");
            check(known, key + " is a registered setting");
        }
        for (String key : Upgrade98.DEFAULTS.keySet()) {
            boolean known = StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key) || StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)
                    || StatusBarSettings.COLOR_DEFAULTS.containsKey(key) || StatusBarSettings.STRING_DEFAULTS.containsKey(key)
                    || key.endsWith("_custom_alpha");
            check(known, key + " (code98) is a registered setting");
        }
        for (String key : Upgrade99.DEFAULTS.keySet()) {
            boolean known = StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key) || StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)
                    || StatusBarSettings.COLOR_DEFAULTS.containsKey(key) || StatusBarSettings.STRING_DEFAULTS.containsKey(key)
                    || key.endsWith("_custom_alpha");
            check(known, key + " (code99) is a registered setting");
        }
        for (String key : Upgrade100.DEFAULTS.keySet()) {
            boolean known = StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key) || StatusBarSettings.NUMERIC_DEFAULTS.containsKey(key)
                    || StatusBarSettings.COLOR_DEFAULTS.containsKey(key) || StatusBarSettings.STRING_DEFAULTS.containsKey(key);
            check(known, key + " (code100) is a registered setting");
        }
        for (String key : Upgrade101.DEFAULTS.keySet())
            check(StatusBarSettings.BOOLEAN_DEFAULTS.containsKey(key), key + " (code101) is a registered setting");
        // code99: the frosted glass takes the fill's place, so the fill options are greyed (with a reason) while it is on.
        values.clear(); values.put(FluidCloudCapsule.MASTER, true); values.put(FluidCloudCapsule.FILL_ENABLED, true);
        values.put(FluidCloudCapsule.GRADIENT_ENABLED, true);
        check(reason(FluidCloudCapsule.FILL_ENABLED, values).isEmpty() && reason(FluidCloudCapsule.GRADIENT_MODE, values).isEmpty(), "without the glass the fill options are free");
        values.put(FluidCloudCapsule.BLUR_ENABLED, true);
        for (String key : new String[]{FluidCloudCapsule.FILL_ENABLED, FluidCloudCapsule.FILL_COLOR, FluidCloudCapsule.FILL_OPACITY, FluidCloudCapsule.FOLLOW_MEDIA,
                FluidCloudCapsule.GRADIENT_ENABLED, FluidCloudCapsule.GRADIENT_MODE, FluidCloudCapsule.GRADIENT_COLOR})
            check(reason(key, values).contains("背景模糊"), key + " is greyed while the glass is on");
        check(reason(FluidCloudCapsule.BLUR_MASK_COLOR, values).isEmpty() && reason(FluidCloudCapsule.BLUR_RADIUS, values).isEmpty(), "the glass's own options are free");
        values.put(FluidCloudCapsule.BLUR_ENABLED, false);
        check(!reason(FluidCloudCapsule.BLUR_MASK_COLOR, values).isEmpty(), "the mask colour waits for the glass");
        // The glow, the colour count and the spectrum colour.
        check(!reason(FluidCloudCapsule.GLOW_ENABLED, values).isEmpty(), "the glow waits for the running light");
        values.put(FluidCloudCapsule.MARQUEE_ENABLED, true);
        check(reason(FluidCloudCapsule.GLOW_ENABLED, values).isEmpty() && !reason(FluidCloudCapsule.GLOW_DEPTH, values).isEmpty(), "the glow depth waits for the glow");
        values.put(FluidCloudCapsule.GLOW_ENABLED, true);
        check(reason(FluidCloudCapsule.GLOW_DEPTH, values).isEmpty(), "the glow switch frees its depth");
        check(!reason(FluidCloudCapsule.MARQUEE_COLOR_COUNT, values).isEmpty(), "the colour count is for the custom preset only");
        values.put(FluidCloudCapsule.MARQUEE_PRESET, "custom");
        check(reason(FluidCloudCapsule.MARQUEE_COLOR_COUNT, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_COLOR_3, values).isEmpty(), "custom: the third colour is in use at the default count");
        check(!reason(FluidCloudCapsule.MARQUEE_COLOR_4, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_COLOR_4, values).contains("3"), "the fourth colour waits for the count");
        values.put(FluidCloudCapsule.MARQUEE_COLOR_COUNT, 5f);
        check(reason(FluidCloudCapsule.MARQUEE_COLOR_4, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_COLOR_5, values).isEmpty()
                && !reason(FluidCloudCapsule.MARQUEE_COLOR_6, values).isEmpty(), "five colours: the sixth stays off");
        check(!reason(FluidCloudCapsule.SPECTRUM_COLOR, values).isEmpty() && !reason(FluidCloudCapsule.SPECTRUM_FOLLOW, values).isEmpty(), "the spectrum colour and following the accent wait for the spectrum");
        values.put(FluidCloudCapsule.SPECTRUM_ENABLED, true);
        check(reason(FluidCloudCapsule.SPECTRUM_COLOR, values).isEmpty() && reason(FluidCloudCapsule.SPECTRUM_FOLLOW, values).isEmpty(), "the spectrum switch frees its colour and the follow switch");
        check(FluidCloudCapsule.MARQUEE_PRESETS.length == FluidCloudCapsule.MARQUEE_PRESET_LABELS.length && FluidCloudCapsule.MARQUEE_PRESETS.length >= 20,
                "twenty or more presets, each with a label");
        check(new java.util.HashSet<>(java.util.Arrays.asList(FluidCloudCapsule.MARQUEE_PRESETS)).size() == FluidCloudCapsule.MARQUEE_PRESETS.length, "preset ids are unique");
        // Dependencies of the new options.
        values.clear(); values.put(FluidCloudCapsule.MASTER, true);
        check(!reason(FluidCloudCapsule.GRADIENT_ENABLED, values).isEmpty(), "a gradient needs a fill to start from");
        values.put(FluidCloudCapsule.FILL_ENABLED, true);
        check(reason(FluidCloudCapsule.GRADIENT_ENABLED, values).isEmpty(), "a custom fill frees the gradient");
        check(!reason(FluidCloudCapsule.GRADIENT_MODE, values).isEmpty() && !reason(FluidCloudCapsule.GRADIENT_COLOR, values).isEmpty(), "gradient values wait for the gradient switch");
        values.put(FluidCloudCapsule.GRADIENT_ENABLED, true);
        check(reason(FluidCloudCapsule.GRADIENT_MODE, values).isEmpty() && reason(FluidCloudCapsule.GRADIENT_COLOR, values).isEmpty(), "the gradient switch frees its values");
        check(!reason(FluidCloudCapsule.BLUR_RADIUS, values).isEmpty(), "blur strength waits for the blur switch");
        values.put(FluidCloudCapsule.BLUR_ENABLED, true);
        check(reason(FluidCloudCapsule.BLUR_RADIUS, values).isEmpty(), "blur switch frees the strength");
        check(!reason(FluidCloudCapsule.MARQUEE_PRESET, values).isEmpty() && !reason(FluidCloudCapsule.MARQUEE_PERIOD, values).isEmpty(), "marquee values wait for the marquee switch");
        check("请先开启自定义边框".equals(reason(FluidCloudCapsule.BORDER_WIDTH, values)), "border width needs the custom border");
        values.put(FluidCloudCapsule.MARQUEE_ENABLED, true);
        check(reason(FluidCloudCapsule.MARQUEE_PRESET, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_OPACITY, values).isEmpty()
                && reason(FluidCloudCapsule.MARQUEE_PERIOD, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_REVERSE, values).isEmpty(), "marquee switch frees its values");
        // code100: the marquee alone does not free the width; it runs at the system's 1.3 dp, and the greyed value shows that.
        check(reason(FluidCloudCapsule.BORDER_WIDTH, values).contains("1.3 dp"), "the marquee alone keeps the system width and says so");
        values.put(FluidCloudCapsule.BORDER_WIDTH, 3f);
        check(Float.valueOf(FluidCloudCapsule.DEFAULT_BORDER_WIDTH).equals(SettingsCatalog.inactiveNumber(FluidCloudCapsule.BORDER_WIDTH, values)),
                "the greyed width shows the width the marquee really uses");
        values.put(FluidCloudCapsule.BORDER_ENABLED, true);
        check(reason(FluidCloudCapsule.BORDER_WIDTH, values).isEmpty() && SettingsCatalog.inactiveNumber(FluidCloudCapsule.BORDER_WIDTH, values) == null,
                "the custom border frees the width for both");
        values.remove(FluidCloudCapsule.BORDER_ENABLED); values.remove(FluidCloudCapsule.BORDER_WIDTH);
        check(!reason(FluidCloudCapsule.BORDER_COLOR, values).isEmpty(), "the plain border colour still needs the plain border");
        check(!reason(FluidCloudCapsule.MARQUEE_COLOR_1, values).isEmpty(), "a preset with its own colours has no colour 1");
        values.put(FluidCloudCapsule.MARQUEE_PRESET, "comet");
        check(reason(FluidCloudCapsule.MARQUEE_COLOR_1, values).isEmpty() && !reason(FluidCloudCapsule.MARQUEE_COLOR_2, values).isEmpty(), "the comet takes colour 1 only");
        values.put(FluidCloudCapsule.MARQUEE_PRESET, "custom");
        check(reason(FluidCloudCapsule.MARQUEE_COLOR_2, values).isEmpty() && reason(FluidCloudCapsule.MARQUEE_COLOR_3, values).isEmpty(), "custom takes all three colours");
        check(!reason(FluidCloudCapsule.SPECTRUM_SENSITIVITY, values).isEmpty(), "sensitivity waits for the spectrum switch");
        values.put(FluidCloudCapsule.SPECTRUM_ENABLED, true);
        check(reason(FluidCloudCapsule.SPECTRUM_SENSITIVITY, values).isEmpty(), "spectrum switch frees the sensitivity");
        // The lock-screen capsule and the colour style live on the lock-screen fluid-cloud page, which is set on its own: no master is needed.
        for (String key : new String[]{FluidCloudStyle.LOCK_CAPSULE, FluidCloudStyle.COLOR_STYLE}) {
            SettingsCatalog.Item item = SettingsCatalog.item(key);
            check(item != null && "lock_awake".equals(item.groupId), key + " is on the lit lock-screen page");
            values.clear();
            check(reason(key, values).isEmpty(), key + " does not wait for the media-accent master");
            values.put(FluidCloudAccent.ENABLED, true);
            check(reason(key, values).isEmpty(), key + " is free with the master on as well");
        }
        // Presets: every id has colours, the lap is closed, custom and comet take the user's colours.
        int[] mine = {0xff111111, 0xff222222, 0xff333333, 0xff444444, 0xff555555, 0xff666666};
        for (String id : FluidCloudCapsule.MARQUEE_PRESETS) {
            int[] lap = FluidCloudCapsule.presetColors(id, mine, 3);
            check(lap.length >= 3 && lap[0] == lap[lap.length - 1] || "comet".equals(id) || lap.length == 3, id + " lap is closed");
            for (int colour : lap) check("comet".equals(id) || (colour >>> 24) == 0xff, id + " colours are opaque");
        }
        check(java.util.Arrays.equals(FluidCloudCapsule.presetColors("custom", mine, 3), new int[]{0xff111111, 0xff222222, 0xff333333, 0xff111111}), "custom lap of three");
        check(java.util.Arrays.equals(FluidCloudCapsule.presetColors("custom", mine, 6),
                new int[]{0xff111111, 0xff222222, 0xff333333, 0xff444444, 0xff555555, 0xff666666, 0xff111111}), "custom lap of six");
        check(FluidCloudCapsule.presetColors("custom", mine, 2).length == 3 && FluidCloudCapsule.presetColors("custom", mine, 99).length == 7
                && FluidCloudCapsule.presetColors("custom", mine, 0).length == 2, "the count is held to what exists");
        check(FluidCloudCapsule.presetColors("comet", mine, 3)[3] == 0xff111111 && (FluidCloudCapsule.presetColors("comet", mine, 3)[0] >>> 24) == 0,
                "the comet fades out behind its head");
        check(FluidCloudCapsule.preset("nope").equals(FluidCloudCapsule.DEFAULT_MARQUEE_PRESET) && FluidCloudCapsule.mode("x").equals(FluidCloudCapsule.TOP_BOTTOM)
                && FluidCloudCapsule.mode("center").equals(FluidCloudCapsule.CENTER), "unknown preset and direction fall back");
    }

    private static void structure() {
        FluidCloudCapsule control = new FluidCloudCapsule(new FluidCloudAccent());
        FluidCloudCapsule.Hooks hooks = control.resolve(FluidCloudCapsuleCheck.class.getClassLoader());
        check(hooks != null && hooks.progress.getName().equals("b"), "the audited structure resolves by shape, not by name");
        check(hooks.onFinishInflate.getName().equals("onFinishInflate") && hooks.draw.getName().equals("onDraw"), "real names for the framework methods");
        check(new FluidCloudCapsule(null).resolve(null) == null, "no class loader, no hooks");
    }

    private static final class Recording extends android.graphics.Canvas {
        final java.util.List<android.graphics.RectF> rects = new java.util.ArrayList<>();
        final java.util.List<Integer> alphas = new java.util.ArrayList<>(), colours = new java.util.ArrayList<>();
        final java.util.List<Float> radii = new java.util.ArrayList<>(), widths = new java.util.ArrayList<>();
        final java.util.List<android.graphics.Shader> shaders = new java.util.ArrayList<>();
        @Override public void drawRoundRect(android.graphics.RectF rect, float rx, float ry, android.graphics.Paint paint) {
            rects.add(new android.graphics.RectF(rect.left, rect.top, rect.right, rect.bottom));
            alphas.add(paint.getAlpha()); colours.add(paint.getColor()); radii.add(rx); widths.add(paint.strokeWidth); shaders.add(paint.shader);
            super.drawRoundRect(rect, rx, ry, paint);
        }
    }
    /** A draw after any fade in progress is over (the fade checks set the clock themselves and draw with {@link #drawAt}). */
    private static Recording draw(android.graphics.drawable.Drawable drawable) { android.os.SystemClock.uptime += 10000L; return drawAt(drawable); }
    private static Recording drawAt(android.graphics.drawable.Drawable drawable) { Recording canvas = new Recording(); drawable.draw(canvas); return canvas; }
    private static int drawnColour(android.graphics.drawable.Drawable pill) { return drawAt(pill).colours.get(0); }

    private static void drawing() {
        FluidCloudAccent accent = new FluidCloudAccent();
        FluidCloudCapsule control = new FluidCloudCapsule(accent);
        CapsuleView view = capsule();
        GradientDrawable plugin = (GradientDrawable) view.getBackground();
        control.resolve(FluidCloudCapsuleCheck.class.getClassLoader());
        // Off: the plugin's own capsule, untouched.
        control.changed(view, Float.NaN);
        check(view.getBackground() == plugin && look(view) == null, "off keeps the plugin's own pill");
        same(0xff000000, plugin.getColor().getDefaultColor(), "off keeps the black pill");
        same(3, outline(view).strokeWidthForCheck(), "off keeps the plugin's outline");
        check(plugin.getCornerRadius() == 40f && outline(view).getCornerRadius() == 40f, "off keeps the plugin's corners");

        // A custom fill with the colour's own alpha off: only the opacity setting makes it translucent. The plugin's drawable is
        // never edited: a look is a drawable of ours in its place.
        Bundle bundle = on();
        bundle.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0xff336699);
        bundle.putFloat(FluidCloudCapsule.FILL_OPACITY, 50f);
        control.configure(bundle);
        control.changed(view, Float.NaN);
        FluidCloudCapsule.PillBackground pill = look(view);
        check(pill != null && pill.nativeDrawable == plugin && view.getBackground() == pill, "a custom fill takes over the background");
        same(0x80336699, pill.fillColor(), "custom fill with 50 % opacity");
        check(pill.mode() == FluidCloudCapsule.PillBackground.SOLID, "a solid fill");
        same(0xff000000, plugin.getColor().getDefaultColor(), "the plugin's own drawable keeps its colour");
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0x40336699);
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.FILL_COLOR), false);
        control.configure(bundle);
        control.beforeDraw(view);
        same(0x80336699, look(view).fillColor(), "a colour whose alpha is not custom is opaque; the next draw picks the change up");
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.FILL_COLOR), true);
        bundle.putFloat(FluidCloudCapsule.FILL_OPACITY, 100f);
        control.configure(bundle);
        control.beforeDraw(view);
        same(0x40336699, look(view).fillColor(), "a custom-alpha colour keeps its own alpha at full opacity");
        check(FluidCloudCapsule.withOpacity(0xff102030, 0f) == 0x00102030 && FluidCloudCapsule.withOpacity(0xff102030, 250f) == 0xff102030,
                "opacity is clamped to 0..100");

        // The border and the corners: one ring in the overlay on exact corners; the plugin rewrites its stroke on every step and the
        // ring stays.
        bundle.putBoolean(FluidCloudCapsule.BORDER_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.BORDER_COLOR, 0xffff0000);
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.BORDER_COLOR), false);
        bundle.putFloat(FluidCloudCapsule.BORDER_WIDTH, 2f);
        bundle.putBoolean(FluidCloudCapsule.RADIUS_ENABLED, true);
        bundle.putFloat(FluidCloudCapsule.RADIUS, 12f);
        control.configure(bundle);
        control.beforeDraw(view);
        pill = look(view);
        pill.setBounds(0, 0, 200, 60); control.beforeDraw(view); // a full capsule: the corner settings apply
        check(pill.cornerPx() == 24f, "12 dp at density 2 on the pill itself");
        FluidCloudCapsule.MarqueeRing ring = ring(view);
        check(ring != null && !ring.isAnimated() && ring.isShown(), "a plain border is one fixed ring");
        same(0, outline(view).strokeWidthForCheck(), "the plugin's own outline is taken off while the ring draws");
        Recording border = draw(ring);
        check(border.rects.size() == 1 && border.colours.get(0) == 0xffff0000 && border.widths.get(0) == 4f, "2 dp border at density 2, in the border colour");
        check(border.rects.get(0).left == 2f && border.rects.get(0).right == 198f && border.radii.get(0) == 22f, "inset by half the stroke, on the custom corners");
        view.b(0.5f);
        same(0, outline(view).strokeWidthForCheck(), "the plugin's expansion step rewrote the stroke");
        control.changed(view, 0.5f);
        same(0, outline(view).strokeWidthForCheck(), "the plugin's stroke stays off while expanded");
        check(draw(ring(view)).rects.size() == 1, "and the ring still draws");
        view.b(0f);
        control.changed(view, 0f);
        same(0, outline(view).strokeWidthForCheck(), "and while collapsed");

        // Following the media: its colour (with the shared mask) takes the fill; the opacity setting still applies.
        kotlinx.coroutines.flow.p colors = new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffd4798e));
        android.view.ViewGroup root = new android.view.ViewGroup(null);
        s1 owner = new s1(null, new K0(colors), root, null);
        accent.mediaChanged(owner);
        bundle.putBoolean(FluidCloudCapsule.FOLLOW_MEDIA, true);
        bundle.putFloat(FluidCloudCapsule.FILL_OPACITY, 50f);
        control.configure(bundle);
        control.beforeDraw(view);
        same(0x80d4798e, look(view).fillColor(), "the media colour wins over the custom fill");
        long before = accent.colorRevision();
        accent.mediaChanged(owner);
        check(accent.colorRevision() >= before, "revision never goes back");
        bundle.putBoolean(FluidCloudCapsule.FOLLOW_MEDIA, false);
        control.configure(bundle);
        control.beforeDraw(view);
        same(0x20336699, look(view).fillColor(), "without following, the custom fill (alpha 0x40 at 50 % opacity) returns");

        // Everything off again: the plugin's own drawable is back in place, and its outline for the step it is in.
        view.b(0.5f);
        control.changed(view, 0.5f);
        control.configure(new Bundle());
        control.changed(view, Float.NaN);
        check(view.getBackground() == plugin && look(view) == null, "the plugin's own pill is back");
        same(0xff000000, plugin.getColor().getDefaultColor(), "native fill intact");
        check(plugin.getCornerRadius() == 40f && outline(view).getCornerRadius() == 40f, "native corners intact");
        check(ring(view) == null, "the ring is gone");
        same(0, outline(view).strokeWidthForCheck(), "native outline restored for the expanded step");
        same(0x40959595, outline(view).strokeColorForCheck(), "native outline colour restored");
        view.b(0f);
        control.changed(view, 0f);
        same(3, outline(view).strokeWidthForCheck(), "native outline while collapsed");

        // Safe mode counts as off.
        Bundle safe = on();
        safe.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        safe.putInt(FluidCloudCapsule.FILL_COLOR, 0xff00ff00);
        control.configure(safe);
        control.changed(view, Float.NaN);
        same(0xff00ff00, look(view).fillColor(), "capsule on");
        safe.putBoolean(StatusBarSettings.SAFE_MODE, true);
        control.configure(safe);
        check(!control.active(), "safe mode switches the capsule off");
        control.changed(view, Float.NaN);
        check(view.getBackground() == plugin, "safe mode restores the plugin's capsule");

        // A view that is not the audited drawable pair is left alone; releasing restores and stops.
        control.configure(safe = on());
        safe.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        safe.putInt(FluidCloudCapsule.FILL_COLOR, 0xffaa0000);
        control.configure(safe);
        control.changed(view, Float.NaN);
        same(0xffaa0000, look(view).fillColor(), "on again");
        CapsuleView odd = capsule();
        odd.setBackground(new android.graphics.drawable.ColorDrawable(0xff123456));
        control.changed(odd, Float.NaN);
        check(odd.getBackground() instanceof android.graphics.drawable.ColorDrawable, "an unexpected drawable is not replaced");
        control.release();
        check(view.getBackground() == plugin, "release restores the plugin's capsule");
        check(!control.active(), "released is inactive");
        control.configure(safe);
        control.changed(view, Float.NaN);
        check(view.getBackground() == plugin, "a released controller never colours again");
    }

    private static FluidCloudCapsule.MarqueeRing ring(CapsuleView view) {
        for (android.graphics.drawable.Drawable drawable : view.getOverlay().drawables)
            if (drawable instanceof FluidCloudCapsule.MarqueeRing) return (FluidCloudCapsule.MarqueeRing) drawable;
        return null;
    }
    /** The platform blur region the pill carries (it is drawn by the pill, first, so its mask colour lies under the capsule's content). */
    private static FluidCloudCapsule.BlurHolder blur(CapsuleView view) { return look(view) == null ? null : look(view).blurHolder(); }

    /** The gradient (three directions, and nothing left behind), the running light with its glow, the frosted glass with its mask
     * colour, and the corner range. */
    private static void newLook() {
        FluidCloudCapsule control = new FluidCloudCapsule(new FluidCloudAccent());
        control.resolve(FluidCloudCapsuleCheck.class.getClassLoader());
        CapsuleView view = capsule();
        GradientDrawable plugin = (GradientDrawable) view.getBackground();
        plugin.setBounds(0, 0, 200, 60);
        Bundle bundle = on();
        bundle.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0xff336699);
        bundle.putFloat(FluidCloudCapsule.FILL_OPACITY, 100f);
        bundle.putBoolean(FluidCloudCapsule.GRADIENT_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.GRADIENT_COLOR, 0xff102030);
        control.configure(bundle);
        control.changed(view, Float.NaN);
        FluidCloudCapsule.PillBackground pill = look(view);
        check(pill.getBounds().width() == 200, "the pill keeps the bounds the plugin had given the background");
        check(pill.mode() == FluidCloudCapsule.PillBackground.TOP_DOWN && pill.fillColor() == 0xff336699 && pill.gradientEnd() == 0xff102030, "top to bottom by default");
        Recording top = draw(pill);
        check(top.shaders.get(0) instanceof android.graphics.LinearGradient, "a linear gradient");
        android.graphics.LinearGradient linear = (android.graphics.LinearGradient) top.shaders.get(0);
        check(linear.colors[0] == 0xff336699 && linear.colors[1] == 0xff102030 && linear.top == 0f && linear.bottom == 1f, "the fill colour first, then the gradient colour, in unit coordinates");
        float[] matrix = new float[9];
        linear.localMatrix.getValues(matrix);
        check(matrix[0] == 200f && matrix[4] == 60f && matrix[2] == 0f && matrix[5] == 0f, "scaled to the pill's bounds by a matrix");
        check(top.rects.get(0).width() == 200f && top.radii.get(0) == 30f, "a rounded rectangle over the bounds; the radius is the plugin's, held to half the height");
        pill.setBounds(10, 5, 410, 125);
        Recording grown = draw(pill);
        linear.localMatrix.getValues(matrix);
        check(grown.shaders.get(0) == top.shaders.get(0) && matrix[0] == 400f && matrix[4] == 120f && matrix[2] == 10f && matrix[5] == 5f,
                "a growing pill reuses its shader and only changes the matrix");
        pill.setBounds(0, 0, 200, 60);
        bundle.putString(FluidCloudCapsule.GRADIENT_MODE, FluidCloudCapsule.BOTTOM_TOP);
        control.configure(bundle); control.beforeDraw(view);
        check(pill.mode() == FluidCloudCapsule.PillBackground.BOTTOM_UP, "bottom to top");
        android.graphics.LinearGradient up = (android.graphics.LinearGradient) draw(pill).shaders.get(0);
        check(up.top == 1f && up.bottom == 0f && up.colors[0] == 0xff336699, "the fill colour at the bottom");
        bundle.putString(FluidCloudCapsule.GRADIENT_MODE, FluidCloudCapsule.CENTER);
        control.configure(bundle); control.beforeDraw(view);
        check(pill.mode() == FluidCloudCapsule.PillBackground.RADIAL, "from the middle outwards");
        android.graphics.RadialGradient radial = (android.graphics.RadialGradient) draw(pill).shaders.get(0);
        check(radial.cx == .5f && radial.cy == .5f && radial.radius == .5f && radial.color0 == 0xff336699 && radial.color1 == 0xff102030,
                "an ellipse that reaches the four sides of the pill");
        bundle.putFloat(FluidCloudCapsule.FILL_OPACITY, 50f);
        control.configure(bundle); control.beforeDraw(view);
        check(pill.fillColor() == 0x80336699 && pill.gradientEnd() == 0x80102030, "the opacity applies to both colours of the gradient");
        // The regression of code98: after the gradient is switched off nothing of it may be left on the pill.
        bundle.putBoolean(FluidCloudCapsule.GRADIENT_ENABLED, false);
        control.configure(bundle); control.beforeDraw(view);
        Recording solid = draw(pill);
        check(pill.mode() == FluidCloudCapsule.PillBackground.SOLID && solid.shaders.get(0) == null && solid.colours.get(0) == 0x80336699 + 0
                || pill.mode() == FluidCloudCapsule.PillBackground.SOLID && solid.shaders.get(0) == null,
                "gradient off: a plain solid paint, no shader left behind");
        check(solid.colours.get(0) == 0x80336699 && solid.alphas.get(0) == 0x80, "in the fill colour with its alpha");
        bundle.putBoolean(FluidCloudCapsule.GRADIENT_ENABLED, true);
        control.configure(bundle); control.beforeDraw(view);
        check(draw(pill).shaders.get(0) != null, "and the gradient comes back");
        control.configure(new Bundle()); control.changed(view, Float.NaN);
        check(view.getBackground() == plugin && plugin.getColor().getDefaultColor() == 0xff000000, "everything off: the plugin's black pill again");

        // Corners alone: the pill is ours, in the plugin's colour, on the custom corners.
        Bundle round = on();
        round.putBoolean(FluidCloudCapsule.RADIUS_ENABLED, true);
        round.putFloat(FluidCloudCapsule.RADIUS, 40f);
        control.configure(round);
        CapsuleView corners = capsule();
        control.changed(corners, Float.NaN);
        FluidCloudCapsule.PillBackground rounded = look(corners);
        // code100 (second fix): what counts is the pill's real shape. Shrunk onto the punch-hole camera it is as wide as tall; the
        // ordinary music capsule (cover and bars) is a full capsule and takes the corner setting.
        rounded.setBounds(0, 0, 60, 60); control.beforeDraw(corners);
        float nativeCorners = rounded.cornerPx();
        check(rounded.fillColor() == 0xff000000 && nativeCorners == 30f, "shrunk onto the camera: the plugin's own round pill");
        rounded.setBounds(0, 0, 200, 60); control.beforeDraw(corners);
        check(rounded.cornerPx() == 30f, "a full capsule: a saved 40 dp is held to 15 dp, which is the full pill at this height");
        round.putFloat(FluidCloudCapsule.RADIUS, 4f);
        control.configure(round); control.beforeDraw(corners);
        check(rounded.cornerPx() == 8f && draw(rounded).radii.get(0) == 8f, "a full capsule: 4 dp at density 2, squarer ends");
        android.graphics.Outline outline = new android.graphics.Outline();
        rounded.getOutline(outline);
        check(!outline.empty && outline.radius == 8f, "the pill describes its outline for shadows and clipping");
        control.changed(corners, 0.5f);
        check(!ring(corners).isShown() && draw(ring(corners)).rects.isEmpty(), "the plugin's collapsed outline is hidden while the card expands, like the plugin's own");
        check(rounded.cornerPx() == 8f, "and the card expansion does not change the corners");
        control.changed(corners, 0f);
        check(ring(corners).isShown(), "back when the card collapses");
        rounded.setBounds(0, 0, 98, 60); control.beforeDraw(corners);
        float halfWay = rounded.cornerPx();
        check(halfWay > 8f && halfWay < 30f, "half way between the camera form and the capsule the corners are between the two, no jump (" + halfWay + ")");
        float previous = 30f;
        for (int width = 60; width <= 200; width += 7) {
            rounded.setBounds(0, 0, width, 60); control.beforeDraw(corners);
            check(rounded.cornerPx() <= previous + 1e-3f, "the corners only tighten as it widens (" + width + ")");
            previous = rounded.cornerPx();
        }
        rounded.setBounds(0, 0, 72, 60); control.beforeDraw(corners);
        check(rounded.cornerPx() == nativeCorners, "still the camera form just above round");
        rounded.setBounds(0, 0, 60, 60); control.beforeDraw(corners);
        check(rounded.cornerPx() == nativeCorners, "shrunk again: the plugin's own shape, whatever the setting");
        FluidCloudCapsule.MarqueeRing native_ = ring(corners);
        check(native_ != null && !native_.isAnimated() && native_.isShown(), "the plugin's own collapsed outline is there");
        Recording nativeOutline = draw(native_);
        check(nativeOutline.colours.get(0) == 0x40959595 && nativeOutline.widths.get(0) == 3f && nativeOutline.radii.get(0) == nativeCorners - 1.5f,
                "in its colour and width, on the plugin's own corners (" + nativeOutline.radii.get(0) + ")");
        check(FluidCloudCapsule.shapeWeight(60f, 60f) == 0f && FluidCloudCapsule.shapeWeight(0f, 0f) == 0f && FluidCloudCapsule.shapeWeight(Float.NaN, 60f) == 0f
                && FluidCloudCapsule.shapeWeight(120f, 60f) == 1f && FluidCloudCapsule.shapeWeight(400f, 60f) == 1f,
                "the weight: none on the camera, full for a capsule");
        rounded.setBounds(0, 0, 200, 60); control.beforeDraw(corners);

        // Running light.
        Bundle light = on();
        light.putBoolean(FluidCloudCapsule.MARQUEE_ENABLED, true);
        light.putFloat(FluidCloudCapsule.BORDER_WIDTH, 2f);
        light.putFloat(FluidCloudCapsule.MARQUEE_PERIOD, 4f);
        light.putFloat(FluidCloudCapsule.MARQUEE_OPACITY, 50f);
        control.configure(light);
        control.changed(view, Float.NaN);
        view.getBackground().setBounds(10, 0, 210, 60);
        FluidCloudCapsule.MarqueeRing running = ring(view);
        check(running != null && running.isAnimated() && running.glow() == 0f, "the running light is added to the capsule");
        same(0, outline(view).strokeWidthForCheck(), "the plugin's thin outline is taken off while the light runs");
        // code100: the marquee alone runs at the system's width (1.3 dp); the width setting is the custom border's.
        Recording systemWidth = draw(running);
        // Whole pixels, as the custom width: 1.3 dp at density 2 is 3 px.
        float thin = Math.max(1, Math.round(FluidCloudCapsule.DEFAULT_BORDER_WIDTH * 2f));
        check(systemWidth.rects.size() == 1 && Math.abs(systemWidth.widths.get(0) - thin) < 1e-3f
                && Math.abs(systemWidth.rects.get(0).left - (10f + thin / 2f)) < 1e-3f,
                "without the custom border the light is 1.3 dp wide, whatever the width setting says ("
                        + (systemWidth.widths.isEmpty() ? "nothing" : systemWidth.widths.get(0) + " px at " + systemWidth.rects.get(0).left) + ")");
        light.putBoolean(FluidCloudCapsule.BORDER_ENABLED, true);
        control.configure(light); control.beforeDraw(view);
        check(ring(view) == running, "the same ring");
        Recording lap = draw(running);
        check(lap.rects.size() == 1 && lap.rects.get(0).left == 12f && lap.rects.get(0).right == 208f && lap.rects.get(0).top == 2f && lap.rects.get(0).bottom == 58f,
                "with the custom border: a ring of its width, inset by half of it, over the pill's current bounds");
        check(lap.alphas.get(0) == 128 && lap.shaders.get(0) instanceof android.graphics.SweepGradient, "50 % opacity, a sweep of colour");
        check(((android.graphics.SweepGradient) lap.shaders.get(0)).colors.length == 7, "the rainbow preset");
        check(view.lastDelayedInvalidateForCheck == 32L, "about thirty frames a second");
        check(running.angle(1000L) == 90f && running.angle(4000L) == 0f && running.angle(5000L) == 90f, "one lap in four seconds");
        light.putBoolean(FluidCloudCapsule.MARQUEE_REVERSE, true);
        control.configure(light); control.beforeDraw(view);
        check(ring(view) == running && running.angle(1000L) == -90f, "the same ring runs the other way");
        // The inward glow (code100): one fill with a runtime shader whose alpha falls off continuously with the distance from the edge,
        // in the running colours. No steps, one draw per frame.
        light.putBoolean(FluidCloudCapsule.GLOW_ENABLED, true);
        light.putFloat(FluidCloudCapsule.GLOW_DEPTH, 8f);
        control.configure(light); control.beforeDraw(view);
        check(running.glow() == 16f, "8 dp at density 2");
        Recording smooth = draw(running);
        check(smooth.rects.size() == 2 && smooth.shaders.get(1) instanceof android.graphics.RuntimeShader, "the ring and one shader fill");
        android.graphics.RuntimeShader glowShader = (android.graphics.RuntimeShader) smooth.shaders.get(1);
        check(glowShader.source.equals(FluidCloudCapsule.MarqueeRing.GLOW_SHADER) && glowShader.source.contains("(1.0-t)*(1.0-t)"),
                "a continuous falloff, not rings");
        check(smooth.rects.get(1).left == 14f && smooth.rects.get(1).top == 4f && smooth.rects.get(1).right == 206f && smooth.rects.get(1).bottom == 56f,
                "filling the inside of the ring");
        check(java.util.Arrays.equals(glowShader.floats.get("box"), new float[]{10f, 0f, 210f, 60f}) && glowShader.floats.get("start")[0] == 4f
                && glowShader.floats.get("depth")[0] == 16f, "measured from the pill's edge, starting at the ring, over the depth");
        check(Math.abs(glowShader.floats.get("strength")[0] - 128f / 255f * FluidCloudCapsule.MarqueeRing.GLOW_STRENGTH) < 1e-4f,
                "as strong as the ring's opacity allows");
        check(glowShader.inputs.get("sweep") == smooth.shaders.get(0), "in the same running colours");
        int uploads = glowShader.uploadCount;
        Recording again = draw(running);
        check(again.shaders.get(1) == glowShader && glowShader.uploadCount > uploads && glowShader.inputs.get("sweep") == again.shaders.get(0),
                "one shader for every frame, given the turned sweep each time");
        // Without runtime shaders (before Android 13) the fallback: many thin overlapping rings, further in and fainter.
        android.os.Build.VERSION.SDK_INT = 32;
        Recording glow = draw(running);
        android.os.Build.VERSION.SDK_INT = 35;
        int steps = FluidCloudCapsule.MarqueeRing.glowSteps(16f);
        check(steps >= 8 && steps <= 64 && FluidCloudCapsule.MarqueeRing.glowSteps(0.1f) == 4 && FluidCloudCapsule.MarqueeRing.glowSteps(4000f) == 64, "a dense run of rings, within bounds");
        check(glow.rects.size() > 1 + steps / 2 && glow.rects.size() <= 1 + steps, "the ring and a dense run of glow rings (" + glow.rects.size() + ")");
        boolean fading = true, inward = true, overlapping = true;
        int biggestDrop = 0;
        for (int i = 2; i < glow.alphas.size(); i++) {
            fading &= glow.alphas.get(i) <= glow.alphas.get(i - 1);
            inward &= glow.rects.get(i).left > glow.rects.get(i - 1).left;
            biggestDrop = Math.max(biggestDrop, glow.alphas.get(i - 1) - glow.alphas.get(i));
            overlapping &= glow.widths.get(i) > glow.rects.get(i).left - glow.rects.get(i - 1).left;
        }
        check(fading && inward && glow.alphas.get(1) < glow.alphas.get(0), "each glow ring is further in and no stronger than the one before, all fainter than the ring");
        check(biggestDrop * 100 <= glow.alphas.get(0) * 12, "no ring is more than an eighth fainter than the one before: a smooth fade, not stripes (" + biggestDrop + " of " + glow.alphas.get(0) + ")");
        check(overlapping, "each ring is wider than the step to the next: no gap between two");
        check(glow.alphas.get(glow.alphas.size() - 1) * 10 < glow.alphas.get(1), "and it has faded out toward the inside");
        check(glow.shaders.get(3) == glow.shaders.get(0), "in the same running colours");
        light.putBoolean(FluidCloudCapsule.GLOW_ENABLED, false);
        control.configure(light); control.beforeDraw(view);
        check(draw(running).rects.size() == 1, "no glow when it is off");
        // Presets and custom colours with a count.
        light.putString(FluidCloudCapsule.MARQUEE_PRESET, "custom");
        light.putFloat(FluidCloudCapsule.MARQUEE_COLOR_COUNT, 5f);
        light.putInt(FluidCloudCapsule.MARQUEE_COLOR_1, 0xff112233); light.putInt(FluidCloudCapsule.MARQUEE_COLOR_2, 0xff445566);
        light.putInt(FluidCloudCapsule.MARQUEE_COLOR_3, 0xff778899); light.putInt(FluidCloudCapsule.MARQUEE_COLOR_4, 0xffaabbcc);
        light.putInt(FluidCloudCapsule.MARQUEE_COLOR_5, 0xffddeeff); light.putInt(FluidCloudCapsule.MARQUEE_COLOR_6, 0xff010203);
        control.configure(light); control.beforeDraw(view);
        check(java.util.Arrays.equals(((android.graphics.SweepGradient) draw(running).shaders.get(0)).colors,
                new int[]{0xff112233, 0xff445566, 0xff778899, 0xffaabbcc, 0xffddeeff, 0xff112233}), "five custom colours");
        light.putString(FluidCloudCapsule.MARQUEE_PRESET, "galaxy");
        control.configure(light); control.beforeDraw(view);
        check(((android.graphics.SweepGradient) draw(running).shaders.get(0)).colors.length == 5, "any of the other presets");
        // The custom border was only on for the width; with both off nothing is left on the capsule.
        light.putBoolean(FluidCloudCapsule.MARQUEE_ENABLED, false); light.putBoolean(FluidCloudCapsule.BORDER_ENABLED, false);
        control.configure(light); control.beforeDraw(view);
        check(ring(view) == null, "the running light is taken off again");
        draw(running);
        same(3, outline(view).strokeWidthForCheck(), "the plugin's outline is back");

        // Frosted glass: the pill is transparent, the platform's blur region takes its place, tinted by the mask colour; the fill
        // options no longer apply.
        Bundle glass = on();
        glass.putBoolean(FluidCloudCapsule.BLUR_ENABLED, true);
        glass.putFloat(FluidCloudCapsule.BLUR_RADIUS, 10f);
        glass.putInt(FluidCloudCapsule.BLUR_MASK_COLOR, 0x80123456);
        glass.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        glass.putInt(FluidCloudCapsule.FILL_COLOR, 0xff00ff00);
        glass.putBoolean(FluidCloudCapsule.GRADIENT_ENABLED, true);
        control.configure(glass);
        control.changed(view, Float.NaN);
        FluidCloudCapsule.BlurHolder holder = blur(view);
        check(holder != null, "the pill carries the blur region");
        for (android.graphics.drawable.Drawable overlay : view.getOverlay().drawables)
            check(overlay instanceof FluidCloudCapsule.MarqueeRing, "nothing but rings in the overlay: it is drawn after the children and would put the mask colour over the text");
        android.view.ViewRootImpl.BackgroundBlurDrawable region = view.nativeRootForCheck.lastBlur;
        check(region != null && region.blurRadius == 20 && region.maskColor == 0x80123456, "blur radius in pixels (density 2) and the mask colour");
        check(look(view) != null && look(view).glass() && draw(look(view)).rects.isEmpty(), "the pill itself is transparent: the glass replaces the fill and its gradient");
        look(view).setBounds(5, 0, 205, 60);
        int drawsBefore = region.draws;
        draw(look(view));
        check(region.draws == drawsBefore + 1 && region.getBounds().equals(look(view).getBounds()) && region.cornerRadius == 30f, "the region follows the pill's bounds and its corners when the pill is drawn");
        glass.putBoolean(FluidCloudCapsule.RADIUS_ENABLED, true);
        glass.putFloat(FluidCloudCapsule.RADIUS, 6f);
        glass.putInt(FluidCloudCapsule.BLUR_MASK_COLOR, 0x40abcdef);
        control.configure(glass); control.beforeDraw(view);
        draw(look(view));
        check(region.cornerRadius == 12f && region.maskColor == 0x40abcdef, "a full capsule: the glass takes the custom corners and a changed mask colour");
        look(view).setBounds(5, 0, 65, 60); control.beforeDraw(view);
        draw(look(view));
        check(region.cornerRadius == 30f, "shrunk onto the camera: the glass keeps the plugin's round form");
        look(view).setBounds(5, 0, 205, 60); control.beforeDraw(view);
        glass.putBoolean(FluidCloudCapsule.BLUR_ENABLED, false);
        control.configure(glass); control.beforeDraw(view);
        check(blur(view) == null && region.blurRadius == 0, "blur off: the region is taken off the pill and emptied");
        check(look(view) != null && !look(view).glass() && look(view).mode() == FluidCloudCapsule.PillBackground.TOP_DOWN, "and the fill options apply again");
        // A system without the window blur: the fill options stay in force and nothing fails.
        CapsuleView plain = capsule();
        plain.nativeRootForCheck = null;
        glass.putBoolean(FluidCloudCapsule.BLUR_ENABLED, true);
        control.configure(glass);
        control.changed(plain, Float.NaN);
        check(blur(plain) == null && look(plain) != null && !look(plain).glass() && look(plain).fillColor() == 0xff00ff00, "no window blur: the fill shows instead");
        control.release();
        check(ring(view) == null && blur(view) == null && view.getBackground() == plugin && region.blurRadius == 0, "release takes the light, the glass and the pill away");
    }

    /** The blend and the fade of the pill on its own: from what is on the screen to a new colour of the same kind, never restarted by the
     * same colour asked for again. */
    private static void fadeMaths() {
        check(FluidCloudCapsule.PillBackground.blend(0xff102030, 0xffa0b0c0, 0f) == 0xff102030 && FluidCloudCapsule.PillBackground.blend(0xff102030, 0xffa0b0c0, 1f) == 0xffa0b0c0
                && FluidCloudCapsule.PillBackground.blend(0xff102030, 0xffa0b0c0, .5f) == 0xff586878, "blending runs from one colour to the other, channel by channel");
        check(FluidCloudCapsule.PillBackground.blend(0x00000000, 0xff000000, .5f) == 0x80000000, "the alpha too");
        long fade = FluidCloudCapsule.PillBackground.FADE_MS;
        android.os.SystemClock.uptime = 1000L;
        FluidCloudCapsule.PillBackground pill = new FluidCloudCapsule.PillBackground(new GradientDrawable());
        pill.setBounds(0, 0, 200, 60);
        pill.look(false, 0xff102030, null, 0, Float.NaN, 8f);
        same(0xff102030, drawnColour(pill), "the first look is shown as it is: there is nothing to fade from");
        pill.look(false, 0xff102030, null, 0, Float.NaN, 8f);
        pill.look(false, 0xff102030, null, 0x40ffffff, Float.NaN, 8f);
        same(0xff102030, drawnColour(pill), "the same colour again changes nothing (the plugin asks at every step of its expansion), and the end colour of a gradient is no business of a solid fill");
        check(!pill.holds(0xff102030), "a pill in the plugin's own colour holds nothing back");
        pill.look(false, 0xffa0b0c0, null, 0, Float.NaN, 8f);
        same(0xffa0b0c0, pill.fillColor(), "the target is the new colour");
        same(0xff102030, drawnColour(pill), "but at that instant the old colour is still on the screen");
        check(pill.holds(0xff102030), "and the pill is still handing over");
        android.os.SystemClock.uptime = 1000L + fade / 4;
        check((drawnColour(pill) >> 16 & 0xff) < 0x10 + 0x90 / 4, "it eases in: a quarter of the way in time is less than a quarter of the way in colour");
        android.os.SystemClock.uptime = 1000L + fade / 2;
        same(0xff586878, drawnColour(pill), "half way: half way between them");
        pill.look(false, 0xffa0b0c0, null, 0, Float.NaN, 8f);
        same(0xff586878, drawnColour(pill), "asking for the colour it is fading to changes nothing");
        pill.look(false, 0xffffffff, null, 0, Float.NaN, 8f);
        same(0xff586878, drawnColour(pill), "a colour that changes again mid-way starts from what is on the screen: no jump");
        android.os.SystemClock.uptime = 1000L + fade / 2 + fade;
        same(0xffffffff, drawnColour(pill), "and arrives");
        check(pill.holds(0xff102030) && !pill.holds(0xffffffff), "settled on the new colour");
        // A different kind of fill takes the place of the old one at once: the colours of two kinds do not blend.
        pill.look(false, 0xff000000, FluidCloudCapsule.TOP_BOTTOM, 0xffffffff, Float.NaN, 8f);
        check(drawAt(pill).shaders.get(0) != null, "a gradient replaces a solid fill at once");
        pill.look(false, 0xff202020, FluidCloudCapsule.TOP_BOTTOM, 0xffffffff, Float.NaN, 8f);
        android.graphics.LinearGradient start = (android.graphics.LinearGradient) drawAt(pill).shaders.get(0);
        check(start.colors[0] == 0xff000000 && start.colors[1] == 0xffffffff, "a gradient of the same direction fades from its old colours");
        android.os.SystemClock.uptime += fade + 1L;
        android.graphics.LinearGradient end = (android.graphics.LinearGradient) drawAt(pill).shaders.get(0);
        check(end.colors[0] == 0xff202020 && end.colors[1] == 0xffffffff, "to the new ones");
        pill.look(true, 0, null, 0, Float.NaN, 8f);
        check(drawAt(pill).rects.isEmpty() && !pill.holds(0xff000000), "the glass does not fade and holds nothing");
    }

    /** The scheduled clean-up of the capsule control (what the main handler was told to run later). */
    private static Runnable scheduled(FluidCloudCapsule control) throws Exception {
        java.lang.reflect.Field field = FluidCloudCapsule.class.getDeclaredField("main");
        field.setAccessible(true);
        return ((android.os.Handler) field.get(control)).delayed;
    }

    /** The media goes: the colour that followed it hands over smoothly, to the chosen colour or to the plugin's own one. */
    private static void handOver() throws Exception {
        android.os.SystemClock.uptime = 50000L;
        long fade = FluidCloudCapsule.PillBackground.FADE_MS;
        FluidCloudAccent accent = new FluidCloudAccent();
        FluidCloudCapsule control = new FluidCloudCapsule(accent);
        control.resolve(FluidCloudCapsuleCheck.class.getClassLoader());
        kotlinx.coroutines.flow.p colors = new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff2060c0));
        android.view.ViewGroup root = new android.view.ViewGroup(null);
        s1 owner = new s1(null, new K0(colors), root, null);
        accent.mediaChanged(owner);

        // A chosen fill that the media colour replaces while it plays.
        Bundle bundle = on();
        bundle.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0xffc02020);
        bundle.putBoolean(FluidCloudCapsule.FOLLOW_MEDIA, true);
        control.configure(bundle);
        CapsuleView view = capsule();
        control.changed(view, Float.NaN);
        FluidCloudCapsule.PillBackground pill = look(view);
        pill.setBounds(0, 0, 200, 60);
        same(0xff2060c0, pill.fillColor(), "playing: the media colour");
        same(0xff2060c0, drawnColour(pill), "and that is what is drawn");
        accent.mediaDisposed(owner);
        control.beforeDraw(view);
        same(0xffc02020, pill.fillColor(), "the media is gone: the chosen colour is the target");
        same(0xff2060c0, drawnColour(pill), "but at that instant the screen still shows the media colour, not the chosen one at once");
        android.os.SystemClock.uptime += fade / 2;
        same(0xff704070, drawnColour(pill), "half way through it is half way between the two");
        android.os.SystemClock.uptime += fade;
        same(0xffc02020, drawnColour(pill), "and the chosen colour when it is over");
        // The media comes back in the middle of a hand-over: the colour goes on from where it is.
        accent.mediaChanged(owner);
        control.beforeDraw(view);
        same(0xff2060c0, pill.fillColor(), "the media again");
        android.os.SystemClock.uptime += fade / 2;
        int middle = drawnColour(pill);
        accent.mediaDisposed(owner);
        control.beforeDraw(view);
        same(middle, drawnColour(pill), "the media goes again half way: no jump, the new fade starts from what is on the screen");

        // No fill chosen: the colour hands over to the plugin's own black, and the plugin's own pill comes back when it has.
        CapsuleView plainView = capsule();
        GradientDrawable plugin = (GradientDrawable) plainView.getBackground();
        Bundle follow = on();
        follow.putBoolean(FluidCloudCapsule.FOLLOW_MEDIA, true);
        control.configure(follow);
        accent.mediaChanged(owner);
        control.changed(plainView, Float.NaN);
        FluidCloudCapsule.PillBackground tinted = look(plainView);
        tinted.setBounds(0, 0, 200, 60);
        check(tinted != null && tinted.fillColor() == 0xff2060c0, "following with no fill chosen: the media colour");
        android.os.SystemClock.uptime += 10000L;
        accent.mediaDisposed(owner);
        control.beforeDraw(plainView);
        check(look(plainView) == tinted && plainView.getBackground() == tinted, "the colour is gone but the pill stays while it hands over");
        same(0xff000000, tinted.fillColor(), "to the plugin's own black");
        same(0xff2060c0, drawnColour(tinted), "from the media colour on the screen");
        android.os.SystemClock.uptime += fade / 2;
        same(0xff103060, drawnColour(tinted), "half way between");
        control.changed(plainView, 0.5f);
        check(plainView.getBackground() == tinted, "a step of the plugin's expansion does not cut the hand-over short");
        android.os.SystemClock.uptime += fade + 100L;
        Runnable revert = scheduled(control);
        check(revert != null, "the control scheduled a look for when it is over");
        revert.run();
        check(plainView.getBackground() == plugin && look(plainView) == null, "when it is over the plugin's own pill is back");
        same(0xff000000, plugin.getColor().getDefaultColor(), "and the plugin's own drawable was never changed");
        // Switching the capsule off does not wait for any fade.
        accent.mediaChanged(owner);
        control.changed(plainView, Float.NaN);
        check(look(plainView) != null, "the media colour again");
        control.configure(new Bundle());
        control.changed(plainView, Float.NaN);
        check(plainView.getBackground() == plugin, "switched off: the plugin's own pill at once");
    }

    /** A running outline only asks for another frame while it has visible pixels to animate. */
    private static void marqueeDrawBudget() {
        CapsuleView view = capsule();
        view.getBackground().setBounds(0, 0, 200, 60);
        FluidCloudCapsule.MarqueeRing ring = new FluidCloudCapsule.MarqueeRing(view);
        int[] colours = {0xff112233, 0xff445566, 0xff112233};
        ring.animated(colours, 0f, 3f, 30f, 3000L, false, 8f);
        int posted = view.postedInvalidations;
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a fully transparent running light draws nothing and schedules no frame");
        ring.animated(colours, 100f, 3f, 30f, 3000L, false, 0f);
        check(!draw(ring).rects.isEmpty() && view.postedInvalidations == posted + 1,
                "an opacity change resumes the same running light");
        posted = view.postedInvalidations;
        view.getBackground().setBounds(0, 0, 0, 0);
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "an unmeasured pill does not schedule empty frames");
        view.getBackground().setBounds(0, 0, 200, 60);
        view.attached = false;
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a detached pill does not keep drawing its overlay");
        view.attached = true; view.shown = false;
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a hidden pill does not keep drawing its overlay");
        view.shown = true; view.windowVisibility = android.view.View.INVISIBLE;
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a hidden window does not keep drawing its overlay");
        view.windowVisibility = android.view.View.VISIBLE; view.setAlpha(0f);
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a faded-out pill does not schedule another frame");
        view.setAlpha(1f);
        check(!draw(ring).rects.isEmpty() && view.postedInvalidations == posted + 1,
                "a visible pill resumes drawing without rebuilding its ring");
        posted = view.postedInvalidations;
        ring.stop();
        check(draw(ring).rects.isEmpty() && view.postedInvalidations == posted,
                "a removed ring never restarts its draw loop");
    }

    /** The colour the spectrum bars follow: the media colour raised to show on the capsule, turned dark or white when the pill is that colour. */
    private static void spectrumAccent() {
        android.os.SystemClock.uptime = 90000L;
        FluidCloudAccent accent = new FluidCloudAccent();
        FluidCloudCapsule control = new FluidCloudCapsule(accent);
        check(control.spectrumAccent() == null, "no media colour, nothing to follow");
        kotlinx.coroutines.flow.p colors = new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff804020));
        android.view.ViewGroup root = new android.view.ViewGroup(null);
        s1 owner = new s1(null, new K0(colors), root, null);
        accent.mediaChanged(owner);
        Integer bars = control.spectrumAccent();
        check(bars != null && bars == 0xd86c36, "the media colour, raised to a brightness that shows");
        Bundle bundle = on();
        bundle.putBoolean(FluidCloudCapsule.FILL_ENABLED, true);
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0xffd86c36);
        control.configure(bundle);
        bars = control.spectrumAccent();
        check(bars != null && bars == 0xffffff, "a pill of about that colour: the bars are white so they do not vanish into it");
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0xffe0c040);
        kotlinx.coroutines.flow.p bright = new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xffe0c040));
        android.view.ViewGroup rootTwo = new android.view.ViewGroup(null);
        FluidCloudAccent second = new FluidCloudAccent();
        FluidCloudCapsule brightControl = new FluidCloudCapsule(second);
        second.mediaChanged(new s1(null, new K0(bright), rootTwo, null));
        brightControl.configure(bundle);
        bars = brightControl.spectrumAccent();
        check(bars != null && bars == 0x202020, "a light pill of about that colour: dark bars");
        bundle.putInt(FluidCloudCapsule.FILL_COLOR, 0x40e0c040);
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.FILL_COLOR), true);
        brightControl.configure(bundle);
        bars = brightControl.spectrumAccent();
        check(bars != null && bars == 0xe0c040, "a pill that is mostly see-through is no reason to change the colour");
    }

    private static final class CoverSection extends android.view.ViewGroup {
        int reads;
        CoverSection() { super(new Context()); }
        @Override public int getChildCount() { reads++; return super.getChildCount(); }
        @Override public android.view.View getChildAt(int i) { reads++; return super.getChildAt(i); }
    }

    private static <T extends android.view.View> T named(T view, int id, String name) {
        view.setId(id); view.getResources().entryNames.put(id, name); return view;
    }

    private static void near(float wanted, float actual, String why) {
        check(Math.abs(wanted - actual) < .0001f, why + ": expected " + wanted + " but was " + actual);
    }

    /** The cover's scale follows native animation and expansion, without changing the outer pill or scanning its tree every draw. */
    private static void coverSize() {
        CapsuleView view = capsule();
        view.getBackground().setBounds(0, 0, 200, 60);
        CoverSection left = named(new CoverSection(), 1, CapsuleCover.LEFT);
        CoverSection right = named(new CoverSection(), 2, CapsuleCover.RIGHT);
        android.view.View image = named(new android.view.View(new Context()), 3, CapsuleCover.IMAGE);
        com.oplus.systemui.plugins.shared.template.component.view.CustomLottieView spectrum = named(
                new com.oplus.systemui.plugins.shared.template.component.view.CustomLottieView(new Context()), 4, CapsuleCover.IMAGE);
        spectrum.composition = new com.airbnb.lottie.LottieBase.Composition(72, 72); spectrum.duration = 2000;
        left.addView(image); right.addView(spectrum); view.addView(left); view.addView(right);
        image.setScaleX(.8f); image.setScaleY(.6f);
        android.graphics.drawable.Drawable nativePill = view.getBackground();
        FluidCloudCapsule control = new FluidCloudCapsule(null);
        Bundle bundle = on(); bundle.putBoolean(FluidCloudCapsule.COVER_SIZE_ENABLED, true); bundle.putFloat(FluidCloudCapsule.COVER_SCALE, 150f);
        control.configure(bundle); control.changed(view, Float.NaN);
        near(1.2f, image.getScaleX(), "expanded image follows native X baseline");
        near(.9f, image.getScaleY(), "expanded image follows native Y baseline independently");
        check(view.getBackground() == nativePill && view.getScaleX() == 1f && view.getScaleY() == 1f && image.layoutRequests == 0,
                "cover-only size needs no radius switch and changes no pill, layout or capsule scale");
        int reads = left.reads + right.reads;
        for (int i = 0; i < 40; i++) control.beforeDraw(view);
        near(1.2f, image.getScaleX(), "stable draws never multiply a scale again");
        check(left.reads + right.reads == reads, "a settled cover uses cached views on every draw");
        // The native animation writes both axes at the same expansion width; the draw gate must still notice it.
        image.setScaleX(.7f); image.setScaleY(.5f); control.beforeDraw(view);
        near(1.05f, image.getScaleX(), "the next native X animation value is adopted");
        near(.75f, image.getScaleY(), "the next native Y animation value is adopted");
        // A changed setting uses those native baselines, not the previous module scale.
        bundle.putFloat(FluidCloudCapsule.COVER_SCALE, 50f); control.configure(bundle);
        near(.35f, image.getScaleX(), "a smaller setting uses native X"); near(.25f, image.getScaleY(), "a smaller setting uses native Y");
        view.getBackground().setBounds(0, 0, 60, 60); control.changed(view, 0f);
        near(.7f, image.getScaleX(), "collapsed returns native X"); near(.5f, image.getScaleY(), "collapsed returns native Y");
        view.getBackground().setBounds(0, 0, 200, 60); control.changed(view, 1f);
        spectrum.composition = null; control.beforeDraw(view);
        near(.7f, image.getScaleX(), "a timer/call replacing music restores the image");
        spectrum.composition = new com.airbnb.lottie.LottieBase.Composition(72, 72); control.beforeDraw(view);
        near(.35f, image.getScaleX(), "the same cached image can become music again");
        bundle.putBoolean(FluidCloudCapsule.COVER_SIZE_ENABLED, false); control.configure(bundle);
        near(.7f, image.getScaleX(), "switch off restores native baseline immediately");
        bundle.putBoolean(FluidCloudCapsule.COVER_SIZE_ENABLED, true); bundle.putFloat(FluidCloudCapsule.COVER_SCALE, 150f); control.configure(bundle);
        // A last native animation write just before release must not be replaced by an older baseline.
        image.setScaleX(.65f); control.release();
        near(.65f, image.getScaleX(), "release preserves fresh native X"); near(.5f, image.getScaleY(), "release restores still-owned Y");

        near(1f, CapsuleCover.scaleFactor(150f, 0f), "collapsed scale is native");
        near(1.25f, CapsuleCover.scaleFactor(150f, .5f), "cover grows with expansion weight");
        near(.5f, CapsuleCover.scaleFactor(-4f, 8f), "scale lower bound"); near(1.5f, CapsuleCover.scaleFactor(999f, 8f), "scale upper bound");
        near(1f, CapsuleCover.scaleFactor(Float.NaN, 1f), "nonfinite percent stays native");
        near(1f, CapsuleCover.scaleFactor(150f, Float.POSITIVE_INFINITY), "nonfinite expansion stays native");

        CapsuleCover cache = new CapsuleCover();
        CoverSection absent = new CoverSection();
        cache.applyScale(absent, 150f, 1f); int missingReads = absent.reads;
        for (int i = 0; i < 40; i++) check(cache.scaleSettled(absent, 150f, 1f), "a missing cover is settled");
        check(absent.reads == missingReads, "missing cover is also cached between lifecycle events");
        cache.bindingChanged(absent); cache.applyScale(absent, 150f, 1f);
        check(absent.reads > missingReads, "a lifecycle event rechecks an absent child");
    }

    public static void main(String[] args) throws Exception {
        defaultsAndCatalog();
        structure();
        drawing();
        newLook();
        fadeMaths();
        handOver();
        marqueeDrawBudget();
        spectrumAccent();
        coverSize();
        System.out.println("FluidCloudCapsuleCheck: " + checks + " checks passed (settings, shape-checked contract, fill/outline/corners, media follow, restore, safe mode, release)");
    }
}
