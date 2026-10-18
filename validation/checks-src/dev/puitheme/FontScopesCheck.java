package dev.puitheme;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.TextView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Real independent library files and provider descriptors; scope parsing never changes a legacy source. */
public final class FontScopesCheck {
    private static int checks;
    private static final String A="a".repeat(64),B="b".repeat(64);
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Font scopes "+checks+": "+expected+" != "+actual);}
    private static void reject(Runnable task){checks++;try{task.run();throw new AssertionError("Accepted invalid scope selection");}catch(IllegalArgumentException expected){}}
    private static void model(){
        equal(7,FontScopes.SCOPES.length);equal(Collections.emptyMap(),FontScopes.parse(" { \n } "));
        equal("custom:"+A,FontScopes.source("custom",A));equal("",FontScopes.revision("system"));equal("custom",FontScopes.mode("custom:"+A));
        Map<String,String> selected=new LinkedHashMap<>();for(String scope:FontScopes.SCOPES){equal(true,!FontScopes.title(scope).isEmpty());selected.put(scope,"pingfang");}
        String all=FontScopes.merge("{}",Arrays.asList(FontScopes.SCOPES),"pingfang");equal(selected,FontScopes.parse(all));
        String subset=FontScopes.merge(all,Arrays.asList(FontScopes.STATUSBAR_CLOCK),"custom:"+A);
        equal("custom:"+A,FontScopes.parse(subset).get(FontScopes.STATUSBAR_CLOCK));equal("pingfang",FontScopes.parse(subset).get(FontScopes.NETWORK_TYPE));
        equal(all,FontScopes.encode(selected));equal(true,FontScopes.references(subset,A));equal(false,FontScopes.references(subset,B));
        equal("system",FontScopes.parse(FontScopes.removeFont(subset,A)).get(FontScopes.STATUSBAR_CLOCK));equal(subset,FontScopes.removeFont(subset,B));
        equal(subset,FontScopes.fallbackMissing(subset,hash->hash.equals(A)));
        equal("system",FontScopes.parse(FontScopes.fallbackMissing(subset,hash->false)).get(FontScopes.STATUSBAR_CLOCK));
        Map<String,Object> saved=new LinkedHashMap<>();saved.put(StatusBarSettings.FONT_MODE,"custom");saved.put(StatusBarSettings.FONT_REVISION,B);
        equal(null,FontScopes.assignment(saved,FontScopes.STATUSBAR_CLOCK));saved.put(FontScopes.KEY,subset);equal("custom:"+A,FontScopes.assignment(saved,FontScopes.STATUSBAR_CLOCK));
        Map<String,Object> before=new LinkedHashMap<>(saved);
        FontScopes.Application stopped=FontScopes.planApplication(subset,false,Arrays.asList(FontScopes.LOCKSCREEN_TEXT),"custom:"+B);
        equal(true,stopped.resetLegacySource);equal(Collections.singletonMap(FontScopes.LOCKSCREEN_TEXT,"custom:"+B),FontScopes.parse(stopped.assignments));
        FontScopes.Application active=FontScopes.planApplication(subset,true,Arrays.asList(FontScopes.LOCKSCREEN_TEXT),"custom:"+B);
        equal(false,active.resetLegacySource);equal("custom:"+A,FontScopes.parse(active.assignments).get(FontScopes.STATUSBAR_CLOCK));
        equal("pingfang",FontScopes.parse(active.assignments).get(FontScopes.NETWORK_TYPE));equal("custom:"+B,FontScopes.parse(active.assignments).get(FontScopes.LOCKSCREEN_TEXT));equal(before,saved);
        reject(()->FontScopes.planApplication(subset,false,Collections.emptyList(),"system"));reject(()->FontScopes.planApplication(subset,true,Arrays.asList("unknown"),"system"));equal(before,saved);
        for(String bad:new String[]{"", "[]", "null", "{\"unknown\":\"system\"}","{\"statusbar_clock\":\"custom\"}",
                "{\"statusbar_clock\":\"custom:../font\"}","{\"statusbar_clock\":\"system\",\"statusbar_clock\":\"pingfang\"}",
                "{\"statusbar_clock\":\"system\",}","{\"statusbar_clock\":null}","{} x","{\"statusbar_\\u0063lock\":\"system\"}"})reject(()->FontScopes.parse(bad));
        reject(()->FontScopes.merge("{}",Collections.emptyList(),"system"));reject(()->FontScopes.merge("{}",Arrays.asList("bad"),"system"));
        reject(()->FontScopes.source("custom","../"));reject(()->FontScopes.parse(" ".repeat(4097)));reject(()->FontScopes.title("unknown"));
    }
    private static final class LibraryContext extends Context{
        final Path directory;int opens;String lastPath;LibraryContext(Path directory){this.directory=directory;}
        @Override public File getFilesDir(){return directory.toFile();}
        @Override public ContentResolver getContentResolver(){return new ContentResolver(){
            @Override public ParcelFileDescriptor openFileDescriptor(Uri uri,String mode)throws FileNotFoundException{
                opens++;lastPath=uri.getPath();try{return ParcelFileDescriptor.open(FontLibrary.providerFile(LibraryContext.this,uri),ParcelFileDescriptor.MODE_READ_ONLY);}
                catch(IOException invalid){throw new FileNotFoundException(invalid.getMessage());}
            }
        };}
    }
    private static String remember(LibraryContext context,String contents)throws Exception{
        Path folder=context.directory.resolve("fonts");Files.createDirectories(folder);Path staged=Files.createTempFile(folder,"scoped-",".font");
        Files.writeString(staged,"\u0000\u0001\u0000\u0000"+contents);String hash=FontFileValidator.sha256(staged.toFile(),1024,null);
        try(FontRepository.PreparedFont prepared=new FontRepository.PreparedFont(staged.toFile(),folder.resolve("custom.font").toFile(),hash)){FontLibrary.remember(context,prepared,contents);}
        return hash;
    }
    private static void runtime()throws Exception{
        Path directory=Files.createTempDirectory("c17-font-scopes-");LibraryContext context=new LibraryContext(directory);ResidentCache cache=new ResidentCache();
        try{
            String a=remember(context,"first"),b=remember(context,"second");Path current=directory.resolve("fonts/custom.font");Files.writeString(current,"previous-provider-font");
            equal(true,FontLibrary.contains(context,a));equal(true,FontLibrary.contains(context,b));equal(false,FontLibrary.contains(context,A));
            equal(directory.resolve("fonts/library/"+a+".font").toFile(),FontLibrary.providerFile(context,FontRepository.libraryUri(a)));
            equal("/font/library/"+a,FontRepository.libraryUri(a).getPath());
            FontRepository.releaseRuntime();FontRepository.configure(context,"system","");
            Bundle settings=new Bundle();settings.putBoolean("font_enabled",true);
            String assignments=FontScopes.merge("{}",Arrays.asList(FontScopes.STATUSBAR_CLOCK),FontScopes.source("custom",a));
            assignments=FontScopes.merge(assignments,Arrays.asList(FontScopes.NETWORK_TYPE),FontScopes.source("custom",b));settings.putString(FontScopes.KEY,assignments);
            FontRepository.configureScopes(settings);equal(true,FontRepository.hasAssignment(FontScopes.STATUSBAR_CLOCK));equal(false,FontRepository.systemMode(FontScopes.STATUSBAR_CLOCK));
            equal("custom",FontRepository.modeForScope("native",FontScopes.STATUSBAR_CLOCK));equal(a,FontRepository.revisionForScope(FontScopes.STATUSBAR_CLOCK));
            equal("native",FontRepository.modeForScope("native",FontScopes.NOTIFICATION_CLOCK));
            Typeface nativeFace=new Typeface();nativeFace.sourceForCheck="native";
            Typeface first=FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK);equal("custom",first.sourceForCheck);equal("/font/library/"+a,context.lastPath);
            Typeface second=FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.NETWORK_TYPE);equal("custom",second.sourceForCheck);equal("/font/library/"+b,context.lastPath);equal(false,first==second);
            int opens=context.opens;long revision=FontRepository.styleRevision();
            for(int i=0;i<500;i++){equal(first,FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK));equal(second,FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.NETWORK_TYPE));}
            equal(opens,context.opens);equal(revision,FontRepository.styleRevision());equal("previous-provider-font",Files.readString(current));
            long uptime=android.os.SystemClock.uptime;
            settings.putString(FontScopes.KEY,FontScopes.merge(assignments,Arrays.asList(FontScopes.NETWORK_TYPE),"custom:"+A));FontRepository.configureScopes(settings);
            FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.NETWORK_TYPE);equal(false,FontRepository.retryDue(FontScopes.NETWORK_TYPE));
            android.os.SystemClock.uptime+=5001;equal(true,FontRepository.retryDue(FontScopes.NETWORK_TYPE));equal(false,FontRepository.retryDue(FontScopes.STATUSBAR_CLOCK));
            first=FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK);opens=context.opens;
            for(int i=0;i<50;i++)FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK);
            equal(opens,context.opens);equal(first,FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK));
            settings.putString(FontScopes.KEY,assignments);FontRepository.configureScopes(settings);android.os.SystemClock.uptime=uptime;
            settings.putBoolean("font_enabled",false);FontRepository.configureScopes(settings);equal(false,FontRepository.hasAssignment(FontScopes.STATUSBAR_CLOCK));equal(true,FontRepository.systemMode(FontScopes.STATUSBAR_CLOCK));
            equal(nativeFace.sourceForCheck,FontRepository.typefaceForMode(context,"native",nativeFace,500,FontScopes.STATUSBAR_CLOCK).sourceForCheck);
            settings.putBoolean("font_enabled",true);settings.putBoolean(StatusBarSettings.SAFE_MODE,true);FontRepository.configureScopes(settings);equal(false,FontRepository.hasAssignment(FontScopes.STATUSBAR_CLOCK));settings.putBoolean(StatusBarSettings.SAFE_MODE,false);
            settings.putString(FontScopes.KEY,FontScopes.merge(assignments,Arrays.asList(FontScopes.STATUSBAR_CLOCK),"system"));FontRepository.configureScopes(settings);
            TextView view=new TextView(context);Typeface axes=new Typeface();axes.variableWeight=true;view.setTypeface(axes);view.setFontVariationSettings("'wght' 640, 'wdth' 88");
            FontRepository.apply(view,axes,500,"'wght' 640, 'wdth' 88",FontScopes.STATUSBAR_CLOCK);equal(true,view.getFontVariationSettings().contains("wdth"));equal(true,FontRepository.systemMode(FontScopes.STATUSBAR_CLOCK));
            cache.initialize(context);FontRepository.resident=cache;cache.font(context,a,true);cache.font(context,b,true);
            Path cachedA=directory.resolve(ResidentCache.FOLDER+"/font-library-"+a+".font"),cachedB=directory.resolve(ResidentCache.FOLDER+"/font-library-"+b+".font");
            long until=System.nanoTime()+2_000_000_000L;while((!Files.isRegularFile(cachedA)||!Files.isRegularFile(cachedB))&&System.nanoTime()<until)Thread.sleep(10);
            equal(true,Files.isRegularFile(cachedA));equal(true,Files.isRegularFile(cachedB));equal("\u0000\u0001\u0000\u0000first",Files.readString(cachedA));equal("\u0000\u0001\u0000\u0000second",Files.readString(cachedB));
            equal(cachedA.toFile(),cache.font(context,a,true));equal(cachedB.toFile(),cache.font(context,b,true));
            Files.writeString(cachedA,"wrong-font-file");equal(null,cache.font(context,a,true));
            until=System.nanoTime()+2_000_000_000L;while(!Files.isRegularFile(cachedA)&&System.nanoTime()<until)Thread.sleep(10);
            equal("\u0000\u0001\u0000\u0000first",Files.readString(cachedA));equal("\u0000\u0001\u0000\u0000second",Files.readString(cachedB));
            equal("previous-provider-font",Files.readString(current));
        }finally{cache.release();FontRepository.resident=null;FontRepository.releaseRuntime();Thread.sleep(20);try(var files=Files.walk(directory)){for(Path path:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    }
    public static void main(String[] args)throws Exception{model();runtime();System.out.println("FontScopesCheck passed: "+checks);}
}
