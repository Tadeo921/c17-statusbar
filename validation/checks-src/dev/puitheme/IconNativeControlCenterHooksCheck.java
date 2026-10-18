// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.media.controls.shared.model.*;
import com.android.systemui.plugins.qs.QSVolume.VolumeState;
import com.oplus.deviceplugin.sdk.entity.PanelDevice;
import com.oplus.deviceplugin.sdk.ui.view.progressbar.BatteryCircularProgressLayout;
import com.oplus.deviceplugin.sdk.ui.view.separatecardview.*;
import com.oplus.systemui.qs.OplusQSFooterImpl;
import com.oplus.systemui.qs.base.seek.*;
import com.oplus.systemui.qs.media.*;
import com.oplus.systemui.qs.slider.OplusQsDetailHookClipBrightnessView;
import io.github.libxposed.api.XposedInterface;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/** Executes the production installer and after hookers, not a second installer.
 * The frozen descriptors below come from the exact OEM dex dump (APK SHA pinned).
 * Optional argv[0]=local audit directory, argv[1]=actual SystemUI.apk rechecks that
 * evidence directly; OEM code and images remain outside the repository. */
public final class IconNativeControlCenterHooksCheck {
    private static int checks;
    private static void equal(Object expected,Object actual,String why){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Native icon hooks "+checks+' '+why+": "+expected+" != "+actual);}
    private static void require(boolean value,String why){equal(true,value,why);}
    private static final String APK_SHA="da83ae63b6cfc43a4ce57f21ee7a54444e14dada85b24ad36d2fe74181f0a927";
    private static final Map<String,String> AUDITED=new LinkedHashMap<>();
    private static void audit(String owner,String file,String...methods){for(String method:methods)AUDITED.put(owner+'#'+method,file);}
    static{
        audit("com.oplus.systemui.qs.base.seek.ClipBrightnessView","non-tile-extra/seek.ClipBrightnessView.txt",
            "init()V","updateIconColor()V","updateIconConfig()V","disableTheme(Z)V","setBrightnessIcon(Landroid/graphics/drawable/Drawable;)V",
            "setAutoMode(Z)V","setIconColorFilter(ILandroid/graphics/PorterDuff$Mode;)V","getBrightnessIconMode()Landroid/graphics/drawable/Drawable;");
        audit("com.oplus.systemui.qs.slider.OplusQsDetailHookClipBrightnessView","non-tile-extra/slider.OplusQsDetailHookClipBrightnessView.txt","updateIconColor()V");
        audit("com.oplus.systemui.qs.base.seek.OplusQsVolumeIconView","non-tile/seek.OplusQsVolumeIconView.txt",
            "updateIconState(Lcom/android/systemui/plugins/qs/QSVolume$VolumeState;)V","updateIconState(Lcom/android/systemui/plugins/qs/QSVolume$VolumeState;Z)V","updateIconColor(ZZ)V","disableTheme(Z)V");
        audit("com.oplus.systemui.qs.OplusQSFooterImpl","non-tile/qs.OplusQSFooterImpl.txt","onFinishInflate()V","updateResources$11()V","updateTextColor(I)V","refreshFooterButtonIconColor()V","updateQsMoreButton(Landroid/content/res/Resources;Z)Z");
        audit("com.oplus.systemui.qs.media.OplusQsBaseMediaPanelView","native-runtime/OplusQsBaseMediaPanelView.txt",
            "setSemanticButton(Lcom/oplus/systemui/qs/media/OplusLottieButton;ILcom/android/systemui/media/controls/shared/model/MediaAction;Lcom/android/systemui/media/controls/shared/model/MediaData;)V",
            "bindViewWithAction(Lcom/oplus/systemui/qs/media/OplusLottieButton;Lcom/android/systemui/media/controls/shared/model/MediaAction;Lcom/android/systemui/media/controls/shared/model/MediaData;)V",
            "updateMediaOutputBtn(I)V","bindMediaDataInner(Lcom/android/systemui/media/controls/shared/model/MediaData;Ljava/lang/String;)V","unBindMediaData()V");
        audit("com.oplus.systemui.qs.media.OplusQsPlayPauseButton","native-runtime/OplusQsPlayPauseButton.txt","setPlaybackState(I)V","handleAnimationEnd()V","setImageResourceInternal(I)V");
        for(String name:new String[]{"SquareEntranceCardView","RectangleEntranceCardView"})audit("com.oplus.deviceplugin.sdk.ui.view.separatecardview."+name,"non-tile/separatecardview."+name+".txt","g()V","i()V","onConfigurationChanged(Landroid/content/res/Configuration;)V");
        audit("com.oplus.deviceplugin.sdk.ui.view.separatecardview.SquareDeviceCardView","non-tile/separatecardview.SquareDeviceCardView.txt","d(Lcom/oplus/deviceplugin/sdk/entity/PanelDevice;)V","g()V");
        audit("com.oplus.deviceplugin.sdk.ui.view.separatecardview.RectangleDeviceCardView","non-tile/separatecardview.RectangleDeviceCardView.txt","d(Lcom/oplus/deviceplugin/sdk/entity/PanelDevice;)V","g()V","onConfigurationChanged(Landroid/content/res/Configuration;)V");
        audit("com.oplus.deviceplugin.sdk.ui.view.progressbar.BatteryCircularProgressLayout","non-tile/progressbar.BatteryCircularProgressLayout.txt","setDeviceIcon(I)V","setDeviceIconWithAnimation(I)V","a(Landroid/graphics/drawable/Drawable;)V","a(II)V","onConfigurationChanged(Landroid/content/res/Configuration;)V");
    }
    private static String descriptor(Class<?> type){if(type.isArray())return type.getName().replace('.','/');if(!type.isPrimitive())return 'L'+type.getName().replace('.','/')+';';if(type==void.class)return "V";if(type==int.class)return "I";if(type==boolean.class)return "Z";throw new AssertionError(type);}
    private static String signature(Method method){StringBuilder key=new StringBuilder(method.getDeclaringClass().getName()).append('#').append(method.getName()).append('(');for(Class<?> type:method.getParameterTypes())key.append(descriptor(type));return key.append(')').append(descriptor(method.getReturnType())).toString();}
    private interface Original{Object run()throws Throwable;}
    private static final class Native extends Drawable{
        public void draw(Canvas canvas){}public void setAlpha(int value){}public void setColorFilter(ColorFilter value){}public int getOpacity(){return -3;}
    }
    private static final class Wrapped extends Drawable{
        final Drawable original;final String role;Wrapped(Drawable original,String role){this.original=original;this.role=role;}
        public void draw(Canvas canvas){original.draw(canvas);}public void setAlpha(int value){original.setAlpha(value);}public void setColorFilter(ColorFilter value){original.setColorFilter(value);}public int getOpacity(){return original.getOpacity();}
    }
    private static final class Graphics implements IconNativeControlCenter.Graphics{
        boolean originalFinished=true;int wraps,requests;
        private void after(){require(originalFinished,"production adapter runs after native original completed");}
        public Drawable wrap(View owner,String role,Drawable original){after();wraps++;return new Wrapped(original,role);}
        public Drawable unwrap(Drawable drawable){return drawable instanceof Wrapped?((Wrapped)drawable).original:drawable;}
        public boolean hasAppearance(String role){after();return true;}
        public void nativeTint(Drawable drawable,int color){after();}
        public void request(View owner,String role,int size){after();requests++;}
    }
    /** Same successful-Executable deduplication boundary as StatusBarModule.installHook. */
    private static final class Framework implements IconNativeControlCenterHooks.Install{
        final Map<Method,XposedInterface.Hooker> hooks=new LinkedHashMap<>();final Set<Method> installed=new HashSet<>();String failOnce;int registrations;
        public void hook(Method method,XposedInterface.Hooker hooker){
            if(installed.contains(method))return;if(signature(method).equals(failOnce)){failOnce=null;throw new IllegalStateException("controlled framework registration failure");}
            require(hooks.put(method,hooker)==null,"framework never replaces a successfully installed hook");installed.add(method);registrations++;
        }
        Method method(String owner,String name,Class<?>...params)throws Exception{return Class.forName(owner).getDeclaredMethod(name,params);}
        Object run(Graphics graphics,Method method,Object receiver,Object[] args,Original original)throws Throwable{
            XposedInterface.Hooker hook=hooks.get(method);require(hook!=null,"real installer registered exact executable "+signature(method));final int[] proceeds={0};graphics.originalFinished=false;
            XposedInterface.Chain chain=(XposedInterface.Chain)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{XposedInterface.Chain.class},(proxy,called,values)->{
                switch(called.getName()){
                    case "getThisObject":return receiver;
                    case "getArg":return args[(Integer)values[0]];
                    case "getArgs":return Arrays.asList(args);
                    case "getExecutable":return method;
                    case "proceed":proceeds[0]++;Object result=original.run();graphics.originalFinished=true;return result;
                    default:throw new AssertionError(called.getName());
                }
            });
            try{return hook.intercept(chain);}finally{equal(1,proceeds[0],"each hook proceeds native original exactly once");graphics.originalFinished=true;}
        }
    }
    private static String role(Drawable drawable){return drawable instanceof Wrapped?((Wrapped)drawable).role:"native";}
    private static Object call(Framework framework,Graphics graphics,String owner,String name,Class<?>[] params,Object receiver,Object[] args,Original original)throws Throwable{return framework.run(graphics,framework.method(owner,name,params),receiver,args,original);}
    private static void installation()throws Exception{
        equal(42,AUDITED.size(),"exact audited native executable count");Graphics graphics=new Graphics();IconNativeControlCenter adapter=new IconNativeControlCenter(graphics);Framework framework=new Framework();
        require(IconNativeControlCenterHooks.install(IconNativeControlCenterHooksCheck.class.getClassLoader(),adapter,framework),"all exact native families install");Set<String> actual=new LinkedHashSet<>();for(Method method:framework.hooks.keySet())actual.add(signature(method));equal(AUDITED.keySet(),actual,"installer requests exactly the audited descriptors and return types");
        int count=framework.registrations;require(IconNativeControlCenterHooks.install(IconNativeControlCenterHooksCheck.class.getClassLoader(),adapter,framework),"same native installation retry succeeds");equal(count,framework.registrations,"successful Executables remain deduplicated across retries");
        Framework failed=new Framework();failed.failOnce=IconNativeControlCenter.FOOTER+"#updateTextColor(I)V";
        equal(false,IconNativeControlCenterHooks.install(IconNativeControlCenterHooksCheck.class.getClassLoader(),adapter,failed),"partial framework failure reports incomplete family");require(failed.hooks.keySet().stream().anyMatch(method->method.getDeclaringClass().getName().equals(IconNativeControlCenter.PROGRESS)),"failed footer family does not block later device families");
        require(IconNativeControlCenterHooks.install(IconNativeControlCenterHooksCheck.class.getClassLoader(),adapter,failed),"failed method is retried with already successful methods preserved");equal(AUDITED.size(),failed.registrations,"partial installation recovery installs every executable once");
        final Set<String> missing=new HashSet<>(Arrays.asList(IconNativeControlCenter.BRIGHTNESS,IconNativeControlCenter.FOOTER));ClassLoader unavailable=new ClassLoader(IconNativeControlCenterHooksCheck.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{if(missing.contains(name))throw new ClassNotFoundException("controlled absent native family");return super.loadClass(name,resolve);}
        };Framework recovered=new Framework();equal(false,IconNativeControlCenterHooks.install(unavailable,adapter,recovered),"missing native hosts preserve native and report incomplete");equal(29,recovered.registrations,"missing independent families leave all other exact hosts installed");missing.clear();require(IconNativeControlCenterHooks.install(unavailable,adapter,recovered),"same loader can recover later available families");equal(42,recovered.registrations,"recovered loader installs missing families without duplicating existing hooks");adapter.releaseRuntime();
    }
    private static void brightnessFooter(Framework framework,Graphics graphics)throws Throwable{
        ClipBrightnessView brightness=new ClipBrightnessView();
        for(String name:new String[]{"init","updateIconColor","updateIconConfig","disableTheme","setBrightnessIcon","setAutoMode","setIconColorFilter"}){
            Class<?>[] types=name.equals("disableTheme")||name.equals("setAutoMode")?new Class<?>[]{boolean.class}:name.equals("setBrightnessIcon")?new Class<?>[]{Drawable.class}:name.equals("setIconColorFilter")?new Class<?>[]{int.class,android.graphics.PorterDuff.Mode.class}:new Class<?>[0];
            Object[] args=types.length==0?new Object[0]:types[0]==boolean.class?new Object[]{true}:types[0]==Drawable.class?new Object[]{new Native()}:new Object[]{0xff123456,android.graphics.PorterDuff.Mode.SRC_IN};Native source=new Native();
            Object result=call(framework,graphics,IconNativeControlCenter.BRIGHTNESS,name,types,brightness,args,()->{brightness.setBrightnessIcon(source);brightness.auto=true;return null;});equal(null,result,"void native brightness result preserved");equal("slider.brightness.default",role(brightness.getBrightnessIcon()),"brightness update wraps latest native field for "+name);equal(source,graphics.unwrap(brightness.getBrightnessIcon()),"after callback retains latest source identity");
        }
        Native overlay=new Native();Object output=call(framework,graphics,IconNativeControlCenter.BRIGHTNESS,"getBrightnessIconMode",new Class<?>[0],brightness,new Object[0],()->overlay);equal("slider.brightness_auto.default",role((Drawable)output),"getter hook replaces actual returned overlay");equal(overlay,graphics.unwrap((Drawable)output),"overlay replacement wraps exact original return");
        OplusQsDetailHookClipBrightnessView detail=new OplusQsDetailHookClipBrightnessView();call(framework,graphics,IconNativeControlCenter.DETAIL_BRIGHTNESS,"updateIconColor",new Class<?>[0],detail,new Object[0],()->{detail.setBrightnessIcon(new Native());return null;});equal("slider.brightness.default",role(detail.getBrightnessIcon()),"child override has its own after binding");
        int wraps=graphics.wraps;Method method=framework.method(IconNativeControlCenter.BRIGHTNESS,"updateIconColor");IllegalStateException originalFailure=new IllegalStateException("controlled original failure");try{framework.run(graphics,method,brightness,new Object[0],()->{throw originalFailure;});throw new AssertionError("native exception swallowed");}catch(IllegalStateException expected){equal(originalFailure,expected,"native original exception preserves identity");}equal(wraps,graphics.wraps,"throwing native original performs no after binding");
        OplusQSFooterImpl footer=new OplusQSFooterImpl();
        for(String name:new String[]{"onFinishInflate","updateResources$11","updateTextColor","refreshFooterButtonIconColor","updateQsMoreButton"}){
            Class<?>[] types=name.equals("updateTextColor")?new Class<?>[]{int.class}:name.equals("updateQsMoreButton")?new Class<?>[]{android.content.res.Resources.class,boolean.class}:new Class<?>[0];Object[] args=types.length==0?new Object[0]:types.length==1?new Object[]{0xff123456}:new Object[]{footer.getResources(),true};
            Object returned=call(framework,graphics,IconNativeControlCenter.FOOTER,name,types,footer,args,()->{footer.mSettingsButtonIcon.setImageDrawable(new Native());footer.mMoreIcon.setImageDrawable(new Native());return name.equals("updateQsMoreButton")?Boolean.FALSE:null;});
            equal(name.equals("updateQsMoreButton")?Boolean.FALSE:null,returned,"footer native boolean/void result preserved");equal("footer.settings.default",role(footer.mSettingsButtonIcon.getDrawable()),"footer settings callback "+name);equal("footer.more.default",role(footer.mMoreIcon.getDrawable()),"footer more callback "+name);
        }
    }
    private static void volumeMedia(Framework framework,Graphics graphics)throws Throwable{
        OplusQsVolumeIconView volume=new OplusQsVolumeIconView();VolumeState state=new VolumeState(2,6,10,false);
        for(Class<?>[] types:new Class<?>[][]{{VolumeState.class},{VolumeState.class,boolean.class}}){Object[] args=types.length==1?new Object[]{state}:new Object[]{state,false};call(framework,graphics,IconNativeControlCenter.VOLUME,"updateIconState",types,volume,args,()->{volume.nativeState(state);volume.setImageDrawable(new Native());return null;});equal("slider.volume.normal.medium",role(volume.getDrawable()),"volume after callback uses exact first model argument");}
        for(String name:new String[]{"updateIconColor","disableTheme"}){Class<?>[] types=name.equals("updateIconColor")?new Class<?>[]{boolean.class,boolean.class}:new Class<?>[]{boolean.class};Object[] args=types.length==1?new Object[]{false}:new Object[]{false,true};call(framework,graphics,IconNativeControlCenter.VOLUME,name,types,volume,args,()->{volume.setImageDrawable(new Native());return null;});equal("slider.volume.normal.medium",role(volume.getDrawable()),"volume appearance hook re-reads actual model");}
        OplusQsBaseMediaPanelView media=new OplusQsBaseMediaPanelView();MediaAction standard=new MediaAction(true,()->{});MediaData data=new MediaData();
        Class<?>[] semantic={OplusLottieButton.class,int.class,MediaAction.class,MediaData.class};
        call(framework,graphics,IconNativeControlCenter.MEDIA,"setSemanticButton",semantic,media,new Object[]{media.preBtn,0x7f080e5a,standard,data},()->{media.preBtn.getImageView().setImageDrawable(new Native());return null;});equal("media.previous.default",role(media.preBtn.getImageView().getDrawable()),"semantic binder forwards button/resource/action/data unchanged");
        call(framework,graphics,IconNativeControlCenter.MEDIA,"setSemanticButton",semantic,media,new Object[]{media.playOrPauseBtn,0x7f080e58,standard,data},()->{media.playOrPauseBtn.getImageView().setImageDrawable(new Native());return null;});
        for(String name:new String[]{"setPlaybackState","handleAnimationEnd","setImageResourceInternal"}){Class<?>[] types=name.equals("handleAnimationEnd")?new Class<?>[0]:new Class<?>[]{int.class};Object[] args=types.length==0?new Object[0]:new Object[]{3};call(framework,graphics,IconNativeControlCenter.PLAY_PAUSE,name,types,media.playOrPauseBtn,args,()->{media.playOrPauseBtn.state=3;media.playOrPauseBtn.getImageView().setImageDrawable(new Native());return null;});equal("media.play_pause.pause",role(media.playOrPauseBtn.getImageView().getDrawable()),"play/pause callback reads native末态 "+name);}
        Native custom=new Native();call(framework,graphics,IconNativeControlCenter.MEDIA,"bindViewWithAction",new Class<?>[]{OplusLottieButton.class,MediaAction.class,MediaData.class},media,new Object[]{media.preBtn,standard,data},()->{media.preBtn.getImageView().setImageDrawable(custom);return null;});equal(custom,media.preBtn.getImageView().getDrawable(),"custom-action binder leaves latest notification artwork");
        call(framework,graphics,IconNativeControlCenter.MEDIA,"updateMediaOutputBtn",new Class<?>[]{int.class},media,new Object[]{2},()->{media.mediaOutputBtn.setImageDrawable(new Native());return null;});equal("media.output.default",role(media.mediaOutputBtn.getDrawable()),"output callback forwards actual route argument");
        Native device=new Native();data.device=new MediaDeviceData();data.device.resource=1;call(framework,graphics,IconNativeControlCenter.MEDIA,"bindMediaDataInner",new Class<?>[]{MediaData.class,String.class},media,new Object[]{data,"key"},()->{media.mediaOutputBtn.setImageDrawable(device);return null;});equal(device,media.mediaOutputBtn.getDrawable(),"bound real device artwork overrides generic route");
        call(framework,graphics,IconNativeControlCenter.MEDIA,"unBindMediaData",new Class<?>[0],media,new Object[0],()->null);equal("native",role(media.playOrPauseBtn.getImageView().getDrawable()),"unbound media callback releases standard binding");
    }
    private static final class Square extends SquareDeviceCardView{
        final android.content.res.Resources glyphs=new android.content.res.Resources(){@Override public int getIdentifier(String name,String type,String pkg){return name.equals("device_card_phone")&&type.equals("drawable")&&pkg.equals("com.android.systemui")?700:0;}};
        @Override public android.content.res.Resources getResources(){return glyphs;}
    }
    private static void devices(Framework framework,Graphics graphics)throws Throwable{
        for(View entrance:new View[]{new SquareEntranceCardView(),new RectangleEntranceCardView()}){
            ImageView image=entrance instanceof SquareEntranceCardView?((SquareEntranceCardView)entrance).m:((RectangleEntranceCardView)entrance).p;
            for(String name:new String[]{"g","i","onConfigurationChanged"}){Class<?>[] types=name.equals("onConfigurationChanged")?new Class<?>[]{android.content.res.Configuration.class}:new Class<?>[0];Object[] args=types.length==0?new Object[0]:new Object[]{new android.content.res.Configuration()};call(framework,graphics,entrance.getClass().getName(),name,types,entrance,args,()->{image.setImageDrawable(new Native());return null;});equal("device.entrance.default",role(image.getDrawable()),"both real entrance classes bind after "+name);}
        }
        Square square=new Square();square.q=new PanelDevice(1);BatteryCircularProgressLayout progress=square.p;
        for(String name:new String[]{"setDeviceIcon","setDeviceIconWithAnimation"}){call(framework,graphics,IconNativeControlCenter.PROGRESS,name,new Class<?>[]{int.class},progress,new Object[]{700},()->{progress.f.setImageDrawable(new Native());return null;});equal("device.phone.default",role(progress.f.getDrawable()),"resource callback forwards generic device identity "+name);}
        for(String name:new String[]{"d","g"}){Class<?>[] types=name.equals("d")?new Class<?>[]{PanelDevice.class}:new Class<?>[0];Object[] args=types.length==0?new Object[0]:new Object[]{square.q};call(framework,graphics,IconNativeControlCenter.SQUARE,name,types,square,args,()->{progress.f.setImageDrawable(new Native());return null;});equal("device.phone.default",role(progress.f.getDrawable()),"device-card update refreshes after native "+name);}
        for(String name:new String[]{"a","onConfigurationChanged"}){Class<?>[] types=name.equals("a")?new Class<?>[]{int.class,int.class}:new Class<?>[]{android.content.res.Configuration.class};Object[] args=types.length==2?new Object[]{1,2}:new Object[]{new android.content.res.Configuration()};call(framework,graphics,IconNativeControlCenter.PROGRESS,name,types,progress,args,()->{progress.f.setImageDrawable(new Native());return null;});equal("device.phone.default",role(progress.f.getDrawable()),"progress update refreshes native icon "+name);}
        Native brand=new Native();call(framework,graphics,IconNativeControlCenter.PROGRESS,"a",new Class<?>[]{Drawable.class},progress,new Object[]{brand},()->{progress.f.setImageDrawable(brand);return null;});equal(brand,progress.f.getDrawable(),"Drawable overload clears previous generic resource identity");
        RectangleDeviceCardView partial=new RectangleDeviceCardView();for(String name:new String[]{"d","g","onConfigurationChanged"}){Class<?>[] types=name.equals("d")?new Class<?>[]{PanelDevice.class}:name.equals("onConfigurationChanged")?new Class<?>[]{android.content.res.Configuration.class}:new Class<?>[0];Object[] args=types.length==0?new Object[0]:types[0]==PanelDevice.class?new Object[]{new PanelDevice(1)}:new Object[]{new android.content.res.Configuration()};int wraps=graphics.wraps;call(framework,graphics,IconNativeControlCenter.RECTANGLE,name,types,partial,args,()->null);equal(wraps,graphics.wraps,"unknown partial rectangle host keeps native after "+name);}
    }
    private static void callbacks()throws Throwable{
        Graphics graphics=new Graphics();IconNativeControlCenter adapter=new IconNativeControlCenter(graphics);adapter.configure(true);Framework framework=new Framework();require(IconNativeControlCenterHooks.install(IconNativeControlCenterHooksCheck.class.getClassLoader(),adapter,framework),"actual callback installer succeeds");
        try{brightnessFooter(framework,graphics);volumeMedia(framework,graphics);devices(framework,graphics);}finally{adapter.releaseRuntime();}
    }
    private static void actualEvidence(String[] args)throws Exception{
        if(args.length==0)return;Path root=Paths.get(args[0]);Pattern pair=Pattern.compile("name\\s*:\\s*'([^']+)'\\s*\\n\\s*type\\s*:\\s*'([^']+)'");Map<String,String> files=new HashMap<>();
        for(Map.Entry<String,String> entry:AUDITED.entrySet()){
            String text=files.get(entry.getValue());if(text==null){text=new String(Files.readAllBytes(root.resolve(entry.getValue())),StandardCharsets.UTF_8);files.put(entry.getValue(),text);}String key=entry.getKey(),owner=key.substring(0,key.indexOf('#')),method=key.substring(key.indexOf('#')+1);
            require(text.contains("Class descriptor  : 'L"+owner.replace('.','/')+";'"),"exact OEM class identity "+owner);Matcher matcher=pair.matcher(text);boolean found=false;while(matcher.find())if((matcher.group(1)+matcher.group(2)).equals(method)){found=true;break;}require(found,"real OEM dex dump declares exact method "+key);
        }
        if(args.length>1){MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream input=Files.newInputStream(Paths.get(args[1]))){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1)digest.update(buffer,0,n);}StringBuilder hex=new StringBuilder();for(byte value:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",value&255));equal(APK_SHA,hex.toString(),"source APK bytes match pinned audited revision");}
    }
    public static void main(String[] args)throws Throwable{
        actualEvidence(args);installation();callbacks();System.out.println("IconNativeControlCenterHooksCheck passed: "+checks+" (42 exact OEM descriptors; real production installer/after callbacks; ART interception remains device validation)");
    }
}
