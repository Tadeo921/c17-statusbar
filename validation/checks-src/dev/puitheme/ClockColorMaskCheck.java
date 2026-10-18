package dev.puitheme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Numeric tests of the colour used by the original glyph drawing path.
 * They do not simulate SystemUI, GPU rendering or a device frame rate. */
public final class ClockColorMaskCheck {
    private static int checks;
    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    private static int expected(int text, int mask, float opacity, float amount) {
        double selected = Float.isFinite(opacity) ? Math.max(0d, Math.min(1d, opacity)) : 0d;
        double strength = Float.isFinite(amount) ? Math.max(0d, Math.min(1d, amount)) : 0d;
        double coverage = ((mask >>> 24) / 255d) * selected * strength;
        int result = text & 0xff000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int start = (text >>> shift) & 255, end = (mask >>> shift) & 255;
            int channel = (int) Math.floor(start + (end - start) * coverage + .5d);
            result |= channel << shift;
        }
        return result;
    }

    private static void zeroAndFull() {
        int text = 0x80402010;
        check(ClockColorMask.apply(text, 0xffffffff, 0f, 1f) == text, "zero opacity restores the exact original colour");
        check(ClockColorMask.apply(text, 0xffffffff, 1f, 0f) == text, "disabled/zero strength restores the exact original colour");
        check(ClockColorMask.apply(text, 0x00ffffff, 1f, 1f) == text, "a transparent custom mask cannot colour the text");
        check(ClockColorMask.apply(text, 0xffaabbcc, 1f, 1f) == 0x80aabbcc, "a full mask changes RGB and retains selected text alpha");
        check(ClockColorMask.apply(text, 0xff000000, 1f, 1f) == 0x80000000, "black is applied only when explicitly selected");
        check(ClockColorMask.apply(0x00402010, 0xffffffff, 1f, 1f) == 0x00ffffff, "transparent text stays transparent even under a full white mask");
        check(ClockColorMask.apply(0xff000000, 0xffffffff, .2f, 1f) == 0xff333333, "default white 20 percent tint uses one colour interpolation");
        check(ClockColorMask.apply(0x80ffffff, 0x80000000, .5f, .5f) == 0x80dfdfdf, "custom mask alpha, opacity and amount multiply exactly once");
    }

    private static void parameterBounds() {
        int text = 0x80604020, mask = 0xffabcdef;
        float[] invalid = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f};
        for (float value : invalid) {
            check(ClockColorMask.apply(text, mask, value, 1f) == text, "invalid opacity is inactive");
            check(ClockColorMask.apply(text, mask, 1f, value) == text, "invalid strength is inactive");
        }
        check(ClockColorMask.apply(text, mask, 2f, 3f) == 0x80abcdef, "out of range finite values clamp to full mask");
        check(ClockColorMask.apply(text, text, .64f, .33f) == text, "matching mask and text cannot create rounding drift");
    }

    private static void colourGrid() {
        int[] rgb = {0, 0x123456, 0x80a0c0, 0xff0000, 0x00ff00, 0x0000ff, 0xffffff};
        float[] selected = {0f, .2f, .5f, 1f};
        float[] strength = {0f, .25f, .5f, 1f};
        int[] textAlpha = {0, 1, 64, 128, 254, 255};
        for (int alpha : textAlpha) for (int original : rgb) for (int cover : rgb)
                for (int maskAlpha = 0; maskAlpha <= 255; maskAlpha += 17)
                for (float opacity : selected) for (float amount : strength) {
            int text = (alpha << 24) | original, mask = (maskAlpha << 24) | cover;
            int actual = ClockColorMask.apply(text, mask, opacity, amount);
            check(actual == expected(text, mask, opacity, amount), "independent colour/alpha grid");
            check((actual >>> 24) == alpha, "selected text alpha survives every mask colour and strength");
        }
    }

    private static void statelessBoundary() {
        for (Field field : ClockColorMask.class.getDeclaredFields())
            check(Modifier.isStatic(field.getModifiers()) && field.getType().isPrimitive(), "colour mixing cannot retain a View, Bitmap, Surface, Paint or engine");
        for (Method method : ClockColorMask.class.getDeclaredMethods()) {
            check(Modifier.isStatic(method.getModifiers()), "colour tool owns no runtime instance");
            check(method.getReturnType().isPrimitive(), "colour methods return primitive values");
            for (Class<?> parameter : method.getParameterTypes())
                check(parameter.isPrimitive(), "colour methods need no renderer or background object");
        }
        int stable = ClockColorMask.apply(0x6f123456, 0xa0ff00cc, .6f, .35f);
        for (int frame = 0; frame < 1000; frame++)
            check(ClockColorMask.apply(0x6f123456, 0xa0ff00cc, .6f, .35f) == stable, "stable frames are deterministic without accumulated alpha");
    }

    public static void main(String[] args) {
        zeroAndFull();
        parameterBounds();
        colourGrid();
        statelessBoundary();
        System.out.println("ClockColorMaskCheck: " + checks + " checks passed (pure colours; device rendering not executed)");
    }
}
