package dev.puitheme;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Real module event entry points with SDK-shaped callback values. This does
 * not execute Android registration, connectivity services or a phone UI. */
public final class BigClockSystemTextEventsCheck {
    private static int checks;
    public static final class ActiveSub {public static int id=7;public static int get(){return id;}}
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void set(String name,Object value)throws Exception{Field field=StatusBarModule.class.getDeclaredField(name);field.setAccessible(true);field.set(null,value);}
    private static Object get(String name)throws Exception{Field field=StatusBarModule.class.getDeclaredField(name);field.setAccessible(true);return field.get(null);}
    private static Object invoke(String name,Class<?>[] types,Object... values)throws Exception{Method method=StatusBarModule.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(null,values);}
    private static void drainSystemText()throws Exception{Runnable task=(Runnable)get("SYSTEM_TEXT_CHANGED");Handler.postedForCheck.remove(task);task.run();}

    private static void batteryAndMergedDispatch()throws Exception {
        set("systemTextRefreshPending",false);BigClockSystemText.clear();Handler.postedForCheck.clear();
        Class<?>[] signature={Intent.class};
        invoke("captureSystemTextBattery",signature,new Intent("unrelated").putExtra("level",5).putExtra("scale",100).putExtra("status",2));
        check(BigClockSystemText.snapshot().battery.equals("电池暂无数据")&&Handler.postedForCheck.isEmpty(),"only the protected native battery event enters the cache");
        Intent nativeEvent=new Intent(Intent.ACTION_BATTERY_CHANGED).putExtra("level",85).putExtra("scale",100).putExtra("status",2);
        invoke("captureSystemTextBattery",signature,nativeEvent);
        check(BigClockSystemText.snapshot().battery.equals("85% 充电中"),"module battery entry consumes native level, scale and charging status");
        Runnable dispatch=(Runnable)get("SYSTEM_TEXT_CHANGED");
        check(Handler.postedForCheck.size()==1&&Handler.postedForCheck.get(0)==dispatch,"first changed event queues one component-only dispatch");
        long revision=BigClockSystemText.revision();
        for(int event=0;event<1000;event++)invoke("captureSystemTextBattery",signature,nativeEvent);
        check(BigClockSystemText.revision()==revision&&Handler.postedForCheck.size()==1,"repeated battery broadcasts neither change revision nor queue duplicate component work");
        invoke("captureSystemTextBattery",signature,new Intent(Intent.ACTION_BATTERY_CHANGED).putExtra("level",84).putExtra("scale",100).putExtra("status",2));
        invoke("systemTextChanged",new Class<?>[]{Boolean.TYPE},true);
        check(Handler.postedForCheck.size()==1&&BigClockSystemText.snapshot().battery.equals("84% 充电中"),"concurrent source changes merge into the same pending component pass");
        drainSystemText();check(!((Boolean)get("systemTextRefreshPending")),"the component dispatch releases its pending marker even when no panel exists");
        invoke("captureSystemTextBattery",signature,new Intent(Intent.ACTION_BATTERY_CHANGED));
        check(BigClockSystemText.snapshot().battery.equals("电池暂无数据")&&Handler.postedForCheck.size()==1,"missing native extras do not become fabricated zero/full battery data");
        drainSystemText();
    }

    private static void nativeCapabilitiesAndRegistration()throws Exception {
        Class<?>[] route={Boolean.TYPE,Boolean.TYPE,NetworkCapabilities.class};
        invoke("captureSystemTextNetwork",route,false,false,null);
        check(BigClockSystemText.snapshot().networkStatus.equals("网络暂无数据"),"absent connectivity manager is unknown, not an offline assertion");
        invoke("captureSystemTextNetwork",route,true,false,null);
        check(BigClockSystemText.snapshot().networkStatus.equals("未联网"),"explicit absent default network is offline");
        NetworkCapabilities wifi=new NetworkCapabilities().addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        invoke("captureSystemTextNetwork",route,true,true,wifi);
        check(BigClockSystemText.snapshot().networkStatus.equals("Wi-Fi（受限）"),"native unvalidated Wi-Fi capabilities are limited");
        wifi.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).addTransportType(NetworkCapabilities.TRANSPORT_VPN);
        invoke("captureSystemTextNetwork",route,true,true,wifi);
        check(BigClockSystemText.snapshot().networkStatus.equals("Wi-Fi"),"validated Wi-Fi/VPN capabilities follow their real underlying Wi-Fi transport");
        invoke("captureSystemTextNetwork",route,true,true,null);
        check(BigClockSystemText.snapshot().networkStatus.equals("网络暂无数据"),"missing capabilities do not reuse the last route");
        ServiceState state=new ServiceState();state.setState(ServiceState.STATE_OUT_OF_SERVICE);
        state.registrations.add(new NetworkRegistrationInfo(AccessNetworkConstants.TRANSPORT_TYPE_WWAN,true));
        check(Boolean.TRUE.equals(invoke("systemTextService",new Class<?>[]{ServiceState.class},state)),"registered WWAN data service wins over an out-of-service voice aggregate");
        state.registrations.clear();state.registrations.add(new NetworkRegistrationInfo(AccessNetworkConstants.TRANSPORT_TYPE_WLAN,true));
        check(Boolean.FALSE.equals(invoke("systemTextService",new Class<?>[]{ServiceState.class},state)),"IWLAN/Wi-Fi calling alone is not cellular service");
        check(invoke("systemTextService",new Class<?>[]{ServiceState.class},new Object[]{null})==null,"a missing native service object is explicitly unknown");
    }

    private static void existingTelephonyLifecycle()throws Exception {
        BigClockSystemText.clear();Handler.postedForCheck.clear();set("systemTextRefreshPending",false);
        final TelephonyManager manager=new TelephonyManager();manager.service.registrations.add(new NetworkRegistrationInfo(AccessNetworkConstants.TRANSPORT_TYPE_WWAN,true));
        Context source=new Context(){@Override public Object getSystemService(String name){return "phone".equals(name)?manager:null;}};
        set("context",source);set("observedDataSub",-1);set("observedTelephony",null);set("dataRadioCallback",null);
        set("getActiveDataSubId",ActiveSub.class.getMethod("get"));set("getDefaultDataSubId",ActiveSub.class.getMethod("get"));
        ActiveSub.id=7;invoke("observeDataSubscription",new Class<?>[]{Integer.TYPE},7);
        check(manager.registrations==1&&manager.serviceReads==1&&manager.signalReads==1,"one existing callback registration seeds service and signal only once");
        check(BigClockSystemText.snapshot().cellularSignal.equals("3/4格"),"initial native current-data SIM state seeds the component immediately");
        TelephonyCallback first=manager.callback;
        for(int unchanged=0;unchanged<1000;unchanged++)invoke("observeDataSubscription",new Class<?>[]{Integer.TYPE},7);
        check(manager.registrations==1&&manager.serviceReads==1&&manager.signalReads==1,"stable SIM refreshes do no registration or service/signal reads");
        ((TelephonyCallback.SignalStrengthsListener)first).onSignalStrengthsChanged(new SignalStrength(4));
        check(BigClockSystemText.snapshot().cellularSignal.equals("4/4格")&&manager.signalReads==1,"signal event consumes its supplied model without re-reading a service");
        set("wifiConnected",true);set("airplaneMode",false);set("wifiSwitchEnabled",true);set("mobileDataSwitchEnabled",true);set("composeStatusBar",true);
        invoke("refreshLabel",new Class<?>[0]);
        check(BigClockSystemText.snapshot().dataGeneration.equals("5G")&&"".equals(get("networkLabel")),"raw canonical RAT reaches components before original Wi-Fi/status-bar label hiding");
        ActiveSub.id=8;invoke("observeDataSubscription",new Class<?>[]{Integer.TYPE},8);
        check(manager.registrations==2&&manager.unregistrations==1&&manager.serviceReads==2&&manager.signalReads==2,"SIM handover reuses one observer lifecycle and seeds only the new SIM");
        BigClockSystemText.Snapshot current=BigClockSystemText.snapshot();
        ((TelephonyCallback.SignalStrengthsListener)first).onSignalStrengthsChanged(new SignalStrength(0));
        check(BigClockSystemText.snapshot()==current,"old callback identity cannot inject stale previous-SIM bars");
        ((TelephonyCallback.SignalStrengthsListener)manager.callback).onSignalStrengthsChanged(new SignalStrength(1));
        check(BigClockSystemText.snapshot().cellularSignal.equals("1/4格"),"the current callback can still update after old callback rejection");
        ActiveSub.id=-1;invoke("observeDataSubscription",new Class<?>[]{Integer.TYPE},-1);
        check(manager.unregistrations==2&&BigClockSystemText.snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"missing active subscription retires the observer without guessing physical SIM absence");
        Handler.postedForCheck.clear();set("systemTextRefreshPending",false);set("refreshPending",false);
    }

    public static void main(String[] args)throws Exception {
        try{batteryAndMergedDispatch();nativeCapabilitiesAndRegistration();existingTelephonyLifecycle();}
        finally{BigClockSystemText.clear();Handler.postedForCheck.clear();}
        System.out.println("BigClockSystemTextEventsCheck: "+checks+" checks passed (real module entries; device delivery not executed)");
    }
}
