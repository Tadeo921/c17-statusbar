// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.os.Build;
import android.os.Bundle;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Portable left/right text components. Historical AppWidget choices stay importable, never reused by text. */
public final class BigClockWidgetsSettings {
    public static final String LEFT_ENABLED="notification_big_clock_widget_left_enabled";
    public static final String RIGHT_ENABLED="notification_big_clock_widget_right_enabled";
    public static final String LEFT_PROVIDER="notification_big_clock_widget_left_provider";
    public static final String RIGHT_PROVIDER="notification_big_clock_widget_right_provider";
    public static final String LEFT_HEIGHT="notification_big_clock_widget_left_height";
    public static final String RIGHT_HEIGHT="notification_big_clock_widget_right_height";
    public static final String LEFT_TEXT="notification_big_clock_widget_left_text";
    public static final String RIGHT_TEXT="notification_big_clock_widget_right_text";
    public static final String LEFT_SIZE="notification_big_clock_widget_left_size";
    public static final String RIGHT_SIZE="notification_big_clock_widget_right_size";
    public static final String LEFT_WEIGHT="notification_big_clock_widget_left_weight";
    public static final String RIGHT_WEIGHT="notification_big_clock_widget_right_weight";
    public static final String LEFT_OFFSET_X="notification_big_clock_widget_left_offset_x";
    public static final String RIGHT_OFFSET_X="notification_big_clock_widget_right_offset_x";
    public static final String LEFT_OFFSET_Y="notification_big_clock_widget_left_offset_y";
    public static final String RIGHT_OFFSET_Y="notification_big_clock_widget_right_offset_y";
    public static final String LEFT_FOLLOW_TIME_COLOR="notification_big_clock_widget_left_follow_time_color";
    public static final String RIGHT_FOLLOW_TIME_COLOR="notification_big_clock_widget_right_follow_time_color";
    public static final String LEFT_COLOR_LIGHT="notification_big_clock_widget_left_color_light";
    public static final String RIGHT_COLOR_LIGHT="notification_big_clock_widget_right_color_light";
    public static final String LEFT_COLOR_DARK="notification_big_clock_widget_left_color_dark";
    public static final String RIGHT_COLOR_DARK="notification_big_clock_widget_right_color_dark";
    public static final String LEFT_CLICK_ENABLED="notification_big_clock_widget_left_click_enabled";
    public static final String RIGHT_CLICK_ENABLED="notification_big_clock_widget_right_click_enabled";
    public static final String LEFT_LAUNCH_APP="notification_big_clock_widget_left_launch_app";
    public static final String RIGHT_LAUNCH_APP="notification_big_clock_widget_right_launch_app";
    public static final String LEFT_MASK_OPACITY="notification_big_clock_widget_left_mask_opacity";
    public static final String RIGHT_MASK_OPACITY="notification_big_clock_widget_right_mask_opacity";
    public static final String SWAP_WITH_DATE="notification_big_clock_widget_swap_with_date";
    public static final float DEFAULT_HEIGHT=96f, MIN_HEIGHT=32f, MAX_HEIGHT=400f;
    public static final float DEFAULT_SIZE=18f, MIN_SIZE=8f, MAX_SIZE=64f, MAX_OFFSET=1000f;
    public static final float DEFAULT_WEIGHT=400f, MIN_WEIGHT=100f, MAX_WEIGHT=1000f;
    public static final String DEFAULT_LEFT_TEXT="（天气）", DEFAULT_RIGHT_TEXT="（日期）";
    public static final int MAX_TEXT=240;
    public static final int LEFT=0, RIGHT=1;
    public static final Map<String,Boolean> BOOLEANS;
    public static final Map<String,Float> NUMBERS;
    public static final Map<String,String> STRINGS;
    public static final Map<String,Integer> COLORS;
    static {
        Map<String,Boolean> booleans=new LinkedHashMap<>();Map<String,Float> numbers=new LinkedHashMap<>();Map<String,String> strings=new LinkedHashMap<>();
        defaults(booleans,numbers,strings);BOOLEANS=Collections.unmodifiableMap(booleans);NUMBERS=Collections.unmodifiableMap(numbers);STRINGS=Collections.unmodifiableMap(strings);
        Map<String,Integer> colors=new LinkedHashMap<>();
        colors.put(LEFT_COLOR_LIGHT,0xff000000);colors.put(RIGHT_COLOR_LIGHT,0xff000000);
        colors.put(LEFT_COLOR_DARK,0xffffffff);colors.put(RIGHT_COLOR_DARK,0xffffffff);
        COLORS=Collections.unmodifiableMap(colors);
    }
    private BigClockWidgetsSettings() { }
    public static boolean supported(){return Build.VERSION.SDK_INT>=30;}
    public static void defaults(Map<String,Boolean> booleans,Map<String,Float> numbers,Map<String,String> strings){
        booleans.put(LEFT_ENABLED,false);booleans.put(RIGHT_ENABLED,false);
        booleans.put(SWAP_WITH_DATE,false);
        numbers.put(LEFT_HEIGHT,DEFAULT_HEIGHT);numbers.put(RIGHT_HEIGHT,DEFAULT_HEIGHT);
        strings.put(LEFT_PROVIDER,"");strings.put(RIGHT_PROVIDER,"");
        strings.put(LEFT_TEXT,DEFAULT_LEFT_TEXT);strings.put(RIGHT_TEXT,DEFAULT_RIGHT_TEXT);
        numbers.put(LEFT_SIZE,DEFAULT_SIZE);numbers.put(RIGHT_SIZE,DEFAULT_SIZE);
        numbers.put(LEFT_WEIGHT,DEFAULT_WEIGHT);numbers.put(RIGHT_WEIGHT,DEFAULT_WEIGHT);
        numbers.put(LEFT_OFFSET_X,0f);numbers.put(RIGHT_OFFSET_X,0f);
        numbers.put(LEFT_OFFSET_Y,0f);numbers.put(RIGHT_OFFSET_Y,0f);
        booleans.put(LEFT_FOLLOW_TIME_COLOR,true);booleans.put(RIGHT_FOLLOW_TIME_COLOR,true);
        booleans.put(LEFT_CLICK_ENABLED,false);booleans.put(RIGHT_CLICK_ENABLED,false);
        strings.put(LEFT_LAUNCH_APP,"");strings.put(RIGHT_LAUNCH_APP,"");
        numbers.put(LEFT_MASK_OPACITY,20f);numbers.put(RIGHT_MASK_OPACITY,20f);
    }
    public static String enabledKey(int side){checkSide(side);return side==LEFT?LEFT_ENABLED:RIGHT_ENABLED;}
    public static String providerKey(int side){checkSide(side);return side==LEFT?LEFT_PROVIDER:RIGHT_PROVIDER;}
    public static String heightKey(int side){checkSide(side);return side==LEFT?LEFT_HEIGHT:RIGHT_HEIGHT;}
    public static String textKey(int side){checkSide(side);return side==LEFT?LEFT_TEXT:RIGHT_TEXT;}
    public static String sizeKey(int side){checkSide(side);return side==LEFT?LEFT_SIZE:RIGHT_SIZE;}
    public static String weightKey(int side){checkSide(side);return side==LEFT?LEFT_WEIGHT:RIGHT_WEIGHT;}
    public static String offsetXKey(int side){checkSide(side);return side==LEFT?LEFT_OFFSET_X:RIGHT_OFFSET_X;}
    public static String offsetYKey(int side){checkSide(side);return side==LEFT?LEFT_OFFSET_Y:RIGHT_OFFSET_Y;}
    public static String followTimeColorKey(int side){checkSide(side);return side==LEFT?LEFT_FOLLOW_TIME_COLOR:RIGHT_FOLLOW_TIME_COLOR;}
    public static String clickEnabledKey(int side){checkSide(side);return side==LEFT?LEFT_CLICK_ENABLED:RIGHT_CLICK_ENABLED;}
    public static String launchAppKey(int side){checkSide(side);return side==LEFT?LEFT_LAUNCH_APP:RIGHT_LAUNCH_APP;}
    public static boolean launchAppKey(String key){return LEFT_LAUNCH_APP.equals(key)||RIGHT_LAUNCH_APP.equals(key);}
    public static String maskOpacityKey(int side){checkSide(side);return side==LEFT?LEFT_MASK_OPACITY:RIGHT_MASK_OPACITY;}
    public static boolean maskOpacityKey(String key){return LEFT_MASK_OPACITY.equals(key)||RIGHT_MASK_OPACITY.equals(key);}
    public static String colorKey(int side,boolean dark){checkSide(side);return side==LEFT?(dark?LEFT_COLOR_DARK:LEFT_COLOR_LIGHT):(dark?RIGHT_COLOR_DARK:RIGHT_COLOR_LIGHT);}
    public static boolean textKey(String key){return LEFT_TEXT.equals(key)||RIGHT_TEXT.equals(key);}
    public static boolean sizeKey(String key){return LEFT_SIZE.equals(key)||RIGHT_SIZE.equals(key);}
    public static boolean weightKey(String key){return LEFT_WEIGHT.equals(key)||RIGHT_WEIGHT.equals(key);}
    public static boolean offsetKey(String key){return LEFT_OFFSET_X.equals(key)||RIGHT_OFFSET_X.equals(key)||LEFT_OFFSET_Y.equals(key)||RIGHT_OFFSET_Y.equals(key);}
    public static int textSide(String key){
        for(int side:new int[]{LEFT,RIGHT})if(enabledKey(side).equals(key)||textKey(side).equals(key)||sizeKey(side).equals(key)||weightKey(side).equals(key)
                ||offsetXKey(side).equals(key)||offsetYKey(side).equals(key)||followTimeColorKey(side).equals(key)
                ||colorKey(side,false).equals(key)||colorKey(side,true).equals(key)
                ||clickEnabledKey(side).equals(key)||launchAppKey(side).equals(key)||maskOpacityKey(side).equals(key))return side;
        return -1;
    }
    public static String textValidationError(String text){
        if(text==null||text.length()>MAX_TEXT||text.indexOf('\n')>=0||text.indexOf('\r')>=0)return "请使用不超过 "+MAX_TEXT+" 个字符的单行文字";
        return BigClockTextContent.templateError(text);
    }
    /** Packages only: never an Intent URI or a flattened activity component. */
    public static String launchAppValidationError(String value) {
        if(value==null||value.length()>255)return "请选择有效应用";
        if(value.isEmpty())return null;
        boolean start=true;
        for(int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c=='.'){if(start)return "请选择有效应用";start=true;continue;}
            boolean letter=c>='a'&&c<='z'||c>='A'&&c<='Z'||c=='_';
            if(!letter&&(start||c<'0'||c>'9'))return "请选择有效应用";
            start=false;
        }
        return start?"请选择有效应用":null;
    }
    public static String numericValidationError(String key,float value){
        if(!Float.isFinite(value))return "请输入有限数字";
        if(sizeKey(key)&&(value<MIN_SIZE||value>MAX_SIZE))return "组件字号应为 8 到 64 dp";
        if(weightKey(key)&&(value<MIN_WEIGHT||value>MAX_WEIGHT))return "组件字体粗细应为 100 到 1000";
        if(offsetKey(key)&&(value<-MAX_OFFSET||value>MAX_OFFSET))return "组件位置应为 -1000 到 1000 dp";
        if(maskOpacityKey(key)&&(value<0f||value>100f))return "组件遮罩强度应为 0 到 100%";
        return null;
    }
    public static int side(String key){
        if(LEFT_PROVIDER.equals(key))return LEFT;if(RIGHT_PROVIDER.equals(key))return RIGHT;
        throw new IllegalArgumentException("Unknown clock widget slot");
    }
    public static void checkSide(int side){if(side!=LEFT&&side!=RIGHT)throw new IllegalArgumentException("Unknown clock widget slot");}
    public static float height(float value){return Float.isFinite(value)?Math.max(MIN_HEIGHT,Math.min(MAX_HEIGHT,value)):DEFAULT_HEIGHT;}
    public static String provider(Bundle values,int side){String value=values==null?null:values.getString(providerKey(side),"");return validProvider(value)?value:"";}
    /** Flattened components, never an arbitrary Intent or executable URI. Availability is a separate check. */
    public static boolean validProvider(String value){
        if(value==null||value.length()>512)return false;if(value.isEmpty())return true;
        int slash=value.indexOf('/');if(slash<1||slash==value.length()-1||value.indexOf('/',slash+1)>=0)return false;
        String pkg=value.substring(0,slash),name=value.substring(slash+1);
        if(pkg.indexOf('.')<1)return false;
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c!='/'&&c!='.'&&c!='_'&&c!='$'&&(c<'a'||c>'z')&&(c<'A'||c>'Z')&&(c<'0'||c>'9'))return false;}
        return pkg.charAt(0)!='.'&&pkg.charAt(pkg.length()-1)!='.'&&!pkg.contains("..")&&!name.equals(".")&&!name.contains("..");
    }
}
