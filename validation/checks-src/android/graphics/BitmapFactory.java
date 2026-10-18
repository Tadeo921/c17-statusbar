package android.graphics;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Decodes actual PNG/JPEG bytes and pixels with ImageIO; invalid media really fails. */
public final class BitmapFactory {
    public static final class Options{public boolean inJustDecodeBounds,inScaled=true;public Bitmap.Config inPreferredConfig=Bitmap.Config.ARGB_8888;public int inSampleSize=1,outWidth=-1,outHeight=-1;public String outMimeType;}
    public static Bitmap decodeFile(String path){return decodeFile(path,null);}
    public static Bitmap decodeFile(String path,Options options){try(InputStream input=new FileInputStream(path)){return decodeStream(input,null,options);}catch(Exception failure){return null;}}
    public static Bitmap decodeStream(InputStream input){return decodeStream(input,null,null);}
    public static Bitmap decodeStream(InputStream input,Rect padding,Options options){try{byte[] bytes=input.readAllBytes();return decodeByteArray(bytes,0,bytes.length,options);}catch(Exception failure){return null;}}
    public static Bitmap decodeByteArray(byte[] bytes,int offset,int length){return decodeByteArray(bytes,offset,length,null);}
    public static Bitmap decodeByteArray(byte[] bytes,int offset,int length,Options options){
        try{
            BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes,offset,length));if(image==null)return null;
            if(options!=null){options.outWidth=image.getWidth();options.outHeight=image.getHeight();options.outMimeType=bytes[offset]==(byte)137?"image/png":"image/jpeg";if(options.inJustDecodeBounds)return null;}
            int sample=options==null?1:Math.max(1,options.inSampleSize),width=Math.max(1,image.getWidth()/sample),height=Math.max(1,image.getHeight()/sample);
            Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);bitmap.setHasAlpha(image.getColorModel().hasAlpha());
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)bitmap.pixels[y*width+x]=image.getRGB(Math.min(image.getWidth()-1,x*sample),Math.min(image.getHeight()-1,y*sample));return bitmap;
        }catch(Exception malformed){return null;}
    }
}
