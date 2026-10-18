package dev.puitheme;

import java.util.HashMap;
import java.util.Map;

/** The code100 batch outside the fluid cloud: the big clock blurs only on the way back up, the control centre's two top buttons can be
 * hidden, and every "过渡范围" means the same thing in the same words. */
public final class Code100Check {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }

    /** The rebound phase (code101): only a clock held above its resting place blurs; pulling down and the spring back up never do. */
    private static void reboundPhase() {
        float density = 4f;
        boolean phase = false;
        float previous = 0f;
        for (float travel = 0f; travel <= 200f; travel += 20f) {
            phase = NotificationBigClock.reboundPhase(phase, previous, travel, density);
            check(!phase, "pulling down to " + travel + " px never blurs");
            previous = travel;
        }
        for (float travel = 180f; travel >= 0f; travel -= 20f) {
            phase = NotificationBigClock.reboundPhase(phase, previous, travel, density);
            check(!phase, "the spring back up from " + travel + " px after a pull-down never blurs");
            previous = travel;
        }
        for (float travel = -20f; travel >= -200f; travel -= 20f)
            check(NotificationBigClock.reboundPhase(false, travel + 20f, travel, density), "pushed up to " + travel + " px: blurs");
        for (float travel = -180f; travel <= -4f; travel += 20f)
            check(NotificationBigClock.reboundPhase(true, travel - 20f, travel, density), "coming back down from above: still blurs");
        check(!NotificationBigClock.reboundPhase(true, -8f, -1f, density), "at rest (under half a dp) there is nothing to blur");
        check(!NotificationBigClock.reboundPhase(true, 8f, Float.NaN, density) && !NotificationBigClock.reboundPhase(true, -8f, -20f, 0f),
                "no travel or no density: no rebound");
    }

    private static void entrances() {
        check(QsEntrances.EDIT_HIDDEN.equals(NetworkBadgeControls.keyFor("qs_edit_button")), "the edit button");
        check(QsEntrances.EDIT_HIDDEN.equals(NetworkBadgeControls.keyFor("edit_button_little_red_dot")), "its red dot goes with it");
        check(QsEntrances.SETTINGS_HIDDEN.equals(NetworkBadgeControls.keyFor("quicksettings_settings_button")), "the settings button");
        check(QsEntrances.keyFor("qs_quick_entrance_container") == null, "the container itself is never hidden");
        for (String key : new String[]{QsEntrances.MASTER, QsEntrances.EDIT_HIDDEN, QsEntrances.SETTINGS_HIDDEN})
            check(Boolean.FALSE.equals(FeatureOptions.DEFAULTS.get(key)), key + " is off by default");
        SettingsCatalog.Group group = SettingsCatalog.group(QsEntrances.GROUP);
        check(group != null && SettingsCatalog.CONTROL_CENTER.equals(group.category) && QsEntrances.MASTER.equals(group.masterKey), "a control-centre page with its master");
        check(SettingsCatalog.item(QsEntrances.EDIT_HIDDEN) != null && SettingsCatalog.item(QsEntrances.SETTINGS_HIDDEN) != null, "both switches are listed");
        boolean listed = false;
        for (SettingsCatalog.Group shown : SettingsCatalog.groups(SettingsCatalog.CONTROL_CENTER)) listed |= shown == group;
        check(listed, "the page is reachable from the control-centre navigation");
        Map<String, Object> values = new HashMap<>();
        values.put(QsEntrances.MASTER, true); values.put(QsEntrances.EDIT_HIDDEN, true);
        FeatureOptions options = FeatureOptions.from(values);
        check(options.effective(QsEntrances.GROUP, QsEntrances.EDIT_HIDDEN) && !options.effective(QsEntrances.GROUP, QsEntrances.SETTINGS_HIDDEN),
                "only the chosen button is hidden");
        values.put(QsEntrances.MASTER, false);
        check(!FeatureOptions.from(values).effective(QsEntrances.GROUP, QsEntrances.EDIT_HIDDEN), "the master off shows both again");
    }

    /** One wording and one scale for every transition range. */
    private static void ranges() {
        int found = 0;
        for (SettingsCatalog.Item item : SettingsCatalog.allItems()) {
            if (!"过渡范围".equals(item.title)) continue;
            found++;
            if ("%".equals(item.unit)) {
                // A share of the card or capsule: 100 % is the old look.
                check(item.min == 5f && item.max == 100f, item.key + ": 5 to 100 %");
                check(item.description.contains(SettingsCatalog.RANGE_BETWEEN) || item.description.contains(SettingsCatalog.RANGE_FADE),
                        item.key + ": one of the shared explanations");
                check(item.description.contains("100%"), item.key + ": says what 100 % is");
            } else {
                check("dp".equals(item.unit) && item.description.contains(SettingsCatalog.RANGE_DISTANCE), item.key + ": a distance, in the shared words");
            }
        }
        // Capsule, lock screen, tiles light/dark, clear button portrait/landscape, the inward glow, the notification edge.
        check(found >= 8, "every gradient has its range: " + found);
        check(SettingsCatalog.item(FluidCloudCapsule.GLOW_DEPTH) != null && "过渡范围".equals(SettingsCatalog.item(FluidCloudCapsule.GLOW_DEPTH).title),
                "the inward glow's depth is a transition range too");
    }

    /** The album cover keeps the plugin's circle while the capsule is collapsed on the camera and reaches the chosen radius as it
     * expands; the contact links are the author's. */
    private static void coverAndLinks() {
        android.view.View cover = new android.view.View(new android.content.Context());
        cover.layout(0, 0, 80, 80);
        check(CapsuleCover.shown(cover, 12f, 0f) == 40f, "collapsed: the circle (half the 80 px cover)");
        check(CapsuleCover.shown(cover, 12f, 1f) == 12f, "expanded: the chosen radius");
        check(CapsuleCover.shown(cover, 12f, .5f) == 26f, "half way: half way between");
        check(CapsuleCover.shown(cover, 12f, 3f) == 12f && CapsuleCover.shown(cover, 12f, -1f) == 40f, "clamped");
        check("https://www.coolapk.com/u/745949".equals(ProjectContact.COOLAPK_URL), "the Coolapk entry opens the author's profile");
        check(ProjectContact.DONATION_NOTE.contains("金额不限") && ProjectContact.DONATION_NOTE.contains("测试群")
                && ProjectContact.DONATION_NOTE.contains("正式版"), "the donation note says the three things");
        check(FreeNotice.DESCRIPTION.contains(ProjectContact.DONATION_NOTE), "the free notice carries it too");
    }

    /** Public 1.8.2 leaves later stacked no-voice mappings to the genuine system renderer. */
    private static void cellular() {
        for(String resource:new String[]{"stat_signal_signal_stacked_novoice_3","stat_signal_single_stacked_novoice_0",
                "stat_signal_single_stacked_novoice_secondary_2","stat_signal_signal_stacked_novoice_4"})
            for(boolean single:new boolean[]{false,true})check(SignalResources.moduleName(resource,single)==null,"public resource selection retains unknown native glyphs");
        check("c17_signal_single_3".equals(SignalResources.moduleName("stat_signal_lte_signal_stacked_primary_3",true)),"public known primary level");
        check("c17_signal_empty".equals(SignalResources.moduleName("stat_signal_lte_signal_stacked_secondary_3",true)),"public known secondary row");
        check(SignalResources.moduleName("stat_signal_stacked_activity_in_public",true)==null,"activity arrows are not signal levels");
        check(Boolean.FALSE.equals(FeatureOptions.DEFAULTS.get(SignalResources.ROAMING_HIDDEN)),"historical roaming setting defaults off");
    }

    /** The appear bounce: only the plugin's two overshoot sizes, only from its own Resources, only while the option is on. */
    private static void bounce() {
        CapsuleEffects effects = new CapsuleEffects();
        android.content.res.Resources plugin = new android.content.res.Resources(), systemUi = new android.content.res.Resources();
        plugin.entryNames.put(1, CapsuleEffects.WIDTH); plugin.entryNames.put(2, CapsuleEffects.HEIGHT); plugin.entryNames.put(3, "capsule_view_max_width");
        systemUi.entryNames.put(1, "status_bar_padding");
        android.view.View capsule = new android.view.View(new android.content.Context()) {
            @Override public android.content.res.Resources getResources() { return plugin; }
        };
        effects.capsuleSeen(capsule);
        check(effects.pixelOffset(plugin, 1, 64) == 64, "off: the plugin's own bounce");
        android.os.Bundle settings = new android.os.Bundle();
        settings.putBoolean(FluidCloudCapsule.MASTER, true); settings.putBoolean(CapsuleEffects.BOUNCE_ENABLED, true); settings.putFloat(CapsuleEffects.BOUNCE, 0f);
        effects.configure(settings);
        check(effects.pixelOffset(plugin, 1, 64) == 0 && effects.pixelOffset(plugin, 2, 12) == 0, "0 %: no bounce");
        check(effects.pixelOffset(plugin, 3, 900) == 900, "other plugin sizes are untouched");
        check(effects.pixelOffset(systemUi, 1, 40) == 40, "a SystemUI resource with the same number is untouched");
        settings.putFloat(CapsuleEffects.BOUNCE, 150f); effects.configure(settings);
        check(effects.pixelOffset(plugin, 1, 64) == 96, "150 %");
        settings.putFloat(CapsuleEffects.BOUNCE, 900f); effects.configure(settings);
        check(effects.pixelOffset(plugin, 1, 64) == 128, "held to 200 %");
        settings.putBoolean(FluidCloudCapsule.MASTER, false); effects.configure(settings);
        check(!effects.active() && effects.pixelOffset(plugin, 1, 64) == 64, "the capsule page off: native");
        check(Boolean.FALSE.equals(FeatureOptions.DEFAULTS.get(CapsuleEffects.BOUNCE_ENABLED))
                && SettingsCatalog.item(CapsuleEffects.BOUNCE) != null, "off by default, on the capsule page");
    }

    /** The weight plans are reused, not rebuilt: many families no longer empty the whole cache, and a family seen again is a hit. */
    private static void fontPlans() {
        FontWeight.release();
        android.graphics.Typeface[] families = new android.graphics.Typeface[60];
        for (int i = 0; i < families.length; i++) families[i] = android.graphics.Typeface.create("family" + i, 0);
        for (android.graphics.Typeface family : families) FontWeight.typeface(family, 700);
        long[] before = FontWeight.stats();
        for (android.graphics.Typeface family : families) FontWeight.typeface(family, 700);
        long[] after = FontWeight.stats();
        check(after[1] == before[1], "60 families seen again: no face is built again (" + (after[1] - before[1]) + " rebuilt)");
        check(after[0] - before[0] == families.length && after[2] == 0, "every one a hit, nothing evicted");
        FontWeight.typeface(families[0], 700);
        check(FontWeight.stats()[1] == after[1], "still a hit");
        FontWeight.release();
    }

    /** Built-in cache starts before settings arrive; old switch values cannot disable it and safety mode retains files. */
    private static void residentCache() throws Exception {
        java.io.File root = java.nio.file.Files.createTempDirectory("c17-resident").toFile();
        android.content.Context context = new android.content.Context() {
            @Override public java.io.File getFilesDir() { return root; }
        };
        ResidentCache cache = new ResidentCache();
        android.os.Bundle settings = new android.os.Bundle();
        check(Boolean.FALSE.equals(StatusBarSettings.BOOLEAN_DEFAULTS.get(ResidentCache.INSTALLED))
                && Boolean.FALSE.equals(StatusBarSettings.BOOLEAN_DEFAULTS.get(ResidentCache.RUNNING)),
                "legacy phone-local state defaults remain false for framework mirrors and old migrations");
        cache.initialize(context);
        java.io.File store = new java.io.File(root, ResidentCache.FOLDER);
        settle(cache);
        check(store.isDirectory() && cache.report().getBoolean(ResidentCache.R_INSTALLED, false)
                && cache.report().getBoolean(ResidentCache.R_RUNNING, false), "first usable context enables real cache despite false legacy defaults, without settings or an install action");
        settings.putBoolean(ResidentCache.INSTALLED, false); settings.putBoolean(ResidentCache.RUNNING, false);
        cache.configure(context, settings); settle(cache);
        check(store.isDirectory() && cache.report().getBoolean(ResidentCache.R_RUNNING, false), "legacy false preferences cannot disable the built-in cache");
        check(!settings.getBoolean(ResidentCache.INSTALLED, true) && !settings.getBoolean(ResidentCache.RUNNING, true), "raw legacy values are preserved");
        java.io.File font = new java.io.File(store, "font-abc.font");
        java.nio.file.Files.write(font.toPath(), new byte[]{1, 2, 3});
        check(font.equals(cache.font(context, "abc")), "runtime default-on reads a cached font while legacy settings and defaults remain false");
        android.os.Bundle report = cache.report();
        check(report.getBoolean(ResidentCache.R_INSTALLED, false) && report.getBoolean(ResidentCache.R_RUNNING, false) && report.getInt(ResidentCache.R_FILES, -1) == 1
                && report.getLong(ResidentCache.R_BYTES, -1L) == 3 && report.getLong(ResidentCache.R_HITS, -1L) == 1, "the report is what is on disk");
        // Icon libraries and the notification/control centre files: read from the app once, then from the store.
        int[] opened = {0};
        String library = "0123456789abcdef0123456789abcdef";
        byte[] glyph = cache.iconEntry(library, "wifi/3.png", () -> { opened[0]++; return new java.io.ByteArrayInputStream(new byte[]{9, 8, 7}); }, 1024);
        byte[] again = cache.iconEntry(library, "wifi/3.png", () -> { opened[0]++; return new java.io.ByteArrayInputStream(new byte[]{1}); }, 1024);
        check(java.util.Arrays.equals(glyph, new byte[]{9, 8, 7}) && java.util.Arrays.equals(again, glyph) && opened[0] == 1, "an icon is read from the app once, then from the store");
        byte[] wall = cache.entry("wallpaper-classic_portrait", "rev1", () -> new java.io.ByteArrayInputStream(new byte[]{1, 1}), 1024);
        byte[] wallAgain = cache.entry("wallpaper-classic_portrait", "rev1", () -> { opened[0]++; return null; }, 1024);
        check(java.util.Arrays.equals(wall, wallAgain) && opened[0] == 1, "a wallpaper of the same revision comes from the store");
        cache.entry("wallpaper-classic_portrait", "rev2", () -> new java.io.ByteArrayInputStream(new byte[]{2, 2, 2}), 1024);
        int wallpapers = 0;
        for (java.io.File file : store.listFiles()) if (file.getName().startsWith("file-wallpaper-classic_portrait-")) wallpapers++;
        check(wallpapers == 1, "a newer wallpaper of the same scene replaces the older copy");
        cache.retainIcons(java.util.Collections.<String>emptySet()); settle(cache);
        boolean iconsLeft = false;
        for (java.io.File file : store.listFiles()) iconsLeft |= file.getName().startsWith("icon-");
        check(!iconsLeft, "icons of libraries no longer in use are removed");
        settings.putBoolean(StatusBarSettings.SAFE_MODE, true); cache.configure(context, settings); settle(cache);
        check(cache.font(context, "abc") == null && font.isFile(), "safety mode pauses reads and keeps cached files");
        cache.initialize(context); settle(cache);
        check(!cache.report().getBoolean(ResidentCache.R_RUNNING, true) && font.isFile(), "repeated initialization cannot reset a safety-mode pause");
        int storedFiles = store.listFiles().length;
        cache.iconEntry(library, "wifi/paused.png", () -> new java.io.ByteArrayInputStream(new byte[]{4, 5}), 1024);
        cache.entry("paused-file", "rev", () -> new java.io.ByteArrayInputStream(new byte[]{6}), 1024);
        cache.retainIcons(java.util.Collections.<String>emptySet()); settle(cache);
        check(store.listFiles().length == storedFiles && font.isFile(), "safety-mode reads do not write or prune cache files");
        settings.putBoolean(StatusBarSettings.SAFE_MODE, false); cache.configure(context, settings); settle(cache);
        check(cache.report().getBoolean(ResidentCache.R_INSTALLED, false) && font.equals(cache.font(context, "abc")), "leaving safety mode resumes the same persisted cache despite raw false values");
        check("a_b_c".equals(ResidentCache.safe("a/b.c")) && ResidentCache.safe("x".repeat(200)).length() == 64, "file names are safe and short");
        android.os.Bundle hostile = new android.os.Bundle();
        hostile.putString(ResidentCache.R_ERROR, "e".repeat(5000)); hostile.putLong(ResidentCache.R_BYTES, -9L); hostile.putString("extra", "ignored");
        android.os.Bundle copied = ModuleRuntimeStatus.residentCopy(hostile);
        check(copied.getString(ResidentCache.R_ERROR).length() == 160 && copied.getLong(ResidentCache.R_BYTES, -1L) == 0 && !copied.containsKey("extra"),
                "the app keeps only bounded, known fields of SystemUI's report");
        check(!ConfigTransfer.types().containsKey(ResidentCache.INSTALLED) && !ConfigTransfer.types().containsKey(ResidentCache.RUNNING),
                "this phone's cache state never travels in a configuration file");
        check(SettingsCatalog.hiddenKeys().contains(ResidentCache.INSTALLED) && SettingsCatalog.hiddenKeys().contains(ResidentCache.RUNNING), "both legacy switches are hidden from settings and search");
        boolean cacheControlVisible = false;
        for (SettingsCatalog.Group group : SettingsCatalog.groups()) for (SettingsCatalog.Item item : group.items)
            cacheControlVisible |= ResidentCache.INSTALLED.equals(item.key) || ResidentCache.RUNNING.equals(item.key);
        check(!cacheControlVisible, "no visible or direct settings item controls the built-in cache");
        cache.release();
        cache.initialize(context);
        check(!cache.report().getBoolean(ResidentCache.R_RUNNING, true) && font.isFile(), "release stops cache without deleting files or permitting a later reinitialization");

        ResidentCache pausedBeforeContext = new ResidentCache();
        settings.putBoolean(StatusBarSettings.SAFE_MODE, true);
        pausedBeforeContext.configure(null, settings);
        pausedBeforeContext.initialize(context); settle(pausedBeforeContext);
        check(!pausedBeforeContext.report().getBoolean(ResidentCache.R_RUNNING, true) && font.isFile(), "safety settings received before context remain paused at initialization");
        settings.putBoolean(StatusBarSettings.SAFE_MODE, false);
        pausedBeforeContext.configure(context, settings); settle(pausedBeforeContext);
        check(font.equals(pausedBeforeContext.font(context, "abc")), "a new SystemUI instance reuses persistent cache after safe-mode resume");
        pausedBeforeContext.release();
    }
    private static void settle(ResidentCache cache) throws Exception {
        java.lang.reflect.Field field = ResidentCache.class.getDeclaredField("worker"); field.setAccessible(true);
        ((java.util.concurrent.ExecutorService) field.get(cache)).submit(() -> { }).get(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        residentCache();
        fontPlans();
        bounce();
        cellular();
        coverAndLinks();
        reboundPhase();
        entrances();
        ranges();
        System.out.println("Code100Check: " + checks + " checks passed");
    }
}
