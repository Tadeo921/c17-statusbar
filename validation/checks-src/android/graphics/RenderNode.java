package android.graphics;
public class RenderNode {
    public static final java.util.List<RenderNode> nodes=new java.util.ArrayList<>();
    public static boolean failBegin;
    public static boolean failEndOnce;
    public static String failBeginOnce;
    public final String name;
    public int width,height,left,top,recordings,discards,ends;
    public boolean clipToBounds;
    public float alpha=1f;
    public int alphaWrites;
    public boolean setAlpha(float value){alphaWrites++;alpha=value;return true;}
    public RecordingCanvas recording;
    public RenderEffect effect;
    public RenderEffect backgroundEffect;
    public static boolean rejectBackground;
    public int backgroundWrites;
    public boolean setBackgroundRenderEffect(RenderEffect value){backgroundWrites++;if(rejectBackground)return false;backgroundEffect=value;return true;}
    public RenderNode(String name) {this.name=name;nodes.add(this);}
    public boolean setPosition(int l,int t,int r,int b) {left=l;top=t;width=r-l;height=b-t;return true;}
    public boolean setClipToBounds(boolean value) {clipToBounds=value;return true;}
    public boolean setRenderEffect(RenderEffect effect) {this.effect=effect;return true;}
    public RecordingCanvas beginRecording(int w,int h) {
        if(failBegin)throw new IllegalStateException("recording unavailable");
        if(failBeginOnce!=null&&name.contains(failBeginOnce)){failBeginOnce=null;throw new IllegalStateException("recording unavailable");}
        recordings++;recording=new RecordingCanvas(w,h);return recording;
    }
    public void endRecording() {ends++;if(failEndOnce){failEndOnce=false;throw new IllegalStateException("end recording unavailable");}}
    public void discardDisplayList() {discards++;}
}
