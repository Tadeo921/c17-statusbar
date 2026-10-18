// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Module-owned weather symbols. No widget drawable, resource loader or IO in draw. */
final class ModuleWeatherIcon {
    private final WeatherTextIcon.Kind kind;
    private final boolean originalColours;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();
    private final RectF box = new RectF();
    private final Path path = new Path();
    private Paint text;

    ModuleWeatherIcon(WeatherTextIcon.Kind kind, boolean originalColours) {
        this.kind = kind; this.originalColours = originalColours;
    }
    void draw(Canvas canvas,float x,int baseline,Paint text) {
        this.text = text;
        text.getFontMetricsInt(metrics);
        float height = Math.max(1f,text.getTextSize()*WeatherTextIcon.Span.ICON_SCALE);
        float y = baseline+(metrics.ascent+metrics.descent-height)*.5f;
        int saved = canvas.save();
        try {
            canvas.translate(x,y); canvas.scale(height/24f,height/24f);
            switch (kind) {
                case SUNNY: sun(canvas,13.8f,12f,4f); break;
                case CLEAR_NIGHT: moon(canvas,13.8f,12f,6f); break;
                case SUNRISE: case SUNSET: horizon(canvas); break;
                case WIND: wind(canvas); break;
                case TORNADO: tornado(canvas); break;
                case FOG: case HAZE: case SINK_FLOAT: case DUST_DEVIL: atmosphere(canvas); break;
                default:
                    if (kind==WeatherTextIcon.Kind.PARTLY_CLOUDY) sun(canvas,9f,8f,3.2f);
                    if (kind==WeatherTextIcon.Kind.CLOUDY_NIGHT) moon(canvas,9f,8f,4.5f);
                    cloud(canvas); precipitation(canvas); break;
            }
        } finally { canvas.restoreToCount(saved); this.text = null; }
    }
    private void colour(int value,boolean fill) {
        paint.set(text);
        if (originalColours) { paint.setShader(null); paint.setColor(value); paint.setAlpha(text.getAlpha()); }
        paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
    }
    private void line(Canvas c,float x,float y,float x2,float y2) {
        path.reset();path.moveTo(x,y);path.lineTo(x2,y2);c.drawPath(path,paint);
    }
    private void sun(Canvas c,float x,float y,float radius) {
        colour(0xffffc43d,true);c.drawCircle(x,y,radius,paint);
        colour(0xffffc43d,false);
        for(int i=0;i<8;i++) {
            double angle=Math.PI*i/4d;float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            line(c,x+dx*(radius+1.8f),y+dy*(radius+1.8f),x+dx*(radius+3.3f),y+dy*(radius+3.3f));
        }
    }
    private void moon(Canvas c,float x,float y,float radius) {
        colour(0xffd9e2ff,true);
        path.reset();path.moveTo(x+radius*.38f,y-radius);
        path.cubicTo(x-radius*1.3f,y-radius*.8f,x-radius*1.3f,y+radius*.9f,x+radius*.1f,y+radius);
        path.cubicTo(x+radius*.65f,y+radius,x+radius*.95f,y+radius*.65f,x+radius,y+radius*.2f);
        path.cubicTo(x-radius*.3f,y+radius*.5f,x-radius*.5f,y-radius*.5f,x+radius*.38f,y-radius);path.close();c.drawPath(path,paint);
    }
    private void cloud(Canvas c) {
        colour(0xffd7e0ed,true);
        box.set(3.2f,10f,23f,17.2f);c.drawRoundRect(box,3.6f,3.6f,paint);
        c.drawCircle(10f,10.7f,4.7f,paint);c.drawCircle(17.1f,10.1f,5.5f,paint);
    }
    private void precipitation(Canvas c) {
        int count;
        switch(kind) {
            case LIGHT_RAIN: count=2;break;
            case MODERATE_RAIN: count=3;break;
            case HEAVY_RAIN: case STORM: count=4;break;
            case LIGHT_SNOW: snow(c,2);return;
            case MODERATE_SNOW: snow(c,3);return;
            case HEAVY_SNOW: snow(c,4);return;
            case HAILSTONE: hail(c);return;
            case SLEET: snow(c,2);count=2;break;
            case ICE_RAIN: hail(c);count=2;break;
            case THUNDERSHOWERS: case THUNDERSTORM: bolt(c);count=2;break;
            case THUNDERSHOWERS_HAILSTONE: bolt(c);hail(c);return;
            default:return;
        }
        colour(0xff5aaaff,false);
        for(int i=0;i<count;i++){float x=6f+16f*(i+.5f)/count;line(c,x,19f,x-1.8f,22f);}
    }
    private void snow(Canvas c,int count) {
        colour(0xfff0f6ff,false);paint.setStrokeWidth(1f);
        for(int i=0;i<count;i++) {
            float x=5f+18f*(i+.5f)/count,y=20.6f;
            line(c,x-1.2f,y,x+1.2f,y);line(c,x,y-1.2f,x,y+1.2f);
        }
    }
    private void hail(Canvas c) {
        colour(0xff9dd8ff,true);
        for(int i=0;i<3;i++)c.drawCircle(8f+i*6f,20.3f+(i%2),1.2f,paint);
    }
    private void bolt(Canvas c) {
        colour(0xffffca38,true);path.reset();path.moveTo(15,15);path.lineTo(11,20);path.lineTo(14,20);
        path.lineTo(12,24);path.lineTo(19,18);path.lineTo(15,18);path.close();c.drawPath(path,paint);
    }
    private void atmosphere(Canvas c) {
        if(kind==WeatherTextIcon.Kind.FOG||kind==WeatherTextIcon.Kind.HAZE)cloud(c);
        colour(kind==WeatherTextIcon.Kind.FOG?0xffb9c6d5:0xffd5bd8b,false);
        for(int i=0;i<3;i++)line(c,4f+i%2*2f,17f+i*2.5f,24f-i%2*2f,17f+i*2.5f);
        if(kind==WeatherTextIcon.Kind.DUST_DEVIL||kind==WeatherTextIcon.Kind.SINK_FLOAT){
            colour(0xffd5bd8b,true);for(int i=0;i<4;i++)c.drawCircle(6f+i*5f,9f+i%2*3f,.9f,paint);
        }
    }
    private void wind(Canvas c) {
        colour(0xffb7c9df,false);
        for(int i=0;i<3;i++) {
            float y=7+i*5;path.reset();path.moveTo(3+i*2,y);path.lineTo(20-i*2,y);
            path.cubicTo(25,y,24,y-5,21,y-4);c.drawPath(path,paint);
        }
    }
    private void tornado(Canvas c) {
        colour(0xffaebfd4,false);
        for(int i=0;i<5;i++)line(c,4+i*2f,5+i*3.5f,24-i*2f,5+i*3.5f);
    }
    private void horizon(Canvas c) {
        sun(c,13.8f,12f,4f);colour(0xffffa83d,false);line(c,3,16,25,16);
        float from=kind==WeatherTextIcon.Kind.SUNRISE?22:18,to=kind==WeatherTextIcon.Kind.SUNRISE?18:22;
        line(c,14,from,14,to);line(c,14,to,12,to+(from>to?2:-2));line(c,14,to,16,to+(from>to?2:-2));
    }
}
