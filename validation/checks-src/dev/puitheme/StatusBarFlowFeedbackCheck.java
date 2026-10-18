package dev.puitheme;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Native animation and native measurement must survive removing a relative gap. */
public final class StatusBarFlowFeedbackCheck {
    private static int checks;
    private static void near(float expected, float actual, String why) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(expected - actual) > .02f)
            throw new AssertionError(why + ": " + expected + " != " + actual);
    }
    private static Object field(Object owner, String name) throws Exception {
        Field value = owner.getClass().getDeclaredField(name); value.setAccessible(true); return value.get(owner);
    }
    private static float number(Object owner, String name) throws Exception { return ((Number) field(owner, name)).floatValue(); }
    private static void set(Object owner, String name, float value) throws Exception {
        Field target = owner.getClass().getDeclaredField(name); target.setAccessible(true); target.setFloat(owner, value);
    }
    private static Object rig() throws Exception {
        Class<?> fixture = Class.forName("dev.puitheme.StatusBarFlowLayoutCheck$Rig");
        Constructor<?> constructor = fixture.getDeclaredConstructor(int.class, int.class); constructor.setAccessible(true);
        return constructor.newInstance(0, 300);
    }
    private static void pass(Object rig) throws Exception {
        Method method = rig.getClass().getDeclaredMethod("pass"); method.setAccessible(true); method.invoke(rig);
    }
    private static Bundle gap() {
        Bundle result = new Bundle(); result.putFloat(StatusBarLayoutSettings.key("clock", "margin_left"), 12); return result;
    }
    private static void animationOnDisable() throws Exception {
        Object rig = rig(); StatusBarFlowLayout flow = (StatusBarFlowLayout) field(rig, "flow");
        flow.configure(gap()); pass(rig);
        Object wifi = field(rig, "wifi"), state = field(wifi, "state");
        float nativeX = number(rig, "wifiNativeX"), target = number(state, "x");
        set(state, "x", target + 9f); // An OEM animator moves its existing planned target.
        flow.beforeApply(state, (View) wifi);
        flow.configure(new Bundle());
        near(nativeX + 9f, number(state, "x"), "removing a gap removes its target basis while retaining the native animator residual");
    }
    private static void measurementOnDisable() throws Exception {
        StatusBarSharedSlotFlowCheck.measurementRegression();
        checks++;
    }
    private static void untouchedNativeHosts() throws Exception {
        Object rig = rig(); StatusBarFlowLayout flow = (StatusBarFlowLayout) field(rig, "flow");
        ViewGroup left = (ViewGroup) field(rig, "left"), right = (ViewGroup) field(rig, "right");
        ViewGroup icons = (ViewGroup) field(rig, "icons");
        int leftStart = left.getLeft(), leftEnd = left.getRight(), rightStart = right.getLeft(), rightEnd = right.getRight();
        int iconsStart = icons.getLeft(), iconsEnd = icons.getRight();
        flow.configure(gap()); pass(rig);
        near(leftStart, left.getLeft(), "a relative clock gap that fits never moves its native side host");
        near(leftEnd, left.getRight(), "a relative clock gap that fits never shrinks its native side host");
        near(rightStart, right.getLeft(), "a clock-only gap leaves the unrelated right side host origin native");
        near(rightEnd, right.getRight(), "a clock-only gap leaves the unrelated right side host extent native");
        near(iconsStart, icons.getLeft(), "a clock-only gap leaves the unrelated icon container origin native");
        near(iconsEnd, icons.getRight(), "a clock-only gap leaves the unrelated icon container extent native");
    }
    private static float screenX(View view) {
        float result = 0;
        for (View current = view; current != null; current = current.getParent() instanceof View ? (View) current.getParent() : null)
            result += current.getLeft() + current.getTranslationX();
        return result;
    }
    private static void signedMargins() throws Exception {
        for (int direction : new int[]{View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL}) {
            Object rig = rig(); StatusBarFlowLayout flow = (StatusBarFlowLayout) field(rig, "flow");
            ViewGroup root = (ViewGroup) field(rig, "root"); View clock = (View) field(rig, "clock");
            Field nativeDirection = root.getClass().getDeclaredField("direction"); nativeDirection.setAccessible(true); nativeDirection.setInt(root, direction);
            clock.setTranslationX(4); root.setTranslationX(-2); clock.setAlpha(.6f);
            Bundle negative = new Bundle(); negative.putBoolean(StatusBarLayoutSettings.MASTER, true);
            negative.putString(StatusBarLayoutSettings.key("clock", "side"), "left");
            negative.putFloat(StatusBarLayoutSettings.key("clock", "margin_left"), -12);
            flow.configure(negative); pass(rig);
            near(-10, screenX(clock), "negative physical left margin remains negative under LTR/RTL and preserves native translations");
            near(4, clock.getTranslationX(), "negative margin remains real layout rather than an extra clock translation");
            near(.6f, clock.getAlpha(), "negative margin never rewrites native animated alpha");
            negative.putString(StatusBarLayoutSettings.key("clock", "side"), "right");
            negative.putFloat(StatusBarLayoutSettings.key("clock", "margin_left"), 0);
            negative.putFloat(StatusBarLayoutSettings.key("clock", "margin_right"), 0);
            flow.configure(negative); pass(rig); float rightBaseline = screenX(clock);
            negative.putFloat(StatusBarLayoutSettings.key("clock", "margin_right"), -8);
            flow.configure(negative); pass(rig);
            near(rightBaseline + 8, screenX(clock), "negative physical right margin moves the right-anchored clock right in both layout directions");
            flow.configure(new Bundle());
            near(2, screenX(clock), "returning signed margins to zero restores native geometry and translations");
        }
        Object standalone = rig(); StatusBarFlowLayout flow = (StatusBarFlowLayout) field(standalone, "flow");
        Bundle negativeOnly = new Bundle(); negativeOnly.putFloat(StatusBarLayoutSettings.key("clock", "margin_left"), -12);
        flow.configure(negativeOnly); pass(standalone);
        near(-12, screenX((View) field(standalone, "clock")), "a negative standalone gap activates layout even when sorting is disabled");
        near(1, flow.wanted() ? 1 : 0, "negative-only settings are active rather than clamped to zero");
        flow.configure(new Bundle()); near(0, screenX((View) field(standalone, "clock")), "zero standalone gap restores native clock position");
    }
    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 1 && arguments[0].equals("animation")) animationOnDisable();
        else if (arguments.length == 1 && arguments[0].equals("measurement")) measurementOnDisable();
        else if (arguments.length == 1 && arguments[0].equals("hosts")) untouchedNativeHosts();
        else if (arguments.length == 1 && arguments[0].equals("signed")) signedMargins();
        else { animationOnDisable(); measurementOnDisable(); untouchedNativeHosts(); signedMargins(); }
        System.out.println("StatusBarFlowFeedbackCheck: " + checks + " checks passed");
    }
}
