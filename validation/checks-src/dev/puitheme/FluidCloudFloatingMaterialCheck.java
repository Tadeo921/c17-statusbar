package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.notification.row.material.NotificationBlurMode;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplusos.systemui.common.blurability.ViewBlurProxy;
import com.oplus.view.ViewRootManager;
import com.heytap.log.nx.obus.a;
import java.util.Arrays;

/** Only-follow desktop regression against audited normal heads-up factory. */
public final class FluidCloudFloatingMaterialCheck {
    private static int checks;
    private static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    private static void drain(){int turns=0;while(!Handler.postedForCheck.isEmpty()){
        if(++turns>30)throw new AssertionError("floating refresh did not settle");Handler.drainForCheck();}}
    private static final class Resources extends android.content.res.Resources {
        @Override public String getResourceEntryName(int id){return id==1?"card_background_mix_color":"card_background_gradient_middle_color";}
    }
    public static void main(String[] args)throws Exception {
        Handler.postedForCheck.clear();FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudFloatingMaterialCheck.class.getClassLoader();
        style.resolve(loader);style.resolveScene(loader);
        a.controller=null;a.style=style;ViewRootManager.style=style;
        CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(new Resources());card.addView(host);
        ViewRootManager manager=new ViewRootManager();a helper=new a(null,3);helper.b(host,manager);
        float[] original=manager.saved.clone();host.setAlpha(.42f);host.setTranslationY(17);style.nativeBlurAlpha(host,.31f);
        Bundle options=new Bundle();options.putBoolean(FluidCloudStyle.MASTER,true);options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,true);
        style.configure(options);drain();
        check(Arrays.equals(manager.saved,original)&&a.gradient==0xff000000,"no native heads-up factory source preserves entire OEM card; no gray recolor approximation");
        NotificationBackgroundView donor=new NotificationBackgroundView(); donor.parent=new com.android.systemui.statusbar.notification.row.ExpandableNotificationRow(donor.getContext(),80,0,0);
        NotificationBackgroundViewExtImp ext=(NotificationBackgroundViewExtImp)donor.mExt;
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.HEADS_UP_MATERIAL_LIGHT;
        style.sceneMaterialChanged(ext);drain();
        check(manager.saved[0]==2&&manager.saved[3]==2,"only-follow uses OEM native heads-up motion builder and blend mode");
        check(manager.saved[4]==0x22/255f&&manager.saved[8]==0x44/255f&&manager.saved[7]==0x40/255f&&manager.saved[11]==0xb3/255f,
                "distinct bottom/top RGB and alpha match normal notification, not flattened tint");
        check(manager.saved[15]==0&&a.gradient>>>24==0,"native factory has no desktop-only extra colour layer or black template gradient");
        for(int slot:new int[]{1,2,16,17})check(manager.saved[slot]==original[slot],"host native shape/geometry retained: "+slot);
        check(host.getAlpha()==.42f&&host.getTranslationY()==17&&host.blurAlpha==.31f,"native position and expansion animation alpha untouched");
        int builds=ViewBlurProxy.motionParamBuildsForCheck,initializers=a.builds,reads=ext.materialForCheck.headsReads;
        for(int i=0;i<10000;i++){boolean frame=style.beginSceneFrame();try{style.sceneMaterialChanged(ext);}finally{style.endSceneFrame(frame);}style.refreshAfterAccent();}
        drain();check(a.builds==initializers&&ViewBlurProxy.motionParamBuildsForCheck==builds&&ext.materialForCheck.headsReads==reads&&Handler.postedForCheck.isEmpty(),
                "10000 stable frames do not rebuild, poll factories or schedule refresh loops");
        ext.materialForCheck.headsModeForCheck=NotificationBlurMode.HEADS_UP_MATERIAL_DARK;
        ext.viewBlurManager.motionColorForCheck=new com.oplusos.systemui.common.blurability.MixColor(3,0x80402010,0x20506070);
        style.scenePaletteChanged(ext);drain();
        check(manager.saved[3]==3&&manager.saved[4]==0x50/255f&&manager.saved[8]==0x40/255f
                &&ViewBlurProxy.motionParamBuildsForCheck==builds+1,"real theme event refreshes native heads-up palette even when ordinary tint/mode are unchanged");
        ext.viewBlurManager.noMotionMixForCheck=true;donor.mTintColor=0xffcccccc;style.sceneMaterialChanged(ext);drain();
        check(Arrays.equals(manager.saved,original)&&a.gradient==0xff000000,"factory unavailability restores OEM card instead of erasing background");
        ext.viewBlurManager.noMotionMixForCheck=false;donor.mTintColor=0xffaaaaaa;style.sceneMaterialChanged(ext);drain();
        check(manager.saved[0]==2&&a.gradient>>>24==0,"later real material event recovers complete native heads-up recipe");
        style.setKeyguard(true);drain();
        check(Arrays.equals(manager.saved,original)&&a.gradient==0xff000000,"desktop heads-up recipe does not leak into lockscreen/AOD");
        style.setKeyguard(false);drain();check(manager.saved[0]==2,"desktop recipe resumes after leaving keyguard");
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true);options.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");
        options.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xff123456);options.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,100);style.configure(options);drain();
        check(manager.saved[0]==original[0]&&manager.saved[4]==0x12/255f,"explicit background retains its independent priority");
        options.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,false);style.configure(options);drain();check(manager.saved[0]==2,"turning off explicit fill restores heads-up recipe");
        options.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION,false);style.configure(options);drain();
        check(Arrays.equals(manager.saved,original)&&a.gradient==0xff000000,"follow OFF restores latest native desktop params and gradient exactly");
        style.release();drain();a.style=null;ViewRootManager.style=null;
        System.out.println("FluidCloudFloatingMaterialCheck: "+checks+" checks passed");
    }
}
