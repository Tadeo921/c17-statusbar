package android.content;
public class ContentResolver {
    public android.database.Cursor query(android.net.Uri uri,String[] projection,String selection,String[] args,String sort){return null;}
    public int notifications;
    public void notifyChange(android.net.Uri uri,android.database.ContentObserver observer){notifications++;}
    public android.os.Bundle call(android.net.Uri uri,String method,String argument,android.os.Bundle extras){return null;}
    public final java.util.List<android.database.ContentObserver> observers=new java.util.ArrayList<>();
    public int observerUser;
    public void registerContentObserver(android.net.Uri uri,boolean descendants,android.database.ContentObserver observer){observers.add(observer);}
    public void registerContentObserver(android.net.Uri uri,boolean descendants,android.database.ContentObserver observer,int user){observerUser=user;registerContentObserver(uri,descendants,observer);}
    public void unregisterContentObserver(android.database.ContentObserver observer){observers.removeIf(value->value==observer);}
    public void dispatchSettingsChange(){for(android.database.ContentObserver observer:new java.util.ArrayList<>(observers))observer.onChange(false);}
    public java.io.InputStream openInputStream(android.net.Uri uri)throws java.io.FileNotFoundException{throw new java.io.FileNotFoundException(uri.toString());}
    public android.os.ParcelFileDescriptor openFileDescriptor(android.net.Uri uri,String mode)throws java.io.FileNotFoundException{throw new java.io.FileNotFoundException(uri.toString());}
}
