package dev.puitheme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.oplus.systemui.notification.row.material.MaterialColorMode;
import com.oplus.systemui.notification.row.material.NotificationColorPalette;
import com.oplus.systemui.notification.row.material.NotificationMaterialColorEx;
import com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.android.systemui.plugins.custom.CustomRowEntry;
import java.util.ArrayList;

/** Exercises the real foreground controller with native setter ordering and
 * actual palette contracts, including stable-event traversal/write budgets. */
public final class FluidCloudForegroundCheck {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void main(String[] args)throws Exception{palette();settersAndEvents();styleThemeEvents();System.out.println("FluidCloudForegroundCheck: "+checks+" checks passed");}
    private static void palette(){
        Context context=new Context();NotificationBackgroundView body=new NotificationBackgroundView();
        ExpandableNotificationRow row=new ExpandableNotificationRow(context,100,0,0);row.addView(body);
        NotificationMaterialColorEx nativeMode=((NotificationBackgroundViewExtImp)body.mExt).materialForCheck;
        check(FluidCloudForeground.nativeColor(body)==null,"UNINITIATED native mode cannot manufacture black/white");
        nativeMode.currentMode=MaterialColorMode.MATERIAL;
        check(FluidCloudForeground.nativeColor(body)==0xff161616,"MATERIAL follows the real current common text palette, despite no LIGHT/DARK name");
        NotificationColorPalette.commonTextMaterialColor=0xfff8f8f8;
        check(FluidCloudForeground.nativeColor(body)==0xfff8f8f8,"same native mode reads the latest dynamic system palette");
        nativeMode.rowMode=MaterialColorMode.NORMAL_LIGHT;
        check(FluidCloudForeground.nativeColor(body)==NotificationColorPalette.lightPrimary,"real row foreground mode wins over global MATERIAL mode");
        nativeMode.rowMode=MaterialColorMode.NORMAL_DARK;
        check(FluidCloudForeground.nativeColor(body)==NotificationColorPalette.darkPrimary,"native dark palette exact shade is retained instead of guessed pure white");
        check(FluidCloudForeground.nativeColor(new View(context))==null,"an unrelated/remote root is not a notification palette source");
        nativeMode.failRead=true;
        check(FluidCloudForeground.nativeColor(body)==null,"native access failure conservatively leaves original foreground");
        nativeMode.failRead=false;nativeMode.rowMode=MaterialColorMode.MATERIAL;NotificationColorPalette.commonTextMaterialColor=0xff1976d2;
        check(FluidCloudForeground.nativeColor(body)==0xff1976d2,"actual system primary hue is retained without a black-white assumption");
        NotificationColorPalette.commonTextMaterialColor=0xff161616;
    }
    private static void settersAndEvents(){
        FluidCloudForeground controller=new FluidCloudForeground();CountingGroup root=new CountingGroup();
        HookedText title=new HookedText(controller);title.setTextColor(0xe6ffffff);root.addView(title);
        HookedImage symbol=new HookedImage(controller);symbol.setImageTintList(ColorStateList.valueOf(0x8affffff));root.addView(symbol);
        HookedImage cover=new HookedImage(controller);root.addView(cover);
        HookedText selector=new HookedText(controller);ColorStateList stateful=new ColorStateList(new int[][]{{1},{}},new int[]{0xff000000,0xffffffff});selector.setTextColor(stateful);root.addView(selector);
        controller.policy(true);controller.register(root,0xff161616);
        check(title.getCurrentTextColor()==0xe6161616&&symbol.getImageTintList().getDefaultColor()==0x8a161616,"native title/icon opacity survives adaptation");
        check(selector.getTextColors()==stateful&&cover.getImageTintList()==null,"stateful selectors and album images remain native");
        int visits=root.visits,reads=title.reads,writes=title.written.size();
        for(int i=0;i<10000;i++)controller.register(root,0xff161616);
        check(root.visits==visits&&title.reads==reads&&title.written.size()==writes,"ten thousand stable refreshes do no scans, property reads or writes");
        title.written.clear();title.setTextColor(0xccffffff);
        check(title.written.size()==1&&title.written.get(0)==0xcc161616,"native white setter is transformed before its only visible write, preventing white-black flashing");
        ColorStateList latestIcon=ColorStateList.valueOf(0x99ffffff);symbol.setImageTintList(latestIcon);
        check(symbol.getImageTintList().getDefaultColor()==0x99161616,"native icon tint refresh adapts immediately without a root scan");
        controller.policy(false);
        check(title.getCurrentTextColor()==0xccffffff&&symbol.getImageTintList()==latestIcon,"disable restores the latest intercepted native colour and alpha");
        controller.policy(true);controller.register(root,0xff161616);title.setTextColor(0xff12d070);
        controller.policy(false);
        check(title.getCurrentTextColor()==0xff12d070,"native semantic rebind is never overwritten by stale restore");
        title.setTextColor(0xffffffff);controller.policy(true);controller.register(root,0xff161616);
        HookedText added=new HookedText(controller);added.setTextColor(0xddffffff);root.addView(added);
        controller.invalidate(root);controller.register(root,0xff161616);
        check(added.getCurrentTextColor()==0xdd161616,"explicit content reinflation discovers newly added local text");
        root.removeView(added);controller.invalidate(root);controller.register(root,0xff161616);
        check(added.getCurrentTextColor()==0xddffffff,"removed content restores and leaves the exact root registry");
        added.setTextColor(0xffffffff);check(added.getCurrentTextColor()==0xffffffff,"former content cannot be recoloured by another root's native setter");
        HookedText recycled=new HookedText(controller);recycled.setTextColor(0xffffffff);root.addView(recycled);
        controller.invalidate(root);controller.register(root,0xff161616);root.removeView(recycled);recycled.setTextColor(0x88ffffff);
        check(recycled.getCurrentTextColor()==0x88ffffff,"recycled view setter passes through immediately before queued content rescan");
        controller.register(root,null);check(title.getCurrentTextColor()==0xffffffff,"lost native palette returns owned foreground to native");
        controller.register(root,0xff1976d2);title.setTextColor(0x99ffffff);controller.policy(false);
        check(title.getCurrentTextColor()==0x99ffffff,"a real system primary hue still restores the latest neutral native text");
        title.setTextColor(0xffffffff);controller.policy(true);
        controller.register(root,0xff161616);controller.detach(null);check(title.getCurrentTextColor()==0xff161616,"null detach cannot clear unrelated root ownership");
        controller.detach(root);check(title.getCurrentTextColor()==0xffffffff,"actual detach restores only its root");
        controller.release();
    }
    private static void styleThemeEvents()throws Exception{
        Handler.drainForCheck();
        FluidCloudStyle style=new FluidCloudStyle();ClassLoader loader=FluidCloudForegroundCheck.class.getClassLoader();
        style.resolveScene(loader);style.resolveNotification(loader);
        NotificationBackgroundView body=new NotificationBackgroundView();OplusCustomRow row=new OplusCustomRow();
        row.setMBackgroundNormal(body);body.parent=row;
        CountingGroup content=new CountingGroup();StyleText title=new StyleText(style);title.setTextColor(0xe6ffffff);content.addView(title);
        CustomRowEntry entry=new CustomRowEntry();entry.customView=content;row.setEntry(entry);
        NotificationBackgroundViewExtImp nativeExt=(NotificationBackgroundViewExtImp)body.mExt;
        nativeExt.materialForCheck.currentMode=MaterialColorMode.MATERIAL;
        NotificationColorPalette.commonTextMaterialColor=0xff161616;
        style.notificationRow(row);
        Bundle settings=new Bundle();settings.putBoolean(FluidCloudStyle.MASTER,true);settings.putBoolean(FluidCloudStyle.FOLLOW_FOREGROUND,true);
        style.configure(settings);nativeThemeEvent(style,nativeExt);Handler.drainForCheck();
        check(title.getCurrentTextColor()==0xe6ffffff&&!style.enabled(),"saved retired flag cannot activate Style or replace native foreground");
        int tint=body.mTintColor;Object blurMode=nativeExt.notificationBlurMode;
        int queued=Handler.postedForCheck.size();NotificationColorPalette.commonTextMaterialColor=0xfff8f8f8;
        nativeThemeEvent(style,nativeExt);
        check(Handler.postedForCheck.size()==queued,"retired foreground cannot queue theme adaptation work");
        for(int i=0;i<10000;i++)nativeThemeEvent(style,nativeExt);
        check(Handler.postedForCheck.size()==queued,"ten thousand identical native theme callbacks cannot add retired-policy refresh batches");
        Handler.drainForCheck();
        check(title.getCurrentTextColor()==0xe6ffffff&&body.mTintColor==tint&&nativeExt.notificationBlurMode==blurMode,
                "same-mode system palette leaves retired foreground and native backdrop untouched");
        int visits=content.visits,writes=title.written.size(),flowReads=nativeExt.materialForCheck.flowReads;
        for(int i=0;i<10000;i++){
            boolean frame=style.beginSceneFrame();
            try{style.sceneMaterialChanged(nativeExt);}finally{style.endSceneFrame(frame);}
        }
        check(Handler.postedForCheck.isEmpty()&&nativeExt.materialForCheck.flowReads==flowReads&&content.visits==visits&&title.written.size()==writes,
                "ten thousand sampled stable draw callbacks do no native foreground reads, scans, writes or queued refreshes");
        NotificationColorPalette.commonTextMaterialColor=0xff202020;nativeThemeEvent(style,nativeExt);Handler.drainForCheck();
        check(title.getCurrentTextColor()==0xe6ffffff&&content.visits==visits,"reverse same-mode theme update does not traverse retired content or modify native text");
        settings.putBoolean(FluidCloudStyle.FOLLOW_FOREGROUND,false);style.configure(settings);Handler.drainForCheck();
        check(title.getCurrentTextColor()==0xe6ffffff,"turning saved retired flag OFF also retains native title colour");
        NotificationColorPalette.commonTextMaterialColor=0xfff8f8f8;nativeThemeEvent(style,nativeExt);
        check(Handler.postedForCheck.isEmpty()&&title.getCurrentTextColor()==0xe6ffffff,"native foreground events while policy OFF cannot queue foreground work or recolour content");
        style.release();Handler.drainForCheck();NotificationColorPalette.commonTextMaterialColor=0xff161616;
    }
    private static void nativeThemeEvent(FluidCloudStyle style,NotificationBackgroundViewExtImp extension){
        extension.notifyMaterialColorChanged();style.sceneMaterialChanged(extension);
    }
    private static final class CountingGroup extends ViewGroup {
        int visits;
        CountingGroup(){super(new Context());}
        @Override public int getChildCount(){visits++;return super.getChildCount();}
        @Override public View getChildAt(int index){visits++;return super.getChildAt(index);}
    }
    private static final class HookedText extends TextView {
        final FluidCloudForeground controller;final ArrayList<Integer> written=new ArrayList<>();int reads;
        HookedText(FluidCloudForeground controller){super(new Context());this.controller=controller;}
        @Override public ColorStateList getTextColors(){reads++;return super.getTextColors();}
        @Override public void setTextColor(int colour){
            int next=controller.textColor(this,colour);
            try{super.setTextColor(next);written.add(next);}finally{controller.colorApplied(this);}
        }
        @Override public void setTextColor(ColorStateList colours){
            ColorStateList next=controller.color(this,colours);
            try{super.setTextColor(next);written.add(next.getDefaultColor());}finally{controller.colorApplied(this);}
        }
    }
    private static final class HookedImage extends ImageView {
        final FluidCloudForeground controller;
        HookedImage(FluidCloudForeground controller){super(new Context());this.controller=controller;}
        @Override public void setImageTintList(ColorStateList colours){
            ColorStateList next=controller.color(this,colours);
            try{super.setImageTintList(next);}finally{controller.colorApplied(this);}
        }
    }
    private static final class StyleText extends TextView {
        final FluidCloudStyle style;final ArrayList<Integer> written=new ArrayList<>();
        StyleText(FluidCloudStyle style){super(new Context());this.style=style;}
        @Override public void setTextColor(int colour){
            int next=style.foregroundTextColor(this,colour);
            try{super.setTextColor(next);written.add(next);}finally{style.foregroundColorApplied(this);}
        }
        @Override public void setTextColor(ColorStateList colours){
            ColorStateList next=style.foregroundColor(this,colours);
            try{super.setTextColor(next);written.add(next.getDefaultColor());}finally{style.foregroundColorApplied(this);}
        }
    }
}
