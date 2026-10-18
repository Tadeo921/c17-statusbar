package android.view;
/** Mirrors the native passive ViewGroupOverlay contract. */
public class ViewGroupOverlay extends ViewOverlay {
    public final java.util.List<View> views=new java.util.ArrayList<>();
    ViewGroupOverlay(View host){super(host);}
    public void add(View view){if(!views.contains(view))views.add(view);}
    public void remove(View view){views.remove(view);}
}
