package dev.puitheme;
import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;

/** Real geometry seam: the released layout keeps its date above the clock. */
public final class BigClockDateSwapCheck {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Field field(Class<?> type,String name)throws Exception{for(Class<?> c=type;c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void set(Object target,String name,Object value)throws Exception{field(target.getClass(),name).set(target,value);}
    private static Object get(Object target,String name)throws Exception{return field(target.getClass(),name).get(target);}
    private static Method method(Class<?> type,String name,Class<?>...args)throws Exception{Method m=type.getDeclaredMethod(name,args);m.setAccessible(true);return m;}
    private static boolean near(float a,float b){return Math.abs(a-b)<.001f;}
    private static NotificationBigClockModel.Frame arrange(NotificationBigClockModel.Frame source,float height,
            float dateHeight,float dateShift,boolean swap)throws Exception {
        try{return (NotificationBigClockModel.Frame)NotificationBigClockModel.class.getDeclaredMethod("withAuxiliaryRows",
                NotificationBigClockModel.Frame.class,Float.TYPE,Float.TYPE,Boolean.TYPE,Float.TYPE,Float.TYPE)
                .invoke(null,source,height,1f,swap,dateHeight,dateShift);}
        catch(NoSuchMethodException released173){return NotificationBigClockModel.withComponents(source,height,1f);}
    }
    private static void geometry()throws Exception {
        NotificationBigClockModel.Frame source=NotificationBigClockModel.measured(1280f,1f,24f,180f,64f,20f,
                12f,18f,0f,0f,0f,600f,600f,0,0f,1f);
        NotificationBigClockModel.Frame swapped=arrange(source,64f,20f,0f,true);
        check(swapped.clockTop==source.clockTop&&swapped.clockHeight==source.clockHeight,"swapping auxiliary rows must preserve the primary clock anchor and glyph height");
        check(swapped.dateTop>=source.clockTop+source.clockHeight+8f,"date must occupy the old lower component row when swap is enabled");
        check(swapped.componentTop+64f<=source.clockTop,"tall component row grows upward and never covers the fixed main clock");
        check(swapped.componentsSwapped,"geometry exposes its active row order");
        NotificationBigClockModel.Frame ordinary=arrange(source,64f,20f,0f,false);
        check(ordinary.dateTop==source.dateTop&&!ordinary.componentsSwapped,"default order preserves the original date anchor");
        check(ordinary.reservedBottom==source.reservedBottom+72f,"default reservation remains exact");
        check(swapped.reservedBottom==source.reservedBottom+28f,"swapped lower date reserves its own real height rather than the taller old component row");
        check(arrange(source,0f,20f,0f,true)==source,"unavailable components add no hole");
        check(!arrange(source,64f,0f,0f,true).componentsSwapped,"unavailable date falls back without a false date slot");
        for(float d:new float[]{.5f,1f,2f,4f,8f})for(float dateShift:new float[]{-10f,0f,10f,400f}) {
            float reservation=Float.NaN;
            for(int scroll=0;scroll<=600;scroll+=3) {
                NotificationBigClockModel.Frame base=NotificationBigClockModel.measured(1280f*d,d,24f*d,180f*d,64f*d,20f*d,
                        12f,18f,0f,0f,dateShift,600f,600f,scroll,0f,1f);
                NotificationBigClockModel.Frame row=NotificationBigClockModel.withAuxiliaryRows(base,64f*d,d,true,20f*d,dateShift*d);
                check(row.clockTop==base.clockTop&&row.clockHeight==base.clockHeight,"scrolling and user offsets preserve the main anchor");
                check(row.progress==base.progress&&row.collapseDistance==base.collapseDistance&&row.entryAlpha==base.entryAlpha,"native progress and fade remain unchanged");
                if(Float.isNaN(reservation))reservation=row.reservedBottom;
                check(near(row.reservedBottom,reservation),"intrinsic notification reservation remains fixed throughout collapse");
            }
        }
        check(NotificationBigClockModel.auxiliarySwapFits(100,20,132,180,64,12,24),"upper slot includes real negative-Y inset");
        check(!NotificationBigClockModel.auxiliarySwapFits(100,20,132,180,128,12,24),"insufficient upper space falls back at the expanded baseline");
        check(!NotificationBigClockModel.auxiliarySwapFits(140,20,132,180,32,0,24),"deliberately overlapping date never produces an automatic overlapping component row");
        check(NotificationBigClockModel.auxiliarySwapFits(400,20,132,180,64,0,24),"an existing lower date supports exchanging the lower rows");
    }

    private static final class Scene implements AutoCloseable {
        final NotificationBigClock owner=new NotificationBigClock();final Context context=new Context();
        final FrameLayout root=new FrameLayout(context),overlay,header;final BigClockTextComponents row;
        final TextView clock,date,left,right;final Bundle values=new Bundle();final Class<?> viewType,settingsType;
        final Constructor<?> settingsConstructor;int clicks,lastSide=-1;
        @SuppressWarnings("unchecked") Scene()throws Exception {
            root.useLayoutBoundsForCheck=true;root.layout(0,0,400,1280);dimensions(root);
            viewType=Class.forName("dev.puitheme.NotificationBigClock$ClockView");settingsType=Class.forName("dev.puitheme.NotificationBigClock$Settings");
            Constructor<?> ctor=viewType.getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);overlay=(FrameLayout)ctor.newInstance(owner,context);root.addView(overlay);overlay.layout(0,0,400,1280);dimensions(overlay);
            set(owner,"panel",new WeakReference<View>(root));set(owner,"overlay",overlay);set(owner,"active",true);set(owner,"barState",0);set(owner,"fraction",1f);
            Constructor<?> h=Class.forName("dev.puitheme.NotificationBigClock$HeaderState").getDeclaredConstructor(View.class);h.setAccessible(true);((java.util.Map<Object,Object>)get(owner,"headers")).put(root,h.newInstance(new FrameLayout(context)));
            header=(FrameLayout)get(overlay,"headerMotion");clock=(TextView)get(overlay,"clock");date=(TextView)get(overlay,"dateLabel");
            settingsConstructor=settingsType.getDeclaredConstructor(Bundle.class,String.class);settingsConstructor.setAccessible(true);
            values.putBoolean(NotificationBigClockSettings.MASTER,true);values.putBoolean(NotificationBigClockSettings.GLASS,false);values.putBoolean(NotificationBigClockSettings.ENTRY_EFFECT_ENABLED,false);
            values.putBoolean(NotificationBigClockSettings.DATE_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(NotificationBigClockSettings.FONT,"system");values.putString(NotificationBigClockSettings.PATTERN,"HH:mm");values.putString(NotificationBigClockSettings.DATE_PATTERN,"M月d日");
            values.putFloat(NotificationBigClockSettings.DATE_GAP,20f);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"左侧");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"右侧");values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,40f);values.putFloat(BigClockWidgetsSettings.RIGHT_SIZE,20f);
            values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-8f);values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,6f);
            values.putBoolean(BigClockWidgetsSettings.LEFT_CLICK_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_CLICK_ENABLED,true);values.putString(BigClockWidgetsSettings.LEFT_LAUNCH_APP,"selected.left");values.putString(BigClockWidgetsSettings.RIGHT_LAUNCH_APP,"selected.right");
            row=new BigClockTextComponents(context);dimensions(row);row.configure(values);row.refresh(0L,java.util.TimeZone.getTimeZone("UTC"));header.addView(row);set(overlay,"components",row);left=(TextView)row.getChildAt(0);right=(TextView)row.getChildAt(1);dimensions(overlay);
            row.setOnComponentClickListener(new BigClockTextComponents.OnComponentClickListener(){public boolean isComponentClickAllowed(View target,int side,String app){return true;}public void onComponentClick(View target,int side,String app){clicks++;lastSide=side;}});
        }
        void dimensions(View v){v.useLayoutBoundsForCheck=true;v.getResources().getDisplayMetrics().density=v.getResources().getDisplayMetrics().scaledDensity=1f;v.getResources().getDisplayMetrics().widthPixels=400;v.getResources().getDisplayMetrics().heightPixels=1280;v.getResources().getConfiguration().orientation=1;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)dimensions(((ViewGroup)v).getChildAt(i));}
        void apply(boolean swap)throws Exception {
            values.putBoolean("notification_big_clock_widget_swap_with_date",swap);Object settings=settingsConstructor.newInstance(values,NotificationBigClockSettings.PORTRAIT_PREFIX);
            set(owner,"settings",settings);set(owner,"portraitSettings",settings);set(owner,"componentSettings",new Bundle(values));set(overlay,"cachedFrame",null);
            method(viewType,"updateWidgets",Boolean.TYPE).invoke(overlay,false);
            row.onLayout(true,0,0,row.getWidth(),row.getHeight());row.updateClickTargets();
        }
        float y(View view){float y=0;for(View v=view;v!=root;v=(View)v.getParent())y+=v.getTop()+v.getTranslationY();return y;}
        boolean tap(View target){float x=0;for(View v=target;v!=root;v=(View)v.getParent())x+=v.getLeft()+v.getTranslationX();float y=y(target)+1f;return root.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN,x+1f,y,1000))&&root.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_UP,x+1f,y,1050));}
        public void close()throws Exception{row.close();method(viewType,"clearMaterials").invoke(overlay);ClockBackdropSource.clear();}
    }
    private static void actualLayout()throws Exception {
        boolean padding=View.keepPaddingForCheck,measure=TextView.measureTextWidth;View.keepPaddingForCheck=TextView.measureTextWidth=true;
        try(Scene scene=new Scene()) {
            scene.apply(false);float clockY=scene.y(scene.clock),oldDateY=scene.y(scene.date);float rowHeight=(Float)get(scene.overlay,"componentHeight");
            check(rowHeight>scene.date.getMeasuredHeight(),"integration uses a genuinely taller component row");
            scene.apply(true);
            check("swapped".equals(scene.owner.componentLayoutDiagnostics()),"actual runtime chooses swap for usable expanded geometry");
            check(near(scene.y(scene.clock),clockY),"actual on-screen main time position is unchanged despite header top and inset changes");
            check(scene.y(scene.left)<clockY&&scene.y(scene.right)<clockY,"both original sides move together above the main time");
            check(scene.y(scene.date)>clockY+scene.clock.getMeasuredHeight(),"actual date TextView moves to the lower row");
            check(Math.abs(scene.y(scene.left)+8f-(oldDateY+scene.date.getMeasuredHeight()-rowHeight))<=.5f,"negative component Y remains relative to the new row anchor: "+scene.y(scene.left)+", old date "+oldDateY+", date h "+scene.date.getMeasuredHeight()+", row h "+rowHeight);
            check(scene.tap(scene.left)&&scene.clicks==1&&scene.lastSide==0,"left glyph remains reachable through actual root/header/row DOWN/UP dispatch");
            check(scene.tap(scene.right)&&scene.clicks==2&&scene.lastSide==1,"right glyph keeps its own target and side after moving");
            float swappedDate=scene.y(scene.date);Object frame=method(scene.viewType,"frame").invoke(scene.overlay);int revision=(Integer)get(scene.overlay,"measurementRevision");
            for(int n=0;n<20;n++)check(method(scene.viewType,"frame").invoke(scene.overlay)==frame,"stable frames reuse cached row geometry");
            check((Integer)get(scene.overlay,"measurementRevision")==revision,"stable frames do not remeasure fonts");
            scene.apply(false);check(near(scene.y(scene.clock),clockY)&&near(scene.y(scene.date),oldDateY),"turning off restores original text coordinates without moving the main time");
            scene.values.putBoolean(NotificationBigClockSettings.DATE_ENABLED,false);scene.apply(true);check("date_unavailable".equals(scene.owner.componentLayoutDiagnostics()),"disabled date has a fixed natural-layout reason");
            check(!((NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay)).componentsSwapped,"disabled date creates no hidden date slot");
            scene.values.putBoolean(NotificationBigClockSettings.DATE_ENABLED,true);scene.values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,64f);scene.values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,200f);scene.apply(true);
            check("insufficient_upper_space_or_date_overlap".equals(scene.owner.componentLayoutDiagnostics()),"oversized row reports the bounded fallback instead of silently doing nothing");
            check(near(scene.y(scene.clock),clockY),"fallback also preserves the main screen anchor");
            scene.values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,40f);scene.values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-8f);scene.apply(true);check(scene.y(scene.date)==swappedDate,"restoring valid geometry re-enables saved row order");
            scene.values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,false);scene.values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,false);scene.apply(true);
            NotificationBigClockModel.Frame noComponents=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
            check(!noComponents.componentsSwapped&&"components_unavailable".equals(scene.owner.componentLayoutDiagnostics()),"both disabled components use natural layout with a fixed reason");
            check(Float.isNaN(noComponents.componentTop),"missing components do not retain a transparent row slot");
            scene.values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);scene.values.putBoolean(StatusBarSettings.SAFE_MODE,true);scene.apply(true);
            NotificationBigClockModel.Frame safe=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
            check(!safe.componentsSwapped&&"safe_mode".equals(scene.owner.componentLayoutDiagnostics()),"safety mode never applies a row swap");
            check(scene.values.getBoolean(BigClockWidgetsSettings.SWAP_WITH_DATE),"fallback does not erase a saved enabled switch");
            scene.values.putBoolean(StatusBarSettings.SAFE_MODE,false);scene.root.getResources().getConfiguration().orientation=2;
            Object landscape=scene.settingsConstructor.newInstance(scene.values,NotificationBigClockSettings.LANDSCAPE_PREFIX);set(scene.owner,"settings",landscape);set(scene.overlay,"cachedFrame",null);
            NotificationBigClockModel.Frame horizontal=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
            check(!horizontal.componentsSwapped&&get(scene.overlay,"components")==null,"landscape releases portrait components rather than borrowing the portrait swap");
        } finally {View.keepPaddingForCheck=padding;TextView.measureTextWidth=measure;}
    }
    private static final class OutsetFont extends android.text.TextPaint {
        @Override public int getFontMetricsInt(android.graphics.Paint.FontMetricsInt fm) {
            float size=getTextSize();fm.ascent=-(int)Math.ceil(size*.8f);fm.descent=(int)Math.ceil(size*.2f);
            fm.top=-(int)Math.ceil(size);fm.bottom=(int)Math.ceil(size*.3f);fm.leading=0;return fm.descent-fm.ascent;
        }
        @Override public void getTextBounds(String value,int start,int end,android.graphics.Rect bounds) {
            super.getTextBounds(value,start,end,bounds);bounds.top=-(int)Math.ceil(getTextSize()*.95f);bounds.bottom=(int)Math.ceil(getTextSize()*.25f);
        }
    }
    /** Own RenderEffects retain only each TextView's content domain. Check the
     * actual ClockView consumes its larger font-safe measurement while keeping
     * the primary clock at the same screen position and real touch geometry. */
    private static void actualCompleteFontLayout()throws Exception {
        boolean padding=View.keepPaddingForCheck,measure=TextView.measureTextWidth,nativePad=TextView.nativeFontPaddingForCheck;
        View.keepPaddingForCheck=TextView.measureTextWidth=TextView.nativeFontPaddingForCheck=true;
        try(Scene scene=new Scene()) {
            Field paint=TextView.class.getDeclaredField("paint");paint.setAccessible(true);
            for(TextView view:new TextView[]{scene.left,scene.right}){OutsetFont font=new OutsetFont();font.set(view.getPaint());paint.set(view,font);}
            scene.values.putString(BigClockWidgetsSettings.LEFT_TEXT,"天气暂无数据");scene.values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"29% 充电中");
            android.graphics.Rect ink=new android.graphics.Rect();
            for(float size:new float[]{18f,40f,64f})for(int offset:new int[]{-12,0,10}) {
                scene.values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,size);scene.values.putFloat(BigClockWidgetsSettings.RIGHT_SIZE,size);
                scene.values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,offset);scene.values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,-offset);
                scene.apply(false);float mainY=scene.y(scene.clock);scene.apply(true);
                check("swapped".equals(scene.owner.componentLayoutDiagnostics())||"insufficient_upper_space_or_date_overlap".equals(scene.owner.componentLayoutDiagnostics()),"full font row uses a safe stable order at size="+size+", offset="+offset);
                check(near(scene.y(scene.clock),mainY),"complete font height must not move the main clock screen anchor");
                for(TextView view:new TextView[]{scene.left,scene.right}) {
                    view.getPaint().getTextBounds(view.getText().toString(),0,view.getText().length(),ink);
                    check(view.getBaseline()+ink.top>=0&&view.getBaseline()+ink.bottom<=view.getMeasuredHeight(),"both upper and lower ink are inside the component's own effect layer");
                    check(scene.y(view)>=scene.y(scene.row)&&scene.y(view)+view.getHeight()<=scene.y(scene.row)+scene.row.getHeight(),"compensated negative and positive Y fit inside the measured row");
                    check(scene.y(view)>=scene.y(scene.header)&&scene.y(view)+view.getHeight()<=scene.y(scene.header)+scene.header.getHeight(),"header allocation contains complete swapped component bounds");
                    check(view.getTextSize()==size&&view.getScaleY()==1f&&view.getEllipsize()==null,"font-safe layout preserves configured font size and full content");
                }
                int clicks=scene.clicks;
                check(scene.tap(scene.left)&&scene.clicks==clicks+1&&scene.lastSide==0,"font padding preserves left component dispatch through its actual parent bounds");
                check(scene.tap(scene.right)&&scene.clicks==clicks+2&&scene.lastSide==1,"font padding preserves right component dispatch through its actual parent bounds");
                scene.apply(false);check(near(scene.y(scene.clock),mainY),"restoring natural order retains the same main clock anchor after font expansion");
            }
        } finally {View.keepPaddingForCheck=padding;TextView.measureTextWidth=measure;TextView.nativeFontPaddingForCheck=nativePad;}
    }
    /** Replays the two captured phone anchors: expanded Y260, collapsed Y143,
     * with status/cutout safeTop160, row128 and complete 97px child layers. */
    private static void capturedCollapseGeometry()throws Exception {
        float expandedDate=Math.max(160f+48f,3168f*.095f),expandedClock=expandedDate+87f+48f;
        check(NotificationBigClockModel.auxiliarySwapFits(expandedDate,87f,expandedClock,585f,128f,0f,160f),"captured expanded phone row is a valid upper slot");
        for(int scroll=0;scroll<=1200;scroll+=3) {
            NotificationBigClockModel.Frame base=NotificationBigClockModel.measured(3168f,4f,160f,585f,248f,87f,12f,18f,0f,0f,0f,600f,600f,scroll,0f,1f);
            NotificationBigClockModel.Frame row;
            try {row=(NotificationBigClockModel.Frame)NotificationBigClockModel.class.getDeclaredMethod("withAuxiliaryRows",
                NotificationBigClockModel.Frame.class,Float.TYPE,Float.TYPE,Boolean.TYPE,Float.TYPE,Float.TYPE,Float.TYPE,Float.TYPE)
                .invoke(null,base,128f,4f,true,87f,0f,160f,0f);}
            catch(NoSuchMethodException old177){row=NotificationBigClockModel.withAuxiliaryRows(base,128f,4f,true,87f,0f);}
            check(row.componentTop>=160f,"captured upper row must never enter the 160px status/cutout region during collapse: scroll="+scroll+", top="+row.componentTop);
            check(row.componentTop+128f<=row.clockTop,"safe upper row must not cover or move the primary time");
            check(row.clockTop==base.clockTop&&row.clockHeight==base.clockHeight&&row.progress==base.progress,"safe row allocation preserves native clock animation");
        }
    }
    private static void actualCollapsedCoverage()throws Exception {
        boolean padding=View.keepPaddingForCheck,measure=TextView.measureTextWidth;View.keepPaddingForCheck=TextView.measureTextWidth=true;
        try(Scene scene=new Scene()) {
            scene.values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,0f);scene.values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,0f);
            for(int scroll:new int[]{0,50,150,400,1000,150,0}) {
                set(scene.owner,"scrollY",scroll);scene.apply(false);float main=scene.y(scene.clock);scene.apply(true);
                check("swapped".equals(scene.owner.componentLayoutDiagnostics()),"a row that fits both endpoints keeps its order throughout collapse");
                check(near(scene.y(scene.clock),main),"clamping auxiliary coverage never shifts main time at scroll="+scroll);
                float safe=(Float)get(scene.overlay,"safeTop");
                for(TextView view:new TextView[]{scene.left,scene.right}) {
                    check(scene.y(view)>=safe,"real ClockView keeps full upper child coverage below status safeTop, scroll="+scroll+", y="+scene.y(view)+", safe="+safe);
                    check(scene.y(view)+view.getHeight()<=scene.y(scene.clock),"real upper child does not overlap the main time");
                }
                check(scene.tap(scene.left)&&scene.tap(scene.right),"safe collapsed geometry still dispatches both component app clicks");
            }
            set(scene.owner,"scrollY",1000);scene.values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,18f);scene.values.putFloat(BigClockWidgetsSettings.RIGHT_SIZE,18f);scene.apply(true);
            NotificationBigClockModel.Frame before=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
            float height=scene.row.desiredHeight();Bundle moved=new Bundle(scene.values);moved.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-4f);scene.row.configure(moved);
            NotificationBigClockModel.Frame after=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
            check(scene.row.desiredHeight()==height&&scene.row.topInset()==4,"negative Y can change full upper bounds while the minimum row height stays unchanged");
            check(after!=before&&after.componentTop-scene.row.topInset()>=(Float)get(scene.overlay,"safeTop"),"frame cache includes changed upper inset independently of the unchanged requested height");
            scene.row.configure(scene.values);
            scene.values.putFloat(BigClockWidgetsSettings.LEFT_SIZE,64f);
            for(int scroll:new int[]{0,200,1000,0}) {
                set(scene.owner,"scrollY",scroll);scene.apply(true);
                check("insufficient_upper_space_or_date_overlap".equals(scene.owner.componentLayoutDiagnostics()),"row too tall for the compact endpoint uses one stable natural order before animation starts");
                NotificationBigClockModel.Frame frame=(NotificationBigClockModel.Frame)method(scene.viewType,"frame").invoke(scene.overlay);
                check(!frame.componentsSwapped&&scene.y(scene.left)>=scene.y(scene.clock)+scene.clock.getHeight(),"large row remains complete below the unchanged main time");
            }
        }finally{View.keepPaddingForCheck=padding;TextView.measureTextWidth=measure;}
    }
    public static void main(String[] args)throws Exception {
        capturedCollapseGeometry();geometry();actualLayout();actualCompleteFontLayout();actualCollapsedCoverage();
        System.out.println("BigClockDateSwapCheck: "+checks+" checks passed");
    }
}
