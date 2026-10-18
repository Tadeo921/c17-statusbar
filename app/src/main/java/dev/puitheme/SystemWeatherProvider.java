// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/** Reads the existing located-city weather cache, independently of clocks and widgets.
 * Contract audited in ColorOS 17 WeatherEffectController.getCurrentCityIndex /
 * getWeatherInfoFromProvider. Never calls its location/update-service request. */
final class SystemWeatherProvider implements AutoCloseable {
    static final Uri CITY = Uri.parse("content://com.oplusos.weather.service.provider.data/attent_city");
    static final Uri WEATHER = Uri.parse("content://com.oplusos.weather.service.provider.data/oplus_weather_info");
    private static final String[] CITY_COLUMNS = {"_id"};
    private static final String[] WEATHER_COLUMNS = {"weather_id", "current_weather", "current_temp", "city_id", "sunrise_time", "sunset_time"};
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Executor worker, delivery;
    private final ContentObserver observer;
    private ContentResolver resolver;
    private Runnable changed;
    private long epoch;
    private boolean pending, dirty, subscribed;

    SystemWeatherProvider() { this(null, null); }
    static boolean needed(Bundle source) {
        if (source == null || SafetyMode.enabled(source)
                || !(NotificationBigClockSettings.enabled(source, false)
                || NotificationBigClockSettings.enabled(source, true))) return false;
        return usesWeather(source, BigClockWidgetsSettings.LEFT_ENABLED, BigClockWidgetsSettings.LEFT_TEXT,
                BigClockWidgetsSettings.DEFAULT_LEFT_TEXT)
                || usesWeather(source, BigClockWidgetsSettings.RIGHT_ENABLED, BigClockWidgetsSettings.RIGHT_TEXT,
                BigClockWidgetsSettings.DEFAULT_RIGHT_TEXT);
    }
    private static boolean usesWeather(Bundle source, String enabled, String text, String fallback) {
        if (!Boolean.TRUE.equals(source.get(enabled))) return false;
        Object template = source.get(text);
        return BigClockTextContent.usesWeather(template instanceof String ? (String) template : fallback);
    }
    /** Executors substitute scheduling only; fixtures exercise the same real provider queries. */
    SystemWeatherProvider(Executor worker, Executor delivery) {
        this.worker = worker == null ? new ThreadPoolExecutor(0, 1, 15L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), task -> {
                    Thread thread = new Thread(task, "C17-weather-cache"); thread.setDaemon(true); return thread;
                }) : worker;
        this.delivery = delivery == null ? task -> main.post(task) : delivery;
        observer = new ContentObserver(main) {
            @Override public void onChange(boolean self) { refresh(); }
            @Override public void onChange(boolean self, Uri uri) { refresh(); }
        };
    }

    void configure(Context context, boolean needed, Runnable changed) {
        ContentResolver next = needed && context != null ? context.getContentResolver() : null;
        synchronized (this) {
            this.changed = changed;
            if (next == resolver && (next == null || subscribed)) return;
            closeSubscription(); resolver = next; epoch++;
            if (next != null) try {
                next.registerContentObserver(CITY, true, observer);
                next.registerContentObserver(WEATHER, true, observer);
                subscribed = true;
            } catch (RuntimeException denied) {
                // A partial registration must not retain a user or duplicate observers.
                try { next.unregisterContentObserver(observer); } catch (RuntimeException ignored) { }
                subscribed = false;
            }
        }
        refresh();
    }

    /** A native wake/language/time-zone event can request a bounded cache read.
     * There is one in-flight task and at most one coalesced follow-up, no timer. */
    void refresh() {
        final ContentResolver selected; final long ticket, generation;
        synchronized (this) {
            if (resolver == null) return;
            dirty = true;
            if (pending) return;
            pending = true; dirty = false;
            selected = resolver; ticket = epoch; generation = SystemWeatherText.generation();
        }
        worker.execute(() -> {
            Result result;
            try { result = read(selected, System.currentTimeMillis()); }
            catch (RuntimeException unavailable) { result = null; }
            final Result obtained = result;
            delivery.execute(() -> complete(selected, ticket, generation, obtained));
        });
    }

    private void complete(ContentResolver selected, long ticket, long generation, Result result) {
        Runnable notify = null; boolean again;
        synchronized (this) {
            // close/reconfigure may have started another generation while this query waited.
            if (epoch != ticket || resolver != selected) return;
            pending = false;
            if (result != null && SystemWeatherText.captureProvider(result, generation)) notify = changed;
            again = dirty; dirty = false;
        }
        if (notify != null) notify.run();
        if (again) refresh();
    }

    private void closeSubscription() {
        if (resolver != null && subscribed) try { resolver.unregisterContentObserver(observer); }
        catch (RuntimeException unavailable) { }
        resolver = null; subscribed = false; pending = dirty = false;
    }
    @Override public synchronized void close() { closeSubscription(); epoch++; changed = null; }

    static final class Result {
        final long cityId;
        final String description, temperature;
        final int code, daylight;
        final WeatherTextIcon.Kind kind;
        Result(long cityId, String description, String temperature, int code, int daylight, WeatherTextIcon.Kind kind) {
            this.cityId = cityId; this.description = description; this.temperature = temperature;
            this.code = code; this.daylight = daylight; this.kind = kind;
        }
        static Result empty() { return new Result(Long.MIN_VALUE, "", "", Integer.MIN_VALUE, -1, WeatherTextIcon.Kind.UNKNOWN); }
    }

    /** Null means denied/unsupported data; an empty Result means a confirmed missing located-city result. */
    static Result read(ContentResolver resolver, long now) {
        if (resolver == null) return null;
        long city;
        try (Cursor cities = resolver.query(CITY, CITY_COLUMNS, "location=?", new String[]{"1"}, null)) {
            if (cities == null) return null;
            if (!cities.moveToFirst()) return Result.empty();
            int column = cities.getColumnIndex("_id");
            if (column < 0 || cities.isNull(column)) return null;
            city = cities.getLong(column);
            if (city == Integer.MAX_VALUE || city < 0) return null;
        }
        try (Cursor weather = resolver.query(WEATHER, WEATHER_COLUMNS, "city_id=? and weather_index=?",
                new String[]{String.valueOf(city), "0"}, "date DESC")) {
            if (weather == null) return null;
            if (!weather.moveToFirst()) return Result.empty();
            int descriptionIndex = weather.getColumnIndex("current_weather"), tempIndex = weather.getColumnIndex("current_temp"),
                    codeIndex = weather.getColumnIndex("weather_id"), cityIndex = weather.getColumnIndex("city_id");
            if (descriptionIndex < 0 || tempIndex < 0 || codeIndex < 0 || cityIndex < 0
                    || weather.isNull(cityIndex) || weather.getLong(cityIndex) != city) return null;
            String description = weather.getString(descriptionIndex);
            if (description == null || description.trim().isEmpty() || "--".equals(description.trim())) return Result.empty();
            String rawTemperature = weather.getString(tempIndex);
            String temperature = rawTemperature != null && rawTemperature.trim().matches("[+−-]?[0-9]{1,3}")
                    ? rawTemperature.trim().replace('−', '-') + "°" : "";
            int code = weather.isNull(codeIndex) ? Integer.MIN_VALUE : weather.getInt(codeIndex);
            int riseIndex = weather.getColumnIndex("sunrise_time"), setIndex = weather.getColumnIndex("sunset_time");
            int daylight = riseIndex < 0 || setIndex < 0 || weather.isNull(riseIndex) || weather.isNull(setIndex) ? -1
                    : daylight(now, weather.getLong(riseIndex), weather.getLong(setIndex));
            return new Result(city, description, temperature, code, daylight, WeatherTextIcon.providerKind(code, daylight));
        }
    }

    /** The audited provider stores milliseconds; match the controller's day-wrap rule.
     * Missing sunrise/sunset never turns a clock-hour guess into a day/night fact. */
    private static int daylight(long now, long sunrise, long sunset) {
        if (sunrise <= 0 || sunset <= 0 || sunrise == sunset) return -1;
        long day = TimeUnit.DAYS.toMillis(1), current = Math.floorMod(now, day), rise = Math.floorMod(sunrise, day), set = Math.floorMod(sunset, day);
        return (set > rise ? current >= rise && current <= set : current >= rise || current <= set) ? 1 : 0;
    }
}
