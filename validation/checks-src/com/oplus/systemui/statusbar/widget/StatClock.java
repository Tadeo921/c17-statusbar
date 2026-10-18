package com.oplus.systemui.statusbar.widget;
public class StatClock extends android.widget.TextView {
    /** The real status bar clock is 120 px tall; the checks of the size maths use an unbounded one unless they set this. */
    public int stripHeight=100000;
    public StatClock(android.content.Context context){super(context);}
    @Override public int getHeight(){return stripHeight;}
}
