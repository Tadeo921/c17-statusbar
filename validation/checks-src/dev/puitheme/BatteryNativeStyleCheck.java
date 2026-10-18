package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.util.Collections;
import java.util.HashMap;

/** OEM percentage setting, live binding and complete native asset/size restoration. */
public final class BatteryNativeStyleCheck {
    private static int checks;
    private static void check(boolean condition,String reason){checks++;if(!condition)throw new AssertionError(reason);}
    public static final class Meter extends ViewGroup {
        final PuiBatteryStyleCheck.NativeHorizontal battery;
        Drawable drawable;
        final BatteryControlsCheck.Charge charge=new BatteryControlsCheck.Charge();
        Meter(PuiBatteryStyleCheck.NativeHorizontal battery){super(battery.context);this.battery=battery;drawable=battery;}
        public Drawable getBatteryStyleDrawable(){return drawable;}
        public BatteryControlsCheck.Charge getBatteryCharge(){return charge;}
    }
    public static void main(String[] args)throws Exception {
        Context context=new Context();Settings.System.values.clear();Settings.System.userValues.clear();Settings.System.currentUser=0;
        check(BatteryNativeStyle.percentStyle(context)==0,"missing setting guessed percentage");
        check(!BatteryNativeStyle.percentEnabled(null),"missing context permitted PUI");
        for(int style:new int[]{-1,0,1,2,3,99}) {
            Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,style);
            check(BatteryNativeStyle.percentEnabled(context)==(style==1||style==2),"wrong OEM percentage mapping "+style);
        }
        Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,1);Settings.System.failReads=true;
        check(!BatteryNativeStyle.percentEnabled(context),"unreadable percentage setting permitted PUI");Settings.System.failReads=false;
        HashMap<String,Integer> second=new HashMap<>();second.put(BatteryNativeStyle.PERCENT_SETTING,0);
        Settings.System.userValues.put(10,second);Settings.System.currentUser=10;
        check(!BatteryNativeStyle.percentEnabled(context),"process-user percentage overrode current user");
        second.put(BatteryNativeStyle.PERCENT_SETTING,2);check(BatteryNativeStyle.percentStyle(context)==2,"current-user outside percentage ignored");
        Settings.System.currentUser=0;Settings.System.userValues.clear();
        liveBinding();
        System.out.println("BatteryNativeStyleCheck: "+checks+" checks passed");
    }
    private static void liveBinding()throws Exception {
        PuiBatteryStyleCheck.NativeHorizontal battery=new PuiBatteryStyleCheck.NativeHorizontal();battery.percentInPaint.setTextSize(38);
        Meter owner=new Meter(battery);ImageView image=new ImageView(owner.getContext());owner.addView(image);
        image.setImageDrawable(battery);image.setLayoutParams(new ViewGroup.LayoutParams(87,48));battery.setCallback(owner);
        Drawable[] nativeShapes={battery.outsideDrawable,battery.insideDrawable,battery.frameDrawable,battery.bgDrawable,battery.progressDrawable};
        Handler handler=new Handler(Looper.getMainLooper());
        BatteryControls controls=new BatteryControls(handler,Meter.class,PuiBatteryStyleCheck.NativeHorizontal.class,BatteryControlsCheck.Charge.class);
        Bundle config=new Bundle();config.putBoolean("battery_enabled",true);config.putBoolean(BatteryTextStyle.MASTER,true);
        config.putBoolean("battery_color_enabled",false);config.putBoolean(StatusBarSettings.BATTERY_CHARGE_INSIDE,true);
        controls.configure(config,StatusBarSettings.COLOR_DEFAULTS,Collections.emptyMap());
        final int[] notices={0};controls.onPercentStyleChanged(view->{check(view==owner,"setting callback targeted another owner");notices[0]++;});
        View external=new View(owner.getContext());Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,0);
        controls.sync(owner,owner.charge,external);
        check(owner.battery.context.resolver.observerUser==-1,"settings observer missed secondary users");
        check(battery.bgDrawable==nativeShapes[3]&&battery.progressDrawable==nativeShapes[4],"startup without percentage replaced native shapes");
        check(image.getLayoutParams().height==48,"startup without percentage resized native icon");
        check(!controls.drawContent(battery,new Canvas(),new RectF(0,0,116,68)),"disabled percentage still supplied custom text");
        for(int percentage:new int[]{1,0,2,0,1}) {
            battery.isShowPercentIn=percentage==1;
            Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,percentage);battery.context.resolver.dispatchSettingsChange();
            if(percentage==0) {
                check(battery.outsideDrawable==nativeShapes[0]&&battery.insideDrawable==nativeShapes[1]&&battery.frameDrawable==nativeShapes[2],"live percentage off retained PUI inside shapes");
                check(battery.bgDrawable==nativeShapes[3]&&battery.progressDrawable==nativeShapes[4],"live percentage off retained PUI outer shapes");
                check(image.getLayoutParams().height==48&&battery.percentInPaint.getTextSize()==38,"live percentage off retained PUI geometry");
                check(external.getVisibility()==View.VISIBLE&&handler.delayed==null,"live percentage off kept inside charging cycle");
                check(!controls.drawContent(battery,new Canvas(),new RectF(0,0,116,68)),"live percentage off kept custom percentage");
            } else {
                check(battery.outsideDrawable!=nativeShapes[0]&&image.getLayoutParams().height==51,"live percentage on did not reapply PUI");
                check(battery.percentInPaint.getTextSize()==40,"live percentage on missed PUI text metrics");
            }
        }
        check(notices[0]==5,"percentage setting did not notify icon-pack gate");
        int reads=Settings.System.reads;
        for(int frame=0;frame<1000;frame++)controls.prepareDraw(battery);
        check(Settings.System.reads==reads,"battery draw queried Settings per frame");
        Drawable latest=new PuiBatteryStyleCheck.Graphic();battery.bgDrawable=latest;
        Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,0);battery.context.resolver.dispatchSettingsChange();
        check(battery.bgDrawable==latest,"percentage off lost latest OEM background");
        check(image.getLayoutParams().height==48,"percentage off lost latest native geometry");
        Settings.System.values.put(BatteryNativeStyle.PERCENT_SETTING,1);battery.context.resolver.dispatchSettingsChange();
        check(image.getLayoutParams().height==51,"replacement fixture did not own PUI geometry");
        owner.drawable=new PuiBatteryStyleCheck.Graphic();controls.sync(owner);
        check(image.getLayoutParams().height==48,"switching native battery type retained horizontal PUI geometry");
        check(battery.bgDrawable==latest,"switching native battery type retained PUI assets");
        controls.detach(owner);check(battery.context.resolver.observers.isEmpty(),"detached battery retained settings observer");
    }
}
