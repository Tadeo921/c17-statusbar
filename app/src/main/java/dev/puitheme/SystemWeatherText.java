// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Immutable weather publication. The independent read-only provider is the
 * primary source; existing native controller deliveries remain a fallback. */
final class SystemWeatherText {
    static final String CONTROLLER = "com.oplus.keyguard.clock.common.util.WeatherEffectController";
    static final String DATA = "com.oplus.keyguard.clock.common.entry.WeatherData";
    static final String AOSP_DATA = "com.android.systemui.plugins.keyguard.data.model.WeatherData";
    static final String COMMON_MANAGER = "com.oplus.keyguard.clock.common.util.ExtraInfoManager";
    static final String BASE_MANAGER = "com.oplus.keyguard.clock.base.util.ExtraInfoManager";
    static final String BASE_MODEL = "com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel";
    static final String GALLERY_MODEL = "com.oplus.keyguard.clock.gallery.domain.model.ExtraMessageViewModel";
    static final String WIDGET_VIEW="com.oplus.keyguard.style.widgets.container.view.InstantCardView";
    static final String WIDGET_CARD="kswc.yN", WIDGET_ENGINE="kswc.IN";
    static final String WIDGET_INSTANT_CARD="kswc.gt";
    private static final String WIDGET_DATA="pantanal.app.bean.PantanalUIData",WIDGET_INFO="pantanal.app.bean.CardViewInfo";
    static final String UNAVAILABLE = "No hay datos meteorológicos";
    static final String TEMPERATURE_UNAVAILABLE = "No hay datos de temperatura";

    static final class Snapshot {
        final String description, temperature, text;
        final int weatherId;
        final WeatherTextIcon.Kind weatherIconKind;
        final WeatherTextIcon.NativeImage weatherIcon;
        final long revision;
        final long cityId;
        /** -1 unknown, 0 night, 1 day; provider sunrise/sunset is authoritative. */
        final int daylight;
        private final int source;
        /** Last time the owning source delivered or re-confirmed this weather. */
        private volatile long confirmed=SystemWeatherText.now();
        private Snapshot(String description, String temperature, int weatherId, WeatherTextIcon.Kind icon,
                long revision, int source) {
            this(description,temperature,weatherId,icon,null,revision,source);
        }
        private Snapshot(String description,String temperature,int weatherId,WeatherTextIcon.Kind icon,
                WeatherTextIcon.NativeImage image,long revision,int source) {
            this(description,temperature,weatherId,icon,image,revision,source,Long.MIN_VALUE,-1);
        }
        private Snapshot(String description,String temperature,int weatherId,WeatherTextIcon.Kind icon,
                WeatherTextIcon.NativeImage image,long revision,int source,long cityId,int daylight) {
            this.description = description;
            this.temperature = temperature;
            this.weatherId = weatherId;
            this.weatherIconKind = icon;
            this.weatherIcon = image;
            this.text = description.isEmpty() ? UNAVAILABLE
                    : temperature.isEmpty() ? description : description + " " + temperature;
            this.revision = revision;
            this.source = source;
            this.cityId = cityId; this.daylight = daylight;
        }
        String temperatureText() { return temperature.isEmpty() ? TEMPERATURE_UNAVAILABLE : temperature; }
        boolean hasIcon() { return weatherIcon != null || source != 0 && weatherIconKind != WeatherTextIcon.Kind.UNKNOWN; }
    }

    private static volatile Snapshot current = new Snapshot("", "", Integer.MIN_VALUE, WeatherTextIcon.Kind.UNKNOWN, 0L, 0);
    private static volatile long generation;
    // Native WeatherData has no user identity. Once SystemUI changes user,
    // only the explicitly user-bound provider can safely publish this process.
    private static volatile boolean unverifiedFallbackAllowed = true;
    private static final Map<Class<?>, ControllerAccess> CONTROLLERS = new HashMap<>();
    private static final Map<Class<?>, DataAccess> DATA_ACCESS = new HashMap<>();
    private static final Map<Class<?>, ManagerAccess> MANAGERS = new HashMap<>();
    private static final Map<Class<?>, ModelAccess> MODELS = new HashMap<>();
    private static final Map<Class<?>, Field> LAZY_VALUES = new HashMap<>();
    private static final Map<Object, WeakReference<Object>> READ_IDENTITIES = new WeakHashMap<>();
    private static final Map<Class<?>,Map<String,Field>> WIDGET_FIELDS=new HashMap<>();
    private static final Map<Object,WeakReference<Object>> WIDGET_READS=new WeakHashMap<>();
    private static final Map<Object,WeakReference<Object>> INSTANT_DOCUMENTS=new WeakHashMap<>();
    private static final Map<Object,java.util.Set<Integer>> INSTANT_TARGETS=new WeakHashMap<>();
    private static final Map<Object,java.util.Set<Integer>> INSTANT_ICONS=new WeakHashMap<>();
    /** Every text node seen at the last read. A node outside it is a re-rendered
     * page, so its update is re-read once instead of being ignored as unrelated. */
    private static final Map<Object,java.util.Set<Integer>> INSTANT_SEEN=new WeakHashMap<>();
    private static final Map<Object,InstantImage> INSTANT_IMAGES=new WeakHashMap<>();
    private static final String INSTANT="a.a.a.v110017.";
    private static WeakReference<Object> lastWidgetOwner=new WeakReference<>(null);
    private static volatile String lastWidgetReason="widget_unobserved";
    private static WeakReference<Object> currentController = new WeakReference<>(null);
    private SystemWeatherText() { }

    static Snapshot snapshot() { return current; }
    static long revision() { return current.revision; }
    static long generation() { return generation; }
    static synchronized void disableUnverifiedFallback() { unverifiedFallbackAllowed = false; }

    /** Called only by the independent cache query's generation-checked delivery. */
    static synchronized boolean captureProvider(SystemWeatherProvider.Result result,long expectedGeneration) {
        if (result == null || expectedGeneration != generation) return false;
        String description = clean(result.description), temperature = clean(result.temperature);
        Snapshot previous = current;
        if (previous.source == 4 && description.equals(previous.description) && temperature.equals(previous.temperature)
                && result.code == previous.weatherId && result.kind == previous.weatherIconKind
                && result.cityId == previous.cityId && result.daylight == previous.daylight) {
            previous.confirmed = now(); return false;
        }
        current = new Snapshot(description,temperature,result.code,result.kind,null,previous.revision+1L,4,result.cityId,result.daylight);
        return true;
    }

    /** A higher-priority source that has gone quiet this long no longer blocks a
     * fresher, lower-priority one (the lock-screen card can stop updating while
     * the located controller keeps delivering). */
    static final long STALE_MILLIS = 60L * 60L * 1000L;
    /** Desktop checks advance the staleness clock with this; always 0 in the app. */
    static volatile long skewForCheck;
    static long now() { return System.nanoTime() / 1000000L + skewForCheck; }

    /** Detached sample for the settings preview only; never published as weather. */
    static Snapshot sample(String description, String temperature, WeatherTextIcon.Kind icon) {
        return new Snapshot(description, temperature, Integer.MIN_VALUE, icon, 0L, 0);
    }

    /** Diagnostic-only reads. No capture/publish, lazy getter, provider or
     * location operation is performed. Values deliberately contain no city,
     * weather description, temperature or other user-visible text. */
    static final class DiagnosticState {
        final String reason, ownerClass;
        final boolean controllerKnown, controllerShape, located, alive, dataPresent, dataShape,
                descriptionPresent, temperaturePresent, snapshotReady,nativeIconReady;
        final int source;
        final long revision;
        DiagnosticState(String reason,String ownerClass,boolean known,boolean shape,boolean located,
                boolean alive,boolean data,boolean dataShape,boolean description,boolean temperature,Snapshot snapshot) {
            this.reason=reason;this.ownerClass=ownerClass;controllerKnown=known;controllerShape=shape;
            this.located=located;this.alive=alive;dataPresent=data;this.dataShape=dataShape;
            descriptionPresent=description;temperaturePresent=temperature;
            snapshotReady=!snapshot.description.isEmpty();nativeIconReady=snapshot.weatherIcon!=null;source=snapshot.source;revision=snapshot.revision;
        }
    }

    static DiagnosticState diagnose(Object owner) {
        Snapshot snapshot=current;String ownerClass=owner==null?"":owner.getClass().getName();
        if(WIDGET_CARD.equals(ownerClass)||WIDGET_ENGINE.equals(ownerClass)) {
            boolean ready=lastWidgetOwner.get()==owner&&lastWidgetReason.endsWith("_ready");
            return new DiagnosticState(lastWidgetOwner.get()==owner?lastWidgetReason:"widget_unobserved",ownerClass,
                    true,true,true,true,ready,ready,ready,ready,snapshot);
        }
        Object controller=owner;String reason="no_controller";
        try {
            if(owner==null)controller=currentController.get();
            else if(COMMON_MANAGER.equals(ownerClass)||BASE_MANAGER.equals(ownerClass)) {
                ManagerAccess access=managerAccess(owner.getClass());
                if(!access.valid)return diagnostic("manager_shape",ownerClass,snapshot);
                controller=access.controller.get(owner);reason="manager_empty";
            } else if(BASE_MODEL.equals(ownerClass)||GALLERY_MODEL.equals(ownerClass)) {
                ModelAccess access=modelAccess(owner.getClass());
                if(!access.valid)return diagnostic("model_shape",ownerClass,snapshot);
                Object lazy=access.delegate.get(owner);
                if(lazy==null||!"j6.f".equals(lazy.getClass().getName()))return diagnostic("lazy_shape",ownerClass,snapshot);
                Field value=lazyValue(lazy.getClass());
                if(value==null)return diagnostic("lazy_shape",ownerClass,snapshot);
                Object manager=value.get(lazy);
                if(manager==null||!access.managerName.equals(manager.getClass().getName()))return diagnostic("lazy_uninitialised",ownerClass,snapshot);
                ManagerAccess managerAccess=managerAccess(manager.getClass());
                if(!managerAccess.valid)return diagnostic("manager_shape",ownerClass,snapshot);
                controller=managerAccess.controller.get(manager);reason="manager_empty";
            } else if(!CONTROLLER.equals(ownerClass))return diagnostic("unsupported_owner",ownerClass,snapshot);
            if(controller==null)return diagnostic(reason,ownerClass,snapshot);
            if(!CONTROLLER.equals(controller.getClass().getName()))return diagnostic("controller_type",ownerClass,snapshot);
            ControllerAccess access=controllerAccess(controller.getClass());
            if(!access.valid)return new DiagnosticState("controller_shape",ownerClass,true,false,false,false,false,false,false,false,snapshot);
            boolean located=access.located.getBoolean(controller),alive=!access.destroyed.getBoolean(controller);
            Object data=access.data.get(controller);boolean dataShape=data!=null&&DATA.equals(data.getClass().getName());
            boolean description=false,temperature=false;
            if(dataShape) {
                DataAccess dataAccess=dataAccess(data.getClass());dataShape=dataAccess.valid;
                if(dataShape) {
                    String nativeDescription=clean(dataAccess.description.invoke(data));
                    description=!nativeDescription.isEmpty()&&!"--".equals(nativeDescription);
                    int number=((Number)dataAccess.temperature.invoke(data)).intValue();
                    temperature=number!=Integer.MIN_VALUE&&number!=Integer.MAX_VALUE;
                }
            }
            reason=!located?"resident_controller":!alive?"destroyed_controller":data==null?"native_data_empty"
                    :!dataShape?"native_data_shape":!description?"native_description_empty":"native_ready";
            return new DiagnosticState(reason,ownerClass,true,true,located,alive,data!=null,dataShape,description,temperature,snapshot);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
            return diagnostic("reflection_failed",ownerClass,snapshot);
        }
    }

    private static DiagnosticState diagnostic(String reason,String ownerClass,Snapshot snapshot) {
        return new DiagnosticState(reason,ownerClass,false,false,false,false,false,false,false,false,snapshot);
    }

    /** Existing widget cache only. getUIData()/getServiceId() are deliberately
     * not called: their native fallbacks can initialize SDK state. */
    static boolean captureWidgetCard(Object owner) {
        return owner!=null&&WIDGET_INSTANT_CARD.equals(owner.getClass().getName())?captureInstantCard(owner):captureWidget(owner,false);
    }

    /** IN.k BEFORE: native code consumes h then clears it in the same call.
     * This is an already pending native render payload, not a cache query. */
    static boolean captureWidgetEngine(Object owner) { return captureWidget(owner,true); }

    static boolean captureWidgetView(Object view) {
        if(view==null||!WIDGET_VIEW.equals(view.getClass().getName()))return false;
        try {
            Object widget=widgetField(view,"instantCardWidget","com.oplus.keyguard.style.widgets.container.data.InstantCardWidget");
            if(widget==null)return false;
            Object card=widgetField(widget,"card","pantanal.app.Card");
            return captureWidgetCard(card);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unknown){lastWidgetReason="widget_view_shape";return false;}
    }

    /** Bounded diagnostic of an already bound widget. This deliberately does
     * not capture/publish, call SDK getters, initialize lazy state, or query a
     * weather provider. No payload keys or values are returned to the report. */
    static final class WidgetDiagnostic {
        String reason="widget_view_missing",cardClass="",engineClass="",innerClass="",serviceClass="",payloadSource="none";
        boolean viewShape,bindingPresent,cardPresent,cardShape,infoPresent,serviceMatch,lockEntrance,localCard,
                cachePresent,enginePresent,pendingPresent,renderReady,payloadPresent,jsonReady,pageCurrent,rootFields,dataFields,
                weatherPackage,cardDataPresent,initDataPresent,instantUriPresent,rendererKnown,preview,pageKnown,documentKnown,temperatureNode,descriptionNode;
        int payloadBytes,nodeCount,imageNodes,iconNodes;
        String imageSource="",imageStage="",imageDetail="";
    }
    static WidgetDiagnostic diagnoseWidgetView(Object view) {
        WidgetDiagnostic state=new WidgetDiagnostic();
        if(view==null)return state;
        if(!WIDGET_VIEW.equals(view.getClass().getName())){state.reason="widget_view_type";return state;}
        state.viewShape=true;
        try {
            state.reason="view_binding_field";
            Object binding=widgetField(view,"instantCardWidget","com.oplus.keyguard.style.widgets.container.data.InstantCardWidget");
            state.bindingPresent=binding!=null;if(binding==null){state.reason="widget_binding_empty";return state;}
            state.reason="binding_card_field";Object card=widgetField(binding,"card","pantanal.app.Card");
            state.cardPresent=card!=null;if(card==null){state.reason="widget_card_empty";return state;}
            state.cardClass=card.getClass().getName();boolean instant=WIDGET_INSTANT_CARD.equals(state.cardClass);
            state.cardShape=WIDGET_CARD.equals(state.cardClass)||instant;
            if(!state.cardShape){state.reason="widget_card_type";return state;}
            state.reason="card_info_field";Object info=widgetField(card,instant?"b":"c",WIDGET_INFO);
            state.infoPresent=info!=null;if(info==null){state.reason="widget_info_empty";return state;}
            state.reason="info_service_field";Object service=widgetField(info,"serviceId","java.lang.String");
            state.serviceMatch="536871060".equals(service)||"53687106002".equals(service);
            state.reason="info_entrance_field";Object entrance=widgetField(info,"entrance","pantanal.app.bean.Entrance");
            state.lockEntrance=entrance instanceof Enum<?> &&"LOCK_SCREEN".equals(((Enum<?>)entrance).name());
            state.reason="info_local_field";state.localCard=Boolean.TRUE.equals(widgetField(info,"isLocalCard","boolean"));
            if(instant){diagnoseInstantCard(card,info,state);return state;}
            state.reason="card_cache_field";Object cached=widgetField(card,"n",WIDGET_DATA);state.cachePresent=cached!=null;
            byte[] raw=null;
            if(cached!=null){state.reason="cache_payload_field";raw=(byte[])widgetField(cached,"data","[B");}
            state.reason="card_engine_field";Object engine=widgetField(card,"i",WIDGET_ENGINE);state.enginePresent=engine!=null;
            if(engine!=null) {
                state.reason="engine_pending_field";byte[] pending=(byte[])widgetField(engine,"h","[B");state.pendingPresent=pending!=null&&pending.length>0;
                state.reason="engine_ready_field";state.renderReady=widgetField(engine,"q","com.oplus.seedling.sdk.seedling.ISeedling")!=null;
                if(raw==null)raw=pending;
            }
            state.payloadPresent=raw!=null&&raw.length>0;state.payloadBytes=raw==null?0:raw.length;
            if(!state.serviceMatch||!state.lockEntrance||"53687106002".equals(service)&&!state.localCard){state.reason="widget_scope";return state;}
            if(!state.payloadPresent){state.reason="widget_no_bound_payload";return state;}
            if(raw.length>262144){state.reason="widget_payload_bound";return state;}
            state.reason="widget_json";JSONObject root=new JSONObject(new String(raw,StandardCharsets.UTF_8));state.jsonReady=true;
            JSONObject options=root.optJSONObject("options");state.pageCurrent=options!=null&&"pages/weather_info".equals(options.optString("pageId",""));
            state.rootFields=widgetWeatherFields(root);state.dataFields=widgetWeatherFields(root.optJSONObject("data"));
            state.reason=!state.pageCurrent?"widget_page":state.rootFields==state.dataFields?state.rootFields?"widget_ambiguous":"widget_schema"
                    :state.rootFields?"widget_root_schema":"widget_data_schema";
        } catch(ReflectiveOperationException|RuntimeException|LinkageError|org.json.JSONException unsupported){ /* The fixed stage is the failure reason. */ }
        return state;
    }

    /** gt is the real InstantCard backend, not a Seedling yN alias. Its
     * getUIData() is always null. Inspect only already stored metadata and
     * engine references; getView() may mutate the view tree and is not used. */
    private static void diagnoseInstantCard(Object card,Object info,WidgetDiagnostic state)throws ReflectiveOperationException {
        state.reason="instant_package_field";state.weatherPackage="com.coloros.weather2".equals(widgetField(info,"packageName","java.lang.String"));
        state.reason="instant_card_data_field";String cardData=(String)widgetField(info,"cardUiData","java.lang.String");state.cardDataPresent=cardData!=null&&!cardData.isEmpty();
        state.reason="instant_init_data_field";String initData=(String)widgetField(info,"initData","java.lang.String");state.initDataPresent=initData!=null&&!initData.isEmpty();
        state.reason="instant_uri_field";String uri=(String)widgetField(info,"instantUri","java.lang.String");state.instantUriPresent=uri!=null&&!uri.isEmpty();
        state.cachePresent=state.cardDataPresent||state.initDataPresent||state.instantUriPresent;
        state.reason="instant_engine_field";Object engine=widgetField(card,"i","kswc.at");state.enginePresent=engine!=null;
        if(engine!=null){
            state.reason="instant_inner_field";Object nativeCard=widgetField(engine,"l","org.hapjs.card.api.Card");
            if(nativeCard!=null){state.engineClass=nativeCard.getClass().getName();state.renderReady=true;
                if("org.hapjs.card.sdk.CardDelegator".equals(state.engineClass)){
                    state.reason="instant_delegate_field";Object delegate=widgetField(nativeCard,"mCard","java.lang.Object");
                    if(delegate!=null)state.innerClass=delegate.getClass().getName();
                    state.reason="instant_service_field";Object service=widgetField(nativeCard,"cardService","org.hapjs.card.api.CardService");
                    if(service!=null)state.serviceClass=service.getClass().getName();
                }
            }
        }
        // These strings are not assumed to be Seedling JSON. A probe reports
        // only fixed known schema predicates, never URI, payload keys or values.
        if(!diagnoseInstantPayload(cardData,"card_ui_data",state)&&!diagnoseInstantPayload(initData,"init_data",state))diagnoseInstantPayload(uri,"instant_uri",state);
        state.reason=state.jsonReady?"widget_instant_json":state.cachePresent?"widget_instant_cache_shape":"widget_instant_cache_empty";
        if("a.a.a.v110017.Dc".equals(state.innerClass)){
            InstantRead read=readInstantCard(card,false);state.reason=read.reason;state.rendererKnown=read.rendererKnown;
            state.preview=read.preview;state.pageKnown=read.pageKnown;state.documentKnown=read.documentKnown;state.nodeCount=read.nodeCount;
            state.temperatureNode=read.temperatureNode;state.descriptionNode=read.descriptionNode;state.weatherPackage|=read.weatherPackage;
            state.imageNodes=read.imageNodes;state.iconNodes=read.iconNodes;state.imageSource=read.imageSource;state.imageStage=lastImageStage;state.imageDetail=lastImageDetail;
        }
    }
    private static boolean diagnoseInstantPayload(String value,String source,WidgetDiagnostic state){
        if(value==null||value.isEmpty()||value.length()>262144||!value.trim().startsWith("{"))return false;
        try{
            JSONObject root=new JSONObject(value);state.payloadSource=source;state.payloadPresent=true;state.payloadBytes=value.length();state.jsonReady=true;
            JSONObject options=root.optJSONObject("options");state.pageCurrent=options!=null&&"pages/weather_info".equals(options.optString("pageId",""));
            state.rootFields=widgetWeatherFields(root);state.dataFields=widgetWeatherFields(root.optJSONObject("data"));return true;
        }catch(org.json.JSONException|RuntimeException malformed){return false;}
    }

    /** Already compiled WeatherNow card attributes. These names come from the
     * installed WeatherNowRpk template, not from text/number guessing. */
    private static final class InstantRead {
        String reason="instant_shape",description="",temperature="";
        WeatherTextIcon.Kind iconKind=WeatherTextIcon.Kind.UNKNOWN;
        WeatherTextIcon.NativeImage image;
        boolean rendererKnown,weatherPackage,preview,pageKnown,documentKnown,temperatureNode,descriptionNode;
        int nodeCount,imageNodes,iconNodes;
        String imageSource="";
    }
    /** Last stage of the native icon capture; a fixed vocabulary for diagnostics, never a payload. */
    private static volatile String lastImageStage="not_attempted",lastImageDetail="";
    /** Class family, sizes and clonability only: enough to tell which native drawable kind the template loaded. */
    private static String drawableDetail(android.graphics.drawable.Drawable drawable){
        try {
            android.graphics.Rect bounds=drawable.getBounds();
            return "sup-"+shortClass(String.valueOf(drawable.getClass().getSuperclass().getName()))+"_i"+drawable.getIntrinsicWidth()+"x"+drawable.getIntrinsicHeight()
                    +"_b"+bounds.width()+"x"+bounds.height()+"_cs"+(drawable.getConstantState()!=null?1:0);
        }catch(RuntimeException|LinkageError unavailable){return "detail_failed";}
    }
    static ClassLoader instantRendererLoader(Object view){
        try{Object binding=widgetField(view,"instantCardWidget","com.oplus.keyguard.style.widgets.container.data.InstantCardWidget");
            Object card=binding==null?null:widgetField(binding,"card","pantanal.app.Card");
            Object inner=instantInner(card);return inner==null?null:inner.getClass().getClassLoader();
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return null;}
    }
    static synchronized boolean observedInstantDocument(Object document){WeakReference<Object> reference=INSTANT_DOCUMENTS.get(document);return reference!=null&&reference.get()!=null;}
    /** The exact loaded renderer's u(Drawable) callback is only observed for a
     * previously verified current-weather image. No global ImageView hook. */
    static synchronized Object instantImageDocument(Object view) {
        InstantImage binding=INSTANT_IMAGES.get(view);if(binding==null)return null;
        Object document=binding.document.get();if(!observedInstantDocument(document))return null;
        binding.delivered=true;return document;
    }
    static Object instantPictureDocument(Object drawable) {
        if(!(drawable instanceof android.graphics.drawable.Drawable))return null;
        return instantImageDocument(((android.graphics.drawable.Drawable)drawable).getCallback());
    }
    private static final class InstantImage {
        final WeakReference<Object> document;
        String source;
        WeatherTextIcon.NativeImage image;
        boolean delivered;
        InstantImage(Object document){this.document=new WeakReference<>(document);}
    }
    private static synchronized WeatherTextIcon.NativeImage instantImage(Object component,Object document,String source) {
        try {
        Object view=widgetBaseField(component,INSTANT+"pi","m","android.view.View");
        if(view==null){lastImageStage="no_host_view";return null;}
        if(!(INSTANT+"ey").equals(view.getClass().getName())){lastImageStage="view_class:"+shortClass(view.getClass().getName());return null;}
        InstantImage binding=INSTANT_IMAGES.get(view);
        if(binding==null||binding.document.get()!=document) {
            if(INSTANT_IMAGES.size()>=8&&!INSTANT_IMAGES.containsKey(view))return null;
            binding=new InstantImage(document);INSTANT_IMAGES.put(view,binding);
        }
        // ey.getDrawable() can synchronously rasterize a native app-animation
        // asset. Read the framework's existing drawable instead of calling it.
        Object owner=widgetField(view,android.widget.ImageView.class,"mDrawable","android.graphics.drawable.Drawable");
        WeatherTextIcon.NativeImage next=WeatherTextIcon.NativeImage.capture((android.graphics.drawable.Drawable)owner,binding.image,binding.delivered);
        if(binding.source!=null&&!binding.source.equals(source)&&next==binding.image&&!binding.delivered){lastImageStage="source_changed_waiting";return null;}
        binding.source=source;binding.image=next;binding.delivered=false;
        lastImageStage=next!=null?(next.recorded?"image_recorded":"image_ready"):owner==null?"no_drawable":"drawable_not_capturable:"+shortClass(owner.getClass().getName());
        lastImageDetail=owner==null?"":drawableDetail((android.graphics.drawable.Drawable)owner);return next;
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){lastImageStage="reflection_failed";return null;}
    }
    private static String shortClass(String name){int cut=name.lastIndexOf('.');String value=cut<0?name:name.substring(cut+1);return value.length()>40?value.substring(0,40):value;}
    static boolean relevantInstantUpdate(Object document,Object action){
        if(!observedInstantDocument(document)||action==null)return false;
        try{
            Map<?,?> attrs=(Map<?,?>)widgetField(action,"j","android.util.ArrayMap");
            boolean value=attrs.containsKey("value"),source=attrs.containsKey("src");if(!value&&!source)return false;
            Integer ref=(Integer)widgetField(action,"d","int");
            synchronized(SystemWeatherText.class){java.util.Set<Integer> known=INSTANT_TARGETS.get(document),icons=INSTANT_ICONS.get(document),seen=INSTANT_SEEN.get(document);
                boolean unseen=seen==null||!seen.contains(ref);
                return value&&(known==null||known.contains(ref)||unseen)||source&&(icons==null||icons.contains(ref)||icons.isEmpty()&&unseen);}
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){return false;}
    }
    static boolean captureInstantDocument(Object document){
        Object owner;synchronized(SystemWeatherText.class){WeakReference<Object> reference=INSTANT_DOCUMENTS.get(document);owner=reference==null?null:reference.get();}
        return owner!=null&&captureInstantCard(owner);
    }
    private static Object instantInner(Object card)throws ReflectiveOperationException{
        if(card==null||!WIDGET_INSTANT_CARD.equals(card.getClass().getName()))return null;
        Object engine=widgetField(card,"i","kswc.at");if(engine==null)return null;
        Object delegate=widgetField(engine,"l","org.hapjs.card.api.Card");
        if(delegate==null||!"org.hapjs.card.sdk.CardDelegator".equals(delegate.getClass().getName()))return null;
        Object inner=widgetField(delegate,"mCard","java.lang.Object");
        return inner!=null&&(INSTANT+"Dc").equals(inner.getClass().getName())?inner:null;
    }
    private static boolean captureInstantCard(Object card){
        long expectedGeneration=generation;InstantRead read=readInstantCard(card,true);
        if(!"instant_ready".equals(read.reason))return widgetRejected(card,read.reason);
        boolean changed=publish(read.description,read.temperature,Integer.MIN_VALUE,read.iconKind,read.image,3,expectedGeneration);
        synchronized(SystemWeatherText.class){if(generation==expectedGeneration){lastWidgetOwner=new WeakReference<>(card);lastWidgetReason=read.reason;}}
        return changed;
    }
    private static InstantRead readInstantCard(Object card,boolean observe){
        InstantRead read=new InstantRead();
        try{
            Object info=widgetField(card,"b",WIDGET_INFO);if(info==null){read.reason="instant_info_empty";return read;}
            Object entrance=widgetField(info,"entrance","pantanal.app.bean.Entrance");
            if(!(entrance instanceof Enum<?>)||!"LOCK_SCREEN".equals(((Enum<?>)entrance).name())){read.reason="instant_not_lock";return read;}
            Object inner=instantInner(card);read.rendererKnown=inner!=null;if(inner==null){read.reason="instant_renderer_empty";return read;}
            Object frontend=widgetField(inner,"b","org.hapjs.card.support.d");if(frontend==null){read.reason="instant_frontend_empty";return read;}
            read.reason="instant_package_field";Object pkg=widgetBaseField(frontend,INSTANT+"A90","g","java.lang.String");
            read.weatherPackage="com.weathernow.desk".equals(pkg);if(!read.weatherPackage){read.reason="instant_other_package";return read;}
            read.reason="instant_preview_field";read.preview=Boolean.TRUE.equals(widgetBaseField(frontend,INSTANT+"A90","A","boolean"));
            if(read.preview){read.reason="instant_preview";return read;}
            read.reason="instant_pages_field";Object manager=widgetBaseField(frontend,INSTANT+"A90","l",INSTANT+"B10");
            if(manager==null){read.reason="instant_pages_empty";return read;}
            java.util.List<?> pages=(java.util.List<?>)widgetField(manager,"b","java.util.List");Object page;
            synchronized(pages){if(pages.isEmpty()||pages.size()>8){read.reason="instant_pages_bound";return read;}page=pages.get(pages.size()-1);}
            read.reason="instant_route_field";Object pageInfo=widgetField(page,"f",INSTANT+"N90");
            if(pageInfo==null||!"org.hapjs.model.CardInfo".equals(pageInfo.getClass().getName())){read.reason="instant_route_type";return read;}
            Object path=widgetField(pageInfo,"mPath","java.lang.String");
            read.pageKnown="cards/card1x1".equals(path)||"/cards/card1x1".equals(path)||"cards/card2x1".equals(path)||"/cards/card2x1".equals(path);
            if(!read.pageKnown){read.reason="instant_other_page";return read;}
            read.reason="instant_document_field";Object document=widgetBaseField(frontend,INSTANT+"A90","c",INSTANT+"bp0");
            if(document==null){read.reason="instant_document_empty";return read;}
            Object context=widgetField(document,"h",INSTANT+"Gr");
            if(context==null||widgetBaseField(context,INSTANT+"Gr","E0",INSTANT+"v10")!=page){read.reason="instant_document_owner";return read;}
            read.documentKnown=true;
            if(observe)synchronized(SystemWeatherText.class){if(INSTANT_DOCUMENTS.size()<4||INSTANT_DOCUMENTS.containsKey(document))INSTANT_DOCUMENTS.put(document,new WeakReference<>(card));}
            read.reason="instant_nodes_field";Map<?,?> nodes=(Map<?,?>)widgetField(document,"g","java.util.HashMap");read.nodeCount=nodes.size();
            if(read.nodeCount>128){read.reason="instant_nodes_bound";return read;}
            String description=null,temperature=null;java.util.Set<Integer> targetRefs=new java.util.HashSet<>(),iconRefs=new java.util.HashSet<>(),seenRefs=new java.util.HashSet<>();
            WeatherTextIcon.Kind icon=null;boolean iconConflict=false;WeatherTextIcon.NativeImage image=null;
            for(Map.Entry<?,?> entry:nodes.entrySet()){
                Object node=entry.getValue();
                if(node==null)continue;String nodeType=(String)widgetBaseField(node,INSTANT+"kp0","d","java.lang.String");
                if(!"text".equals(nodeType)&&!"image".equals(nodeType))continue;
                Object component=widgetBaseField(node,INSTANT+"kp0","e",INSTANT+"yi");
                if(component==null)continue;
                if(hasBase(component,INSTANT+"ni"))component=widgetBaseField(component,INSTANT+"ni","m",INSTANT+"pi");
                if(component==null||!hasBase(component,INSTANT+"pi"))continue;
                Object element=widgetBaseField(component,INSTANT+"pi","e",INSTANT+"d80");if(element==null)continue;
                Map<?,?> attrs=(Map<?,?>)widgetBaseField(component,INSTANT+"pi","D","android.util.ArrayMap");
                if("image".equals(nodeType)){
                    read.imageNodes++;
                    Object parent;synchronized(element){parent=widgetField(element,"b",INSTANT+"d80");}if(parent==null)continue;
                    String[] parentClasses;synchronized(parent){parentClasses=(String[])widgetField(parent,"g","[Ljava.lang.String;");}
                    // The actual template puts class=icon on the parent div;
                    // its unavailable sunny placeholder puts it on the image.
                    if(!containsClass(parentClasses,"icon"))continue;
                    read.iconNodes++;if(attrs.get("src") instanceof String)read.imageSource=imageName((String)attrs.get("src"));
                    if(entry.getKey() instanceof Integer)iconRefs.add((Integer)entry.getKey());
                    WeatherTextIcon.Kind candidate=instantIcon(attrs.get("src"),(String)path);
                    if(icon!=null)iconConflict=true;icon=candidate;
                    if(observe&&candidate!=WeatherTextIcon.Kind.UNKNOWN&&attrs.get("src") instanceof String)
                        image=instantImage(component,document,(String)attrs.get("src"));
                    continue;
                }
                if(entry.getKey() instanceof Integer&&seenRefs.size()<256)seenRefs.add((Integer)entry.getKey());
                String[] classes;synchronized(element){classes=(String[])widgetField(element,"g","[Ljava.lang.String;");}
                boolean temp=containsClass(classes,"tmp"),desc=containsClass(classes,"cond");if(!temp&&!desc)continue;
                if(entry.getKey() instanceof Integer)targetRefs.add((Integer)entry.getKey());
                Object raw=attrs.get("value");
                if(!(raw instanceof String))continue;String value=clean(raw);
                if(temp){read.temperatureNode=true;String normalized=instantTemperature(value);if(normalized.isEmpty()){read.reason="instant_temperature_empty";return read;}
                    if(temperature!=null&&!temperature.equals(normalized)){read.reason="instant_temperature_conflict";return read;}temperature=normalized;}
                if(desc){read.descriptionNode=true;if(value.isEmpty()||"--".equals(value)){read.reason="instant_description_empty";return read;}
                    if(description!=null&&!description.equals(value)){read.reason="instant_description_conflict";return read;}description=value;}
            }
            if(observe)synchronized(SystemWeatherText.class){if(!targetRefs.isEmpty())INSTANT_TARGETS.put(document,targetRefs);INSTANT_ICONS.put(document,iconRefs);INSTANT_SEEN.put(document,seenRefs);}
            if(description==null||temperature==null){read.reason="instant_weather_nodes_empty";return read;}
            read.iconKind=icon==null||iconConflict?WeatherTextIcon.Kind.UNKNOWN:icon;
            read.image=iconConflict?null:image;
            read.description=description;read.temperature=temperature;read.reason="instant_ready";
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported){/* Fixed stage only; no payload/exception text. */}
        return read;
    }
    /** The widget template may lead or trail with its unit, use ℃/℉, a typographic
     * minus or a space, or leave the unit to another node. All of these are the
     * same bounded current temperature; anything else is rejected, not guessed. */
    private static final java.util.regex.Pattern INSTANT_TEMPERATURE=java.util.regex.Pattern.compile(
            "(°[CF]?|℃|℉)?\\s*([-\u2212\u2013]?)([0-9]{1,3}(?:\\.[0-9])?)\\s*(°[CF]?|℃|℉|[CF])?");
    /** Resource tail only (directory + file), e.g. "images:sunny.svg": template vocabulary, no user data. */
    private static String imageName(String source){
        int last=source.lastIndexOf('/'),previous=last<=0?-1:source.lastIndexOf('/',last-1);
        String value=source.substring(previous<0?0:previous+1).replace('/',':');return value.length()>48?value.substring(0,48):value;
    }
    private static String instantTemperature(String value){
        java.util.regex.Matcher match=INSTANT_TEMPERATURE.matcher(value.trim());
        if(!match.matches()||match.group(1)!=null&&match.group(4)!=null)return "";
        String unit=match.group(1)!=null?match.group(1):match.group(4);
        if(unit==null)unit="°";
        else if(unit.equals("℃")||unit.equals("C"))unit="°C";
        else if(unit.equals("℉")||unit.equals("F"))unit="°F";
        return (match.group(2).isEmpty()?"":"-")+match.group(3)+unit;
    }
    /** Native resolved filenames already include its day/night choice. Keep
     * the exact current route and fixed installed-template vocabulary; never
     * classify descriptions, arbitrary URLs, or launcher/placeholder assets. */
    private static WeatherTextIcon.Kind instantIcon(Object value,String path){
        if(!(value instanceof String)||path==null)return WeatherTextIcon.Kind.UNKNOWN;
        String route=path.startsWith("/")?path:"/"+path,source=(String)value,prefix=route+"/images/";
        if(!source.startsWith(prefix))return WeatherTextIcon.Kind.UNKNOWN;
        switch(source.substring(prefix.length())){
            case "sunny.svg":return WeatherTextIcon.Kind.SUNNY;
            case "night_sunny.svg":return WeatherTextIcon.Kind.CLEAR_NIGHT;
            case "cloudy.svg":return WeatherTextIcon.Kind.PARTLY_CLOUDY;
            case "cloudy_nights.svg":return WeatherTextIcon.Kind.CLOUDY_NIGHT;
            case "negative.svg":return WeatherTextIcon.Kind.CLOUDY;
            case "light_rain.svg":return WeatherTextIcon.Kind.LIGHT_RAIN;
            case "mid_rain.svg":return WeatherTextIcon.Kind.MODERATE_RAIN;
            case "heavy_rain.svg":return WeatherTextIcon.Kind.HEAVY_RAIN;
            case "freezing_rain.svg":return WeatherTextIcon.Kind.ICE_RAIN;
            case "hail.svg":return WeatherTextIcon.Kind.HAILSTONE;
            case "thunder.svg":return WeatherTextIcon.Kind.THUNDERSHOWERS;
            case "mixed_rain_and_hail.svg":return WeatherTextIcon.Kind.THUNDERSHOWERS_HAILSTONE;
            case "thunderstorms.svg":return WeatherTextIcon.Kind.THUNDERSTORM;
            case "flurries.svg":return WeatherTextIcon.Kind.LIGHT_SNOW;
            case "mid_snow.svg":return WeatherTextIcon.Kind.MODERATE_SNOW;
            case "heavy_snow.svg":return WeatherTextIcon.Kind.HEAVY_SNOW;
            case "rain_snow.svg":return WeatherTextIcon.Kind.SLEET;
            case "fog.svg":return WeatherTextIcon.Kind.FOG;
            case "float_dust.svg":return WeatherTextIcon.Kind.SINK_FLOAT;
            case "sand_dust.svg":return WeatherTextIcon.Kind.DUST_DEVIL;
            case "haze.svg":return WeatherTextIcon.Kind.HAZE;
            case "wind.svg":return WeatherTextIcon.Kind.WIND;
            case "storm.svg":return WeatherTextIcon.Kind.STORM;
            case "tornado.svg":return WeatherTextIcon.Kind.TORNADO;
            case "sunrise.svg":return WeatherTextIcon.Kind.SUNRISE;
            case "sunset.svg":return WeatherTextIcon.Kind.SUNSET;
            default:return WeatherTextIcon.Kind.UNKNOWN;
        }
    }
    private static boolean containsClass(String[] values,String wanted){if(values!=null&&values.length<=16)for(String value:values)if(wanted.equals(value))return true;return false;}
    private static boolean hasBase(Object owner,String wanted){for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())if(wanted.equals(type.getName()))return true;return false;}
    private static Object widgetBaseField(Object owner,String declaring,String name,String expectedType)throws ReflectiveOperationException{
        if(owner==null)throw new NoSuchFieldException(name);for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())
            if(declaring.equals(type.getName()))return widgetField(owner,type,name,expectedType);throw new NoSuchFieldException(name);
    }

    private static boolean captureWidget(Object owner,boolean engine) {
        if(owner==null||!(engine?WIDGET_ENGINE:WIDGET_CARD).equals(owner.getClass().getName()))return false;
        long expectedGeneration=generation;
        try {
            Object info=widgetField(owner,engine?"a":"c",WIDGET_INFO);
            if(info==null)return widgetRejected(owner,"widget_info_empty");
            Object service=widgetField(info,"serviceId","java.lang.String");
            Object entrance=widgetField(info,"entrance","pantanal.app.bean.Entrance");
            boolean local=Boolean.TRUE.equals(widgetField(info,"isLocalCard","boolean"));
            if(!(entrance instanceof Enum<?>)||!"LOCK_SCREEN".equals(((Enum<?>)entrance).name())
                    ||!("536871060".equals(service)||local&&"53687106002".equals(service)))return widgetRejected(owner,"widget_scope");
            byte[] raw;
            if(engine) {
                if(widgetField(owner,"q","com.oplus.seedling.sdk.seedling.ISeedling")==null)return widgetRejected(owner,"widget_engine_empty");
                raw=(byte[])widgetField(owner,"h","[B");
            } else {
                Object data=widgetField(owner,"n",WIDGET_DATA);
                if(data==null)return widgetRejected(owner,"widget_cache_empty");
                raw=(byte[])widgetField(data,"data","[B");
            }
            if(raw==null||raw.length==0||raw.length>262144)return widgetRejected(owner,"widget_payload_empty");
            synchronized(SystemWeatherText.class) {
                WeakReference<Object> read=WIDGET_READS.get(owner);
                if(current.source==3&&read!=null&&read.get()==raw)return false;
            }
            JSONObject root=new JSONObject(new String(raw,StandardCharsets.UTF_8));
            JSONObject options=root.optJSONObject("options");
            if(options==null||!"pages/weather_info".equals(options.optString("pageId","")))return widgetRejected(owner,"widget_page");
            JSONObject nested=root.optJSONObject("data");
            boolean rootFields=widgetWeatherFields(root),dataFields=widgetWeatherFields(nested);
            if(rootFields==dataFields)return widgetRejected(owner,rootFields?"widget_ambiguous":"widget_schema");
            JSONObject weather=rootFields?root:nested;
            if(!"1".equals(String.valueOf(weather.opt("is_location")))||widgetFuture(weather))return widgetRejected(owner,"widget_location");
            String description=clean(weather.opt("weather_des")),temperature=clean(weather.opt("template_temp")),unit=clean(weather.opt("template_unit"));
            if(description.isEmpty()||"--".equals(description)||!temperature.matches("-?[0-9]{1,3}(?:\\.[0-9])?")
                    ||!("°".equals(unit)||"℃".equals(unit)||"℉".equals(unit)||"°C".equals(unit)||"°F".equals(unit)))return widgetRejected(owner,"widget_default_or_invalid");
            boolean changed=publish(description,temperature+unit,Integer.MIN_VALUE,WeatherTextIcon.Kind.UNKNOWN,3,expectedGeneration);
            synchronized(SystemWeatherText.class) {
                if(generation==expectedGeneration) {
                    if(WIDGET_READS.size()<8||WIDGET_READS.containsKey(owner))WIDGET_READS.put(owner,new WeakReference<>(raw));
                    lastWidgetOwner=new WeakReference<>(owner);lastWidgetReason=rootFields?"widget_root_ready":"widget_data_ready";
                }
            }
            return changed;
        } catch(ReflectiveOperationException|RuntimeException|LinkageError|org.json.JSONException unknown){return widgetRejected(owner,"widget_shape_or_json");}
    }

    private static boolean widgetWeatherFields(JSONObject object) {
        return object!=null&&object.has("weather_des")&&object.has("template_temp")&&object.has("template_unit")&&object.has("is_location");
    }
    private static boolean widgetFuture(JSONObject object) {
        for(String key:new String[]{"is_tomorrow","is_destionation","is_destination"}) {
            Object value=object.opt(key);if(Boolean.TRUE.equals(value)||"1".equals(String.valueOf(value))||"true".equalsIgnoreCase(String.valueOf(value)))return true;
        }
        return false;
    }
    private static synchronized boolean widgetRejected(Object owner,String reason) {lastWidgetOwner=new WeakReference<>(owner);lastWidgetReason=reason;return false;}
    private static Object widgetField(Object owner,String name,String expectedType)throws ReflectiveOperationException {
        return widgetField(owner,owner.getClass(),name,expectedType);
    }
    private static Object widgetField(Object owner,Class<?> type,String name,String expectedType)throws ReflectiveOperationException {
        Field found;
        synchronized(SystemWeatherText.class) {
            Map<String,Field> fields=WIDGET_FIELDS.get(type);
            if(fields==null){if(WIDGET_FIELDS.size()>=32)WIDGET_FIELDS.clear();fields=new HashMap<>();WIDGET_FIELDS.put(type,fields);}
            found=fields.get(name);
            if(found==null){found=type.getDeclaredField(name);if(!expectedType.equals(found.getType().getName()))throw new NoSuchFieldException(name);found.setAccessible(true);fields.put(name,found);}
        }
        return found.get(owner);
    }

    /** Call after the real located controller's native delivery, including its
     * queued sendWeatherData$lambda$15 callback. Resident weather is excluded. */
    static boolean captureController(Object controller) {
        return captureController(controller, generation);
    }

    /** A hook installed after the first native delivery must also seed the
     * existing cache. Reading these exact fields cannot construct a controller
     * or start another provider/location request. */
    static boolean captureManager(Object manager) {
        return captureManager(manager, generation);
    }

    private static boolean captureManager(Object manager,long expectedGeneration) {
        if(manager==null)return false;
        try {
            ManagerAccess access=managerAccess(manager.getClass());
            if(!access.valid)return false;
            return captureController(access.controller.get(manager),expectedGeneration,true);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
            return false;
        }
    }

    /** Only an already initialized active native model may supply a manager.
     * This ROM's j6.f.a() lazy getter can create it; read its verified volatile
     * value field instead and reject the uninitialized sentinel by exact type. */
    static boolean captureViewModel(Object model) {
        long expectedGeneration=generation;
        if(model==null)return false;
        try {
            ModelAccess access=modelAccess(model.getClass());
            if(!access.valid)return false;
            Object lazy=access.delegate.get(model);
            if(lazy==null||!"j6.f".equals(lazy.getClass().getName()))return false;
            Field value=lazyValue(lazy.getClass());if(value==null)return false;
            Object manager=value.get(lazy);
            if(manager==null||!access.managerName.equals(manager.getClass().getName()))return false;
            return captureManager(manager,expectedGeneration);
        } catch(ReflectiveOperationException|RuntimeException|LinkageError unsupported) {
            return false;
        }
    }

    /** The verified native WeatherData fields are final. Repeated native cache
     * reads of the same model skip reflection and formatting entirely. Weak
     * identities do not keep a controller, its Context or model alive. */
    static boolean captureRead(Object controller, Object returnedData) {
        if (!unverifiedFallbackAllowed) return false;
        long expectedGeneration = generation;
        if (controller == null || !CONTROLLER.equals(controller.getClass().getName())) return false;
        synchronized (SystemWeatherText.class) {
            if (generation != expectedGeneration) return false;
            WeakReference<Object> previous = READ_IDENTITIES.get(controller);
            if (currentController.get()==controller&&previous != null && previous.get() == returnedData) return false;
        }
        boolean changed = captureController(controller, expectedGeneration);
        synchronized (SystemWeatherText.class) {
            if (generation == expectedGeneration) READ_IDENTITIES.put(controller, new WeakReference<>(returnedData));
        }
        return changed;
    }

    private static boolean captureController(Object controller, long expectedGeneration) {
        return captureController(controller,expectedGeneration,false);
    }

    private static boolean captureController(Object controller,long expectedGeneration,boolean passiveSeed) {
        if (!unverifiedFallbackAllowed) return false;
        if (controller == null || !CONTROLLER.equals(controller.getClass().getName())) return false;
        try {
            ControllerAccess access = controllerAccess(controller.getClass());
            if (!access.valid || !access.located.getBoolean(controller)) return false;
            if (access.destroyed.getBoolean(controller)) return false;
            Object data = access.data.get(controller);
            // A second native clock may have created its located controller
            // before its first result. Only a real delivery can clear weather;
            // observing that prewarming manager must preserve valid data.
            if(passiveSeed&&(data==null||!DATA.equals(data.getClass().getName())))return false;
            synchronized(SystemWeatherText.class) {
                if(generation!=expectedGeneration)return false;
                WeakReference<Object> previous=READ_IDENTITIES.get(controller);
                if(currentController.get()==controller&&previous!=null&&previous.get()==data)return false;
            }
            boolean changed;
            if (data == null || !DATA.equals(data.getClass().getName())) changed = publish("", "", 2, expectedGeneration);
            else changed = capture(data, 2, expectedGeneration);
            synchronized (SystemWeatherText.class) {
                if (generation == expectedGeneration) {
                    READ_IDENTITIES.put(controller, new WeakReference<>(data));
                    if(currentController.get()!=controller)currentController=new WeakReference<>(controller);
                }
            }
            return changed;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            return false;
        }
    }

    /** Optional fallback from an actual onWeatherDataChanged argument. Do not
     * hook constructors: preview/placeholder models are constructed natively. */
    static boolean captureAosp(Object data) {
        if (!unverifiedFallbackAllowed) return false;
        long expectedGeneration = generation;
        if (current.source == 2) return false;
        if (data == null) return publish("", "", 1, expectedGeneration);
        if (!AOSP_DATA.equals(data.getClass().getName())) return false;
        return capture(data, 1, expectedGeneration);
    }

    private static boolean capture(Object data, int source, long expectedGeneration) {
        try {
            DataAccess access = dataAccess(data.getClass());
            if (!access.valid) return false;
            String description = clean(access.description.invoke(data));
            // Empty native description is also used for missing provider data;
            // do not turn its numeric fallback into a fabricated 0 degree value.
            if (description.isEmpty() || "--".equals(description)) return publish("", "", source, expectedGeneration);
            int value = ((Number) access.temperature.invoke(data)).intValue();
            String temperature = "";
            if (value != Integer.MIN_VALUE && value != Integer.MAX_VALUE) {
                if (source == 2) {
                    // The verified native formatter is locale integer + °.
                    // Use a private NumberFormat, avoiding its shared mutable
                    // cache when native delivery originates off the UI thread.
                    temperature = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value) + "°";
                } else {
                    boolean celsius = Boolean.TRUE.equals(access.celsius.invoke(data));
                    temperature = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value) + (celsius ? "°C" : "°F");
                }
            }
            int weatherId = Integer.MIN_VALUE;
            WeatherTextIcon.Kind icon = WeatherTextIcon.Kind.UNKNOWN;
            if (source == 2) {
                weatherId = ((Number) access.weatherId.invoke(data)).intValue();
                int resourceId = ((Number) access.resId.invoke(data)).intValue();
                icon = WeatherTextIcon.nativeKind(data.getClass().getClassLoader(), resourceId);
            } else if (access.state != null) {
                Object state = access.state.invoke(data);
                if (state instanceof Enum<?>) icon = WeatherTextIcon.aospKind(((Enum<?>) state).name());
                if (state != null && access.stateId != null) weatherId = ((Number) access.stateId.invoke(state)).intValue();
            }
            return publish(description, temperature, weatherId, icon, source, expectedGeneration);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            return false;
        }
    }

    private static synchronized boolean publish(String description, String temperature, int source, long expectedGeneration) {
        return publish(description, temperature, Integer.MIN_VALUE, WeatherTextIcon.Kind.UNKNOWN, source, expectedGeneration);
    }

    private static synchronized boolean publish(String description, String temperature, int weatherId,
            WeatherTextIcon.Kind icon, int source, long expectedGeneration) {
        return publish(description,temperature,weatherId,icon,null,source,expectedGeneration);
    }
    private static synchronized boolean publish(String description,String temperature,int weatherId,
            WeatherTextIcon.Kind icon,WeatherTextIcon.NativeImage image,int source,long expectedGeneration) {
        if (expectedGeneration != generation || source < 4 && !unverifiedFallbackAllowed) return false;
        Snapshot previous = current;
        // A located plugin result is authoritative while it exists. AOSP can
        // still supply data on systems without that native pipeline.
        boolean takeover = source < previous.source && !description.isEmpty() && !previous.description.isEmpty() && now() - previous.confirmed > STALE_MILLIS;
        if (source < previous.source && !takeover) return false;
        if (description.equals(previous.description) && temperature.equals(previous.temperature)
                && weatherId == previous.weatherId && icon == previous.weatherIconKind && image==previous.weatherIcon) {
            if (source > previous.source) current = new Snapshot(description, temperature, weatherId, icon,image, previous.revision, source);
            else previous.confirmed = now();
            return false;
        }
        current = new Snapshot(description, temperature, weatherId, icon,image, previous.revision + 1L, source);
        return true;
    }

    /** Release on module teardown/user switch. No old city survives the reset. */
    static synchronized boolean clear() {
        generation++;
        Snapshot previous = current;
        boolean changed = !previous.description.isEmpty() || !previous.temperature.isEmpty();
        current = new Snapshot("", "", Integer.MIN_VALUE, WeatherTextIcon.Kind.UNKNOWN, previous.revision + (changed ? 1L : 0L), 0);
        CONTROLLERS.clear(); DATA_ACCESS.clear(); MANAGERS.clear(); MODELS.clear(); LAZY_VALUES.clear(); READ_IDENTITIES.clear();
        WIDGET_FIELDS.clear();WIDGET_READS.clear();INSTANT_DOCUMENTS.clear();INSTANT_TARGETS.clear();INSTANT_ICONS.clear();INSTANT_SEEN.clear();INSTANT_IMAGES.clear();lastWidgetOwner.clear();lastWidgetReason="widget_unobserved";
        currentController.clear();
        WeatherTextIcon.clearNativeCache();
        return changed;
    }

    private static String clean(Object value) {
        if (!(value instanceof CharSequence)) return "";
        String input = value.toString().trim();
        if (input.isEmpty() || "null".equalsIgnoreCase(input)) return "";
        StringBuilder result = new StringBuilder(Math.min(80, input.length()));
        for (int index = 0; index < input.length() && result.length() < 80; index++) {
            char next = input.charAt(index);
            if (!Character.isISOControl(next)) result.append(next);
        }
        return result.toString().trim();
    }

    private static synchronized ControllerAccess controllerAccess(Class<?> type) {
        ControllerAccess access = CONTROLLERS.get(type);
        if (access == null) {
            if (CONTROLLERS.size() >= 4) CONTROLLERS.clear();
            access = new ControllerAccess(type); CONTROLLERS.put(type, access);
        }
        return access;
    }

    private static synchronized DataAccess dataAccess(Class<?> type) {
        DataAccess access = DATA_ACCESS.get(type);
        if (access == null) {
            if (DATA_ACCESS.size() >= 4) DATA_ACCESS.clear();
            access = new DataAccess(type); DATA_ACCESS.put(type, access);
        }
        return access;
    }

    private static synchronized ManagerAccess managerAccess(Class<?> type) {
        ManagerAccess access=MANAGERS.get(type);
        if(access==null) {
            if(MANAGERS.size()>=4)MANAGERS.clear();
            access=new ManagerAccess(type);MANAGERS.put(type,access);
        }
        return access;
    }

    private static synchronized ModelAccess modelAccess(Class<?> type) {
        ModelAccess access=MODELS.get(type);
        if(access==null) {
            if(MODELS.size()>=4)MODELS.clear();
            access=new ModelAccess(type);MODELS.put(type,access);
        }
        return access;
    }

    private static synchronized Field lazyValue(Class<?> type)throws ReflectiveOperationException {
        Field value=LAZY_VALUES.get(type);
        if(value==null) {
            value=type.getDeclaredField("b");
            if(value.getType()!=Object.class||!java.lang.reflect.Modifier.isVolatile(value.getModifiers()))return null;
            value.setAccessible(true);
            if(LAZY_VALUES.size()>=4)LAZY_VALUES.clear();
            LAZY_VALUES.put(type,value);
        }
        return value;
    }

    private static final class ManagerAccess {
        Field controller;boolean valid;
        ManagerAccess(Class<?> type) {
            String name=type.getName();
            if(!COMMON_MANAGER.equals(name)&&!BASE_MANAGER.equals(name))return;
            try {
                controller=type.getDeclaredField(COMMON_MANAGER.equals(name)?"locatedWeatherController":"weatherEffectController");
                if(!CONTROLLER.equals(controller.getType().getName()))return;
                controller.setAccessible(true);valid=true;
            } catch(ReflectiveOperationException|RuntimeException unsupported) { /* Unknown ROM. */ }
        }
    }

    private static final class ModelAccess {
        Field delegate;String managerName;boolean valid;
        ModelAccess(Class<?> type) {
            String name=type.getName();
            if(!BASE_MODEL.equals(name)&&!GALLERY_MODEL.equals(name))return;
            try {
                delegate=type.getDeclaredField("extraInfoManager$delegate");
                if(!"j6.b".equals(delegate.getType().getName()))return;
                managerName=BASE_MODEL.equals(name)?BASE_MANAGER:COMMON_MANAGER;
                delegate.setAccessible(true);valid=true;
            } catch(ReflectiveOperationException|RuntimeException unsupported) { /* Unknown ROM. */ }
        }
    }

    private static final class ControllerAccess {
        Field data, located, destroyed;
        boolean valid;
        ControllerAccess(Class<?> type) {
            try {
                data = type.getDeclaredField("currentData");
                located = type.getDeclaredField("isLocated");
                destroyed = type.getDeclaredField("isDestroyed");
                if (!DATA.equals(data.getType().getName()) || located.getType() != Boolean.TYPE
                        || destroyed.getType() != Boolean.TYPE) return;
                data.setAccessible(true); located.setAccessible(true); destroyed.setAccessible(true);
                valid = true;
            } catch (ReflectiveOperationException | RuntimeException unsupported) { /* Unknown ROM. */ }
        }
    }

    private static final class DataAccess {
        Method description, temperature, celsius, weatherId, resId, state, stateId;
        boolean valid;
        DataAccess(Class<?> type) {
            boolean plugin = DATA.equals(type.getName());
            if (!plugin && !AOSP_DATA.equals(type.getName())) return;
            try {
                description = type.getMethod(plugin ? "getWeatherDescription" : "getDescription");
                temperature = type.getMethod(plugin ? "getDayTemperature" : "getTemperature");
                if (description.getReturnType() != String.class || temperature.getReturnType() != Integer.TYPE) return;
                if (plugin) {
                    weatherId = type.getMethod("getWeatherId"); resId = type.getMethod("getResId");
                    if (weatherId.getReturnType() != Integer.TYPE || resId.getReturnType() != Integer.TYPE) return;
                } else {
                    celsius = type.getMethod("getUseCelsius");
                    if (celsius.getReturnType() != Boolean.TYPE) return;
                    try {
                        Method candidate = type.getMethod("getState");
                        if (candidate.getReturnType().isEnum()) {
                            state = candidate; stateId = candidate.getReturnType().getMethod("getId");
                            if (stateId.getReturnType() != Integer.TYPE) stateId = null;
                        }
                    } catch (ReflectiveOperationException optionalState) { /* Unknown icon, real description retained. */ }
                }
                valid = true;
            } catch (ReflectiveOperationException | RuntimeException unsupported) { /* Unknown ROM. */ }
        }
    }
}
