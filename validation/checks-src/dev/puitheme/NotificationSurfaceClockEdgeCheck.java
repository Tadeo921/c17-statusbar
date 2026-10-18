package dev.puitheme;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceView;
import android.view.ViewGroupOverlay;
import android.os.Handler;
import com.android.systemui.plugins.custom.CustomRowEntry;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.systemui.notification.blur.OplusNotificationTiltShiftBlurContainer;

public final class NotificationSurfaceClockEdgeCheck {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void apply(View stack,float top,float bottom,float radius,boolean enabled){
        NotificationSurfaceClockEdge.update(stack,top,bottom,radius,enabled);Handler.drainForCheck();
    }
    public static void main(String[] args) throws Exception {
        ViewGroup parent=new ViewGroup(null),stack=new ViewGroup(null);parent.addView(stack);
        OplusCustomRow row=new OplusCustomRow();stack.addView(row);
        SurfaceView external=new SurfaceView();external.parent=row;external.setTranslationY(40f);
        CustomRowEntry entry=new CustomRowEntry();entry.miniCardView=external;row.setEntry(entry);
        View nativeOverlay=new View(null);parent.getOverlay().add(nativeOverlay);
        int nativeChildren=parent.getChildCount();
        NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);
        NotificationSurfaceClockEdge.update(stack,50f,90f,8f,true);
        ViewGroupOverlay overlays=parent.getOverlay();
        require(overlays.views.size()==1,"draw does not synchronously add native overlay children");
        Handler.drainForCheck();
        require(overlays.views.size()==2,"external Surface gets native OEM effect overlay");
        View owned=overlays.views.get(1);
        require(owned instanceof OplusNotificationTiltShiftBlurContainer,"uses audited native TiltShift container");
        require(parent.getChildCount()==nativeChildren&&stack.getChildCount()==1,"native stack layout children are untouched");
        require(owned.getMeasuredWidth()==124&&owned.getMeasuredHeight()==40,"native backdrop scoped to exact narrow band");
        require(owned.getTop()==50&&owned.getLeft()==0,"owned view aligned to header boundary in parent coordinates");
        require(!owned.isClickable()&&!owned.isFocusable(),"passive overlay does not intercept notification interaction");
        require(com.oplus.graphics.OplusRenderEffect.start==8f&&com.oplus.graphics.OplusRenderEffect.end==0f
                &&com.oplus.graphics.OplusRenderEffect.vertical&&com.oplus.graphics.OplusRenderEffect.speed==4f,"native gradient uses exact system contract");
        require(((OplusNotificationTiltShiftBlurContainer)owned).backgroundEffect!=null,"native background effect applied rather than RuntimeShader to external app");
        require(external.renderEffect==null&&external.getAlpha()==1f&&row.getAlpha()==1f,"external buffer alpha/effect unchanged");
        int effects=com.oplus.graphics.OplusRenderEffect.created,layouts=owned.layoutCalls;
        apply(stack,50f,90f,8f,true);
        require(owned==overlays.views.get(1)&&com.oplus.graphics.OplusRenderEffect.created==effects,"unchanged geometry reuses native view and effect");
        require(owned.layoutCalls==layouts,"unchanged geometry does not relayout");
        external.setTranslationY(60f);NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);
        apply(stack,50f,90f,8f,true);
        require(owned.getTop()==60&&owned.getMeasuredHeight()==30,"partial card intersection excludes wallpaper outside its pixels");
        require(com.oplus.graphics.OplusRenderEffect.start==6f,"partial band starts at matching intermediate radius");
        require(overlays.views.contains(nativeOverlay),"original system overlay remains present");
        apply(stack,50f,90f,8f,false);
        require(overlays.views.size()==1&&overlays.views.get(0)==nativeOverlay,"paging/off removes only module view");
        require(((OplusNotificationTiltShiftBlurContainer)owned).backgroundEffect==null,"owned native effect cleared before detach");
        NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);apply(stack,50f,90f,8f,true);
        require(overlays.views.size()==2,"return restores bounded owned native overlay");
        NotificationClockEdge.detach(stack);require(overlays.views.size()==1,"stack detach removes OEM overlay");
        NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);apply(stack,50f,90f,8f,true);
        NotificationClockEdge.releaseResources();require(overlays.views.size()==1,"global disable releases OEM overlay");
        external.setTranslationY(100f);NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);
        apply(stack,50f,90f,8f,true);
        require(overlays.views.size()==1,"fully clear Surface does not fog wallpaper or native card");
        NotificationRemoteClockEdge.releaseStack(stack);NotificationSurfaceClockEdge.release(stack);
        require(overlays.views.size()==1,"repeat cleanup is safe");
        // More than four unrelated visible Surfaces must not consume the in-band budget.
        for(int i=0;i<16;i++){
            OplusCustomRow extra=new OplusCustomRow();stack.addView(extra);
            SurfaceView surface=new SurfaceView();surface.parent=extra;surface.setTranslationY(i<12?200f:40f);
            CustomRowEntry other=new CustomRowEntry();other.miniCardView=surface;extra.setEntry(other);
        }
        NotificationRemoteClockEdge.update(stack,50f,50f,90f,8f,true);Handler.drainForCheck();
        NotificationSurfaceClockEdge.update(stack,50f,90f,8f,true);
        NotificationSurfaceClockEdge.update(stack,55f,95f,8f,true);
        require(Handler.postedForCheck.size()==1,"many draw calls coalesce native overlay work");
        require(overlays.views.size()==1,"queued native work leaves draw tree untouched");Handler.drainForCheck();
        require(overlays.views.size()==5,"four intersecting cards selected despite many out-of-band surfaces");
        for(View value:overlays.views)if(value!=nativeOverlay)require(value.getTop()==55,"latest band wins over earlier queued geometry");
        // Emulate cleared weak source references without relying on nondeterministic desktop GC.
        java.lang.reflect.Field statesField=NotificationSurfaceClockEdge.class.getDeclaredField("states");statesField.setAccessible(true);
        Object state=((java.util.Map<?,?>)statesField.get(null)).get(stack);
        java.lang.reflect.Field ownedField=state.getClass().getDeclaredField("overlays");ownedField.setAccessible(true);
        java.util.List<?> records=(java.util.List<?>)ownedField.get(state);View stale=overlays.views.get(1);
        for(Object record:records){java.lang.reflect.Field sourceField=record.getClass().getDeclaredField("source");sourceField.setAccessible(true);
            ((java.lang.ref.WeakReference<?>)sourceField.get(record)).clear();}
        apply(stack,55f,95f,8f,true);
        require(overlays.views.size()==5&&!overlays.views.contains(stale),"collected Surface cleanup record cannot silently orphan its native overlay");
        require(((OplusNotificationTiltShiftBlurContainer)stale).backgroundEffect==null,"orphaned native material cleared during replacement");
        NotificationSurfaceClockEdge.release(stack);
        NotificationSurfaceClockEdge.update(stack,50f,90f,8f,true);NotificationSurfaceClockEdge.release(stack);Handler.drainForCheck();
        require(overlays.views.size()==1,"detach cancels stale queued overlays");
        NotificationRemoteClockEdge.releaseStack(stack);NotificationSurfaceClockEdge.releaseResources();
        System.out.println("Notification native Surface edge checks passed: "+checks);
    }
}
