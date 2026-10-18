package dev.puitheme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;

/** Device code162: iOS 27 uses PUI's exact logic with its own outline, also without a percentage. */
public final class Ios27BatteryStyleCheck {
    private static int checks;
    private static void check(boolean value, String why) { checks++; if (!value) throw new AssertionError(why); }
    /** Records which asset each slot was built from. */
    public static final class Named extends PuiBatteryStyleCheck.Graphic {
        final String name;
        Named(String name) { this.name = name; }
    }
    public static final class Assets extends Resources {
        static final String[] NAMES = {"c17_pui_battery_bg", "c17_pui_battery_frame", "c17_pui_battery_outer_bg", "c17_pui_battery_outer_fill",
                "c17_ios27_battery_bg", "c17_ios27_battery_fill", "c17_ios27_battery_frame", "c17_ios27_battery_outer_bg", "c17_ios27_battery_outer_fill"};
        public int getIdentifier(String name, String kind, String pkg) {
            for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i + 1;
            return 0;
        }
        public Drawable getDrawable(int id, Theme theme) { return new Named(NAMES[id - 1]); }
    }
    public static final class Horizontal extends PuiBatteryStyleCheck.NativeHorizontal {
        public final Context assets = new PuiBatteryStyleCheck.NativeContext() {
            public Context createPackageContext(String name, int flags) { return new Context() { public Resources getResources() { return new Assets(); } }; }
        };
        public Context getContext() { return assets; }
    }
    private static Bundle settings(String style) {
        Bundle values = new Bundle();
        values.putBoolean("battery_enabled", true); values.putBoolean("battery_style_enabled", true);
        values.putString(StatusBarSettings.BATTERY_STYLE, style);
        return values;
    }
    private static String name(Drawable drawable) {
        if (drawable instanceof android.graphics.drawable.LayerDrawable)drawable=((android.graphics.drawable.LayerDrawable)drawable).getDrawable(0);
        if (drawable instanceof android.graphics.drawable.InsetDrawable) drawable = ((android.graphics.drawable.InsetDrawable) drawable).getDrawable();
        if(drawable instanceof ClipDrawable)drawable=((ClipDrawable)drawable).getWrapped();
        return drawable instanceof Named ? ((Named) drawable).name : "native";
    }
    public static void main(String[] args) throws Exception {
        bodyLevelGeometry();
        for (String saved : new String[]{Ios27BatteryStyle.STYLE, "ios27_number", "ios27_plain"}) check(Ios27BatteryStyle.known(saved), saved);
        check(!Ios27BatteryStyle.known("pui") && !Ios27BatteryStyle.known("native"), "other styles stay PUI or native");

        Horizontal battery = new Horizontal(); battery.percentInPaint.setTextSize(38);
        Drawable nativeOutside = battery.outsideDrawable, nativeOuter = battery.bgDrawable;
        PuiBatteryStyle style = new PuiBatteryStyle(PuiBatteryStyleCheck.NativeHorizontal.class);
        style.configure(settings(Ios27BatteryStyle.STYLE));
        check(style.prepare(battery, true), "iOS 27 applies with the number inside");
        check(name(battery.outsideDrawable).equals("c17_ios27_battery_bg"), "iOS body and terminal in the native slot");
        check(name(battery.insideDrawable).equals("c17_ios27_battery_fill"), "iOS level fills the body only");
        check(name(battery.bgDrawable).equals("c17_ios27_battery_outer_bg") && name(battery.progressDrawable).equals("c17_ios27_battery_outer_fill"),
                "iOS outline also without a percentage");
        check(battery.outsideDrawable.getBounds().left == 8 && battery.outsideDrawable.getBounds().right == 108, "the native PUI rect");
        check(battery.percentInPaint.getTextSize() == 40, "the native number at PUI's 10dp");
        check(((PuiBatteryStyleCheck.Graphic) battery.outsideDrawable).tint == 0x8c000000, "a 55% track in the icon colour: "
                + Integer.toHexString(((PuiBatteryStyleCheck.Graphic) battery.outsideDrawable).tint));
        check(((PuiBatteryStyleCheck.Graphic) battery.bgDrawable).tint == 0x8c000000, "no opaque outline without a percentage");
        android.graphics.drawable.InsetDrawable level = (android.graphics.drawable.InsetDrawable)
                ((android.graphics.drawable.LayerDrawable) battery.insideDrawable).getDrawable(0);
        check(level.right > .06f && level.right < .07f && level.left < .01f, "the level is clipped across the body only, not the terminal");
        check(((PuiBatteryStyleCheck.Graphic)((ClipDrawable)level.getDrawable()).getWrapped()).tint == 0xff22c55e, "an opaque level fill");

        ViewGroup owner = new ViewGroup(new PuiBatteryStyleCheck.NativeContext()); ImageView image = new ImageView(owner.getContext()); owner.addView(image);
        image.setImageDrawable(battery); image.setLayoutParams(new ViewGroup.LayoutParams(116, 64));
        style.updateView(owner, battery, false);
        check(image.getLayoutParams().height == 68 && image.getLayoutParams().width == 116, "PUI's 29x17dp box even without a percentage");
        check(style.prepare(battery, false), "iOS 27 still applies when the system hides the percentage");

        style.configure(settings("pui")); style.prepare(battery, true);
        check(name(battery.outsideDrawable).equals("c17_pui_battery_bg") && name(battery.insideDrawable).equals("c17_pui_battery_bg"), "switching to PUI rebuilds PUI assets");
        check(!style.prepare(battery, false) && battery.outsideDrawable == nativeOutside && battery.bgDrawable == nativeOuter,
                "PUI keeps its percentage requirement and restores native");
        style.configure(settings(Ios27BatteryStyle.STYLE)); style.prepare(battery, true);
        style.configure(settings("native"));
        check(battery.outsideDrawable == nativeOutside && battery.bgDrawable == nativeOuter && battery.percentInPaint.getTextSize() == 38, "native restored");
        check(image.getLayoutParams().height == 64, "native size restored");
        System.out.println("Ios27BatteryStyleCheck: " + checks + " checks passed");
    }
    private static void bodyLevelGeometry() throws Exception {
        Horizontal battery=new Horizontal();PuiBatteryStyle style=new PuiBatteryStyle(PuiBatteryStyleCheck.NativeHorizontal.class);
        style.configure(settings(Ios27BatteryStyle.STYLE));
        for(int amount:new int[]{47,0,1,7,73,99,100}) {
            battery.batteryLevel=amount;style.prepare(battery,true);
            Drawable[] levels={battery.insideDrawable,battery.progressDrawable};
            float[][] inset={PuiBatteryStyle.IOS_FILL,PuiBatteryStyle.IOS_OUTER_FILL};
            for(int slot=0;slot<2;slot++)for(int width:new int[]{116,2200}) {
                android.graphics.Canvas canvas=new android.graphics.Canvas();
                levels[slot].setBounds(8,3,8+width,1203);levels[slot].draw(canvas);
                if(amount==0){check(canvas.clips==0,"zero level draws no fill");continue;}
                // Insets belong outside the level clip, excluding its terminal gap.
                int left=8+(int)(width*inset[slot][0]),right=8+width-(int)(width*inset[slot][1]);
                int expected=left+(right-left)-(right-left)*(10000-amount*100)/10000;
                check(canvas.clipLeft==left&&canvas.clipRight==expected,
                        amount+"% must clip body ["+left+","+expected+"], got ["+canvas.clipLeft+","+canvas.clipRight+"]");
                check(expected<=right,"the terminal is never part of level fill");
                check(canvas.saves==canvas.restores,"native canvas clip is restored");
            }
        }
    }
}
