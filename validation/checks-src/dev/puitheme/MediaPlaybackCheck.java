package dev.puitheme;

import android.media.session.PlaybackState;
import java.util.Objects;

/** Which media states count as playing for the follow-only-while-playing option. */
public final class MediaPlaybackCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Playback check "+checks+": "+expected+" != "+actual);}
    public static void main(String[] args) {
        for(int state:new int[]{PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING,PlaybackState.STATE_FAST_FORWARDING,
                PlaybackState.STATE_REWINDING,PlaybackState.STATE_SKIPPING_TO_NEXT,PlaybackState.STATE_SKIPPING_TO_PREVIOUS,PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM})
            equal(true,MediaPlaybackObserver.active(state));
        for(int state:new int[]{PlaybackState.STATE_NONE,PlaybackState.STATE_STOPPED,PlaybackState.STATE_PAUSED,PlaybackState.STATE_ERROR})
            equal(false,MediaPlaybackObserver.active(state));
        // The fluid-cloud policy only waits for playback when the option is on and following is active at all.
        FluidCloudStyle style=new FluidCloudStyle();
        equal(false,style.wantsPlaybackState());
        android.os.Bundle on=new android.os.Bundle();on.putBoolean(FluidCloudAccent.ENABLED,true);on.putBoolean(FluidCloudStyle.FOLLOW_MEDIA,true);
        on.putBoolean(FluidCloudStyle.MASTER,true);on.putBoolean(FluidCloudStyle.DESKTOP_BACKGROUND,true); // following needs the custom base colour (code100)
        style.configure(on);equal(true,style.wantsPlaybackState());
        on.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,false);style.configure(on);equal(false,style.wantsPlaybackState());
        on.putBoolean(FluidCloudStyle.FOLLOW_ONLY_PLAYING,true);on.putBoolean(FluidCloudAccent.ENABLED,false);style.configure(on);
        equal(false,style.wantsPlaybackState());
        style.release();
        System.out.println("MediaPlaybackCheck: "+checks+" checks passed");
    }
}
