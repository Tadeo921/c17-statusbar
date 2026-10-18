package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.heytap.log.nx.obus.a;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplus.view.ViewRootManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Native shape/gradient contract, source isolation, scene gates and latest-native restoration. */
public final class FluidCloudAppearanceCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private static void drain() {
        for (int i = 0; !Handler.postedForCheck.isEmpty(); i++) {
            if (i > 40) throw new AssertionError("Appearance refresh did not settle");
            Handler.drainForCheck();
        }
    }
    private static GradientDrawable nativeGradient() {
        GradientDrawable shape = new GradientDrawable();
        shape.setColors(new int[]{0x00112233, 0x80445566, 0xe0778899}, new float[]{0f, .3f, 1f});
        shape.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
        shape.setCornerRadii(new float[]{12, 12, 18, 18, 21, 21, 14, 14});
        shape.setStroke(3, 0x50ffffff); shape.setBounds(4, 6, 304, 106);
        shape.setLevel(19); shape.setState(new int[]{2, 7}); shape.setAlpha(127);
        return shape;
    }
    public static void main(String[] args) throws Exception {
        gradientContract(); solidContract(); unknownSafety(); policyAndRestore(); settings();
        System.out.println("FluidCloudAppearanceCheck: " + checks + " checks passed");
    }
    private static void gradientContract() {
        GradientDrawable original = nativeGradient();
        check(FluidCloudAppearance.gradient(original, null, 100f) == original, "100 percent leaves exact native object and stops unchanged");
        check(FluidCloudAppearance.gradient(original, null, Float.NaN) == original, "invalid range falls back to original native range");
        for (float range : new float[]{1f, 23.4f, 50f, 99.9f}) {
            GradientDrawable copy = (GradientDrawable) FluidCloudAppearance.gradient(original, null, range);
            check(copy != original && copy.getConstantState() != original.getConstantState(), "private native gradient state " + range);
            int[] colors = copy.getColors(); float[] positions = copy.mGradientState.mPositions;
            check(Arrays.equals(colors, new int[]{0x00112233, 0x80445566, 0xe0778899, 0xe0778899}), "native gradient RGB and alpha retained " + range);
            check(Math.abs(positions[1] - .3f * range / 100f) < .00001f && positions[0] == 0f
                    && Math.abs(positions[2] - range / 100f) < .00001f && positions[3] == 1f, "non-uniform native transition scaled and endpoint extended " + range);
            check(copy.getOrientation() == GradientDrawable.Orientation.BOTTOM_TOP && copy.getGradientType() == original.getGradientType(), "native direction and gradient type retained");
            check(copy.getAlpha() == 127 && copy.getLevel() == 19 && Arrays.equals(copy.getState(), new int[]{2, 7})
                    && copy.getBounds().left == 4 && Arrays.equals(copy.getCornerRadii(), original.getCornerRadii()), "native animation alpha, state, bounds and asymmetric shape retained");
            check(copy.strokeWidthForCheck() == 3 && copy.strokeColorForCheck() == 0x50ffffff, "native outline not made solid");
        }
        check(Arrays.equals(original.mGradientState.mPositions, new float[]{0f, .3f, 1f}) && original.getColors().length == 3,
                "source gradient and native offsets never mutated");
        GradientDrawable invalid = nativeGradient(); invalid.mGradientState.mPositions = new float[]{0f, .9f, .2f};
        check(FluidCloudAppearance.gradient(invalid, null, 50) == invalid, "invalid OEM stop order fails closed");
        GradientDrawable radial = nativeGradient(); radial.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        check(FluidCloudAppearance.gradient(radial, null, 50) == radial, "unverified non-linear recipe remains native");
        LayerDrawable nativeLayers = new LayerDrawable(new Drawable[]{original, new GradientDrawable()});
        nativeLayers.setLayerInset(0, 3, 5, 7, 9); nativeLayers.setBounds(10, 20, 330, 140);
        LayerDrawable layered = (LayerDrawable) FluidCloudAppearance.gradient(nativeLayers, null, 50);
        check(layered != nativeLayers && layered.getLayerInsetLeft(0) == 3 && layered.getLayerInsetBottom(0) == 9,
                "native layer insets retained");
        check(layered.getDrawable(0).getBounds().left == 13 && layered.getDrawable(0).getAlpha() == 127,
                "layer bounds and per-leaf animation alpha retained");
    }
    private static void solidContract() {
        GradientDrawable original = nativeGradient(); original.setTint(0xff000000);
        for (int color : new int[]{0xffa04020, 0x80a04020, 0x01a04020}) {
            GradientDrawable solid = (GradientDrawable) FluidCloudAppearance.solid(original, null, color);
            check(solid != original && solid.getColors() == null && solid.getColor().getDefaultColor() == color,
                    "one flat selected color without inherited gradient alpha " + color);
            check(solid.getAlpha() == original.getAlpha() && solid.strokeWidthForCheck() == 3
                    && Arrays.equals(solid.getCornerRadii(), original.getCornerRadii()), "flat fill preserves animated opacity, native shape and outline");
            check(solid.tintForCheck == null && original.tintForCheck.getDefaultColor() == 0xff000000,
                    "private flat fill clears black native tint while source tint remains");
        }
        GradientDrawable transparentOutline = new GradientDrawable(); transparentOutline.setColor(0x00ffffff); transparentOutline.setStroke(2, 0x70ffffff);
        GradientDrawable second = nativeGradient();
        LayerDrawable source = new LayerDrawable(new Drawable[]{transparentOutline, original, second});
        LayerDrawable solid = (LayerDrawable) FluidCloudAppearance.solid(source, null, 0x80123456);
        check(((GradientDrawable) solid.getDrawable(0)).getColor().getDefaultColor() >>> 24 == 0,
                "transparent outline leaf does not become a full plate");
        check(((GradientDrawable) solid.getDrawable(1)).getColor().getDefaultColor() == 0x80123456
                && ((GradientDrawable) solid.getDrawable(2)).getColor().getDefaultColor() == 0x00123456,
                "selected fill alpha belongs to exactly one native base, never multiplied by stacked plates");
        check(original.getColors().length == 3 && second.getColors().length == 3 && transparentOutline.strokeWidthForCheck() == 2,
                "all shared source fills and outlines unchanged");
    }
    private static void unknownSafety() {
        Drawable image = new Drawable() {
            public void draw(Canvas c) { } public void setAlpha(int alpha) { }
            public void setColorFilter(ColorFilter filter) { } public int getOpacity() { return 0; }
        };
        LayerDrawable album = new LayerDrawable(new Drawable[]{nativeGradient(), image});
        check(FluidCloudAppearance.solid(album, null, 0xff123456) == album
                && FluidCloudAppearance.gradient(album, null, 20) == album, "artwork and unknown containers remain native as a whole");
        GradientDrawable subclass = new GradientDrawable() { };
        check(FluidCloudAppearance.solid(subclass, null, 0xff123456) == subclass, "unknown OEM shape subclass excluded");
        GradientDrawable stateful = new GradientDrawable();
        stateful.setColor(new android.content.res.ColorStateList(new int[][]{new int[]{2}, new int[0]}, new int[]{0xffaabbcc, 0xff112233}));
        check(FluidCloudAppearance.solid(stateful, null, 0xff123456) == stateful, "stateful interaction selector remains native");
        android.content.res.ColorStateList states = new android.content.res.ColorStateList(
                new int[][]{new int[]{2}, new int[0]}, new int[]{0xffaabbcc, 0xff112233});
        GradientDrawable statefulTint = nativeGradient(); statefulTint.setTintList(states);
        check(FluidCloudAppearance.solid(statefulTint, null, 0xff123456) == statefulTint,
                "stateful native tint does not become a private flat default colour");
        check(FluidCloudAppearance.gradient(statefulTint, null, 50) == statefulTint,
                "stateful native tint keeps its own gradient transitions");
        GradientDrawable statefulStops = nativeGradient();
        statefulStops.mGradientState.mGradientColors = new android.content.res.ColorStateList[]{
                android.content.res.ColorStateList.valueOf(0), states, android.content.res.ColorStateList.valueOf(0xff778899)};
        check(FluidCloudAppearance.solid(statefulStops, null, 0xff123456) == statefulStops,
                "stateful gradient stops cannot be flattened to their default RGB");
        check(FluidCloudAppearance.gradient(statefulStops, null, 50) == statefulStops,
                "stateful gradient stops are not replaced by fixed colour arrays");
    }
    private static void policyAndRestore() throws Exception {
        Handler.postedForCheck.clear(); FluidCloudStyle style = new FluidCloudStyle();
        style.resolve(FluidCloudAppearanceCheck.class.getClassLoader()); a.style = style; a.controller = null; ViewRootManager.style = style;
        CardView card = new CardView(); CardBackgroundView host = new CardBackgroundView(new android.content.res.Resources()); card.addView(host);
        ViewRootManager manager = new ViewRootManager(); a helper = new a(null, 3);
        a.nativeBackgroundForCheck = nativeGradient(); helper.b(host, manager);
        Bundle settings = new Bundle(); settings.putBoolean(FluidCloudStyle.MASTER, true);
        settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND, true); settings.putString(FluidCloudStyle.BACKGROUND_MODE, "native");
        settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 40f);
        host.setAlpha(.42f); host.setTranslationX(31f); host.setTranslationY(11f); host.setScaleX(.8f);
        style.configure(settings); drain(); style.finishFillFadesForCheck();
        check(((GradientDrawable) host.getBackground()).mGradientState.mPositions.length == 4, "actual completed initializer applies native transition range");
        check(host.getAlpha() == .42f && host.getTranslationX() == 31f && host.getTranslationY() == 11f
                && host.getScaleX() == .8f, "card position, expansion and alpha animation remain native");
        int builds = a.builds;
        for (int i = 0; i < 1000; i++) { style.refreshAfterAccent(); drain(); }
        check(a.builds == builds, "stable frames do not clone or rebuild appearance");
        settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT, false); style.configure(settings); drain();
        check(((GradientDrawable) host.getBackground()).getColors().length == 3, "floating scope off restores latest native gradient");
        settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT, true); style.configure(settings); drain();
        style.setKeyguard(true); drain();
        check(((GradientDrawable) host.getBackground()).getColors().length == 3, "floating gradient does not override lockscreen or AOD appearance");
        style.setKeyguard(false); drain();
        settings.putString(FluidCloudStyle.BACKGROUND_MODE, "custom"); settings.putInt(FluidCloudStyle.BACKGROUND_COLOR, 0xff123456);
        style.configure(settings); drain(); style.finishFillFadesForCheck();
        GradientDrawable solid = (GradientDrawable) host.getBackground();
        check(solid.getColors() == null && solid.getColor().getDefaultColor() == 0xff123456, "custom mode completes as a flat selected base");
        a.nativeBackgroundForCheck = nativeGradient(); ((GradientDrawable) a.nativeBackgroundForCheck).setColors(new int[]{0x00334455, 0xffaabbcc});
        settings.putBoolean(FluidCloudStyle.MASTER, false); style.configure(settings); drain();
        check(Arrays.equals(((GradientDrawable) host.getBackground()).getColors(), new int[]{0x00334455, 0xffaabbcc}),
                "master off restores current native theme, not a saved older template");
        settings.putBoolean(FluidCloudStyle.MASTER, true); style.configure(settings); drain(); style.finishFillFadesForCheck();
        style.release(); drain();
        check(((GradientDrawable) host.getBackground()).getColors().length == 2, "release reruns native initializer and restores latest native appearance");
        a.nativeBackgroundForCheck = null; a.style = null; a.controller = null; ViewRootManager.style = null;
    }
    private static void settings() {
        SettingsCatalog.Item mode = SettingsCatalog.item(FluidCloudStyle.BACKGROUND_MODE);
        check(Arrays.equals(mode.values, new String[]{"native", "custom"})
                && Arrays.equals(mode.labels, new String[]{"原生渐变", "自定义纯色背景"}), "exactly two selectable modes, old saved mode values retained");
        SettingsCatalog.Item range = SettingsCatalog.item(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE);
        check(range != null && range.defaultValue.equals(100f)
                && ConfigTransfer.types().get(range.key) == ConfigTransfer.Type.NUMBER, "range default and configuration transport registered");
        Map<String, Object> values = new HashMap<>();
        check(SettingsCatalog.unavailableReason(range.key, values).contains("流体云样式"), "page master gates new range");
        values.put(FluidCloudStyle.MASTER, true);
        check(SettingsCatalog.unavailableReason(range.key, values).contains("自定义底色"), "appearance switch gates range");
        values.put(FluidCloudStyle.DESKTOP_BACKGROUND, true); values.put(FluidCloudStyle.BACKGROUND_MODE, "custom");
        check(SettingsCatalog.unavailableReason(range.key, values).contains("原生渐变"), "solid mode does not expose effective gradient control");
        values.put(FluidCloudStyle.BACKGROUND_MODE, "native");
        check(SettingsCatalog.unavailableReason(range.key, values).isEmpty(), "native mode exposes range");
    }
}
