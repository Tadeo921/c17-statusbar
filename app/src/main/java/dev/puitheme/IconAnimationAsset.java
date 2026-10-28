// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.io.IOException;
import java.util.*;

/** Shape-only bounded Lottie admission. This validator never evaluates expressions or invokes an OEM parser. */
public final class IconAnimationAsset {
    public static final int MAX_BYTES=256*1024;
    public final int width,height,layerCount,nodeCount;public final float fps,durationMs,startFrame,endFrame;
    private IconAnimationAsset(int width,int height,float fps,float start,float end,int layers,int nodes){this.width=width;this.height=height;this.fps=fps;startFrame=start;endFrame=end;durationMs=(end-start)*1000/fps;layerCount=layers;nodeCount=nodes;}
    private static final class Budget{int shapes,keyframes,vertices,animatedProperties;double ip,op;}
    private enum Kind{SCALAR,NONNEG,OPACITY,COLOR,POINT,SIZE,SCALE,PATH}
    public static IconAnimationAsset parse(byte[] bytes,boolean transientLoop)throws IOException{
        Map<String,Object> root=IconManagementJson.parse(bytes,MAX_BYTES);IconManagementJson.only(root,"v","w","h","fr","ip","op","layers","assets","nm","ddd","markers");
        String version=IconManagementJson.text(root,"v","",24);if(!version.matches("[0-9]+(?:\\.[0-9]+){1,3}"))throw IconManagementJson.error("La marca de versión de Lottie no es válida");
        int width=IconManagementJson.integer(root.get("w"),1,256),height=IconManagementJson.integer(root.get("h"),1,256);double fps=bounded(root.get("fr"),1,60),ip=bounded(root.get("ip"),0,100000),op=bounded(root.get("op"),0,100000);
        if(op<=ip||(op-ip)*1000/fps>(transientLoop?1500:800)+.001)throw IconManagementJson.error("La duración de la animación del icono supera el límite");zero(root,"ddd");empty(root,"assets");empty(root,"markers");
        List<Object> layers=IconManagementJson.array(root.get("layers"));if(layers.isEmpty()||layers.size()>16)throw IconManagementJson.error("La cantidad de capas animadas no es válida");Budget budget=new Budget();budget.ip=ip;budget.op=op;Set<Integer> indices=new HashSet<>();
        for(Object value:layers){Map<String,Object> layer=IconManagementJson.object(value);IconManagementJson.only(layer,"ddd","ind","ty","nm","ks","ao","shapes","ip","op","st","sr","bm","hd");
            if(IconManagementJson.integer(layer.get("ty"),4,4)!=4)throw IconManagementJson.error("La animación solo admite capas de formas");zero(layer,"ddd");zero(layer,"ao");zero(layer,"bm");zero(layer,"st");if(layer.containsKey("sr")&&bounded(layer.get("sr"),1,1)!=1)throw IconManagementJson.error("La animación no admite remapeo de tiempo");
            if(!indices.add(IconManagementJson.integer(layer.get("ind"),0,65535)))throw IconManagementJson.error("Hay identificadores de capas animadas duplicados");
            double layerStart=layer.containsKey("ip")?bounded(layer.get("ip"),ip,op):ip,layerEnd=layer.containsKey("op")?bounded(layer.get("op"),ip,op):op;if(layerEnd<=layerStart)throw IconManagementJson.error("El intervalo de tiempo de la capa no es válido");
            text(layer);hidden(layer);transform(IconManagementJson.object(layer.get("ks")),budget,false);shapes(IconManagementJson.array(layer.get("shapes")),budget,0);
        }
        if(budget.animatedProperties==0)throw IconManagementJson.error("El recurso animado debe contener fotogramas clave que cambien realmente; para imágenes estáticas, usa PNG o vectoriales");
        return new IconAnimationAsset(width,height,(float)fps,(float)ip,(float)op,layers.size(),budget.shapes);
    }
    private static void empty(Map<String,Object> map,String key)throws IOException{if(map.containsKey(key)&&!IconManagementJson.array(map.get(key)).isEmpty())throw IconManagementJson.error("La animación no permite recursos externos, texto ni marcas: "+key);}
    private static void zero(Map<String,Object> map,String key)throws IOException{if(map.containsKey(key))bounded(map.get(key),0,0);}
    private static double bounded(Object value,double min,double max)throws IOException{double number=IconManagementJson.number(value);if(number<min||number>max)throw IconManagementJson.error("Un valor de la animación supera el límite");return number;}
    private static void text(Map<String,Object> value)throws IOException{IconManagementJson.text(value,"nm","",128);}
    private static void hidden(Map<String,Object> value)throws IOException{if(value.containsKey("hd"))IconManagementJson.bool(value.get("hd"));}
    private static void transform(Map<String,Object> value,Budget budget,boolean shape)throws IOException{
        if(shape)IconManagementJson.only(value,"ty","nm","hd","a","p","s","r","o","sk","sa");else IconManagementJson.only(value,"a","p","s","r","o","sk","sa");
        for(String key:new String[]{"a","p","s","r","o","sk","sa"})if(value.containsKey(key))property(value.get(key),key.equals("a")||key.equals("p")?Kind.POINT:key.equals("s")?Kind.SCALE:key.equals("o")?Kind.OPACITY:Kind.SCALAR,budget);
    }
    private static void shapes(List<Object> shapes,Budget budget,int depth)throws IOException{
        if(depth>8||shapes.isEmpty())throw IconManagementJson.error("La jerarquía de formas de la animación no es válida");
        for(Object value:shapes){if(++budget.shapes>256)throw IconManagementJson.error("La cantidad de formas de la animación supera el límite");Map<String,Object> shape=IconManagementJson.object(value);String type=IconManagementJson.text(shape,"ty","",8);text(shape);hidden(shape);
            switch(type){
                case "gr":IconManagementJson.only(shape,"ty","it","nm","hd","np","cix","bm");zero(shape,"bm");if(shape.containsKey("np"))IconManagementJson.integer(shape.get("np"),0,256);if(shape.containsKey("cix"))IconManagementJson.integer(shape.get("cix"),0,256);shapes(IconManagementJson.array(shape.get("it")),budget,depth+1);break;
                case "sh":IconManagementJson.only(shape,"ty","ks","nm","hd","ind","d");if(shape.containsKey("ind"))IconManagementJson.integer(shape.get("ind"),0,65535);if(shape.containsKey("d"))IconManagementJson.integer(shape.get("d"),1,3);property(shape.get("ks"),Kind.PATH,budget);break;
                case "fl":IconManagementJson.only(shape,"ty","c","o","r","nm","hd","bm");zero(shape,"bm");if(shape.containsKey("r"))IconManagementJson.integer(shape.get("r"),1,2);property(shape.get("c"),Kind.COLOR,budget);property(shape.get("o"),Kind.OPACITY,budget);break;
                case "st":IconManagementJson.only(shape,"ty","c","o","w","lc","lj","ml","nm","hd","bm");zero(shape,"bm");property(shape.get("c"),Kind.COLOR,budget);property(shape.get("o"),Kind.OPACITY,budget);property(shape.get("w"),Kind.NONNEG,budget);if(shape.containsKey("lc"))IconManagementJson.integer(shape.get("lc"),1,3);if(shape.containsKey("lj"))IconManagementJson.integer(shape.get("lj"),1,3);if(shape.containsKey("ml"))bounded(shape.get("ml"),1,64);break;
                case "tr":transform(shape,budget,true);break;
                case "el":IconManagementJson.only(shape,"ty","p","s","d","nm","hd");property(shape.get("p"),Kind.POINT,budget);property(shape.get("s"),Kind.SIZE,budget);if(shape.containsKey("d"))IconManagementJson.integer(shape.get("d"),1,3);break;
                case "rc":IconManagementJson.only(shape,"ty","p","s","r","d","nm","hd");property(shape.get("p"),Kind.POINT,budget);property(shape.get("s"),Kind.SIZE,budget);property(shape.get("r"),Kind.NONNEG,budget);if(shape.containsKey("d"))IconManagementJson.integer(shape.get("d"),1,3);break;
                default:throw IconManagementJson.error("Forma Lottie no compatible: "+type);
            }
        }
    }
    private static void property(Object raw,Kind kind,Budget budget)throws IOException{
        Map<String,Object> property=IconManagementJson.object(raw);IconManagementJson.only(property,"a","k","ix");int animated=property.containsKey("a")?IconManagementJson.integer(property.get("a"),0,1):0;if(property.containsKey("ix"))IconManagementJson.integer(property.get("ix"),0,65535);
        if(animated==0){data(property.get("k"),kind,budget);return;}List<Object> frames=IconManagementJson.array(property.get("k"));if(frames.size()<2||frames.size()>64)throw IconManagementJson.error("La cantidad de fotogramas clave no es válida");double last=Double.NEGATIVE_INFINITY;String firstValue=null;boolean changes=false;
        for(int index=0;index<frames.size();index++){
            if(++budget.keyframes>256)throw IconManagementJson.error("El total de fotogramas clave supera el límite");Map<String,Object> frame=IconManagementJson.object(frames.get(index));IconManagementJson.only(frame,"t","s","e","i","o","h","to","ti");double t=bounded(frame.get("t"),budget.ip,budget.op);if(t<=last)throw IconManagementJson.error("Los tiempos de los fotogramas clave deben aumentar");last=t;
            if(frame.containsKey("s"))keyData(frame.get("s"),kind,budget);else if(index!=frames.size()-1)throw IconManagementJson.error("Falta el valor inicial del fotograma clave");if(frame.containsKey("e"))keyData(frame.get("e"),kind,budget);if(frame.containsKey("h"))IconManagementJson.integer(frame.get("h"),0,1);
            for(String key:new String[]{"s","e"})if(frame.containsKey(key)){String value=IconManagementJson.encode(frame.get(key));if(firstValue==null)firstValue=value;else if(!firstValue.equals(value))changes=true;}
            for(String key:new String[]{"i","o"})if(frame.containsKey(key)){Map<String,Object> ease=IconManagementJson.object(frame.get(key));IconManagementJson.only(ease,"x","y");ease(ease.get("x"),0,1);ease(ease.get("y"),-16,16);}
            for(String key:new String[]{"to","ti"})if(frame.containsKey(key))point(frame.get(key),false);
        }
        if(changes)budget.animatedProperties++;
    }
    private static void ease(Object value,double min,double max)throws IOException{if(value instanceof List){List<Object> values=IconManagementJson.array(value);if(values.isEmpty()||values.size()>4)throw IconManagementJson.error("El parámetro de aceleración/desaceleración no es válido");for(Object number:values)bounded(number,min,max);}else bounded(value,min,max);}
    private static void keyData(Object value,Kind kind,Budget budget)throws IOException{if(kind==Kind.PATH){List<Object> paths=IconManagementJson.array(value);if(paths.size()!=1)throw IconManagementJson.error("La ruta del fotograma clave no es válida");data(paths.get(0),kind,budget);}else if(kind==Kind.SCALAR||kind==Kind.NONNEG||kind==Kind.OPACITY){List<Object> values=IconManagementJson.array(value);if(values.size()!=1)throw IconManagementJson.error("El escalar del fotograma clave no es válido");data(values.get(0),kind,budget);}else data(value,kind,budget);}
    private static void data(Object value,Kind kind,Budget budget)throws IOException{
        switch(kind){case SCALAR:bounded(value,-4096,4096);break;case NONNEG:bounded(value,0,4096);break;case OPACITY:bounded(value,0,100);break;case POINT:point(value,false);break;case SIZE:{List<Object> size=IconManagementJson.array(value);if(size.size()!=2)throw IconManagementJson.error("Las dimensiones de la animación deben ser bidimensionales");for(Object number:size)bounded(number,0,4096);break;}case SCALE:point(value,true);break;
            case COLOR:{List<Object> values=IconManagementJson.array(value);if(values.size()!=3&&values.size()!=4)throw IconManagementJson.error("El color de la animación no es válido");for(Object number:values)bounded(number,0,1);break;}
            case PATH:{Map<String,Object> path=IconManagementJson.object(value);IconManagementJson.only(path,"i","o","v","c");IconManagementJson.bool(path.get("c"));List<Object> vertices=IconManagementJson.array(path.get("v")),in=IconManagementJson.array(path.get("i")),out=IconManagementJson.array(path.get("o"));if(vertices.isEmpty()||vertices.size()>512||vertices.size()!=in.size()||vertices.size()!=out.size()||(budget.vertices+=vertices.size())>2048)throw IconManagementJson.error("La cantidad de vértices de la ruta de animación supera el límite o no coincide");for(List<Object> points:Arrays.asList(vertices,in,out))for(Object coordinate:points){List<Object> xy=IconManagementJson.array(coordinate);if(xy.size()!=2)throw IconManagementJson.error("La ruta de animación debe ser bidimensional");for(Object number:xy)bounded(number,-4096,4096);}break;}
        }
    }
    private static void point(Object value,boolean scale)throws IOException{List<Object> values=IconManagementJson.array(value);if(values.size()!=2&&values.size()!=3)throw IconManagementJson.error("Las coordenadas de la animación deben ser bidimensionales");for(int i=0;i<2;i++)bounded(values.get(i),-4096,4096);if(values.size()==3)bounded(values.get(2),scale?100:0,scale?100:0);}
}
