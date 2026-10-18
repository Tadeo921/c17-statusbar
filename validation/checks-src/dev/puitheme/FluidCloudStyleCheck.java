package dev.puitheme;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.statusbar.notification.row.NotificationBackgroundView;
import com.heytap.log.nx.obus.a;
import com.oplus.graphics.OplusBlurParam;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplus.systemui.plugins.shared.template.section.media.K0;
import com.oplus.systemui.plugins.shared.template.section.media.s1;
import com.oplus.systemui.statusbar.notification.customcard.OplusCustomRow;
import com.oplus.view.ViewRootManager;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.lang.reflect.Field;

/** Real Release behavior against the audited parameter and owner contracts. */
public final class FluidCloudStyleCheck {
    private static int checks;
    private static void check(boolean okay, String why) { checks++; if (!okay) throw new AssertionError(why); }
    private static final class Resources extends android.content.res.Resources {
        boolean failGradient;
        @Override public String getResourceEntryName(int id) {
            if (id == 2 && failGradient) { failGradient = false; throw new IllegalStateException("Native later lookup failed"); }
            return id == 1 ? "card_background_mix_color" : id == 2 ? "card_background_gradient_middle_color" : "card_content_title_color";
        }
    }
    private static final class NotificationResources extends android.content.res.Resources {
        int color = 0xfffafafa, reads;
        @Override public int getIdentifier(String name, String type, String pkg) {
            check(name.equals("notification_material_background_color_compass") && type.equals("color")
                    && pkg.equals("com.android.systemui"), "exact final normal native notification resource"); return 71;
        }
        @Override public int getColor(int id, Theme theme) { check(id == 71, "resolved overlay resource used"); reads++; return color; }
    }
    private static final class NotificationBody extends NotificationBackgroundView {
        final NotificationResources resources = new NotificationResources();
        @Override public android.content.res.Resources getResources() { return resources; }
    }
    private static int owned(Object controller, String field) throws Exception {
        Field value = controller.getClass().getDeclaredField(field); value.setAccessible(true);
        return ((java.util.Map<?,?>) value.get(controller)).size();
    }
    private static final class Env {
        final FluidCloudStyle style = new FluidCloudStyle();
        final Resources resources = new Resources();
        final CardView card = new CardView();
        final CardBackgroundView host = new CardBackgroundView(resources);
        final ViewRootManager manager = new ViewRootManager();
        final a helper = new a(null, 3);
        Env() throws Exception {
            Handler.postedForCheck.clear(); style.resolve(FluidCloudStyleCheck.class.getClassLoader());
            a.controller = null; a.style = style; ViewRootManager.style = style;
            card.addView(host); helper.b(host, manager);
        }
        void configure(boolean highlights, boolean background, boolean follow) {
            style.configure(settings(highlights, background, follow)); Handler.drainForCheck();
        }
        void close() { style.release(); Handler.drainForCheck(); a.style = null; ViewRootManager.style = null; a.controller = null; }
    }
    private static Bundle settings(boolean highlights, boolean background, boolean follow) {
        Bundle bundle = new Bundle(); bundle.putBoolean(FluidCloudStyle.MASTER,true); bundle.putBoolean(C17HighlightRemoval.ENABLED, highlights);
        bundle.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND, background); bundle.putBoolean(FluidCloudStyle.FOLLOW_NOTIFICATION, follow);
        // These identity checks are about the native bodies: the shade scope (on by default since code99) is set off here and has its own checks.
        bundle.putBoolean(FluidCloudStyle.SCOPE_SHADE, false);
        return bundle;
    }
    public static void main(String[] args) throws Exception {
        parameters(); ownershipAndRestore(); nativeFailure(); accentPriority(); notificationIdentity(); floatingIsolation();
        System.out.println("FluidCloudStyleCheck: " + checks + " checks passed");
    }
    private static void parameters() throws Exception {
        float[] source = new OplusBlurParam().toFloatArray();
        float[] original = source.clone();
        for (int mode = 0; mode <= 6; mode++) {
            source[3] = mode;
            for (int remove = 0; remove < 4; remove++) {
                float[] result = FluidCloudStyle.transformParams(source, (remove & 1) != 0, (remove & 2) != 0);
                check(result != source, "18float material copies incoming array");
                for (int i = 0; i < 18; i++) {
                    float expected = source[i];
                    if (i == 3 && (remove & 1) != 0) expected = mode == 4 ? 2f : mode == 5 ? 3f : mode;
                    if ((i == 7 || i == 11 || i == 15) && (remove & 2) != 0) expected = 0f;
                    check(result[i] == expected, "exact native material slot " + i + " mode " + mode + " policy " + remove);
                }
            }
            check(source[3] == mode, "shared source not mutated");
        }
        float[] shortValues = new float[17]; check(FluidCloudStyle.transformParams(shortValues, true, true) == shortValues, "unknown native shape stays original");
        original[6] = Float.NaN; check(FluidCloudStyle.transformParams(original, true, true) == original, "invalid param stays original");
        check(FluidCloudStyle.transformParams(null, true, true) == null, "null params stay native");
    }
    private static void ownershipAndRestore() throws Exception {
        Env env = new Env();
        check(!env.style.enabled() && env.manager.saved[3] == 4f, "three features default OFF");
        env.host.setAlpha(.34f); env.host.setScaleX(.7f); env.host.setTranslationY(13f);
        env.style.nativeBlurAlpha(env.host, .42f);
        env.configure(true, false, false);
        check(env.manager.saved[3] == 2f, "desktop glow removed through owned manager");
        check(env.manager.saved[7] > 0f && env.manager.saved[11] > 0f, "highlight removal keeps default material fill");
        check((a.gradient >>> 24) == 0, "native gradient removed without replacing card");
        check(env.host.blurAlpha == .42f && env.host.getAlpha() == .34f, "native animated alpha preserved");
        check(env.host.getScaleX() == .7f && env.host.getTranslationY() == 13f && env.manager.blurRadius == 540,
                "native shape, layout, interaction and blur unchanged");
        check(env.style.resourceColor(env.resources, 1, 0xb3123456) == 0xb3123456, "native scope doesn't leak");
        OplusBlurParam shared = new OplusBlurParam(); float[] before = shared.toFloatArray();
        ViewRootManager outsider = new ViewRootManager();
        check(env.style.blurParams(env.manager, shared) == shared, "outside scope keeps original param");
        try (FluidCloudStyle.BackgroundScope outer = env.style.beginBackground(env.helper, env.host, env.manager)) {
            check(env.style.blurParams(outsider, shared) == shared, "manager identity isolates unrelated setters");
            Object derived = env.style.blurParams(env.manager, shared);
            check(derived != shared && Arrays.equals(shared.toFloatArray(), before), "shared OplusBlurParam isn't mutated");
            for (int i = 0; i < 1000; i++) check(env.style.blurParams(env.manager, shared) == derived, "unchanged native material reuses clone");
            check(env.style.resourceColor(new Resources(), 2, 0xffffffff) == 0xffffffff, "other Resources isolated");
            check(env.style.resourceColor(env.resources, 3, 0xffffffff) == 0xffffffff, "content text not recolored");
            try (FluidCloudStyle.BackgroundScope nested = env.style.beginBackground(env.helper, env.host, env.manager)) { nested.completed(); }
            check((env.style.resourceColor(env.resources, 2, 0xff445566) >>> 24) == 0, "nested scope restores outer scope");
            outer.completed();
        }
        check(env.style.beginBackground(env.helper, new View(null), env.manager) == null, "arbitrary View never acquires desktop policy");
        env.configure(true, true, false);
        check(env.manager.saved[7] == 0 && env.manager.saved[11] == 0 && env.manager.saved[15] == 0, "only default fill alpha removed");
        int builds = a.builds;
        for (int i = 0; i < 1000; i++) { env.style.refreshAfterAccent(); Handler.drainForCheck(); }
        check(a.builds == builds, "unchanged events never rebuild blur");
        Bundle safe = settings(true, true, false); safe.putBoolean(StatusBarSettings.SAFE_MODE, true);
        env.style.configure(safe); Handler.drainForCheck();
        check(!env.style.enabled() && env.manager.saved[3] == 4f && env.manager.saved[7] > 0f && a.gradient == 0xff000000,
                "safe mode reruns actual native initializer");
        env.configure(true, true, false); env.style.detach(env.host);
        check(env.manager.saved[3] == 4f && env.manager.saved[7] > 0f, "detach restores owned native inputs");
        env.close();
        env = new Env(); env.configure(true, true, false); env.style.release(); Handler.drainForCheck();
        check(env.manager.saved[3] == 4f && env.manager.saved[7] > 0f, "release restores exact native material"); env.close();
    }
    private static void nativeFailure() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            Env env = new Env(); env.style.configure(settings(true, true, false)); env.resources.failGradient = true;
            boolean threw = false;
            try { env.helper.b(env.host, env.manager); } catch (IllegalStateException expected) { threw = true; }
            check(threw && env.manager.saved[7] == 0f, "real partial native write precedes failed later resource lookup");
            check(env.style.resourceColor(env.resources, 1, 0xff112233) == 0xff112233, "failed scope doesn't leak");
            if (mode == 0) env.configure(false, false, false);
            else if (mode == 1) env.style.detach(env.host);
            else env.style.release();
            Handler.drainForCheck();
            check(env.manager.saved[3] == 4f && env.manager.saved[7] > 0f && a.gradient == 0xff000000,
                    "partial native material remains owned and is fully restored " + mode); env.close();
        }
    }
    private static void accentPriority() throws Exception {
        Env env = new Env(); FluidCloudAccent accent = new FluidCloudAccent(); accent.resolve(FluidCloudStyleCheck.class.getClassLoader());
        a.controller = accent; env.style.setAccentController(accent);
        ViewGroup media = new ViewGroup(null); env.card.addView(media);
        kotlinx.coroutines.flow.p flow = new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff2468ac));
        s1 section = new s1(null, new K0(flow), media, null);
        env.helper.b(env.host, env.manager); accent.mediaChanged(section);
        Bundle enabled = new Bundle(); enabled.putBoolean(FluidCloudAccent.ENABLED, true); enabled.putFloat(FluidCloudAccent.OPACITY, 50f);
        accent.configure(enabled); Handler.drainForCheck();
        Bundle appearance = settings(true,true,false);
        appearance.putString(FluidCloudStyle.BACKGROUND_MODE,"native");
        appearance.putFloat(FluidCloudStyle.BACKGROUND_OPACITY,50f);
        env.style.configure(appearance); Handler.drainForCheck();
        check(a.mixed == 0x5a2468ac && a.gradient == 0x802468ac, "native source keeps album RGB and applies the media-accent page opacity once while GLOW is removed");
        check(env.manager.saved[3] == 2f && Math.abs(env.manager.saved[7]-90f/255)<.00001f,
                "active accent no longer prevents native appearance opacity");
        flow.value = new com.oplus.systemui.plugins.shared.data.media.model.a(0xffd06040);
        accent.mediaChanged(section); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.mixed == 0x5ad06040 && a.gradient == 0x80d06040, "new album gradient updates beneath unchanged appearance opacity");
        int builds = a.builds;
        for (int i = 0; i < 1000; i++) { accent.mediaChanged(section); env.style.refreshAfterAccent(); Handler.drainForCheck(); }
        check(a.builds == builds, "same native media event never reparses artwork/rebuilds material");
        accent.configure(new Bundle()); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.mixed == 0x5a262626 && Math.abs(env.manager.saved[7]-90f/255)<.00001f && env.manager.saved[3]==2f && a.gradient==0xff000000,
                "accent OFF restores native RGB; independent base opacity still fades the blur material while native gradient opacity and optical removal stay intact");
        accent.configure(enabled); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.mixed == 0x5ad06040, "accent ON restores latest song beneath unchanged appearance opacity");
        Bundle masked = new Bundle(enabled); masked.putBoolean(FluidCloudAccent.MASK_ENABLED, true);
        masked.putInt(FluidCloudAccent.MASK_COLOR, 0xff000000); masked.putFloat(FluidCloudAccent.MASK_OPACITY, 50f);
        accent.configure(masked); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.mixed == 0x5a683020 && a.gradient == 0x80683020, "media mask RGB and appearance opacity combine without losing native alpha");
        flow.value = new com.oplus.systemui.plugins.shared.data.media.model.a(0xff408060);
        accent.mediaChanged(section); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.gradient == 0x80204030 && Math.abs(env.manager.saved[7]-90f/255)<.00001f,
                "song extraction continues beneath media mask and appearance opacity");
        accent.configure(enabled); env.style.refreshAfterAccent(); Handler.drainForCheck();
        check(a.gradient == 0x80408060 && env.manager.saved[3]==2f, "mask OFF restores newest album gradient with opacity and GLOW policy retained");
        appearance.putString(FluidCloudStyle.BACKGROUND_MODE,"custom");
        appearance.putInt(FluidCloudStyle.BACKGROUND_COLOR,0xffc04020);
        env.style.configure(appearance);Handler.drainForCheck();
        check(a.mixed==0x5a408060&&a.gradient==0x80408060,"an active media accent outranks the custom base colour while its opacity still applies");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff60a0e0);
        accent.mediaChanged(section);env.style.refreshAfterAccent();Handler.drainForCheck();
        check(a.mixed==0x5a60a0e0&&accent.nativeBackgroundMediaColor(env.host)==0xff60a0e0,
                "a new album replaces the accent while a custom base colour is saved underneath it");
        env.style.release(); Handler.drainForCheck();
        check(a.mixed == 0xb360a0e0 && a.gradient==0xff60a0e0 && env.manager.saved[3] == 4f,
                "style release restores latest live album colour and native alpha instead of stale override");
        accent.release(); env.close();
        env = new Env(); accent = new FluidCloudAccent(); accent.resolve(FluidCloudStyleCheck.class.getClassLoader());
        a.controller = accent; env.style.setAccentController(accent); env.helper.b(env.host, env.manager);
        media = new ViewGroup(null); env.card.addView(media);
        accent.mediaChanged(new s1(null, new K0(flow), media, null)); accent.configure(enabled);
        Handler.drainForCheck(); env.configure(true, true, false);
        try (FluidCloudAccent.NativeRestoreScope accentRestore = accent.beginNativeRestore();
                FluidCloudStyle.NativeRestoreScope styleRestore = env.style.beginNativeRestore()) {
            env.style.detach(env.host); accent.detach(env.host);
        }
        check(a.mixed == 0xb3262626 && a.gradient == 0xff000000 && env.manager.saved[3] == 4f,
                "paired detach restores native instead of controllers reapplying each other");
        check(owned(accent, "backgrounds") == 0 && owned(env.style, "backgrounds") == 0,
                "restore replay does not re-register a departing background");
        accent.release(); env.close();
    }
    private static void notificationIdentity() throws Exception {
        Env env = new Env(); FluidCloudStyle.NotificationHooks hooks = env.style.resolveNotification(FluidCloudStyleCheck.class.getClassLoader());
        check(hooks.inflate.getName().equals("onFinishInflate") && hooks.setEntry.getParameterTypes()[0].getName()
                .equals("com.android.systemui.plugins.custom.CustomRowEntry"), "real native notification lifecycle resolves");
        IdentityHashMap<View, Boolean> owned = new IdentityHashMap<>(); int[] calls = {0};
        env.style.setNotificationStyler(new FluidCloudStyle.NotificationStyler() {
            @Override public void update(View body, boolean follow, boolean keep) { owned.put(body, follow); calls[0]++; }
            @Override public void forget(View body) { owned.remove(body); calls[0]++; }
        });
        OplusCustomRow row = new OplusCustomRow(); env.style.notificationRow(row);
        check(owned.isEmpty(), "native ctor can have no body");
        hooks.inflate.invoke(row); env.style.notificationRow(row);
        NotificationBackgroundView first = row.getMBackgroundNormal();
        check(Boolean.FALSE.equals(owned.get(first)), "OFF records exact fluid body without altering regular notifications");
        env.configure(false, false, true); check(Boolean.TRUE.equals(owned.get(first)), "follow independently registers native notification style");
        check(env.manager.saved[3] == 4f, "notification feature doesn't alter desktop material");
        int changes = calls[0]; for (int i = 0; i < 1000; i++) env.style.notificationRow(row);
        check(calls[0] == changes, "unchanged notification binds don't recalculate shader state");
        env.style.notificationBody(new View(null)); check(owned.size() == 1, "immersive/art/unknown View not registered as body");
        NotificationBody second = new NotificationBody(); row.setMBackgroundNormal(second); env.style.notificationRow(row);
        check(!owned.containsKey(first) && Boolean.TRUE.equals(owned.get(second)), "recycled old body loses exact identity, new body takes policy");
        check(env.style.normalNotificationColor(second) == 0xfffafafa, "normal native tint is cached at real body bind");
        int reads = second.resources.reads; for (int i = 0; i < 1000; i++) env.style.normalNotificationColor(second);
        check(second.resources.reads == reads, "drawing material accesses do not re-read theme colors");
        second.resources.color = 0xff404040; changes = calls[0]; env.style.notificationRow(row);
        check(env.style.normalNotificationColor(second) == 0xff404040 && calls[0] == changes + 1,
                "native theme event refreshes follow tint despite unchanged boolean policy");
        // code98: the user's C17 highlight removal reaches every fluid-cloud body, not only the ones a colour decision owns.
        env.configure(true, true, false); check(Boolean.TRUE.equals(owned.get(second)), "C17 highlight removal covers a fluid body even when follow is OFF");
        check(env.manager.saved[3] == 2f, "desktop policy stays independent from notification off");
        env.configure(false, false, false); check(Boolean.FALSE.equals(owned.get(second)), "no highlight removal and follow OFF restores native fluid body independently");
        env.configure(true, true, false); check(Boolean.TRUE.equals(owned.get(second)), "highlight removal comes back with the C17 switch");
        Bundle noNotification = settings(true, true, false); noNotification.putBoolean(C17HighlightRemoval.NOTIFICATION_ENABLED, false);
        env.style.configure(noNotification); Handler.drainForCheck();
        check(Boolean.FALSE.equals(owned.get(second)), "the C17 notification scope switch off leaves the fluid body native");
        env.configure(true, true, false);
        env.style.detach(row); check(owned.isEmpty(), "row detach forgets body identity for reuse");
        env.style.notificationRow(row); env.style.release(); Handler.drainForCheck();
        check(owned.isEmpty(), "release forgets notification registration"); env.close();
    }
    private static void floatingIsolation()throws Exception {
        Env env=new Env();env.style.resolveNotification(FluidCloudStyleCheck.class.getClassLoader());env.style.setNotificationStyler((body,following,keep)->{});
        env.configure(true,true,false);check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,"floating settings apply before scene move");
        OplusCustomRow row=new OplusCustomRow();row.setMBackgroundNormal(new NotificationBackgroundView());
        com.android.systemui.plugins.custom.CustomRowEntry entry=new com.android.systemui.plugins.custom.CustomRowEntry();entry.immersiveCardView=env.card;row.setEntry(entry);
        env.style.notificationRow(row);env.style.refreshAfterAccent();Handler.drainForCheck();
        check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,
                "a retained notification entry without a real row host cannot disable floating appearance");
        env.card.parent=row;env.style.notificationContentChanged(row);Handler.drainForCheck();
        check(env.manager.saved[3]==4f&&env.manager.saved[7]>0f&&a.mixed==0xb3262626,
                "a real custom-row ancestor restores exact native template instead of floating appearance");
        env.card.parent=null;env.style.notificationContentChanged(row);Handler.drainForCheck();
        check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,
                "leaving the actual row restores floating appearance without deleting the retained entry");
        check(entry.immersiveCardView==env.card&&row.getEntry()==entry,"scene classification never mutates cached OEM notification entry");
        android.view.SurfaceView surface=new android.view.SurfaceView();surface.parent=row;
        Object remoteHost=new Object();
        android.view.SurfaceControl layer=new android.view.SurfaceControl("floating-isolation",null);
        android.view.SurfaceControlViewHost.SurfacePackage pack=new android.view.SurfaceControlViewHost.SurfacePackage(layer);
        FluidCloudSurfaceRoots.hostView(remoteHost,env.card);FluidCloudSurfaceRoots.hostPackage(remoteHost,pack);
        FluidCloudSurfaceRoots.childPackage(surface,pack);env.style.mediaOwnershipChanged();Handler.drainForCheck();
        check(env.manager.saved[3]==4f&&env.manager.saved[7]>0f,
                "attached visible notification Surface owns its remote material and excludes floating policy");
        surface.shown=false;env.style.surfacePresentationChanged(surface);Handler.drainForCheck();
        check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,
                "hidden notification Surface cannot hold floating appearance disabled");
        surface.shown=true;surface.attached=false;env.style.surfacePresentationChanged(surface);Handler.drainForCheck();
        check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,
                "detached notification Surface cannot hold floating appearance disabled");
        surface.attached=true;surface.parent=null;env.style.surfacePresentationChanged(surface);Handler.drainForCheck();
        check(env.manager.saved[3]==2f&&env.manager.saved[7]==0f,
                "visible Surface moved outside the row regains floating appearance without losing entry references");
        FluidCloudSurfaceRoots.childDetached(surface);FluidCloudSurfaceRoots.releaseHost(remoteHost);
        env.style.setKeyguard(true);Handler.drainForCheck();
        check(env.manager.saved[3]==4f&&env.manager.saved[7]>0f,"floating appearance cannot recolour lockscreen templates");
        env.close();
    }

}
