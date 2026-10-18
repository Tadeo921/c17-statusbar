package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Same-app mini stack against fake ColorOS 17 classes with the reference contract's exact member shapes:
 * geometry, contract resolution, eligibility, closed endpoint, draw folding, count badge, click hand-off,
 * native hand-back on expansion, and full restoration when switched off. No hook framework is involved. */
public final class NotificationMiniStackCheck {
    private static int checks;
    private static void equal(Object expected,Object actual,String what){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Mini stack "+checks+" "+what+": "+expected+" != "+actual);}
    private static void near(float expected,float actual,String what){checks++;if(Math.abs(expected-actual)>.001f)throw new AssertionError("Mini stack "+checks+" "+what+": "+expected+" != "+actual);}
    private static void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError("Mini stack "+checks+" "+what);}

    // ---- Fake ColorOS 17 SystemUI, member names and types as in C17DeckTargets (17.99.02). ----
    public interface Source { static Source from(String name){return new Source(){@Override public String toString(){return name;}};} }
    public interface Menu { boolean isMenuVisible(); }
    public static final class Stack extends ViewGroup { Object mAmbientState=new Object(); Stack(){super(null);} }
    public static final class Overlay { boolean isGroupExpandAnimating,isUserLocked; Object overlayContainer; ViewGroup opusCollapsedGroupHeader; }
    public static final class GroupExtension { TextView numberView; ViewGroup expandedPill; }
    public static final class ContainerExtension { GroupExtension oplusNotificationGroupExtImpl=new GroupExtension(); }
    public static class Container extends ViewGroup {
        Row mContainingNotification; Object mOverlayEx; boolean mIsUserSwipingToExpandRow; Path mChildClipPath=new Path();
        int nativeHeight=300,drawn; Path clipDuringDraw; View lastDrawn;
        TextView mOverflowNumber; ContainerExtension mExt=new ContainerExtension();
        final List<View> transients=new ArrayList<>();
        public int getTransientViewCount(){return transients.size();}
        public View getTransientView(int index){return transients.get(index);}
        Container(){super(null);}
        public int getIntrinsicHeight(){return nativeHeight;}
        public int getCollapsedHeight(){return nativeHeight-10;}
        public int getMinHeight(){return nativeHeight-20;}
        protected boolean drawChild(Canvas canvas,View child,long time){drawn++;clipDuringDraw=mChildClipPath;lastDrawn=child;return true;}
        public void setUserSwipingToExpandRow(boolean value){mIsUserSwipingToExpandRow=value;}
    }
    /** Lacks the C17 swipe state: must be treated as unsupported. */
    public static class OldContainer extends ViewGroup { Object mOverlayEx; OldContainer(){super(null);} }
    public static final class Extension {
        boolean isChildrenExpandedAnimating,encrypted; float groupExpansionProgress,groupExpandContentAlpha=1f; View view;
        public boolean getShouldEncrypted(){return encrypted;}
        public void setChildrenExpandedAnimating(boolean value){isChildrenExpandedAnimating=value;}
        public View getView(){return view;}
    }
    public static final class Contracted extends ViewGroup {
        final View icon=new View(null);int measured=80;
        Contracted(){super(null);addView(icon);}
        @Override public View findViewById(int id){return id==android.R.id.icon?icon:null;}
        @Override public int getMeasuredHeight(){return measured;}
    }
    public static class Content extends ViewGroup {
        View mContractedChild; boolean mIsChildInGroup=true; int mVisibleType=3; Row mContainingNotification; int contracted=120,selects;
        Content(){super(null);}
        public int getMinHeight(boolean likeGroupExpanded){return contracted;}
        public int calculateVisibleType(){return 3;}
        public int getVisualTypeForHeight(float height){return 3;}
        public void setIsChildInGroup(boolean value){mIsChildInGroup=value;}
        public void selectLayout(boolean animate,boolean force){selects++;mVisibleType=mIsChildInGroup?3:0;}
    }
    public static final class ScaleState { float x=1f,y=1f;public float getScaleX(){return x;}public float getScaleY(){return y;} }
    public static final class RowAnimation {
        final ScaleState scale=new ScaleState();Object requested;
        public ScaleState getRowScale(Object key){requested=key;return scale;}
    }
    public static final class StateEx { float contentAlpha=-1f;final RowAnimation animation=new RowAnimation();
        public void setGroupExpandContentAlpha(float value){contentAlpha=value;}
        public RowAnimation getRowAnimationStateEx(){return animation;}
    }
    public static final class MissingScaleStateEx { public void setGroupExpandContentAlpha(float value){ } }
    public static final class State {
        int height=-1; Object mExpandableViewStateEx=new StateEx(); boolean hidden; int clipTopAmount=5,clipBottomAmount=5; float y=-1f,alpha=-1f;
        public void setYTranslation(float value,String source){y=value;}
        public void setAlpha(float value,String source){alpha=value;}
    }
    static NotificationMiniStack replayTarget;
    private static final int COVERED_PROGRESS_ID=4811;
    private static final class CoveredResources extends android.content.res.Resources {
        boolean missing,fail;int lookups;
        @Override public int getIdentifier(String name,String type,String pkg){
            lookups++;
            if(fail)throw new NotFoundException("native progress resource unavailable");
            return !missing&&"stacked_notification_covered_curr_progress".equals(name)&&"id".equals(type)
                    &&"com.android.systemui".equals(pkg)?COVERED_PROGRESS_ID:0;
        }
    }
    public static class Row extends ViewGroup {
        boolean mIsSummaryWithChildren,mIsBundle,mChildrenExpanded,mGroupExpansionChanging,mIsUserSwipingToExpandRow,mIsHeadsUp,mExpandAnimationRunning,mIsMinimized;
        Container mChildrenContainer; Row mNotificationParent; Object mRowEx; Content mPrivateLayout,mPublicLayout; Object mBackgroundNormal,mExpandClickListener;
        Menu mMenuRow; int mClipTopAmount,mClipBottomAmount;
        boolean publicShown,guts; int actualHeight=200,backgroundUpdates,roundness,resets,childUpdates; float radius=28f;
        Row parentDuringBackground=this; final List<Row> members=new ArrayList<>(); final State state=new State();
        CoveredResources coveredResources;Object coveredProgress;int coveredReads;
        Row(){super(null);}
        @Override public android.content.res.Resources getResources(){return coveredResources==null?super.getResources():coveredResources;}
        @Override public Object getTag(int key){if(key==COVERED_PROGRESS_ID){coveredReads++;return coveredProgress;}return super.getTag(key);}
        public List<Row> getAttachedChildren(){return members;}
        public Content getShowingLayout(){return mPrivateLayout;}
        public boolean shouldShowPublic(){return publicShown;}
        public boolean areGutsExposed(){return guts;}
        public int getActualHeight(){return actualHeight;}
        public float getMaxRadius(){return radius;}
        public void updateBackgroundForGroupState(){backgroundUpdates++;parentDuringBackground=mNotificationParent;}
        /** Calls the module like the installed after-hook would, to prove the hand-back replay is ignored. */
        public void updateChildrenStates(){childUpdates++;if(replayTarget!=null)replayTarget.childrenUpdated(this);}
        protected void dispatchDraw(Canvas canvas){ }
        public void setChildrenExpanded(boolean value){mChildrenExpanded=value;}
        public void setGroupExpansionChanging(boolean value){mGroupExpansionChanging=value;}
        public void setUserSwipingToExpandRow(boolean value){mIsUserSwipingToExpandRow=value;}
        public State getViewState(){return state;}
        public boolean requestRoundness(float top,float bottom,Source source,boolean animate){roundness++;return true;}
        public void requestRoundnessReset(Source source,boolean animate){resets++;}
    }
    public static final class Clicker { public void onClick(View view){ } }

    private static NotificationMiniStack.Targets targets()throws Exception{
        return new NotificationMiniStack.Targets(Row.class,Container.class,Extension.class,Overlay.class,Stack.class,StateEx.class,Source.class,Clicker.class,ContainerExtension.class,GroupExtension.class);
    }
    private static Content content(Row owner){
        Content content=new Content();content.mContainingNotification=owner;content.mContractedChild=new Contracted();content.addView(content.mContractedChild);return content;
    }
    private static final class Group {
        final Stack stack=new Stack();final Row summary=new Row();final Container container=new Container();final Overlay overlay=new Overlay();
        final Extension extension=new Extension();final View header=new View(null);final List<Row> children=new ArrayList<>();int expandClicks;
        Group(int count){
            summary.mIsSummaryWithChildren=true;stack.addView(summary);summary.addView(container);
            summary.mChildrenContainer=container;container.mContainingNotification=summary;container.mOverlayEx=overlay;
            extension.view=summary;summary.mRowEx=extension;summary.mPrivateLayout=content(summary);summary.mPublicLayout=summary.mPrivateLayout;
            summary.mExpandClickListener=(View.OnClickListener)view->{if(view==summary)expandClicks++;};
            container.addView(header);
            for(int index=0;index<count;index++) {
                Row child=new Row();child.mNotificationParent=summary;Extension own=new Extension();own.view=child;child.mRowEx=own;
                child.mPrivateLayout=content(child);child.mPublicLayout=content(child);child.addView(child.mPrivateLayout);
                container.addView(child);summary.members.add(child);children.add(child);
            }
        }
    }
    private static Bundle settings(boolean on,boolean hideBadge){Bundle result=new Bundle();result.putBoolean(NotificationMiniStack.ENABLED,on);result.putBoolean(NotificationMiniStack.HIDE_BADGE,hideBadge);return result;}
    private static NotificationMiniStack ready(boolean hideBadge)throws Exception{
        NotificationMiniStack mini=new NotificationMiniStack(new Handler(null));
        mini.bind(targets());mini.hooksReady(true);mini.configure(settings(true,hideBadge));Handler.drainForCheck();
        check(mini.active(),"active after bind, ready and on");return mini;
    }
    private static Object height(NotificationMiniStack mini,Container container,boolean intrinsic)throws Throwable{
        return mini.height(container,intrinsic,()->intrinsic?container.getIntrinsicHeight():container.getCollapsedHeight());
    }

    private static void geometry(){
        equal(1,NotificationMiniStack.depth(2),"depth 2");equal(2,NotificationMiniStack.depth(3),"depth 3");equal(2,NotificationMiniStack.depth(5),"depth 5");
        equal(0,NotificationMiniStack.depth(1),"depth 1");
        near(1f,NotificationMiniStack.scale(0),"front scale");near(.92f,NotificationMiniStack.scale(1),"first rear");near(.84f,NotificationMiniStack.scale(2),"second rear");
        equal(4,NotificationMiniStack.tail(2,1f),"2 cards tail");equal(8,NotificationMiniStack.tail(3,1f),"3 cards tail");equal(8,NotificationMiniStack.tail(5,1f),"5 cards tail");
        equal(24,NotificationMiniStack.tail(5,3f),"tail density");equal(0,NotificationMiniStack.tail(5,Float.NaN),"NaN density");equal(0,NotificationMiniStack.tail(5,-1f),"negative density");
        equal(128,NotificationMiniStack.stackHeight(120,5,1f),"stack height");equal(-1,NotificationMiniStack.stackHeight(0,5,1f),"unmeasured front");
        equal(-1,NotificationMiniStack.stackHeight(Integer.MAX_VALUE-2,5,1f),"overflow");
        near(4f,NotificationMiniStack.foldedY(120,120,1,4f),"rear 1 y");near(8f,NotificationMiniStack.foldedY(120,120,2,4f),"rear 2 y");
        near(68f,NotificationMiniStack.foldedY(120,60,1,8f),"short rear y");near(8f,NotificationMiniStack.foldedY(120,120,9,4f),"layer clamp");
        RectF front=new RectF(10f,20f,110f,120f),out=new RectF();
        NotificationMiniStack.backing(front,1,2,8f,out);near(14f,out.left,"rear 1 left");near(106f,out.right,"rear 1 right");near(124f,out.bottom,"rear 1 bottom");near(32f,out.top,"rear 1 top");
        NotificationMiniStack.backing(front,2,2,8f,out);near(18f,out.left,"rear 2 left");near(128f,out.bottom,"rear 2 bottom");near(44f,out.top,"rear 2 top");
        NotificationMiniStack.backing(front,1,1,4f,out);near(124f,out.bottom,"two-card bottom");
        near(1f,NotificationMiniStack.opacity(0),"front alpha");near(.55f,NotificationMiniStack.opacity(1),"rear 1 alpha");near(.30f,NotificationMiniStack.opacity(2),"rear 2 alpha");near(0f,NotificationMiniStack.opacity(3),"hidden alpha");
        equal("2",NotificationMiniStack.badge(2),"badge");equal("99",NotificationMiniStack.badge(99),"badge 99");equal("99+",NotificationMiniStack.badge(100),"badge 100");
        near(5f,NotificationMiniStack.blend(5f,9f,0f),"closed blend");near(9f,NotificationMiniStack.blend(5f,9f,1f),"live blend");
    }

    private static void contract()throws Exception{
        check(NotificationMiniStack.supports(Row.class,Container.class),"C17 contract");
        check(!NotificationMiniStack.supports(Row.class,OldContainer.class),"C16 container refused");
        check(!NotificationMiniStack.supports(new ClassLoader(null){}),"no SystemUI classes");
        NotificationMiniStack.Targets t=targets();
        equal(Row.class,t.rowDraw.getDeclaringClass(),"own dispatchDraw");equal(Container.class,t.drawChild.getDeclaringClass(),"own drawChild");
        // A class without its own dispatchDraw resolves to the framework ViewGroup: that must never be hooked.
        check(NotificationMiniStack.Targets.framework(NotificationMiniStack.Targets.method(Stack.class,"dispatchDraw",Void.TYPE,Canvas.class).getDeclaringClass()),"framework fallback detected");
        try{NotificationMiniStack.Targets.field(Row.class,"mClipTopAmount",Boolean.TYPE);check(false,"wrong field type accepted");}catch(NoSuchFieldException expected){check(true,"wrong field type");}
        try{NotificationMiniStack.Targets.method(Row.class,"getActualHeight",Float.TYPE);check(false,"wrong return type accepted");}catch(NoSuchMethodException expected){check(true,"wrong return type");}
        try{NotificationMiniStack.Targets.method(Row.class,"missing",null);check(false,"missing method accepted");}catch(NoSuchMethodException expected){check(true,"missing method");}
        Object source=NotificationMiniStack.sourceType(Source.class,"C17-mini-stack");
        check(source instanceof Source,"source type");equal("C17-mini-stack",source.toString(),"source name");
    }

    private static void inactive()throws Throwable{
        Group group=new Group(5);
        NotificationMiniStack off=new NotificationMiniStack(new Handler(null));
        equal(300,height(off,group.container,true),"unbound intrinsic native");
        off.bind(targets());off.hooksReady(true);
        equal(290,height(off,group.container,false),"switched off: native");
        off.configure(settings(true,false));off.hooksReady(false);Handler.drainForCheck();
        check(!off.active(),"partial install inert");equal(290,height(off,group.container,false),"partial install: native");
        equal(false,off.click(group.children.get(0)),"partial install: native click");
    }

    private static void eligibility()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(5);
        equal(152,height(mini,group.container,false),"collapsed: first + two 4dp layers at density 4");
        equal(152,height(mini,group.container,true),"intrinsic closed");
        Group pair=new Group(2);equal(136,height(mini,pair.container,false),"two cards: one layer");
        Group single=new Group(1);equal(290,height(mini,single.container,false),"single child stays native");
        String[] blockers={"bundle","headsUp","launching","minimized","public","guts","encrypted","childHeadsUp","childPublic","detached","ambient","unmeasured","reparented"};
        for(String blocker:blockers) {
            Group g=new Group(3);
            switch(blocker) {
                case "bundle": g.summary.mIsBundle=true;break;
                case "headsUp": g.summary.mIsHeadsUp=true;break;
                case "launching": g.summary.mExpandAnimationRunning=true;break;
                case "minimized": g.summary.mIsMinimized=true;break;
                case "public": g.summary.publicShown=true;break;
                case "guts": g.summary.guts=true;break;
                case "encrypted": g.extension.encrypted=true;break;
                case "childHeadsUp": g.children.get(1).mIsHeadsUp=true;break;
                case "childPublic": g.children.get(2).publicShown=true;break;
                case "detached": g.stack.removeView(g.summary);break;
                case "ambient": g.stack.mAmbientState=null;break;
                case "unmeasured": ((Contracted)g.children.get(0).mPrivateLayout.mContractedChild).measured=0;break;
                default: g.children.get(1).mNotificationParent=null;break;
            }
            equal(290,height(mini,g.container,false),"native when "+blocker);
            mini.childrenUpdated(g.summary);equal(-1,g.children.get(0).state.height,"no endpoint when "+blocker);
        }
        // Expanded: the intrinsic (expanded) height stays native, the collapse endpoint is the stack.
        group.summary.mChildrenExpanded=true;
        equal(300,height(mini,group.container,true),"expanded intrinsic native");equal(152,height(mini,group.container,false),"expanded keeps closed endpoint");
        group.summary.mChildrenExpanded=false;group.summary.mIsUserSwipingToExpandRow=true;
        equal(300,height(mini,group.container,true),"swiping intrinsic native");
    }

    private static void closedEndpoint()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(5);
        Row first=group.children.get(0),second=group.children.get(1),third=group.children.get(2),fourth=group.children.get(3);
        mini.childrenUpdated(group.summary);
        equal(120,first.state.height,"front height");near(0f,first.state.y,"front y");near(1f,first.state.alpha,"front state alpha");
        equal(0,first.state.clipTopAmount,"front clip");equal(0,first.state.clipBottomAmount,"front clip bottom");
        near(16f,second.state.y,"rear 1 y at density 4");near(32f,third.state.y,"rear 2 y");near(.55f,second.state.alpha,"rear 1 state alpha");near(.30f,third.state.alpha,"rear 2 state alpha");
        check(fourth.state.hidden&&!third.state.hidden,"fourth onward hidden");near(0f,fourth.state.alpha,"fourth alpha");equal(-1,fourth.state.height,"fourth keeps native height");
        near(1f,((StateEx)first.state.mExpandableViewStateEx).contentAlpha,"front content");near(0f,((StateEx)second.state.mExpandableViewStateEx).contentAlpha,"rear content");
        // Real cards, ungrouped templates, standalone background refresh with the parent restored afterwards.
        for(int index=0;index<3;index++) {
            Row child=group.children.get(index);
            check(!child.mPrivateLayout.mIsChildInGroup&&!child.mPublicLayout.mIsChildInGroup,"deck card leaves single-line template "+index);
            equal(0,child.mPrivateLayout.mVisibleType,"contracted selected "+index);equal(null,child.parentDuringBackground,"standalone background "+index);
            equal(group.summary,child.mNotificationParent,"parent restored "+index);equal(1,child.roundness,"rounded once "+index);
        }
        check(fourth.mPrivateLayout.mIsChildInGroup,"hidden child keeps its template");
        near(1f,first.mPrivateLayout.getAlpha(),"front content visible");near(0f,second.mPrivateLayout.getAlpha(),"rear content hidden");
        near(0f,group.summary.mPrivateLayout.getAlpha(),"summary content hidden");
        near(1f,first.getAlpha(),"front row");near(.55f,second.getAlpha(),"rear 1 row");near(.30f,third.getAlpha(),"rear 2 row");near(0f,fourth.getAlpha(),"fourth row");
        equal(View.INVISIBLE,group.header.getVisibility(),"native header hidden");equal(View.INVISIBLE,fourth.getVisibility(),"fourth hidden");
        equal(View.VISIBLE,third.getVisibility(),"rear stays visible");
        equal(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,second.getImportantForAccessibility(),"rear accessibility");
        equal(0,first.getImportantForAccessibility(),"front accessibility untouched");
        // Idempotent: a second pass does not refresh backgrounds or reselect templates again.
        int selects=first.mPrivateLayout.selects;mini.childrenUpdated(group.summary);
        equal(1,first.roundness,"no repeated refresh");equal(selects,first.mPrivateLayout.selects,"no repeated select");
        // Closed cards keep their contracted template.
        equal(0,mini.contentType(first.mPrivateLayout,()->3),"front template");equal(0,mini.contentType(third.mPrivateLayout,()->3),"rear template");
        equal(3,mini.contentType(fourth.mPrivateLayout,()->3),"hidden child native template");
        equal(3,mini.contentType(new Group(1).children.get(0).mPrivateLayout,()->3),"ungrouped native template");
    }

    private static void drawing()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(5);mini.childrenUpdated(group.summary);
        Path nativeClip=group.container.mChildClipPath;
        Canvas canvas=new Canvas();
        equal(false,mini.drawChild(group.container,canvas,group.children.get(3),()->group.container.drawChild(canvas,group.children.get(3),0L)),"fourth not drawn");
        equal(false,mini.drawChild(group.container,canvas,group.header,()->true),"header not drawn");
        equal(0,group.container.drawn,"skipped draws");
        Canvas front=new Canvas();
        equal(true,mini.drawChild(group.container,front,group.children.get(0),()->group.container.drawChild(front,group.children.get(0),0L)),"front drawn");
        equal(null,group.container.clipDuringDraw,"native group clip bypassed during the card");check(group.container.mChildClipPath==nativeClip,"native clip restored");
        equal(1,front.pathClips,"front clipped to its outline");equal(0,front.outClips,"front not occluded");equal(front.saves,front.restores,"front canvas balanced");
        equal(1,front.circles,"count badge drawn with the front card");near(120f,front.circleX,"badge x at icon end");near(4f,front.circleY,"badge y");near(32f,front.circleRadius,"badge radius 8dp");
        equal(255,front.circleAlpha,"badge opaque when closed");equal("5",front.text,"real child count");
        Canvas rear=new Canvas();
        mini.drawChild(group.container,rear,group.children.get(1),()->group.container.drawChild(rear,group.children.get(1),0L));
        equal(1,rear.outClips,"rear occluded by the front");equal(1,rear.pathClips,"rear clipped to its own card");
        // backing(): front (0,0,124,120), tail 32 → rear 1 is 92% wide, bottom 136, its own contracted height scaled.
        near(4.96f,rear.translateX,"rear 1 left");near(25.6f,rear.translateY,"rear 1 top");near(.92f,rear.scaleX,"rear 1 width");near(120f*.92f/200f,rear.scaleY,"rear 1 height");
        equal(0,rear.circles,"badge only on the front");equal(rear.saves,rear.restores,"rear canvas balanced");
        Canvas back=new Canvas();
        mini.drawChild(group.container,back,group.children.get(2),()->true);
        equal(2,back.outClips,"second rear occluded by front and first rear");near(.84f,back.scaleX,"rear 2 width");near(9.92f,back.translateX,"rear 2 left");
        // A clipped-away group draws no rear peeks.
        group.summary.mClipBottomAmount=150;
        equal(false,mini.drawChild(group.container,new Canvas(),group.children.get(1),()->true),"rear hidden when the group is clipped");
        group.summary.mClipBottomAmount=0;
        NotificationMiniStack hidden=ready(true);Group retained=new Group(3);hidden.childrenUpdated(retained.summary);
        Canvas hiddenBadge=new Canvas();hidden.drawChild(retained.container,hiddenBadge,retained.children.get(0),()->true);
        equal(0,hiddenBadge.circles,"hide=true suppresses badge background");equal(null,hiddenBadge.text,"hide=true suppresses badge number");
        equal(true,hidden.hideBadge(),"requested count visibility is effective");
        hidden.configure(settings(true,false));Handler.drainForCheck();
        Canvas restoredBadge=new Canvas();hidden.drawChild(retained.container,restoredBadge,retained.children.get(0),()->true);
        equal(1,restoredBadge.circles,"count returns after showing it");equal("3",restoredBadge.text,"restored count uses current real children");
        Group many=new Group(120);mini.childrenUpdated(many.summary);Canvas big=new Canvas();
        mini.drawChild(many.container,big,many.children.get(0),()->true);equal("99+",big.text,"over 99");
    }

    /** The native progress transformer applies this exact per-row tag during its spring. */
    private static void outerStackVisibility()throws Throwable{
        NotificationMiniStack mini=ready(false);Group group=new Group(4);
        mini.setOuterStackPolicy(row->true);Handler.drainForCheck();
        CoveredResources resources=new CoveredResources();group.summary.coveredResources=resources;
        mini.childrenUpdated(group.summary);
        for(Object progress:new Object[]{1f,0f,.001f,.5f,1f,.5f,0f,Float.NaN,Float.POSITIVE_INFINITY,-1f,"unknown",null}) {
            group.summary.coveredProgress=progress;int reads=group.summary.coveredReads;
            boolean covered=progress instanceof Float&&Float.isFinite((Float)progress)&&(Float)progress>0f;
            mini.rowDraw(group.summary,()->{
                int[] nativeDraws={0};Canvas front=new Canvas();
                equal(true,mini.drawChild(group.container,front,group.children.get(0),()->{nativeDraws[0]++;return true;}),"outer stack retains the group's first real card");
                for(int index=1;index<=2;index++) {
                    Canvas rear=new Canvas();
                    equal(!covered,mini.drawChild(group.container,rear,group.children.get(index),()->{nativeDraws[0]++;return true;}),"only covered outer-stack rows suppress their mini rear cards: "+progress);
                    equal(rear.saves,rear.restores,"covered/open rear drawing keeps the native canvas balanced");
                }
                equal(covered?1:3,nativeDraws[0],"covered rear cards never enter native drawing");
                return null;
            });
            equal(1,group.summary.coveredReads-reads,"one live native covered-progress read serves both rear cards per row draw");
            equal(View.VISIBLE,group.children.get(1).getVisibility(),"draw suppression does not hide or reparent native children");
        }
        equal(1,resources.lookups,"native progress id resolves once across transition frames");
        group.summary.coveredProgress=1f;group.summary.mChildrenExpanded=true;
        mini.rowDraw(group.summary,()->{
            equal(true,mini.drawChild(group.container,new Canvas(),group.children.get(1),()->true),"native group expansion owns every child despite an old outer covered tag");return null;
        });
        group.summary.mChildrenExpanded=false;group.summary.mIsHeadsUp=true;
        equal(true,mini.drawChild(group.container,new Canvas(),group.children.get(1),()->true),"unmanaged heads-up groups keep native drawing");
        group.summary.mIsHeadsUp=false;
        for(boolean fail:new boolean[]{false,true}) {
            Group absent=new Group(3);CoveredResources missing=new CoveredResources();missing.missing=true;missing.fail=fail;
            absent.summary.coveredResources=missing;absent.summary.coveredProgress=1f;mini.childrenUpdated(absent.summary);
            for(int frame=0;frame<5;frame++)mini.rowDraw(absent.summary,()->{
                equal(true,mini.drawChild(absent.container,new Canvas(),absent.children.get(1),()->true),"missing native signal retains normal mini-stack drawing");return null;
            });
            equal(1,missing.lookups,"an unavailable native progress id is not resolved again every frame");
        }
        mini.configure(settings(false,false));Handler.drainForCheck();
        near(1f,group.children.get(1).getAlpha(),"release restores the native rear alpha");
        equal(true,mini.drawChild(group.container,new Canvas(),group.children.get(1),()->true),"feature release restores unmodified native drawing");
    }
    private static void preDraw(View row){
        for(android.view.ViewTreeObserver.OnPreDrawListener listener:new ArrayList<>(row.getViewTreeObserver().preDrawListeners))listener.onPreDraw();
    }
    /** RenderNode reuse need not call drawChild again; persistent row alpha must still change. */
    private static void cachedOuterStack()throws Throwable{
        NotificationMiniStack mini=ready(false);Group group=new Group(3);
        mini.setOuterStackPolicy(row->true);Handler.drainForCheck();
        group.summary.coveredResources=new CoveredResources();group.summary.coveredProgress=0f;
        mini.childrenUpdated(group.summary);near(.55f,group.children.get(1).getAlpha(),"normal mini rear");
        StateEx state=(StateEx)group.summary.state.mExpandableViewStateEx;
        state.animation.scale.x=.8f;state.animation.scale.y=.8f;group.summary.setScaleX(.8f);group.summary.setScaleY(.8f);
        preDraw(group.summary);
        near(0f,group.children.get(1).getAlpha(),"cached rear RenderNode hides when native outer stack compresses this managed row");
        near(0f,group.children.get(2).getAlpha(),"both rear nodes hidden");near(1f,group.children.get(0).getAlpha(),"first real card remains");
        equal("key_stacked_notification",state.animation.requested,"only native outer-stack scale owner is read");
        state.animation.scale.x=state.animation.scale.y=1f;preDraw(group.summary);
        near(0f,group.children.get(1).getAlpha(),"reset target does not lead the real spring");
        group.summary.setScaleX(1f);group.summary.setScaleY(1f);preDraw(group.summary);
        near(.55f,group.children.get(1).getAlpha(),"fully opened outer row restores mini rear");
        group.summary.setScaleX(.8f);group.summary.setScaleY(.8f);preDraw(group.summary);
        near(.55f,group.children.get(1).getAlpha(),"generic entry or pinch scale cannot establish outer coverage");
        state.animation.scale.x=state.animation.scale.y=.8f;group.summary.setScaleX(1f);group.summary.setScaleY(1f);preDraw(group.summary);
        near(.55f,group.children.get(1).getAlpha(),"target cannot hide rear before actual native compression");
        group.summary.setScaleX(.8f);group.summary.setScaleY(.8f);preDraw(group.summary);
        nativeWrite(mini,group.children.get(1),.67f);near(0f,group.children.get(1).getAlpha(),"native alpha write remains hidden on cached GPU node");
        nativeWrite(mini,group.children.get(2),0f);near(0f,group.children.get(2).getAlpha(),"native zero intent retained while already hidden");
        Bundle options=settings(true,false);options.putBoolean(NotificationMiniStack.AUTO_HIDE_IN_STACK,false);mini.configure(options);Handler.drainForCheck();
        near(.55f,group.children.get(1).getAlpha(),"child option off restores normal mini opacity");
        equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"option off removes only the outer observer");
        check(!options.getBoolean(NotificationMiniStack.AUTO_HIDE_IN_STACK,true),"saved option is retained");
        options.putBoolean(NotificationMiniStack.AUTO_HIDE_IN_STACK,true);mini.configure(options);Handler.drainForCheck();
        near(0f,group.children.get(1).getAlpha(),"option on catches already compressed row");
        mini.setOuterStackPolicy(row->false);Handler.drainForCheck();near(.55f,group.children.get(1).getAlpha(),"unmanaged parent never hides mini");
        check(options.getBoolean(NotificationMiniStack.AUTO_HIDE_IN_STACK,false),"parent disable never resets saved child option");
        mini.setOuterStackPolicy(row->true);Handler.drainForCheck();near(0f,group.children.get(1).getAlpha(),"actual parent ownership resumes hide");
        options.putBoolean(StatusBarSettings.SAFE_MODE,true);mini.configure(options);Handler.drainForCheck();near(.55f,group.children.get(1).getAlpha(),"safe mode restores native mini rear");
        options.putBoolean(StatusBarSettings.SAFE_MODE,false);mini.configure(options);Handler.drainForCheck();
        group.summary.mChildrenExpanded=true;preDraw(group.summary);near(.55f,group.children.get(1).getAlpha(),"group expansion releases outer rear lease");
        group.summary.mChildrenExpanded=false;preDraw(group.summary);near(0f,group.children.get(1).getAlpha(),"collapsed group can be covered again");
        mini.configure(settings(false,false));Handler.drainForCheck();
        near(.67f,group.children.get(1).getAlpha(),"release restores freshest native alpha");
        near(0f,group.children.get(2).getAlpha(),"release preserves a native zero written while hidden");
        equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"release removes outer observer");
        NotificationMiniStack unmanaged=ready(false);Group ordinary=new Group(3);ordinary.summary.coveredResources=new CoveredResources();ordinary.summary.coveredProgress=1f;
        unmanaged.childrenUpdated(ordinary.summary);preDraw(ordinary.summary);near(.55f,ordinary.children.get(1).getAlpha(),"unknown parent contract keeps the native mini deck");
        check(Boolean.TRUE.equals(NotificationMiniStack.BOOLEANS.get(NotificationMiniStack.AUTO_HIDE_IN_STACK)),"auto hide defaults on");
        NotificationMiniStack missing=new NotificationMiniStack(new Handler(null));
        missing.bind(new NotificationMiniStack.Targets(Row.class,Container.class,Extension.class,Overlay.class,Stack.class,MissingScaleStateEx.class,Source.class,Clicker.class,ContainerExtension.class,GroupExtension.class));
        missing.hooksReady(true);missing.configure(settings(true,false));missing.setOuterStackPolicy(row->true);Handler.drainForCheck();
        Group absent=new Group(3);absent.summary.coveredResources=new CoveredResources();absent.summary.coveredProgress=0f;absent.summary.setScaleX(.8f);
        missing.childrenUpdated(absent.summary);preDraw(absent.summary);near(.55f,absent.children.get(1).getAlpha(),"missing native keyed-scale reflection retains normal mini rear");
        absent.summary.coveredProgress=1f;preDraw(absent.summary);near(0f,absent.children.get(1).getAlpha(),"independently verified covered spring still works without optional scale reflection");
        List<android.view.ViewTreeObserver.OnPreDrawListener> obsolete=new ArrayList<>(absent.summary.getViewTreeObserver().preDrawListeners);
        for(View.OnAttachStateChangeListener listener:new ArrayList<>(absent.summary.attachListeners))listener.onViewDetachedFromWindow(absent.summary);
        near(1f,absent.children.get(1).getAlpha(),"detach restores rear node alpha");
        equal(0,absent.summary.getViewTreeObserver().preDrawListeners.size(),"detach removes outer lease observer");
        for(android.view.ViewTreeObserver.OnPreDrawListener listener:obsolete)listener.onPreDraw();near(1f,absent.children.get(1).getAlpha(),"late obsolete preDraw cannot reacquire detached owner");
        Group moved=new Group(3);moved.summary.coveredResources=new CoveredResources();moved.summary.coveredProgress=1f;
        missing.childrenUpdated(moved.summary);near(0f,moved.children.get(1).getAlpha(),"rear lease acquired before reparent");
        Container foreign=new Container();moved.container.removeView(moved.children.get(1));foreign.addView(moved.children.get(1));
        preDraw(moved.summary);near(1f,moved.children.get(1).getAlpha(),"native reparent cannot inherit another group's hidden rear lease");
        equal(0,moved.summary.getViewTreeObserver().preDrawListeners.size(),"reparent releases outer observer without waiting for another draw");
    }
    private static void nativeWrite(NotificationMiniStack mini,View view,float alpha){view.setAlpha(mini.nativeBadgeAlpha(view,alpha));}
    private static void nativeBadges()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(4);mini.childrenUpdated(group.summary);
        TextView overflow=new TextView(null);overflow.setText("+1");overflow.setAlpha(.7f);
        group.container.mOverflowNumber=overflow;group.container.addView(overflow);
        group.summary.mGroupExpansionChanging=true;group.extension.groupExpansionProgress=.5f;
        int[] draws={0};
        equal(false,mini.drawChild(group.container,new Canvas(),overflow,()->{draws[0]++;return true;}),"current native overflow not rendered during collapse");
        equal(0,draws[0],"overflow draw intercepted before native painting");
        near(0f,overflow.getAlpha(),"overflow RenderNode stays hidden outside draw");equal("+1",overflow.getText(),"overflow native text untouched");
        equal(View.VISIBLE,overflow.getVisibility(),"overflow native visibility untouched");
        group.container.removeView(overflow);group.container.mOverflowNumber=null;group.container.transients.add(overflow);
        preDraw(group.summary);
        equal(false,mini.drawChild(group.container,new Canvas(),overflow,()->{draws[0]++;return true;}),"known overflow stays hidden through native transient fade");
        TextView replacement=new TextView(null);replacement.setText("+2");replacement.setAlpha(.8f);
        group.container.addView(replacement);group.container.mOverflowNumber=replacement;
        preDraw(group.summary);near(0f,replacement.getAlpha(),"replacement overflow leased before its first GPU frame");
        TextView unrelated=new TextView(null);unrelated.setText("unrelated transient text");group.container.transients.add(unrelated);
        equal(true,mini.drawChild(group.container,new Canvas(),unrelated,()->{draws[0]++;return true;}),"an unrelated transient TextView remains native");
        near(1f,unrelated.getAlpha(),"unrelated transient text alpha unchanged");
        ViewGroup nativeHeader=new ViewGroup(null),pill=new ViewGroup(null);
        TextView number=new TextView(null);number.setText("4");number.setAlpha(.8f);
        nativeHeader.addView(pill);pill.addView(number);group.container.addView(nativeHeader);
        GroupExtension ext=group.container.mExt.oplusNotificationGroupExtImpl;ext.numberView=number;ext.expandedPill=pill;
        pill.setAlpha(.6f);View icon=new View(null);nativeHeader.addView(icon);
        pill.setTranslationX(3f);nativeHeader.setTranslationY(2f);
        Canvas headerCanvas=new Canvas();headerCanvas.hardware=true;
        equal(true,mini.drawChild(group.container,headerCanvas,nativeHeader,()->{
            near(0f,pill.getAlpha(),"OEM pure number pill is hidden at native draw time");
            near(.8f,number.getAlpha(),"the child number alpha remains native under the hidden pill");
            near(1f,icon.getAlpha(),"full icon remains visible beneath the former badge");
            equal(0,headerCanvas.outRectClips,"no canvas hole can erase the underlying icon");
            return true;
        }),"the rest of the native header still draws");
        near(0f,pill.getAlpha(),"GPU delayed playback cannot observe a draw-time alpha restoration");
        near(3f,pill.getTranslationX(),"pill position remains native");near(2f,nativeHeader.getTranslationY(),"header animation position remains native");
        nativeWrite(mini,pill,.3f);near(0f,pill.getAlpha(),"a native alpha animation retains hidden GPU alpha");
        Canvas failed=new Canvas();int beforeFailure=failed.getSaveCount();
        try{mini.drawChild(group.container,failed,nativeHeader,()->{throw new IllegalStateException("native draw failed");});check(false,"draw failure swallowed");}
        catch(IllegalStateException expected){equal("native draw failed",expected.getMessage(),"native exception propagated");}
        equal(beforeFailure,failed.getSaveCount(),"draw failure keeps the native canvas balanced");near(0f,pill.getAlpha(),"native exception does not release the persistent lease");
        ext.expandedPill=new ViewGroup(null);pill.removeView(number);nativeHeader.addView(number);
        preDraw(group.summary);
        near(.3f,pill.getAlpha(),"stale pill reference is restored to latest native alpha");near(0f,number.getAlpha(),"a reparented native number receives its own lease");
        nativeWrite(mini,number,0f);nativeWrite(mini,number,1f);near(0f,number.getAlpha(),"native 0 and 1 intent does not reveal the leased number");
        for(String blocker:new String[]{"privacy","single","detached"}) {
            Group nativeGroup=new Group(blocker.equals("single")?1:3);TextView count=new TextView(null);nativeGroup.container.mOverflowNumber=count;
            switch(blocker){
                case "privacy":nativeGroup.summary.publicShown=true;break;
                case "detached":nativeGroup.stack.removeView(nativeGroup.summary);break;
                default:break;
            }
            equal(true,mini.drawChild(nativeGroup.container,new Canvas(),count,()->true),"out-of-scope native count remains visible: "+blocker);
            near(1f,count.getAlpha(),"out-of-scope native alpha unchanged: "+blocker);
        }
        mini.configure(settings(true,false));Handler.drainForCheck();
        near(.7f,overflow.getAlpha(),"turning hide off restores transient overflow baseline");near(.8f,replacement.getAlpha(),"turning hide off restores replacement baseline");
        near(1f,number.getAlpha(),"turning hide off restores latest native 1 rather than stale alpha");
        equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"turning hide off removes badge observer immediately");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);near(0f,number.getAlpha(),"reenabling leases the count again");
        mini.configure(settings(false,true));Handler.drainForCheck();near(1f,number.getAlpha(),"master off restores native number immediately");
        mini.configure(settings(true,true));mini.hooksReady(true);mini.childrenUpdated(group.summary);near(0f,number.getAlpha(),"ready feature reacquires exact count");
        mini.hooksReady(false);Handler.drainForCheck();near(1f,number.getAlpha(),"partial hook removal returns native number");
        equal(true,mini.drawChild(group.container,new Canvas(),replacement,()->true),"partial hook install never suppresses native badge");
    }

    /** Native count copies can outlive the one pointer retained by OplusNotificationGroupExtImpl. */
    private static void nativeTemplateBadges()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);mini.childrenUpdated(group.summary);
        ViewGroup front=(ViewGroup)group.children.get(0).mPrivateLayout.mContractedChild;
        final int[] resourceReads={0};
        android.content.res.Resources nativeResources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){
                return id==7001?"oplus_expand_button_number":id==7002?"oplus_expand_button_pill":id==7003?"expand_button_number"
                        :id==7004?"oplus_top_right_badge_number":id==7005?"oplus_capsule_top_right_badge_pill":"title";
            }
            @Override public String getResourcePackageName(int id){return id==7003?"android":"com.android.systemui";}
        };
        android.content.res.Resources remoteResources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){return "oplus_expand_button_number";}
            @Override public String getResourcePackageName(int id){return "example.remote.views";}
        };
        List<View> nativeNumbers=new ArrayList<>(),nativePills=new ArrayList<>();
        for(int index=0;index<2;index++) {
            ViewGroup pill=new ViewGroup(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
            pill.setId(7002);pill.setAlpha(.6f);
            TextView count=new TextView(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
            count.setId(7001);count.setText("3");count.setAlpha(.7f);
            pill.addView(count);front.addView(pill);nativeNumbers.add(count);nativePills.add(pill);
        }
        GroupExtension ext=group.container.mExt.oplusNotificationGroupExtImpl;
        ext.numberView=new TextView(null);ext.expandedPill=new ViewGroup(null);ext.expandedPill.addView(ext.numberView);
        // The count on the group icon's top-right corner.
        ViewGroup cornerPill=new ViewGroup(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
        cornerPill.setId(7005);cornerPill.setAlpha(.8f);
        TextView cornerCount=new TextView(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
        cornerCount.setId(7004);cornerCount.setText("2");cornerPill.addView(cornerCount);front.addView(cornerPill);
        ViewGroup androidPill=new ViewGroup(null);front.addView(androidPill);
        TextView androidCount=new TextView(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
        androidCount.setId(7003);androidPill.addView(androidCount);
        View expandArrow=new View(null);androidPill.addView(expandArrow);
        TextView remoteCount=new TextView(null){@Override public android.content.res.Resources getResources(){return remoteResources;}};
        remoteCount.setId(7001);remoteCount.setText("unrelated RemoteViews text");front.addView(remoteCount);
        Canvas canvas=new Canvas();canvas.hardware=true;
        preDraw(group.summary);
        mini.drawChild(group.container,canvas,group.children.get(0),()->{
            for(View pill:nativePills)near(0f,pill.getAlpha(),"every OEM copied pill stays hidden despite stale extension pointer");
            near(0f,cornerPill.getAlpha(),"the count on the group icon's corner is hidden with the others");near(1f,cornerCount.getAlpha(),"its text keeps native alpha");
            for(View number:nativeNumbers){near(.7f,number.getAlpha(),"OEM child count alpha remains native");equal(View.VISIBLE,number.getVisibility(),"copied count native visibility remains unchanged");}
            near(0f,androidCount.getAlpha(),"Android template number is hidden");near(1f,androidPill.getAlpha(),"Android arrow container remains native");
            near(1f,expandArrow.getAlpha(),"Android expand arrow retains native alpha");near(1f,remoteCount.getAlpha(),"a coincident resource id from another package is untouched");
            equal(0,canvas.outRectClips,"template suppression never clips underlying icon pixels");return true;
        });
        for(View pill:nativePills)near(0f,pill.getAlpha(),"OEM GPU alpha remains zero after CPU draw returns");
        near(0f,androidCount.getAlpha(),"Android GPU alpha remains zero after CPU draw returns");
        int resolvedResources=resourceReads[0];
        for(int frame=0;frame<12;frame++)preDraw(group.summary);
        equal(resolvedResources,resourceReads[0],"stable preDraw frames do not reclassify template resources");
        near(0f,androidCount.getAlpha(),"count GPU alpha stays hidden across later hardware playback frames");
        group.summary.shown=false;
        TextView hiddenLate=new TextView(null){@Override public android.content.res.Resources getResources(){resourceReads[0]++;return nativeResources;}};
        hiddenLate.setId(7003);front.addView(hiddenLate);preDraw(group.summary);
        equal(resolvedResources,resourceReads[0],"hidden shade does not scan late template children on every window frame");
        group.summary.shown=true;preDraw(group.summary);
        near(0f,hiddenLate.getAlpha(),"late hidden-shade template is leased before its first visible frame");
        TextView rebuilt=new TextView(null){@Override public android.content.res.Resources getResources(){return nativeResources;}};
        rebuilt.setId(7001);rebuilt.setAlpha(.9f);front.addView(rebuilt);
        preDraw(group.summary);near(0f,rebuilt.getAlpha(),"preDraw leases late replacement without a row dispatchDraw call");
        front.removeView(nativePills.get(0));preDraw(group.summary);
        near(.6f,nativePills.get(0).getAlpha(),"a copied pill removed from the owned subtree is returned immediately");
        equal(0,nativePills.get(0).attachListeners.size(),"obsolete copied pill listener is detached");
        nativeWrite(mini,androidCount,1f);nativeWrite(mini,rebuilt,0f);
        mini.transition(NotificationMiniStack.SET_EXPANDED,group.summary,true,()->{
            near(0f,androidCount.getAlpha(),"native expansion does not restore copied count alpha before its setter");
            near(0f,rebuilt.getAlpha(),"native alpha zero remains hidden through geometry hand-back");
            near(0f,nativePills.get(1).getAlpha(),"copied OEM pill stays hidden during expansion hand-back");
            equal(1,group.summary.getViewTreeObserver().preDrawListeners.size(),"expansion keeps the independent badge observer");
            group.summary.mChildrenExpanded=true;return null;
        });
        Canvas expanded=new Canvas();mini.drawChild(group.container,expanded,group.children.get(0),()->true);
        near(0f,androidCount.getAlpha(),"expanded group retains requested count hiding");equal(0,expanded.outRectClips,"expanded rendering has no count mask");
        mini.transition(NotificationMiniStack.SET_EXPANDED,group.summary,false,()->{group.summary.mChildrenExpanded=false;return null;});
        mini.childrenUpdated(group.summary);
        near(0f,androidCount.getAlpha(),"closed group reacquires native template count");
        mini.configure(settings(true,false));Handler.drainForCheck();near(1f,androidCount.getAlpha(),"turning hide off restores copied Android count");
    }

    /** The same setter callback the main View.setAlpha hook installs, including calls made by the lease itself. */
    private static final class HookedBadge extends TextView {
        final NotificationMiniStack mini;
        HookedBadge(NotificationMiniStack mini,float alpha){super(null);this.mini=mini;super.setAlpha(alpha);}
        @Override public void setAlpha(float alpha){super.setAlpha(mini.nativeBadgeAlpha(this,alpha));}
        void bypassAlpha(float alpha){super.setAlpha(alpha);}
    }
    /** A native expansion hands geometry back before its first frame; the hidden count must never be handed back with it. */
    private static void badgeExpansionContinuity()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);
        ViewGroup front=(ViewGroup)group.children.get(0).mPrivateLayout.mContractedChild;
        final android.content.res.Resources cornerResources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){return id==7101?"oplus_top_right_badge_number":"oplus_capsule_top_right_badge_pill";}
            @Override public String getResourcePackageName(int id){return "com.android.systemui";}
        };
        ViewGroup cornerPill=new ViewGroup(null){@Override public android.content.res.Resources getResources(){return cornerResources;}};
        cornerPill.setId(7102);cornerPill.setAlpha(.8f);
        TextView cornerCount=new TextView(null){@Override public android.content.res.Resources getResources(){return cornerResources;}};
        cornerCount.setId(7101);cornerCount.setText("3");cornerPill.addView(cornerCount);front.addView(cornerPill);
        HookedBadge overflow=new HookedBadge(mini,.7f);group.container.addView(overflow);group.container.mOverflowNumber=overflow;
        mini.childrenUpdated(group.summary);near(0f,cornerPill.getAlpha(),"closed icon corner starts hidden");
        mini.transition(NotificationMiniStack.SET_EXPANDED,group.summary,true,()->{
            near(0f,cornerPill.getAlpha(),"the first native expansion setter must not expose the corner RenderNode");
            near(0f,overflow.getAlpha(),"geometry hand-back leaves overflow hidden");
            group.summary.setChildrenExpanded(true);overflow.setAlpha(1f);return null;
        });
        near(0f,overflow.getAlpha(),"native alpha 1 after expanded=true remains hidden before animation flags arrive");
        group.extension.isChildrenExpandedAnimating=true;group.extension.groupExpansionProgress=.01f;
        preDraw(group.summary);
        mini.rowDraw(group.summary,()->{
            mini.drawChild(group.container,new Canvas(),group.children.get(0),()->{
                near(0f,cornerPill.getAlpha(),"first expanding GPU frame cannot show the icon corner");return true;
            });return null;
        });
        mini.transition(NotificationMiniStack.SET_ANIMATING,group.extension,false,()->{
            group.extension.isChildrenExpandedAnimating=false;group.extension.groupExpansionProgress=1f;return null;
        });
        preDraw(group.summary);near(0f,cornerPill.getAlpha(),"stable expanded icon corner stays hidden");
        overflow.setAlpha(.4f);near(0f,overflow.getAlpha(),"stable expanded native alpha writer remains leased");
        ViewGroup expandedHeader=new ViewGroup(null);group.container.addView(expandedHeader);
        HookedBadge replacement=new HookedBadge(mini,.6f);expandedHeader.addView(replacement);
        group.container.mExt.oplusNotificationGroupExtImpl.numberView=replacement;
        preDraw(group.summary);near(0f,replacement.getAlpha(),"late expanded header count is hidden before its first visible frame");
        group.overlay.isUserLocked=true;preDraw(group.summary);near(0f,cornerPill.getAlpha(),"holding the expanded group does not reveal its count");
        group.overlay.isUserLocked=false;
        mini.configure(settings(true,false));Handler.drainForCheck();
        near(.8f,cornerPill.getAlpha(),"hide off restores the expanded corner baseline");near(.4f,overflow.getAlpha(),"hide off restores latest expanded native alpha");
        near(.6f,replacement.getAlpha(),"hide off restores late expanded header baseline");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);
        near(0f,cornerPill.getAlpha(),"expanded children update reacquires hidden counts without folded geometry");
        mini.configure(settings(false,true));Handler.drainForCheck();near(.8f,cornerPill.getAlpha(),"master off returns stable expanded count");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);
        Bundle safe=SafetyMode.runtimeSettings(ModuleLifecycle.nativeSettings());mini.configure(safe);Handler.drainForCheck();
        near(.8f,cornerPill.getAlpha(),"safe mode returns the expanded count");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);group.summary.members.remove(2);group.summary.members.remove(1);
        preDraw(group.summary);near(.8f,cornerPill.getAlpha(),"group splitting to one child returns the expanded count");
        Group detached=new Group(3);HookedBadge detachedCount=new HookedBadge(mini,.3f);detached.container.addView(detachedCount);detached.container.mOverflowNumber=detachedCount;
        detached.summary.mChildrenExpanded=true;mini.childrenUpdated(detached.summary);near(0f,detachedCount.getAlpha(),"an initially expanded eligible group receives badge hiding");
        for(View.OnAttachStateChangeListener listener:new ArrayList<>(detached.summary.attachListeners))listener.onViewDetachedFromWindow(detached.summary);
        near(.3f,detachedCount.getAlpha(),"expanded owner detach returns latest native alpha immediately");
        equal(0,detachedCount.attachListeners.size(),"expanded owner detach removes count listener");
    }
    private static void badgeLeaseLifecycle()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);
        ViewGroup front=(ViewGroup)group.children.get(0).mPrivateLayout.mContractedChild;
        HookedBadge count=new HookedBadge(mini,.7f);front.addView(count);
        GroupExtension ext=group.container.mExt.oplusNotificationGroupExtImpl;ext.numberView=count;
        mini.childrenUpdated(group.summary);near(0f,count.getAlpha(),"internal setter is applied to native GPU property");
        mini.configure(settings(true,false));Handler.drainForCheck();near(.7f,count.getAlpha(),"internal zero setter never overwrites the captured native baseline");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);count.setAlpha(0f);count.setAlpha(1f);
        near(0f,count.getAlpha(),"native 0 then 1 writes are captured while GPU remains hidden");
        mini.configure(settings(true,false));Handler.drainForCheck();near(1f,count.getAlpha(),"latest native 1 is restored rather than the original .7");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);count.setAlpha(0f);
        mini.configure(settings(true,false));Handler.drainForCheck();near(0f,count.getAlpha(),"latest native 0 is also restored honestly");
        count.setAlpha(.8f);mini.configure(settings(true,true));mini.childrenUpdated(group.summary);
        count.bypassAlpha(.25f);preDraw(group.summary);near(0f,count.getAlpha(),"observable native bypass is re-hidden before GPU playback");
        mini.configure(settings(true,false));Handler.drainForCheck();near(.25f,count.getAlpha(),"observable native bypass becomes the latest baseline");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);
        count.attached=false;
        for(View.OnAttachStateChangeListener listener:new ArrayList<>(count.attachListeners))listener.onViewDetachedFromWindow(count);
        near(.25f,count.getAlpha(),"exact number detach restores its latest native alpha immediately");equal(0,count.attachListeners.size(),"number detach releases its listener");
        preDraw(group.summary);near(.25f,count.getAlpha(),"a detached count stays native during a tree cache rebuild");
        count.attached=true;preDraw(group.summary);near(0f,count.getAlpha(),"preDraw reacquires a reattached count even when child identities do not change");
        android.os.Looper.setCurrentForCheck(null);
        try{near(.4f,mini.nativeBadgeAlpha(count,.4f),"unrelated off-main setters do not inspect UI ownership maps");}
        finally{android.os.Looper.resetCurrentForCheck();}
        Bundle safe=SafetyMode.runtimeSettings(ModuleLifecycle.nativeSettings());mini.configure(safe);Handler.drainForCheck();
        near(.25f,count.getAlpha(),"safe-mode native settings immediately restore count alpha");equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"safe mode removes badge preDraw observer");
        mini.configure(settings(true,true));mini.childrenUpdated(group.summary);count.setAlpha(.9f);
        for(View.OnAttachStateChangeListener listener:new ArrayList<>(group.summary.attachListeners))listener.onViewDetachedFromWindow(group.summary);
        near(.9f,count.getAlpha(),"owner detach restores the latest native write");equal(0,count.attachListeners.size(),"owner detach releases all number listeners");
        equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"owner detach removes badge observer");
    }

    /** Stable GPU frames must keep native count leases without repeatedly enumerating the RemoteViews tree. */
    private static void stableBadgeTree()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);
        final int[] childReads={0};
        ViewGroup header=new ViewGroup(null){
            @Override public View getChildAt(int index){childReads[0]++;return super.getChildAt(index);}
        };
        HookedBadge count=new HookedBadge(mini,.7f);header.addView(count);header.addView(new View(null));
        ((ViewGroup)group.children.get(0).mPrivateLayout.mContractedChild).addView(header);
        group.container.mExt.oplusNotificationGroupExtImpl.numberView=count;
        mini.childrenUpdated(group.summary);near(0f,count.getAlpha(),"initial native count is leased");
        int initialReads=childReads[0];check(initialReads>0,"initial count discovery enumerates its header");
        for(int frame=0;frame<12;frame++)preDraw(group.summary);
        equal(initialReads,childReads[0],"stable badge frames do not enumerate descendant child indices");
        near(0f,count.getAlpha(),"stable badge frames retain the hidden GPU count");
        header.removeView(count);header.addView(count);preDraw(group.summary);
        equal(initialReads,childReads[0],"same-parent reordering does not rediscover the unchanged count set");
        near(0f,count.getAlpha(),"same-parent reordering preserves the existing count lease");
        final android.content.res.Resources nativeResources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){return "expand_button_number";}
            @Override public String getResourcePackageName(int id){return "android";}
        };
        TextView replacement=new TextView(null){@Override public android.content.res.Resources getResources(){return nativeResources;}};
        replacement.setId(7201);replacement.setAlpha(.9f);
        // The OEM pointer is deliberately stale: same-count RemoteViews replacement must invalidate membership.
        header.removeView(count);header.addView(replacement);preDraw(group.summary);
        check(childReads[0]>initialReads,"same-count replacement rebuilds descendant count discovery");
        near(.7f,count.getAlpha(),"replaced count is restored even when the OEM pointer still names it");
        equal(0,count.attachListeners.size(),"replaced count relinquishes its attach listener");
        near(0f,replacement.getAlpha(),"same-count replacement is hidden before its first GPU frame");
        int rebuiltReads=childReads[0];preDraw(group.summary);
        equal(rebuiltReads,childReads[0],"a rebuilt stable tree resumes cached membership checks");
        TextView transientNumber=new TextView(null){@Override public android.content.res.Resources getResources(){return nativeResources;}};
        transientNumber.setId(7201);transientNumber.setAlpha(.5f);group.container.transients.add(transientNumber);
        preDraw(group.summary);near(0f,transientNumber.getAlpha(),"late transient native count receives a lease");
        TextView transientReplacement=new TextView(null){@Override public android.content.res.Resources getResources(){return nativeResources;}};
        transientReplacement.setId(7201);transientReplacement.setAlpha(.4f);group.container.transients.set(0,transientReplacement);
        preDraw(group.summary);near(.5f,transientNumber.getAlpha(),"same-count transient replacement restores its predecessor");
        near(0f,transientReplacement.getAlpha(),"same-count transient replacement is hidden before GPU playback");
        mini.configure(settings(true,false));Handler.drainForCheck();
        near(.9f,replacement.getAlpha(),"rebuilt cached count restores native alpha");
        near(.4f,transientReplacement.getAlpha(),"rebuilt transient count restores native alpha");
    }

    /** ViewGroup retains a disappearing child's parent/attachment until endViewTransition, after removing its index. */
    private static final class TransitionHeader extends ViewGroup {
        View transitioning;
        TransitionHeader(){super(null);}
        public void startViewTransition(View view){transitioning=view;}
        @Override public void removeView(View view){super.removeView(view);if(view==transitioning)view.parent=this;}
        public void endViewTransition(View view){
            if(view!=transitioning)return;
            transitioning=null;
            if(indexOfChild(view)<0&&view.parent==this) {
                view.parent=null;view.attached=false;
                for(View.OnAttachStateChangeListener listener:new ArrayList<>(view.attachListeners))listener.onViewDetachedFromWindow(view);
            }
        }
    }
    private static void transitioningBadgeTree()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);TransitionHeader header=new TransitionHeader();
        HookedBadge old=new HookedBadge(mini,.7f);header.addView(old);header.addView(new View(null));
        ((ViewGroup)group.children.get(0).mPrivateLayout.mContractedChild).addView(header);
        group.container.mExt.oplusNotificationGroupExtImpl.numberView=old;mini.childrenUpdated(group.summary);
        final android.content.res.Resources nativeResources=new android.content.res.Resources(){
            @Override public String getResourceEntryName(int id){return "expand_button_number";}
            @Override public String getResourcePackageName(int id){return "android";}
        };
        TextView replacement=new TextView(null){@Override public android.content.res.Resources getResources(){return nativeResources;}};
        replacement.setId(7301);replacement.setAlpha(.9f);
        header.startViewTransition(old);header.removeView(old);header.addView(replacement);
        check(header.getChildCount()==2&&old.getParent()==header&&old.isAttachedToWindow(),"native removal retains the disappearing child's parent and attachment");
        check(header.indexOfChild(old)<0,"the disappearing child is absent from the real child array");
        preDraw(group.summary);
        near(0f,replacement.getAlpha(),"a same-count replacement is hidden while its predecessor retains a transition parent");
        near(0f,old.getAlpha(),"the retained native count stays hidden throughout its disappearance animation");
        header.endViewTransition(old);near(.7f,old.getAlpha(),"ending the native transition returns the detached count lease");
        equal(0,old.attachListeners.size(),"ending the native transition removes the detached count observer");
        preDraw(group.summary);near(0f,replacement.getAlpha(),"ending the predecessor transition preserves the replacement lease");
        mini.configure(settings(true,false));Handler.drainForCheck();near(.9f,replacement.getAlpha(),"hide off returns the replacement's native alpha after the transition");
    }

    private static void badgeUninstall()throws Throwable{
        NotificationMiniStack mini=ready(true);Group group=new Group(3);
        HookedBadge count=new HookedBadge(mini,.8f);group.children.get(0).mPrivateLayout.addView(count);
        group.container.mExt.oplusNotificationGroupExtImpl.numberView=count;mini.childrenUpdated(group.summary);
        near(0f,count.getAlpha(),"uninstall fixture begins with owned hidden count");
        Group expanded=new Group(3);HookedBadge expandedCount=new HookedBadge(mini,.2f);expanded.container.addView(expandedCount);expanded.container.mOverflowNumber=expandedCount;
        expanded.summary.mChildrenExpanded=true;mini.childrenUpdated(expanded.summary);near(0f,expandedCount.getAlpha(),"expanded uninstall fixture owns the count");
        ModuleLifecycle.markRemoved();preDraw(group.summary);
        preDraw(expanded.summary);near(.2f,expandedCount.getAlpha(),"uninstall returns stable expanded count");
        near(.8f,count.getAlpha(),"removed module releases count before the next GPU frame");equal(0,count.attachListeners.size(),"uninstall releases number listener");
        equal(0,group.summary.getViewTreeObserver().preDrawListeners.size(),"uninstall removes badge preDraw observer");
    }

    private static void returning()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(3);mini.childrenUpdated(group.summary);
        // Native collapse spring in flight: the deck geometry blends with the live position; hidden flags stay native.
        group.extension.groupExpansionProgress=.5f;group.summary.mGroupExpansionChanging=true;
        group.children.get(2).state.hidden=false;
        mini.childrenUpdated(group.summary);check(!group.children.get(2).state.hidden,"no hidden flag while returning");
        Canvas canvas=new Canvas();mini.drawChild(group.container,canvas,group.children.get(0),()->true);equal(128,canvas.circleAlpha,"badge fades with the spring");
        group.extension.groupExpansionProgress=Float.NaN;equal(152,height(mini,group.container,false),"NaN progress: closed endpoint still answered");
        mini.childrenUpdated(group.summary);
        group.extension.groupExpansionProgress=0f;group.summary.mGroupExpansionChanging=false;
        group.overlay.isUserLocked=true;
        Canvas locked=new Canvas();equal(true,mini.drawChild(group.container,locked,group.children.get(0),()->true),"user-locked: native draw");
        equal(0,locked.pathClips,"no deck clip while the user holds the group");
    }

    private static void clicks()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(3);mini.childrenUpdated(group.summary);
        equal(true,mini.click(group.children.get(0)),"front click expands");equal(1,group.expandClicks,"native expand listener");
        equal(false,mini.click(group.children.get(1)),"rear card: native click");equal(false,mini.click(group.summary),"summary: native click");
        group.children.get(0).mMenuRow=()->true;equal(false,mini.click(group.children.get(0)),"open menu stays native");group.children.get(0).mMenuRow=null;
        group.summary.mChildrenExpanded=true;equal(false,mini.click(group.children.get(0)),"expanded group: native click");group.summary.mChildrenExpanded=false;
        group.summary.attached=false;equal(false,mini.click(group.children.get(0)),"detached: native click");group.summary.attached=true;
        equal(false,mini.click(new View(null)),"non-row");equal(1,group.expandClicks,"no other expansions");
    }

    private static void handBack()throws Throwable{
        NotificationMiniStack mini=ready(false);replayTarget=mini;
        try {
            Group group=new Group(5);mini.childrenUpdated(group.summary);
            Row first=group.children.get(0),second=group.children.get(1),fourth=group.children.get(3);
            int updates=group.summary.childUpdates;
            mini.transition(NotificationMiniStack.SET_EXPANDED,group.summary,true,()->{group.summary.setChildrenExpanded(true);return null;});
            equal(updates+1,group.summary.childUpdates,"native states recomputed before the setter");
            // Everything this class owned is back to the host values.
            near(1f,second.mPrivateLayout.getAlpha(),"rear content restored");near(1f,group.summary.mPrivateLayout.getAlpha(),"summary content restored");
            near(1f,second.getAlpha(),"rear row alpha restored");near(1f,fourth.getAlpha(),"hidden row alpha restored");
            equal(View.VISIBLE,fourth.getVisibility(),"hidden row visible");equal(View.VISIBLE,group.header.getVisibility(),"header visible");
            equal(0,second.getImportantForAccessibility(),"accessibility restored");
            check(first.mPrivateLayout.mIsChildInGroup&&second.mPublicLayout.mIsChildInGroup,"group templates restored");
            equal(1,first.resets,"roundness handed back");equal(2,first.backgroundUpdates,"native background recomputed");
            // While expanded the endpoint is not re-applied, even through the replayed hook.
            mini.childrenUpdated(group.summary);near(1f,second.getAlpha(),"expanded group untouched");
            equal(true,mini.drawChild(group.container,new Canvas(),fourth,()->true),"expanded: every child drawn");
            int layouts=group.summary.layoutRequests;
            mini.transition(NotificationMiniStack.SET_EXPANDED,group.summary,false,()->{group.summary.setChildrenExpanded(false);return null;});
            equal(layouts+1,group.summary.layoutRequests,"collapse end re-lays out the stack");
            mini.childrenUpdated(group.summary);near(.55f,second.getAlpha(),"stack back after collapse");
            // Owner resolution for the container and extension setters; false values never hand back.
            mini.transition(NotificationMiniStack.CONTAINER_SWIPING,group.container,false,()->null);near(.55f,second.getAlpha(),"false swipe keeps stack");
            mini.transition(NotificationMiniStack.CONTAINER_SWIPING,group.container,true,()->null);near(1f,second.getAlpha(),"container swipe hands back");
            mini.childrenUpdated(group.summary);
            mini.transition(NotificationMiniStack.SET_ANIMATING,group.extension,true,()->null);near(1f,second.getAlpha(),"extension animation hands back");
            // Returning (not closed) state: the replayed native update must not re-apply the stack it just released.
            group.summary.mGroupExpansionChanging=true;group.extension.groupExpansionProgress=.5f;
            mini.childrenUpdated(group.summary);near(0f,group.summary.mPrivateLayout.getAlpha(),"returning stack applied");
            mini.transition(NotificationMiniStack.SET_CHANGING,group.summary,true,()->null);
            near(1f,group.summary.mPrivateLayout.getAlpha(),"replay leaves the group native");
        } finally { replayTarget=null; }
    }

    private static void switchOff()throws Throwable{
        NotificationMiniStack mini=ready(false);
        Group group=new Group(4);mini.childrenUpdated(group.summary);
        near(.55f,group.children.get(1).getAlpha(),"applied");
        mini.configure(settings(false,false));check(!mini.active(),"off immediately");
        near(.55f,group.children.get(1).getAlpha(),"release is posted to the main thread");
        Handler.drainForCheck();
        near(1f,group.children.get(1).getAlpha(),"restored on main");equal(View.VISIBLE,group.children.get(3).getVisibility(),"hidden row restored");
        near(1f,group.summary.mPrivateLayout.getAlpha(),"summary restored");check(group.children.get(0).mPrivateLayout.mIsChildInGroup,"template restored");
        equal(290,height(mini,group.container,false),"native height when off");equal(false,mini.click(group.children.get(0)),"native click when off");
        // Detach releases too; a host value changed meanwhile is not overwritten.
        NotificationMiniStack again=ready(false);Group other=new Group(3);again.childrenUpdated(other.summary);
        other.children.get(1).setAlpha(.7f);
        for(View.OnAttachStateChangeListener listener:new ArrayList<>(other.summary.attachListeners))listener.onViewDetachedFromWindow(other.summary);
        near(.7f,other.children.get(1).getAlpha(),"fresh host alpha kept");check(other.summary.attachListeners.isEmpty(),"listener removed");
        check(again.diagnosticSummary().contains("sessions=0"),"diagnostics hold counters only");
        check(!again.diagnosticSummary().contains("com."),"no package names in diagnostics");
    }

    private static void helpers(){
        View owner=new View(null),summary=new View(null),a=new View(null),b=new View(null);
        b.setVisibility(View.INVISIBLE);
        NotificationMiniStack.Backgrounds backgrounds=new NotificationMiniStack.Backgrounds();
        List<View> children=new ArrayList<>();children.add(a);children.add(b);
        backgrounds.apply(owner,summary,children);
        equal(View.INVISIBLE,summary.getVisibility(),"summary background hidden");equal(View.VISIBLE,b.getVisibility(),"child background shown");
        b.setVisibility(View.GONE);backgrounds.apply(owner,summary,children);equal(View.GONE,b.getVisibility(),"GONE stays host-owned");
        b.setVisibility(View.VISIBLE);backgrounds.apply(owner,summary,children);
        b.setVisibility(View.INVISIBLE);backgrounds.hostUpdated(b);backgrounds.apply(owner,summary,children);
        backgrounds.restore(owner);equal(View.VISIBLE,summary.getVisibility(),"summary restored");equal(View.INVISIBLE,b.getVisibility(),"latest host visibility restored");
        NotificationMiniStack.Visibility visibility=new NotificationMiniStack.Visibility();
        View gone=new View(null);gone.setVisibility(View.GONE);List<View> hide=new ArrayList<>();hide.add(a);hide.add(gone);
        visibility.hide(owner,hide,true);equal(View.INVISIBLE,a.getVisibility(),"hidden");equal(View.GONE,gone.getVisibility(),"GONE untouched");
        visibility.restore(owner);equal(View.VISIBLE,a.getVisibility(),"visibility restored");equal(View.GONE,gone.getVisibility(),"GONE still GONE");
        visibility.hide(owner,hide,true);a.setVisibility(View.GONE);visibility.restore(owner);equal(View.GONE,a.getVisibility(),"host GONE not overwritten");
    }

    private static void settingsPlumbing(){
        equal(false,StatusBarSettings.BOOLEAN_DEFAULTS.get(NotificationMiniStack.ENABLED),"default off");
        equal(false,StatusBarSettings.BOOLEAN_DEFAULTS.get(NotificationMiniStack.HIDE_BADGE),"badge default shown");
        check(!NotificationMiniStack.ENABLED.equals(NotificationGroupStack.MASTER),"new key, not the retired one");
        SettingsCatalog.Group page=SettingsCatalog.group("notification_mini_stack");
        check(page!=null&&SettingsCatalog.NOTIFICATION.equals(page.category),"page in the notification centre");
        equal("同应用小堆叠",page.title,"page title");check(page.independentSwitches,"labelled own switch");
        SettingsCatalog.Item master=SettingsCatalog.item(NotificationMiniStack.ENABLED);
        equal("启用小堆叠（同应用）",master.title,"switch label");check(!master.master,"switch is a visible, searchable row");
        check(page.description.startsWith(NotificationMiniStackSource.SHORT+"。"),"entry note is the short source declaration");
        boolean listed=false;
        for(SettingsCatalog.NavigationSection section:SettingsCatalog.navigationSections(SettingsCatalog.NOTIFICATION))
            for(SettingsCatalog.Group candidate:section.groups)if(candidate==page){listed=true;equal("通知堆叠",section.title,"section");}
        check(listed,"listed once under 通知堆叠");
        for(String category:new String[]{SettingsCatalog.STATUSBAR,SettingsCatalog.CONTROL_CENTER,SettingsCatalog.LOCK_SCREEN,SettingsCatalog.OTHER})
            for(SettingsCatalog.NavigationSection section:SettingsCatalog.navigationSections(category))check(!section.groups.contains(page),"no second home in "+category);
        java.util.Map<String,Object> values=new java.util.HashMap<>();
        equal("notification_mini_stack",SettingsCatalog.item(NotificationMiniStack.HIDE_BADGE).groupId,"badge setting returns on the mini-stack page");
        check(!SettingsCatalog.hiddenKeys().contains(NotificationMiniStack.HIDE_BADGE),"badge setting is no longer retired");
        check(SettingsCatalog.isVisible(NotificationMiniStack.HIDE_BADGE),"badge setting is reachable in the editor");
        equal("请先开启小堆叠（同应用）",SettingsCatalog.unavailableReason(NotificationMiniStack.HIDE_BADGE,values),"badge adjustment follows the mini-stack master");
        values.put(NotificationMiniStack.ENABLED,true);
        equal("",SettingsCatalog.unavailableReason(NotificationMiniStack.HIDE_BADGE,values),"badge adjustment is usable on an enabled mini-stack");
        values.put(NotificationMiniStack.HIDE_BADGE,true);
        equal(true,StatusBarSettings.bool(values,NotificationMiniStack.HIDE_BADGE),"stored true is read honestly");
        equal(true,SettingsSnapshot.fromPreferences(values).getBoolean(NotificationMiniStack.HIDE_BADGE),"stored true reaches the runtime snapshot");
        equal(true,SettingsSnapshot.runtimeCopy(settings(true,true)).getBoolean(NotificationMiniStack.HIDE_BADGE),"runtime copy retains the requested hide flag");
        Bundle complete=SettingsSnapshot.fromPreferences(values);
        java.util.Map<String,Object> wire=new java.util.LinkedHashMap<>();
        for(String key:complete.keySet())wire.put(key,complete.get(key));
        wire.put(SettingsFrameworkMirror.SCHEMA,SettingsFrameworkMirror.VERSION);
        wire.put(SettingsFrameworkMirror.REVISION,114L);
        equal(true,SettingsFrameworkMirror.decode(wire).getBoolean(NotificationMiniStack.HIDE_BADGE),"framework IPC decode retains requested hide flag");
        equal("",SettingsCatalog.unavailableReason(NotificationMiniStack.ENABLED,values),"no big-clock or fluid-cloud dependency");
        equal(ConfigTransfer.Type.BOOLEAN,ConfigTransfer.types().get(NotificationMiniStack.HIDE_BADGE),"legacy backups retain the Boolean type");
        String conflicts=SettingsCatalog.conflictDescription("notification_mini_stack",SettingsCatalog.NOTIFICATION);
        check(conflicts.contains("OplusX")&&conflicts.contains("xxxxawl")&&conflicts.contains("VivoGroupDeck test21"),"page help keeps both sources");
        Bundle saved=SettingsSnapshot.fromPreferences(new java.util.LinkedHashMap<>());saved.putBoolean(NotificationMiniStack.ENABLED,true);
        saved.putBoolean(StatusBarSettings.SAFE_MODE,true);
        equal(false,SafetyMode.runtimeSettings(saved).getBoolean(NotificationMiniStack.ENABLED,true),"safe mode switches it off");
        saved.putBoolean(StatusBarSettings.SAFE_MODE,false);
        equal(true,SafetyMode.runtimeSettings(saved).getBoolean(NotificationMiniStack.ENABLED,false),"kept outside safe mode");
    }

    public static void main(String[] args)throws Throwable{
        geometry();contract();inactive();eligibility();closedEndpoint();drawing();outerStackVisibility();cachedOuterStack();nativeBadges();badgeExpansionContinuity();nativeTemplateBadges();badgeLeaseLifecycle();stableBadgeTree();transitioningBadgeTree();returning();clicks();handBack();switchOff();helpers();settingsPlumbing();badgeUninstall();
        System.out.println("NotificationMiniStackCheck passed "+checks+" checks");
    }
}
