// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** The two buttons at the top of the control centre: "编辑" on the left (qs_edit_button, with its red dot) and "设置" on the right
 * (quicksettings_settings_button). Hidden means the native view is GONE: not drawn and not touchable; the visibility the system
 * asks for is remembered and given back when the switch goes off (NetworkBadgeControls). Nothing else in the header moves.
 * With "使隐藏可点击" a hidden button keeps its native visibility, place and touch handling and is only made fully transparent
 * (transition alpha 0, which the plugin's own alpha animations never write), so tapping where it was still opens it. */
public final class QsEntrances {
    public static final String GROUP = "qs_entrances";
    public static final String MASTER = "qs_entrances_enabled";
    public static final String EDIT_HIDDEN = "qs_edit_button_hidden";
    public static final String SETTINGS_HIDDEN = "qs_settings_button_hidden";
    public static final String HIDDEN_CLICKABLE = "qs_hidden_buttons_clickable";
    static final String COMPONENT = "com.oplus.systemui.plugins.qs.quickentrance.OplusQSQuickEntranceComponent";
    static final String CONTAINER = "com.oplus.systemui.plugins.qs.quickentrance.ui.view.OplusQSQuickEntranceContainerView";
    static final String BUTTON = "com.oplus.systemui.qs.base.spotlight.SpotLightImageButton";

    /** The setting that hides a native control-centre view, by its resource entry name (null: not one of them). */
    static String keyFor(String name) {
        if ("qs_edit_button".equals(name) || "edit_button_little_red_dot".equals(name)) return EDIT_HIDDEN;
        // The button, its spotlight layer and its icon: whatever path the plugin uses to show one of them, none is drawn.
        if ("quicksettings_settings_button".equals(name) || "quicksettings_settings_button_remote".equals(name)
                || "quicksettings_settings_button_icon".equals(name)) return SETTINGS_HIDDEN;
        return null;
    }
    static boolean blockTouch(View view,NetworkBadgeControls controls) {
        if(view==null||view.getId()<=0)return false;
        try{return EDIT_HIDDEN.equals(keyFor(view.getResources().getResourceEntryName(view.getId())))&&controls.suppressDraw(view)&&!controls.touchable(view);}
        catch(Exception missing){return false;}
    }

    /** The captured resource callback resets editBtn to VISIBLE. Its compiled setter may bypass the framework hook. */
    static Access bind(Class<?> owner) throws ReflectiveOperationException {
        Field[] fields=new Field[5];String[] names={"editBtn","editBtnRedDot","settingsButton","settingsButtonRemote","settingsButtonIcon"};
        for(int i=0;i<names.length;i++) {
            fields[i]=owner.getDeclaredField(names[i]);
            if(!View.class.isAssignableFrom(fields[i].getType()))throw new NoSuchFieldException(names[i]+" is not a View");
            fields[i].setAccessible(true);
        }
        return new Access(fields,new Method[]{owner.getDeclaredMethod("onInit"),owner.getDeclaredMethod("onViewAttached"),
                owner.getDeclaredMethod("updateResource",Boolean.TYPE,Context.class)});
    }
    static final class Access {
        private final Field[] fields;
        final Method[] events;
        Access(Field[] fields,Method[] events){this.fields=fields;this.events=events;}
        Update begin(Object owner,NetworkBadgeControls controls) {
            Update update=new Update(controls);capture(owner,update);return update;
        }
        void capture(Object owner,Update update) {
            for(Field field:fields)try {update.add((View)field.get(owner));}catch(IllegalAccessException ignored){ }
        }
    }
    static Update begin(View root,NetworkBadgeControls controls) {
        Update update=new Update(controls);collect(root,update);return update;
    }
    static void capture(View root,Update update){collect(root,update);}
    private static void collect(View view,Update update) {
        if(view==null)return;update.add(view);
        if(view instanceof ViewGroup) {
            ViewGroup parent=(ViewGroup)view;
            for(int i=0;i<parent.getChildCount();i++)collect(parent.getChildAt(i),update);
        }
    }
    /** Cold resource/layout updates temporarily own visibility; finally restores the hiding policy before the next frame. */
    static final class Update {
        private final NetworkBadgeControls controls;
        private final List<View> views=new ArrayList<>();
        private boolean finished;
        Update(NetworkBadgeControls controls){this.controls=controls;}
        void add(View view) {
            if(!finished&&view!=null&&!views.contains(view)&&controls.beginQsUpdate(view))views.add(view);
        }
        void finish() {
            if(finished)return;finished=true;
            for(View view:views)controls.endQsUpdate(view);
        }
    }

    private QsEntrances() { }
}
