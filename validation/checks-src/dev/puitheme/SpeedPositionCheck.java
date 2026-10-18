package dev.puitheme;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.content.res.Resources;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView;
import com.oplus.systemui.statusbar.phone.netspeed.NetworkSpeedIconState;
import java.util.*;

/** Executes native target allocation, state callbacks and FrameLayout reservations; never uses draw translations. */
public final class SpeedPositionCheck {
    private static int checks;
    /** Nothing is learned any more, so there is nothing to wait for; kept so the call sites still read as "the state has settled". */
    private static void settled(){ }
    private static void equal(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError("SpeedPosition "+checks+": "+a+" != "+b);}
    private static final Resources RES=new Resources(){@Override public int getIdentifier(String name,String type,String pkg){
        switch(name){case "status_bar_view_state_tag":return 1;case "clock":return 2;case "clock_for_fake":return 3;
            case "status_bar_start_side_container":return 4;case "network_speed_view":return 5;default:return 0;}
    }};
    private static final class Phone extends PhoneStatusBarView {
        final Map<Integer,View> ids=new HashMap<>();
        @Override public View findViewById(int id){return ids.get(id);}
    }
    private static final class Parent extends StatusIconContainer {
        int width=100,extraWidth=16,requests;boolean rtl;
        @Override public void requestLayout(){requests++;super.requestLayout();}
        @Override public Resources getResources(){return RES;}
        @Override public int getWidth(){return width;}
        @Override public int getLeft(){return 400-width;} // Phone's real end-aligned wrap-content row.
        @Override public int getLayoutDirection(){return rtl?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR;}
    }
    private static final class ClockFrame extends FrameLayout {
        int l=3,r=5,t=1,b=2;
        ClockFrame(){super(null);}
        @Override public void setPadding(int l,int t,int r,int b){this.l=l;this.t=t;this.r=r;this.b=b;requestLayout();}
        @Override public int getPaddingLeft(){return l;}@Override public int getPaddingTop(){return t;}
        @Override public int getPaddingRight(){return r;}@Override public int getPaddingBottom(){return b;}
        @Override public int getMeasuredWidth(){return 50+l+r;}
    }
    private static final class Clock extends TextView {Clock(){super(null);}@Override public int getMeasuredWidth(){return 50;}}
    private static final class Start extends FrameLayout {int width=150;Start(){super(null);}@Override public int getWidth(){return width;}}
    private static final class Icon extends View implements StatusIconDisplayable {
        String slot;boolean shown=true,blocked;int width=20;final StatusIconContainer.StatusIconState state=new StatusIconContainer.StatusIconState();
        Icon(String slot){super(null);this.slot=slot;}
        public String getSlot(){return slot;}public boolean isIconVisible(){return shown;}public boolean isIconBlocked(){return blocked;}
        @Override public int getWidth(){return width;}@Override public Object getTag(int id){return id==1?state:null;}
    }
    private static Bundle settings(String where){Bundle b=new Bundle();b.putBoolean("speed_enabled",true);b.putBoolean("speed_position_enabled",true);b.putString(SpeedPosition.POSITION,where);b.putFloat(SpeedPosition.EDGE_RESERVE,40f);return b;}
    private static Bundle settings(String where,float reserveDp){Bundle b=settings(where);b.putFloat(SpeedPosition.EDGE_RESERVE,reserveDp);return b;}
    private static StatusIconContainer.StatusIconState state(View v){return (StatusIconContainer.StatusIconState)v.getTag(1);}
    /** Actual DEX allocates from the end, then mirrors RTL, then calls each native state.applyToView. */
    private static void targets(SpeedPosition h,Parent p,boolean rtl){
        p.rtl=rtl;float x=p.getWidth();boolean previous=false;
        for(int i=p.getChildCount()-1;i>=0;i--){View v=p.getChildAt(i);StatusIconContainer.StatusIconState s=state(v);
            StatusIconDisplayable nativeIcon=(StatusIconDisplayable)v;
            boolean blocked=h.iconBlocked(v,nativeIcon.isIconBlocked())||p.mIgnoredSlots.contains(nativeIcon.getSlot())||!nativeIcon.isIconVisible();
            if(blocked){s.visibleState=2;continue;}
            if(previous)x-=p.mIconSpacing;previous=true;
            x-=v.getWidth()+v.getPaddingStart()+h.allocationPaddingEnd(v,v.getPaddingEnd());s.visibleState=0;s.setXTranslation(x);}
        if(rtl)for(int i=0;i<p.getChildCount();i++){View v=p.getChildAt(i);StatusIconContainer.StatusIconState s=state(v);s.setXTranslation(p.getWidth()-s.getXTranslation()-v.getWidth());}
    }
    private static void measure(SpeedPosition h,Parent p){
        int width=p.extraWidth,count=0;
        try(SpeedPosition.Scope scope=h.begin(p,false)){
            for(int i=0;i<p.getChildCount();i++){
                View child=p.getChildAt(i);StatusIconDisplayable displayable=(StatusIconDisplayable)child;
                if(h.iconBlocked(child,displayable.isIconBlocked())||!displayable.isIconVisible()||p.mIgnoredSlots.contains(displayable.getSlot()))continue;
                width+=child.getMeasuredWidth()+child.getPaddingStart()+h.allocationPaddingEnd(child,child.getPaddingEnd());count++;
            }
        }
        p.width=width+Math.max(0,count-1)*p.mIconSpacing;
    }
    private static void layout(SpeedPosition h,Parent p,boolean rtl,boolean change){
        p.rtl=rtl;measure(h,p);
        if(change)h.beforeLayout(p);
        try(SpeedPosition.Scope scope=h.begin(p,true)){
            targets(h,p,rtl);
            for(int i=0;i<p.getChildCount();i++){View v=p.getChildAt(i);h.beforeApply(state(v),v);}
        }
    }
    public static void main(String[] args)throws Exception{
        equal("native",SpeedPosition.position("bad"));equal("native",SpeedPosition.position(null));
        equal("native",StatusBarSettings.string(Collections.emptyMap(),SpeedPosition.POSITION));
        SettingsCatalog.Item item=SettingsCatalog.item(SpeedPosition.POSITION);
        equal(null,item);
        for(String old:new String[]{SpeedPosition.POSITION,SpeedPosition.SAFE_GAP_ENABLED,SpeedPosition.SAFE_GAP,
                SpeedPosition.EDGE_RESERVE,StatusBarSettings.SPEED_OFFSET_X}) {
            equal(null,SettingsCatalog.item(old));equal(true,SettingsCatalog.hiddenKeys().contains(old));
            equal(true,ConfigTransfer.types().containsKey(old));
        }
        Map<String,Object> legacy=new LinkedHashMap<>();legacy.put(SpeedPosition.POSITION,SpeedPosition.CLOCK_RIGHT);
        legacy.put(SpeedPosition.SAFE_GAP_ENABLED,true);legacy.put(SpeedPosition.SAFE_GAP,-3.25f);
        legacy.put(StatusBarSettings.SPEED_OFFSET_X,12.5f);
        equal(legacy,ConfigTransfer.prepare(ConfigTransfer.exportJson(legacy,legacy.keySet()),true).values());
        SettingsCatalog.Item layoutSide=SettingsCatalog.item(StatusBarLayoutSettings.key(StatusBarLayoutSettings.SPEED,"side"));
        equal(StatusBarLayoutSettings.GROUP,layoutSide.groupId);equal(SettingsCatalog.OPTIONS,layoutSide.type);
        equal(Arrays.asList("native","left","right"),Arrays.asList(layoutSide.values));
        for(String suffix:new String[]{"order","margin_left","margin_right"}) {
            SettingsCatalog.Item current=SettingsCatalog.item(StatusBarLayoutSettings.key(StatusBarLayoutSettings.SPEED,suffix));
            equal("order".equals(suffix)?StatusBarLayoutSettings.GROUP:"speed",current.groupId);equal(0f,current.defaultValue);
        }
        equal("speed",SettingsCatalog.item(StatusBarSettings.SPEED_OFFSET_Y).groupId);
        int[] made={0};SpeedPosition h=new SpeedPosition((source,parent,layout)->{made[0]++;return new NetworkSpeedView();});h.resolve(SpeedPositionCheck.class.getClassLoader());
        Phone phone=new Phone();Parent parent=new Parent();phone.addView(parent);
        ClockFrame frame=new ClockFrame();Clock clock=new Clock();Start start=new Start();phone.addView(start);start.addView(frame);frame.addView(clock);
        phone.ids.put(2,clock);phone.ids.put(3,frame);phone.ids.put(4,start);clock.setTextColor(0xffabcdef);
        Icon hint=new Icon("bluetooth"),wifi=new Icon("wifi"),mobile=new Icon("stacked_mobile");NetworkSpeedView speed=new NetworkSpeedView();
        speed.getResources().getDisplayMetrics().density=1f; // This compact allocation fixture uses pixels == dp; PJZ checks below explicitly use density 4.
        StatusIconContainer.StatusIconState speedState=new StatusIconContainer.StatusIconState();speed.tags.put(1,speedState);
        NetworkSpeedIconState value=new NetworkSpeedIconState();value.visible=true;value.speedText=42;speed.applyNetworkState(value);
        parent.addView(hint);parent.addView(speed);parent.addView(wifi);parent.addView(mobile);
        h.attached(speed);Object identity=speed.getParent();List<View> nativeChildren=Arrays.asList(hint,speed,wifi,mobile);
        equal(true,h.configure(settings("native")));layout(h,parent,false,true);equal(0,made[0]);equal(60f,state(wifi).getXTranslation());
        equal(true,h.configure(settings("cellular_left")));equal(null,h.begin(parent,true)); // Incomplete hook installation is native.
        h.setHooksReady(true);layout(h,parent,false,true);
        equal(null,h.begin(parent,true));equal(36f,speedState.getXTranslation()); // Allocation hook is mandatory too.
        h.setAllocationReady(true);layout(h,parent,false,true);
        equal(0f,h.drawOffsetXdp(speed,17f));equal(17f,h.drawOffsetXdp(new NetworkSpeedView(),17f));
        equal(36f,speedState.getXTranslation());equal(60f,state(wifi).getXTranslation());equal(80f,state(mobile).getXTranslation());
        equal(true,h.configure(settings("right_start",10f)));layout(h,parent,false,true);settled();layout(h,parent,false,false);
        equal(46f,speedState.getXTranslation());equal(40f,state(hint).getXTranslation());equal(60f,state(wifi).getXTranslation());
        h.beforeLayout(parent);
        try(SpeedPosition.Scope outer=h.begin(parent,true)){
            targets(h,parent,false);h.beforeApply(state(hint),hint);h.beforeApply(speedState,speed);
            for(int i=0;i<100;i++){h.beforeApply(speedState,speed);h.beforeApply(state(hint),hint);}
            equal(46f,speedState.getXTranslation());equal(40f,state(hint).getXTranslation());
            layout(h,parent,false,false); // Nested native update resets all targets before applying them.
            h.beforeApply(speedState,speed);h.beforeApply(state(hint),hint);
            equal(46f,speedState.getXTranslation());equal(40f,state(hint).getXTranslation());
        }
        float wifiOnSpeed=parent.getLeft()+speedState.getXTranslation();
        wifi.shown=false;layout(h,parent,false,false);equal(2,state(wifi).visibleState);equal(26f,speedState.getXTranslation());equal(40f,state(hint).getXTranslation());
        equal(wifiOnSpeed,parent.getLeft()+speedState.getXTranslation());equal(80,parent.getWidth());
        wifi.shown=true;layout(h,parent,false,false);equal(46f,speedState.getXTranslation());equal(40f,state(hint).getXTranslation());
        equal(0,speed.getPaddingEnd());equal(24,speed.getLayoutParams().width); // The virtual reservation is never real padding/size.
        int reads=parent.childReads;for(int i=0;i<1000;i++)layout(h,parent,false,false);
        equal(reads+parent.getChildCount()*3000,parent.childReads); // Only native measure/target/apply loops read children.
        for(int i=0;i<parent.getChildCount();i++)equal(nativeChildren.get(i),parent.getChildAt(i));equal(identity,speed.getParent());
        h.configure(settings("cellular_left"));layout(h,parent,true,true);equal(40f,speedState.getXTranslation());equal(0f,state(mobile).getXTranslation());
        wifi.shown=false;layout(h,parent,true,false);equal(40f,speedState.getXTranslation());wifi.shown=true;layout(h,parent,true,false);
        mobile.slot="other";h.changed(parent);layout(h,parent,false,true);equal(36f,speedState.getXTranslation());
        mobile.slot="stacked_mobile";h.changed(parent);layout(h,parent,false,true);equal(36f,speedState.getXTranslation());
        spacingAndOffsets(h,parent,speed,speedState,hint,wifi,mobile);
        h.configure(settings("clock_left"));equal(1,made[0]);equal(27,frame.l);equal(5,frame.r);equal(2,frame.getChildCount());
        NetworkSpeedView copy=(NetworkSpeedView)frame.getChildAt(1);equal(-1,copy.getId());equal("c17_network_speed_clock",copy.getSlot());
        equal(42L,copy.mState.speedText);equal(0xffabcdef,copy.tint);equal(identity,speed.getParent());equal(1f,copy.getAlpha());
        speed.setAlpha(0f);h.nativeUpdated(speed);equal(1f,copy.getAlpha()); // Source's native right mask is never copied.
        layout(h,parent,false,true);equal(2,speedState.visibleState);equal(false,h.iconBlocked(speed,false));
        value.speedText=123;speed.applyNetworkState(value);h.nativeUpdated(speed);equal(123L,copy.mState.speedText);
        clock.setTextColor(0xff112233);h.clockTintChanged(clock);equal(0xff112233,copy.tint);
        speed.setSlot("network_speed_alt");parent.mIgnoredSlots.add("network_speed_alt");h.slotChanged(speed);
        equal(View.GONE,copy.getVisibility());equal(3,frame.l);equal(5,frame.r);
        parent.mIgnoredSlots.clear();h.changed(parent);equal(View.VISIBLE,copy.getVisibility());equal(27,frame.l);
        value.visible=false;speed.applyNetworkState(value);h.nativeUpdated(speed);equal(View.GONE,copy.getVisibility());equal(3,frame.l);
        value.visible=true;speed.applyNetworkState(value);h.nativeUpdated(speed);equal(View.VISIBLE,copy.getVisibility());equal(27,frame.l);
        h.configure(settings("clock_right"));equal(copy,frame.getChildAt(1));equal(1,made[0]);equal(3,frame.l);equal(29,frame.r);
        for(String side:new String[]{"clock_left","clock_right"})for(float gap:new float[]{0f,-4f,7f}){
            Bundle adjacent=settings(side);adjacent.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);
            adjacent.putFloat(SpeedPosition.SAFE_GAP,gap);adjacent.putFloat("speed_offset_x",55f);
            h.configure(adjacent);FrameLayout.LayoutParams copyParams=(FrameLayout.LayoutParams)copy.getLayoutParams();
            if("clock_left".equals(side)){
                float copyRight=frame.l+copyParams.leftMargin+24f;close(gap,frame.l-copyRight); // the 24px host box edge
            }else{
                float clockEnd=frame.getMeasuredWidth()-frame.r;
                float copyLeft=frame.getMeasuredWidth()-frame.r-copy.getWidth()-copyParams.rightMargin;
                close(gap,copyLeft-clockEnd);
            }
            equal(0f,h.drawOffsetXdp(copy,55f));equal(identity,speed.getParent());
        }
        h.configure(settings("clock_right"));equal(3,frame.l);equal(29,frame.r);
        frame.setPadding(7,4,31,6);h.nativeUpdated(speed); // A new OEM inset replaces our previous padding, not a saved stale value.
        h.configure(settings("native"));equal(7,frame.l);equal(31,frame.r);equal(4,frame.t);equal(6,frame.b);equal(1,frame.getChildCount());
        layout(h,parent,false,true);equal(0,speedState.visibleState);equal(36f,speedState.getXTranslation());
        start.width=65;h.configure(settings("clock_left"));equal(1,frame.getChildCount());equal(1,made[0]);
        start.width=150;phone.dispatchLayoutChange(150,80,65,80);equal(2,frame.getChildCount());equal(2,made[0]);
        h.setHooksReady(false);equal(1,frame.getChildCount());equal(null,h.begin(parent,true));equal(false,h.iconBlocked(speed,false));
        h.setHooksReady(true);equal(2,frame.getChildCount());
        Bundle safe=settings("clock_left");safe.putBoolean(StatusBarSettings.SAFE_MODE,true);h.configure(safe);equal(1,frame.getChildCount());equal(null,h.begin(parent,true));
        Bundle positionOff=settings("clock_left");positionOff.putBoolean("speed_position_enabled",false);equal(true,h.configure(positionOff));
        equal(null,h.begin(parent,true));equal(false,h.handlesHorizontalOffset(speed));
        Bundle masterOff=settings("right_start");masterOff.putBoolean("speed_enabled",false);equal(true,h.configure(masterOff));
        equal(null,h.begin(parent,true));equal(false,h.configure(masterOff)); // repeated disabled configuration is inert
        h.configure(settings("clock_right"));equal(2,frame.getChildCount());h.detached(speed);equal(1,frame.getChildCount());equal(0,phone.layoutListeners.size());
        h.attached(speed);h.configure(settings("native"));h.configure(settings("clock_left"));equal(2,frame.getChildCount());
        h.releaseRuntime();equal(1,frame.getChildCount());equal(0,phone.layoutListeners.size());equal(null,h.begin(parent,true));equal(false,h.configure(settings("clock_left")));equal(false,h.iconBlocked(speed,false));
        pjzNativeAllocation();
        speedStaysWhereItIs();
        theAuthorsLayout();
        footprintIgnoresHistory();
        effectiveNetworkOrder();
        clockCopyIgnoresDigits();
        System.out.println("SpeedPositionCheck: "+checks+" checks passed");
    }
    /** The clock copy is placed from the host box: a changing number neither moves it nor asks for another layout. */
    private static void clockCopyIgnoresDigits()throws Exception{
        SpeedPosition h=new SpeedPosition((source,parent,layout)->new NetworkSpeedView());h.resolve(SpeedPositionCheck.class.getClassLoader());
        Phone phone=new Phone();Parent p=new Parent();phone.addView(p);
        ClockFrame frame=new ClockFrame();Clock clock=new Clock();Start start=new Start();phone.addView(start);start.addView(frame);frame.addView(clock);
        phone.ids.put(2,clock);phone.ids.put(3,frame);phone.ids.put(4,start);
        NetworkSpeedView speed=new NetworkSpeedView();StatusIconContainer.StatusIconState nativeState=new StatusIconContainer.StatusIconState();speed.tags.put(1,nativeState);
        speed.getResources().getDisplayMetrics().density=1f;speed.nativeLayout();
        NetworkSpeedIconState value=new NetworkSpeedIconState();value.visible=true;value.speedText=12;speed.applyNetworkState(value);
        int baseline=android.view.Gravity.CENTER_HORIZONTAL|android.view.Gravity.TOP;
        speed.mSpeedNumber.setGravity(baseline);speed.mSpeedUnit.setGravity(baseline);p.addView(speed);
        h.attached(speed);h.setHooksReady(true);h.setAllocationReady(true);NetworkSpeedControls controls=new NetworkSpeedControls();
        Bundle prefs=settings("clock_left");prefs.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);prefs.putFloat(SpeedPosition.SAFE_GAP,3f);
        controls.configure(prefs);start.width=150;h.configure(prefs);
        NetworkSpeedView copy=(NetworkSpeedView)frame.getChildAt(1);copy.nativeLayout();
        FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)copy.getLayoutParams();
        close(-(3f+speed.getLayoutParams().width),lp.leftMargin); // the gap plus the host box, whatever the digits are
        int requests=p.requests,frameRequests=frame.layoutRequests,margin=lp.leftMargin,copyBaseline=copy.mSpeedNumber.getGravity();
        for(String text:new String[]{"0","123456","9","0.00","77"}){
            speed.applyNetworkState(text,"KB/s");h.nativeUpdated(speed);
            equal(margin,lp.leftMargin);equal(requests,p.requests);equal(frameRequests,frame.layoutRequests);
            equal(baseline,speed.mSpeedNumber.getGravity());equal(copyBaseline,copy.mSpeedNumber.getGravity());
        }
        start.width=65;h.nativeUpdated(speed);equal(1,frame.getChildCount()); // no room: the native clock stays alone
        start.width=150;h.nativeUpdated(speed);equal(2,frame.getChildCount());
        prefs.putString(SpeedPosition.POSITION,"clock_right");h.configure(prefs);equal(baseline,speed.mSpeedNumber.getGravity());
        prefs.putBoolean("speed_enabled",false);h.configure(prefs);controls.configure(prefs);equal(baseline,speed.mSpeedNumber.getGravity());
        h.releaseRuntime();controls.configure(null);
    }
    /** Exact native passes in both hook nesting orders; model ordering, not registration order, owns the edge. */
    private static void orderedPass(SpeedPosition h,NetworkIconOrder order,Parent p,boolean targets,boolean speedFirst){
        SpeedPosition.Scope speedScope=null;NetworkIconOrder.IndexScope orderScope=null;
        try{
            if(speedFirst){speedScope=h.begin(p,targets);orderScope=order.begin(p);}
            else{orderScope=order.begin(p);speedScope=h.begin(p,targets);}
            if(targets){targets(h,p,p.rtl);for(int i=0;i<p.getChildCount();i++){View v=p.getChildAt(i);h.beforeApply(state(v),v);}}
            else{
                int width=p.extraWidth,count=0;
                for(int i=0;i<p.getChildCount();i++){
                    View v=p.getChildAt(i);StatusIconDisplayable icon=(StatusIconDisplayable)v;
                    if(h.iconBlocked(v,icon.isIconBlocked())||!icon.isIconVisible()||p.mIgnoredSlots.contains(icon.getSlot()))continue;
                    width+=v.getMeasuredWidth()+v.getPaddingStart()+h.allocationPaddingEnd(v,v.getPaddingEnd());count++;
                }
                p.width=width+Math.max(0,count-1)*p.mIconSpacing;
            }
        }finally{
            if(speedFirst){if(orderScope!=null)orderScope.close();if(speedScope!=null)speedScope.close();}
            else{if(speedScope!=null)speedScope.close();if(orderScope!=null)orderScope.close();}
        }
    }
    /**
     * The radios right of the speed come and go; the number must not follow them. Every on/off/blocked/ignored combination of
     * the two radios, in both hook nesting orders and with the order swap, leaves the speed where all-radios-shown put it.
     * RTL keeps the plain follow-the-neighbour placement.
     */
    private static void effectiveNetworkOrder()throws Exception{
        SpeedPosition h=new SpeedPosition();h.resolve(SpeedPositionCheck.class.getClassLoader());
        NetworkIconOrder order=new NetworkIconOrder();order.resolve(SpeedPositionCheck.class.getClassLoader());h.setOrderReader(order::orderedChildren);
        Phone phone=new Phone();Parent p=new Parent();phone.addView(p);p.ordering=order;p.extraWidth=80;
        Icon hint=new Icon("alarm_clock"),wifi=new Icon("wifi"),cell=new Icon("mobile"),tail=new Icon("bluetooth");
        wifi.width=40;cell.width=60;
        NetworkSpeedView speed=new NetworkSpeedView();StatusIconContainer.StatusIconState source=new StatusIconContainer.StatusIconState();speed.tags.put(1,source);
        speed.getResources().getDisplayMetrics().density=1f;wifi.getResources().getDisplayMetrics().density=1f;cell.getResources().getDisplayMetrics().density=1f;
        NetworkSpeedIconState model=new NetworkSpeedIconState();model.visible=true;model.speedText=12;speed.applyNetworkState(model);
        speed.nativeLayout();
        p.addView(hint);p.addView(wifi);p.addView(speed);p.addView(cell);p.addView(tail);
        h.attached(speed);h.setHooksReady(true);h.setAllocationReady(true);NetworkSpeedControls controls=new NetworkSpeedControls();
        Bundle prefs=settings("native");prefs.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);prefs.putFloat(SpeedPosition.SAFE_GAP,3f);prefs.putFloat(SpeedPosition.EDGE_RESERVE,100f);h.configure(prefs);
        int boxWidth=speed.getWidth();
        for(boolean rtl:new boolean[]{false,true})for(boolean speedFirst:new boolean[]{false,true})for(boolean swap:new boolean[]{false,true}){
            p.rtl=rtl;Bundle sorting=new Bundle();sorting.putBoolean(NetworkIconOrder.SWAP,swap);order.configure(sorting);
            // Visit every native model; the selected frame is fixed and no state is learned.
            for(int shown=0;shown<4;shown++){
                wifi.shown=(shown&1)!=0;cell.shown=(shown&2)!=0;wifi.blocked=cell.blocked=false;p.mIgnoredSlots.clear();
                orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);settled();
                orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);
            }
            wifi.shown=cell.shown=true;wifi.blocked=cell.blocked=false;p.mIgnoredSlots.clear();
            orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);settled();
            orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);
            float home=rtl?source.getXTranslation():p.getLeft()+source.getXTranslation(); // native RTL uses the parent's logical edge
            for(int shown=0;shown<4;shown++){
                wifi.shown=(shown&1)!=0;cell.shown=(shown&2)!=0;wifi.blocked=cell.blocked=false;
                p.mIgnoredSlots.clear();
                for(int unavailable=0;unavailable<3;unavailable++){
                    if(unavailable==1)cell.blocked=true;
                    if(unavailable==2){cell.blocked=false;p.mIgnoredSlots.add("wifi");}
                    List<View> actual=new ArrayList<>();
                    try(NetworkIconOrder.IndexScope nativeInput=order.begin(p)){for(int i=0;i<p.getChildCount();i++)actual.add(p.getChildAt(i));}
                    equal(actual,order.orderedChildren(p));ArrayList<View> eligible=new ArrayList<>();
                    for(View v:actual){StatusIconDisplayable icon=(StatusIconDisplayable)v;
                        if(icon.isIconVisible()&&!icon.isIconBlocked()&&!p.mIgnoredSlots.contains(icon.getSlot()))eligible.add(v);}
                    if(rtl)Collections.reverse(eligible);int at=eligible.indexOf(speed);
                    orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);
                    String where=" rtl="+rtl+" speedFirst="+speedFirst+" swap="+swap+" shown="+shown+" unavailable="+unavailable;
                    float placed=rtl?source.getXTranslation():p.getLeft()+source.getXTranslation();
                    if(Math.abs(home-placed)>.01f)throw new AssertionError("relative speed frame moved from "+home+" to "+placed+where);
                    equal(.0f,speed.getTranslationX());equal(0,speed.getPaddingEnd());equal(p,speed.getParent());
                    int requests=p.requests,queued=android.os.Handler.postedForCheck.size();float edge=rtl?source.getXTranslation():p.getLeft()+source.getXTranslation();
                    for(int repeat=0;repeat<8;repeat++){
                        h.nativeUpdated(speed);
                        orderedPass(h,order,p,false,speedFirst);orderedPass(h,order,p,true,speedFirst);
                        close(edge,rtl?source.getXTranslation():p.getLeft()+source.getXTranslation());equal(requests,p.requests);equal(queued,android.os.Handler.postedForCheck.size());
                    }
                }
            }
        }
        // Original registration can change without a speed binding recapture; read the new
        // model sequence rather than retaining the old nearest radio identity.
        p.rtl=false;wifi.shown=cell.shown=true;cell.blocked=false;p.mIgnoredSlots.clear();order.configure(new Bundle());
        p.removeView(tail);p.removeView(hint);p.addView(hint);order.changed(p);
        orderedPass(h,order,p,false,false);orderedPass(h,order,p,true,false);
        p.removeView(cell);order.changed(p); // Source becomes the tail of the active native model list.
        hint.shown=false;
        orderedPass(h,order,p,false,true);orderedPass(h,order,p,true,true);
        // No eligible non-radio tail: its virtual signal edge retains the selected 100px radio frame.
        close(100f+3f+boxWidth,p.getWidth()-source.getXTranslation());
        wifi.shown=false;
        h.releaseRuntime();controls.configure(null);order.releaseRuntime();
    }
    /** PJZ110 dump: density=4, speed=88(+9 native padding), mobile=80, Wi-Fi=88. */
    private static void pjzNativeAllocation()throws Exception{
        SpeedPosition h=new SpeedPosition();h.resolve(SpeedPositionCheck.class.getClassLoader());
        Phone phone=new Phone();Parent p=new Parent();p.extraWidth=0;phone.addView(p);
        NetworkSpeedView speed=new NetworkSpeedView();speed.applyNativeConfiguration(88,36,44);speed.nativeHeight=80;speed.nativePaddingEnd=9;
        speed.getResources().getDisplayMetrics().density=4f;
        StatusIconContainer.StatusIconState speedState=new StatusIconContainer.StatusIconState();speed.tags.put(1,speedState);
        NetworkSpeedIconState model=new NetworkSpeedIconState();model.visible=true;speed.applyNetworkState(model);
        Icon mobile=new Icon("stacked_mobile"),wifi=new Icon("wifi");mobile.width=80;wifi.width=88;
        p.addView(speed);p.addView(mobile);p.addView(wifi);h.attached(speed);h.setHooksReady(true);h.setAllocationReady(true);
        Bundle b=settings("native");b.putFloat("speed_offset_x",-3f);b.putBoolean("data_enabled",true);b.putFloat("data_offset_x",3f);b.putFloat(SpeedPosition.EDGE_RESERVE,42f);
        h.configure(b);layout(h,p,false,true);settled();layout(h,p,false,false);
        equal(265,p.getWidth());equal(-12f,speedState.getXTranslation());equal(97f,state(mobile).getXTranslation());equal(177f,state(wifi).getXTranslation());
        float sourceWorld=p.getLeft()+speedState.getXTranslation();
        // With safety disabled, free offsets never reserve width or rewrite sibling targets.
        b.putFloat("speed_offset_x",5f);h.configure(b);layout(h,p,false,true);settled();layout(h,p,false,false);
        equal(265,p.getWidth());equal(20f,speedState.getXTranslation());equal(97f,state(mobile).getXTranslation());equal(177f,state(wifi).getXTranslation());
        b.putFloat("speed_offset_x",-3f);h.configure(b);layout(h,p,false,true);settled();layout(h,p,false,false);
        for(int i=0;i<32;i++){
            wifi.shown=i%2!=0;layout(h,p,false,false);
            equal(265,p.getWidth());equal(sourceWorld,p.getLeft()+speedState.getXTranslation());
            // Remaining mobile follows its native end alignment, not a synthetic Wi-Fi ghost slot.
            equal(wifi.shown?97f:185f,state(mobile).getXTranslation());
        }
        // The custom 4G/5G slot can replace all 88px of hidden Wi-Fi space. It must
        // not receive another Wi-Fi reservation on top of its expanded native host.
        for(int i=0;i<16;i++){
            wifi.shown=i%2!=0;mobile.width=wifi.shown?80:168;layout(h,p,false,false);settled();layout(h,p,false,false);
            equal(265,p.getWidth());equal(sourceWorld,p.getLeft()+speedState.getXTranslation());
            equal(97f,state(mobile).getXTranslation());
        }
        wifi.shown=false;mobile.width=208;layout(h,p,false,false);equal(305,p.getWidth());
        // A genuinely wider native label is never squeezed down to the saved network budget.
        mobile.width=80;wifi.shown=true;layout(h,p,false,false);
        wifi.shown=true; speed.nativeLayout();
        b.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);b.putFloat(SpeedPosition.SAFE_GAP,2f);h.configure(b);
        // Virtual signed allocation stays out of inner OEM text measurement.
        try(SpeedPosition.Scope row=h.begin(p,false)){
            int allocated=h.allocationPaddingEnd(speed,9);
            try(SpeedPosition.ChildMeasureScope child=h.childMeasurement(p,speed)){
                equal(9,h.allocationPaddingEnd(speed,9));equal(null,h.childMeasurement(p,mobile));
                try(SpeedPosition.ChildMeasureScope nested=h.childMeasurement(p,speed)){equal(9,h.allocationPaddingEnd(speed,9));}
                equal(9,h.allocationPaddingEnd(speed,9));
            }
            equal(allocated,h.allocationPaddingEnd(speed,9));
        }
        equal(9,h.allocationPaddingEnd(speed,9));equal(88,speed.getLayoutParams().width);
        h.networkDrawable(true,88f,4f);h.networkDrawable(false,72f,4f);
        h.configureNetwork(-3f,150f,3f,200f);android.os.Handler.drainForCheck();layout(h,p,false,true);
        // The anchor uses the fixed 20dp frame: density 4, scale 200%, offset 3dp => left -28px.
        // The current 72px delegate still paints [-20,124], independently of this frame anchor.
        close(8f,state(mobile).getXTranslation()-28f-(speedState.getXTranslation()+88f));
        equal(88,speed.getLayoutParams().width);equal(9,speed.getPaddingEnd());
        // Changing/deleting drawables on a background resource thread caches numbers only.
        // A burst queues one main-thread invalidation and cannot touch Views from that thread.
        android.os.Handler.drainForCheck();int requests=p.requests,reads=p.childReads,queued=android.os.Handler.postedForCheck.size();
        Thread resourceThread=new Thread(()->{
            android.os.Looper.setCurrentForCheck(null);
            for(int i=0;i<80;i++)h.networkDrawable(false,76f+i%4,4f);
            android.os.Looper.resetCurrentForCheck();
        });resourceThread.start();resourceThread.join();
        equal(requests,p.requests);equal(reads,p.childReads);equal(queued+1,android.os.Handler.postedForCheck.size());
        equal(false,h.networkDrawable(false,Float.NaN,4f));equal(false,h.networkDrawable(true,-3f,4f));
        android.os.Handler.drainForCheck();equal(true,p.requests>requests);
        // Disabling restores the saved free offset; enabling never applies it as fallback.
        b.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,false);h.configure(b);layout(h,p,false,true);
        equal(265,p.getWidth());equal(-12f,speedState.getXTranslation());equal(97f,state(mobile).getXTranslation());
        b.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);h.configure(b);layout(h,p,false,true);
        equal(0f,h.drawOffsetXdp(new NetworkSpeedView(),-3f));
        wifi.shown=true;mobile.width=80;
        b.putBoolean(StatusBarSettings.SAFE_MODE,true);h.configure(b);layout(h,p,false,true);
        equal(265,p.getWidth());equal(0f,speedState.getXTranslation());equal(9,speed.getPaddingEnd());
        h.releaseRuntime();equal(null,h.childMeasurement(p,speed));
        speed.getResources().getDisplayMetrics().density=1f;
    }
    private static void close(float expected,float actual){checks++;if(Math.abs(expected-actual)>.01f)throw new AssertionError("SpeedPosition "+checks+": "+expected+" != "+actual);}
    /**
     * The layout reported on the PJZ110: speed, stacked mobile, Wi-Fi; position master off, negative gap on. Switching Wi-Fi
     * and mobile data in any order, any number of times, never moves the number, whatever the radios were doing at start.
     */
    /** The width kept at the edge for the radios (dp): the widest state of the reported layout, 197 px at density 4. */
    private static final float RESERVE=49.25f;
    private static final class Rig {
        final SpeedPosition h=new SpeedPosition();final Phone phone=new Phone();final Parent p=new Parent();
        final NetworkSpeedView speed=new NetworkSpeedView();final StatusIconContainer.StatusIconState speedState=new StatusIconContainer.StatusIconState();
        final Icon mobile=new Icon("stacked_mobile"),wifi=new Icon("wifi");
        final Bundle b;
        Rig(float gap,float reserve)throws Exception{
            h.resolve(SpeedPositionCheck.class.getClassLoader());
            p.extraWidth=0;phone.addView(p);speed.applyNativeConfiguration(88,36,44);speed.nativeHeight=80;speed.nativePaddingEnd=9;
            speed.getResources().getDisplayMetrics().density=4f;speed.tags.put(1,speedState);
            NetworkSpeedIconState model=new NetworkSpeedIconState();model.visible=true;speed.applyNetworkState(model);
            mobile.width=80;wifi.width=88;p.addView(speed);p.addView(mobile);p.addView(wifi);
            h.attached(speed);h.setHooksReady(true);h.setAllocationReady(true);
            b=settings("native");b.putBoolean("speed_position_enabled",false);b.putFloat("speed_offset_x",10f);
            b.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);b.putFloat(SpeedPosition.SAFE_GAP,gap);b.putFloat(SpeedPosition.EDGE_RESERVE,reserve);h.configure(b);
        }
        void state(boolean wifiOn,int mobileWidth,boolean fresh)throws Exception{
            wifi.shown=wifiOn;mobile.shown=true;mobile.width=mobileWidth;layout(h,p,false,fresh);layout(h,p,false,false);
        }
        float x(){return p.getLeft()+speedState.getXTranslation();}
        void close(){h.releaseRuntime();speed.getResources().getDisplayMetrics().density=1f;}
    }
    /**
     * The layout reported on the PJZ110: speed, stacked mobile, Wi-Fi; position master off, negative gap on. The number sits at one place
     * measured from the edge: switching Wi-Fi and mobile data in any order, any number of times, from any first state, with any gap, never
     * moves it. Nothing is remembered, so there is no first state to get wrong.
     */
    private static void theAuthorsLayout()throws Exception{
        for(int start=0;start<4;start++){
            Rig rig=new Rig(start==0?-4.27f:start==1?-9f:3f,RESERVE);
            if(start==3)rig.h.configureNetwork(0f,150f,0f,100f);
            boolean wifiFirst=start!=2;
            rig.state(wifiFirst,wifiFirst?80:197,true);float begin=rig.x();
            for(int i=0;i<12;i++){boolean wifiOn=(i&1)==0?!wifiFirst:wifiFirst;rig.state(wifiOn,wifiOn?80:197,false);close(begin,rig.x());}
            rig.close();
        }
        // Every combination, including a missing mobile and no radio at all, and the native label animations passing through wider moments.
        Rig rig=new Rig(-4.27f,RESERVE);
        rig.state(false,197,true);float home=rig.x();
        int requests=rig.p.requests;
        for(int i=0;i<64;i++){
            rig.wifi.shown=(i*5&1)==0;rig.mobile.shown=(i*3&2)==0||i%7==0;rig.mobile.width=rig.wifi.shown?80:197;
            layout(rig.h,rig.p,false,false);layout(rig.h,rig.p,false,false);close(home,rig.x());
        }
        rig.wifi.shown=true;rig.mobile.shown=true;rig.mobile.width=197;layout(rig.h,rig.p,false,false);rig.mobile.width=104;layout(rig.h,rig.p,false,false);
        rig.mobile.width=80;layout(rig.h,rig.p,false,false);close(home,rig.x());
        rig.close();
        // The reserve is the one number: more of it moves the number left by exactly the difference; none follows the radios.
        Rig a=new Rig(-4.27f,RESERVE);
        a.state(true,80,true);float base=a.x();a.close();
        Rig more=new Rig(-4.27f,RESERVE+5f); // the selection is shared by every SpeedPosition: one rig at a time
        more.state(true,80,true);close(5f*4f,base-more.x());
        more.close();
        Rig follows=new Rig(-4.27f,0f);
        follows.state(true,80,true);float withWifi=follows.x();follows.state(false,197,false);
        close(withWifi,follows.x()); // zero reserve is still a fixed relative budget
        follows.close();
        // Wider native radios keep their own allocation; they do not override the selected relative position.
        Rig wide=new Rig(-4.27f,RESERVE);
        wide.state(false,197,true);float full=wide.x();
        wide.state(true,197,false);
        close(full,wide.x());equal(197,wide.mobile.width);equal(88,wide.wifi.width);equal(88,wide.speed.getLayoutParams().width);
        wide.close();
        // A painting setting changes where the edge of the icon is, not the width of the row: afterwards the number is again the same
        // in every state.
        Rig painted=new Rig(-9f,RESERVE);
        painted.state(true,80,true);painted.h.configureNetwork(0f,150f,0f,100f);painted.state(true,80,true);
        float after=painted.x();
        for(int i=0;i<8;i++){boolean wifiOn=(i&1)==0;painted.state(wifiOn,wifiOn?80:197,false);close(after,painted.x());}
        painted.close();
        equal(true,requests>=0);
    }
    private static void footprintIgnoresHistory()throws Exception{
        theAuthorsLayoutIsIndependentOfHistory();
    }
    /** What the number does cannot depend on the order of states, on settings changed on the way, or on a restart. */
    private static void theAuthorsLayoutIsIndependentOfHistory()throws Exception{
        float[] places=new float[4];
        for(int order=0;order<4;order++){
            Rig rig=new Rig(-4.27f,RESERVE);
            if(order==1)rig.h.configureNetwork(0f,100f,0f,100f); // the painting settings in their default state again
            boolean[] wifi=order<2?new boolean[]{true,false,true,false}:new boolean[]{false,true,false,true};
            for(boolean on:wifi)rig.state(on,on?80:197,order%2==0);
            places[order]=rig.x();
            rig.h.detached(rig.speed);rig.h.attached(rig.speed);rig.h.configure(rig.b);rig.state(true,80,true);
            close(places[order],rig.x());
            rig.close();
        }
        for(int order=1;order<4;order++)close(places[0],places[order]);
    }
    /** Real native setter/layout order and sampled strings, rather than repeating one frozen geometry. */
    private static void speedStaysWhereItIs()throws Exception{
        SpeedPosition h=new SpeedPosition();h.resolve(SpeedPositionCheck.class.getClassLoader());
        Phone phone=new Phone();Parent p=new Parent();phone.addView(p);p.extraWidth=0;
        NetworkSpeedView speed=new NetworkSpeedView();speed.applyNativeConfiguration(88,36,44);speed.nativePaddingEnd=9;speed.nativeHeight=80;
        speed.getResources().getDisplayMetrics().density=4f;
        StatusIconContainer.StatusIconState nativeState=new StatusIconContainer.StatusIconState();speed.tags.put(1,nativeState);
        NetworkSpeedIconState model=new NetworkSpeedIconState();model.visible=true;speed.applyNetworkState(model);
        Icon neighbour=new Icon("alarm_clock"),wifi=new Icon("wifi"),cell=new Icon("stacked_mobile");neighbour.width=20;wifi.width=88;cell.width=80;
        p.addView(speed);p.addView(neighbour);p.addView(cell);p.addView(wifi);h.attached(speed);h.setHooksReady(true);h.setAllocationReady(true);
        NetworkSpeedControls controls=new NetworkSpeedControls();Bundle settings=settings("native");settings.putFloat("speed_offset_x",10f);
        settings.putFloat(SpeedPosition.EDGE_RESERVE,42f);
        settings.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);settings.putFloat(SpeedPosition.SAFE_GAP,3.43f);h.configure(settings);
        int centred=android.view.Gravity.CENTER_HORIZONTAL;speed.mSpeedNumber.setGravity(centred);speed.mSpeedUnit.setGravity(centred);
        for(boolean rtl:new boolean[]{false,true}){
            p.rtl=rtl;h.configure(settings);speed.nativeLayout();layout(h,p,rtl,true);settled();layout(h,p,rtl,false);
            float edge=rtl?nativeState.getXTranslation():nativeState.getXTranslation()+88f;
            int width=p.width,requests=p.requests,queued=android.os.Handler.postedForCheck.size();
            float neighbourX=state(neighbour).getXTranslation();
            for(String number:new String[]{"0","0.0","99.9","1024","1","0","0.00","88","99999999999999999999"}){
                speed.applyNetworkState(number,"KB/s");h.nativeUpdated(speed);
                speed.mSpeedNumber.measure(88,80);speed.mSpeedUnit.measure(88,80);speed.nativeLayout();
                layout(h,p,rtl,false);
                // The box edge is the reference, so no digit string moves it, resizes the row or asks for a pass.
                close(edge,rtl?nativeState.getXTranslation():nativeState.getXTranslation()+88f);
                equal(width,p.width);close(neighbourX,state(neighbour).getXTranslation());
                equal(requests,p.requests);equal(queued,android.os.Handler.postedForCheck.size());
                equal(centred,speed.mSpeedNumber.getGravity());equal(centred,speed.mSpeedUnit.getGravity());
            }
            // Radio model switches never move the number. The always-visible individual keeps
            // its native place, so the room of a radio that is off stays as a hole beside it.
            float home=rtl?nativeState.getXTranslation():p.getLeft()+nativeState.getXTranslation();
            for(int i=0;i<16;i++){
                wifi.shown=(i&1)==0;cell.shown=(i&2)==0;layout(h,p,rtl,false);settled();layout(h,p,rtl,false);
                close(home,rtl?nativeState.getXTranslation():p.getLeft()+nativeState.getXTranslation());
            }
            // No frame/alpha/appearance value is an ownership signal; hold the same side
            // through native DOT/HIDDEN then ICON, without forcing any state or View alpha.
            wifi.shown=cell.shown=true;speed.setAlpha(.37f);neighbour.setAlpha(.12f);layout(h,p,rtl,true);
            for(int transition:new int[]{1,2,0})try(SpeedPosition.Scope scope=h.begin(p,true)){
                targets(h,p,rtl);nativeState.visibleState=transition;float original=nativeState.getXTranslation();
                h.beforeApply(nativeState,speed);equal(transition,nativeState.visibleState);
                if(transition!=0)close(original,nativeState.getXTranslation());
                equal(.37f,speed.getAlpha());equal(.12f,neighbour.getAlpha());
            }
            for(int transition:new int[]{1,2,0})try(SpeedPosition.Scope scope=h.begin(p,true)){
                targets(h,p,rtl);state(neighbour).visibleState=transition;
                float anchor=state(neighbour).getXTranslation();h.beforeApply(nativeState,speed);
                if(!rtl)close(3.43f*4f,anchor-nativeState.getXTranslation()-88f);
                else close(3.43f*4f,nativeState.getXTranslation()-anchor-neighbour.width);
                equal(transition,state(neighbour).visibleState);
            }
            // Signed overlap can cross the original target coordinate. The IconManager
            // child individual still owns the same side instead of sorting into a new one.
            for(float gap:new float[]{-20f,0f,12f}){
                settings.putFloat(SpeedPosition.SAFE_GAP,gap);h.configure(settings);layout(h,p,rtl,true);settled();layout(h,p,rtl,false);
                if(!rtl)close(gap*4f,state(neighbour).getXTranslation()-nativeState.getXTranslation()-88f);
                else close(gap*4f,nativeState.getXTranslation()-state(neighbour).getXTranslation()-neighbour.width);
                equal(neighbour,p.getChildAt(1));equal(p,speed.getParent());equal(9,speed.getPaddingEnd());
            }
            settings.putFloat(SpeedPosition.SAFE_GAP,3.43f);
            for(float scale:new float[]{75f,125f,200f}){
                settings.putFloat("speed_scale",scale);h.configure(settings);layout(h,p,rtl,true);settled();layout(h,p,rtl,false);
                float localLeft=44f*(1f-scale/100f),localRight=44f*(1f+scale/100f);
                if(!rtl)close(13.72f,state(neighbour).getXTranslation()-nativeState.getXTranslation()-localRight);
                else close(13.72f,nativeState.getXTranslation()+localLeft-state(neighbour).getXTranslation()-neighbour.width);
            }
            settings.putFloat("speed_scale",100f);
            settings.putFloat(SpeedPosition.SAFE_GAP,3.43f);
        }
        settings.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,false);h.configure(settings);controls.configure(settings);layout(h,p,false,true);
        close(40f,nativeState.getXTranslation()); // Saved 10dp horizontal offset returns only after gap is disabled.
        controls.configure(null);h.releaseRuntime();speed.getResources().getDisplayMetrics().density=1f;
    }
    private static void spacingAndOffsets(SpeedPosition h,Parent parent,NetworkSpeedView speed,
            StatusIconContainer.StatusIconState speedState,Icon hint,Icon wifi,Icon mobile)throws Exception{
        equal(false,SpeedPosition.booleanDefaults().get(SpeedPosition.SAFE_GAP_ENABLED));equal(2f,SpeedPosition.numberDefaults().get(SpeedPosition.SAFE_GAP));equal(49f,SpeedPosition.numberDefaults().get(SpeedPosition.EDGE_RESERVE));
        speed.getResources().getDisplayMetrics().density=1f;parent.mIconSpacing=3;
        NetworkSpeedControls controls=new NetworkSpeedControls();
        for(boolean rtl:new boolean[]{false,true})for(int radios=0;radios<4;radios++)for(float gap:new float[]{0f,-4f,7f}){
            wifi.shown=(radios&1)!=0;mobile.shown=(radios&2)!=0;parent.rtl=rtl;
            Bundle s=settings("cellular_left");s.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);
            s.putFloat(SpeedPosition.SAFE_GAP,gap);s.putFloat("speed_offset_x",37f);
            h.configure(s);speed.nativeLayout();layout(h,parent,rtl,true);
            // The 40px radio frame stays selected when radios vanish. mIconSpacing is 3px;
            // the gap is measured against its virtual edge rather than the currently visible individual.
            if(rtl)close(gap,speedState.getXTranslation()-37f);
            else close(gap,parent.getWidth()-61f-speedState.getXTranslation());
            float target=speedState.getXTranslation();int width=parent.width,requests=parent.requests;
            s.putFloat("speed_offset_x",-91f);equal(false,h.configure(s));layout(h,parent,rtl,false);
            close(target,speedState.getXTranslation());equal(width,parent.width);
            for(int i=0;i<24;i++)layout(h,parent,rtl,false);
            equal(requests,parent.requests);equal(0f,h.drawOffsetXdp(speed,-91f));
            equal(0,speed.getPaddingEnd());equal(24,speed.getLayoutParams().width);equal(1f,speed.getAlpha());
        }
        // Even a visible non-network icon is an individual neighbour; radio changes cannot
        // move speed past that neighbour to the opposite side of a distant cellular slot.
        controls.configure(null);parent.mIconSpacing=0;wifi.shown=true;mobile.shown=true;
        h.configure(settings("cellular_left"));layout(h,parent,false,true);
        close(36f,speedState.getXTranslation());close(60f,state(wifi).getXTranslation());
        // Position master may be off while the independent adjacent gap remains active.
        Bundle s=settings("native");s.putBoolean("speed_position_enabled",false);s.putBoolean(SpeedPosition.SAFE_GAP_ENABLED,true);s.putFloat(SpeedPosition.SAFE_GAP,-3f);
        h.configure(s);
        layout(h,parent,false,true);close(-3f,state(wifi).getXTranslation()-speedState.getXTranslation()-24f);
        NetworkSpeedIconState empty=speed.mState.copy();empty.visible=false;speed.applyNetworkState(empty);
        layout(h,parent,false,true);equal(2,speedState.visibleState);equal(1f,speed.getAlpha());
        empty.visible=true;speed.applyNetworkState(empty);layout(h,parent,false,true);
        try(SpeedPosition.Scope scope=h.begin(parent,true)){
            targets(h,parent,false);speedState.visibleState=1;float dot=speedState.getXTranslation();
            h.beforeApply(speedState,speed);equal(1,speedState.visibleState);equal(dot,speedState.getXTranslation());
        }
        empty.speedText=42;speed.applyNetworkState(empty);
        controls.configure(null);parent.mIconSpacing=0;h.configure(settings("cellular_left"));layout(h,parent,false,true);
    }
}
