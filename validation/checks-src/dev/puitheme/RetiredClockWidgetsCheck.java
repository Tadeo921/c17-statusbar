package dev.puitheme;
import android.content.*;
import android.appwidget.AppWidgetHost;
import java.lang.reflect.Field;
public final class RetiredClockWidgetsCheck {
    private static int checks;
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    private static void restart()throws Exception{Field field=RetiredClockWidgets.class.getDeclaredField("attempted");field.setAccessible(true);field.set(null,false);}
    private static final class Owner extends Context {
        final ActivationGuardPreferencesCheck.MemoryPreferences privateStore=new ActivationGuardPreferencesCheck.MemoryPreferences();
        String name=ModuleDiagnostics.PACKAGE_NAME;
        public String getPackageName(){return name;}
        public SharedPreferences getSharedPreferences(String key,int mode){check(key.equals("clock_widget_slots_v1"),"only retired host journal is opened");return privateStore;}
    }
    public static void main(String[] args)throws Exception {
        restart();AppWidgetHost.retiredHosts.clear();Owner owner=new Owner();owner.privateStore.values.put("0.id",19);
        owner.name="com.android.systemui";RetiredClockWidgets.release(owner);
        check(AppWidgetHost.retiredHosts.isEmpty()&&owner.privateStore.values.containsKey("0.id"),"foreign context cannot retire another UID host");
        owner.name=ModuleDiagnostics.PACKAGE_NAME;AppWidgetHost.failRetire=true;RetiredClockWidgets.release(owner);
        check(owner.privateStore.values.containsKey("0.id")&&!owner.privateStore.getBoolean("text_components_retired169",false),"service failure preserves retry information");
        AppWidgetHost.failRetire=false;RetiredClockWidgets.release(owner);
        check(AppWidgetHost.retiredHosts.isEmpty(),"failure is bounded to once per app process");
        restart();RetiredClockWidgets.release(owner);
        check(AppWidgetHost.retiredHosts.size()==1&&AppWidgetHost.retiredHosts.get(0)==0x43313757,"only this app's exact code168 host is released");
        check(owner.privateStore.getBoolean("text_components_retired169",false)&&!owner.privateStore.values.containsKey("0.id"),"retirement is recorded only after service success");
        restart();RetiredClockWidgets.release(owner);
        check(AppWidgetHost.retiredHosts.size()==1,"later app starts never repeat confirmed retirement");
        System.out.println("RetiredClockWidgetsCheck: "+checks+" checks passed (service fixture)");
    }
}
