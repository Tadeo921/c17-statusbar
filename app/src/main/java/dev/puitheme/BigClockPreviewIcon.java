// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/** A small sketch of the weather, battery, signal and network icons, drawn only in the settings app's preview.
 * SystemUI never uses it: the real row draws the native images. The sketch follows the text colour. */
final class BigClockPreviewIcon extends ReplacementSpan {
    private static final int WEATHER = 0, BATTERY = 1, SIGNAL = 2, WIFI = 3, OFFLINE = 4, WIRED = 5;
    private final int shape, level;
    private final WeatherTextIcon.Kind kind;
    private final boolean colours, charging;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF box = new RectF();
    private int ink, inkAlpha;

    BigClockPreviewIcon(WeatherTextIcon.Kind kind, boolean nativeColours) {
        shape = WEATHER; this.kind = kind == null ? WeatherTextIcon.Kind.UNKNOWN : kind; colours = nativeColours;
        level = 0; charging = false;
    }

    BigClockPreviewIcon(int token, BigClockSystemText.Snapshot state) {
        kind = null; colours = false;
        if (token == BigClockTextContent.BATTERY) {
            shape = BATTERY; level = state.batteryPercent;
            charging = state.charging == BigClockSystemText.Charging.CHARGING || state.charging == BigClockSystemText.Charging.FULL;
        } else if (token == BigClockTextContent.NETWORK && state.transport != BigClockSystemText.Transport.CELLULAR) {
            charging = false;
            switch (state.transport) {
                case NONE: shape = OFFLINE; level = 0; break;
                case ETHERNET: shape = WIRED; level = 0; break;
                default: shape = WIFI; level = state.wifiLevel; break;
            }
        } else { shape = SIGNAL; level = state.signalLevel; charging = false; }
    }

    private float height(Paint text) {
        float size = text.getTextSize();
        float base = Float.isFinite(size) && size > 0 ? size : 14f;
        return Math.max(1f, base * (shape == WEATHER ? WeatherTextIcon.Span.ICON_SCALE : .94f));
    }

    private float width(Paint text) {
        float h = height(text);
        switch (shape) {
            case BATTERY: return h * 1.7f;
            case WEATHER: return h * 1.15f;
            case SIGNAL: return h * .95f;
            default: return h;
        }
    }

    @Override public int getSize(Paint text, CharSequence value, int start, int end, Paint.FontMetricsInt output) {
        if (output != null) {
            text.getFontMetricsInt(output);
            float centre = (output.ascent + output.descent) * .5f, half = height(text) * .5f;
            int top = (int) Math.floor(centre - half), bottom = (int) Math.ceil(centre + half);
            if (top < output.ascent) output.ascent = top;
            if (bottom > output.descent) output.descent = bottom;
            if (top < output.top) output.top = top;
            if (bottom > output.bottom) output.bottom = bottom;
        }
        return Math.max(1, (int) Math.ceil(width(text)));
    }

    @Override public void draw(Canvas canvas, CharSequence value, int start, int end, float x, int top, int baseline, int bottom, Paint text) {
        Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
        text.getFontMetricsInt(metrics);
        float h = height(text), w = width(text), y = baseline + (metrics.ascent + metrics.descent - h) * .5f;
        ink = text.getColor(); inkAlpha = text.getAlpha();
        box.set(x, y, x + w, y + h);
        switch (shape) {
            case BATTERY: battery(canvas); break;
            case SIGNAL: bars(canvas); break;
            case WIFI: wifi(canvas); break;
            case OFFLINE: offline(canvas); break;
            case WIRED: wired(canvas); break;
            default: weather(canvas); break;
        }
    }

    /** The text colour, or the artwork's own colour when "原色" was asked for. */
    private void colour(int artwork, boolean solid) {
        paint.setColor(colours ? artwork : ink);
        paint.setAlpha(inkAlpha);
        paint.setStyle(solid ? Paint.Style.FILL : Paint.Style.STROKE);
    }

    private void dim(boolean lit) { paint.setAlpha(lit ? inkAlpha : Math.max(28, inkAlpha / 4)); }

    private void battery(Canvas canvas) {
        float h = box.height(), body = h * .56f, cy = box.centerY(), left = box.left, right = box.right - h * .1f;
        colour(ink, false); paint.setStrokeWidth(Math.max(1f, h * .07f));
        RectF outline = new RectF(left, cy - body / 2, right, cy + body / 2);
        canvas.drawRoundRect(outline, body * .28f, body * .28f, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(right + h * .02f, cy - body * .17f, box.right, cy + body * .17f), h * .03f, h * .03f, paint);
        int percent = level < 0 ? 85 : Math.min(100, level);
        float inset = h * .11f, span = outline.width() - inset * 2;
        canvas.drawRoundRect(new RectF(left + inset, cy - body / 2 + inset, left + inset + Math.max(h * .06f, span * percent / 100f), cy + body / 2 - inset),
                body * .12f, body * .12f, paint);
        if (charging) {
            paint.setColor(0xff000000 | (ink ^ 0x00ffffff)); paint.setAlpha(inkAlpha);
            float bx = outline.centerX(), s = body * .42f;
            path.reset(); path.moveTo(bx + s * .15f, cy - s); path.lineTo(bx - s * .45f, cy + s * .1f); path.lineTo(bx - s * .02f, cy + s * .1f);
            path.lineTo(bx - s * .15f, cy + s); path.lineTo(bx + s * .45f, cy - s * .1f); path.lineTo(bx + s * .02f, cy - s * .1f); path.close();
            canvas.drawPath(path, paint);
        }
    }

    private void bars(Canvas canvas) {
        int lit = level < 0 ? 3 : Math.min(4, level);
        float h = box.height() * .86f, gap = box.width() * .08f, bar = (box.width() - gap * 3) / 4f;
        for (int i = 0; i < 4; i++) {
            colour(ink, true); dim(i < lit);
            float barHeight = h * (.34f + .22f * i), left = box.left + i * (bar + gap);
            canvas.drawRoundRect(new RectF(left, box.centerY() + h / 2 - barHeight, left + bar, box.centerY() + h / 2), bar * .3f, bar * .3f, paint);
        }
    }

    private void wifi(Canvas canvas) {
        int lit = level < 0 ? 3 : (level * 3 + 3) / 4;
        float cx = box.centerX(), h = box.height(), bottom = box.bottom - h * .12f;
        for (int i = 1; i <= 3; i++) {
            float r = h * (.22f + .22f * i);
            colour(ink, false); paint.setStrokeWidth(Math.max(1f, h * .09f)); dim(i <= lit);
            canvas.drawArc(new RectF(cx - r, bottom - r, cx + r, bottom + r), -135f, 90f, false, paint);
        }
        colour(ink, true); dim(level != 0);
        canvas.drawCircle(cx, bottom, h * .07f, paint);
    }

    private void offline(Canvas canvas) {
        float r = box.height() * .34f, cx = box.centerX(), cy = box.centerY();
        colour(ink, false); paint.setStrokeWidth(Math.max(1f, box.height() * .08f));
        canvas.drawCircle(cx, cy, r, paint);
        path.reset(); path.moveTo(cx - r * .7f, cy + r * .7f); path.lineTo(cx + r * .7f, cy - r * .7f);
        canvas.drawPath(path, paint);
    }

    private void wired(Canvas canvas) {
        float h = box.height(), cx = box.centerX(), cy = box.centerY(), r = h * .3f;
        colour(ink, false); paint.setStrokeWidth(Math.max(1f, h * .08f));
        canvas.drawRoundRect(new RectF(cx - r, cy - r * .8f, cx + r, cy + r * .8f), h * .06f, h * .06f, paint);
        path.reset(); path.moveTo(cx - r * .5f, cy + r * .8f); path.lineTo(cx - r * .5f, cy + r * 1.3f);
        path.moveTo(cx + r * .5f, cy + r * .8f); path.lineTo(cx + r * .5f, cy + r * 1.3f);
        canvas.drawPath(path, paint);
    }

    // ---- weather: a cloud, a sun or a moon, plus what falls from the cloud ----

    private void weather(Canvas canvas) {
        float h = box.height();
        switch (kind) {
            case SUNNY: case SUNRISE: case SUNSET: sun(canvas, box.centerX(), box.centerY(), h * .22f); break;
            case CLEAR_NIGHT: moon(canvas, box.centerX(), box.centerY(), h * .3f); break;
            case FOG: case HAZE: case SINK_FLOAT: case DUST_DEVIL: case WIND: case STORM: case TORNADO: lines(canvas); break;
            default:
                if (kind == WeatherTextIcon.Kind.PARTLY_CLOUDY) sun(canvas, box.left + box.width() * .34f, box.top + h * .34f, h * .17f);
                else if (kind == WeatherTextIcon.Kind.CLOUDY_NIGHT) moon(canvas, box.left + box.width() * .34f, box.top + h * .34f, h * .2f);
                cloud(canvas, kind == WeatherTextIcon.Kind.CLOUDY || kind == WeatherTextIcon.Kind.UNKNOWN ? box.top + h * .2f : box.top + h * .3f);
                fall(canvas);
        }
    }

    private void sun(Canvas canvas, float cx, float cy, float r) {
        colour(0xffffc21a, true);
        canvas.drawCircle(cx, cy, r, paint);
        colour(0xffffc21a, false); paint.setStrokeWidth(Math.max(1f, r * .28f));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i; float c = (float) Math.cos(a), s = (float) Math.sin(a);
            path.reset(); path.moveTo(cx + c * r * 1.45f, cy + s * r * 1.45f); path.lineTo(cx + c * r * 1.9f, cy + s * r * 1.9f);
            canvas.drawPath(path, paint);
        }
    }

    private void moon(Canvas canvas, float cx, float cy, float r) {
        colour(0xffe8e8ff, true);
        path.reset();
        path.moveTo(cx + r * .2f, cy - r); path.lineTo(cx - r * .9f, cy - r * .1f); path.lineTo(cx - r * .3f, cy + r * .9f);
        path.lineTo(cx + r * .8f, cy + r * .6f); path.lineTo(cx + r * .1f, cy + r * .1f); path.close();
        canvas.drawPath(path, paint);
    }

    private void cloud(Canvas canvas, float top) {
        colour(0xffd7dde6, true);
        float h = box.height() * .46f, left = box.left + box.width() * .06f, right = box.right - box.width() * .04f;
        canvas.drawRoundRect(new RectF(left, top + h * .38f, right, top + h), h * .31f, h * .31f, paint);
        canvas.drawCircle(left + (right - left) * .36f, top + h * .5f, h * .38f, paint);
        canvas.drawCircle(left + (right - left) * .62f, top + h * .36f, h * .46f, paint);
    }

    private void fall(Canvas canvas) {
        boolean rain = false, snow = false, bolt = false, hail = false;
        switch (kind) {
            case LIGHT_RAIN: case MODERATE_RAIN: case HEAVY_RAIN: case ICE_RAIN: rain = true; break;
            case LIGHT_SNOW: case MODERATE_SNOW: case HEAVY_SNOW: snow = true; break;
            case SLEET: rain = snow = true; break;
            case HAILSTONE: hail = true; break;
            case THUNDERSHOWERS: case THUNDERSTORM: rain = true; bolt = true; break;
            case THUNDERSHOWERS_HAILSTONE: hail = bolt = true; break;
            default: return;
        }
        float h = box.height(), y = box.top + h * .74f;
        int count = kind == WeatherTextIcon.Kind.HEAVY_RAIN || kind == WeatherTextIcon.Kind.HEAVY_SNOW ? 4 : 3;
        if (rain && !bolt) {
            colour(0xff5aa9ff, false); paint.setStrokeWidth(Math.max(1f, h * .06f));
            for (int i = 0; i < count; i++) {
                float x = box.left + box.width() * (.22f + .56f * i / (count - 1));
                path.reset(); path.moveTo(x + h * .05f, y); path.lineTo(x - h * .05f, y + h * .2f);
                canvas.drawPath(path, paint);
            }
        }
        if (snow) {
            colour(0xffffffff, true);
            for (int i = 0; i < count; i++) canvas.drawCircle(box.left + box.width() * (.24f + .52f * i / (count - 1)), y + h * (i % 2 == 0 ? .08f : .18f), h * .05f, paint);
        }
        if (hail) {
            colour(0xffbfe7ff, true);
            for (int i = 0; i < 3; i++) canvas.drawCircle(box.left + box.width() * (.28f + .22f * i), y + h * (i % 2 == 0 ? .08f : .18f), h * .06f, paint);
        }
        if (bolt) {
            colour(0xffffc21a, true);
            float cx = box.centerX(), s = h * .2f;
            path.reset(); path.moveTo(cx + s * .3f, y - s * .2f); path.lineTo(cx - s * .5f, y + s * .7f); path.lineTo(cx, y + s * .7f);
            path.lineTo(cx - s * .3f, y + s * 1.5f); path.lineTo(cx + s * .6f, y + s * .4f); path.lineTo(cx + s * .1f, y + s * .4f); path.close();
            canvas.drawPath(path, paint);
        }
    }

    private void lines(Canvas canvas) {
        float h = box.height();
        colour(0xffb8c2cf, false); paint.setStrokeWidth(Math.max(1f, h * .07f));
        for (int i = 0; i < 3; i++) {
            float y = box.top + h * (.32f + .18f * i), inset = i == 1 ? 0f : box.width() * .12f;
            path.reset(); path.moveTo(box.left + inset, y); path.lineTo(box.right - inset, y);
            canvas.drawPath(path, paint);
        }
    }
}
