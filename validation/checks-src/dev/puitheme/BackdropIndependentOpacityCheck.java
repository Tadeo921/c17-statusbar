package dev.puitheme;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapShader;
import android.graphics.RuntimeShader;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.TimeZone;

/** Real ClockView and component callsites. This verifies
 * uploaded parameters and work isolation, not AGSL pixels or device FPS. */
public final class BackdropIndependentOpacityCheck {
    // Literals deliberately let the same check run against the released 172 classes.
    private static final String DATE="notification_big_clock_backdrop_date_mask_opacity";
    private static final String FOOTER="notification_big_clock_backdrop_footer_mask_opacity";
    private static final String LEFT="notification_big_clock_widget_left_mask_opacity";
    private static final String RIGHT="notification_big_clock_widget_right_mask_opacity";
    private static final float[] INITIAL={67f,13f,29f,53f,79f};
    private static final String[] NAMES={"time","date","footer","left","right"};
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object get(Object target,String name)throws Exception{return field(target.getClass(),name).get(target);}
    private static Method method(Class<?> type,String name,Class<?>... args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static String key(String name,boolean horizontal){return horizontal?NotificationBigClockSettings.landscapeKey(name):name;}
    private static float tint(TextView text){return text.renderEffect.shader.floats.get("tint")[3];}
    private static int tintWrites(TextView text){return text.renderEffect.shader.floatWritesForCheck.get("tint");}
    private static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    private static boolean flat(RuntimeShader shader){float[] e=shader.floats.get("edgeColor"),r=shader.floats.get("relief");return(e==null||e[3]==0f)&&(r==null||r[2]==0f&&r[3]==0f);}

    private static final class Scene implements AutoCloseable {
        final ClockBackdropCheckScene nativeScene;
        final Class<?> viewType,settingsType;
        final Constructor<?> settingsConstructor;
        final NotificationBigClock owner=new NotificationBigClock();
        final FrameLayout root,overlay;
        final BigClockTextComponents components;
        final TextView[] targets=new TextView[5];
        final Bundle values=new Bundle();
        final boolean horizontal;
        final String[] keys;
        Scene(boolean horizontal)throws Exception {
            this.horizontal=horizontal;
            nativeScene=new ClockBackdropCheckScene();root=nativeScene.root;
            viewType=Class.forName("dev.puitheme.NotificationBigClock$ClockView");
            Constructor<?> ctor=viewType.getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);
            overlay=(FrameLayout)ctor.newInstance(owner,new Context());root.addView(overlay);
            field(NotificationBigClock.class,"panel").set(owner,new WeakReference<>(root));
            field(NotificationBigClock.class,"overlay").set(owner,overlay);
            targets[0]=(TextView)get(overlay,"clock");targets[1]=(TextView)get(overlay,"dateLabel");targets[2]=(TextView)get(overlay,"footerLabel");
            for(TextView text:targets)if(text!=null){text.setText("文字");text.getResources().getConfiguration().orientation=horizontal?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;}
            root.getResources().getConfiguration().orientation=horizontal?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
            values.putBoolean(NotificationBigClockSettings.MASTER,true);
            values.putBoolean(key(NotificationBigClockSettings.DIGIT_REVEAL_ENABLED,horizontal),true);
            values.putBoolean(key(NotificationBigClockSettings.FOLLOW_STYLE,horizontal),true);
            values.putInt(key(NotificationBigClockSettings.REVEAL_MASK_COLOR,horizontal),0x80336699);
            values.putBoolean(key(NotificationBigClockSettings.REVEAL_MASK_COLOR,horizontal)+"_custom_alpha",true);
            values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"左文字");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右文字");
            keys=new String[]{key(NotificationBigClockSettings.REVEAL_MASK_OPACITY,horizontal),key(DATE,horizontal),key(FOOTER,horizontal),LEFT,RIGHT};
            for(int i=0;i<keys.length;i++)values.putFloat(keys[i],INITIAL[i]);
            // The other orientation is intentionally different: no accidental portrait fallback.
            values.putFloat(key(NotificationBigClockSettings.REVEAL_MASK_OPACITY,!horizontal),9f);
            values.putFloat(key(DATE,!horizontal),91f);values.putFloat(key(FOOTER,!horizontal),92f);
            components=new BigClockTextComponents(new Context());components.configure(values);components.refresh(0L,TimeZone.getTimeZone("UTC"));
            ((FrameLayout)get(overlay,"headerMotion")).addView(components);field(viewType,"components").set(overlay,components);
            targets[3]=(TextView)components.getChildAt(0);targets[4]=(TextView)components.getChildAt(1);
            settingsType=Class.forName("dev.puitheme.NotificationBigClock$Settings");
            settingsConstructor=settingsType.getDeclaredConstructor(Bundle.class,String.class);settingsConstructor.setAccessible(true);
            apply();
        }
        void apply()throws Exception {
            Object settings=settingsConstructor.newInstance(values,horizontal?NotificationBigClockSettings.LANDSCAPE_PREFIX:NotificationBigClockSettings.PORTRAIT_PREFIX);
            field(NotificationBigClock.class,"settings").set(owner,settings);
            method(viewType,"applyTextColors",settingsType,int.class,boolean.class).invoke(overlay,settings,0x40123456,false);
            method(viewType,"prepareAppearance",settingsType).invoke(overlay,settings);
            components.configure(values);
            components.appearance(0x40123456,false,true,values.getInt(key(NotificationBigClockSettings.REVEAL_MASK_COLOR,horizontal)),((Number)values.get(keys[0])).floatValue()/100f);
            method(viewType,"updateBackdrops").invoke(overlay);
        }
        void levels(float[] amounts)throws Exception {
            Object bitmap=nativeScene.bitmap;
            for(int i=0;i<targets.length;i++) {
                TextView text=targets[i];
                check(text.renderEffect!=null,"actual "+NAMES[i]+" material exists");
                check(near(tint(text),128f/255f*amounts[i]/100f),(horizontal?"landscape ":"portrait ")+NAMES[i]+" must use its own opacity: wanted "+amounts[i]+", got tint "+tint(text));
                check(text.getCurrentTextColor()==0xffffffff,"mask strength never becomes translucent glyph coverage: "+NAMES[i]);
                check(((BitmapShader)text.renderEffect.shader.inputs.get("background")).bitmap==bitmap,"each class still borrows the same native blur: "+NAMES[i]);
                if(i!=0)check(flat(text.renderEffect.shader),"only primary digits retain depth: "+NAMES[i]);
            }
        }
        @Override public void close()throws Exception{components.close();method(viewType,"clearMaterials").invoke(overlay);nativeScene.close();}
    }

    private static void independence(boolean horizontal)throws Exception {
        try(Scene scene=new Scene(horizontal)) {
            scene.levels(INITIAL);
            for(int changed=0;changed<scene.keys.length;changed++) {
                for(float opacity:new float[]{0f,100f,INITIAL[changed]}) {
                    int[] effects=new int[5],uploads=new int[5];RuntimeShader[] shaders=new RuntimeShader[5];
                    for(int i=0;i<5;i++){effects[i]=scene.targets[i].renderEffectWrites;uploads[i]=tintWrites(scene.targets[i]);shaders[i]=scene.targets[i].renderEffect.shader;}
                    scene.values.putFloat(scene.keys[changed],opacity);scene.apply();
                    float[] wanted=INITIAL.clone();wanted[changed]=opacity;scene.levels(wanted);
                    for(int i=0;i<5;i++) {
                        check(scene.targets[i].renderEffect.shader==shaders[i],"changing "+NAMES[changed]+" reuses "+NAMES[i]+" shader");
                        check(scene.targets[i].renderEffectWrites==effects[i]+(i==changed?1:0),"only changed class resubmits effect: "+NAMES[changed]+" -> "+NAMES[i]);
                        check(tintWrites(scene.targets[i])==uploads[i]+(i==changed?1:0),"only changed class uploads tint: "+NAMES[changed]+" -> "+NAMES[i]);
                    }
                }
            }
            int[] effects=new int[5],writes=new int[5];for(int i=0;i<5;i++){effects[i]=scene.targets[i].renderEffectWrites;writes[i]=tintWrites(scene.targets[i]);}
            for(int n=0;n<10;n++)scene.apply();
            for(int i=0;i<5;i++)check(scene.targets[i].renderEffectWrites==effects[i]&&tintWrites(scene.targets[i])==writes[i],"stable appearance performs no per-class effect/tint writes: "+NAMES[i]);
            // Partial callers from the old schema must inherit their own orientation's main strength.
            for(int i=1;i<5;i++)scene.values.keySet().remove(scene.keys[i]);scene.values.putFloat(scene.keys[0],37f);scene.apply();
            scene.levels(new float[]{37f,37f,37f,37f,37f});
            scene.values.putFloat(scene.keys[0],0f);scene.apply();scene.levels(new float[]{0f,0f,0f,0f,0f});
            // Explicit zero must override the inherited value; invalid typed values retain safe fallback.
            scene.values.putFloat(scene.keys[0],64f);scene.values.putFloat(scene.keys[1],0f);scene.values.putFloat(scene.keys[3],0f);
            scene.values.putFloat(scene.keys[2],Float.NaN);scene.values.putString(scene.keys[4],"bad");scene.apply();
            scene.levels(new float[]{64f,0f,64f,0f,64f});
            scene.values.putBoolean(key(NotificationBigClockSettings.FOLLOW_STYLE,horizontal),false);
            scene.values.putBoolean(BigClockWidgetsSettings.LEFT_FOLLOW_TIME_COLOR,false);scene.values.putInt(BigClockWidgetsSettings.LEFT_COLOR_LIGHT,0xff112233);
            scene.apply();
            check(scene.targets[1].renderEffect==null&&scene.targets[2].renderEffect==null,"date/footer following off restores their independent ink without a mask effect");
            check(scene.targets[3].renderEffect==null&&scene.targets[3].getCurrentTextColor()==0xff112233,"independent component color ignores its stored mask strength");
            check(scene.targets[4].renderEffect!=null,"independent color change leaves the other following component active");
        }
    }
    public static void main(String[] args)throws Exception {
        independence(false);independence(true);
        System.out.println("BackdropIndependentOpacityCheck: "+checks+" checks passed");
    }
}
