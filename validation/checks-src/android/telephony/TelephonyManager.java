package android.telephony;
public class TelephonyManager {
    public static final int DATA_CONNECTED=2,DATA_SUSPENDED=3,NETWORK_TYPE_UNKNOWN=0,NETWORK_TYPE_IWLAN=18;
    public int registrations,unregistrations,serviceReads,signalReads,subscription=-1;
    public boolean dataEnabled=true;
    public int dataState=DATA_CONNECTED,networkType=20;
    public ServiceState service=new ServiceState();
    public SignalStrength signal=new SignalStrength(3);
    public TelephonyCallback callback;
    public TelephonyManager createForSubscriptionId(int sub){subscription=sub;return this;}
    public void registerTelephonyCallback(java.util.concurrent.Executor executor,TelephonyCallback callback){registrations++;this.callback=callback;}
    public void unregisterTelephonyCallback(TelephonyCallback callback){unregistrations++;if(this.callback==callback)this.callback=null;}
    public boolean isDataEnabled(){return dataEnabled;}
    public int getDataState(){return dataState;}
    public int getDataNetworkType(){return networkType;}
    public ServiceState getServiceState(){serviceReads++;return service;}
    public SignalStrength getSignalStrength(){signalReads++;return signal;}
}
