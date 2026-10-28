// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.graphics.*;
import android.util.Xml;
import androidx.core.graphics.PathParser;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Restricted textual VectorDrawable parsed into owned nodes; external XML is never inflated as a Drawable. */
public final class IconVectorAsset {
    public static final int MAX_BYTES=128*1024;private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private static final Pattern NUMBER=Pattern.compile("[-+]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?");
    private static final class Node{final String tag;final Map<String,String> attrs;final List<Node> children=new ArrayList<>();Node(String tag,Map<String,String> attrs){this.tag=tag;this.attrs=attrs;}}
    public final float viewportWidth,viewportHeight,alpha;public final int nodes,pathCharacters;private final Node root;
    private IconVectorAsset(Node root,int nodes,int pathCharacters)throws IOException{this.root=root;this.nodes=nodes;this.pathCharacters=pathCharacters;viewportWidth=number(root.attrs,"viewportWidth",Float.NaN,1,1024);viewportHeight=number(root.attrs,"viewportHeight",Float.NaN,1,1024);alpha=number(root.attrs,"alpha",1,0,1);}
    public static IconVectorAsset parse(byte[] bytes)throws IOException{
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw IconManagementJson.error("El tamaño del XML vectorial no es válido.");String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
        catch(java.nio.charset.CharacterCodingException bad){throw IconManagementJson.error("El vector debe ser texto UTF-8.");}
        if(text.indexOf('&')>=0||Pattern.compile("(?is)<!\\s*(DOCTYPE|ENTITY)|<\\?(?!xml(?:\\s|\\?))").matcher(text).find())throw IconManagementJson.error("El vector no puede incluir entidades, DTD ni instrucciones de procesamiento.");
        try{
            XmlPullParser parser=Xml.newPullParser();parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);parser.setInput(new StringReader(text));Deque<Node> stack=new ArrayDeque<>();Node root=null;int count=0,pathChars=0;
            for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.nextToken()){
                if(event==XmlPullParser.DOCDECL||event==XmlPullParser.ENTITY_REF||event==XmlPullParser.PROCESSING_INSTRUCTION)throw IconManagementJson.error("El vector no puede incluir declaraciones, entidades ni instrucciones de procesamiento.");
                if(event==XmlPullParser.TEXT&&!parser.getText().trim().isEmpty())throw IconManagementJson.error("El vector no admite contenido de texto.");
                if(event==XmlPullParser.START_TAG){
                    String tag=parser.getName();if(++count>128||stack.size()>=8||!Arrays.asList("vector","group","path","clip-path").contains(tag)||parser.getNamespace()!=null&&!parser.getNamespace().isEmpty())throw IconManagementJson.error("Los nodos, niveles o etiquetas del vector no son válidos.");
                    if(root==null&&!tag.equals("vector")||root!=null&&stack.isEmpty()||tag.equals("vector")&&root!=null||!stack.isEmpty()&&!Arrays.asList("vector","group").contains(stack.peek().tag))throw IconManagementJson.error("La estructura del vector no es válida.");
                    Map<String,String> attrs=new LinkedHashMap<>();Set<String> allowed=new HashSet<>(Arrays.asList(tag.equals("vector")?new String[]{"width","height","viewportWidth","viewportHeight","alpha","name"}:tag.equals("group")?new String[]{"rotation","pivotX","pivotY","scaleX","scaleY","translateX","translateY","name"}:tag.equals("clip-path")?new String[]{"pathData","fillType","name"}:new String[]{"pathData","fillColor","fillAlpha","strokeColor","strokeAlpha","strokeWidth","strokeLineCap","strokeLineJoin","strokeMiterLimit","fillType","name"}));
                    for(int i=0;i<parser.getAttributeCount();i++){String name=parser.getAttributeName(i),value=parser.getAttributeValue(i);if(!ANDROID.equals(parser.getAttributeNamespace(i))||!allowed.contains(name)||attrs.put(name,value)!=null||value.indexOf('@')>=0||value.indexOf('?')>=0)throw IconManagementJson.error("El atributo o la referencia de recurso vectorial no es válida: "+name);}
                    if(attrs.containsKey("name")&&attrs.get("name").length()>64)throw IconManagementJson.error("El nombre del vector es demasiado largo.");
                    if(tag.equals("vector")){dimension(attrs.get("width"));dimension(attrs.get("height"));number(attrs,"viewportWidth",Float.NaN,1,1024);number(attrs,"viewportHeight",Float.NaN,1,1024);number(attrs,"alpha",1,0,1);}
                    else if(tag.equals("group")){for(String key:new String[]{"rotation","pivotX","pivotY","scaleX","scaleY","translateX","translateY"})number(attrs,key,key.startsWith("scale")?1:0,-4096,4096);}
                    else{String data=attrs.get("pathData");if(data==null||(pathChars+=data.length())>65536)throw IconManagementJson.error("Falta la ruta vectorial o su longitud total supera el límite.");validatePath(data);if(attrs.containsKey("fillType")&&!Arrays.asList("nonZero","evenOdd").contains(attrs.get("fillType")))throw IconManagementJson.error("La regla de relleno no es válida.");
                        if(tag.equals("path")){color(attrs.get("fillColor"),0xff000000);color(attrs.get("strokeColor"),0);number(attrs,"fillAlpha",1,0,1);number(attrs,"strokeAlpha",1,0,1);number(attrs,"strokeWidth",0,0,4096);number(attrs,"strokeMiterLimit",4,1,64);choice(attrs,"strokeLineCap","butt","round","square");choice(attrs,"strokeLineJoin","miter","round","bevel");}}
                    Node node=new Node(tag,Collections.unmodifiableMap(attrs));if(root==null)root=node;else stack.peek().children.add(node);stack.push(node);
                }else if(event==XmlPullParser.END_TAG){if(stack.isEmpty())throw IconManagementJson.error("La etiqueta de cierre del vector no es válida.");stack.pop();}
            }if(root==null||!stack.isEmpty())throw IconManagementJson.error("El contenido vectorial está incompleto.");return new IconVectorAsset(root,count,pathChars);
        }catch(IOException failure){throw failure;}catch(Exception failure){throw new IOException("Gestión de iconos: no se pudo analizar el XML vectorial de forma segura.",failure);}
    }
    private static void dimension(String value)throws IOException{if(value==null)return;String number=value.endsWith("dp")||value.endsWith("px")?value.substring(0,value.length()-2):value;try{float amount=Float.parseFloat(number);if(!Float.isFinite(amount)||amount<=0||amount>1024)throw new NumberFormatException();}catch(NumberFormatException bad){throw IconManagementJson.error("Las dimensiones del vector no son válidas.");}}
    private static void choice(Map<String,String> attrs,String key,String... allowed)throws IOException{if(attrs.containsKey(key)&&!Arrays.asList(allowed).contains(attrs.get(key)))throw IconManagementJson.error("El estilo del vector no es válido: "+key);}
    private static float number(Map<String,String> attrs,String key,float fallback,float min,float max)throws IOException{String value=attrs.get(key);float number;try{number=value==null?fallback:Float.parseFloat(value);}catch(NumberFormatException bad){throw IconManagementJson.error("El valor vectorial no es válido: "+key);}if(!Float.isFinite(number)||number<min||number>max)throw IconManagementJson.error("El valor vectorial supera el límite: "+key);return number;}
    private static int color(String value,int fallback)throws IOException{if(value==null)return fallback;if(!value.matches("#[0-9a-fA-F]{8}"))throw IconManagementJson.error("El color vectorial debe usar el formato #AARRGGBB.");return (int)Long.parseLong(value.substring(1),16);}
    static void validatePath(String data)throws IOException{
        if(data.isEmpty()||data.length()>65536)throw IconManagementJson.error("El tamaño de los datos de ruta no es válido.");char command=0;int count=0,total=0;List<Float> args=new ArrayList<>();
        for(int at=0;at<data.length();){char c=data.charAt(at);if(Character.isWhitespace(c)||c==','){at++;continue;}
            if("MmLlHhVvCcSsQqTtAaZz".indexOf(c)>=0){if(command!=0)checkArguments(command,args);if(command==0&&c!='M'&&c!='m')throw IconManagementJson.error("La ruta debe comenzar con M.");command=c;args.clear();at++;if(++count>2048)throw IconManagementJson.error("Hay demasiados comandos de ruta.");continue;}
            if(command==0)throw IconManagementJson.error("Falta un comando de ruta.");Matcher number=NUMBER.matcher(data);number.region(at,data.length());if(!number.lookingAt())throw IconManagementJson.error("El comando o el valor de la ruta no es válido.");float value;try{value=Float.parseFloat(number.group());}catch(NumberFormatException bad){throw IconManagementJson.error("El valor de la ruta no es válido.");}if(!Float.isFinite(value)||Math.abs(value)>4096||++total>8192)throw IconManagementJson.error("El valor o la cantidad de parámetros de la ruta supera el límite.");args.add(value);at=number.end();
        }checkArguments(command,args);
    }
    private static void checkArguments(char command,List<Float> args)throws IOException{char type=Character.toUpperCase(command);int arity=type=='Z'?0:type=='H'||type=='V'?1:type=='M'||type=='L'||type=='T'?2:type=='S'||type=='Q'?4:type=='C'?6:7;
        if(arity==0?!args.isEmpty():args.isEmpty()||args.size()%arity!=0)throw IconManagementJson.error("La cantidad de parámetros de la ruta no es correcta.");if(type=='A')for(int i=0;i<args.size();i+=7)if(args.get(i)<0||args.get(i+1)<0||args.get(i+3)!=0&&args.get(i+3)!=1||args.get(i+4)!=0&&args.get(i+4)!=1)throw IconManagementJson.error("El radio o el indicador del arco no es válido.");}
    public Bitmap render(int size)throws IOException{if(size<1||size>256)throw IconManagementJson.error("El tamaño de decodificación vectorial no es válido.");Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);try{Canvas canvas=new Canvas(bitmap);int layer=alpha<1?canvas.saveLayerAlpha(0,0,size,size,Math.round(alpha*255)):canvas.save();float scale=Math.min(size/viewportWidth,size/viewportHeight);canvas.translate((size-viewportWidth*scale)/2,(size-viewportHeight*scale)/2);canvas.scale(scale,scale);draw(canvas,root);canvas.restoreToCount(layer);return bitmap;}catch(RuntimeException failure){bitmap.recycle();throw new IOException("Gestión de iconos: no se pudo renderizar el vector.",failure);}}
    private void draw(Canvas canvas,Node node)throws IOException{
        Map<String,String> a=node.attrs;if(node.tag.equals("vector")){for(Node child:node.children)draw(canvas,child);return;}
        if(node.tag.equals("group")){int save=canvas.save();float px=number(a,"pivotX",0,-4096,4096),py=number(a,"pivotY",0,-4096,4096);canvas.translate(px+number(a,"translateX",0,-4096,4096),py+number(a,"translateY",0,-4096,4096));canvas.rotate(number(a,"rotation",0,-4096,4096));canvas.scale(number(a,"scaleX",1,-4096,4096),number(a,"scaleY",1,-4096,4096));canvas.translate(-px,-py);for(Node child:node.children)draw(canvas,child);canvas.restoreToCount(save);return;}
        Path path=PathParser.createPathFromPathData(a.get("pathData"));if(path==null)throw IconManagementJson.error("La ruta vectorial no se puede dibujar.");path.setFillType("evenOdd".equals(a.get("fillType"))?Path.FillType.EVEN_ODD:Path.FillType.WINDING);
        if(node.tag.equals("clip-path")){canvas.clipPath(path);return;}Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int fill=color(a.get("fillColor"),0xff000000),stroke=color(a.get("strokeColor"),0);
        paint.setStyle(Paint.Style.FILL);paint.setColor(fill);paint.setAlpha(Math.round(Color.alpha(fill)*number(a,"fillAlpha",1,0,1)));if(paint.getAlpha()>0)canvas.drawPath(path,paint);
        float width=number(a,"strokeWidth",0,0,4096);if(width>0&&Color.alpha(stroke)>0){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setColor(stroke);paint.setAlpha(Math.round(Color.alpha(stroke)*number(a,"strokeAlpha",1,0,1)));paint.setStrokeMiter(number(a,"strokeMiterLimit",4,1,64));String cap=a.get("strokeLineCap"),join=a.get("strokeLineJoin");paint.setStrokeCap("round".equals(cap)?Paint.Cap.ROUND:"square".equals(cap)?Paint.Cap.SQUARE:Paint.Cap.BUTT);paint.setStrokeJoin("round".equals(join)?Paint.Join.ROUND:"bevel".equals(join)?Paint.Join.BEVEL:Paint.Join.MITER);canvas.drawPath(path,paint);}
    }
}
