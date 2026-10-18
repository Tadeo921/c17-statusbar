package com.oplus.systemui.media.seedling.livealert;

import android.os.Handler;
import android.os.Looper;

/** Replays the locally audited SystemUI five-second timeout/release R8 merge. */
public final class MediaLiveAlertController {
    private final Runnable statusBarTimeoutRunnable = new release$1(this, 1);
    private final Handler myHandler = new Handler(Looper.getMainLooper());
    public boolean showOnStatusBar = true;
    private boolean isMediaCardDestroy;
    private int mediaCurrentState = 1;
    private String lastStatusBarPlayerId = "player";
    private String timeStamp = "event";
    public int sends, releases;
    public boolean sendLiveAlertData(String method) { sends++; return true; }
    public Runnable timer() { return statusBarTimeoutRunnable; }
    public Runnable releaseTask() { return new release$1(this, 0); }
    public Handler handler() { return myHandler; }
    public void destroy() { isMediaCardDestroy = true; showOnStatusBar = false; timeStamp = "closed"; }
    public void closeEvent() { timeStamp = "manual-close"; }
    public void playAgain() { mediaCurrentState = 3; }
    public void playerChanged() { lastStatusBarPlayerId = "new-player"; }
    public static final class release$1 implements Runnable {
        public final int $r8$classId;
        public MediaLiveAlertController this$0;
        public release$1(MediaLiveAlertController source, int id) { this$0 = source; $r8$classId = id; }
        @Override public void run() {
            if ($r8$classId == 0) { this$0.releases++; this$0.destroy(); }
            else { this$0.showOnStatusBar = false; this$0.sendLiveAlertData("sendLiveAlert"); }
        }
    }
}
