package dev.puitheme;

import android.os.Bundle;
import java.util.LinkedHashMap;
import java.util.Map;

/** AOD has a look of its own (opacity, mask, white base, gradient);  */
public final class AodLookCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError("AodLook " + checks + ": " + why); }
    private static void close(float expected, float actual, String why) { check(Math.abs(expected - actual) < .0005f, why + " (" + expected + " vs " + actual + ")"); }
    public static void main(String[] args) {
        Bundle options = new Bundle();
        options.putBoolean(FluidCloudAccent.ENABLED, true);
        options.putBoolean(FluidCloudAccent.LOCK_CUSTOM, true); options.putFloat(FluidCloudAccent.LOCK_OPACITY, 25f);
        options.putFloat(FluidCloudAccent.LOCK_LIGHT, 40f); options.putBoolean(FluidCloudAccent.LOCK_GRADIENT, true);
        options.putBoolean(FluidCloudAccent.AOD_CUSTOM, true); options.putFloat(FluidCloudAccent.AOD_OPACITY, 60f);
        options.putFloat(FluidCloudAccent.AOD_LIGHT, 10f); options.putBoolean(FluidCloudAccent.AOD_GRADIENT, false);
        options.putBoolean(FluidCloudAccent.MASK_ENABLED, false);
        FluidCloudAccent accent = new FluidCloudAccent();
        accent.configure(options);
        // The lit lock screen and AOD each read their own.
        close(.25f, accent.fillFactor(true, false), "lit lock opacity");
        close(.60f, accent.fillFactor(true, true), "AOD opacity");
        close(.40f, accent.lockLight(false), "lit lock white base"); close(.10f, accent.lockLight(true), "AOD white base");
        check(accent.lockGradient(false) && !accent.lockGradient(true), "gradient on when lit, off in AOD");
        close(.25f, accent.lockCustomOpacity(false), "custom opacity lit"); close(.60f, accent.lockCustomOpacity(true), "custom opacity AOD");
        // Not the lock screen at all: the shared opacity, whatever AOD says.
        close(1f, accent.fillFactor(false, true), "the shade keeps the shared opacity");
        // Each has its own mask.
        options.putBoolean(FluidCloudAccent.LOCK_CUSTOM, true); options.putInt(FluidCloudAccent.LOCK_MASK_COLOR, 0xff000000); options.putFloat(FluidCloudAccent.LOCK_MASK_OPACITY, 50f);
        options.putInt(FluidCloudAccent.AOD_MASK_COLOR, 0xffffffff); options.putFloat(FluidCloudAccent.AOD_MASK_OPACITY, 50f);
        accent.configure(options);
        int media = 0xff3366cc;
        check(accent.maskedMediaColor(media, true, false) == FluidCloudAccent.mask(media, 0xff000000, 50f), "lit lock mask");
        check(accent.maskedMediaColor(media, true, true) == FluidCloudAccent.mask(media, 0xffffffff, 50f), "AOD mask");
        check(!accent.maskedMediaColor(media, true, true).equals(accent.maskedMediaColor(media, true, false)), "the two masks differ");
        // Unify style is about the notifications only: both looks stay what the user set.
        options.putBoolean(LockNotificationSettings.UNIFY_AOD, true);
        accent.configure(options);
        close(.25f, accent.fillFactor(true, false), "unified: the lit opacity is still its own"); close(.60f, accent.fillFactor(true, true), "unified: AOD keeps its own opacity");
        close(.40f, accent.lockLight(false), "unified: lit white base"); close(.10f, accent.lockLight(true), "unified: AOD white base");
        check(accent.lockGradient(false) && !accent.lockGradient(true), "unified: the gradients stay apart");
        check(accent.maskedMediaColor(media, true, true) == FluidCloudAccent.mask(media, 0xffffffff, 50f), "unified: AOD keeps its mask");
        options.putBoolean(LockNotificationSettings.UNIFY_AOD, false);
        accent.configure(options);
        close(.60f, accent.fillFactor(true, true), "switching it off changes nothing for AOD");
        // Defaults: AOD starts as the lit lock screen's defaults, so a fresh install looks the same in both.
        FluidCloudAccent fresh = new FluidCloudAccent(); Bundle on = new Bundle(); on.putBoolean(FluidCloudAccent.ENABLED, true); fresh.configure(on);
        close(fresh.lockLight(false), fresh.lockLight(true), "default white base"); close(fresh.fillFactor(true, false), fresh.fillFactor(true, true), "default opacity");
        check(!fresh.lockGradient(true) && !fresh.lockGradient(false), "gradient off by default");
        // An upgrade copies what the lit lock screen had onto AOD's keys (only the ones that are new), leaving the rest.
        Map<String, Object> saved = new LinkedHashMap<>();
        saved.put(FluidCloudAccent.LOCK_CUSTOM, true); saved.put(FluidCloudAccent.LOCK_OPACITY, 33f); saved.put(FluidCloudAccent.LOCK_LIGHT, 12f);
        saved.put(FluidCloudAccent.LOCK_MASK_COLOR, 0x80112233); saved.put(StatusBarSettings.alphaKey(FluidCloudAccent.LOCK_MASK_COLOR), true);
        saved.put(FluidCloudStyle.NOTIFICATION_FOLLOW_OPACITY, 55f);
        Map<String, Object> target = new LinkedHashMap<>(Upgrade118.DEFAULTS);
        Upgrade118.carryLockLook(target, saved);
        check(Boolean.TRUE.equals(target.get(FluidCloudAccent.AOD_CUSTOM)), "custom copied");
        check(Float.valueOf(33f).equals(target.get(FluidCloudAccent.AOD_OPACITY)) && Float.valueOf(12f).equals(target.get(FluidCloudAccent.AOD_LIGHT)), "opacity and white base copied");
        check(Integer.valueOf(0x80112233).equals(target.get(FluidCloudAccent.AOD_MASK_COLOR)) && Boolean.TRUE.equals(target.get(StatusBarSettings.alphaKey(FluidCloudAccent.AOD_MASK_COLOR))), "mask colour and its transparency switch copied");
        check(Float.valueOf(55f).equals(target.get(LockNotificationSettings.AOD_NOTIFICATION_FOLLOW_OPACITY)), "notification strength copied");
        check(Boolean.FALSE.equals(target.get(FluidCloudAccent.AOD_GRADIENT)), "what the lit lock screen did not have stays the default");
        System.out.println("AodLookCheck passed " + checks + " checks");
    }
}
