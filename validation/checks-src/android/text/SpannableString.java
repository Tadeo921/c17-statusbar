package android.text;
import java.lang.reflect.Array;
import java.util.ArrayList;
/** Immutable characters with mutable span ranges, matching the subset used by clock digit cells. */
public class SpannableString implements Spanned {
    private final String text;
    private final ArrayList<Range> spans=new ArrayList<>();
    private int spanRevision;
    public int spanRevisionForCheck(){return spanRevision;}
    private static final class Range {
        final Object span;final int start,end,flags;
        Range(Object span,int start,int end,int flags){this.span=span;this.start=start;this.end=end;this.flags=flags;}
    }
    public SpannableString(CharSequence source){
        text=source.toString();
        if(source instanceof Spanned){Spanned nativeText=(Spanned)source;
            for(Object span:nativeText.getSpans(0,source.length(),Object.class))
                setSpan(span,nativeText.getSpanStart(span),nativeText.getSpanEnd(span),nativeText.getSpanFlags(span));
        }
    }
    public void setSpan(Object span,int start,int end,int flags){
        if(start<0||end<start||end>text.length())throw new IndexOutOfBoundsException();
        spans.add(new Range(span,start,end,flags));spanRevision++;
    }
    @SuppressWarnings("unchecked") public <T>T[] getSpans(int start,int end,Class<T> type){
        ArrayList<T> result=new ArrayList<>();
        for(int i=0;i<spans.size();i++){Range range=spans.get(i);if(type.isInstance(range.span)&&range.start<end&&range.end>start)result.add(type.cast(range.span));}
        return result.toArray((T[])Array.newInstance(type,result.size()));
    }
    public int getSpanStart(Object span){for(int i=0;i<spans.size();i++){Range range=spans.get(i);if(range.span==span)return range.start;}return -1;}
    public int getSpanEnd(Object span){for(int i=0;i<spans.size();i++){Range range=spans.get(i);if(range.span==span)return range.end;}return -1;}
    public int getSpanFlags(Object span){for(int i=0;i<spans.size();i++){Range range=spans.get(i);if(range.span==span)return range.flags;}return 0;}
    public int length(){return text.length();}
    public char charAt(int index){return text.charAt(index);}
    public CharSequence subSequence(int start,int end){
        SpannableString result=new SpannableString(text.substring(start,end));
        for(Range range:spans)if(range.start<end&&range.end>start)
            result.setSpan(range.span,Math.max(start,range.start)-start,Math.min(end,range.end)-start,range.flags);
        return result;
    }
    public String toString(){return text;}
}
