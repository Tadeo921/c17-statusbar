// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** The thanks wall: the public list of supporters kept in SANWU5/c17-supporters (thanks.json at the root, avatars under avatars/).
 * The APK carries the list as it was at build time, with small copies of the avatars; afterwards the last valid download is shown.
 * Opening the wall checks for a newer list only when the last successful check is more than a day old; the wall also has a refresh.
 * A download replaces the cached list only after it has been validated (structure, unique ids, avatar paths inside avatars/); a
 * network failure or an invalid file keeps the list that was shown. Only the settings app reads it; SystemUI never does. */
public final class ThanksWall {
    public static final String REPO_URL = "https://github.com/SANWU5/c17-supporters";
    public static final String MOTTO = "感谢每一位为项目提供捐赠、反馈和帮助的伙伴。你们的支持，让项目得以持续改进，也让每一次更新更有动力。";
    /** Where the list and the avatars are read from, in order: GitHub's raw content, then a CDN mirror of the same repository. */
    static final String[] BASES = {
            "https://raw.githubusercontent.com/SANWU5/c17-supporters/main/",
            "https://cdn.jsdelivr.net/gh/SANWU5/c17-supporters@main/"};
    static final String LIST = "thanks.json";
    static final long CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    static final String ASSET = "thanks/thanks.json", ASSET_AVATARS = "thanks/avatars/";
    static final int AVATAR_PX = 160;
    private static final int TIMEOUT_MS = 8000, MAX_LIST_BYTES = 512 * 1024, MAX_AVATAR_BYTES = 4 * 1024 * 1024;
    private static final String PREFS = "thanks_wall", LAST_CHECK = "last_check", CACHE_FILE = "thanks.json";
    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9_-]{1,63}$");
    private static final Pattern AVATAR = Pattern.compile("^avatars/[a-z0-9_-]+\\.(png|jpg|jpeg|webp)$");
    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final ExecutorService AVATARS = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "C17-thanks-avatar");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Bitmap> DECODED = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) { return size() > 96; }
    });

    public static final class Person {
        public final String id, name, group, avatar, note;
        public final boolean featured;
        public final int order;
        Person(String id, String name, String group, String avatar, String note, boolean featured, int order) {
            this.id = id; this.name = name; this.group = group; this.avatar = avatar; this.note = note; this.featured = featured; this.order = order;
        }
    }

    public static final class Group {
        public final String id, title;
        Group(String id, String title) { this.id = id; this.title = title; }
    }

    /** One validated list: its groups in display order and its visible people in display order. */
    public static final class Wall {
        public final int revision;
        public final String updatedAt;
        public final List<Group> groups;
        public final List<Person> people;
        Wall(int revision, String updatedAt, List<Group> groups, List<Person> people) {
            this.revision = revision; this.updatedAt = updatedAt;
            this.groups = Collections.unmodifiableList(groups); this.people = Collections.unmodifiableList(people);
        }
        /** The specially thanked, shown in their own area and left out of their group. */
        public List<Person> featured() {
            List<Person> result = new ArrayList<>();
            for (Person person : people) if (person.featured) result.add(person);
            return result;
        }
        public List<Person> members(String group) {
            List<Person> result = new ArrayList<>();
            for (Person person : people) if (!person.featured && person.group.equals(group)) result.add(person);
            return result;
        }
    }

    public interface Callback { void onResult(Result result); }

    public static final class Result {
        /** The new list (null: keep the one shown). */
        public final Wall wall;
        public final String message;
        Result(Wall wall, String message) { this.wall = wall; this.message = message; }
    }

    /** The list to show now: the last valid download, else the one in the APK. */
    public static Wall load(Context context) {
        try {
            File cache = new File(new File(context.getFilesDir(), "thanks"), CACHE_FILE);
            if (cache.isFile() && cache.length() <= MAX_LIST_BYTES) {
                Wall cached = parse(read(new java.io.FileInputStream(cache), MAX_LIST_BYTES));
                Wall bundled = bundled(context);
                // A newer APK can carry a newer list than an old download.
                return bundled != null && bundled.revision > cached.revision ? bundled : cached;
            }
        } catch (IOException | IllegalArgumentException invalid) {
            // An unreadable cache falls back to the APK's own list.
        }
        Wall bundled = bundled(context);
        return bundled != null ? bundled : new Wall(0, "", defaultGroups(), new ArrayList<Person>());
    }

    static Wall bundled(Context context) {
        try { return parse(read(context.getAssets().open(ASSET), MAX_LIST_BYTES)); }
        catch (IOException | IllegalArgumentException missing) { return null; }
    }

    /** True when the last successful check is more than a day old (or there was none). */
    public static boolean due(Context context) {
        long last = prefs(context).getLong(LAST_CHECK, 0L), now = System.currentTimeMillis();
        return last <= 0L || now - last >= CHECK_INTERVAL_MS || now < last;
    }

    public static long lastCheck(Context context) { return prefs(context).getLong(LAST_CHECK, 0L); }

    /** Downloads and validates the list off the main thread; the result always arrives on the main looper. */
    public static void refresh(Context context, Callback callback) {
        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        Handler main = new Handler(Looper.getMainLooper());
        Thread worker = new Thread(() -> {
            Result result = download(app);
            main.post(() -> callback.onResult(result));
        }, "C17-thanks-list");
        worker.setDaemon(true);
        worker.start();
    }

    private static Result download(Context context) {
        String failure = "无法连接名单仓库，继续显示已有名单";
        for (String base : BASES) {
            try {
                Wall wall = parse(fetchText(base + LIST));
                File folder = new File(context.getFilesDir(), "thanks");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("No cache folder");
                File temporary = new File(folder, CACHE_FILE + ".new"), target = new File(folder, CACHE_FILE);
                try (FileOutputStream output = new FileOutputStream(temporary)) {
                    output.write(serialize(wall).getBytes(StandardCharsets.UTF_8));
                }
                if (!temporary.renameTo(target)) throw new IOException("Cache not replaced");
                prefs(context).edit().putLong(LAST_CHECK, System.currentTimeMillis()).apply();
                Wall shown = load(context);
                return new Result(shown, "名单已是最新（版本 " + shown.revision + "）");
            } catch (IllegalArgumentException invalid) {
                failure = "下载到的名单格式不正确，继续显示已有名单";
            } catch (SocketTimeoutException timeout) {
                failure = "连接名单仓库超时，继续显示已有名单";
            } catch (IOException | SecurityException unavailable) {
                // Try the next source.
            }
        }
        return new Result(null, failure);
    }

    /** The avatar of one person (already decoded and cut to a square), from memory, the cache, the APK's copy or a download; null shows
     * the default avatar. The result arrives on the main looper. */
    public static void avatar(Context context, Wall wall, Person person, java.util.function.Consumer<Bitmap> callback) {
        if (person == null || person.avatar == null) { callback.accept(null); return; }
        final String key = wall.revision + "/" + person.avatar;
        Bitmap ready = DECODED.get(key);
        if (ready != null) { callback.accept(ready); return; }
        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        Handler main = new Handler(Looper.getMainLooper());
        AVATARS.execute(() -> {
            Bitmap bitmap = loadAvatar(app, wall, person);
            if (bitmap != null) DECODED.put(key, bitmap);
            main.post(() -> callback.accept(bitmap));
        });
    }

    public static Bitmap cachedAvatar(Wall wall, Person person) {
        return person == null || person.avatar == null ? null : DECODED.get(wall.revision + "/" + person.avatar);
    }

    private static Bitmap loadAvatar(Context context, Wall wall, Person person) {
        String name = person.avatar.substring("avatars/".length());
        File folder = new File(new File(context.getCacheDir(), "thanks-avatars"), "r" + wall.revision);
        File cached = new File(folder, name + ".png");
        if (cached.isFile()) {
            Bitmap bitmap = BitmapFactory.decodeFile(cached.getPath());
            if (bitmap != null) return bitmap;
        }
        Wall bundled = bundled(context);
        // The APK's copy is the right picture while the list is the one the APK was built with.
        if (bundled != null && bundled.revision == wall.revision) {
            try (InputStream input = context.getAssets().open(ASSET_AVATARS + name)) {
                Bitmap bitmap = square(BitmapFactory.decodeStream(input));
                if (bitmap != null) return bitmap;
            } catch (IOException missing) {
                // Download it instead.
            }
        }
        for (String base : BASES) {
            try {
                byte[] bytes = fetch(base + person.avatar, MAX_AVATAR_BYTES);
                Bitmap bitmap = square(decode(bytes));
                if (bitmap == null) continue;
                clearOtherRevisions(folder);
                if (folder.isDirectory() || folder.mkdirs()) try (FileOutputStream output = new FileOutputStream(cached)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                } catch (IOException notCached) {
                    // Shown anyway; fetched again next time.
                }
                return bitmap;
            } catch (IOException | SecurityException unavailable) {
                // Try the next source.
            }
        }
        return null;
    }

    private static void clearOtherRevisions(File current) {
        File[] old = current.getParentFile() == null ? null : current.getParentFile().listFiles();
        if (old == null) return;
        for (File folder : old) if (folder.isDirectory() && !folder.getName().equals(current.getName())) {
            File[] files = folder.listFiles();
            if (files != null) for (File file : files) if (!file.delete()) file.deleteOnExit();
            if (!folder.delete()) folder.deleteOnExit();
        }
    }

    /** Decoded at most about twice the shown size, so a large photo never takes much memory. */
    static Bitmap decode(byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sample = 1;
        while (Math.min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= AVATAR_PX * 2) sample *= 2;
        options.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    /** The middle square of the picture, scaled to the avatar size. */
    static Bitmap square(Bitmap source) {
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) return null;
        int side = Math.min(source.getWidth(), source.getHeight());
        Bitmap cropped = Bitmap.createBitmap(source, (source.getWidth() - side) / 2, (source.getHeight() - side) / 2, side, side);
        return side == AVATAR_PX ? cropped : Bitmap.createScaledBitmap(cropped, AVATAR_PX, AVATAR_PX, true);
    }

    /** Validates one list. Required fields, their types and limits, the two groups, unique ids and avatar paths inside avatars/ are
     * checked; unknown fields are ignored, so a later field in the repository does not break an older APK. Hidden people are left out;
     * the rest are ordered by "order" (100 when absent), equal orders keeping the file's order. */
    public static Wall parse(String json) {
        try {
            if (json == null || json.length() > MAX_LIST_BYTES) throw new IllegalArgumentException("size");
            JSONObject root = new JSONObject(json);
            if (root.optInt("schema_version", -1) != 1 || !(root.opt("schema_version") instanceof Integer))
                throw new IllegalArgumentException("schema_version");
            Object revision = root.opt("revision");
            if (!(revision instanceof Integer) || (Integer) revision < 1) throw new IllegalArgumentException("revision");
            String updated = root.optString("updated_at", "");
            if (!DATE.matcher(updated).matches()) throw new IllegalArgumentException("updated_at");
            JSONArray groupArray = root.optJSONArray("groups");
            if (groupArray == null || groupArray.length() != 2) throw new IllegalArgumentException("groups");
            List<Group> groups = new ArrayList<>();
            Set<String> groupIds = new HashSet<>();
            for (int i = 0; i < groupArray.length(); i++) {
                JSONObject group = groupArray.optJSONObject(i);
                if (group == null) throw new IllegalArgumentException("group");
                String id = group.optString("id", ""), title = group.optString("title", "");
                if (!("donation".equals(id) || "support".equals(id)) || !groupIds.add(id)) throw new IllegalArgumentException("group id");
                if (!(group.opt("title") instanceof String) || length(title) < 1 || length(title) > 30) throw new IllegalArgumentException("group title");
                groups.add(new Group(id, title));
            }
            JSONArray peopleArray = root.optJSONArray("people");
            if (peopleArray == null) throw new IllegalArgumentException("people");
            List<Person> people = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < peopleArray.length(); i++) {
                JSONObject person = peopleArray.optJSONObject(i);
                if (person == null) throw new IllegalArgumentException("person");
                String id = person.optString("id", "");
                if (!(person.opt("id") instanceof String) || !ID.matcher(id).matches() || !ids.add(id)) throw new IllegalArgumentException("id " + id);
                Object nameValue = person.opt("display_name");
                if (!(nameValue instanceof String) || length((String) nameValue) < 1 || length((String) nameValue) > 80)
                    throw new IllegalArgumentException("display_name " + id);
                String group = person.optString("group", "");
                if (!groupIds.contains(group)) throw new IllegalArgumentException("group " + id);
                Object avatarValue = person.opt("avatar");
                String avatar = null;
                if (avatarValue instanceof String) {
                    avatar = (String) avatarValue;
                    if (!AVATAR.matcher(avatar).matches()) throw new IllegalArgumentException("avatar " + id);
                } else if (avatarValue != null && avatarValue != JSONObject.NULL) throw new IllegalArgumentException("avatar " + id);
                Object featured = person.opt("featured"), visible = person.opt("visible"), note = person.opt("note"), order = person.opt("order");
                if (featured != null && !(featured instanceof Boolean) || visible != null && !(visible instanceof Boolean))
                    throw new IllegalArgumentException("flags " + id);
                if (note != null && (!(note instanceof String) || length((String) note) > 200)) throw new IllegalArgumentException("note " + id);
                if (order != null && (!(order instanceof Integer) || (Integer) order < 0)) throw new IllegalArgumentException("order " + id);
                if (Boolean.FALSE.equals(visible)) continue;
                people.add(new Person(id, plain((String) nameValue), group, avatar, note == null ? "" : plain((String) note),
                        Boolean.TRUE.equals(featured), order == null ? 100 : (Integer) order));
            }
            // A stable sort: equal orders keep the file's order.
            List<Person> sorted = new ArrayList<>(people);
            Collections.sort(sorted, (a, b) -> Integer.compare(a.order, b.order));
            return new Wall((Integer) revision, updated, groups, sorted);
        } catch (JSONException malformed) {
            throw new IllegalArgumentException("json", malformed);
        }
    }

    /** The validated list written back for the cache (only fields this version reads). */
    static String serialize(Wall wall) {
        try {
            JSONObject root = new JSONObject();
            root.put("schema_version", 1).put("revision", wall.revision).put("updated_at", wall.updatedAt);
            JSONArray groups = new JSONArray();
            for (Group group : wall.groups) groups.put(new JSONObject().put("id", group.id).put("title", group.title));
            root.put("groups", groups);
            JSONArray people = new JSONArray();
            for (Person person : wall.people) {
                JSONObject entry = new JSONObject().put("id", person.id).put("display_name", person.name).put("group", person.group)
                        .put("featured", person.featured).put("note", person.note).put("order", person.order).put("visible", true);
                entry.put("avatar", person.avatar == null ? JSONObject.NULL : person.avatar);
                people.put(entry);
            }
            root.put("people", people);
            return root.toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static List<Group> defaultGroups() {
        List<Group> groups = new ArrayList<>();
        groups.add(new Group("donation", "捐赠支持"));
        groups.add(new Group("support", "其他支持"));
        return groups;
    }

    private static int length(String text) { return text.codePointCount(0, text.length()); }

    /** Text without control characters (a name or note is one line of plain text). */
    private static String plain(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int offset = 0; offset < text.length();) {
            int point = text.codePointAt(offset);
            offset += Character.charCount(point);
            if (Character.isISOControl(point)) { if (point == '\n') result.append(' '); continue; }
            result.appendCodePoint(point);
        }
        return result.toString().trim();
    }

    private static String fetchText(String url) throws IOException {
        return new String(fetch(url, MAX_LIST_BYTES), StandardCharsets.UTF_8);
    }

    private static byte[] fetch(String url, int limit) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            // A fixed HTTPS address must not be redirected to another origin.
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "C17-StatusBar-thanks-wall");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + connection.getResponseCode());
            if (connection.getContentLengthLong() > limit) throw new IOException("Too large");
            return readBytes(connection.getInputStream(), limit);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String read(InputStream input, int limit) throws IOException {
        return new String(readBytes(input, limit), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(InputStream input, int limit) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[8192];
            int count;
            while ((count = source.read(bytes)) != -1) {
                if (output.size() + count > limit) throw new IOException("Too large");
                output.write(bytes, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private ThanksWall() {}
}
