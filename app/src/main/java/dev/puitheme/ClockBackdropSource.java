// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLongArray;

/** Observes an existing behind-scrim blur ID from its creation. Never registers,
 * updates or releases a blur, changes its shader/parameters, or copies pixels.
 * Every accepted submission is unmixed; delayed native images on that ID are
 * consequently unmixed too. Unknown or previously used IDs cannot be adopted.
 */
final class ClockBackdropSource {
    private static final String HELPER="com.oplusos.systemui.common.blurability.platformblur.PlatformBlurHelper";
    private static final String WRAPPER="com.oplusos.systemui.common.blurability.platformblur.PlatformBlurDrawable";
    private static final String ENGINE="com.oplus.posteffect.BlurDrawable";
    private static final String PARAM="com.oplus.posteffect.BlurParam";
    private static final String IMAGE="com.oplus.posteffect.image.ContentImage";
    private static final Object LOCK=new Object();
    private static final ThreadLocal<Scope> SCOPES=new ThreadLocal<>();
    private static final ThreadLocal<DrawScope> DRAWS=new ThreadLocal<>();
    private static final WeakHashMap<Object,Entry> ENGINES=new WeakHashMap<>();
    // BlurParam.hashCode includes mutable properties changed by copyFrom().
    // Weak identity keys preserve the transport association across that write.
    private static final Map<Object,Entry[]> PARAMS=new HashMap<>();
    private static final ReferenceQueue<Object> PARAM_QUEUE=new ReferenceQueue<>();
    private static final ParamLookup PARAM_LOOKUP=new ParamLookup();
    private static final Map<Class<?>,Binding> BINDINGS=new HashMap<>();
    private static volatile Entry[] entries=new Entry[0];
    private static volatile WeakReference<Runnable>[] invalidations=emptyCallbacks();
    private static volatile long epoch;
    private static long nextRevision;
    private static volatile String failure="no_observed_source";
    private static final String[] STAT_NAMES={"created_sources","image_events","draw_events","image_changes","bitmap_changes",
            "generation_changes","geometry_changes","active_changes","wrapping_changes","invalidations","snapshot_calls",
            "ready_frames","cached_frames","new_frames","unavailable_frames","inactive_frames","missing_images",
            "ambiguous_frames","invalid_frames","poisoned_sources","promotion_scopes","coalesced_image_events",
            "deferred_frames","promotion_failures"};
    private static final AtomicLongArray STATS=new AtomicLongArray(STAT_NAMES.length);

    private ClockBackdropSource() { }

    /** Must surround the original helper.ensureBlurDrawable(), including its
     * constructors and created callback. A nested unrelated helper masks scope. */
    static Scope beginHelper(Object helper) {
        View host=null;
        try {
            if(helper!=null&&HELPER.equals(helper.getClass().getName())) {
                Object value=field(helper.getClass(),"hostView").get(helper);
                if(value instanceof View&&behind((View)value))host=(View)value;
            }
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) { }
        Scope scope=new Scope(SCOPES.get(),host);SCOPES.set(scope);return scope;
    }

    static final class Scope implements AutoCloseable {
        private final Scope previous;
        private final View host;
        private final long epoch=ClockBackdropSource.epoch;
        private final Thread thread=Thread.currentThread();
        private boolean closed;
        private Scope(Scope previous,View host){this.previous=previous;this.host=host;}
        @Override public void close() {
            if(closed||Thread.currentThread()!=thread)return;
            closed=true;
            if(SCOPES.get()==this){Scope active=previous;while(active!=null&&active.closed)active=active.previous;if(active==null)SCOPES.remove();else SCOPES.set(active);}
        }
    }

    /** Native promotion releases its old image before directly assigning the
     * new image and matrix. Observe that synchronous transaction only at its
     * final boundary; no observer lock is held while native code runs. */
    static DrawScope beginDraw(Object engine) {
        Entry entry=find(engine);if(entry==null||entry.poisoned)return DrawScope.NONE;
        synchronized(LOCK) {
            if(entry.epoch!=epoch||entry.poisoned)return DrawScope.NONE;
            if(entry.promotionDepth==0)entry.promotionFailed=false;
            entry.promotionDepth++;entry.promotionVersion++;STATS.incrementAndGet(20);
        }
        DrawScope scope=new DrawScope(DRAWS.get(),entry);DRAWS.set(scope);return scope;
    }
    static final class DrawScope implements AutoCloseable {
        private static final DrawScope NONE=new DrawScope(null,null);
        private final DrawScope previous;
        private final Entry entry;
        private final Thread thread=Thread.currentThread();
        private boolean completed,closed;
        private DrawScope(DrawScope previous,Entry entry){this.previous=previous;this.entry=entry;}
        void complete(){if(entry!=null)completed=true;}
        @Override public void close() {
            if(entry==null||closed||Thread.currentThread()!=thread)return;
            closed=true;
            if(DRAWS.get()==this){DrawScope active=previous;while(active!=null&&active.closed)active=active.previous;if(active==null)DRAWS.remove();else DRAWS.set(active);}
            boolean finish,refresh;
            synchronized(LOCK) {
                if(entry.epoch!=epoch)return;
                entry.promotionDepth=Math.max(0,entry.promotionDepth-1);entry.promotionVersion++;
                if(!completed){entry.promotionFailed=true;entry.cached.clear();STATS.incrementAndGet(23);}
                finish=entry.promotionDepth==0;refresh=finish&&entry.refreshAfterDraw;
                if(finish)entry.refreshAfterDraw=false;
            }
            if(!finish||entry.poisoned)return;
            if(entry.promotionFailed){failure="native_promotion_failed";notifyInvalidation();return;}
            Object engine=entry.engine.get();boolean changed=engine!=null&&observeDrawing(engine,true);
            if(refresh&&!changed)notifyInvalidation();
        }
    }
    private static boolean drawing(Entry entry) {
        for(DrawScope scope=DRAWS.get();scope!=null;scope=scope.previous)
            if(!scope.closed&&scope.entry==entry&&entry.epoch==epoch)return true;
        return false;
    }

    /** The exact PlatformBlurDrawable constructor AFTER, before its helper's
     * onBlurDrawableCreated callback can submit the first nonzero radius. */
    static void created(Object wrapper) {
        Scope scope=SCOPES.get();
        if(scope==null||scope.epoch!=epoch||scope.host==null||wrapper==null||!WRAPPER.equals(wrapper.getClass().getName()))return;
        try {
            Object engine=field(wrapper.getClass(),"blurDrawable").get(wrapper);
            if(engine==null||!ENGINE.equals(engine.getClass().getName()))return;
            Binding binding=binding(engine.getClass());if(binding==null)return;
            synchronized(LOCK){if(ENGINES.containsKey(engine))return;}
            if(binding.registered.getBoolean(engine)||binding.image.invoke(engine)!=null
                    ||!valid(engine,null,binding,true)) {failure="source_already_used_or_mixed";return;}
            Entry entry=new Entry(engine,scope.host,binding,epoch);
            synchronized(LOCK) {
                if(entry.epoch!=epoch||ENGINES.containsKey(engine))return;
                ENGINES.put(engine,entry);publishEntries();STATS.incrementAndGet(0);
            }
            associate(binding.params.invoke(engine),entry);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) {failure="unsupported_native_shape";}
    }

    /** Before a native parameter submission. Pass the upcoming BlurParam, or
     * null only when the native method submits its current drawing parameters. */
    static void onParams(Object engine,Object upcomingParam) {
        Entry entry=find(engine);if(entry==null||entry.poisoned)return;
        associate(upcomingParam,entry);
        try{associate(entry.binding.params.invoke(engine),entry);}catch(ReflectiveOperationException|RuntimeException ignored){poison(entry,"params_unavailable");return;}
        if(!valid(engine,upcomingParam,entry.binding,false))poison(entry,"mixed_or_unknown_submission");
    }

    /** BlurParam.toFloatArray AFTER: validate the actual service transport,
     * including DEBUG overrides initialised inside the first native add. This
     * observer must never encode again, otherwise its hook would recurse. */
    static void onEncoded(Object param,float[] encoded) {
        Entry[] associated;synchronized(LOCK){associated=associated(param);}
        if(associated==null)return;
        for(Entry entry:associated)if(!entry.poisoned&&entry.epoch==epoch) {
            boolean pure=pure(encoded);
            try{pure&=!(entry.binding.debug.getBoolean(null)&&entry.binding.serverMode.getInt(null)==2);}
            catch(ReflectiveOperationException|RuntimeException unavailable){pure=false;}
            if(!pure)poison(entry,"mixed_native_encoding");
        }
    }

    /** Call BEFORE and AFTER native addBlurDrawable. The post check matters:
     * manager.add can first initialise the DEBUG server-blend override itself. */
    static void onRegistration(Object engine) {
        Entry entry=find(engine);if(entry==null||entry.poisoned)return;
        try {
            if(!valid(engine,null,entry.binding,false)){poison(entry,"mixed_registration");return;}
            boolean registered=entry.binding.registered.getBoolean(engine);
            boolean missed=false;
            synchronized(LOCK) {
                if(entry.poisoned||entry.epoch!=epoch)return;
                if(!registered){entry.registrationStarted=true;entry.ready=false;}
                else if(entry.registrationStarted)entry.ready=true;
                else missed=true;
            }
            if(missed)poison(entry,"registration_not_observed");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) {poison(entry,"registration_unavailable");}
    }

    /** Observe both setEnableBlurShader and direct setEnableShader writes. One
     * false request permanently disqualifies the ID, even if later reset true. */
    static void onShaderChanged(Object engine,boolean enabled) {
        Entry entry=find(engine);if(entry!=null&&!enabled)poison(entry,"shader_was_disabled");
    }

    /** Native image promotion AFTER. Stable preDraw calls do not notify or
     * rebuild frames; the callback itself must arrange any required UI post. */
    static void onImageChanged(Object engine) {
        Entry entry=find(engine);
        if(entry!=null&&drawing(entry)) {
            synchronized(LOCK){if(entry.epoch==epoch)entry.refreshAfterDraw=true;}
            STATS.incrementAndGet(21);return;
        }
        observeDrawing(engine,false);
    }

    /** preDrawContent AFTER, which also directly promotes native image fields.
     * Stable native draws produce no invalidation and no frame allocation. */
    static void onDrawn(Object engine) {
        observeDrawing(engine,true);
    }

    private static boolean observeDrawing(Object engine,boolean geometry) {
        Entry entry=find(engine);if(entry==null||entry.poisoned)return false;
        STATS.incrementAndGet(geometry?2:1);
        try {
            boolean changed=false;Object dataLock=entry.binding.dataLock.invoke(engine);
            if(dataLock==null)return false;
            synchronized(dataLock) {
                Object image=entry.binding.image.invoke(engine);
                Bitmap bitmap=bitmap(entry.binding,image);
                Matrix matrix=geometry?(Matrix)entry.binding.matrix.invoke(engine):null;
                Rect bounds=geometry?(Rect)entry.binding.bounds.invoke(engine):null;
                Object drawingParam=geometry?entry.binding.params.invoke(engine):null;
                boolean active=geometry&&drawingParam!=null&&((Number)entry.binding.radius.invoke(drawingParam)).intValue()>0;
                Shader.TileMode tileMode=geometry?entry.binding.tileMode(drawingParam):null;
                synchronized(LOCK) {
                    if(entry.epoch!=epoch||entry.poisoned)return false;
                    changed=observeContent(entry,image,bitmap);
                    if(geometry) {
                        matrix.getValues(entry.values);
                        boolean moved=!entry.observedGeometry||!same(entry.values,entry.observedMatrix)||!entry.observedBounds.equals(bounds);
                        if(moved)STATS.incrementAndGet(6);changed|=moved;
                        if(entry.observedActive!=active){STATS.incrementAndGet(7);changed=true;}entry.observedActive=active;
                        if(entry.observedTileMode!=tileMode){STATS.incrementAndGet(8);changed=true;}entry.observedTileMode=tileMode;
                        System.arraycopy(entry.values,0,entry.observedMatrix,0,9);entry.observedBounds.set(bounds);entry.observedGeometry=true;
                    }
                    if(changed)entry.imageRevision=++nextRevision;
                }
            }
            if(changed)notifyInvalidation();
            return changed;
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) {poison(entry,"image_unavailable");return true;}
    }

    /** The caller retains its callback strongly. No clock, panel or controller
     * is retained through this source's invalidation callback. */
    static void setInvalidationCallback(Runnable callback) {
        synchronized(LOCK){invalidations=emptyCallbacks();addInvalidationCallback(callback);}
    }
    /** Clock and notification glyphs share the same native source observation.
     * Listeners are weak, bounded and copied only when listeners change. */
    static void addInvalidationCallback(Runnable callback) {
        if(callback==null)return;
        synchronized(LOCK) {
            java.util.ArrayList<WeakReference<Runnable>> live=new java.util.ArrayList<>();
            for(WeakReference<Runnable> reference:invalidations) {
                Runnable existing=reference.get();
                if(existing==callback)return;
                if(existing!=null)live.add(reference);
            }
            if(live.size()>=8)return;
            live.add(new WeakReference<>(callback));
            invalidations=live.toArray(emptyCallbacks());
        }
    }
    @SuppressWarnings("unchecked") private static WeakReference<Runnable>[] emptyCallbacks() {
        return (WeakReference<Runnable>[])new WeakReference<?>[0];
    }

    /** Source bounds describe drawable coordinates. Actual glyph-domain
     * coverage belongs to the material; an oversized TextView is not rejected. */
    static Frame snapshot(View target) {
        STATS.incrementAndGet(10);
        if(target==null||!target.isAttachedToWindow())return unavailable("target_detached",14);
        if(entries.length==0)return unavailable("no_observed_source",14);
        View root=target.getRootView();Frame result=null;
        for(Entry entry:entries) {
            if(entry.poisoned||!entry.ready||entry.epoch!=epoch)continue;
            View host=entry.host.get();Object engine=entry.engine.get();
            if(host==null||engine==null||!host.isAttachedToWindow()||host.getRootView()!=root)continue;
            if(entry.promotionFailed){failure="native_promotion_failed";continue;}
            long version=entry.promotionVersion;
            boolean promoting=entry.promotionDepth>0;
            Frame frame=promoting?defer(entry):read(entry,engine,host);
            if(entry.epoch!=epoch||entry.poisoned)frame=null;
            else if(entry.promotionFailed)frame=null;
            else if(!promoting&&(entry.promotionDepth>0||entry.promotionVersion!=version))frame=defer(entry);
            if(frame==null)continue;
            if(result!=null){STATS.incrementAndGet(14);return unavailable("ambiguous_native_source",17);}
            result=frame;
        }
        if(result!=null&&result.deferredEntry!=null)failure="native_image_promoting";
        else if(result!=null){failure="";STATS.incrementAndGet(11);}else STATS.incrementAndGet(14);
        return result;
    }

    static final class Frame {
        final Bitmap bitmap;
        /** Owned snapshots: the material must not mutate these values. */
        final Matrix bitmapToDrawable;
        final View host;
        final Rect bounds;
        final long revision;
        /** Native delivery/content lifetime, independent of its moving geometry. */
        final long contentRevision;
        final int bitmapGeneration;
        final Shader.TileMode tileMode;
        private final Entry deferredEntry;
        private final Entry sourceEntry;
        private Frame(Bitmap bitmap,Matrix matrix,View host,Rect bounds,long revision,Shader.TileMode tileMode) {
            this(bitmap,matrix,host,bounds,revision,tileMode,0L);
        }
        private Frame(Bitmap bitmap,Matrix matrix,View host,Rect bounds,long revision,Shader.TileMode tileMode,long contentRevision) {
            this(bitmap,matrix,host,bounds,revision,tileMode,contentRevision,null);
        }
        private Frame(Bitmap bitmap,Matrix matrix,View host,Rect bounds,long revision,Shader.TileMode tileMode,long contentRevision,Entry entry) {
            this.bitmap=bitmap;bitmapToDrawable=matrix;this.host=host;this.bounds=bounds;this.revision=revision;this.tileMode=tileMode;
            this.contentRevision=contentRevision;bitmapGeneration=bitmap.getGenerationId();deferredEntry=null;sourceEntry=entry;
        }
        private Frame(Entry entry) {
            bitmap=null;bitmapToDrawable=null;host=null;bounds=null;revision=contentRevision=0L;bitmapGeneration=0;tileMode=null;deferredEntry=entry;sourceEntry=null;
        }
        boolean deferred(){return deferredEntry!=null&&deferredEntry.epoch==epoch&&!deferredEntry.poisoned&&!deferredEntry.promotionFailed;}
        boolean deferUpdate(View target) {
            Entry entry=deferredEntry!=null?deferredEntry:sourceEntry;
            if(entry==null||entry.epoch!=epoch||entry.poisoned||entry.promotionFailed||target==null)return false;
            View source=entry.host.get();if(source==null||source.getRootView()!=target.getRootView())return false;
            if(deferredEntry!=null)return true;
            // A valid snapshot can race a promotion that starts immediately
            // afterwards. Recheck at submission too, before touching its bitmap.
            return entry.promotionDepth>0&&defer(entry)!=null;
        }
    }
    private static Frame defer(Entry entry) {
        boolean refresh;
        synchronized(LOCK) {
            if(entry.epoch!=epoch||entry.poisoned||entry.promotionFailed)return null;
            refresh=entry.promotionDepth==0;
            if(!refresh)entry.refreshAfterDraw=true;
            STATS.incrementAndGet(22);
        }
        // If promotion completed between reading and validating the frame,
        // arrange the final refresh ourselves. Never wait for the RT thread.
        if(refresh)notifyInvalidation();
        return entry.deferredFrame;
    }

    static String lastFailure(){return failure;}
    /** Read only at a verified runtime probe; never emits per-frame records. */
    static Map<String,Object> diagnostics() {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<STAT_NAMES.length;i++)result.put(STAT_NAMES[i],STATS.get(i));
        int ready=0,poisoned=0;for(Entry entry:entries){if(entry.ready&&!entry.poisoned)ready++;if(entry.poisoned)poisoned++;}
        result.put("source_count",entries.length);result.put("ready_source_count",ready);result.put("poisoned_source_count",poisoned);
        result.put("source_reason",failure);return result;
    }
    static void clear() {
        synchronized(LOCK){epoch++;ENGINES.clear();PARAMS.clear();BINDINGS.clear();entries=new Entry[0];failure="no_observed_source";}
        SCOPES.remove();DRAWS.remove();invalidations=emptyCallbacks();
        for(int i=0;i<STAT_NAMES.length;i++)STATS.set(i,0L);
    }

    private static Frame read(Entry entry,Object engine,View host) {
        Binding binding=entry.binding;
        try {
            // A changed DEBUG switch can invalidate buffers without a public
            // shader flag write. Recheck the transport only at that boundary.
            boolean debug=binding.debug.getBoolean(null);int mode=binding.serverMode.getInt(null);
            if(debug!=entry.debug||mode!=entry.mode) {
                if(!valid(engine,null,binding,false)){poison(entry,"debug_blend_changed");return null;}
                entry.debug=debug;entry.mode=mode;
            }
            Object lock=binding.dataLock.invoke(engine);if(lock==null)return null;
            synchronized(lock) {
                if(!binding.registered.getBoolean(engine)||!Boolean.TRUE.equals(binding.enabled.invoke(engine)))return unavailable("native_source_inactive",15);
                Object currentParam=binding.params.invoke(engine);
                if(currentParam==null||((Number)binding.radius.invoke(currentParam)).intValue()<=0)return unavailable("native_blur_inactive",15);
                Shader.TileMode tileMode=binding.tileMode(currentParam);
                if(tileMode==null)return unavailable("native_wrapping_unavailable",18);
                if(binding.axisRemap.getBoolean(engine)&&binding.axisRotation.getInt(engine)!=0)return unavailable("unsupported_axis_remap",18);
                Object image=binding.image.invoke(engine);if(image==null)return unavailable("native_image_missing",16);
                Bitmap bitmap=bitmap(binding,image);
                if(bitmap==null)return unavailable("native_bitmap_unavailable",16);
                Object matrixValue=binding.matrix.invoke(engine),boundsValue=binding.bounds.invoke(engine);
                if(!(matrixValue instanceof Matrix)||!(boundsValue instanceof Rect))return unavailable("native_geometry_missing",18);
                Matrix matrix=(Matrix)matrixValue;Rect bounds=(Rect)boundsValue;
                if(bounds.isEmpty())return unavailable("native_bounds_empty",18);
                synchronized(LOCK) {
                    if(entry.poisoned||entry.epoch!=epoch)return null;
                    if(observeContent(entry,image,bitmap))entry.imageRevision=++nextRevision;
                    matrix.getValues(entry.values);
                    Frame cached=entry.cached.get();boolean same=cached!=null&&cached.bitmap==bitmap&&cached.host==host
                            &&same(entry.values,entry.matrixValues)&&entry.cachedBounds.equals(bounds)
                            &&entry.cachedImageRevision==entry.imageRevision&&cached.tileMode==tileMode;
                    if(same&&cached.bitmapGeneration==bitmap.getGenerationId()){STATS.incrementAndGet(12);return cached;}
                    // Only new native geometry needs inversion/finite checking.
                    // All live registration, blur, renderer, image and debug
                    // provenance checks above still run before this fast path.
                    for(float value:entry.values)if(!Float.isFinite(value))return unavailable("native_geometry_nonfinite",18);
                    if(!matrix.invert(entry.inverse))return unavailable("native_geometry_singular",18);
                    System.arraycopy(entry.values,0,entry.matrixValues,0,9);entry.cachedBounds.set(bounds);
                    Matrix copy=new Matrix();copy.set(matrix);
                    Frame frame=new Frame(bitmap,copy,host,new Rect(bounds.left,bounds.top,bounds.right,bounds.bottom),++nextRevision,tileMode,entry.contentRevision,entry);
                    STATS.incrementAndGet(13);
                    entry.cachedImageRevision=entry.imageRevision;entry.cached=new WeakReference<>(frame);return frame;
                }
            }
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) {failure="native_frame_unavailable";return null;}
    }

    private static Frame unavailable(String reason,int counter) {failure=reason;STATS.incrementAndGet(counter);return null;}
    private static Bitmap bitmap(Binding binding,Object image)throws ReflectiveOperationException {
        if(image==null)return null;
        ImageBinding imageBinding=binding.imageBinding(image.getClass());Object content=imageBinding.image.invoke(image);
        if(content==null||!IMAGE.equals(content.getClass().getName()))return null;
        Object raw=imageBinding.bitmap.invoke(content);if(!(raw instanceof Bitmap))return null;
        Bitmap bitmap=(Bitmap)raw;return bitmap.isRecycled()||bitmap.getWidth()<=0||bitmap.getHeight()<=0?null:bitmap;
    }
    /** LOCK held. A hardware-buffer delivery can reuse Bitmap identity, while
     * its new ContentImage owns a fresh native sampler. Geometry is separate. */
    private static boolean observeContent(Entry entry,Object image,Bitmap bitmap) {
        Bitmap previous=entry.notifiedBitmap.get();int generation=bitmap==null?0:bitmap.getGenerationId();
        boolean imageChanged=entry.notifiedImage.get()!=image,bitmapChanged=previous!=bitmap;
        boolean generationChanged=bitmap!=null&&previous==bitmap&&entry.notifiedGeneration!=generation;
        if(!imageChanged&&!bitmapChanged&&!generationChanged)return false;
        if(imageChanged)STATS.incrementAndGet(3);if(bitmapChanged)STATS.incrementAndGet(4);if(generationChanged)STATS.incrementAndGet(5);
        entry.notifiedImage=new WeakReference<>(image);entry.notifiedBitmap=new WeakReference<>(bitmap);entry.notifiedGeneration=generation;
        entry.contentRevision=++nextRevision;return true;
    }

    private static boolean valid(Object engine,Object upcoming,Binding binding,boolean fresh) {
        try {
            if(!Boolean.TRUE.equals(binding.enabled.invoke(engine)))return false;
            if(binding.debug.getBoolean(null)&&binding.serverMode.getInt(null)==2)return false;
            Object param=upcoming==null?binding.params.invoke(engine):upcoming;
            if(param==null||!PARAM.equals(param.getClass().getName()))return false;
            if(fresh&&((Number)binding.radius.invoke(param)).intValue()!=0)return false;
            Object raw=binding.encode.invoke(param,true);if(!(raw instanceof float[]))return false;
            return pure((float[])raw);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown) {return false;}
    }

    private static boolean pure(float[] encoded) {
        if(encoded==null||encoded.length!=30)return false;
        for(float value:encoded)if(!Float.isFinite(value))return false;
        for(int index=7;index<=15;index++)if(encoded[index]!=0f)return false;
        for(int index=19;index<=27;index++)if(encoded[index]!=0f)return false;
        return encoded[28]==0f&&encoded[29]==1f;
    }
    private static void associate(Object param,Entry entry) {
        if(param==null||!PARAM.equals(param.getClass().getName()))return;
        synchronized(LOCK) {
            if(entry.epoch!=epoch||entry.poisoned)return;
            Entry[] previous=associated(param);
            if(previous==null){PARAMS.put(new ParamKey(param),new Entry[]{entry});return;}
            for(Entry value:previous)if(value==entry)return;
            Entry[] replacement=new Entry[previous.length+1];System.arraycopy(previous,0,replacement,0,previous.length);replacement[previous.length]=entry;PARAMS.put(new ParamKey(param),replacement);
        }
    }
    /** LOCK held; the reusable lookup never retains a parameter across calls. */
    private static Entry[] associated(Object param) {
        Object stale;while((stale=PARAM_QUEUE.poll())!=null)PARAMS.remove(stale);
        if(param==null)return null;
        PARAM_LOOKUP.value=param;
        try{return PARAMS.get(PARAM_LOOKUP);}finally{PARAM_LOOKUP.value=null;}
    }
    private static final class ParamKey extends WeakReference<Object> {
        final int hash;
        ParamKey(Object value){super(value,PARAM_QUEUE);hash=System.identityHashCode(value);}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object other){return this==other||(other instanceof ParamKey&&get()!=null&&get()==((ParamKey)other).get());}
    }
    private static final class ParamLookup {
        Object value;
        @Override public int hashCode(){return System.identityHashCode(value);}
        @Override public boolean equals(Object other){return other instanceof ParamKey&&value!=null&&value==((ParamKey)other).get();}
    }

    private static Entry find(Object engine){synchronized(LOCK){return engine==null?null:ENGINES.get(engine);}}
    private static void poison(Entry entry,String reason) {
        boolean changed;
        synchronized(LOCK){changed=!entry.poisoned;entry.poisoned=true;entry.ready=false;entry.cached.clear();entry.reason=reason;failure=reason;}
        if(changed){STATS.incrementAndGet(19);notifyInvalidation();}
    }
    private static void notifyInvalidation() {
        STATS.incrementAndGet(9);
        for(WeakReference<Runnable> reference:invalidations) {
            Runnable callback=reference.get();if(callback!=null)try{callback.run();}catch(RuntimeException ignored){}
        }
    }
    private static void publishEntries(){entries=ENGINES.values().toArray(new Entry[0]);}
    private static boolean behind(View host) {
        if(!"com.android.systemui.scrim.ScrimView".equals(host.getClass().getName()))return false;
        try{return host.getId()!=View.NO_ID&&"scrim_behind".equals(host.getResources().getResourceEntryName(host.getId()))
                &&"com.android.systemui".equals(host.getResources().getResourcePackageName(host.getId()));}
        catch(RuntimeException unknown){return false;}
    }
    private static boolean same(float[] one,float[] two){for(int i=0;i<9;i++)if(one[i]!=two[i])return false;return true;}
    private static Binding binding(Class<?> type) {
        synchronized(LOCK) {
            if(BINDINGS.containsKey(type))return BINDINGS.get(type);
            Binding binding=null;try{binding=new Binding(type);}catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){ }
            if(BINDINGS.size()>=4)BINDINGS.clear();BINDINGS.put(type,binding);return binding;
        }
    }
    private static Method method(Class<?> type,String name,Class<?>... args)throws NoSuchMethodException {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try{Method found=current.getDeclaredMethod(name,args);found.setAccessible(true);return found;}
        catch(NoSuchMethodException missing){ }
        throw new NoSuchMethodException(name);
    }
    private static Field field(Class<?> type,String name)throws NoSuchFieldException {
        for(Class<?> current=type;current!=null;current=current.getSuperclass())try{Field found=current.getDeclaredField(name);found.setAccessible(true);return found;}
        catch(NoSuchFieldException missing){ }
        throw new NoSuchFieldException(name);
    }
    private static final class Binding {
        final Method params,enabled,image,matrix,bounds,dataLock,encode,radius,base,wrapping,shaderMode;
        final Field registered,debug,serverMode,axisRemap,axisRotation;
        private volatile ImageBinding cachedImage;
        Binding(Class<?> type)throws ReflectiveOperationException {
            ClassLoader loader=type.getClassLoader();Class<?> param=Class.forName(PARAM,false,loader);
            params=method(type,"getDrawingParam");enabled=method(type,"getEnableShader");image=method(type,"getDrawingImageInfo");
            matrix=method(type,"getBitmapMatrix");bounds=method(type,"getBounds");dataLock=method(type,"getDataLock");
            registered=field(type,"isRegistered");encode=method(param,"toFloatArray",boolean.class);radius=method(param,"getBlurRadius");
            base=method(param,"getBaseParam");wrapping=method(base.getReturnType(),"getWrappingType");shaderMode=method(wrapping.getReturnType(),"toShaderTileMode");
            axisRemap=field(type,"viewAxisRemapEnabled");axisRotation=field(type,"viewAxisRemapRotation");
            debug=field(Class.forName("com.oplus.posteffect.util.Log",false,loader),"DEBUG");
            serverMode=field(Class.forName("com.oplus.posteffect.manager.BlurDrawableManager",false,loader),"sBlendTypeValue");
        }
        Shader.TileMode tileMode(Object param)throws ReflectiveOperationException {
            if(param==null)return null;
            Object baseValue=base.invoke(param);if(baseValue==null)return null;
            Object wrappingValue=wrapping.invoke(baseValue);if(wrappingValue==null)return null;
            Object result=shaderMode.invoke(wrappingValue);return result instanceof Shader.TileMode?(Shader.TileMode)result:null;
        }
        ImageBinding imageBinding(Class<?> type)throws ReflectiveOperationException {
            ImageBinding value=cachedImage;
            if(value!=null&&value.type==type)return value;
            value=new ImageBinding(type);cachedImage=value;return value;
        }
    }
    private static final class ImageBinding {
        final Class<?> type;
        final Method image,bitmap;
        ImageBinding(Class<?> type)throws ReflectiveOperationException {
            this.type=type;image=method(type,"getImage");
            Class<?> content=Class.forName(IMAGE,false,type.getClassLoader());bitmap=method(content,"get");
        }
    }
    private static final class Entry {
        final WeakReference<Object> engine;
        final WeakReference<View> host;
        final Binding binding;
        final long epoch;
        volatile boolean poisoned,ready;
        boolean registrationStarted,debug;
        volatile int promotionDepth;
        volatile long promotionVersion;
        volatile boolean promotionFailed;
        boolean refreshAfterDraw;
        final Frame deferredFrame;
        int mode=Integer.MIN_VALUE;
        String reason="";
        final float[] values=new float[9],matrixValues=new float[9];
        final float[] observedMatrix=new float[9];
        final Matrix inverse=new Matrix();
        final Rect cachedBounds=new Rect(),observedBounds=new Rect();
        boolean observedGeometry,observedActive;
        Shader.TileMode observedTileMode;
        WeakReference<Frame> cached=new WeakReference<>(null);
        WeakReference<Object> notifiedImage=new WeakReference<>(null);
        WeakReference<Bitmap> notifiedBitmap=new WeakReference<>(null);
        int notifiedGeneration;
        long imageRevision,cachedImageRevision,contentRevision;
        Entry(Object engine,View host,Binding binding,long epoch){this.engine=new WeakReference<>(engine);this.host=new WeakReference<>(host);this.binding=binding;this.epoch=epoch;deferredFrame=new Frame(this);}
    }
}
