package dev.puitheme;

import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;
import java.util.TimeZone;

/** Existing placeholders must carry actual glyph spans, not renamed strings. */
public final class BigClockSystemTokenIconCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static ReplacementSpan[] spans(CharSequence text) {
        return text instanceof Spanned?((Spanned)text).getSpans(0,text.length(),ReplacementSpan.class):new ReplacementSpan[0];
    }
    private static final class Glyph extends Drawable {
        final int source;int color=0xffffffff,alpha=255,draws;final Paint paint=new Paint();
        Glyph(int source){this.source=source;}
        @Override public void draw(Canvas canvas){draws++;paint.setColor(color);paint.setAlpha(alpha);canvas.drawRect(0,0,getBounds().right,getBounds().bottom,paint);}
        @Override public void setTint(int color){this.color=color;}
        @Override public void setAlpha(int alpha){this.alpha=alpha;}
        @Override public int getAlpha(){return alpha;}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){}
        @Override public int getOpacity(){return -3;}
        @Override public int getIntrinsicWidth(){return 22;}
        @Override public int getIntrinsicHeight(){return 14;}
    }
    private static final class IconResources extends Resources {
        final boolean module;int loads;final java.util.List<Glyph> glyphs=new java.util.ArrayList<>();
        IconResources(boolean module){this.module=module;getDisplayMetrics().density=getDisplayMetrics().scaledDensity=1f;}
        @Override public Drawable getDrawable(int id,Theme theme){loads++;Glyph glyph=new Glyph(id+(module?10000:0));glyphs.add(glyph);return glyph;}
        @Override public int getIdentifier(String name,String kind,String pkg){return name.startsWith("stat_signal_")||name.startsWith("c17_signal_")?100+Math.abs(name.hashCode()%1000):0;}
    }
    private static final class IconContext extends Context {
        final IconResources nativeResources=new IconResources(false),moduleResources=new IconResources(true);
        @Override public Resources getResources(){return nativeResources;}
        @Override public Context createPackageContext(String name,int flags){return new Context(){@Override public Resources getResources(){return moduleResources;}};}
    }
    private static BigClockSystemIcon.Span systemIcon(TextView view) {
        Spanned value=(Spanned)view.getText();BigClockSystemIcon.Span[] icons=value.getSpans(0,value.length(),BigClockSystemIcon.Span.class);
        check(icons.length==1,"network slot owns one system icon");return icons[0];
    }
    private static boolean closed(BigClockSystemIcon.Span span)throws Exception {java.lang.reflect.Field field=span.getClass().getDeclaredField("closed");field.setAccessible(true);return field.getBoolean(span);}
    private static Canvas draw(TextView view)throws Exception {Canvas canvas=new Canvas();java.lang.reflect.Method method=TextView.class.getDeclaredMethod("onDraw",Canvas.class);method.setAccessible(true);method.invoke(view,canvas);return canvas;}
    private static void rowEvents()throws Exception {
        boolean padding=View.keepPaddingForCheck,width=TextView.measureTextWidth;View.keepPaddingForCheck=TextView.measureTextWidth=true;
        IconContext context=new IconContext();BigClockTextComponents row=new BigClockTextComponents(context);row.useLayoutBoundsForCheck=true;
        row.getResources().getDisplayMetrics().density=row.getResources().getDisplayMetrics().scaledDensity=1f;
        android.widget.FrameLayout root=new android.widget.FrameLayout(context);root.useLayoutBoundsForCheck=true;root.layout(0,0,400,800);root.addView(row);
        Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
        values.putBoolean(NotificationBigClockSettings.MASTER,true);
        values.putString(BigClockWidgetsSettings.LEFT_TEXT,"（天气）");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"网络 (网络状态)");
        values.putBoolean(BigClockWidgetsSettings.RIGHT_CLICK_ENABLED,true);values.putString(BigClockWidgetsSettings.RIGHT_LAUNCH_APP,"selected.network");
        final int[] clicks={0};row.setOnComponentClickListener(new BigClockTextComponents.OnComponentClickListener(){
            public boolean isComponentClickAllowed(View target,int side,String pkg){return side==BigClockWidgetsSettings.RIGHT&&"selected.network".equals(pkg);}
            public void onComponentClick(View target,int side,String pkg){clicks[0]++;}
        });
        try {
            BigClockSystemIcon.configure(context,values);BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);
            BigClockSystemText.wifiIcon(101,"stat_signal_wifi_signal_1_os17",true);row.configure(values);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            TextView left=(TextView)row.getChildAt(0),right=(TextView)row.getChildAt(1);left.useLayoutBoundsForCheck=right.useLayoutBoundsForCheck=true;
            CharSequence weather=left.getText(),network=right.getText();BigClockSystemIcon.Span first=systemIcon(right);
            BigClockSystemText.wifiIcon(104,"stat_signal_wifi_signal_4_os17",true);row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();
            check(network.toString().equals(right.getText().toString())&&first!=systemIcon(right),"icon-only Wi-Fi strength changes update the actual row with identical visible text");
            check(closed(first)&&left.getText()==weather,"replaced native icon is closed without replacing unrelated weather text");
            first=systemIcon(right);int nativeLoads=context.nativeResources.loads;
            values.putBoolean("wifi_enabled",true);values.putBoolean("wifi_icon_enabled",true);check(BigClockSystemIcon.configure(context,values),"effective style event publishes a changed policy");
            row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();check(first!=systemIcon(right)&&closed(first),"style-only change rebuilds the actual icon while text stays unchanged");
            check(context.moduleResources.loads>0&&context.nativeResources.loads==nativeLoads,"module style uses fresh module assets instead of the original native drawable");
            row.appearance(0x806699cc,false,false,0xffffffff,.2f);Canvas canvas=draw(right);
            Glyph glyph=context.moduleResources.glyphs.get(context.moduleResources.glyphs.size()-1);
            check(glyph.alpha==128&&(glyph.color>>>24)==255,"independent component tint applies source alpha exactly once");
            check(glyph.getBounds().width()>glyph.getBounds().height(),"the Wi-Fi glyph retains its landscape aspect ratio");
            int loads=context.nativeResources.loads+context.moduleResources.loads;
            int bitmaps=android.graphics.Bitmap.creations,copies=android.graphics.Bitmap.copies,reads=android.graphics.Bitmap.reads,scales=android.graphics.Bitmap.scales;
            int effects=right.renderEffectWrites,rebuilds=right.layoutRebuilds;
            for(int n=0;n<120;n++){row.refresh(0L,TimeZone.getTimeZone("UTC"));row.desiredHeight();draw(right);}
            check(loads==context.nativeResources.loads+context.moduleResources.loads&&rebuilds==right.layoutRebuilds,"steady icon draws reuse drawable instances and measured text");
            check(bitmaps==android.graphics.Bitmap.creations&&copies==android.graphics.Bitmap.copies&&reads==android.graphics.Bitmap.reads&&scales==android.graphics.Bitmap.scales,"steady icon rendering has no bitmap creation, copying, sampling or scaling");
            check(effects==right.renderEffectWrites&&right.renderEffect==null&&row.renderEffect==null,"plain inline icons create no independent render effect");
            BigClockSystemText.network(BigClockSystemText.Transport.CELLULAR,true);
            BigClockSystemText.cellularIcons(202,"stat_signal_signal_lte_single_2_os17",0,null);
            row.refresh(0L,TimeZone.getTimeZone("UTC"));first=systemIcon(right);CharSequence cellularText=right.getText();
            BigClockSystemText.cellularIcons(204,"stat_signal_signal_lte_single_4_os17",0,null);
            row.refresh(0L,TimeZone.getTimeZone("UTC"));
            check(first!=systemIcon(right)&&closed(first)&&cellularText.toString().equals(right.getText().toString()),"network-only component ignored cellular icon change while RAT text stayed identical");
            BigClockSystemIcon.assetsChanged();row.refresh(0L,TimeZone.getTimeZone("UTC"));check(left.getText()==weather,"pack-ready events leave the unrelated weather span and text intact");
            int height=(int)Math.ceil(row.desiredHeight()+row.topInset());row.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));row.layout(0,40,400,40+height);row.onLayout(true,0,0,400,height);row.updateClickTargets();
            float x=right.getLeft()+1f,y=40+right.getTop()+1f;
            boolean down=root.dispatchTouchEvent(new android.view.MotionEvent(android.view.MotionEvent.ACTION_DOWN,x,y,1000)),up=root.dispatchTouchEvent(new android.view.MotionEvent(android.view.MotionEvent.ACTION_UP,x,y,1040));
            check(down&&up&&clicks[0]==1,"mixed icon and text retain the original selected-app touch target: "+down+"/"+up+"/"+clicks[0]+", clickable="+right.isClickable()+", row="+row.getWidth()+"x"+row.getHeight()+", text="+right.getLeft()+","+right.getTop()+","+right.getWidth()+","+right.getHeight());
            first=systemIcon(right);row.close();check(closed(first),"closing the component releases its retained icon resources");
        } finally {row.close();BigClockSystemIcon.releaseRuntime();View.keepPaddingForCheck=padding;TextView.measureTextWidth=width;}
    }
    public static class NativeWifiModel { }
    public static final class NativeWifiVisible extends NativeWifiModel {
        private final int res;NativeWifiVisible(int res){this.res=res;}public int getRes(){return res;}
    }
    public static final class NativeWifiHidden extends NativeWifiModel { }
    private static void nativeWifiEntry()throws Exception {
        java.lang.reflect.Field model=StatusBarModule.class.getDeclaredField("nativeWifiIconClass"),visible=StatusBarModule.class.getDeclaredField("nativeWifiVisibleClass"),resource=StatusBarModule.class.getDeclaredField("nativeWifiResource");
        model.setAccessible(true);visible.setAccessible(true);resource.setAccessible(true);
        Object oldModel=model.get(null),oldVisible=visible.get(null),oldResource=resource.get(null);
        java.lang.reflect.Method event=StatusBarModule.class.getDeclaredMethod("captureSystemTextWifi",Object.class,Resources.class);event.setAccessible(true);
        Resources resources=new Resources();resources.entryNames.put(101,"stat_signal_wifi_signal_1_os17");resources.entryNames.put(104,"stat_signal_wifi_signal_4_os17");resources.entryNames.put(999,"unrelated_private_name");
        try {
            model.set(null,NativeWifiModel.class);visible.set(null,NativeWifiVisible.class);resource.set(null,NativeWifiVisible.class.getMethod("getRes"));
            BigClockSystemText.clear();BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);
            event.invoke(null,new NativeWifiVisible(101),resources);BigClockSystemText.Snapshot low=BigClockSystemText.snapshot();
            check(low.wifiLevel==1&&low.wifiResourceId==101,"the actual installed-module callback reads the native resolved resource");
            event.invoke(null,new NativeWifiVisible(104),resources);BigClockSystemText.Snapshot high=BigClockSystemText.snapshot();
            check(high.wifiLevel==4&&high.networkStatus.equals(low.networkStatus)&&high.networkRevision==low.networkRevision+1,"the module event publishes icon-only Wi-Fi changes with unchanged network text");
            event.invoke(null,new Object(),resources);check(BigClockSystemText.snapshot()==high,"unrelated callback objects cannot change component radio evidence");
            event.invoke(null,new NativeWifiHidden(),resources);
            check(Boolean.FALSE.equals(BigClockSystemText.snapshot().wifiVisible)&&BigClockSystemText.snapshot().wifiLevel==-1,"native Hidden clears the previous resource without inventing a disconnected full-strength icon");
            event.invoke(null,new NativeWifiVisible(999),resources);
            check(BigClockSystemText.snapshot().wifiResourceId==0&&BigClockSystemText.snapshot().wifiResourceName.isEmpty(),"unrecognised resource names are excluded from the published component snapshot");
        } finally {model.set(null,oldModel);visible.set(null,oldVisible);resource.set(null,oldResource);BigClockSystemText.clear();}
    }
    public static void main(String[] args)throws Exception {
        BigClockSystemText.clear();SystemWeatherText.clear();
        try {
            long token=BigClockSystemText.selectSubscription(7);BigClockSystemText.service(token,7,true);BigClockSystemText.signal(token,7,3);
            BigClockSystemText.dataNetwork(token,7,"5G");BigClockSystemText.network(BigClockSystemText.Transport.WIFI,true);BigClockSystemText.batteryRaw(85,100,2);
            String template="前（蜂窝信号） / (网络状态) · （电池）后";
            CharSequence rendered=BigClockTextContent.resolveStyled(template,0L,TimeZone.getTimeZone("UTC"));
            ReplacementSpan[] icons=spans(rendered);
            check(icons.length==3,"all three existing system placeholders must include real independent icon spans; got "+icons.length);
            String plain=BigClockTextContent.resolve(template,0L,TimeZone.getTimeZone("UTC"));
            check(rendered.toString().replace("\ufffc ","").equals(plain),"icons preserve current data text, ordinary user words and mixed punctuation");
            for(ReplacementSpan icon:icons) {
                Spanned value=(Spanned)rendered;int start=value.getSpanStart(icon),end=value.getSpanEnd(icon);
                check(end-start==1&&rendered.charAt(start)=='\ufffc',"each glyph owns its replacement character rather than replacing the user's data text");
            }
            for(ReplacementSpan icon:icons)((BigClockSystemIcon.Span)icon).close();
            rowEvents();
            nativeWifiEntry();
        } finally {BigClockSystemText.clear();SystemWeatherText.clear();}
        System.out.println("BigClockSystemTokenIconCheck: "+checks+" checks passed");
    }
}
