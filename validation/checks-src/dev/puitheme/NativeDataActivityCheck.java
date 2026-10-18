package dev.puitheme;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
public final class NativeDataActivityCheck {
    private static int checks;
    private static void eq(Object expected,Object actual){checks++;if(!java.util.Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual);}
    private static final class Arrow extends Drawable {
        int calls,filters,tintCalls,drawnColor;ColorFilter filter;int alpha=255;boolean fail;
        android.content.res.ColorStateList colors;PorterDuff.Mode tintMode;BlendMode blendMode;
        public void draw(Canvas c){
            calls++;if(fail)throw new IllegalStateException("native failed");
            drawnColor=filter instanceof PorterDuffColorFilter?((PorterDuffColorFilter)filter).getColor()
                    :filter instanceof BlendModeColorFilter?((BlendModeColorFilter)filter).getColor()
                    :colors==null?0xffffffff:colors.getColorForState(getState(),colors.getDefaultColor());
        }
        public void setAlpha(int value){alpha=value;}
        public int getAlpha(){return alpha;}
        public void setColorFilter(ColorFilter value){filters++;filter=value;}
        public ColorFilter getColorFilter(){return filter;}
        public int getOpacity(){return -3;}
        public int getIntrinsicWidth(){return 12;}
        public int getIntrinsicHeight(){return 24;}
        public void setTintList(android.content.res.ColorStateList value){colors=value;tintCalls++;}
        public void setTintMode(PorterDuff.Mode value){tintMode=value;}
        public void setTintBlendMode(BlendMode value){blendMode=value;}
    }
    private static final class Res extends Resources {
        String name="stat_signal_stacked_activity_inout_public",pkg="com.android.systemui";
        public String getResourceEntryName(int id){return name;}
        public String getResourcePackageName(int id){return pkg;}
    }
    private static final class NativeStates extends android.content.res.ColorStateList {
        NativeStates(){super(new int[][]{{16842919},{}},new int[]{0xff000000,0xffffffff});}
        @Override public boolean isStateful(){return true;}
        @Override public int getColorForState(int[] state,int fallback){
            for(int value:state)if(value==16842919)return 0xff000000;
            return 0xffffffff;
        }
    }
    /** The shared desktop Drawable stub stores state only. Dispatch the exact
     * callback which Android's final state bookkeeping invokes afterward. */
    private static void nativeState(Drawable wrapper,int[] state)throws Exception {
        wrapper.setState(state);
        java.lang.reflect.Method changed=wrapper.getClass().getDeclaredMethod("onStateChange",int[].class);
        changed.setAccessible(true);changed.invoke(wrapper,(Object)state);
    }
    private static void nativeStateAndRestore()throws Exception {
        NativeDataActivity helper=new NativeDataActivity();Arrow source=new Arrow();
        Drawable wrapper=helper.wrap(new Res(),7,source);NativeStates states=new NativeStates();
        wrapper.setTintList(states);eq(true,wrapper.isStateful());wrapper.draw(new Canvas());eq(0xffffffff,source.drawnColor);
        nativeState(wrapper,new int[]{16842919});wrapper.draw(new Canvas());eq(0xff000000,source.drawnColor);
        eq(0xff000000,((NativeDataActivity.ArrowDrawable)wrapper).nativeTint);eq(states,source.colors);
        Bundle config=new Bundle();config.putBoolean(NativeDataActivity.MASTER,true);config.putBoolean(NativeDataActivity.COLOR_ENABLED,true);
        config.putInt(NativeDataActivity.COLOR_LIGHT,0xff112233);config.putInt(NativeDataActivity.COLOR_DARK,0xffabcdef);
        helper.configure(config);wrapper.draw(new Canvas());eq(0xff112233,source.drawnColor);
        nativeState(wrapper,new int[0]);wrapper.draw(new Canvas());eq(0xffabcdef,source.drawnColor);
        eq(0xffffffff,((NativeDataActivity.ArrowDrawable)wrapper).nativeTint);eq(states,source.colors);
        config.putBoolean(NativeDataActivity.COLOR_ENABLED,false);helper.configure(config);wrapper.draw(new Canvas());
        eq(null,source.filter);eq(0xffffffff,source.drawnColor);eq(states,source.colors);
        nativeState(wrapper,new int[]{16842919});wrapper.draw(new Canvas());eq(0xff000000,source.drawnColor);
        ColorFilter explicit=new BlendModeColorFilter(0xff246880,BlendMode.SRC_ATOP);
        wrapper.setColorFilter(explicit);config.putBoolean(NativeDataActivity.COLOR_ENABLED,true);helper.configure(config);wrapper.draw(new Canvas());
        helper.configure(null);wrapper.draw(new Canvas());eq(explicit,source.filter);eq(0xff246880,source.drawnColor);
        wrapper.setColorFilter(null);wrapper.draw(new Canvas());eq(null,source.filter);eq(0xff000000,source.drawnColor);
        wrapper.setTint(0xffffffff);wrapper.draw(new Canvas());eq(0xffffffff,source.drawnColor);
        helper.releaseRuntime();
    }
    private static void clearingFilterKeepsUnderlyingTint() {
        NativeDataActivity helper=new NativeDataActivity();Arrow source=new Arrow();source.draw(new Canvas());int original=source.drawnColor;
        Drawable wrapper=helper.wrap(new Res(),7,source);
        Bundle config=new Bundle();config.putBoolean(NativeDataActivity.MASTER,true);config.putBoolean(NativeDataActivity.COLOR_ENABLED,true);
        config.putInt(NativeDataActivity.COLOR_LIGHT,0xff112233);config.putInt(NativeDataActivity.COLOR_DARK,0xffabcdef);
        helper.configure(config);
        for(ColorFilter filter:new ColorFilter[]{new PorterDuffColorFilter(0xff000000,PorterDuff.Mode.SRC_IN),
                new BlendModeColorFilter(0xff000000,BlendMode.SRC_IN)}) {
            wrapper.setColorFilter(filter);wrapper.draw(new Canvas());eq(helper.tint(0xff000000),source.drawnColor);
            wrapper.setColorFilter(null);wrapper.draw(new Canvas());eq(helper.tint(original),source.drawnColor);
        }
        wrapper.setTint(0xff000000);wrapper.setColorFilter(new BlendModeColorFilter(0xffffffff,BlendMode.SRC_IN));
        wrapper.draw(new Canvas());eq(helper.tint(0xffffffff),source.drawnColor);
        wrapper.setColorFilter(null);wrapper.draw(new Canvas());eq(helper.tint(0xff000000),source.drawnColor);
        wrapper.setTintList(null);wrapper.draw(new Canvas());eq(helper.tint(original),source.drawnColor);
        helper.configure(null);wrapper.draw(new Canvas());eq(original,source.drawnColor);eq(null,source.filter);helper.releaseRuntime();
    }
    public static void main(String[] args)throws Exception {
        NativeDataActivity helper=new NativeDataActivity();Res res=new Res();res.getDisplayMetrics().density=2f;Arrow nativeArrow=new Arrow();
        Drawable wrapped=helper.wrap(res,7,nativeArrow);eq(true,wrapped instanceof NativeDataActivity.ArrowDrawable);
        eq(wrapped,helper.wrap(res,7,wrapped));eq(12,wrapped.getIntrinsicWidth());eq(24,wrapped.getIntrinsicHeight());
        res.name="stat_signal_activity_wifi_inout";eq(nativeArrow,helper.wrap(res,7,nativeArrow));
        res.name="stat_signal_stacked_activity_inout_public";res.pkg="other.package";eq(nativeArrow,helper.wrap(res,7,nativeArrow));res.pkg="com.android.systemui";
        for(String variant:new String[]{"default","in","out","inout"})for(String family:new String[]{"activity","activity_soft","soft_stacked_activity","stacked_activity"})
            eq(true,NativeDataActivity.source("stat_signal_"+family+"_"+variant+"_public_os17"));
        eq(false,helper.enabled());Canvas off=new Canvas();wrapped.setBounds(0,0,12,24);wrapped.draw(off);eq(0f,off.translateX);eq(1f,off.scaleX);
        Bundle config=new Bundle();config.putBoolean(NativeDataActivity.MASTER,true);config.putFloat(NativeDataActivity.X,4f);config.putFloat(NativeDataActivity.Y,-3f);config.putFloat(NativeDataActivity.SCALE,150f);
        config.putBoolean(NativeDataActivity.COLOR_ENABLED,true);config.putInt(NativeDataActivity.COLOR_LIGHT,0xffff0000);config.putInt(NativeDataActivity.COLOR_DARK,0xffff0000);
        helper.configure(config);Canvas styled=new Canvas();wrapped.draw(styled);eq(8f,styled.translateX);eq(-6f,styled.translateY);eq(1.5f,styled.scaleX);eq(6f,styled.pivotX);eq(12f,styled.pivotY);
        eq(0xffff0000,((PorterDuffColorFilter)nativeArrow.filter).getColor());int writes=nativeArrow.filters;
        for(int i=0;i<1000;i++)wrapped.draw(new Canvas());eq(writes,nativeArrow.filters);
        ((NativeDataActivity.ArrowDrawable)wrapped).composePlacement=true;Canvas compose=new Canvas();wrapped.draw(compose);eq(0f,compose.translateX);eq(1f,compose.scaleX);eq(null,nativeArrow.filter);
        ((NativeDataActivity.ArrowDrawable)wrapped).composePlacement=false;ColorFilter systemFilter=new PorterDuffColorFilter(0xff000000,PorterDuff.Mode.SRC_IN);wrapped.setColorFilter(systemFilter);wrapped.draw(new Canvas());
        helper.configure(null);wrapped.draw(new Canvas());eq(systemFilter,nativeArrow.filter);eq(false,helper.enabled());
        // ImageView tint setters must cross the wrapper, even with all adjustments OFF.
        wrapped.setColorFilter(null);wrapped.setTint(0xff000000);eq(0xff000000,nativeArrow.colors.getDefaultColor());
        eq(0xff000000,((NativeDataActivity.ArrowDrawable)wrapped).nativeTint);
        wrapped.setTint(0xffffffff);eq(0xffffffff,nativeArrow.colors.getDefaultColor());
        wrapped.setTintMode(PorterDuff.Mode.SRC_IN);eq(PorterDuff.Mode.SRC_IN,nativeArrow.tintMode);
        wrapped.setTintBlendMode(BlendMode.SRC_ATOP);eq(BlendMode.SRC_ATOP,nativeArrow.blendMode);
        // Modern BlendMode filters feed the same light/dark custom-color selection.
        config.putInt(NativeDataActivity.COLOR_LIGHT,0xff112233);config.putInt(NativeDataActivity.COLOR_DARK,0xffabcdef);helper.configure(config);
        wrapped.setColorFilter(new BlendModeColorFilter(0xff000000,BlendMode.SRC_IN));wrapped.draw(new Canvas());eq(0xff112233,((PorterDuffColorFilter)nativeArrow.filter).getColor());
        wrapped.setColorFilter(new BlendModeColorFilter(0xffffffff,BlendMode.SRC_IN));wrapped.draw(new Canvas());eq(0xffabcdef,((PorterDuffColorFilter)nativeArrow.filter).getColor());
        wrapped.setColorFilter(null);wrapped.setTint(0xff000000);wrapped.draw(new Canvas());eq(0xff112233,((PorterDuffColorFilter)nativeArrow.filter).getColor());
        wrapped.setTint(0xffffffff);wrapped.draw(new Canvas());eq(0xffabcdef,((PorterDuffColorFilter)nativeArrow.filter).getColor());
        helper.configure(null);wrapped.draw(new Canvas());eq(null,nativeArrow.filter);eq(0xffffffff,nativeArrow.colors.getDefaultColor());
        helper.configure(config);config.putBoolean(StatusBarSettings.SAFE_MODE,true);helper.configure(config);eq(false,helper.enabled());eq(0f,helper.x(2f));eq(1f,helper.scale(24f));
        nativeArrow.fail=true;Canvas failed=new Canvas();try{wrapped.draw(failed);throw new AssertionError("missing native error");}catch(IllegalStateException expected){eq(failed.saves,failed.restores);}
        helper.releaseRuntime();eq(false,helper.enabled());
        nativeStateAndRestore();
        clearingFilterKeepsUnderlyingTint();
        System.out.println("NativeDataActivityCheck passed: "+checks);
    }
}
