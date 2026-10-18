package com.airbnb.lottie;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class LottieCompositionFactory {
    public static int parses,nonNullCacheKeys;public static boolean fail;public static Runnable afterParse;
    public static LottieResult fromJsonInputStreamSync(InputStream input,String key){parses++;if(key!=null)nonNullCacheKeys++;
        try{if(fail)throw new IOException("fixture OEM parse failure");org.json.JSONObject json=new org.json.JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8));if(json.getJSONArray("layers").length()==0)throw new IOException("missing shape layers");LottieComposition value=new LottieComposition(json);if(afterParse!=null)afterParse.run();return new LottieResult(value,null);}catch(Exception error){return new LottieResult(null,error);}}
}
