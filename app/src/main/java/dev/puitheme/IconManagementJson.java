// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict bounded JSON shared by imported v3 data and the runtime's independent validation. */
final class IconManagementJson {
    private final String text;private int at,nodes;private final int depthLimit;
    private IconManagementJson(String text,int depthLimit){this.text=text;this.depthLimit=depthLimit;}
    static Map<String,Object> parse(byte[] bytes,int limit)throws IOException{
        if(bytes==null||bytes.length==0||bytes.length>limit)throw error("El tamaño del JSON supera el límite.");
        try{return parse(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(),limit);}
        catch(java.nio.charset.CharacterCodingException malformed){throw error("El JSON debe estar codificado en UTF-8 válido.");}
    }
    static Map<String,Object> parse(String text,int limit)throws IOException{
        if(text==null||text.length()>limit||text.getBytes(StandardCharsets.UTF_8).length>limit)throw error("El tamaño del JSON supera el límite.");
        IconManagementJson reader=new IconManagementJson(text,24);Object value=reader.value(0);reader.space();if(reader.at!=text.length())throw error("Hay contenido sobrante al final del JSON.");return object(value);
    }
    @SuppressWarnings("unchecked") static Map<String,Object> object(Object value)throws IOException{if(!(value instanceof Map))throw error("Se esperaba un objeto JSON.");return (Map<String,Object>)value;}
    @SuppressWarnings("unchecked") static List<Object> array(Object value)throws IOException{if(!(value instanceof List))throw error("Se esperaba una matriz JSON.");return (List<Object>)value;}
    static void only(Map<String,Object> object,String... allowed)throws IOException{Set<String> keys=new HashSet<>(Arrays.asList(allowed));for(String key:object.keySet())if(!keys.contains(key))throw error("Campo no compatible: "+key);}
    static String text(Map<String,Object> object,String key,String fallback,int max)throws IOException{Object value=object.get(key);if(value==null&&!object.containsKey(key))return fallback;if(!(value instanceof String)||((String)value).length()>max)throw error("El campo de texto no es válido: "+key);return (String)value;}
    static double number(Object value)throws IOException{if(!(value instanceof Number))throw error("Se esperaba un valor numérico.");double number=((Number)value).doubleValue();if(!Double.isFinite(number))throw error("El valor numérico debe ser finito.");return number;}
    static int integer(Object value,int min,int max)throws IOException{number(value);try{int number=new BigDecimal(value.toString()).intValueExact();if(number<min||number>max)throw error("El entero está fuera de rango.");return number;}catch(ArithmeticException|NumberFormatException bad){throw error("Se esperaba un entero exacto dentro del rango.");}}
    static boolean bool(Object value)throws IOException{if(!(value instanceof Boolean))throw error("Se esperaba un valor booleano.");return (Boolean)value;}
    static IOException error(String message){return new IOException("Gestión de iconos: "+message);}
    private void space(){while(at<text.length()&&" \t\n\r".indexOf(text.charAt(at))>=0)at++;}
    private boolean take(char token){space();if(at<text.length()&&text.charAt(at)==token){at++;return true;}return false;}
    private Object value(int depth)throws IOException{
        if(depth>depthLimit||++nodes>32768)throw error("La estructura del JSON supera el límite.");space();if(at>=text.length())throw error("El JSON está incompleto.");char token=text.charAt(at);
        if(token=='{'){
            at++;Map<String,Object> values=new LinkedHashMap<>();if(take('}'))return values;
            do{space();String key=string();if(key.length()>128||values.containsKey(key))throw error("Campo demasiado largo o duplicado: "+key);if(!take(':'))throw error("Falta dos puntos.");values.put(key,value(depth+1));}while(take(','));if(!take('}'))throw error("El objeto no está cerrado.");return values;
        }
        if(token=='['){at++;List<Object> values=new ArrayList<>();if(take(']'))return values;do{values.add(value(depth+1));}while(take(','));if(!take(']'))throw error("La matriz no está cerrada.");return values;}
        if(token=='"')return string();
        for(String literal:new String[]{"true","false","null"})if(text.startsWith(literal,at)){at+=literal.length();return literal.equals("null")?null:Boolean.valueOf(literal);}
        int start=at;if(token=='-')at++;if(at>=text.length())throw error("数值不完整");if(text.charAt(at)=='0')at++;else{if(text.charAt(at)<'1'||text.charAt(at)>'9')throw error("无效 JSON 值");while(at<text.length()&&asciiDigit(text.charAt(at)))at++;}
        if(at<text.length()&&text.charAt(at)=='.'){at++;int digit=at;while(at<text.length()&&asciiDigit(text.charAt(at)))at++;if(digit==at)throw error("小数不完整");}
        if(at<text.length()&&(text.charAt(at)=='e'||text.charAt(at)=='E')){at++;if(at<text.length()&&(text.charAt(at)=='+'||text.charAt(at)=='-'))at++;int digit=at;while(at<text.length()&&asciiDigit(text.charAt(at)))at++;if(digit==at)throw error("指数不完整");}
        if(at-start>64)throw error("数值过长");try{BigDecimal number=new BigDecimal(text.substring(start,at));if(!Double.isFinite(number.doubleValue())||Math.abs((long)number.scale())>1024)throw error("数值超限");return number;}catch(NumberFormatException bad){throw error("数值无效");}
    }
    private String string()throws IOException{
        if(at>=text.length()||text.charAt(at++)!='"')throw error("Se esperaba una cadena de texto.");StringBuilder value=new StringBuilder();boolean closed=false;
        while(at<text.length()){
            char c=text.charAt(at++);if(c=='"'){closed=true;break;}if(c<' ')throw error("字符串包含控制字符");
            if(c=='\\'){
                if(at>=text.length())throw error("转义不完整");char escape=text.charAt(at++);
                switch(escape){case '"':case '\\':case '/':c=escape;break;case 'b':c='\b';break;case 'f':c='\f';break;case 'n':c='\n';break;case 'r':c='\r';break;case 't':c='\t';break;
                    case 'u':if(at+4>text.length())throw error("La secuencia de escape Unicode está incompleta.");int code=0;for(int i=0;i<4;i++){char hex=text.charAt(at++);int digit=hex>='0'&&hex<='9'?hex-'0':hex>='A'&&hex<='F'?hex-'A'+10:hex>='a'&&hex<='f'?hex-'a'+10:-1;if(digit<0)throw error("La secuencia de escape Unicode no es válida.");code=code*16+digit;}c=(char)code;break;default:throw error("La secuencia de escape no es válida.");}
            }value.append(c);if(value.length()>65536)throw error("La cadena de texto es demasiado larga.");
        }
        if(!closed)throw error("La cadena de texto no está cerrada.");for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=value.length()||!Character.isLowSurrogate(value.charAt(i)))throw error("El carácter Unicode no es válido.");}else if(Character.isLowSurrogate(c))throw error("El carácter Unicode no es válido.");}return value.toString();
    }
    private static boolean asciiDigit(char digit){return digit>='0'&&digit<='9';}
    static String encode(Object value)throws IOException{StringBuilder out=new StringBuilder();write(out,value);return out.toString();}
    private static void write(StringBuilder out,Object value)throws IOException{
        if(value==null){out.append("null");return;}if(value instanceof String){quote(out,(String)value);return;}if(value instanceof Boolean){out.append(value);return;}
        if(value instanceof Number){double number=((Number)value).doubleValue();if(!Double.isFinite(number))throw error("El valor numérico debe ser finito.");out.append(value instanceof BigDecimal?((BigDecimal)value).stripTrailingZeros().toString():new BigDecimal(value.toString()).stripTrailingZeros().toString());return;}
        if(value instanceof Map){out.append('{');boolean first=true;for(Map.Entry<String,Object> item:new TreeMap<>(object(value)).entrySet()){if(!first)out.append(',');first=false;quote(out,item.getKey());out.append(':');write(out,item.getValue());}out.append('}');return;}
        if(value instanceof Collection){out.append('[');boolean first=true;for(Object item:(Collection<?>)value){if(!first)out.append(',');first=false;write(out,item);}out.append(']');return;}throw error("No se pueden codificar estos datos.");
    }
    private static void quote(StringBuilder out,String value){out.append('"');for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '"':out.append("\\\"");break;case '\\':out.append("\\\\");break;case '\n':out.append("\\n");break;case '\r':out.append("\\r");break;case '\t':out.append("\\t");break;default:if(c<' ')out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}out.append('"');}
}
