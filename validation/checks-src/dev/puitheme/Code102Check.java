package dev.puitheme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** code102, the first step of the power and smoothness work: hooks on framework methods that every view or every resource read of
 * SystemUI passes are installed only while the feature that needs them is in effect. */
public final class Code102Check {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }

    /** The appear bounce asks for the resource hook only when it changes something: on, and not the system's own 100 %. */
    private static void bounceWanted() {
        CapsuleEffects effects = new CapsuleEffects();
        check(!effects.active(), "nothing configured: no hook");
        android.os.Bundle settings = new android.os.Bundle();
        settings.putBoolean(FluidCloudCapsule.MASTER, true); settings.putBoolean(CapsuleEffects.BOUNCE_ENABLED, true);
        settings.putFloat(CapsuleEffects.BOUNCE, 100f); effects.configure(settings);
        check(!effects.active(), "on at 100 %: the system's own bounce, no hook");
        settings.putFloat(CapsuleEffects.BOUNCE, 150f); effects.configure(settings);
        check(effects.active(), "on at 150 %: hooked");
        settings.putFloat(CapsuleEffects.BOUNCE, 0f); effects.configure(settings);
        check(effects.active(), "on at 0 %: hooked");
        settings.putBoolean(CapsuleEffects.BOUNCE_ENABLED, false); effects.configure(settings);
        check(!effects.active(), "off: no hook");
    }

    /** The two code100/101 hooks on framework-wide methods are registered as on-demand hooks, never installed at start. */
    private static void onDemand() throws Exception {
        String source = new String(Files.readAllBytes(new File("app/src/main/java/dev/puitheme/StatusBarModule.java").toPath()), StandardCharsets.UTF_8);
        int bounce = source.indexOf("lazyHook(\"capsule_bounce\", CAPSULE_EFFECTS::active");
        int offset = source.indexOf("Resources.class.getDeclaredMethod(\"getDimensionPixelOffset\"");
        check(bounce >= 0 && offset > bounce && offset - bounce < 400, "the resource read is hooked inside the on-demand install");
        check(source.indexOf("getDimensionPixelOffset\"", offset + 60) < 0, "and nowhere else");
        check(!source.contains("wifi_merged_glyph") && !source.contains("wifiGlyphChanged"),
                "restoring public Wi-Fi removes the later merge image hooks entirely");
        check(!source.contains("ImageView.class.getDeclaredMethod(\"invalidateDrawable\""),
                "the original renderer needs no merge invalidation hook");
    }

    public static void main(String[] args) throws Exception {
        bounceWanted();
        onDemand();
        System.out.println("Code102Check passed " + checks + " checks");
    }
}
