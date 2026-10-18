// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Whether any media session is playing right now. It runs only while the fluid-cloud follow policy asks
 * for it ("follow only while playing"); otherwise no listener or session callback exists. SystemUI holds
 * the media-control permission, so no notification-listener component is needed. If the platform refuses,
 * the answer stays "playing" and following behaves exactly as without this option. */
final class MediaPlaybackObserver {
    interface Listener { void playing(boolean playing); }

    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<MediaController, MediaController.Callback> controllers = new IdentityHashMap<>();
    private MediaSessionManager manager;
    private boolean observing, unavailableLogged;
    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::sessionsChanged;

    MediaPlaybackObserver(Listener listener) { this.listener = listener; }

    /** Start or stop observing. Safe to call on every settings apply. */
    void configure(Context context, boolean wanted) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(() -> configure(context, wanted)); return; }
        if (wanted == observing) return;
        if (!wanted) { stop(); listener.playing(true); return; }
        if (context == null) return;
        try {
            manager = context.getSystemService(MediaSessionManager.class);
            if (manager == null) throw new IllegalStateException("MediaSessionManager unavailable");
            manager.addOnActiveSessionsChangedListener(sessions, null, main);
            observing = true;
            sessionsChanged(manager.getActiveSessions(null));
        } catch (RuntimeException | LinkageError unavailable) {
            stop();
            if (!unavailableLogged) {
                unavailableLogged = true;
                ModuleDiagnostics.error("fluid", "Media playback state unavailable; following does not wait for playback", unavailable);
            }
            listener.playing(true);
        }
    }

    private void sessionsChanged(List<MediaController> active) {
        if (!observing) return;
        List<MediaController> next = active == null ? new ArrayList<MediaController>() : active;
        for (MediaController controller : new ArrayList<>(controllers.keySet())) {
            if (!containsIdentity(next, controller)) {
                MediaController.Callback callback = controllers.remove(controller);
                if (callback != null) try { controller.unregisterCallback(callback); } catch (RuntimeException ignored) { }
            }
        }
        for (MediaController controller : next) {
            if (controller == null || controllers.containsKey(controller)) continue;
            MediaController.Callback callback = new MediaController.Callback() {
                @Override public void onPlaybackStateChanged(PlaybackState state) { publish(); }
                @Override public void onSessionDestroyed() { publish(); }
            };
            try { controller.registerCallback(callback, main); controllers.put(controller, callback); }
            catch (RuntimeException ignored) { }
        }
        publish();
    }

    private static boolean containsIdentity(List<MediaController> list, MediaController controller) {
        for (MediaController candidate : list) if (candidate == controller) return true;
        return false;
    }

    private void publish() {
        boolean playing = false;
        for (MediaController controller : controllers.keySet()) {
            PlaybackState state = controller.getPlaybackState();
            if (state != null && active(state.getState())) { playing = true; break; }
        }
        listener.playing(playing);
    }

    /** Buffering and seeking count as playing: the track is still the one being heard. */
    static boolean active(int state) {
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                || state == PlaybackState.STATE_CONNECTING || state == PlaybackState.STATE_FAST_FORWARDING
                || state == PlaybackState.STATE_REWINDING || state == PlaybackState.STATE_SKIPPING_TO_NEXT
                || state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS || state == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
    }

    private void stop() {
        if (manager != null) try { manager.removeOnActiveSessionsChangedListener(sessions); } catch (RuntimeException ignored) { }
        for (Map.Entry<MediaController, MediaController.Callback> entry : controllers.entrySet())
            try { entry.getKey().unregisterCallback(entry.getValue()); } catch (RuntimeException ignored) { }
        controllers.clear();
        observing = false;
    }
}
