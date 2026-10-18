package dev.puitheme;

import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.phone.StatusIconContainer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Replays the OEM phase boundary: slot allocation -> module target -> seeding animation -> apply.
 * The same native source belongs to a scene-local StatusIconContainer; a Phone ancestor is optional.
 * Actual phone animation timing and GPU output still require device acceptance. */
public final class SpeedScenePositionCheck {
    static int checks;
    static void check(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
    static void close(float expected,float actual,String why){check(Math.abs(expected-actual)<.02f,why+": "+expected+" != "+actual);}
    static String[] modes(){return new String[]{SpeedPosition.NATIVE,SpeedPosition.CELLULAR_LEFT,SpeedPosition.RIGHT_START};}
    static float option(SpeedRelativePositionCheck.Rig rig,String key){return ((Number)rig.options.get(key)).floatValue();}

    static final class Scene extends FrameLayout {
        final String name;
        final FrameLayout unrelatedClockHost=new FrameLayout(null);
        final TextView unrelatedClock=new TextView(null);
        int moved;
        Scene(String name){super(null);this.name=name;unrelatedClockHost.addView(unrelatedClock);addView(unrelatedClockHost);}
        @Override public int getLeft(){return moved;}
        // Similar IDs in a panel/keyguard do not make this the Phone's clock_for_fake host.
        @Override public View findViewById(int id){return id==2?unrelatedClock:id==3?unrelatedClockHost:id==4?this:null;}
    }

    static final class SceneRig {
        final SpeedRelativePositionCheck.Rig r;
        Scene scene;
        final String mode;
        final boolean adjacent;
        float beforeAnimationTarget,nativeAllocationBias;
        SceneRig(String name,String mode,boolean adjacent,boolean rtl)throws Exception{
            this.mode=mode;this.adjacent=adjacent;
            r=new SpeedRelativePositionCheck.Rig(mode,adjacent,rtl,49.25f);
            r.h.detached(r.speed);r.phone.removeView(r.p);
            scene=new Scene(name);scene.addView(r.p);r.h.attached(r.speed);
            r.p.constrainedWidth=640;r.p.paddingLeft=3;r.p.paddingRight=7;r.p.mIconSpacing=5;
        }
        void moveTo(Scene next){r.h.detached(r.speed);scene.removeView(r.p);scene=next;scene.addView(r.p);r.h.attached(r.speed);}
        float worldX(){return scene.getLeft()+scene.getTranslationX()+r.p.getLeft()+r.p.getTranslationX()+r.source.getXTranslation();}
        float edgeDistance(){return r.p.rtl&&!SpeedPosition.RIGHT_START.equals(mode)
                ?r.source.getXTranslation():r.p.getWidth()-r.source.getXTranslation();}
        float desiredTarget(){
            float density=r.speed.getResources().getDisplayMetrics().density;
            float endPadding=SpeedPosition.RIGHT_START.equals(mode)?r.p.getPaddingRight():r.p.getPaddingEnd();
            float edge=r.p.getWidth()-endPadding-option(r,SpeedPosition.EDGE_RESERVE)*density;
            float desired=adjacent?edge+r.p.mIconSpacing-option(r,SpeedPosition.SAFE_GAP)*density-r.speed.getWidth()
                    :edge-r.speed.getWidth()-r.speed.getPaddingStart()-r.speed.getPaddingEnd();
            if(r.p.rtl&&!SpeedPosition.RIGHT_START.equals(mode))desired=r.p.getWidth()-desired-r.speed.getWidth();
            return adjacent?desired:desired+option(r,"speed_offset_x")*density;
        }
        void pass(boolean wifiOn,boolean cellOn,int cellWidth,float seedingShift,boolean hiddenAfterPrepare,boolean invisibleBeforePrepare)throws Exception{
            r.wifi.shown=wifiOn;r.cell.shown=cellOn;r.cell.width=cellWidth;
            int width=r.p.extra+r.p.getPaddingLeft()+r.p.getPaddingRight(),count=0;
            try(SpeedPosition.Scope scope=r.h.begin(r.p,false)){
                check(scope!=null,scene.name+" must bind its own native status-icon container without a Phone ancestor");
                for(int i=0;i<r.p.getChildCount();i++){
                    View child=r.p.getChildAt(i);StatusIconDisplayable icon=(StatusIconDisplayable)child;
                    if(!icon.isIconVisible()||r.h.iconBlocked(child,icon.isIconBlocked())||r.p.mIgnoredSlots.contains(icon.getSlot()))continue;
                    width+=child.getMeasuredWidth()+child.getPaddingStart()+r.h.allocationPaddingEnd(child,child.getPaddingEnd());count++;
                }
            }
            r.p.width=r.p.constrainedWidth>0?r.p.constrainedWidth:width+Math.max(0,count-1)*r.p.mIconSpacing;
            r.speed.setAlpha(0f);r.speed.setTranslationX(-64f);r.speed.setTranslationY(11f);
            r.source.setAlpha(0f,"native fade-out");r.source.setYTranslation(47f,"native panel transition");
            try(SpeedPosition.Scope scope=r.h.begin(r.p,true)){
                check(scope!=null,scene.name+" target scope must be active");
                float x=r.p.getWidth()-r.p.getPaddingEnd();boolean previous=false;
                for(int i=r.p.getChildCount()-1;i>=0;i--){
                    View child=r.p.getChildAt(i);StatusIconDisplayable icon=(StatusIconDisplayable)child;
                    StatusIconContainer.StatusIconState state=(StatusIconContainer.StatusIconState)child.getTag(1);
                    if(!icon.isIconVisible()||r.h.iconBlocked(child,icon.isIconBlocked())||r.p.mIgnoredSlots.contains(icon.getSlot())){state.visibleState=2;continue;}
                    if(previous)x-=r.p.mIconSpacing;previous=true;
                    x-=child.getWidth()+child.getPaddingStart()+r.h.allocationPaddingEnd(child,child.getPaddingEnd());
                    state.visibleState=0;state.setXTranslation(x);
                }
                if(r.p.rtl)for(int i=0;i<r.p.getChildCount();i++){
                    View child=r.p.getChildAt(i);StatusIconContainer.StatusIconState state=(StatusIconContainer.StatusIconState)child.getTag(1);
                    state.setXTranslation(r.p.getWidth()-state.getXTranslation()-child.getWidth());
                }
                // Native constraint/underflow work need not match a reconstructed allocation sum.
                // Normalise its actual pre-animation target rather than retaining a guessed residual.
                r.source.setXTranslation(r.source.getXTranslation()+nativeAllocationBias);
                if(invisibleBeforePrepare)r.source.visibleState=2;
                float nativeTarget=r.source.getXTranslation();
                prepare(r.h,r.p);
                beforeAnimationTarget=r.source.getXTranslation();
                close(invisibleBeforePrepare?nativeTarget:desiredTarget(),beforeAnimationTarget,
                        scene.name+" module position must exist before the native seeding controller reads targets");
                close(0f,r.source.getAlpha(),"target preparation preserves native alpha even when fully faded");
                close(47f,r.source.getYTranslation(),"target preparation preserves native Y animation");
                close(-64f,r.speed.getTranslationX(),"target preparation cannot write actual view animation position");
                close(11f,r.speed.getTranslationY(),"target preparation cannot write actual view animation Y");
                close(0f,r.speed.getAlpha(),"target preparation cannot restore an invisible native source");
                if(invisibleBeforePrepare)check(r.source.visibleState==2,"already hidden native state remains hidden");

                // An OEM controller consumes the prepared target, then changes target/alpha/coord.
                // applyToView is too late to calculate the module's own relative position again.
                r.source.setXTranslation(beforeAnimationTarget+seedingShift);
                r.source.setYTranslation(59f,"native seeding-card controller");
                r.source.setAlpha(0f,"native seeding-card controller");
                if(hiddenAfterPrepare)r.source.visibleState=2;
                float cellAfterAnimation=r.cell.state.getXTranslation()+11.25f;
                float wifiAfterAnimation=r.wifi.state.getXTranslation()-19.5f;
                r.cell.state.setXTranslation(cellAfterAnimation);r.wifi.state.setXTranslation(wifiAfterAnimation);
                r.cell.state.setAlpha(0f,"native seeding-card controller");r.wifi.state.setAlpha(.25f,"native seeding-card controller");
                Field coordinate=coordinateField();coordinate.setFloat(r.speed,123.75f);
                for(int repeat=0;repeat<4;repeat++){
                    prepare(r.h,r.p);
                    for(int i=0;i<r.p.getChildCount();i++){
                        View child=r.p.getChildAt(i);r.h.beforeApply(child.getTag(1),child);
                    }
                    close(beforeAnimationTarget+seedingShift,r.source.getXTranslation(),"repeated prepare/apply preserves the OEM target displacement");
                    close(59f,r.source.getYTranslation(),"repeated prepare/apply preserves the OEM Y target");
                    close(0f,r.source.getAlpha(),"repeated prepare/apply preserves the OEM alpha target");
                    close(123.75f,coordinate.getFloat(r.speed),"repeated prepare/apply preserves NetworkSpeedView.transXForCoord");
                    close(cellAfterAnimation,r.cell.state.getXTranslation(),"repeated prepare/apply preserves sibling mobile animation target");
                    close(wifiAfterAnimation,r.wifi.state.getXTranslation(),"repeated prepare/apply preserves sibling WiFi animation target");
                    close(0f,r.cell.state.getAlpha(),"repeated prepare/apply preserves sibling mobile fade");
                    close(.25f,r.wifi.state.getAlpha(),"repeated prepare/apply preserves sibling WiFi fade");
                    if(hiddenAfterPrepare||invisibleBeforePrepare)check(r.source.visibleState==2,"seeding visibility must survive repeated native apply");
                }
            }
            close(-5.25f,option(r,"speed_offset_x"),"layout never rewrites the user's saved relative offset");
            check(r.speed.getParent()==r.p,"scene placement preserves the native source owner");
            check(r.p.getChildAt(0)==r.speed,"scene placement preserves the IconManager child index");
            check(scene.unrelatedClockHost.getChildCount()==1,"relative scene placement must not borrow a panel/keyguard clock");
            nativeCopy(31.5f);
        }
        void nativeCopy(float hostTranslation){
            // Native fake frames draw the original container with a host-specific translation.
            // Source draw must contribute zero extra X after its state target was allocated.
            float nativeDrawX=scene.getLeft()+scene.getTranslationX()+r.p.getLeft()+r.p.getTranslationX()+r.speed.getTranslationX();
            Canvas canvas=new Canvas();canvas.translate(hostTranslation+nativeDrawX,0f);
            canvas.translate(r.h.drawOffsetXdp(r.speed,option(r,"speed_offset_x"))*4f,0f);
            close(hostTranslation+nativeDrawX,canvas.translateX,"native fake-frame copy inherits the current native animation position without a second X offset");
        }
        void release(){r.close();}
    }

    static void prepare(SpeedPosition helper,StatusIconContainer parent)throws Exception{
        Method method;
        try{method=SpeedPosition.class.getMethod("prepareTargets",android.view.ViewGroup.class);}
        catch(NoSuchMethodException missing){throw new AssertionError("prepareTargets must run before OEM calculateIconTranslations, not only before applyToView",missing);}
        try{method.invoke(helper,parent);}
        catch(InvocationTargetException failed){Throwable cause=failed.getCause();if(cause instanceof Exception)throw (Exception)cause;throw failed;}
    }
    static Field coordinateField()throws Exception{
        try{return com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView.class.getField("transXForCoord");}
        catch(NoSuchFieldException missing){throw new AssertionError("the exact OEM NetworkSpeedView.transXForCoord field must be represented by the fixture",missing);}
    }

    static void allSceneRadioStates()throws Exception{
        for(String name:new String[]{"notification center","control center","keyguard"})for(String mode:modes())
            for(boolean adjacent:new boolean[]{false,true})for(boolean rtl:new boolean[]{false,true}){
                SceneRig rig=new SceneRig(name,mode,adjacent,rtl);
                rig.pass(true,true,80,0f,false,false);float home=rig.edgeDistance(),world=rig.worldX();
                rig.scene.moved=71;rig.scene.setTranslationX(13.5f);rig.r.p.setTranslationX(-7f);
                rig.pass(true,true,80,0f,false,false);close(world+77.5f,rig.worldX(),"scene and container movement carry the speed with their exact translations");
                close(home,rig.edgeDistance(),"scene movement preserves its relative edge distance");
                for(int state=0;state<4;state++)for(int width:new int[]{0,80,197,400}){
                    rig.pass((state&1)!=0,(state&2)!=0,width,0f,false,false);
                    close(home,rig.edgeDistance(),name+" WiFi/mobile visibility and width cannot change the selected scene-relative position");
                    rig.pass((state&1)!=0,(state&2)!=0,width,97.5f,true,false);
                    close(home+(rtl&&!SpeedPosition.RIGHT_START.equals(mode)?97.5f:-97.5f),rig.edgeDistance(),
                            "OEM seeding-card movement remains relative to its current scene");
                }
                rig.pass(true,true,197,38f,false,true);
                rig.nativeAllocationBias=33.25f;rig.pass(true,true,197,0f,false,false);
                close(home,rig.edgeDistance(),"pre-seeding native allocation differences must not survive as a guessed animation residual");
                rig.nativeAllocationBias=0f;
                rig.pass(false,true,400,0f,false,false);close(home,rig.edgeDistance(),"native show after hidden state returns to the selected scene-relative target");
                rig.release();
            }
    }
    static void sceneTransfer()throws Exception{
        SceneRig rig=new SceneRig("notification center",SpeedPosition.CELLULAR_LEFT,false,false);
        rig.pass(true,true,80,0f,false,false);float home=rig.edgeDistance();
        Scene control=new Scene("control center");control.moved=144;rig.moveTo(control);
        rig.r.p.constrainedWidth=812;rig.pass(false,true,400,0f,false,false);
        close(home,rig.edgeDistance(),"notification-to-control handoff uses the new scene's own relative edge");
        Scene keyguard=new Scene("keyguard");keyguard.moved=-83;rig.moveTo(keyguard);
        rig.r.p.constrainedWidth=552;rig.pass(false,false,0,0f,false,false);
        close(home,rig.edgeDistance(),"control-to-keyguard handoff cannot retain an earlier parent's absolute target");
        rig.release();
    }
    static void clockFallback()throws Exception{
        for(String mode:new String[]{SpeedPosition.CLOCK_LEFT,SpeedPosition.CLOCK_RIGHT}){
            SceneRig rig=new SceneRig("keyguard",mode,false,false);
            check(!rig.r.h.handlesHorizontalOffset(rig.r.speed),"non-Phone clock mode must explicitly use native fallback");
            close(-5.25f,rig.r.h.drawOffsetXdp(rig.r.speed,-5.25f),"native clock fallback retains saved draw offset instead of suppressing it because a source was captured");
            try(SpeedPosition.Scope scope=rig.r.h.begin(rig.r.p,true)){
                check(!rig.r.h.iconBlocked(rig.r.speed,false),"missing Phone clock cannot mask the native scene source");
            }
            check(rig.scene.unrelatedClockHost.getChildCount()==1,"matching non-Phone clock IDs cannot create a borrowed clock copy");
            close(-5.25f,option(rig.r,"speed_offset_x"),"clock fallback leaves stored position adjustment intact");
            rig.release();
        }
    }
    static void earlySceneOwnerRebind()throws Exception{
        for(String mode:new String[]{SpeedPosition.CLOCK_LEFT,SpeedPosition.CLOCK_RIGHT}){
            final int[] created={0};
            SpeedPosition helper=new SpeedPosition((source,host,layout)->{
                created[0]++;
                return new com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView();
            });
            helper.resolve(SpeedScenePositionCheck.class.getClassLoader());
            SpeedRelativePositionCheck.Parent parent=new SpeedRelativePositionCheck.Parent();parent.width=640;
            com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView speed=
                    new com.oplus.systemui.statusbar.phone.netspeed.widget.NetworkSpeedView();
            speed.applyNativeConfiguration(88,36,44);speed.getResources().getDisplayMetrics().density=4f;
            StatusIconContainer.StatusIconState state=new StatusIconContainer.StatusIconState();speed.tags.put(1,state);
            com.oplus.systemui.statusbar.phone.netspeed.NetworkSpeedIconState model=
                    new com.oplus.systemui.statusbar.phone.netspeed.NetworkSpeedIconState();model.visible=true;speed.applyNetworkState(model);
            parent.addView(speed);Scene early=new Scene("early inflation");early.addView(parent);
            helper.attached(speed);helper.setHooksReady(true);helper.setAllocationReady(true);
            android.os.Bundle options=new android.os.Bundle();options.putBoolean("speed_enabled",true);
            options.putBoolean("speed_position_enabled",true);options.putString(SpeedPosition.POSITION,mode);options.putFloat("speed_offset_x",-5.25f);
            helper.configure(options);
            check(parent.layoutListeners.size()==1,"early scene binding owns exactly its container's layout listener");
            check(!helper.handlesHorizontalOffset(speed),"early binding without a Phone uses clock fallback");
            close(-5.25f,helper.drawOffsetXdp(speed,-5.25f),"early clock fallback preserves the saved draw offset");

            SpeedRelativePositionCheck.Phone phone=new SpeedRelativePositionCheck.Phone();
            SpeedRelativePositionCheck.ClockFrame host=new SpeedRelativePositionCheck.ClockFrame();
            TextView clock=new TextView(null){@Override public int getMeasuredWidth(){return 50;}};
            FrameLayout start=new FrameLayout(null){@Override public int getWidth(){return 1000;}};
            phone.addView(start);start.addView(host);host.addView(clock);phone.ids.put(2,clock);phone.ids.put(3,host);phone.ids.put(4,start);
            early.removeView(parent);phone.addView(parent);
            // Native inflation/attachment reports the newly available ancestor without a manual module detach.
            helper.attached(speed);
            check(host.getChildCount()==2&&created[0]==1,"late Phone ancestor creates exactly one actual clock mirror");
            check(parent.layoutListeners.isEmpty()&&phone.layoutListeners.size()==1,"late Phone attachment moves listener ownership off the early container");
            check(helper.handlesHorizontalOffset(speed),"late Phone attachment activates clock allocation");
            close(0f,helper.drawOffsetXdp(speed,-5.25f),"active clock mirror suppresses a second source draw offset");
            try(SpeedPosition.Scope scope=helper.begin(parent,true)){
                check(scope!=null&&helper.iconBlocked(speed,false),"active Phone clock mirror masks only the native source allocation");
            }
            View firstCopy=host.getChildAt(1);

            phone.removeView(parent);early.addView(parent);helper.changed(parent);
            check(host.getChildCount()==1&&firstCopy.getParent()==null,"Phone-to-panel rebind removes the exact old clock mirror");
            check(host.left==3&&host.right==5,"Phone-to-panel rebind restores the clock's native padding");
            check(phone.layoutListeners.isEmpty()&&parent.layoutListeners.size()==1,"Phone-to-panel rebind releases old Phone listener ownership");
            check(!helper.handlesHorizontalOffset(speed)&&!helper.iconBlocked(speed,false),"Phone-to-panel rebind keeps the original source available");
            close(-5.25f,helper.drawOffsetXdp(speed,-5.25f),"Phone-to-panel rebind restores saved fallback offset");
            close(-5.25f,((Number)options.get("speed_offset_x")).floatValue(),"scene ownership changes never rewrite saved adjustment");

            early.removeView(parent);phone.addView(parent);helper.changed(parent);
            check(host.getChildCount()==2&&created[0]==2,"returning to Phone creates one new mirror without accumulating stale copies");
            check(parent.layoutListeners.isEmpty()&&phone.layoutListeners.size()==1,"returning to Phone owns one current listener");
            helper.releaseRuntime();
            check(host.getChildCount()==1&&phone.layoutListeners.isEmpty()&&parent.layoutListeners.isEmpty(),"release after ownership changes cleans mirrors and all listeners");
            speed.getResources().getDisplayMetrics().density=1f;
        }
    }
    public static void main(String[] args)throws Exception{
        allSceneRadioStates();sceneTransfer();clockFallback();earlySceneOwnerRebind();
        System.out.println("SpeedScenePositionCheck: "+checks+" checks passed (scene-local parents, pre-seeding targets, native animation ownership, copy offsets, clock fallback)");
    }
}
