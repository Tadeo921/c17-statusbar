package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.plugins.shared.template.component.light.f;
import com.oplus.systemui.plugins.shared.template.component.media.view.MediaBackGroundView;
import com.oplus.systemui.plugins.shared.template.section.media.K0;
import com.oplus.systemui.plugins.shared.template.section.media.s1;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;

/** Real owner identities and scene gates, not global Drawable/tint hooks. */
public final class FluidCloudMediaStyleCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args) throws Exception {
        ClassLoader loader=FluidCloudMediaStyleCheck.class.getClassLoader();
        FluidCloudStyle style=new FluidCloudStyle(); FluidCloudAccent accent=new FluidCloudAccent();
        style.setAccentController(accent); accent.resolve(loader); style.resolveMedia(loader); style.resolveNotification(loader);
        ViewGroup card=new ViewGroup(new android.content.Context());
        MediaBackGroundView host=new MediaBackGroundView(); host.parent=card;
        host.F=new f(); host.setBackground(host.F);
        K0 model=new K0(new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff123456)));
        com.oplus.systemui.plugins.shared.template.page.entity.t page=new com.oplus.systemui.plugins.shared.template.page.entity.t();
        page.u=new com.oplus.systemui.plugins.shared.template.page.view.PageBackgroundView();page.u.b.setBounds(0,0,100,80);
        s1 section=new s1(new android.content.Context(),model,card,page);
        section.q=host; style.mediaChanged(section);
        check(!style.suppressMediaLight(host.F),"native decorations remain while all switches are OFF");
        Bundle config=new Bundle(); config.putBoolean(FluidCloudStyle.MASTER,true); config.putBoolean(FluidCloudStyle.SCOPE_SHADE,false); config.putBoolean(C17HighlightRemoval.ENABLED,true); style.configure(config); Handler.drainForCheck();
        check(style.suppressMediaLight(host.F),"desktop media decoration covered independently of outer CardBackgroundView");
        check(!style.suppressMediaLight(new f()),"unknown light drawable is never suppressed");
        config.putBoolean(C17HighlightRemoval.ENABLED,false); config.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        style.configure(config); ((kotlinx.coroutines.flow.p)model.U.b).value=Boolean.TRUE; style.mediaChanged(section); Handler.drainForCheck();
        check(!style.suppressMediaLight(host.F),"following the notification style alone never takes a music card's glow away");
        config.putBoolean(C17HighlightRemoval.ENABLED,true); style.configure(config); style.mediaChanged(section); Handler.drainForCheck();
        check(style.suppressMediaLight(host.F),"the user's own highlight removal still removes the glow from a notification music card");
        config.putBoolean(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,true); style.configure(config); style.mediaChanged(section); Handler.drainForCheck();
        check(!style.suppressMediaLight(host.F),"keeping the music highlight wins over the removal");
        config.putBoolean(C17HighlightRemoval.ENABLED,false); config.putBoolean(FluidCloudStyle.MEDIA_KEEP_HIGHLIGHT,false);
        style.configure(config); style.mediaChanged(section); Handler.drainForCheck();
        OplusCustomRow row=new OplusCustomRow(); NotificationBackgroundView body=new NotificationBackgroundView();
        row.setMBackgroundNormal(body); CustomRowEntry entry=new CustomRowEntry(); entry.immersiveCardView=card; row.setEntry(entry);
        final Integer[] applied={null}; final boolean[] following={false}, protectedAccent={false};
        style.setNotificationStyler((target,follow,keep)->{ if(target==body){applied[0]=style.normalNotificationColor(target);
            following[0]=follow; protectedAccent[0]=style.notificationUsesAccent(target);}});
        config.putBoolean(FluidCloudAccent.ENABLED,true); accent.configure(config); style.notificationRow(row); style.setKeyguard(true); Handler.drainForCheck();
        check(following[0]&&protectedAccent[0]&&applied[0]==0xff123456,"exact immersive card uses native primary color on lockscreen as soon as the accent is on");
        android.graphics.Canvas materialCanvas=new android.graphics.Canvas();
        try{style.drawMediaPage(page.u.b,materialCanvas,()->null);}catch(Throwable failed){throw new AssertionError(failed);}
        check(materialCanvas.layers==0,"native page animated lights are not recoloured as an opaque material base");
        android.view.SurfaceView remoteCard=new android.view.SurfaceView();Object surfaceHost=new Object();
        android.view.SurfaceControl nativeLayer=new android.view.SurfaceControl("music",null);
        android.view.SurfaceControl copyLayer=new android.view.SurfaceControl("copy",null);copyLayer.sameLayerForCheck=nativeLayer;
        FluidCloudSurfaceRoots.hostView(surfaceHost,card);
        FluidCloudSurfaceRoots.hostPackage(surfaceHost,new android.view.SurfaceControlViewHost.SurfacePackage(nativeLayer));
        FluidCloudSurfaceRoots.childPackage(remoteCard,new android.view.SurfaceControlViewHost.SurfacePackage(copyLayer));
        entry.immersiveCardView=remoteCard;style.notificationRow(row);Handler.drainForCheck();
        check(protectedAccent[0]&&applied[0]==0xff123456,"Surface package copies preserve per-entry media owner without a View parent chain");
        entry.immersiveCardView=new ViewGroup(new android.content.Context());style.notificationRow(row);Handler.drainForCheck();
        android.graphics.Canvas afterRebind=new android.graphics.Canvas();
        try{style.drawMediaPage(page.u.b,afterRebind,()->null);}catch(Throwable failed){throw new AssertionError(failed);}
        check(!protectedAccent[0]&&afterRebind.layers==0,"recycled non-media entry restores the former page accent immediately");
        entry.immersiveCardView=card;style.notificationRow(row);Handler.drainForCheck();FluidCloudSurfaceRoots.releaseHost(surfaceHost);
        com.android.systemui.statusbar.policy.KeyguardStateControllerImpl keyguard=new com.android.systemui.statusbar.policy.KeyguardStateControllerImpl();
        com.android.systemui.statusbar.StatusBarStateControllerImpl state=new com.android.systemui.statusbar.StatusBarStateControllerImpl();
        keyguard.showingForCheck(true); style.onNativeKeyguard(keyguard); state.state=2; style.onNativeState(state); Handler.drainForCheck();
        check(protectedAccent[0],"locked shade state retains actual showing lockscreen accent");
        keyguard.showingForCheck(false); style.onNativeKeyguard(keyguard); state.state=1; style.onNativeState(state); Handler.drainForCheck();
        check(!protectedAccent[0],"actual keyguard unlock takes priority over stale status enum, and the shade card gets its own colour back");
        keyguard.showingForCheck(true); style.onNativeKeyguard(keyguard); Handler.drainForCheck();
        config.putBoolean(FluidCloudAccent.MASK_ENABLED,true); config.putInt(FluidCloudAccent.MASK_COLOR,0xff000000);
        config.putFloat(FluidCloudAccent.MASK_OPACITY,50f); accent.configure(config); style.refreshAfterAccent(); Handler.drainForCheck();
        check(applied[0]==FluidCloudAccent.mask(0xff123456,0xff000000,50f),"lockscreen uses identical latest mask recipe");
        ((kotlinx.coroutines.flow.p)model.N.b).value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xffabcdef); style.mediaChanged(section); Handler.drainForCheck();
        check(applied[0]==FluidCloudAccent.mask(0xffabcdef,0xff000000,50f),"media event updates color without replacing artwork or stopping native observers");
        int invalidations=host.invalidations,pageInvalidations=page.u.invalidations;
        int pending=Handler.postedForCheck.size();
        for(int i=0;i<1000;i++)style.mediaChanged(section);
        check(host.invalidations==invalidations&&page.u.invalidations==pageInvalidations&&Handler.postedForCheck.size()==pending,"unchanged native media events keep registered drawables and do not enqueue or invalidate");
        style.setKeyguard(false); Handler.drainForCheck(); check(!protectedAccent[0],"unlock removes the media accent: the shade is never recoloured");
        style.setKeyguard(true); config.putBoolean(FluidCloudAccent.ENABLED,false); accent.configure(config);
        style.refreshAfterAccent(); Handler.drainForCheck(); check(!protectedAccent[0],"turning the media accent OFF restores followed notification recipe");
        config.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false); style.configure(config); Handler.drainForCheck();
        check(!following[0]&&!style.suppressMediaLight(host.F),"independent disable restores body and native decoration");
        entry.immersiveCardView=null;style.notificationRow(row);style.setKeyguard(false);
        // The desktop (heads-up) removal is on; the notification removal stays off, so a lock-screen music card keeps its glow.
        config.putBoolean(C17HighlightRemoval.ENABLED,true); config.putBoolean(C17HighlightRemoval.NOTIFICATION_ENABLED,false);
        style.configure(config); ((kotlinx.coroutines.flow.p)model.U.b).value=Boolean.FALSE;
        style.mediaChanged(section); Handler.drainForCheck(); check(style.suppressMediaLight(host.F),"native desktop ownership rebinds");
        style.mediaDisposed(section); check(!style.suppressMediaLight(host.F),"disposed instance cannot retain borrowed material ownership");
        ((kotlinx.coroutines.flow.p)model.U.b).value=Boolean.TRUE;entry.immersiveCardView=card;style.notificationRow(row);
        config.putBoolean(FluidCloudAccent.ENABLED,true);accent.configure(config);style.setKeyguard(true);
        style.mediaChanged(section);
        s1 replacement=new s1(new android.content.Context(),model,card,page);replacement.q=host;style.mediaChanged(replacement);
        style.mediaDisposed(section);Handler.drainForCheck();android.graphics.Canvas replacedCanvas=new android.graphics.Canvas();
        try{style.drawMediaPage(page.u.b,replacedCanvas,()->null);}catch(Throwable failed){throw new AssertionError(failed);}
        Handler.drainForCheck();check(protectedAccent[0]&&replacedCanvas.layers==0&&!style.suppressMediaLight(page.u.b.c),"disposing old section retains new lockscreen accent and native highlight; floating highlight setting stays isolated");
        style.mediaDisposed(replacement);Handler.drainForCheck();check(!protectedAccent[0],"last media disposal restores the associated notification body");
        section.q=null;style.mediaChanged(section);entry.immersiveCardView=card;style.notificationRow(row);Handler.drainForCheck();
        check(protectedAccent[0]&&applied[0]==FluidCloudAccent.mask(0xffabcdef,0xff000000,50f),"lockscreen exact s1.l owner keeps its primary colour before optional q decoration exists");
        card.attached=false;for(android.view.View.OnAttachStateChangeListener listener:new java.util.ArrayList<>(card.attachListeners))listener.onViewDetachedFromWindow(card);Handler.drainForCheck();
        check(!protectedAccent[0],"detached media root loses lockscreen material ownership");
        card.attached=true;for(android.view.View.OnAttachStateChangeListener listener:new java.util.ArrayList<>(card.attachListeners))listener.onViewAttachedToWindow(card);Handler.drainForCheck();
        check(protectedAccent[0],"reattached native media root restores exact lockscreen colour without a timer");
        final int[] callbacks={0};
        style.setNotificationStyler((target,follow,keep)->{callbacks[0]++;style.notificationRow(row);});Handler.drainForCheck();
        config.putBoolean(FluidCloudAccent.MASK_ENABLED,false);accent.configure(config);Handler.drainForCheck();
        check(callbacks[0]<=1&&Handler.postedForCheck.isEmpty(),"native material callback cannot recursively rebind and enqueue another refresh batch");
        style.mediaChanged(section); style.release(); accent.release(); Handler.drainForCheck();
        check(!style.suppressMediaLight(host.F),"release bypasses all material transformations");
        System.out.println("FluidCloudMediaStyleCheck: "+checks+" checks passed");
    }
}
