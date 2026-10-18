package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Replays the captured QS resource rebind and ConstraintSet visibility writes without a framework setter interception. */
public final class QsEntrancesCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Button extends View {
        final String name;
        final NetworkBadgeControls controls;
        int draws,taps;String pkg="com.oplus.systemui";
        Button(String name,NetworkBadgeControls controls){super(null);this.name=name;this.controls=controls;}
        @Override public int getId(){return 100;}
        @Override public Resources getResources(){return new Resources(){
            @Override public String getResourceEntryName(int id){return name;}
            @Override public String getResourcePackageName(int id){return pkg;}
        };}
        @Override public void setVisibility(int value){super.setVisibility(controls.requestedVisibility(this,value));}
        void nativeWrite(int value){super.setVisibility(value);}
        void frame(){if(getVisibility()==VISIBLE){draws++;taps++;}}
    }
    private static final class Component {
        private View editBtn,editBtnRedDot,settingsButton,settingsButtonRemote,settingsButtonIcon;
        Component(NetworkBadgeControls controls){
            editBtn=new Button("qs_edit_button",controls);editBtnRedDot=new Button("edit_button_little_red_dot",controls);
            settingsButton=new Button("quicksettings_settings_button",controls);
            settingsButtonRemote=new Button("quicksettings_settings_button_remote",controls);
            settingsButtonIcon=new Button("quicksettings_settings_button_icon",controls);
        }
        public void onInit(){ }
        public void onViewAttached(){ }
        public void updateResource(boolean changed,Context context){if(changed)((Button)editBtn).nativeWrite(View.VISIBLE);}
        View[] views(){return new View[]{editBtn,editBtnRedDot,settingsButton,settingsButtonRemote,settingsButtonIcon};}
    }
    private static final class Update {
        final Object nativeUpdate;
        final Object access,owner;
        Update(Object nativeUpdate,Object access,Object owner){this.nativeUpdate=nativeUpdate;this.access=access;this.owner=owner;}
        void finish() throws Exception {
            if(nativeUpdate==null)return;
            try {
                Method capture=access==null?QsEntrances.class.getDeclaredMethod("capture",View.class,nativeUpdate.getClass())
                        :access.getClass().getDeclaredMethod("capture",Object.class,nativeUpdate.getClass());
                capture.setAccessible(true);capture.invoke(access,owner,nativeUpdate);
            }finally{Method method=nativeUpdate.getClass().getDeclaredMethod("finish");method.setAccessible(true);method.invoke(nativeUpdate);}
        }
    }
    // An absent binding is the old code102 hook path: run the native caller without a caller-specific guard.
    private static Update component(Component owner,NetworkBadgeControls controls) throws Exception {
        try {
            Method bind=QsEntrances.class.getDeclaredMethod("bind",Class.class);bind.setAccessible(true);
            Object access=bind.invoke(null,Component.class);
            Method begin=access.getClass().getDeclaredMethod("begin",Object.class,NetworkBadgeControls.class);begin.setAccessible(true);
            return new Update(begin.invoke(access,owner,controls),access,owner);
        }catch(NoSuchMethodException oldPath){return new Update(null,null,null);}
    }
    private static Update container(View root,NetworkBadgeControls controls) throws Exception {
        try {
            Method begin=QsEntrances.class.getDeclaredMethod("begin",View.class,NetworkBadgeControls.class);begin.setAccessible(true);
            return new Update(begin.invoke(null,root,controls),null,root);
        }catch(NoSuchMethodException oldPath){return new Update(null,null,null);}
    }
    public static void main(String[] args) throws Exception {
        NetworkBadgeControls controls=new NetworkBadgeControls(new Handler(Looper.getMainLooper()));
        Map<String,Object> values=new HashMap<>();values.put(QsEntrances.MASTER,true);values.put(QsEntrances.EDIT_HIDDEN,true);
        values.put(QsEntrances.SETTINGS_HIDDEN,true);controls.configure(FeatureOptions.from(values));
        Component owner=new Component(controls);for(View view:owner.views())controls.apply(view);
        Button edit=(Button)owner.editBtn,red=(Button)owner.editBtnRedDot;
        for(int theme=0;theme<32;theme++) {
            Update update=component(owner,controls);
            try {owner.updateResource(true,new Context());}finally{update.finish();}
            check(edit.getVisibility()==View.GONE,"native theme/resource update resurrected edit button "+theme);
            edit.frame();check(edit.draws==0&&edit.taps==0,"hidden edit still draws or receives a visible hit");
        }
        ViewGroup row=new ViewGroup(null);for(View view:owner.views())row.addView(view);
        Button other=new Button("qs_multi_user_button",controls);row.addView(other);
        Update outer=component(owner,controls);
        try {
            Update constraints=container(row,controls);
            try {edit.nativeWrite(View.VISIBLE);red.nativeWrite(View.INVISIBLE);other.nativeWrite(View.INVISIBLE);}finally{constraints.finish();}
            check(edit.getVisibility()==View.VISIBLE,"inner ConstraintSet guard captured module GONE instead of the outer native request");
        }finally{outer.finish();}
        check(edit.getVisibility()==View.GONE&&red.getVisibility()==View.GONE,"ConstraintSet re-arrangement leaks an entrance");
        check(other.getVisibility()==View.INVISIBLE,"unrelated user entry changed");
        values.put(QsEntrances.EDIT_HIDDEN,false);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.VISIBLE&&red.getVisibility()==View.INVISIBLE,"individual switch did not restore latest native requests");
        check(owner.settingsButton.getVisibility()==View.GONE,"edit switch restored settings button too");
        values.put(QsEntrances.EDIT_HIDDEN,true);controls.configure(FeatureOptions.from(values));
        Update nativeGone=component(owner,controls);
        try {edit.nativeWrite(View.GONE);}finally{nativeGone.finish();}
        values.put(QsEntrances.MASTER,false);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.GONE&&red.getVisibility()==View.INVISIBLE,"master off resurrected a natively absent entry");
        check(owner.settingsButton.getVisibility()==View.VISIBLE,"master off did not restore settings");
        values.put(QsEntrances.MASTER,true);controls.configure(FeatureOptions.from(values));
        Update intercepted=component(owner,controls);
        try {edit.setVisibility(View.INVISIBLE);}finally{intercepted.finish();}
        values.put(QsEntrances.EDIT_HIDDEN,false);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.INVISIBLE,"hooked native setter lost its latest requested visibility");
        values.put(QsEntrances.EDIT_HIDDEN,true);controls.configure(FeatureOptions.from(values));
        Update exceptional=component(owner,controls);
        try {edit.nativeWrite(View.VISIBLE);throw new IllegalStateException("native resource callback failure");}
        catch(IllegalStateException expected){ }
        finally {exceptional.finish();exceptional.finish();}
        check(edit.getVisibility()==View.GONE,"exception path leaked an edit entry");
        values.put(StatusBarSettings.SAFE_MODE,true);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.VISIBLE,"safe mode failed to restore native requested visibility");
        Button badge=new Button("mobile_type_5g",controls);row.addView(badge);badge.nativeWrite(View.INVISIBLE);
        Update onlyEntrances=container(row,controls);onlyEntrances.finish();
        check(badge.getVisibility()==View.INVISIBLE&&other.getVisibility()==View.INVISIBLE,"QS rebind mutated non-entrance views");
        check(row.getVisibility()==View.VISIBLE,"QS entry container was hidden");
        values.remove(StatusBarSettings.SAFE_MODE);controls.configure(FeatureOptions.from(values));
        Component late=new Component(controls);late.editBtn=null;
        Update initial=component(late,controls);
        try {late.editBtn=new Button("qs_edit_button",controls);}finally{initial.finish();}
        check(late.editBtn.getVisibility()==View.GONE,"onInit late field was not hidden");
        Button replacement=new Button("qs_edit_button",controls);
        Update replace=component(late,controls);
        try {late.editBtn=replacement;replacement.nativeWrite(View.INVISIBLE);}finally{replace.finish();}
        check(replacement.getVisibility()==View.GONE,"resource callback replacement was not hidden");
        values.put(QsEntrances.EDIT_HIDDEN,false);controls.configure(FeatureOptions.from(values));
        check(replacement.getVisibility()==View.INVISIBLE,"replacement native INVISIBLE was not restored");
        values.put(QsEntrances.EDIT_HIDDEN,true);controls.configure(FeatureOptions.from(values));
        Button appended=new Button("qs_edit_button",controls);Update freshTree=container(row,controls);
        try {row.addView(appended);}finally{freshTree.finish();}
        check(appended.getVisibility()==View.GONE,"ConstraintSet callback's new entrance escaped current tree capture");
        Button unknown=new Button("qs_edit_button_background",controls);Update unrelated=component(late,controls);
        try {late.editBtn=unknown;unknown.nativeWrite(View.INVISIBLE);}finally{unrelated.finish();}
        check(unknown.getVisibility()==View.INVISIBLE,"unrecognized replacement was hidden");
        check(QsEntrances.blockTouch(edit,controls)&&QsEntrances.blockTouch(appended,controls),"expanded TouchDelegate still dispatches to hidden edit");
        check(!QsEntrances.blockTouch(other,controls)&&!QsEntrances.blockTouch(unknown,controls),"unrelated spotlight touch was blocked");
        values.put(QsEntrances.EDIT_HIDDEN,false);controls.configure(FeatureOptions.from(values));
        check(!QsEntrances.blockTouch(edit,controls)&&!QsEntrances.blockTouch(appended,controls),"restored edit remains untouchable");
        clickableHidden();
        System.out.println("QsEntrancesCheck passed: "+checks);
    }
    /** "使隐藏可点击": hidden buttons keep their native visibility/place/touch and are only made fully transparent. */
    private static void clickableHidden() throws Exception {
        NetworkBadgeControls controls=new NetworkBadgeControls(new Handler(Looper.getMainLooper()));
        Map<String,Object> values=new HashMap<>();values.put(QsEntrances.MASTER,true);values.put(QsEntrances.EDIT_HIDDEN,true);
        values.put(QsEntrances.SETTINGS_HIDDEN,true);values.put(QsEntrances.HIDDEN_CLICKABLE,true);controls.configure(FeatureOptions.from(values));
        Component owner=new Component(controls);for(View view:owner.views())controls.apply(view);
        Button edit=(Button)owner.editBtn,settings=(Button)owner.settingsButton;
        for(View view:owner.views()){
            check(view.getVisibility()==View.VISIBLE,"clickable hidden button lost its native place");
            check(view.getTransitionAlpha()==0f,"clickable hidden button is still visible");
            check(controls.suppressDraw(view),"clickable hidden image still draws its content");
        }
        check(!QsEntrances.blockTouch(edit,controls),"clickable hidden edit blocks taps");
        edit.frame();check(edit.taps==1,"clickable hidden edit cannot be tapped");
        // The plugin's own resource rebinding and visibility writes keep it transparent and in place.
        for(int theme=0;theme<8;theme++){
            Update update=component(owner,controls);
            try {owner.updateResource(true,new Context());}finally{update.finish();}
            check(edit.getVisibility()==View.VISIBLE&&edit.getTransitionAlpha()==0f,"resource update showed the transparent edit "+theme);
        }
        edit.setTransitionAlpha(1f);controls.apply(edit);check(edit.getTransitionAlpha()==0f,"external transition reset not re-applied");
        Update gone=component(owner,controls);
        try {edit.nativeWrite(View.GONE);}finally{gone.finish();}
        check(edit.getVisibility()==View.GONE,"native GONE request was overridden");
        Update back=component(owner,controls);
        try {edit.nativeWrite(View.VISIBLE);}finally{back.finish();}
        // Turning the option off returns to the full hide; turning hiding off restores native transparency.
        values.put(QsEntrances.HIDDEN_CLICKABLE,false);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.GONE&&settings.getVisibility()==View.GONE,"option off did not fully hide");
        check(edit.getTransitionAlpha()==1f&&settings.getTransitionAlpha()==1f,"option off kept the module transparency");
        check(QsEntrances.blockTouch(edit,controls),"fully hidden edit must not receive taps");
        values.put(QsEntrances.HIDDEN_CLICKABLE,true);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.VISIBLE&&edit.getTransitionAlpha()==0f,"option on again did not keep the place");
        values.put(QsEntrances.EDIT_HIDDEN,false);controls.configure(FeatureOptions.from(values));
        check(edit.getVisibility()==View.VISIBLE&&edit.getTransitionAlpha()==1f,"edit not restored to native");
        check(settings.getTransitionAlpha()==0f,"settings switch was changed by the edit switch");
        values.put(QsEntrances.MASTER,false);controls.configure(FeatureOptions.from(values));
        check(settings.getVisibility()==View.VISIBLE&&settings.getTransitionAlpha()==1f,"master off did not restore settings");
        // Only the control centre's top buttons qualify: a hidden network badge is never kept in place.
        Map<String,Object> badgeValues=new HashMap<>(values);badgeValues.put("data_enabled",true);badgeValues.put("data_badge_hidden",true);
        controls.configure(FeatureOptions.from(badgeValues));Button badge=new Button("mobile_type_5g",controls);badge.pkg="com.android.systemui";controls.apply(badge);
        check(badge.getVisibility()==View.GONE&&badge.getTransitionAlpha()==1f,"network badge took the clickable option");
    }
}
