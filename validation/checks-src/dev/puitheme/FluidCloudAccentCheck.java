package dev.puitheme;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardBackgroundView;
import com.oplus.systemui.plugins.seedling.card.ui.view.CardView;
import com.oplus.systemui.plugins.shared.template.section.media.K0;
import com.oplus.systemui.plugins.shared.template.section.media.s1;

/** Verifies exact ownership, balanced native scopes, alpha preservation and restore. */
public final class FluidCloudAccentCheck {
    private static int checks;
    private static void check(boolean okay, String why) {
        checks++; if (!okay) throw new AssertionError(why);
    }
    private static class PluginResources extends android.content.res.Resources {
        @Override public String getResourceEntryName(int id) {
            return id == 1 ? "card_background_mix_color" : id == 2 ? "card_background_gradient_middle_color" : "card_content_title_color";
        }
    }
    private static final class LateFailureResources extends PluginResources {
        boolean fail=true;
        @Override public String getResourceEntryName(int id) {
            if(id==2&&fail){fail=false;throw new IllegalStateException("Native later gradient lookup failed after mix was written");}
            return super.getResourceEntryName(id);
        }
    }
    private static Bundle enabled(boolean value) {
        Bundle result = new Bundle(); result.putBoolean(FluidCloudAccent.ENABLED, value); return result;
    }
    private static void refresh(FluidCloudAccent control) throws Exception {
        // Desktop Handler doesn't run posts; explicitly execute the real batch entries.
        Field backgrounds = FluidCloudAccent.class.getDeclaredField("backgrounds"); backgrounds.setAccessible(true);
        Class<?> bindingClass = Class.forName("dev.puitheme.FluidCloudAccent$BackgroundBinding");
        Method apply = FluidCloudAccent.class.getDeclaredMethod("replay", bindingClass, Boolean.TYPE); apply.setAccessible(true);
        for (Object binding : new java.util.ArrayList<>(((java.util.Map<?,?>) backgrounds.get(control)).values()))
            apply.invoke(control, binding, control.enabled());
    }
    public static void main(String[] args) throws Exception {
        FluidCloudAccent control = new FluidCloudAccent();
        FluidCloudAccent.Hooks hooks = control.resolve(FluidCloudAccentCheck.class.getClassLoader());
        check(hooks.background.getName().equals("b"), "real fingerprint resolves");
        check(!control.enabled(), "default disabled");
        check(!control.lockscreenEnabled(), "lockscreen accent default disabled");
        check(!FluidCloudAccent.isBackgroundColor("card_content_title_color"), "content color stays native");
        check(!FluidCloudAccent.isBackgroundColor("capsule_background_color"), "collapsed capsule stays native");
        check(FluidCloudAccent.preserveAlpha(0x00123456, 0xffaabbcc) == 0x00123456, "transparent native colors stay unchanged");
        check(FluidCloudAccent.mask(0xff804020, 0xff000000, 50f) == 0xff402010, "mask blends RGB with exact opacity");
        check(FluidCloudAccent.mask(0x80804020, 0xffffffff, 100f) == 0x80ffffff, "mask never replaces native accent alpha");
        check(FluidCloudAccent.mask(0xff804020, 0x00ffffff, 100f) == 0xff804020, "transparent mask keeps extraction");
        check(FluidCloudAccent.mask(0xff804020, 0xff000000, Float.NaN) == 0xff804020, "invalid opacity keeps native extraction");
        PluginResources resources = new PluginResources();
        CardView card = new CardView(); CardBackgroundView backdrop = new CardBackgroundView(resources);
        ViewGroup root = new ViewGroup(null); card.addView(backdrop); card.addView(root);
        kotlinx.coroutines.flow.p colors = new kotlinx.coroutines.flow.p(
                new com.oplus.systemui.plugins.shared.data.media.model.a(0xffd4798e));
        s1 owner = new s1(null, new K0(colors), root, null);
        FluidCloudAccent mediaOnly=new FluidCloudAccent();
        check(mediaOnly.nativeMediaColor(owner)==0xffd4798e,"lockscreen native colour contract resolves without desktop helper hooks or accent enable");
        mediaOnly.mediaChanged(owner);
        check(mediaOnly.nativeBackgroundMediaColor(backdrop)==0xffd4798e,"media-only event records native ownership independently of desktop contract installation");
        mediaOnly.release();
        com.heytap.log.nx.obus.a.controller = control;
        com.heytap.log.nx.obus.a helper = new com.heytap.log.nx.obus.a(null, 3);
        com.oplus.view.ViewRootManager manager = new com.oplus.view.ViewRootManager();
        helper.b(backdrop, manager);
        check(com.heytap.log.nx.obus.a.gradient == 0xff000000, "disabled native black remains");
        control.mediaChanged(owner); control.configure(enabled(true)); refresh(control);
        check(control.mediaAccent(owner)==0xffd4798e, "non-card owner can read exact current native color");
        check(control.lockscreenEnabled(), "enabling the media accent covers the lock screen too; it is no longer a separate switch");
        check(com.heytap.log.nx.obus.a.mixed == 0xb3d4798e, "mix adopts exact OEM accent preserving alpha");
        check(com.heytap.log.nx.obus.a.gradient == 0xffd4798e, "black gradient receives accent");
        check(control.resourceColor(resources, 2, 0xff000000) == 0xff000000, "native scope does not leak");
        int builds = com.heytap.log.nx.obus.a.builds;
        for (int i = 0; i < 1000; i++) refresh(control);
        check(com.heytap.log.nx.obus.a.builds == builds, "unchanged bindings don't rebuild blur or gradient");
        control.nativeBlurAlpha(backdrop, .36f);
        colors.value = new com.oplus.systemui.plugins.shared.data.media.model.a(0xff66aadd);
        control.mediaChanged(owner); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff66aadd, "native color update is observed");
        check(backdrop.blurAlpha == .36f, "native transition alpha survives recolor");
        check(control.protectsBackground(backdrop), "active media color is protected from native base removal");
        Bundle masked = enabled(true); masked.putBoolean(FluidCloudAccent.MASK_ENABLED, true);
        masked.putInt(FluidCloudAccent.MASK_COLOR, 0xff000000); masked.putFloat(FluidCloudAccent.MASK_OPACITY, 50f);
        control.configure(masked); refresh(control);
        check(control.mediaAccent(owner)==0xff33556f, "non-card native accent includes configured mask");
        check(com.heytap.log.nx.obus.a.gradient == 0xff33556f, "mask overlays the extracted media RGB");
        check(com.heytap.log.nx.obus.a.mixed == 0xb333556f, "native fill alpha survives mask");
        colors.value = new com.oplus.systemui.plugins.shared.data.media.model.a(0xff40a060);
        control.mediaChanged(owner); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff205030, "song changes continue beneath enabled mask");
        builds = com.heytap.log.nx.obus.a.builds;
        for (int i = 0; i < 1000; i++) { control.mediaChanged(owner); refresh(control); }
        check(com.heytap.log.nx.obus.a.builds == builds, "same masked native color does not rebuild");
        control.configure(enabled(true)); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff40a060, "mask OFF restores latest accent, not stale pre-mask color");
        check(backdrop.blurAlpha == .36f, "mask changes preserve native animation alpha");
        masked.putInt(FluidCloudAccent.MASK_COLOR, 0x00336699);
        masked.putBoolean(StatusBarSettings.alphaKey(FluidCloudAccent.MASK_COLOR), false);
        control.configure(masked); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff3a837d, "automatic color alpha is opaque; mask percentage owns strength");
        masked.putBoolean(StatusBarSettings.alphaKey(FluidCloudAccent.MASK_COLOR), true);
        control.configure(masked); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff40a060, "explicit transparent mask alpha keeps extracted accent");
        control.configure(enabled(true)); refresh(control);
        check(control.beginBackground(helper, new View(null), manager) == null, "unrelated host can't open scope");
        FluidCloudAccent.BackgroundScope scope = control.beginBackground(helper, backdrop, manager);
        try {
            check(control.resourceColor(new PluginResources(), 2, 0xff000000) == 0xff000000, "other Resources isn't recolored");
            check(control.resourceColor(resources, 3, 0xffffffff) == 0xffffffff, "title color retained");
            throw new IllegalStateException("native failure");
        } catch (IllegalStateException expected) { } finally { scope.close(); }
        check(control.resourceColor(resources, 2, 0xff000000) == 0xff000000, "failed native scope closes");
        Bundle safe = enabled(true); safe.putBoolean(StatusBarSettings.SAFE_MODE, true);
        control.configure(safe); refresh(control);
        check(!control.enabled() && com.heytap.log.nx.obus.a.gradient == 0xff000000, "safe mode restores original native material");
        control.configure(enabled(true)); refresh(control); control.mediaDisposed(owner); refresh(control);
        check(com.heytap.log.nx.obus.a.gradient == 0xff000000, "unbind discards stale accent and restores");
        control.mediaChanged(owner); refresh(control); control.release();
        check(!control.enabled(), "release cannot retain ownership");
        lateNativeFailureRecovery();
        nullManagerFallback();
        System.out.println("FluidCloudAccentCheck: " + checks + " checks passed");
    }
    private static void nullManagerFallback() throws Exception {
        FluidCloudAccent control=new FluidCloudAccent();control.resolve(FluidCloudAccentCheck.class.getClassLoader());
        CardView card=new CardView();CardBackgroundView host=new CardBackgroundView(new PluginResources());
        ViewGroup root=new ViewGroup(null);card.addView(host);card.addView(root);
        kotlinx.coroutines.flow.p flow=new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff408060));
        s1 owner=new s1(null,new K0(flow),root,null);
        com.heytap.log.nx.obus.a.controller=control;com.heytap.log.nx.obus.a.style=null;
        com.heytap.log.nx.obus.a helper=new com.heytap.log.nx.obus.a(null,3);
        helper.b(host,null);
        android.graphics.drawable.GradientDrawable nativeResource=(android.graphics.drawable.GradientDrawable)host.getBackground();
        control.mediaChanged(owner);
        Bundle settings=enabled(true);settings.putBoolean(FluidCloudAccent.LOCKSCREEN_ENABLED,true);
        settings.putBoolean(FluidCloudAccent.MASK_ENABLED,true);settings.putInt(FluidCloudAccent.MASK_COLOR,0xff000000);
        settings.putFloat(FluidCloudAccent.MASK_OPACITY,50f);control.configure(settings);refresh(control);
        check(control.lockscreenEnabled()&&control.mediaAccent(owner)==0xff204030,"lockscreen flag exposes latest masked OEM flow");
        android.graphics.drawable.GradientDrawable tint=(android.graphics.drawable.GradientDrawable)host.getBackground();
        check(tint.getColor().getDefaultColor()==0xb3204030,"null-manager resource fallback receives masked RGB");
        check(nativeResource.getColor().getDefaultColor()==0xb3262626,"cached shared native resource stays intact");
        check(tint.getCornerRadius()==36f&&tint.mStrokePaint.strokeWidth==2,"fallback tint retains native shape and stroke");
        int builds=com.heytap.log.nx.obus.a.builds;for(int i=0;i<1000;i++)refresh(control);
        check(com.heytap.log.nx.obus.a.builds==builds,"unchanged fallback is not recolored per frame");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff80c040);control.mediaChanged(owner);refresh(control);
        check(((android.graphics.drawable.GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0xb3406020,"fallback follows native media update");
        // code98: the native colour is empty for a moment while the track changes; the last colour stays for a grace period.
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0);control.mediaChanged(owner);
        check(control.nativeCurrentMediaColor(null)!=null&&control.nativeCurrentMediaColor(null)==0xff80c040,"a momentarily empty native colour keeps the last colour");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff102030);control.mediaChanged(owner);
        check(control.nativeCurrentMediaColor(null)==0xff102030,"the new colour replaces it without a gap");
        control.expireColorGraceForCheck();check(control.nativeCurrentMediaColor(null)==0xff102030,"a cancelled grace changes nothing");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0);control.mediaChanged(owner);control.expireColorGraceForCheck();
        check(control.nativeCurrentMediaColor(null)==null,"a colour that stays missing is let go after the grace");
        flow.value=new com.oplus.systemui.plugins.shared.data.media.model.a(0xff80c040);control.mediaChanged(owner);refresh(control);
        control.configure(enabled(false));refresh(control);
        android.graphics.drawable.GradientDrawable restored=(android.graphics.drawable.GradientDrawable)host.getBackground();
        check(restored.getColor().getDefaultColor()==0xb3262626&&restored.mStrokePaint.strokeWidth==2,"disable rebuilds native null-manager resource");
        check(control.mediaAccent(owner)==null&&!control.lockscreenEnabled(),"disabled accent does not expose fallback black");
        control.configure(enabled(true));refresh(control);control.detach(host);
        check(((android.graphics.drawable.GradientDrawable)host.getBackground()).getColor().getDefaultColor()==0xb3262626,"null-manager detach restores native fill");
        control.release();
    }
    private static void lateNativeFailureRecovery() throws Exception {
        // The real intercepted helper writes mix first and then reads gradient. An
        // exception in that later read must retain dirty ownership on the very first tint.
        for(int mode=0;mode<6;mode++){
            FluidCloudAccent control=new FluidCloudAccent();control.resolve(FluidCloudAccentCheck.class.getClassLoader());
            LateFailureResources resources=new LateFailureResources();
            CardView card=new CardView();CardBackgroundView backdrop=new CardBackgroundView(resources);
            ViewGroup root=new ViewGroup(null);card.addView(backdrop);card.addView(root);
            s1 owner=new s1(null,new K0(new kotlinx.coroutines.flow.p(new com.oplus.systemui.plugins.shared.data.media.model.a(0xff773399))),root,null);
            com.heytap.log.nx.obus.a.controller=control;com.heytap.log.nx.obus.a helper=new com.heytap.log.nx.obus.a(null,3);
            com.oplus.view.ViewRootManager manager=new com.oplus.view.ViewRootManager();
            helper.b(backdrop,manager);control.mediaChanged(owner);control.configure(enabled(true));
            int before=com.heytap.log.nx.obus.a.builds;
            boolean threw=false;
            try{helper.b(backdrop,manager);}catch(IllegalStateException expected){threw=true;}
            check(threw,"later native helper failure is real: "+mode);
            check(com.heytap.log.nx.obus.a.builds==before+1,"failure doesn't immediately replay the throwing helper: "+mode);
            check(com.heytap.log.nx.obus.a.mixed==0xb3773399,"native mix was partially tinted before failure: "+mode);
            check(com.heytap.log.nx.obus.a.gradient==0xff000000,"later native write did not complete: "+mode);
            check(control.resourceColor(resources,1,0xb3262626)==0xb3262626,"throwing scope leaves unrelated reads native: "+mode);
            before=com.heytap.log.nx.obus.a.builds;
            if(mode==0){control.configure(enabled(false));refresh(control);}
            else if(mode==1){Bundle safe=enabled(true);safe.putBoolean(StatusBarSettings.SAFE_MODE,true);control.configure(safe);refresh(control);}
            else if(mode==2){control.mediaDisposed(owner);refresh(control);}
            else if(mode==3)control.detach(backdrop);
            else if(mode==4)control.release();
            else{
                refresh(control);
                check(com.heytap.log.nx.obus.a.gradient==0xff773399,"enabled dirty binding retries a full material: "+mode);
                int built=com.heytap.log.nx.obus.a.builds;for(int i=0;i<1000;i++)refresh(control);
                check(com.heytap.log.nx.obus.a.builds==built,"completed retry resumes color deduplication: "+mode);
                control.configure(enabled(false));refresh(control);
            }
            check(com.heytap.log.nx.obus.a.builds>before,"dirty material receives native reinitialization: "+mode);
            check(com.heytap.log.nx.obus.a.mixed==0xb3262626&&com.heytap.log.nx.obus.a.gradient==0xff000000,"native material fully restored after failure: "+mode);
            control.release();
        }
    }
}
