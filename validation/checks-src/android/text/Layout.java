package android.text;

/** Stores measured layout metrics independently from subsequent direct Paint writes. */
public class Layout {
    private final int width, ellipsisCount;
    private final CharSequence text;
    private final TextPaint paint;
    private final int baseline;
    public final float measuredTextSize, glyphWidth;
    public Layout(int width, int ellipsisCount, float measuredTextSize, float glyphWidth) {
        this(width, ellipsisCount, measuredTextSize, glyphWidth, "", new TextPaint());
    }
    public Layout(int width, int ellipsisCount, float measuredTextSize, float glyphWidth, CharSequence text, TextPaint paint) {
        this(width,ellipsisCount,measuredTextSize,glyphWidth,text,paint,Math.round(measuredTextSize*.8f));
    }
    public Layout(int width,int ellipsisCount,float measuredTextSize,float glyphWidth,CharSequence text,TextPaint paint,int baseline) {
        this.width = width; this.ellipsisCount = ellipsisCount;
        this.text = text; this.paint = paint;
        this.baseline=baseline;
        this.measuredTextSize = measuredTextSize; this.glyphWidth = glyphWidth;
    }
    public CharSequence getText() { return text; }
    public TextPaint getPaint() { return paint; }
    public int getLineStart(int line) { return 0; }
    public int getLineVisibleEnd(int line) { return text.length(); }
    public int getWidth() { return width; }
    public int getEllipsizedWidth() { return width; }
    public int getLineCount() { return 1; }
    public int getEllipsisCount(int line) { return ellipsisCount; }
    public enum Alignment { ALIGN_NORMAL, ALIGN_OPPOSITE, ALIGN_CENTER }
    /** Row metrics for the status clock bridge; the first line spans the measured glyph width. */
    public float getLineLeft(int line) { return 0f; }
    public float getLineWidth(int line) { return glyphWidth; }
    public float getLineRight(int line) {return getLineLeft(line)+getLineWidth(line);}
    public int getLineAscent(int line){return Math.round(-measuredTextSize*.8f);}
    public int getLineDescent(int line){return Math.round(measuredTextSize*.2f);}
    public int getLineBaseline(int line) { return baseline; }
    private static final android.text.style.ReplacementSpan[] NO_SPANS=new android.text.style.ReplacementSpan[0];
    private android.text.style.ReplacementSpan[] drawSpans;
    private int[] drawStarts,drawEnds;
    private int drawRevision=Integer.MIN_VALUE;
    /** Native Layout does not allocate a new Java span tree for every glyph.
     * Retain measured ranges while still observing mutable span edits. */
    private void prepareDrawSpans(){
        if(!(text instanceof Spanned)){drawSpans=NO_SPANS;return;}
        int revision=text instanceof SpannableString?((SpannableString)text).spanRevisionForCheck():Integer.MIN_VALUE;
        if(drawSpans!=null&&text instanceof SpannableString&&revision==drawRevision)return;
        Spanned source=(Spanned)text;drawSpans=source.getSpans(0,text.length(),android.text.style.ReplacementSpan.class);
        drawStarts=new int[drawSpans.length];drawEnds=new int[drawSpans.length];drawRevision=revision;
        for(int i=0;i<drawSpans.length;i++){drawStarts[i]=source.getSpanStart(drawSpans[i]);drawEnds[i]=source.getSpanEnd(drawSpans[i]);}
    }
    public int drawCalls;
    public void draw(android.graphics.Canvas canvas) {
        drawCalls++;float x=getLineLeft(0);int baseline=getLineBaseline(0);
        prepareDrawSpans();
        if(drawSpans.length==0){if(text.length()>0)canvas.drawText(text,0,text.length(),x,baseline,paint);return;}
        for(int i=0;i<text.length();) {
            android.text.style.ReplacementSpan selected=null;int end=i+1;
            for(int span=0;span<drawSpans.length;span++)if(drawStarts[span]==i){selected=drawSpans[span];end=drawEnds[span];break;}
            if(selected!=null){selected.draw(canvas,text,i,end,x,0,baseline,Math.round(measuredTextSize),paint);x+=selected.getSize(paint,text,i,end,null);}
            else {canvas.drawText(text,i,end,x,baseline,paint);x+=paint.measureText(text,i,end);}
            i=end;
        }
    }
    /** Spans replace their characters; everything else is measured by the paint, like the framework. */
    public static float getDesiredWidth(CharSequence source, int start, int end, TextPaint paint) {
        android.text.style.ReplacementSpan[] spans = source instanceof Spanned
                ? ((Spanned) source).getSpans(start, end, android.text.style.ReplacementSpan.class)
                : new android.text.style.ReplacementSpan[0];
        float width = 0f;
        for (int i = start; i < end;) {
            android.text.style.ReplacementSpan match = null; int spanEnd = i + 1;
            for (android.text.style.ReplacementSpan span : spans)
                if (((Spanned) source).getSpanStart(span) == i) { match = span; spanEnd = ((Spanned) source).getSpanEnd(span); break; }
            width += match == null ? paint.measureText(source, i, i + 1) : match.getSize(paint, source, i, spanEnd, null);
            i = spanEnd;
        }
        return width;
    }
}
