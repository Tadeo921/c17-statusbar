// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Worker-only public GitHub download. Every redirect is validated; no credentials are attached. */
public final class IconManagementDownload {
    private static final int TIMEOUT=10000,MAX_JSON=256*1024;
    private static final long DEADLINE_MS=120000;
    private static final Set<String> HOSTS=new HashSet<>(Arrays.asList("github.com","api.github.com","raw.githubusercontent.com",
            "objects.githubusercontent.com","release-assets.githubusercontent.com","codeload.github.com"));
    private IconManagementDownload(){ }
    public static final class Cancellation implements BooleanSupplier {
        private volatile boolean cancelled;private volatile HttpsURLConnection connection;
        @Override public boolean getAsBoolean(){return cancelled;}
        public void cancel(){cancelled=true;HttpsURLConnection active=connection;if(active!=null)active.disconnect();}
        void attach(HttpsURLConnection active){connection=active;if(cancelled)active.disconnect();}
        void detach(HttpsURLConnection active){if(connection==active)connection=null;}
    }
    public static IconManagementRepository.PreparedPack prepare(Context context,String input,String name,BooleanSupplier cancelled)throws Exception{
        long started=System.nanoTime();check(cancelled,started);
        String repository=repository(input);URL url;
        if(repository!=null){
            byte[] latest=fetch(new URL("https://api.github.com/repos/"+repository+"/releases/latest"),MAX_JSON,true,cancelled,started,true);
            if(latest!=null)url=releaseAsset(new String(latest,StandardCharsets.UTF_8));
            else throw new IOException("Este repositorio aún no publica un paquete de iconos v3. Consulta las especificaciones de creación o introduce la URL exacta de una versión publicada.");
        }else{
            url=checkedUrl(input==null?"":input.trim());
            String host=url.getHost(),path=url.getPath().toLowerCase(Locale.ROOT);
            boolean asset=path.endsWith(".zip")||path.endsWith(".rar")||host.equals("codeload.github.com")
                    ||host.equals("api.github.com")&&path.contains("/zipball/");
            if(!asset)throw new IOException("Introduce owner/repo, la URL de un repositorio de GitHub o la URL de descarga ZIP/RAR.");
        }
        File source=IconManagementRepository.createSource(context);
        try{
            byte[] archive=fetch(url,IconManagementArchive.MAX_ARCHIVE,false,cancelled,started,false);
            try(FileOutputStream output=new FileOutputStream(source)){output.write(archive);output.getFD().sync();}
            check(cancelled,started);IconManagementRepository.PreparedPack pack=IconManagementRepository.prepare(context,source,name);
            try{check(cancelled,started);return pack;}catch(Exception aborted){pack.close();throw aborted;}
        }finally{source.delete();}
    }
    static String repository(String input)throws IOException{
        if(input==null)return null;String value=input.trim();
        if(value.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}"))return value;
        if(!value.startsWith("https://"))return null;
        URL url=checkedUrl(value);
        if(!url.getHost().equals("github.com")||url.getQuery()!=null)return null;
        String path=url.getPath();if(path.endsWith("/"))path=path.substring(0,path.length()-1);
        if(path.endsWith(".git"))path=path.substring(0,path.length()-4);
        String candidate=path.startsWith("/")?path.substring(1):path;
        return candidate.matches("[A-Za-z0-9_.-]{1,100}/[A-Za-z0-9_.-]{1,100}")?candidate:null;
    }
    static URL checkedUrl(String input)throws IOException{
        try{
            URI uri=new URI(input);String host=uri.getHost();
            if(!"https".equals(uri.getScheme())||host==null||!HOSTS.contains(host.toLowerCase(Locale.ROOT))
                    ||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null
                    ||input.length()>4096)throw new IOException("Solo se permiten descargas HTTPS desde GitHub oficial.");
            return uri.toURL();
        }catch(java.net.URISyntaxException malformed){throw new IOException("La URL de descarga de GitHub no es válida.",malformed);}
    }
    static URL releaseAsset(String source)throws IOException{
        try{
            JSONObject json=new JSONObject(source);if(Boolean.TRUE.equals(json.opt("draft")))throw new IOException("El paquete de iconos aún no tiene una versión pública.");
            JSONArray assets=json.optJSONArray("assets");URL selected=null;int score=-1,count=0;
            if(assets!=null)for(int i=0;i<assets.length();i++){
                JSONObject asset=assets.optJSONObject(i);if(asset==null)continue;
                String name=asset.optString("name","").toLowerCase(Locale.ROOT);
                if(!name.equals("icon-pack-v3.zip"))continue;
                Object size=asset.opt("size");if(!(size instanceof Number)||((Number)size).longValue()>IconManagementArchive.MAX_ARCHIVE
                        ||((Number)size).longValue()<=0)continue;
                URL url=checkedUrl(asset.optString("browser_download_url",""));
                if(!url.getHost().equals("github.com")||!url.getPath().contains("/releases/download/"))continue;
                int candidate=3;
                if(candidate>score){selected=url;score=candidate;count=1;}else if(candidate==score)count++;
            }
            if(selected==null)throw new IOException("La última versión pública no contiene icon-pack-v3.zip (límite: 24 MiB).");
            if(count>1)throw new IOException("La última versión contiene varios paquetes de iconos. Introduce la URL exacta de descarga.");
            return selected;
        }catch(JSONException malformed){throw new IOException("Los datos de la versión de GitHub no son válidos.",malformed);}
    }
    private static byte[] fetch(URL start,int maximum,boolean json,BooleanSupplier cancelled,long started,boolean missingAllowed)throws IOException{
        URL url=checkedUrl(start.toString());
        for(int redirects=0;redirects<=5;redirects++){
            check(cancelled,started);HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();
            connection.setConnectTimeout(TIMEOUT);connection.setReadTimeout(TIMEOUT);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept-Encoding","identity");connection.setRequestProperty("User-Agent","C17-IconPack");
            connection.setRequestProperty("Accept",json?"application/vnd.github+json":"application/octet-stream");
            if(cancelled instanceof Cancellation)((Cancellation)cancelled).attach(connection);
            try{
                check(cancelled,started);int code=connection.getResponseCode();check(cancelled,started);
                if(code==301||code==302||code==303||code==307||code==308){
                    String location=connection.getHeaderField("Location");if(location==null)throw new IOException("La redirección de descarga de GitHub no es válida.");
                    url=checkedUrl(new URL(url,location).toString());continue;
                }
                if(code==404&&missingAllowed)return null;
                if(code!=200)throw new IOException(code==403||code==429?"GitHub ha limitado las solicitudes. Inténtalo más tarde.":"La descarga de GitHub ha fallado ("+code+"）");
                long declared=connection.getContentLengthLong();if(declared>maximum)throw new IOException("El archivo descargado supera el tamaño permitido.");
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int count;
                    while((count=input.read(buffer))!=-1){check(cancelled,started);if(bytes.size()+count>maximum)throw new IOException("El archivo descargado supera el tamaño permitido.");bytes.write(buffer,0,count);}
                    if(declared>=0&&declared!=bytes.size())throw new IOException("La descarga está incompleta.");return bytes.toByteArray();
                }
            }finally{if(cancelled instanceof Cancellation)((Cancellation)cancelled).detach(connection);connection.disconnect();}
        }
        throw new IOException("La descarga de GitHub ha redirigido demasiadas veces.");
    }
    private static String encodePath(String value)throws IOException{return java.net.URLEncoder.encode(value,"UTF-8").replace("+","%20");}
    private static void check(BooleanSupplier cancelled,long started)throws IOException{
        if(Thread.currentThread().isInterrupted()||cancelled!=null&&cancelled.getAsBoolean())throw new InterruptedIOException("Se canceló la descarga del paquete de iconos.");
        if((System.nanoTime()-started)/1000000L>DEADLINE_MS)throw new IOException("La descarga del paquete de iconos ha agotado el tiempo de espera. Inténtalo más tarde.");
    }
}
