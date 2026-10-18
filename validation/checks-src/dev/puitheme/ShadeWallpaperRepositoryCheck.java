package dev.puitheme;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class ShadeWallpaperRepositoryCheck {
    private static int checks;
    private static void equal(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("Wallpaper repository "+checks+": "+expected+" != "+actual);}
    private static void rejected(Context context,String uri)throws Exception{
        checks++;try{ShadeWallpaperRepository.providerFile(context,Uri.parse(uri));}catch(IOException expected){return;}throw new AssertionError("Unsafe wallpaper path accepted: "+uri);
    }
    private static final class UndeletableAsset extends File {
        int attempts;
        UndeletableAsset(String name){super(name);}
        @Override public boolean exists(){return true;}
        @Override public boolean isFile(){return true;}
        @Override public boolean delete(){attempts++;return false;}
    }
    private static File fixtureDirectory(File... files){
        return new File("wallpaper-delete-fixture"){
            @Override public boolean exists(){return true;}
            @Override public File[] listFiles(){return files;}
            @Override public File[] listFiles(FileFilter filter){return java.util.Arrays.stream(files).filter(filter::accept).toArray(File[]::new);}
        };
    }
    /** What the user cut out: turns are quarter turns, fractions are kept inside the picture, a sliver or a bad number is no crop. */
    private static void crops(){
        ShadeWallpaperRepository.Crop turned=new ShadeWallpaperRepository.Crop(-90,-1f,.2f,2f,Float.NaN);
        equal(270,turned.rotation);equal(0f,turned.left);equal(.2f,turned.top);equal(1f,turned.right);equal(0f,turned.bottom);
        equal(false,turned.valid());
        equal(90,new ShadeWallpaperRepository.Crop(450,0f,0f,1f,1f).rotation);
        equal(0,new ShadeWallpaperRepository.Crop(360,0f,0f,1f,1f).rotation);
        equal(180,new ShadeWallpaperRepository.Crop(185,0f,0f,1f,1f).rotation);
        equal(true,new ShadeWallpaperRepository.Crop(0,.1f,.2f,.9f,.8f).valid());
        equal(false,new ShadeWallpaperRepository.Crop(0,.5f,.5f,.505f,.9f).valid());
        // a picture to be cut is decoded larger than the result, and stays within its bounds either way
        int[] large=ShadeWallpaperRepository.boundedSize(6000,4000,3072,8L*1024*1024);
        equal(true,large[0]<=3072&&large[1]<=3072&&(long)large[0]*large[1]<=8L*1024*1024);
        equal(true,Math.abs(large[0]/(float)large[1]-1.5f)<.01f);
        int[] small=ShadeWallpaperRepository.boundedSize(800,600,3072,8L*1024*1024);
        equal(800,small[0]);equal(600,small[1]);
        int[] result=ShadeWallpaperRepository.normalizedSize(2400,5280);
        equal(true,result[0]<=ShadeWallpaperRepository.MAX_EDGE&&result[1]<=ShadeWallpaperRepository.MAX_EDGE&&(long)result[0]*result[1]<=ShadeWallpaperRepository.MAX_PIXELS);
        equal(true,Math.abs(result[0]/(float)result[1]-2400f/5280f)<.01f);
    }
    public static void main(String[] args)throws Exception{
        crops();
        Path privateRoot=Files.createTempDirectory("c17-wallpaper-check-");
        Context context=new Context(){@Override public File getFilesDir(){return privateRoot.toFile();}};
        String scene=ShadeWallpaperSettings.CLASSIC_PORTRAIT,hash="a".repeat(64);
        try {
            String base="content://"+StatusBarSettings.AUTHORITY+"/shade-wallpaper/";
            File target=ShadeWallpaperRepository.providerFile(context,ShadeWallpaperRepository.uri(scene,hash));
            equal(privateRoot.resolve("shade-wallpapers").resolve(scene).resolve(hash+".png").toFile().getCanonicalFile(),target);
            for(String uri:new String[]{base+"../fonts/custom.font",base+scene+"/../current",base+scene+"/%2e%2e",base+scene+"/%2f"+hash+".png",
                    base+scene+"/"+hash+".png/extra",base+"other/"+hash+".png",base+scene+"/"+hash+".jpg",
                    base+scene+"/"+hash+".png?read=1",base+scene+"/"+hash+".png#fragment",
                    "file:///shade-wallpaper/"+scene+"/"+hash+".png","content://other/shade-wallpaper/"+scene+"/"+hash+".png"})rejected(context,uri);
            for(int[] size:new int[][]{{100,50},{32768,32768},{32768,1},{1,32768},{8000,4000}}){
                int[] normalized=ShadeWallpaperRepository.normalizedSize(size[0],size[1]);
                equal(true,normalized[0]>0&&normalized[1]>0);equal(true,Math.max(normalized[0],normalized[1])<=ShadeWallpaperRepository.MAX_EDGE);
                equal(true,(long)normalized[0]*normalized[1]<=ShadeWallpaperRepository.MAX_PIXELS);
            }
            target.getParentFile().mkdirs();
            File previous=new File(target.getParentFile(),"b".repeat(64)+".png");Files.writeString(previous.toPath(),"previous image");
            AtomicInteger saves=new AtomicInteger(),restores=new AtomicInteger();
            File staged=File.createTempFile("pending-",".png",target.getParentFile());Files.writeString(staged.toPath(),"new image");
            ShadeWallpaperRepository.PreparedWallpaper prepared=new ShadeWallpaperRepository.PreparedWallpaper(staged,target,scene,hash);
            try {prepared.commit(()->true,()->{saves.incrementAndGet();return false;},restores::incrementAndGet);throw new AssertionError("Failed save published");}catch(IOException expected){checks++;}
            equal(1,saves.get());equal(1,restores.get());equal(false,target.exists());equal("previous image",Files.readString(previous.toPath()));equal(false,staged.exists());
            staged=File.createTempFile("pending-",".png",target.getParentFile());Files.writeString(staged.toPath(),"committed image");
            prepared=new ShadeWallpaperRepository.PreparedWallpaper(staged,target,scene,hash);
            equal(hash,prepared.commit(()->true,()->true,restores::incrementAndGet));equal("committed image",Files.readString(target.toPath()));equal(true,previous.exists());
            File duplicate=File.createTempFile("pending-",".png",target.getParentFile());Files.writeString(duplicate.toPath(),"same revision stage");
            prepared=new ShadeWallpaperRepository.PreparedWallpaper(duplicate,target,scene,hash);
            try {prepared.commit(()->true,()->false,restores::incrementAndGet);throw new AssertionError("Failed save accepted");}catch(IOException expected){checks++;}
            equal("committed image",Files.readString(target.toPath()));equal(false,duplicate.exists());
            File cancelled=File.createTempFile("pending-",".png",target.getParentFile());
            prepared=new ShadeWallpaperRepository.PreparedWallpaper(cancelled,target,scene,hash);
            try {prepared.commit(()->false,()->{throw new AssertionError("Cancelled save ran");},()->{});throw new AssertionError("Cancelled import accepted");}catch(IOException expected){checks++;}
            prepared.close();equal(false,cancelled.exists());equal("committed image",Files.readString(target.toPath()));
            for(char c='c';c<='f';c++)Files.writeString(new File(target.getParentFile(),String.valueOf(c).repeat(64)+".png").toPath(),"old image");
            ShadeWallpaperRepository.prune(context,scene,hash);equal(2,target.getParentFile().listFiles().length);equal(true,target.exists());
            ShadeWallpaperRepository.delete(context,scene);equal(0,target.getParentFile().listFiles().length);
            UndeletableAsset failed=new UndeletableAsset("c".repeat(64)+".png");
            try{ShadeWallpaperRepository.deleteFiles(fixtureDirectory(failed));throw new AssertionError("Delete failure reported success");}catch(IOException expected){checks++;}
            equal(1,failed.attempts);
            try{ShadeWallpaperRepository.deleteFiles(fixtureDirectory((File[])null));throw new AssertionError("Unreadable directory reported success");}catch(IOException expected){checks++;}
            UndeletableAsset current=new UndeletableAsset(hash+".png"),retained=new UndeletableAsset("b".repeat(64)+".png");
            ShadeWallpaperRepository.pruneFiles(fixtureDirectory(current,retained,failed),hash);
            equal(0,current.attempts);equal(0,retained.attempts);equal(2,failed.attempts);
            // A failed scene cleanup must not stop maintenance from removing later scenes.
            String blockedScene=ShadeWallpaperSettings.SCENES.get(1),laterScene=ShadeWallpaperSettings.SCENES.get(2);
            File sceneRoot=target.getParentFile().getParentFile();
            Files.writeString(new File(sceneRoot,blockedScene).toPath(),"directory unavailable");
            File later=new File(new File(sceneRoot,laterScene),hash+".png");later.getParentFile().mkdirs();Files.writeString(later.toPath(),"later scene");
            ShadeWallpaperRepository.clear(context);equal(false,later.exists());
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.walk(privateRoot)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{Files.deleteIfExists(path);}catch(IOException failure){throw new RuntimeException(failure);}});}
        }
        System.out.println("Shade wallpaper repository checks passed: "+checks);
    }
}
