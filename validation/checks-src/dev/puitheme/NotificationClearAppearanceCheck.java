package dev.puitheme;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import com.oplus.systemui.notification.clearall.OplusClearAllButton;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Real scene draw and durable raw migration; no device, live preferences or UI automation. */
public final class NotificationClearAppearanceCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Clear appearance "+checks+": "+expected+" != "+actual);}
    private static final class Store {
        final ActivationGuardPreferencesCheck.MemoryPreferences memory=new ActivationGuardPreferencesCheck.MemoryPreferences();
        int fail;
        final SharedPreferences preferences=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(owner,method,args)->{
            if(!method.getName().equals("edit"))return method.invoke(memory,args);
            SharedPreferences.Editor delegate=memory.edit();
            return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(editor,operation,parameters)->{
                if(operation.getName().equals("commit")){delegate.commit();if(fail>0){fail--;return false;}return true;}
                if(operation.getName().equals("apply")){delegate.apply();return null;}
                operation.invoke(delegate,parameters);return editor;
            });
        });
    }
    private static void migration(){
        Store old=new Store(),metadata=new Store();
        old.memory.values.put(NotificationClearAppearance.MASTER,true);
        old.memory.values.put(NotificationClearAppearance.GRADIENT_ENABLED,true);
        old.memory.values.put(NotificationClearAppearance.OPACITY,37.125f);
        old.memory.values.put(NotificationClearAppearance.GRADIENT_ANGLE,-22.5f);
        old.memory.values.put(NotificationClearAppearance.COLOR_LIGHT,0x80112233);
        old.memory.values.put(StatusBarSettings.alphaKey(NotificationClearAppearance.COLOR_LIGHT),false);
        Map<String,Object> before=new LinkedHashMap<>(old.memory.values);
        Map<String,Object> inherited=NotificationClearAppearance.inheritedLandscape(before);
        equal(13,inherited.size());equal(true,NotificationClearAppearance.migrate(old.preferences,metadata.preferences,true));
        for(String key:before.keySet())equal(before.get(key),old.memory.values.get(key));
        for(String key:inherited.keySet())equal(inherited.get(key),old.memory.values.get(key));
        old.memory.values.put(NotificationClearAppearance.COLOR_LIGHT,0xffabcdef);
        old.memory.values.put(NotificationClearAppearance.MASTER,false);
        equal(true,NotificationClearAppearance.migrate(old.preferences,metadata.preferences,true));
        equal(0x80112233,old.memory.values.get(NotificationClearAppearance.LANDSCAPE_COLOR_LIGHT));
        equal(true,old.memory.values.get(NotificationClearAppearance.LANDSCAPE_MASTER));
        equal(false,old.memory.values.get(StatusBarSettings.alphaKey(NotificationClearAppearance.LANDSCAPE_COLOR_LIGHT)));

        Store fresh=new Store(),freshMeta=new Store();
        equal(false,NotificationClearAppearance.migrate(fresh.preferences,freshMeta.preferences,false));equal(0,freshMeta.memory.writes);
        equal(true,NotificationClearAppearance.migrate(fresh.preferences,freshMeta.preferences,true));equal(true,fresh.memory.values.isEmpty());
        fresh.memory.values.put(NotificationClearAppearance.MASTER,true);
        equal(true,NotificationClearAppearance.migrate(fresh.preferences,freshMeta.preferences,true));
        equal(false,fresh.memory.values.get(NotificationClearAppearance.LANDSCAPE_MASTER));

        Store failed=new Store(),failedMeta=new Store();failed.memory.values.put(NotificationClearAppearance.MASTER,true);failed.fail=1;
        equal(false,NotificationClearAppearance.migrate(failed.preferences,failedMeta.preferences,true));
        equal(false,failedMeta.memory.values.containsKey("notification_clear_landscape_migrated_v62"));
        equal(false,PreferenceWrites.values(failed.preferences).containsKey(NotificationClearAppearance.LANDSCAPE_MASTER));
        equal(true,NotificationClearAppearance.migrate(failed.preferences,failedMeta.preferences,true));
        equal(true,failed.memory.values.get(NotificationClearAppearance.LANDSCAPE_MASTER));

        Store metaFailed=new Store(),badMeta=new Store();metaFailed.memory.values.put(NotificationClearAppearance.MASTER,true);badMeta.fail=1;
        equal(false,NotificationClearAppearance.migrate(metaFailed.preferences,badMeta.preferences,true));
        equal(false,PreferenceWrites.values(badMeta.preferences).containsKey("notification_clear_landscape_migrated_v62"));
        equal(true,NotificationClearAppearance.migrate(metaFailed.preferences,badMeta.preferences,true));
        Store malformed=new Store(),unmarked=new Store();malformed.memory.values.put(NotificationClearAppearance.OPACITY,"invalid");
        equal(false,NotificationClearAppearance.migrate(malformed.preferences,unmarked.preferences,true));equal(0,unmarked.memory.writes);
    }
    private static Shader draw(NotificationClearAppearance appearance,OplusClearAllButton button,ShapeDrawable background) throws Throwable {
        Shader[] seen={null};appearance.drawButton(button,new Canvas(),canvas->seen[0]=background.getPaint().getShader());
        equal(null,background.getPaint().getShader());return seen[0];
    }
    private static void glass(NotificationClearAppearance appearance,OplusClearAllButton button,Bundle settings) throws Throwable {
        com.oplusos.systemui.common.blurability.drawable.AutoBlurDrawable wrapper=new com.oplusos.systemui.common.blurability.drawable.AutoBlurDrawable();
        com.oplus.posteffect.drawable.BlendDrawable engine=wrapper.viewBlurProxy.actual.blurDrawable;
        engine.setBounds(0,0,96,48);wrapper.setBounds(0,0,96,48);button.setBackground(wrapper);
        com.oplus.posteffect.agsl.DrawableShader nativeShader=engine.drawableShader;
        nativeShader.multiBlendParam.clear();nativeShader.summaryBlendParam.clear();
        nativeShader.multiBlendParam.add(new com.oplus.posteffect.agsl.ShaderBlendParam(5,0xffaabbcc));
        nativeShader.multiBlendParam.add(new com.oplus.posteffect.agsl.ShaderBlendParam(3,0xffddeeff));
        nativeShader.summaryBlendParam.addAll(nativeShader.multiBlendParam);
        android.graphics.RuntimeShader original=nativeShader.shader;
        settings.putBoolean(StatusBarSettings.SAFE_MODE,false);appearance.configure(settings);appearance.bind(button);
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_LANDSCAPE;
        appearance.drawBlur(wrapper,new Canvas(),canvas->appearance.drawGlassContent(engine,canvas,engine::onDrawContent));
        equal(true,engine.recorded!=original);equal(original,nativeShader.shader);equal(null,engine.drawableShaderPaint.getShader());
        LinearGradient fill=(LinearGradient)engine.recorded.inputs.get("c17QsFill");equal(0x40123456,fill.colors[0]);equal(0x10998877,fill.colors[1]);
        engine.contentDirty=false;
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_PORTRAIT;
        appearance.drawBlur(wrapper,new Canvas(),canvas->appearance.drawGlassContent(engine,canvas,engine::onDrawContent));
        equal(true,engine.contentDirty);equal(original,engine.recorded);equal(original,nativeShader.shader);
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_LANDSCAPE;
        appearance.drawBlur(wrapper,new Canvas(),canvas->appearance.drawGlassContent(engine,canvas,engine::onDrawContent));
        equal(0x40123456,((LinearGradient)engine.recorded.inputs.get("c17QsFill")).colors[0]);equal(original,nativeShader.shader);
        engine.contentDirty=false;settings.putBoolean(NotificationClearAppearance.LANDSCAPE_MASTER,false);appearance.configure(settings);
        equal(true,engine.contentDirty);
        appearance.drawBlur(wrapper,new Canvas(),canvas->appearance.drawGlassContent(engine,canvas,engine::onDrawContent));
        // Disabled customization preserves the paint update performed by native rendering.
        equal(original,engine.recorded);equal(original,nativeShader.shader);equal(original,engine.drawableShaderPaint.getShader());
    }
    public static final class LiveCorner {public String type="ROUND";public String getType(){return type;}}
    public static final class LiveMaterialShader extends com.oplus.posteffect.agsl.DrawableShader {
        public final LiveCorner mCornerParams=new LiveCorner();
        public final java.util.List<Object> effects=new java.util.ArrayList<>();
        public Shader bitmap=new LinearGradient(0,0,1,1,new int[]{1,1},new float[]{0,1},Shader.TileMode.CLAMP);
        @Override public java.util.List getAllEffects(){allEffectsReads++;return effects;}
        @Override public void setBaseUniform(android.graphics.RuntimeShader target){super.setBaseUniform(target);target.setInputShader("uniBDFBitmap",bitmap);}
    }
    public static final class LiveMaterial extends com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable {
        public Object currentBlurMixConfig;
    }
    private static final class Recording {
        final NotificationClearAppearance appearance=new NotificationClearAppearance();
        final QsTileAppearance unrelated=new QsTileAppearance();
        final OplusClearAllButton button=new OplusClearAllButton();
        final com.oplusos.systemui.common.blurability.drawable.AutoBlurDrawable wrapper=new com.oplusos.systemui.common.blurability.drawable.AutoBlurDrawable();
        LiveMaterial material=new LiveMaterial();
        com.oplus.posteffect.BlurDrawable engine=material.blurDrawable;
        final LiveMaterialShader shader=new LiveMaterialShader();
        final com.oplus.posteffect.agsl.effects.InnerShadowEffect effect=new com.oplus.posteffect.agsl.effects.InnerShadowEffect();
        final Bundle settings=new Bundle();
        final Canvas canvas=new Canvas();
        int nativeDraws;
        Recording() {
            effect.name="nativeGlassHighlight";shader.effects.add(effect);
            equal(true,QsTileAppearance.setField(engine,"drawableShader",shader));
            material.currentBlurMixConfig=config();
            equal(true,QsTileAppearance.setField(wrapper.viewBlurProxy,"actual",material));
            engine.setBounds(0,0,96,48);wrapper.setBounds(0,0,96,48);button.setBackground(wrapper);
            settings.putBoolean(NotificationClearAppearance.MASTER,true);settings.putInt(NotificationClearAppearance.COLOR_LIGHT,0xff123456);
            appearance.configure(settings);appearance.bind(button);
            ModuleDiagnostics.configure(new Bundle());
        }
        Object config() {
            com.oplusos.systemui.common.blurability.BlurMixConfig.BlurMixMultiWithShader value=
                    new com.oplusos.systemui.common.blurability.BlurMixConfig.BlurMixMultiWithShader(
                            new com.oplusos.systemui.common.blurability.MixColorWithShader(5,shader.multiBlendParam.get(2).color,3,shader.multiBlendParam.get(3).color),
                            new com.oplusos.systemui.common.blurability.MixColorWithShader(5,shader.multiBlendParam.get(0).color,2,shader.multiBlendParam.get(1).color),1f,false,0);
            equal(true,QsTileAppearance.setField(value,"mixMultiShaderParams",shader.multiBlendParam));return value;
        }
        android.graphics.RuntimeShader draw() throws Throwable {
            android.graphics.RuntimeShader original=shader.shader;Shader paint=engine.drawableShaderPaint.getShader();
            appearance.drawBlur(wrapper,canvas,target->appearance.drawGlassContent(engine,target,
                    nested->unrelated.drawGlassContent(engine,nested,actual->{nativeDraws++;engine.onDrawContent(actual);} )));
            equal(original,shader.shader);equal(paint,engine.drawableShaderPaint.getShader());
            equal(true,engine.recorded!=original);return engine.recorded;
        }
        void rebuiltOnce() throws Throwable {
            int before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;draw();
            equal(before+1,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);draw();
            equal(before+1,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);
        }
    }
    /** Actual clear ownership chain: cache only source work, never live bitmap/shape/optical uploads. */
    private static void materialWork() throws Throwable {
        Recording record=new Recording();int before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;
        int uploads=com.oplus.posteffect.agsl.DrawableShader.baseUploads,reads=com.oplus.posteffect.agsl.DrawableShader.allEffectsReads;
        android.graphics.RuntimeShader first=record.draw();
        for(int i=0;i<1000;i++) {
            record.shader.localMatrixScalePointF.x=i+1f;record.effect.opticalValue=i+.25f;
            record.shader.multiBlendParam.get(0).color=((i&255)<<24)|0x123456;
            record.shader.enableBlend=(i&1)==0;
            record.shader.bitmap=new LinearGradient(0,0,1,1,new int[]{i,i},new float[]{0,1},Shader.TileMode.CLAMP);
            android.graphics.RuntimeShader current=record.draw();equal(first,current);
            equal(record.shader.bitmap,current.inputs.get("uniBDFBitmap"));
            equal(i+1f,current.floats.get("nativeMatrixScale")[0]);equal(i+.25f,current.floats.get(record.effect.name)[0]);
            equal((i&255)/255f,current.floats.get("u_multiBlendParams")[9]);equal((i&1)==0?1:0,current.ints.get("uEnableBlend"));
        }
        int builds=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds-before;
        System.out.println("Clear material work: nativeDraws="+record.nativeDraws+", sourceBuilds="+builds+", baseUploads="+(com.oplus.posteffect.agsl.DrawableShader.baseUploads-uploads));
        equal(1001,record.nativeDraws);equal(1001,com.oplus.posteffect.agsl.DrawableShader.baseUploads-uploads);
        equal(1001,com.oplus.posteffect.agsl.DrawableShader.allEffectsReads-reads);equal(1001,record.effect.uploads);
        equal(1,builds);
        Map<?,?> cache=(Map<?,?>)QsTileAppearance.field(record.appearance,"shaders");Object cached=cache.get(record.shader);
        Object uniformScratch=QsTileAppearance.field(cached,"blendUniforms"),boundsScratch=QsTileAppearance.field(cached,"fillBounds");
        equal(true,uniformScratch instanceof float[]);equal(true,boundsScratch instanceof android.graphics.Rect);
        for(int i=0;i<20;i++)record.draw();equal(uniformScratch,QsTileAppearance.field(cached,"blendUniforms"));equal(boundsScratch,QsTileAppearance.field(cached,"fillBounds"));
        equal(0,((java.util.Set<?>)QsTileAppearance.field(record.appearance,"traceStages")).size());
        record.shader.blendAlgorithmMask++;record.rebuiltOnce();record.shader.mCornerParams.type="G2";record.rebuiltOnce();
        record.effect.order++;record.rebuiltOnce();record.effect.name="changedHighlight";record.rebuiltOnce();
        record.effect.structuralKey=42;record.rebuiltOnce();record.effect.enabled=false;record.rebuiltOnce();record.effect.enabled=true;record.rebuiltOnce();
        record.shader.shader=new android.graphics.RuntimeShader("native replacement");record.rebuiltOnce();
        record.material.currentBlurMixConfig=record.config();record.rebuiltOnce();
        java.util.ArrayList<com.oplus.posteffect.agsl.ShaderBlendParam> changed=new java.util.ArrayList<>(record.shader.multiBlendParam);
        equal(true,QsTileAppearance.setField(record.shader,"multiBlendParam",changed));
        equal(true,QsTileAppearance.setField(record.material.currentBlurMixConfig,"mixMultiShaderParams",changed));record.rebuiltOnce();
        equal(true,QsTileAppearance.setField(record.shader,"summaryBlendParam",new java.util.ArrayList<>(record.shader.summaryBlendParam)));record.rebuiltOnce();
        LiveMaterial newMaterial=new LiveMaterial();newMaterial.blurDrawable=record.engine;newMaterial.currentBlurMixConfig=record.material.currentBlurMixConfig;
        record.material=newMaterial;equal(true,QsTileAppearance.setField(record.wrapper.viewBlurProxy,"actual",newMaterial));record.rebuiltOnce();
        record.engine=new com.oplus.posteffect.BlurDrawable();record.engine.setBounds(0,0,96,48);newMaterial.blurDrawable=record.engine;
        equal(true,QsTileAppearance.setField(record.engine,"drawableShader",record.shader));record.rebuiltOnce();
        before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;
        record.engine.setBounds(10,20,210,120);record.draw();equal(before,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);
        LinearGradient fill=(LinearGradient)record.engine.recorded.inputs.get("c17QsFill");equal(100f,(fill.left+fill.right)/2f);equal(50f,(fill.top+fill.bottom)/2f);
        record.settings.putInt(NotificationClearAppearance.COLOR_LIGHT,0xffabcdef);record.appearance.configure(record.settings);record.draw();
        equal(before,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);equal(0xffabcdef,((LinearGradient)record.engine.recorded.inputs.get("c17QsFill")).colors[0]);
        record.effect.structuralKey=new StringBuilder("mutable");before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;
        record.draw();((StringBuilder)record.effect.structuralKey).append(" changed");record.draw();equal(before+2,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);
        record.effect.structuralKey=true;record.effect.failKey=true;before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;
        record.draw();record.draw();equal(before+2,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);record.effect.failKey=false;
        record.shader.effects.set(0,new com.oplus.posteffect.agsl.DrawableShader().effects.get(0));before=com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds;
        record.draw();record.draw();equal(before+2,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);
        int[] nativePasses={0};com.oplus.posteffect.drawable.BlendDrawable foreign=new com.oplus.posteffect.drawable.BlendDrawable();
        record.appearance.drawGlassContent(foreign,record.canvas,target->record.unrelated.drawGlassContent(foreign,target,actual->nativePasses[0]++));
        equal(1,nativePasses[0]);equal(before+2,com.oplus.posteffect.agsl.BlurDrawableShaderBaseStringKt.builds);
        android.graphics.RuntimeShader original=record.shader.shader;
        com.oplus.posteffect.agsl.DrawableShader.failBaseUniform=true;
        try {
            record.appearance.drawBlur(record.wrapper,record.canvas,target->record.appearance.drawGlassContent(record.engine,target,record.engine::onDrawContent));
            equal(original,record.engine.recorded);equal(original,record.shader.shader);
        } finally {com.oplus.posteffect.agsl.DrawableShader.failBaseUniform=false;}
        boolean thrown=false;Shader paint=record.engine.drawableShaderPaint.getShader();
        try {
            record.appearance.drawBlur(record.wrapper,record.canvas,target->record.appearance.drawGlassContent(record.engine,target,actual->{
                record.engine.onDrawContent(actual);throw new IllegalStateException("native draw failed");
            }));
        } catch(IllegalStateException nativeFailure){thrown=true;}
        equal(true,thrown);equal(original,record.shader.shader);equal(paint,record.engine.drawableShaderPaint.getShader());
        equal(null,((ThreadLocal<?>)QsTileAppearance.field(record.appearance,"drawingEngines")).get());
        record.appearance.forget(record.button);
    }
    public static void main(String[] args) throws Throwable {
        migration();
        equal(false,NotificationClearAppearance.BOOLEANS.get(NotificationClearAppearance.LANDSCAPE_MASTER));
        equal(false,NotificationClearAppearance.BOOLEANS.get(NotificationClearAppearance.LANDSCAPE_GRADIENT_ENABLED));
        NotificationClearAppearance appearance=new NotificationClearAppearance();OplusClearAllButton button=new OplusClearAllButton();
        ShapeDrawable background=new ShapeDrawable();background.setBounds(0,0,96,48);button.setBackground(background);
        button.setAlpha(.7f);button.setTranslationX(12f);button.setTranslationY(23f);
        Bundle settings=new Bundle();settings.putBoolean(NotificationClearAppearance.MASTER,true);
        settings.putInt(NotificationClearAppearance.COLOR_LIGHT,0xffff1122);
        settings.putBoolean(NotificationClearAppearance.LANDSCAPE_MASTER,true);
        settings.putInt(NotificationClearAppearance.LANDSCAPE_COLOR_LIGHT,0x80123456);
        settings.putBoolean(StatusBarSettings.alphaKey(NotificationClearAppearance.LANDSCAPE_COLOR_LIGHT),true);
        settings.putInt(NotificationClearAppearance.LANDSCAPE_COLOR_DARK,0xff778899);
        settings.putInt(NotificationClearAppearance.LANDSCAPE_GRADIENT_COLOR_LIGHT,0x20998877);
        settings.putBoolean(StatusBarSettings.alphaKey(NotificationClearAppearance.LANDSCAPE_GRADIENT_COLOR_LIGHT),true);
        settings.putBoolean(NotificationClearAppearance.LANDSCAPE_GRADIENT_ENABLED,true);
        settings.putFloat(NotificationClearAppearance.LANDSCAPE_OPACITY,50f);appearance.configure(settings);
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_PORTRAIT;
        equal(0xffff1122,((LinearGradient)draw(appearance,button,background)).colors[0]);
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_LANDSCAPE;
        LinearGradient landscape=(LinearGradient)draw(appearance,button,background);
        equal(0x40123456,landscape.colors[0]);equal(0x10998877,landscape.colors[1]);
        button.getResources().getConfiguration().uiMode=Configuration.UI_MODE_NIGHT_YES;
        equal(0x80778899,((LinearGradient)draw(appearance,button,background)).colors[0]);
        settings.putBoolean(NotificationClearAppearance.LANDSCAPE_MASTER,false);appearance.configure(settings);
        equal(null,draw(appearance,button,background));
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_PORTRAIT;
        button.getResources().getConfiguration().uiMode=Configuration.UI_MODE_NIGHT_NO;
        equal(0xffff1122,((LinearGradient)draw(appearance,button,background)).colors[0]);
        settings.putBoolean(NotificationClearAppearance.LANDSCAPE_MASTER,true);settings.putBoolean(NotificationClearAppearance.MASTER,false);appearance.configure(settings);
        equal(null,draw(appearance,button,background));
        button.getResources().getConfiguration().orientation=Configuration.ORIENTATION_LANDSCAPE;
        equal(0x40123456,((LinearGradient)draw(appearance,button,background)).colors[0]);
        settings.putBoolean(StatusBarSettings.SAFE_MODE,true);appearance.configure(settings);equal(null,draw(appearance,button,background));
        equal(.7f,button.getAlpha());equal(12f,button.getTranslationX());equal(23f,button.getTranslationY());
        glass(appearance,button,settings);
        materialWork();
        appearance.configure(null);equal(null,draw(appearance,button,background));appearance.forget(button);
        System.out.println("NotificationClearAppearanceCheck passed: "+checks);
    }
}
