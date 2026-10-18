// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.net.Uri;
import android.view.View;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** V3-only read cache. Native bindings request roles; all provider, XML, PNG and JSON work stays on WORK. */
public final class IconManagementRuntime {
    private static final int MAX_PIXELS=4*1024*1024,SIZE=128;
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"C17-icon-v3");thread.setDaemon(true);return thread;});
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Map<View,Set<String>> OWNERS=new WeakHashMap<>();
    private static final Set<String> REQUESTED=new HashSet<>(),ANIMATION_REQUESTS=new HashSet<>();
    private static volatile Map<String,Image> images=Collections.emptyMap();
    private static volatile Map<Bitmap,Image> bitmapImages=Collections.emptyMap();
    private static volatile Map<String,Animation> animations=Collections.emptyMap();
    private static volatile Map<String,IconManagementManifest> manifests=Collections.emptyMap();
    private static volatile IconManagementSettings.State state=empty();
    private static volatile boolean enabled;
    private static volatile Runnable invalidationListener;
    private static String records="";
    private static volatile long generation;
    private static volatile long sourceRevision;
    private static volatile Map<String,IconManagementSelection.ColorPlan[]> appearances=Collections.emptyMap();
    private static volatile Set<String> configuredRoles=Collections.emptySet(),explicitRoles=Collections.emptySet();
    private static boolean resourcesChanged;
    private static int pixels,failures;
    private static Context resolver;
    private static ContentResolver observedResolver;
    private static boolean permitted,managerEnabled=true;
    private static final ContentObserver RESOURCES=new ContentObserver(MAIN){@Override public void onChange(boolean selfChange){refresh();}};
    private static final ThreadLocal<Set<Drawable>> NATIVE=ThreadLocal.withInitial(()->Collections.newSetFromMap(new IdentityHashMap<Drawable,Boolean>()));
    private static IconManagementSettings.State empty(){try{return IconManagementSettings.parse(IconManagementSettings.DEFAULT);}catch(IOException impossible){throw new ExceptionInInitializerError(impossible);}}
    private static final class Image {
        final String role,packId,sourceRole;final Bitmap bitmap;final boolean mask;final IconVisibleBounds visibleBounds;final Rect sourceRect;final float fitWidth,fitHeight;
        final IconManagementSelection.ColorPlan light,dark;
        Image(String role,IconAssetDecoder.Decoded source,IconManagementSelection.AssetRef ref,IconManagementSettings.State settings){this.role=role;packId=ref.packId;sourceRole=ref.role;bitmap=source.bitmap;mask=source.mask;visibleBounds=source.visibleBounds;
            boolean shared=IconRoleRegistry.sharedSignalCanvas(role);sourceRect=shared?new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()):new Rect(visibleBounds.left,visibleBounds.top,visibleBounds.right,visibleBounds.bottom);
            fitWidth=shared?source.canvasWidth:visibleBounds.width;fitHeight=shared?source.canvasHeight:visibleBounds.height;
            light=IconManagementSelection.appearance(settings,role,false,mask?"mask":"color");dark=IconManagementSelection.appearance(settings,role,true,mask?"mask":"color");}
        Image(Image source,IconManagementSettings.State settings){role=source.role;packId=source.packId;sourceRole=source.sourceRole;bitmap=source.bitmap;mask=source.mask;visibleBounds=source.visibleBounds;sourceRect=source.sourceRect;fitWidth=source.fitWidth;fitHeight=source.fitHeight;
            light=IconManagementSelection.appearance(settings,role,false,mask?"mask":"color");dark=IconManagementSelection.appearance(settings,role,true,mask?"mask":"color");}
    }
    public static final class Animation {
        public final String target,kind,hash,render,from,to;public final byte[] bytes;public final boolean reversible;
        public final IconManagementSelection.ColorPlan light,dark;
        Animation(String target,String kind,byte[] bytes,IconManagementManifest.Transition source,IconManagementSettings.State settings,String render){this.target=target;this.kind=kind;this.bytes=bytes;hash=source.sha256;reversible=source.reversible;from=source.from;to=source.to;this.render=render;
            light=IconManagementSelection.appearance(settings,target+'.'+to,false,render);dark=IconManagementSelection.appearance(settings,target+'.'+to,true,render);}
    }
    private static final class DrawState {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final RectF target=new RectF();
        final int[] colors=new int[16];final ColorFilter[] filters=new ColorFilter[16];int count,next,recentColor;ColorFilter recentFilter;
        // Primitive keys avoid allocating Integer boxes on every draw of coloured artwork.
        ColorFilter tint(int color){if(recentFilter!=null&&recentColor==color)return recentFilter;
            for(int i=0;i<count;i++)if(colors[i]==color){recentColor=color;return recentFilter=filters[i];}
            ColorFilter filter=new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN);colors[next]=color;filters[next]=filter;next=(next+1)%colors.length;if(count<colors.length)count++;
            recentColor=color;return recentFilter=filter;}
    }
    private static final ThreadLocal<DrawState> DRAW=ThreadLocal.withInitial(DrawState::new);
    // This public framework method is hidden from the SDK stubs on the audited OEM build.
    private static final Method PORTER_COLOR=porterColor();
    private static final Map<ColorFilter,Integer> FILTER_TINTS=new WeakHashMap<>();
    private static Method porterColor(){try{Method method=PorterDuffColorFilter.class.getDeclaredMethod("getColor");method.setAccessible(true);return method;}catch(ReflectiveOperationException|RuntimeException unavailable){return null;}}
    public static void setInvalidationListener(Runnable listener){invalidationListener=listener;}
    public static void configure(Context context,Bundle settings){configure(context,settings,null);}
    public static void configure(Context context,Bundle settings,Runnable callback){
        boolean allowed=context!=null&&settings!=null&&!SafetyMode.enabled(settings)&&!ModuleLifecycle.removed();
        boolean requested=settings==null||settings.getBoolean(IconManagementSettings.ENABLED,true);
        String next=allowed?settings.getString(IconManagementSettings.KEY,IconManagementSettings.DEFAULT):IconManagementSettings.DEFAULT;
        IconManagementSettings.State planned;
        try{planned=IconManagementSettings.parse(next);}catch(IOException invalid){allowed=false;next=IconManagementSettings.DEFAULT;planned=empty();note(invalid);}
        boolean active=allowed&&requested;
        final long token;final boolean reload;final IconManagementSettings.State snapshot=planned;final Context app=context==null?null:context.getApplicationContext();
        synchronized(IconManagementRuntime.class){
            if(!resourcesChanged&&permitted==allowed&&managerEnabled==requested&&records.equals(next))return;
            reload=resourcesChanged||permitted!=allowed||managerEnabled!=requested||!sameSources(state,planned);
            resourcesChanged=false;
            permitted=allowed;managerEnabled=requested;
            active=active&&(!planned.scopePacks.isEmpty()||!planned.overrides.isEmpty()||!planned.appearances.isEmpty());
            enabled=active;records=next;state=planned;resolver=app==null?context:app;++generation;
            appearances=colorPlans(planned);
            configureRoles(planned);
            if(reload){sourceRevision++;
                REQUESTED.clear();ANIMATION_REQUESTS.clear();images=Collections.emptyMap();bitmapImages=Collections.emptyMap();animations=Collections.emptyMap();manifests=Collections.emptyMap();pixels=0;failures=0;
            }else{
                Map<String,Image> styled=new LinkedHashMap<>();Map<Bitmap,Image> owners=new IdentityHashMap<>();
                for(Image previous:images.values()){Image image=new Image(previous,planned);styled.put(image.role,image);owners.put(image.bitmap,image);}
                images=Collections.unmodifiableMap(styled);bitmapImages=Collections.unmodifiableMap(owners);
            }
            token=sourceRevision;
        }
        observeResources(context);
        invalidate(callback);if(!active||!reload)return;
        WORK.execute(()->{
            Map<String,IconManagementManifest> found=new LinkedHashMap<>();Context owner=resolver;
            for(String id:IconManagementSelection.referencedPacks(snapshot)){
                if(!current(token))return;
                try{IconManagementManifest manifest=IconManagementManifest.parse(read(owner,id,"manifest.json",null,IconManagementManifest.MAX_BYTES));manifest.requireRuntimeHashes();found.put(id,manifest);}
                catch(IOException|RuntimeException unavailable){note(unavailable);}
            }
            synchronized(IconManagementRuntime.class){if(!current(token))return;manifests=Collections.unmodifiableMap(found);}
            // The fixed status families are small. Their existing draw hooks have no View binding for individual levels.
            for(String scope:new String[]{"hint","wifi","cellular","battery"})for(String role:IconRoleRegistry.roles(scope))load(role,SIZE,token);
            Set<String> bound=new LinkedHashSet<>();synchronized(OWNERS){for(Set<String> roles:OWNERS.values())bound.addAll(roles);}
            for(String role:bound)load(role,SIZE,token);
            ResidentCache cache=ResidentCache.current;if(cache!=null)cache.retainIconsV3(IconManagementSelection.referencedPacks(snapshot));
            invalidate(callback);
        });
    }
    private static boolean sameSources(IconManagementSettings.State a,IconManagementSettings.State b){
        if(!a.scopePacks.equals(b.scopePacks)||!a.overrides.keySet().equals(b.overrides.keySet()))return false;
        for(String role:a.overrides.keySet()){IconManagementSettings.Override x=a.overrides.get(role),y=b.overrides.get(role);
            if(!x.mode.equals(y.mode)||!x.packId.equals(y.packId)||!x.sourceRole.equals(y.sourceRole))return false;}
        return true;
    }
    private static Map<String,IconManagementSelection.ColorPlan[]> colorPlans(IconManagementSettings.State settings){
        Map<String,IconManagementSelection.ColorPlan[]> result=new HashMap<>();
        for(String scope:IconRoleRegistry.SCOPES)for(String role:IconRoleRegistry.roles(scope))result.put(role,new IconManagementSelection.ColorPlan[]{
            IconManagementSelection.appearance(settings,role,false,"mask"),IconManagementSelection.appearance(settings,role,true,"mask"),
            IconManagementSelection.appearance(settings,role,false,"color"),IconManagementSelection.appearance(settings,role,true,"color")});
        return Collections.unmodifiableMap(result);
    }
    private static void configureRoles(IconManagementSettings.State settings){
        Set<String> configured=new HashSet<>(),explicit=new HashSet<>();
        for(IconRoleRegistry.Target target:IconRoleRegistry.targets(null))for(String role:target.roles){
            boolean styled=settings.appearances.containsKey("role:"+role)||settings.appearances.containsKey("target:"+target.id)||settings.appearances.containsKey("scope:"+target.scope);
            if(styled)explicit.add(role);IconManagementSettings.Override override=settings.overrides.get(role);
            if(target.supported&&(styled||override!=null&&"pack".equals(override.mode)||override==null&&settings.scopePacks.containsKey(target.scope)))configured.add(role);
        }
        configuredRoles=Collections.unmodifiableSet(configured);explicitRoles=Collections.unmodifiableSet(explicit);
    }
    private static synchronized void observeResources(Context context){
        if(context==null||observedResolver!=null||ModuleLifecycle.removed())return;
        try{ContentResolver candidate=context.getContentResolver();candidate.registerContentObserver(Uri.parse(IconManagementRepository.URI_PREFIX),true,RESOURCES);observedResolver=candidate;}
        catch(RuntimeException unavailable){note(unavailable);}
    }
    /** Explicit provider publication or host/service recovery event; never a draw timer. */
    public static void refresh(){Context context;String saved;boolean allowed,selected;
        synchronized(IconManagementRuntime.class){context=resolver;saved=records;allowed=permitted;selected=managerEnabled;if(context==null||saved.isEmpty()||ModuleLifecycle.removed())return;resourcesChanged=true;}
        Bundle values=new Bundle();values.putString(IconManagementSettings.KEY,saved);values.putBoolean(StatusBarSettings.SAFE_MODE,!allowed);values.putBoolean(IconManagementSettings.ENABLED,selected);configure(context,values);
    }
    private static synchronized boolean current(long token){return enabled&&sourceRevision==token&&!ModuleLifecycle.removed();}
    private static byte[] read(Context context,String id,String source,String hash,int limit)throws IOException{
        if(context==null)throw new IOException("Icon provider context unavailable");ResidentCache cache=ResidentCache.current;
        if(cache!=null)return cache.iconV3Entry(id,source,hash,()->context.getContentResolver().openInputStream("manifest.json".equals(source)?IconManagementRepository.manifestUri(id):IconManagementRepository.assetUri(id,source)),limit);
        try(InputStream input=context.getContentResolver().openInputStream("manifest.json".equals(source)?IconManagementRepository.manifestUri(id):IconManagementRepository.assetUri(id,source))){if(input==null)throw new IOException("Icon provider unavailable");byte[] bytes=IconManagementArchive.readEntry(input,limit);if(hash!=null)IconAssetDecoder.verify(bytes,hash);return bytes;}
    }
    private static void load(String role,int size,long token){
        IconRoleRegistry.Role registered=IconRoleRegistry.role(role);if(registered==null||!registered.supported||!current(token))return;
        IconManagementSettings.State snapshot=state;IconManagementSelection.Plan plan=IconManagementSelection.plan(snapshot,role,manifests);if(plan.system)return;
        synchronized(IconManagementRuntime.class){if(!current(token)||images.containsKey(role)||pixels>=MAX_PIXELS)return;}
        for(IconManagementSelection.AssetRef ref:plan.candidates){
            if(!current(token))return;
            try{int remaining;synchronized(IconManagementRuntime.class){remaining=MAX_PIXELS-pixels;}
                IconAssetDecoder.Decoded decoded=IconAssetDecoder.decode(ref.asset,read(resolver,ref.packId,ref.asset.source,ref.asset.sha256,IconManagementArchive.MAX_ENTRY),Math.min(256,Math.max(16,size)),remaining);
                synchronized(IconManagementRuntime.class){if(!current(token)){decoded.bitmap.recycle();return;}if(images.containsKey(role)){decoded.bitmap.recycle();return;}
                    Map<String,Image> next=new LinkedHashMap<>(images);Image image=new Image(role,decoded,ref,state);next.put(role,image);images=Collections.unmodifiableMap(next);
                    Map<Bitmap,Image> owners=new IdentityHashMap<>(bitmapImages);owners.put(decoded.bitmap,image);bitmapImages=Collections.unmodifiableMap(owners);pixels+=decoded.pixels;
                }return;
            }catch(IOException|RuntimeException unavailable){note(unavailable);}
        }
    }
    public static void observe(View view){if(view!=null)synchronized(OWNERS){if(!OWNERS.containsKey(view))OWNERS.put(view,new LinkedHashSet<>());}}
    public static void request(View owner,String role,int size){
        if(owner==null||!configured(role))return;observe(owner);synchronized(OWNERS){OWNERS.get(owner).add(role);}
        final long token;synchronized(IconManagementRuntime.class){if(!enabled||!REQUESTED.add(role))return;token=sourceRevision;}
        WORK.execute(()->{load(role,size,token);if(current(token))invalidate(null);});
    }
    private static boolean explicitAppearance(String role){return explicitRoles.contains(role);}
    public static boolean configured(String role){return enabled&&!ModuleLifecycle.removed()&&configuredRoles.contains(role);}
    public static boolean hasAppearance(String role){return configured(role);}
    public static boolean hasExplicitAppearance(String role){return active()&&explicitAppearance(role);}
    public static boolean active(){return enabled&&!ModuleLifecycle.removed();}
    public static boolean hasImage(String role){return active()&&images.containsKey(role);}
    public static String imageRender(String role){Image image=active()?images.get(role):null;return image==null?null:image.mask?"mask":"color";}
    public static IconVisibleBounds imageVisibleBounds(String role){Image image=active()?images.get(role):null;return image==null?null:image.visibleBounds;}
    public static long sourceRevision(){return sourceRevision;}
    public static boolean animationEnabled(String target){return active()&&IconManagementSelection.animationEnabled(state,target);}
    public static IconManagementSelection.ColorPlan appearance(String role,boolean dark,String render){
        IconManagementSelection.ColorPlan[] plans=appearances.get(role);
        return plans==null?IconManagementSelection.appearance(state,role,dark,render):plans[("color".equals(render)?2:0)+(dark?1:0)];
    }
    private static Bitmap bitmap(String role){Image image=active()?images.get(role):null;return image==null?null:image.bitmap;}
    static Bitmap bitmapForRole(String role){return bitmap(role);}
    public static Bitmap bitmapForHint(String slot,boolean on){return slot==null?null:bitmap("hint."+slot+("bluetooth".equals(slot)?on?".on":".off":".default"));}
    public static Bitmap bitmapForWifi(int level,boolean connected){return bitmap("wifi."+(connected?Math.max(0,Math.min(4,level)):"none"));}
    public static Bitmap bitmapForCellular(int level,boolean connected){return bitmap("cellular."+(connected?Math.max(0,Math.min(4,level)):"none"));}
    public static Bitmap bitmapForBattery(int level,boolean charging){return bitmap("battery."+(charging?"charging.":"")+(Math.max(0,Math.min(100,level))/10*10));}
    /** A null result means this bitmap belongs to an existing non-v3 renderer. */
    static Boolean draw(Bitmap bitmap,Canvas canvas,RectF bounds,int tint,int alpha,ColorFilter nativeFilter){
        Image image=bitmapImages.get(bitmap);if(image==null)return null;
        if(!active()||bitmap.isRecycled()||canvas==null||bounds.width()<=0||bounds.height()<=0)return false;
        Integer filterTint=filterColor(nativeFilter);int actualTint=filterTint==null?tint:filterTint;
        DrawState draw=DRAW.get();IconManagementSelection.ColorPlan style=bright(actualTint)?image.dark:image.light;Paint paint=draw.paint;
        boolean nativeTint="system".equals(style.mode);paint.setColorFilter(null);
        if(image.mask){paint.setColor(nativeTint&&nativeFilter!=null?0xffffffff:nativeTint?tint:style.color);if(nativeTint&&nativeFilter!=null)paint.setColorFilter(nativeFilter);}
        else{paint.setColor(0xffffffff);if(!style.artwork)paint.setColorFilter(nativeTint&&nativeFilter!=null?nativeFilter:draw.tint(nativeTint?tint:style.color));}
        int tintAlpha=nativeTint&&nativeFilter!=null?255:actualTint>>>24;
        paint.setAlpha(Math.round(tintAlpha*Math.max(0,Math.min(255,alpha))/255f*style.alpha));
        float scale=Math.min(bounds.width()/image.fitWidth,bounds.height()/image.fitHeight)*style.scale;if(!Float.isFinite(scale)||scale<=0||!Float.isFinite(bounds.centerX())||!Float.isFinite(bounds.centerY()))return false;
        float w=image.sourceRect.width()*scale,h=image.sourceRect.height()*scale;
        draw.target.set(bounds.centerX()-w*.5f,bounds.centerY()-h*.5f,bounds.centerX()+w*.5f,bounds.centerY()+h*.5f);
        try{canvas.drawBitmap(bitmap,image.sourceRect,draw.target,paint);return true;}
        catch(RuntimeException unavailable){note(unavailable);return false;}
    }
    private static boolean bright(int color){return ((color>>>16&255)*299+(color>>>8&255)*587+(color&255)*114)>128000;}
    static Integer filterColor(ColorFilter filter){
        if(filter==null)return null;synchronized(FILTER_TINTS){Integer cached=FILTER_TINTS.get(filter);if(cached!=null)return cached;}
        Integer result=null;
        if(filter instanceof PorterDuffColorFilter&&PORTER_COLOR!=null)try{result=(Integer)PORTER_COLOR.invoke(filter);}catch(ReflectiveOperationException|RuntimeException unavailable){return null;}
        else if(Build.VERSION.SDK_INT>=29&&filter instanceof BlendModeColorFilter)result=((BlendModeColorFilter)filter).getColor();
        if(result!=null)synchronized(FILTER_TINTS){FILTER_TINTS.put(filter,result);}return result;
    }
    static boolean nativeDrawing(Drawable drawable){return NATIVE.get().contains(drawable);}
    static boolean enterNative(Drawable drawable){return NATIVE.get().add(drawable);}
    static void exitNative(Drawable drawable){NATIVE.get().remove(drawable);}
    static ColorFilter nativeTintFilter(int color){return DRAW.get().tint(color);}
    static boolean drawNativeRole(String role,Drawable graphic,Canvas canvas,int nativeTint){
        if(graphic==null||canvas==null||nativeDrawing(graphic)||!hasExplicitAppearance(role))return false;
        Integer filtered=filterColor(graphic.getColorFilter());int actual=filtered==null?nativeTint:filtered;
        IconNativeAppearance.draw(graphic,canvas,appearance(role,bright(actual),"color"),nativeTint);return true;
    }
    private static void drawNative(Drawable drawable,Canvas canvas,IconManagementSelection.ColorPlan style){
        IconNativeAppearance.draw(drawable,canvas,style);
    }
    public static Drawable unwrap(Drawable drawable){return drawable instanceof RoleDrawable?((RoleDrawable)drawable).nativeGraphic:drawable;}
    /** Host color context only. It never forwards a new tint or filter into the OEM drawable. */
    public static void nativeTint(Drawable drawable,int argb){if(drawable instanceof RoleDrawable){RoleDrawable wrapped=(RoleDrawable)drawable;wrapped.tint=argb;wrapped.colors=null;wrapped.invalidateSelf();}}
    public static Drawable wrap(View owner,String role,Drawable nativeGraphic){
        nativeGraphic=unwrap(nativeGraphic);if(owner==null||nativeGraphic==null||!configured(role))return nativeGraphic;
        request(owner,role,SIZE);return new RoleDrawable(owner,role,nativeGraphic);
    }
    private static final class RoleDrawable extends Drawable implements Drawable.Callback {
        final WeakReference<View> owner;final String role;final Drawable nativeGraphic;
        final RectF target=new RectF();
        int tint=0xffffffff;ColorStateList colors;
        RoleDrawable(View owner,String role,Drawable graphic){this.owner=new WeakReference<>(owner);this.role=role;nativeGraphic=graphic;setBounds(graphic.getBounds());setState(graphic.getState());setLevel(graphic.getLevel());}
        @Override public void draw(Canvas canvas){
            View view=owner.get();if(view==null||!configured(role)){nativeGraphic.draw(canvas);return;}
            nativeGraphic.setCallback(this);Image image=images.get(role);Rect r=getBounds();int color=colors==null?tint:colors.getColorForState(getState(),colors.getDefaultColor());
            target.set(r.left,r.top,r.right,r.bottom);
            if(image!=null&&Boolean.TRUE.equals(IconManagementRuntime.draw(image.bitmap,canvas,target,color,nativeGraphic.getAlpha(),nativeGraphic.getColorFilter())))return;
            if(!explicitAppearance(role)){nativeGraphic.draw(canvas);return;}
            drawNativeRole(role,nativeGraphic,canvas,color);
        }
        @Override protected void onBoundsChange(Rect bounds){nativeGraphic.setBounds(bounds);nativeGraphic.setCallback(this);}
        @Override protected boolean onStateChange(int[] state){boolean changed=nativeGraphic.setState(state);invalidateSelf();return changed;}
        @Override public boolean isStateful(){return nativeGraphic.isStateful()||colors!=null&&colors.isStateful();}
        @Override protected boolean onLevelChange(int level){return nativeGraphic.setLevel(level);}
        @Override public void setAlpha(int alpha){nativeGraphic.setAlpha(alpha);invalidateSelf();}
        @Override public int getAlpha(){return nativeGraphic.getAlpha();}
        @Override public void setColorFilter(ColorFilter filter){nativeGraphic.setColorFilter(filter);invalidateSelf();}
        @Override public ColorFilter getColorFilter(){return nativeGraphic.getColorFilter();}
        @Override public void setTint(int color){tint=color;colors=null;nativeGraphic.setTint(color);invalidateSelf();}
        @Override public void setTintList(ColorStateList color){colors=color;nativeGraphic.setTintList(color);invalidateSelf();}
        @Override public void setTintMode(PorterDuff.Mode mode){nativeGraphic.setTintMode(mode);}
        @Override public int getOpacity(){return nativeGraphic.getOpacity();}
        @Override public int getIntrinsicWidth(){return nativeGraphic.getIntrinsicWidth();}
        @Override public int getIntrinsicHeight(){return nativeGraphic.getIntrinsicHeight();}
        @Override public int getMinimumWidth(){return nativeGraphic.getMinimumWidth();}
        @Override public int getMinimumHeight(){return nativeGraphic.getMinimumHeight();}
        @Override public boolean getPadding(Rect rect){return nativeGraphic.getPadding(rect);}
        @Override public boolean setVisible(boolean visible,boolean restart){return super.setVisible(visible,restart)|nativeGraphic.setVisible(visible,restart);}
        @Override public void invalidateDrawable(Drawable drawable){invalidateSelf();}
        @Override public void scheduleDrawable(Drawable drawable,Runnable task,long when){scheduleSelf(task,when);}
        @Override public void unscheduleDrawable(Drawable drawable,Runnable task){unscheduleSelf(task);}
    }
    public static Animation animation(String target,String kind){return active()?animations.get(target+'.'+kind):null;}
    public static void requestAnimation(View owner,String target,String kind){
        if(owner==null||!active()||!IconManagementSelection.animationEnabled(state,target))return;observe(owner);
        String key=target+'.'+kind;final long token;synchronized(IconManagementRuntime.class){if(!ANIMATION_REQUESTS.add(key))return;token=sourceRevision;}
        WORK.execute(()->{
            if(!current(token))return;
            for(String to:"transient".equals(kind)?new String[]{"on","off"}:new String[]{"on_to_off".equals(kind)?"off":"on"}){
                String role=target+'.'+to;load(role,SIZE,token);Image endImage=images.get(role);if(endImage==null)continue;
                IconManagementManifest manifest=manifests.get(endImage.packId);IconManagementManifest.Transition transition=manifest==null?null:manifest.transitions.get(key);
                if(transition==null||!to.equals(transition.to)||!role.equals(endImage.sourceRole))continue;
                if(!"transient".equals(kind)){String fromRole=target+'.'+transition.from;load(fromRole,SIZE,token);Image startImage=images.get(fromRole);
                    if(startImage==null||!startImage.packId.equals(endImage.packId)||!fromRole.equals(startImage.sourceRole))continue;}
                try{byte[] bytes=read(resolver,endImage.packId,transition.source,transition.sha256,256*1024);IconAnimationAsset.parse(bytes,"transient".equals(kind));Animation result=new Animation(target,kind,bytes,transition,state,endImage.mask?"mask":"color");
                    synchronized(IconManagementRuntime.class){if(!current(token))return;Map<String,Animation> next=new LinkedHashMap<>(animations);next.put(key,result);animations=Collections.unmodifiableMap(next);}invalidate(null);return;
                }catch(IOException|RuntimeException unavailable){note(unavailable);}
            }
        });
    }
    private static void note(Throwable error){synchronized(IconManagementRuntime.class){if(failures++>=8)return;}ModuleDiagnostics.error("icon_packs","V3 icon unavailable; native glyph retained",error);}
    private static void invalidate(Runnable extra){List<WeakReference<View>> views=new ArrayList<>();synchronized(OWNERS){for(View view:OWNERS.keySet())if(view!=null)views.add(new WeakReference<>(view));}
        MAIN.post(()->{for(WeakReference<View> ref:views){View view=ref.get();if(view!=null&&view.isAttachedToWindow())view.invalidate();}if(extra!=null)extra.run();Runnable listener=invalidationListener;if(listener!=null&&listener!=extra)listener.run();});}
    public static void releaseRuntime(){synchronized(IconManagementRuntime.class){enabled=false;permitted=false;managerEnabled=true;generation++;sourceRevision++;resourcesChanged=false;records="";resolver=null;state=empty();appearances=Collections.emptyMap();configuredRoles=Collections.emptySet();explicitRoles=Collections.emptySet();images=Collections.emptyMap();bitmapImages=Collections.emptyMap();animations=Collections.emptyMap();manifests=Collections.emptyMap();REQUESTED.clear();ANIMATION_REQUESTS.clear();pixels=0;
        if(observedResolver!=null){try{observedResolver.unregisterContentObserver(RESOURCES);}catch(RuntimeException ignored){ }observedResolver=null;}}
        invalidate(null);invalidationListener=null;synchronized(OWNERS){OWNERS.clear();}synchronized(FILTER_TINTS){FILTER_TINTS.clear();}IconNativeAppearance.releaseRuntime();NATIVE.remove();DRAW.remove();}
    private IconManagementRuntime(){ }
}
