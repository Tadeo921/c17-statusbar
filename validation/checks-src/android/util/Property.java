package android.util;

/** Minimal real property contract used by the native X animator fixture. */
public abstract class Property<T,V> {
    private final Class<V> type;
    private final String name;
    public Property(Class<V> type,String name){this.type=type;this.name=name;}
    public String getName(){return name;}
    public Class<V> getType(){return type;}
    public abstract V get(T object);
    public void set(T object,V value){throw new UnsupportedOperationException();}
}
