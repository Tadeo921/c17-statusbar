package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import com.oplus.systemui.plugins.shared.template.component.view.CustomLottieView;
import java.util.ArrayList;
import java.util.List;

/** The real-time spectrum: the plugin's canned six-bar animation is replaced by bars driven by the audio, only while on and shown. */
public final class CapsuleSpectrumCheck {
    private static int checks;
    private static void check(boolean okay, String why) { checks++; if (!okay) throw new AssertionError(why); }

    private static final int VOICE = 7, VOICE_15 = 8, OTHER = 9;

    private static final class Recording extends Canvas {
        final List<RectF> rects = new ArrayList<>();
        final List<Integer> colours = new ArrayList<>();
        @Override public void drawRoundRect(RectF rect, float rx, float ry, android.graphics.Paint paint) {
            rects.add(new RectF(rect.left, rect.top, rect.right, rect.bottom));
            colours.add(paint.getColor());
            super.drawRoundRect(rect, rx, ry, paint);
        }
    }

    private static final class FakeFeed implements CapsuleSpectrum.Feed {
        boolean available = true;
        int acquires, releases;
        float percent = -1f;
        final float[] levels = new float[CapsuleSpectrum.BARS];
        final List<Runnable> listeners = new ArrayList<>();
        Runnable failure;
        @Override public boolean available() { return available; }
        @Override public void acquire(Context context, Runnable changed) { acquires++; listeners.add(changed); }
        @Override public void release(Runnable changed) { releases++; listeners.remove(changed); }
        @Override public float[] levels() { return levels; }
        @Override public void sensitivity(float value) { percent = value; }
        @Override public void onFailure(Runnable run) { failure = run; }
        void moved() { for (Runnable run : new ArrayList<>(listeners)) run.run(); }
    }

    private static final class Lottie extends android.graphics.drawable.ColorDrawable {
        Lottie() { super(0xffffffff); }
    }

    private static CustomLottieView view() {
        CustomLottieView view = new CustomLottieView(new Context());
        view.getResources().entryNames.put(VOICE, "voice_print_dark");
        view.getResources().entryNames.put(VOICE_15, "voice_print_dark_15hz");
        view.getResources().entryNames.put(OTHER, "top_right_immersive_icon_off_to_on");
        return view;
    }

    private static Bundle on() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(FluidCloudCapsule.MASTER, true);
        bundle.putBoolean(FluidCloudCapsule.SPECTRUM_ENABLED, true);
        return bundle;
    }

    /** What the plugin does to a spectrum view: an animation by raw resource, then its drawable. */
    private static Lottie plugin(CapsuleSpectrum spectrum, CustomLottieView view, int resource) {
        view.setAnimation(resource);
        spectrum.animationSet(view, resource);
        Lottie lottie = new Lottie();
        view.setImageDrawable(lottie);
        spectrum.drawableSet(view, lottie);
        return lottie;
    }

    /** One frame at 44.1 kHz in 256 bytes (bins of 172 Hz): a tone of this magnitude in bin 6, which is the 700 Hz to 1.5 kHz band. */
    private static byte[] tone(int magnitude) {
        byte[] fft = new byte[256];
        fft[12] = (byte) magnitude;
        return fft;
    }

    /** The same, and a second tone in bin 12 (2 kHz), the 1.5 to 3.5 kHz band. */
    private static byte[] tones(int third, int fourth) {
        byte[] fft = tone(third);
        fft[24] = (byte) fourth;
        return fft;
    }

    private static float[] frames(CapsuleSpectrum.Levels model, byte[] frame, int count, float percent) {
        float[] out = new float[CapsuleSpectrum.BARS];
        for (int i = 0; i < count; i++) model.update(frame, 44100000, percent, out);
        return out;
    }

    private static void maths() {
        check(CapsuleSpectrum.BARS == 6 && CapsuleSpectrum.CENTRES.length == 6, "six bars, like the plugin's animation");
        check(CapsuleSpectrum.Bars.height(0f) == 6f && CapsuleSpectrum.Bars.height(1f) == 43f && CapsuleSpectrum.Bars.height(.5f) == 24.5f, "rest, full and half height in the animation's units");
        check(CapsuleSpectrum.Bars.height(-3f) == 6f && CapsuleSpectrum.Bars.height(9f) == 43f, "heights are clamped");
        // The magnitude of each band: a tone at about 1 kHz is in the fourth band only.
        byte[] fft = tone(100);
        float[] out = new float[CapsuleSpectrum.BARS];
        CapsuleSpectrum.magnitudes(fft, 44100000, out);
        check(out[3] == 100f, "the 700 Hz to 1.5 kHz band carries the tone");
        for (int band : new int[]{0, 1, 2, 4, 5}) check(out[band] == 0f, "band " + band + " is silent");
        CapsuleSpectrum.magnitudes(new byte[256], 44100000, out);
        for (float level : out) check(level == 0f, "silence has no magnitude");
        CapsuleSpectrum.magnitudes(null, 44100000, out);
        CapsuleSpectrum.magnitudes(fft, 0, out);
        for (float level : out) check(level == 0f, "no frame or no sample rate has none");
        // A bass note in the first bands, a hi-hat in the last.
        byte[] mixed = new byte[256];
        mixed[2 * 1] = 90; mixed[2 * 30] = 90;       // 172 Hz and 5.2 kHz
        CapsuleSpectrum.magnitudes(mixed, 44100000, out);
        check(out[1] == 90f && out[5] == 90f && out[2] == 0f && out[3] == 0f && out[4] == 0f, "a low and a high note are in their own bands, the middle stays quiet");
        // Decibels against full scale (128); under 2 is the capture's own rounding noise.
        check(CapsuleSpectrum.decibels(0f) == CapsuleSpectrum.SILENCE_DB && CapsuleSpectrum.decibels(1.9f) == CapsuleSpectrum.SILENCE_DB, "rounding noise is silence");
        check(Math.abs(CapsuleSpectrum.decibels(128f)) < 1e-4f && Math.abs(CapsuleSpectrum.decibels(64f) + 6.0206f) < 1e-3f, "128 is full scale, half of it is six dB down");
        // Fast up, slower down.
        float[] shown = new float[CapsuleSpectrum.BARS], target = new float[CapsuleSpectrum.BARS];
        java.util.Arrays.fill(target, 1f);
        CapsuleSpectrum.smooth(shown, target);
        check(Math.abs(shown[0] - .65f) < 1e-6f, "a bar rises most of the way at once");
        java.util.Arrays.fill(target, 0f);
        java.util.Arrays.fill(shown, 1f);
        CapsuleSpectrum.smooth(shown, target);
        check(Math.abs(shown[0] - .82f) < 1e-6f, "and falls back slowly");
        // The colour the bars follow, raised to show on the capsule's black.
        check(CapsuleSpectrum.visible(0xff804020) == 0xd86c36, "a dark colour keeps its hue and gets bright");
        check(CapsuleSpectrum.visible(0xff101010) == 0xd8d8d8 && CapsuleSpectrum.visible(0xff00ff00) == 0x00ff00, "grey stays grey, a bright colour is left alone");
        check(CapsuleSpectrum.visible(0xff000000) == 0xffffff, "black has no hue to keep: white");
    }

    /** The bar levels of a stream of frames: every band against its own recent loudest and quietest, shaped by the user's sensitivity. */
    private static void levels() {
        float[] out = new float[CapsuleSpectrum.BARS];
        CapsuleSpectrum.Levels silent = new CapsuleSpectrum.Levels();
        silent.update(new byte[256], 44100000, 100f, out);
        for (float level : out) check(level == 0f, "silence: every bar at rest");
        silent.update(null, 44100000, 100f, out);
        for (float level : out) check(level == 0f, "no frame: rest");
        // A steady sound sits part-way up, never at the top, whatever its volume.
        float expect = (float) Math.pow(.5, .7);
        CapsuleSpectrum.Levels steady = new CapsuleSpectrum.Levels();
        out = frames(steady, tone(100), 1, 100f);
        check(Math.abs(out[3] - expect) < .005f, "the first frame of a sound: the middle of its window, shaped by the curve (" + out[3] + ")");
        for (int band : new int[]{0, 1, 2, 4, 5}) check(out[band] == 0f, "a band with no sound shows no bar");
        out = frames(steady, tone(100), 300, 100f);
        check(Math.abs(out[3] - expect) < .005f && out[3] < .7f, "and it stays there: a steady sound is not a bar pinned at the top (" + out[3] + ")");
        float louder = frames(new CapsuleSpectrum.Levels(), tone(120), 200, 100f)[3], softer = frames(new CapsuleSpectrum.Levels(), tone(30), 200, 100f)[3];
        check(Math.abs(louder - softer) < .01f, "the same sound at four times the level looks the same: the window follows the music (" + louder + ", " + softer + ")");
        // A beat after quiet lifts the bar to the top of its window and it falls back between beats.
        CapsuleSpectrum.Levels beat = new CapsuleSpectrum.Levels();
        frames(beat, tone(20), 60, 100f);
        out = frames(beat, tone(100), 1, 100f);
        check(out[3] > .95f && out[3] <= 1f, "a beat lifts the bar to the top of its window (" + out[3] + ")");
        out = frames(beat, tone(20), 8, 100f);
        check(out[3] < .15f, "and it falls back to the bottom of the window between beats (" + out[3] + ")");
        // The user's sensitivity: the peaks rise with it over its whole range.
        float previous = -1f;
        for (float percent : new float[]{0f, 25f, 50f, 75f, 100f}) {
            CapsuleSpectrum.Levels model = new CapsuleSpectrum.Levels();
            frames(model, tone(20), 60, percent);
            float peak = frames(model, tone(100), 1, percent)[3];
            check(peak > previous, "more sensitivity, higher peak (" + percent + ": " + peak + ")");
            previous = peak;
            if (percent == 0f) check(peak < .4f, "at 0 the bars reach about a third of their height (" + peak + ")");
            if (percent == 100f) check(peak > .95f, "at 100 they reach the top (" + peak + ")");
        }
        check(frames(new CapsuleSpectrum.Levels(), tone(100), 1, -50f)[3] == frames(new CapsuleSpectrum.Levels(), tone(100), 1, 0f)[3]
                && frames(new CapsuleSpectrum.Levels(), tone(100), 1, 900f)[3] == frames(new CapsuleSpectrum.Levels(), tone(100), 1, 100f)[3], "a sensitivity outside 0..100 is held to it");
        // Very quiet playback is calmer, not flat and not full.
        out = frames(new CapsuleSpectrum.Levels(), tone(3), 5, 100f);
        check(out[3] > 0f && out[3] < .3f, "very quiet playback gives a calmer bar (" + out[3] + ")");
        // A second of silence starts the next sound afresh; a shorter one keeps what the window learnt.
        CapsuleSpectrum.Levels forgetful = new CapsuleSpectrum.Levels(), brief = new CapsuleSpectrum.Levels();
        for (CapsuleSpectrum.Levels model : new CapsuleSpectrum.Levels[]{forgetful, brief}) {
            frames(model, tone(20), 60, 100f);
            frames(model, tone(100), 1, 100f);
        }
        for (int i = 0; i < CapsuleSpectrum.Levels.FORGET_FRAMES; i++) forgetful.update(new byte[256], 44100000, 100f, out);
        for (int i = 0; i < CapsuleSpectrum.Levels.FORGET_FRAMES - 1; i++) brief.update(new byte[256], 44100000, 100f, out);
        check(Math.abs(frames(forgetful, tone(100), 1, 100f)[3] - expect) < .005f, "after a second of silence the next sound starts in the middle of a fresh window");
        check(frames(brief, tone(100), 1, 100f)[3] > .9f, "a shorter silence keeps the window");
        // One band dropping out for a frame does not throw it to the top when it returns.
        CapsuleSpectrum.Levels dropout = new CapsuleSpectrum.Levels();
        frames(dropout, tones(100, 80), 40, 100f);
        dropout.update(tones(0, 80), 44100000, 100f, out);
        check(out[3] == 0f && out[4] > 0f, "a band that drops out shows no bar while the others carry on");
        out = frames(dropout, tones(100, 80), 1, 100f);
        check(Math.abs(out[3] - expect) < .02f, "and when it returns it is where it was (" + out[3] + ")");
        // Music-like frames: real music sat within about 10 dB of full scale in most bands, which on the old fixed scale (55 dB of range
        // times a gain of up to 2) was nearly always the top. On this input the old scale is at the top nearly all the time; the
        // windows are not.
        java.util.Random random = new java.util.Random(11);
        int[] bins = {1, 2, 5, 12, 30};
        CapsuleSpectrum.Levels music = new CapsuleSpectrum.Levels();
        int values = 0, pinned = 0, oldPinned = 0;
        double sum = 0;
        for (int frame = 0; frame < 800; frame++) {
            byte[] f = new byte[256];
            for (int bin : bins) f[2 * bin] = (byte) (30 + random.nextInt(98));
            music.update(f, 44100000, 100f, out);
            float[] magnitudes = new float[CapsuleSpectrum.BARS];
            CapsuleSpectrum.magnitudes(f, 44100000, magnitudes);
            if (frame < 32) continue;
            for (int band = 0; band < CapsuleSpectrum.BARS; band++) {
                values++; sum += out[band];
                if (out[band] >= .97f) pinned++;
                if (Math.min(1f, (CapsuleSpectrum.decibels(magnitudes[band]) + 55f) / 55f * 2f) >= .97f) oldPinned++;
            }
        }
        check(oldPinned > values * .9, "the old scale put almost every value at the top (" + oldPinned + " of " + values + ")");
        check(pinned < values * .25, "the bars are at the top now and then, not all the time (" + pinned + " of " + values + ")");
        check(sum / values > .3 && sum / values < .8, "and on average well up the height (" + sum / values + ")");
    }

    /** Bars that follow the accent: the colour the source gives (FluidCloudCapsule.spectrumAccent hands over the media colour already raised
     * to show), in the chosen colour's alpha; the chosen colour while there is none. */
    private static void follow() {
        final Integer[] media = {null};
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed, () -> media[0]);
        Bundle bundle = on();
        spectrum.configure(bundle);
        CustomLottieView view = view();
        plugin(spectrum, view, VOICE);
        CapsuleSpectrum.Bars bars = (CapsuleSpectrum.Bars) view.getDrawable();
        bars.setBounds(0, 0, 72, 72);
        media[0] = 0xd86c36;
        check(firstColour(bars) == FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR, "not following by default: the chosen colour, whatever the media colour");
        bundle.putBoolean(FluidCloudCapsule.SPECTRUM_FOLLOW, true);
        int posted = view.postedInvalidations;
        spectrum.configure(bundle);
        check(view.postedInvalidations > posted, "switching it on asks the bars to draw again");
        check(firstColour(bars) == 0xccd86c36, "following: the source's colour, in the chosen colour's alpha");
        media[0] = 0x00ff00;
        check(firstColour(bars) == 0xcc00ff00, "and the bars follow it as it changes");
        media[0] = 0xff112233;
        check(firstColour(bars) == 0xcc112233, "whatever alpha the source's colour carries, the chosen colour's alpha is used");
        media[0] = null;
        check(firstColour(bars) == FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR, "with no media colour yet, the chosen colour");
        media[0] = 0xd86c36;
        bundle.putInt(FluidCloudCapsule.SPECTRUM_COLOR, 0x4000ff80);
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.SPECTRUM_COLOR), true);
        spectrum.configure(bundle);
        check(firstColour(bars) == 0x40d86c36, "the alpha is the chosen colour's own");
        bundle.putBoolean(FluidCloudCapsule.SPECTRUM_FOLLOW, false);
        posted = view.postedInvalidations;
        spectrum.configure(bundle);
        check(view.postedInvalidations > posted && firstColour(bars) == 0x4000ff80, "off again: the chosen colour, and the bars draw again");
        // No source of colour at all: following the accent still draws.
        CapsuleSpectrum plain = new CapsuleSpectrum(new FakeFeed());
        plain.configure(bundle);
        bundle.putBoolean(FluidCloudCapsule.SPECTRUM_FOLLOW, true);
        plain.configure(bundle);
        CustomLottieView other = view();
        plugin(plain, other, VOICE);
        CapsuleSpectrum.Bars bare = (CapsuleSpectrum.Bars) other.getDrawable();
        bare.setBounds(0, 0, 72, 72);
        check(firstColour(bare) == 0x4000ff80, "without a colour source the chosen colour stays");
    }

    private static int firstColour(CapsuleSpectrum.Bars bars) {
        Recording canvas = new Recording();
        bars.draw(canvas);
        return canvas.colours.get(0);
    }

    /** What the platform's feed does with a frame: the levels come out of the same windows, with the sensitivity the setting gave. */
    private static void feedFrames() {
        CapsuleSpectrum.SpectrumFeed high = new CapsuleSpectrum.SpectrumFeed(), low = new CapsuleSpectrum.SpectrumFeed();
        high.sensitivity(100f);
        low.sensitivity(0f);
        for (int i = 0; i < 40; i++) {
            high.onFftDataCapture(null, tone(100), 44100000);
            low.onFftDataCapture(null, tone(100), 44100000);
        }
        check(Math.abs(high.levels()[3] - (float) Math.pow(.5, .7)) < .01f, "a steady sound settles part-way up at the highest sensitivity (" + high.levels()[3] + ")");
        check(low.levels()[3] < high.levels()[3] / 3f && low.levels()[3] > 0f, "and much lower at the lowest (" + low.levels()[3] + ")");
        check(high.levels()[0] == 0f && high.levels()[5] == 0f, "the other bars stay at rest");
        for (int i = 0; i < 80; i++) high.onFftDataCapture(null, new byte[256], 44100000);
        check(high.levels()[3] < .03f, "silence brings the bar back down");
    }

    private static void drawing() {
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed);
        CustomLottieView view = view();
        spectrum.configure(on());
        Lottie lottie = plugin(spectrum, view, VOICE);
        check(view.getDrawable() instanceof CapsuleSpectrum.Bars, "the spectrum view shows the bars");
        CapsuleSpectrum.Bars bars = (CapsuleSpectrum.Bars) view.getDrawable();
        check(feed.acquires == 1 && feed.releases == 0, "a visible spectrum looks at the audio");
        bars.setBounds(0, 0, 72, 72);
        feed.levels[0] = 0f; feed.levels[1] = .5f; feed.levels[2] = 1f; feed.levels[5] = .25f;
        Recording canvas = new Recording();
        bars.draw(canvas);
        check(canvas.rects.size() == 6, "six bars");
        float[] heights = {6f, 24.5f, 43f, 6f, 6f, 15.25f};
        for (int i = 0; i < 6; i++) {
            RectF r = canvas.rects.get(i);
            check(Math.abs(r.width() - 6f) < 1e-3f && Math.abs(r.height() - heights[i]) < 1e-3f, "bar " + i + " has the animation's width and its level's height");
            check(Math.abs(r.centerX() - CapsuleSpectrum.CENTRES[i]) < 1e-3f && Math.abs(r.centerY() - 35.806f) < 1e-3f, "bar " + i + " is where the animation had it");
        }
        // Scaled to the view: 80 px, the 72-unit box centred in it.
        bars.setBounds(0, 0, 144, 72);
        Recording wide = new Recording();
        bars.draw(wide);
        check(Math.abs(wide.rects.get(0).centerX() - (36f + 9.831f)) < 1e-3f, "bars stay at the middle of a wider view, not stretched");
        bars.setBounds(0, 0, 0, 0);
        Recording empty = new Recording();
        bars.draw(empty);
        check(empty.rects.isEmpty(), "nothing is drawn without bounds");
        // The feed moving asks the view to redraw.
        int posted = view.postedInvalidations;
        feed.moved();
        check(view.postedInvalidations == posted + 1, "new levels ask for a redraw");
        // Hidden: no more looking at the audio.
        bars.setVisible(false, false);
        check(feed.releases == 1 && feed.listeners.isEmpty(), "a hidden spectrum lets go of the audio");
        bars.setVisible(true, false);
        bars.setVisible(true, false);
        check(feed.acquires == 2, "being shown twice looks once");
        // The setting going off puts the plugin's animation back, and stops watching.
        spectrum.configure(new Bundle());
        check(view.getDrawable() == lottie, "off: the plugin's own animation again");
        check(feed.releases == 2, "and the audio is let go");
        spectrum.configure(on());
        check(view.getDrawable() == bars, "on again: the same bars");
        check(feed.acquires == 3, "looking at the audio again");
        // The plugin hands the view its animation again (it sets null, then the drawable): the bars come back each time.
        view.setImageDrawable(null);
        spectrum.drawableSet(view, null);
        Lottie second = new Lottie();
        view.setImageDrawable(second);
        spectrum.drawableSet(view, second);
        check(view.getDrawable() == bars, "a new animation of the plugin is shown as the bars");
        spectrum.configure(new Bundle());
        check(view.getDrawable() == second, "and the latest animation is the one that comes back");
    }

    private static void identification() {
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed);
        spectrum.configure(on());
        CustomLottieView fifteen = view();
        plugin(spectrum, fifteen, VOICE_15);
        check(fifteen.getDrawable() instanceof CapsuleSpectrum.Bars, "the 15 Hz variant is a spectrum too");
        CustomLottieView other = view();
        Lottie icon = plugin(spectrum, other, OTHER);
        check(other.getDrawable() == icon, "any other animation is left alone");
        CustomLottieView unknown = view();
        Lottie none = plugin(spectrum, unknown, 12345);
        check(unknown.getDrawable() == none, "an id that is not a resource is left alone");
        check(!CapsuleSpectrum.isVoicePrint(other, 0), "no resource, no spectrum");
        // The view is given another animation: the bars go, the plugin's drawable comes back.
        spectrum.animationSet(fifteen, OTHER);
        check(!(fifteen.getDrawable() instanceof CapsuleSpectrum.Bars), "a view that stops being a spectrum shows its own animation");
        // A cached animation arrives before setAnimation returns: its drawable was set before the view was known.
        CustomLottieView cached = view();
        Lottie early = new Lottie();
        cached.setImageDrawable(early);
        spectrum.drawableSet(cached, early);
        check(cached.getDrawable() == early, "a drawable of an unknown view is not touched");
        cached.setAnimation(VOICE);
        spectrum.animationSet(cached, VOICE);
        check(cached.getDrawable() instanceof CapsuleSpectrum.Bars, "the cached case is picked up when the view is identified");
        // A view that is not an image view cannot take bars.
        spectrum.animationSet(new android.view.View(new Context()), VOICE);
        spectrum.drawableSet(new android.view.View(new Context()), new Lottie());
        // The audited structure resolves by shape.
        CapsuleSpectrum.Hooks hooks = spectrum.resolve(CapsuleSpectrumCheck.class.getClassLoader());
        check(hooks != null && hooks.setAnimation.getName().equals("setAnimation") && hooks.setImageDrawable.getName().equals("setImageDrawable"), "hooks resolve");
        check(new CapsuleSpectrum(feed).resolve(null) == null, "no class loader, no hooks");
        check(hooks.json.length == 2 && hooks.json[0].getName().equals("setAnimationFromJson") && hooks.json[0].getParameterCount() == 1
                && hooks.json[1].getParameterCount() == 2, "both JSON entry points of the Lottie base class are found by shape");
    }

    /** What the plugin really does: it reads the raw resource itself and hands the view the JSON text, so the view never sees the
     * resource. The JSON is recognised by its shape, and so is a composition the view has loaded some other way. */
    private static String voiceJson() {
        StringBuilder text = new StringBuilder("{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":62,\"w\":72,\"h\":72,\"nm\":\"voice\",\"layers\":[");
        for (int layer = 7; layer <= 12; layer++) text.append("{\"ind\":").append(layer).append(",\"nm\":\"Shape Layer ").append(layer).append("\",\"ks\":{},\"shapes\":[]},");
        while (text.length() < 1500) text.append(' ');
        return text.append("{}]}").toString();
    }
    private static String otherJson() {
        StringBuilder text = new StringBuilder("{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":24,\"w\":69,\"h\":69,\"nm\":\"icon\",\"layers\":[");
        while (text.length() < 1500) text.append(' ');
        return text.append("{}]}").toString();
    }

    private static void jsonAndComposition() {
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed);
        spectrum.configure(on());
        check(CapsuleSpectrum.isVoicePrintJson(voiceJson()) && !CapsuleSpectrum.isVoicePrintJson(otherJson()), "the spectrum's JSON is told from the plugin's other animations");
        check(!CapsuleSpectrum.isVoicePrintJson("{}") && !CapsuleSpectrum.isVoicePrintJson(""), "short or empty text is not the spectrum");
        // JSON text: the plugin sets the animation, then the drawable.
        CustomLottieView view = view();
        view.setAnimationFromJson(voiceJson());
        spectrum.animationJson(view, voiceJson());
        Lottie lottie = new Lottie();
        view.setImageDrawable(lottie);
        spectrum.drawableSet(view, lottie);
        check(view.getDrawable() instanceof CapsuleSpectrum.Bars, "an animation handed over as JSON text becomes the bars");
        // It is given another animation: the plugin's drawable comes back.
        spectrum.animationJson(view, otherJson());
        check(view.getDrawable() == lottie, "another JSON animation puts the plugin's own back");
        // The cached case: the drawable is already there when the JSON is seen.
        CustomLottieView cached = view();
        Lottie early = new Lottie();
        cached.setImageDrawable(early);
        spectrum.animationJson(cached, voiceJson());
        check(cached.getDrawable() instanceof CapsuleSpectrum.Bars, "a drawable that was set before the JSON was seen is taken too");
        // Not a spectrum, not touched.
        CustomLottieView icon = view();
        icon.setAnimationFromJson(otherJson());
        spectrum.animationJson(icon, otherJson());
        Lottie plain = new Lottie();
        icon.setImageDrawable(plain);
        spectrum.drawableSet(icon, plain);
        check(icon.getDrawable() == plain, "any other animation is left alone");
        spectrum.animationJson(new android.view.View(new Context()), voiceJson());
        spectrum.animationJson(icon, null);
        // A composition the view loaded some other way (a ready composition, a cache): 72 x 72 and about two seconds.
        CustomLottieView loaded = view();
        loaded.composition = new com.airbnb.lottie.LottieBase.Composition(72, 72);
        loaded.duration = 2070L;
        Lottie made = new Lottie();
        loaded.setImageDrawable(made);
        spectrum.drawableSet(loaded, made);
        check(loaded.getDrawable() instanceof CapsuleSpectrum.Bars, "a loaded composition of the spectrum's size and length is the spectrum");
        loaded.duration = 2030L;
        check(CapsuleSpectrum.voicePrintComposition(loaded), "the 15 Hz file runs 2.03 s");
        for (long length : new long[]{900L, 1800L, 2300L, 0L, -1L}) {
            loaded.duration = length;
            check(!CapsuleSpectrum.voicePrintComposition(loaded), "a length of " + length + " ms is not the spectrum");
        }
        loaded.duration = 2070L;
        loaded.composition = new com.airbnb.lottie.LottieBase.Composition(69, 69);
        check(!CapsuleSpectrum.voicePrintComposition(loaded), "69 x 69 is another animation");
        loaded.composition = null;
        check(!CapsuleSpectrum.voicePrintComposition(loaded), "no composition, no spectrum");
        CustomLottieView other = view();
        other.composition = new com.airbnb.lottie.LottieBase.Composition(69, 69);
        other.duration = 700L;
        Lottie unrelated = new Lottie();
        other.setImageDrawable(unrelated);
        spectrum.drawableSet(other, unrelated);
        check(other.getDrawable() == unrelated, "a short 69 x 69 animation is left alone");
        check(!CapsuleSpectrum.voicePrintComposition(new android.view.View(new Context())), "a view that is not a Lottie view is never the spectrum");
        // Off: the composition is not looked at, nothing is taken.
        CapsuleSpectrum off = new CapsuleSpectrum(new FakeFeed());
        CustomLottieView idle = view();
        idle.composition = new com.airbnb.lottie.LottieBase.Composition(72, 72);
        idle.duration = 2070L;
        Lottie kept = new Lottie();
        idle.setImageDrawable(kept);
        off.drawableSet(idle, kept);
        check(idle.getDrawable() == kept, "the spectrum off takes nothing");
    }

    /** The spectrum's colour: the bars are drawn in it, a changed colour repaints at once, an opaque colour ignores its alpha unless
     * the colour has its own alpha switched on. */
    private static void colour() {
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed);
        spectrum.configure(on());
        CustomLottieView view = view();
        plugin(spectrum, view, VOICE);
        CapsuleSpectrum.Bars bars = (CapsuleSpectrum.Bars) view.getDrawable();
        bars.setBounds(0, 0, 72, 72);
        Recording first = new Recording();
        bars.draw(first);
        check(first.colours.size() == 6 && first.colours.get(0) == FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR && FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR == 0xccffffff,
                "the plugin's own colour (80 % white) by default");
        Bundle bundle = on();
        bundle.putInt(FluidCloudCapsule.SPECTRUM_COLOR, 0x4000ff80);
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.SPECTRUM_COLOR), false);
        int posted = view.postedInvalidations;
        spectrum.configure(bundle);
        check(view.postedInvalidations > posted, "a changed colour asks the bars to draw again");
        check(view.getDrawable() == bars, "and the same bars stay");
        Recording opaque = new Recording();
        bars.draw(opaque);
        check(opaque.colours.get(0) == 0xff00ff80 && opaque.colours.get(5) == 0xff00ff80, "a colour without its own alpha is opaque, on every bar");
        bundle.putBoolean(StatusBarSettings.alphaKey(FluidCloudCapsule.SPECTRUM_COLOR), true);
        spectrum.configure(bundle);
        Recording translucent = new Recording();
        bars.draw(translucent);
        check(translucent.colours.get(0) == 0x4000ff80, "with its own alpha on, the alpha is kept");
        posted = view.postedInvalidations;
        spectrum.configure(bundle);
        check(view.postedInvalidations == posted, "an unchanged colour repaints nothing");
        spectrum.configure(on());
        Recording back = new Recording();
        bars.draw(back);
        check(back.colours.get(0) == FluidCloudCapsule.DEFAULT_SPECTRUM_COLOR, "no colour setting: the plugin's own again");
    }

    private static void switches() {
        FakeFeed feed = new FakeFeed();
        CapsuleSpectrum spectrum = new CapsuleSpectrum(feed);
        CustomLottieView view = view();
        Lottie lottie = plugin(spectrum, view, VOICE);
        check(view.getDrawable() == lottie && feed.acquires == 0, "off by default: nothing is swapped and nothing is watched");
        Bundle bundle = new Bundle();
        bundle.putBoolean(FluidCloudCapsule.SPECTRUM_ENABLED, true);
        spectrum.configure(bundle);
        check(view.getDrawable() == lottie, "the spectrum needs the capsule page's master too");
        bundle.putBoolean(FluidCloudCapsule.MASTER, true);
        spectrum.configure(bundle);
        check(view.getDrawable() instanceof CapsuleSpectrum.Bars, "master and switch on");
        check(feed.percent == FluidCloudCapsule.DEFAULT_SPECTRUM_SENSITIVITY, "the default sensitivity reaches the feed");
        bundle.putFloat(FluidCloudCapsule.SPECTRUM_SENSITIVITY, 80f);
        spectrum.configure(bundle);
        check(feed.percent == 80f, "and a chosen one");
        bundle.putFloat(FluidCloudCapsule.SPECTRUM_SENSITIVITY, 900f);
        spectrum.configure(bundle);
        check(feed.percent == 100f, "held to 0..100");
        bundle.putBoolean(StatusBarSettings.SAFE_MODE, true);
        spectrum.configure(bundle);
        check(view.getDrawable() == lottie, "safe mode counts as off");
        bundle.putBoolean(StatusBarSettings.SAFE_MODE, false);
        spectrum.configure(bundle);
        // The platform refusing the audio capture puts the plugin's animation back for good.
        check(view.getDrawable() instanceof CapsuleSpectrum.Bars, "on again");
        feed.available = false;
        feed.failure.run();
        Handler.drainForCheck();
        check(view.getDrawable() == lottie && !spectrum.active(), "no audio capture: the plugin's animation is back and stays");
        CustomLottieView later = view();
        Lottie plain = plugin(spectrum, later, VOICE);
        check(later.getDrawable() == plain, "and later spectrum views are left alone");
        // Release.
        FakeFeed feed2 = new FakeFeed();
        CapsuleSpectrum second = new CapsuleSpectrum(feed2);
        second.configure(on());
        CustomLottieView again = view();
        Lottie original = plugin(second, again, VOICE);
        check(again.getDrawable() instanceof CapsuleSpectrum.Bars, "a fresh spectrum takes the view");
        second.release();
        check(again.getDrawable() == original && !second.active(), "release restores the plugin's animation");
        second.configure(on());
        Lottie after = plugin(second, view(), VOICE);
        check(after != null && !second.active(), "a released spectrum never takes a view again");
    }

    public static void main(String[] args) {
        maths();
        drawing();
        identification();
        jsonAndComposition();
        colour();
        levels();
        follow();
        feedFrames();
        switches();
        System.out.println("CapsuleSpectrumCheck: " + checks + " checks passed (band magnitudes, bar levels and sensitivity, bars geometry, identification by raw name, JSON text and loaded composition, colour and following the accent, on/off, platform refusal, release)");
    }
}
