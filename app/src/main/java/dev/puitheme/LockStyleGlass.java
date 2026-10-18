// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * The lock-screen clock's milky glass material for the notification page's big clock: a rounded silhouette, a soft body that is
 * densest at the top, a bright rim lit from one side, an inner shade on the other side and a faint outer glow.
 *
 * The lock screen builds this with its own wallpaper-sampling shader inside the clock plugin. This one needs nothing but the
 * clock's own pixels: the content is blurred (that is what rounds the glyphs), and the blurred mask's edge, slope and depth
 * drive everything else. It approximates the look; it does not sample the wallpaper. Every part has its own strength, 100% being
 * the look tuned against the lock screen on the author's phone.
 */
final class LockStyleGlass {
    private static final String SHADER = ""
            + "uniform shader content;\n"
            + "uniform float uTop;\n"
            + "uniform float uHeight;\n"
            + "uniform float uBody;\n"
            + "uniform float uGradient;\n"
            + "uniform float uRim;\n"
            + "uniform float uRimDepth;\n"
            + "uniform float uShade;\n"
            + "uniform float uGlow;\n"
            + "uniform float2 uLight;\n"
            + "uniform float uStep;\n"
            + "half4 main(float2 p) {\n"
            + "  half4 c = content.eval(p);\n"
            + "  float a = clamp(float(c.a), 0.0, 1.0);\n"
            + "  float m = smoothstep(0.40, 0.60, a);\n"
            + "  float dx = float(content.eval(p + float2(uStep, 0.0)).a) - float(content.eval(p - float2(uStep, 0.0)).a);\n"
            + "  float dy = float(content.eval(p + float2(0.0, uStep)).a) - float(content.eval(p - float2(0.0, uStep)).a);\n"
            + "  float2 g = float2(dx, dy);\n"
            + "  float gl = length(g);\n"
            + "  float2 n = gl > 0.0001 ? -g / gl : float2(0.0, 0.0);\n"
            + "  float lit = clamp(dot(n, uLight) * 0.5 + 0.5, 0.0, 1.0);\n"
            + "  float vy = clamp((p.y - uTop) / max(uHeight, 1.0), 0.0, 1.0);\n"
            + "  float body = uBody * mix(1.0 + 0.30 * uGradient, 1.0 - 0.30 * uGradient, vy);\n"
            + "  float inner = 1.0 - smoothstep(0.50, 0.50 + max(uRimDepth, 0.02), a);\n"
            + "  float rim = m * inner * uRim * (0.30 + 0.70 * lit);\n"
            + "  float shade = m * inner * uShade * (1.0 - lit);\n"
            + "  float halo = (1.0 - m) * smoothstep(0.02, 0.40, a) * uGlow;\n"
            + "  float alpha = clamp(m * body + rim + halo, 0.0, 1.0);\n"
            + "  float3 tint = c.a > 0.001 ? float3(c.rgb) / float(c.a) : float3(1.0, 1.0, 1.0);\n"
            + "  float3 rgb = mix(tint, float3(0.55, 0.58, 0.64), clamp(shade, 0.0, 1.0));\n"
            + "  rgb = mix(rgb, float3(1.0, 1.0, 1.0), clamp(rim, 0.0, 1.0));\n"
            + "  return half4(half3(rgb * alpha), half(alpha));\n"
            + "}\n";

    /** Percentages: 100 is the tuned lock-screen look; the angle is the light's direction in degrees. */
    static final class Params {
        float body = 100f, gradient = 100f, rim = 100f, rimDepth = 100f, round = 100f, shade = 100f, glow = 100f, angle = 300f;
        boolean same(Params o) {
            return o != null && body == o.body && gradient == o.gradient && rim == o.rim && rimDepth == o.rimDepth && round == o.round
                    && shade == o.shade && glow == o.glow && angle == o.angle;
        }
        Params copy() {
            Params p = new Params();
            p.body = body; p.gradient = gradient; p.rim = rim; p.rimDepth = rimDepth; p.round = round; p.shade = shade; p.glow = glow; p.angle = angle;
            return p;
        }
    }

    private LockStyleGlass() { }

    static boolean supported() { return Build.VERSION.SDK_INT >= 33; }

    /** The blur that rounds the glyph, in pixels, for the given strength and density. */
    static float blurRadius(Params p, float density) { return Math.max(0f, p.round / 100f) * 5f * density; }

    /** One clock view's material: the compiled shader is kept and only its numbers change. */
    static final class Instance {
        private RuntimeShader shader;
        private RenderEffect blur;
        private Params applied;
        private float appliedDensity = -1f, appliedTop = Float.NaN, appliedHeight = Float.NaN;
        private boolean active;
        private final ClockMaterialMask colour=new ClockMaterialMask();
        private int appliedMask;
        private float appliedMaskOpacity,appliedTextAlpha=1f;
        private java.lang.ref.WeakReference<View> owner;

        /** glyphTop/glyphHeight are in the view's own pixels; returns true when the effect changed. */
        boolean apply(View view, Params p, float density, float glyphTop, float glyphHeight) {
            return apply(view,p,density,glyphTop,glyphHeight,0,0f,1f);
        }
        boolean apply(View view, Params p, float density, float glyphTop, float glyphHeight,
                int mask,float maskOpacity,float textAlpha) {
            if (view == null || p == null || !supported()) return false;
            // The height changes every frame while the clock shrinks; a couple of pixels never show in a gradient this soft.
            if (active && owner != null && owner.get() == view && p.same(applied) && density == appliedDensity
                    && Math.abs(glyphTop - appliedTop) < 2f
                    && Math.abs(glyphHeight - appliedHeight) < 2f&&mask==appliedMask
                    &&maskOpacity==appliedMaskOpacity&&textAlpha==appliedTextAlpha) return false;
            if (shader == null) shader = new RuntimeShader(SHADER);
            shader.setFloatUniform("uTop", glyphTop);
            shader.setFloatUniform("uHeight", glyphHeight);
            shader.setFloatUniform("uBody", .8125f * p.body / 100f);
            shader.setFloatUniform("uGradient", 1.4f * p.gradient / 100f);
            shader.setFloatUniform("uRim", .45f * p.rim / 100f);
            shader.setFloatUniform("uRimDepth", .21f * p.rimDepth / 100f);
            shader.setFloatUniform("uShade", .132f * p.shade / 100f);
            shader.setFloatUniform("uGlow", .14f * p.glow / 100f);
            double radians = Math.toRadians(p.angle);
            shader.setFloatUniform("uLight", (float) Math.cos(radians), (float) -Math.sin(radians));
            shader.setFloatUniform("uStep", Math.max(1f, density * 1.2f));
            RenderEffect shaderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content");
            float radius = blurRadius(p, density);
            RenderEffect next;
            if (radius < .5f) next = shaderEffect;
            else {
                if (applied == null || applied.round != p.round || density != appliedDensity || blur == null)
                    blur = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.DECAL);
                next = RenderEffect.createChainEffect(shaderEffect, blur);
            }
            view.setRenderEffect(colour.apply(next,mask,maskOpacity,textAlpha));
            appliedMask=mask;appliedMaskOpacity=maskOpacity;appliedTextAlpha=textAlpha;
            applied = p.copy(); appliedDensity = density; appliedTop = glyphTop; appliedHeight = glyphHeight; active = true;
            owner = new java.lang.ref.WeakReference<>(view);
            return true;
        }

        void clear(View view) {
            if (!active) return;
            active = false; applied = null;
            colour.clear();
            View last = owner == null ? null : owner.get(); owner = null;
            if (last != null && last != view && supported()) last.setRenderEffect(null);
            if (view != null && supported()) view.setRenderEffect(null);
        }
    }
}
