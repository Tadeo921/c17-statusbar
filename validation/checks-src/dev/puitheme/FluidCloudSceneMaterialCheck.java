package dev.puitheme;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.posteffect.drawable.MetaBallBlurDrawable;
import com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler;

/** Native setter contracts, exact scene donors, primary gradient, and restoration. */
public final class FluidCloudSceneMaterialCheck {
    private static int checks;
    private static final android.content.res.Resources SCENE_RESOURCES=new android.content.res.Resources();
    private static final class SceneBody extends NotificationBackgroundView {
        @Override public android.content.res.Resources getResources(){return SCENE_RESOURCES;}
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static NotificationBackgroundViewExtImp ext(NotificationBackgroundView body){return (NotificationBackgroundViewExtImp)body.mExt;}
    private static void mode(FluidCloudSceneMaterial controller,NotificationBackgroundView body,NotificationBlurMode mode){
        ext(body).notifyBlurParamChanged((NotificationBlurMode)controller.mode(ext(body),mode));controller.observed(ext(body));
    }
    public static void main(String[] args)throws Exception {
        FluidCloudSceneMaterial controller=new FluidCloudSceneMaterial();
        controller.resolve(FluidCloudSceneMaterialCheck.class.getClassLoader());
        NotificationBackgroundView ordinary=new SceneBody();
        ordinary.parent=new ExpandableNotificationRow(new Context(),80,0,0);
        ordinary.mTintColor=0xcceef2f8;mode(controller,ordinary,NotificationBlurMode.SHADE_LIGHT);
        NotificationBackgroundView floating=new SceneBody();
        ExpandableNotificationRow floatingRow=new ExpandableNotificationRow(new Context(),80,0,1);
        floating.parent=floatingRow;floatingRow.parent=new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();
        floating.mTintColor=cctint();mode(controller,floating,NotificationBlurMode.NO_BLUR_DARK);
        NotificationBackgroundView target=new SceneBody();CustomRowEntry entry=new CustomRowEntry();
        target.parent=new com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow();
        controller.register(target,entry);mode(controller,target,NotificationBlurMode.WALLPAPER_A);
        NotificationBackgroundCustomRowHandler nativeHelper=new NotificationBackgroundCustomRowHandler();
        MetaBallBlurDrawable nativeDrawable=new MetaBallBlurDrawable();Context context=new Context();
        nativeHelper.updateDrawableMetaColor(context,entry,nativeDrawable);controller.meta(nativeHelper,context,entry,nativeDrawable);
        check(nativeDrawable.metaColor==nativeHelper.nativeColor,"all-OFF preserves native JSON template colour");
        controller.policy(true,false);
        check(ext(target).notificationBlurMode==NotificationBlurMode.SHADE_LIGHT,"shade borrows immutable ordinary shade mode, not another proxy");
        check(nativeDrawable.metaColor==nativeHelper.nativeColor&&nativeDrawable.gradientColors==null,"mode-only discovery keeps template material until complete platform submission succeeds");
        int updates=nativeHelper.nativeUpdates;
        for(int i=0;i<1000;i++)controller.observed(ext(ordinary));
        check(nativeHelper.nativeUpdates==updates,"stable native per-frame colour setter performs no material replay");
        GradientDrawable black=new GradientDrawable();black.setColor(0xff000000);black.setCornerRadius(33);
        ext(target).tempDrawable=black;
        try(FluidCloudSceneMaterial.TempScope scope=controller.draw(ext(target))){check(ext(target).tempDrawable==black,"no complete native platform submit keeps the fallback plate");}
        check(ext(target).tempDrawable==black,"temporary base restored by identity after draw");
        ((android.view.View)target.parent).parent=new com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout();
        controller.observed(ext(target));
        check(ext(target).notificationBlurMode==NotificationBlurMode.NO_BLUR_DARK&&nativeDrawable.metaColor==nativeHelper.nativeColor,"moving to heads-up discovers correct scene but keeps fallback before real submit");
        controller.policy(false,false);
        check(ext(target).notificationBlurMode==NotificationBlurMode.WALLPAPER_A&&nativeDrawable.metaColor==nativeHelper.nativeColor,"disable restores latest native entry material and original mode");
        controller.policy(true,true);controller.accent(target,0xffad4761);
        check(nativeDrawable.gradientColors!=null&&nativeDrawable.orientation==GradientDrawable.Orientation.TOP_BOTTOM,"lockscreen primary accent goes through real native gradient setter");
        check(nativeDrawable.gradientColors[0]==0xffad4761&&nativeDrawable.gradientColors[1]==0x00ad4761,"top colour fades to transparent native blur; text is outside material policy");
        controller.policy(false,true);
        check(nativeDrawable.gradientColors!=null,"style master OFF does not disable independently opted-in lockscreen accent");
        controller.accent(target,0xff123456);
        check(nativeDrawable.gradientColors[0]==0xff123456,"new masked primary colour updates native gradient without polling artwork");
        controller.accent(target,null);
        check(nativeDrawable.metaColor==nativeHelper.nativeColor&&nativeDrawable.gradientColors==null,"accent OFF restores current native entry colours");
        controller.policy(true,false);
        NotificationBackgroundView recycled=new SceneBody();recycled.parent=new com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow();
        controller.register(recycled,new CustomRowEntry());mode(controller,recycled,NotificationBlurMode.WALLPAPER_B);
        check(ext(recycled).notificationBlurMode==NotificationBlurMode.SHADE_LIGHT,"new target applies same-scene mode");
        controller.detach(recycled);
        check(ext(recycled).notificationBlurMode==NotificationBlurMode.WALLPAPER_B,"detached target restores its own original mode");
        controller.register(recycled,new CustomRowEntry());mode(controller,recycled,NotificationBlurMode.WALLPAPER_B);
        ext(recycled).notificationBlurMode=NotificationBlurMode.NO_BLUR_LIGHT;controller.detach(recycled);
        check(ext(recycled).notificationBlurMode==NotificationBlurMode.NO_BLUR_LIGHT,"detach cannot undo a newer external native mode");
        controller.policy(true,true);
        check(nativeDrawable.metaColor==nativeHelper.nativeColor,"no audited widget donor means lockscreen retains readable OEM material, not notification tint");
        controller.policy(false,false);controller.accent(target,0xff445566);controller.policy(false,true);
        controller.detach(target);
        check(nativeDrawable.metaColor==nativeHelper.nativeColor&&nativeDrawable.gradientColors==null,"entry detach restores owned MetaBall without leaving gradient state");
        NotificationBackgroundView fallback=new SceneBody();CustomRowEntry other=new CustomRowEntry();
        fallback.parent=new com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow();
        controller.register(fallback,other);mode(controller,fallback,NotificationBlurMode.WALLPAPER_A);
        GradientDrawable source=new GradientDrawable();source.setColor(0xff000000);source.setCornerRadius(39);source.setAlpha(127);ext(fallback).tempDrawable=source;
        ((com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow)fallback.parent).immersiveCustomDrawable=source;
        controller.accent(fallback,0xff456789);
        Drawable copy;
        try(FluidCloudSceneMaterial.TempScope scope=controller.draw(ext(fallback))){copy=ext(fallback).tempDrawable;check(copy!=source&&((GradientDrawable)copy).getColors()[1]==0x00456789,"static null-MetaBall base has private top-to-transparent gradient");}
        try(FluidCloudSceneMaterial.TempScope scope=controller.draw(ext(fallback))){check(ext(fallback).tempDrawable==copy,"unchanged draw reuses cached private shape, no per-frame allocation");}
        check(source.getColor().getDefaultColor()==0xff000000&&source.getCornerRadius()==39&&source.getAlpha()==127,"shared source alpha/corners/fill are never mutated");
        controller.release();check(ext(fallback).tempDrawable==source,"release cannot keep a borrowed native temp field");
        System.out.println("FluidCloudSceneMaterialCheck: "+checks+" checks passed");
    }
    private static int cctint(){return 0xcc223344;}
}
