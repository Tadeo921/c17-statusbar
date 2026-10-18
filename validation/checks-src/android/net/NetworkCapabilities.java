package android.net;
public class NetworkCapabilities {
    public static final int TRANSPORT_CELLULAR=0,TRANSPORT_WIFI=1,TRANSPORT_ETHERNET=3,TRANSPORT_VPN=4;
    public static final int NET_CAPABILITY_VALIDATED=16;
    private final java.util.Set<Integer> transports=new java.util.HashSet<>(),capabilities=new java.util.HashSet<>();
    public NetworkCapabilities addTransportType(int type){transports.add(type);return this;}
    public NetworkCapabilities addCapability(int capability){capabilities.add(capability);return this;}
    public boolean hasTransport(int type){return transports.contains(type);}
    public boolean hasCapability(int capability){return capabilities.contains(capability);}
}
