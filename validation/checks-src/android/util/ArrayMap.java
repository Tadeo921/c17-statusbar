package android.util;
/** JVM implementation of the Map contract used by native renderer attribute caches. */
public class ArrayMap<K,V> extends java.util.LinkedHashMap<K,V> {
    public ArrayMap(){super();}
    public ArrayMap(int capacity){super(capacity);}
    public ArrayMap(java.util.Map<? extends K,? extends V> values){super(values);}
    public K keyAt(int index){return new java.util.ArrayList<K>(keySet()).get(index);}
    public V valueAt(int index){return get(keyAt(index));}
    public V setValueAt(int index,V value){return put(keyAt(index),value);}
    public V removeAt(int index){return remove(keyAt(index));}
    public int indexOfKey(Object key){return new java.util.ArrayList<K>(keySet()).indexOf(key);}
    public int indexOfValue(Object value){return new java.util.ArrayList<V>(values()).indexOf(value);}
    public void append(K key,V value){put(key,value);}
    public void putAll(ArrayMap<? extends K,? extends V> values){super.putAll(values);}
}
