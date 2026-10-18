// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

/** Immutable visible-pixel bounds. Scan only while decoding; fitting never reads pixels. */
public final class IconVisibleBounds {
    public final int left,top,right,bottom,width,height;
    private IconVisibleBounds(int left,int top,int right,int bottom){this.left=left;this.top=top;this.right=right;this.bottom=bottom;width=right-left;height=bottom-top;}
    /** Every nonzero alpha counts, including deliberately faint artwork edges. */
    public static IconVisibleBounds scan(int[] argb,int width,int height){
        if(argb==null||width<1||height<1||(long)width*height>argb.length)return null;
        int left=width,top=height,right=0,bottom=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if((argb[y*width+x]>>>24)!=0){
            if(x<left)left=x;if(y<top)top=y;if(x+1>right)right=x+1;if(y+1>bottom)bottom=y+1;
        }
        return left<right&&top<bottom?new IconVisibleBounds(left,top,right,bottom):null;
    }
    /** Match native glyph height; constrain width when preserving the aspect ratio requires it. */
    public float fitScale(float availableWidth,float availableHeight){
        if(!Float.isFinite(availableWidth)||!Float.isFinite(availableHeight)||availableWidth<=0||availableHeight<=0)return 0;
        float scale=Math.min(availableHeight/height,availableWidth/width);
        return Float.isFinite(scale)&&scale>0?scale:0;
    }
}
