package dev.puitheme;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The pull-down wallpaper at work: what loads, what is drawn at each point of the pull, the swipe between the two panels, the clock zone,
 * the glass for the big clock, and everything that must stay inert (no switch, no picture, a failed read, the lock screen).
 */
public final class ShadeWallpaperRuntimeCheck {
    private static int checks;
    private static void equal(Object expected, Object actual, String why) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("Wallpaper runtime " + checks + " (" + why + "): " + expected + " != " + actual);
    }
    private static void check(boolean value, String why) { equal(true, value, why); }
    private static void close(float expected, float actual, float tolerance, String why) { check(Math.abs(expected - actual) <= tolerance, why + " (" + expected + " vs " + actual + ")"); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static final String REVISION = "a".repeat(64), OTHER = "b".repeat(64);
    private static final class Window extends View {
        int width = 1440, height = 3168;
        Window(Context context) { super(context); }
        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }
    }
    private static final class Rig {
        final List<Runnable> work = new ArrayList<>(), completed = new ArrayList<>();
        final int[] reads = {0};
        boolean failRead, translucent;
        final Context systemUi = new Context() { @Override public String getPackageName() { return "com.android.systemui"; } };
        final ShadeWallpaper wallpaper = new ShadeWallpaper(work::add, completed::add, (context, scene, revision) -> {
            reads[0]++;
            if (failRead) throw new java.io.IOException("unreadable");
            // 100 x 200: a vertical ramp, so the rows differ and a blur is visible
            Bitmap picture = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888);
            for (int y = 0; y < 200; y++) for (int x = 0; x < 100; x++)
                picture.pixels[y * 100 + x] = (translucent ? 0x80000000 : 0xff000000) | (y % 2 == 0 ? 0xff : 0x00) << 16 | (x < 50 ? 0x40 : 0xc0);
            return picture;
        });
        final Window root = new Window(systemUi);
        Rig() { root.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT; }
        Bundle settings(boolean master) {
            Bundle result = new Bundle();
            result.putBoolean(ShadeWallpaperSettings.PORTRAIT_MASTER, master);
            result.putBoolean(ShadeWallpaperSettings.LANDSCAPE_MASTER, master);
            return result;
        }
        void scene(Bundle values, String scene, String revision, float blur) {
            values.putBoolean(ShadeWallpaperSettings.enabledKey(scene), true);
            values.putString(ShadeWallpaperSettings.revisionKey(scene), revision);
            values.putFloat(ShadeWallpaperSettings.blurKey(scene), blur);
        }
        void drain() {
            // bounded: a task may queue another
            for (int i = 0; i < 20 && (!work.isEmpty() || !completed.isEmpty()); i++) {
                List<Runnable> now = new ArrayList<>(work); work.clear(); for (Runnable task : now) task.run();
                List<Runnable> done = new ArrayList<>(completed); completed.clear(); for (Runnable task : done) task.run();
            }
        }
        Canvas draw() { Canvas canvas = new Canvas(); wallpaper.draw(root, canvas); return canvas; }
        Object frame() throws Exception { return field(ShadeWallpaper.class, "frame").get(wallpaper); }
    }

    public static void main(String[] args) throws Exception {
        inertUntilAsked();
        loadsAndDraws();
        blurFollowsThePull();
        swipeBetweenPanels();
        clockZoneAndGlass();
        settingChangesKeepThePicture();
        layerUnderTheShade();
        handOverNeverDips();
        scrimBlurShare();
        slidingPages();
        layeredSwipe();
        failureAndLifecycle();
        System.out.println("Shade wallpaper runtime checks passed: " + checks);
    }

    private static void inertUntilAsked() throws Exception {
        Rig rig = new Rig();
        // no master, no picture: nothing is wanted, nothing runs, nothing is drawn
        Bundle off = rig.settings(false); rig.scene(off, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 10f);
        rig.wallpaper.configure(rig.systemUi, off);
        equal(false, rig.wallpaper.wanted(), "master off");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        equal(0, rig.work.size(), "no work with the master off");
        equal(0, rig.draw().bitmapDraws, "no drawing with the master off");
        Bundle noPicture = rig.settings(true);
        rig.wallpaper.configure(rig.systemUi, noPicture);
        equal(false, rig.wallpaper.wanted(), "master on, no picture: the panel hooks are not wanted");
        rig.wallpaper.configure(rig.systemUi, new Bundle());
        rig.wallpaper.configure(null, off);
        rig.wallpaper.configure(new Context(), off);
        equal(0, rig.reads[0], "nothing was read");
        // a picture that is not a valid revision is no picture
        Bundle invalid = rig.settings(true); rig.scene(invalid, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, "not-a-revision", 0f);
        rig.wallpaper.configure(rig.systemUi, invalid);
        equal(false, rig.wallpaper.wanted(), "an invalid revision is no picture");
        // the safe mode keeps it all off
        Bundle safe = rig.settings(true); rig.scene(safe, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        safe.putBoolean(StatusBarSettings.SAFE_MODE, true);
        rig.wallpaper.configure(rig.systemUi, safe);
        equal(false, rig.wallpaper.wanted(), "safe mode");
    }

    private static void loadsAndDraws() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 12f);
        values.putBoolean(ShadeWallpaperSettings.BLUR_WITH_PULL, false);
        rig.wallpaper.configure(rig.systemUi, values);
        equal(true, rig.wallpaper.wanted(), "a picture and the master");
        equal(0, rig.work.size(), "nothing loads before a panel is bound");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 0f, true);
        equal(1, rig.work.size(), "binding the panel starts one load");
        rig.drain();
        equal(1, rig.reads[0], "only the notification centre has a picture: one read");
        check(rig.frame() != null, "the picture is ready");
        // closed: nothing is drawn
        equal(0, rig.draw().bitmapDraws, "a closed panel draws nothing");
        // the first part of the pull: the system's curve is still at nothing
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .1f, false);
        equal(0, rig.draw().bitmapDraws, "the first sixth of the pull shows nothing");
        // half pulled: blurred from the start (blur with pull is off), partly there
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .6f, false);
        Canvas half = rig.draw();
        equal(1, half.bitmapLog.size(), "the blurred picture only: one bitmap");
        float power = WallpaperPixels.pullCurve(.6f);
        equal(Math.round(power * 255f), half.bitmapLog.get(0).alpha, "strength follows the pull curve");
        check(half.bitmapLog.get(0).bitmap.getWidth() <= ShadeWallpaper.SMALL_EDGE, "it is the small blurred copy");
        // fully pulled: full strength
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        Canvas full = rig.draw();
        equal(255, full.bitmapLog.get(0).alpha, "full expansion: full strength");
        // the picture is cropped to fill the window: 100x200 into 1440x3168: width is cropped
        Canvas.BitmapDraw drawn = full.bitmapLog.get(0);
        float visible = 200f * 1440f / 3168f / 100f;
        float left = (1f - visible) / 2f;
        int bw = drawn.bitmap.getWidth();
        check(Math.abs(drawn.source.left - Math.round(left * bw)) <= 1, "cropped on the left by " + left + " of the picture: " + drawn.source.left + "/" + bw);
        equal(drawn.bitmap.getHeight(), drawn.source.bottom - drawn.source.top, "all of the height shows");
        equal(1440f, drawn.dest.right - drawn.dest.left, "it fills the window");
        equal(3168f, drawn.dest.bottom - drawn.dest.top, "from top to bottom");
        // the lock screen (status bar state 1) never shows it
        rig.wallpaper.onPanelChanged(rig.root, "separate", 1, 1f, false);
        equal(0, rig.draw().bitmapDraws, "not on the lock screen");
        // another view is not the bound root
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        Canvas other = new Canvas(); rig.wallpaper.draw(new Window(rig.systemUi), other);
        equal(0, other.bitmapDraws, "only the bound shade window");
        // a picture without blur is just the sharp one
        Bundle sharp = rig.settings(true); rig.scene(sharp, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.wallpaper.configure(rig.systemUi, sharp); rig.drain();
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        Canvas plain = rig.draw();
        equal(1, plain.bitmapLog.size(), "no blur: one bitmap");
        equal(100, plain.bitmapLog.get(0).bitmap.getWidth(), "and it is the picture itself, not a copy");
        // orientation: landscape uses the landscape scene, which has none: nothing is drawn, nothing is read
        rig.root.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        int before = rig.reads[0];
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        equal(0, rig.draw().bitmapDraws, "no landscape picture: the native background");
        equal(before, rig.reads[0], "and no read");
    }

    private static void blurFollowsThePull() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 20f);
        rig.wallpaper.configure(rig.systemUi, values);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .5f, false); rig.drain();
        // pulled part way: the sharp picture first, the blurred one over it, at alphas that make (1 - b) sharp and b blurred
        Canvas part = rig.draw();
        equal(2, part.bitmapLog.size(), "part way: the sharp and the blurred picture");
        Canvas.BitmapDraw sharp = part.bitmapLog.get(0), blurred = part.bitmapLog.get(1);
        equal(100, sharp.bitmap.getWidth(), "the first is the sharp one");
        check(blurred.bitmap.getWidth() < 100 || blurred.bitmap != sharp.bitmap, "the second is the blurred copy");
        float power = WallpaperPixels.pullCurve(.5f), second = power * power, first = WallpaperPixels.firstLayerAlpha(power, second);
        equal(Math.round(second * 255f), blurred.alpha, "the blurred layer has strength x blend");
        equal(Math.round(first * 255f), sharp.alpha, "the sharp layer is chosen to make the pair right");
        check(!sharp.additive && !blurred.additive, "plain layers: no offscreen buffer");
        // as the pull goes on the share of the blurred picture rises steadily, from sharp towards the set blur
        float previousShare = -1f;
        for (int i = 30; i <= 100; i += 10) {
            rig.wallpaper.onPanelChanged(rig.root, "separate", 0, i / 100f, false);
            Canvas step = rig.draw();
            float p = WallpaperPixels.pullCurve(i / 100f), share;
            if (step.bitmapLog.size() == 1) share = step.bitmapLog.get(0).bitmap == sharp.bitmap ? 0f : 1f;
            else {
                float blurWeight = step.bitmapLog.get(1).alpha / 255f, sharpAlpha = step.bitmapLog.get(0).alpha / 255f;
                float coverage = 1f - (1f - sharpAlpha) * (1f - blurWeight);
                share = blurWeight / coverage;
            }
            close(p, share, .02f, "the blurred share is the pull's progress at " + i + "%");
            check(share >= previousShare - .01f, "the picture only gets blurrier as the pull goes on (step " + i + ")");
            previousShare = share;
        }
        // with the layer decomposition no saveLayer is needed for one picture
        equal(0, rig.draw().layers, "one picture needs no layer");
    }

    private static void swipeBetweenPanels() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true);
        rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.scene(values, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        rig.wallpaper.configure(rig.systemUi, values);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        equal(2, rig.reads[0], "both panels' pictures are read");
        rig.wallpaper.onHorizontalProgress(0f);
        Canvas notification = rig.draw();
        equal(1, notification.bitmapLog.size(), "the notification centre alone");
        rig.wallpaper.onHorizontalProgress(1f);
        Canvas control = rig.draw();
        equal(1, control.bitmapLog.size(), "the control centre alone");
        rig.wallpaper.onHorizontalProgress(.4f);
        Canvas mixed = rig.draw();
        equal(0, mixed.layers, "solid pictures at full strength need no offscreen buffer to cross-fade");
        equal(2, mixed.bitmapLog.size(), "one bitmap each");
        check(!mixed.bitmapLog.get(0).additive && !mixed.bitmapLog.get(1).additive, "the second is drawn over the first");
        equal(255, mixed.bitmapLog.get(0).alpha, "the notification centre stays whole under it");
        equal(Math.round(.4f * 255f), mixed.bitmapLog.get(1).alpha, "and the control centre comes in by the native progress");
        // see-through pictures (and a pull that is not complete) are still added in one layer, so the middle never dims
        Rig glassy = new Rig(); glassy.translucent = true;
        Bundle glassValues = glassy.settings(true);
        glassy.scene(glassValues, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        glassy.scene(glassValues, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        glassy.wallpaper.configure(glassy.systemUi, glassValues);
        glassy.wallpaper.onPanelChanged(glassy.root, "separate", 0, 1f, false); glassy.drain();
        glassy.wallpaper.onHorizontalProgress(.4f);
        Canvas seeThrough = glassy.draw();
        equal(1, seeThrough.layers, "see-through pictures are added in one layer");
        check(!seeThrough.bitmapLog.get(0).additive && seeThrough.bitmapLog.get(1).additive, "the second is added to the first");
        equal(Math.round(.6f * 255f), seeThrough.bitmapLog.get(0).alpha, "the first fades out");
        equal(Math.round(.4f * 255f), seeThrough.bitmapLog.get(1).alpha, "as the second comes in");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .7f, false);
        Canvas pulling = rig.draw();
        equal(1, pulling.layers, "still being pulled: the pair is added in one layer");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        rig.wallpaper.onHorizontalProgress(Float.NaN); rig.wallpaper.onHorizontalProgress(.4f);
        equal(2, rig.draw().bitmapLog.size(), "a bad progress value changes nothing");
        // the classic mode has one panel and ignores the swipe
        Bundle classic = rig.settings(true); rig.scene(classic, ShadeWallpaperSettings.CLASSIC_PORTRAIT, REVISION, 0f);
        rig.wallpaper.configure(rig.systemUi, classic);
        rig.wallpaper.onPanelChanged(rig.root, "classic", 0, 1f, false); rig.drain();
        rig.wallpaper.onHorizontalProgress(.5f);
        equal(1, rig.draw().bitmapLog.size(), "classic: one picture, whatever the swipe");
        // a picture on one panel only: the other panel shows the same one, so a swipe between them changes nothing
        Rig single = new Rig();
        Bundle one = single.settings(true); single.scene(one, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        single.wallpaper.configure(single.systemUi, one);
        single.wallpaper.onPanelChanged(single.root, "separate", 0, 1f, false); single.drain();
        equal(1, single.reads[0], "one picture is read once, for both panels");
        single.wallpaper.onHorizontalProgress(0f);
        equal(1, single.draw().bitmapLog.size(), "the notification centre has its picture");
        single.wallpaper.onHorizontalProgress(1f);
        Canvas otherPanel = single.draw();
        equal(1, otherPanel.bitmapLog.size(), "the control centre shows the same picture instead of the system background");
        equal(255, otherPanel.bitmapLog.get(0).alpha, "at full strength");
        single.wallpaper.onHorizontalProgress(.4f);
        Canvas between = single.draw();
        equal(0, between.layers, "mid-swipe: no offscreen buffer for a solid picture");
        equal(255, between.bitmapLog.get(0).alpha, "mid-swipe the picture stays at full strength: it is whole under its own copy");
        for (int i = 1; i < between.bitmapLog.size(); i++) check(!between.bitmapLog.get(i).additive, "the copy over it is drawn over, never added to more than one");
        // the other way round: the picture is on the control centre only
        Rig controlOnly = new Rig();
        Bundle secondValues = controlOnly.settings(true); controlOnly.scene(secondValues, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        controlOnly.wallpaper.configure(controlOnly.systemUi, secondValues);
        controlOnly.wallpaper.onPanelChanged(controlOnly.root, "separate", 0, 1f, false); controlOnly.drain();
        controlOnly.wallpaper.onHorizontalProgress(0f);
        equal(1, controlOnly.draw().bitmapLog.size(), "the notification centre shows the control centre's picture");
    }

    private static int mean(Bitmap bitmap, int fromRow, int toRow) {
        long sum = 0; int count = 0;
        for (int y = fromRow; y < toRow; y++) for (int x = 0; x < bitmap.getWidth(); x++) {
            int p = bitmap.pixels[y * bitmap.getWidth() + x];
            sum += (((p >> 16) & 255) + ((p >> 8) & 255) + (p & 255)) / 3; count++;
        }
        return count == 0 ? 0 : (int) (sum / count);
    }
    private static void clockZoneAndGlass() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true);
        rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 2f);
        values.putFloat(ShadeWallpaperSettings.brightnessKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 100f);
        values.putBoolean(ShadeWallpaperSettings.zoneEnabledKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), true);
        values.putFloat(ShadeWallpaperSettings.zoneBlurKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 40f);
        values.putFloat(ShadeWallpaperSettings.zoneFeatherKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 100f);
        values.putBoolean(ShadeWallpaperSettings.zoneFollowKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), false);
        values.putFloat(ShadeWallpaperSettings.zoneHeightKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 200f);
        rig.wallpaper.configure(rig.systemUi, values);
        // the glass is for the panel's picture while it shows: nothing before the panel is open and loaded
        equal(null, rig.wallpaper.glass(), "no glass before anything is loaded");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        rig.wallpaper.onLayoutChanged(rig.root); rig.drain();
        ShadeWallpaper.Glass glass = rig.wallpaper.glass();
        check(glass != null, "an open panel with a picture has glass");
        equal(1440, glass.width, "the glass is mapped over the window's width");
        equal(3168, glass.height, "and its height");
        check(glass.bitmap.getWidth() <= ShadeWallpaper.SMALL_EDGE && glass.bitmap.getHeight() <= ShadeWallpaper.SMALL_EDGE, "it is a small copy");
        // zone: the top of the picture is blurred more than the bottom (rows alternate light/dark: a stronger blur flattens them more)
        int w = glass.bitmap.getWidth(), h = glass.bitmap.getHeight();
        int topSpread = spread(glass.bitmap, 25, 50), bottomSpread = spread(glass.bitmap, h - 40, h - 4);
        check(topSpread < bottomSpread, "the zone at the top is blurrier than the rest: " + topSpread + " < " + bottomSpread);
        // the zone's own signed lift works on the zone only: -100 darkens the picture under the clock, +100 whitens it, the rest stays
        int bottomBefore = mean(glass.bitmap, h - 40, h - 4);
        for (float lift : new float[]{-100f, 100f}) {
            Bundle lifted = new Bundle();
            for (String key : values.keySet()) lifted.putForWallpaperCheck(key, values.get(key));
            lifted.putFloat(ShadeWallpaperSettings.zoneLiftKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), lift);
            rig.wallpaper.configure(rig.systemUi, lifted); rig.drain(); rig.wallpaper.onLayoutChanged(rig.root); rig.drain();
            ShadeWallpaper.Glass changed = rig.wallpaper.glass();
            check(changed != null, "the glass is rebuilt with the lift");
            int top = mean(changed.bitmap, 25, 50);
            check(lift < 0 ? top <= 4 : top >= 251, "a lift of " + lift + " at the top: " + top);
            check(Math.abs(mean(changed.bitmap, h - 40, h - 4) - bottomBefore) <= 2, "the rest of the picture is not touched by the zone's lift");
        }
        rig.wallpaper.configure(rig.systemUi, values); rig.drain(); rig.wallpaper.onLayoutChanged(rig.root); rig.drain();
        // following the clock: the zone's edge moves, and the picture is rebuilt, but only for a real change
        Bundle follow = rig.settings(true);
        rig.scene(follow, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 2f);
        follow.putBoolean(ShadeWallpaperSettings.zoneEnabledKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), true);
        follow.putFloat(ShadeWallpaperSettings.zoneBlurKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 40f);
        follow.putFloat(ShadeWallpaperSettings.zoneFeatherKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 100f);
        rig.wallpaper.configure(rig.systemUi, follow); rig.drain(); rig.wallpaper.onLayoutChanged(rig.root); rig.drain();
        rig.wallpaper.onClockBottom(600f); rig.drain();
        int rebuilds = rig.reads[0];
        rig.work.clear();
        rig.wallpaper.onClockBottom(600.5f);
        equal(0, rig.work.size(), "a half pixel is not worth a rebuild");
        rig.wallpaper.onClockBottom(900f);
        equal(1, rig.work.size(), "a clock that moved is");
        rig.drain();
        equal(rebuilds, rig.reads[0], "the picture itself is not read again");
        rig.wallpaper.onClockBottom(Float.NaN); rig.drain();
        // not the glass when the panel is the control centre, or a swipe is under way, or it is the lock screen
        rig.wallpaper.onHorizontalProgress(.5f);
        equal(null, rig.wallpaper.glass(), "mid-swipe the glass has no single picture");
        rig.wallpaper.onHorizontalProgress(0f);
        check(rig.wallpaper.glass() != null, "back on the notification centre");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 1, 1f, false);
        equal(null, rig.wallpaper.glass(), "on the lock screen there is no panel wallpaper");
        rig.wallpaper.onPanelChanged(rig.root, "classic", 0, 1f, false); rig.drain();
        equal(null, rig.wallpaper.glass(), "the classic mode has no big clock");
    }
    /** How far apart the lightest and darkest rows of a band are: a blur flattens the picture's alternating rows. */
    private static int spread(Bitmap bitmap, int from, int to) {
        int lo = 255, hi = 0, w = bitmap.getWidth();
        for (int y = from; y < to; y++) {
            int red = (bitmap.pixels[y * w + w / 4] >> 16) & 255;
            lo = Math.min(lo, red); hi = Math.max(hi, red);
        }
        return hi - lo;
    }

    private static void settingChangesKeepThePicture() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 6f);
        rig.wallpaper.configure(rig.systemUi, values);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        Object shown = rig.frame();
        check(shown != null, "loaded");
        int reads = rig.reads[0];
        // only the pull behaviour: nothing is rebuilt
        Bundle pull = new Bundle(values); pull.putBoolean(ShadeWallpaperSettings.BLUR_WITH_PULL, false);
        rig.wallpaper.configure(rig.systemUi, pull);
        equal(0, rig.work.size(), "the pull behaviour needs no new pixels");
        check(rig.frame() == shown, "and the picture on screen is the same one");
        // the blur slider: the old picture stays until the new one is ready, instead of the native background flashing in
        Bundle slider = new Bundle(values); slider.putFloat(ShadeWallpaperSettings.blurKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), 9f);
        rig.wallpaper.configure(rig.systemUi, slider);
        check(rig.frame() == shown, "a changed blur keeps the shown picture while the new one is made");
        equal(1, rig.work.size(), "and makes one new one");
        rig.drain();
        check(rig.frame() != shown && rig.frame() != null, "then the new one replaces it");
        equal(reads, rig.reads[0], "from the decoded picture still in memory: no second read");
        // a different picture is a different asset: the old one goes at once (it is not the new one)
        Bundle another = new Bundle(slider); another.putString(ShadeWallpaperSettings.revisionKey(ShadeWallpaperSettings.NOTIFICATION_PORTRAIT), OTHER);
        rig.wallpaper.configure(rig.systemUi, another);
        equal(null, rig.frame(), "another picture is never shown as the old one");
        rig.drain();
        equal(reads + 1, rig.reads[0], "it is read once");
        // turning it off removes everything
        rig.wallpaper.configure(rig.systemUi, rig.settings(false));
        equal(null, rig.frame(), "switched off");
        equal(false, rig.wallpaper.wanted(), "and not wanted");
        equal(0, rig.draw().bitmapDraws, "nothing is drawn");
    }

    /** The system's behind-scrim (the base class carries the flag the real one has) with the hook around its setter, as the module installs it. */
    public static class FakeScrimBase { public boolean isBehind; }
    public static final class FakeScrim extends FakeScrimBase {
        static ShadeWallpaperScrimBlur hook;
        float applied = Float.NaN; int calls;
        FakeScrim(boolean behind) { isBehind = behind; }
        public void setBlurAmount(float amount, String from) { calls++; applied = hook == null ? amount : hook.amount(this, amount); }
    }

    /** The system's full-screen blur gives up as much as the picture shows, and gets it back when the picture goes. */
    private static void scrimBlurShare() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.wallpaper.configure(rig.systemUi, values);
        equal(0f, rig.wallpaper.nativeBlurShare(), "no layer: the system's blur is untouched");
        final boolean[] ready = {false};
        rig.wallpaper.useLayer(new ShadeWallpaper.Layer() {
            @Override public boolean ready() { return ready[0]; }
            @Override public void sync(Context context, boolean visible) { }
            @Override public void release() { }
        });
        ShadeWallpaperScrimBlur blur = new ShadeWallpaperScrimBlur(rig.wallpaper);
        java.lang.reflect.Method setter = FakeScrim.class.getDeclaredMethod("setBlurAmount", Float.TYPE, String.class);
        blur.bind(FakeScrimBase.class, setter);
        FakeScrim.hook = blur;
        FakeScrim behind = new FakeScrim(true), other = new FakeScrim(false);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        equal(0f, rig.wallpaper.nativeBlurShare(), "the layer is not ready yet: the shade paints the picture itself, the system's blur stays");
        behind.setBlurAmount(1f, "system");
        equal(1f, behind.applied, "so the behind-scrim keeps its blur");
        ready[0] = true;
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        equal(1f, rig.wallpaper.nativeBlurShare(), "the picture shows whole");
        close(0f, behind.applied, 0f, "the blur that was already asked for is taken away as soon as the picture is whole (no new call by the system)");
        behind.setBlurAmount(.8f, "system");
        close(0f, behind.applied, 0f, "and every later ask is taken away too");
        other.setBlurAmount(.8f, "system");
        close(.8f, other.applied, 0f, "the scrims that are not the behind one are left alone");
        // part of the pull: the same share of the system's blur goes
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .7f, false);
        float share = WallpaperPixels.pullCurve(.7f);
        close(share, rig.wallpaper.nativeBlurShare(), 1e-5f, "the share follows the pull curve");
        close(.8f * (1f - share), behind.applied, 1e-4f, "the scrim keeps what the picture does not cover");
        behind.setBlurAmount(.5f, "system");
        close(.5f * (1f - share), behind.applied, 1e-4f, "also for a new ask");
        // closed: the system's blur is back, at the amount it last asked for
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 0f, true);
        equal(0f, rig.wallpaper.nativeBlurShare(), "closed: nothing is replaced");
        close(.5f, behind.applied, 1e-5f, "the system's own last amount is back");
        // barely pulled: no picture yet, the system's blur is its own
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .1f, false);
        equal(0f, rig.wallpaper.nativeBlurShare(), "the first sixth of the pull: no picture");
        // a non-finite ask is passed on as it is
        equal(Float.POSITIVE_INFINITY, blur.amount(behind, Float.POSITIVE_INFINITY), "not a number of ours");
        FakeScrim.hook = null;
    }

    /** A swipe hands the shade from one panel to the other; their pulls add up, the picture never dips. */
    private static void handOverNeverDips() {
        close(1f, ShadeWallpaper.pullOf(true, 1f, 0f), 0f, "notification centre pulled");
        close(1f, ShadeWallpaper.pullOf(true, 0f, 1f), 0f, "control centre pulled");
        close(1f, ShadeWallpaper.pullOf(true, .5f, .5f), 0f, "half and half: the picture stays whole");
        close(.6f, ShadeWallpaper.pullOf(true, .6f, 0f), 0f, "one panel being pulled: its own pull");
        close(.3f, ShadeWallpaper.pullOf(true, 0f, .3f), 0f, "the other panel being pulled");
        close(1f, ShadeWallpaper.pullOf(true, 1f, 1f), 0f, "never more than one");
        close(.4f, ShadeWallpaper.pullOf(false, .4f, .9f), 0f, "the classic shade has one panel: the second is ignored");
        close(0f, ShadeWallpaper.pullOf(true, Float.NaN, -1f), 0f, "bad values are nothing");
    }

    /** The picture is shown by a layer under the shade once there is one: the shade itself then paints nothing, and the layer is told when to show. */
    private static void layerUnderTheShade() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        values.putBoolean(ShadeWallpaperSettings.BLUR_WITH_PULL, false);
        rig.wallpaper.configure(rig.systemUi, values);
        final boolean[] ready = {false};
        final List<Object> told = new ArrayList<>();
        rig.wallpaper.useLayer(new ShadeWallpaper.Layer() {
            @Override public boolean ready() { return ready[0]; }
            @Override public void sync(Context context, boolean visible) { told.add(visible); }
            @Override public void release() { told.add("released"); }
        });
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        check(told.size() > 0 && Boolean.TRUE.equals(told.get(told.size() - 1)), "an open panel with its picture loaded: the layer is told to show");
        // not ready yet (the window is being made): the shade paints the picture itself
        check(rig.draw().bitmapDraws > 0, "a layer that is not ready leaves the painting inside the shade");
        ready[0] = true;
        equal(0, rig.draw().bitmapDraws, "with the layer ready the shade paints nothing");
        Canvas layerCanvas = new Canvas();
        rig.wallpaper.paintLayer(layerCanvas, 1440, 3168);
        check(layerCanvas.bitmapDraws > 0, "and the layer paints the picture");
        // the first sixth of the pull is nothing: the layer stays hidden
        told.clear();
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .1f, false);
        check(told.size() > 0 && Boolean.FALSE.equals(told.get(told.size() - 1)), "barely pulled: the layer is hidden");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .7f, false);
        check(Boolean.TRUE.equals(told.get(told.size() - 1)), "pulled far enough: shown");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 0f, true);
        check(Boolean.FALSE.equals(told.get(told.size() - 1)), "closed: hidden again");
        // a layer that goes away leaves the painting to the shade
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        rig.wallpaper.useLayer(null);
        check(told.contains("released"), "the layer is released when it is replaced");
        check(rig.draw().bitmapDraws > 0, "without a layer the shade paints again");
    }

    /** Device code162: a swipe re-places the layer's two drawn pictures; it never draws them again or redraws the shade. */
    private static void layeredSwipe() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true);
        rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.scene(values, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        rig.wallpaper.configure(rig.systemUi, values);
        final int[] composed = {0}, synced = {0};
        rig.wallpaper.useLayer(new ShadeWallpaper.Layer() {
            @Override public boolean ready() { return true; }
            @Override public void sync(Context context, boolean visible) { synced[0]++; }
            @Override public void release() { }
            @Override public void compose() { composed[0]++; }
        });
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        int revision = rig.wallpaper.contentRevision(), syncs = synced[0];
        ShadeWallpaper.Composition out = new ShadeWallpaper.Composition();
        rig.wallpaper.compose(out, 1440);
        check(out.alpha0 == 1f && out.alpha1 == 0f, "the notification centre alone before the swipe");
        rig.wallpaper.onHorizontalProgress(.4f);
        equal(1, composed[0], "a swipe step re-places the pictures");
        equal(revision, rig.wallpaper.contentRevision(), "a swipe step never asks for the pictures to be drawn again");
        equal(syncs, synced[0], "nor for the layer to be synced again");
        rig.wallpaper.compose(out, 1440);
        check(out.alpha0 == 1f && Math.abs(out.alpha1 - .4f) < 1e-6f && !out.additive && out.shift0 == 0f && out.shift1 == 0f,
                "solid pictures: the second over the whole first by the native progress");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false);
        equal(revision, rig.wallpaper.contentRevision(), "the hand-over repeating the same pull draws nothing again");
        rig.wallpaper.onHorizontalProgress(1f); rig.wallpaper.compose(out, 1440);
        check(out.alpha0 == 0f && out.alpha1 == 1f, "the control centre alone after the swipe");
        Canvas side = new Canvas(); rig.wallpaper.paintSide(side, 1440, 3168, 1);
        equal(1, side.bitmapLog.size(), "each view draws only its own picture");
        equal(255, side.bitmapLog.get(0).alpha, "at the full pull");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .7f, false);
        check(rig.wallpaper.contentRevision() != revision, "a real pull change draws the pictures again");
        rig.wallpaper.onHorizontalProgress(.5f); rig.wallpaper.compose(out, 1440);
        check(out.additive && Math.abs(out.alpha0 - .5f) < 1e-6f && Math.abs(out.alpha1 - .5f) < 1e-6f, "while still pulled the pair is added, never dimmed");
        Bundle sliding = rig.settings(true);
        rig.scene(sliding, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.scene(sliding, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        sliding.putBoolean(ShadeWallpaperSettings.SWIPE_SLIDE, true);
        rig.wallpaper.configure(rig.systemUi, sliding); rig.drain();
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        rig.wallpaper.onHorizontalProgress(.25f); rig.wallpaper.compose(out, 1000);
        check(out.alpha0 == 1f && out.alpha1 == 1f && out.shift0 == -250f && out.shift1 == 750f, "sliding: both whole, moved with the pages");
    }

    /** Sliding with the pages: the two pictures move instead of cross-fading, but only when they are two different pictures. */
    private static void slidingPages() throws Exception {
        Rig rig = new Rig();
        Bundle values = rig.settings(true);
        rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        rig.scene(values, ShadeWallpaperSettings.CONTROL_PORTRAIT, OTHER, 0f);
        values.putBoolean(ShadeWallpaperSettings.SWIPE_SLIDE, true);
        rig.wallpaper.configure(rig.systemUi, values);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        rig.wallpaper.onHorizontalProgress(.4f);
        Canvas sliding = rig.draw();
        equal(0, sliding.layers, "sliding needs no layer to add the pictures");
        equal(2, sliding.bitmapLog.size(), "both pictures are drawn");
        equal(255, sliding.bitmapLog.get(0).alpha, "each at full strength, they move instead of fading");
        equal(255, sliding.bitmapLog.get(1).alpha, "the other too");
        // one picture for both panels: nothing to slide
        Rig same = new Rig();
        Bundle one = same.settings(true); same.scene(one, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 0f);
        one.putBoolean(ShadeWallpaperSettings.SWIPE_SLIDE, true);
        same.wallpaper.configure(same.systemUi, one);
        same.wallpaper.onPanelChanged(same.root, "separate", 0, 1f, false); same.drain();
        same.wallpaper.onHorizontalProgress(.4f);
        Canvas still = same.draw();
        equal(0, still.layers, "the same solid picture on both panels needs no offscreen buffer");
        equal(255, still.bitmapLog.get(0).alpha, "the same picture on both panels does not move: it is whole");
        for (int i = 1; i < still.bitmapLog.size(); i++) check(!still.bitmapLog.get(i).additive, "and nothing is added to it");
    }

    private static void failureAndLifecycle() throws Exception {
        Rig rig = new Rig();
        rig.failRead = true;
        Bundle values = rig.settings(true); rig.scene(values, ShadeWallpaperSettings.NOTIFICATION_PORTRAIT, REVISION, 5f);
        rig.wallpaper.configure(rig.systemUi, values);
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, 1f, false); rig.drain();
        equal(1, rig.reads[0], "tried once");
        equal(0, rig.draw().bitmapDraws, "an unreadable picture: the native background");
        rig.wallpaper.onPanelChanged(rig.root, "separate", 0, .9f, false); rig.wallpaper.onLayoutChanged(rig.root); rig.drain();
        equal(1, rig.reads[0], "no retry on every frame");
        // detaching and retiring release everything
        rig.failRead = false;
        Rig fresh = new Rig();
        fresh.wallpaper.configure(fresh.systemUi, values);
        fresh.wallpaper.onPanelChanged(fresh.root, "separate", 0, 1f, false); fresh.drain();
        check(fresh.frame() != null, "loaded");
        fresh.wallpaper.onDetached(new Window(fresh.systemUi));
        check(fresh.frame() != null, "another view detaching changes nothing");
        fresh.wallpaper.onDetached(fresh.root); fresh.drain();
        equal(null, fresh.frame(), "the shade window going away releases the picture");
        equal(0, fresh.draw().bitmapDraws, "and draws nothing");
        fresh.wallpaper.reset(); fresh.drain();
        equal(null, field(ShadeWallpaper.class, "context").get(fresh.wallpaper), "retirement forgets the context");
        // a task that was queued before a change finds the world moved on
        Rig stale = new Rig();
        stale.wallpaper.configure(stale.systemUi, values);
        stale.wallpaper.onPanelChanged(stale.root, "separate", 0, 1f, false);
        stale.wallpaper.configure(stale.systemUi, stale.settings(false));
        stale.drain();
        equal(null, stale.frame(), "a load that finished after the switch went off is not shown");
        // the normal constructor starts a real worker and an unconfigured instance is idle
        ShadeWallpaper normal = new ShadeWallpaper();
        equal(false, normal.wanted(), "idle until configured");
        normal.reset();
    }
}
