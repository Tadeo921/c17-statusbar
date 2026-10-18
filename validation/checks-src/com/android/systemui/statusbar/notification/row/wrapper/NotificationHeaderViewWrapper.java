package com.android.systemui.statusbar.notification.row.wrapper;
public class NotificationHeaderViewWrapper extends NotificationViewWrapper {
    public com.android.internal.widget.CachingIconView mIcon;
    public NotificationHeaderViewWrapper(android.content.Context context,android.view.View root,
            com.android.systemui.statusbar.notification.row.ExpandableNotificationRow row){super(context,root,row);resolveHeaderViews();}
    public void onContentUpdated(com.android.systemui.statusbar.notification.row.ExpandableNotificationRow row){mRow=row;resolveHeaderViews();}
    public void onReinflated(){resolveHeaderViews();}
    public final void resolveHeaderViews(){mIcon=(com.android.internal.widget.CachingIconView)mView.findViewById(0x01020006);}
    public com.android.internal.widget.CachingIconView getIcon(){return mIcon;}
}
