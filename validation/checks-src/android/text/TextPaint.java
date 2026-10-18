package android.text;
/** Only the real TextView.getPaint return type is needed by the native clock bridge checks. */
public class TextPaint extends android.graphics.Paint {
    public int baselineShift;
    public void set(TextPaint source){super.set(source);baselineShift=source.baselineShift;}
    public TextPaint(){super(0);}
    public TextPaint(int flags){super(flags);}
    public TextPaint(android.graphics.Paint source){super(0);set(source);}
}
