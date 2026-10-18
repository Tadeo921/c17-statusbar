// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.provider.Settings;
import java.lang.reflect.Method;
import org.json.JSONObject;

/** The system's own icon outline (the corner radius set in the system's icon settings), applied to an
 * application icon after the launcher produced it. The launcher draws icons the icon pack lacks without that
 * outline, so the same shape is applied here to every icon read from the launcher. The outline comes from the
 * OEM's path builder (a 150 x 150 smooth rounded square); if it is not reachable, a plain rounded square with the
 * configured corner ratio is used. If neither is known the icon is left exactly as the launcher returned it. */
final class SystemIconShape {
    private static final float OEM_UNITS = 150f;
    private static final String PARSER = "android.app.OplusUxIconConfigParser";
    private SystemIconShape() { }

    /** A copy of the icon clipped to the system outline, or the icon itself when no outline is known. */
    static Bitmap apply(Context context, Bitmap icon) {
        if (icon == null || icon.getWidth() <= 0 || icon.getHeight() <= 0) return icon;
        Path outline = outline(context, icon.getWidth(), icon.getHeight());
        return outline == null ? icon : clip(icon, outline);
    }

    static Bitmap clip(Bitmap icon, Path outline) {
        int width = icon.getWidth(), height = icon.getHeight();
        Bitmap mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        new Canvas(mask).drawPath(outline, fill);
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(icon, 0f, 0f, null);
        Paint keep = new Paint(Paint.ANTI_ALIAS_FLAG);
        keep.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(mask, 0f, 0f, keep);
        mask.recycle();
        return result;
    }

    /** The outline scaled to this bitmap, or null. */
    static Path outline(Context context, int width, int height) {
        Path source = oemPath();
        float ratio = Float.NaN;
        if (source == null) ratio = cornerRatio(context);
        Path scaled = new Path();
        if (source != null) {
            Matrix matrix = new Matrix();
            matrix.setScale(width / OEM_UNITS, height / OEM_UNITS);
            source.transform(matrix, scaled);
            return scaled;
        }
        if (Float.isNaN(ratio) || ratio <= 0f) return null;
        float radius = Math.min(width, height) * Math.min(.5f, ratio);
        scaled.addRoundRect(new RectF(0f, 0f, width, height), radius, radius, Path.Direction.CW);
        return scaled;
    }

    private static Path oemPath() {
        try {
            Class<?> parser = Class.forName(PARSER);
            Object corner = parser.getMethod("getSmoothCornerG2").invoke(null);
            if (corner instanceof Number) {
                Object path = parser.getMethod("getOplusPathG2", float.class).invoke(null, ((Number) corner).floatValue());
                if (path instanceof Path) return (Path) path;
            }
            Object plain = parser.getMethod("getSmoothCorner").invoke(null);
            if (plain instanceof Number) {
                Method builder = parser.getMethod("getOplusPath", float.class);
                Object path = builder.invoke(null, ((Number) plain).floatValue());
                if (path instanceof Path) return (Path) path;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { /* fall back to the setting */ }
        return null;
    }

    /** Corner radius as a fraction of the icon side, from the system's icon configuration. */
    static float cornerRatio(Context context) {
        if (context == null) return Float.NaN;
        try {
            String json = Settings.Secure.getString(context.getContentResolver(), "com.oplus.icons.icon_config");
            if (json == null || json.isEmpty()) return Float.NaN;
            JSONObject config = new JSONObject(json);
            double corner = config.optDouble("smooth_corner", Double.NaN);
            return Double.isNaN(corner) || corner <= 0 ? Float.NaN : (float) (corner / OEM_UNITS);
        } catch (Exception | LinkageError unavailable) { return Float.NaN; }
    }
}
