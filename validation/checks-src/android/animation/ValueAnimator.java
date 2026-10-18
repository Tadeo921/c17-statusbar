package android.animation;

/** Deterministic animator retaining identity, time and native listener ownership. */
public class ValueAnimator {
    public static final int INFINITE=-1,RESTART=1,REVERSE=2;
    public static boolean areAnimatorsEnabled(){return true;}
    protected PropertyValuesHolder[] values=new PropertyValuesHolder[0];
    private long playTime,duration=1000,startDelay;
    private boolean running,started,paused;
    private int repeatCount,repeatMode=RESTART;
    public int startsForCheck,cancelsForCheck,seeksForCheck;
    public Runnable seekCallbackForCheck;
    public PropertyValuesHolder[] getValues(){return values;}
    public void setValues(PropertyValuesHolder... values){this.values=values;}
    public boolean isRunning(){return running;}
    public boolean isStarted(){return started;}
    public boolean isPaused(){return paused;}
    public long getCurrentPlayTime(){return playTime;}
    public long getDuration(){return duration;}
    public ValueAnimator setDuration(long value){duration=value;return this;}
    public long getStartDelay(){return startDelay;}
    public void setStartDelay(long value){startDelay=value;}
    public int getRepeatCount(){return repeatCount;}
    public void setRepeatCount(int value){repeatCount=value;}
    public int getRepeatMode(){return repeatMode;}
    public void setRepeatMode(int value){repeatMode=value;}
    public float getAnimatedFraction(){return duration==0?1:Math.max(0,Math.min(1,(float)playTime/duration));}
    public void start(){startsForCheck++;started=true;running=startDelay==0;}
    public void cancel(){cancelsForCheck++;running=started=false;}
    public void pause(){paused=true;}
    public void resume(){paused=false;}
    public void setCurrentPlayTime(long value){playTime=value;seeksForCheck++;applyForCheck();if(seekCallbackForCheck!=null)seekCallbackForCheck.run();}
    protected void applyForCheck(){ }
}
