package dev.puitheme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

/** Native null-manager resource material, private copies and unknown-image safety. */
public final class FluidCloudFallbackCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static GradientDrawable gradient(int color){
        GradientDrawable result=new GradientDrawable();result.setColor(color);result.setStroke(3,0x60ffffff);
        result.setCornerRadius(39f);result.setBounds(4,6,104,76);result.setAlpha(137);result.setLevel(42);return result;
    }
    public static void main(String[] args){
        GradientDrawable original=gradient(0xb3262626);
        Drawable unmodified=FluidCloudFallback.derive(original,null,null,false,false);
        check(unmodified==original,"disabled derivation does not clone");
        GradientDrawable tint=(GradientDrawable)FluidCloudFallback.derive(original,null,0xffd4798e,false,false);
        check(tint!=original&&tint.getConstantState()!=original.getConstantState(),"host gets a private constant state");
        check(tint.getColor().getDefaultColor()==0xb3d4798e,"native fill alpha retained");
        check(original.getColor().getDefaultColor()==0xb3262626,"shared resource untouched");
        original.setTint(0xff000000);
        GradientDrawable privateTint=(GradientDrawable)FluidCloudFallback.material(original,null,0x8044aabb,false,false);
        check(privateTint.tintForCheck==null&&original.tintForCheck!=null,"native black tint cannot hide replacement fill; original retained");
        check(privateTint.getColor().getDefaultColor()==0x5a44aabb,"configured alpha remains single-pass on tinted source");
        check(tint.getAlpha()==137&&tint.getLevel()==42&&tint.getBounds().left==4,"runtime alpha geometry level retained");
        check(tint.getCornerRadius()==39f&&tint.mStrokePaint.strokeWidth==3,"native radius and stroke retained by accent");
        GradientDrawable flat=(GradientDrawable)FluidCloudFallback.derive(tint,null,null,true,true);
        check(flat.getColor().getDefaultColor()==0x00d4798e&&flat.mStrokePaint.strokeWidth==0,"base and highlight removed independently");
        check(tint.mStrokePaint.strokeWidth==3&&tint.getColor().getDefaultColor()==0xb3d4798e,"prior private accent copy also unchanged");
        GradientDrawable multi=gradient(0xff111111);multi.setColors(new int[]{0x00445566,0x88223344,0xff000000});
        LayerDrawable tree=new LayerDrawable(new Drawable[]{multi,new LayerDrawable(new Drawable[]{gradient(0x40123456)})});
        LayerDrawable tinted=(LayerDrawable)FluidCloudFallback.derive(tree,null,0xff557799,true,false);
        check(tinted!=tree,"nested native gradient tree supported");
        check(tinted.getDrawable(0).getAlpha()==137&&((LayerDrawable)tinted.getDrawable(1)).getDrawable(0).getAlpha()==137,"container alpha does not overwrite per-leaf animation alpha");
        int[] colors=((GradientDrawable)tinted.getDrawable(0)).getColors();
        check(colors[0]==0x00445566&&colors[1]==0x88557799&&colors[2]==0xff557799,"all gradient alpha stops preserved including invisible RGB");
        check(multi.getColors()[1]==0x88223344&&multi.mStrokePaint.strokeWidth==3,"nested source states untouched");
        Drawable image=new Drawable(){public void draw(Canvas c){}public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return 0;}};
        LayerDrawable album=new LayerDrawable(new Drawable[]{original,image});
        check(FluidCloudFallback.derive(album,null,0xff112233,true,true)==album,"unknown image layer excludes whole material");
        Drawable aliased=new GradientDrawable(){public ConstantState getConstantState(){return new ConstantState(){public Drawable newDrawable(){return aliasedSource();}};}private Drawable aliasedSource(){return this;}};
        check(FluidCloudFallback.derive(aliased,null,0xff112233,true,true)==aliased,"shared drawable identity is never mutated");
        check(original.getColor().getDefaultColor()==0xb3262626&&original.mStrokePaint.strokeWidth==3,"failed derivations leave native material intact");
        System.out.println("FluidCloudFallbackCheck: "+checks+" checks passed");
    }
}
