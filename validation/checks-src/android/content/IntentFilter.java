package android.content;
public class IntentFilter {
    public final java.util.Set<String> actions=new java.util.LinkedHashSet<>(),schemes=new java.util.LinkedHashSet<>();
    public IntentFilter(){}
    public IntentFilter(String action){addAction(action);}
    public void addAction(String action){actions.add(action);}
    public void addDataScheme(String scheme){schemes.add(scheme);}
}
