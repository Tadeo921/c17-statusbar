package dev.puitheme;

import com.oplus.keyguard.clock.common.entry.WeatherData;
import com.oplus.keyguard.clock.common.util.WeatherEffectController;
import org.json.JSONObject;

/** JVM native-shape observations, not an Android hook delivery claim. */
public final class SystemWeatherDiagnosticsCheck {
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static WeatherData data(){return new WeatherData(54,"PRIVATE_WEATHER_TEXT",16,13,com.oplus.keyguard.clock.common.R.drawable.ic_weather_sunny,123L);}
    public static void main(String[] args)throws Exception {
        SystemWeatherText.clear();
        try {
            SystemWeatherText.Snapshot empty=SystemWeatherText.snapshot();
            SystemWeatherText.DiagnosticState state=SystemWeatherText.diagnose(null);
            check(state.reason.equals("no_controller")&&!state.snapshotReady,"missing observed controller is explicit");
            WeatherEffectController located=new WeatherEffectController(true,data());
            state=SystemWeatherText.diagnose(located);
            check(state.reason.equals("native_ready")&&state.controllerKnown&&state.controllerShape&&state.located&&state.alive,"native cache shape and located lifecycle are distinguished");
            check(state.dataPresent&&state.dataShape&&state.descriptionPresent&&state.temperaturePresent&&!state.snapshotReady,"real cache with empty module snapshot can be diagnosed");
            check(SystemWeatherText.snapshot()==empty&&located.getterReads==0,"diagnosis neither captures/publishes nor calls native cache getter");
            com.oplus.keyguard.clock.common.util.ExtraInfoManager common=new com.oplus.keyguard.clock.common.util.ExtraInfoManager(located,new WeatherEffectController(false,data()));
            check(SystemWeatherText.diagnose(common).reason.equals("native_ready"),"common manager reads the exact existing located field");
            com.oplus.keyguard.clock.base.util.ExtraInfoManager base=new com.oplus.keyguard.clock.base.util.ExtraInfoManager(located);
            check(SystemWeatherText.diagnose(base).reason.equals("native_ready"),"base manager reads its exact cached field");
            j6.f lazy=new j6.f(()->{throw new AssertionError("diagnostic must never initialize native weather");});
            com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel model=new com.oplus.keyguard.clock.base.domain.model.DateMessageViewModel(lazy);
            check(SystemWeatherText.diagnose(model).reason.equals("lazy_uninitialised")&&lazy.getterCalls==0&&lazy.initializations==0,"native Lazy is only inspected, never invoked");
            lazy.b=base;
            check(SystemWeatherText.diagnose(model).reason.equals("native_ready")&&lazy.getterCalls==0,"already initialized native model is inspected without its lazy getter");
            located.setDestroyed(true);check(SystemWeatherText.diagnose(located).reason.equals("destroyed_controller"),"destroyed native cache is explicit");
            located.setDestroyed(false);located.setData(null);check(SystemWeatherText.diagnose(located).reason.equals("native_data_empty"),"empty native cache is distinct from missing owner");
            located.setData(new WeatherData(1,"--",Integer.MIN_VALUE,0,0,0L));
            state=SystemWeatherText.diagnose(located);check(state.reason.equals("native_description_empty")&&!state.descriptionPresent&&!state.temperaturePresent,"native numeric sentinels are not genuine data");
            check(SystemWeatherText.diagnose(new WeatherEffectController(false,data())).reason.equals("resident_controller"),"resident weather remains excluded");
            check(SystemWeatherText.diagnose(new Object()).reason.equals("unsupported_owner"),"unknown owners remain explicit");
            located.setData(data());check(SystemWeatherText.captureController(located),"ordinary production capture still works");
            SystemWeatherText.Snapshot delivered=SystemWeatherText.snapshot();
            for(int i=0;i<100;i++) {
                state=SystemWeatherText.diagnose(located);
                check(state.snapshotReady&&state.source==2&&state.revision==delivered.revision&&SystemWeatherText.snapshot()==delivered,"read-only diagnostics cannot change source, revision or snapshot identity");
            }
            JSONObject raw=new JSONObject().put("kind","fact").put("topic","weather").put("values",new JSONObject()
                    .put("owner_class",SystemWeatherText.CONTROLLER).put("reason","native_ready").put("data_present",true)
                    .put("snapshot_ready",false).put("native_icon_ready",true).put("revision",0).put("weather_text","PRIVATE_WEATHER_TEXT").put("city_id",123));
            JSONObject values=DiagnosticReport.validateDetails(raw).getJSONObject("values");
            check(values.getString("owner_class").equals(SystemWeatherText.CONTROLLER),"actual bounded native class names survive the report channel");
            check(values.getBoolean("data_present")&&!values.getBoolean("snapshot_ready"),"native/cache mismatch remains structured");
            check(values.getBoolean("native_icon_ready"),"native icon readiness is a fixed boolean fact without resource paths or pixels");
            check(!values.has("weather_text")&&!values.has("city_id")&&!values.toString().contains("PRIVATE_WEATHER_TEXT"),"weather and city content cannot enter diagnostics");
            raw.getJSONObject("values").put("owner_class","com.oplus.keyguard.clock.BAD\nPRIVATE_WEATHER_TEXT");
            check(!DiagnosticReport.validateDetails(raw).getJSONObject("values").has("owner_class"),"class-name escape text is rejected");
            raw.getJSONObject("values").put("owner_class","java.lang.String");
            check(!DiagnosticReport.validateDetails(raw).getJSONObject("values").has("owner_class"),"arbitrary runtime class names are not reported");
            check(DiagnosticReport.validateDetails(new JSONObject().put("kind","fact").put("topic","weather_slots").put("values",new JSONObject().put("left_placeholder",true))).getJSONObject("values").getBoolean("left_placeholder"),"slot placeholder observation is structured without text");
        } finally {SystemWeatherText.clear();}
        System.out.println("SystemWeatherDiagnosticsCheck: "+checks+" checks passed (JVM native shapes; device hook delivery pending)");
    }
}
