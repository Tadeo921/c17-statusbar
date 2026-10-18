package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.oplus.systemui.plugins.seedling.capsule.ui.view.CapsuleView;
import com.oplus.systemui.plugins.shared.template.component.view.ContentSwitchView;
import com.oplus.systemui.plugins.shared.template.component.view.r;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Replays the audited native foreground, then verifies the real capsule text fade fix. Pixel arithmetic models
 * the blend equations only; native blur pixels are neither rendered nor claimed to be verified on a phone. */
public final class CapsuleTextFadeCheck {
    private static int checks;
    private static void check(boolean okay, String why) { checks++; if (!okay) throw new AssertionError(why); }
    private static void same(int expected, int actual, String why) {
        check(expected == actual, why + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual));
    }
    private static final ClassLoader LOADER = CapsuleTextFadeCheck.class.getClassLoader();

    private static final class Rig {
        final CapsuleView camera = new CapsuleView(new Context());
        final ContentSwitchView content = new ContentSwitchView(new Context());
        final r text = content.getCurrentView(), target = content.getTargetView();
        boolean wanted = true;
        final CapsuleTextFade fade = new CapsuleTextFade(view -> view == camera && wanted);
        Rig() {
            camera.addView(content);
            prepare(text); prepare(target);
            check(fade.resolve(LOADER) != null, "audited two-child template resolves");
        }
        void apply() { fade.capsuleChanged(camera); }
    }
    private static void prepare(r text) {
        text.setText("17:40–17:55", TextView.BufferType.NORMAL);
        text.g();
        check(text.getForeground() instanceof LayerDrawable, "native text overflows and creates its foreground");
        text.getForeground().setBounds(0, 0, text.getWidth(), text.getHeight());
    }
    private static LayerDrawable layers(r text) { return (LayerDrawable) text.getForeground(); }
    private static GradientDrawable nativeGradient(r text) { return (GradientDrawable) layers(text).getDrawable(0); }

    /** Captures the actual small saveLayer and its restore paint without changing the shared Canvas stub. */
    private static final class ProbeCanvas extends Canvas {
        PorterDuff.Mode restoreMode;
        int restoreAlpha, restoreCount;
        RectF maskBounds;
        @Override public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
            restoreMode = paint != null && paint.mode instanceof PorterDuffXfermode
                    ? ((PorterDuffXfermode) paint.mode).mode : PorterDuff.Mode.SRC_OVER;
            restoreAlpha = paint == null ? 255 : paint.getAlpha();
            maskBounds = new RectF(left, top, right, bottom);
            return super.saveLayer(left, top, right, bottom, paint);
        }
        @Override public void restoreToCount(int count) { restoreCount++; super.restoreToCount(count); }
    }

    private static void contract() throws Exception {
        CapsuleTextFade fade = new CapsuleTextFade(view -> true);
        check(fade.resolve(null) == null, "missing loader keeps native text");
        CapsuleTextFade.Hooks hooks = fade.resolve(LOADER);
        check(hooks != null && hooks.type == r.class, "text type is derived from the two ContentSwitchView children");
        Set<String> expected = new HashSet<>();
        for (String name : new String[]{"onAttachedToWindow", "onSizeChanged", "setFadingEdgeLength", "setCustomFadingEdgeEnabled", "setText"}) expected.add(name);
        check(hooks.updates.length == expected.size(), "only audited update points are hooked");
        for (Method method : hooks.updates) {
            check(method.getDeclaringClass() == r.class, "no View, ViewGroup, TextView or Chronometer framework hook");
            check(expected.remove(method.getName()), "update hooks are exact and unique");
            if ("setText".equals(method.getName()))
                check(method.getParameterTypes().length == 2 && method.getParameterTypes()[1] == TextView.BufferType.class,
                        "native setText hook uses the two-argument declaration");
        }
        check(expected.isEmpty(), "every required declared hook resolved");
        check(hooks.detached.getDeclaringClass() == r.class && "onDetachedFromWindow".equals(hooks.detached.getName()),
                "detach hook is on the plugin text class alone");
        ClassLoader denied = new ClassLoader(LOADER) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (CapsuleTextFade.CONTENT.equals(name)) throw new ClassNotFoundException("fixture changed plugin");
                return super.loadClass(name, resolve);
            }
        };
        check(new CapsuleTextFade(view -> true).resolve(denied) == null, "unavailable plugin structure falls back safely");
        fade.release();
        check(fade.resolve(LOADER) == null, "released owner cannot install again");
    }

    /** The minimised red signal: the native opaque foreground alters a grey backdrop at a pixel with no glyph ink. */
    private static int sourceOverBlack(int background, float alpha) {
        int result = 0xff000000;
        for (int shift : new int[]{16, 8, 0}) result |= Math.round((background >>> shift & 255) * (1f - alpha)) << shift;
        return result;
    }
    private static int textOver(int background, int textColor, float glyphAlpha) {
        int result = 0xff000000;
        for (int shift : new int[]{16, 8, 0}) {
            float channel = (textColor >>> shift & 255) * glyphAlpha + (background >>> shift & 255) * (1f - glyphAlpha);
            result |= Math.round(channel) << shift;
        }
        return result;
    }

    private static void nativeRedAndFixedComposition() {
        Rig rig = new Rig();
        LayerDrawable foreground = layers(rig.text);
        GradientDrawable gradient = nativeGradient(rig.text);
        GradientDrawable targetGradient = nativeGradient(rig.target);
        check(gradient.getOrientation() == GradientDrawable.Orientation.LEFT_RIGHT, "native fade runs horizontally");
        check(gradient.getColors()[0] == 0 && gradient.getColors()[1] == 0xff000000, "native end is opaque black");
        same(58, foreground.getLayerInsetLeft(0), "native fade is inset to the last 20 pixels of the text");
        same(20, gradient.getIntrinsicWidth(), "native fade width");
        same(14, gradient.getIntrinsicHeight(), "native fade height");
        for (int frame = 0; frame < 4; frame++) {
            rig.text.g();
            check(rig.text.getForeground() == foreground, "native defect persists on a stable frame");
            same(0xff000000, sourceOverBlack(0xff707070, 1f), "native opaque foreground produces a persistent black edge");
            check(sourceOverBlack(0xff707070, .5f) != 0xff707070, "native midpoint darkens the glass background");
        }
        rig.apply();
        check(rig.text.getForeground() == foreground && foreground instanceof LayerDrawable, "native foreground object/type are preserved");
        check(foreground.getDrawable(0) != gradient, "only the native fade child is replaced");
        check(layers(rig.target).getDrawable(0) != targetGradient && !(layers(rig.target).getDrawable(0) instanceof GradientDrawable),
                "both transition texts receive a fade mask");
        same(View.LAYER_TYPE_HARDWARE, rig.text.getLayerType(), "text gets a layer independent of capsule glass");
        same(View.LAYER_TYPE_NONE, rig.camera.getLayerType(), "camera glass parent gets no text layer");
        same(View.LAYER_TYPE_NONE, rig.content.getLayerType(), "content transition parent gets no text layer");
        ProbeCanvas canvas = new ProbeCanvas();
        foreground.draw(canvas);
        check(canvas.restoreMode == PorterDuff.Mode.DST_OUT, "real mask restore removes destination ink instead of painting black");
        same(255, canvas.restoreAlpha, "mask restore paint does not add an unintended alpha");
        same(1, canvas.layers, "one layer covers just the fade rectangle");
        same(1, canvas.restoreCount, "small mask layer is restored");
        check(canvas.maskBounds.left == 58f && canvas.maskBounds.right == 78f && canvas.maskBounds.top == 0f && canvas.maskBounds.bottom == 14f,
                "mask uses the exact native inset rectangle");
        same(255, gradient.seenAlpha, "wrapper draws the original gradient");
        for (int background : new int[]{0xff707070, 0xffe8d6bb, 0xff18487a, 0xff000000, 0xffffffff}) {
            for (float maskAlpha : new float[]{0f, .25f, .5f, .75f, 1f}) {
                same(background, textOver(background, 0xffffffff, 0f * (1f - maskAlpha)),
                        "without glyph ink the isolated DST_OUT leaves the complete backdrop unchanged");
                float ink = .8f * (1f - maskAlpha);
                int result = textOver(background, 0xffffffff, ink);
                check(maskAlpha < 1f || result == background, "right edge fades text to the original backdrop");
            }
        }
        rig.fade.release();
    }

    private static void nativeGeometryAndStableUpdates() {
        Rig rig = new Rig();
        LayerDrawable foreground = layers(rig.text);
        GradientDrawable gradient = nativeGradient(rig.text);
        int[] colors = gradient.getColors();
        gradient.setAlpha(117);
        gradient.setBounds(58, 2, 78, 12);
        Rect originalBounds = new Rect(gradient.getBounds());
        rig.fade.changed(rig.text);
        Drawable mask = foreground.getDrawable(0);
        same(117, mask.getAlpha(), "native alpha is retained by the wrapper");
        check(mask.getBounds().equals(originalBounds), "native mask bounds are retained");
        same(58, foreground.getLayerInsetLeft(0), "left inset retained");
        same(0, foreground.getLayerInsetRight(0), "right inset retained");
        same(0, foreground.getLayerInsetTop(0), "top inset retained");
        same(0, foreground.getLayerInsetBottom(0), "bottom inset retained");
        same(20, mask.getIntrinsicWidth(), "native intrinsic width retained");
        same(14, mask.getIntrinsicHeight(), "native intrinsic height retained");
        int builds = rig.text.nativeForegroundBuildsForCheck, writes = rig.text.nativeForegroundWritesForCheck;
        for (int frame = 0; frame < 80; frame++) {
            rig.text.g(); rig.fade.changed(rig.text); rig.fade.refresh();
            check(rig.text.getForeground() == foreground && foreground.getDrawable(0) == mask, "stable native/update/refresh retains all owned objects");
        }
        same(builds, rig.text.nativeForegroundBuildsForCheck, "LayerDrawable identity prevents native per-frame rebuilding");
        same(writes, rig.text.nativeForegroundWritesForCheck, "stable foreground is not rewritten");
        check(gradient.getColors()[0] == colors[0] && gradient.getColors()[1] == colors[1], "native gradient colours remain untouched");
        ProbeCanvas canvas = new ProbeCanvas();
        mask.draw(canvas);
        same(117, gradient.seenAlpha, "native partial alpha feeds the alpha mask exactly");
        mask.setAlpha(73);
        same(73, gradient.getAlpha(), "native alpha updates keep operating through the wrapper");
        mask.setBounds(61, 3, 83, 15);
        check(gradient.getBounds().equals(mask.getBounds()), "native bounds follow subsequent wrapper layout");
        rig.wanted = false; rig.fade.refresh();
        check(rig.text.getForeground() == foreground && foreground.getDrawable(0) == gradient, "off restores the same native objects");
        same(73, gradient.getAlpha(), "off retains later native alpha");
        check(gradient.getBounds().equals(new Rect(61, 3, 83, 15)), "off retains later native bounds");
        same(View.LAYER_TYPE_NONE, rig.text.getLayerType(), "off returns the borrowed layer");
        check(rig.text.attachListeners.isEmpty(), "off removes ownership listener");
        ProbeCanvas stale = new ProbeCanvas(); mask.draw(stale);
        same(0, stale.layers, "a stale wrapper does not continue erasing after off");
        rig.fade.release();
    }

    private static void lifecycleAndNativeReplacement() {
        Rig rig = new Rig();
        LayerDrawable first = layers(rig.text);
        GradientDrawable original = nativeGradient(rig.text);
        rig.apply();
        Drawable oldMask = first.getDrawable(0);
        rig.text.setForeground(null); rig.fade.changed(rig.text);
        check(rig.text.getForeground() == null, "native clearing the foreground remains authoritative");
        check(first.getDrawable(0) == original, "displaced native foreground is restored for reuse");
        same(View.LAYER_TYPE_NONE, rig.text.getLayerType(), "native clear returns the borrowed layer");
        ProbeCanvas stale = new ProbeCanvas(); oldMask.draw(stale);
        same(0, stale.layers, "displaced wrapper is inert");
        rig.text.g();
        LayerDrawable second = layers(rig.text);
        GradientDrawable secondGradient = nativeGradient(rig.text);
        check(second != first, "native g makes a new foreground when overflow reappears");
        rig.fade.changed(rig.text);
        check(second.getDrawable(0) != secondGradient, "replacement native foreground is masked");
        rig.text.setText("OK", TextView.BufferType.NORMAL); rig.text.g(); rig.fade.changed(rig.text);
        check(rig.text.getForeground() == null && second.getDrawable(0) == secondGradient, "non-overflow text stays native and releases ownership");
        rig.text.setText("17:40–17:55", TextView.BufferType.NORMAL); rig.text.g(); rig.fade.changed(rig.text);
        LayerDrawable third = layers(rig.text);
        Drawable thirdMask = third.getDrawable(0);
        rig.text.attached = false;
        for (View.OnAttachStateChangeListener listener : new java.util.ArrayList<>(rig.text.attachListeners)) listener.onViewDetachedFromWindow(rig.text);
        check(third.getDrawable(0) instanceof GradientDrawable, "detach restores native fade");
        same(View.LAYER_TYPE_NONE, rig.text.getLayerType(), "detach restores borrowed layer");
        check(rig.text.attachListeners.isEmpty(), "detach removes its listener");
        rig.fade.detached(rig.text);
        rig.text.attached = true; rig.text.onAttachedToWindow(); rig.fade.changed(rig.text);
        check(third.getDrawable(0) != thirdMask && !(third.getDrawable(0) instanceof GradientDrawable), "reattach installs a fresh mask on retained native foreground");
        ColorDrawable replacement = new ColorDrawable(0xff236789);
        rig.text.setForeground(replacement); rig.fade.release();
        check(rig.text.getForeground() == replacement, "release respects a later external foreground");
        check(third.getDrawable(0) instanceof GradientDrawable, "release restores displaced owned foreground too");
        check(rig.text.attachListeners.isEmpty() && rig.target.attachListeners.isEmpty(), "release removes all owners/listeners");
        rig.fade.changed(rig.text); rig.fade.capsuleChanged(rig.camera); rig.fade.refresh();
        check(rig.text.getForeground() == replacement, "released controller remains inactive");
    }

    private static void independentExistingLayers() {
        for (int nativeLayer : new int[]{View.LAYER_TYPE_SOFTWARE, View.LAYER_TYPE_HARDWARE}) {
            Rig rig = new Rig();
            Paint nativePaint = new Paint(); nativePaint.setAlpha(146);
            rig.text.setLayerType(nativeLayer, nativePaint);
            int writes = rig.text.layerTypeWrites;
            rig.apply();
            same(nativeLayer, rig.text.getLayerType(), "existing text layer is retained");
            check(rig.text.getLayerPaintForCheck() == nativePaint, "existing text layer paint object is retained");
            same(writes, rig.text.layerTypeWrites, "existing layer is not rewritten when adopting fade");
            rig.fade.restoreAll();
            same(nativeLayer, rig.text.getLayerType(), "existing layer survives restore");
            check(rig.text.getLayerPaintForCheck() == nativePaint && nativePaint.getAlpha() == 146, "restore preserves native layer paint and its alpha");
            same(writes, rig.text.layerTypeWrites, "existing layer paint is never overwritten");
            rig.fade.release();
        }
        Rig rig = new Rig(); rig.apply();
        Paint laterPaint = new Paint(); laterPaint.setAlpha(92);
        rig.text.setLayerType(View.LAYER_TYPE_SOFTWARE, laterPaint);
        rig.fade.restoreAll();
        same(View.LAYER_TYPE_SOFTWARE, rig.text.getLayerType(), "later external layer ownership survives restore");
        check(rig.text.getLayerPaintForCheck() == laterPaint && laterPaint.getAlpha() == 92, "later external layer paint survives restore");
        rig.fade.release();
    }

    private static void nativeBackgroundChange() {
        Rig rig = new Rig();
        LayerDrawable foreground = layers(rig.text);
        GradientDrawable gradient = nativeGradient(rig.text);
        rig.apply();
        ColorDrawable background = new ColorDrawable(0xff387ab2);
        rig.text.setBackground(background); rig.fade.refresh();
        check(rig.text.getBackground() == background && foreground.getDrawable(0) == gradient,
                "native text adopting its own background returns its original foreground");
        same(View.LAYER_TYPE_NONE, rig.text.getLayerType(), "new text background releases borrowed layer");
        check(rig.text.attachListeners.isEmpty(), "new text background removes owner listener");
        rig.fade.release();
    }

    private static void unsupportedAndOtherScenes() {
        for (int caseId = 0; caseId < 12; caseId++) {
            Rig rig = new Rig();
            LayerDrawable nativeForeground = layers(rig.text);
            GradientDrawable gradient = nativeGradient(rig.text);
            switch (caseId) {
                case 0: gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM); break;
                case 1: gradient.setColors(new int[]{0, 0xff112233}); break;
                case 2: gradient.setColors(new int[]{0x33000000, 0xff000000}); break;
                case 3: gradient.setColors(new int[]{0, 0x99000000}); break;
                case 4: gradient.setColors(new int[]{0, 0x33000000, 0xff000000}); break;
                case 5: gradient.setColor(0xff000000); break;
                case 6: gradient.setGradientType(GradientDrawable.RADIAL_GRADIENT); break;
                case 7: gradient.setShape(GradientDrawable.OVAL); break;
                case 8: gradient.setCornerRadius(3f); break;
                case 9: gradient.setCornerRadii(new float[]{1,1,1,1,1,1,1,1}); break;
                case 10: rig.text.setForeground(new LayerDrawable(new Drawable[]{gradient, new ColorDrawable(0xffffffff)})); break;
                case 11: rig.text.setBackground(new ColorDrawable(0xff345678)); break;
            }
            Drawable current = rig.text.getForeground();
            rig.fade.changed(rig.text);
            check(rig.text.getForeground() == current && nativeForeground.getDrawable(0) == gradient, "unrecognised foreground/background stays native, case " + caseId);
            same(View.LAYER_TYPE_NONE, rig.text.getLayerType(), "unsupported case borrows no layer, case " + caseId);
            check(rig.text.attachListeners.isEmpty(), "unsupported case creates no ownership listener");
            rig.fade.release();
        }
        Rig rig = new Rig();
        rig.wanted = false; rig.apply();
        check(layers(rig.text).getDrawable(0) instanceof GradientDrawable, "disabled camera predicate leaves native fade");
        rig.wanted = true;
        ViewGroup otherScene = new ViewGroup(new Context());
        rig.content.removeView(rig.text);
        otherScene.addView(rig.text);
        rig.fade.changed(rig.text);
        check(layers(rig.text).getDrawable(0) instanceof GradientDrawable, "same template outside camera scene stays native");
        TextView frameworkText = new TextView(new Context());
        GradientDrawable gradient = new GradientDrawable(); gradient.setColors(new int[]{0, 0xff000000});
        LayerDrawable frameworkForeground = new LayerDrawable(new Drawable[]{gradient});
        frameworkText.setForeground(frameworkForeground); rig.camera.addView(frameworkText); rig.fade.changed(frameworkText);
        check(frameworkForeground.getDrawable(0) == gradient, "framework TextView is never treated as audited native text");
        rig.fade.release();
    }

    private static final class CountedGroup extends ViewGroup {
        int reads;
        CountedGroup() { super(new Context()); }
        @Override public View getChildAt(int index) { reads++; return super.getChildAt(index); }
    }

    private static void boundedDiscovery() {
        Rig rig = new Rig();
        rig.camera.removeView(rig.content);
        CountedGroup wide = new CountedGroup(); rig.camera.addView(wide);
        for (int index = 0; index < 180; index++) wide.addView(new View(new Context()));
        wide.addView(rig.content);
        rig.fade.capsuleChanged(rig.camera);
        check(wide.reads <= 127, "one discovery reads at most the bounded 128-node budget");
        check(layers(rig.text).getDrawable(0) instanceof GradientDrawable, "text beyond discovery budget stays native");
        int reads = wide.reads;
        for (int frame = 0; frame < 80; frame++) rig.fade.refresh();
        same(reads, wide.reads, "stable refresh never rescans the hierarchy");
        Rig deep = new Rig(); deep.camera.removeView(deep.content);
        ViewGroup parent = deep.camera;
        for (int level = 0; level < 18; level++) { ViewGroup next = new ViewGroup(new Context()); parent.addView(next); parent = next; }
        parent.addView(deep.content); deep.fade.changed(deep.text);
        check(layers(deep.text).getDrawable(0) instanceof GradientDrawable, "unbounded ancestor lookup is avoided");
        rig.fade.release(); deep.fade.release();
    }

    private static void capsuleIntegration() {
        for (int option = 0; option < 3; option++) {
            CapsuleView camera = new CapsuleView(new Context());
            camera.getBackground().setBounds(0, 0, 200, 30);
            ContentSwitchView content = new ContentSwitchView(new Context()); camera.addView(content);
            prepare(content.b); prepare(content.c);
            LayerDrawable foreground = layers(content.b); GradientDrawable gradient = nativeGradient(content.b);
            FluidCloudCapsule capsule = new FluidCloudCapsule(null);
            check(capsule.resolve(LOADER) != null && capsule.resolveTextFade(LOADER) != null, "capsule and precise plugin text contract resolve together");
            Bundle config = new Bundle(); config.putBoolean(FluidCloudCapsule.MASTER, true);
            config.putBoolean(option == 0 ? FluidCloudCapsule.BLUR_ENABLED : option == 1 ? FluidCloudCapsule.FILL_ENABLED : FluidCloudCapsule.RADIUS_ENABLED, true);
            config.putInt(FluidCloudCapsule.FILL_COLOR, 0xff477a86);
            capsule.configure(config); capsule.changed(camera, Float.NaN);
            check(camera.getBackground() instanceof FluidCloudCapsule.PillBackground, "camera has the real custom background");
            check(foreground.getDrawable(0) != gradient && !(foreground.getDrawable(0) instanceof GradientDrawable), "background adoption discovers and masks both text children");
            ProbeCanvas canvas = new ProbeCanvas(); foreground.draw(canvas);
            check(canvas.restoreMode == PorterDuff.Mode.DST_OUT, "real camera controller installs the same erase mask");
            capsule.beforeDraw(camera);
            check(content.b.getForeground() == foreground, "stable capsule draw preserves native foreground object");
            capsule.configure(new Bundle());
            check(foreground.getDrawable(0) == gradient, "camera master off restores text immediately through the root controller");
            android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS + 1;
            capsule.beforeDraw(camera);
            capsule.release();
            check(foreground.getDrawable(0) == gradient, "capsule release restores native text fade");
            same(View.LAYER_TYPE_NONE, content.b.getLayerType(), "capsule release restores the borrowed text layer");
        }
    }

    public static void main(String[] ignored) throws Exception {
        contract(); nativeRedAndFixedComposition(); nativeGeometryAndStableUpdates(); lifecycleAndNativeReplacement();
        independentExistingLayers(); nativeBackgroundChange(); unsupportedAndOtherScenes(); boundedDiscovery(); capsuleIntegration();
        System.out.println("CapsuleTextFadeCheck: " + checks + " checks passed (audited native red signal, isolated DST_OUT, geometry, ownership, lifecycle, bounded discovery, real capsule integration)");
    }
}
