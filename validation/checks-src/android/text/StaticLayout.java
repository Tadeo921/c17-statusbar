package android.text;

/** One measured single line, enough for the two-row clock's lower row. */
public class StaticLayout extends Layout {
    private final CharSequence text;
    private final TextPaint paint;
    private StaticLayout(CharSequence text, TextPaint paint, int width) {
        super(width, 0, paint.getTextSize(), Layout.getDesiredWidth(text, 0, text.length(), paint), text, paint);
        this.text = text; this.paint = paint;
    }
    public CharSequence textForCheck() { return text; }
    public TextPaint paintForCheck() { return paint; }
    public static final class Builder {
        private final CharSequence text; private final int start, end, width; private final TextPaint paint;
        private Builder(CharSequence text, int start, int end, TextPaint paint, int width) {
            this.text = text; this.start = start; this.end = end; this.paint = paint; this.width = width;
        }
        public static Builder obtain(CharSequence text, int start, int end, TextPaint paint, int width) { return new Builder(text, start, end, paint, width); }
        public Builder setAlignment(Layout.Alignment alignment) { return this; }
        public Builder setIncludePad(boolean include) { return this; }
        public Builder setMaxLines(int lines) { return this; }
        public StaticLayout build() { return new StaticLayout(text.subSequence(start, end), paint, width); }
    }
}
