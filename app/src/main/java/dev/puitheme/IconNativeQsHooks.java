// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.view.View;
import android.widget.ImageView;
import java.lang.reflect.Method;
import io.github.libxposed.api.XposedInterface;

/** The proxy is the single canonical icon binder, including OplusQSIconView/Impl delegates. */
final class IconNativeQsHooks {
    interface Install {void hook(Method method,XposedInterface.Hooker hooker);}
    static void install(ClassLoader loader,IconNativeQs adapter,Install install)throws ReflectiveOperationException{
        Class<?> proxy=Class.forName(IconNativeQs.PROXY,false,loader),state=Class.forName(IconNativeQs.STATE,false,loader),icon=Class.forName("com.android.systemui.plugins.qs.QSTile$Icon",false,loader);
        install.hook(proxy.getDeclaredMethod("setIcon",state,boolean.class),chain->{
            Object owner=chain.getThisObject(),model=chain.getArg(0);boolean animate=(Boolean)chain.getArg(1);IconNativeQs.Ticket ticket=adapter.beforeSetIcon(owner,model,animate);boolean succeeded=false;
            try{Object result=chain.proceed();succeeded=true;return result;}finally{adapter.afterSetIcon(owner,model,animate,ticket,succeeded);}
        });
        install.hook(proxy.getDeclaredMethod("updateIcon",icon,boolean.class,boolean.class),chain->{Object result=chain.proceed();adapter.afterNativeUpdate(chain.getThisObject());return result;});
        install.hook(proxy.getDeclaredMethod("updateIconTint"),chain->{Object result=chain.proceed();adapter.afterNativeTint(chain.getThisObject());return result;});
        install.hook(proxy.getDeclaredMethod("onRecycle"),chain->{adapter.onRecycle(chain.getThisObject());return chain.proceed();});
        try{Class<?> detail=Class.forName(IconNativeQs.DETAIL,false,loader),tile=Class.forName("com.oplus.systemui.qs.slider.detail.tile.QsDetailTile",false,loader);
            install.hook(detail.getDeclaredMethod("init",tile),chain->{Object result=chain.proceed();adapter.bindDetail(chain.getThisObject(),chain.getArg(0));return result;});
        }catch(ReflectiveOperationException unavailable){ModuleDiagnostics.error("icon_packs","QS detail identity bridge unavailable; preserving detail native icons",unavailable);}
        // Framework events cover this exact host even though its OEM subclass does not override them.
        install.hook(View.class.getDeclaredMethod("onVisibilityAggregated",boolean.class),chain->{Object result=chain.proceed();adapter.onVisibilityChanged((View)chain.getThisObject(),(Boolean)chain.getArg(0));return result;});
        install.hook(View.class.getDeclaredMethod("onWindowVisibilityChanged",int.class),chain->{Object result=chain.proceed();adapter.onVisibilityChanged((View)chain.getThisObject(),(Integer)chain.getArg(0)==View.VISIBLE);return result;});
        install.hook(ImageView.class.getDeclaredMethod("setImageAlpha",int.class),chain->{Object result=chain.proceed();adapter.onImageAlphaChanged((View)chain.getThisObject());return result;});
    }
    private IconNativeQsHooks(){ }
}
