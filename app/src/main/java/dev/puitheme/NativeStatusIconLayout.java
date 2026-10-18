// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Per-slot placement data. Parsed once with settings; native Views and visibility remain authoritative. */
public final class NativeStatusIconLayout {
    public static final String KEY=NativeStatusIcons.LAYOUT,DEFAULT="{}";
    private static final Pattern NUMBER=Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    private static final String ERROR="系统提示图标排列格式不正确";
    private static final Snapshot EMPTY=new Snapshot(Collections.emptyMap());
    private NativeStatusIconLayout(){ }
    public static final class Placement {
        public final String side;
        /** Positive values sort first; zero preserves native order after those explicit positions. */
        public final int order;
        private Placement(String side,int order){this.side=side;this.order=order;}
    }
    public static final class Snapshot {
        private final Map<String,Placement> entries;
        private Snapshot(Map<String,Placement> entries){this.entries=Collections.unmodifiableMap(new LinkedHashMap<>(entries));}
        public boolean empty(){return entries.isEmpty();}
        public boolean specified(String slot){return entries.containsKey(slot);}
        public String side(String slot){Placement value=entries.get(slot);return value==null?StatusBarLayoutSettings.NATIVE:value.side;}
        public int order(String slot){Placement value=entries.get(slot);return value==null?0:value.order;}
        public Map<String,Placement> entries(){return entries;}
        public String serialize(){
            StringBuilder out=new StringBuilder("{");boolean comma=false;
            for(String slot:NativeStatusIcons.PRIORITY_SLOTS){
                Placement value=entries.get(slot);if(value==null)continue;
                if(comma)out.append(',');comma=true;
                out.append('"').append(slot).append("\":{\"side\":\"").append(value.side)
                        .append("\",\"order\":").append(value.order).append('}');
            }
            return out.append('}').toString();
        }
    }
    /** Damaged local preferences keep the established whole-group placement. */
    public static Snapshot parse(String source){try{return read(source);}catch(IllegalArgumentException invalid){return EMPTY;}}
    public static String validate(String source){return read(source).serialize();}
    public static String validationError(String source){try{read(source);return null;}catch(IllegalArgumentException invalid){return ERROR;}}
    private static Snapshot read(String source){
        if(source==null||source.length()>8192)throw new IllegalArgumentException(ERROR);
        return new Reader(source).document();
    }
    /** A narrow strict JSON grammar also rejects duplicates on Android's otherwise lenient JSON implementation. */
    private static final class Reader {
        final String source;int at;
        Reader(String source){this.source=source;}
        Snapshot document(){
            Map<String,Placement> values=new LinkedHashMap<>();expect('{');
            if(!take('}'))for(;;){
                String slot=string();boolean known=false;
                for(String candidate:NativeStatusIcons.PRIORITY_SLOTS)if(candidate.equals(slot)){known=true;break;}
                if(!known||values.containsKey(slot))throw invalid();
                expect(':');expect('{');String side=null;Integer order=null;
                if(take('}'))throw invalid();
                for(;;){
                    String key=string();expect(':');
                    if(key.equals("side")&&side==null){side=string();if(!StatusBarLayoutSettings.validSide(side))throw invalid();}
                    else if(key.equals("order")&&order==null)order=order();
                    else throw invalid();
                    if(take('}'))break;expect(',');
                }
                if(side==null||order==null)throw invalid();values.put(slot,new Placement(side,order));
                if(take('}'))break;expect(',');
            }
            space();if(at!=source.length())throw invalid();return values.isEmpty()?EMPTY:new Snapshot(values);
        }
        int order(){
            space();int begin=at;
            while(at<source.length()&&",} \t\r\n".indexOf(source.charAt(at))<0)at++;
            String value=source.substring(begin,at);if(value.length()>64||!NUMBER.matcher(value).matches())throw invalid();
            try{int result=new BigDecimal(value).intValueExact();if(result<0)throw invalid();return result;}
            catch(ArithmeticException|NumberFormatException malformed){throw invalid();}
        }
        String string(){
            space();if(at>=source.length()||source.charAt(at++)!='"')throw invalid();StringBuilder out=new StringBuilder();
            while(at<source.length()){
                char value=source.charAt(at++);if(value=='"')return out.toString();if(value<0x20)throw invalid();
                if(value!='\\'){out.append(value);continue;}
                if(at>=source.length())throw invalid();char escape=source.charAt(at++);
                switch(escape){
                    case '"':case '\\':case '/':out.append(escape);break;
                    case 'b':out.append('\b');break;case 'f':out.append('\f');break;case 'n':out.append('\n');break;
                    case 'r':out.append('\r');break;case 't':out.append('\t');break;
                    case 'u':
                        if(at+4>source.length())throw invalid();int code=0;
                        for(int i=0;i<4;i++){char hex=source.charAt(at++);int digit=hex<128?Character.digit(hex,16):-1;if(digit<0)throw invalid();code=code*16+digit;}
                        out.append((char)code);break;
                    default:throw invalid();
                }
            }
            throw invalid();
        }
        void space(){while(at<source.length()&&" \t\r\n".indexOf(source.charAt(at))>=0)at++;}
        boolean take(char value){space();if(at<source.length()&&source.charAt(at)==value){at++;return true;}return false;}
        void expect(char value){if(!take(value))throw invalid();}
        IllegalArgumentException invalid(){return new IllegalArgumentException(ERROR);}
    }
}
