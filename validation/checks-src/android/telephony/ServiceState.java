package android.telephony;
public class ServiceState {
    public static final int STATE_IN_SERVICE=0,STATE_OUT_OF_SERVICE=1,STATE_EMERGENCY_ONLY=2,STATE_POWER_OFF=3;
    private int state=STATE_OUT_OF_SERVICE;
    public final java.util.List<NetworkRegistrationInfo> registrations=new java.util.ArrayList<>();
    public int getState(){return state;}
    public void setState(int state){this.state=state;}
    public java.util.List<NetworkRegistrationInfo> getNetworkRegistrationInfoList(){return registrations;}
}
