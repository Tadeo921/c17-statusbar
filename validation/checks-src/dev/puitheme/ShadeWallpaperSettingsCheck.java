package dev.puitheme;

import android.os.Bundle;
import java.util.HashSet;
import java.util.Objects;

public final class ShadeWallpaperSettingsCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Wallpaper settings "+checks+": "+expected+" != "+actual);}
    public static void main(String[] args) {
        equal(6,ShadeWallpaperSettings.SCENES.size());equal(6,new HashSet<>(ShadeWallpaperSettings.SCENES).size());
        equal(false,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.MASTER));
        equal(true,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.BLUR_WITH_PULL));
        // shelved: it cannot be opened until the author takes it up again
        equal(true,ShadeWallpaperSettings.available()); // device code162: opened again by the author
        ShadeWallpaperSettings.openForCheck(false); // the shelving switch itself still works
        equal(false,ShadeWallpaperSettings.available());
        equal(false,ShadeWallpaperSettings.unavailableReason().isEmpty());
        for(String key:ShadeWallpaperSettings.BOOLEANS.keySet())equal(false,ShadeWallpaperSettings.unavailableReason(key).isEmpty());
        equal(false,ShadeWallpaperSettings.unavailableReason(ShadeWallpaperSettings.PORTRAIT_MASTER).isEmpty());
        equal("",ShadeWallpaperSettings.unavailableReason(null));equal("",ShadeWallpaperSettings.unavailableReason("tiles_enabled"));
        ShadeWallpaperSettings.openForCheck(true);
        equal(true,ShadeWallpaperSettings.available());
        equal("",ShadeWallpaperSettings.unavailableReason());equal("",ShadeWallpaperSettings.unavailableReason(ShadeWallpaperSettings.MASTER));
        equal("",ShadeWallpaperSettings.unavailableReason(null));equal("",ShadeWallpaperSettings.unavailableReason("tiles_enabled"));
        for(String scene:ShadeWallpaperSettings.SCENES) {
            equal(false,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.enabledKey(scene)));
            equal(100f,ShadeWallpaperSettings.NUMBERS.get(ShadeWallpaperSettings.brightnessKey(scene)));
            equal(0f,ShadeWallpaperSettings.NUMBERS.get(ShadeWallpaperSettings.blurKey(scene)));
            equal("",ShadeWallpaperSettings.STRINGS.get(ShadeWallpaperSettings.revisionKey(scene)));
            equal(true,ShadeWallpaperSettings.title(scene).contains(scene.endsWith("landscape")?"横屏":"竖屏"));
            Bundle values=new Bundle();values.putBoolean(ShadeWallpaperSettings.enabledKey(scene),true);
            // A scene is on only when its page is on too.
            for(String other:ShadeWallpaperSettings.SCENES)equal(false,ShadeWallpaperSettings.enabled(values,other));
            values.putBoolean(ShadeWallpaperSettings.masterKey(scene),true);
            for(String other:ShadeWallpaperSettings.SCENES)equal(other.equals(scene),ShadeWallpaperSettings.enabled(values,other));
            equal(scene.endsWith("landscape"),ShadeWallpaperSettings.landscape(scene));
            equal(ShadeWallpaperSettings.landscape(scene)?ShadeWallpaperSettings.LANDSCAPE_MASTER:ShadeWallpaperSettings.PORTRAIT_MASTER,ShadeWallpaperSettings.masterKey(scene));
            // The clock zone belongs to the notification centre only.
            boolean zone=scene.startsWith("notification_");
            equal(zone,ShadeWallpaperSettings.hasZone(scene));
            if(zone) {
                equal(false,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.zoneEnabledKey(scene)));
                equal(true,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.zoneFollowKey(scene)));
                equal(ShadeWallpaperSettings.DEFAULT_ZONE_BLUR_DP,ShadeWallpaperSettings.NUMBERS.get(ShadeWallpaperSettings.zoneBlurKey(scene)));
                equal(ShadeWallpaperSettings.DEFAULT_ZONE_HEIGHT_DP,ShadeWallpaperSettings.NUMBERS.get(ShadeWallpaperSettings.zoneHeightKey(scene)));
                equal(ShadeWallpaperSettings.DEFAULT_ZONE_FEATHER_DP,ShadeWallpaperSettings.NUMBERS.get(ShadeWallpaperSettings.zoneFeatherKey(scene)));
            } else {
                try{ShadeWallpaperSettings.zoneBlurKey(scene);throw new AssertionError("a scene without a clock has no zone key");}
                catch(IllegalArgumentException expected){equal(true,true);}
            }
        }
        // the first release's master (no longer read), the two page switches, a pull-blur switch per page, 6 scene switches, 2 zones x (switch, follow)
        // | 6 x (brightness, blur) + 2 zones x (blur, height, feather) | 6 pictures
        equal(1+2+2+2+6+2*2,ShadeWallpaperSettings.BOOLEANS.size());
        equal(false,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.PORTRAIT_MASTER));equal(false,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.LANDSCAPE_MASTER));
        equal(true,ShadeWallpaperSettings.BOOLEANS.get(ShadeWallpaperSettings.LANDSCAPE_BLUR_WITH_PULL));
        equal(ShadeWallpaperSettings.BLUR_WITH_PULL,ShadeWallpaperSettings.blurWithPullKey(false));equal(ShadeWallpaperSettings.LANDSCAPE_BLUR_WITH_PULL,ShadeWallpaperSettings.blurWithPullKey(true));equal(6*2+2*6,ShadeWallpaperSettings.NUMBERS.size());equal(2,ShadeWallpaperSettings.COLORS.size());equal(6,ShadeWallpaperSettings.STRINGS.size());
        // The first release's batch stays as it was: master, switches, brightness, pictures.
        equal(1+6+6+6,ShadeWallpaperSettings.firstReleaseDefaults().size());
        equal(ShadeWallpaperSettings.CLASSIC_PORTRAIT,ShadeWallpaperSettings.scene("classic",true,false));
        equal(ShadeWallpaperSettings.CLASSIC_LANDSCAPE,ShadeWallpaperSettings.scene("classic",false,true));
        equal(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT,ShadeWallpaperSettings.scene("separate",false,false));
        equal(ShadeWallpaperSettings.NOTIFICATION_LANDSCAPE,ShadeWallpaperSettings.scene("separate",false,true));
        equal(ShadeWallpaperSettings.CONTROL_PORTRAIT,ShadeWallpaperSettings.scene("separate",true,false));
        equal(ShadeWallpaperSettings.CONTROL_LANDSCAPE,ShadeWallpaperSettings.scene("separate",true,true));
        equal(null,ShadeWallpaperSettings.scene("unknown",true,false));equal(null,ShadeWallpaperSettings.scene(null,false,true));
        equal(0f,ShadeWallpaperSettings.brightness(-4f));equal(200f,ShadeWallpaperSettings.brightness(204f));
        equal(100f,ShadeWallpaperSettings.brightness(Float.NaN));equal(100f,ShadeWallpaperSettings.brightness(Float.POSITIVE_INFINITY));
        equal(100f,ShadeWallpaperSettings.brightness("0"));equal(76.5f,ShadeWallpaperSettings.brightness(76.5d));
        // blur and zone numbers are bounded; a value that is not a finite number is the default
        equal(0f,ShadeWallpaperSettings.blur(-3f));equal(ShadeWallpaperSettings.MAX_BLUR_DP,ShadeWallpaperSettings.blur(1000f));
        equal(0f,ShadeWallpaperSettings.blur(Float.NaN));equal(12.5f,ShadeWallpaperSettings.blur(12.5f));
        equal(ShadeWallpaperSettings.DEFAULT_ZONE_BLUR_DP,ShadeWallpaperSettings.zoneBlur("x"));
        equal(ShadeWallpaperSettings.MAX_ZONE_HEIGHT_DP,ShadeWallpaperSettings.zoneHeight(99999f));equal(0f,ShadeWallpaperSettings.zoneHeight(-1f));
        equal(ShadeWallpaperSettings.DEFAULT_ZONE_FEATHER_DP,ShadeWallpaperSettings.zoneFeather(Float.POSITIVE_INFINITY));
        equal(ShadeWallpaperSettings.MAX_ZONE_FEATHER_DP,ShadeWallpaperSettings.zoneFeather(5000f));
        // the zone's lift is signed and bounded; saturation and the tint's strength have their own ranges
        equal(-100f,ShadeWallpaperSettings.zoneLift(-900f));equal(100f,ShadeWallpaperSettings.zoneLift(900f));equal(0f,ShadeWallpaperSettings.zoneLift("x"));equal(-35f,ShadeWallpaperSettings.zoneLift(-35f));
        equal(0f,ShadeWallpaperSettings.zoneSaturation(-5f));equal(200f,ShadeWallpaperSettings.zoneSaturation(999f));equal(100f,ShadeWallpaperSettings.zoneSaturation(Float.NaN));
        equal(0f,ShadeWallpaperSettings.zoneTintStrength(-1f));equal(100f,ShadeWallpaperSettings.zoneTintStrength(101f));
        for(String revision:new String[]{"",null,"../font/current",new String(new char[64]).replace('\0','G'),"a".repeat(63),"a".repeat(65)})
            equal(false,ShadeWallpaperSettings.validRevision(revision));
        equal(true,ShadeWallpaperSettings.validRevision("0123456789abcdef".repeat(4)));
        System.out.println("Shade wallpaper settings checks passed: "+checks);
    }
}
