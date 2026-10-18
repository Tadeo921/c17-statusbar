package dev.puitheme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Executes the event-fed production cache with explicit callback values.
 * No Android service, phone, provider or renderer is simulated here. */
public final class BigClockSystemTextCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static BigClockSystemText.Snapshot snapshot(){return BigClockSystemText.snapshot();}

    private static boolean wifiIcon(Integer resourceId,String resourceName,Boolean visible) {
        try {
            Method method=BigClockSystemText.class.getDeclaredMethod("wifiIcon",Integer.class,String.class,Boolean.class);
            method.setAccessible(true);return (Boolean)method.invoke(null,resourceId,resourceName,visible);
        } catch (ReflectiveOperationException missing) {throw new AssertionError("native Wi-Fi icon events must enter the immutable system snapshot",missing);}
    }
    private static Object wifiField(String name) {
        try {Field field=BigClockSystemText.Snapshot.class.getDeclaredField(name);field.setAccessible(true);return field.get(snapshot());}
        catch(ReflectiveOperationException missing){throw new AssertionError("system snapshot must retain exact Wi-Fi icon evidence: "+name,missing);}
    }

    private static void wifiSignalWithoutTextChange() {
        BigClockSystemText.clear();BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);
        check(wifiIcon(41,"stat_signal_wifi_signal_1",true),"native Wi-Fi icon evidence is a component-visible change");
        BigClockSystemText.Snapshot one=snapshot();
        check(((Integer)wifiField("wifiLevel"))==1&&((Integer)wifiField("wifiResourceId"))==41
                &&wifiField("wifiResourceName").equals("stat_signal_wifi_signal_1")
                &&Boolean.TRUE.equals(wifiField("wifiVisible")),"the real resource and its exact one-bar level survive in the snapshot");
        check(wifiIcon(44,"stat_signal_wifi_signal_4_os17",true),"a native level change invalidates the network component even with unchanged text");
        BigClockSystemText.Snapshot four=snapshot();
        check(one.networkStatus.equals("Wi-Fi")&&four.networkStatus.equals(one.networkStatus)
                &&four.revision==one.revision+1&&four.networkRevision==one.networkRevision+1
                &&four.cellularRevision==one.cellularRevision&&four.batteryRevision==one.batteryRevision,
                "same Wi-Fi label with different bars changes only network and total display revisions");
        for(int repeated=0;repeated<1000;repeated++)check(!wifiIcon(44,"stat_signal_wifi_signal_4_os17",true)
                &&snapshot()==four,"duplicate icon callbacks do not allocate a snapshot or advance revisions");
        for(boolean os17:new boolean[]{false,true})for(int level=0;level<=4;level++) {
            String name="stat_signal_wifi_signal_"+level+(os17?"_os17":"");
            wifiIcon(100+level,name,true);
            check(((Integer)wifiField("wifiLevel"))==level&&wifiField("wifiResourceName").equals(name),
                    "all five exact native resources retain their real level for both native icon families");
        }
        check(((Integer)oneWifi(one,"wifiLevel"))==1&&((Integer)oneWifi(one,"wifiResourceId"))==41,
                "previous icon snapshots remain immutable after subsequent native events");
    }

    private static Object oneWifi(BigClockSystemText.Snapshot value,String name) {
        try {Field field=BigClockSystemText.Snapshot.class.getDeclaredField(name);field.setAccessible(true);return field.get(value);}
        catch(ReflectiveOperationException missing){throw new AssertionError(missing);}
    }

    private static void wifiHiddenUnknownAndLifecycle() {
        BigClockSystemText.clear();
        check(((Integer)wifiField("wifiResourceId"))==0&&wifiField("wifiResourceName").equals("")
                &&((Integer)wifiField("wifiLevel"))==-1&&wifiField("wifiVisible")==null,
                "no native icon evidence is explicitly unknown, never guessed as full bars");
        wifiIcon(204,"stat_signal_wifi_signal_4",true);BigClockSystemText.Snapshot shown=snapshot();
        check(wifiIcon(null,null,false)&&((Integer)wifiField("wifiLevel"))==-1
                &&((Integer)wifiField("wifiResourceId"))==0&&wifiField("wifiResourceName").equals("")
                &&Boolean.FALSE.equals(wifiField("wifiVisible")),"the native Hidden event removes the previously visible bars and resource");
        BigClockSystemText.Snapshot hidden=snapshot();
        check(hidden.networkStatus.equals(shown.networkStatus)&&hidden.revision==shown.revision+1
                &&hidden.networkRevision==shown.networkRevision+1,"hiding an icon updates components without rewriting connectivity text");
        check(!wifiIcon(204,"stat_signal_wifi_signal_4",false)&&snapshot()==hidden,
                "a hidden stale resource cannot restore old bars or allocate a duplicate hidden snapshot");
        String[] unknown={"stat_signal_wifi_signal_5","stat_signal_wifi_signal_-1","stat_signal_wifi_signal_04",
                "stat_signal_wifi_signal_4_dark","stat_signal_wifi_signal_4_os17_extra","stat_signal_wifi_signal_4_os17 ",
                "stat_signal_wifi_signal_４","prefix_stat_signal_wifi_signal_4","STAT_SIGNAL_WIFI_SIGNAL_4",
                "My private SSID","192.168.0.1","stat_signal_wifi_signal_4\n",null,""};
        for(String name:unknown) {
            wifiIcon(204,"stat_signal_wifi_signal_4_os17",true);
            check(wifiIcon(311,name,true)&&((Integer)wifiField("wifiLevel"))==-1
                    &&((Integer)wifiField("wifiResourceId"))==0&&wifiField("wifiResourceName").equals("")
                    &&Boolean.TRUE.equals(wifiField("wifiVisible")),"unknown names remove stale bars without storing identifiers or guessing a level");
            BigClockSystemText.Snapshot unavailable=snapshot();
            check(!wifiIcon(312,name,true)&&snapshot()==unavailable,"unknown resource IDs do not invent changing drawable evidence");
        }
        for(Integer missing:new Integer[]{null,0,-1,Integer.MIN_VALUE}) {
            wifiIcon(204,"stat_signal_wifi_signal_4",true);
            check(wifiIcon(missing,"stat_signal_wifi_signal_4",true)&&((Integer)wifiField("wifiLevel"))==-1,
                    "a valid resource name without an actual resource ID cannot fabricate drawable evidence");
        }
        wifiIcon(202,"stat_signal_wifi_signal_2",true);
        check(wifiIcon(204,"stat_signal_wifi_signal_4",null)&&((Integer)wifiField("wifiLevel"))==-1
                &&wifiField("wifiVisible")==null,"unknown visibility drops the old known bars");
        BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);
        wifiIcon(203,"stat_signal_wifi_signal_3",true);
        check(BigClockSystemText.network(BigClockSystemText.Transport.NONE,true)
                &&snapshot().networkStatus.equals("未联网")&&((Integer)wifiField("wifiLevel"))==-1
                &&Boolean.FALSE.equals(wifiField("wifiVisible")),"an explicit absent default route discards stale Wi-Fi bars as one event");
        wifiIcon(204,"stat_signal_wifi_signal_4",true);
        check(BigClockSystemText.network(BigClockSystemText.Transport.NONE,null)
                &&((Integer)wifiField("wifiResourceId"))==0&&Boolean.FALSE.equals(wifiField("wifiVisible")),
                "an unchanged offline route still retires a later stale icon observation");
        wifiIcon(203,"stat_signal_wifi_signal_3_os17",true);
        check(BigClockSystemText.network(BigClockSystemText.Transport.UNKNOWN,false)
                &&snapshot().networkStatus.equals("网络暂无数据")&&((Integer)wifiField("wifiLevel"))==-1
                &&wifiField("wifiVisible")==null,"unknown capabilities discard old Wi-Fi evidence without claiming an offline or hidden fact");
        BigClockSystemText.Snapshot unknownNetwork=snapshot();
        check(!BigClockSystemText.network(null,true)&&snapshot()==unknownNetwork,
                "duplicate unknown route plus empty icon evidence keeps the immutable snapshot");
        wifiIcon(203,"stat_signal_wifi_signal_3_os17",true);long beforeEpoch=snapshot().subscriptionEpoch;
        check(BigClockSystemText.clear()&&((Integer)wifiField("wifiResourceId"))==0
                &&((Integer)wifiField("wifiLevel"))==-1&&wifiField("wifiVisible")==null,
                "clear retires all Wi-Fi resource evidence together with the other event caches");
        BigClockSystemText.Snapshot cleared=snapshot();BigClockSystemText.clear();
        check(snapshot().subscriptionEpoch>cleared.subscriptionEpoch&&cleared.subscriptionEpoch>beforeEpoch
                &&snapshot().revision==cleared.revision,"empty clear only advances observer epoch, without a fake Wi-Fi display revision");
    }

    private static void noDataAndNoWork() {
        BigClockSystemText.clear();BigClockSystemText.Snapshot initial=snapshot();
        check(initial.cellularSignal.equals("蜂窝信号暂无数据"),"unknown signal is never fabricated as zero bars");
        check(initial.networkStatus.equals("网络暂无数据"),"unknown capabilities are not assumed offline or connected");
        check(initial.battery.equals("电池暂无数据")&&initial.batteryPercent==-1,"missing battery is never fabricated as zero percent");
        for(int frame=0;frame<1000;frame++)check(snapshot()==initial&&BigClockSystemText.revision()==initial.revision,"reads retain the immutable snapshot without polling or allocation");
        check(!BigClockSystemText.network(null,true),"null route cannot turn unknown data into an online network");
        check(!BigClockSystemText.batteryRaw(-1,100,1),"unknown native battery status preserves missing data");
        for(Field field:BigClockSystemText.Snapshot.class.getDeclaredFields()) {
            Class<?> type=field.getType();
            check(type.isPrimitive()||type==String.class||type==Boolean.class||type.isEnum(),"snapshot holds no Context, View, manager, provider, callback or identifier object");
        }
    }

    private static void signalAndSubscription() {
        BigClockSystemText.clear();long token=BigClockSystemText.selectSubscription(7);
        check(!BigClockSystemText.signal(token,8,4)&&snapshot().signalLevel==-1,"other SIM signal cannot be displayed for the active-data SIM");
        check(!BigClockSystemText.signal(token,7,3)&&snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"bars alone do not invent an in-service state");
        check(BigClockSystemText.service(token,7,true)&&snapshot().cellularSignal.equals("3/4格"),"a real in-service callback combines with the current signal event");
        for(int level=0;level<=4;level++){
            BigClockSystemText.signal(token,7,level);
            check(snapshot().signalLevel==level&&snapshot().cellularSignal.equals(level+"/4格"),"every real native level zero through four is shown exactly");
        }
        for(Integer invalid:new Integer[]{null,-1,5,Integer.MIN_VALUE,Integer.MAX_VALUE}){
            BigClockSystemText.signal(token,7,invalid);
            check(snapshot().signalLevel==-1&&snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"invalid native levels become unavailable instead of clamping to invented bars");
        }
        BigClockSystemText.signal(token,7,4);BigClockSystemText.dataNetwork(token,7,"NR");
        check(BigClockSystemText.service(token,7,false)&&snapshot().cellularSignal.equals("无服务")
                &&snapshot().signalLevel==-1&&snapshot().dataGeneration.isEmpty(),"out-of-service clears prior bars and RAT together");
        check(!BigClockSystemText.signal(token,7,4)&&!BigClockSystemText.dataNetwork(token,7,"5G"),"late RF events cannot revive data while no service is known");
        BigClockSystemText.service(token,7,true);
        check(snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"service recovery waits for fresh bars instead of restoring stale pre-outage bars");
        BigClockSystemText.signal(token,7,2);
        long other=BigClockSystemText.selectSubscription(8);
        check(other>token&&snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"switching the active-data SIM resets its RF evidence");
        check(!BigClockSystemText.signal(token,7,4)&&!BigClockSystemText.service(token,7,true),"a previous SIM observer token is rejected");
        long reused=BigClockSystemText.selectSubscription(7);
        check(reused>other&&!BigClockSystemText.signal(token,7,4),"re-selecting the same numeric SIM ID cannot revive its old observer lifecycle");
        check(BigClockSystemText.selectSubscription(7)==reused,"repeated current-SIM selection keeps the existing callback token");
        BigClockSystemText.service(reused,7,true);BigClockSystemText.signal(reused,7,1);
        BigClockSystemText.clear();long afterClear=snapshot().subscriptionEpoch;
        check(afterClear>reused&&!BigClockSystemText.signal(reused,7,3),"clear invalidates callbacks even before another SIM is selected");
        BigClockSystemText.selectSubscription(-1);
        check(snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"missing active subscription is not guessed as physical SIM absence");
        check(BigClockSystemText.simAbsent(true)&&snapshot().cellularSignal.equals("无SIM卡"),"explicit native SIM absence is distinguished from unknown subscription");
        check(BigClockSystemText.airplane(true)&&snapshot().cellularSignal.equals("飞行模式"),"flight mode is explicit and has priority over signal/SIM presentation");
        BigClockSystemText.airplane(false);
        check(snapshot().cellularSignal.equals("无SIM卡"),"leaving flight mode preserves only the known SIM absence fact");
        long present=BigClockSystemText.selectSubscription(9);
        check(!Boolean.TRUE.equals(snapshot().simAbsent)&&snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"an actual newly selected data subscription clears obsolete SIM absence");
        BigClockSystemText.service(present,9,true);BigClockSystemText.signal(present,9,4);BigClockSystemText.airplane(true);
        check(snapshot().signalLevel==-1&&!BigClockSystemText.signal(present,9,4),"flight mode drops old bars and rejects late signal callbacks");
        BigClockSystemText.airplane(false);
        check(snapshot().cellularSignal.equals("蜂窝信号暂无数据"),"leaving flight mode never restores stale bars");
    }

    private static void networkAndPrivacy() {
        BigClockSystemText.clear();long token=BigClockSystemText.selectSubscription(3);
        BigClockSystemText.service(token,3,true);
        check(BigClockSystemText.network(BigClockSystemText.Transport.NONE,true)&&snapshot().networkStatus.equals("未联网"),"only explicit absent default route says offline");
        check(BigClockSystemText.network(BigClockSystemText.Transport.WIFI,null)&&snapshot().networkStatus.equals("Wi-Fi（联网状态未知）"),"known transport without validation keeps connectivity uncertainty");
        check(BigClockSystemText.network(BigClockSystemText.Transport.WIFI,false)&&snapshot().networkStatus.equals("Wi-Fi（受限）"),"unvalidated Wi-Fi is shown as limited");
        check(BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true)&&snapshot().networkStatus.equals("Wi-Fi"),"validated Wi-Fi stays short");
        BigClockSystemText.airplane(true);
        check(snapshot().networkStatus.equals("Wi-Fi"),"flight mode does not hide a still-valid Wi-Fi default route");
        BigClockSystemText.airplane(false);BigClockSystemText.service(token,3,true);
        BigClockSystemText.dataNetwork(token,3,"LTE");BigClockSystemText.network(BigClockSystemText.Transport.CELLULAR,true);
        check(snapshot().networkStatus.equals("4G"),"cellular transport combines with a recognised native RAT alias");
        BigClockSystemText.dataNetwork(token,3,"NR");
        check(snapshot().networkStatus.equals("5G"),"5G follows the current native RAT event");
        BigClockSystemText.dataNetwork(token,3,"5G NSA");
        check(snapshot().networkStatus.equals("5G NSA"),"recognised native enhanced RAT keeps its actual short generation label");
        for(String privateText:new String[]{"My private SSID","192.168.0.1","13800138000","carrier:123456","UNKNOWN",null}){
            BigClockSystemText.dataNetwork(token,3,privateText);
            check(snapshot().dataGeneration.isEmpty()&&snapshot().networkStatus.equals("蜂窝网络"),"unknown or identifying native text cannot enter the public generation token");
        }
        BigClockSystemText.network(BigClockSystemText.Transport.CELLULAR,false);
        check(snapshot().networkStatus.equals("蜂窝网络（受限）"),"missing RAT does not fabricate a generation on a limited cellular route");
        BigClockSystemText.network(BigClockSystemText.Transport.ETHERNET,true);
        check(snapshot().networkStatus.equals("以太网"),"other real transport types are distinct from offline");
        BigClockSystemText.network(BigClockSystemText.Transport.OTHER,true);
        check(snapshot().networkStatus.equals("其他网络"),"a connected unknown transport is not mislabeled as Wi-Fi or cellular");
        BigClockSystemText.network(BigClockSystemText.Transport.UNKNOWN,false);
        check(snapshot().networkStatus.equals("网络暂无数据")&&snapshot().validated==null,"unavailable capabilities do not become an invented limited network");
    }

    private static void batteryBoundaries() {
        BigClockSystemText.clear();
        check(BigClockSystemText.batteryRaw(85,100,3)&&snapshot().battery.equals("85%"),"normal battery percentage is short");
        check(BigClockSystemText.batteryRaw(85,100,2)&&snapshot().battery.equals("85% 充电中"),"charging status comes from its native event");
        check(BigClockSystemText.batteryRaw(100,100,5)&&snapshot().battery.equals("100% 已充满"),"full native status is distinct from merely plugged/charging");
        check(BigClockSystemText.batteryRaw(99,100,5)&&snapshot().battery.equals("99% 已充满"),"full status never fabricates a different reported percentage");
        check(BigClockSystemText.batteryRaw(85,100,4)&&snapshot().battery.equals("85% 未充电"),"plugged but not charging is not reported as charging");
        check(BigClockSystemText.batteryRaw(85,100,1)&&snapshot().battery.equals("85% 充电状态未知"),"unknown charging status is not guessed from percentage");
        BigClockSystemText.batteryRaw(0,100,3);
        check(snapshot().battery.equals("0%")&&snapshot().batteryPercent==0,"an explicit real zero percent remains valid");
        BigClockSystemText.batteryRaw(1,2,3);
        check(snapshot().battery.equals("50%"),"native level/scale is converted without assuming a scale of 100");
        BigClockSystemText.batteryRaw(Integer.MAX_VALUE,Integer.MAX_VALUE,3);
        check(snapshot().battery.equals("100%"),"large positive scales cannot overflow percentage math");
        int[][] invalid={{-1,100},{101,100},{0,0},{1,-1},{Integer.MAX_VALUE,100}};
        for(int[] event:invalid){BigClockSystemText.batteryRaw(event[0],event[1],2);
            check(snapshot().battery.equals("电池暂无数据")&&snapshot().batteryPercent==-1,"invalid or missing level/scale never clamps to a plausible percentage");}
    }

    private static void revisionsAndIsolation() {
        BigClockSystemText.clear();long token=BigClockSystemText.selectSubscription(5);
        BigClockSystemText.service(token,5,true);BigClockSystemText.signal(token,5,3);
        BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.batteryRaw(85,100,3);
        BigClockSystemText.Snapshot first=snapshot();
        for(int repeated=0;repeated<1000;repeated++){
            BigClockSystemText.signal(token,5,3);BigClockSystemText.service(token,5,true);
            BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.batteryRaw(170,200,3);
        }
        check(snapshot()==first&&snapshot().revision==first.revision,"duplicate native events and equal percentage scales do not replace snapshots or revisions");
        BigClockSystemText.batteryRaw(84,100,3);BigClockSystemText.Snapshot battery=snapshot();
        check(battery.revision==first.revision+1&&battery.batteryRevision==first.batteryRevision+1
                &&battery.cellularRevision==first.cellularRevision&&battery.networkRevision==first.networkRevision,"battery changes invalidate only battery-dependent slots");
        BigClockSystemText.network(BigClockSystemText.Transport.WIFI,false);BigClockSystemText.Snapshot network=snapshot();
        check(network.revision==battery.revision+1&&network.networkRevision==battery.networkRevision+1
                &&network.cellularRevision==battery.cellularRevision&&network.batteryRevision==battery.batteryRevision,"network changes invalidate only network-dependent slots");
        BigClockSystemText.signal(token,5,2);BigClockSystemText.Snapshot signal=snapshot();
        check(signal.revision==network.revision+1&&signal.cellularRevision==network.cellularRevision+1
                &&signal.networkRevision==network.networkRevision&&signal.batteryRevision==network.batteryRevision,"signal changes invalidate only signal-dependent slots");
        check(first.cellularSignal.equals("3/4格")&&first.networkStatus.equals("Wi-Fi")&&first.battery.equals("85%"),"earlier snapshots stay immutable after later mixed events");
        long last=snapshot().revision;
        for(int event=0;event<400;event++){
            switch(event%4){case 0:BigClockSystemText.batteryRaw(event%101,100,2);break;
                case 1:BigClockSystemText.signal(token,5,event%5);break;
                case 2:BigClockSystemText.network(BigClockSystemText.Transport.WIFI,event%3==0);break;
                default:BigClockSystemText.dataNetwork(token,5,event%2==0?"4G":"5G");break;}
            check(snapshot().revision>=last,"mixed event ordering cannot roll back the public revision");last=snapshot().revision;
        }
        BigClockSystemText.Snapshot beforeHidden=snapshot();BigClockSystemText.dataNetwork(token,5,"LTE+");
        check(snapshot().networkRevision==beforeHidden.networkRevision,"a hidden RAT change under Wi-Fi does not relayout a network text component");
        BigClockSystemText.network(BigClockSystemText.Transport.CELLULAR,true);
        check(snapshot().networkStatus.equals("4G+"),"the newest cached RAT becomes visible when that transport is selected");
        BigClockSystemText.clear();BigClockSystemText.Snapshot cleared=snapshot();BigClockSystemText.clear();
        check(snapshot().revision==cleared.revision&&snapshot().subscriptionEpoch>cleared.subscriptionEpoch,"empty reset invalidates old observers without creating fake display revisions");
    }

    public static void main(String[] args) {
        try{wifiSignalWithoutTextChange();wifiHiddenUnknownAndLifecycle();noDataAndNoWork();signalAndSubscription();networkAndPrivacy();batteryBoundaries();revisionsAndIsolation();}
        finally{BigClockSystemText.clear();}
        System.out.println("BigClockSystemTextCheck: "+checks+" checks passed (event cache only; device callbacks not executed)");
    }
}
