// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.CRC32;

/** Worker-only shared preview/runtime decoder. Provider bytes are checked again before use. */
public final class IconAssetDecoder {
    public static final class Metadata{public final int width,height;public final String format;Metadata(int width,int height,String format){this.width=width;this.height=height;this.format=format;}}
    public static final class Decoded{public final Bitmap bitmap;public final boolean mask;public final int pixels;public final String assetHash;public final IconVisibleBounds visibleBounds;
        /** Logical canvas inside the raster; vectors are letterboxed by render(size). */
        public final float canvasWidth,canvasHeight;
        Decoded(Bitmap bitmap,boolean mask,String hash,IconVisibleBounds bounds,float canvasWidth,float canvasHeight){this.bitmap=bitmap;this.mask=mask;pixels=bitmap.getWidth()*bitmap.getHeight();assetHash=hash;visibleBounds=bounds;this.canvasWidth=canvasWidth;this.canvasHeight=canvasHeight;}}
    private static final byte[] PNG={(byte)137,80,78,71,13,10,26,10};
    private static final class Png{final byte[] bytes;final int width,height;Png(byte[] bytes,int width,int height){this.bytes=bytes;this.width=width;this.height=height;}}
    public static String sha256(byte[] bytes)throws IOException{try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte value:hash)out.append(String.format(Locale.ROOT,"%02x",value&255));return out.toString();}catch(java.security.NoSuchAlgorithmException impossible){throw new IOException(impossible);}}
    public static void verify(byte[] bytes,String digest)throws IOException{if(!IconManagementSettings.validPackId(digest)||!digest.equals(sha256(bytes)))throw IconManagementJson.error("Falló la verificación del resumen del recurso");}
    public static Metadata validate(String source,byte[] bytes)throws IOException{
        if(source.endsWith(".xml")){IconVectorAsset vector=IconVectorAsset.parse(bytes);return new Metadata((int)Math.ceil(vector.viewportWidth),(int)Math.ceil(vector.viewportHeight),"vector");}
        if(!source.endsWith(".png"))throw IconManagementJson.error("El formato del recurso estático no es compatible");Png png=png(bytes);Bitmap decoded=null;
        try{decoded=BitmapFactory.decodeByteArray(png.bytes,0,png.bytes.length);if(decoded==null||!decoded.hasAlpha())throw IconManagementJson.error("El PNG debe poder decodificarse y tener fondo transparente");int[] pixels=new int[decoded.getWidth()*decoded.getHeight()];decoded.getPixels(pixels,0,decoded.getWidth(),0,0,decoded.getWidth(),decoded.getHeight());boolean transparent=false;for(int pixel:pixels)if((pixel>>>24)<255){transparent=true;break;}if(!transparent)throw IconManagementJson.error("El PNG no tiene fondo transparente");return new Metadata(png.width,png.height,"png");}
        catch(OutOfMemoryError failure){throw new IOException("Gestión de iconos: no hay memoria suficiente para validar el PNG",failure);}finally{if(decoded!=null)decoded.recycle();}
    }
    public static Decoded decode(IconManagementManifest.Asset asset,byte[] bytes,int size,int remainingPixels)throws IOException{
        if(size<1||size>256||remainingPixels<1)throw IconManagementJson.error("El límite de decodificación o las dimensiones del icono no son válidos");verify(bytes,asset.sha256);Bitmap decoded=null,scaled=null;float canvasWidth=0,canvasHeight=0;
        try{
            if("vector".equals(asset.format)){if((long)size*size>remainingPixels)throw IconManagementJson.error("El presupuesto de píxeles vectoriales es insuficiente");IconVectorAsset vector=IconVectorAsset.parse(bytes);decoded=vector.render(size);float scale=Math.min(size/vector.viewportWidth,size/vector.viewportHeight);canvasWidth=vector.viewportWidth*scale;canvasHeight=vector.viewportHeight*scale;}
            else{Png png=png(bytes);BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(png.width,png.height)/options.inSampleSize>size*2)options.inSampleSize*=2;decoded=BitmapFactory.decodeByteArray(png.bytes,0,png.bytes.length,options);if(decoded==null||!decoded.hasAlpha())throw IconManagementJson.error("No se pudo decodificar el PNG");
                float ratio=Math.min(1f,size/(float)Math.max(decoded.getWidth(),decoded.getHeight()));int width=Math.max(1,Math.round(decoded.getWidth()*ratio)),height=Math.max(1,Math.round(decoded.getHeight()*ratio));if((long)width*height>remainingPixels)throw IconManagementJson.error("El presupuesto de píxeles del icono es insuficiente");if(width!=decoded.getWidth()||height!=decoded.getHeight()){scaled=Bitmap.createScaledBitmap(decoded,width,height,true);decoded.recycle();decoded=scaled;scaled=null;}}
            // Scan the owned ARGB raster once on the decoder worker, before extracting an ALPHA_8 mask.
            int width=decoded.getWidth(),height=decoded.getHeight();int[] argb=new int[width*height];decoded.getPixels(argb,0,width,0,0,width,height);
            if(canvasWidth==0){canvasWidth=width;canvasHeight=height;}
            IconVisibleBounds bounds=IconVisibleBounds.scan(argb,width,height);if(bounds==null)throw IconManagementJson.error("El icono no contiene píxeles visibles");
            if("mask".equals(asset.render)){Bitmap mask=decoded.extractAlpha();decoded.recycle();decoded=null;return new Decoded(mask,true,asset.sha256,bounds,canvasWidth,canvasHeight);}Bitmap result=decoded;decoded=null;return new Decoded(result,false,asset.sha256,bounds,canvasWidth,canvasHeight);
        }catch(OutOfMemoryError failure){throw new IOException("Gestión de iconos: no hay memoria suficiente para decodificar el icono",failure);}catch(RuntimeException failure){throw new IOException("Gestión de iconos: falló la decodificación del icono",failure);}finally{if(decoded!=null)decoded.recycle();if(scaled!=null)scaled.recycle();}
    }
    /** Worker-only preview; show the same visible aspect ratio as the native runtime without altering source pixels. */
    public static Bitmap preview(IconManagementManifest.Asset asset,byte[] bytes,int size,int remainingPixels)throws IOException{
        return preview(asset,bytes,size,remainingPixels,false);
    }
    public static Bitmap preview(IconManagementManifest.Asset asset,byte[] bytes,int size,int remainingPixels,boolean sharedCanvas)throws IOException{
        Decoded decoded=decode(asset,bytes,size,remainingPixels);Bitmap source=decoded.bitmap;IconVisibleBounds bounds=decoded.visibleBounds;
        int left=bounds.left,top=bounds.top,right=bounds.right,bottom=bounds.bottom;
        if(sharedCanvas){left=Math.max(0,(int)Math.floor((source.getWidth()-decoded.canvasWidth)*.5f));top=Math.max(0,(int)Math.floor((source.getHeight()-decoded.canvasHeight)*.5f));right=Math.min(source.getWidth(),(int)Math.ceil((source.getWidth()+decoded.canvasWidth)*.5f));bottom=Math.min(source.getHeight(),(int)Math.ceil((source.getHeight()+decoded.canvasHeight)*.5f));}
        if(left==0&&top==0&&right==source.getWidth()&&bottom==source.getHeight())return source;
        try{Bitmap cropped=Bitmap.createBitmap(source,left,top,right-left,bottom-top);if(cropped!=source)source.recycle();return cropped;}
        catch(RuntimeException|OutOfMemoryError failure){source.recycle();throw new IOException("Gestión de iconos: falló el recorte de la vista previa",failure);}
    }
    private static int integer(byte[] bytes,int at)throws IOException{if(at<0||at+4>bytes.length)throw IconManagementJson.error("Los datos PNG están truncados");long number=((long)(bytes[at]&255)<<24)|((long)(bytes[at+1]&255)<<16)|((long)(bytes[at+2]&255)<<8)|(bytes[at+3]&255);if(number>Integer.MAX_VALUE)throw IconManagementJson.error("El tamaño del bloque PNG no es válido");return (int)number;}
    private static Png png(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length<45||bytes.length>1024*1024||!Arrays.equals(PNG,Arrays.copyOf(bytes,8)))throw IconManagementJson.error("Se requiere un archivo PNG real (hasta 1 MiB por imagen)");
        ByteArrayOutputStream cleaned=new ByteArrayOutputStream();cleaned.write(PNG);int at=8,width=0,height=0,count=0;boolean idat=false,ended=false;
        while(at<bytes.length){if(++count>1024||at+12>bytes.length)throw IconManagementJson.error("La cantidad o el tamaño de los bloques PNG no es válido");int length=integer(bytes,at);long end=(long)at+12+length;if(end>bytes.length)throw IconManagementJson.error("El bloque PNG está truncado");String type=new String(bytes,at+4,4,java.nio.charset.StandardCharsets.US_ASCII);if(!type.matches("[A-Za-z]{4}")||Arrays.asList("acTL","fcTL","fdAT").contains(type))throw IconManagementJson.error("No se admite APNG ni bloques PNG no válidos");
            CRC32 crc=new CRC32();crc.update(bytes,at+4,length+4);long expected=((long)(bytes[(int)end-4]&255)<<24)|((long)(bytes[(int)end-3]&255)<<16)|((long)(bytes[(int)end-2]&255)<<8)|(bytes[(int)end-1]&255);if(crc.getValue()!=expected)throw IconManagementJson.error("Falló la comprobación CRC del PNG");
            if(count==1){if(!type.equals("IHDR")||length!=13)throw IconManagementJson.error("Falta IHDR en el PNG");width=integer(bytes,at+8);height=integer(bytes,at+12);if(width<1||height<1||width>1024||height>1024)throw IconManagementJson.error("El lado más largo del PNG no puede superar los 1024 píxeles");}
            else if(type.equals("IHDR"))throw IconManagementJson.error("El PNG contiene un IHDR duplicado");
            if(type.equals("IDAT"))idat=true;if(type.equals("IEND")){if(length!=0||!idat||end!=bytes.length)throw IconManagementJson.error("El IEND del PNG no es válido");ended=true;}
            if(Character.isUpperCase(type.charAt(0))&&!Arrays.asList("IHDR","PLTE","IDAT","IEND").contains(type))throw IconManagementJson.error("El PNG contiene un bloque crítico desconocido");
            // Non-graphic metadata is not passed to the platform image decoder (ICC/text payloads can be compressed).
            if(Arrays.asList("IHDR","PLTE","IDAT","IEND","tRNS").contains(type))cleaned.write(bytes,at,(int)end-at);at=(int)end;
        }if(!ended)throw IconManagementJson.error("Falta IEND en el PNG");return new Png(cleaned.toByteArray(),width,height);
    }
    private IconAssetDecoder(){ }
}
