// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.drawable.Drawable;
import android.view.View;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedInterface;

/** Fixed audited binding sites. Packages can supply graphics, never hook names or native identities. */
final class IconNativeControlCenterHooks {
    interface Install {void hook(Method method,XposedInterface.Hooker hooker);}
    private static final String CONFIG="android.content.res.Configuration",DRAWABLE="android.graphics.drawable.Drawable";
    private static int errors;
    static boolean install(ClassLoader loader,IconNativeControlCenter adapter,Install install){
        boolean complete=true;
        complete&=family(loader,adapter,install,IconNativeControlCenter.BRIGHTNESS,1,new String[][]{
            {"init"},{"updateIconColor"},{"updateIconConfig"},{"disableTheme","boolean"},{"setBrightnessIcon",DRAWABLE},
            {"setAutoMode","boolean"},{"setIconColorFilter","int","android.graphics.PorterDuff$Mode"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.BRIGHTNESS,2,new String[][]{{"getBrightnessIconMode"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.DETAIL_BRIGHTNESS,1,new String[][]{{"updateIconColor"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.VOLUME,3,new String[][]{
            {"updateIconState",IconNativeControlCenter.VOLUME_STATE},{"updateIconState",IconNativeControlCenter.VOLUME_STATE,"boolean"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.VOLUME,4,new String[][]{{"updateIconColor","boolean","boolean"},{"disableTheme","boolean"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.FOOTER,5,new String[][]{
            {"onFinishInflate"},{"updateResources$11"},{"updateTextColor","int"},{"refreshFooterButtonIconColor"},{"updateQsMoreButton","android.content.res.Resources","boolean"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.MEDIA,6,new String[][]{
            {"setSemanticButton",IconNativeControlCenter.BUTTON,"int",IconNativeControlCenter.MEDIA_ACTION,IconNativeControlCenter.MEDIA_DATA}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.MEDIA,7,new String[][]{
            {"bindViewWithAction",IconNativeControlCenter.BUTTON,IconNativeControlCenter.MEDIA_ACTION,IconNativeControlCenter.MEDIA_DATA}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.MEDIA,8,new String[][]{{"updateMediaOutputBtn","int"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.MEDIA,9,new String[][]{{"bindMediaDataInner",IconNativeControlCenter.MEDIA_DATA,"java.lang.String"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.MEDIA,10,new String[][]{{"unBindMediaData"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.PLAY_PAUSE,11,new String[][]{
            {"setPlaybackState","int"},{"handleAnimationEnd"},{"setImageResourceInternal","int"}});
        for(String owner:new String[]{IconNativeControlCenter.SQUARE_ENTRANCE,IconNativeControlCenter.RECTANGLE_ENTRANCE})
            complete&=family(loader,adapter,install,owner,12,new String[][]{{"g"},{"i"},{"onConfigurationChanged",CONFIG}});
        for(String owner:new String[]{IconNativeControlCenter.SQUARE,IconNativeControlCenter.RECTANGLE})
            complete&=family(loader,adapter,install,owner,12,new String[][]{{"d",IconNativeControlCenter.SDK+"entity.PanelDevice"},{"g"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.RECTANGLE,12,new String[][]{{"onConfigurationChanged",CONFIG}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.PROGRESS,13,new String[][]{{"setDeviceIcon","int"},{"setDeviceIconWithAnimation","int"}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.PROGRESS,14,new String[][]{{"a",DRAWABLE}});
        complete&=family(loader,adapter,install,IconNativeControlCenter.PROGRESS,12,new String[][]{{"a","int","int"},{"onConfigurationChanged",CONFIG}});
        return complete;
    }
    private static boolean family(ClassLoader loader,IconNativeControlCenter adapter,Install install,String owner,int event,String[][] sites){
        try{
            Class<?> type=Class.forName(owner,false,loader);Method[] methods=new Method[sites.length];
            for(int i=0;i<sites.length;i++){String[] site=sites[i];Class<?>[] parameters=new Class<?>[site.length-1];
                for(int p=0;p<parameters.length;p++)parameters[p]=type(loader,site[p+1]);
                methods[i]=type.getDeclaredMethod(site[0],parameters);
                Class<?> returns=event==2?Drawable.class:site[0].equals("updateQsMoreButton")?boolean.class:void.class;
                if(methods[i].getReturnType()!=returns)throw new NoSuchMethodException("Icon host result contract");
            }
            for(Method method:methods)install.hook(method,chain->{Object result=chain.proceed();View view=(View)chain.getThisObject();
                switch(event){
                    case 1:adapter.onBrightnessUpdated(view);break;
                    case 2:return adapter.brightnessOverlay(view,(Drawable)result);
                    case 3:adapter.onVolumeState(view,chain.getArg(0));break;
                    case 4:adapter.onVolumeUpdated(view);break;
                    case 5:adapter.onFooterUpdated(view);break;
                    case 6:adapter.onMediaSemantic(view,(View)chain.getArg(0),(Integer)chain.getArg(1),chain.getArg(2),chain.getArg(3));break;
                    case 7:adapter.onMediaCustom(view,(View)chain.getArg(0));break;
                    case 8:adapter.onMediaOutput(view,(Integer)chain.getArg(0));break;
                    case 9:adapter.onMediaBound(view,chain.getArg(0));break;
                    case 10:adapter.onMediaUnbound(view);break;
                    case 11:adapter.onMediaPlayPause(view);break;
                    case 12:adapter.onDeviceUpdated(view);break;
                    case 13:adapter.onDeviceIcon(view,(Integer)chain.getArg(0));break;
                    case 14:adapter.onDeviceDrawable(view);break;
                    default:throw new AssertionError("Unknown icon binding event");
                }return result;
            });
            return true;
        }catch(ReflectiveOperationException|RuntimeException unavailable){
            if(errors++<8)ModuleDiagnostics.error("icon_packs","Native icon host contract unavailable: "+owner,unavailable);
            return false;
        }
    }
    private static Class<?> type(ClassLoader loader,String name)throws ClassNotFoundException{
        if("int".equals(name))return int.class;if("boolean".equals(name))return boolean.class;return Class.forName(name,false,loader);
    }
    private IconNativeControlCenterHooks(){ }
}
