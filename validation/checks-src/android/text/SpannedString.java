package android.text;

/** Immutable NORMAL TextView buffer: preserve ranges, expose no span mutation. */
public final class SpannedString implements Spanned {
    private final SpannableString value;
    public SpannedString(CharSequence source){value=new SpannableString(source);}
    public int length(){return value.length();}public char charAt(int index){return value.charAt(index);}
    public CharSequence subSequence(int start,int end){return new SpannedString(value.subSequence(start,end));}
    public String toString(){return value.toString();}
    public <T>T[] getSpans(int start,int end,Class<T> type){return value.getSpans(start,end,type);}
    public int getSpanStart(Object span){return value.getSpanStart(span);}public int getSpanEnd(Object span){return value.getSpanEnd(span);}
    public int getSpanFlags(Object span){return value.getSpanFlags(span);}
}
