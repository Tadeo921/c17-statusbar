package android.content;
public final class ContentUris {
    public static android.net.Uri withAppendedId(android.net.Uri base,long value){return android.net.Uri.withAppendedPath(base,Long.toString(value));}
}
