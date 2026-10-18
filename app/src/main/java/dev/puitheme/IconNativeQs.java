// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Exact QSIconViewProxy binding. External compositions belong only to our own OEM LottieDrawable. */
final class IconNativeQs {
    static final String PROXY="com.oplus.systemui.qs.base.res.widget.QSIconViewProxy";
    static final String HOST="com.oplus.systemui.qs.base.tile.QSLottieAnimationView";
    static final String STATE="com.android.systemui.plugins.qs.QSTile$State";
    static final String BOOLEAN_STATE="com.android.systemui.plugins.qs.QSTile$BooleanState";
    static final String DETAIL="com.oplus.systemui.qs.slider.detail.widget.QsDetailTileBaseView";
    private static final int MAX_COMPOSITIONS=8,MAX_COMPOSITION_BYTES=2*1024*1024;
    private static final Executor WORK=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"C17-QS-composition");thread.setDaemon(true);return thread;});
    private static final Handler MAIN=new Handler(Looper.getMainLooper());

    static final class Motion {
        final String target,kind,hash,from,to;final byte[] bytes;final boolean reversible,artwork,reversed;
        Motion(String target,String kind,String hash,String from,String to,byte[] bytes,boolean reversible,boolean artwork){this(target,kind,hash,from,to,bytes,reversible,artwork,false);}
        private Motion(String target,String kind,String hash,String from,String to,byte[] bytes,boolean reversible,boolean artwork,boolean reversed){this.target=target;this.kind=kind;this.hash=hash;this.from=from;this.to=to;this.bytes=bytes;this.reversible=reversible;this.artwork=artwork;this.reversed=reversed;}
    }
    interface Graphics {
        boolean configured(String role);boolean hasImage(String role);boolean explicitAppearance(String role);String imageRender(String role);
        boolean animationEnabled(String target);long sourceRevision();
        void request(View owner,String role,int size);Drawable wrap(View owner,String role,Drawable source);Drawable unwrap(Drawable graphic);
        void nativeTint(Drawable graphic,int color);Motion animation(String target,String kind);void requestAnimation(View owner,String target,String kind);
        IconManagementSelection.ColorPlan appearance(String role,boolean dark,String render);
    }
    private static final Graphics RUNTIME=new Graphics(){
        public boolean configured(String role){return IconManagementRuntime.hasAppearance(role);}
        public boolean hasImage(String role){return IconManagementRuntime.hasImage(role);}
        public boolean explicitAppearance(String role){return IconManagementRuntime.hasExplicitAppearance(role);}
        public String imageRender(String role){return IconManagementRuntime.imageRender(role);}
        public boolean animationEnabled(String target){return IconManagementRuntime.animationEnabled(target);}
        public long sourceRevision(){return IconManagementRuntime.sourceRevision();}
        public void request(View owner,String role,int size){IconManagementRuntime.request(owner,role,size);}
        public Drawable wrap(View owner,String role,Drawable source){return IconManagementRuntime.wrap(owner,role,source);}
        public Drawable unwrap(Drawable graphic){return IconManagementRuntime.unwrap(graphic);}
        public void nativeTint(Drawable graphic,int color){IconManagementRuntime.nativeTint(graphic,color);}
        public Motion animation(String target,String kind){IconManagementRuntime.Animation value=IconManagementRuntime.animation(target,kind);return value==null?null:new Motion(value.target,value.kind,value.hash,value.from,value.to,value.bytes,value.reversible,"color".equals(value.render));}
        public void requestAnimation(View owner,String target,String kind){IconManagementRuntime.requestAnimation(owner,target,kind);}
        public IconManagementSelection.ColorPlan appearance(String role,boolean dark,String render){return IconManagementRuntime.appearance(role,dark,render);}
    };
    private final Graphics graphics;private final Executor work;private final Consumer<Runnable> main;
    private final Map<Object,Binding> bindings=new WeakHashMap<>();private final Map<View,WeakReference<Binding>> owners=new WeakHashMap<>();
    private final Map<Object,String> detailSpecs=new WeakHashMap<>();
    private final Map<Class<?>,Members> members=new HashMap<>();
    private final LinkedHashMap<CacheKey,Parsed> compositions=new LinkedHashMap<>(8,.75f,true);
    private final Set<CacheKey> failed=new LinkedHashSet<>();private int compositionBytes,errors;private long failedRevision=Long.MIN_VALUE;
    private boolean enabled,interactive=true;private volatile boolean released;
    IconNativeQs(){this(RUNTIME,WORK,task->MAIN.post(task));}
    IconNativeQs(Graphics graphics,Executor work,Consumer<Runnable> main){this.graphics=graphics;this.work=work;this.main=main;}
    private static final class Members {final Map<String,Field> fields=new HashMap<>();final Map<String,Method> methods=new HashMap<>();}
    private static final class Binding {
        final WeakReference<Object> proxy;final WeakReference<ImageView> owner;final Access access;
        WeakReference<Object> model=new WeakReference<>(null);WeakReference<Drawable> nativeGraphic=new WeakReference<>(null),applied=new WeakReference<>(null);
        String spec="",target="",role="",prefix="",pendingKind,requestedRole;int state=-1,mutating;boolean transientState,allowed;long requestedRevision=-1;
        long revision,deadline;volatile long token;volatile String loading;Session session;
        Binding(Object proxy,ImageView owner,Access access){this.proxy=new WeakReference<>(proxy);this.owner=new WeakReference<>(owner);this.access=access;}
    }
    static final class Ticket {
        final Binding binding;final String spec,role,prefix;final int state;final boolean transientState;final long revision;
        Ticket(Binding binding){this.binding=binding;spec=binding.spec;role=binding.role;prefix=binding.prefix;state=binding.state;transientState=binding.transientState;revision=binding.revision;}
    }
    private static final class CacheKey {
        final ClassLoader loader;final String hash;
        CacheKey(ClassLoader loader,String hash){this.loader=loader;this.hash=hash;}
        @Override public int hashCode(){return System.identityHashCode(loader)*31+hash.hashCode();}
        @Override public boolean equals(Object other){return other instanceof CacheKey&&((CacheKey)other).loader==loader&&((CacheKey)other).hash.equals(hash);}
    }
    private static final class Parsed {final Api api;final Object composition;final int bytes;Parsed(Api api,Object composition,int bytes){this.api=api;this.composition=composition;this.bytes=bytes;}}
    private static final class Session {
        final Api api;final Motion motion;final WeakReference<Drawable> drawable;final WeakReference<Object> animator;
        final List<WeakReference<Object>> callbacks=new ArrayList<>();WeakReference<SizeGraphic> sizeGraphic=new WeakReference<>(null);Object listener;boolean stopped,recolored;int color=Integer.MIN_VALUE,alpha=-1;
        Session(Api api,Motion motion,Drawable drawable,Object animator){this.api=api;this.motion=motion;this.drawable=new WeakReference<>(drawable);this.animator=new WeakReference<>(animator);}
    }
    /** Scale only the running composition's canvas, keeping OEM bounds, state and animator intact. */
    static final class SizeGraphic extends android.graphics.drawable.DrawableWrapper {
        float scale=1f;
        SizeGraphic(Drawable drawable){super(drawable);}
        @Override public void draw(android.graphics.Canvas canvas){int saved=canvas.save();try{android.graphics.Rect bounds=getBounds();canvas.scale(scale,scale,bounds.exactCenterX(),bounds.exactCenterY());super.draw(canvas);}finally{canvas.restoreToCount(saved);}}
    }
    private final View.OnAttachStateChangeListener attachment=new View.OnAttachStateChangeListener(){
        public void onViewAttachedToWindow(View view){onVisibilityChanged(view,true);}
        public void onViewDetachedFromWindow(View view){onVisibilityChanged(view,false);}
    };
    void configure(boolean value){enabled=value&&!released&&!ModuleLifecycle.removed();refresh();}
    void setInteractive(boolean value){interactive=value;if(!value)for(Binding binding:new ArrayList<>(bindings.values()))settle(binding,true);else refresh();}
    void onVisibilityChanged(View view,boolean visible){WeakReference<Binding> reference=owners.get(view);Binding binding=reference==null?null:reference.get();if(binding==null)return;
        if(!visible||!visible(binding))settle(binding,true);else applyStatic(binding,false);}
    void onImageAlphaChanged(View view){WeakReference<Binding> ref=owners.get(view);Binding binding=ref==null?null:ref.get();if(binding!=null&&binding.session!=null)updateColors(binding,binding.session);}
    void bindDetail(Object detail,Object tile){if(!named(detail,DETAIL))return;Object proxy=call(detail,"getIconViewProxy"),spec=call(tile,"getTileSpec");if(named(proxy,PROXY)&&spec instanceof String)detailSpecs.put(proxy,(String)spec);}
    Ticket beforeSetIcon(Object proxy,Object model,boolean allowAnimation){
        if(released||!named(proxy,PROXY))return null;Binding binding=bindings.get(proxy);
        if(binding==null)try{Access access=new Access(proxy.getClass());Object icon=access.getIconView.invoke(proxy);if(!(icon instanceof ImageView)||!named(icon,HOST))return null;
            binding=new Binding(proxy,(ImageView)icon,access);bindings.put(proxy,binding);owners.put((View)icon,new WeakReference<>(binding));((View)icon).addOnAttachStateChangeListener(attachment);
        }catch(ReflectiveOperationException|RuntimeException unsupported){note(unsupported);return null;}
        Ticket ticket=new Ticket(binding);binding.mutating++;String nextSpec=spec(proxy,model),nextRole=role(nextSpec,model);String prefix=text(read(model,"lottiePrefix"));
        if(!nextSpec.equals(binding.spec)||!nextRole.equals(binding.role)||!prefix.equals(binding.prefix)||graphics.sourceRevision()!=binding.revision){binding.token++;binding.pendingKind=null;binding.loading=null;stop(binding);restore(binding);}
        return ticket;
    }
    void afterSetIcon(Object proxy,Object model,boolean allowAnimation,Ticket ticket,boolean succeeded){
        if(ticket==null)return;Binding binding=ticket.binding;binding.mutating=Math.max(0,binding.mutating-1);if(released||binding.proxy.get()!=proxy)return;
        if(!succeeded){settle(binding,true);return;}ImageView owner=binding.owner.get();if(owner==null)return;
        String spec=spec(proxy,model),role=role(spec,model),prefix=text(read(model,"lottiePrefix"));int state=integer(read(model,"state"),-1);boolean transientState=Boolean.TRUE.equals(read(model,"isTransient"));
        boolean stable=spec.equals(ticket.spec)&&prefix.equals(ticket.prefix)&&ticket.revision==graphics.sourceRevision()&&!ticket.role.isEmpty();
        binding.spec=spec;binding.role=role;binding.target=role.isEmpty()?"":role.substring(0,role.lastIndexOf('.'));binding.prefix=prefix;binding.state=state;binding.transientState=transientState;binding.revision=graphics.sourceRevision();binding.model=new WeakReference<>(model);binding.allowed=allowAnimation;
        if(!enabled||role.isEmpty()||!graphics.configured(role)||!visible(binding)){settle(binding,true);return;}
        request(binding);
        if(binding.session!=null&&stable&&role.equals(ticket.role)&&transientState==ticket.transientState&&permitted(binding)){Drawable drawable=binding.session.drawable.get();if(drawable!=null){updateColors(binding,binding.session);return;}}
        binding.token++;binding.pendingKind=null;binding.loading=null;stop(binding);
        if(stable&&permitted(binding)&&state!=0&&ticket.state!=0){
            if(transientState&&!ticket.transientState)binding.pendingKind="transient";
            else if(!transientState&&!ticket.transientState&&state!=ticket.state)binding.pendingKind=state==2?"off_to_on":"on_to_off";
        }
        if(binding.pendingKind!=null){binding.deadline=SystemClock.uptimeMillis()+800;graphics.requestAnimation(owner,binding.target,binding.pendingKind);}
        applyStatic(binding,true);tryPending(binding);
    }
    void afterNativeUpdate(Object proxy){Binding binding=bindings.get(proxy);if(binding==null||binding.mutating>0||released)return;
        if(!enabled||!visible(binding)||!graphics.configured(binding.role)||binding.revision!=graphics.sourceRevision()){settle(binding,true);return;}
        if(binding.session!=null){if(!permitted(binding)){settle(binding,true);return;}Session session=binding.session;Drawable drawable=session.drawable.get();ImageView owner=binding.owner.get();if(drawable!=null&&owner!=null){updateColors(binding,session);return;}}
        applyStatic(binding,false);
    }
    void afterNativeTint(Object proxy){Binding binding=bindings.get(proxy);if(binding==null||binding.mutating>0||released)return;
        if(binding.session!=null){if(!permitted(binding))settle(binding,true);else updateColors(binding,binding.session);}else{Drawable applied=binding.applied.get();if(applied!=null)graphics.nativeTint(applied,tint(binding));}}
    void onRecycle(Object proxy){Binding binding=bindings.remove(proxy);detailSpecs.remove(proxy);if(binding==null)return;binding.token++;binding.pendingKind=null;binding.loading=null;stop(binding);restore(binding);ImageView owner=binding.owner.get();if(owner!=null){owner.removeOnAttachStateChangeListener(attachment);owners.remove(owner);}}
    void refresh(){synchronized(compositions){long revision=graphics.sourceRevision();if(failedRevision!=revision){failed.clear();failedRevision=revision;}}for(Binding binding:new ArrayList<>(bindings.values())){
        if(!enabled||!visible(binding)||!graphics.configured(binding.role)){settle(binding,true);continue;}
        if(binding.revision!=graphics.sourceRevision()){binding.revision=graphics.sourceRevision();binding.token++;binding.pendingKind=null;binding.loading=null;stop(binding);restore(binding);}
        if(binding.session!=null){if(!graphics.animationEnabled(binding.target))settle(binding,true);else updateColors(binding,binding.session);}
        else{applyStatic(binding,false);tryPending(binding);}
    }}
    private void settle(Binding binding,boolean clearPending){binding.token++;if(clearPending){binding.pendingKind=null;binding.loading=null;}stop(binding);if(enabled&&visible(binding)&&graphics.configured(binding.role))applyStatic(binding,true);else restore(binding);}
    private void applyStatic(Binding binding,boolean updateNative){ImageView owner=binding.owner.get();Object proxy=binding.proxy.get(),model=binding.model.get();
        if(owner==null||proxy==null||!enabled||!visible(binding)||binding.role.isEmpty()||!graphics.configured(binding.role)){restore(binding);return;}
        request(binding);
        if(!graphics.hasImage(binding.role)&&!graphics.explicitAppearance(binding.role)){restore(binding);return;}
        if(binding.session!=null)return;
        if(updateNative&&!forceStatic(binding,model))return;
        Drawable source=binding.access.nativeGraphic(proxy);if(source==null)source=graphics.unwrap(owner.getDrawable());if(source==null)return;source=graphics.unwrap(source);binding.nativeGraphic=new WeakReference<>(source);
        Drawable existing=binding.applied.get();if(existing!=null&&owner.getDrawable()==existing){graphics.nativeTint(existing,tint(binding));return;}
        Drawable replacement=graphics.wrap(owner,binding.role,source);graphics.nativeTint(replacement,tint(binding));binding.applied=new WeakReference<>(replacement);install(binding,replacement);
    }
    private void request(Binding binding){ImageView owner=binding.owner.get();long revision=graphics.sourceRevision();if(owner==null||binding.role.equals(binding.requestedRole)&&revision==binding.requestedRevision)return;
        binding.requestedRole=binding.role;binding.requestedRevision=revision;graphics.request(owner,binding.role,Math.max(owner.getWidth(),owner.getHeight()));}
    private boolean forceStatic(Binding binding,Object model){Object proxy=binding.proxy.get();ImageView owner=binding.owner.get();if(proxy==null||owner==null||model==null)return false;
        // Native has already evaluated iconSupplier. Never invoke a supplier a second time or guess its returned Icon.
        Object icon=read(model,"icon");if(icon==null||read(model,"iconSupplier")!=null)return false;
        try{binding.mutating++;binding.access.updateIcon.invoke(proxy,icon,binding.transientState,false);binding.access.cancelLoader.invoke(owner);return true;}
        catch(ReflectiveOperationException|RuntimeException failure){note(failure);return false;}finally{binding.mutating--;}
    }
    private void install(Binding binding,Drawable graphic){ImageView owner=binding.owner.get();if(owner==null||owner.getDrawable()==graphic)return;try{binding.mutating++;owner.setImageDrawable(graphic);}finally{binding.mutating--;}}
    private void restore(Binding binding){ImageView owner=binding.owner.get();if(owner==null)return;Drawable current=owner.getDrawable(),applied=binding.applied.get();if(current==applied){Drawable source=binding.nativeGraphic.get();if(source!=null)install(binding,source);}binding.applied.clear();}
    private void stop(Binding binding){Session session=binding.session;binding.session=null;if(session==null)return;session.stopped=true;Drawable drawable=session.drawable.get();Object animator=session.animator.get();
        try{if(animator!=null&&session.listener!=null)session.api.removeListener.invoke(animator,session.listener);if(animator!=null)session.api.cancel.invoke(animator);if(drawable!=null){session.api.clear.invoke(drawable);drawable.setCallback(null);}}
        catch(ReflectiveOperationException|RuntimeException failure){note(failure);}finally{session.callbacks.clear();session.listener=null;ImageView owner=binding.owner.get();if(owner!=null&&(owner.getDrawable()==drawable||owner.getDrawable()==session.sizeGraphic.get())){Drawable source=binding.nativeGraphic.get();if(source!=null)install(binding,source);}session.sizeGraphic.clear();}
    }
    private void tryPending(Binding binding){String kind=binding.pendingKind;ImageView owner=binding.owner.get();if(kind==null||owner==null||binding.loading!=null||binding.session!=null)return;
        if(SystemClock.uptimeMillis()>binding.deadline||!permitted(binding)||binding.revision!=graphics.sourceRevision()){binding.pendingKind=null;return;}
        Motion motion=motion(binding,kind);if(motion==null)return;
        String end=binding.state==2?"on":"off";if(!motion.target.equals(binding.target)||!motion.kind.equals(kind)||!motion.to.equals(end)||!kind.equals("transient")&&!kind.equals(motion.from+"_to_"+motion.to)){binding.pendingKind=null;return;}
        CacheKey key=new CacheKey(owner.getClass().getClassLoader(),motion.hash);synchronized(compositions){if(failed.contains(key)){binding.pendingKind=null;return;}}
        binding.loading=motion.hash;final long token=binding.token;WeakReference<Binding> reference=new WeakReference<>(binding);
        work.execute(()->{Binding pending=reference.get();if(pending==null||released||pending.token!=token||!Objects.equals(pending.loading,motion.hash))return;Parsed parsed=null;Throwable error=null;try{parsed=composition(key,motion);}catch(Exception|LinkageError failure){error=failure;}
            final Parsed ready=parsed;final Throwable problem=error;main.accept(()->{Binding current=reference.get();if(current==null||released||current.token!=token||current.revision!=graphics.sourceRevision()||!Objects.equals(current.loading,motion.hash))return;current.loading=null;
                if(problem!=null){synchronized(compositions){failed.add(key);while(failed.size()>16)failed.remove(failed.iterator().next());}current.pendingKind=null;note(problem);return;}
                if(ready==null||!permitted(current)||SystemClock.uptimeMillis()>current.deadline){current.pendingKind=null;return;}
                start(current,motion,ready);
            });
        });
    }
    private Motion motion(Binding binding,String kind){Motion selected=graphics.animation(binding.target,kind);if(selected!=null||"transient".equals(kind))return selected;
        String inverse="off_to_on".equals(kind)?"on_to_off":"off_to_on";Motion reverse=graphics.animation(binding.target,inverse);
        if(reverse==null){View owner=binding.owner.get();if(owner!=null)graphics.requestAnimation(owner,binding.target,inverse);return null;}
        String render=graphics.imageRender(binding.role);if(!reverse.reversible||!reverse.target.equals(binding.target)||!reverse.kind.equals(inverse)||!inverse.equals(reverse.from+"_to_"+reverse.to)||render==null)return null;
        return new Motion(reverse.target,kind,reverse.hash,reverse.to,reverse.from,reverse.bytes,true,"color".equals(render),true);
    }
    private Parsed composition(CacheKey key,Motion motion)throws Exception{
        if(released)return null;
        synchronized(compositions){Parsed cached=compositions.get(key);if(cached!=null)return cached;}
        IconAssetDecoder.verify(motion.bytes,motion.hash);IconAnimationAsset.parse(motion.bytes,"transient".equals(motion.kind));Api api=new Api(key.loader);
        Object result=api.parse.invoke(null,new ByteArrayInputStream(motion.bytes),null),composition=api.value.invoke(result);if(!api.composition.isInstance(composition)){Object exception=api.exception.invoke(result);throw new IOException("OEM composition parse failed",exception instanceof Throwable?(Throwable)exception:null);}
        Parsed parsed=new Parsed(api,composition,motion.bytes.length);synchronized(compositions){if(released)return null;Parsed existing=compositions.get(key);if(existing!=null)return existing;
            while(!compositions.isEmpty()&&(compositions.size()>=MAX_COMPOSITIONS||compositionBytes+parsed.bytes>MAX_COMPOSITION_BYTES)){CacheKey oldest=compositions.keySet().iterator().next();compositionBytes-=compositions.remove(oldest).bytes;}compositions.put(key,parsed);compositionBytes+=parsed.bytes;}
        return parsed;
    }
    private void start(Binding binding,Motion motion,Parsed parsed){ImageView owner=binding.owner.get();if(owner==null||!forceStatic(binding,binding.model.get())){binding.pendingKind=null;return;}Drawable allocated=null;
        try{Drawable drawable=(Drawable)parsed.api.drawable.newInstance();allocated=drawable;parsed.api.setRender.invoke(drawable,parsed.api.software);parsed.api.setComposition.invoke(drawable,parsed.composition);
            parsed.api.repeat.invoke(drawable,"transient".equals(motion.kind)?-1:0);parsed.api.speed.invoke(drawable,motion.reversed?-1f:1f);parsed.api.progress.invoke(drawable,motion.reversed?1f:0f);Object animator=parsed.api.animator.get(drawable);
            Session session=new Session(parsed.api,motion,drawable,animator);binding.session=session;binding.pendingKind=null;
            WeakReference<Binding> reference=new WeakReference<>(binding);session.listener=Proxy.newProxyInstance(parsed.api.listener.getClassLoader(),new Class<?>[]{parsed.api.listener},(proxy,method,args)->{
                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);if(method.getName().equals("equals"))return args!=null&&args.length==1&&proxy==args[0];if(method.getName().equals("toString"))return "C17 owned QS completion";
                if(method.getName().equals("onAnimationEnd")||method.getName().equals("onAnimationCancel")){Binding current=reference.get();if(current!=null&&current.session==session&&!session.stopped){stop(current);applyStatic(current,true);}}return null;
            });
            parsed.api.addListener.invoke(animator,session.listener);
            binding.applied=new WeakReference<>(drawable);install(binding,drawable);updateColors(binding,session);parsed.api.play.invoke(drawable);
        }catch(ReflectiveOperationException|RuntimeException failure){note(failure);boolean owned=binding.session!=null;stop(binding);if(!owned&&allocated!=null)try{parsed.api.clear.invoke(allocated);allocated.setCallback(null);}catch(ReflectiveOperationException|RuntimeException ignored){}applyStatic(binding,false);}
    }
    private void installCallbacks(Session session,Drawable drawable,boolean enabled)throws ReflectiveOperationException{
        session.callbacks.clear();session.color=Integer.MIN_VALUE;
        Object query=session.api.keyPath.newInstance((Object)new String[]{"**"});Object result=session.api.resolve.invoke(drawable,query);if(!(result instanceof List)||((List<?>)result).size()>512)throw new IllegalStateException("OEM KeyPath result exceeds admitted shape budget");
        for(Object path:(List<?>)result){Object element=session.api.element.invoke(path);String type=element==null?"":element.getClass().getName();int property=type.equals("com.airbnb.lottie.animation.content.FillContent")?1:type.equals("com.airbnb.lottie.animation.content.StrokeContent")?2:0;if(property==0)continue;
            Object callback=enabled?session.api.callback.newInstance(Integer.valueOf(0xffffffff)):null;session.api.addCallback.invoke(drawable,path,Integer.valueOf(property),callback);if(callback!=null)session.callbacks.add(new WeakReference<>(callback));}
        session.recolored=enabled;
    }
    private void updateColors(Binding binding,Session session){Drawable drawable=session.drawable.get();ImageView owner=binding.owner.get();if(drawable==null||owner==null||session.stopped)return;
        int nativeColor=tint(binding);boolean dark=bright(nativeColor);IconManagementSelection.ColorPlan style=graphics.appearance(binding.role,dark,session.motion.artwork?"color":"mask");
        int color=("custom".equals(style.mode)?style.color:nativeColor)|0xff000000;
        try{if(session.recolored==style.artwork)installCallbacks(session,drawable,!style.artwork);
            if(!style.artwork&&session.color!=color){for(WeakReference<Object> ref:session.callbacks){Object callback=ref.get();if(callback!=null)session.api.callbackValue.invoke(callback,Integer.valueOf(color));}session.color=color;}
            // forceStatic uses the OEM static binding: native tint Alpha is in its ColorStateList, not an extra C17 View alpha.
            int alpha=Math.round(owner.getImageAlpha()*(nativeColor>>>24)/255f*style.alpha);if(session.alpha!=alpha){drawable.setAlpha(alpha);session.alpha=alpha;}
            Drawable displayed=drawable;
            if(style.scale!=1f){SizeGraphic sized=session.sizeGraphic.get();if(sized==null){sized=new SizeGraphic(drawable);session.sizeGraphic=new WeakReference<>(sized);}sized.scale=style.scale;displayed=sized;}
            binding.applied=new WeakReference<>(displayed);install(binding,displayed);displayed.invalidateSelf();
        }catch(ReflectiveOperationException|RuntimeException failure){note(failure);settle(binding,true);}
    }
    private boolean permitted(Binding binding){ImageView owner=binding.owner.get();Object proxy=binding.proxy.get(),model=binding.model.get();if(!enabled||released||!interactive||!visible(binding)||owner==null||proxy==null||model==null||!graphics.animationEnabled(binding.target)||!Boolean.TRUE.equals(read(proxy,"animationEnabled"))||Boolean.TRUE.equals(read(model,"disabledByPolicy")))return false;
        IconRoleRegistry.Target target=IconRoleRegistry.target(binding.target);if(target==null||!target.dynamicSupported||!binding.allowed)return false;
        try{return ValueAnimator.areAnimatorsEnabled()&&Boolean.TRUE.equals(binding.access.globalAnimation.invoke(null))&&(!Boolean.TRUE.equals(binding.access.dialog.invoke(null))||Boolean.TRUE.equals(read(proxy,"isDetailTile")))
                &&binding.access.nativeAsset.invoke(null,owner.getContext(),model)!=null;
        }catch(ReflectiveOperationException|RuntimeException unsupported){return false;}
    }
    private boolean visible(Binding binding){ImageView owner=binding.owner.get();if(owner==null||!owner.isAttachedToWindow()||!owner.isShown()||owner.getWindowVisibility()!=View.VISIBLE||!interactive)return false;
        Object power=owner.getContext().getSystemService(Context.POWER_SERVICE);return !(power instanceof PowerManager)||((PowerManager)power).isInteractive();}
    private int tint(Binding binding){Object proxy=binding.proxy.get();ImageView owner=binding.owner.get();if(owner==null)return 0xffffffff;Object host=read(proxy,"iconHostView"),value=read(proxy,"tintList");ColorStateList colors=value instanceof ColorStateList?(ColorStateList)value:owner.getImageTintList();return colors==null?0xffffffff:colors.getColorForState(host instanceof View?((View)host).getDrawableState():owner.getDrawableState(),colors.getDefaultColor());}
    private String spec(Object proxy,Object model){Object value=read(model,"spec");return value instanceof String&&!((String)value).isEmpty()?(String)value:detailSpecs.getOrDefault(proxy,"");}
    static String role(String spec,Object model){String target=IconRoleRegistry.tileTarget(spec);if(target==null||!named(model,STATE))return "";IconRoleRegistry.Target entry=IconRoleRegistry.target(target);if(entry==null||!entry.supported)return "";
        int state=integerStatic(model,"state",-1),three=integerStatic(model,"threeStageMode",-1);if(state<0||state>2)return "";
        if(target.equals("tile.ringer_mode"))return named(model,BOOLEAN_STATE)&&state==2&&three>=0&&three<=2?target+'.'+new String[]{"silent","vibrate","ring"}[three]:"";
        if(three!=0)return "";if(entry.stateLabels.containsKey("default"))return state==1?target+".default":"";
        if(!named(model,BOOLEAN_STATE))return "";String suffix=target.equals("tile.screen_record")?(state==0?"unavailable":state==1?"idle":"recording"):(state==0?"unavailable":state==1?"off":"on");return entry.stateLabels.containsKey(suffix)?target+'.'+suffix:"";}
    private static boolean bright(int color){return ((color>>>16&255)*299+(color>>>8&255)*587+(color&255)*114)>128000;}
    private static int integer(Object value,int fallback){return value instanceof Integer?(Integer)value:fallback;}
    private static String text(Object value){return value instanceof String?(String)value:"";}
    private static int integerStatic(Object value,String field,int fallback){try{for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{Field member=type.getDeclaredField(field);member.setAccessible(true);return integer(member.get(value),fallback);}catch(NoSuchFieldException absent){}return fallback;}catch(ReflectiveOperationException|RuntimeException absent){return fallback;}}
    private static boolean named(Object value,String name){if(value==null)return false;for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals(name))return true;return false;}
    private Object read(Object value,String name){if(value==null)return null;try{Members cache=members.computeIfAbsent(value.getClass(),key->new Members());if(!cache.fields.containsKey(name)){Field found=null;for(Class<?> type=value.getClass();type!=null&&found==null;type=type.getSuperclass())try{found=type.getDeclaredField(name);found.setAccessible(true);}catch(NoSuchFieldException absent){}cache.fields.put(name,found);}Field field=cache.fields.get(name);return field==null?null:field.get(value);}catch(ReflectiveOperationException|RuntimeException absent){return null;}}
    private Object call(Object value,String name){if(value==null)return null;try{Members cache=members.computeIfAbsent(value.getClass(),key->new Members());if(!cache.methods.containsKey(name)){Method found=null;try{found=value.getClass().getMethod(name);found.setAccessible(true);}catch(NoSuchMethodException absent){}cache.methods.put(name,found);}Method method=cache.methods.get(name);return method==null?null:method.invoke(value);}catch(ReflectiveOperationException|RuntimeException absent){return null;}}
    private static final class Access {
        final Method getIconView,updateIcon,cancelLoader,nativeAsset,globalAnimation,dialog;final Field iconDrawable;
        Access(Class<?> proxy)throws ReflectiveOperationException{ClassLoader loader=proxy.getClassLoader();Class<?> state=Class.forName(STATE,false,loader),icon=Class.forName("com.android.systemui.plugins.qs.QSTile$Icon",false,loader),host=Class.forName(HOST,false,loader);
            getIconView=proxy.getMethod("getIconView");updateIcon=proxy.getMethod("updateIcon",icon,boolean.class,boolean.class);cancelLoader=host.getMethod("cancelLoaderTask");nativeAsset=proxy.getMethod("getLottieAnimAsset",Context.class,state);
            globalAnimation=Class.forName("com.oplus.systemui.qs.base.res.util.QSConstant",false,loader).getMethod("getQsTileAnimationAllowed");dialog=Class.forName("com.oplusos.systemui.common.util.GlobalUtils",false,loader).getMethod("hasDialogShowing");iconDrawable=proxy.getDeclaredField("iconDrawable");iconDrawable.setAccessible(true);}
        Drawable nativeGraphic(Object proxy){try{Object value=iconDrawable.get(proxy);return value instanceof Drawable?(Drawable)value:null;}catch(IllegalAccessException absent){return null;}}
    }
    private static final class Api {
        final Class<?> composition,listener;final Constructor<?> drawable,keyPath,callback;final Method parse,value,exception,setComposition,clear,play,repeat,speed,progress,setRender,resolve,element,addCallback,callbackValue,addListener,removeListener,cancel;final Field animator;final Object software;
        @SuppressWarnings({"unchecked","rawtypes"}) Api(ClassLoader loader)throws ReflectiveOperationException{Class<?> factory=Class.forName("com.airbnb.lottie.LottieCompositionFactory",false,loader),result=Class.forName("com.airbnb.lottie.LottieResult",false,loader),type=Class.forName("com.airbnb.lottie.LottieDrawable",false,loader),path=Class.forName("com.airbnb.lottie.model.KeyPath",false,loader),cb=Class.forName("com.airbnb.lottie.value.LottieValueCallback",false,loader),render=Class.forName("com.airbnb.lottie.RenderMode",false,loader);
            composition=Class.forName("com.airbnb.lottie.LottieComposition",false,loader);listener=Class.forName("android.animation.Animator$AnimatorListener",false,loader);drawable=type.getConstructor();keyPath=path.getConstructor(String[].class);callback=cb.getConstructor(Object.class);
            parse=factory.getMethod("fromJsonInputStreamSync",InputStream.class,String.class);value=result.getMethod("getValue");exception=result.getMethod("getException");setComposition=type.getMethod("setComposition",composition);clear=type.getMethod("clearComposition");play=type.getMethod("playAnimation");repeat=type.getMethod("setRepeatCount",int.class);speed=type.getMethod("setSpeed",float.class);progress=type.getMethod("setProgress",float.class);setRender=type.getMethod("setRenderMode",render);software=Enum.valueOf((Class)render,"SOFTWARE");
            resolve=type.getMethod("resolveKeyPath",path);element=path.getMethod("getResolvedElement");addCallback=type.getMethod("addValueCallback",path,Object.class,cb);callbackValue=cb.getMethod("setValue",Object.class);animator=type.getField("animator");Class<?> animation=animator.getType();addListener=animation.getMethod("addListener",listener);removeListener=animation.getMethod("removeListener",listener);cancel=animation.getMethod("cancel");}
    }
    private void note(Throwable failure){if(errors++<8)ModuleDiagnostics.error("icon_packs","Exact QS glyph bridge unavailable; preserving native icon",failure);}
    void releaseRuntime(){if(released)return;enabled=false;released=true;for(Object proxy:new ArrayList<>(bindings.keySet()))onRecycle(proxy);synchronized(compositions){compositions.clear();failed.clear();compositionBytes=0;}members.clear();owners.clear();detailSpecs.clear();}
    String summary(){synchronized(compositions){return "QS glyph hosts "+bindings.size()+", compositions "+compositions.size()+"/"+compositionBytes+"B, enabled "+enabled;}}
}
