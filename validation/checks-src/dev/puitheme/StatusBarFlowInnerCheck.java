package dev.puitheme;
import android.os.Bundle;
import androidx.compose.ui.Modifier;
import androidx.compose.foundation.layout.PaddingKt.PaddingElement;
import androidx.compose.foundation.layout.RowMeasurePolicy;
import androidx.compose.ui.layout.MeasurePolicy;
import androidx.compose.ui.node.LayoutNode;
import androidx.compose.ui.node.MeasurePassDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.View;
import android.widget.TextView;
import java.util.*;

/** Actual native signed-margin measure/placement and reciprocal ConstraintLayout paths. */
public final class StatusBarFlowInnerCheck {
    private static int checks;
    private static void check(boolean result,String why) { checks++;if(!result)throw new AssertionError(why); }
    public static final class Scope {
        final boolean rtl; Scope(){this(false);} Scope(boolean rtl){this.rtl=rtl;}
        public String getLayoutDirection(){return rtl?"Rtl":"Ltr";} public float getDensity(){return 1f;}
    }
    private static Bundle bundle(int order,float left,float right) {
        Bundle values=new Bundle();values.putBoolean(StatusBarLayoutSettings.MASTER,true);
        values.putFloat(StatusBarLayoutSettings.key("label","order"),order);
        values.putFloat(StatusBarLayoutSettings.key("label","margin_left"),left);
        values.putFloat(StatusBarLayoutSettings.key("label","margin_right"),right);return values;
    }
    private static int paddingCount(Modifier modifier,float left,float right) {
        return (Integer)modifier.foldIn(0,(count,element)->{
            if(element instanceof PaddingElement){PaddingElement p=(PaddingElement)element;check(p.left==left&&p.right==right,"physical gaps exact");return (Integer)count+1;}
            return count;
        });
    }
    private static final class R extends android.content.res.Resources {
        R(){getDisplayMetrics().density=1f;}
        @Override public int getIdentifier(String name,String type,String pkg){return name.equals("mobile_signal_data_outer")?11:0;}
    }
    private static final class Label extends TextView {
        final R r=new R();int direction;
        Label(){super(null);setId(12);}
        @Override public android.content.res.Resources getResources(){return r;}
        @Override public int getLayoutDirection(){return direction;}
    }
    public static void main(String[] ignored)throws Exception {
        NetworkIconOrder helper=new NetworkIconOrder();helper.resolveCompose(StatusBarFlowInnerCheck.class.getClassLoader());
        helper.configure(bundle(1,3,5));
        Modifier nativeModifier=new Modifier.Element(), current=(Modifier)helper.textModifier(nativeModifier);
        for(int i=0;i<100;i++){check(paddingCount(current,3,5)==0,"native signed margin uses no nonnegative-only padding");current=(Modifier)helper.textModifier(current);}
        helper.configure(bundle(2,7,2));current=(Modifier)helper.textModifier(current);check(paddingCount(current,7,2)==0,"settings update never introduces padding");
        LayoutNode text=new LayoutNode("text",17),signal=new LayoutNode("signal",20);
        text.setModifier(current);helper.nodeChanged(text,current);
        helper.configure(bundle(2,11,13));check(paddingCount(text.modifier,11,13)==0,"live native node retains only its native modifier and identity tag");
        Bundle independent=bundle(2,4,6);independent.putBoolean(StatusBarLayoutSettings.MASTER,false);
        helper.configure(independent);check(paddingCount(text.modifier,4,6)==0,"independent margins never create padding");
        check(StatusBarLayoutSettings.Snapshot.parse(independent).order("label")==0,"persisted inner ordering ignored with sorting off");
        helper.configure(bundle(2,11,13));
        current=text.modifier;
        Modifier signalModifier=(Modifier)helper.signalModifier(Modifier.Companion);signal.setModifier(signalModifier);helper.nodeChanged(signal,signalModifier);
        RowMeasurePolicy nativePolicy=new RowMeasurePolicy();nativePolicy.placementForCheck=helper;MeasurePolicy policy;
        try(NetworkIconOrder.RowScope scope=helper.beginRow(new Object())){policy=(MeasurePolicy)helper.rowPolicy(nativePolicy);}
        List<MeasurePassDelegate> children=Arrays.asList(new MeasurePassDelegate(text),new MeasurePassDelegate(signal));
        Map<String,Integer> first=policy.measure(new Scope(),children,123);check(nativePolicy.lastChildren.get(0)==children.get(1),"order2 signal before type");
        check(first.get("signal")==0&&first.get("text")==31&&nativePolicy.lastMeasuredWidth==61,"positive margins reserve actual measured width and native X");
        for(boolean rtl:new boolean[]{false,true})for(int order:new int[]{1,2})for(int[] gaps:new int[][]{{3,5},{-3,-5},{-3,5},{3,-5},{-100,0},{0,-100}}){
            helper.configure(bundle(order,gaps[0],gaps[1]));
            Map<String,Integer> actual=policy.measure(new Scope(rtl),children,123);
            int expectedText=(order==1?0:20)+gaps[0],expectedSignal=order==1?17+gaps[0]+gaps[1]:0;
            check(actual.get("text")==expectedText&&actual.get("signal")==expectedSignal,"physical signed native row positions honor RTL and order");
            check(nativePolicy.lastMeasuredWidth==Math.max(0,37+gaps[0]+gaps[1]),"signed margins change actual row width without negative dimensions");
            check(policy.minIntrinsicWidth(new Scope(rtl),children,80)==Math.max(0,37+gaps[0]+gaps[1]),"intrinsic width uses same signed margins");
        }
        helper.configure(bundle(1,-3,-5));Map<String,Integer> previous=policy.measure(new Scope(),children,123);int writes=text.modifier==null?0:1;
        for(int i=0;i<20;i++)check(policy.measure(new Scope(),children,123).equals(previous),"negative margins do not accumulate across frames");
        check(writes==1&&paddingCount(text.modifier,0,0)==0,"negative mode uses no padding allocation");
        check(NetworkIconOrder.marginPixels(-2.5f,4)==-10&&NetworkIconOrder.marginPixels(2.5f,4)==10,"signed dp uses native density");
        check(NetworkIconOrder.marginPixels(-Float.MAX_VALUE,4)==-65535&&NetworkIconOrder.marginPixels(Float.MAX_VALUE,4)==65535,"huge stored values only clamp at native renderer boundary");
        Bundle savedNegative=bundle(1,-3,-5);savedNegative.putBoolean(StatusBarLayoutSettings.MASTER,false);helper.configure(savedNegative);
        Map<String,Integer> independentActual=policy.measure(new Scope(),children,123);
        check(independentActual.get("text")==-3&&independentActual.get("signal")==9&&nativePolicy.lastMeasuredWidth==29,"signed margins work with sorting off");
        NetworkSharedSlotLayout bridge=new NetworkSharedSlotLayout();bridge.resolve(StatusBarFlowInnerCheck.class.getClassLoader());bridge.configure(true);helper.sharedLayout(bridge);
        Bundle shared=bundle(1,-30,-50);shared.putBoolean(StatusBarLayoutSettings.WIFI_LABEL_SHARED,true);shared.putFloat(StatusBarLayoutSettings.key("wifi","margin_left"),-12);
        helper.configure(shared);Map<String,Integer> sharedActual=policy.measure(new Scope(),children,123);
        check(sharedActual.get("text")==0&&sharedActual.get("signal")==17&&nativePolicy.lastMeasuredWidth==37,"shared outer canonical margins are never duplicated inside native label Row");
        helper.configure(new Bundle());current=(Modifier)helper.textModifier(current);check(paddingCount(current,0,0)==0,"closing restores modifier gaps");
        check(paddingCount(text.modifier,0,0)==0,"live native node restores immediately");
        check(current==nativeModifier,"exact native modifier restored");
        for(int direction:new int[]{View.LAYOUT_DIRECTION_LTR,View.LAYOUT_DIRECTION_RTL}) for(int order:new int[]{1,2})for(int[] gaps:new int[][]{{3,5},{-3,-5},{-3,5},{3,-5}}) {
            ConstraintLayout parent=new ConstraintLayout();Label label=new Label();label.direction=direction;View signalView=new View(null);signalView.setId(11);
            ConstraintLayout.LayoutParams labelNative=new ConstraintLayout.LayoutParams(-2,-2),signalNative=new ConstraintLayout.LayoutParams(-2,-2);
            parent.addView(label,labelNative);parent.addView(signalView,signalNative);
            SingleNetworkLabelPlacement placement=new SingleNetworkLabelPlacement(label);placement.configure(order,gaps[0],gaps[1]);
            check(placement.apply(),"real LP copy accepted");ConstraintLayout.LayoutParams actual=(ConstraintLayout.LayoutParams)label.getLayoutParams();
            check(actual.leftMargin==gaps[0]&&actual.rightMargin==gaps[1],"physical signed LP margins");
            check(actual.getMarginStart()==(direction==View.LAYOUT_DIRECTION_RTL?gaps[1]:gaps[0]),"native relative margin mapping preserves physical negative values in RTL");
            boolean after=order==2&&direction==View.LAYOUT_DIRECTION_LTR||order==1&&direction==View.LAYOUT_DIRECTION_RTL;
            check(after?actual.startToEnd==11:actual.endToStart==11,"physical label order honors RTL");
            check(labelNative.leftMargin==0&&signalNative.rightMargin==0,"native LPs untouched");placement.restore();
            check(label.getLayoutParams()==labelNative&&signalView.getLayoutParams()==signalNative,"original LP identities restored");
        }
        ConstraintLayout parent=new ConstraintLayout();Label label=new Label();View signalView=new View(null);signalView.setId(11);
        ConstraintLayout.LayoutParams labelNative=new ConstraintLayout.LayoutParams(-2,-2),signalNative=new ConstraintLayout.LayoutParams(-2,-2);
        parent.addView(label,labelNative);parent.addView(signalView,signalNative);label.setText("5G");
        SingleNetworkLabelControls controls=new SingleNetworkLabelControls(new SingleNetworkLabelControls.Access(){
            public TextView text(Object binding){return label;} public View image(Object binding){return null;}
            public int subscription(Object binding){return 8;} public void bind(Object binding,Object model){label.setText(model==null?"5G":model.toString());}
        });
        controls.bound(label);Bundle options=new Bundle();options.putBoolean("label_enabled",true);options.putBoolean("label_text_style_enabled",false);
        FeatureOptions style=FeatureOptions.from(options);
        controls.configureLayout(bundle(1,3,5));controls.update(style,"5G",8,0,0,100,22,600,Collections.emptyMap(),Collections.emptyMap());
        check(((ConstraintLayout.LayoutParams)label.getLayoutParams()).leftMargin==3,"first single-SIM flow gap");
        controls.configureLayout(bundle(2,7,9));controls.update(style,"5G",8,0,0,100,22,600,Collections.emptyMap(),Collections.emptyMap());
        ConstraintLayout.LayoutParams changed=(ConstraintLayout.LayoutParams)label.getLayoutParams();
        check(changed.leftMargin==7&&changed.rightMargin==9&&changed.startToEnd==11,"only inner settings change applies immediately despite style cache");
        controls.configureLayout(bundle(1,-7,-9));controls.update(style,"5G",8,0,0,100,22,600,Collections.emptyMap(),Collections.emptyMap());
        changed=(ConstraintLayout.LayoutParams)label.getLayoutParams();check(changed.leftMargin==-7&&changed.rightMargin==-9,"single controls preserve saved signed values");
        controls.configureLayout(shared);controls.update(style,"5G",8,0,0,100,22,600,Collections.emptyMap(),Collections.emptyMap());
        changed=(ConstraintLayout.LayoutParams)label.getLayoutParams();check(changed.leftMargin==0&&changed.rightMargin==0,"single shared outer margins never repeat inside the label chain");
        controls.releaseRuntime();check(label.getLayoutParams()==labelNative&&signalView.getLayoutParams()==signalNative,"single-SIM release restores original geometry");
        System.out.println("StatusBarFlowInnerCheck: "+checks+" checks passed");
    }
}
