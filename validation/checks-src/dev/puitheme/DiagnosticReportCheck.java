package dev.puitheme;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

/** Export-only schema and privacy boundaries, independent of a device or private preferences. */
public final class DiagnosticReportCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("expected "+expected+" got "+actual);}
    private static final class Device extends Context {
        final Resources resources=new Resources();
        @Override public Resources getResources(){return resources;}
    }
    public static int run() throws Exception {
        Device device=new Device();device.resources.getConfiguration().orientation=Configuration.ORIENTATION_LANDSCAPE;
        device.resources.getConfiguration().fontScale=1.25f;device.resources.getDisplayMetrics().scaledDensity=5f;
        JSONObject display=DiagnosticReport.display(device);
        equal("landscape",display.getString("orientation"));equal(1440,display.getInt("width_px"));
        equal(3168,display.getInt("height_px"));equal(640,display.getInt("density_dpi"));
        equal(1.25,display.getDouble("font_scale"));equal(5.0,display.getDouble("scaled_density"));
        JSONObject hardware=DiagnosticReport.device(device);
        equal("PJZ110",hardware.getString("model"));equal("OnePlus",hardware.getString("manufacturer"));
        equal("16",hardware.getString("android"));equal(false,hardware.has("serial"));equal(false,hardware.has("unavailable"));
        equal("OnePlus/PJZ110/OP5D0DL1:16/TEST.1/test:user/release-keys",hardware.getString("fingerprint"));
        equal("OP5D0DL1",hardware.getString("device"));equal("PJZ110",hardware.getString("product"));equal("qcom",hardware.getString("hardware"));
        equal("test.1",hardware.getString("incremental"));equal("2026-09-05",hardware.getString("security_patch"));
        equal("arm64-v8a",hardware.getJSONArray("abis").getString(0));equal(true,hardware.has("time_zone")&&hardware.has("locale")&&hardware.has("rom"));
        equal("unknown",DiagnosticReport.safeFingerprint("/data/private\nline"));
        equal("unknown",DiagnosticReport.safeBuild("/data/private"));equal("unknown",DiagnosticReport.safeBuild("secret\nline"));

        LinkedHashMap<String,Object> values=new LinkedHashMap<>();
        values.putAll(StatusBarSettings.BOOLEAN_DEFAULTS);values.putAll(StatusBarSettings.NUMERIC_DEFAULTS);
        values.putAll(StatusBarSettings.COLOR_DEFAULTS);values.putAll(StatusBarSettings.STRING_DEFAULTS);
        values.put("clock_scale",79f);values.put("clock_weight",824f);values.put("battery_color_light",0x80aabbcc);
        values.put("clock_pattern","HH:mm secret private text");values.put(StatusBarSettings.CARRIER_TEXT,"private carrier content");
        values.put(StatusBarSettings.FONT_NAME,"/data/user/private.ttf");values.put(StatusBarSettings.FONT_REVISION,"some-private-font-hash");
        values.put("ssid","private network");values.put("pin","PIN_FIXTURE_A7");values.put("unknown_secret","private unknown text");
        values.put(NativeStatusIcons.PRIORITY,"bluetooth,location,private_token_PIN_FIXTURE_A7");
        JSONObject snapshot=DiagnosticReport.settings(values),report=DiagnosticReport.report(device,values);
        equal(79.0,snapshot.getJSONObject("clock_scale").getDouble("value"));
        equal(824.0,snapshot.getJSONObject("clock_weight").getDouble("value"));
        equal("#80AABBCC",snapshot.getJSONObject("battery_color_light").getString("value"));
        equal("HH:mm secret private text".length(),snapshot.getJSONObject("clock_pattern").getInt("length"));
        equal("imported",snapshot.getJSONObject(StatusBarSettings.FONT_REVISION).getString("source"));
        equal("name-redacted",snapshot.getJSONObject(StatusBarSettings.FONT_NAME).getString("source"));
        equal(2,snapshot.getJSONObject(NativeStatusIcons.PRIORITY).getJSONArray("slots").length());
        for(String key:StatusBarSettings.BOOLEAN_DEFAULTS.keySet())equal(true,snapshot.has(key));
        for(String key:StatusBarSettings.NUMERIC_DEFAULTS.keySet())equal(true,snapshot.has(key));
        for(String key:StatusBarSettings.COLOR_DEFAULTS.keySet())equal(true,snapshot.has(key));
        for(String key:StatusBarSettings.STRING_DEFAULTS.keySet())equal(true,snapshot.has(key));
        for(String color:StatusBarSettings.COLOR_DEFAULTS.keySet())equal(true,snapshot.has(StatusBarSettings.alphaKey(color)));
        String serialized=report.toString(2);
        for(String privateValue:new String[]{"secret private text","private carrier content","/data/user","private.ttf",
                "some-private-font-hash","private network","PIN_FIXTURE_A7","private unknown text","private_token"})equal(false,serialized.contains(privateValue));
        equal(true,serialized.getBytes(StandardCharsets.UTF_8).length<=DiagnosticReport.MAX_REPORT_BYTES);
        equal(2,report.getInt("report_schema"));equal(true,report.getJSONArray("guide").length()>=5);equal(true,report.has("packages")&&report.has("diagnosis")&&report.has("latest_facts"));equal(ModuleRuntimeStatus.BUILD_TOKEN,report.getJSONObject("app").getString("expected_runtime"));
        equal(true,report.getString("limitations").contains("不能")||report.getString("limitations").contains("不足"));
        // Wrong types retain a useful failure marker and their applied fallback, without printing the raw value.
        values.put("clock_scale","private corrupted numeric");values.put("clock_enabled","private corrupted boolean");
        snapshot=DiagnosticReport.settings(values);
        equal(true,snapshot.getJSONObject("clock_scale").getJSONObject("stored").getBoolean("invalid_type"));
        equal(true,snapshot.getJSONObject("clock_enabled").getJSONObject("stored").getBoolean("invalid_type"));
        equal(false,snapshot.toString().contains("private corrupted"));
        JSONObject change=DiagnosticReport.change("clock_weight",600f,824f);
        equal(600.0,change.getJSONObject("previous").getDouble("value"));equal(824.0,change.getJSONObject("next").getDouble("value"));
        equal(null,DiagnosticReport.change("pin",1234,528));
        JSONObject textChange=DiagnosticReport.change(StatusBarSettings.CARRIER_TEXT,"private before","private after");
        equal(false,textChange.toString().contains("private"));equal(13,textChange.getJSONObject("next").getInt("length"));
        JSONObject font=DiagnosticReport.settingValue(StatusBarSettings.FONT_REVISION,FontCatalog.entries().get(0).sha256);
        equal("catalog:"+FontCatalog.entries().get(0).id,font.getString("source"));
        JSONObject malicious=new JSONObject(change.toString());malicious.put("private_extra","secret");
        malicious.getJSONObject("next").put("user_text","private payload");
        JSONObject validated=DiagnosticReport.validateDetails(malicious);
        equal(false,validated.toString().contains("secret"));equal(false,validated.toString().contains("private payload"));
        malicious.put("key","pin");equal(null,DiagnosticReport.validateDetails(malicious));
        JSONObject fakeText=new JSONObject().put("kind","setting_change").put("key",StatusBarSettings.CARRIER_TEXT)
                .put("next",new JSONObject().put("type","option").put("value","private payload").put("length",15));
        validated=DiagnosticReport.validateDetails(fakeText);equal(false,validated.toString().contains("private payload"));
        equal(15,validated.getJSONObject("next").getInt("length"));
        JSONObject runtime=new JSONObject().put("kind","runtime_applied").put("runtime",ModuleRuntimeStatus.BUILD_TOKEN)
                .put("sequence",2).put("setting_count",400).put("display",display).put("serial","private serial");
        validated=DiagnosticReport.validateDetails(runtime);equal(false,validated.has("serial"));
        equal("landscape",validated.getJSONObject("display").getString("orientation"));
        runtimeTokens(values);
        JSONObject metrics=new JSONObject().put("owner_known",true).put("elapsed_ms",12.5).put("pin",528).put("ssid","private network");
        JSONObject stage=new JSONObject().put("kind","hook_stage").put("phase","prepare").put("metrics",metrics);
        validated=DiagnosticReport.validateDetails(stage);equal(2,validated.getJSONObject("metrics").length());
        equal(false,validated.toString().contains("528"));equal(false,validated.toString().contains("private network"));
        stage.put("phase","private phase payload");equal(null,DiagnosticReport.validateDetails(stage));
        equal(null,DiagnosticReport.settingValue("clock_scale",null).opt("value"));
        equal(true,DiagnosticReport.settingValue("clock_scale",Float.NaN).getBoolean("invalid_type"));
        JSONObject event=ModuleDiagnostics.event("bigclock","native hook failed",new IllegalStateException("private exception"));
        event.put("details",runtime);validated=ModuleDiagnostics.validateEvent(event);
        equal("bigclock",validated.getString("source"));equal("runtime_applied",validated.getJSONObject("details").getString("kind"));
        equal(false,validated.toString().contains("private exception"));
        facts(device,values);
        fluidGradientFacts();
        return checks;
    }

    /** Real dotted build identities must survive the export boundary and drive the correct diagnosis. */
    private static void runtimeTokens(Map<String,Object> values) throws Exception {
        String reported="c17-runtime-20261012-1.8.3-beta50-code133";
        JSONObject details=new JSONObject().put("kind","runtime_applied").put("runtime",reported);
        equal(reported,DiagnosticReport.validateDetails(details).optString("runtime",""));
        details.put("runtime",ModuleRuntimeStatus.BUILD_TOKEN);
        JSONObject event=ModuleDiagnostics.event("setting","系统界面配置应用成功",null).put("details",details);
        JSONObject validated=ModuleDiagnostics.validateEvent(event);
        equal(ModuleRuntimeStatus.BUILD_TOKEN,validated.getJSONObject("details").optString("runtime",""));
        java.util.ArrayList<JSONObject> events=new java.util.ArrayList<>();events.add(validated);
        JSONArray diagnosis=DiagnosticReport.diagnose(new JSONObject(),new JSONObject(),events,values);
        JSONObject token=null;
        for(int i=0;i<diagnosis.length();i++)
            if("runtime_token".equals(diagnosis.getJSONObject(i).getString("id")))token=diagnosis.getJSONObject(i);
        equal(true,token!=null);equal("ok",token.getString("status"));
        equal(true,token.getString("text").contains(ModuleRuntimeStatus.BUILD_TOKEN));
        for(String invalid:new String[]{reported+"/private",reported+"\nprivate",reported+" private",
                "private-runtime",reported+"?secret=value",reported+new String(new char[121]).replace('\0','x')}) {
            details.put("runtime",invalid);
            equal(false,DiagnosticReport.validateDetails(details).has("runtime"));
        }
    }

    /** Structured facts keep only whitelisted names with plain values, and drive the diagnosis hints. */
    private static void facts(Device device,Map<String,Object> values) throws Exception {
        JSONObject raw=new JSONObject().put("kind","fact").put("topic","clock")
                .put("values",new JSONObject().put("reason","pending_lost").put("gap_ms",4200).put("interactive",true).put("pending",false)
                        .put("secret_name","private").put("group","private text with spaces and / slash").put("view_width",1080));
        JSONObject fact=DiagnosticReport.validateDetails(raw);
        equal("clock",fact.getString("topic"));equal("pending_lost",fact.getJSONObject("values").getString("reason"));
        equal(4200,fact.getJSONObject("values").getInt("gap_ms"));equal(true,fact.getJSONObject("values").getBoolean("interactive"));
        equal(false,fact.getJSONObject("values").has("secret_name"));equal(false,fact.getJSONObject("values").has("group"));
        equal(false,fact.toString().contains("private"));
        equal(null,DiagnosticReport.validateDetails(new JSONObject().put("kind","fact").put("topic","private_topic").put("values",new JSONObject())));
        java.util.ArrayList<JSONObject> events=new java.util.ArrayList<>();
        events.add(new JSONObject().put("timestamp","2026-10-06T10:00:00Z").put("message","A").put("details",fact));
        events.add(new JSONObject().put("timestamp","2026-10-06T10:00:01Z").put("message","B").put("details",new JSONObject().put("kind","fact").put("topic","scene")
                .put("values",new JSONObject().put("scene","lock").put("previous","shade"))));
        JSONObject report=DiagnosticReport.report(device,values,events);
        equal("lock",report.getJSONObject("latest_facts").getJSONObject("scene").getJSONObject("values").getString("scene"));
        equal("A",report.getJSONObject("latest_facts").getJSONObject("clock").getString("message"));
        boolean healed=false,runtimeMissing=false;
        JSONArray diagnosis=report.getJSONArray("diagnosis");
        for(int i=0;i<diagnosis.length();i++) {
            String id=diagnosis.getJSONObject(i).getString("id");
            if(id.equals("clock_tick"))healed=true;if(id.equals("runtime_applied"))runtimeMissing=true;
        }
        equal(true,healed);equal(true,runtimeMissing);
        equal(false,report.toString().contains("private"));
    }

    /** Range and renderer evidence survive both detail filtering and the received-event boundary. */
    private static void fluidGradientFacts() throws Exception {
        for(double range:new double[]{1d,23.4d,50d,100d}) {
            JSONObject raw=new JSONObject().put("kind","fact").put("topic","fluid").put("private_extra","private payload")
                    .put("values",new JSONObject().put("gradient_range",range).put("gradient_owned",range<100d)
                            .put("gradient_drawn",true).put("gradient_rebuilt",false).put("view_width",1312).put("view_height",576)
                            .put("gradient_unknown_number",17).put("gradient_unknown_boolean",true)
                            .put("gradient_unknown_text","identifier").put("reason","private payload / path"));
            JSONObject details=DiagnosticReport.validateDetails(raw);
            JSONObject received=ModuleDiagnostics.validateEvent(new JSONObject(ModuleDiagnostics.event("fluid","渐变范围诊断",null)
                    .put("details",details).toString())).getJSONObject("details");
            for(JSONObject filtered:new JSONObject[]{details,received}) {
                JSONObject safe=filtered.getJSONObject("values");
                equal("fact",filtered.getString("kind"));equal("fluid",filtered.getString("topic"));
                equal(range,safe.optDouble("gradient_range",Double.NaN));equal(range<100d,safe.opt("gradient_owned"));
                equal(true,safe.opt("gradient_drawn"));equal(false,safe.opt("gradient_rebuilt"));
                equal(1312,safe.getInt("view_width"));equal(576,safe.getInt("view_height"));
                equal(6,safe.length());equal(false,filtered.toString().contains("private"));
            }
        }
        JSONObject invalid=new JSONObject("{\"kind\":\"fact\",\"topic\":\"fluid\",\"values\":{\"gradient_range\":1e999,"
                +"\"gradient_owned\":\"private payload / path\",\"gradient_drawn\":[],\"gradient_rebuilt\":{},"
                +"\"gradient_unknown_number\":1,\"gradient_unknown_boolean\":true,\"gradient_unknown_text\":\"identifier\"}}");
        JSONObject filtered=DiagnosticReport.validateDetails(invalid);
        equal(0,filtered.getJSONObject("values").length());
        JSONObject received=ModuleDiagnostics.validateEvent(new JSONObject(ModuleDiagnostics.event("fluid","渐变范围诊断",null)
                .put("details",filtered).toString())).getJSONObject("details");
        equal(0,received.getJSONObject("values").length());
        for(String name:new String[]{"gradient_range","gradient_owned","gradient_drawn","gradient_rebuilt"}) {
            JSONObject privateText=new JSONObject().put("kind","fact").put("topic","fluid")
                    .put("values",new JSONObject().put(name,"private payload / path"));
            equal(0,DiagnosticReport.validateDetails(privateText).getJSONObject("values").length());
        }
    }
    public static void main(String[] args) throws Exception {System.out.println("DiagnosticReportCheck: "+run()+" checks passed");}
}
