// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie

package dev.puitheme;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** Remembers the system's requested visibility so each hiding switch is reversible. */
final class NetworkBadgeControls {
    private final Map<View, State> views = new WeakHashMap<>();
    private final Handler main;
    private FeatureOptions options;
    private boolean applying;
    private static final class State {
        final String key;
        final boolean contextual;
        int requested;
        boolean hidden;
        int qsUpdates;
        /** Transparent but still laid out and touchable ("使隐藏可点击"); the native transition alpha to give back. */
        boolean faded;
        float transition = 1f;
        State(String key, int requested,boolean contextual) { this.key=key; this.requested=requested;this.contextual=contextual; }
    }
    NetworkBadgeControls(Handler main) { this.main=main; }
    static String keyFor(String name) {
        if (name == null) return null;
        String entrance = QsEntrances.keyFor(name);
        if (entrance != null) return entrance;
        if (name.startsWith("mobile_type")) return "data_badge_hidden";
        switch(name) {
            case "wifi_left": case "wifi_left_tv": return "wifi_badge_hidden";
            case "wifi_in": case "wifi_out": case "wifi_inout":
            case "wifi_activity": case "wifi_activity_container": return "wifi_activity_hidden";
            case "mobile_in": case "mobile_out": case "mobile_inout":
            case "mobile_activity": case "mobile_activity_container":
            case "data_inout": case "data_inout_alone": return "data_activity_hidden";
            default: return null;
        }
    }
    private State state(View view) {
        // The independently enabled label owns this exact native TextView. The native
        // badge hiding switch still applies to all other nodes, including Tigo images.
        if (SingleNetworkLabelControls.managed(view)) { views.remove(view); return null; }
        if (view == null || view.getId() <= 0) return null;
        State state=views.get(view);
        if(state!=null) {
            if(!state.contextual||WifiPlacement.belongsToWifi(view))return state;
            views.remove(view);restore(view,state);return null;
        }
        try {
            String name=view.getResources().getResourceEntryName(view.getId());
            // The control centre's top buttons come from the QS plugin, whose resources are not SystemUI's own; their names are
            // specific enough to be matched in any package (code100: they were never found, so never fully hidden).
            if(QsEntrances.keyFor(name)==null&&!"com.android.systemui".equals(view.getResources().getResourcePackageName(view.getId())))return null;
            String key=keyFor(name);boolean contextual=key==null;
            if(contextual)key=WifiPlacement.contextualKey(view,name);
            if(key==null)return null;
            state=new State(key,view.getVisibility(),contextual);views.put(view,state);return state;
        }catch(Exception ignored){return null;}
    }
    private boolean enabled(State state) {
        if(state==null)return false;
        String group=state.key.startsWith("wifi_")?"wifi":state.key.startsWith("qs_")?QsEntrances.GROUP:"data";
        return options==null || options.effective(group,state.key)
                || "data_badge_hidden".equals(state.key) && options.hideNetworkLabel();
    }
    /** Only the control centre's top buttons offer it; a hidden radio badge never keeps its place. */
    private boolean clickable(State state) {
        return state!=null&&state.key.startsWith("qs_")&&options!=null&&options.effective(QsEntrances.GROUP,QsEntrances.HIDDEN_CLICKABLE);
    }
    boolean touchable(View view){State state=state(view);return enabled(state)&&clickable(state);}
    int requestedVisibility(View view,int visibility) {
        State state=state(view);
        if(state==null || applying)return visibility;
        state.requested=visibility;
        if(state.qsUpdates>0)return visibility;
        state.hidden=enabled(state);
        return state.hidden&&!clickable(state)?View.GONE:visibility;
    }
    private void restore(View view,State state) {
        if(state.hidden&&view.getVisibility()!=state.requested) {
            boolean wasApplying=applying;applying=true;
            try{view.setVisibility(state.requested);}finally{applying=wasApplying;}
        }
        fade(view,state,false);
    }
    private static void fade(View view,State state,boolean faded) {
        if(faded) {
            if(!state.faded){state.transition=view.getTransitionAlpha();state.faded=true;}
            if(view.getTransitionAlpha()!=0f)view.setTransitionAlpha(0f);
        } else if(state.faded) {
            state.faded=false;
            if(view.getTransitionAlpha()==0f)view.setTransitionAlpha(state.transition);
        }
    }
    void idChanged(View view){State previous=views.remove(view);if(previous!=null)restore(view,previous);apply(view);}
    boolean suppressDraw(View view){return enabled(state(view));}
    // Only the concrete QS resource/ConstraintSet callbacks use this boundary. A module-produced GONE must not be
    // mistaken for a native GONE request when compiled visibility writes bypass the general setter hook.
    boolean beginQsUpdate(View view) {
        State state=state(view);
        if(state==null||!state.key.startsWith("qs_"))return false;
        if(state.qsUpdates++==0)restore(view,state);
        return true;
    }
    void endQsUpdate(View view) {
        State state=views.get(view);
        if(state==null||state.qsUpdates<=0)return;
        if(--state.qsUpdates==0){state.requested=view.getVisibility();apply(view);}
    }
    void apply(View view) {
        State state=state(view);if(state==null||state.qsUpdates>0)return;
        boolean hide=enabled(state),keep=hide&&clickable(state);
        int target=hide&&!keep?View.GONE:state.requested;
        if(view.getVisibility()!=target) {
            applying=true;try{view.setVisibility(target);}finally{applying=false;}
        }
        fade(view,state,keep);
        if(state.hidden!=hide){view.invalidate();view.requestLayout();}
        state.hidden=hide;
    }
    void tree(View view) {
        if(view==null)return;apply(view);
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)tree(group.getChildAt(i));
        }
    }
    void configure(FeatureOptions options) {
        this.options=options;
        if(Looper.myLooper()!=main.getLooper()){main.post(this::refresh);return;}
        refresh();
    }
    void refresh(){for(View view:new ArrayList<>(views.keySet()))if(view!=null)apply(view);}
}
