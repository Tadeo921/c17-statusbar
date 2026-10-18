package android.appwidget;
public class AppWidgetHost {
    public static int nextId=10;
    public static final java.util.Set<Integer> allocated=new java.util.HashSet<>(),deleted=new java.util.HashSet<>(),failDelete=new java.util.HashSet<>();
    public static final java.util.List<Integer> retiredHosts=new java.util.ArrayList<>();
    public static boolean failRetire;
    private final int hostId;
    public AppWidgetHost(android.content.Context context,int hostId) {this.hostId=hostId;}
    public void deleteHost(){if(failRetire)throw new IllegalStateException("Synthetic host retirement failure");retiredHosts.add(hostId);}
    public int allocateAppWidgetId(){int id=nextId++;allocated.add(id);return id;}
    public void deleteAppWidgetId(int id){if(failDelete.contains(id))throw new IllegalStateException("Synthetic widget service delete failure");allocated.remove(id);deleted.add(id);AppWidgetManager.INSTANCE.widgets.remove(id);}
    public int[] getAppWidgetIds(){return allocated.stream().mapToInt(Integer::intValue).toArray();}
    public void onAppWidgetRemoved(int id) { }
    protected void onProvidersChanged() { }
    protected void clearViews() { }
}
