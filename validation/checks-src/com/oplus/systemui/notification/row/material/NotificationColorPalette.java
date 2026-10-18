package com.oplus.systemui.notification.row.material;
/** Native wrapper contract: Companion.from(mode).getPrimaryColor(). MATERIAL
 * reads the current Config.commonTextMaterialColor rather than a named resource. */
public final class NotificationColorPalette {
    public static final Companion Companion=new Companion();
    public static int commonTextMaterialColor=0xff161616;
    public static int lightPrimary=0xff181818,darkPrimary=0xfff4f4f4;
    private final int primaryColor;
    private NotificationColorPalette(int color){primaryColor=color;}
    public int getPrimaryColor(){return primaryColor;}
    public static final class Companion {
        public NotificationColorPalette from(MaterialColorMode mode){
            return new NotificationColorPalette(mode==MaterialColorMode.MATERIAL?commonTextMaterialColor:mode==MaterialColorMode.NORMAL_DARK?darkPrimary:lightPrimary);
        }
    }
}
