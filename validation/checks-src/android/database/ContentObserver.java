package android.database;
public abstract class ContentObserver {
    public ContentObserver(android.os.Handler handler){}
    public void onChange(boolean selfChange){}
    public void onChange(boolean selfChange,android.net.Uri uri){onChange(selfChange);}
}
