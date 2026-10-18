package j6;
/** Native lazy implementation's b is its volatile value. Calling a() would
 * invoke its initializer, which cache seeding must never do. */
public final class f implements b {
    public volatile Object b;
    public int getterCalls,initializations;
    private final java.util.function.Supplier<Object> initializer;
    public f(Object value){b=value;initializer=null;}
    public f(java.util.function.Supplier<Object> value){b=new Object();initializer=value;}
    public Object a(){getterCalls++;if(initializer!=null){initializations++;b=initializer.get();}return b;}
}
