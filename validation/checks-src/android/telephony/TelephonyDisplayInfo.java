package android.telephony;
public class TelephonyDisplayInfo {
    public static final int OVERRIDE_NETWORK_TYPE_NONE=0;
    private final int networkType,override;
    public TelephonyDisplayInfo(int networkType,int override){this.networkType=networkType;this.override=override;}
    public int getNetworkType(){return networkType;}
    public int getOverrideNetworkType(){return override;}
}
