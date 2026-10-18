package dev.puitheme;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceView;
import android.view.View;

/** Compare the physical native surface, and release only our Java associations. */
public final class FluidCloudSurfaceRootsCheck {
    private static int count;
    private static void check(boolean condition,String detail){count++;if(!condition)throw new AssertionError(detail);}
    public static void main(String[] args) {
        View root=new View(null);SurfaceView child=new SurfaceView();Object host=new Object();
        SurfaceControl a=new SurfaceControl("native-a",null),copy=new SurfaceControl("native-copy",null);
        copy.sameLayerForCheck=a;
        SurfaceControlViewHost.SurfacePackage first=new SurfaceControlViewHost.SurfacePackage(a),second=new SurfaceControlViewHost.SurfacePackage(copy);
        check(FluidCloudSurfaceRoots.contentOf(root)==root,"ordinary roots retain identity");
        check(FluidCloudSurfaceRoots.contentOf(child)==null,"unknown remote process has no invented root");
        FluidCloudSurfaceRoots.childPackage(child,second);FluidCloudSurfaceRoots.hostView(host,root);FluidCloudSurfaceRoots.hostPackage(host,first);
        check(FluidCloudSurfaceRoots.contentOf(child)==root,"copied package arriving first links physical surface");
        android.view.ViewGroup wrapper=new android.view.ViewGroup(null);wrapper.addView(child);
        check(FluidCloudSurfaceRoots.contains(wrapper,root),"nested native SurfaceView wrapper resolves remote media root");
        check(!FluidCloudSurfaceRoots.contains(wrapper,new View(null)),"unrelated media root is not borrowed");
        SurfaceView unrelated=new SurfaceView();
        FluidCloudSurfaceRoots.childPackage(unrelated,new SurfaceControlViewHost.SurfacePackage(new SurfaceControl("other",null)));
        check(FluidCloudSurfaceRoots.contentOf(unrelated)==null,"different native surface not borrowed");
        check(FluidCloudSurfaceRoots.childDetached(child)==root,"detach returns associated content for effect restore");
        check(FluidCloudSurfaceRoots.contentOf(child)==null,"detach clears released child root");
        check(!FluidCloudSurfaceRoots.contains(wrapper,root),"detached remote root cannot retain accent ownership");
        FluidCloudSurfaceRoots.childPackage(child,second);check(FluidCloudSurfaceRoots.contentOf(child)==root,"valid reattach binds again");
        FluidCloudSurfaceRoots.releaseHost(host);check(FluidCloudSurfaceRoots.contentOf(child)==null,"host release clears stale root");
        a.valid=false;FluidCloudSurfaceRoots.hostView(host,root);FluidCloudSurfaceRoots.hostPackage(host,first);
        FluidCloudSurfaceRoots.childPackage(child,first);
        check(FluidCloudSurfaceRoots.contentOf(child)==null,"same Java package cannot bypass native validity");
        a.valid=true;FluidCloudSurfaceRoots.hostPackage(host,first);check(FluidCloudSurfaceRoots.contentOf(child)==root,"transient invalid layer does not permanently disable native contract");
        FluidCloudSurfaceRoots.childPackage(child,null);check(FluidCloudSurfaceRoots.contentOf(child)==null,"clearing package revokes association");
        FluidCloudSurfaceRoots.releaseHost(host);FluidCloudSurfaceRoots.childDetached(unrelated);
        displayedNotificationOwnership();
        check(SurfaceControl.operationsForCheck.isEmpty(),"no native surface mutations, ownership or transactions");
        System.out.println("FluidCloudSurfaceRootsCheck: "+count+" checks passed");
    }
    private static void displayedNotificationOwnership() {
        com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow row=
                new com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow();
        android.view.ViewGroup wrapper=new android.view.ViewGroup(null);
        wrapper.parent=row;
        View direct=new View(null);wrapper.addView(direct);
        check(FluidCloudSurfaceRoots.notificationHosted(direct),"direct content with an exact native row ancestor is notification hosted");
        check(FluidCloudSurfaceRoots.notificationHosted(row),"exact native row itself owns its material");
        check(!FluidCloudSurfaceRoots.notificationHosted(null),"null has no displayed notification owner");
        wrapper.removeView(direct);
        check(!FluidCloudSurfaceRoots.notificationHosted(direct),"a detached direct root is no longer notification hosted");

        android.view.ViewGroup remote=new android.view.ViewGroup(null);
        View content=new View(null);remote.addView(content);
        Object nativeHost=new Object();
        SurfaceControl physical=new SurfaceControl("hosted-notification",null);
        SurfaceControl copy=new SurfaceControl("copied-notification",null);copy.sameLayerForCheck=physical;
        SurfaceControlViewHost.SurfacePackage pack=new SurfaceControlViewHost.SurfacePackage(physical);
        SurfaceControlViewHost.SurfacePackage copied=new SurfaceControlViewHost.SurfacePackage(copy);
        SurfaceView child=new SurfaceView();wrapper.addView(child);
        FluidCloudSurfaceRoots.hostView(nativeHost,remote);
        FluidCloudSurfaceRoots.hostPackage(nativeHost,pack);
        FluidCloudSurfaceRoots.childPackage(child,copied);
        com.android.systemui.plugins.custom.CustomRowEntry retained=new com.android.systemui.plugins.custom.CustomRowEntry();
        retained.immersiveCardView=child;row.setEntry(retained);
        check(FluidCloudSurfaceRoots.notificationHosted(content),"visible attached row SurfaceView owns its registered remote content");
        check(!FluidCloudSurfaceRoots.notificationHosted(new View(null)),"an unrelated remote root is never borrowed");
        child.shown=false;
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"a hidden row SurfaceView does not block floating appearance despite retained entry");
        child.shown=true;child.attached=false;
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"a detached row SurfaceView does not block floating appearance despite retained entry");
        child.attached=true;wrapper.removeView(child);
        android.view.ViewGroup floating=new android.view.ViewGroup(null);floating.addView(child);
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"a shared Surface moved to a floating host restores appearance without clearing entry");
        check(row.getEntry()==retained&&retained.immersiveCardView==child,"current display ownership leaves cached notification entry untouched");
        floating.removeView(child);wrapper.addView(child);
        check(FluidCloudSurfaceRoots.notificationHosted(content),"the same registered Surface returning to a visible row is notification hosted again");
        SurfaceView second=new SurfaceView();floating.addView(second);
        FluidCloudSurfaceRoots.childPackage(second,pack);
        child.shown=false;
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"a floating physical-surface copy plus a hidden row copy does not create a notification owner");
        child.shown=true;
        check(FluidCloudSurfaceRoots.notificationHosted(content),"a real visible row copy takes ownership while a floating copy is also registered");
        FluidCloudSurfaceRoots.childDetached(child);
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"a released row Surface association cannot borrow a floating copy");
        FluidCloudSurfaceRoots.childPackage(child,pack);
        check(FluidCloudSurfaceRoots.notificationHosted(content),"native rebind restores visible row ownership");
        FluidCloudSurfaceRoots.releaseHost(nativeHost);
        check(!FluidCloudSurfaceRoots.notificationHosted(content),"native host release revokes notification ownership immediately");
        FluidCloudSurfaceRoots.childDetached(child);FluidCloudSurfaceRoots.childDetached(second);
        for(int i=0;i<1000;i++)check(!FluidCloudSurfaceRoots.notificationHosted(content),"stable released lookups never recreate ownership");
    }
}
