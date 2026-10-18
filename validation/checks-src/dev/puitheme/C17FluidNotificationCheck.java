package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.View;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.posteffect.drawable.BlendDrawable;

/** Exact body policy: one colour layer for every notification card, optics ownership, media follow, identity recycling. */
public final class C17FluidNotificationCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    private static Paint draw(C17HighlightRemoval control, View body, Canvas canvas) throws Throwable {
        int[] nativeCalls = {0};
        control.drawNotificationBackground(body, canvas, target -> { nativeCalls[0]++; return null; });
        check(nativeCalls[0] == 1, "native background draw retained once");
        return canvas.lastLayerPaint;
    }
    private static int tint(Paint paint) {
        return paint == null ? 0 : ((PorterDuffColorFilter) paint.getColorFilter()).getColor();
    }
    public static void main(String[] args) throws Throwable {
        C17HighlightRemoval control = new C17HighlightRemoval();
        NotificationBackgroundView fluid = new NotificationBackgroundView();
        NotificationBackgroundView normal = new NotificationBackgroundView();
        BlendDrawable engine = new BlendDrawable();
        C17HighlightRemovalCheck.NativeMaterial material = new C17HighlightRemovalCheck.NativeMaterial();
        C17HighlightRemovalCheck.HookedShader shader = new C17HighlightRemovalCheck.HookedShader(control);
        material.shader = shader;
        for (int i = 0; i < material.optics.values.length; i++) material.optics.values[i] = .1f * (i + 1);
        QsTileAppearance.setField(engine, "drawableShader", material);
        try (C17HighlightRemoval.SurfaceScope scope = control.beginSurface(fluid)) { control.bindDrawable(engine); }
        control.setFluidNotificationBackground(fluid, true);
        check(draw(control, fluid, new Canvas()) == null, "registered fluid body keeps its native material while no colour policy is active");
        check(draw(control, normal, new Canvas()) == null, "unregistered regular notification keeps its native material");
        NotificationBackgroundView banner = new NotificationBackgroundView();
        banner.parent = new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();
        control.setFluidNotificationBackground(banner, true);
        check(draw(control, banner, new Canvas()) == null, "a heads-up banner never takes the notification colour layer");
        int invalidations = fluid.invalidations;
        for (int i = 0; i < 1000; i++) control.setFluidNotificationBackground(fluid, true);
        check(fluid.invalidations == invalidations, "identical policy reuses its state");
        Bundle custom = new Bundle(); custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, true);
        custom.putInt(C17HighlightRemoval.LIGHT_BACKGROUND, 0xb2112233);
        control.configure(custom);
        check(tint(draw(control, fluid, new Canvas())) == 0xb2112233, "unified colour covers the registered fluid body");
        check(tint(draw(control, normal, new Canvas())) == 0xb2112233, "and every ordinary notification the same way");
        check(draw(control, banner, new Canvas()) == null, "heads-up banners keep their own material under the unified colour");
        control.setFluidNotificationBackground(fluid, false);
        check(tint(draw(control, fluid, new Canvas())) == 0xb2112233, "fluid follow OFF does not switch the unified colour off");
        control.setFluidNotificationBackground(fluid, true, true);
        check(tint(draw(control, fluid, new Canvas())) == 0xb2112233, "a music card that keeps its highlight still gets the unified colour");
        android.os.SystemClock.uptime = 1000;
        control.setMediaFollowColor(0x66abcdef);
        // The colour crosses in rather than popping: at the start of the fade nothing of it is shown yet.
        check(tint(draw(control, normal, new Canvas())) == 0xb2112233 || draw(control, normal, new Canvas()) != null && (tint(draw(control, normal, new Canvas())) >>> 24) < 0x66,
                "the media colour starts from nothing instead of popping in");
        android.os.SystemClock.uptime = 1000 + C17HighlightRemoval.FOLLOW_FADE_MS / 2;
        int half = tint(draw(control, normal, new Canvas()));
        check((half & 0xffffff) == 0xabcdef && (half >>> 24) > 0 && (half >>> 24) < 0x66, "half way through the fade it is half shown");
        control.finishFollowFadeForCheck();
        check(tint(draw(control, normal, new Canvas())) == 0x66abcdef, "ordinary notifications follow the media colour ahead of the acrylic pair");
        check(tint(draw(control, fluid, new Canvas())) == 0xb2112233, "a fluid-cloud card never takes the follow colour: its own colour comes from the fluid-cloud policy");
        check(draw(control, banner, new Canvas()) == null, "media follow does not recolour heads-up banners either");
        custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, false); control.configure(custom);
        check(tint(draw(control, normal, new Canvas())) == 0x66abcdef, "media follow works without the unified colour switch");
        check(draw(control, fluid, new Canvas()) == null, "and leaves the fluid-cloud card native");
        control.setMediaFollowColor(null);
        android.os.SystemClock.uptime += 10;
        check(tint(draw(control, normal, new Canvas())) != 0 && (tint(draw(control, normal, new Canvas())) & 0xffffff) == 0xabcdef, "stopping the follow fades the colour out in place");
        control.finishFollowFadeForCheck();
        check(draw(control, normal, new Canvas()) == null && draw(control, fluid, new Canvas()) == null, "stopping the follow restores the native material");
        custom.putBoolean(C17HighlightRemoval.ENABLED, true); custom.putBoolean(C17HighlightRemoval.BACKGROUND_ENABLED, true);
        control.configure(custom);
        control.setFluidNotificationBackground(fluid, true, false);
        check(shader.floats.get("u_opticsArray")[3] == 0f, "an owned fluid body takes the C17 optical removal");
        control.setFluidNotificationBackground(fluid, true, true);
        check(shader.floats.get("u_opticsArray")[3] == material.optics.values[3], "a music card that keeps its highlight restores the native optics");
        control.setFluidNotificationBackground(fluid, true, false);
        check(shader.floats.get("u_opticsArray")[3] == 0f, "keep OFF removes the highlight again");
        control.setFluidNotificationBackground(fluid, false);
        check(shader.floats.get("u_opticsArray")[3] == material.optics.values[3], "follow OFF restores owned native optical slots");
        control.forgetFluidNotificationBackground(fluid);
        check(shader.floats.get("u_opticsArray")[3] == 0f, "recycled body resumes ordinary C17 policy, no stale false gate");
        control.setFluidNotificationBackground(fluid, true); control.detach(fluid);
        control.configure(new Bundle());
        check(draw(control, fluid, new Canvas()) == null, "detach removes fluid ownership");
        // code98: a fluid-cloud card may carry its media colour as a gradient layer over its own material.
        control.setFluidNotificationBackground(fluid, true);
        control.setFluidGradient(fluid, 0x80123456);
        // code100: a new colour fades in over COLOUR_FADE_MS (smoothstep) instead of popping.
        Canvas first = new Canvas(); draw(control, fluid, first);
        check(first.rectFills == 0, "at the first frame the new layer is still transparent");
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS / 2;
        Canvas middle = new Canvas(); draw(control, fluid, middle);
        int halfAlpha = middle.lastFillShader instanceof android.graphics.LinearGradient
                ? ((android.graphics.LinearGradient) middle.lastFillShader).colors[0] >>> 24 : -1;
        check(middle.rectFills == 1 && halfAlpha > 0x20 && halfAlpha < 0x60, "half way through the fade the layer is half shown");
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS;
        Canvas layered = new Canvas();
        Paint layerPaint = draw(control, fluid, layered);
        check(layerPaint != null && layerPaint.getColorFilter() == null, "the gradient rides on a layer that changes nothing by itself");
        check(layered.rectFills == 1 && layered.lastFillShader instanceof android.graphics.LinearGradient, "one gradient rectangle over the native draw");
        android.graphics.LinearGradient shaderUsed = (android.graphics.LinearGradient) layered.lastFillShader;
        check(shaderUsed.colors[0] == 0x80123456 && shaderUsed.colors[1] == 0x00123456 && shaderUsed.left == shaderUsed.right && shaderUsed.top < shaderUsed.bottom,
                "the colour at the top fading to nothing at the bottom");
        check(draw(control, normal, new Canvas()) == null && draw(control, banner, new Canvas()) == null, "other cards and banners are not touched");
        control.setFluidGradient(banner, 0x80123456);
        check(draw(control, banner, new Canvas()) == null, "a heads-up banner never takes the layer");
        int sets = fluid.invalidations;
        for (int i = 0; i < 100; i++) control.setFluidGradient(fluid, 0x80123456);
        check(fluid.invalidations == sets, "an unchanged layer does not invalidate the card");
        custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, true); custom.putInt(C17HighlightRemoval.LIGHT_BACKGROUND, 0xb2112233);
        control.configure(custom);
        Canvas both = new Canvas();
        check(tint(draw(control, fluid, both)) == 0xb2112233 && both.rectFills == 1, "the unified colour and the gradient layer work together");
        control.setFluidGradient(fluid, null);
        Canvas leaving = new Canvas(); draw(control, fluid, leaving);
        check(leaving.rectFills == 1, "taking the layer off fades it out instead of cutting it");
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS;
        Canvas none = new Canvas();
        check(tint(draw(control, fluid, none)) == 0xb2112233 && none.rectFills == 0, "taking the layer off leaves the unified colour only");
        custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, false); control.configure(custom);
        check(draw(control, fluid, new Canvas()) == null, "and nothing at all when that is off too");
        control.setFluidGradient(fluid, 0x80123456); control.forgetFluidNotificationBackground(fluid);
        check(draw(control, fluid, new Canvas()) == null, "a recycled body forgets its layer");
        // code99: the fill the platform material did not take is drawn over the card as one flat colour while its gate says so.
        control.setFluidNotificationBackground(fluid, true);
        boolean[] needed = {false};
        java.util.function.BooleanSupplier gate = () -> needed[0];
        control.setFluidOverlay(fluid, 0x80abcdef, gate);
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS;
        Canvas closed = new Canvas();
        check(draw(control, fluid, closed) == null && closed.rectFills == 0, "gate closed: the native draw is untouched");
        needed[0] = true;
        Canvas open = new Canvas();
        Paint openPaint = draw(control, fluid, open);
        check(openPaint != null && openPaint.getColorFilter() == null, "gate open: a layer that changes nothing by itself");
        check(open.rectFills == 1 && open.lastRectColor == 0x80abcdef && open.lastRectMode instanceof android.graphics.PorterDuffXfermode,
                "one flat rectangle of the fill, only where the native draw left pixels");
        check(draw(control, normal, new Canvas()) == null && draw(control, banner, new Canvas()) == null, "other cards and banners are not touched");
        control.setFluidOverlay(banner, 0x80abcdef, gate);
        check(draw(control, banner, new Canvas()) == null, "a heads-up banner never takes the overlay");
        int invalidated = fluid.invalidations;
        for (int i = 0; i < 100; i++) control.setFluidOverlay(fluid, 0x80abcdef, gate);
        check(fluid.invalidations == invalidated, "an unchanged overlay does not invalidate the card");
        control.setFluidOverlay(fluid, 0x80112233, gate);
        Canvas crossing = new Canvas(); draw(control, fluid, crossing);
        check(crossing.lastRectColor == 0x80abcdef, "a changed colour starts from the colour on the screen");
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS / 2;
        Canvas between = new Canvas(); draw(control, fluid, between);
        check(between.lastRectColor != 0x80abcdef && between.lastRectColor != 0x80112233 && (between.lastRectColor >>> 24) == 0x80,
                "half way it is a blend of the two at the same opacity");
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS;
        int before = fluid.invalidations;
        Canvas changed = new Canvas(); draw(control, fluid, changed);
        check(fluid.invalidations == before && changed.lastRectColor == 0x80112233, "after the fade the new colour is drawn and no more frames are asked for");
        custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, true); custom.putInt(C17HighlightRemoval.LIGHT_BACKGROUND, 0xb2112233);
        control.configure(custom);
        Canvas together = new Canvas();
        check(tint(draw(control, fluid, together)) == 0xb2112233 && together.rectFills == 1, "the unified colour and the overlay work together");
        needed[0] = false;
        Canvas unified = new Canvas();
        check(tint(draw(control, fluid, unified)) == 0xb2112233 && unified.rectFills == 0, "the gate closing leaves the unified colour only");
        needed[0] = true;
        control.setFluidOverlay(fluid, null, gate);
        android.os.SystemClock.uptime += C17HighlightRemoval.COLOUR_FADE_MS;
        check(tint(draw(control, fluid, new Canvas())) == 0xb2112233 && draw(control, fluid, new Canvas()).getColorFilter() != null, "taking the overlay off leaves the unified colour only");
        control.setFluidOverlay(fluid, 0x80abcdef, null);
        Canvas noGate = new Canvas(); draw(control, fluid, noGate);
        check(noGate.rectFills == 0, "a overlay without its gate draws nothing");
        custom.putBoolean(C17HighlightRemoval.UNIFORM_NOTIFICATION_ENABLED, false); control.configure(custom);
        control.setFluidOverlay(fluid, 0x80abcdef, gate); control.forgetFluidNotificationBackground(fluid);
        check(draw(control, fluid, new Canvas()) == null, "a recycled body forgets its overlay");
        control.setFluidOverlay(fluid, 0x80abcdef, gate);
        control.setFluidGradient(fluid, 0x80123456);
        control.setMediaFollowColor(0x66abcdef); control.release();
        check(draw(control, fluid, new Canvas()) == null && draw(control, normal, new Canvas()) == null, "release cannot retain fluid or media material");
        System.out.println("C17FluidNotificationCheck: " + checks + " checks passed");
    }
}
