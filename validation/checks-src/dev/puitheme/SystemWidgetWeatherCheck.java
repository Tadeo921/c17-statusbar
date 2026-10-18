package dev.puitheme;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import kswc.*;
import pantanal.app.bean.*;
/** Native shape seam: real yN.n assignment and IN.k pending-buffer lifetime. */
public final class SystemWidgetWeatherCheck {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static boolean invoke(String method,Object owner)throws Exception {
        try {Method m=SystemWeatherText.class.getDeclaredMethod(method,Object.class);m.setAccessible(true);return Boolean.TRUE.equals(m.invoke(null,owner));}
        catch(NoSuchMethodException old173){return false;}
    }
    private static byte[] payload(String page,String body){return ("{\"options\":{\"pageId\":\""+page+"\"},\"data\":"+body+"}").getBytes(StandardCharsets.UTF_8);}
    private static String body(String description,String value,String unit,String location){return "{\"weather_des\":\""+description+"\",\"template_temp\":\""+value+"\",\"template_unit\":\""+unit+"\",\"is_location\":\""+location+"\"}";}
    private static a.a.a.v110017.pi textNode(String token,String value){a.a.a.v110017.pi component=new a.a.a.v110017.pi(){};component.e=new a.a.a.v110017.d80();component.e.g=new String[]{token};component.D.put("value",value);return component;}
    private static a.a.a.v110017.pi imageNode(String source,String parentToken){a.a.a.v110017.pi component=textNode("","");component.D.clear();component.D.put("src",source);component.e.b=new a.a.a.v110017.d80();component.e.b.g=new String[]{parentToken};return component;}
    private static a.a.a.v110017.Dc renderer(){
        a.a.a.v110017.Dc result=new a.a.a.v110017.Dc();result.b=new org.hapjs.card.support.d();result.b.g="com.weathernow.desk";
        result.b.l=new a.a.a.v110017.B10();a.a.a.v110017.v10 page=new a.a.a.v110017.v10(new org.hapjs.model.CardInfo("/cards/card2x1"));result.b.l.b.add(page);
        result.b.c=new a.a.a.v110017.bp0();result.b.c.h=new a.a.a.v110017.Gr();result.b.c.h.E0=page;
        result.b.c.g.put(1,new a.a.a.v110017.kp0("text",textNode("tmp","16°")));
        result.b.c.g.put(2,new a.a.a.v110017.kp0("text",textNode("cond","小雨")));return result;
    }
    public static void main(String[] args)throws Exception {
        SystemWeatherText.clear();
        try {
            CardViewInfo online=new CardViewInfo("536871060",Entrance.LOCK_SCREEN,false);
            yN card=new yN(online,null);byte[] current=payload("pages/weather_info",body("小雨","16","°","1"));
            com.oplus.keyguard.style.widgets.container.view.InstantCardView view=new com.oplus.keyguard.style.widgets.container.view.InstantCardView();
            SystemWeatherText.WidgetDiagnostic observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.reason.equals("widget_binding_empty")&&observed.viewShape&&!observed.bindingPresent,"unbound actual widget view has an explicit diagnostic state");
            com.oplus.keyguard.style.widgets.container.view.InstantCardView.access$setInstantCardWidget$p(view,new com.oplus.keyguard.style.widgets.container.data.InstantCardWidget(card));
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.bindingPresent&&observed.cardPresent&&observed.cardShape&&observed.infoPresent&&observed.serviceMatch&&observed.lockEntrance&&!observed.cachePresent,"already bound card chain is visible independently of native callback history");
            check(observed.reason.equals("widget_no_bound_payload")&&!observed.payloadPresent,"bound but empty native cache does not claim SDK data success");
            yN.a(card,new PantanalUIData(current));
            SystemWeatherText.Snapshot unmodified=SystemWeatherText.snapshot();observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.cachePresent&&observed.payloadPresent&&observed.jsonReady&&observed.pageCurrent&&observed.dataFields&&!observed.rootFields,"payload existence and proven JSON path are diagnosed without printing keys/values");
            check(observed.reason.equals("widget_data_schema")&&SystemWeatherText.snapshot()==unmodified,"diagnostic probe cannot publish or change snapshot revision");
            check(invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot().text.equals("小雨 16°"),"actual widget yN.n cache feeds the weather text instead of unrelated empty clock ExtraInfoManager");
            SystemWeatherText.Snapshot first=SystemWeatherText.snapshot();
            check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"same widget data retains revision and snapshot identity");
            CardViewInfo local=new CardViewInfo("53687106002",Entrance.LOCK_SCREEN,true);
            IN engine=new IN(local,payload("pages/weather_info",body("阴","17","℃","1")));
            yN localCard=new yN(local,null);localCard.i=engine;
            com.oplus.keyguard.style.widgets.container.view.InstantCardView.access$setInstantCardWidget$p(view,new com.oplus.keyguard.style.widgets.container.data.InstantCardWidget(localCard));
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.localCard&&observed.enginePresent&&observed.pendingPresent&&observed.renderReady&&!observed.cachePresent,"native local pending buffer and persistent card cache are distinct diagnostic facts");
            check(invoke("captureWidgetEngine",engine)&&SystemWeatherText.snapshot().text.equals("阴 17℃"),"local native fallback uses already pending IN.h before updateData and preserves native unit");
            engine.k();first=SystemWeatherText.snapshot();
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(!observed.pendingPresent&&!observed.payloadPresent&&observed.reason.equals("widget_no_bound_payload"),"after native k clears h the probe reports no bound payload rather than replaying history");
            check(engine.h==null&&!invoke("captureWidgetEngine",engine)&&SystemWeatherText.snapshot()==first,"real native k clears h; after-hook cannot invent a persistent engine cache or erase latest weather");
            String rootWeather="{\"options\":{\"pageId\":\"pages/weather_info\"},"+body("晴","18","°F","1").substring(1);
            card.n=new PantanalUIData(rootWeather.getBytes(StandardCharsets.UTF_8));
            check(invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot().text.equals("晴 18°F"),"only the explicit root weather schema is the supported alternative to root.data");
            first=SystemWeatherText.snapshot();
            card.n=new PantanalUIData((rootWeather.substring(0,rootWeather.length()-1)+",\"data\":"+body("目的地","99","°","1")+"}").getBytes(StandardCharsets.UTF_8));
            check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"two weather schema candidates are ambiguous and rejected");
            card.n=new PantanalUIData(payload("pages/weather_info","{\"nested\":"+body("目的地","99","°","1")+"}"));
            check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"parser never searches arbitrary nested objects for weather keys");
            card.n=new PantanalUIData(payload("pages/weather_info",body("明日","99","°","1").replace("}",",\"is_tomorrow\":true}")));
            check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"future weather flag cannot overwrite current weather even on current route");
            for(String page:new String[]{"pages/destination_weather","pages/index","pages/warning","weather_info",""}) {
                card.n=new PantanalUIData(payload(page,body("错误城市","99","°","1")));
                check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"only proven current-weather route is accepted");
            }
            for(String[] sample:new String[][]{{"--","16","°","1"},{"小雨","--","°","1"},{"小雨","16","°","0"},{"小雨","999999","°","1"},{"小雨","16","bad","1"}}) {
                card.n=new PantanalUIData(payload("pages/weather_info",body(sample[0],sample[1],sample[2],sample[3])));
                check(!invoke("captureWidgetCard",card)&&SystemWeatherText.snapshot()==first,"template/default/nonlocated/invalid values do not become native weather");
            }
            for(CardViewInfo wrong:new CardViewInfo[]{new CardViewInfo("536871060",Entrance.LAUNCHER,false),new CardViewInfo("53687106002",Entrance.LOCK_SCREEN,false),new CardViewInfo("different",Entrance.LOCK_SCREEN,true)})
                check(!invoke("captureWidgetCard",new yN(wrong,new PantanalUIData(current))),"card scope must match verified service, lock entrance and local flag");
            engine=new IN(local,current);engine.q=null;
            check(!invoke("captureWidgetEngine",engine),"uninitialized native engine has not consumed weather");
            card.n=new PantanalUIData("not-json".getBytes(StandardCharsets.UTF_8));check(!invoke("captureWidgetCard",card),"unknown native payload remains readable fallback");
            com.oplus.keyguard.clock.common.util.WeatherEffectController emptyClock=new com.oplus.keyguard.clock.common.util.WeatherEffectController(true,null);
            check(!SystemWeatherText.captureController(emptyClock)&&SystemWeatherText.snapshot()==first,"unrelated empty native clock pipeline cannot clear genuine current widget weather");
            check(!invoke("captureWidgetEngine",new IN(new CardViewInfo("53687106002",Entrance.LOCK_SCREEN,false),current)),"long fallback service requires actual native local-card flag for engine too");
            check(!invoke("captureWidgetCard",new Object())&&!invoke("captureWidgetEngine",null),"unrelated objects are untouched");
            gt actualBackend=new gt(online,new at(new org.hapjs.card.sdk.CardDelegator(new a.a.a.v110017.Dc(),new org.hapjs.card.impl.CardServiceWorker())));
            com.oplus.keyguard.style.widgets.container.view.InstantCardView.access$setInstantCardWidget$p(view,new com.oplus.keyguard.style.widgets.container.data.InstantCardWidget(actualBackend));
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.cardShape&&observed.infoPresent&&observed.serviceMatch&&observed.lockEntrance&&observed.enginePresent,
                    "actual device gt backend must expose its own b metadata and at engine instead of stopping at widget_card_type");
            check(observed.weatherPackage&&observed.engineClass.equals("org.hapjs.card.sdk.CardDelegator")
                    &&observed.innerClass.equals("a.a.a.v110017.Dc")&&observed.serviceClass.equals("org.hapjs.card.impl.CardServiceWorker"),
                    "the native dynamic card implementation is observed through its existing fields without getView or getUIData");
            check(!observed.cachePresent&&!observed.jsonReady&&observed.reason.equals("instant_frontend_empty"),"an existing InstantCard delegate with no frontend is distinct from Seedling n cache absence");
            first=SystemWeatherText.snapshot();
            online.cachedStrings(null,null,"hap://app/com.coloros.weather2/pages/weather?city=PRIVATE_CITY&temp=99");
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.instantUriPresent&&!observed.cardDataPresent&&!observed.initDataPresent&&observed.cachePresent&&!observed.jsonReady,
                    "existing URI is only a structural fact and is not interpreted as current weather");
            check(observed.reason.equals("instant_frontend_empty")&&SystemWeatherText.snapshot()==first,"probe cannot promote a URI or class match into actual weather");
            online.cachedStrings(new String(current,StandardCharsets.UTF_8),"PRIVATE_INITIAL",null);
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.cardDataPresent&&observed.initDataPresent&&!observed.instantUriPresent&&observed.jsonReady&&observed.dataFields
                    &&observed.payloadSource.equals("card_ui_data"),"fixed schema predicates expose cache shape without treating unknown backend input as proven weather");
            check(SystemWeatherText.snapshot()==first,"even recognized JSON in a diagnostic probe cannot publish");
            online.cachedStrings(null,null,null);actualBackend.i=null;
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(!observed.enginePresent&&observed.engineClass.isEmpty()&&!observed.cachePresent,"probe reads current cleared native fields rather than historical engine/cache");
            a.a.a.v110017.Dc nativeRenderer=renderer();actualBackend.i=new at(new org.hapjs.card.sdk.CardDelegator(nativeRenderer,new org.hapjs.card.impl.CardServiceWorker()));
            SystemWeatherText.clear();long coldRevision=SystemWeatherText.revision();a.a.a.v110017.bp0 readyDocument=nativeRenderer.b.c;nativeRenderer.b.c=null;
            readyDocument.g.put(3,new a.a.a.v110017.kp0("image",imageNode("/cards/card2x1/images/light_rain.svg","icon")));
            check(!invoke("captureWidgetView",view)&&SystemWeatherText.revision()==coldRevision&&SystemWeatherText.snapshot().description.isEmpty(),"native bound event may precede asynchronous frontend document readiness");
            nativeRenderer.b.c=readyDocument;
            check(SystemWeatherText.diagnoseWidgetView(view).reason.equals("instant_ready")&&SystemWeatherText.revision()==coldRevision,
                    "176 device seam: readonly ready diagnostics alone leave snapshot0 after the early empty bound event");
            java.lang.reflect.Field tracked=StatusBarModule.class.getDeclaredField("WEATHER_WIDGET_VIEWS");tracked.setAccessible(true);
            @SuppressWarnings("unchecked") java.util.Map<android.view.View,Boolean> knownViews=(java.util.Map<android.view.View,Boolean>)tracked.get(null);
            java.util.Map<android.view.View,Boolean> knownBefore=new java.util.WeakHashMap<>(knownViews);knownViews.clear();knownViews.put(view,true);
            Method probe=StatusBarModule.class.getDeclaredMethod("weatherWidgetProbe",String.class);probe.setAccessible(true);
            try{
                check(!ModuleDiagnostics.enabled(),"configuration recovery has no dependency on opt-in logging");
                probe.invoke(null,"configure");check(SystemWeatherText.snapshot().text.equals("小雨 16°")&&SystemWeatherText.revision()==coldRevision+1,
                        "actual module configure reconciliation must publish an already ready native cache, even with diagnostics disabled");
                check(SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.LIGHT_RAIN&&SystemWeatherText.observedInstantDocument(readyDocument),
                        "late initial recovery publishes the matching native icon and registers future attribute delivery");
                first=SystemWeatherText.snapshot();probe.invoke(null,"probe");check(SystemWeatherText.snapshot()==first,"repeated verified probes do not republish unchanged weather");
                SystemWeatherText.clear();coldRevision=SystemWeatherText.revision();
                Method renderReady=StatusBarModule.class.getDeclaredMethod("queueWeatherWidgetRefresh");renderReady.setAccessible(true);
                java.lang.reflect.Field mainField=StatusBarModule.class.getDeclaredField("MAIN");mainField.setAccessible(true);android.os.Handler main=(android.os.Handler)mainField.get(null);
                int queuedBefore=android.os.Handler.postedForCheck.size(),timersBefore=main.scheduled;Runnable timerBefore=main.delayed;
                renderReady.invoke(null);renderReady.invoke(null);
                check(android.os.Handler.postedForCheck.size()==queuedBefore+1,"two native render successes coalesce into one normal UI delivery");
                check(SystemWeatherText.revision()==coldRevision,"native render callback does not read view/frontend state before the UI event runs");
                android.os.Handler.drainForCheck();
                check(SystemWeatherText.snapshot().text.equals("小雨 16°")&&SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.LIGHT_RAIN
                        &&SystemWeatherText.revision()==coldRevision+1,"actual native-ready event reconciles the late-filled text and icon without a manual probe");
                check(main.scheduled==timersBefore&&main.delayed==timerBefore&&android.os.Handler.postedForCheck.isEmpty(),"recovery adds no polling/delayed timer or self-refresh loop");
            } finally{knownViews.clear();knownViews.putAll(knownBefore);readyDocument.g.remove(3);}
            SystemWeatherText.clear();
            check(invoke("captureWidgetView",view)&&SystemWeatherText.snapshot().text.equals("小雨 16°"),
                    "actual gt/Dc loaded current WeatherNow page must supply native tmp/cond attribute caches");
            first=SystemWeatherText.snapshot();
            check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"unchanged native attributes preserve revision and snapshot identity");
            observed=SystemWeatherText.diagnoseWidgetView(view);
            check(observed.rendererKnown&&observed.weatherPackage&&!observed.preview&&observed.pageKnown&&observed.documentKnown
                    &&observed.temperatureNode&&observed.descriptionNode&&observed.nodeCount==2&&observed.reason.equals("instant_ready"),"real document scope and cache readiness have bounded structural diagnostics");
            check(SystemWeatherText.snapshot()==first,"ready document diagnostic remains read only");
            a.a.a.v110017.bp0 document=nativeRenderer.b.c;
            a.a.a.v110017.jp0 update=new a.a.a.v110017.jp0();update.d=1;update.j.put("value","17°");
            check(SystemWeatherText.relevantInstantUpdate(document,update),"native updateAttrs target and value key select observed temperature slot");
            update.d=999;check(SystemWeatherText.relevantInstantUpdate(document,update),"a never-seen node (a re-rendered page) is read once rather than ignored");
            update.d=1;update.j.clear();update.j.put("src","native-animation.png");check(!SystemWeatherText.relevantInstantUpdate(document,update),"native image/style changes never rescan text attributes");
            update.j.clear();update.j.put("value","17°");
            check(!SystemWeatherText.relevantInstantUpdate(new a.a.a.v110017.bp0(),update),"unrelated SDK documents are rejected before field reads");
            ((a.a.a.v110017.pi)document.g.get(1).e).D.put("value","17°");
            check(SystemWeatherText.captureInstantDocument(document)&&SystemWeatherText.snapshot().text.equals("小雨 17°"),"actual stored native update is captured through its registered document");
            first=SystemWeatherText.snapshot();check(!SystemWeatherText.captureInstantDocument(document)&&SystemWeatherText.snapshot()==first,"repeated native callback is a no-op");
            nativeRenderer.b.A=true;check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"preview26/sunny values cannot replace real weather");nativeRenderer.b.A=false;
            nativeRenderer.b.g="com.other.weather";check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"lookalike class selectors from other packages cannot become weather");nativeRenderer.b.g="com.weathernow.desk";
            a.a.a.v110017.v10 currentPage=document.h.E0;
            nativeRenderer.b.l.b.add(new a.a.a.v110017.v10(new org.hapjs.model.CardInfo("/cards/card2x1")));
            check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"stale document cannot publish against a replacement current page");nativeRenderer.b.l.b.remove(1);
            document.h.E0=null;check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"unbound document cannot publish");document.h.E0=currentPage;
            nativeRenderer.b.l.b.set(0,new a.a.a.v110017.v10(new org.hapjs.model.CardInfo("/cards/tomorrow")));
            document.h.E0=nativeRenderer.b.l.b.get(0);check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"forecast/destination routes cannot become current weather");
            nativeRenderer.b.l.b.set(0,currentPage);document.h.E0=currentPage;
            ((a.a.a.v110017.pi)document.g.get(1).e).D.put("value","--°");check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"native unavailable temperature is not fabricated zero");
            ((a.a.a.v110017.pi)document.g.get(1).e).D.put("value","17°");
            document.g.put(3,new a.a.a.v110017.kp0("text",textNode("tmp","99°")));check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"ambiguous native temperature nodes are rejected");document.g.remove(3);
            document.g.put(3,new a.a.a.v110017.kp0("text",textNode("cond","错误天气")));check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"ambiguous native condition nodes are rejected");document.g.remove(3);
            document.g.put(3,new a.a.a.v110017.kp0("text",textNode("tmp","°17")));check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"equivalent RTL temperature binding is deduplicated");document.g.remove(3);
            document.g.put(2,new a.a.a.v110017.kp0("text",new a.a.a.v110017.ni(textNode("cond","阴"))));
            check(invoke("captureWidgetView",view)&&SystemWeatherText.snapshot().text.equals("阴 17°"),"deferred native ni component unwraps its already instantiated pi without creating a component");first=SystemWeatherText.snapshot();
            for(int extra=3;extra<131;extra++)document.g.put(extra,new a.a.a.v110017.kp0("div",null));
            check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"oversized native document is bounded and preserves last readable weather");
            for(int extra=3;extra<131;extra++)document.g.remove(extra);
            document.g.put(3,new a.a.a.v110017.kp0("image",imageNode("/cards/card2x1/images/sunny.svg","icon")));
            check(invoke("captureWidgetView",view)&&SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.SUNNY,
                    "native WeatherNow image src under the exact icon parent must select the matching existing monochrome icon");
            String[] iconCases={"sunny.svg:SUNNY","night_sunny.svg:CLEAR_NIGHT","cloudy.svg:PARTLY_CLOUDY","cloudy_nights.svg:CLOUDY_NIGHT","negative.svg:CLOUDY",
                    "light_rain.svg:LIGHT_RAIN","mid_rain.svg:MODERATE_RAIN","heavy_rain.svg:HEAVY_RAIN","freezing_rain.svg:ICE_RAIN","hail.svg:HAILSTONE",
                    "thunder.svg:THUNDERSHOWERS","mixed_rain_and_hail.svg:THUNDERSHOWERS_HAILSTONE","thunderstorms.svg:THUNDERSTORM",
                    "flurries.svg:LIGHT_SNOW","mid_snow.svg:MODERATE_SNOW","heavy_snow.svg:HEAVY_SNOW","rain_snow.svg:SLEET","fog.svg:FOG",
                    "float_dust.svg:SINK_FLOAT","sand_dust.svg:DUST_DEVIL","haze.svg:HAZE","wind.svg:WIND","storm.svg:STORM","tornado.svg:TORNADO","sunrise.svg:SUNRISE","sunset.svg:SUNSET"};
            check(iconCases.length==26,"all26 installed native icon basename meanings remain explicit");
            for(String sample:iconCases){String[] parts=sample.split(":");WeatherTextIcon.Kind expected=WeatherTextIcon.Kind.valueOf(parts[1]);
                first=SystemWeatherText.snapshot();((a.a.a.v110017.pi)document.g.get(3).e).D.put("src","/cards/card2x1/images/"+parts[0]);invoke("captureWidgetView",view);
                check(SystemWeatherText.snapshot().weatherIconKind==expected&&SystemWeatherText.snapshot().text.equals("阴 17°"),"native resolved icon maps without classifying the unchanged condition text: "+parts[0]);
                check(SystemWeatherText.revision()==first.revision+(first.weatherIconKind==expected?0:1),"icon-only changes are revision-visible and exact duplicates remain no-op");}
            update.d=3;update.j.clear();update.j.put("src","/cards/card2x1/images/night_sunny.svg");
            check(SystemWeatherText.relevantInstantUpdate(document,update),"known weather image ref and src key participate in native update delivery");
            update.d=1;check(!SystemWeatherText.relevantInstantUpdate(document,update),"src changes to a temperature node do not schedule scans");
            update.d=999;check(!SystemWeatherText.relevantInstantUpdate(document,update),"background/unrelated image refs remain rejected");
            for(String invalid:new String[]{"sunny.svg","/cards/card1x1/images/sunny.svg","https://example.invalid/cards/card2x1/images/sunny.svg",
                    "/cards/card2x1/images/launcher_sunny.svg","/cards/card2x1/images/../sunny.svg","/cards/card2x1/images/sunny.svg?city=PRIVATE_CITY","/cards/card2x1/images/new_unknown.svg"}){
                ((a.a.a.v110017.pi)document.g.get(3).e).D.put("src",invalid);invoke("captureWidgetView",view);
                check(SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.UNKNOWN&&SystemWeatherText.snapshot().text.equals("阴 17°"),"unknown/out-of-route/native-launcher image cannot fake a weather kind and never drops readable text");}
            ((a.a.a.v110017.pi)document.g.get(3).e).D.put("src","/cards/card2x1/images/light_rain.svg");first=SystemWeatherText.snapshot();
            check(SystemWeatherText.captureInstantDocument(document)&&SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.LIGHT_RAIN
                    &&SystemWeatherText.revision()==first.revision+1,"native src-only update reaches snapshot even if temperature and condition text are unchanged");
            a.a.a.v110017.pi placeholder=imageNode("/cards/card2x1/images/sunny.svg","right");placeholder.e.g=new String[]{"icon"};
            document.g.put(4,new a.a.a.v110017.kp0("image",placeholder));first=SystemWeatherText.snapshot();
            check(!invoke("captureWidgetView",view)&&SystemWeatherText.snapshot()==first,"error placeholder sunny with class on image is excluded from the real parent.icon binding");
            update.d=4;check(!SystemWeatherText.relevantInstantUpdate(document,update),"placeholder ref never enters the weather icon observer set");
            document.g.put(4,new a.a.a.v110017.kp0("image",imageNode("/cards/card2x1/images/heavy_rain.svg","icon")));
            check(invoke("captureWidgetView",view)&&SystemWeatherText.snapshot().weatherIconKind==WeatherTextIcon.Kind.UNKNOWN
                    &&SystemWeatherText.snapshot().text.equals("阴 17°"),"ambiguous genuine icon bindings fall back explicitly while retaining weather text");
            document.g.remove(4);document.g.remove(3);invoke("captureWidgetView",view);first=SystemWeatherText.snapshot();
            check(first.weatherIconKind==WeatherTextIcon.Kind.UNKNOWN&&first.text.equals("阴 17°"),"a missing native icon does not block a complete text snapshot");
            update.d=3;check(SystemWeatherText.relevantInstantUpdate(document,update),"a removed icon ref is unseen, so a later update is re-read once instead of trusting a stale filter");
            update.d=2;update.j.clear();update.j.put("src","native-animation.png");check(!SystemWeatherText.relevantInstantUpdate(document,update),"src on a known text node never rescans even when no icon is bound");
            update.j.clear();update.j.put("value","17°");
            actualBackend=new gt(new CardViewInfo("536871060",Entrance.LAUNCHER,false),actualBackend.i);
            check(!invoke("captureWidgetCard",actualBackend)&&SystemWeatherText.snapshot()==first,"launcher weather cannot populate the lock-screen source");
            SystemWeatherText.clear();check(!SystemWeatherText.observedInstantDocument(document)&&!SystemWeatherText.captureInstantDocument(document),"clear drops observed document generation and pending native callbacks cannot resurrect stale weather");
            org.json.JSONObject report=new org.json.JSONObject().put("kind","fact").put("topic","weather_widget").put("values",new org.json.JSONObject()
                    .put("card_class","kswc.yN").put("widget_cache",true).put("widget_data_fields",true).put("widget_payload_bytes",current.length)
                    .put("engine_class","org.hapjs.card.sdk.CardDelegator").put("inner_class","a.a.a.v110017.Dc").put("service_class","org.hapjs.card.impl.CardServiceWorker")
                    .put("widget_payload_source","card_ui_data").put("widget_weather_package",true).put("widget_instant_uri",true)
                    .put("widget_renderer",true).put("widget_document",true).put("widget_node_count",2).put("widget_temperature_node",true).put("widget_description_node",true)
                    .put("weather_des","PRIVATE_WEATHER").put("city_name","PRIVATE_CITY"));
            org.json.JSONObject safe=DiagnosticReport.validateDetails(report).getJSONObject("values");
            check(safe.getString("card_class").equals("kswc.yN")&&safe.getBoolean("widget_cache")&&safe.getBoolean("widget_data_fields"),"widget diagnostic report preserves bounded native structure");
            check(!safe.has("weather_des")&&!safe.has("city_name")&&!safe.toString().contains("PRIVATE_"),"no payload text, city or arbitrary JSON keys enter widget diagnostics");
            check(safe.getString("engine_class").equals("org.hapjs.card.sdk.CardDelegator")&&safe.getString("inner_class").equals("a.a.a.v110017.Dc")
                    &&safe.getString("service_class").equals("org.hapjs.card.impl.CardServiceWorker")&&safe.getBoolean("widget_instant_uri"),"verified native backend class structure survives report validation");
            report.getJSONObject("values").put("card_class","kswc.Bad\nPRIVATE_CITY");
            check(!DiagnosticReport.validateDetails(report).getJSONObject("values").has("card_class"),"card-class diagnostic rejects escaped payload strings");
        } finally {SystemWeatherText.clear();}
        System.out.println("SystemWidgetWeatherCheck: "+checks+" checks passed (actual captured ROM shapes; GPU/device delivery pending)");
    }
}
