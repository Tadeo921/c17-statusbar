package dev.puitheme;

import java.util.Arrays;

/** The pull-down wallpaper's pixel maths: blur, zone blend, brightness, the system's pull curve and the two-layer alpha decomposition. */
public final class WallpaperPixelsCheck {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError("WallpaperPixels "+checks+": "+why);}
    private static void close(float expected,float actual,float tolerance,String why){check(Math.abs(expected-actual)<=tolerance,why+" ("+expected+" vs "+actual+")");}
    private static int grey(int value){return 0xff000000|value<<16|value<<8|value;}

    public static void main(String[] args){
        blur();zone();brightness();curve();layers();zoneAdjustments();
        System.out.println("WallpaperPixelsCheck passed: "+checks);
    }

    private static void zoneAdjustments(){
        // lift: positive toward white, negative toward black, alpha untouched, zero and bad input change nothing
        int[] px={0xff204060,0x80ffffff,0xff000000};int[] copy=px.clone();
        WallpaperPixels.lift(px,0f);check(Arrays.equals(px,copy),"no lift changes nothing");
        WallpaperPixels.lift(px,Float.NaN);WallpaperPixels.lift(null,50f);check(Arrays.equals(px,copy),"a bad lift changes nothing");
        WallpaperPixels.lift(px,50f);
        check((px[0]>>>24)==255&&((px[0]>>16)&255)==Math.round(0x20*.5f+255*.5f)&&(px[1]>>>24)==0x80,"half a positive lift is half way to white, alpha kept");
        check((px[2]&0xffffff)==0x808080,"black lifted by half is mid grey");
        int[] dark={0xff808080,0xffffffff};WallpaperPixels.lift(dark,-50f);
        check((dark[0]&255)==64&&(dark[1]&255)==128,"a negative lift goes toward black");
        int[] full={0xff102030};WallpaperPixels.lift(full,100f);check((full[0]&0xffffff)==0xffffff,"a full lift is white");
        WallpaperPixels.lift(full,-100f);check((full[0]&0xffffff)==0,"darkened by 100 afterwards: black, the second works on what is there");
        int[] black={0xff102030};WallpaperPixels.lift(black,-100f);check((black[0]&0xffffff)==0,"a full negative lift is black");
        // saturation: 100 keeps, 0 is grey, 200 pushes colours away from grey
        int[] sat={0xffc04020};int[] satCopy=sat.clone();
        WallpaperPixels.saturation(sat,100f);check(Arrays.equals(sat,satCopy),"100% saturation keeps the colour");
        WallpaperPixels.saturation(sat,0f);
        check(((sat[0]>>16)&255)==((sat[0]>>8)&255)&&((sat[0]>>8)&255)==(sat[0]&255),"0% saturation is grey");
        int[] more={0xffc04020};WallpaperPixels.saturation(more,200f);
        check(((more[0]>>16)&255)>=0xc0&&(more[0]&255)<=0x20,"200% saturation pushes the colours further apart");
        // tint: its alpha is the strength; the picture's alpha stays
        int[] tinted={0xff000000,0x80ff0000};WallpaperPixels.tint(tinted,0x00ffffff);
        check(tinted[0]==0xff000000&&tinted[1]==0x80ff0000,"a tint of no strength changes nothing");
        WallpaperPixels.tint(tinted,0xffffffff);
        check((tinted[0]&0xffffff)==0xffffff&&(tinted[0]>>>24)==255&&(tinted[1]>>>24)==0x80,"a full-strength tint replaces the colour and keeps the alpha");
        int[] half={0xff000000};WallpaperPixels.tint(half,0x80ffffff);
        check(Math.abs((half[0]&255)-128)<=1,"half a white tint over black is mid grey");
    }

    private static void blur(){
        int w=40,h=30;
        // A flat picture stays flat, whatever the blur.
        int[] flat=new int[w*h];Arrays.fill(flat,0xff336699);int[] copy=flat.clone();
        WallpaperPixels.blur(copy,w,h,6f);check(Arrays.equals(flat,copy),"a flat picture is unchanged by blurring");
        // No blur radius, or a picture too small to blur, does nothing.
        int[] edge=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)edge[y*w+x]=x<w/2?grey(0):grey(255);
        int[] same=edge.clone();WallpaperPixels.blur(same,w,h,0f);check(Arrays.equals(edge,same),"zero blur leaves the picture");
        WallpaperPixels.blur(same,w,h,.3f);check(Arrays.equals(edge,same),"a blur under half a pixel leaves the picture");
        WallpaperPixels.blur(same,1,1,5f);check(Arrays.equals(edge,same),"a one-pixel picture is left alone");
        // A hard edge becomes a ramp: dark far left, light far right, mid-grey in the middle, and monotonic.
        int[] soft=edge.clone();WallpaperPixels.blur(soft,w,h,3f);
        int row=10*w;
        check((soft[row]&255)<8&&(soft[row+w-1]&255)>247,"far from the edge the picture is as it was");
        int previous=-1;boolean monotonic=true;for(int x=0;x<w;x++){int v=soft[row+x]&255;if(v<previous)monotonic=false;previous=v;}
        check(monotonic,"the blurred edge rises steadily");
        int middle=soft[row+w/2-1]&255,next=soft[row+w/2]&255;
        check(middle>40&&middle<130&&next>125&&next<215,"the two pixels at the edge are the middle of the ramp: "+middle+","+next);
        // The ramp's width follows the blur: a bigger blur spreads the edge over more pixels.
        int[] wide=edge.clone();WallpaperPixels.blur(wide,w,h,6f);
        check((wide[row+w/2-8]&255)>(soft[row+w/2-8]&255),"a bigger blur reaches further from the edge");
        // The mean brightness of an opaque picture is kept (the blur only moves light about; edges repeat).
        long before=0,after=0;for(int i=0;i<edge.length;i++){before+=edge[i]&255;after+=wide[i]&255;}
        check(Math.abs(before-after)<edge.length*2L,"blurring keeps the overall brightness");
        // Alpha is blurred like any channel.
        int[] seethrough=new int[w*h];for(int i=0;i<seethrough.length;i++)seethrough[i]=(i%w<w/2?0xff:0)<<24|0xffffff;
        WallpaperPixels.blur(seethrough,w,h,3f);
        int alphaMiddle=seethrough[row+w/2]>>>24;check(alphaMiddle>40&&alphaMiddle<215,"transparency fades across the edge too");
    }

    private static void zone(){
        check(WallpaperPixels.zoneWeight(10f,100f,50f)==1f,"inside the zone: all of the zone's blur");
        check(WallpaperPixels.zoneWeight(100f,100f,50f)==1f,"at its lower edge: still all of it");
        check(WallpaperPixels.zoneWeight(150f,100f,50f)==0f,"past the feather: none");
        close(.5f,WallpaperPixels.zoneWeight(125f,100f,50f),.001f,"half way down the feather: half");
        check(WallpaperPixels.zoneWeight(120f,100f,50f)>WallpaperPixels.zoneWeight(130f,100f,50f),"the weight only falls going down");
        check(WallpaperPixels.zoneWeight(100.01f,100f,0f)==0f,"no feather: a hard edge");
        check(WallpaperPixels.zoneWeight(-5f,100f,50f)==0f,"rows above the picture's visible part do not matter");
        // A composite: top rows are the zone's, bottom rows the main's, the middle rows are mixed.
        int w=4,h=100;int[] main=new int[w*h],zone=new int[w*h];Arrays.fill(main,grey(0));Arrays.fill(zone,grey(200));
        // window as tall as the picture (1 row per window pixel, no crop): zone to 30, feather 40
        int[] mixed=WallpaperPixels.zoneComposite(main,zone,w,h,0f,1f,30f,40f);
        check((mixed[5*w]&255)==200,"the top is the zone's blur");
        check((mixed[95*w]&255)==0,"the bottom is the main blur");
        int mid=mixed[50*w]&255;check(mid>0&&mid<200,"between them the two are mixed: "+mid);
        for(int y=1;y<h;y++)check((mixed[y*w]&255)<=(mixed[(y-1)*w]&255),"the zone fades out downwards, never back in (row "+y+")");
        // A crop: the picture is taller than the window, so the window shows rows 20..80 and one window pixel covers two picture rows.
        int[] cropped=WallpaperPixels.zoneComposite(main,zone,w,h,20f,2f,15f,10f);
        check((cropped[(20+29)*w]&255)==200,"the zone's lower edge is measured in window pixels: 15 of them are 30 picture rows");
        check((cropped[(20+31+20)*w]&255)==0,"and the feather ends 10 window pixels (20 rows) after it");
        check(WallpaperPixels.mix(grey(0),grey(255),0)==grey(0)&&WallpaperPixels.mix(grey(0),grey(255),256)==grey(255),"mix weights 0 and 256 are the ends");
        close(128,WallpaperPixels.mix(grey(0),grey(255),128)&255,1f,"mix 128 is the middle");
    }

    private static void brightness(){
        int[] px={0xff808080,0x80ff0000};
        WallpaperPixels.brightness(px,100f);check(px[0]==0xff808080&&px[1]==0x80ff0000,"100% keeps the picture");
        WallpaperPixels.brightness(px,50f);check((px[0]&255)==64&&(px[0]>>>24)==255,"50% halves it");
        check((px[1]>>>24)==0x80,"alpha is never touched");
        int[] light={0xff404040};WallpaperPixels.brightness(light,150f);
        check((light[0]&255)==Math.round(64*.5f+50f*2.55f),"above 100% it lightens, not multiplies: "+(light[0]&255));
        int[] zero={0xff808080};WallpaperPixels.brightness(zero,0f);check((zero[0]&255)==0,"0% is black");
        int[] max={0xff808080};WallpaperPixels.brightness(max,200f);check((max[0]&255)==255,"200% is white");
        WallpaperPixels.brightness(null,50f);WallpaperPixels.brightness(new int[0],Float.NaN);
    }

    private static void curve(){
        check(WallpaperPixels.pullCurve(0f)==0f&&WallpaperPixels.pullCurve(-1f)==0f&&WallpaperPixels.pullCurve(Float.NaN)==0f,"nothing at rest or on bad input");
        check(WallpaperPixels.pullCurve(.1f)==0f&&WallpaperPixels.pullCurve(1f/6f)<1e-4f,"nothing for the first sixth: the system's curve");
        close(1f,WallpaperPixels.pullCurve(1f),.0001f,"all of it at full expansion");
        check(WallpaperPixels.pullCurve(2f)==WallpaperPixels.pullCurve(1f),"beyond full is full");
        float previous=0f;for(int i=0;i<=100;i++){float v=WallpaperPixels.pullCurve(i/100f);check(v>=previous-1e-6f,"the curve only rises");previous=v;}
        check(WallpaperPixels.pullCurve(.3f)>0f&&WallpaperPixels.pullCurve(.3f)<.5f&&WallpaperPixels.pullCurve(.6f)>.8f,"it comes in slowly and is mostly there well before the end");
    }

    /** Two layers, the sharp one first and the blurred one over it, show (1-blend) of the first and blend of the second at the coverage. */
    private static void layers(){
        for(float coverage:new float[]{.15f,.5f,.9f,1f})for(float blend:new float[]{0f,.25f,.5f,.8f,1f}){
            float second=coverage*blend,first=WallpaperPixels.firstLayerAlpha(coverage,second);
            // over a background of 0 (a transparent layer): total coverage and the weights of the two pictures
            float blurWeight=second,sharpWeight=(1f-second)*first,coverageNow=blurWeight+sharpWeight;
            close(coverage,coverageNow,.001f,"total coverage "+coverage+"/"+blend);
            close(coverage*(1f-blend),sharpWeight,.001f,"sharp weight "+coverage+"/"+blend);
            close(coverage*blend,blurWeight,.001f,"blurred weight "+coverage+"/"+blend);
        }
        check(WallpaperPixels.firstLayerAlpha(1f,1f)==0f,"fully blurred at full coverage: the sharp layer is not needed");
    }
}
