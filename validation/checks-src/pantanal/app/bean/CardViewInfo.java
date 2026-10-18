package pantanal.app.bean;
public final class CardViewInfo {
    private final String serviceId;private final Entrance entrance;private boolean isLocalCard;
    private String packageName="com.coloros.weather2",cardUiData,initData,instantUri;
    public CardViewInfo(String service,Entrance entrance,boolean local){serviceId=service;this.entrance=entrance;isLocalCard=local;}
    public String getServiceId(){throw new AssertionError("read verified cache fields only");}
    public void cachedStrings(String data,String initial,String uri){cardUiData=data;initData=initial;instantUri=uri;}
    public void packageName(String value){packageName=value;}
}
