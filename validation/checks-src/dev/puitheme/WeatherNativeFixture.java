package dev.puitheme;
/** Replays the captured ROM's live gt/document/image seam for row tests. */
final class WeatherNativeFixture {
    final a.a.a.v110017.Dc renderer=new a.a.a.v110017.Dc();
    final a.a.a.v110017.pi description=node("cond",""),temperature=node("tmp",""),icon=node("","");
    final a.a.a.v110017.ey view=new a.a.a.v110017.ey();
    final kswc.gt card;
    private Object lastData;private String source;
    WeatherNativeFixture(){
        renderer.b=new org.hapjs.card.support.d();renderer.b.g="com.weathernow.desk";renderer.b.l=new a.a.a.v110017.B10();
        a.a.a.v110017.v10 page=new a.a.a.v110017.v10(new org.hapjs.model.CardInfo("/cards/card2x1"));renderer.b.l.b.add(page);
        renderer.b.c=new a.a.a.v110017.bp0();renderer.b.c.h=new a.a.a.v110017.Gr();renderer.b.c.h.E0=page;
        renderer.b.c.g.put(1,new a.a.a.v110017.kp0("text",temperature));renderer.b.c.g.put(2,new a.a.a.v110017.kp0("text",description));
        icon.D.clear();icon.e.b=new a.a.a.v110017.d80();icon.e.b.g=new String[]{"icon"};icon.m=view;
        renderer.b.c.g.put(3,new a.a.a.v110017.kp0("image",icon));
        card=new kswc.gt(new pantanal.app.bean.CardViewInfo("536871060",pantanal.app.bean.Entrance.LOCK_SCREEN,false),
            new kswc.at(new org.hapjs.card.sdk.CardDelegator(renderer,new org.hapjs.card.impl.CardServiceWorker())));
    }
    static a.a.a.v110017.pi node(String name,String value){a.a.a.v110017.pi node=new a.a.a.v110017.pi(){};node.e=new a.a.a.v110017.d80();node.e.g=new String[]{name};node.D.put("value",value);return node;}
    static android.graphics.drawable.PictureDrawable image(){android.graphics.Picture recording=new android.graphics.Picture();recording.beginRecording(40,40);recording.endRecording();return new android.graphics.drawable.PictureDrawable(recording);}
    boolean update(String description,String temperature,String basename,boolean deliver){
        this.description.D.put("value",description);this.temperature.D.put("value",temperature);
        String next="/cards/card2x1/images/"+basename;icon.D.put("src",next);
        if(deliver&&!next.equals(source)){view.u(image());SystemWeatherText.instantImageDocument(view);source=next;}
        return SystemWeatherText.captureWidgetCard(card);
    }
    private static final java.util.Map<Object,WeatherNativeFixture> CONTROLLERS=new java.util.WeakHashMap<>();
    static boolean capture(com.oplus.keyguard.clock.common.util.WeatherEffectController controller){
        com.oplus.keyguard.clock.common.entry.WeatherData data=controller.getCurrentData();
        WeatherNativeFixture fixture=CONTROLLERS.get(controller);if(fixture==null){fixture=new WeatherNativeFixture();CONTROLLERS.put(controller,fixture);}
        if(data==null)return false;
        WeatherTextIcon.Kind kind=WeatherTextIcon.nativeKind(data.getClass().getClassLoader(),data.getResId());
        String file=kind==WeatherTextIcon.Kind.SUNNY?"sunny.svg":kind==WeatherTextIcon.Kind.CLEAR_NIGHT?"night_sunny.svg":kind==WeatherTextIcon.Kind.LIGHT_RAIN?"light_rain.svg":"unknown.svg";
        fixture.lastData=data;return fixture.update(data.getWeatherDescription(),data.getDayTemperature()+"°",file,true);
    }
}
