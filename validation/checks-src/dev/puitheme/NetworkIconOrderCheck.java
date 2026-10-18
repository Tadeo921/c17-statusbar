package dev.puitheme;

import android.os.Bundle;
import android.view.View;
import androidx.compose.foundation.layout.RowMeasurePolicy;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.MeasurePolicy;
import androidx.compose.ui.node.LayoutNode;
import androidx.compose.ui.node.MeasurePassDelegate;
import androidx.compose.ui.node.LookaheadPassDelegate;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import java.util.*;

/** Executes the native index/state/RTL chain and deferred Compose node updates, rather than draw translations. */
public final class NetworkIconOrderCheck {
    private static int checks;
    private static void equal(Object expected,Object actual) { checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Network order "+checks+": "+expected+" != "+actual); }
    private static Bundle settings(boolean on) { Bundle b=new Bundle();b.putBoolean(NetworkIconOrder.SWAP,on);return b; }
    private static final class Icon extends View implements StatusIconDisplayable {
        final String name; String slot; final int width,padding; boolean visible=true,blocked; float target;
        int animations; final Object nativeState=new Object();
        Icon(String name,String slot,int width,int padding){super(null);this.name=name;this.slot=slot;this.width=width;this.padding=padding;}
        public String getSlot(){return slot;}public boolean isIconVisible(){return visible;}public boolean isIconBlocked(){return blocked;}
        @Override public int getWidth(){return width;}
        @Override public int getPaddingStart(){return padding;}
        @Override public int getPaddingEnd(){return padding;}
    }
    /** DEX updateStates enumerates from native end, accumulates each width/padding, then mirrors for RTL. */
    private static Map<String,Float> nativeLayout(NetworkIconOrder order,StatusIconContainer parent,boolean rtl) {
        NetworkIconOrder.IndexScope scope=order.begin(parent);
        try {
            parent.mMeasureViews.clear();
            for(int i=0;i<parent.getChildCount();i++){Icon icon=(Icon)parent.getChildAt(i);if(icon.visible&&!icon.blocked&&!parent.mIgnoredSlots.contains(icon.slot))parent.mMeasureViews.add(icon);}
            float x=300;Map<String,Float> positions=new LinkedHashMap<>();
            for(int i=parent.getChildCount()-1;i>=0;i--){Icon icon=(Icon)parent.getChildAt(i);if(!icon.visible||icon.blocked||parent.mIgnoredSlots.contains(icon.slot))continue;x-=icon.width+2*icon.padding;icon.target=rtl?300-x-icon.width:x;icon.animations++;positions.put(icon.name,icon.target);}
            return positions;
        } finally { if(scope!=null)scope.close(); }
    }
    private static List<String> measured(StatusIconContainer parent){List<String> result=new ArrayList<>();for(View view:parent.mMeasureViews)result.add(((Icon)view).name);return result;}
    private static void apply(NetworkIconOrder helper,LayoutNode node,Modifier modifier){node.setModifier(modifier);helper.nodeChanged(node,modifier);}
    private static MeasurePolicy policy(NetworkIconOrder helper,RowMeasurePolicy nativePolicy) {NetworkIconOrder.RowScope scope=helper.beginRow(new Object());try{return (MeasurePolicy)helper.rowPolicy(nativePolicy);}finally{if(scope!=null)scope.close();}}
    private static NetworkIconOrder helper()throws Exception{NetworkIconOrder h=new NetworkIconOrder();h.resolve(NetworkIconOrderCheck.class.getClassLoader());h.resolveCompose(NetworkIconOrderCheck.class.getClassLoader());return h;}
    public static void main(String[] ignored)throws Exception {
        nativeIndexGate();
        NetworkIconOrder h=helper();StatusIconContainer parent=new StatusIconContainer();parent.ordering=h;
        Icon other=new Icon("other","alarm_clock",9,1),wifi=new Icon("wifi","wifi",17,2),mobile=new Icon("mobile","stacked_mobile",31,3);
        parent.addView(other);parent.addView(wifi);parent.addView(mobile);
        equal(false,h.configure(new Bundle()));nativeLayout(h,parent,false);equal(Arrays.asList("other","wifi","mobile"),measured(parent));
        equal(false,StatusBarSettings.bool(Collections.emptyMap(),NetworkIconOrder.SWAP));
        Object state=mobile.nativeState;wifi.setAlpha(.37f);mobile.setAlpha(.81f);
        equal(true,h.configure(settings(true)));Map<String,Float> swapped=nativeLayout(h,parent,false);
        equal(Arrays.asList("other","mobile","wifi"),measured(parent));equal(279f,swapped.get("wifi"));equal(242f,swapped.get("mobile"));equal(231f,swapped.get("other"));
        equal(state,mobile.nativeState);equal(.37f,wifi.getAlpha());equal(.81f,mobile.getAlpha());equal(0f,wifi.getTranslationX());equal(0f,mobile.getTranslationX());equal(2,mobile.animations);
        equal(wifi,parent.mMeasureViews.get(parent.mMeasureViews.size()-1)); // Actual tail consumed by DataBatterySpacing.
        Map<String,Float> rtl=nativeLayout(h,parent,true);equal(4f,rtl.get("wifi"));equal(27f,rtl.get("mobile"));equal(60f,rtl.get("other"));
        int reads=parent.childReads,requests=parent.layoutRequests;
        for(int i=0;i<1000;i++){NetworkIconOrder.IndexScope scope=h.begin(parent);try{equal(2,h.childIndex(parent,1));}finally{scope.close();}}
        equal(reads,parent.childReads);equal(requests,parent.layoutRequests); // No child/resource scan or layout invalidation at steady scope entry.
        equal(1,h.childIndex(parent,1));equal(-1,h.childIndex(parent,-1));
        try(NetworkIconOrder.IndexScope a=h.begin(parent)){try(NetworkIconOrder.IndexScope b=h.begin(parent)){equal(2,h.childIndex(parent,1));}equal(2,h.childIndex(parent,1));}equal(1,h.childIndex(parent,1));
        try{try(NetworkIconOrder.IndexScope s=h.begin(parent)){throw new IllegalStateException();}}catch(IllegalStateException expected){}equal(1,h.childIndex(parent,1));
        wifi.visible=false;nativeLayout(h,parent,false);equal(Arrays.asList("other","mobile"),measured(parent));wifi.visible=true;
        parent.mIgnoredSlots.add("wifi");nativeLayout(h,parent,false);equal(Arrays.asList("other","mobile"),measured(parent));parent.mIgnoredSlots.clear();
        mobile.blocked=true;nativeLayout(h,parent,false);equal(Arrays.asList("other","wifi"),measured(parent));mobile.blocked=false;
        Icon secondary=new Icon("secondary","mobile_2",23,1);parent.addView(secondary);h.changed(parent);nativeLayout(h,parent,false);equal(Arrays.asList("other","mobile","secondary","wifi"),measured(parent));
        secondary.visible=false;nativeLayout(h,parent,false);equal(Arrays.asList("other","mobile","wifi"),measured(parent));secondary.visible=true;
        parent.removeView(mobile);h.changed(parent);nativeLayout(h,parent,false);equal(Arrays.asList("other","secondary","wifi"),measured(parent));
        secondary.slot="unknown_slot";h.bound(secondary);nativeLayout(h,parent,false);equal(Arrays.asList("other","wifi","secondary"),measured(parent));secondary.slot="mobile_2";h.bound(secondary);
        Bundle safe=settings(true);safe.putBoolean(StatusBarSettings.SAFE_MODE,true);equal(true,h.configure(safe));nativeLayout(h,parent,false);equal(Arrays.asList("other","wifi","secondary"),measured(parent));
        equal(true,h.configure(settings(true)));nativeLayout(h,parent,false);equal(Arrays.asList("other","secondary","wifi"),measured(parent));
        // Composer defers node.setModifier until AFTER both exact native composable scopes returned.
        Modifier.Element nativeSignal=new Modifier.Element(),nativeText=new Modifier.Element(),nativeRoam=new Modifier.Element();
        LayoutNode signal=new LayoutNode("signal",31),text=new LayoutNode("5G",12),roam=new LayoutNode("roaming",7);
        Modifier taggedSignal=(Modifier)h.signalModifier(nativeSignal),taggedText=(Modifier)h.textModifier(nativeText);
        apply(h,signal,taggedSignal);apply(h,text,taggedText);apply(h,roam,nativeRoam);
        MeasurePassDelegate s=new MeasurePassDelegate(signal),t=new MeasurePassDelegate(text),r=new MeasurePassDelegate(roam);
        RowMeasurePolicy original=new RowMeasurePolicy();MeasurePolicy wrapped=policy(h,original);
        equal(false,wrapped==original);equal(wrapped,policy(h,original));equal(original,h.rowPolicy(original));
        Map<String,Integer> left=wrapped.measure(false,Arrays.asList(r,s,t),0x12345678L);equal(0,left.get("roaming"));equal(7,left.get("5G"));equal(19,left.get("signal"));
        equal(false,original.lastScope);equal(0x12345678L,original.lastConstraints);equal(Arrays.asList(r,t,s),original.lastChildren);equal(50,wrapped.minIntrinsicWidth(false,Arrays.asList(r,s,t),20));
        Map<String,Integer> right=wrapped.measure(true,Arrays.asList(r,s,t),77);equal(43,right.get("roaming"));equal(31,right.get("5G"));equal(0,right.get("signal"));
        // Same retained native policy after type/info disappears: roaming must never exchange with signal.
        left=wrapped.measure(false,Arrays.asList(r,s),88);equal(0,left.get("roaming"));equal(7,left.get("signal"));
        left=wrapped.measure(false,Collections.singletonList(s),88);equal(0,left.get("signal"));
        LayoutNode impostor=new LayoutNode("fake5G",12);apply(h,impostor,new Modifier.Element());MeasurePassDelegate fake=new MeasurePassDelegate(impostor);
        left=wrapped.measure(false,Arrays.asList(s,fake),88);equal(0,left.get("signal"));equal(31,left.get("fake5G"));
        // Native reuse clears old classification; two tagged signal boxes are also deliberately ambiguous.
        apply(h,text,nativeText);left=wrapped.measure(false,Arrays.asList(s,t),88);equal(0,left.get("signal"));
        apply(h,text,taggedText);left=wrapped.measure(false,Arrays.asList(s,t),88);equal(12,left.get("signal"));
        left=wrapped.measure(false,Arrays.asList(s,s,t),88);equal(Arrays.asList(s,s,t),original.lastChildren);
        LookaheadPassDelegate lookSignal=new LookaheadPassDelegate(signal),lookText=new LookaheadPassDelegate(text);
        left=wrapped.measure(false,Arrays.asList(lookSignal,lookText),99);equal(0,left.get("5G"));equal(12,left.get("signal"));
        int signalReads=nativeSignal.reads,textReads=nativeText.reads;
        for(int i=0;i<1000;i++){left=wrapped.measure(false,Arrays.asList(s,t),1);equal(12,left.get("signal"));}
        equal(signalReads,nativeSignal.reads);equal(textReads,nativeText.reads); // Modifier traversal is an event, never part of each measurement.
        Object composer=new Object(),modelA=new Object(),modelB=new Object(),strategyA=new Object(),strategyB=new Object();
        equal(true,h.forceStrategy(composer,modelA,strategyA));equal(true,h.forceStrategy(composer,modelB,strategyA));equal(true,h.forceStrategy(composer,modelA,strategyB));equal(false,h.forceStrategy(composer,modelA,strategyA));
        equal(true,h.configure(settings(false)));equal(true,h.forceStrategy(composer,modelA,strategyA));equal(true,h.forceStrategy(composer,modelB,strategyA));equal(true,h.forceStrategy(composer,modelA,strategyB));equal(false,h.forceStrategy(composer,new Object(),strategyA));
        equal(nativeSignal,h.signalModifier(nativeSignal));equal(original,policy(h,original));left=wrapped.measure(false,Arrays.asList(s,t),1);equal(0,left.get("signal"));equal(31,left.get("5G"));
        equal(true,h.configure(settings(true)));equal(true,h.forceStrategy(composer,modelA,strategyA));left=wrapped.measure(false,Arrays.asList(s,t),1);equal(12,left.get("signal"));
        h.releaseRuntime();equal(false,h.configure(settings(true)));equal(null,h.begin(parent));equal(false,h.forceStrategy(composer,modelA,strategyA));equal(nativeText,h.textModifier(nativeText));left=wrapped.measure(false,Arrays.asList(s,t),1);equal(0,left.get("signal"));
        equal(false,((Map<?,?>)field(h,"containers")).size()>0);equal(false,((Map<?,?>)field(h,"nodeKinds")).size()>0);equal(false,((Map<?,?>)field(h,"composerRevisions")).size()>0);
        effectiveNativeChildren();
        System.out.println("NetworkIconOrderCheck: "+checks+" checks passed");
    }
    /** Device code163 shared-slot settings needed Compose placement but installed a no-op global getChildAt hook. */
    private static void nativeIndexGate()throws Exception{
        NetworkIconOrder h=helper();equal(false,h.nativeIndexWanted());
        Bundle swap=settings(true);h.configure(swap);equal(true,h.nativeIndexWanted());
        Bundle margin=settings(true);margin.putFloat(StatusBarLayoutSettings.key("clock","margin_left"),2);
        h.configure(margin);equal(false,h.nativeIndexWanted());equal(true,h.wanted());
        Bundle order=settings(true);order.putBoolean(StatusBarLayoutSettings.MASTER,true);
        h.configure(order);equal(false,h.nativeIndexWanted());
        NetworkSharedSlotLayout shared=new NetworkSharedSlotLayout();shared.configure(true);h.sharedLayout(shared);
        Bundle sharing=new Bundle();sharing.putBoolean(StatusBarLayoutSettings.WIFI_LABEL_SHARED,true);
        h.configure(sharing);equal(true,h.wanted());equal(false,h.nativeIndexWanted());
        h.configure(swap);equal(true,h.nativeIndexWanted());
        swap.putBoolean(StatusBarSettings.SAFE_MODE,true);h.configure(swap);equal(false,h.nativeIndexWanted());
        h.releaseRuntime();equal(false,h.nativeIndexWanted());
    }
    private static final class ThrowingParent extends StatusIconContainer {
        boolean fail;
        @Override public View getChildAt(int index){if(fail)throw new IllegalStateException("native read failed");return super.getChildAt(index);}
    }
    /** Reader use before/inside native scopes must yield the same model input without swapping twice. */
    private static void effectiveNativeChildren()throws Exception{
        NetworkIconOrder h=helper();ThrowingParent p=new ThrowingParent();p.ordering=h;
        Icon other=new Icon("other","alarm_clock",9,1),wifi=new Icon("wifi","wifi",17,2),cell=new Icon("cell","mobile",31,3);
        p.addView(other);p.addView(wifi);p.addView(cell);
        List<View> raw=Arrays.asList(other,wifi,cell),swapped=Arrays.asList(other,cell,wifi);
        equal(raw,h.orderedChildren(p));h.configure(settings(true));equal(swapped,h.orderedChildren(p));
        try(NetworkIconOrder.IndexScope outer=h.begin(p)){
            equal(cell,p.getChildAt(1));equal(swapped,h.orderedChildren(p));equal(cell,p.getChildAt(1));
            try(NetworkIconOrder.IndexScope inner=h.begin(p)){
                equal(swapped,h.orderedChildren(p));equal(2,h.childIndex(p,1));
            }
            equal(2,h.childIndex(p,1));
            StatusIconContainer otherParent=new StatusIconContainer();otherParent.ordering=h;
            Icon otherWifi=new Icon("otherWifi","wifi",17,2),otherCell=new Icon("otherCell","mobile",31,3);
            otherParent.addView(otherWifi);otherParent.addView(otherCell);
            try(NetworkIconOrder.IndexScope inner=h.begin(otherParent)){
                equal(Arrays.asList(otherCell,otherWifi),h.orderedChildren(otherParent));
                equal(swapped,h.orderedChildren(p));equal(1,h.childIndex(otherParent,0));equal(1,h.childIndex(p,1));
            }
            equal(2,h.childIndex(p,1));equal(0,h.childIndex(otherParent,0));
            p.fail=true;try{h.orderedChildren(p);throw new AssertionError("reader must propagate native read failure");}
            catch(IllegalStateException expected){}finally{p.fail=false;}
            equal(2,h.childIndex(p,1));equal(cell,p.getChildAt(1));
        }
        equal(1,h.childIndex(p,1));
        int requests=p.layoutRequests;for(int i=0;i<64;i++)equal(swapped,h.orderedChildren(p));equal(requests,p.layoutRequests);
        wifi.visible=false;equal(raw,h.orderedChildren(p));wifi.visible=true;
        cell.blocked=true;equal(raw,h.orderedChildren(p));cell.blocked=false;
        p.mIgnoredSlots.add("wifi");equal(raw,h.orderedChildren(p));p.mIgnoredSlots.clear();
        wifi.setVisibility(View.GONE);equal(raw,h.orderedChildren(p));wifi.setVisibility(View.VISIBLE);
        wifi.setAlpha(0f);equal(swapped,h.orderedChildren(p)); // Native animation alpha is not source eligibility.
        h.configure(settings(false));equal(raw,h.orderedChildren(p));h.configure(settings(true));
        Map<?,?> cache=(Map<?,?>)field(h,"containers");equal(true,cache instanceof WeakHashMap);
        Object stored=cache.get(p);List<?> references=(List<?>)field(stored,"sources");
        for(Object reference:references){equal(true,reference instanceof java.lang.ref.WeakReference);((java.lang.ref.WeakReference<?>)reference).clear();}
        equal(swapped,h.orderedChildren(p)); // A snapshot never relies on a stale weak-cache identity.
        p.removeView(cell);equal(Arrays.asList(other,wifi),h.orderedChildren(p));
        p.addView(cell);h.changed(p);equal(swapped,h.orderedChildren(p));
        equal(null,h.orderedChildren(new android.widget.FrameLayout(null)));
        Bundle safe=settings(true);safe.putBoolean(StatusBarSettings.SAFE_MODE,true);h.configure(safe);equal(raw,h.orderedChildren(p));
        h.releaseRuntime();equal(null,h.orderedChildren(p));equal(0,cache.size());
    }
    private static Object field(Object owner,String name)throws Exception{java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
}
