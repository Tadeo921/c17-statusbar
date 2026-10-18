package dev.puitheme;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Preserve original horizontal/header preferences while the portrait replacement owns its space. */
public final class NotificationBigClockSettingsCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Big clock check "+checks+": "+expected+" != "+actual);}
    private static String document(String settings){return "{\"package\":\""+ConfigTransfer.PACKAGE_NAME+"\",\"schema\":1,\"settings\":{"+settings+"}}";}
    private static void invalid(String settings) throws IOException {
        checks++;try{ConfigTransfer.prepare(document(settings),true);}catch(IOException expected){return;}throw new AssertionError("Invalid big-clock configuration accepted");
    }
    public static void main(String[] args) throws Exception {
        materialSettings();
        backdropDefaults171();
        // The lock-screen clock plugin bundles three variable families; all of them are native height-axis fonts.
        equal(7,NotificationBigClockSettings.FONT_VALUES.length);equal(NotificationBigClockSettings.FONT_VALUES.length,NotificationBigClockSettings.FONT_LABELS.length);
        for(String mode:new String[]{"native","native_sans","native_digit","native_serif"})equal(true,NotificationBigClockSettings.nativeHeightFont(mode));
        for(String mode:new String[]{"system","pingfang","custom","global",null})equal(false,NotificationBigClockSettings.nativeHeightFont(mode));
        equal("OPPOSans4.0.ttf",NotificationBigClockSettings.nativeFamily("native_sans"));
        equal("OPPODigit00.ttf",NotificationBigClockSettings.nativeFamily("native_digit"));
        equal("OPPOSerif.ttf",NotificationBigClockSettings.nativeFamily("native_serif"));
        equal(null,NotificationBigClockSettings.nativeFamily("native"));equal(null,NotificationBigClockSettings.nativeFamily("system"));
        for(String mode:NotificationBigClockSettings.FONT_VALUES) {
            if("custom".equals(mode))continue;
            checks++;ConfigTransfer.prepare(document("\""+NotificationBigClockSettings.FONT+"\":\""+mode+"\",\""+NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.FONT)+"\":\""+mode+"\""),true);
        }
        invalid("\""+NotificationBigClockSettings.FONT+"\":\"native_comic\"");
        equal(Arrays.asList(NotificationBigClockSettings.FONT_VALUES),Arrays.asList(SettingsCatalog.item(NotificationBigClockSettings.FONT).values));
        equal(false,SettingsCatalog.item(NotificationBigClockSettings.FONT).description.isEmpty());
        equal(false,NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.MASTER));equal(true,NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.GLASS));
        equal(100f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.SCALE));equal(36f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.COMPACT_SCALE));
        equal(1f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.VISIBLE_COUNT));
        equal(600f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.WEIGHT));equal(300f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.COMPACT_WEIGHT));equal(0f,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.OFFSET_Y));
        equal("HH:mm",NotificationBigClockSettings.STRINGS.get(NotificationBigClockSettings.PATTERN));equal("M月d日{周} {干支}{农历日期}",NotificationBigClockSettings.STRINGS.get(NotificationBigClockSettings.DATE_PATTERN));
        equal(0xffffffff,NotificationBigClockSettings.COLORS.get(NotificationBigClockSettings.COLOR_LIGHT));equal(0xffffffff,NotificationBigClockSettings.COLORS.get(NotificationBigClockSettings.COLOR_DARK));
        for(Map.Entry<String,Boolean> value:NotificationBigClockSettings.BOOLEANS.entrySet()){equal(value.getValue(),StatusBarSettings.BOOLEAN_DEFAULTS.get(value.getKey()));equal(ConfigTransfer.Type.BOOLEAN,ConfigTransfer.types().get(value.getKey()));}
        for(Map.Entry<String,Float> value:NotificationBigClockSettings.NUMBERS.entrySet()){equal(value.getValue(),StatusBarSettings.NUMERIC_DEFAULTS.get(value.getKey()));equal(ConfigTransfer.Type.NUMBER,ConfigTransfer.types().get(value.getKey()));}
        for(Map.Entry<String,Integer> value:NotificationBigClockSettings.COLORS.entrySet()){equal(value.getValue(),StatusBarSettings.COLOR_DEFAULTS.get(value.getKey()));equal(ConfigTransfer.Type.COLOR,ConfigTransfer.types().get(value.getKey()));}
        for(Map.Entry<String,String> value:NotificationBigClockSettings.STRINGS.entrySet()){equal(value.getValue(),StatusBarSettings.STRING_DEFAULTS.get(value.getKey()));equal(ConfigTransfer.Type.STRING,ConfigTransfer.types().get(value.getKey()));}
        for(String group:new String[]{"shade_clock","clock",NotificationBigClockSettings.NATIVE_CLOCK,CarrierPanels.NOTIFICATION}) {
            equal(true,NotificationBigClockSettings.suppressed(group,true,true,true));equal(true,NotificationBigClockSettings.suppressed(group,false,true,true));
            equal(false,NotificationBigClockSettings.suppressed(group,true,false,true));equal(false,NotificationBigClockSettings.suppressed(group,true,true,false));
            equal(false,NotificationBigClockSettings.suppressed(group,false,false,true));equal(false,NotificationBigClockSettings.suppressed(group,false,true,false));
        }
        for(String group:new String[]{CarrierPanels.CONTROL,CarrierPanels.LOCKSCREEN,"carrier","wifi","notification_big_clock",null})equal(false,NotificationBigClockSettings.suppressed(group,true,true,true));
        for(String key:new String[]{StatusBarSettings.SHADE_CLOCK_CONTROLS_ENABLED,StatusBarSettings.SHADE_CLOCK_PATTERN,StatusBarSettings.SHADE_CLOCK_SCALE,"carrier_notification_enabled","carrier_notification_mode"}) {
            equal(true,NotificationBigClockSettings.conflictsWith(key,true));equal(true,NotificationBigClockSettings.conflictsWith(key,0f));equal(false,NotificationBigClockSettings.conflictsWith(key,false));equal(false,NotificationBigClockSettings.conflictsWith(key,null));
        }
        for(String key:new String[]{StatusBarSettings.CLOCK_PATTERN,"carrier_pattern","carrier_control_scale","carrier_lockscreen_enabled",NotificationBigClockSettings.MASTER,"shade_clockwise_enabled",null})equal(false,NotificationBigClockSettings.conflictsWith(key,true));
        Map<String,Object> stored=new LinkedHashMap<>();stored.put(NotificationBigClockSettings.MASTER,true);stored.put(NotificationBigClockSettings.PATTERN,"HH:mm:ss");stored.put(NotificationBigClockSettings.DATE_PATTERN,"M月d日 {星期}");
        stored.put(StatusBarSettings.SHADE_CLOCK_CONTROLS_ENABLED,true);stored.put(StatusBarSettings.SHADE_CLOCK_SCALE,165f);stored.put("carrier_notification_text","保留原运营商文字");
        Map<String,Object> imported=ConfigTransfer.prepare(ConfigTransfer.exportJson(stored),true).values();for(Map.Entry<String,Object> value:stored.entrySet())equal(value.getValue(),imported.get(value.getKey()));
        for(String setting:new String[]{"\"notification_big_clock_pattern\":\"{未知}\"","\"notification_big_clock_date_pattern\":\"'未配对\"",
                "\"notification_big_clock_date_pattern\":\"HH:mm\\n\"","\"notification_big_clock_date_pattern\":\"\"","\"notification_big_clock_pattern\":true",
                "\"notification_big_clock_enabled\":\"true\""})invalid(setting);
        equal(99f,ConfigTransfer.prepare(document("\"notification_big_clock_weight\":99"),true).values().get(NotificationBigClockSettings.WEIGHT));
        equal(901f,ConfigTransfer.prepare(document("\"notification_big_clock_compact_weight\":901"),true).values().get(NotificationBigClockSettings.COMPACT_WEIGHT));
        equal(-1f,ConfigTransfer.prepare(document("\"notification_big_clock_compact_scale\":-1"),true).values().get(NotificationBigClockSettings.COMPACT_SCALE));
        equal(false,NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.STACK_ENABLED));
        equal(false,StatusBarSettings.bool(java.util.Collections.emptyMap(),NotificationBigClockSettings.STACK_ENABLED));
        equal(true,StatusBarSettings.bool(java.util.Collections.singletonMap(NotificationBigClockSettings.STACK_ENABLED,true),NotificationBigClockSettings.STACK_ENABLED));
        equal(true,ConfigTransfer.prepare(document("\"notification_big_clock_stack_enabled\":true"),true).values().get(NotificationBigClockSettings.STACK_ENABLED));
        equal(false,ConfigTransfer.prepare(document("\"notification_big_clock_stack_enabled\":false"),true).values().get(NotificationBigClockSettings.STACK_ENABLED));
        for(String value:new String[]{"\"true\"","1","null","[]"})invalid("\"notification_big_clock_stack_enabled\":"+value);
        stored.put(NotificationBigClockSettings.STACK_ENABLED,true);
        Bundle saved=SettingsSnapshot.fromPreferences(stored);
        equal(true,SettingsSnapshot.complete(saved));
        equal(true,saved.get(NotificationBigClockSettings.STACK_ENABLED));
        equal(true,ConfigTransfer.prepare(ConfigTransfer.exportJson(stored),true).values().get(NotificationBigClockSettings.STACK_ENABLED));
        stackingRuntimeGate(saved);
        ActivationGuardPreferencesCheck.MemoryPreferences raw=new ActivationGuardPreferencesCheck.MemoryPreferences();raw.values.putAll(stored);AtomicBoolean active=new AtomicBoolean(true);
        SharedPreferences guarded=new ActivationGuardPreferences(raw,active::get,()->{});
        equal(true,guarded.edit().putBoolean(NotificationBigClockSettings.STACK_ENABLED,true).commit());
        equal(true,guarded.getBoolean(NotificationBigClockSettings.STACK_ENABLED,false));
        equal(true,ConfigTransfer.commit(guarded,ConfigTransfer.prepare(document("\"notification_big_clock_stack_enabled\":false"),true)));
        equal(false,raw.values.get(NotificationBigClockSettings.STACK_ENABLED));
        equal(true,ConfigTransfer.commit(guarded,ConfigTransfer.prepare(document("\"notification_big_clock_stack_enabled\":true"),true)));
        equal(true,raw.values.get(NotificationBigClockSettings.STACK_ENABLED));
        for(String settings:new String[]{"\"notification_big_clock_enabled\":true,\"shade_clock_scale\":200,\"carrier_notification_text\":\"横屏保留\"",
                "\"carrier_notification_text\":\"横屏保留\",\"shade_clock_scale\":200,\"notification_big_clock_enabled\":true"}) {
            equal(true,ConfigTransfer.commit(guarded,ConfigTransfer.prepare(document(settings),true)));equal(true,raw.values.get(NotificationBigClockSettings.MASTER));equal(200f,raw.values.get(StatusBarSettings.SHADE_CLOCK_SCALE));equal("横屏保留",raw.values.get("carrier_notification_text"));
            equal(true,NotificationBigClockSettings.suppressed("shade_clock",false,true,(Boolean)raw.values.get(NotificationBigClockSettings.MASTER)));
        }
        active.set(false);int writes=raw.writes;equal(false,ConfigTransfer.commit(guarded,ConfigTransfer.prepare(document("\"carrier_notification_text\":\"禁止修改\""),true)));equal(writes,raw.writes);equal(true,raw.values.get(NotificationBigClockSettings.MASTER));equal("横屏保留",raw.values.get("carrier_notification_text"));
        completeCardCount();
        reboundBlur();
        System.out.println("Notification big-clock settings checks passed: "+checks);
    }
    private static void materialSettings() throws Exception {
        String follow="notification_big_clock_follow_style_enabled";
        String mask="notification_big_clock_reveal_mask_color";
        String opacity="notification_big_clock_reveal_mask_opacity";
        equal("字内背景透出",SettingsCatalog.item(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED).title);
        java.lang.reflect.Method effective=NotificationBigClockSettings.class.getMethod("effectiveFollowStyle",Bundle.class,Boolean.TYPE);
        for(boolean landscape:new boolean[]{false,true}) {
            String parent=landscape?NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED):NotificationBigClockSettings.DIGIT_REVEAL_ENABLED;
            String child=landscape?NotificationBigClockSettings.landscapeKey(follow):follow;
            String colour=landscape?NotificationBigClockSettings.landscapeKey(mask):mask;
            String strength=landscape?NotificationBigClockSettings.landscapeKey(opacity):opacity;
            equal(false,NotificationBigClockSettings.BOOLEANS.get(child));
            equal(0xffffffff,NotificationBigClockSettings.COLORS.get(colour));
            equal(20f,NotificationBigClockSettings.NUMBERS.get(strength));
            SettingsCatalog.Item item=SettingsCatalog.item(child),maskItem=SettingsCatalog.item(colour),opacityItem=SettingsCatalog.item(strength);
            equal("时间和日期样式跟随",item.title);equal(false,item.defaultValue);
            equal("背景透出与遮罩",item.section);equal(item.section,SettingsCatalog.item(parent).section);
            equal(item.section,maskItem.section);equal(item.section,opacityItem.section);
            equal(SettingsCatalog.COLOR,maskItem.type);equal("",maskItem.pairedKey);
            equal("主时间遮罩强度",opacityItem.title);equal(0f,opacityItem.min);equal(100f,opacityItem.max);
            equal("look",pageOf(item));
            for(String key:new String[]{parent,colour,strength})
                equal("look",pageOf(SettingsCatalog.item(key)));
            String oldAmount=landscape?NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.DIGIT_REVEAL_AMOUNT):NotificationBigClockSettings.DIGIT_REVEAL_AMOUNT;
            equal(null,SettingsCatalog.item(oldAmount));equal(true,SettingsCatalog.hiddenKeys().contains(oldAmount));
            equal(ConfigTransfer.Type.NUMBER,ConfigTransfer.types().get(oldAmount));equal(60f,StatusBarSettings.NUMERIC_DEFAULTS.get(oldAmount));
            Bundle source=new Bundle();Map<String,Object> values=new LinkedHashMap<>();
            equal(false,effective.invoke(null,source,landscape));
            source.putBoolean(child,true);values.put(child,true);
            equal(false,effective.invoke(null,source,landscape));
            equal(false,SettingsCatalog.unavailableReason(child,values).isEmpty());
            equal(true,source.get(child));equal(true,values.get(child));
            source.putBoolean(parent,true);values.put(parent,true);
            equal(true,effective.invoke(null,source,landscape));
            equal("",SettingsCatalog.unavailableReason(child,values));
            equal(false,effective.invoke(null,source,!landscape));
            source.putBoolean(parent,false);values.put(parent,false);
            equal(false,effective.invoke(null,source,landscape));
            equal(true,source.get(child));equal(true,values.get(child));
            equal(false,SettingsCatalog.unavailableReason(colour,values).isEmpty());
            equal(false,SettingsCatalog.unavailableReason(strength,values).isEmpty());
            equal(null,SettingsCatalog.validationError(opacityItem,0f));equal(null,SettingsCatalog.validationError(opacityItem,100f));
            equal(true,SettingsCatalog.validationError(opacityItem,-1f)!=null);equal(true,SettingsCatalog.validationError(opacityItem,101f)!=null);
        }
        equal(false,effective.invoke(null,(Bundle)null,false));
    }
    private static void backdropDefaults171() {
        for(boolean landscape:new boolean[]{false,true}) {
            String edge=landscape?NotificationBigClockSettings.landscapeKey(ClockBackdropSettings.EDGE_ENABLED):ClockBackdropSettings.EDGE_ENABLED;
            equal(true,NotificationBigClockSettings.BOOLEANS.get(edge));
            for(Map.Entry<String,Float> item:ClockBackdropSettings.NUMBERS.entrySet())equal(item.getValue(),NotificationBigClockSettings.NUMBERS.get(landscape?NotificationBigClockSettings.landscapeKey(item.getKey()):item.getKey()));
            String color=landscape?NotificationBigClockSettings.landscapeKey(ClockBackdropSettings.EDGE_COLOR):ClockBackdropSettings.EDGE_COLOR;equal(0xffffffff,NotificationBigClockSettings.COLORS.get(color));
        }
        equal(false,StatusBarSettings.BOOLEAN_DEFAULTS.get(ClockBackdropSettings.NOTIFICATION_FOLLOW));equal(false,NotificationBigClockSettings.BOOLEANS.containsKey(ClockBackdropSettings.NOTIFICATION_FOLLOW));
        equal(false,StatusBarSettings.BOOLEAN_DEFAULTS.get(ClockBackdropSettings.NOTIFICATION_RELIEF));equal(false,NotificationBigClockSettings.BOOLEANS.containsKey(ClockBackdropSettings.NOTIFICATION_RELIEF));
        equal(0f,ClockBackdropSettings.NUMBERS.get(ClockBackdropSettings.SHADE_STRENGTH));equal(.6f,ClockBackdropSettings.NUMBERS.get(ClockBackdropSettings.EDGE_WIDTH));
        equal(315f,ClockBackdropSettings.NUMBERS.get(ClockBackdropSettings.HIGHLIGHT_ANGLE));equal(1.5f,ClockBackdropSettings.NUMBERS.get(ClockBackdropSettings.DEPTH_WIDTH));
    }
    private static String pageOf(SettingsCatalog.Item item) {
        for(SettingsCatalog.DetailPage page:SettingsCatalog.detailPages(SettingsCatalog.group(item.groupId)))
            if(page.items.contains(item))return page.id;
        return null;
    }
    /** code98: the clock blurs while the list's overscroll spring has pushed it away, and sharpens as it settles. */
    private static void reboundBlur() throws Exception {
        equal(false,NotificationBigClockSettings.BOOLEANS.get(NotificationBigClockSettings.REBOUND_BLUR_ENABLED));
        equal(NotificationBigClockSettings.DEFAULT_REBOUND_BLUR_RADIUS,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.REBOUND_BLUR_RADIUS));
        // Portrait only: the landscape clock has no such keys.
        equal(false,NotificationBigClockSettings.BOOLEANS.containsKey(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REBOUND_BLUR_ENABLED)));
        equal(false,NotificationBigClockSettings.NUMBERS.containsKey(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REBOUND_BLUR_RADIUS)));
        SettingsCatalog.Item item=SettingsCatalog.item(NotificationBigClockSettings.REBOUND_BLUR_ENABLED);
        equal(true,item!=null&&"notification_big_clock".equals(item.groupId));
        equal(null,SettingsCatalog.item(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REBOUND_BLUR_ENABLED)));
        // The amount pushed away: nothing at rest, everything at 48 dp, on a smooth curve that is flat at both ends.
        equal(0f,NotificationBigClock.reboundTarget(0f,3f));
        equal(0f,NotificationBigClock.reboundTarget(-5f,3f));
        equal(0f,NotificationBigClock.reboundTarget(10f,0f));
        equal(1f,NotificationBigClock.reboundTarget(48f*3f,3f));
        equal(1f,NotificationBigClock.reboundTarget(500f,3f));
        equal(.5f,NotificationBigClock.reboundTarget(24f*3f,3f));
        float low=NotificationBigClock.reboundTarget(2f*3f,3f),high=NotificationBigClock.reboundTarget(46f*3f,3f);
        equal(true,low<2f/48f&&low>0f);       // eases in: slower than a straight line near rest
        equal(true,high>46f/48f&&high<1f);    // and eases out near the end
        float last=0f;boolean rising=true;
        for(int dp=1;dp<=48;dp++){float now=NotificationBigClock.reboundTarget(dp*3f,3f);rising&=now>=last;last=now;}
        equal(true,rising);
        // The blur for an amount: nothing at rest, the whole radius at 1, never more than 64 px.
        equal(0f,NotificationBigClock.reboundBlur(0f,14f,3f));
        equal(0f,NotificationBigClock.reboundBlur(.5f,0f,3f));
        equal(0f,NotificationBigClock.reboundBlur(-1f,14f,3f));
        equal(0f,NotificationBigClock.reboundBlur(.5f,14f,0f));
        equal(14f*3f,NotificationBigClock.reboundBlur(1f,14f,3f));
        equal(14f*3f,NotificationBigClock.reboundBlur(7f,14f,3f));
        equal(14f*3f/2f,NotificationBigClock.reboundBlur(.5f,14f,3f));
        equal(64f,NotificationBigClock.reboundBlur(1f,100f,4f));
        // Time smoothing: it follows a push quickly and lets go slowly, so a spring swinging back and forth fades instead of flickering.
        equal(.4f,NotificationBigClock.reboundStep(.4f,1f,0L));
        equal(true,NotificationBigClock.reboundStep(0f,1f,16L)>.2f);                // 16 ms: about a quarter of the way up
        equal(true,NotificationBigClock.reboundStep(1f,0f,16L)>.9f);                // 16 ms: barely down
        float rise=NotificationBigClock.reboundStep(0f,1f,60L),fall=1f-NotificationBigClock.reboundStep(1f,0f,60L);
        equal(true,rise>fall*2f);                                                  // up is much faster than down
        float shown=1f;for(int frame=0;frame<30;frame++)shown=NotificationBigClock.reboundStep(shown,0f,16L);
        equal(true,shown>.1f&&shown<.5f);                                          // half a second later it is mostly sharp but still easing
        for(int frame=0;frame<120;frame++)shown=NotificationBigClock.reboundStep(shown,0f,16L);
        equal(true,shown<.004f);                                                   // and it ends sharp
        float hold=0f;for(int frame=0;frame<200;frame++)hold=NotificationBigClock.reboundStep(hold,1f,16L);
        equal(true,hold>.99f&&hold<=1f);                                           // it never overshoots the target
        // It grows as the clock goes away and shrinks as it comes back.
        float away=NotificationBigClock.reboundBlur(NotificationBigClock.reboundTarget(10f,1f),14f,3f),farther=NotificationBigClock.reboundBlur(NotificationBigClock.reboundTarget(40f,1f),14f,3f),back=NotificationBigClock.reboundBlur(NotificationBigClock.reboundTarget(5f,1f),14f,3f);
        equal(true,away<farther&&back<away);
        // code99: the fade (how much the clock dims while it is pushed away) is a setting of its own, portrait only.
        equal(NotificationBigClockSettings.DEFAULT_REBOUND_FADE,NotificationBigClockSettings.NUMBERS.get(NotificationBigClockSettings.REBOUND_FADE));
        equal(false,NotificationBigClockSettings.NUMBERS.containsKey(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REBOUND_FADE)));
        SettingsCatalog.Item fade=SettingsCatalog.item(NotificationBigClockSettings.REBOUND_FADE);
        equal(true,fade!=null&&"notification_big_clock".equals(fade.groupId));
        equal(null,SettingsCatalog.item(NotificationBigClockSettings.landscapeKey(NotificationBigClockSettings.REBOUND_FADE)));
        java.util.Map<String,Object> values=new java.util.LinkedHashMap<>();
        values.put(NotificationBigClockSettings.MASTER,true);
        String waiting=SettingsCatalog.unavailableReason(NotificationBigClockSettings.REBOUND_FADE,values);
        equal(true,waiting!=null&&!waiting.isEmpty());                              // it waits for the blur switch
        values.put(NotificationBigClockSettings.REBOUND_BLUR_ENABLED,true);
        String freed=SettingsCatalog.unavailableReason(NotificationBigClockSettings.REBOUND_FADE,values);equal(true,freed==null||freed.isEmpty());
    }

    private static void completeCardCount() throws Exception {
        for(int count:new int[]{1,2,3,4,5,6,1000000,Integer.MAX_VALUE})
            equal(count,NotificationBigClockSettings.visibleCount(count));
        equal(Integer.MAX_VALUE,NotificationBigClockSettings.visibleCount((float)Integer.MAX_VALUE));
        equal(Integer.MAX_VALUE,NotificationBigClockSettings.visibleCount(Float.MAX_VALUE));
        equal(Integer.MAX_VALUE,NotificationBigClockSettings.visibleCount(Long.MAX_VALUE));
        for(Object invalid:new Object[]{null,"6",Float.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
            equal(1,NotificationBigClockSettings.visibleCount(invalid));
        for(Number invalid:new Number[]{0,-1,-1000000,-Float.MAX_VALUE})equal(1,NotificationBigClockSettings.visibleCount(invalid));
        NotificationNativeStack stack=new NotificationNativeStack();
        java.lang.reflect.Field visible=NotificationNativeStack.class.getDeclaredField("completeCount");visible.setAccessible(true);
        for(float count:new float[]{3f,4f,5f,6f,1000000f,(float)Integer.MAX_VALUE}) {
            Bundle source=new Bundle();source.putBoolean(NotificationBigClockSettings.MASTER,true);
            source.putBoolean(NotificationBigClockSettings.STACK_ENABLED,true);source.putFloat(NotificationBigClockSettings.VISIBLE_COUNT,count);
            stack.configure(source);
            equal(NotificationBigClockSettings.visibleCount(count),visible.get(stack));
            equal(count,source.get(NotificationBigClockSettings.VISIBLE_COUNT));
        }
    }
    /** Saved requests stay intact, but only an enabled direction's clock can permit runtime stacking. */
    private static void stackingRuntimeGate(Bundle saved) throws Exception {
        NotificationNativeStack stack=new NotificationNativeStack();
        java.lang.reflect.Field enabled=NotificationNativeStack.class.getDeclaredField("enabled");
        enabled.setAccessible(true);
        equal(false,enabled.get(stack));
        for(boolean portrait:new boolean[]{false,true})for(boolean landscape:new boolean[]{false,true})
                for(boolean landscapeStack:new boolean[]{false,true})for(boolean requested:new boolean[]{false,true})
                for(boolean safe:new boolean[]{false,true}){
            Bundle source=new Bundle(saved);
            source.putBoolean(NotificationBigClockSettings.MASTER,portrait);
            source.putBoolean(NotificationBigClockSettings.LANDSCAPE_MASTER,landscape);
            source.putBoolean(FeatureOptions.STACK_LANDSCAPE_ENABLED,landscapeStack);
            source.putBoolean(NotificationBigClockSettings.STACK_ENABLED,requested);
            source.putBoolean(StatusBarSettings.SAFE_MODE,safe);
            stack.configure(source);
            equal(requested&&!safe&&(portrait||landscape&&landscapeStack),enabled.get(stack));
            equal(requested,source.get(NotificationBigClockSettings.STACK_ENABLED));
            equal(portrait,source.get(NotificationBigClockSettings.MASTER));
            equal(landscape,source.get(NotificationBigClockSettings.LANDSCAPE_MASTER));
            equal(landscapeStack,source.get(FeatureOptions.STACK_LANDSCAPE_ENABLED));
        }
        Bundle legacy=new Bundle();legacy.putBoolean(NotificationBigClockSettings.STACK_ENABLED,true);
        legacy.putBoolean(FeatureOptions.STACK_LANDSCAPE_ENABLED,true);
        stack.configure(legacy);equal(false,enabled.get(stack));
        equal(true,legacy.get(NotificationBigClockSettings.STACK_ENABLED));
        legacy.putBoolean(NotificationBigClockSettings.MASTER,true);
        stack.configure(legacy);equal(true,enabled.get(stack));
        legacy.putBoolean(NotificationBigClockSettings.MASTER,false);
        stack.configure(legacy);equal(false,enabled.get(stack));
        legacy.putBoolean(NotificationBigClockSettings.LANDSCAPE_MASTER,true);
        stack.configure(legacy);equal(true,enabled.get(stack));
        legacy.putBoolean(FeatureOptions.STACK_LANDSCAPE_ENABLED,false);
        stack.configure(legacy);equal(false,enabled.get(stack));
        saved.putBoolean(StatusBarSettings.SAFE_MODE,true);
        Bundle runtime=SafetyMode.runtimeSettings(saved);
        equal(true,saved.get(NotificationBigClockSettings.STACK_ENABLED));
        equal(true,runtime.get(NotificationBigClockSettings.STACK_ENABLED));
        stack.configure(runtime);equal(false,enabled.get(stack));
        saved.putBoolean(StatusBarSettings.SAFE_MODE,false);
        stack.configure(SafetyMode.runtimeSettings(saved));equal(true,enabled.get(stack));
        equal(false,stack.enabled()); // A saved request alone cannot activate uninstalled native hooks.
        stack.configure(null);equal(false,enabled.get(stack));
    }
}
