package com.android.systemui.media.controls.shared.model;
public final class MediaAction {
    private final com.android.systemui.media.controls.models.player.OplusMediaActionEx extra;
    private final Runnable action;
    public MediaAction(boolean standard,Runnable action){extra=new com.android.systemui.media.controls.models.player.OplusMediaActionEx(standard);this.action=action;}
    public com.android.systemui.media.controls.models.player.OplusMediaActionEx getMediaActionEx(){return extra;}
    public Runnable getAction(){return action;}
}
