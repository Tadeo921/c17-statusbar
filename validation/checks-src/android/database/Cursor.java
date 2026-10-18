package android.database;
public interface Cursor extends java.io.Closeable {
    boolean moveToFirst();
    int getColumnIndex(String name);
    boolean isNull(int index);
    String getString(int index);
    int getInt(int index);
    long getLong(int index);
    @Override void close();
}
