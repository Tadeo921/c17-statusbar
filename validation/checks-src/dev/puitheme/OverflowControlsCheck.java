package dev.puitheme;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/** Shared ancestor restoration, changing native trees, and weak owner cleanup. */
public final class OverflowControlsCheck {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value)throw new AssertionError(message); }
    private static OverflowControls registry() throws Exception {
        Constructor<OverflowControls> constructor=OverflowControls.class.getDeclaredConstructor();
        constructor.setAccessible(true);return constructor.newInstance();
    }
    private static void bounds(Rect expected,Rect actual,String message) {
        check(expected==null?actual==null:actual!=null&&expected.left==actual.left&&expected.top==actual.top
                &&expected.right==actual.right&&expected.bottom==actual.bottom,message);
    }
    public static void main(String[] args) throws Exception {
        check(OverflowControls.shared()==OverflowControls.shared(),"registry is not shared");
        sharedAncestors();changingTrees();depthLimits();weakOwners();preservedClipBounds();nativeAnimationBoundary();legacyNetworkClipping();mixedLegacyNetworkOwners();
        System.out.println("OverflowControlsCheck passed: "+checks);
    }
    private static void nativeAnimationBoundary() throws Exception {
        OverflowControls registry=registry();ViewGroup outer=new ViewGroup(null);
        com.android.systemui.statusbar.phone.StatusIconContainer row=new com.android.systemui.statusbar.phone.StatusIconContainer();
        ViewGroup host=new ViewGroup(null),wrapper=new ViewGroup(null);View text=new View(null);
        outer.addView(row);row.addView(host);host.addView(wrapper);wrapper.addView(text);
        Rect mask=new Rect(4,5,80,90);row.setClipBounds(mask);
        registry.acquire(host,true,true);
        check(!host.getClipChildren()&&!wrapper.getClipChildren(),"glyph wrappers still clip the configured ink");
        check(row.getClipChildren()&&row.getClipToPadding(),"native allocation row lost its clipping contract");
        check(outer.getClipChildren()&&outer.getClipToPadding(),"overflow escaped the native animation boundary");
        bounds(mask,row.getClipBounds(),"native animation boundary lost its explicit clip");
        // Even release must not write flags that this owner never changed.
        row.setClipChildren(false);outer.setClipToPadding(false);row.setClipBounds(new Rect(8,9,50,60));
        registry.release(host);
        check(!row.getClipChildren()&&!outer.getClipToPadding(),"release restored stale flags on untouched system containers");
        bounds(new Rect(8,9,50,60),row.getClipBounds(),"release restored stale system bounds");
        check(host.getClipChildren()&&wrapper.getClipChildren(),"glyph-local flags were not restored");
    }
    /** Components using preserving leases leave native clip bounds alone, including their later changes. */
    private static void preservedClipBounds() throws Exception {
        OverflowControls registry=registry();
        ViewGroup parent=new ViewGroup(null),speed=new ViewGroup(null);View text=new View(null);
        parent.addView(speed);speed.addView(text);
        Rect mask=new Rect(5,6,70,80),own=new Rect(1,1,30,30),leaf=new Rect(2,2,8,8);
        parent.setClipBounds(mask);speed.setClipBounds(own);text.setClipBounds(leaf);
        registry.acquire(speed,true,true);
        check(!parent.getClipChildren()&&!parent.getClipToPadding(),"preserving lease did not relax the ancestor flags");
        check(!speed.getClipChildren()&&!speed.getClipToPadding(),"preserving lease did not relax the owner flags");
        bounds(mask,parent.getClipBounds(),"ancestor clip bounds cleared");
        bounds(own,speed.getClipBounds(),"owner clip bounds cleared");
        bounds(leaf,text.getClipBounds(),"leaf clip bounds cleared");
        // The system animates its mask; a repeated acquisition (every draw) neither clears nor reverts it.
        Rect next=new Rect(9,9,40,40);parent.setClipBounds(next);speed.setClipBounds(null);
        registry.acquire(speed,true,true);
        bounds(next,parent.getClipBounds(),"animated ancestor mask overwritten");bounds(null,speed.getClipBounds(),"owner bounds invented");
        Rect last=new Rect(11,11,50,50);parent.setClipBounds(last);speed.setClipBounds(new Rect(3,3,20,20));
        registry.release(speed);
        bounds(last,parent.getClipBounds(),"release wrote a stale ancestor rectangle");
        bounds(new Rect(3,3,20,20),speed.getClipBounds(),"release wrote a stale owner rectangle");
        check(parent.getClipChildren()&&parent.getClipToPadding()&&speed.getClipChildren(),"flags not restored");
        // A clearing lease and a preserving lease on the same ancestor: bounds come back once, and only because somebody cleared them.
        ViewGroup row=new ViewGroup(null),clock=new ViewGroup(null),speedView=new ViewGroup(null);
        row.addView(clock);row.addView(speedView);Rect rowMask=new Rect(4,4,60,60);row.setClipBounds(rowMask);
        registry.acquire(speedView,false,true);bounds(rowMask,row.getClipBounds(),"preserving lease cleared the shared row");
        registry.acquire(clock,false);bounds(null,row.getClipBounds(),"clearing lease did not clear");
        registry.release(clock);bounds(null,row.getClipBounds(),"restored while the preserving lease is still active");
        registry.release(speedView);bounds(rowMask,row.getClipBounds(),"last release did not restore the cleared bounds");
        // Switching a lease between modes re-leases without leaking.
        ViewGroup solo=new ViewGroup(null),child=new ViewGroup(null);solo.addView(child);solo.setClipBounds(new Rect(1,1,9,9));
        registry.acquire(child,false);bounds(null,solo.getClipBounds(),"clearing lease did not clear");
        registry.acquire(child,false,true);bounds(new Rect(1,1,9,9),solo.getClipBounds(),"mode switch did not restore the bounds");
        registry.release(child);bounds(new Rect(1,1,9,9),solo.getClipBounds(),"release after mode switch changed the bounds");
        check(solo.getClipChildren(),"flags leaked after mode switch");
    }
    private static void legacyNetworkClipping() throws Exception {
        OverflowControls registry=registry();ViewGroup outer=new ViewGroup(null);
        com.android.systemui.statusbar.phone.StatusIconContainer row=new com.android.systemui.statusbar.phone.StatusIconContainer();
        ViewGroup speed=new ViewGroup(null);View text=new View(null);outer.addView(row);row.addView(speed);speed.addView(text);
        Rect rowMask=new Rect(3,4,70,80),speedMask=new Rect(1,2,30,40),textMask=new Rect(5,6,10,11);
        row.setClipBounds(rowMask);speed.setClipBounds(speedMask);text.setClipBounds(textMask);
        registry.acquireLegacyNetwork(speed,true);
        bounds(null,row.getClipBounds(),"legacy speed retained row mask");
        bounds(null,speed.getClipBounds(),"legacy speed retained owner mask");
        bounds(null,text.getClipBounds(),"legacy speed retained child mask");
        check(!row.getClipChildren()&&!row.getClipToPadding()&&!outer.getClipChildren(),"legacy speed stopped at the newer animation boundary");
        row.setClipBounds(new Rect(8,9,20,30));row.setClipChildren(true);registry.acquireLegacyNetwork(speed,true);
        bounds(null,row.getClipBounds(),"legacy repeat failed to clear updated mask");check(!row.getClipChildren(),"legacy repeat failed to clear flags");
        registry.release(speed);
        bounds(rowMask,row.getClipBounds(),"legacy speed did not restore original row mask");
        bounds(speedMask,speed.getClipBounds(),"legacy speed did not restore owner mask");
        bounds(textMask,text.getClipBounds(),"legacy speed did not restore child mask");
        check(row.getClipChildren()&&row.getClipToPadding()&&outer.getClipChildren(),"legacy speed did not restore the full flags");
        // 1.8.2 restores its initial snapshot even for fields that were already relaxed when the lease began.
        ViewGroup relaxed=new ViewGroup(null);relaxed.setClipChildren(false);relaxed.setClipToPadding(false);
        registry.acquireLegacyNetwork(relaxed,false);relaxed.setClipBounds(new Rect(7,8,9,10));relaxed.setClipChildren(true);relaxed.setClipToPadding(true);
        registry.release(relaxed);bounds(null,relaxed.getClipBounds(),"legacy null snapshot was not restored");
        check(!relaxed.getClipChildren()&&!relaxed.getClipToPadding(),"legacy relaxed snapshot was not restored");
        registry.acquire(relaxed,false,true);relaxed.setClipBounds(new Rect(11,12,13,14));relaxed.setClipChildren(true);relaxed.setClipToPadding(true);
        registry.release(relaxed);bounds(new Rect(11,12,13,14),relaxed.getClipBounds(),"modern preserving lease changed because speed was reverted");
        check(relaxed.getClipChildren()&&relaxed.getClipToPadding(),"modern unowned flags changed because speed was reverted");
        registry.acquireLegacyNetwork(null,true);registry.release(null);
    }
    private static void mixedLegacyNetworkOwners() throws Exception {
        for(boolean legacyFirst:new boolean[]{false,true})for(boolean releaseLegacyFirst:new boolean[]{false,true}){
            OverflowControls registry=registry();ViewGroup row=new ViewGroup(null),clock=new ViewGroup(null),network=new ViewGroup(null);
            row.addView(clock);row.addView(network);Rect mask=new Rect(4,5,70,80);row.setClipBounds(mask);
            if(legacyFirst){registry.acquireLegacyNetwork(network,true);registry.acquire(clock,true,true);}
            else{registry.acquire(clock,true,true);registry.acquireLegacyNetwork(network,true);}
            bounds(null,row.getClipBounds(),"mixed legacy network did not clear shared mask");
            check(!row.getClipChildren()&&!clock.getClipChildren()&&!network.getClipChildren(),"mixed owners did not retain their leases");
            registry.release(releaseLegacyFirst?network:clock);
            bounds(null,row.getClipBounds(),"mixed first release restored a shared ancestor prematurely");
            check(!row.getClipChildren(),"mixed first release restored shared flags prematurely");
            ViewGroup remaining=releaseLegacyFirst?clock:network;
            check(!remaining.getClipChildren(),"mixed release disturbed the remaining owner's local wrapper");
            registry.release(remaining);bounds(mask,row.getClipBounds(),"mixed final release did not restore native shared mask");
            check(row.getClipChildren()&&clock.getClipChildren()&&network.getClipChildren(),"mixed final release leaked flags");
        }
        OverflowControls registry=registry();ViewGroup group=new ViewGroup(null);Rect mask=new Rect(1,2,30,40);group.setClipBounds(mask);
        registry.acquire(group,true,true);bounds(mask,group.getClipBounds(),"modern mode cleared original mask");
        registry.acquireLegacyNetwork(group,true);bounds(null,group.getClipBounds(),"legacy mode switch did not clear mask");
        registry.acquire(group,true,true);bounds(mask,group.getClipBounds(),"modern mode switch did not restore legacy mask");
        registry.release(group);bounds(mask,group.getClipBounds(),"mode switch final release changed native mask");
        check(group.getClipChildren()&&group.getClipToPadding(),"mode switch left owned flags behind");
    }
    private static void sharedAncestors() throws Exception {
        OverflowControls registry=registry();
        ViewGroup parent=new ViewGroup(null),network=new ViewGroup(null),battery=new ViewGroup(null);
        parent.addView(network);parent.addView(battery);
        Rect original=new Rect(1,2,90,70);parent.setClipBounds(original);
        network.setClipToPadding(false);battery.setClipChildren(false);
        registry.acquire(network,true);registry.acquire(battery,false);
        check(!parent.getClipChildren()&&!parent.getClipToPadding(),"shared parent remains clipped");
        bounds(null,parent.getClipBounds(),"parent clip bounds remain");
        registry.acquire(network,true);registry.acquire(network,true);
        registry.release(network);
        check(!parent.getClipChildren(),"network release restored battery ancestor too early");
        check(network.getClipChildren()&&!network.getClipToPadding(),"network flags did not restore exactly");
        original.right=1000;
        registry.release(battery);
        check(parent.getClipChildren()&&parent.getClipToPadding(),"last release did not restore shared parent");
        bounds(new Rect(1,2,90,70),parent.getClipBounds(),"snapshot aliased caller's mutable rectangle");
        check(!battery.getClipChildren()&&battery.getClipToPadding(),"original mixed battery flags changed");
        registry.release(battery);bounds(new Rect(1,2,90,70),parent.getClipBounds(),"duplicate release changed parent");
    }
    private static void changingTrees() throws Exception {
        OverflowControls registry=registry();
        ViewGroup parent=new ViewGroup(null),host=new ViewGroup(null),compose=new ViewGroup(null);
        View child=new View(null);parent.addView(host);host.addView(compose);compose.addView(child);
        Rect clip=new Rect(2,3,40,50);child.setClipBounds(clip);
        registry.acquire(host,true);
        check(!compose.getClipChildren(),"nested native Compose container clips");
        bounds(null,child.getClipBounds(),"leaf clip bounds remain");
        compose.setClipChildren(true);child.setClipBounds(clip);
        registry.acquire(host,true);
        check(!compose.getClipChildren(),"native flag reset was not refreshed");
        bounds(null,child.getClipBounds(),"native bound reset was not refreshed");
        registry.acquire(host,false);
        check(compose.getClipChildren(),"removed child-tree lease did not restore descendant");
        bounds(clip,child.getClipBounds(),"removed leaf did not restore bounds");
        check(!host.getClipChildren()&&!parent.getClipChildren(),"ancestor lease vanished when tree mode changed");
        ViewGroup nextParent=new ViewGroup(null);host.parent=nextParent;
        registry.acquire(host,false);
        check(parent.getClipChildren(),"old ancestor retained a lease after reparenting");
        check(!nextParent.getClipChildren(),"new ancestor is clipped");
        registry.release(host);
        check(host.getClipChildren()&&nextParent.getClipChildren(),"reparented lease did not restore");
    }
    private static void depthLimits() throws Exception {
        OverflowControls registry=registry();ViewGroup owner=new ViewGroup(null);
        ViewGroup[] descendants=new ViewGroup[10];ViewGroup current=owner;
        for(int index=0;index<descendants.length;index++){descendants[index]=new ViewGroup(null);current.addView(descendants[index]);current=descendants[index];}
        ViewGroup[] ancestors=new ViewGroup[14];current=owner;
        for(int index=0;index<ancestors.length;index++){ancestors[index]=new ViewGroup(null);current.parent=ancestors[index];current=ancestors[index];}
        registry.acquire(owner,true);
        check(!descendants[7].getClipChildren(),"eighth descendant excluded");
        check(descendants[8].getClipChildren(),"child depth exceeded eight");
        check(!ancestors[11].getClipChildren(),"twelfth ancestor excluded");
        check(ancestors[12].getClipChildren(),"ancestor depth exceeded twelve");
        registry.release(owner);
        for(ViewGroup group:descendants)check(group.getClipChildren(),"descendant not restored");
        for(ViewGroup group:ancestors)check(group.getClipChildren(),"ancestor not restored");
        registry.acquire(null,true);registry.release(null);
    }
    private static void weakOwners() throws Exception {
        OverflowControls registry=registry();ViewGroup parent=new ViewGroup(null);View owner=new View(null);
        owner.parent=parent;registry.acquire(owner,false);
        Field leases=OverflowControls.class.getDeclaredField("leases");leases.setAccessible(true);
        Object lease=((List<?>)leases.get(registry)).get(0);
        Field ownerField=lease.getClass().getDeclaredField("owner");ownerField.setAccessible(true);
        ((WeakReference<?>)ownerField.get(lease)).clear();
        // Deterministically simulate GC clearing the owner; a later API call must release its ancestors.
        registry.release(null);
        check(parent.getClipChildren()&&parent.getClipToPadding(),"collected owner leaked ancestor flags");
        check(((List<?>)leases.get(registry)).isEmpty(),"collected owner lease retained");
        registry.release(owner);
    }
}
