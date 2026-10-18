// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.TimeZone;

/** Two optional text components. Their parent owns entry motion and opacity. */
final class BigClockTextComponents extends FrameLayout {
    interface OnComponentClickListener {
        boolean isComponentClickAllowed(View target,int side,String packageName);
        void onComponentClick(View target,int side,String packageName);
    }
    private final Slot[] slots={new Slot(BigClockWidgetsSettings.LEFT),new Slot(BigClockWidgetsSettings.RIGHT)};
    private final int[] rowLocation=new int[2],rootLocation=new int[2];
    private long minute=Long.MIN_VALUE,weatherRevision=Long.MIN_VALUE,systemRevision=Long.MIN_VALUE,iconRevision=Long.MIN_VALUE;
    private String zoneId;
    private int zoneOffset;
    private int mainColor=0xffffffff,maskColor=0xffffffff;
    private float maskOpacity;
    private boolean dark,maskEnabled,closed,globalFont;
    private ClockBackdropSource.Frame backdrop;
    private ClockBackdropMaterial.Appearance backdropAppearance=ClockBackdropMaterial.Appearance.NONE;
    private OnComponentClickListener clickListener;
    private boolean clicksPaused=true;

    BigClockTextComponents(Context context) {
        super(context);
        setClipChildren(false);setClipToPadding(false);
        setClickable(false);setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    void configure(Bundle values) {
        if(closed)return;
        globalFont=FeatureOptions.from(values).enabled("font");
        boolean changed=false;
        for(Slot slot:slots)changed|=slot.configure(values);
        refreshClickTargets();
        if(changed){minute=Long.MIN_VALUE;requestLayout();invalidate();}
    }

    /** Whether either visible component shows weather; safe to ask from a native event thread. */
    boolean usesWeather() {
        for(Slot slot:slots)if(slot.view!=null&&(slot.dependencies&BigClockTextContent.WEATHER)!=0)return true;
        return false;
    }

    void setOnComponentClickListener(OnComponentClickListener listener) {
        if(closed)return;
        clickListener=listener;refreshClickTargets();
    }

    void updateClickTargets() {
        clicksPaused=false;refreshClickTargets();
    }

    void disableClickTargets() {
        clicksPaused=true;refreshClickTargets();
    }

    private void refreshClickTargets() {
        boolean accessible=false;
        for(Slot slot:slots)accessible|=slot.updateClickTarget();
        int importance=accessible?View.IMPORTANT_FOR_ACCESSIBILITY_NO:View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS;
        if(getImportantForAccessibility()!=importance)setImportantForAccessibility(importance);
    }

    /** Weather is a snapshot supplied by an existing system event, never a query. */
    void refresh(long now,TimeZone zone) {
        if(closed)return;
        boolean fontChanged=false;
        for(Slot slot:slots)fontChanged|=slot.applyFont();
        if(fontChanged){requestLayout();invalidate();}
        TimeZone current=zone==null?TimeZone.getDefault():zone;
        SystemWeatherText.Snapshot weather=SystemWeatherText.snapshot();
        BigClockSystemText.Snapshot system=BigClockSystemText.snapshot();
        long nextIconRevision=BigClockSystemIcon.revision();
        long nextMinute=Math.floorDiv(now,60000L),revision=weather.revision;
        String id=current.getID();int offset=current.getOffset(now);
        if(minute==nextMinute&&weatherRevision==revision&&systemRevision==system.revision&&iconRevision==nextIconRevision&&offset==zoneOffset&&id.equals(zoneId))return;
        minute=nextMinute;weatherRevision=revision;systemRevision=system.revision;iconRevision=nextIconRevision;zoneId=id;zoneOffset=offset;
        for(Slot slot:slots)if(slot.view!=null)slot.refresh(now,current,nextMinute,weather,system,id,offset,nextIconRevision);
    }

    void appearance(int mainResolvedColor,boolean darkBackground,boolean enabled,int color,float opacity) {
        if(closed)return;
        mainColor=mainResolvedColor;dark=darkBackground;maskEnabled=enabled;maskColor=color;
        if(!enabled)backdrop=null;
        maskOpacity=Float.isFinite(opacity)?Math.max(0f,Math.min(1f,opacity)):0f;
        for(Slot slot:slots)slot.applyColor();
    }

    void backdrop(ClockBackdropSource.Frame source) {
        backdrop(source,ClockBackdropMaterial.Appearance.NONE);
    }

    void backdrop(ClockBackdropSource.Frame source,ClockBackdropMaterial.Appearance look) {
        if(closed)return;
        backdrop=source;
        backdropAppearance=look==null?ClockBackdropMaterial.Appearance.NONE:look;
        for(Slot slot:slots)slot.applyBackdrop();
    }

    float desiredHeight() {
        if(closed)return 0f;
        float bottom=0f;boolean visible=false;
        for(Slot slot:slots)if(slot.view!=null){visible=true;slot.measureText();bottom=Math.max(bottom,slot.y+slot.view.getMeasuredHeight());}
        return visible?Math.max(32f*density(),bottom):0f;
    }

    /** Extend only the real parent geometry above its nominal origin. The
     * caller moves the row up by this amount, while child Y is compensated,
     * so both visible glyphs and notification bottom reservation stay put. */
    int topInset() {
        if(closed)return 0;
        float top=0f;
        for(Slot slot:slots)if(slot.view!=null)top=Math.min(top,slot.y);
        return NumericPolicy.layoutPixels(Math.ceil(-top));
    }

    void close() {
        if(closed)return;
        closed=true;clickListener=null;disableClickTargets();
        for(Slot slot:slots){if(slot.view!=null){closeIcons(slot.view.getText(),null);slot.material.clear(slot.view);removeView(slot.view);}slot.view=null;}
        backdrop=null;backdropAppearance=ClockBackdropMaterial.Appearance.NONE;
        minute=weatherRevision=systemRevision=iconRevision=Long.MIN_VALUE;zoneId=null;
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec);
        int wanted=NumericPolicy.layoutPixels(Math.ceil(desiredHeight()+topInset()));
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(wanted,heightSpec));
    }

    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
        int width=Math.max(0,right-left);
        int inset=topInset();
        for(Slot slot:slots)if(slot.view!=null){
            slot.measureText();int w=slot.view.getMeasuredWidth(),h=slot.view.getMeasuredHeight();
            int x=Math.round((slot.side==BigClockWidgetsSettings.RIGHT?width-w:0)+slot.x),y=Math.round(slot.y)+inset;
            slot.view.layout(x,y,x+w,y+h);
            slot.applyBackdrop();
        }
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        int saved=canvas.save();
        try {
            // Negative Y may move above the row. It must stay inside the actual
            // window, while the allocated bottom preserves notification space.
            View root=getRootView();getLocationInWindow(rowLocation);root.getLocationInWindow(rootLocation);
            int visibleTop=rootLocation[1]-rowLocation[1];
            canvas.clipRect(0,visibleTop,getWidth(),getHeight());
            super.dispatchDraw(canvas);
        } finally {canvas.restoreToCount(saved);}
    }

    private float density(){return getResources().getDisplayMetrics().density;}
    /** Spans own their independent native resources. Retained spans stay live
     * when only a neighbouring data token changes its text. */
    private static void closeIcons(CharSequence previous,CharSequence retained) {
        if(!(previous instanceof Spanned))return;
        Spanned old=(Spanned)previous,next=retained instanceof Spanned?(Spanned)retained:null;
        for(BigClockSystemIcon.Span icon:old.getSpans(0,old.length(),BigClockSystemIcon.Span.class))
            if(next==null||next.getSpanStart(icon)<0)icon.close();
    }
    private static float number(Bundle values,String key,float fallback,float min,float max) {
        Object raw=values==null?null:values.get(key);
        float result=raw instanceof Number?((Number)raw).floatValue():fallback;
        return Float.isFinite(result)?Math.max(min,Math.min(max,result)):fallback;
    }
    private static int color(Bundle values,String key,String alphaKey,int fallback) {
        Object raw=values==null?null:values.get(key);
        int value=raw instanceof Integer?(Integer)raw:fallback;
        return values!=null&&values.getBoolean(alphaKey,false)?value:value|0xff000000;
    }

    private final class Slot {
        final int side;
        final String lightKey,darkKey,lightAlphaKey,darkAlphaKey;
        TextView view;
        Typeface nativeFamily;
        String nativeAxes;
        int weight=400,appliedWeight=Integer.MIN_VALUE;
        long fontRevision=Long.MIN_VALUE;
        boolean appliedGlobalFont;
        final ClockBackdropMaterial material=new ClockBackdropMaterial();
        String template="";
        String launchPackage="";
        boolean clickEnabled,clickBound;
        final View.OnClickListener onClick;
        String renderedTemplate;
        int dependencies,renderedZoneOffset;
        String renderedZone;
        long renderedMinute=Long.MIN_VALUE,renderedWeatherRevision=Long.MIN_VALUE;
        long renderedCellularRevision=Long.MIN_VALUE,renderedNetworkRevision=Long.MIN_VALUE,renderedBatteryRevision=Long.MIN_VALUE;
        long renderedIconRevision=Long.MIN_VALUE;
        float size=Float.NaN,x,y,ownMaskOpacity=Float.NaN;
        boolean follows=true,measurementDirty=true;
        int light=0xff000000,darkColor=0xffffffff;
        Slot(int side){
            this.side=side;lightKey=BigClockWidgetsSettings.colorKey(side,false);darkKey=BigClockWidgetsSettings.colorKey(side,true);
            lightAlphaKey=StatusBarSettings.alphaKey(lightKey);darkAlphaKey=StatusBarSettings.alphaKey(darkKey);
            onClick=target->{
                if(target!=view||!target.isClickable()||!canClick())return;
                clickListener.onComponentClick(target,this.side,launchPackage);
            };
        }
        boolean configure(Bundle values) {
            boolean enabled=values!=null&&values.getBoolean(BigClockWidgetsSettings.enabledKey(side),false);
            if(!enabled){if(view==null)return false;clearClickTarget();closeIcons(view.getText(),null);material.clear(view);removeView(view);view=null;return true;}
            boolean changed=false;
            if(view==null){
                view=new TextView(getContext());view.setSingleLine(true);
                // The material's content input is bounded by this TextView.
                // Tight ascent/descent can cut fallback glyphs before the row
                // or its non-clipping parents get a chance to draw them.
                view.setIncludeFontPadding(true);view.setFallbackLineSpacing(true);
                nativeFamily=view.getTypeface();nativeAxes=view.getFontVariationSettings();appliedWeight=Integer.MIN_VALUE;fontRevision=Long.MIN_VALUE;
                view.setGravity(Gravity.TOP|Gravity.LEFT);view.setPadding(0,0,0,0);
                view.setClickable(false);view.setFocusable(false);
                view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                addView(view,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
                size=Float.NaN;renderedTemplate=null;renderedWeatherRevision=Long.MIN_VALUE;measurementDirty=true;changed=true;
            }
            String fallback=side==BigClockWidgetsSettings.LEFT?BigClockWidgetsSettings.DEFAULT_LEFT_TEXT:BigClockWidgetsSettings.DEFAULT_RIGHT_TEXT;
            String text=values.getString(BigClockWidgetsSettings.textKey(side),fallback);
            if(BigClockWidgetsSettings.textValidationError(text)!=null)text=fallback;
            if(!text.equals(template)){template=text;dependencies=BigClockTextContent.dependencies(text);changed=true;}
            if((dependencies&(BigClockTextContent.CELLULAR|BigClockTextContent.NETWORK|BigClockTextContent.BATTERY))!=0)BigClockSystemIcon.observe(view);
            float nextSize=number(values,BigClockWidgetsSettings.sizeKey(side),BigClockWidgetsSettings.DEFAULT_SIZE,BigClockWidgetsSettings.MIN_SIZE,BigClockWidgetsSettings.MAX_SIZE)*density();
            if(size!=nextSize){size=nextSize;view.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);measurementDirty=true;changed=true;}
            weight=Math.round(number(values,BigClockWidgetsSettings.weightKey(side),BigClockWidgetsSettings.DEFAULT_WEIGHT,BigClockWidgetsSettings.MIN_WEIGHT,BigClockWidgetsSettings.MAX_WEIGHT));
            changed|=applyFont();
            float nextX=number(values,BigClockWidgetsSettings.offsetXKey(side),0f,-BigClockWidgetsSettings.MAX_OFFSET,BigClockWidgetsSettings.MAX_OFFSET)*density();
            float nextY=number(values,BigClockWidgetsSettings.offsetYKey(side),0f,-BigClockWidgetsSettings.MAX_OFFSET,BigClockWidgetsSettings.MAX_OFFSET)*density();
            if(x!=nextX||y!=nextY){x=nextX;y=nextY;changed=true;}
            follows=values.getBoolean(BigClockWidgetsSettings.followTimeColorKey(side),true);
            // A missing/invalid new value follows the row's current main
            // strength; finite zero is an explicit, fully untinted backdrop.
            float nextMask=number(values,BigClockWidgetsSettings.maskOpacityKey(side),Float.NaN,0f,100f);
            ownMaskOpacity=Float.isFinite(nextMask)?nextMask/100f:Float.NaN;
            light=color(values,lightKey,lightAlphaKey,0xff000000);
            darkColor=color(values,darkKey,darkAlphaKey,0xffffffff);
            clickEnabled=values.getBoolean(BigClockWidgetsSettings.clickEnabledKey(side),false);
            String nextPackage=values.getString(BigClockWidgetsSettings.launchAppKey(side),"");
            launchPackage=BigClockWidgetsSettings.launchAppValidationError(nextPackage)==null?nextPackage:"";
            applyColor();return changed;
        }
        boolean applyFont() {
            if(view==null)return false;
            long revision=globalFont?FontRepository.styleRevision():0L;
            if(appliedWeight==weight&&appliedGlobalFont==globalFont&&fontRevision==revision&&(!globalFont||!FontRepository.retryDue(FontScopes.NOTIFICATION_CLOCK)))return false;
            if(globalFont)fontRevision=FontRepository.applyRevision(view,nativeFamily,weight,nativeAxes,FontScopes.NOTIFICATION_CLOCK);
            else {FontWeight.apply(view,nativeFamily,weight,nativeAxes);fontRevision=0L;}
            appliedGlobalFont=globalFont;appliedWeight=weight;measurementDirty=true;
            return true;
        }
        boolean canClick() {
            return !closed&&!clicksPaused&&view!=null&&clickEnabled&&!launchPackage.isEmpty()
                    &&view.getRight()>0&&view.getLeft()<getWidth()&&view.getBottom()>0&&view.getTop()<getHeight()
                    &&clickListener!=null&&clickListener.isComponentClickAllowed(view,side,launchPackage);
        }
        boolean updateClickTarget() {
            if(view==null)return false;
            boolean enabled=canClick();
            if(clickBound!=enabled){view.setOnClickListener(enabled?onClick:null);clickBound=enabled;}
            if(view.isClickable()!=enabled)view.setClickable(enabled);
            if(view.isFocusable()!=enabled)view.setFocusable(enabled);
            int importance=enabled?View.IMPORTANT_FOR_ACCESSIBILITY_YES:View.IMPORTANT_FOR_ACCESSIBILITY_NO;
            if(view.getImportantForAccessibility()!=importance)view.setImportantForAccessibility(importance);
            return enabled;
        }
        void clearClickTarget() {
            if(view==null)return;
            view.setOnClickListener(null);view.setClickable(false);view.setFocusable(false);clickBound=false;
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        void refresh(long now,TimeZone zone,long nextMinute,SystemWeatherText.Snapshot weather,BigClockSystemText.Snapshot system,String id,int offset,long styleRevision) {
            boolean templateChanged=!template.equals(renderedTemplate);
            boolean weatherChanged=(dependencies&BigClockTextContent.WEATHER)!=0&&renderedWeatherRevision!=weather.revision;
            boolean timeChanged=(dependencies&BigClockTextContent.TIME)!=0
                    &&(renderedMinute!=nextMinute||renderedZoneOffset!=offset||!id.equals(renderedZone));
            boolean systemChanged=(dependencies&BigClockTextContent.CELLULAR)!=0&&renderedCellularRevision!=system.cellularRevision
                    ||(dependencies&BigClockTextContent.NETWORK)!=0&&renderedNetworkRevision!=system.networkRevision
                    ||(dependencies&BigClockTextContent.BATTERY)!=0&&renderedBatteryRevision!=system.batteryRevision;
            boolean iconChanged=(dependencies&(BigClockTextContent.CELLULAR|BigClockTextContent.NETWORK|BigClockTextContent.BATTERY))!=0&&renderedIconRevision!=styleRevision;
            if(!templateChanged&&!weatherChanged&&!timeChanged&&!systemChanged&&!iconChanged)return;
            CharSequence text=BigClockTextContent.resolveStyled(template,now,zone,weather,system,view.getText());
            renderedTemplate=template;renderedWeatherRevision=weather.revision;renderedMinute=nextMinute;
            renderedZone=id;renderedZoneOffset=offset;renderedCellularRevision=system.cellularRevision;
            renderedNetworkRevision=system.networkRevision;renderedBatteryRevision=system.batteryRevision;
            renderedIconRevision=styleRevision;
            if(!templateChanged&&!weatherChanged&&!systemChanged&&!iconChanged&&view.getText().toString().contentEquals(text))return;
            closeIcons(view.getText(),text);
            view.setText(text);measurementDirty=true;requestLayout();
        }
        void applyColor() {
            if(view==null)return;
            int resolved=follows?(maskEnabled?0xffffffff:mainColor):(dark?darkColor:light);
            if(view.getCurrentTextColor()!=resolved)view.setTextColor(resolved);
            applyBackdrop();
        }
        void applyBackdrop() {
            if(view==null)return;
            if(follows&&maskEnabled)material.apply(view,backdrop,maskColor,
                    Float.isFinite(ownMaskOpacity)?ownMaskOpacity:maskOpacity,backdropAppearance);
            else material.clear(view);
        }
        void measureText() {
            if(view==null||!measurementDirty&&!view.isLayoutRequested())return;
            int free=MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED);
            view.measure(free,free);measurementDirty=false;
        }
    }
}
