package android.appwidget;
public class AppWidgetManager {
    public static final AppWidgetManager INSTANCE=new AppWidgetManager();
    public final java.util.Map<Integer,AppWidgetProviderInfo> widgets=new java.util.HashMap<>();
    public static AppWidgetManager getInstance(android.content.Context context){return INSTANCE;}
    public AppWidgetProviderInfo getAppWidgetInfo(int id){return widgets.get(id);}
}
