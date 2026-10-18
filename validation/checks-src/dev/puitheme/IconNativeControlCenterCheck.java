package dev.puitheme;

import android.animation.Animator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.media.controls.shared.model.MediaAction;
import com.android.systemui.media.controls.shared.model.MediaData;
import com.android.systemui.media.controls.shared.model.MediaDeviceData;
import com.android.systemui.plugins.qs.QSVolume.VolumeState;
import com.oplus.deviceplugin.sdk.entity.DeviceType;
import com.oplus.deviceplugin.sdk.entity.PanelDevice;
import com.oplus.deviceplugin.sdk.ui.view.progressbar.BatteryCircularProgressLayout;
import com.oplus.deviceplugin.sdk.ui.view.separatecardview.SquareDeviceCardView;
import com.oplus.deviceplugin.sdk.ui.view.separatecardview.SquareEntranceCardView;
import com.oplus.deviceplugin.sdk.ui.view.separatecardview.RectangleEntranceCardView;
import com.oplus.systemui.qs.OplusQSFooterImpl;
import com.oplus.systemui.qs.base.seek.ClipBrightnessView;
import com.oplus.systemui.qs.base.seek.OplusQsVolumeIconView;
import com.oplus.systemui.qs.media.OplusQsBaseMediaPanelView;
import com.oplus.systemui.qs.slider.OplusQsDetailHookClipBrightnessView;
import java.util.*;

/** Executes the production adapter against exact native field/type contracts.
 * Its injected graphics port isolates asynchronous asset IO from binder ownership. */
public final class IconNativeControlCenterCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Non-tile "+checks+": "+expected+" != "+actual);}
    private static final class Native extends Drawable {
        int alpha=187,draws,drawAlpha;ColorFilter filter,drawFilter;
        public void draw(Canvas c){draws++;drawAlpha=alpha;drawFilter=filter;}public void setAlpha(int value){alpha=value;}public int getAlpha(){return alpha;}
        public void setColorFilter(ColorFilter value){filter=value;}public ColorFilter getColorFilter(){return filter;}public int getOpacity(){return -3;}
    }
    private static final class Wrapped extends Drawable {
        final String role;final Drawable source;Integer tint;Wrapped(String role,Drawable source){this.role=role;this.source=source;}
        public void draw(Canvas c){source.draw(c);}public void setAlpha(int value){source.setAlpha(value);}
        public void setColorFilter(ColorFilter value){source.setColorFilter(value);}public int getOpacity(){return source.getOpacity();}
    }
    private static final class Port implements IconNativeControlCenter.Graphics {
        boolean available=true;int requests,wraps,tints;final List<String> roles=new ArrayList<>();
        public Drawable wrap(View owner,String role,Drawable graphic){wraps++;return new Wrapped(role,graphic);}
        public Drawable unwrap(Drawable graphic){while(graphic instanceof Wrapped)graphic=((Wrapped)graphic).source;return graphic;}
        public boolean hasAppearance(String role){return available;}
        public void nativeTint(Drawable wrapper,int argb){tints++;((Wrapped)wrapper).tint=argb;}
        public void request(View owner,String role,int pixels){requests++;roles.add(role);equal(true,pixels>0&&pixels<=1024);}
    }
    private static String role(Drawable d){return d instanceof Wrapped?((Wrapped)d).role:"native";}
    private static void detach(View view){view.attached=false;for(View.OnAttachStateChangeListener l:new ArrayList<>(view.attachListeners))l.onViewDetachedFromWindow(view);}
    private static void attach(View view){view.attached=true;for(View.OnAttachStateChangeListener l:new ArrayList<>(view.attachListeners))l.onViewAttachedToWindow(view);}
    private static void brightness(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);ClipBrightnessView view=new ClipBrightnessView();
        Native original=new Native();view.setBrightnessIcon(original);view.setAlpha(.43f);view.setTranslationY(9);
        adapter.onBrightnessUpdated(view);equal(original,view.getBrightnessIcon());adapter.configure(true);
        equal("slider.brightness.default",role(view.getBrightnessIcon()));equal(.43f,view.getAlpha());equal(9f,view.getTranslationY());
        equal(187,original.getAlpha());int wraps=port.wraps,requests=port.requests;
        for(int i=0;i<100;i++)adapter.onBrightnessUpdated(view);equal(wraps,port.wraps);equal(requests,port.requests);
        Native newer=new Native();view.setBrightnessIcon(newer);adapter.onBrightnessUpdated(view);equal(newer,port.unwrap(view.getBrightnessIcon()));
        Native overlay=new Native();equal(overlay,adapter.brightnessOverlay(view,overlay));view.auto=true;adapter.onBrightnessUpdated(view);
        Drawable wrapped=adapter.brightnessOverlay(view,overlay);equal("slider.brightness_auto.default",role(wrapped));
        requests=port.requests;for(int i=0;i<100;i++)equal(wrapped,adapter.brightnessOverlay(view,overlay));equal(requests,port.requests);
        view.auto=false;adapter.onBrightnessUpdated(view);equal(overlay,adapter.brightnessOverlay(view,overlay));
        detach(view);equal(newer,view.getBrightnessIcon());attach(view);equal("slider.brightness.default",role(view.getBrightnessIcon()));
        port.available=false;adapter.refresh();equal(newer,view.getBrightnessIcon());
        port.available=true;adapter.refresh();equal("slider.brightness.default",role(view.getBrightnessIcon()));
        adapter.configure(false);equal(newer,view.getBrightnessIcon());equal(overlay,adapter.brightnessOverlay(view,overlay));
        OplusQsDetailHookClipBrightnessView detail=new OplusQsDetailHookClipBrightnessView();Native detailNative=new Native();detail.setBrightnessIcon(detailNative);
        adapter.configure(true);adapter.onBrightnessUpdated(detail);equal("slider.brightness.default",role(detail.getBrightnessIcon()));
        adapter.releaseRuntime();equal(newer,view.getBrightnessIcon());equal(detailNative,detail.getBrightnessIcon());equal(0,view.attachListeners.size());equal(0,detail.attachListeners.size());
        adapter.onBrightnessUpdated(view);equal(newer,view.getBrightnessIcon());
    }
    private static void nativeMetadata(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);adapter.configure(true);
        ClipBrightnessView direct=new ClipBrightnessView();Native original=new Native();ColorFilter black=new PorterDuffColorFilter(0xff112233,PorterDuff.Mode.SRC_IN);
        original.filter=black;direct.setBrightnessIcon(original);adapter.onBrightnessUpdated(direct);
        equal(0xff112233,((Wrapped)direct.getBrightnessIcon()).tint);equal(black,original.filter);equal(187,original.alpha);
        int tints=port.tints,wraps=port.wraps,requests=port.requests;
        for(int i=0;i<100;i++)adapter.onBrightnessUpdated(direct);
        equal(tints,port.tints);equal(wraps,port.wraps);equal(requests,port.requests);
        ColorFilter white=new PorterDuffColorFilter(0xffeeeeee,PorterDuff.Mode.SRC_IN);original.filter=white;adapter.onBrightnessUpdated(direct);
        equal(0xffeeeeee,((Wrapped)direct.getBrightnessIcon()).tint);equal(tints+1,port.tints);equal(white,original.filter);
        direct.auto=true;adapter.onBrightnessUpdated(direct);Native overlay=new Native();overlay.filter=black;
        Drawable wrapped=adapter.brightnessOverlay(direct,overlay);equal(0xff112233,((Wrapped)wrapped).tint);tints=port.tints;
        for(int i=0;i<100;i++)equal(wrapped,adapter.brightnessOverlay(direct,overlay));equal(tints,port.tints);
        overlay.filter=white;equal(wrapped,adapter.brightnessOverlay(direct,overlay));equal(0xffeeeeee,((Wrapped)wrapped).tint);equal(tints+1,port.tints);
        OplusQSFooterImpl footer=new OplusQSFooterImpl();Native image=new Native();footer.mSettingsButtonIcon.setImageDrawable(image);
        footer.mSettingsButtonIcon.setImageTintList(android.content.res.ColorStateList.valueOf(0xff556677));adapter.onFooterUpdated(footer);
        equal(0xff556677,((Wrapped)footer.mSettingsButtonIcon.getDrawable()).tint);equal(null,image.filter);equal(187,image.alpha);
        adapter.configure(false);equal(original,direct.getBrightnessIcon());equal(white,original.filter);equal(image,footer.mSettingsButtonIcon.getDrawable());equal(null,image.filter);
        equal(overlay,adapter.brightnessOverlay(direct,overlay));equal(white,overlay.filter);adapter.releaseRuntime();
    }
    private static void runtimeMetadata()throws Exception{
        IconManagementRuntime.releaseRuntime();
        String records=IconManagementSettings.applyAppearance(IconManagementSettings.DEFAULT,"target:slider.brightness",new IconManagementSettings.Appearance("custom",0xff123456,0xffabcdef,50f));
        android.os.Bundle settings=new android.os.Bundle();settings.putString(IconManagementSettings.KEY,records);
        IconManagementRuntime.configure(new android.content.Context(),settings);
        IconNativeControlCenter adapter=new IconNativeControlCenter();adapter.configure(true);ClipBrightnessView direct=new ClipBrightnessView();
        Native original=new Native();ColorFilter black=new PorterDuffColorFilter(0xff112233,PorterDuff.Mode.SRC_IN);original.filter=black;
        direct.setBrightnessIcon(original);adapter.onBrightnessUpdated(direct);Drawable wrapped=direct.getBrightnessIcon();equal(false,wrapped==original);
        wrapped.draw(new Canvas());equal(0xff123456,((PorterDuffColorFilter)original.drawFilter).getColor());equal(94,original.drawAlpha);
        equal(black,original.filter);equal(187,original.alpha);
        ColorFilter white=new PorterDuffColorFilter(0xffeeeeee,PorterDuff.Mode.SRC_IN);original.filter=white;adapter.onBrightnessUpdated(direct);
        equal(wrapped,direct.getBrightnessIcon());wrapped.draw(new Canvas());equal(0xffabcdef,((PorterDuffColorFilter)original.drawFilter).getColor());
        equal(94,original.drawAlpha);equal(white,original.filter);equal(187,original.alpha);
        adapter.configure(false);equal(original,direct.getBrightnessIcon());original.draw(new Canvas());equal(white,original.drawFilter);equal(187,original.drawAlpha);
        adapter.releaseRuntime();IconManagementRuntime.releaseRuntime();android.os.Handler.drainForCheck();
    }
    private static void volume(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);adapter.configure(true);OplusQsVolumeIconView view=new OplusQsVolumeIconView();
        int[] levels={0,1,3,4,6,7,10};String[] names={"mute","low","low","medium","medium","high","high"};
        for(int i=0;i<levels.length;i++){VolumeState state=new VolumeState(2,levels[i],10,false);view.nativeState(state);view.setImageDrawable(new Native());adapter.onVolumeState(view,state);equal("slider.volume.normal."+names[i],role(view.getDrawable()));}
        for(int route=0;route<2;route++)for(int level:new int[]{0,6}){
            VolumeState state=new VolumeState(route,level,10,false);view.nativeState(state);view.setImageDrawable(new Native());adapter.onVolumeState(view,state);
            equal("slider.volume."+(route==0?"wired":"bluetooth")+'.'+(level==0?"mute":"unmute"),role(view.getDrawable()));
        }
        for(VolumeState state:new VolumeState[]{new VolumeState(-1,1,10,false),new VolumeState(3,1,10,false),new VolumeState(2,-1,10,false),new VolumeState(2,4,0,false),new VolumeState(2,11,10,false),new VolumeState(2,4,10,true)}){
            Native original=new Native();view.setImageDrawable(original);view.nativeState(state);adapter.onVolumeState(view,state);equal(original,view.getDrawable());
        }
        VolumeState state=new VolumeState(2,6,10,false);Native original=new Native();view.nativeState(state);view.setImageDrawable(original);view.animating=true;
        Animator.AnimatorListener nativeListener=new Animator.AnimatorListener(){public void onAnimationStart(Animator a){}public void onAnimationEnd(Animator a){}public void onAnimationCancel(Animator a){}public void onAnimationRepeat(Animator a){}};
        view.listeners.add(nativeListener);view.queuePostedForCheck=true;adapter.onVolumeState(view,state);equal(original,view.getDrawable());equal(2,view.listeners.size());
        for(int i=0;i<20;i++)adapter.onVolumeState(view,state);equal(2,view.listeners.size());
        view.finish();equal(1,view.listeners.size());equal(nativeListener,view.listeners.get(0));equal(original,view.getDrawable());
        view.runPostedForCheck();equal("slider.volume.normal.medium",role(view.getDrawable()));
        detach(view);equal(original,view.getDrawable());attach(view);equal("slider.volume.normal.medium",role(view.getDrawable()));
        view.animating=true;adapter.onVolumeState(view,state);equal(2,view.listeners.size());adapter.configure(false);equal(1,view.listeners.size());equal(original,view.getDrawable());
        adapter.configure(true);adapter.onVolumeState(view,state);view.finish();adapter.releaseRuntime();view.runPostedForCheck();equal(original,view.getDrawable());equal(0,view.attachListeners.size());
    }
    private static void footer(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);adapter.configure(true);OplusQSFooterImpl panel=new OplusQSFooterImpl();
        Native settings=new Native(),more=new Native(),avatar=new Native();panel.mSettingsButtonIcon.setImageDrawable(settings);panel.mMoreIcon.setImageDrawable(more);panel.mMultiUserAvatar.setImageDrawable(avatar);
        panel.mSettingsButtonIcon.setContentDescription("native accessibility");final int[] clicks={0};panel.mSettingsButtonIcon.setOnClickListener(v->clicks[0]++);
        adapter.onFooterUpdated(panel);equal("footer.settings.default",role(panel.mSettingsButtonIcon.getDrawable()));equal("footer.more.default",role(panel.mMoreIcon.getDrawable()));
        equal(avatar,panel.mMultiUserAvatar.getDrawable());equal("native accessibility",panel.mSettingsButtonIcon.getContentDescription());panel.mSettingsButtonIcon.performClick();equal(1,clicks[0]);
        ImageView old=panel.mMoreIcon;Native newer=new Native();panel.removeView(old);panel.mMoreIcon=new ImageView(panel.getContext());panel.addView(panel.mMoreIcon);panel.mMoreIcon.setImageDrawable(newer);
        adapter.onFooterUpdated(panel);equal(more,old.getDrawable());equal("footer.more.default",role(panel.mMoreIcon.getDrawable()));
        ImageView outsider=new ImageView(panel.getContext());Native foreign=new Native();outsider.setImageDrawable(foreign);panel.mSettingsButtonIcon=outsider;adapter.onFooterUpdated(panel);equal(foreign,outsider.getDrawable());
        adapter.releaseRuntime();equal(newer,panel.mMoreIcon.getDrawable());equal(0,panel.attachListeners.size());equal(avatar,panel.mMultiUserAvatar.getDrawable());
    }
    private static void media(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);adapter.configure(true);OplusQsBaseMediaPanelView panel=new OplusQsBaseMediaPanelView();
        Native previous=new Native(),next=new Native(),play=new Native(),output=new Native();panel.preBtn.getImageView().setImageDrawable(previous);panel.nextBtn.getImageView().setImageDrawable(next);
        panel.playOrPauseBtn.getImageView().setImageDrawable(play);panel.mediaOutputBtn.setImageDrawable(output);
        MediaAction standard=new MediaAction(true,()->{}),custom=new MediaAction(false,()->{});MediaData data=new MediaData();
        adapter.onMediaPlayPause(panel.playOrPauseBtn);equal(play,panel.playOrPauseBtn.getImageView().getDrawable());
        adapter.onMediaSemantic(panel,panel.preBtn,0x7f080e5a,standard,data);adapter.onMediaSemantic(panel,panel.nextBtn,0x7f080e54,standard,data);
        adapter.onMediaSemantic(panel,panel.playOrPauseBtn,0x7f080e58,standard,data);
        equal("media.previous.default",role(panel.preBtn.getImageView().getDrawable()));equal("media.next.default",role(panel.nextBtn.getImageView().getDrawable()));equal("media.play_pause.play",role(panel.playOrPauseBtn.getImageView().getDrawable()));
        panel.playOrPauseBtn.state=3;adapter.onMediaPlayPause(panel.playOrPauseBtn);equal("media.play_pause.pause",role(panel.playOrPauseBtn.getImageView().getDrawable()));
        int effectiveVisibility=panel.playOrPauseBtn.effective.getVisibility();adapter.onMediaCustom(panel,panel.playOrPauseBtn);equal(play,panel.playOrPauseBtn.getImageView().getDrawable());equal(effectiveVisibility,panel.playOrPauseBtn.effective.getVisibility());
        adapter.onMediaSemantic(panel,panel.preBtn,0x7f080e5a,custom,data);equal(previous,panel.preBtn.getImageView().getDrawable());
        adapter.onMediaSemantic(panel,panel.nextBtn,1,standard,data);equal(next,panel.nextBtn.getImageView().getDrawable());
        adapter.onMediaSemantic(panel,panel.preBtn,0x7f080e5a,new MediaAction(true,null),data);equal(previous,panel.preBtn.getImageView().getDrawable());
        adapter.onMediaOutput(panel,2);equal("media.output.default",role(panel.mediaOutputBtn.getDrawable()));
        data.device=new MediaDeviceData();data.device.resource=0;adapter.onMediaBound(panel,data);equal(output,panel.mediaOutputBtn.getDrawable());
        adapter.onMediaOutput(panel,2);data.device.resource=0x7f0818f6;adapter.onMediaBound(panel,data);equal("media.output.default",role(panel.mediaOutputBtn.getDrawable()));
        panel.inAudioSharing=true;adapter.onMediaOutput(panel,2);equal(output,panel.mediaOutputBtn.getDrawable());panel.inAudioSharing=false;
        for(int route:new int[]{-1,0,1,7}){adapter.onMediaOutput(panel,route);equal(output,panel.mediaOutputBtn.getDrawable());}
        adapter.onMediaSemantic(panel,panel.preBtn,0x7f080e5a,standard,data);adapter.onMediaOutput(panel,2);adapter.onMediaUnbound(panel);
        equal(previous,panel.preBtn.getImageView().getDrawable());equal(output,panel.mediaOutputBtn.getDrawable());adapter.releaseRuntime();equal(0,panel.preBtn.getImageView().attachListeners.size());
    }
    private static final Map<String,Integer> RESOURCES=new HashMap<>();
    static {String[] names={"phone","pad","computer","headset","tv","watch","wristband"};for(int i=0;i<names.length;i++)RESOURCES.put("device_card_"+names[i],700+i);}
    private static final android.content.res.Resources DEVICE_RESOURCES=new android.content.res.Resources(){
        @Override public int getIdentifier(String name,String kind,String pkg){Integer id=RESOURCES.get(name);return id==null?0:id;}
    };
    private static final class Square extends SquareDeviceCardView {@Override public android.content.res.Resources getResources(){return DEVICE_RESOURCES;}}
    private static final class Rectangle extends com.oplus.deviceplugin.sdk.ui.view.separatecardview.RectangleDeviceCardView{
        public final BatteryCircularProgressLayout p=new BatteryCircularProgressLayout();Rectangle(){p.parent=body;}
        @Override public android.content.res.Resources getResources(){return DEVICE_RESOURCES;}
    }
    public static final class Animation {
        public boolean running=true;public final List<Animator.AnimatorListener> listeners=new ArrayList<>();
        public boolean isRunning(){return running;}public void addListener(Animator.AnimatorListener listener){listeners.add(listener);}public void removeListener(Animator.AnimatorListener listener){listeners.remove(listener);}
        void finish(){running=false;for(Animator.AnimatorListener l:new ArrayList<>(listeners))l.onAnimationEnd(null);}
    }
    private static void devices(){
        Port port=new Port();IconNativeControlCenter adapter=new IconNativeControlCenter(port);adapter.configure(true);Square card=new Square();card.q=new PanelDevice(1);
        View ring=card.p.c;ImageView charge=card.p.d;Native charging=new Native();charge.setImageDrawable(charging);
        for(DeviceType type:new DeviceType[]{DeviceType.PHONE,DeviceType.PAD,DeviceType.COMPUTER,DeviceType.HEADSET,DeviceType.TV,DeviceType.WATCH,DeviceType.WRISTBAND}){
            String name=type.name().toLowerCase(java.util.Locale.ROOT);card.q.type=type;card.p.f.setImageDrawable(new Native());adapter.onDeviceIcon(card.p,RESOURCES.get("device_card_"+name));equal("device."+name+".default",role(card.p.f.getDrawable()));
            equal(ring,card.p.c);equal(charging,charge.getDrawable());
        }
        card.q.type=DeviceType.PHONE;card.q.iconUri="content://remote/photo";Native photo=new Native();card.p.f.setImageDrawable(photo);adapter.onDeviceIcon(card.p,700);equal(photo,card.p.f.getDrawable());
        card.q.iconUri=null;card.q.type=DeviceType.IPHONE;adapter.onDeviceIcon(card.p,700);equal(photo,card.p.f.getDrawable());card.q.type=DeviceType.PHONE;
        adapter.onDeviceIcon(card.p,9999);equal(photo,card.p.f.getDrawable());adapter.onDeviceIcon(card.p,700);equal("device.phone.default",role(card.p.f.getDrawable()));
        adapter.onDeviceDrawable(card.p);equal(photo,card.p.f.getDrawable());adapter.onDeviceUpdated(card);equal(photo,card.p.f.getDrawable());
        Animation animation=new Animation();card.p.k=animation;card.p.queuePostedForCheck=true;adapter.onDeviceIcon(card.p,700);equal(photo,card.p.f.getDrawable());equal(1,animation.listeners.size());
        Native nextFrame=new Native();card.p.f.setImageDrawable(nextFrame);animation.finish();equal(0,animation.listeners.size());card.p.runPostedForCheck();equal("device.phone.default",role(card.p.f.getDrawable()));
        Rectangle rectangle=new Rectangle();rectangle.v=new PanelDevice(1);rectangle.v.type=DeviceType.PAD;Native pad=new Native();rectangle.p.f.setImageDrawable(pad);adapter.onDeviceIcon(rectangle.p,701);equal("device.pad.default",role(rectangle.p.f.getDrawable()));
        SquareEntranceCardView square=new SquareEntranceCardView();RectangleEntranceCardView wide=new RectangleEntranceCardView();Native s=new Native(),w=new Native();square.m.setImageDrawable(s);wide.p.setImageDrawable(w);
        adapter.onDeviceUpdated(square);adapter.onDeviceUpdated(wide);equal("device.entrance.default",role(square.m.getDrawable()));equal("device.entrance.default",role(wide.p.getDrawable()));
        adapter.releaseRuntime();equal(nextFrame,card.p.f.getDrawable());equal(pad,rectangle.p.f.getDrawable());equal(s,square.m.getDrawable());equal(w,wide.p.getDrawable());equal(0,card.p.f.attachListeners.size());
        adapter.onDeviceIcon(card.p,700);equal(nextFrame,card.p.f.getDrawable());
    }
    public static void main(String[] args)throws Exception{brightness();nativeMetadata();runtimeMetadata();volume();footer();media();devices();System.out.println("IconNativeControlCenter checks passed: "+checks);}
}
