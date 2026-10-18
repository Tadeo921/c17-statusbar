package dev.puitheme;
import android.os.Bundle;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
/** The four retired parents cannot disable existing child consumers. */
public final class FeatureGroupsRuntimeCheck {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static Bundle base(){return SettingsSnapshot.fromPreferences(Collections.emptyMap());}
    private static Map<String,Object> map(Bundle b){Map<String,Object> result=new LinkedHashMap<>();for(String key:b.keySet())if(!FeatureGroups.BOOLEANS.containsKey(key))result.put(key,b.get(key));return result;}
    private static void retirement() {
        check(FeatureGroups.BOOLEANS.size()==4&&Upgrade175.DEFAULTS.equals(FeatureGroups.BOOLEANS),"published schema42 migration remains frozen");
        for(String key:FeatureGroups.BOOLEANS.keySet()) {
            check(SettingsCatalog.item(key)==null,"retired parent has no editable or searchable setting");
            check(ConfigTransfer.types().containsKey(key),"old configuration values remain lossless");
            check(ConfigScope.keys(Collections.singleton(ConfigScope.OTHER_ID)).contains(key),"hidden compatibility key belongs to other settings");
        }
        for(String id:new String[]{"layout_controls","stack_controls","clear_controls","lock_effects"})check(SettingsCatalog.group(id)==null,"removed parent page is absent");
        Bundle reference=null;
        for(int mask=0;mask<16;mask++) {
            Bundle saved=base();int index=0;
            for(String key:FeatureGroups.BOOLEANS.keySet())saved.putBoolean(key,(mask&(1<<index++))!=0);
            saved.putBoolean(StatusBarLayoutSettings.MASTER,true);
            saved.putFloat(StatusBarLayoutSettings.key(StatusBarLayoutSettings.CLOCK,"margin_left"),7f);
            saved.putBoolean(NotificationBigClockSettings.STACK_ENABLED,true);saved.putBoolean(NotificationMiniStack.ENABLED,true);
            saved.putBoolean(NotificationClearAppearance.MASTER,true);saved.putBoolean(FluidCloudAccent.LOCK_CUSTOM,true);saved.putBoolean(FluidCloudAccent.AOD_CUSTOM,true);
            Map<String,Object> before=map(saved);
            Bundle live=PanelMode.runtimeSettings(SafetyMode.runtimeSettings(saved),PanelMode.SEPARATE);
            check(before.equals(map(saved)),"delivery changes no saved child setting");
            if(reference==null)reference=live;else check(map(reference).equals(map(live)),"all sixteen historical parent combinations have identical runtime delivery");
            check(StatusBarLayoutSettings.orderingEnabled(live)&&StatusBarLayoutSettings.enabled(live),"actual layout consumer ignores retired parent");
            check(StatusBarLayoutSettings.Snapshot.parse(live).orderingEnabled,"parsed flow snapshot ignores retired parent");
            check(live.getBoolean(NotificationBigClockSettings.STACK_ENABLED)&&live.getBoolean(NotificationMiniStack.ENABLED)&&live.getBoolean(NotificationClearAppearance.MASTER),"independent stack and clear switches remain enabled");
            FluidCloudAccent accent=new FluidCloudAccent();accent.configure(live);
            check(accent.lockOwn(false)&&accent.lockOwn(true),"actual lock and AOD consumer ignores retired parent");accent.release();
        }
    }
    private static void safetyCopy() {
        Bundle saved=base();saved.putBoolean(StatusBarSettings.SAFE_MODE,true);
        for(String key:new String[]{FluidCloudAccent.LOCK_CUSTOM,FluidCloudAccent.LOCK_GRADIENT,FluidCloudAccent.AOD_CUSTOM,FluidCloudAccent.AOD_GRADIENT})saved.putBoolean(key,true);
        Bundle live=SafetyMode.runtimeSettings(saved);
        for(String key:new String[]{FluidCloudAccent.LOCK_CUSTOM,FluidCloudAccent.LOCK_GRADIENT,FluidCloudAccent.AOD_CUSTOM,FluidCloudAccent.AOD_GRADIENT}) {
            check(!live.getBoolean(key),"safe runtime copy must pause the actual lock/AOD look: "+key);
            check(saved.getBoolean(key),"safe mode must preserve saved lock/AOD look");
        }
    }
    private static void safetyConsumer() {
        Bundle saved=base();saved.putBoolean(StatusBarSettings.SAFE_MODE,true);saved.putBoolean(FluidCloudAccent.LOCK_CUSTOM,true);saved.putBoolean(FluidCloudAccent.LOCK_GRADIENT,true);saved.putBoolean(FluidCloudAccent.AOD_CUSTOM,true);saved.putBoolean(FluidCloudAccent.AOD_GRADIENT,true);
        FluidCloudAccent accent=new FluidCloudAccent();accent.configure(saved);
        check(!accent.lockOwn(false)&&!accent.lockOwn(true),"actual accent consumer must reject saved lock/AOD look while safe mode is on");
        check(accent.mediaAccentColor(0xff347899,true,false)==null,"safe lock media colour remains native");
        saved.putBoolean(StatusBarSettings.SAFE_MODE,false);accent.configure(saved);
        check(accent.lockOwn(false)&&accent.lockOwn(true),"leaving safe mode restores both saved scenes without recreating the consumer");accent.release();
    }
    public static void main(String[] args) {retirement();safetyCopy();safetyConsumer();System.out.println("FeatureGroupsRuntimeCheck: "+checks+" checks passed");}
}
