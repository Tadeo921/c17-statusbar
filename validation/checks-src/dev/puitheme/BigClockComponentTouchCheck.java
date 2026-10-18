package dev.puitheme;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/** A real root→header→row→TextView DOWN/UP path with parent rectangle hit
 * tests. No performClick shortcut is used to establish touch reachability. */
public final class BigClockComponentTouchCheck {
    private static int checks;
    private static void check(boolean result,String why){checks++;if(!result)throw new AssertionError(why);}
    private static int inset(BigClockTextComponents row)throws Exception {
        try {return ((Number)BigClockTextComponents.class.getDeclaredMethod("topInset").invoke(row)).intValue();}
        catch(NoSuchMethodException old171){return 0;}
    }
    private static void layout(FrameLayout root,FrameLayout header,BigClockTextComponents row)throws Exception {
        int inset=inset(row),nominal=100,reserved=Math.round(row.desiredHeight());
        int headerTop=Math.min(50,nominal-inset);
        root.layout(0,0,400,400);header.layout(0,headerTop,400,nominal+reserved);
        row.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(reserved+inset,View.MeasureSpec.EXACTLY));
        row.layout(0,nominal-headerTop-inset,400,nominal-headerTop+reserved);row.onLayout(true,0,0,400,reserved+inset);row.updateClickTargets();
    }
    private static boolean tap(FrameLayout root,float x,float y) {
        boolean down=root.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN,x,y,1000));
        boolean up=root.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_UP,x,y,1050));return down&&up;
    }
    public static void main(String[] args)throws Exception {
        boolean padding=View.keepPaddingForCheck,measure=TextView.measureTextWidth;View.keepPaddingForCheck=TextView.measureTextWidth=true;
        try {
            Context context=new Context();FrameLayout root=new FrameLayout(context),header=new FrameLayout(context);
            BigClockTextComponents row=new BigClockTextComponents(context);root.addView(header);header.addView(row);
            row.getResources().getDisplayMetrics().density=1f;
            root.useLayoutBoundsForCheck=header.useLayoutBoundsForCheck=row.useLayoutBoundsForCheck=true;
            Bundle values=new Bundle();values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_TEXT,"LEFT");values.putString(BigClockWidgetsSettings.RIGHT_TEXT,"RIGHT");
            values.putBoolean(BigClockWidgetsSettings.LEFT_CLICK_ENABLED,true);values.putBoolean(BigClockWidgetsSettings.RIGHT_CLICK_ENABLED,true);
            values.putString(BigClockWidgetsSettings.LEFT_LAUNCH_APP,"selected.left");values.putString(BigClockWidgetsSettings.RIGHT_LAUNCH_APP,"selected.right");
            values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-20f);values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,-20f);
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_X,-250f);
            row.configure(values);row.refresh(0L,java.util.TimeZone.getTimeZone("UTC"));
            TextView left=(TextView)row.getChildAt(0),right=(TextView)row.getChildAt(1);left.useLayoutBoundsForCheck=right.useLayoutBoundsForCheck=true;
            int[] clicks={0},side={-1};row.setOnComponentClickListener(new BigClockTextComponents.OnComponentClickListener(){
                public boolean isComponentClickAllowed(View view,int which,String pkg){return true;}
                public void onComponentClick(View view,int which,String pkg){clicks[0]++;side[0]=which;}
            });row.updateClickTargets();layout(root,header,row);
            check(tap(root,1f,81f)&&clicks[0]==1&&side[0]==BigClockWidgetsSettings.LEFT,"visible negative-Y text is reached through its real parent dispatch rather than falling through above the row");
            check(inset(row)==20&&row.getTop()==30&&left.getTop()==0,"row upper geometry includes negative text while child placement is compensated");
            check(row.desiredHeight()==32f&&header.getBottom()==132,"negative-Y touch support preserves the existing notification bottom reservation");
            check(tap(root,right.getLeft()+1f,81f)&&clicks[0]==2&&side[0]==BigClockWidgetsSettings.RIGHT,"right text may cross its half-row and still dispatch to its own app");
            check(!tap(root,250f,81f)&&clicks[0]==2,"transparent space in the expanded row does not intercept a notification touch");
            values.putFloat(BigClockWidgetsSettings.LEFT_OFFSET_Y,-90f);row.configure(values);layout(root,header,row);
            check(tap(root,1f,11f)&&clicks[0]==3&&header.getTop()==10,"extreme negative Y also expands the header parent geometry, preserving the visible text's touch path");
            values.putBoolean(BigClockWidgetsSettings.LEFT_ENABLED,false);values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_X,-380f);row.configure(values);layout(root,header,row);
            check(right.getLeft()<0&&tap(root,1f,81f)&&clicks[0]==4&&side[0]==BigClockWidgetsSettings.RIGHT,"the visible part of a horizontally clipped right component keeps its correct side after the left is removed");
            check(!tap(root,-1f,81f)&&clicks[0]==4,"pixels outside the actual viewport never become clickable");
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_X,500f);row.configure(values);layout(root,header,row);
            check(!tap(root,399f,81f)&&clicks[0]==4,"a wholly offscreen component creates no viewport hit region");
            right.performClick();check(!right.isClickable()&&clicks[0]==4,"manual or accessibility clicks also reject a text view wholly outside the row's actual drawing viewport");
            values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_X,0f);values.putFloat(BigClockWidgetsSettings.RIGHT_OFFSET_Y,0f);row.configure(values);layout(root,header,row);
            check(inset(row)==0&&row.getTop()==50&&row.getMeasuredHeight()==32,"zero Y preserves the original row geometry without an added inset");
            row.close();check(!tap(root,399f,101f)&&clicks[0]==4,"closed rows release every real touch target");
        } finally {View.keepPaddingForCheck=padding;TextView.measureTextWidth=measure;}
        System.out.println("BigClockComponentTouchCheck: "+checks+" checks passed");
    }
}
