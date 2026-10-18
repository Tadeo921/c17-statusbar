package android.telephony;
public class TelephonyCallback {
    public interface DisplayInfoListener { void onDisplayInfoChanged(TelephonyDisplayInfo info); }
    public interface DataConnectionStateListener { void onDataConnectionStateChanged(int state,int networkType); }
    public interface ServiceStateListener { void onServiceStateChanged(ServiceState state); }
    public interface SignalStrengthsListener { void onSignalStrengthsChanged(SignalStrength strength); }
}
