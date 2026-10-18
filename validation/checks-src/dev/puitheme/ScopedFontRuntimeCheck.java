package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.compose.ui.Modifier;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import kotlin.jvm.functions.Function1;

/** Executes the real ClockView, TextView badge and Compose-family caches across provider failure and recovery. */
public final class ScopedFontRuntimeCheck {
    private static int checks;
    private static final String MISSING="e".repeat(64);
    private static void equal(Object expected,Object actual,String why){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Scoped runtime "+checks+" "+why+": "+expected+" != "+actual);}
    private static void check(boolean value,String why){equal(true,value,why);}
    private static Field field(Class<?> type,String name)throws Exception{Field result=type.getDeclaredField(name);result.setAccessible(true);return result;}
    private static Object get(Object owner,String name)throws Exception{return field(owner.getClass(),name).get(owner);}
    private static void set(Object owner,String name,Object value)throws Exception{field(owner.getClass(),name).set(owner,value);}
    private static Method method(Class<?> type,String name,Class<?>... arguments)throws Exception{Method result=type.getDeclaredMethod(name,arguments);result.setAccessible(true);return result;}
    private static boolean custom(Typeface face){return face!=null&&"custom".equals(face.sourceForCheck);}
    private static final class ProviderContext extends Context implements AutoCloseable{
        final Path directory=Files.createTempDirectory("c17-scoped-runtime-");final String hash;
        int opens;boolean available;
        ProviderContext()throws Exception{
            Files.createDirectories(directory.resolve("fonts"));Path staged=Files.createTempFile(directory.resolve("fonts"),"stage-",".font");
            Files.writeString(staged,"\u0000\u0001\u0000\u0000actual-library-font");hash=FontFileValidator.sha256(staged.toFile(),1024,null);
            try(FontRepository.PreparedFont source=new FontRepository.PreparedFont(staged.toFile(),directory.resolve("fonts/custom.font").toFile(),hash)){FontLibrary.remember(this,source,"测试字体");}
        }
        @Override public File getFilesDir(){return directory.toFile();}
        @Override public ContentResolver getContentResolver(){return new ContentResolver(){
            @Override public ParcelFileDescriptor openFileDescriptor(Uri uri,String mode)throws FileNotFoundException{
                opens++;if(!available)throw new FileNotFoundException("provider temporarily unavailable");
                try{return ParcelFileDescriptor.open(FontLibrary.providerFile(ProviderContext.this,uri),ParcelFileDescriptor.MODE_READ_ONLY);}
                catch(IOException unavailable){throw new FileNotFoundException(unavailable.getMessage());}
            }
        };}
        Bundle settings(String target,String other){
            Bundle result=new Bundle();result.putBoolean("font_enabled",true);result.putString(StatusBarSettings.FONT_MODE,"system");
            String assignments=FontScopes.merge("{}",Arrays.asList(target),FontScopes.source("custom",hash));
            result.putString(FontScopes.KEY,FontScopes.merge(assignments,Arrays.asList(other),FontScopes.source("custom",MISSING)));return result;
        }
        void configure(Bundle settings){FontRepository.releaseRuntime();FontRepository.configure(this,"system","");FontRepository.configureScopes(settings);}
        void failOther(String scope){FontRepository.typefaceForMode(this,"native",Typeface.DEFAULT,400,scope);SystemClock.uptime+=5001;check(FontRepository.retryDue(scope),"other scope has a real overdue failed source");}
        public void close()throws Exception{FontRepository.releaseRuntime();try(var files=Files.walk(directory)){for(Path path:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    private static void clock()throws Exception{
        try(ProviderContext context=new ProviderContext()){
            Bundle values=context.settings(FontScopes.NOTIFICATION_CLOCK,FontScopes.NETWORK_TYPE);context.configure(values);
            values.putBoolean(NotificationBigClockSettings.MASTER,true);values.putFloat(NotificationBigClockSettings.MAX_SIZE,32f);
            values.putFloat(NotificationBigClockSettings.COMPACT_MAX_SIZE,24f);values.putFloat(NotificationBigClockSettings.DATE_SIZE,12f);values.putFloat(NotificationBigClockSettings.FOOTER_SIZE,12f);
            NotificationBigClock owner=new NotificationBigClock();FrameLayout panel=new FrameLayout(context);panel.useLayoutBoundsForCheck=true;panel.layout(0,0,400,1280);
            Class<?> settingsType=Class.forName("dev.puitheme.NotificationBigClock$Settings");Constructor<?> settings=settingsType.getDeclaredConstructor(Bundle.class,String.class);settings.setAccessible(true);
            set(owner,"settings",settings.newInstance(values,NotificationBigClockSettings.PORTRAIT_PREFIX));set(owner,"panel",new WeakReference<View>(panel));
            Class<?> viewType=Class.forName("dev.puitheme.NotificationBigClock$ClockView");Constructor<?> ctor=viewType.getDeclaredConstructor(NotificationBigClock.class,Context.class);ctor.setAccessible(true);
            FrameLayout overlay=(FrameLayout)ctor.newInstance(owner,context);overlay.useLayoutBoundsForCheck=true;panel.addView(overlay);overlay.layout(0,0,400,1280);set(owner,"overlay",overlay);
            Method prepare=method(viewType,"prepareMeasurements");TextView probe=(TextView)get(overlay,"timeProbe"),date=(TextView)get(overlay,"dateLabel"),footer=(TextView)get(overlay,"footerLabel");
            prepare.invoke(overlay);check(context.opens>0,"actual ClockView attempted its configured provider source");check(!custom(probe.getTypeface()),"unavailable provider uses the native time family");
            int attempts=context.opens,measurements=(Integer)get(overlay,"measurementRevision");
            for(int i=0;i<20;i++)prepare.invoke(overlay);
            equal(attempts,context.opens,"ClockView suppresses provider retries before the source deadline");equal(measurements,get(overlay,"measurementRevision"),"native fallback measurement is stable until retry");
            context.available=true;SystemClock.uptime+=4999;prepare.invoke(overlay);equal(attempts,context.opens,"provider returning early does not bypass retry throttle");
            SystemClock.uptime+=2;prepare.invoke(overlay);check(custom(probe.getTypeface()),"prepareMeasurements refreshes its previously cached native time family");
            check(custom(date.getTypeface())&&custom(footer.getTypeface()),"real textStyle refreshes date and footer typography after recovery");check((Integer)get(overlay,"measurementRevision")>measurements,"source recovery causes a new measurement");
            prepare.invoke(overlay);prepare.invoke(overlay);attempts=context.opens;measurements=(Integer)get(overlay,"measurementRevision");int writes=probe.typefaceWrites+date.typefaceWrites+footer.typefaceWrites;
            Object typography=((Map<?,?>)get(overlay,"typography")).get(probe);
            for(int i=0;i<50;i++)prepare.invoke(overlay);
            equal(attempts,context.opens,"recovered ClockView does not reopen the provider on stable frames");equal(measurements,get(overlay,"measurementRevision"),"recovered ClockView preserves its measurement cache");
            equal(writes,probe.typefaceWrites+date.typefaceWrites+footer.typefaceWrites,"stable textStyle performs no repeated typeface writes");equal(typography,((Map<?,?>)get(overlay,"typography")).get(probe),"stable real typography record is retained");
            context.failOther(FontScopes.NETWORK_TYPE);attempts=context.opens;check(!FontRepository.retryDue(FontScopes.NOTIFICATION_CLOCK),"clock source is independent of a failed network source");
            for(int i=0;i<25;i++)prepare.invoke(overlay);
            equal(attempts,context.opens,"unseen network retry cannot trigger clock provider loads");equal(measurements,get(overlay,"measurementRevision"),"unseen network retry cannot invalidate clock measurements");
            equal(typography,((Map<?,?>)get(overlay,"typography")).get(probe),"unseen network retry cannot clear real clock typography");
            method(viewType,"clearMaterials").invoke(overlay);
        }
    }
    private static ReplacementSpan span(TextView view){Spanned text=(Spanned)view.getText();return text.getSpans(0,text.length(),ReplacementSpan.class)[0];}
    private static Typeface draw(TextView view){
        Spanned text=(Spanned)view.getText();ReplacementSpan selected=span(view);TextPaint paint=new TextPaint();paint.set(view.getPaint());Canvas canvas=new Canvas();
        selected.draw(canvas,text,text.getSpanStart(selected),text.getSpanEnd(selected),0,0,20,30,paint);return canvas.textTypeface;
    }
    private static void textViewBadge()throws Exception{
        try(ProviderContext context=new ProviderContext()){
            Bundle values=context.settings(FontScopes.NETWORK_TYPE,FontScopes.NOTIFICATION_CLOCK);context.configure(values);values.putBoolean(NativeNetworkBadgeControls.MASTER,true);
            NativeNetworkBadgeControls controls=new NativeNetworkBadgeControls();controls.configure(values);
            TextView view=new TextView(context);Typeface original=new Typeface();original.sourceForCheck="native-badge";view.setTypeface(original);view.setText("5G+");controls.onTextViewNativeUpdated(view,"5G","+",1);
            check(context.opens>0,"real TextView binding attempts the configured provider");check(!custom(draw(view)),"real ReplacementSpan draws the native fallback while provider is unavailable");
            CharSequence fallback=view.getText();int attempts=context.opens;
            for(int i=0;i<20;i++)controls.refreshTextViews();equal(attempts,context.opens,"TextView binding throttles provider failure");equal(fallback,view.getText(),"TextView reuses its native fallback spans before retry");
            context.available=true;SystemClock.uptime+=5001;controls.refreshTextViews();check(custom(draw(view)),"real TextView ReplacementSpan recovers the selected font at its deadline");
            controls.refreshTextViews();CharSequence recovered=view.getText();ReplacementSpan selected=span(view);attempts=context.opens;
            for(int i=0;i<50;i++)controls.refreshTextViews();equal(attempts,context.opens,"recovered badge does not reopen provider");equal(recovered,view.getText(),"recovered TextView preserves its span text identity");equal(selected,span(view),"recovered TextView preserves its ReplacementSpan identity");
            context.failOther(FontScopes.NOTIFICATION_CLOCK);attempts=context.opens;
            for(int i=0;i<25;i++)controls.refreshTextViews();equal(attempts,context.opens,"unseen clock retry cannot reload a stable badge");equal(recovered,view.getText(),"unseen clock retry cannot recreate badge spans");
            controls.releaseRuntime();equal(original,view.getTypeface(),"release preserves the real native TextView typeface");
        }
    }
    /** Only the OEM reflection bridge is supplied here. FontRepository and NativeAccess.family remain production objects. */
    public static final class LayerFactory{public static Modifier graphicsLayer(Modifier modifier,Function1 action){return modifier;}}
    public static final class BridgeUnit{public static final BridgeUnit INSTANCE=new BridgeUnit();}
    public static final class LayerScope{public void setTranslationX(float x){}public void setTranslationY(float y){}}
    public static final class Weight{public final int weight;public Weight(int value){weight=value;}}
    public static final class LoadedFamily{final Wrapper value;public LoadedFamily(Wrapper value){this.value=value;}public Object getTypeface(){return value;}}
    public static final class Wrapper{final Typeface value;public Wrapper(Typeface value){this.value=value;}public Typeface getTypeface(){return value;}}
    public static final class FamilyFactory{static int wraps;public static Object FontFamily(Typeface value){wraps++;return new LoadedFamily(new Wrapper(value));}}
    private static final class BridgeLoader extends ClassLoader{
        BridgeLoader(){super(ScopedFontRuntimeCheck.class.getClassLoader());}
        @Override public Class<?> loadClass(String name)throws ClassNotFoundException{
            switch(name){
                case "kotlin.Unit":return BridgeUnit.class;
                case "androidx.compose.ui.graphics.GraphicsLayerModifierKt":return LayerFactory.class;
                case "androidx.compose.ui.graphics.GraphicsLayerScope":return LayerScope.class;
                case "androidx.compose.ui.text.font.FontWeight":return Weight.class;
                case "androidx.compose.ui.text.font.LoadedFontFamily":return LoadedFamily.class;
                case "androidx.compose.ui.text.platform.AndroidTypefaceWrapper":return Wrapper.class;
                case "androidx.compose.ui.text.font.AndroidTypeface_androidKt":return FamilyFactory.class;
                default:return super.loadClass(name);
            }
        }
    }
    private static Typeface familyFace(Object family){return ((LoadedFamily)family).value.value;}
    private static void composeFamily()throws Exception{
        try(ProviderContext context=new ProviderContext()){
            Bundle values=context.settings(FontScopes.NETWORK_TYPE,FontScopes.NOTIFICATION_CLOCK);context.configure(values);
            NativeNetworkBadgeControls controls=new NativeNetworkBadgeControls();Class<?> accessType=Class.forName("dev.puitheme.NativeNetworkBadgeControls$NativeAccess");
            Constructor<?> ctor=accessType.getDeclaredConstructor(NativeNetworkBadgeControls.class,ClassLoader.class);ctor.setAccessible(true);Object access=ctor.newInstance(controls,new BridgeLoader());
            Method family=method(accessType,"family",Context.class,Object.class,String.class,String.class,int.class);
            Typeface nativeFace=new Typeface();nativeFace.sourceForCheck="native-compose";Object original=new LoadedFamily(new Wrapper(nativeFace));
            Object fallback=family.invoke(access,context,original,"custom","same-source",500);check(!custom(familyFace(fallback)),"real NativeAccess.family wraps the native fallback after failure");
            int attempts=context.opens,wraps=FamilyFactory.wraps;
            for(int i=0;i<20;i++)equal(fallback,family.invoke(access,context,original,"custom","same-source",500),"before deadline the real family cache retains fallback identity");
            equal(attempts,context.opens,"Compose family failure is throttled");equal(wraps,FamilyFactory.wraps,"Compose family fallback is not repeatedly wrapped");
            context.available=true;SystemClock.uptime+=5001;Object recovered=family.invoke(access,context,original,"custom","same-source",500);
            check(custom(familyFace(recovered)),"real NativeAccess.family does not permanently cache native fallback after the provider recovers");
            recovered=family.invoke(access,context,original,"custom","same-source",500);attempts=context.opens;wraps=FamilyFactory.wraps;
            for(int i=0;i<50;i++)equal(recovered,family.invoke(access,context,original,"custom","same-source",500),"recovered family cache retains identity");
            equal(attempts,context.opens,"recovered Compose family performs no provider I/O");equal(wraps,FamilyFactory.wraps,"recovered Compose family performs no wrapper rebuilds");
            context.failOther(FontScopes.NOTIFICATION_CLOCK);attempts=context.opens;
            equal(recovered,family.invoke(access,context,original,"custom","same-source",500),"an overdue clock source cannot invalidate the real network family cache");equal(attempts,context.opens,"unrelated retry performs no family provider I/O");
            controls.releaseRuntime();
        }
    }
    public static void main(String[] args)throws Exception{
        long uptime=SystemClock.uptime;try{
            if(args.length==0){clock();textViewBadge();composeFamily();}
            else if("clock".equals(args[0]))clock();else if("textview".equals(args[0]))textViewBadge();else if("compose".equals(args[0]))composeFamily();
            else throw new IllegalArgumentException("Unknown font runtime scenario");
        }finally{SystemClock.uptime=uptime;FontRepository.releaseRuntime();}
        System.out.println("ScopedFontRuntimeCheck passed: "+checks);
    }
}
