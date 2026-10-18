package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** The mini stack's source declaration: exact project/author/link texts, and a durable app-only acknowledgement that is
 * independent of the free notice, of the feature switch and of imported configurations. */
public final class NotificationMiniStackSourceCheck {
    private static int checks;
    private static void equal(Object expected,Object actual,String what){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Mini stack source "+checks+" "+what+": "+expected+" != "+actual);}
    private static final class Store {
        final ActivationGuardPreferencesCheck.MemoryPreferences memory=new ActivationGuardPreferencesCheck.MemoryPreferences();
        final Map<String,Object> disk=new LinkedHashMap<>();
        int failures;
        final SharedPreferences raw=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(owner,method,args)->{
            if(!method.getName().equals("edit"))return method.invoke(memory,args);
            SharedPreferences.Editor nativeEditor=memory.edit();
            return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(wrapper,operation,parameters)->{
                if(!operation.getName().equals("commit")&&!operation.getName().equals("apply")){operation.invoke(nativeEditor,parameters);return wrapper;}
                nativeEditor.commit();boolean failed=failures>0;if(failed)failures--;
                if(!failed){disk.clear();disk.putAll(memory.values);}
                return operation.getName().equals("apply")?null:!failed;
            });
        });
        Store reopen(){Store result=new Store();result.memory.values.putAll(disk);result.disk.putAll(disk);return result;}
    }
    private static final class Storage extends Context {
        final Map<String,Store> stores=new HashMap<>();
        @Override public SharedPreferences getSharedPreferences(String name,int mode){equal(0,mode,"private mode");return stores.computeIfAbsent(name,key->new Store()).raw;}
    }
    public static void main(String[] args){
        equal("OplusX",NotificationMiniStackSource.PROJECT,"project name casing");equal("xxxxawl",NotificationMiniStackSource.AUTHOR,"author");
        equal("https://www.coolapk.com/u/18956155",NotificationMiniStackSource.URL,"author page");
        equal("功能参考 OplusX「启用小堆叠（同应用）」。感谢 OplusX 作者 xxxxawl 提供实现参考。点击查看作者酷安主页。",NotificationMiniStackSource.DECLARATION,"declaration text");
        equal("同应用通知小堆叠 · 参考 OplusX / xxxxawl",NotificationMiniStackSource.SHORT,"entry note");
        equal(true,NotificationMiniStackSource.NOTICE.startsWith(NotificationMiniStackSource.DECLARATION),"first-use notice carries the declaration");
        equal(true,NotificationMiniStackSource.NOTICE.contains("不会自动重启系统界面"),"no automatic restart");
        equal(true,NotificationMiniStackSource.TECHNICAL_ORIGIN.contains("VivoGroupDeck test21"),"technical origin kept");
        equal(false,NotificationMiniStackSource.DECLARATION.contains("Oplusx")||NotificationMiniStackSource.DECLARATION.contains("OPLUSX"),"casing");
        equal(false,ProjectContact.COOLAPK_URL.equals(NotificationMiniStackSource.URL),"not this module's own author page");
        // Only switching the stack on needs the declaration.
        equal(true,NotificationMiniStackSource.requiresDeclaration(NotificationMiniStack.ENABLED,true),"enable");
        equal(false,NotificationMiniStackSource.requiresDeclaration(NotificationMiniStack.ENABLED,false),"disable");
        equal(false,NotificationMiniStackSource.requiresDeclaration(NotificationMiniStack.HIDE_BADGE,true),"badge switch");
        Map<String,Object> imported=new HashMap<>();imported.put(NotificationMiniStack.ENABLED,true);
        equal(true,NotificationMiniStackSource.importNeedsDeclaration(imported,false),"import that turns it on");
        equal(false,NotificationMiniStackSource.importNeedsDeclaration(imported,true),"already acknowledged");
        imported.put(NotificationMiniStack.ENABLED,false);equal(false,NotificationMiniStackSource.importNeedsDeclaration(imported,false),"import that keeps it off");
        equal(false,NotificationMiniStackSource.importNeedsDeclaration(null,false),"no import");
        // Durable, private and independent: neither the free notice nor the runtime settings stand in for it.
        Storage context=new Storage();Store runtime=new Store();runtime.memory.values.put(NotificationMiniStack.ENABLED,true);runtime.disk.putAll(runtime.memory.values);
        context.stores.put("statusbar_settings",runtime);
        Store free=new Store();free.memory.values.put("accepted_version",1);free.disk.putAll(free.memory.values);context.stores.put("app_free_notice",free);
        equal(true,FreeNotice.accepted(context),"free notice accepted");
        equal(false,NotificationMiniStackSource.acknowledged(context),"an enabled setting and the free notice are not the acknowledgement");
        Store record=context.stores.get("app_mini_stack_source");equal(true,record!=null,"own store");
        for(int failures:new int[]{1,2}) {
            record.failures=failures;equal(false,NotificationMiniStackSource.acknowledge(context),"failed write reported");
            equal(false,NotificationMiniStackSource.acknowledged(context),"failed write not remembered");equal(0,record.disk.size(),"nothing on disk");
        }
        equal(true,NotificationMiniStackSource.acknowledge(context),"acknowledged");equal(true,NotificationMiniStackSource.acknowledged(context),"remembered");
        Storage reopened=new Storage();reopened.stores.put("app_mini_stack_source",record.reopen());
        equal(true,NotificationMiniStackSource.acknowledged(reopened),"survives restart");
        equal(1,record.disk.get("acknowledged_version"),"versioned record");equal(1,record.disk.size(),"single key");
        equal(0,runtime.memory.writes,"feature setting untouched");equal(true,runtime.memory.values.get(NotificationMiniStack.ENABLED),"switch state independent");
        equal(1,free.memory.values.get("accepted_version"),"free notice untouched");
        equal(false,StatusBarSettings.BOOLEAN_DEFAULTS.containsKey("acknowledged_version"),"not a hook setting");
        System.out.println("NotificationMiniStackSourceCheck passed "+checks+" checks");
    }
}
