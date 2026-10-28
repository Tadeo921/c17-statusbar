// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.util.function.BooleanSupplier;

/** Bounded SFNT inspection. Downloaded fonts must really contain the advertised weight axis. */
public final class FontFileValidator {
    private FontFileValidator() { }
    public static final class WeightAxis {
        public final float minimum, defaultValue, maximum;
        WeightAxis(float minimum, float defaultValue, float maximum) {
            this.minimum=minimum; this.defaultValue=defaultValue; this.maximum=maximum;
        }
    }
    public static WeightAxis weightAxis(File file) throws IOException {
        try (RandomAccessFile input=new RandomAccessFile(file,"r")) {
            long size=input.length();
            if(size<12)throw new IOException("El archivo de fuente está incompleto");
            int magic=input.readInt();
            if(magic!=0x00010000&&magic!=0x4f54544f)throw new IOException("El archivo descargado no es una fuente TTF ni OTF");
            int tables=input.readUnsignedShort();
            if(tables<1||tables>256||12L+tables*16L>size)throw new IOException("La carpeta de fuentes no es válida");
            for(int i=0;i<tables;i++) {
                input.seek(12L+i*16L);
                int tag=input.readInt();input.readInt();
                long offset=Integer.toUnsignedLong(input.readInt()),length=Integer.toUnsignedLong(input.readInt());
                if(offset>size||length>size-offset)throw new IOException("La tabla de fuentes supera los límites del archivo");
                if(tag!=0x66766172)continue;
                if(length<16)throw new IOException("La tabla de ejes de fuente variable está incompleta");
                input.seek(offset);
                if(input.readInt()!=0x00010000)throw new IOException("La versión de los ejes de fuente variable no es compatible");
                int start=input.readUnsignedShort();input.readUnsignedShort();
                int count=input.readUnsignedShort(),stride=input.readUnsignedShort();
                if(start<16||count<1||count>64||stride<20||stride>256||start+(long)count*stride>length)
                    throw new IOException("El directorio de ejes de fuente variable no es válido");
                for(int axis=0;axis<count;axis++) {
                    input.seek(offset+start+(long)axis*stride);
                    int axisTag=input.readInt();
                    float min=input.readInt()/65536f,normal=input.readInt()/65536f,max=input.readInt()/65536f;
                    if(axisTag!=0x77676874)continue;
                    if(min<1||max>1000||min>=max||normal<min||normal>max)
                        throw new IOException("El eje de grosor de la fuente no es válido");
                    return new WeightAxis(min,normal,max);
                }
                return null;
            }
            return null;
        }
    }
    public static void validate(File file,FontCatalog.Entry entry,BooleanSupplier cancelled) throws IOException {
        checkCancelled(cancelled);
        if(file.length()!=entry.bytes||file.length()>FontCatalog.MAX_DOWNLOAD_BYTES)
            throw new IOException("El tamaño del archivo de fuente no coincide; vuelve a descargarlo");
        if(!entry.sha256.equals(sha256(file,FontCatalog.MAX_DOWNLOAD_BYTES,cancelled)))
            throw new IOException("No se pudo verificar la fuente; vuelve a descargarla");
        WeightAxis axis=weightAxis(file);
        if(axis==null||Math.abs(axis.minimum-entry.minWeight)>.001f||Math.abs(axis.maximum-entry.maxWeight)>.001f)
            throw new IOException("La fuente no contiene el eje de grosor variable declarado");
        checkCancelled(cancelled);
    }
    static String sha256(File file,long limit,BooleanSupplier cancelled) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(FileInputStream input=new FileInputStream(file)) {
                byte[] buffer=new byte[32768];long size=0;int count;
                while((count=input.read(buffer))!=-1) {
                    checkCancelled(cancelled);size+=count;
                    if(size>limit)throw new IOException("El archivo de fuente supera el tamaño permitido");
                    digest.update(buffer,0,count);
                }
            }
            StringBuilder hash=new StringBuilder(64);
            for(byte value:digest.digest()) {
                hash.append(Character.forDigit((value>>>4)&15,16));hash.append(Character.forDigit(value&15,16));
            }
            return hash.toString();
        } catch(java.security.NoSuchAlgorithmException unavailable) {
            throw new IOException("La verificación SHA-256 no está disponible",unavailable);
        }
    }
    static void checkCancelled(BooleanSupplier cancelled) throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted()||(cancelled!=null&&cancelled.getAsBoolean()))
            throw new InterruptedIOException("Se canceló la descarga de la fuente");
    }
}
