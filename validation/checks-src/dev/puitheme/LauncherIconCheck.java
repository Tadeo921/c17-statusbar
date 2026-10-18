package dev.puitheme;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Own-component state, the two launcher names and the independent manager entry; never executes a device operation. */
public final class LauncherIconCheck {
    private static final String PACKAGE="dev.puitheme.iosstatusbar",ACTIVITY="dev.puitheme.ModernMainActivity";
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Launcher "+checks+": "+expected+" != "+actual);}
    private static final class Manager extends PackageManager {
        final Map<String,ActivityInfo> activities=new HashMap<>();
        final Map<String,Integer> states=new HashMap<>();
        final List<String> writes=new ArrayList<>();
        int mutations,reads,settingsReads,lastFlags,lastState;ComponentName lastComponent;
        boolean failRead,failWrite,ignoreWrite;
        Manager(){activity(LauncherIcon.LAUNCHER_ALIAS);activity(LauncherIcon.SETTINGS_ALIAS);activity(LauncherIcon.LEGACY_ALIAS).enabled=false;}
        ActivityInfo activity(String name){ActivityInfo info=new ActivityInfo();info.name=name;info.packageName=PACKAGE;info.targetActivity=ACTIVITY;activities.put(name,info);return info;}
        private void own(ComponentName component){
            String name=component.getClassName();
            if(!PACKAGE.equals(component.getPackageName())||!LauncherIcon.LAUNCHER_ALIAS.equals(name)&&!LauncherIcon.SETTINGS_ALIAS.equals(name)&&!LauncherIcon.LEGACY_ALIAS.equals(name))
                throw new AssertionError("Foreign component");
        }
        @Override public ActivityInfo getActivityInfo(ComponentName component,int flags)throws NameNotFoundException {
            own(component);reads++;if(flags!=MATCH_DISABLED_COMPONENTS)throw new AssertionError("Disabled alias must remain queryable");
            if(LauncherIcon.SETTINGS_ALIAS.equals(component.getClassName()))settingsReads++;
            ActivityInfo info=activities.get(component.getClassName());if(failRead||info==null)throw new NameNotFoundException(component.getClassName());return info;
        }
        @Override public int getComponentEnabledSetting(ComponentName component){own(component);return states.getOrDefault(component.getClassName(),COMPONENT_ENABLED_STATE_DEFAULT);}
        @Override public void setComponentEnabledSetting(ComponentName component,int state,int flags){
            own(component);if(LauncherIcon.SETTINGS_ALIAS.equals(component.getClassName()))throw new AssertionError("Settings entry must never be toggled");
            mutations++;lastComponent=component;lastFlags=flags;lastState=state;
            writes.add(component.getClassName()+(state==COMPONENT_ENABLED_STATE_ENABLED?" on":" off"));
            if(failWrite)throw new SecurityException("Rejected fixture operation");if(!ignoreWrite)states.put(component.getClassName(),state);
        }
    }
    /** In-memory preferences: only what LauncherIcon uses. */
    private static SharedPreferences preferences(Map<String,Object> store) {
        return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(self,method,args)->{
            switch(method.getName()) {
                case "getBoolean":return store.containsKey(args[0])?store.get(args[0]):args[1];
                case "edit":
                    Map<String,Object> pending=new HashMap<>();
                    return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(editor,operation,parameters)->{
                        switch(operation.getName()) {
                            case "putBoolean":pending.put((String)parameters[0],parameters[1]);return editor;
                            case "apply":store.putAll(pending);return null;
                            case "commit":store.putAll(pending);return true;
                            default:throw new AssertionError("Unexpected editor call "+operation.getName());
                        }
                    });
                default:throw new AssertionError("Unexpected preferences call "+method.getName());
            }
        });
    }
    private static final class Own extends Context {
        final Manager manager=new Manager();String packageName=PACKAGE;int managerRequests;
        final Map<String,Object> remembered=new HashMap<>();
        @Override public String getPackageName(){return packageName;}
        @Override public PackageManager getPackageManager(){managerRequests++;return manager;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){equal("launcher_name",name);return preferences(remembered);}
    }
    private static void manifest()throws Exception {
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        Element manifest=factory.newDocumentBuilder().parse(new File("app/src/main/AndroidManifest.xml")).getDocumentElement();
        equal(PACKAGE,manifest.getAttribute("package"));NodeList aliases=manifest.getElementsByTagName("activity-alias");equal(4,aliases.getLength());
        int launcherCount=0,settingsCount=0,legacyCount=0;
        for(int i=0;i<aliases.getLength();i++) {
            Element alias=(Element)aliases.item(i);String name=alias.getAttributeNS(ANDROID,"name");
            if ("dev.puitheme.PerformanceControlActivity".equals(name)) {
                equal("dev.puitheme.PerformanceWebActivity",alias.getAttributeNS(ANDROID,"targetActivity"));
                equal("true",alias.getAttributeNS(ANDROID,"exported"));
                equal(0,alias.getElementsByTagName("intent-filter").getLength());
                continue; // An explicit manager entry is never another launcher/settings icon.
            }
            equal(ACTIVITY,alias.getAttributeNS(ANDROID,"targetActivity"));equal("true",alias.getAttributeNS(ANDROID,"exported"));
            NodeList categories=alias.getElementsByTagName("category");boolean launcher=false,settings=false,info=false;
            for(int j=0;j<categories.getLength();j++){String value=((Element)categories.item(j)).getAttributeNS(ANDROID,"name");launcher|="android.intent.category.LAUNCHER".equals(value);settings|=LauncherIcon.SETTINGS_CATEGORY.equals(value);info|="android.intent.category.INFO".equals(value);}
            NodeList actions=alias.getElementsByTagName("action");equal(1,actions.getLength());equal("android.intent.action.MAIN",((Element)actions.item(0)).getAttributeNS(ANDROID,"name"));
            if(LauncherIcon.LAUNCHER_ALIAS.equals(name)){equal("true",alias.getAttributeNS(ANDROID,"enabled"));equal(true,launcher);equal(false,settings);launcherCount++;}
            else if(LauncherIcon.SETTINGS_ALIAS.equals(name)){equal("true",alias.getAttributeNS(ANDROID,"enabled"));equal(false,launcher);equal(true,settings);equal(true,info);settingsCount++;}
            else if(LauncherIcon.LEGACY_ALIAS.equals(name)){
                // The old name is a second launcher entry, off until chosen.
                equal("false",alias.getAttributeNS(ANDROID,"enabled"));equal(true,launcher);equal(false,settings);
                equal("@string/app_name_legacy",alias.getAttributeNS(ANDROID,"label"));legacyCount++;
            }
            else throw new AssertionError("Unexpected alias "+name);
        }
        equal(1,launcherCount);equal(1,settingsCount);equal(1,legacyCount);
        String strings=new String(Files.readAllBytes(new File("app/src/main/res/values/strings.xml").toPath()),StandardCharsets.UTF_8);
        equal(true,strings.contains("<string name=\"app_name\">"+LauncherIcon.NAME+"</string>"));
        equal(true,strings.contains("<string name=\"app_name_legacy\">"+LauncherIcon.LEGACY_NAME+"</string>"));
    }
    private static void names() {
        Own context=new Own();
        equal(false,LauncherIcon.read(context).legacyName);equal(LauncherIcon.NAME,LauncherIcon.displayName(context));
        LauncherIcon.Result result=LauncherIcon.setLegacyName(context,true);
        equal(true,result.success);equal(true,result.legacyName);equal(false,result.hidden);
        // The old entry is enabled before the new one goes off: there is always an icon.
        equal(2,context.manager.writes.size());
        equal(LauncherIcon.LEGACY_ALIAS+" on",context.manager.writes.get(0));equal(LauncherIcon.LAUNCHER_ALIAS+" off",context.manager.writes.get(1));
        equal(LauncherIcon.LEGACY_NAME,LauncherIcon.displayName(context));
        equal(true,LauncherIcon.setLegacyName(context,true).success);equal(2,context.manager.mutations);
        // Hiding turns both off and remembers the old name; showing brings that one back.
        equal(true,LauncherIcon.setHidden(context,true).hidden);equal(4,context.manager.mutations);
        equal(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,context.manager.states.get(LauncherIcon.LAUNCHER_ALIAS));
        equal(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,context.manager.states.get(LauncherIcon.LEGACY_ALIAS));
        equal(true,LauncherIcon.read(context).legacyName);
        result=LauncherIcon.setLegacyName(context,false);equal(true,result.success);equal(true,result.hidden);equal(4,context.manager.mutations);
        equal(false,LauncherIcon.read(context).legacyName);
        result=LauncherIcon.setHidden(context,false);equal(true,result.success);equal(false,result.hidden);
        equal(LauncherIcon.LAUNCHER_ALIAS+" on",context.manager.writes.get(context.manager.writes.size()-1));
        equal(LauncherIcon.NAME,LauncherIcon.displayName(context));
        // Back to the new name from the old one.
        LauncherIcon.setLegacyName(context,true);result=LauncherIcon.setLegacyName(context,false);
        equal(true,result.success);equal(false,result.legacyName);
        equal(LauncherIcon.LAUNCHER_ALIAS+" on",context.manager.writes.get(context.manager.writes.size()-2));
        equal(LauncherIcon.LEGACY_ALIAS+" off",context.manager.writes.get(context.manager.writes.size()-1));
        Own rejected=new Own();rejected.manager.failWrite=true;result=LauncherIcon.setLegacyName(rejected,true);
        equal(false,result.success);equal(false,result.legacyName);
        Own wrong=new Own();wrong.manager.activities.get(LauncherIcon.LEGACY_ALIAS).targetActivity="other.Activity";equal(false,LauncherIcon.read(wrong).success);
    }
    public static void main(String[] args)throws Exception {
        manifest();Own context=new Own();LauncherIcon.Result result=LauncherIcon.read(context);
        equal(true,result.success);equal(false,result.hidden);equal(false,LauncherIcon.isHidden(context));
        equal(true,LauncherIcon.setHidden(context,false).success);equal(0,context.manager.mutations);equal(0,context.manager.settingsReads);
        result=LauncherIcon.setHidden(context,true);equal(true,result.success);equal(true,result.hidden);equal(true,LauncherIcon.isHidden(context));
        equal(2,context.manager.mutations);equal(PackageManager.DONT_KILL_APP,context.manager.lastFlags);equal(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,context.manager.lastState);
        equal(PACKAGE,context.manager.lastComponent.getPackageName());
        equal(PackageManager.COMPONENT_ENABLED_STATE_DISABLED,context.manager.states.get(LauncherIcon.LAUNCHER_ALIAS));
        equal(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,context.manager.states.getOrDefault(LauncherIcon.SETTINGS_ALIAS,0));
        equal(true,LauncherIcon.setHidden(context,true).success);equal(2,context.manager.mutations);
        result=LauncherIcon.setHidden(context,false);equal(true,result.success);equal(false,result.hidden);equal(3,context.manager.mutations);equal(PackageManager.COMPONENT_ENABLED_STATE_ENABLED,context.manager.lastState);
        equal(LauncherIcon.LAUNCHER_ALIAS,context.manager.lastComponent.getClassName());
        for(int state:new int[]{PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED}){context.manager.states.put(LauncherIcon.LAUNCHER_ALIAS,state);equal(true,LauncherIcon.isHidden(context));}
        context.manager.states.clear();context.manager.activities.get(LauncherIcon.LAUNCHER_ALIAS).enabled=false;equal(true,LauncherIcon.isHidden(context));
        context.manager.states.put(LauncherIcon.LAUNCHER_ALIAS,PackageManager.COMPONENT_ENABLED_STATE_ENABLED);equal(false,LauncherIcon.isHidden(context));
        for(int invalid=0;invalid<4;invalid++) {
            Own blocked=new Own();ActivityInfo settings=blocked.manager.activities.get(LauncherIcon.SETTINGS_ALIAS);
            if(invalid==0)settings.exported=false;else if(invalid==1)settings.targetActivity="other.Activity";else if(invalid==2)blocked.manager.states.put(LauncherIcon.SETTINGS_ALIAS,PackageManager.COMPONENT_ENABLED_STATE_DISABLED);else blocked.manager.activities.remove(LauncherIcon.SETTINGS_ALIAS);
            result=LauncherIcon.setHidden(blocked,true);equal(false,result.success);equal(false,result.hidden);equal(0,blocked.manager.mutations);
        }
        Own recovery=new Own();recovery.manager.states.put(LauncherIcon.LAUNCHER_ALIAS,PackageManager.COMPONENT_ENABLED_STATE_DISABLED);recovery.manager.activities.remove(LauncherIcon.SETTINGS_ALIAS);
        equal(true,LauncherIcon.setHidden(recovery,false).success);equal(false,LauncherIcon.isHidden(recovery));
        Own foreign=new Own();foreign.packageName="other.package";equal(false,LauncherIcon.read(foreign).success);equal(false,LauncherIcon.setHidden(foreign,true).success);equal(0,foreign.managerRequests);
        equal(false,LauncherIcon.read(null).success);
        Own failed=new Own();failed.manager.failRead=true;equal(false,LauncherIcon.read(failed).success);equal(false,LauncherIcon.setHidden(failed,true).success);equal(0,failed.manager.mutations);
        Own wrong=new Own();wrong.manager.activities.get(LauncherIcon.LAUNCHER_ALIAS).targetActivity="other.Activity";equal(false,LauncherIcon.read(wrong).success);equal(false,LauncherIcon.setHidden(wrong,true).success);equal(0,wrong.manager.mutations);
        Own noOp=new Own();noOp.manager.ignoreWrite=true;result=LauncherIcon.setHidden(noOp,true);equal(false,result.success);equal(false,result.hidden);
        Own rejected=new Own();rejected.manager.failWrite=true;result=LauncherIcon.setHidden(rejected,true);equal(false,result.success);equal(false,result.hidden);equal(false,result.message.isEmpty());
        Own unknown=new Own();unknown.manager.states.put(LauncherIcon.LAUNCHER_ALIAS,99);equal(false,LauncherIcon.read(unknown).success);equal(0,unknown.manager.mutations);
        names();
        System.out.println("LauncherIconCheck passed "+checks+" checks");
    }
}
