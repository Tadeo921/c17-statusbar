package android.telephony;
public class NetworkRegistrationInfo {
    private final int transport;
    private final boolean registered;
    public NetworkRegistrationInfo(int transport,boolean registered){this.transport=transport;this.registered=registered;}
    public int getTransportType(){return transport;}
    public boolean isRegistered(){return registered;}
}
