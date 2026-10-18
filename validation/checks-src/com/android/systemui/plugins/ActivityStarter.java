package com.android.systemui.plugins;
/** Native SystemUI public contract verified from the local OEM DEX. */
public interface ActivityStarter {
    void postStartActivityDismissingKeyguard(android.content.Intent intent,int delay);
    void startActivity(android.content.Intent intent,boolean dismissShade);
}
