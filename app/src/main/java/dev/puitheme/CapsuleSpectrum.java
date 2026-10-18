// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** The music spectrum of the fluid cloud (the camera-area capsule and the expanded media card): the plugin plays a canned
 * six-bar Lottie ("voice print", raw resources voice_print_dark and voice_print_dark_15hz). While this is on, those views show six
 * bars of their own instead, driven by the real audio the phone is playing.
 *
 * Power: the audio is only looked at while a spectrum is on screen and something is playing. The platform's Visualizer on the
 * output mix runs at about 16 reads a second with a 512-point FFT on a thread of its own, hands six numbers to the bars and asks the
 * view to redraw; it is closed the moment the last bar is hidden, the screen goes off, or about three seconds of silence pass (a cheap
 * "is music active" check then watches for the next sound). Nothing runs for a spectrum that is not shown.
 *
 * The plugin's own animation is put back when the setting goes off, when the platform refuses the audio capture, or when the view
 * is given another animation. Identification never uses a number or an obfuscated class name: the plugin hands the animation to
 * the view as JSON text (read from the raw resource, a view never sees the resource itself), recognised by its shape (72 x 72,
 * six bar layers); a view that is given the raw resource directly is recognised by the resource's entry name. */
public final class CapsuleSpectrum {
    public static final String VIEW = "com.oplus.systemui.plugins.shared.template.component.view.CustomLottieView";
    /** The plugin's canned spectrum animations (raw resource entry names). */
    static final String[] RAW_NAMES = {"voice_print_dark", "voice_print_dark_15hz"};
    public static final int BARS = 6;
    /** Geometry of the plugin's animation, in its own 72 x 72 units: bar width, resting and fullest height, bar centres, middle line. */
    static final float UNITS = 72f, BAR_WIDTH = 6f, REST_HEIGHT = 6f, FULL_HEIGHT = 43f, MIDDLE = 35.806f;
    static final float[] CENTRES = {9.831f, 20.008f, 30.435f, 40.737f, 51.633f, 62.018f};

    /** Where the six levels come from. The real one is the platform's audio visualizer; checks bring their own. */
    interface Feed {
        /** False once the platform has refused the capture; the plugin's animation stays or comes back then. */
        boolean available();
        /** One more spectrum is on screen: start looking at the audio. {@code changed} runs when the levels move (any thread). */
        void acquire(Context context, Runnable changed);
        void release(Runnable changed);
        /** BARS values, 0 (rest) to 1 (full). */
        float[] levels();
        /** 0..100 */
        void sensitivity(float percent);
        /** Called once when the capture fails for good. */
        void onFailure(Runnable failure);
    }

    private final Feed feed;
    private final Supplier<Integer> accent;
    private volatile int barColor = FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR;
    private volatile boolean follow;
    private final Map<View, Binding> bindings = Collections.synchronizedMap(new WeakHashMap<>());
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean enabled, released;
    private boolean swapping;
    private volatile Hooks hooks;

    public CapsuleSpectrum() { this(new SpectrumFeed(), null); }
    /** {@code accent}: the colour of the moment for bars that follow the accent (RGB; null while there is none). */
    public CapsuleSpectrum(Supplier<Integer> accent) { this(new SpectrumFeed(), accent); }
    CapsuleSpectrum(Feed feed) { this(feed, null); }
    CapsuleSpectrum(Feed feed, Supplier<Integer> accent) {
        this.feed = feed;
        this.accent = accent;
        feed.onFailure(() -> main.post(this::restoreAll));
    }

    private static final class Binding {
        Drawable original;
        Bars bars;
    }

    /** What the hooks need, resolved by shape from the plugin's own class. */
    public static final class Hooks {
        public final Method setAnimation, setImageDrawable;
        /** The methods that hand a Lottie view its animation as JSON text: setAnimationFromJson(String) and the (String, String)
         * method it forwards to (JSON, then a cache key), found on the Lottie base class by shape. */
        public final Method[] json;
        public Hooks(ClassLoader loader) throws ReflectiveOperationException {
            Class<?> type = Class.forName(VIEW, false, loader);
            if (!ImageView.class.isAssignableFrom(type)) throw new NoSuchMethodException(VIEW + " is not an ImageView");
            setAnimation = type.getMethod("setAnimation", int.class);
            setImageDrawable = type.getMethod("setImageDrawable", Drawable.class);
            Method fromJson = type.getMethod("setAnimationFromJson", String.class);
            ArrayList<Method> setters = new ArrayList<>();
            setters.add(fromJson);
            for (Method method : fromJson.getDeclaringClass().getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()) && parameters.length == 2
                        && parameters[0] == String.class && parameters[1] == String.class && method.getReturnType() == void.class)
                    setters.add(method);
            }
            json = setters.toArray(new Method[0]);
            setAnimation.setAccessible(true); setImageDrawable.setAccessible(true);
            for (Method method : json) method.setAccessible(true);
        }
    }

    public Hooks resolve(ClassLoader loader) {
        if (loader == null || released) return null;
        try { Hooks found = new Hooks(loader); hooks = found; return found; }
        catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            ModuleDiagnostics.info("fluid", "Spectrum contract unavailable; the plugin's canned spectrum stays");
            return null;
        }
    }

    public boolean active() { return enabled && !released && feed.available(); }

    public void configure(Bundle settings) {
        if (released) return;
        boolean next = settings != null && !SafetyMode.enabled(settings) && settings.getBoolean(FluidCloudCapsule.MASTER, false)
                && settings.getBoolean(FluidCloudCapsule.SPECTRUM_ENABLED, false);
        Object value = settings == null ? null : settings.get(FluidCloudCapsule.SPECTRUM_SENSITIVITY);
        float sensitivity = value instanceof Number ? ((Number) value).floatValue() : FluidCloudCapsule.DEFAULT_SPECTRUM_SENSITIVITY;
        if (!Float.isFinite(sensitivity)) sensitivity = FluidCloudCapsule.DEFAULT_SPECTRUM_SENSITIVITY;
        feed.sensitivity(Math.max(0f, Math.min(100f, sensitivity)));
        int colour = FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR;
        if (settings != null && settings.get(FluidCloudCapsule.SPECTRUM_COLOR) instanceof Number) {
            colour = ((Number) settings.get(FluidCloudCapsule.SPECTRUM_COLOR)).intValue();
            Object custom = settings.get(StatusBarSettings.alphaKey(FluidCloudCapsule.SPECTRUM_COLOR));
            if (!(custom instanceof Boolean ? (Boolean) custom : (colour >>> 24) != 255)) colour |= 0xff000000;
        }
        boolean nextFollow = settings != null && settings.getBoolean(FluidCloudCapsule.SPECTRUM_FOLLOW, false);
        boolean recoloured = colour != barColor || nextFollow != follow;
        barColor = colour;
        follow = nextFollow;
        if (recoloured) repaintAll();
        if (next == enabled) return;
        enabled = next;
        Runnable apply = this::refreshAll;
        if (Looper.myLooper() == Looper.getMainLooper()) apply.run(); else main.post(apply);
    }

    /** After the plugin gave a Lottie view its animation as JSON text: is it the spectrum? */
    public void animationJson(View view, String json) {
        if (released || !(view instanceof ImageView) || json == null) return;
        if (isVoicePrintJson(json)) {
            synchronized (bindings) { if (!bindings.containsKey(view)) bindings.put(view, new Binding()); }
            Drawable current = ((ImageView) view).getDrawable();
            if (current != null && !(current instanceof Bars)) drawableSet(view, current);
        } else if (json.length() > 64 && json.charAt(0) == '{') {
            // Another animation of the same view.
            Binding other = bindings.remove(view);
            if (other != null) restore(view, other);
        }
    }

    /** The composition the view has loaded, by what the Lottie base class's public methods report: 72 x 72 and about two seconds
     * (the two spectrum files run 2.07 and 2.03 s; the plugin's other animations are 69 x 69 and under a second). */
    static boolean voicePrintComposition(View view) {
        try {
            Method composition = view.getClass().getMethod("getComposition");
            Object loaded = composition.invoke(view);
            if (loaded == null) return false;
            Rect bounds = null;
            for (Method method : loaded.getClass().getMethods())
                if (method.getParameterCount() == 0 && method.getReturnType() == Rect.class) { bounds = (Rect) method.invoke(loaded); break; }
            if (bounds == null || bounds.width() != 72 || bounds.height() != 72) return false;
            long duration = ((Number) view.getClass().getMethod("getDuration").invoke(view)).longValue();
            return duration >= 1900L && duration <= 2200L;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError notALottieView) { return false; }
    }

    /** Whether this view shows the spectrum: our bars, or the plugin's own voice print. */
    static boolean showsSpectrum(View view) {
        return view instanceof ImageView && (((ImageView) view).getDrawable() instanceof Bars || voicePrintComposition(view));
    }

    /** The plugin's six-bar animation: a 72 x 72 composition with the layers "Shape Layer 7" to "Shape Layer 12". */
    static boolean isVoicePrintJson(String json) {
        return json.length() > 1000 && json.length() < 40000 && json.contains("\"w\":72,\"h\":72") && json.contains("\"Shape Layer 12\"")
                && json.contains("\"Shape Layer 7\"");
    }

    /** After the plugin gave a Lottie view an animation by raw resource: is it the spectrum? */
    public void animationSet(View view, int resource) {
        if (released || !(view instanceof ImageView)) return;
        if (isVoicePrint(view, resource)) {
            synchronized (bindings) { if (!bindings.containsKey(view)) bindings.put(view, new Binding()); }
            // A cached animation is delivered before setAnimation returns: its drawable was set before this view was known.
            Drawable current = ((ImageView) view).getDrawable();
            if (current != null && !(current instanceof Bars)) drawableSet(view, current);
        } else {
            Binding other = bindings.remove(view);
            if (other != null) restore(view, other);
        }
    }

    /** After the plugin set the drawable of a Lottie view: a spectrum view takes the bars instead. */
    public void drawableSet(View view, Drawable drawable) {
        if (released || swapping || drawable == null || drawable instanceof Bars) return;
        Binding binding = bindings.get(view);
        // However the plugin delivered the animation (resource, JSON text or a ready composition), the loaded composition tells.
        if (binding == null && enabled && view instanceof ImageView && voicePrintComposition(view)) {
            binding = new Binding();
            synchronized (bindings) { bindings.put(view, binding); }
        }
        if (binding == null) return;
        binding.original = drawable;
        apply((ImageView) view, binding);
    }

    static boolean isVoicePrint(View view, int resource) {
        if (resource == 0) return false;
        try {
            String name = view.getResources().getResourceEntryName(resource);
            for (String raw : RAW_NAMES) if (raw.equals(name)) return true;
        } catch (RuntimeException notAResource) { }
        return false;
    }

    /** The colour of the bars is read at every draw; a changed colour only needs the views to draw again. */
    private void repaintAll() {
        final ArrayList<View> views;
        synchronized (bindings) { views = new ArrayList<>(bindings.keySet()); }
        for (View view : views) if (view != null) view.postInvalidate();
    }

    /** The colour the bars are drawn in at this instant: the media colour when they follow it and there is one (with the alpha of the
     * chosen colour), else the chosen colour. */
    int barColor() {
        int chosen = barColor;
        if (follow) {
            Supplier<Integer> source = accent;
            Integer media = source == null ? null : source.get();
            if (media != null) return (chosen & 0xff000000) | (media & 0x00ffffff);
        }
        return chosen;
    }

    /** A media colour raised to a brightness that shows on the capsule's black: the hue stays, the brightest channel reaches 0xd8. */
    static int visible(int argb) {
        int red = (argb >> 16) & 0xff, green = (argb >> 8) & 0xff, blue = argb & 0xff, top = Math.max(red, Math.max(green, blue));
        if (top == 0) return 0xffffff;
        float scale = top < 0xd8 ? 0xd8 / (float) top : 1f;
        return Math.min(255, Math.round(red * scale)) << 16 | Math.min(255, Math.round(green * scale)) << 8 | Math.min(255, Math.round(blue * scale));
    }

    private void refreshAll() {
        final ArrayList<Map.Entry<View, Binding>> all;
        synchronized (bindings) { all = new ArrayList<>(bindings.entrySet()); }
        for (Map.Entry<View, Binding> entry : all) {
            View view = entry.getKey();
            if (view instanceof ImageView && entry.getValue().original != null) apply((ImageView) view, entry.getValue());
        }
    }

    private void restoreAll() {
        final ArrayList<Map.Entry<View, Binding>> all;
        synchronized (bindings) { all = new ArrayList<>(bindings.entrySet()); }
        for (Map.Entry<View, Binding> entry : all) if (entry.getKey() != null) restore(entry.getKey(), entry.getValue());
    }

    private void apply(ImageView view, Binding binding) {
        if (active()) {
            if (binding.bars == null) binding.bars = new Bars(view, feed, this::barColor);
            swap(view, binding.bars);
        } else restore(view, binding);
    }

    private void restore(View view, Binding binding) {
        if (!(view instanceof ImageView) || binding.original == null || binding.bars == null) return;
        if (((ImageView) view).getDrawable() == binding.bars) swap((ImageView) view, binding.original);
    }

    private void swap(ImageView view, Drawable drawable) {
        if (view.getDrawable() == drawable) return;
        boolean previous = swapping;
        swapping = true;
        try { view.setImageDrawable(drawable); }
        catch (RuntimeException unavailable) { ModuleDiagnostics.info("fluid", "Spectrum could not be swapped in"); }
        finally { swapping = previous; }
    }

    /** Everything back to the plugin's animation, and no more watching. */
    public void release() {
        if (released) return;
        enabled = false; released = true;
        Runnable restore = () -> {
            restoreAll();
            bindings.clear(); hooks = null;
        };
        if (Looper.myLooper() == Looper.getMainLooper()) restore.run(); else main.post(restore);
    }

    /** The bars: six rounded bars where the plugin's animation had them, over the drawable's bounds. Visible means looking at the audio. */
    static final class Bars extends Drawable {
        private final View owner;
        private final Feed feed;
        private final java.util.function.IntSupplier colour;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Runnable changed;
        private boolean watching;

        Bars(View owner, Feed feed, java.util.function.IntSupplier colour) {
            this.owner = owner; this.feed = feed; this.colour = colour;
            changed = owner::postInvalidate;
        }

        @Override public boolean setVisible(boolean visible, boolean restart) {
            boolean result = super.setVisible(visible, restart);
            if (visible && !watching) { watching = true; feed.acquire(owner.getContext(), changed); }
            else if (!visible && watching) { watching = false; feed.release(changed); }
            return result;
        }

        /** The height of bar {@code level} (0..1) in the animation's units. */
        static float height(float level) {
            return REST_HEIGHT + Math.max(0f, Math.min(1f, level)) * (FULL_HEIGHT - REST_HEIGHT);
        }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) return;
            float unit = Math.min(bounds.width(), bounds.height()) / UNITS;
            float left = bounds.exactCenterX() - UNITS * unit / 2f, top = bounds.exactCenterY() - UNITS * unit / 2f;
            float[] levels = feed.levels();
            paint.setColor(colour.getAsInt());
            float width = BAR_WIDTH * unit;
            for (int i = 0; i < BARS; i++) {
                float h = height(i < levels.length ? levels[i] : 0f) * unit, cx = left + CENTRES[i] * unit, cy = top + MIDDLE * unit;
                rect.set(cx - width / 2f, cy - h / 2f, cx + width / 2f, cy + h / 2f);
                canvas.drawRoundRect(rect, width / 2f, width / 2f, paint);
            }
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return -1; }
        @Override public int getIntrinsicHeight() { return -1; }
    }

    /** Loudness of a band in dB relative to full scale (a magnitude of 128 on the capture's 8-bit scale). A magnitude under 2 is the
     * capture's own rounding noise and counts as silence. */
    static final float SILENCE_DB = -60f;
    static float decibels(float magnitude) { return magnitude < 2f ? SILENCE_DB : 20f * (float) Math.log10(magnitude / 128f); }

    /** The peak magnitude of each band in one FFT frame of the Visualizer (byte pairs: real, imaginary; slot 0 is DC and slot 1 the
     * Nyquist term). Bands run from about 40 Hz to 9 kHz. */
    static final float[] BAND_EDGES_HZ = {40f, 120f, 300f, 700f, 1500f, 3500f, 9000f};
    static void magnitudes(byte[] fft, int samplingRateMilliHz, float[] out) {
        int count = fft == null ? 0 : fft.length;
        float sampleRate = samplingRateMilliHz / 1000f;
        for (int band = 0; band < BARS && band < out.length; band++) {
            float peak = 0f;
            if (count >= 4 && sampleRate > 0f) {
                float binHz = sampleRate / count;
                int first = Math.max(1, Math.round(BAND_EDGES_HZ[band] / binHz));
                int last = Math.min(count / 2 - 1, Math.max(first, Math.round(BAND_EDGES_HZ[band + 1] / binHz) - 1));
                for (int k = first; k <= last; k++) {
                    float re = fft[2 * k], im = fft[2 * k + 1];
                    peak = Math.max(peak, (float) Math.sqrt(re * re + im * im));
                }
            }
            out[band] = peak;
        }
    }

    /** The bar levels (0..1) of a stream of frames. Every band is judged against its own recent loudest and quietest, a window that
     * follows the music: a bar rises with its band's beat and falls between beats, whatever the volume and whatever the band's usual
     * strength. A fixed scale did not do that: real music sits within about 10 dB of full scale in most bands, so on a fixed scale
     * nearly every bar was on its maximum nearly all the time (measured on music playing on the phone: 96 % of the bar values at full
     * height at the highest sensitivity, 70 % at the default). The user's sensitivity shapes the response over its whole range (how
     * high the bars can reach and how readily they rise); it is never a gain that clips. */
    static final class Levels {
        /** How fast the window follows: its top falls 0.14 dB a frame (about 2 dB a second at 16 frames a second) and its bottom rises
         * 0.10 dB; it is never narrower than 14 dB (a steady sound sits mid-way) nor wider than 30 dB (a dropout cannot stick). */
        static final float FALL_DB = .14f, RISE_DB = .10f, MIN_SPAN_DB = 14f, MAX_SPAN_DB = 30f;
        /** A second of silence forgets the windows, so the next track starts fresh. */
        static final int FORGET_FRAMES = 16;
        private final float[] magnitude = new float[BARS], top = new float[BARS], bottom = new float[BARS];
        private final boolean[] ready = new boolean[BARS];
        private int silent;

        void reset() { java.util.Arrays.fill(ready, false); silent = 0; }

        /** {@code out}: BARS values, 0 (rest) to 1; {@code percent}: 0..100. */
        void update(byte[] fft, int samplingRateMilliHz, float percent, float[] out) {
            magnitudes(fft, samplingRateMilliHz, magnitude);
            float loudest = SILENCE_DB;
            for (int i = 0; i < BARS; i++) loudest = Math.max(loudest, decibels(magnitude[i]));
            int count = Math.min(BARS, out.length);
            if (loudest <= SILENCE_DB) {
                if (++silent >= FORGET_FRAMES) java.util.Arrays.fill(ready, false);
                for (int i = 0; i < count; i++) out[i] = 0f;
                return;
            }
            silent = 0;
            // The user's sensitivity: at 0 the bars reach 35 % of their height and only strong beats lift them (a squared curve); at 100
            // they reach all of it and most sounds lift them (a curve under 1). Measured on real music: the mean bar height runs from
            // about 0.17 at 0 over 0.4 at the default 60 to 0.7 at 100, and at 100 about one value in eight touches the top.
            float level01 = Math.max(0f, Math.min(100f, percent)) / 100f;
            float reach = .35f + .65f * level01, curve = 2f - 1.3f * level01;
            // Quiet playback is a little calmer, never flat: the whole frame under -45 dB reaches 30 % of the bars' height.
            float volume = Math.max(.3f, Math.min(1f, (loudest + 45f) / 30f));
            for (int i = 0; i < count; i++) {
                float level = decibels(magnitude[i]);
                if (level <= SILENCE_DB) {
                    // A band with no sound shows no bar and does not drag its window down; the window only drifts toward quiet.
                    if (ready[i]) top[i] = Math.max(bottom[i], top[i] - FALL_DB);
                    out[i] = 0f;
                    continue;
                }
                if (!ready[i]) { top[i] = level; bottom[i] = level; ready[i] = true; }
                top[i] = Math.max(level, top[i] - FALL_DB);
                bottom[i] = Math.max(Math.min(level, bottom[i] + RISE_DB), top[i] - MAX_SPAN_DB);
                float high = top[i], low = bottom[i];
                if (high - low < MIN_SPAN_DB) {
                    float middle = (high + low) / 2f;
                    high = middle + MIN_SPAN_DB / 2f;
                    low = middle - MIN_SPAN_DB / 2f;
                }
                float position = Math.max(0f, Math.min(1f, (level - low) / (high - low)));
                out[i] = (float) Math.pow(position, curve) * reach * volume;
            }
        }
    }

    /** Fast up, slower down: a bar jumps with the beat and falls back between them. */
    static void smooth(float[] shown, float[] target) {
        for (int i = 0; i < shown.length && i < target.length; i++)
            shown[i] = target[i] > shown[i] ? shown[i] + (target[i] - shown[i]) * .65f : shown[i] * .82f + target[i] * .18f;
    }

    /** The platform's audio visualizer on the output mix. Held only while something is looking at the spectrum. */
    static final class SpectrumFeed implements Feed, Visualizer.OnDataCaptureListener {
        /** 1024 points: at 48 kHz the bins are 47 Hz apart, which is what it takes for the two lowest bands (40 to 300 Hz) to have bins
         * of their own; at 512 they shared one and the first two bars were the same bar. */
        private static final int CAPTURE_SIZE = 1024, MAX_RATE_MILLIHZ = 16000, SILENT_FRAMES = 48;
        private static final long POLL_MS = 2000L;
        private final Object lock = new Object();
        private final float[] levels = new float[BARS], target = new float[BARS];
        private final ArrayList<Runnable> listeners = new ArrayList<>();
        private volatile boolean unavailable;
        private volatile float percent = FluidCloudCapsule.DEFAULT_SPECTRUM_SENSITIVITY;
        private final Levels model = new Levels();
        private HandlerThread thread;
        private Handler handler;
        private Visualizer visualizer;
        private Context context;
        private AudioManager audio;
        private int silent;
        private Runnable failure;
        private android.content.BroadcastReceiver screen;

        @Override public boolean available() { return !unavailable; }
        @Override public float[] levels() { return levels; }
        @Override public void onFailure(Runnable runnable) { failure = runnable; }
        @Override public void sensitivity(float value) { percent = value; }

        private static final long CLOSE_DELAY_MS = 1500L;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final Runnable closeTask = () -> { synchronized (lock) { if (listeners.isEmpty()) close(); } };

        @Override public void acquire(Context host, Runnable changed) {
            synchronized (lock) {
                if (unavailable || host == null || listeners.contains(changed)) return;
                mainHandler.removeCallbacks(closeTask);
                listeners.add(changed);
                if (thread == null) open(host);
            }
        }

        /** The last spectrum going away closes the capture a moment later, so a view that is given its drawable again does not restart it. */
        @Override public void release(Runnable changed) {
            synchronized (lock) {
                listeners.remove(changed);
                if (listeners.isEmpty()) mainHandler.postDelayed(closeTask, CLOSE_DELAY_MS);
            }
        }

        private void open(Context host) {
            context = host.getApplicationContext() != null ? host.getApplicationContext() : host;
            audio = context.getSystemService(AudioManager.class);
            thread = new HandlerThread("c17-spectrum", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            handler = new Handler(thread.getLooper());
            // The screen going off ends the capture at once; coming back lets the poll find the sound again.
            screen = new android.content.BroadcastReceiver() {
                @Override public void onReceive(Context c, android.content.Intent intent) {
                    Handler target = handler;
                    if (target == null) return;
                    if (android.content.Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) target.post(SpectrumFeed.this::stopCapture);
                    else target.post(SpectrumFeed.this::poll);
                }
            };
            try {
                android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF);
                filter.addAction(android.content.Intent.ACTION_SCREEN_ON);
                context.registerReceiver(screen, filter, null, handler);
            } catch (RuntimeException unavailableReceiver) { screen = null; }
            handler.post(this::poll);
        }

        private void close() {
            Handler target = handler; HandlerThread worker = thread;
            if (screen != null) try { context.unregisterReceiver(screen); } catch (RuntimeException ignored) { }
            screen = null; handler = null; thread = null;
            if (target != null) target.post(() -> { stopCapture(); if (worker != null) worker.quitSafely(); });
        }

        private boolean screenOn() {
            android.os.PowerManager power = context == null ? null : context.getSystemService(android.os.PowerManager.class);
            return power == null || power.isInteractive();
        }

        /** Look for sound: capture when music is active and the screen is on, otherwise look again in a moment. */
        private void poll() {
            Handler target = handler;
            if (target == null || unavailable) return;
            target.removeCallbacks(pollTask);
            if (visualizer != null) return;
            if (screenOn() && audio != null && audio.isMusicActive()) startCapture();
            else if (screenOn()) target.postDelayed(pollTask, POLL_MS);
        }
        private final Runnable pollTask = this::poll;

        private void startCapture() {
            if (visualizer != null || unavailable) return;
            try {
                Visualizer v = new Visualizer(0);
                v.setEnabled(false);
                int[] range = Visualizer.getCaptureSizeRange();
                v.setCaptureSize(Math.max(range[0], Math.min(CAPTURE_SIZE, range[1])));
                v.setDataCaptureListener(this, Math.min(Visualizer.getMaxCaptureRate(), MAX_RATE_MILLIHZ), false, true);
                v.setEnabled(true);
                visualizer = v; silent = 0;
                model.reset();
            } catch (RuntimeException | LinkageError refused) {
                unavailable = true;
                android.util.Log.w("C17Spectrum", "the platform refused the audio capture: " + refused);
                ModuleDiagnostics.info("fluid", "Spectrum: the platform refused the audio capture; the plugin's animation stays");
                Runnable run = failure;
                if (run != null) run.run();
            }
        }

        private void stopCapture() {
            Visualizer v = visualizer; visualizer = null;
            if (v != null) {
                try { v.setEnabled(false); } catch (RuntimeException ignored) { }
                try { v.release(); } catch (RuntimeException ignored) { }
            }
            java.util.Arrays.fill(levels, 0f); java.util.Arrays.fill(target, 0f);
            notifyChanged();
            Handler h = handler;
            if (h != null && screenOn()) h.postDelayed(pollTask, POLL_MS);
        }

        private void notifyChanged() {
            final ArrayList<Runnable> copy;
            synchronized (lock) { copy = new ArrayList<>(listeners); }
            for (Runnable run : copy) run.run();
        }

        @Override public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int samplingRate) { }

        @Override public void onFftDataCapture(Visualizer v, byte[] fft, int samplingRate) {
            model.update(fft, samplingRate, percent, target);
            smooth(levels, target);
            float peak = 0f;
            for (float level : levels) peak = Math.max(peak, level);
            silent = peak < .03f ? silent + 1 : 0;
            notifyChanged();
            if (silent > SILENT_FRAMES) {
                Handler h = handler;
                if (h != null) h.post(this::stopCapture);
            }
        }
    }
}
