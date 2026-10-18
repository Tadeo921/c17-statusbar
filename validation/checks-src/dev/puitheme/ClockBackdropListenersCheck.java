package dev.puitheme;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Exercise real listener delivery without requiring a native blur request. */
public final class ClockBackdropListenersCheck {
    private static int checks;
    private static Method notify;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void fire()throws Exception{notify.invoke(null);}
    private static final class EqualListener implements Runnable {
        int calls;
        @Override public void run(){calls++;}
        @Override public boolean equals(Object other){return other instanceof EqualListener;}
        @Override public int hashCode(){return 1;}
    }
    public static void main(String[] args)throws Exception {
        notify=ClockBackdropSource.class.getDeclaredMethod("notifyInvalidation");notify.setAccessible(true);ClockBackdropSource.clear();
        final int[] clock={0},notification={0};Runnable clockListener=()->clock[0]++,notificationListener=()->notification[0]++;
        ClockBackdropSource.addInvalidationCallback(clockListener);ClockBackdropSource.addInvalidationCallback(notificationListener);fire();check(clock[0]==1&&notification[0]==1,"clock and notification share one native source event");
        ClockBackdropSource.addInvalidationCallback(clockListener);ClockBackdropSource.addInvalidationCallback(notificationListener);ClockBackdropSource.addInvalidationCallback(null);fire();check(clock[0]==2&&notification[0]==2,"adding same callback identity twice never duplicates delivery");
        EqualListener first=new EqualListener(),second=new EqualListener();ClockBackdropSource.addInvalidationCallback(first);ClockBackdropSource.addInvalidationCallback(second);fire();check(first.calls==1&&second.calls==1,"distinct callback identities survive even when equals says equal");
        ClockBackdropSource.clear();final int[] afterFailure={0};Runnable failing=()->{throw new IllegalStateException("one listener failed");},remaining=()->afterFailure[0]++;
        ClockBackdropSource.addInvalidationCallback(failing);ClockBackdropSource.addInvalidationCallback(remaining);fire();check(afterFailure[0]==1,"one listener exception cannot block later listeners");
        Field callbacks=ClockBackdropSource.class.getDeclaredField("invalidations");callbacks.setAccessible(true);Object[] references=(Object[])callbacks.get(null);check(references.length==2&&references[0] instanceof WeakReference&&((WeakReference<?>)references[0]).get()==failing,"native observer does not strongly retain listener owners");
        ((WeakReference<?>)references[0]).clear();ClockBackdropSource.addInvalidationCallback(clockListener);references=(Object[])callbacks.get(null);check(references.length==2,"registering another listener prunes a dead weak slot");
        ClockBackdropSource.setInvalidationCallback(notificationListener);int oldClock=clock[0],oldNotification=notification[0],oldRemaining=afterFailure[0];fire();check(clock[0]==oldClock&&afterFailure[0]==oldRemaining&&notification[0]==oldNotification+1,"legacy set callback replaces all prior listeners");
        ClockBackdropSource.setInvalidationCallback(null);fire();check(notification[0]==oldNotification+1,"legacy set null clears callbacks");
        ClockBackdropSource.addInvalidationCallback(clockListener);ClockBackdropSource.addInvalidationCallback(notificationListener);ClockBackdropSource.clear();fire();check(clock[0]==oldClock&&notification[0]==oldNotification+1,"lifecycle clear drops every listener");
        final int[] lateCalls={0};Runnable late=()->lateCalls[0]++;Runnable adding=()->ClockBackdropSource.addInvalidationCallback(late);ClockBackdropSource.addInvalidationCallback(adding);fire();check(lateCalls[0]==0,"listener registration while delivering does not mutate the current event");fire();check(lateCalls[0]==1,"new listener joins the next event once");
        ClockBackdropSource.clear();EqualListener[] retained=new EqualListener[12];for(int i=0;i<retained.length;i++){retained[i]=new EqualListener();ClockBackdropSource.addInvalidationCallback(retained[i]);}fire();for(int i=0;i<retained.length;i++)check(retained[i].calls==(i<8?1:0),"listener registry stays bounded without duplicate delivery");
        ClockBackdropSource.clear();System.out.println("ClockBackdropListenersCheck: "+checks+" checks passed");
    }
}
