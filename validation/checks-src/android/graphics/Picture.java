package android.graphics;
/** Vector recording fixture. Copying shares immutable commands, not a mutable drawable. */
public class Picture {
    private int width,height;private final Object commands;
    public Picture(){commands=new Object();}
    public Picture(Picture source){width=source.width;height=source.height;commands=source.commands;}
    public Canvas beginRecording(int width,int height){this.width=width;this.height=height;return new Canvas();}
    public void endRecording(){}
    public int getWidth(){return width;}public int getHeight(){return height;}
    public Object commandsForCheck(){return commands;}
}
