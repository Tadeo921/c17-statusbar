package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import com.heytap.log.nx.obus.a;
import com.android.internal.graphics.drawable.BackgroundBlurDrawable;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplus.systemui.plugins.seedling.util.d;
import com.oplus.view.ViewRootManager;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;
import kotlin.Triple;
import org.json.JSONObject;

/** Replays the real expanded-card custom Drawable, not a resource GradientDrawable. */
public final class FluidCloudNativeGradientCheck {
    private static int checks;
    private static void check(boolean value, String why) {
        checks++; if (!value) throw new AssertionError(why);
    }
    private static boolean near(float a, float b) { return Math.abs(a - b) < .00001f; }
    private static d nativeShape() {
        d value = new d(0x00000000, 0xff000000, 0x05000000, new Triple<>(36f, 1, 3f));
        value.setBounds(13, 17, 413, 217);
        value.setAlpha(127); value.setLevel(19); value.setState(new int[]{2, 7});
        return value;
    }
    private static LinearGradient drawn(Drawable value) {
        Canvas canvas = new Canvas(); value.draw(canvas);
        check(canvas.rects == 1 && canvas.pathClips == 1 && canvas.saves == 1 && canvas.restores == 1,
                "OEM shape still draws exactly once inside its native path clip and save scope");
        check(canvas.lastShader instanceof LinearGradient, "native drawable supplies its actual Paint shader at draw time");
        return (LinearGradient) canvas.lastShader;
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "policy".equals(args[0])) policyAndRestore();
        else if (args.length == 1 && "draw-repair".equals(args[0])) nativeDrawRepair();
        else if (args.length == 1 && "draw-diagnostic".equals(args[0])) nativeDrawDiagnostic();
        else if (args.length == 1 && "custom-visible".equals(args[0])) customNativeColourVisible();
        else { nativeReplay(); nativeRange(); gradientShape(); nativeRangeBoundaries(); nativeLayerRange(); nativeBaseAndGradientIndependence(); customNativeColourVisible(); policyAndRestore(); discoveryLoaderPolicy(); nativeDrawRepair(); nativeDrawDiagnostic(); safety(); }
        System.out.println("FluidCloudNativeGradientCheck: " + checks + " checks passed");
    }
    private static void nativeReplay() {
        d source = nativeShape();
        check(source.getConstantState() == null, "OEM custom gradient cannot be cloned via ConstantState");
        LinearGradient shader = drawn(source);
        check(Arrays.equals(shader.colors, new int[]{0x00000000, 0xff000000, 0x05000000})
                && Arrays.equals(shader.positions, new float[]{0f, 0f, 1f}), "audited OEM DEX colours and native stop payload retained");
        check(near(shader.left, 13) && near(shader.right, 13) && near(shader.top, 17) && near(shader.bottom, 217),
                "native gradient runs from local top to bottom, not horizontally");
        check(near(shader.alpha(13, 117), 130f / 255f), "native midpoint opacity follows real [0, 0, 1] stops");
        check(FluidCloudAppearance.gradient(source, null, 100f) == source && source.g == shader,
                "100 percent returns the exact native shape and shader");
        BackgroundBlurDrawable blur = new BackgroundBlurDrawable();
        LayerDrawable layers = new LayerDrawable(new Drawable[]{blur, source});
        check(layers.getConstantState() == null, "actual blur plus custom-gradient stack also has no ConstantState");
        check(FluidCloudAppearance.gradient(layers, null, 100f) == layers && layers.getDrawable(0) == blur,
                "default range preserves the native blur sibling and owning layer");
    }
    private static void nativeRange() {
        d source = nativeShape();
        LinearGradient nativeShader = source.g;
        Paint paint = source.e; Path path = source.f; Triple<Float, Integer, Float> corners = source.d;
        ColorFilter filter = new ColorFilter(); source.setColorFilter(filter);
        Drawable changed = FluidCloudAppearance.gradient(source, null, 20f);
        LinearGradient actual = drawn(changed);
        check(actual != nativeShader && near(actual.alpha(13, 57), 5f / 255f),
                "20 percent must change the actual shader drawn by OEM d and reach the native endpoint after 40 px");
        // Code161: the ramp eases out (8 steps) so it meets the flat end colour without a kink.
        boolean monotonic = true;
        for (int i = 1; i < actual.positions.length; i++) monotonic &= actual.positions[i] >= actual.positions[i - 1];
        check(actual.colors.length == 15 && actual.colors[0] == 0x00000000 && actual.colors[1] == 0xff000000
                && actual.colors[13] == 0x05000000 && actual.colors[14] == 0x05000000 && actual.positions[0] == 0f
                && actual.positions[1] == 0f && near(actual.positions[13], .2f) && actual.positions[14] == 1f && monotonic,
                "native colours and hard first stop retained while endpoint extends");
        check(actual.colors[5] != FluidCloudAppearance.mix(0xff000000, 0x05000000, .5f), "the ramp is eased, not linear");
        check(source.e == paint && source.f == path && source.d == corners && source.e.getAlpha() == 127
                && source.e.getColorFilter() == filter, "native Paint, path, smooth corners, animated alpha and colour filter survive");
        RectF bounds = new RectF(); source.f.computeBounds(bounds, true);
        check(near(bounds.left, 13) && near(bounds.top, 17) && near(bounds.right, 413) && near(bounds.bottom, 217),
                "OEM draw still constructs the path from the current bounds");
        check(source.g == actual && source.e.getShader() == actual && changed == source,
                "range updates the native owned instance without changing Paint or introducing a replacement drawable");
        check(source.getLevel() == 19 && Arrays.equals(source.getState(), new int[]{2, 7}),
                "native state and animation level remain intact");
        source.setBounds(3, 11, 703, 411);
        LinearGradient resized = drawn(source);
        check(resized != actual && near(resized.top, 11) && near(resized.bottom, 411)
                && near(resized.alpha(3, 91), 5f / 255f), "OEM expansion reconstructs a correctly ranged shader from the new height");
        source.setBounds(0, 0, 0, 0);
        check(source.g == null, "empty native bounds retain the OEM no-draw state");
        Canvas empty = new Canvas(); source.draw(empty);
        check(empty.rects == 0 && empty.pathClips == 0, "empty animation frame creates no plate or clip");
        source.setBounds(7, 9, 407, 209);
        LinearGradient restoredBounds = drawn(source);
        check(near(restoredBounds.alpha(7, 49), 5f / 255f), "range survives a zero-size-to-visible expansion");
        FluidCloudAppearance.gradient(source, null, 50f);
        LinearGradient half = drawn(source);
        check(near(half.alpha(7, 59), 130f / 255f) && near(half.alpha(7, 109), 5f / 255f),
                "moving the range to 50 percent recomputes from the native recipe without compounding old stops");
        FluidCloudAppearance.gradient(source, null, 100f);
        LinearGradient restored = drawn(source);
        check(Arrays.equals(restored.positions, new float[]{0f, 0f, 1f})
                && restored.colors.length == 3 && near(restored.alpha(7, 109), 130f / 255f),
                "100 percent restores the current native bounds recipe and releases the adjustment");
        source.setBounds(7, 9, 407, 409);
        check(Arrays.equals(drawn(source).positions, new float[]{0f, 0f, 1f}),
                "resizing after restoration does not reapply an old range");
    }
    /** Start, end, mid line, strength and opacity reshape the same native recipe; all-default is the native gradient itself. */
    private static void gradientShape() {
        FluidCloudAppearance.GradientShape nativeLook = FluidCloudAppearance.GradientShape.NATIVE;
        check(nativeLook.isNative() && FluidCloudAppearance.GradientShape.ofEnd(100f).isNative()
                && FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 100f, 100f).isNative(), "all-default numbers are the native gradient");
        check(!FluidCloudAppearance.GradientShape.of(1f, 100f, 50f, 100f, 100f).isNative()
                && !FluidCloudAppearance.GradientShape.of(0f, 100f, 49f, 100f, 100f).isNative()
                && !FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 99f, 100f).isNative()
                && !FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 100f, 99f).isNative(), "each number alone leaves the native look");
        FluidCloudAppearance.GradientShape inverted = FluidCloudAppearance.GradientShape.of(60f, 20f, 50f, 100f, 100f);
        check(inverted.end - inverted.start >= .0099f && inverted.start == .6f, "an end before the start is kept one point after it");
        check(FluidCloudAppearance.GradientShape.of(Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN).isNative(),
                "non-finite input falls back to native");
        for (float mid : new float[]{.1f, .25f, .5f, .8f, .95f}) {
            FluidCloudAppearance.GradientShape shape = FluidCloudAppearance.GradientShape.of(0f, 100f, mid * 100f, 100f, 100f);
            check(Math.abs(shape.weight(mid) - .5f) < .0005f, "the mid line is where the colour is half way: " + mid);
            check(near(shape.weight(0f), 0f) && near(shape.weight(1f), 1f), "weights run from 0 to 1");
            float previous = -1f; boolean rising = true;
            for (int i = 0; i <= 20; i++) { float w = shape.weight(i / 20f); rising &= w >= previous - .00001f; previous = w; }
            check(rising, "the ramp never runs backwards at mid " + mid);
        }
        check(Math.abs(FluidCloudAppearance.GradientShape.of(0f, 99.9f, 50f, 100f, 100f).weight(.3f)
                - FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 100f, 100f).weight(.3f)) < .01f,
                "the curve is continuous where a flat end first appears");

        // Start: the first stretch stays the start colour.
        d source = nativeShape();
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.of(30f, 100f, 50f, 100f, 100f));
        LinearGradient late = drawn(source);
        check(near(late.alpha(13, 17 + 200f * .15f), 1f) && near(late.alpha(13, 17 + 200f * .30f), 1f)
                && near(late.alpha(13, 17 + 200f * 1f), 5f / 255f), "before the start the card keeps the native start colour");
        check(late.alpha(13, 17 + 200f * .65f) < 1f && late.alpha(13, 17 + 200f * .65f) > 5f / 255f, "the ramp lies between start and end");
        // Mid line: a lower mid reaches the half-way colour earlier.
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.of(0f, 100f, 25f, 100f, 100f));
        check(near(drawn(source).alpha(13, 17 + 200f * .25f), 130f / 255f), "half way at the chosen mid line");
        // Strength: 0 is no gradient, 200 pushes the end past the native end colour.
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 0f, 100f));
        LinearGradient flat = drawn(source);
        check(near(flat.alpha(13, 117), 1f) && near(flat.alpha(13, 217), 1f), "strength 0 keeps the start colour everywhere");
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 200f, 100f));
        check(near(drawn(source).alpha(13, 217), 0f), "strength 200 drives the end colour to fully transparent");
        // Opacity: fades the whole layer.
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.of(0f, 100f, 50f, 100f, 50f));
        LinearGradient faded = drawn(source);
        check(near(faded.alpha(13, 117), 66f / 255f), "opacity halves the whole gradient layer");
        // The previous shape is not compounded and native is restored exactly.
        FluidCloudAppearance.gradient(source, null, FluidCloudAppearance.GradientShape.NATIVE);
        LinearGradient restored = drawn(source);
        check(Arrays.equals(restored.positions, new float[]{0f, 0f, 1f}) && near(restored.alpha(13, 117), 130f / 255f),
                "returning to the all-default shape restores the native recipe");
    }
    private static void nativeLayerRange() {
        d source = nativeShape(); BackgroundBlurDrawable blur = new BackgroundBlurDrawable();
        LayerDrawable layer = new LayerDrawable(new Drawable[]{blur, source});
        layer.setLayerInset(1, 3, 5, 7, 9); layer.setBounds(10, 20, 410, 220);
        Drawable.Callback callback = new Drawable.Callback() {
            public void invalidateDrawable(Drawable value) { }
            public void scheduleDrawable(Drawable value, Runnable work, long when) { }
            public void unscheduleDrawable(Drawable value, Runnable work) { }
        };
        layer.setCallback(callback); blur.setCallback(callback); source.setCallback(callback);
        Drawable ranged = FluidCloudAppearance.gradient(layer, null, 25f);
        LinearGradient shader = drawn(ranged);
        check(ranged == layer && layer.getDrawable(0) == blur && layer.getDrawable(1) == source,
                "true blur sibling, foreground and LayerDrawable keep identity and ordering");
        check(layer.getCallback() == callback && blur.getCallback() == callback && source.getCallback() == callback,
                "native layer callbacks remain attached without copying or replacing blur");
        check(layer.getLayerInsetLeft(1) == 3 && layer.getLayerInsetBottom(1) == 9
                && near(shader.top, 25) && near(shader.bottom, 211), "native foreground layer insets remain effective");
        check(near(shader.alpha(13, 25 + 186f * .25f), 5f / 255f), "actual foreground stops change inside the native blur stack");
        LinearGradient unchangedBlurSiblingShader = source.g;
        for (int i = 0; i < 1000; i++) {
            FluidCloudAppearance.gradient(layer, null, 25f);
            check(source.g == unchangedBlurSiblingShader, "steady range does not allocate another foreground shader " + i);
        }
        FluidCloudAppearance.gradient(layer, null, 100f);
        check(Arrays.equals(source.g.positions, new float[]{0f, 0f, 1f}) && layer.getDrawable(0) == blur,
                "range reset preserves the native blur sibling and restores native stops");
    }
    /** Code161 eased range: [a, b] at 0, eight eased stops ending at the boundary, the end colour at 1. */
    private static boolean ranged(float[] positions, float boundary) {
        return positions.length == 15 && positions[0] == 0f && positions[1] == 0f && near(positions[13], boundary) && positions[14] == 1f;
    }
    private static void nativeRangeBoundaries() {
        for (float range : new float[]{1f, 23.4f, 99.9f}) {
            d source = nativeShape(); FluidCloudAppearance.gradient(source, null, range);
            LinearGradient shader = drawn(source); float ratio = range / 100f;
            check(near(shader.alpha(13, 17 + 200f * ratio), 5f / 255f),
                    "actual draw reaches the native endpoint at the requested range boundary " + range);
            // Halfway through the requested range the eased curve is 87.5% of the way (alpha 36).
            check(near(shader.alpha(13, 17 + 100f * ratio), 130f / 255f),
                    "native fading curve scales continuously across the requested range " + range);
            check(shader.positions.length == 15 && near(shader.positions[13], ratio), "the finite slider boundary reaches the real shader " + range);
            FluidCloudAppearance.gradient(source, null, 100f);
        }
    }
    private static void drain() {
        for (int i = 0; !Handler.postedForCheck.isEmpty(); i++) {
            if (i > 40) throw new AssertionError("Native appearance refresh did not settle");
            Handler.drainForCheck();
        }
    }
    /** Faithful hook-dispatch seam, not proof of phone/GPU acceptance. Native
     * initialization registers the empty drawable; OEM bounds then replace g
     * without the bounds observer, and the exact draw entry must recover it. */
    private static void nativeDrawRepair() throws Exception {
        boolean oldBoundsHook = d.dispatchBoundsHookForCheck, oldDrawHook = d.dispatchDrawHookForCheck;
        d.dispatchBoundsHookForCheck = false; d.dispatchDrawHookForCheck = true;
        Handler.postedForCheck.clear(); FluidCloudAppearance.releaseNativeGradients();
        FluidCloudStyle style = new FluidCloudStyle(); style.resolve(FluidCloudNativeGradientCheck.class.getClassLoader());
        a.style = style; a.controller = null; a.nativeBackgroundForCheck = null; ViewRootManager.style = style;
        final d[] latest = new d[1];
        a.nativeBackgroundFactoryForCheck = (view, manager) -> {
            latest[0] = new d(0x00000000, 0xff000000, 0x05000000, new Triple<>(36f, 1, 3f));
            latest[0].setAlpha(122);
            // View establishes native local bounds during its later draw. Do
            // not eagerly assign them before initializer completion here.
            return new LayerDrawable(new Drawable[]{new BackgroundBlurDrawable(), latest[0]});
        };
        try {
            CardView card = new CardView(); CardBackgroundView host = new CardBackgroundView(new android.content.res.Resources()); card.addView(host);
            ViewRootManager manager = new ViewRootManager(); a helper = new a(null, 3);
            Bundle settings = new Bundle(); settings.putBoolean(FluidCloudStyle.MASTER, true);
            settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND, true); settings.putString(FluidCloudStyle.BACKGROUND_MODE, "native");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 1f);
            style.configure(settings); drain(); helper.b(host, manager); drain();
            d first = latest[0]; Drawable root = host.getBackground();
            check(first.getBounds().isEmpty() && first.g == null && FluidCloudAppearance.nativeGradientOwned(root),
                    "initializer completion registers a saved 1 percent lease before native local bounds exist");
            Canvas empty = new Canvas(); root.draw(empty);
            check(empty.rects == 0 && first.g == null, "draw entry keeps initial empty native bounds unpainted");
            root.setBounds(0, 0, 1312, 576); LinearGradient firstNative = first.g;
            check(Arrays.equals(firstNative.positions, new float[]{0f, 0f, 1f}) && FluidCloudAppearance.nativeGradientOwned(root),
                    "suppressed bounds observer reproduces a live lease whose actual OEM shader has reverted to 100 percent");
            Paint paint = first.e; Path path = first.f; Triple<Float, Integer, Float> corners = first.d;
            int paintAlpha = paint.getAlpha(); int builds = a.builds;
            LinearGradient one = drawn(root);
            check(ranged(one.positions, .01f) && near(one.alpha(0, 288), 5f / 255f),
                    "exact native draw entry repairs the saved 1 percent range after OEM bounds rebuild skipped its observer");
            check(one.colors[0] == first.a && one.colors[1] == first.b && one.colors[13] == first.c && one.colors[14] == first.c
                    && first.e == paint && first.f == path && first.d == corners && paint.getAlpha() == paintAlpha,
                    "draw recovery preserves native ARGB, Paint, animated alpha, path and corner configuration");
            for (int i = 0; i < 1000; i++) root.draw(new Canvas());
            check(first.g == one && a.builds == builds, "steady native draw frames keep the same shader and do not reinitialize the owner");
            root.setBounds(0, 0, 1312, 864); LinearGradient resizedNative = first.g;
            check(resizedNative != one && Arrays.equals(resizedNative.positions, new float[]{0f, 0f, 1f}),
                    "OEM expansion overwrites the adjusted shader with a new native recipe");
            LinearGradient resized = drawn(root);
            check(near(resized.bottom, 864) && near(resized.alpha(0, 432), 5f / 255f),
                    "draw recovery uses the current expansion bounds and saved spatial range");
            root.setBounds(0, 0, 0, 0); Canvas hidden = new Canvas(); root.draw(hidden);
            check(hidden.rects == 0 && first.g == null, "zero-size animation frame retains the native null shader");
            root.setBounds(0, 0, 1312, 576); LinearGradient restoredNative = first.g;
            check(near(drawn(root).alpha(0, 288), 5f / 255f), "draw entry repairs a zero-size-to-visible native reconstruction");

            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 50f); style.configure(settings); drain();
            check(first.g == restoredNative && !FluidCloudAppearance.nativeGradientOwned(first),
                    "range reconfiguration restores the latest OEM shader of a retained old owner");
            root = host.getBackground(); d halfOwner = latest[0]; root.setBounds(0, 0, 1312, 576); LinearGradient halfNative = halfOwner.g;
            LinearGradient half = drawn(root);
            check(ranged(half.positions, .5f)
                    && near(half.alpha(0, 144), 130f / 255f) && near(half.alpha(0, 288), 5f / 255f),
                    "saved 50 percent remains a spatial transition after a missed bounds observer");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 100f); style.configure(settings); drain();
            check(halfOwner.g == halfNative && !FluidCloudAppearance.nativeGradientOwned(halfOwner),
                    "100 percent restores the latest OEM shader and releases the old range lease");
            root = host.getBackground(); root.setBounds(0, 0, 1312, 576); LinearGradient fullNative = latest[0].g;
            check(drawn(root) == fullNative && near(fullNative.alpha(0, 288), 130f / 255f)
                    && !FluidCloudAppearance.nativeGradientOwned(root), "100 percent draw preserves exact native shader identity and stops");

            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 1f); style.configure(settings); drain();
            root = host.getBackground(); root.setBounds(0, 0, 1312, 576); d beforeOff = latest[0]; LinearGradient offNative = beforeOff.g;
            drawn(root);
            settings.putBoolean(FluidCloudStyle.MASTER, false); style.configure(settings); drain();
            check(beforeOff.g == offNative && !FluidCloudAppearance.nativeGradientOwned(beforeOff),
                    "master off restores and detaches the latest native recipe of a repaired owner");
            root = host.getBackground(); root.setBounds(0, 0, 1312, 576); LinearGradient disabledNative = latest[0].g;
            check(drawn(root) == disabledNative && !FluidCloudAppearance.nativeGradientOwned(root),
                    "draw recovery remains inactive after appearance master off");
            settings.putBoolean(FluidCloudStyle.MASTER, true); style.configure(settings); drain();
            root = host.getBackground(); root.setBounds(0, 0, 1312, 576); d beforeRelease = latest[0]; LinearGradient releaseNative = beforeRelease.g;
            drawn(root); style.release(); drain();
            check(beforeRelease.g == releaseNative && !FluidCloudAppearance.nativeGradientOwned(beforeRelease),
                    "release restores the latest OEM shader without leaving a retained draw lease");
            d unrelated = nativeShape(); LinearGradient unrelatedNative = unrelated.g;
            check(drawn(unrelated) == unrelatedNative && !FluidCloudAppearance.nativeGradientOwned(unrelated),
                    "exact OEM draw entry cannot change a drawable that never belonged to a registered appearance owner");
            nativeResetBeforeDraw();
        } finally {
            style.release(); drain(); FluidCloudAppearance.releaseNativeGradients();
            a.nativeBackgroundFactoryForCheck = null; a.style = null; a.controller = null; ViewRootManager.style = null;
            d.dispatchBoundsHookForCheck = oldBoundsHook; d.dispatchDrawHookForCheck = oldDrawHook;
        }
    }
    /** The owner may be disabled or reconfigured between native reconstruction
     * and its next draw; restoring a stale earlier native recipe is incorrect. */
    private static void nativeResetBeforeDraw() {
        d beforeReset = nativeShape(); FluidCloudAppearance.gradient(beforeReset, null, 20f);
        beforeReset.setBounds(13, 17, 1325, 593); LinearGradient newest = beforeReset.g;
        FluidCloudAppearance.gradient(beforeReset, null, 100f);
        check(beforeReset.g == newest && !FluidCloudAppearance.nativeGradientOwned(beforeReset),
                "100 percent before the next draw restores the newest OEM reconstruction, not a stale earlier shader");

        d beforeRange = nativeShape(); FluidCloudAppearance.gradient(beforeRange, null, 20f);
        beforeRange.setBounds(13, 17, 1325, 593); LinearGradient rangeNative = beforeRange.g;
        FluidCloudAppearance.gradient(beforeRange, null, 50f);
        LinearGradient changed = drawn(beforeRange);
        check(ranged(changed.positions, .5f) && near(changed.bottom, 593),
                "20 to 50 percent before a pending native draw uses current geometry without compounding the old range");
        FluidCloudAppearance.gradient(beforeRange, null, 100f);
        check(beforeRange.g == rangeNative, "restoring after an unpainted range change retains the latest OEM shader identity");

        d repeated = nativeShape(); LinearGradient repeatedNative = repeated.g;
        FluidCloudAppearance.gradient(repeated, null, 20f); LinearGradient applied = repeated.g;
        for (int i = 0; i < 20; i++) FluidCloudAppearance.nativeBoundsChanged(repeated);
        check(repeated.g == applied, "duplicate bounds observers do not reapply or adopt the already adjusted shader as native");
        FluidCloudAppearance.gradient(repeated, null, 100f);
        check(repeated.g == repeatedNative && Arrays.equals(repeated.g.positions, new float[]{0f, 0f, 1f}),
                "duplicate observers cannot cause restoration to leave a short-range shader behind");

        d beforeRelease = nativeShape(); FluidCloudAppearance.gradient(beforeRelease, null, 20f);
        beforeRelease.setBounds(13, 17, 1325, 593); LinearGradient releaseNative = beforeRelease.g;
        FluidCloudAppearance.releaseNativeGradients();
        check(beforeRelease.g == releaseNative && !FluidCloudAppearance.nativeGradientOwned(beforeRelease),
                "release between an OEM reset and draw preserves the newest original shader");
        d beforeEmptyReset = nativeShape(); FluidCloudAppearance.gradient(beforeEmptyReset, null, 20f);
        beforeEmptyReset.setBounds(0, 0, 0, 0); FluidCloudAppearance.gradient(beforeEmptyReset, null, 100f);
        check(beforeEmptyReset.g == null && !FluidCloudAppearance.nativeGradientOwned(beforeEmptyReset),
                "reset before an empty frame draws cannot resurrect the previous nonempty native shader");
    }
    /** Verify the fact emitted by the actual fixture draw interceptor, rather
     * than calling fact() with a hand-built values object. These observations
     * establish draw inputs and logging behavior, not GPU/phone acceptance. */
    private static void nativeDrawDiagnostic() throws Exception {
        boolean oldEnabled = ModuleDiagnostics.enabled();
        boolean oldBoundsHook = d.dispatchBoundsHookForCheck, oldDrawHook = d.dispatchDrawHookForCheck;
        Field pendingField = ModuleDiagnostics.class.getDeclaredField("PENDING"); pendingField.setAccessible(true);
        @SuppressWarnings("unchecked") ArrayDeque<String> pending = (ArrayDeque<String>) pendingField.get(null);
        Bundle disabled = new Bundle(); disabled.putBoolean(ModuleDiagnostics.KEY_ENABLED, false);
        Bundle active = new Bundle(); active.putBoolean(ModuleDiagnostics.KEY_ENABLED, true);
        ModuleDiagnostics.configure(disabled); FluidCloudAppearance.releaseNativeGradients();
        d.dispatchBoundsHookForCheck = false; d.dispatchDrawHookForCheck = true;
        try {
            d owner = new d(0, 0xff000000, 0x05000000, new Triple<>(36f, 1, 3f));
            FluidCloudAppearance.gradient(owner, null, 1f);
            owner.setBounds(0, 0, 1312, 576); LinearGradient unadjusted = owner.g;
            check(near(drawn(owner).alpha(0, 288), 5f / 255f) && owner.g != unadjusted,
                    "real fixture draw repairs native shader inputs even while diagnostics are disabled");
            check(pending.isEmpty(), "disabled diagnostics enqueue no gradient draw observation");

            ModuleDiagnostics.configure(active);
            d empty = new d(0, 0xff000000, 0x05000000, new Triple<>(36f, 1, 3f));
            FluidCloudAppearance.gradient(empty, null, 1f); Canvas emptyCanvas = new Canvas(); empty.draw(emptyCanvas);
            check(emptyCanvas.rects == 0 && pending.isEmpty(), "an enabled but empty native owner cannot report gradient_drawn true");
            drawn(owner);
            check(pending.size() == 1, "enabling diagnostics after an earlier draw still records the first observation for this lease");
            JSONObject event = new JSONObject(pending.peekLast()); JSONObject details = event.getJSONObject("details");
            JSONObject values = details.getJSONObject("values");
            check("fact".equals(details.getString("kind")) && "fluid".equals(details.getString("topic"))
                    && "fluid".equals(event.getString("source")), "native draw completion uses the fluid fact diagnostic contract");
            check(near((float) values.getDouble("gradient_range"), 1f), "actual native draw fact retains the saved 1 percent numeric range");
            check(values.getBoolean("gradient_owned"), "actual native draw fact retains its registered ownership flag");
            check(values.getBoolean("gradient_drawn"), "actual nonempty native draw fact retains its draw observation flag");
            check(values.getBoolean("gradient_rebuilt"), "actual native draw fact records recovery after the suppressed bounds observer");
            check(values.getInt("view_width") == 1312 && values.getInt("view_height") == 576,
                    "native draw fact records current local foreground dimensions");
            JSONObject persisted = ModuleDiagnostics.validateEvent(new JSONObject(event.toString())).getJSONObject("details").getJSONObject("values");
            for (String key : new String[]{"gradient_range", "gradient_owned", "gradient_drawn", "gradient_rebuilt", "view_width", "view_height"})
                check(values.get(key).toString().equals(persisted.get(key).toString()),
                        "actual draw observation survives serialization and event validation: " + key);
            LinearGradient fixed = owner.g;
            for (int i = 0; i < 100; i++) owner.draw(new Canvas());
            check(pending.size() == 1 && owner.g == fixed, "one hundred stable native draws emit only one observation and reuse the same shader");

            FluidCloudAppearance.gradient(owner, null, 50f); drawn(owner);
            check(pending.size() == 2, "a changed spatial range receives a new once-per-lease native draw observation");
            JSONObject changed = new JSONObject(pending.peekLast()).getJSONObject("details").getJSONObject("values");
            check(near((float) changed.getDouble("gradient_range"), 50f) && changed.getBoolean("gradient_owned")
                    && changed.getBoolean("gradient_drawn") && !changed.getBoolean("gradient_rebuilt"),
                    "a preapplied 50 percent draw preserves all four diagnostic fields without claiming another draw-time repair");
            FluidCloudAppearance.gradient(owner, null, 100f); drawn(owner);
            check(pending.size() == 2 && !FluidCloudAppearance.nativeGradientOwned(owner),
                    "restoring 100 percent releases ownership and cannot emit a custom range draw fact");
            d unrelated = nativeShape(); LinearGradient unrelatedNative = unrelated.g; drawn(unrelated); empty.draw(new Canvas());
            check(pending.size() == 2 && unrelated.g == unrelatedNative,
                    "unknown native owner and empty owner emit no extra drawn fact or mutate unrelated inputs");
        } finally {
            FluidCloudAppearance.releaseNativeGradients(); ModuleDiagnostics.configure(disabled);
            if (oldEnabled) ModuleDiagnostics.configure(active);
            d.dispatchBoundsHookForCheck = oldBoundsHook; d.dispatchDrawHookForCheck = oldDrawHook;
        }
    }
    /** Real BackgroundUtils order: resource colours first, then the OEM constructor, finally the appearance completion hook. */
    private static int nativeResourceColor(FluidCloudStyle style,android.content.res.Resources resources,int id,int nativeColor) {
        int accent=a.controller==null?nativeColor:a.controller.resourceColor(resources,id,nativeColor);
        return style.resourceColor(resources,id,accent);
    }
    /** Changing the base material is not enough: the top OEM gradient actually reaches the screen. */
    private static void customNativeColourVisible() throws Exception {
        Handler.postedForCheck.clear();FluidCloudAppearance.releaseNativeGradients();
        FluidCloudStyle style=new FluidCloudStyle();style.resolve(FluidCloudNativeGradientCheck.class.getClassLoader());
        a.style=style;a.controller=null;ViewRootManager.style=style;
        android.content.res.Resources resources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){
                return id==1?"card_background_mix_color":id==2||id==102?"card_background_gradient_middle_color"
                        :id==101?"card_background_gradient_top_color":"card_background_gradient_bottom_color";
            }
        };
        final d[] leaf=new d[1];final LinearGradient[] nativeShader=new LinearGradient[1];
        final BackgroundBlurDrawable[] blur=new BackgroundBlurDrawable[1];
        a.nativeBackgroundFactoryForCheck=(host,manager)->{
            leaf[0]=new d(nativeResourceColor(style,resources,101,0),nativeResourceColor(style,resources,102,0xff000000),
                    nativeResourceColor(style,resources,103,0x05000000),new Triple<>(36f,1,3f));
            blur[0]=new BackgroundBlurDrawable();LayerDrawable layers=new LayerDrawable(new Drawable[]{blur[0],leaf[0]});
            layers.setBounds(13,17,413,217);leaf[0].setAlpha(173);nativeShader[0]=leaf[0].g;return layers;
        };
        try {
            CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(resources);card.addView(host);
            ViewRootManager manager=new ViewRootManager();a helper=new a(null,3);
            Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);
            settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);settings.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
            style.configure(settings);drain();helper.b(host,manager);
            check(drawn(host.getBackground())==nativeShader[0],"unedited default keeps the exact code185 OEM gradient");
            for(int colour:new int[]{0xffed2840,0xff2486ec,0xff34c862,FluidCloudStyle.DEFAULT_BACKGROUND_COLOR}) {
                settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,true);
                settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,colour);
                style.configure(settings);drain();style.finishFillFadesForCheck();
                LinearGradient visible=drawn(host.getBackground());
                check((visible.colors[1]&0xffffff)==(colour&0xffffff),
                        "explicit native-mode colour reaches the VISIBLE OEM gradient rather than remaining black");
                check(Arrays.equals(visible.positions,new float[]{0f,0f,1f})&&(visible.colors[1]>>>24)==255
                        &&(visible.colors[2]>>>24)==5,"colour changes retain native stop positions and independent original alpha");
                check(leaf[0].a==0&&leaf[0].b==0xff000000&&leaf[0].c==0x05000000&&leaf[0].getAlpha()==173
                        &&((LayerDrawable)host.getBackground()).getDrawable(0)==blur[0],"custom colour leases only the shader and retains OEM recipe, Paint animation and blur owner");
            }
            boolean boundsHook=d.dispatchBoundsHookForCheck;
            try {
                d.dispatchBoundsHookForCheck=false;leaf[0].setBounds(5,7,505,307);
                LinearGradient repaired=drawn(host.getBackground());
                check(repaired!=nativeShader[0]&&(repaired.colors[1]&0xffffff)==(FluidCloudStyle.DEFAULT_BACKGROUND_COLOR&0xffffff)
                        &&near(repaired.top,7)&&near(repaired.bottom,307),"native resize keeps the chosen visible hue even when only the draw boundary catches its reset");
                int builds=a.builds;for(int i=0;i<20;i++)leaf[0].draw(new Canvas());
                check(leaf[0].g==repaired&&a.builds==builds,"steady custom-colour frames retain their shader and never replay native initialization");
            } finally {d.dispatchBoundsHookForCheck=boundsHook;}
            settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,29.92f);
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,45.38f);
            style.configure(settings);drain();style.finishFillFadesForCheck();
            LinearGradient faded=drawn(host.getBackground());
            check((faded.colors[1]>>>24)==116&&(faded.colors[1]&0xffffff)==(FluidCloudStyle.DEFAULT_BACKGROUND_COLOR&0xffffff),
                    "custom gradient colour keeps gradient opacity independent of the 29.92-percent base");
            settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT,false);style.configure(settings);drain();
            check(drawn(host.getBackground())==nativeShader[0],"floating scope opt-out restores the exact native black shader");
            settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT,true);settings.putBoolean(FluidCloudScope.Category.OTHER.key,false);
            style.configure(settings);drain();
            check(drawn(host.getBackground())==nativeShader[0],"business opt-out never keeps a previous custom shader colour");
            settings.putBoolean(FluidCloudScope.Category.OTHER.key,true);style.configure(settings);drain();style.finishFillFadesForCheck();
            check((drawn(host.getBackground()).colors[1]&0xffffff)==(FluidCloudStyle.DEFAULT_BACKGROUND_COLOR&0xffffff),
                    "re-enabling the selected business restores its chosen visible colour");
            style.setKeyguard(true);drain();
            check(drawn(host.getBackground())==nativeShader[0],"floating custom hue never replaces the independently controlled lockscreen/AOD material");
            style.setKeyguard(false);drain();style.finishFillFadesForCheck();
            d retained=leaf[0];settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,false);style.configure(settings);drain();
            check(drawn(host.getBackground())==nativeShader[0]&&!FluidCloudAppearance.nativeGradientOwned(retained)
                    &&retained.g.colors[1]==0xff000000,"turning base colour off restores both current and retained native drawable shaders");
            settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);style.configure(settings);drain();style.finishFillFadesForCheck();
            settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,false);settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,100f);
            style.configure(settings);drain();
            check(drawn(host.getBackground())==nativeShader[0],"clearing explicit default-colour editing restores exact native appearance");
        } finally {
            style.release();drain();FluidCloudAppearance.releaseNativeGradients();
            a.nativeBackgroundFactoryForCheck=null;a.style=null;a.controller=null;ViewRootManager.style=null;
        }
    }
    private static void nativeBaseAndGradientIndependence() throws Exception {
        Handler.postedForCheck.clear();FluidCloudAppearance.releaseNativeGradients();
        FluidCloudStyle style=new FluidCloudStyle();style.resolve(FluidCloudNativeGradientCheck.class.getClassLoader());
        a.style=style;a.controller=null;a.nativeBackgroundForCheck=null;ViewRootManager.style=style;
        final boolean[] resourceNames={true};
        android.content.res.Resources resources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){
                if(!resourceNames[0])throw new android.content.res.Resources.NotFoundException("No audited resource name");
                return id==1?"card_background_mix_color":id==2||id==102?"card_background_gradient_middle_color"
                        :id==101?"card_background_gradient_top_color":id==103?"card_background_gradient_bottom_color":"card_content_title_color";
            }
        };
        final d[] latest=new d[1];final LayerDrawable[] nativeRoot=new LayerDrawable[1];
        final BackgroundBlurDrawable[] blur=new BackgroundBlurDrawable[1];final LinearGradient[] originalShader=new LinearGradient[1];
        a.nativeBackgroundFactoryForCheck=(host,manager)->{
            // Do not inject preconstructed constant colours: the live OEM initializer obtains all three through hooked getColor first.
            int first=nativeResourceColor(style,host.getResources(),101,0x00000000);
            int middle=nativeResourceColor(style,host.getResources(),102,0xff000000);
            int bottom=nativeResourceColor(style,host.getResources(),103,0x05000000);
            latest[0]=new d(first,middle,bottom,new Triple<>(36f,1,3f));latest[0].setAlpha(173);
            blur[0]=new BackgroundBlurDrawable();nativeRoot[0]=new LayerDrawable(new Drawable[]{blur[0],latest[0]});
            nativeRoot[0].setBounds(13,17,413,217);originalShader[0]=latest[0].g;
            return nativeRoot[0];
        };
        try {
            CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(resources);card.addView(host);
            ViewRootManager manager=new ViewRootManager();a helper=new a(null,3);
            Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);
            settings.putString(FluidCloudStyle.BACKGROUND_MODE,"native");settings.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,29.92f);
            style.configure(settings);drain();helper.b(host,manager);drain();
            check(latest[0].a==0&&latest[0].b==0xff000000&&latest[0].c==0x05000000,"base opacity 29.92 percent never enters the three OEM gradient constructor colours");
            check(a.mixed==FluidCloudStyle.materialOpacity(0xb3262626,.2992f)&&near(manager.saved[7],(a.mixed>>>24)/255f),"the native blur material still receives the independent 29.92 percent base opacity once");
            check(drawn(host.getBackground())==originalShader[0]&&Arrays.equals(originalShader[0].colors,new int[]{0,0xff000000,0x05000000})
                    &&Arrays.equals(originalShader[0].positions,new float[]{0f,0f,1f}),"all default shape values preserve the exact OEM shader despite a translucent base");
            check(latest[0].getAlpha()==173&&host.getBackground()==nativeRoot[0]&&nativeRoot[0].getDrawable(0)==blur[0],"base opacity never scales the gradient owner's animated Paint alpha or replaces its blur sibling");

            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,45.38f);style.configure(settings);drain();
            LinearGradient faded=drawn(host.getBackground());
            check(latest[0].b==0xff000000&&(faded.colors[1]>>>24)==116,"gradient opacity 45.38 percent derives alpha 116 from native 255 rather than multiplying the base opacity into alpha 34");
            check(a.mixed==FluidCloudStyle.materialOpacity(0xb3262626,.2992f)&&latest[0].getAlpha()==173,"changing only gradient opacity leaves the independent blur material and animation alpha unchanged");

            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,100f);settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,true);
            settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff8c6844);style.configure(settings);drain();style.finishFillFadesForCheck();
            int customFill=Math.round(255f*.2992f)<<24|0x008c6844;
            check(a.mixed==FluidCloudStyle.materialFill(0xb3262626,customFill),"a customized native-mode base colour and opacity still tint the underlying material");
            LinearGradient selected=drawn(host.getBackground());
            check(latest[0].a==0&&latest[0].b==0xff000000&&latest[0].c==0x05000000
                    &&Arrays.equals(selected.colors,new int[]{0x008c6844,0xff8c6844,0x058c6844})
                    &&Arrays.equals(selected.positions,originalShader[0].positions),"explicit native-mode colour reaches the visible gradient while its OEM constructor recipe and alpha stay original");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,45.38f);style.configure(settings);drain();
            LinearGradient tintedFade=drawn(host.getBackground());
            check((tintedFade.colors[1]>>>24)==116&&(tintedFade.colors[1]&0xffffff)==0x8c6844
                    &&latest[0].b==0xff000000,"gradient opacity remains independent when the native-mode base has its own visible custom colour");

            // Missing resource names force the existing blur-parameter path and the conservative unsupported-drawable fallback.
            resourceNames[0]=false;settings.putBoolean(FluidCloudStyle.BACKGROUND_COLOR_CUSTOMIZED,false);
            settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,FluidCloudStyle.DEFAULT_BACKGROUND_COLOR);
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,100f);style.configure(settings);drain();
            check(near(manager.saved[7],(0xb3/255f)*.2992f),"unavailable resource names still apply base opacity through the native blur parameters");
            check(host.getBackground()==nativeRoot[0]&&nativeRoot[0].getDrawable(0)==blur[0]&&nativeRoot[0].getDrawable(1)==latest[0]
                    &&latest[0].getAlpha()==173&&drawn(host.getBackground())==originalShader[0],"opacity fallback never mutates util.d, its Paint alpha or its owning blur layer when resources were unavailable");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_OPACITY,45.38f);style.configure(settings);drain();
            check(latest[0].b==0xff000000&&(drawn(host.getBackground()).colors[1]>>>24)==116,"missing resource names retain independent gradient opacity without alpha contamination");

            resourceNames[0]=true;settings.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");settings.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff8c6844);
            style.configure(settings);drain();style.finishFillFadesForCheck();LinearGradient solid=drawn(host.getBackground());
            check(latest[0].b==customFill&&solid.colors.length==2&&solid.colors[0]==customFill&&solid.colors[1]==customFill,"explicit custom solid mode retains its existing ownership of gradient resources and final shader");
            FluidCloudAccent accent=new FluidCloudAccent();accent.resolve(FluidCloudNativeGradientCheck.class.getClassLoader());style.setAccentController(accent);a.controller=accent;
            try {
                final int media=0xff2468ac;android.view.ViewGroup mediaRoot=new android.view.ViewGroup(null);
                kotlinx.coroutines.flow.p flow=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(media));
                com.oplus.systemui.plugins.shared.template.section.media.s1 section=new com.oplus.systemui.plugins.shared.template.section.media.s1(null,
                        new com.oplus.systemui.plugins.shared.template.section.media.K0(flow),mediaRoot,null);
                accent.mediaChanged(section);settings.putBoolean(FluidCloudAccent.ENABLED,true);settings.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
                settings.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,false);settings.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
                accent.configure(settings);style.configure(settings);drain();style.finishFillFadesForCheck();
                check(latest[0].b==media&&drawn(host.getBackground())==originalShader[0],"active follow-media retains its prior resource tint and is not redefined by native-base gradient independence");
                card.addView(mediaRoot);accent.cardChanged(card);settings.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,false);style.configure(settings);drain();helper.b(host,manager);
                check(latest[0].b==media&&drawn(host.getBackground())==originalShader[0],"independent card media accent keeps its existing resource gradient and shader ownership");
            } finally {accent.release();drain();a.controller=null;}
        } finally {
            style.release();drain();FluidCloudAppearance.releaseNativeGradients();
            a.nativeBackgroundFactoryForCheck=null;a.style=null;a.controller=null;ViewRootManager.style=null;
        }
    }
    private static void policyAndRestore() throws Exception {
        Handler.postedForCheck.clear();
        FluidCloudStyle style = new FluidCloudStyle(); style.resolve(FluidCloudNativeGradientCheck.class.getClassLoader());
        a.style = style; a.controller = null; a.nativeBackgroundForCheck = null; ViewRootManager.style = style;
        final d[] latest = new d[1]; final Drawable[] blur = new Drawable[1];
        a.nativeBackgroundFactoryForCheck = (host, manager) -> {
            latest[0] = nativeShape(); blur[0] = new BackgroundBlurDrawable();
            LayerDrawable nativeBackground = new LayerDrawable(new Drawable[]{blur[0], latest[0]});
            nativeBackground.setBounds(13, 17, 413, 217); return nativeBackground;
        };
        try {
            CardView card = new CardView(); CardBackgroundView host = new CardBackgroundView(new android.content.res.Resources()); card.addView(host);
            ViewRootManager manager = new ViewRootManager(); a helper = new a(null, 3);
            helper.b(host, manager); d before = latest[0];
            Bundle settings = new Bundle(); settings.putBoolean(FluidCloudStyle.MASTER, true);
            settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND, true); settings.putString(FluidCloudStyle.BACKGROUND_MODE, "native");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 20f);
            host.setAlpha(.42f); host.setTranslationX(31f); host.setTranslationY(11f); host.setScaleX(.8f);
            style.configure(settings); drain(); style.finishFillFadesForCheck();
            check(latest[0] != before && near(drawn(host.getBackground()).alpha(13, 57), 5f / 255f),
                    "the real initializer completion applies the saved 20 percent range to OEM d");
            check(((LayerDrawable) host.getBackground()).getDrawable(0) == blur[0], "initializer completion retains its native hardware blur layer");
            check(host.getAlpha() == .42f && host.getTranslationX() == 31f && host.getTranslationY() == 11f && host.getScaleX() == .8f,
                    "saved gradient range leaves the expansion position, translation and animation alpha native");
            int builds = a.builds; LinearGradient fixed = latest[0].g;
            for (int i = 0; i < 1000; i++) { style.refreshAfterAccent(); drain(); }
            check(a.builds == builds && latest[0].g == fixed, "stable expanded-card frames do not rerun the native initializer or rebuild the shader");
            d firstAdjusted = latest[0];
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 50f); style.configure(settings); drain();
            check(fixed.positions.length == 15 && near(fixed.positions[13], .2f) && fixed.positions[14] == 1f,
                    "a previously drawn adjusted shader stays immutable while its old drawable is restored");
            check(Arrays.equals(firstAdjusted.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(firstAdjusted),
                    "previous native drawable references cannot keep a range lease after reinitialization");
            check(near(drawn(host.getBackground()).alpha(13, 67), 130f / 255f), "saved slider change from 20 to 50 percent reaches actual draw output");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 100f); style.configure(settings); drain();
            check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f}), "saved 100 percent restores the latest native background");
            settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 20f); style.configure(settings); drain();
            d scoped = latest[0];
            settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT, false); style.configure(settings); drain();
            check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f}), "disabling floating scope restores the native gradient");
            check(Arrays.equals(scoped.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(scoped),
                    "scope off restores and detaches the previous native drawable, including retained references");
            settings.putBoolean(FluidCloudStyle.SCOPE_FLOAT, true); style.configure(settings); drain();
            style.setKeyguard(true); drain();
            check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f}), "floating range does not override lockscreen/AOD appearance");
            style.setKeyguard(false); drain();
            d activeBeforeMasterOff = latest[0];
            settings.putBoolean(FluidCloudStyle.MASTER, false); style.configure(settings); drain();
            check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f}), "master off reruns the OEM initializer and restores native stops");
            check(Arrays.equals(activeBeforeMasterOff.g.positions, new float[]{0f, 0f, 1f})
                    && !FluidCloudAppearance.nativeGradientOwned(activeBeforeMasterOff), "master off also restores and detaches retained old background references");
            settings.putBoolean(FluidCloudStyle.MASTER, true); style.configure(settings); drain();
            check(near(drawn(host.getBackground()).alpha(13, 57), 5f / 255f), "re-enabling the same saved range uses a fresh native card background");
            d detached = latest[0]; host.attached = false;
            // StatusBarModule's exact CardBackgroundView.onDetachedFromWindow
            // lifecycle hook calls this same component event.
            style.detach(host);
            drain();
            check(Arrays.equals(detached.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(detached),
                    "detaching the expanded-card host restores and releases its native shader lease");
            host.attached = true;
            helper.b(host, manager); drain();
            check(near(drawn(host.getBackground()).alpha(13, 57), 5f / 255f), "reattachment initializes a current native gradient using the saved range");
            d parentDetached = latest[0]; card.attached = false;
            style.detach(card); drain();
            check(Arrays.equals(parentDetached.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(parentDetached),
                    "detaching the expanded-card parent also restores and releases its child's native shader lease");
            card.attached = true; helper.b(host, manager); drain();
            d beforeRelease = latest[0];
            style.release(); drain();
            check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f}), "module release restores native stops through the real initializer");
            check(Arrays.equals(beforeRelease.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(beforeRelease),
                    "release restores and detaches retained background references");
        } finally {
            a.nativeBackgroundFactoryForCheck = null; a.style = null; a.controller = null; ViewRootManager.style = null;
        }
    }
    /** A plugin discovery loader may delegate the actual class definition.
     * Method discovery must then still be usable by the scoped host callbacks;
     * this is a contract replay, not phone or GPU acceptance. */
    private static void discoveryLoaderPolicy() throws Exception {
        for (boolean delegated : new boolean[]{false, true}) {
            Handler.postedForCheck.clear();
            ClassLoader defining = CardBackgroundView.class.getClassLoader();
            ClassLoader discovery = delegated ? new ClassLoader(defining) { } : defining;
            String scene = delegated ? "delegating discovery loader" : "defining discovery loader";
            FluidCloudStyle style = new FluidCloudStyle(); FluidCloudAccent accent = new FluidCloudAccent();
            style.setAccentController(accent);
            FluidCloudStyle.Hooks styleAccess = style.resolve(discovery);
            FluidCloudAccent.Hooks accentAccess = accent.resolve(discovery);
            check(styleAccess.backgroundType == CardBackgroundView.class && accentAccess.backgroundType == CardBackgroundView.class,
                    scene + ": both controllers discover the actual native background class");
            check(styleAccess.background.getDeclaringClass() == a.class && styleAccess.backgroundType.getClassLoader() == defining,
                    scene + ": resolved method and host retain their defining identity");
            a.style = style; a.controller = accent; a.nativeBackgroundForCheck = null; ViewRootManager.style = style;
            final d[] latest = new d[1];
            a.nativeBackgroundFactoryForCheck = (view, manager) -> {
                latest[0] = nativeShape();
                LayerDrawable layers = new LayerDrawable(new Drawable[]{new BackgroundBlurDrawable(), latest[0]});
                layers.setBounds(13, 17, 413, 217); return layers;
            };
            try {
                CardView card = new CardView();
                CardBackgroundView host = new CardBackgroundView(new android.content.res.Resources()); card.addView(host);
                ViewRootManager manager = new ViewRootManager(); a helper = new a(null, 3);
                FluidCloudStyle.BackgroundScope styleScope = style.beginBackground(helper, host, manager);
                FluidCloudAccent.BackgroundScope accentScope = accent.beginBackground(helper, host, manager);
                if (styleScope != null) styleScope.close(); if (accentScope != null) accentScope.close();
                check(styleScope != null && accentScope != null,
                        scene + ": native callbacks recover both contracts by the actual host's defining loader");
                Bundle settings = new Bundle(); settings.putBoolean(FluidCloudStyle.MASTER, true);
                settings.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND, true);
                settings.putString(FluidCloudStyle.BACKGROUND_MODE, "native");
                settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 20f);
                host.setAlpha(.42f); host.setTranslationX(31f); host.setTranslationY(11f);
                style.configure(settings); drain(); helper.b(host, manager); drain();
                LinearGradient adjusted = drawn(host.getBackground()); d first = latest[0];
                check(ranged(adjusted.positions, .2f)
                        && near(adjusted.alpha(13, 57), 5f / 255f),
                        scene + ": saved range reaches actual native initializer completion and draw output");
                check(adjusted.colors[0] == first.a && adjusted.colors[1] == first.b && adjusted.colors[13] == first.c && adjusted.colors[14] == first.c,
                        scene + ": loader aliasing never changes resolved native ARGB");
                check(host.getAlpha() == .42f && host.getTranslationX() == 31f && host.getTranslationY() == 11f,
                        scene + ": callback lookup keeps native expansion alpha and position");
                settings.putFloat(FluidCloudStyle.BACKGROUND_GRADIENT_RANGE, 50f); style.configure(settings); drain();
                check(ranged(drawn(host.getBackground()).positions, .5f),
                        scene + ": later settings refresh reaches the registered native owner");
                check(Arrays.equals(first.g.positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(first),
                        scene + ": reconfiguration releases the previous owner's shader lease");
                d beforeOff = latest[0]; settings.putBoolean(FluidCloudStyle.MASTER, false); style.configure(settings); drain();
                check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f})
                        && Arrays.equals(beforeOff.g.positions, new float[]{0f, 0f, 1f}),
                        scene + ": disabling appearance restores current and retained native shaders");
                settings.putBoolean(FluidCloudStyle.MASTER, true); style.configure(settings); drain();
                d beforeRelease = latest[0];
                check(ranged(drawn(host.getBackground()).positions, .5f),
                        scene + ": re-enabling uses the same saved relative range");
                style.release(); drain();
                check(Arrays.equals(drawn(host.getBackground()).positions, new float[]{0f, 0f, 1f})
                        && Arrays.equals(beforeRelease.g.positions, new float[]{0f, 0f, 1f})
                        && !FluidCloudAppearance.nativeGradientOwned(beforeRelease),
                        scene + ": release restores the real native initializer and detaches old leases");
            } finally {
                style.release(); accent.release(); drain(); a.nativeBackgroundFactoryForCheck = null;
                a.style = null; a.controller = null; ViewRootManager.style = null;
            }
        }
    }
    private static void safety() {
        d source = nativeShape(); LinearGradient original = source.g;
        Drawable artwork = new Drawable() {
            public void draw(Canvas canvas) { } public void setAlpha(int value) { }
            public void setColorFilter(ColorFilter value) { } public int getOpacity() { return -3; }
        };
        LayerDrawable mixed = new LayerDrawable(new Drawable[]{source, artwork});
        check(FluidCloudAppearance.gradient(mixed, null, 20f) == mixed && source.g == original,
                "artwork mixed with an OEM-shaped drawable cannot accidentally acquire the base appearance range");
        // Device code161: "自定义纯色背景" looked identical to the native gradient because the OEM gradient was
        // never flattened. The solid mode now makes that gradient one colour, in place.
        check(FluidCloudAppearance.solid(source, null, 0xff123456) == source && source.g != original,
                "the custom solid mode recolours the OEM gradient in place");
        LinearGradient solid = drawn(source);
        check(solid.colors.length == 2 && solid.colors[0] == 0xff123456 && solid.colors[1] == 0xff123456, "one solid colour");
        check(FluidCloudAppearance.nativeGradientOwned(source), "the solid colour is a lease that can be released");
        FluidCloudAppearance.gradient(source, null, 100f);
        check(Arrays.equals(drawn(source).positions, new float[]{0f, 0f, 1f}) && !FluidCloudAppearance.nativeGradientOwned(source),
                "returning to the native look restores the OEM gradient");
    }
}
