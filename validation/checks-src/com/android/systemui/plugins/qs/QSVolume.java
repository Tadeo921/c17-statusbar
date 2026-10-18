package com.android.systemui.plugins.qs;
public final class QSVolume {
    public static final class VolumeState {
        private final int route,level,max;private final boolean zen;
        public VolumeState(int route,int level,int max,boolean zen){this.route=route;this.level=level;this.max=max;this.zen=zen;}
        public int getRoute(){return route;}public int getLevel(){return level;}public int getMaxLevel(){return max;}public boolean getZenMuted(){return zen;}
    }
}
