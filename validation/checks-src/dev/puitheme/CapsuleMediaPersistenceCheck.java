package dev.puitheme;

import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import com.oplus.systemui.media.seedling.livealert.MediaLiveAlertController;

public final class CapsuleMediaPersistenceCheck {
    private static int checks;
    private static void check(boolean value, String reason) { checks++; if (!value) throw new AssertionError(reason); }
    private static Bundle setting(boolean enabled) { Bundle value=new Bundle(); value.putBoolean(CapsulePersistence.ENABLED,enabled);return value; }
    private static final class Rig {
        final CapsuleMediaPersistence persistence=new CapsuleMediaPersistence();
        final CapsuleMediaPersistence.Hooks hooks;
        final MediaLiveAlertController source=new MediaLiveAlertController();
        Rig() throws Exception { hooks=persistence.resolve(CapsuleMediaPersistenceCheck.class.getClassLoader());check(hooks!=null,"native contract resolves"); }
        void enabled(){persistence.configure(setting(true));}
        void timeout(){if(!persistence.beforeTimeout(hooks,source.timer()))source.timer().run();}
        void disabled(){persistence.configure(setting(false));Handler.drainForCheck();}
    }
    public static void main(String[] args) throws Exception {
        Rig nativeRig=new Rig();nativeRig.timeout();check(!nativeRig.source.showOnStatusBar&&nativeRig.source.sends==1,"default preserves native expiry");
        Rig retained=new Rig();retained.enabled();retained.timeout();
        check(retained.source.showOnStatusBar&&retained.source.sends==0,"resident mode keeps paused music capsule without resend");
        check(retained.hooks.timeout.getDeclaringClass().getName().endsWith("$release$1")&&retained.hooks.timeout.getReturnType()==void.class,"hook only audited Runnable");
        check(retained.hooks.inlineCallers.length==1&&retained.hooks.inlineCallers[0].getName().equals("sendLiveAlertData"),"native scheduler deoptimization is bounded");
        retained.disabled();check(!retained.source.showOnStatusBar&&retained.source.sends==1,"disable replays expired native callback once");
        retained.disabled();check(retained.source.sends==1,"repeat disable never repeats callback");
        Rig release=new Rig();release.enabled();Runnable manual=release.source.releaseTask();
        check(!release.persistence.beforeTimeout(release.hooks,manual),"shared release branch remains native");manual.run();
        check(release.source.releases==1&&!release.source.showOnStatusBar,"real release removes capsule");
        Rig unrelated=new Rig();unrelated.enabled();
        Runnable impostor=new MediaLiveAlertController.release$1(unrelated.source,1);
        check(!unrelated.persistence.beforeTimeout(unrelated.hooks,impostor),"classId alone cannot own a timeout");
        check(!unrelated.persistence.beforeTimeout(unrelated.hooks,(Runnable)()->{}),"unrelated Runnable stays native");
        check(!unrelated.persistence.beforeTimeout(null,unrelated.source.timer()),"unresolved hook stays native");
        Rig canceled=new Rig();canceled.enabled();canceled.timeout();canceled.source.destroy();canceled.disabled();
        check(canceled.source.sends==0&&!canceled.source.showOnStatusBar,"destroy after suppressed timeout cannot resurrect content");
        Rig manualClose=new Rig();manualClose.enabled();manualClose.timeout();manualClose.source.closeEvent();manualClose.disabled();
        check(manualClose.source.sends==0,"manual event close invalidates old expiry");
        Rig playing=new Rig();playing.enabled();playing.timeout();playing.source.playAgain();playing.disabled();
        check(playing.source.showOnStatusBar&&playing.source.sends==0,"resumed media is not hidden by old pause callback");
        Rig newPlayer=new Rig();newPlayer.enabled();newPlayer.timeout();newPlayer.source.playerChanged();newPlayer.disabled();
        check(newPlayer.source.showOnStatusBar&&newPlayer.source.sends==0,"new player is not hidden by stale expiry");
        Rig fresh=new Rig();fresh.enabled();fresh.timeout();fresh.source.handler().postDelayed(fresh.source.timer(),5000);fresh.disabled();
        check(fresh.source.showOnStatusBar&&fresh.source.sends==0,"a fresh native deadline is kept");
        fresh.source.handler().removeCallbacks(fresh.source.timer());fresh.timeout();check(!fresh.source.showOnStatusBar,"fresh native timeout still works after disabling");
        Rig toggle=new Rig();toggle.enabled();toggle.timeout();toggle.persistence.configure(setting(false));toggle.enabled();Handler.drainForCheck();
        check(toggle.source.showOnStatusBar&&toggle.source.sends==0,"re-enable gates already posted disable callback");
        toggle.disabled();check(!toggle.source.showOnStatusBar&&toggle.source.sends==1,"later disable restores exactly one callback");
        Rig repeated=new Rig();repeated.enabled();repeated.timeout();repeated.timeout();repeated.disabled();check(repeated.source.sends==1,"repeated suppressed runs share one current callback");
        Rig cleanup=new Rig();cleanup.enabled();cleanup.timeout();cleanup.persistence.release();Handler.drainForCheck();
        check(!cleanup.source.showOnStatusBar&&cleanup.source.sends==1,"release restores expired native behavior");
        check(cleanup.persistence.resolve(CapsuleMediaPersistenceCheck.class.getClassLoader())==null,"released helper cannot attach again");
        cleanup.enabled();check(!cleanup.persistence.beforeTimeout(cleanup.hooks,cleanup.source.timer()),"released helper stays native");
        Rig nullConfig=new Rig();nullConfig.enabled();nullConfig.timeout();nullConfig.persistence.configure(null);Handler.drainForCheck();
        check(!nullConfig.source.showOnStatusBar,"missing configuration restores native behavior");
        Rig hidden=new Rig();hidden.enabled();hidden.source.showOnStatusBar=false;
        check(!hidden.persistence.beforeTimeout(hidden.hooks,hidden.source.timer()),"already hidden capsule is never forced open");
        check(hidden.persistence.resolve(null)==null,"missing loader stays native");
        Rig safe=new Rig();safe.enabled();safe.timeout();Bundle safeMode=setting(true);safeMode.putBoolean(StatusBarSettings.SAFE_MODE,true);
        safe.persistence.configure(safeMode);Handler.drainForCheck();check(!safe.source.showOnStatusBar,"safe mode restores native expiry even when residency is saved");
        ClassLoader absent=new ClassLoader(null) { @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException { throw new ClassNotFoundException(name); } };
        try { hidden.persistence.resolve(absent);throw new AssertionError("unsupported ROM must reject exact contract"); }
        catch(ClassNotFoundException expected){checks++;}
        int sdk=Build.VERSION.SDK_INT;
        try {
            Rig old=new Rig();old.enabled();old.timeout();Build.VERSION.SDK_INT=28;
            check(old.persistence.resolve(CapsuleMediaPersistenceCheck.class.getClassLoader())==null,"API28 retains native contract without hasCallbacks");
            check(!old.persistence.beforeTimeout(old.hooks,old.source.timer()),"API28 never intercepts old-ROM callback");
            old.disabled();check(old.source.sends==0,"API28 does not invoke missing hasCallbacks during pending cleanup");
            old.enabled();check(!old.persistence.beforeTimeout(old.hooks,old.source.timer()),"API28 setting remains native");
        } finally {Build.VERSION.SDK_INT=sdk;}
        Rig restored=new Rig();restored.enabled();restored.timeout();check(restored.source.showOnStatusBar,"restoring supported SDK resolves residency again");
        System.out.println("CapsuleMediaPersistenceCheck passed "+checks+" checks");
    }
}
