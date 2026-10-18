package dev.puitheme;
import android.os.Bundle;
import android.graphics.Canvas;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.service.notification.StatusBarNotification;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.phone.NotificationIconContainer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
public final class NotificationIconAreaCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);}
    private static void near(float expected,float actual){checks++;if(Math.abs(expected-actual)>.0001f)throw new AssertionError(expected+" != "+actual);}
    private static Object field(Object owner,String name)throws Exception{
        Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }
    /** Allocate only metadata: the desktop test never constructs Android drawing members. */
    private static Object metadata(Class<?> type)throws Exception{
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");
        Field singleton=unsafeClass.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        return unsafeClass.getMethod("allocateInstance",Class.class).invoke(singleton.get(null),type);
    }
    private static final class ScopedResources extends Resources {
        int lookups;
        @Override public String getResourceEntryName(int id){
            lookups++;return id==101?"notificationIcons":id==202?"status_bar":id==404?"aod_notification_icons":"new_statusbar_host";
        }
        @Override public int getIdentifier(String name,String type,String pkg){return "notificationIcons".equals(name)?101:0;}
    }
    private static class PhoneContainer extends NotificationIconContainer {
        final ScopedResources scoped=new ScopedResources();
        PhoneContainer(){super(null);scoped.getDisplayMetrics().density=1f;}
        @Override public int getId(){return 101;}
        @Override public Resources getResources(){return scoped;}
    }
    private static final class NativeHost extends ViewGroup {
        final int id;final ScopedResources scoped=new ScopedResources();
        NativeHost(int id){super(null);this.id=id;}
        @Override public int getId(){return id;}
        @Override public Resources getResources(){return scoped;}
        @Override public View findViewById(int id){for(int i=0;i<getChildCount();i++)if(getChildAt(i).getId()==id)return getChildAt(i);return null;}
    }
    @SuppressWarnings("unchecked")
    private static void firstEnableDrawing()throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        PhoneContainer owner=new PhoneContainer();new NativeHost(202).addView(owner);
        StatusBarIconView first=new StatusBarIconView(null);first.setNotification((StatusBarNotification)metadata(StatusBarNotification.class));owner.addView(first);
        StatusBarIconView second=new StatusBarIconView(null);second.mBundleEntry=new Object();owner.addView(second);
        Map<ViewGroup,Object> states=(Map<ViewGroup,Object>)field(area,"states");

        // Native layout must observe the cached host while the new feature is still OFF.
        area.changed(owner);equal(1,states.size());equal(0,owner.invalidations);
        int[] nativeDraws={0};NotificationIconArea.Draw nativeDraw=()->{nativeDraws[0]++;return null;};
        int lookups=owner.scoped.lookups,reads=first.getterCalls;
        for(int i=0;i<10;i++)area.draw(owner,new Canvas(),nativeDraw);
        equal(10,nativeDraws[0]);equal(lookups,owner.scoped.lookups);equal(reads,first.getterCalls);

        Bundle enabled=new Bundle();enabled.putBoolean(NotificationIconArea.MASTER,true);enabled.putString(NotificationIconArea.MODE,"heart");
        enabled.putBoolean(NotificationIconArea.SIZE_ENABLED,false);
        area.configure(null,enabled);equal(1,owner.invalidations);
        Canvas canvas=new Canvas();area.draw(owner,canvas,nativeDraw);
        equal("♥",canvas.text);equal(255,canvas.textAlpha);equal(10,nativeDraws[0]);equal(canvas.saves,canvas.restores);
        equal(2,((List<?>)field(states.get(owner),"icons")).size());

        // ColorOS preserves full-color app artwork with a zero static tint. Mono symbols use
        // the real DarkIconDispatcher decor tone, including black on a light background.
        first.staticDrawableColor=0;first.mDecorColor=0xff000000;
        canvas=new Canvas();area.draw(owner,canvas,nativeDraw);equal(0xff000000,canvas.textColor);equal(255,canvas.textAlpha);
        first.mDecorColor=0xffffffff;canvas=new Canvas();area.draw(owner,canvas,nativeDraw);equal(0xffffffff,canvas.textColor);

        first.setNotification(null);second.mBundleEntry=null;area.changed(owner);
        canvas=new Canvas();area.draw(owner,canvas,nativeDraw);equal(true,canvas.text==null);equal(11,nativeDraws[0]);
        area.configure(null,new Bundle());equal(3,owner.invalidations); // change event plus disable each invalidate once
        reads=first.getterCalls;lookups=owner.scoped.lookups;
        area.draw(owner,new Canvas(),nativeDraw);equal(12,nativeDraws[0]);equal(reads,first.getterCalls);equal(lookups,owner.scoped.lookups);

        // Bind precisely through the native Phone binder when ancestor names change, then
        // revoke that identity if the same container is reparented outside that bound area.
        NotificationIconArea bound=new NotificationIconArea();bound.resolve(NotificationIconAreaCheck.class.getClassLoader());
        PhoneContainer renamed=new PhoneContainer();NativeHost realArea=new NativeHost(303);realArea.addView(renamed);
        StatusBarIconView live=new StatusBarIconView(null);live.mBundleEntry=new Object();renamed.addView(live);
        bound.changed(renamed);equal(0,((Map<?,?>)field(bound,"states")).size());
        bound.bindPhoneArea(realArea);equal(1,((Map<?,?>)field(bound,"states")).size());equal(0,renamed.invalidations);
        bound.configure(null,enabled);canvas=new Canvas();bound.draw(renamed,canvas,nativeDraw);equal("♥",canvas.text);
        new NativeHost(404).addView(renamed);bound.changed(renamed);
        canvas=new Canvas();bound.draw(renamed,canvas,nativeDraw);equal(true,canvas.text==null);
        equal(0,((Map<?,?>)field(bound,"states")).size());equal(0,((Map<?,?>)field(bound,"phoneBindings")).size());
        equal(true,field(bound,"states") instanceof WeakHashMap);
        equal(true,field(bound,"phoneBindings") instanceof WeakHashMap);
    }
    @SuppressWarnings("unchecked")
    private static void nativeContracts()throws Exception{
        Method privateHeight=StatusBarIconView.class.getDeclaredMethod("getIconHeight");
        equal(true,Modifier.isPrivate(privateHeight.getModifiers()));
        boolean publiclyVisible=true;try{StatusBarIconView.class.getMethod("getIconHeight");}catch(NoSuchMethodException expected){publiclyVisible=false;}
        equal(false,publiclyVisible);

        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        equal(privateHeight,field(area,"iconHeight"));
        equal(StatusBarIconView.class.getMethod("getIconScale"),field(area,"iconScale"));
        equal(StatusBarIconView.class.getDeclaredField("mBundleEntry"),field(area,"bundleEntry"));
        StatusBarIconView icon=new StatusBarIconView(null);
        equal(96f,((Method)field(area,"iconHeight")).invoke(icon)); // Production resolve makes the private getter callable.
        equal(.5f,((Method)field(area,"iconScale")).invoke(icon));

        NotificationIconContainer owner=new NotificationIconContainer(null);
        owner.getResources().getDisplayMetrics().density=2f;
        owner.addView(icon);
        Class<?> stateClass=Class.forName("dev.puitheme.NotificationIconArea$State");
        java.lang.reflect.Constructor<?> constructor=stateClass.getDeclaredConstructor();constructor.setAccessible(true);
        Object state=constructor.newInstance();
        Field iconsField=stateClass.getDeclaredField("icons");iconsField.setAccessible(true);
        ArrayList<WeakReference<View>> icons=new ArrayList<>();iconsField.set(state,icons);
        Method update=NotificationIconArea.class.getDeclaredMethod("update",ViewGroup.class,stateClass);update.setAccessible(true);

        // A real ordinary notification is non-null; no Binder or Android constructor is needed.
        icon.setNotification((StatusBarNotification)metadata(StatusBarNotification.class));
        update.invoke(area,owner,state);equal(1,icons.size());equal(icon,icons.get(0).get());equal(48f,field(state,"height"));
        icon.nativeScale=.25f;icon.iconAppearAmount=.01f;
        update.invoke(area,owner,state);equal(24f,field(state,"height"));
        icon.iconAppearAmount=1f;update.invoke(area,owner,state);equal(24f,field(state,"height"));
        // Density affects configured dp, while native stable height is intrinsic pixels * native scale.
        owner.getResources().getDisplayMetrics().density=4f;update.invoke(area,owner,state);equal(24f,field(state,"height"));

        icon.setNotification(null);update.invoke(area,owner,state);equal(0,icons.size());
        icon.mBundleEntry=new Object();update.invoke(area,owner,state);equal(1,icons.size());equal(24f,field(state,"height"));
        icon.blocked=true;update.invoke(area,owner,state);equal(0,icons.size());
        icon.blocked=false;icon.visibleState=2;update.invoke(area,owner,state);equal(0,icons.size());
        icon.visibleState=0;icon.setVisibility(View.INVISIBLE);update.invoke(area,owner,state);equal(0,icons.size());
        icon.setVisibility(View.VISIBLE);owner.addView(new View(null));update.invoke(area,owner,state);equal(1,icons.size());

        // Re-applying options must refresh native cached tint/size after a safe-mode interval.
        Map<ViewGroup,Object> states=(Map<ViewGroup,Object>)field(area,"states");states.put(owner,state);
        equal(false,field(state,"dirty"));area.configure(null,new Bundle());equal(true,field(state,"dirty"));
        equal(1,owner.invalidations);
    }
    private static Bundle countOptions(boolean master,int max){
        Bundle b=new Bundle();b.putBoolean(NotificationIconArea.MASTER,master);
        b.putBoolean(NotificationIconArea.COUNT_ENABLED,true);b.putFloat(NotificationIconArea.MAX_COUNT,max);
        b.putBoolean(NotificationIconArea.SIZE_ENABLED,false);b.putBoolean(NotificationIconArea.POSITION,false);return b;
    }
    private static PhoneContainer phone(int children){
        PhoneContainer owner=new PhoneContainer();new NativeHost(202).addView(owner);
        for(int i=0;i<children;i++){StatusBarIconView icon=new StatusBarIconView(null);icon.mBundleEntry=new Object();owner.addView(icon);}
        return owner;
    }
    private static void nativeCounts()throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        PhoneContainer owner=phone(5);area.changed(owner);area.configure(null,countOptions(false,2));
        equal(Integer.MAX_VALUE,owner.mMaxIcons);equal(0,owner.stateUpdates);
        area.configure(null,countOptions(true,2));equal(2,owner.mMaxIcons);equal(1,owner.stateUpdates);
        equal(0,((StatusBarIconView)owner.getChildAt(0)).visibleState);equal(0,((StatusBarIconView)owner.getChildAt(1)).visibleState);
        equal(1,((StatusBarIconView)owner.getChildAt(2)).visibleState);equal(2,((StatusBarIconView)owner.getChildAt(3)).visibleState);
        int updates=owner.stateUpdates,layouts=owner.layoutRequests;
        for(int i=0;i<1000;i++)area.beforeLayout(owner);
        equal(updates,owner.stateUpdates);equal(layouts,owner.layoutRequests);
        owner.setMaxIconsAmount(area.nativeMaxIcons(owner,7));equal(2,owner.mMaxIcons);
        area.configure(null,new Bundle());equal(7,owner.mMaxIcons);equal(0,((StatusBarIconView)owner.getChildAt(3)).visibleState);
        area.configure(null,countOptions(true,2));Bundle heart=countOptions(true,0);heart.putString(NotificationIconArea.MODE,"heart");
        area.configure(null,heart);equal(7,owner.mMaxIcons);Canvas canvas=new Canvas();int[] draws={0};
        area.draw(owner,canvas,()->{draws[0]++;return null;});equal("♥",canvas.text);equal(0,draws[0]);
        area.configure(null,countOptions(true,0));equal(0,owner.mMaxIcons);equal(1,((StatusBarIconView)owner.getChildAt(0)).visibleState);
        canvas=new Canvas();area.draw(owner,canvas,()->{draws[0]++;return null;});equal(0,draws[0]);equal(true,canvas.text==null);equal(5,owner.getChildCount());
        Bundle safe=countOptions(true,0);safe.putBoolean(StatusBarSettings.SAFE_MODE,true);area.configure(null,safe);equal(7,owner.mMaxIcons);
        area.draw(owner,new Canvas(),()->{draws[0]++;return null;});equal(1,draws[0]);
        area.configure(null,countOptions(true,1000000));equal(5,owner.mMaxIcons);
        owner.removeView(owner.getChildAt(4));area.beforeLayout(owner);equal(4,owner.mMaxIcons);
        area.configure(null,new Bundle());equal(7,owner.mMaxIcons);
        area.configure(null,countOptions(true,2));owner.mMaxIcons=9;area.beforeLayout(owner);equal(2,owner.mMaxIcons);
        area.configure(null,new Bundle());equal(9,owner.mMaxIcons);
        area.configure(null,countOptions(true,2));owner.failNextStateUpdate=true;
        area.configure(null,countOptions(true,3));equal(9,owner.mMaxIcons);equal(0,((StatusBarIconView)owner.getChildAt(3)).visibleState);
        area.beforeLayout(owner);equal(3,owner.mMaxIcons);area.configure(null,new Bundle());equal(9,owner.mMaxIcons);
        area.configure(null,countOptions(true,2));new NativeHost(404).addView(owner);area.changed(owner);
        equal(9,owner.mMaxIcons);equal(0,((Map<?,?>)field(area,"states")).size());
        equal(8,area.nativeMaxIcons(owner,8));area.beforeLayout(owner);equal(9,owner.mMaxIcons);
        new NativeHost(202).addView(owner);area.changed(owner);equal(2,owner.mMaxIcons);
        area.detach(owner);equal(9,owner.mMaxIcons);equal(0,((Map<?,?>)field(area,"states")).size());
        area.changed(owner);equal(2,owner.mMaxIcons);
        Bundle large=heart;large.putBoolean(NotificationIconArea.SIZE_ENABLED,true);large.putFloat(NotificationIconArea.SIZE,Float.MAX_VALUE);
        area.configure(null,large);canvas=new Canvas();area.draw(owner,canvas,()->{draws[0]++;return null;});
        equal(NumericPolicy.MAX_TEXT_PIXELS,canvas.textSize);equal(canvas.saves,canvas.restores);
        large.putFloat(NotificationIconArea.SIZE,0f);area.configure(null,large);canvas=new Canvas();area.draw(owner,canvas,()->{draws[0]++;return null;});equal(0f,canvas.textSize);
        large=countOptions(true,2);large.putBoolean(NotificationIconArea.SIZE_ENABLED,true);large.putFloat(NotificationIconArea.SIZE,Float.MAX_VALUE);
        area.configure(null,large);canvas=new Canvas();int beforeNative=draws[0];area.draw(owner,canvas,()->{draws[0]++;return null;});
        equal(beforeNative+1,draws[0]);equal(true,Float.isFinite(canvas.scaleX));equal(true,canvas.scaleX>0f);equal(canvas.saves,canvas.restores);
        area.configure(null,countOptions(true,2));
        Field removed=ModuleLifecycle.class.getDeclaredField("removed");removed.setAccessible(true);
        beforeNative=draws[0];
        try{removed.setBoolean(null,true);area.draw(owner,new Canvas(),()->{draws[0]++;return null;});equal(9,owner.mMaxIcons);equal(beforeNative+1,draws[0]);}
        finally{removed.setBoolean(null,false);}
        equal(false,NotificationIconArea.BOOLEANS.get(NotificationIconArea.COUNT_ENABLED));equal(3f,NotificationIconArea.NUMBERS.get(NotificationIconArea.MAX_COUNT));
        equal(0,NotificationIconArea.safeCount(-1,5));equal(5,NotificationIconArea.safeCount(Float.MAX_VALUE,5));
        equal(3,NotificationIconArea.safeCount(Float.NaN,5));equal(0,NotificationIconArea.safeCount(100,0));
    }
    private static final class Glyph extends android.graphics.drawable.Drawable {
        final int side;Glyph(int side){this.side=side;setBounds(0,0,side,side);}
        @Override public int getIntrinsicWidth(){return side;}@Override public int getIntrinsicHeight(){return side;}
        public void draw(Canvas canvas){}public void setAlpha(int value){}public void setColorFilter(android.graphics.ColorFilter value){}public int getOpacity(){return -3;}
    }
    private static void glyphDimensions()throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        PhoneContainer owner=phone(2);owner.getResources().getDisplayMetrics().density=4f;
        StatusBarIconView first=(StatusBarIconView)owner.getChildAt(0),second=(StatusBarIconView)owner.getChildAt(1);
        first.getResources().getDisplayMetrics().density=second.getResources().getDisplayMetrics().density=4f;
        first.intrinsicHeight=512f;first.nativeScale=.5f;first.setImageDrawable(new Glyph(512));
        android.graphics.Matrix fit=new android.graphics.Matrix();fit.setScale(.125f,.125f);first.setImageMatrix(fit);
        second.intrinsicHeight=64f;second.nativeScale=.75f;second.setImageDrawable(new Glyph(64));
        Bundle options=countOptions(true,3);options.putBoolean(NotificationIconArea.SIZE_ENABLED,true);options.putFloat(NotificationIconArea.SIZE,14.89f);
        options.putBoolean(NotificationIconArea.POSITION,true);options.putFloat(NotificationIconArea.X,4.86f);options.putFloat(NotificationIconArea.Y,1f);
        area.changed(owner);area.configure(null,options);
        Canvas row=new Canvas();area.draw(owner,row,()->null);near(1f,row.scaleX);near(19.44f,row.translateX);near(4f,row.translateY);
        int geometryReads=first.getterCalls,nativeInvalidations=first.invalidations;
        for(int i=0;i<1000;i++){
            Canvas image=new Canvas();area.drawNativeIcon(first,image,()->{image.scale(first.nativeScale,first.nativeScale);return area.drawNativeGlyph(first,image,()->null);});
            near(59.56f,512f*.125f*image.scaleY);equal(image.saves,image.restores);
            Canvas other=new Canvas();area.drawNativeIcon(second,other,()->{other.scale(second.nativeScale,second.nativeScale);return area.drawNativeGlyph(second,other,()->null);});
            near(59.56f,64f*other.scaleY);equal(other.saves,other.restores);
        }
        equal(geometryReads,first.getterCalls);equal(nativeInvalidations,first.invalidations);
        // Cached stable baselines never absorb our canvas scale or a native appear animation.
        near(.5f,first.nativeScale);near(.75f,second.nativeScale);first.iconAppearAmount=.01f;
        Canvas appearing=new Canvas();area.drawNativeIcon(first,appearing,()->{appearing.scale(first.nativeScale*first.iconAppearAmount,first.nativeScale*first.iconAppearAmount);return area.drawNativeGlyph(first,appearing,()->null);});
        near(.5956f,512f*.125f*appearing.scaleY);
        // Real updateDrawable/layout/native scale events recalculate the new ImageView matrix once.
        first.iconAppearAmount=1f;fit.setScale(.25f,.25f);first.setImageMatrix(fit);area.changed(first);
        area.draw(owner,new Canvas(),()->null);Canvas image=new Canvas();Canvas updated=image;
        area.drawNativeIcon(first,image,()->{updated.scale(first.nativeScale,first.nativeScale);return area.drawNativeGlyph(first,updated,()->null);});near(59.56f,512f*.25f*image.scaleY);
        first.visibleState=1;area.changed(first);area.draw(owner,new Canvas(),()->null);
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY); // Native overflow dot is untouched.
        first.visibleState=0;area.changed(first);area.draw(owner,new Canvas(),()->null);
        int invalidations=first.invalidations;
        options.putBoolean(NotificationIconArea.SIZE_ENABLED,false);area.configure(null,options);equal(true,first.invalidations>invalidations);area.draw(owner,new Canvas(),()->null);
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);
        options.putBoolean(NotificationIconArea.SIZE_ENABLED,true);options.putFloat(NotificationIconArea.SIZE,200f);area.configure(null,options);area.draw(owner,new Canvas(),()->null);
        Canvas larger=new Canvas();area.drawNativeIcon(first,larger,()->area.drawNativeGlyph(first,larger,()->null));near(800f,512f*.25f*.5f*larger.scaleY); // Above the suggested range is honored.
        invalidations=first.invalidations;options.putBoolean(NotificationIconArea.MASTER,false);area.configure(null,options);equal(true,first.invalidations>invalidations);
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);
        options.putBoolean(NotificationIconArea.MASTER,true);options.putBoolean(StatusBarSettings.SAFE_MODE,true);area.configure(null,options);
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);
        options.putBoolean(StatusBarSettings.SAFE_MODE,false);area.configure(null,options);area.draw(owner,new Canvas(),()->null);
        new NativeHost(404).addView(owner);
        owner.setMaxIconsAmount(area.nativeMaxIcons(owner,7)); // This setter can drop owner state before changed()/detach arrives.
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);equal(0,((Map<?,?>)field(area,"nativeSizes")).size());
        equal(0,((Map<?,?>)field(area,"states")).size());
        new NativeHost(202).addView(owner);area.changed(owner);area.draw(owner,new Canvas(),()->null);
        invalidations=first.invalidations;area.releaseRuntime();equal(true,first.invalidations>invalidations);
        image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);equal(0,((Map<?,?>)field(area,"nativeSizes")).size());
        area.configure(null,options);image=new Canvas();area.drawNativeIcon(first,image,()->null);near(1f,image.scaleY);
        equal(true,field(area,"nativeSizes") instanceof WeakHashMap);
    }
    private static final class MatrixCanvas extends Canvas {
        final float[] sx=new float[64],sy=new float[64];
        @Override public int save(){int n=super.save();sx[n]=scaleX;sy[n]=scaleY;return n;}
        @Override public void restoreToCount(int n){super.restoreToCount(n);scaleX=sx[n];scaleY=sy[n];}
    }
    private static void glyphOnlyScope()throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());PhoneContainer owner=phone(1);
        StatusBarIconView icon=(StatusBarIconView)owner.getChildAt(0);icon.intrinsicHeight=96f;icon.nativeScale=.5f;icon.iconAppearAmount=.7f;
        Bundle options=countOptions(true,1);options.putBoolean(NotificationIconArea.SIZE_ENABLED,true);options.putFloat(NotificationIconArea.SIZE,20f);
        area.changed(owner);area.configure(null,options);area.draw(owner,new Canvas(),()->null);
        MatrixCanvas canvas=new MatrixCanvas();float[] actual={0,0};
        area.drawNativeIcon(icon,canvas,()->{
            int nativeSave=canvas.save();canvas.scale(icon.nativeScale*icon.iconAppearAmount,icon.nativeScale*icon.iconAppearAmount);
            area.drawNativeGlyph(icon,canvas,()->{actual[0]=96f*canvas.scaleY;return null;});canvas.restoreToCount(nativeSave);
            actual[1]=6f*canvas.scaleY;return null; // StatusBarIconView draws transitional dot AFTER the ImageView branch restores.
        });
        near(14f,actual[0]);near(6f,actual[1]);equal(canvas.saves,canvas.restores);
        Canvas outside=new Canvas();area.drawNativeGlyph(icon,outside,()->null);near(1f,outside.scaleY);
        try{area.drawNativeIcon(icon,new Canvas(),()->{throw new IllegalStateException("native draw");});throw new AssertionError("failure lost");}
        catch(IllegalStateException expected){checks++;}
        area.drawNativeGlyph(icon,outside,()->null);near(1f,outside.scaleY);
    }
    /** The captured startup frame has real notification identities and drawable pixels,
     * but its leaf has not received native measurement/layout yet. */
    private static final class LayoutIcon extends StatusBarIconView {
        int width,height;
        LayoutIcon(){super(null);}
        @Override public int getWidth(){return width;}
        @Override public int getHeight(){return height;}
        @Override protected void onMeasure(int width,int height){
            setMeasuredDimension(View.MeasureSpec.getSize(width),View.MeasureSpec.getSize(height));
        }
        void nativeGeometry(int width,int height){
            this.width=width;this.height=height;
            measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            layout(0,0,width,height);
        }
    }
    private static void unmeasuredNativeGlyph()throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        PhoneContainer owner=phone(0);LayoutIcon icon=new LayoutIcon();owner.addView(icon);
        icon.mBundleEntry=new Object();icon.nativeScale=.5f;icon.setImageDrawable(new Glyph(64));icon.nativeGeometry(0,0);
        Bundle options=countOptions(true,1);options.putBoolean(NotificationIconArea.SIZE_ENABLED,true);
        options.putFloat(NotificationIconArea.SIZE,20f);options.putBoolean(NotificationIconArea.COLOR_ENABLED,true);
        options.putInt("notification_icons_color_light",0xff00ff00);
        area.changed(owner);area.configure(null,options);area.draw(owner,new Canvas(),()->null);
        NotificationIconOverridesCheck.NativeCanvas canvas=new NotificationIconOverridesCheck.NativeCanvas();
        NotificationIconOverridesCheck.nativeIcon(area,icon,canvas);
        near(32f,canvas.glyphHeight);near(6f,canvas.dotSize);equal(0,canvas.layers);
        equal(0,canvas.bitmapDraws);equal(true,canvas.text==null);equal(canvas.saves,canvas.restores);
        icon.nativeGeometry(80,120);
        canvas=new NotificationIconOverridesCheck.NativeCanvas();NotificationIconOverridesCheck.nativeIcon(area,icon,canvas);
        near(20f,canvas.glyphHeight);near(6f,canvas.dotSize);equal(1,canvas.layers);
        area.releaseRuntime();
    }
    private static final class LayoutOwner extends PhoneContainer {
        int width=124,height=120;
        @Override public int getWidth(){return width;}
        @Override public int getHeight(){return height;}
        @Override protected void onMeasure(int width,int height){
            setMeasuredDimension(View.MeasureSpec.getSize(width),View.MeasureSpec.getSize(height));
        }
        void nativeGeometry(int width,int height){
            this.width=width;this.height=height;
            measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            layout(0,20,width,20+height);
        }
    }
    private static void unmeasuredRows()throws Throwable{
        for(String mode:new String[]{"native","heart","text","image"}){
            NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
            LayoutOwner owner=new LayoutOwner();new NativeHost(202).addView(owner);owner.nativeGeometry(124,120);
            LayoutIcon icon=new LayoutIcon();icon.mBundleEntry=new Object();icon.setImageDrawable(new Glyph(64));
            icon.nativeGeometry(80,120);owner.addView(icon);
            Bundle options=countOptions(true,1);options.putString(NotificationIconArea.MODE,mode);
            options.putString(NotificationIconArea.TEXT,"通知");options.putBoolean(NotificationIconArea.POSITION,true);
            options.putFloat(NotificationIconArea.X,6f);options.putFloat(NotificationIconArea.Y,8f);
            area.changed(owner);area.configure(null,options);
            // A normalized image has already arrived from the repository/cache; only native
            // view geometry changes below, through the same public draw entry point as SystemUI.
            if("image".equals(mode)){
                Field bitmap=NotificationIconArea.class.getDeclaredField("bitmap");bitmap.setAccessible(true);
                bitmap.set(area,android.graphics.Bitmap.createBitmap(128,64,android.graphics.Bitmap.Config.ARGB_8888));
            }
            area.draw(owner,new Canvas(),()->null);
            for(int phase=0;phase<5;phase++){
                owner.nativeGeometry(phase==0?0:124,phase==1?0:120);
                icon.nativeGeometry(phase==2?0:80,phase==3?0:120);
                if(phase==4)icon.measure(View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.EXACTLY));
                int nativeCount=owner.mMaxIcons;
                Canvas canvas=new Canvas();int[] calls={0};
                Object result=area.draw(owner,canvas,()->{calls[0]++;return "native-frame";});
                equal("native-frame",result);equal(1,calls[0]);equal(true,canvas.text==null);equal(0,canvas.bitmapDraws);
                near(0f,canvas.translateX);near(0f,canvas.translateY);near(1f,canvas.scaleX);equal(0,canvas.layers);
                equal(nativeCount,owner.mMaxIcons);
                owner.nativeGeometry(124,120);icon.nativeGeometry(80,120);
                canvas=new Canvas();calls[0]=0;
                area.draw(owner,canvas,()->{calls[0]++;return null;});
                near(6f,canvas.translateX);near(8f,canvas.translateY);
                equal("native".equals(mode)?1:0,calls[0]);
                if("heart".equals(mode))equal("♥",canvas.text);
                if("text".equals(mode))equal("通知",canvas.text);
                if("image".equals(mode)){
                    equal(1,canvas.bitmapDraws);
                    near(60f,(canvas.bitmapLog.get(0).dest.top+canvas.bitmapLog.get(0).dest.bottom)/2f);
                }
                equal(canvas.saves,canvas.restores);
            }
            area.releaseRuntime();
        }
    }
    private static void unmeasuredGlyphSource(String source)throws Throwable{
        NotificationIconArea area=new NotificationIconArea();area.resolve(NotificationIconAreaCheck.class.getClassLoader());
        LayoutOwner owner=new LayoutOwner();new NativeHost(202).addView(owner);owner.nativeGeometry(124,120);
        LayoutIcon icon=new LayoutIcon();owner.addView(icon);
        StatusBarNotification notification=new StatusBarNotification(1);notification.packageName="com.source";
        icon.setNotification(notification);icon.setImageDrawable(new Glyph(64));icon.nativeScale=.5f;icon.nativeGeometry(0,0);
        ArrayDeque<Runnable> work=new ArrayDeque<>(),done=new ArrayDeque<>();
        android.graphics.Bitmap artwork=android.graphics.Bitmap.createBitmap(128,64,android.graphics.Bitmap.Config.ARGB_8888);
        boolean launcher="launcher".equals(source);
        NotificationAppIconRuntime cache=new NotificationAppIconRuntime(work::add,done::add,(context,pkg)->artwork,
                LauncherNotificationIconsCheck.changed(area),launcher);
        LauncherNotificationIconsCheck.install(area,launcher?"launcherIcons":"applicationIcons",cache);
        Bundle options=countOptions(launcher||"native".equals(source),1);
        options.putBoolean(NotificationIconArea.SIZE_ENABLED,"native".equals(source));
        options.putFloat(NotificationIconArea.SIZE,20f);
        options.putBoolean(NotificationIconArea.COLOR_ENABLED,true);
        options.putInt("notification_icons_color_light",0xff00ff00);
        if(launcher)options.putBoolean(NotificationIconArea.FOLLOW_LAUNCHER,true);
        else if(!"native".equals(source)){
            options.putBoolean(NotificationIconOverrides.MASTER,true);
            options.putString(NotificationIconOverrides.RULES,"text".equals(source)
                    ?NotificationIconOverrides.put("[]","com.source","X",true)
                    :NotificationIconOverrides.putAppIcon("[]","com.source","com.artwork",true));
        }
        area.changed(owner);area.configure(new android.content.Context(),options);
        area.draw(owner,new Canvas(),()->null);android.os.Handler.drainForCheck();
        LauncherNotificationIconsCheck.drain(work);LauncherNotificationIconsCheck.drain(done);
        area.draw(owner,new Canvas(),()->null);
        for(int phase=0;phase<5;phase++){
            icon.nativeGeometry(phase==1?80:0,phase==2?120:0);
            if(phase>=3){
                icon.nativeGeometry(80,120);
                icon.measure(View.MeasureSpec.makeMeasureSpec(phase==3?0:80,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(phase==4?0:120,View.MeasureSpec.EXACTLY));
            }
            NotificationIconOverridesCheck.NativeCanvas canvas=new NotificationIconOverridesCheck.NativeCanvas();
            NotificationIconOverridesCheck.nativeIcon(area,icon,canvas);
            equal(0,canvas.bitmapDraws);equal(true,canvas.text==null);equal(0,canvas.layers);
            near(32f,canvas.glyphHeight);near(6f,canvas.dotSize);equal(canvas.saves,canvas.restores);
            icon.nativeGeometry(80,120);
            canvas=new NotificationIconOverridesCheck.NativeCanvas();NotificationIconOverridesCheck.nativeIcon(area,icon,canvas);
            if("text".equals(source)){equal("X",canvas.text);near(0f,canvas.glyphHeight);}
            else if("native".equals(source)){near(20f,canvas.glyphHeight);equal(1,canvas.layers);}
            else{equal(1,canvas.bitmapDraws);equal(artwork,canvas.drawnBitmapsForCheck.get(0));near(0f,canvas.glyphHeight);}
            near(6f,canvas.dotSize);equal(canvas.saves,canvas.restores);
        }
        owner.nativeGeometry(0,120);
        NotificationIconOverridesCheck.NativeCanvas collapsed=new NotificationIconOverridesCheck.NativeCanvas();
        NotificationIconOverridesCheck.nativeIcon(area,icon,collapsed);
        equal(0,collapsed.bitmapDraws);equal(true,collapsed.text==null);equal(0,collapsed.layers);
        near(32f,collapsed.glyphHeight);near(6f,collapsed.dotSize);
        owner.nativeGeometry(124,120);
        // Native onDraw can itself invalidate geometry before reaching ImageView.onDraw.
        // The last glyph seam must also decline customization, preserving the following dot.
        NotificationIconOverridesCheck.NativeCanvas interrupted=new NotificationIconOverridesCheck.NativeCanvas();
        area.drawNativeIcon(icon,interrupted,()->{
            int saved=interrupted.save();interrupted.scale(icon.nativeScale,icon.nativeScale);
            icon.nativeGeometry(0,0);
            area.drawNativeGlyph(icon,interrupted,()->{interrupted.glyphHeight=64f*interrupted.scaleY;return null;});
            interrupted.restoreToCount(saved);interrupted.dotSize=6f*interrupted.scaleY;return null;
        });
        equal(0,interrupted.bitmapDraws);equal(true,interrupted.text==null);equal(0,interrupted.layers);
        near(32f,interrupted.glyphHeight);near(6f,interrupted.dotSize);equal(interrupted.saves,interrupted.restores);
        area.releaseRuntime();
    }
    public static void main(String[] args)throws Throwable{
        if(args.length>0){unmeasuredGlyphSource(args[0]);System.out.println(checks+" zero-layout "+args[0]+" checks passed");return;}
        unmeasuredRows();
        unmeasuredNativeGlyph();
        for(String source:new String[]{"launcher","text","app","native"})unmeasuredGlyphSource(source);
        NotificationIconSpacingCheck.run();
        nativeContracts();
        firstEnableDrawing();
        nativeCounts();
        glyphDimensions();
        glyphOnlyScope();
        equal(false,NotificationIconArea.BOOLEANS.get(NotificationIconArea.MASTER));
        Bundle settings=SettingsSnapshot.fromPreferences(Collections.emptyMap());
        equal(true,SettingsSnapshot.complete(settings));equal(false,NotificationIconArea.active(settings));
        settings.putBoolean(NotificationIconArea.MASTER,true);equal(true,NotificationIconArea.active(settings));
        settings.putBoolean(StatusBarSettings.SAFE_MODE,true);equal(false,NotificationIconArea.active(settings));
        equal(false,SafetyMode.runtimeSettings(settings).get(NotificationIconArea.MASTER));equal(true,settings.get(NotificationIconArea.MASTER));
        for(String mode:new String[]{"native","heart","text","image"}){
            equal(mode,NotificationIconArea.mode(mode));
            Map<String,Object> input=new HashMap<>();input.put(NotificationIconArea.MODE,mode);
            equal(mode,ConfigTransfer.prepare(ConfigTransfer.exportJson(input),true).values().get(NotificationIconArea.MODE));
        }
        equal("native",NotificationIconArea.mode("unknown"));equal("native",NotificationIconArea.mode(null));
        equal("♥",NotificationIconArea.customText(""));equal("♥",NotificationIconArea.customText(null));
        String unicode="💙💙💙💙💙💙💙💙💙💙💙💙";equal(unicode,NotificationIconArea.customText(unicode+"x"));equal(unicode,NotificationIconArea.customText(unicode));
        equal(4000f,NotificationIconArea.safeSize(1000f,4f));equal(0f,NotificationIconArea.safeSize(-100f,4f));equal(80f,NotificationIconArea.safeSize(Float.NaN,4f));
        equal(0f,NotificationIconArea.safeSize(0f,4f));equal(NumericPolicy.MAX_DRAW_PIXELS,NotificationIconArea.safeSize(Float.MAX_VALUE,4f));
        Map<String,Object> metadata=new HashMap<>();metadata.put(NotificationIconArea.IMAGE_NAME,"private.png");metadata.put(NotificationIconArea.IMAGE_REVISION,"local");
        equal(false,ConfigTransfer.exportJson(metadata).contains("private.png"));equal(false,ConfigTransfer.exportJson(metadata).contains("local"));
        for(String bad:new String[]{"1234567890123","a\nb","a\u0000b"}){
            org.json.JSONObject document=new org.json.JSONObject();document.put("package",ConfigTransfer.PACKAGE_NAME);document.put("schema",1);
            document.put("settings",new org.json.JSONObject().put(NotificationIconArea.TEXT,bad));
            boolean rejected=false;try{ConfigTransfer.prepare(document.toString(),true);}catch(java.io.IOException expected){rejected=true;}equal(true,rejected);
        }
        System.out.println(checks+" checks passed (first-enable redraw, native binder ownership, mono tint, private getter/scale, modes, Unicode, safety and local image metadata)");
    }
}
