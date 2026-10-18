// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewParent;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Same-scene OEM inputs, not a theme-colour filter over the finished card.
 * NotificationBackgroundViewExtImp owns each body's BlurProxy. Its immutable
 * NotificationBlurMode can be reused; its drawables, observers and proxy cannot.
 * Custom-row MetaBall colours are rebuilt by the native entry/JSON initializer.
 * All target mutation is event-driven and reversible through that initializer.
 */
final class FluidCloudSceneMaterial {
    private static final String BODY="com.android.systemui.statusbar.notification.row.NotificationBackgroundView";
    private static final String EXT="com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp";
    private static final String EXT_BASE="com.android.systemui.statusbar.notification.row.NotificationBackgroundViewExt";
    private static final String MODE="com.oplus.systemui.notification.row.material.NotificationBlurMode";
    private static final String ROW="com.android.systemui.statusbar.notification.row.ExpandableNotificationRow";
    private static final String HEADS="com.oplus.systemui.notification.headsup.windowframe.HeadsUpLayout";
    private static final String META="com.oplus.posteffect.drawable.MetaBallBlurDrawable";
    private static final String ENTRY="com.android.systemui.plugins.custom.CustomRowEntry";
    private static final String HANDLER="com.oplus.systemui.notification.row.NotificationBackgroundCustomRowHandler";
    private final Map<ClassLoader,Hooks> hooks=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View,Target> targets=Collections.synchronizedMap(new WeakHashMap<>());
    /** The native AOD mask the OEM set on a body before it was registered (a card that comes up while the screen is in AOD gets its
     * mask at creation, our decision for it comes later): kept so the registration can still replace it with the neutral glass. */
    private final Map<View,WeakReference<Drawable>> pendingAodMasks=Collections.synchronizedMap(new WeakHashMap<>());
    /** Colour resource ids by name, per resources: the name lookup walks the whole string pool of SystemUI. */
    private static final Map<android.content.res.Resources,Map<String,Integer>> COLOR_IDS=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object,Meta> metas=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<View,Native> nativeBodies=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object,WeakReference<Target>> proxyTargets=Collections.synchronizedMap(new WeakHashMap<>());
    private final java.util.Set<Object> aodBoundaries=Collections.newSetFromMap(new WeakHashMap<Object,Boolean>());
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadLocal<Boolean> replaying=new ThreadLocal<>();
    private final ThreadLocal<Boolean> confirmingPlatform=new ThreadLocal<>();
    private final ThreadLocal<Boolean> drawing=new ThreadLocal<>();
    private boolean refreshPending;
    private boolean follow,keyguard,dozing,released;
    private boolean lockEffectsEnabled=true;
    void lockEffectsEnabled(boolean enabled) {
        if(released||lockEffectsEnabled==enabled)return;
        lockEffectsEnabled=enabled;
        if(!enabled)forgetCompositions();
        refresh();
    }
    /** "Same card colour in AOD and on the lock screen": the neutral glass replaces the black AOD mask on every registered card. */
    private boolean unifyAod;
    void unifyAod(boolean on) {
        if(released||unifyAod==on)return;
        unifyAod=on;refresh();
    }
    private long materialEpoch;
    private final Map<View,HeadsMaterial> headsMaterials=Collections.synchronizedMap(new WeakHashMap<>());
    long revision(){return materialEpoch;}
    boolean headsPaletteChanged(Object extension) {
        if(released||Boolean.TRUE.equals(replaying.get())||access(extension)==null)return false;
        // Theme/material notification is distinct from setDrawableMaterialColor,
        // which is also called by draw. Native optical controls may change in place.
        headsMaterials.clear();View body=body(access(extension),extension);Native state=body==null?null:nativeBodies.get(body);
        if(state!=null&&!Boolean.TRUE.equals(drawing.get()))state.dirty=true;return true;
    }

    /** Desktop SeedlingCard has its own ViewRootManager, not a notification
     * BlurProxy. Obtain the normal HEADS_UP recipe from the native factory and
     * convert it with the OEM parameter builder; do not flatten it to one RGB.
     * Called at native initialization/material events, never from draw(). */
    float[] headsUpParams(View owner) {
        if(released||owner==null||keyguard||dozing)return null;
        HeadsMaterial cached=headsMaterials.get(owner);
        if(cached!=null&&cached.epoch==materialEpoch)return cached.values;
        float[] values=null;
        synchronized(nativeBodies){for(Native candidate:nativeBodies.values()) {
            Object extension=candidate.extension.get();View body=candidate.body.get();
            Hooks access=access(extension);
            if(body==null||!body.isAttachedToWindow()||extension==null||access==null||access.buildMotion==null)continue;
            try {
                Object manager=access.manager.get(extension),material=access.nativeMaterial.invoke(extension);
                Object mode=material==null?null:access.headsMode.invoke(material,null,true);
                if(manager==null||!headsMode(mode))continue;
                Object mix=access.motionMix.invoke(manager,access.normalType,mode);
                if(mix==null)continue;
                Object param=access.buildMotion.invoke(null,access.mixSingle.invoke(mix),null);
                float[] result=param==null?null:(float[])access.motionArray.invoke(param);
                if(result==null||result.length!=18)continue;
                boolean valid=true;for(float value:result)if(!Float.isFinite(value)){valid=false;break;}
                if(valid){values=result.clone();break;}
            }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){ }
        }}
        headsMaterials.put(owner,new HeadsMaterial(materialEpoch,values));return values;
    }
    private int diagnosticCount;
    private void diagnostic(Target target,String state) {
        if(target==null||state.equals(target.diagnostic)||target.diagnosticCount>=12||diagnosticCount>=160)return;
        target.diagnostic=state;target.diagnosticCount++;diagnosticCount++;
        android.util.Log.i("C17FluidScene",state);
    }

    Integer notificationColor(View owner) {
        Native selected = donor(owner);
        if (selected != null && selected.tint != null) return selected.tint;
        return resource(owner, "notification_material_background_color_compass");
    }
    Integer foregroundColor(View owner) {
        Native selected = donor(owner);
        return FluidCloudForeground.nativeColor(selected==null?owner:selected.body.get());
    }
    boolean beginFrame(){boolean previous=Boolean.TRUE.equals(drawing.get());drawing.set(Boolean.TRUE);return previous;}
    void endFrame(boolean previous){if(previous)drawing.set(Boolean.TRUE);else drawing.remove();}
    /** The id of a colour of SystemUI's own resources; a name that is not found is not cached (it may appear with another theme). */
    static int colorId(android.content.res.Resources resources, String name) {
        Map<String,Integer> known;
        synchronized(COLOR_IDS){known=COLOR_IDS.get(resources);if(known==null){known=new java.util.HashMap<>();COLOR_IDS.put(resources,known);}}
        synchronized(known){
            Integer id=known.get(name);
            if(id!=null)return id;
            int found=resources.getIdentifier(name,"color","com.android.systemui");
            if(found!=0)known.put(name,found);
            return found;
        }
    }
    private static Integer resource(View owner, String name) {
        if (owner == null) return null;
        try {
            android.content.res.Resources resources = owner.getResources();
            int id = colorId(resources, name);
            if (id != 0) return resources.getColor(id, owner.getContext() == null ? null : owner.getContext().getTheme());
            Context context = owner.getContext();
            if (context == null) return null;
            Context system = context.createPackageContext("com.android.systemui", 0);
            resources = system.getResources();
            id = resources.getIdentifier(name, "color", "com.android.systemui");
            return id == 0 ? null : resources.getColor(id, system.getTheme());
        } catch (Exception | LinkageError unavailable) { return null; }
    }

    Hooks resolve(ClassLoader loader) throws ReflectiveOperationException {
        Hooks result=hooks.get(loader);if(result!=null)return result;
        result=new Hooks(loader);hooks.put(loader,result);
        // Runtime objects can come from a delegated parent of the plugin loader.
        hooks.put(result.extension.getDeclaringClass().getClassLoader(),result);
        hooks.put(result.blur.getDeclaringClass().getClassLoader(),result);
        hooks.put(result.proxyView.getDeclaringClass().getClassLoader(),result);
        hooks.put(result.metaUpdate.getDeclaringClass().getClassLoader(),result);
        return result;
    }
    void register(View body,Object entry) { register(body,entry,body==null?null:body.getParent()); }
    void register(View body,Object entry,Object row) {
        if(released||body==null)return;
        Target target=targets.get(body);
        if(target==null){
            target=new Target(body);targets.put(body,target);target.awakeTint=notificationColor(body);
            WeakReference<Drawable> pending=pendingAodMasks.remove(body);
            if(pending!=null&&pending.get()!=null&&target.aodMaskSource.get()==null)target.aodMaskSource=pending;
        }
        target.heads=heads(body);
        if(target.row.get()!=row){target.row=new WeakReference<>(row);target.tempSource.clear();target.tempCopy=null;}
        bindProxy(target);
        if(target.entry.get()!=entry){target.entry=new WeakReference<>(entry);target.nativeFill=false;target.nativeGradient=false;target.tempSource.clear();target.tempCopy=null;refresh();}
    }
    void policy(boolean following,boolean onKeyguard) {
        policy(following,onKeyguard,false);
    }
    private boolean sceneDirty;
    /** Leaving the keyguard ends every lock-screen composition; the next lock starts from what is on screen then. */
    private void forgetCompositions(){synchronized(targets){for(Target target:targets.values())target.composed=false;}}
    /** Publish the scene at the moment the native state event arrives, so draw, platform and AOD
     * code never read an older scene than the other controllers. Only fields change here; the
     * material refresh still runs once from policy() on the main thread. */
    void sceneState(boolean following,boolean onKeyguard,boolean onDoze) {
        if(released)return;
        if(follow!=following||keyguard!=onKeyguard||dozing!=onDoze)sceneDirty=true;
        if(!onKeyguard&&keyguard)forgetCompositions();
        follow=following;keyguard=onKeyguard;dozing=onDoze;
    }
    void policy(boolean following,boolean onKeyguard,boolean onDoze) {
        if(released||follow==following&&keyguard==onKeyguard&&dozing==onDoze&&!sceneDirty)return;
        sceneDirty=false;
        if(!onKeyguard&&keyguard)forgetCompositions();
        follow=following;keyguard=onKeyguard;dozing=onDoze;refresh();
    }
    /** Business scope controls only this page's notification-follow recipe, leaving independent scene colours intact. */
    void businessFollowing(View body,boolean selected) {
        Target target=body==null?null:targets.get(body);
        if(released||target==null||target.businessFollowing==selected)return;
        target.businessFollowing=selected;
        refresh();
    }
    private boolean following(Target target){return follow&&target!=null&&target.businessFollowing;}
    void accent(View body,Integer color) {
        Target target=targets.get(body);if(target==null)return;
        if(color==null?target.accent==null:color.equals(target.accent))return;
        target.accent=color;refresh();
    }
    /** Explicit fill is shared by the real notification/lockscreen backends;
     * native source changes alpha only. Media accent remains the higher owner. */
    void fill(View body,Integer color,float nativeOpacity) {
        Target target=targets.get(body);if(target==null)return;
        if((color==null?target.fill==null:color.equals(target.fill))&&target.fillOpacity==nativeOpacity)return;
        target.fill=color;target.fillOpacity=nativeOpacity;refresh();
    }
    /** The one final colour layer already owned by the card renderer. A native AOD mask must not
     * paint this tint a second time or promote its user-selected alpha to an opaque flat base. */
    void colourLayer(View body,Integer color) {
        Target target=targets.get(body);if(target==null)return;
        if(color==null?target.colourLayer==null:color.equals(target.colourLayer))return;
        target.colourLayer=color;refresh();
    }
    /** Only the selected media hue qualifies. Opacity belongs to the native optical recipe. */
    void highlightColor(View body,Integer color) {
        Target target=body==null?null:targets.get(body);if(released||target==null)return;
        Integer rgb=color==null?null:Integer.valueOf(color&0x00ffffff);
        if(rgb==null?target.highlightColor==null:rgb.equals(target.highlightColor))return;
        target.highlightColor=rgb;refresh();
    }
    private boolean materialWanted(Target target) {
        return sceneAllowed(target)&&(following(target) || target.fill!=null || target.fillOpacity!=1f || keyguard&&(target.lockLook||target.accent!=null||target.colourLayer!=null));
    }
    private boolean sceneAllowed(Target target){return !keyguard||lockEffectsEnabled||target.heads;}
    /**
     * The card's lock-screen / AOD look is asked for on that scene's own page. Such a card takes the system's normal wallpaper glass in place of
     * its own dark template material and drops the opaque template plate (the big black base), as with "follow system notification" but without
     * needing the fluid-cloud style master that switch lives under. Only on the keyguard.
     */
    void lockLook(View body,boolean look) {
        Target target=body==null?null:targets.get(body);if(released||target==null||target.lockLook==look)return;
        target.lockLook=look;refresh();
    }
    /** The exact native progress setter is event-driven. Intermediate AOD
     * frames leave native material alone; only eligibility changes refresh. */
    void aodProgress(Object repository,float progress) {
        if(released||repository==null)return;
        boolean changed=markAodProgress(repository,progress);
        synchronized(aodBoundaries){changed=aodBoundaries.remove(repository)||changed;}
        if(changed)refresh();
    }
    void prepareAodProgress(Object repository,float progress) {
        if(markAodProgress(repository,progress))synchronized(aodBoundaries){aodBoundaries.add(repository);}
    }
    private boolean markAodProgress(Object repository,float progress) {
        if(released||repository==null)return false;
        boolean clear=aodClear(progress),known=Float.isFinite(progress),changed=false;
        synchronized(targets){for(Target target:targets.values())if(target.aodRepository.get()==repository&&(target.aodClear!=clear||target.aodKnown!=known)) {
            target.aodClear=clear;target.aodKnown=known;changed=true;
            diagnostic(target,"AOD transition: known="+known+", clear="+clear
                    +", retainedNotification="+holdingAod(target)+", backend="+target.recipeBackend);
        }}
        return changed;
    }
    void motionChanged(Object manager,Object proxy) {
        Target target=proxyTarget(proxy);
        if(released||target==null||!materialWanted(target)||asleep(target)||lockBlocked(target))return;
        Hooks access=hooks.get(manager.getClass().getClassLoader());if(access==null)return;
        try {
            boolean motion=Boolean.TRUE.equals(access.motion.invoke(manager));
            if(target.recipe!=null&&target.recipeMotion==motion&&target.recipeManager.get()==manager)return;
            target.recipeEpoch=-1;target.appliedEpoch=-1;target.requestEpoch=-1;requestApply(target);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){ }
    }
    private static boolean aodClear(float progress){return !Float.isNaN(progress)&&!Float.isInfinite(progress)&&progress==0f;}
    /** The OEM AOD crossfade holds a composition that already exists. A card that never had one (a direct lock from the
     * desktop: AOD is the first thing the lock screen shows of it) is not held, so it takes its material in AOD at once
     * instead of showing the native one and jumping to ours when the screen wakes. */
    private boolean lockBlocked(Target target){return keyguard&&(dozing||!target.aodClear)&&target.composed;}
    /** Asleep with something to hold; outside the keyguard any doze state keeps the native material. */
    private boolean asleep(Target target){return dozing&&(!keyguard||target.composed);}
    private boolean holdingAod(Target target) {
        // OEM draw() crossfades the existing body against its separate panoramic
        // AOD drawable. Swapping the body to raw CUSTOM colour at the first
        // nonzero progress causes a visible flash. Keep only a proven, plain
        // normal wallpaper recipe, including music under its one final gradient;
        // native alpha/blur/AOD drawing still run.
        // The card an AOD transition starts from is the awake lock composition: the follow recipe,
        // a custom/media fill or an opacity. Hold whichever of them was submitted instead of
        // reverting to the raw system material at the first non-zero progress.
        return target!=null&&keyguard&&lockBlocked(target)&&target.aodKnown
                &&target.platformReady&&wallpaperMode(target.recipeMode)
                &&(!target.recipeHighlightTinted||target.highlightColor!=null)
                &&(target.accent==null||target.recipeNormalNotification||target.recipeHighlightTinted)&&materialWanted(target);
    }
    boolean holdingAod(View body){return !released&&holdingAod(targets.get(body));}
    /** The final colour layer may freeze only with proven, media-tinted native optics. */
    boolean holdingMediaComposition(View body){
        Target target=body==null?null:targets.get(body);
        return !released&&target!=null&&target.recipeHighlightTinted&&holdingAod(target);
    }
    /** Backend and readiness of one body, for structured diagnostics only. */
    String backendName(View body){
        Target target=targets.get(body);
        if(target==null)return "none";
        return !target.platformReady?"native":target.recipeBackend==0?"platform":target.recipeBackend==1?"motion":target.recipeBackend==2?"wallpaper":"native";
    }
    boolean platformReady(View body){Target target=targets.get(body);return target!=null&&target.platformReady;}
    /** The fill (or, for the music card, the accent) this card wants is drawn over the card itself, as one flat colour, in the same way and at the same strength as the
     * colour of an ordinary notification (code99). The platform material takes the same fill into its own mix, which on this device
     * is much weaker and darker than a flat colour (measured on the lock screen: an ordinary notification came out at about
     * (105, 101, 89), the stopwatch card at (63, 53, 50) from the same media colour), and which is not always taken at all. The
     * music card's own gradient is another path (only visible while the card is immersive) and has no fill, so its accent is the wish. Reads fields only; draws nothing itself. */
    boolean tintOverlay(View body){
        Target target=body==null?null:targets.get(body);
        boolean wanted=!released&&target!=null&&(target.fill!=null||target.accent!=null||target.colourLayer!=null)&&materialWanted(target);
        // Called at every draw of the card: the text of the log line is only built when the answer changed.
        if(target!=null&&(target.overlayLast==null||target.overlayLast!=wanted)){target.overlayLast=wanted;logOverlay(target,wanted);}
        return wanted;
    }
    /** The overlay's answer changed (bounded by diagnostic()): the card's own decision, readable in logcat. */
    private void logOverlay(Target target,boolean answer){
        diagnostic(target,"tint overlay "+answer+": fill="+hex(target.fill)+", platformReady="+target.platformReady+", keyguard="+keyguard
                +", dozing="+dozing+", aodKnown="+target.aodKnown+", aodClear="+target.aodClear);
    }
    /** OEM applyMask installs a separate black AOD ColorDrawable over the body.
     * Retaining BlurProxy alone cannot remove it. Keep the exact OEM rounded-mask
     * class and native fading alpha, with a per-row copy and the awake scene tint.
     * This native setter event performs no recipe submission or per-frame lookup. */
    Drawable aodMask(Object extension,Drawable original) {
        Hooks access=access(extension);View body=body(access,extension);
        Target target=body==null?null:targets.get(body);
        if(released)return original;
        if(target==null) {
            // Not ours yet: remember the native mask, so the card that is registered later in AOD can still swap it.
            if(body!=null&&original!=null)pendingAodMasks.put(body,new WeakReference<>(original));
            return original;
        }
        if(original!=null&&original==target.aodMask)return original;
        // Observe null/unknown writes too, so a later policy refresh cannot
        // resurrect a removed mask or replace newer native material.
        target.aodMaskSource=new WeakReference<>(original);
        if(!keyguard||!target.aodKnown||target.aodClear
                ||!(materialWanted(target)||unifyAod)||access.aodMaskConstructor==null
                ||original==null||original.getClass()!=access.aodMaskConstructor.getDeclaringClass())return original;
        // The final overlay/gradient owns its configured alpha, mask and lightening in both scenes.
        // Keep the AOD crossfade a neutral native glass layer beneath it; tinting this mask again
        // produced an extra flat colour plate (and fill's alpha used to be forced to 255 here).
        Integer tint=target.colourLayer!=null?target.awakeTint:
                target.accent!=null?target.accent:target.fill!=null?Integer.valueOf(0xff000000|target.fill):target.awakeTint;
        if(tint==null)return original;
        try {
            if(target.aodMask==null)target.aodMask=(ColorDrawable)access.aodMaskConstructor.newInstance(body.getContext());
            int nativeColor=((ColorDrawable)original).getColor();
            int color=Math.round((nativeColor>>>24)*(tint>>>24)/255f)<<24|(tint&0x00ffffff);
            if(target.aodMask.getColor()!=color)target.aodMask.setColor(color);
            target.aodMask.setBounds(original.getBounds());
            diagnostic(target,"native AOD mask: isolated=true, media="+(target.accent!=null));
            traceMaterial(target,"aod_mask",color);
            return target.aodMask;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){return original;}
    }
    private boolean sampleAod(Target target,Object extension,Hooks access) {
        boolean clear=false,known=false;Object repository=null;
        if(access.aodRepository!=null&&access.aodGetter!=null)try {
            repository=access.aodRepository.get(extension);
            Object value=repository==null?null:access.aodGetter.invoke(repository);
            known=value instanceof Number&&Float.isFinite(((Number)value).floatValue());
            clear=known&&aodClear(((Number)value).floatValue());
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){ }
        if(target.aodRepository.get()!=repository)target.aodRepository=new WeakReference<>(repository);
        boolean changed=target.aodClear!=clear||target.aodKnown!=known;target.aodClear=clear;target.aodKnown=known;return changed;
    }
    /** Runs before the native setter. Never translates a lockscreen mode to a
     * shade mode. A missing real same-scene ordinary donor keeps OEM inputs. */
    Object mode(Object extension,Object incoming) {
        Hooks access=access(extension);View body=body(access,extension);
        if(body==null||incoming==null)return incoming;
        if(!Boolean.TRUE.equals(replaying.get())) {
            Native state=nativeBodies.get(body);
            if(state==null){state=new Native(body);nativeBodies.put(body,state);}
            state.extension=new WeakReference<>(extension);
            if(state.mode!=incoming){state.mode=incoming;state.dirty=true;}
        }
        Target target=targets.get(body);Native donor=target==null||!sceneAllowed(target)||!following(target)||asleep(target)||lockBlocked(target)?null:donor(body);
        return donor==null||donor.mode==null?incoming:donor.mode;
    }
    /** Called after native blur/palette/headsup updates. No frame polling. */
    boolean observed(Object extension) {
        Hooks access=access(extension);View body=body(access,extension);
        if(released||body==null||Boolean.TRUE.equals(replaying.get()))return false;
        try {
            Native state=nativeBodies.get(body);
            if(state==null){state=new Native(body);nativeBodies.put(body,state);}
            if(state.extension.get()!=extension)state.extension=new WeakReference<>(extension);
            // An ordinary donor can first become visible from native draw().
            // Read its first material once, then defer all mutation out of draw.
            // Stable draws reuse that snapshot until a native material event.
            if(Boolean.TRUE.equals(drawing.get())&&state.sampled&&!state.dirty)return false;
            Object mode=access.mode.get(extension);
            if((state.mode==null||!targets.containsKey(body))&&state.mode!=mode){state.mode=mode;state.dirty=true;}
            int tint=access.tint.getInt(body);if(tint==0)tint=access.normal.getInt(body);
            boolean locked=access.keyguard.getBoolean(body),floating=heads(body),ordinary=ordinary(body);
            Integer foreground=FluidCloudForeground.nativeColor(body);
            Target target=targets.get(body);
            boolean aodChanged=target!=null&&!Boolean.TRUE.equals(drawing.get())&&sampleAod(target,extension,access);
            boolean changed=state.dirty||state.tint==null||state.tint!=tint||state.keyguard!=locked||state.heads!=floating||state.ordinary!=ordinary
                    ||aodChanged||(foreground==null?state.foreground!=null:!foreground.equals(state.foreground));
            state.tint=tint;state.foreground=foreground;state.keyguard=locked;state.heads=floating;state.ordinary=ordinary;state.dirty=false;state.sampled=true;
            // OEM draw() also calls setDrawableMaterialColor. Identical material
            // values must not rebuild MetaBall colours or allocate snapshots.
            if(changed&&(state.ordinary||targets.containsKey(body)))refresh();
            return changed;
        }catch(ReflectiveOperationException|RuntimeException ignored){return false;}
    }
    /** Only native material events identify ordinary rows. The style consumes
     * these snapshots outside draw; no new row hook or per-frame tree scan. */
    ArrayList<View> ordinaryBodies() {
        ArrayList<View> result=new ArrayList<>();
        synchronized(nativeBodies){for(Native state:nativeBodies.values()) {
            View body=state.body.get();
            if(body!=null&&state.sampled&&state.ordinary&&!state.heads)result.add(body);
        }}
        return result;
    }
    void meta(Object handler,Context context,Object entry,Drawable drawable) {
        Hooks access=handler==null?null:hooks.get(handler.getClass().getClassLoader());
        if(released||access==null||context==null||entry==null||!access.metaType.isInstance(drawable))return;
        if(Boolean.TRUE.equals(replaying.get()))return;
        Meta previous=metas.get(drawable);
        if(previous!=null&&previous.handler.get()==handler&&previous.context.get()==context&&previous.entry.get()==entry) {
            // The native initializer just wrote fresh entry colours. Record
            // that event without replacing the binding or replaying it again.
            previous.applied=null;previous.gradient=false;markNative(entry,false,false);apply(previous);return;
        }
        Meta binding=new Meta(handler,context,entry,drawable,access);previous=metas.put(drawable,binding);
        if(previous!=null&&previous.entry.get()!=entry)markNative(previous.entry.get(),false,false);
        apply(binding);
    }
    /** Native custom tempDrawable is an additional opaque template base. It is
     * absent from regular notifications. Clear only during this exact body's
     * draw when a same-scene native material or accent gradient was applied. */
    TempScope draw(Object extension) {
        Hooks access=access(extension);View body=body(access,extension);
        Target target=body==null?null:targets.get(body);
        boolean held=holdingAod(target);
        boolean accentHeld=keyguard&&target!=null&&target.accent!=null;
        if(target==null||released||!held&&!accentHeld&&(asleep(target)||lockBlocked(target))||!(keyguard&&target.accent!=null||target.platformReady&&materialWanted(target)))return null;
        try {
            Drawable nativeTemp=(Drawable)access.temp.get(extension),replacement=null;
            if(nativeTemp==null)return null;
            boolean nativePlatform=target.platformReady&&materialWanted(target)
                    &&(target.accent==null||target.recipeNormalNotification&&target.colourLayer!=null);
            // Only the audited music template plate may be omitted. Unknown image/animation
            // backgrounds still draw unchanged even if the same row has proven native glass.
            if(nativePlatform&&target.accent!=null&&!immersiveSource(target,access,nativeTemp))return null;
            if(!nativePlatform&&nativeTemp!=null) {
                boolean gradient=keyguard&&target.accent!=null;
                if(gradient&&!immersiveSource(target,access,nativeTemp))return null;
                Native donor=gradient?null:donor(body);
                Integer color=gradient?target.accent:donor==null?null:donor.tint;
                if(color==null)return null;
                if(!Boolean.TRUE.equals(drawing.get()))prepareTemp(target,body,nativeTemp,color,gradient);
                else if(target.tempSource.get()!=nativeTemp||target.tempColor!=color||target.tempGradient!=gradient) {
                    requestRefresh();return null;
                }
                replacement=target.tempCopy;
                // Unknown image/animated material must stay native. No opaque
                // fallback rectangle is placed over template content.
                if(replacement==null||replacement==nativeTemp)return null;
                // Native setImmersiveProgress updates only this drawable's alpha.
                // The private gradient keeps that exact transition without a
                // per-frame clone or a compositor/material resubmission.
                if(replacement.getAlpha()!=nativeTemp.getAlpha())replacement.setAlpha(nativeTemp.getAlpha());
            }
            return new TempScope(access.temp,extension,replacement);
        }catch(ReflectiveOperationException|RuntimeException ignored){return null;}
    }
    void refresh() {
        if(released)return;
        materialEpoch++;
        if(Boolean.TRUE.equals(drawing.get())) {
            requestRefresh();
            return;
        }
        refreshNow();
    }
    private void requestRefresh() {
        if(!refreshPending){refreshPending=true;main.post(()->{refreshPending=false;if(!released)refreshNow();});}
    }
    private void refreshNow() {
        for(Target target:snapshotTargets()){rebuildBody(target);requestApply(target);refreshAodMask(target);}
        for(Meta binding:snapshotMetas())rebuild(binding);
    }
    private void refreshAodMask(Target target) {
        Drawable source=target.aodMaskSource.get();View body=target.body.get();
        if(source==null||body==null)return;
        Hooks access=hooks.get(body.getClass().getClassLoader());
        if(access==null||access.aodMaskSetter==null)return;
        try{access.aodMaskSetter.invoke(access.extension.get(body),source);}
        catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){ }
    }
    private void bindProxy(Target target) {
        View body=target.body.get();if(body==null)return;
        Hooks access=hooks.get(body.getClass().getClassLoader());if(access==null)return;
        try {
            Object extension=access.extension.get(body),proxy=access.bgProxy.invoke(extension);
            sampleAod(target,extension,access);
            Object before=target.proxy.get();if(before==proxy)return;
            if(before!=null)proxyTargets.remove(before);
            target.proxy=new WeakReference<>(proxy);target.platformReady=false;target.platformDrawable.clear();target.requestEpoch=-1;
            if(proxy!=null)proxyTargets.put(proxy,new WeakReference<>(target));
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){ }
    }
    private void rebuildBody(Target target) {
        View body=target.body.get();if(body==null)return;
        bindProxy(target);
        if(holdingAod(target))return;
        Hooks access=hooks.get(body.getClass().getClassLoader());if(access==null)return;
        Native original=nativeBodies.get(body),selected=sceneAllowed(target)&&following(target)&&!asleep(target)&&!lockBlocked(target)?donor(body):null;
        target.followed=selected!=null;
        try {
            Object extension=access.extension.get(body);
            Drawable nativeTemp=(Drawable)access.temp.get(extension);
            boolean gradient=keyguard&&target.accent!=null;
            Integer color=gradient?target.accent:selected==null?null:selected.tint;
            if(keyguard&&!lockBlocked(target))target.awakeTint=notificationColor(body);
            if(nativeTemp!=null&&color!=null&&(!gradient||immersiveSource(target,access,nativeTemp)))prepareTemp(target,body,nativeTemp,color,gradient);
        }catch(ReflectiveOperationException|RuntimeException ignored){ }
        if(original==null||original.mode==null)return;
        if(selected==null&&target.appliedMode==null)return;
        Object wanted=selected==null?original.mode:selected.mode;
        if(wanted==null||wanted==target.appliedMode)return;
        Boolean previous=replaying.get();replaying.set(Boolean.TRUE);
        try{Object extension=access.extension.get(body);access.blur.invoke(extension,wanted);target.appliedMode=selected==null?null:wanted;body.invalidate();}
        catch(ReflectiveOperationException|RuntimeException ignored){ }
        finally{if(previous==null)replaying.remove();else replaying.set(previous);}
    }
    private void rebuild(Meta binding) {
        Object handler=binding.handler.get(),entry=binding.entry.get();Context context=binding.context.get();Drawable drawable=binding.drawable.get();
        if(handler==null||entry==null||context==null||drawable==null)return;
        Integer wanted=color(entry);
        boolean gradient=keyguard&&accent(entry)!=null&&!compositorOwns(entry);
        if(wanted==null?binding.applied==null:wanted.equals(binding.applied)&&binding.gradient==gradient)return;
        Boolean previous=replaying.get();replaying.set(Boolean.TRUE);
        try {
            // Restore the latest native colours before each policy application;
            // entry JSON/album events may have changed since this registration.
            binding.access.metaUpdate.invoke(handler,context,entry,drawable);
            binding.applied=null;binding.gradient=false;markNative(entry,false,false);apply(binding);
        }catch(ReflectiveOperationException|RuntimeException ignored){ }
        finally{if(previous==null)replaying.remove();else replaying.set(previous);}
    }
    private void apply(Meta binding) {
        Drawable drawable=binding.drawable.get();Object entry=binding.entry.get();if(drawable==null||entry==null)return;
        Integer color=color(entry);if(color==null)return;
        boolean gradient=keyguard&&accent(entry)!=null&&!compositorOwns(entry);
        try {
            if(gradient)binding.access.gradient.invoke(drawable,GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{color,color&0x00ffffff});
            else binding.access.metaColor.invoke(drawable,0);
            binding.applied=color;binding.gradient=gradient;markNative(entry,true,gradient);drawable.invalidateSelf();
        }catch(ReflectiveOperationException|RuntimeException ignored){ }
    }
    private Integer color(Object entry) {
        if(compositorOwns(entry))return 0;
        Integer accent=keyguard?accent(entry):null;if(accent!=null)return accent;
        return null;
    }
    private boolean compositorOwns(Object entry) {
        synchronized(targets){for(Target target:targets.values())if(target.entry.get()==entry&&materialWanted(target)
                &&(holdingAod(target)||!asleep(target)&&!lockBlocked(target))&&target.platformReady
                &&(target.accent==null||target.recipeNormalNotification&&target.colourLayer!=null))return true;}
        return false;
    }

    /** A custom-row background color is a second plate on top of normal native
     * platform material. Removing that plate is scoped to the registered entry;
     * the music gradient uses one final colour layer once normal glass is proven;
     * the native MetaBall and immersive shape remain as the unproven-backend fallback. */
    boolean needsMeta(Object entry, boolean original) {
        if (released || entry == null) return original;
        synchronized(targets){for (Target target : targets.values()) if (target.entry.get() == entry && materialWanted(target)
                && (target.accent == null||target.recipeNormalNotification&&target.colourLayer!=null)
                && (holdingAod(target)||!asleep(target)&&!lockBlocked(target)) && target.platformReady) return false;}
        return original;
    }
    /** Records why a wanted material was not submitted, once per change of reason (never per frame). */
    private static String hex(Integer color){return color==null?"none":"#"+Integer.toHexString(color);}
    private PlatformScope skipped(Target target,String reason) {
        if(target!=null&&!reason.equals(target.lastReason)) {
            target.lastReason=reason;
            // Visible in logcat without the diagnostic switch (bounded by diagnostic()): why this card kept the system material.
            diagnostic(target,"material skipped: "+reason+", keyguard="+keyguard+", dozing="+dozing+", follow="+following(target)+", fill="+hex(target.fill)
                    +", accent="+hex(target.accent)+", aodKnown="+target.aodKnown+", aodClear="+target.aodClear+", composed="+target.composed);
            if(ModuleDiagnostics.enabled())ModuleDiagnostics.fact("fluid","fluid","材质未提交","host","notification",
                    "applied",false,"reason",reason,"keyguard",keyguard,"dozing",dozing,"native_follow",following(target),
                    "backend",backendName(target.body.get()),"mode",String.valueOf(target.recipeMode==null?"none":target.recipeMode).replaceAll("[^A-Za-z0-9_]",""),
                    "aod_known",target.aodKnown);
        }
        return null;
    }
    PlatformScope platform(Object proxy) { return platform(proxy,null); }
    PlatformScope platform(Object proxy,Method submitted) {
        Hooks access = proxy == null ? null : hooks.get(proxy.getClass().getClassLoader());
        if (access == null || released || Boolean.TRUE.equals(confirmingPlatform.get())) return null;
        if(Boolean.TRUE.equals(drawing.get())){Target target=proxyTarget(proxy);if(target!=null)requestApply(target);return null;}
        try {
            View body = (View) access.proxyView.invoke(proxy); Target target = targets.get(body);
            if (target == null) return null;
            // Native void submission can return without any usable drawable.
            // Revoke the previous success before validating this submission.
            target.platformReady=false;
            if (!materialWanted(target)) return skipped(target,"not_wanted");
            if (asleep(target)) return skipped(target,"dozing");
            if (!keyguard&&target.accent != null) return skipped(target,"accent_in_shade");
            Object extension = access.extension.get(body), manager = access.manager.get(extension);
            sampleAod(target,extension,access);
            if(lockBlocked(target)){diagnostic(target,"native AOD material: doze="+dozing+", clear="+target.aodClear
                    +", repository="+(target.aodRepository.get()!=null));return skipped(target,"aod_blocked");}
            if (manager == null) return skipped(target,"no_manager");
            int backend=submitted==null||submitted.equals(access.platformApply)?0:
                    submitted.equals(access.motionApply)?1:submitted.equals(access.wallpaperApply)?2:-1;
            if(backend<0)return skipped(target,"unknown_backend");
            Native selected = donor(body), original = nativeBodies.get(body);
            Object mode = selected != null ? selected.mode : heads(body) ? original == null ? null : original.mode : access.globalMode.invoke(manager);
            if(!keyguard&&heads(body)&&selected==null) {
                // CUSTOM rows are not ExpandableNotificationRow. Their native
                // notifyBlurParamChanged returns early and can leave SHADE or
                // UNINITIATED in the extension. Ask the same OEM controller as
                // ordinary heads-up notifications; true is its native forced
                // heads-up input, including actual dark-icon/theme capability.
                Object material=access.nativeMaterial.invoke(extension);
                Object live=material==null?null:access.headsMode.invoke(material,null,true);
                if(live instanceof Enum&&!((Enum<?>)live).name().equals("UNINITIATED"))mode=live;
            }
            if(keyguard) {
                // The native per-row WALLPAPER region owns lockscreen material;
                // never substitute a SHADE/HEADS_UP recipe during transition.
                Object live=access.globalMode.invoke(manager);
                Object local=original==null?access.mode.get(extension):original.mode;
                mode=wallpaperMode(local)?local:live;
                if(!wallpaperMode(mode)){diagnostic(target,"native lockscreen material: no WALLPAPER recipe");return skipped(target,"no_wallpaper_recipe");}
            } else if(wallpaperMode(mode))return skipped(target,"wallpaper_mode_in_shade");
            if (!(mode instanceof Enum) || ((Enum<?>) mode).name().equals("UNINITIATED")) return skipped(target,"mode_uninitiated");
            // A floating target without a floating recipe keeps its OEM style.
            if (heads(body) && selected == null && !headsMode(mode)) return skipped(target,"no_floating_recipe");
            int width = body.getWidth(), height = ((Number) access.actualHeight.invoke(body)).intValue();
            if (height <= 0) height = body.getHeight();
            if (width <= 0 || height <= 0) return skipped(target,"no_size");
            Object config = access.proxyConfig.invoke(proxy);
            if (config == null) return skipped(target,"no_config");
            boolean motion=Boolean.TRUE.equals(access.motion.invoke(manager));
            if (target.recipe == null || target.recipeEpoch != materialEpoch || target.recipeMode != mode
                    || target.recipeWidth != width || target.recipeHeight != height || target.recipeMotion != motion
                    || target.recipeBackend!=backend || target.recipeManager.get() != manager) {
                // The music gradient is a colour layer, not a different opaque material. When
                // following is requested it uses this row's normal WALLPAPER glass as other cards do.
                boolean normalRecipe=following(target)||keyguard&&target.lockLook;
                Object mix = normalRecipe ? access.mix.invoke(manager, access.normalType, mode) : access.configMix.get(config);
                Object stroke = normalRecipe ? access.stroke.invoke(access.params, access.normalType, mode) : access.configStroke.get(config);
                Object shadow = normalRecipe ? access.shadow.invoke(access.params, access.normalType, mode) : access.configShadow.get(config);
                Object motionMix=backend==1?(normalRecipe?access.motionMix.invoke(manager,access.normalType,mode):access.configMotionMix.get(config)):null;
                if (mix == null) return skipped(target,"no_mix");
                if(backend==1&&motionMix==null)return skipped(target,"no_motion_mix");
                // Registered lockscreen cards receive the selected media hue. Copy every native
                // control before the normal size updater; unknown APIs keep the complete OEM recipe.
                boolean highlightTinted=false;
                if(keyguard&&!heads(body)&&target.highlightColor!=null&&access.highlightTint!=null) {
                    Object[] tinted=access.highlightTint.tint(stroke,shadow,target.highlightColor);
                    if(tinted!=null){stroke=tinted[0];shadow=tinted[1];highlightTinted=stroke!=null||shadow!=null;}
                }
                // OEM can intentionally omit stroke/shadow for this wallpaper
                // or optical capability; null is a native recipe, not failure.
                if(normalRecipe&&stroke!=null)access.sizeStroke.invoke(access.params, body.getContext(), stroke, (float) width, (float) height, mode);
                if(normalRecipe&&shadow!=null)access.sizeShadow.invoke(access.params, body.getContext(), shadow, (float) width, (float) height, mode);
                // MixColor's top/bottom are constant blend layers, not spatial
                // stops. The lockscreen spatial gradient belongs to the native
                // final card colour layer, with the immersive tempDrawable as the native fallback.
                if(target.fill!=null||target.fillOpacity!=1f) {
                    mix=access.withFill(mix,target.fill,target.fillOpacity);
                    if(mix==null)return skipped(target,"fill_unsupported_mix");
                    if(backend==1){motionMix=access.withFill(motionMix,target.fill,target.fillOpacity);if(motionMix==null)return skipped(target,"fill_unsupported_motion");}
                }
                // Immutable colour copies create a new BlurMixConfig. Apply the
                // OEM geometry to that owned copy, not a factory/shared source.
                if (normalRecipe) access.mirror.invoke(access.params,access.normalType,mode,mix,motion);
                target.recipe = new Object[]{mix, stroke, shadow,motionMix}; target.recipeEpoch = materialEpoch;
                target.recipeBackend=backend;
                target.recipePlainNotification=normalRecipe&&target.fill==null&&target.fillOpacity==1f;
                target.recipeNormalNotification=normalRecipe;
                target.recipeHighlightTinted=highlightTinted;
                target.recipeMode = mode; target.recipeMotion=motion;target.recipeWidth = width; target.recipeHeight = height;
                target.recipeManager = new WeakReference<>(manager);
            }
            return new PlatformScope(access, target, proxy, config, target.recipe);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return null; }
    }
    /** A native draw must not resubmit this target's raw CUSTOM_CARD recipe to
     * the same cached PlatformBlurDrawable. Only owned following targets defer;
     * the latest geometry/policy is submitted once outside the draw stack. */
    boolean deferPlatform(Object proxy) {
        if(!released&&!Boolean.TRUE.equals(replaying.get())&&!Boolean.TRUE.equals(confirmingPlatform.get())
                &&holdingAod(proxyTarget(proxy)))return true;
        if(released||!Boolean.TRUE.equals(drawing.get())
                ||Boolean.TRUE.equals(replaying.get())||Boolean.TRUE.equals(confirmingPlatform.get()))return false;
        Target target=proxyTarget(proxy);if(target==null||asleep(target)||!materialWanted(target)||!keyguard&&target.accent!=null)return false;
        if(lockBlocked(target)||keyguard&&!wallpaperMode(target.recipeMode))return false;
        requestApply(target);return true;
    }
    void proxyChanged(Object proxy) {
        if(released||Boolean.TRUE.equals(replaying.get()))return;
        Target target=proxyTarget(proxy);if(target==null)return;
        if(holdingAod(target))return;
        if(Boolean.TRUE.equals(drawing.get())){requestApply(target);return;}
        Hooks access=hooks.get(proxy.getClass().getClassLoader());if(access==null)return;
        try {
            View body=target.body.get();if(body==null)return;
            boolean wanted = materialWanted(target) && !asleep(target) && !lockBlocked(target) && (keyguard||target.accent==null);
            if (!wanted && !target.platformReady) return;
            int height = ((Number) access.actualHeight.invoke(body)).intValue(); if(height<=0)height=body.getHeight();
            if (wanted && target.platformReady && target.appliedEpoch == materialEpoch
                    && target.appliedWidth == body.getWidth() && target.appliedHeight == height) return;
            Boolean previous = replaying.get(); replaying.set(Boolean.TRUE);
            try { access.applyConfig.invoke(proxy); }
            finally { if(previous==null)replaying.remove();else replaying.set(previous); }
            if (!wanted) {
                target.platformReady = false;
                // Ending ownership restored the raw OEM recipe. The next AOD
                // has no composition to hold, even while keyguard stays shown.
                // A temporary doze/unknown-progress block retains its history.
                if (!materialWanted(target)) target.composed = false;
            }
            for(Meta meta:snapshotMetas())if(meta.entry.get()==target.entry.get())rebuild(meta);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { revokePlatform(target); }
    }
    Drawable drawable(View body, Drawable original) {
        Target target = targets.get(body);
        if(!released&&holdingAod(target)) {
            Drawable held=target.platformDrawable.get();return held==null?original:held;
        }
        if (target == null || released || !materialWanted(target) || asleep(target) || lockBlocked(target) || !keyguard&&target.accent != null) return original;
        if(Boolean.TRUE.equals(drawing.get())) {
            requestApply(target);
            Drawable cached=target.platformReady?target.platformDrawable.get():null;
            return cached==null?original:cached;
        }
        Hooks access = hooks.get(body.getClass().getClassLoader());
        if (access == null) { revokePlatform(target); return original; }
        try {
            Object extension = access.extension.get(body), proxy = access.bgProxy.invoke(extension);
            if (proxy == null) { revokePlatform(target); return original; }
            proxyChanged(proxy);
            if (!target.platformReady) { revokePlatform(target); return original; }
            Object own = access.proxyDrawable.invoke(proxy, original);
            if (validPlatform(access,target,own)) return (Drawable)own;
            revokePlatform(target);
            return original;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            revokePlatform(target); return original;
        }
    }
    private void revokePlatform(Target target) {
        if (target == null) return;
        target.platformReady=false;
        target.platformDrawable.clear();
        if(Boolean.TRUE.equals(drawing.get())){requestApply(target);return;}
        // The fallback must restore the latest template material too: a previous
        // valid submission may have already cleared its MetaBall colour.
        for(Meta meta:snapshotMetas())if(meta.entry.get()==target.entry.get())rebuild(meta);
    }
    private Target proxyTarget(Object proxy) {
        WeakReference<Target> reference=proxyTargets.get(proxy);return reference==null?null:reference.get();
    }
    void geometry(View body) {
        Target target=targets.get(body);if(target==null||released)return;
        Hooks access=hooks.get(body.getClass().getClassLoader());if(access==null)return;
        try {
            int width=body.getWidth(),height=body.getHeight(),actual=((Number)access.actualHeight.invoke(body)).intValue();
            if(target.geometryKnown&&target.geometryWidth==width&&target.geometryHeight==height&&target.geometryActual==actual)return;
            target.geometryKnown=true;target.geometryWidth=width;target.geometryHeight=height;target.geometryActual=actual;
            target.geometryVersion++;target.requestEpoch=-1;bindProxy(target);requestApply(target);
        }catch(ReflectiveOperationException|RuntimeException unavailable){ }
    }
    private void requestApply(Target target) {
        View body=target.body.get();if(body==null||released)return;
        if(holdingAod(target))return;
        if((!materialWanted(target)||asleep(target)||lockBlocked(target)||!keyguard&&target.accent!=null)&&!target.platformReady)return;
        int width=body.getWidth(),height=body.getHeight();
        if(target.pending||target.requestEpoch==materialEpoch&&target.requestWidth==width
                &&target.requestHeight==height&&target.requestGeometry==target.geometryVersion)return;
        target.requestEpoch=materialEpoch;target.requestWidth=width;target.requestHeight=height;
        target.requestGeometry=target.geometryVersion;target.pending=true;
        main.post(()->{
            target.pending=false;View current=target.body.get();
            if(released||current==null||targets.get(current)!=target)return;
            bindProxy(target);Object proxy=target.proxy.get();
            target.requestEpoch=materialEpoch;target.requestWidth=current.getWidth();target.requestHeight=current.getHeight();
            target.requestGeometry=target.geometryVersion;
            if(proxy==null){revokePlatform(target);return;}
            proxyChanged(proxy);current.invalidate();
        });
    }
    private Integer accent(Object entry) {
        for(Target target:snapshotTargets())if(target.entry.get()==entry&&sceneAllowed(target))return target.accent;
        return null;
    }
    private void markNative(Object entry,boolean fill,boolean gradient){for(Target target:snapshotTargets())if(target.entry.get()==entry){target.nativeFill=fill;target.nativeGradient=gradient;}}
    private static void prepareTemp(Target target,View body,Drawable source,int color,boolean gradient) {
        if(target.tempSource.get()==source&&target.tempColor==color&&target.tempGradient==gradient)return;
        target.tempSource=new WeakReference<>(source);target.tempColor=color;target.tempGradient=gradient;
        Drawable copy=gradient?FluidCloudFallback.gradient(source,body.getResources(),color)
                :FluidCloudFallback.derive(source,body.getResources(),color,false,false);
        target.tempCopy=copy==source?null:copy;
        if(target.tempCopy!=null)target.tempCopy.setCallback(null);
    }
    private static boolean immersiveSource(Target target,Hooks access,Drawable source)throws ReflectiveOperationException {
        Object row=target.row.get();
        return row!=null&&access.customRowType.isInstance(row)&&access.immersive.get(row)==source
                &&source.getClass()==GradientDrawable.class;
    }
    private Native donor(View target) {
        // Lockscreen uses its own native WALLPAPER factory. Never borrow a
        // shade donor's palette or a drawable from another lockscreen card.
        if(target==null||keyguard)return null;
        boolean floating=heads(target);
        synchronized(nativeBodies){for(Native candidate:nativeBodies.values()) {
            View body=candidate.body.get();
            if(candidate.ordinary&&candidate.mode instanceof Enum&&!((Enum<?>)candidate.mode).name().equals("UNINITIATED")&&body!=null&&body.isAttachedToWindow()
                    &&candidate.heads==floating&&candidate.keyguard==keyguard
                    &&body.getResources()==target.getResources())return candidate;
        }}
        return null;
    }
    private static boolean wallpaperMode(Object mode) {
        if(!(mode instanceof Enum))return false;
        String name=((Enum<?>)mode).name();return name.equals("WALLPAPER_A")||name.equals("WALLPAPER_B");
    }
    private static boolean headsMode(Object mode) {
        return mode instanceof Enum&&((Enum<?>)mode).name().startsWith("HEADS_UP");
    }
    /** Ending an ordinary colour policy keeps its native event snapshot. A
     * real detach/recycle clears it, so the next row cannot inherit stale data. */
    void stopOrdinaryFollowing(View body) { detach(body,true); }
    void detach(View body) { detach(body,false); }
    private void detach(View body,boolean keepNative) {
        Target target=targets.remove(body);Native original=keepNative?nativeBodies.get(body):nativeBodies.remove(body);
        if(target==null)return;
        Object proxy=target.proxy.get();if(proxy!=null)proxyTargets.remove(proxy);
        refreshAodMask(target);
        restorePlatform(body, target);
        // Recycled backgrounds must not retain another scene's blur mode. Only
        // undo the exact mode we last applied; a newer native write owns itself.
        Hooks access=body==null?null:hooks.get(body.getClass().getClassLoader());
        if(access!=null&&original!=null&&original.mode!=null&&target.appliedMode!=null) {
            Boolean previous=replaying.get();replaying.set(Boolean.TRUE);
            try{Object extension=access.extension.get(body);
                if(access.mode.get(extension)==target.appliedMode)access.blur.invoke(extension,original.mode);
            }catch(ReflectiveOperationException|RuntimeException ignored){ }
            finally{if(previous==null)replaying.remove();else replaying.set(previous);}
        }
        Object entry=target.entry.get();
        for(Meta binding:snapshotMetas())if(binding.entry.get()==entry){rebuild(binding);Drawable drawable=binding.drawable.get();if(drawable!=null)metas.remove(drawable);}
    }
    void release() {
        follow=false;keyguard=false;
        for(Target target:snapshotTargets()){target.accent=null;target.fill=null;target.colourLayer=null;target.highlightColor=null;target.fillOpacity=1f;}
        refresh();for(Target target:snapshotTargets())restorePlatform(target.body.get(),target);
        released=true;targets.clear();metas.clear();nativeBodies.clear();headsMaterials.clear();proxyTargets.clear();aodBoundaries.clear();hooks.clear();replaying.remove();confirmingPlatform.remove();drawing.remove();
    }
    private void restorePlatform(View body, Target target) {
        Hooks access = body == null ? null : hooks.get(body.getClass().getClassLoader());
        if (access == null || !target.platformReady) return;
        try { Object extension = access.extension.get(body), proxy = access.bgProxy.invoke(extension);
            if(proxy != null) access.applyConfig.invoke(proxy);
        } catch(ReflectiveOperationException | RuntimeException | LinkageError unavailable) { }
        target.platformReady=false;
    }
    private Hooks access(Object extension){return extension==null?null:hooks.get(extension.getClass().getClassLoader());}
    private static View body(Hooks access,Object extension){if(access==null||extension==null)return null;try{return (View)access.host.invoke(extension);}catch(ReflectiveOperationException|RuntimeException unavailable){return null;}}
    private static boolean ordinary(View body) {
        for(ViewParent parent=body.getParent();parent instanceof View;parent=parent.getParent()) {
            String name=parent.getClass().getName();if(name.equals(FluidCloudStyle.CUSTOM_ROW))return false;
            for(Class<?> type=parent.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals(ROW))return true;
        }
        return false;
    }
    private static boolean heads(View body){for(ViewParent parent=body.getParent();parent instanceof View;parent=parent.getParent())if(parent.getClass().getName().equals(HEADS))return true;return false;}
    private ArrayList<Target> snapshotTargets(){synchronized(targets){return new ArrayList<>(targets.values());}}
    private ArrayList<Meta> snapshotMetas(){synchronized(metas){return new ArrayList<>(metas.values());}}
    static final class TempScope implements AutoCloseable {
        private final Field field;private final Object owner,value,replacement;private boolean closed;
        TempScope(Field field,Object owner,Object replacement)throws ReflectiveOperationException{this.field=field;this.owner=owner;this.replacement=replacement;value=field.get(owner);field.set(owner,replacement);}
        public void close(){if(closed)return;closed=true;try{if(field.get(owner)==replacement)field.set(owner,value);}catch(ReflectiveOperationException|RuntimeException ignored){ }}
    }
    static final class PlatformScope implements AutoCloseable {
        private final Hooks access; private final Target target; private final Object proxy,config;
        private final Object[] before, applied; private boolean closed;
        PlatformScope(Hooks access, Target target, Object proxy, Object config, Object[] recipe) throws ReflectiveOperationException {
            this.access=access;this.target=target;this.proxy=proxy;this.config=config;applied=recipe;
            before=new Object[]{access.configMix.get(config),access.configStroke.get(config),access.configShadow.get(config),access.configMotionMix.get(config)};
            try { access.configMix.set(config,recipe[0]);access.configStroke.set(config,recipe[1]);access.configShadow.set(config,recipe[2]);
                if(target.recipeBackend==1)access.configMotionMix.set(config,recipe[3]); }
            catch (ReflectiveOperationException | RuntimeException failed) { restore();throw failed; }
        }
        void completed(long epoch) { target.platformReady=true;target.appliedEpoch=epoch;target.appliedWidth=target.recipeWidth;target.appliedHeight=target.recipeHeight; }
        private void restore() {
            Field[] fields={access.configMix,access.configStroke,access.configShadow,access.configMotionMix};
            for(int i=0;i<fields.length;i++)try{if(i==3&&target.recipeBackend!=1)continue;
                if(fields[i].get(config)==applied[i])fields[i].set(config,before[i]);}catch(ReflectiveOperationException|RuntimeException ignored){ }
        }
        public void close(){if(closed)return;closed=true;restore();}
    }
    void platformCompleted(PlatformScope scope) {
        if(scope==null)return;
        Boolean previous=replaying.get(),checking=confirmingPlatform.get();
        replaying.set(Boolean.TRUE);confirmingPlatform.set(Boolean.TRUE);
        try {
            Object own=scope.access.proxyDrawable.invoke(scope.proxy,(Drawable)null);
            if(validPlatform(scope.access,scope.target,own)){
                scope.completed(materialEpoch);scope.target.platformDrawable=new WeakReference<>((Drawable)own);
                scope.target.composed=keyguard;
                // Proof arrives after the queued native submit. Retire any earlier fallback
                // tint at that event, instead of waiting for a future palette/album change.
                for(Meta meta:snapshotMetas())if(meta.entry.get()==scope.target.entry.get())rebuild(meta);
                traceMaterial(scope.target,"applied",null);
                diagnostic(scope.target,"material applied: mode="+scope.target.recipeMode+", backend="+scope.target.recipeBackend
                        +", width="+scope.target.recipeWidth+", height="+scope.target.recipeHeight
                        +", accent="+(scope.target.accent!=null)+", fill="+(scope.target.fill!=null));
                if(!"ok".equals(scope.target.lastReason)) {
                    scope.target.lastReason="ok";
                    if(ModuleDiagnostics.enabled())ModuleDiagnostics.fact("fluid","fluid","材质已提交","host","notification","applied",true,
                            "reason","ok","backend",backendName(scope.target.body.get()),"keyguard",keyguard,"dozing",dozing,
                            "native_follow",following(scope.target),"fill_argb",scope.target.fill==null?"none":"#"+Integer.toHexString(scope.target.fill),
                            "opacity",scope.target.fillOpacity,"mask",scope.target.accent!=null);
                }
            } else {
                diagnostic(scope.target,"native material: backend has no confirmed drawable");revokePlatform(scope.target);
                skipped(scope.target,"backend_unconfirmed");
                traceMaterial(scope.target,"fallback",null);
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){revokePlatform(scope.target);}
        finally{if(previous==null)replaying.remove();else replaying.set(previous);if(checking==null)confirmingPlatform.remove();else confirmingPlatform.set(checking);}
    }
    /** At most four non-sensitive records per coloured row, only at material/mask events and only
     * in the existing diagnostic mode. Never poll a view tree or log from a draw frame. */
    private void traceMaterial(Target target,String event,Integer mask) {
        if(!ModuleDiagnostics.enabled()||!keyguard||target==null||target.traceCount>=4
                ||target.accent==null&&target.fill==null)return;
        String state=event+"|"+target.recipeNormalNotification+"|"+hex(target.colourLayer)+"|"+hex(mask);
        if(state.equals(target.traceState))return;
        target.traceState=state;target.traceCount++;
        android.util.Log.i("C17GlassMaterial",event+" normal="+target.recipeNormalNotification
                +" backend="+target.recipeBackend+" ready="+target.platformReady+" dozing="+dozing
                +" accent="+hex(target.accent)+" fill="+hex(target.fill)+" layer="+hex(target.colourLayer)
                +" mask="+hex(mask)+" nativeGradient="+target.nativeGradient);
    }
    private static boolean validPlatform(Hooks access,Target target,Object drawable)throws ReflectiveOperationException {
        if(drawable==null||target.recipe==null)return false;
        if(target.recipeBackend==0)return access.platformDrawableType.isInstance(drawable)&&access.platformNativeDrawable.invoke(drawable)!=null;
        if(target.recipeBackend==1)return target.recipe[3]!=null&&access.motionDrawableType.isInstance(drawable);
        // BlendWallpaper can lazily create its drawable without submitting a
        // recipe. Its own current mix and host prove this target's submission;
        // otherwise its opaque CUSTOM MetaBall/template must remain available.
        return target.recipeBackend==2&&access.wallpaperDrawableType.isInstance(drawable)
                &&access.wallpaperNativeDrawable.invoke(drawable)!=null
                &&access.wallpaperHost.invoke(drawable)==target.body.get()
                &&access.wallpaperMix.invoke(drawable)==target.recipe[0];
    }
    static final class Hooks {
        final Method blur,palette,materialEvent,headsup,metaUpdate,gradient,metaColor,host,draw;
        final Method platformApply,motionApply,wallpaperApply,applyConfig,proxyDrawable,platformNativeDrawable,wallpaperNativeDrawable,wallpaperMix,wallpaperHost,nativeMaterial,headsMode,motionMix;
        final Method needMeta,proxyView,proxyConfig,bgProxy,globalMode,mix,stroke,shadow,sizeStroke,sizeShadow,actualHeight,motion,mirror,motionEvent;
        final Field manager,configMix,configStroke,configShadow,configMotionMix; final Object normalType,params;
        final Class<?> platformDrawableType,motionDrawableType,wallpaperDrawableType;
        final Field extension,mode,temp,tint,normal,keyguard;final Class<?> metaType;
        final Class<?> customRowType;final Field immersive;
        final Field aodRepository;final Method aodGetter,aodSetter,aodMaskSetter;
        final java.lang.reflect.Constructor<?> aodMaskConstructor;
        final Class<?> multiType,singleType;final Method mixForeground,mixBackground,mixSingle,multiCopy,singleCopy,colorCopy,colorMode,colorTop,colorBottom;
        final Method mixAlpha,mixMirror,mixPlaceholder,setMixAlpha,setMixMirror,setMixPlaceholder;
        final Method buildMotion,motionArray;
        final Class<?> shaderType;final Method shaderForeground,shaderBackground,shaderCopy,shaderColorCopy,shaderTopMode,shaderBottomMode,shaderTop,shaderBottom;
        final SceneHighlightTint highlightTint;
        private Object withFill(Object mix,Integer fill,float opacity)throws ReflectiveOperationException {
            if(shaderType.isInstance(mix))return copyShader(mix,
                    shaderFillColor(shaderForeground.invoke(mix),fill,opacity),shaderFillColor(shaderBackground.invoke(mix),fill,opacity));
            if(multiType.isInstance(mix))return copyBase(mix,multiCopy.invoke(mix,
                    fillColor(mixForeground.invoke(mix),fill,opacity),fillColor(mixBackground.invoke(mix),fill,opacity)));
            if(singleType.isInstance(mix))return copyBase(mix,singleCopy.invoke(mix,fillColor(mixSingle.invoke(mix),fill,opacity)));
            return null;
        }
        private Object copyBase(Object source,Object copy)throws ReflectiveOperationException {
            if(copy==null||copy==source)return null;
            // OEM Kotlin copy() only copies colour constructor arguments. These
            // superclass compositor controls must retain their current values.
            setMixAlpha.invoke(copy,mixAlpha.invoke(source));setMixMirror.invoke(copy,mixMirror.invoke(source));
            setMixPlaceholder.invoke(copy,mixPlaceholder.invoke(source));return copy;
        }
        private Object copyShader(Object source,Object foreground,Object background)throws ReflectiveOperationException {
            // The OEM copy constructor retains its shader modes and compositor
            // controls while allocating an independent derived-parameter buffer.
            return shaderCopy.invoke(source,foreground,background,mixMirror.invoke(source),mixAlpha.invoke(source),mixPlaceholder.invoke(source));
        }
        private Object shaderFillColor(Object color,Integer fill,float opacity)throws ReflectiveOperationException {
            int top=((Number)shaderTop.invoke(color)).intValue(),bottom=((Number)shaderBottom.invoke(color)).intValue();
            return shaderColorCopy.invoke(color,shaderTopMode.invoke(color),
                    fill==null?FluidCloudStyle.materialOpacity(top,opacity):FluidCloudStyle.materialFill(top,fill),shaderBottomMode.invoke(color),
                    fill==null?FluidCloudStyle.materialOpacity(bottom,opacity):FluidCloudStyle.materialFill(bottom,fill));
        }
        private Object fillColor(Object color,Integer fill,float opacity)throws ReflectiveOperationException {
            int mode=((Number)colorMode.invoke(color)).intValue(),top=((Number)colorTop.invoke(color)).intValue(),bottom=((Number)colorBottom.invoke(color)).intValue();
            return colorCopy.invoke(color,mode,fill==null?FluidCloudStyle.materialOpacity(top,opacity):FluidCloudStyle.materialFill(top,fill),
                    fill==null?FluidCloudStyle.materialOpacity(bottom,opacity):FluidCloudStyle.materialFill(bottom,fill));
        }
        Hooks(ClassLoader loader)throws ReflectiveOperationException {
            highlightTint=SceneHighlightTint.resolve(loader);
            Class<?> body=loader.loadClass(BODY),ext=loader.loadClass(EXT),base=loader.loadClass(EXT_BASE),modeType=loader.loadClass(MODE);
            metaType=loader.loadClass(META);
            customRowType=loader.loadClass(FluidCloudStyle.CUSTOM_ROW);
            immersive=customRowType.getDeclaredField("immersiveCustomDrawable");
            if(immersive.getType()!=Drawable.class)throw new NoSuchFieldException("Native immersive shape changed");
            immersive.setAccessible(true);
            host=base.getDeclaredMethod("getBgView");extension=body.getDeclaredField("mExt");
            mode=ext.getDeclaredField("notificationBlurMode");temp=ext.getDeclaredField("tempDrawable");
            tint=body.getDeclaredField("mTintColor");keyguard=body.getDeclaredField("mOnKeyguard");
            normal=body.getDeclaredField("mNormalColor");
            if(host.getReturnType()!=body||!Drawable.class.isAssignableFrom(metaType)||mode.getType()!=modeType
                    ||temp.getType()!=Drawable.class||tint.getType()!=Integer.TYPE||keyguard.getType()!=Boolean.TYPE)throw new NoSuchFieldException("Native Live Alert scene material changed");
            blur=ext.getDeclaredMethod("notifyBlurParamChanged",modeType);
            draw=ext.getDeclaredMethod("draw",android.graphics.Canvas.class,Drawable.class);
            palette=ext.getDeclaredMethod("setDrawableMaterialColor");materialEvent=ext.getDeclaredMethod("notifyMaterialColorChanged");headsup=ext.getDeclaredMethod("onHeadsUpStateChanged",Boolean.TYPE);
            metaUpdate=loader.loadClass(HANDLER).getDeclaredMethod("updateDrawableMetaColor",Context.class,loader.loadClass(ENTRY),metaType);
            gradient=metaType.getMethod("setGradientColors",GradientDrawable.Orientation.class,int[].class);
            metaColor=metaType.getMethod("setMetaColor",Integer.TYPE);
            Class<?> proxy=loader.loadClass("com.oplusos.systemui.common.blurability.ViewBlurProxy");
            Class<?> blurManager=loader.loadClass("com.oplus.systemui.notification.blur.ViewBlurManager");
            Class<?> cardType=loader.loadClass("com.oplus.systemui.notification.blur.ViewBlurManager$CardType");
            Class<?> paramsType=loader.loadClass("com.oplus.systemui.notification.blur.NotificationPlatFormBlurParamsManager");
            Class<?> config=loader.loadClass("com.oplusos.systemui.common.blurability.BlurConfig");
            Class<?> mixType=loader.loadClass("com.oplusos.systemui.common.blurability.BlurMixConfig");
            // Shader mixes override these controls with their own backing fields.
            // Virtual accessors retain the concrete compositor's actual values.
            mixAlpha=mixType.getDeclaredMethod("getAlphaWithBlurAmount");mixMirror=mixType.getDeclaredMethod("getMirrorScale");
            mixPlaceholder=mixType.getDeclaredMethod("getPlaceHolderColor");
            setMixAlpha=mixType.getDeclaredMethod("setAlphaWithBlurAmount",Boolean.TYPE);setMixMirror=mixType.getDeclaredMethod("setMirrorScale",Float.TYPE);
            setMixPlaceholder=mixType.getDeclaredMethod("setPlaceHolderColor",Integer.TYPE);
            if(mixAlpha.getReturnType()!=Boolean.TYPE||mixMirror.getReturnType()!=Float.TYPE||mixPlaceholder.getReturnType()!=Integer.TYPE
                    ||setMixAlpha.getReturnType()!=Void.TYPE||setMixMirror.getReturnType()!=Void.TYPE||setMixPlaceholder.getReturnType()!=Void.TYPE)
                throw new NoSuchMethodException("Live Alert mix superclass controls changed");
            for(Method method:new Method[]{mixAlpha,mixMirror,mixPlaceholder,setMixAlpha,setMixMirror,setMixPlaceholder})method.setAccessible(true);
            shaderType=loader.loadClass("com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixMultiWithShader");
            Class<?> shaderColor=loader.loadClass("com.oplusos.systemui.common.blurability.MixColorWithShader");
            shaderForeground=shaderType.getDeclaredMethod("getForegroundShaderParam");shaderBackground=shaderType.getDeclaredMethod("getBackgroundShaderParam");
            shaderCopy=shaderType.getDeclaredMethod("copy",shaderColor,shaderColor,Float.TYPE,Boolean.TYPE,Integer.TYPE);
            shaderColorCopy=shaderColor.getDeclaredMethod("copy",Integer.TYPE,Integer.TYPE,Integer.TYPE,Integer.TYPE);
            shaderTopMode=shaderColor.getDeclaredMethod("getTopMode");shaderBottomMode=shaderColor.getDeclaredMethod("getBottomMode");
            shaderTop=shaderColor.getDeclaredMethod("getTopLayerColor");shaderBottom=shaderColor.getDeclaredMethod("getBottomLayerColor");
            if(!mixType.isAssignableFrom(shaderType)||shaderCopy.getReturnType()!=shaderType||shaderColorCopy.getReturnType()!=shaderColor
                    ||shaderTopMode.getReturnType()!=Integer.TYPE||shaderBottomMode.getReturnType()!=Integer.TYPE
                    ||shaderTop.getReturnType()!=Integer.TYPE||shaderBottom.getReturnType()!=Integer.TYPE)
                throw new NoSuchMethodException("Live Alert shader material copy changed");
            for(Method method:new Method[]{shaderForeground,shaderBackground,shaderCopy,shaderColorCopy,shaderTopMode,shaderBottomMode,shaderTop,shaderBottom})method.setAccessible(true);
            multiType=loader.loadClass("com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixMulti");
            singleType=loader.loadClass("com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixSingle");
            Class<?> mixColor=loader.loadClass("com.oplusos.systemui.common.blurability.MixColor");
            Method builder=null,array=null;
            try {
                Class<?> paramType=loader.loadClass("com.oplus.graphics.OplusBlurParam");
                builder=proxy.getDeclaredMethod("buildMotionBlurParam",mixColor,Float.class);
                array=paramType.getDeclaredMethod("toFloatArray");
                if(builder.getReturnType()!=paramType||!java.lang.reflect.Modifier.isStatic(builder.getModifiers())
                        ||array.getReturnType()!=float[].class)throw new NoSuchMethodException("Native heads-up motion builder changed");
                builder.setAccessible(true);array.setAccessible(true);
            }catch(ReflectiveOperationException|LinkageError unavailable){builder=null;array=null;}
            // Optional desktop contract cannot disable working shade/lockscreen material.
            buildMotion=builder;motionArray=array;
            mixForeground=multiType.getDeclaredMethod("getForegroundMixColor");mixBackground=multiType.getDeclaredMethod("getBackgroundMixColor");
            mixSingle=singleType.getDeclaredMethod("getMixColor");multiCopy=multiType.getDeclaredMethod("copy",mixColor,mixColor);
            singleCopy=singleType.getDeclaredMethod("copy",mixColor);colorCopy=mixColor.getDeclaredMethod("copy",Integer.TYPE,Integer.TYPE,Integer.TYPE);
            colorMode=mixColor.getDeclaredMethod("getMode");colorTop=mixColor.getDeclaredMethod("getTopLayerColor");colorBottom=mixColor.getDeclaredMethod("getBottomLayerColor");
            if(!mixType.isAssignableFrom(multiType)||!mixType.isAssignableFrom(singleType)||multiCopy.getReturnType()!=multiType
                    ||singleCopy.getReturnType()!=singleType||colorCopy.getReturnType()!=mixColor||colorMode.getReturnType()!=Integer.TYPE
                    ||colorTop.getReturnType()!=Integer.TYPE||colorBottom.getReturnType()!=Integer.TYPE)throw new NoSuchMethodException("Live Alert immutable mix copy changed");
            for(Method method:new Method[]{mixForeground,mixBackground,mixSingle,multiCopy,singleCopy,colorCopy,colorMode,colorTop,colorBottom})method.setAccessible(true);
            Class<?> strokeType=loader.loadClass("com.oplus.posteffect.params.GradientStrokeLineParams");
            Class<?> shadowType=loader.loadClass("com.oplus.posteffect.params.InnerShadowParams");
            platformDrawableType=loader.loadClass("com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable");
            motionDrawableType=loader.loadClass("com.android.internal.graphics.drawable.BackgroundBlurDrawable");
            wallpaperDrawableType=loader.loadClass("com.oplusos.systemui.common.blurability.wallpaper.BlendWallpaperBlurDrawable");
            manager=ext.getDeclaredField("viewBlurManager");configMix=config.getDeclaredField("platformMixConfig");
            configStroke=config.getDeclaredField("gradientStrokeLineParam");configShadow=config.getDeclaredField("innerShadowParams");
            Class<?> motionMixType=loader.loadClass("com.oplusos.systemui.common.blurability.BlurMixConfig$BlurMixSingle");
            configMotionMix=config.getDeclaredField("motionBlurMixConfig");
            if(manager.getType()!=blurManager||configMix.getType()!=mixType||configStroke.getType()!=strokeType
                    ||configShadow.getType()!=shadowType||configMotionMix.getType()!=motionMixType
                    ||!Drawable.class.isAssignableFrom(platformDrawableType)||!Drawable.class.isAssignableFrom(motionDrawableType)
                    ||!Drawable.class.isAssignableFrom(wallpaperDrawableType))throw new NoSuchFieldException("Live Alert native material recipe changed");
            normalType=cardType.getField("NOTIFICATION").get(null);params=paramsType.getField("INSTANCE").get(null);
            platformApply=proxy.getDeclaredMethod("applyConfigToPlatformBlur");proxyDrawable=proxy.getDeclaredMethod("getBlurDrawable",Drawable.class);
            motionApply=proxy.getDeclaredMethod("applyConfigToMotionBlur");wallpaperApply=proxy.getDeclaredMethod("applyConfigToBlendWallpaper");
            applyConfig=proxy.getDeclaredMethod("applyBlurConfig");
            platformNativeDrawable=platformDrawableType.getDeclaredMethod("getBlurDrawable");
            if(platformNativeDrawable.getReturnType()!=loader.loadClass("com.oplus.posteffect.BlurDrawable"))throw new NoSuchMethodException("Live Alert platform drawable changed");
            wallpaperNativeDrawable=wallpaperDrawableType.getDeclaredMethod("getBlendDrawable");
            wallpaperMix=wallpaperDrawableType.getDeclaredMethod("getCurrentBlurMixConfig");wallpaperHost=wallpaperDrawableType.getDeclaredMethod("getHostView");
            if(wallpaperNativeDrawable.getReturnType()!=loader.loadClass("com.oplus.posteffect.drawable.BlendDrawable")
                    ||wallpaperMix.getReturnType()!=mixType||wallpaperHost.getReturnType()!=View.class)throw new NoSuchMethodException("Live Alert wallpaper drawable changed");
            Class<?> materialType=loader.loadClass("com.oplus.systemui.notification.row.material.NotificationMaterialColorEx");
            nativeMaterial=ext.getDeclaredMethod("getNotificationMaterialColorEx");
            headsMode=materialType.getDeclaredMethod("getBlurModeForRow",loader.loadClass(ROW),Boolean.TYPE);
            motionMix=blurManager.getDeclaredMethod("headsupCardMotionMixConfig",cardType,modeType);
            if(nativeMaterial.getReturnType()!=materialType||headsMode.getReturnType()!=modeType||motionMix.getReturnType()!=motionMixType)
                throw new NoSuchMethodException("Live Alert heads-up native material changed");
            proxyView=proxy.getDeclaredMethod("getView");proxyConfig=proxy.getDeclaredMethod("getBlurConfig");bgProxy=ext.getDeclaredMethod("getViewBlurProxy");
            globalMode=blurManager.getDeclaredMethod("globalBlurMode");mix=blurManager.getDeclaredMethod("getMixConfig",cardType,modeType);
            motion=blurManager.getDeclaredMethod("useMotionBlur");mirror=paramsType.getDeclaredMethod("updateMirrorScale",cardType,modeType,mixType,Boolean.TYPE);
            motionEvent=blurManager.getDeclaredMethod("updatePlatformBlurParams",proxy,Boolean.TYPE);
            if(motion.getReturnType()!=Boolean.TYPE||mirror.getReturnType()!=Void.TYPE)throw new NoSuchMethodException("Native notification mirror contract changed");
            stroke=paramsType.getDeclaredMethod("getGradientStrokeLineParams",cardType,modeType);shadow=paramsType.getDeclaredMethod("getInnerShadowParams",cardType,modeType);
            sizeStroke=paramsType.getDeclaredMethod("updateGradientStrokeLineParamsForNotificationSize",Context.class,strokeType,Float.TYPE,Float.TYPE,modeType);
            sizeShadow=paramsType.getDeclaredMethod("updateInnerShadowParamsForNotificationSize",Context.class,shadowType,Float.TYPE,Float.TYPE,modeType);
            actualHeight=body.getDeclaredMethod("getActualHeight");needMeta=loader.loadClass(HANDLER).getDeclaredMethod("needDrawBgColorByMetaBall",Context.class,loader.loadClass(ENTRY));
            for(Method method:new Method[]{platformApply,motionApply,wallpaperApply,applyConfig,proxyDrawable,platformNativeDrawable,wallpaperNativeDrawable,
                    wallpaperMix,wallpaperHost,nativeMaterial,headsMode,motionMix,needMeta,proxyView,proxyConfig,bgProxy,globalMode,mix,stroke,shadow,sizeStroke,sizeShadow,actualHeight,motion,mirror,motionEvent})method.setAccessible(true);
            for(Field field:new Field[]{manager,configMix,configStroke,configShadow,configMotionMix})field.setAccessible(true);
            for(Method method:new Method[]{blur,palette,materialEvent,headsup,metaUpdate,gradient,metaColor,host,draw})method.setAccessible(true);
            for(Field field:new Field[]{extension,mode,temp,tint,normal,keyguard})field.setAccessible(true);
            Field aodField=null;Method progressGetter=null,progressSetter=null;
            try {
                Class<?> repositoryType=loader.loadClass("com.oplus.systemui.notification.aod.OplusPanoramicAodRepository");
                aodField=ext.getDeclaredField("oplusPanoramicAodRepository");
                progressGetter=repositoryType.getDeclaredMethod("getPanoramicAodProgress");
                progressSetter=loader.loadClass("com.oplus.systemui.notification.aod.OplusPanoramicAodCoreStartable")
                        .getDeclaredMethod("setPanoramicAodProgress",Float.TYPE);
                if(aodField.getType()!=repositoryType||progressGetter.getReturnType()!=Float.TYPE||progressSetter.getReturnType()!=Void.TYPE)
                    throw new NoSuchMethodException("Native panoramic AOD progress changed");
                aodField.setAccessible(true);progressGetter.setAccessible(true);progressSetter.setAccessible(true);
            }catch(ReflectiveOperationException|LinkageError unavailable){aodField=null;progressGetter=null;progressSetter=null;}
            aodRepository=aodField;aodGetter=progressGetter;aodSetter=progressSetter;
            Method maskSetter=null;java.lang.reflect.Constructor<?> maskConstructor=null;
            try {
                Class<?> mask=loader.loadClass("com.oplus.systemui.notification.aod.notification.OplusAodRowBackgroundDrawable");
                if(!ColorDrawable.class.isAssignableFrom(mask))throw new NoSuchMethodException("Native AOD mask changed");
                maskConstructor=mask.getDeclaredConstructor(Context.class);
                maskSetter=ext.getDeclaredMethod("setPanoramicAodDrawable",Drawable.class);
                maskConstructor.setAccessible(true);maskSetter.setAccessible(true);
            }catch(ReflectiveOperationException|LinkageError unavailable){maskSetter=null;maskConstructor=null;}
            aodMaskSetter=maskSetter;aodMaskConstructor=maskConstructor;
        }
    }
    private static final class Target {
        final WeakReference<View> body;WeakReference<Object> entry=new WeakReference<>(null),row=new WeakReference<>(null);Integer accent,fill;float fillOpacity=1f;Object appliedMode;
        boolean followed,nativeGradient,nativeFill,tempGradient,lockLook,heads;int tempColor;
        boolean businessFollowing=true;
        boolean platformReady,recipeMotion; long recipeEpoch=-1,appliedEpoch=-1; int recipeWidth,recipeHeight,appliedWidth,appliedHeight;
        Integer awakeTint,colourLayer,highlightColor;ColorDrawable aodMask;
        WeakReference<Drawable> aodMaskSource=new WeakReference<>(null);
        boolean aodClear,aodKnown,recipePlainNotification,recipeNormalNotification,recipeHighlightTinted,composed;WeakReference<Object> aodRepository=new WeakReference<>(null);
        int traceCount;String traceState;
        Boolean overlayLast;
        String diagnostic,lastReason;int diagnosticCount;
        WeakReference<Object> proxy=new WeakReference<>(null);WeakReference<Drawable> platformDrawable=new WeakReference<>(null);
        boolean pending,geometryKnown;long requestEpoch=-1,geometryVersion,requestGeometry;int requestWidth,requestHeight,geometryWidth,geometryHeight,geometryActual;
        Object[] recipe;Object recipeMode;int recipeBackend;WeakReference<Object> recipeManager=new WeakReference<>(null);
        WeakReference<Drawable> tempSource=new WeakReference<>(null);Drawable tempCopy;
        Target(View body){this.body=new WeakReference<>(body);}
    }
    private static final class HeadsMaterial {final long epoch;final float[] values;HeadsMaterial(long epoch,float[] values){this.epoch=epoch;this.values=values;}}
    private static final class Native {final WeakReference<View> body;WeakReference<Object> extension=new WeakReference<>(null);Object mode;Integer tint,foreground;boolean heads,keyguard,ordinary,dirty,sampled;Native(View body){this.body=new WeakReference<>(body);}}
    private static final class Meta {
        final WeakReference<Object> handler,entry;final WeakReference<Context> context;final WeakReference<Drawable> drawable;final Hooks access;
        Integer applied;boolean gradient;
        Meta(Object handler,Context context,Object entry,Drawable drawable,Hooks access){this.handler=new WeakReference<>(handler);this.context=new WeakReference<>(context);this.entry=new WeakReference<>(entry);this.drawable=new WeakReference<>(drawable);this.access=access;}
    }
}
