package com.android.systemui.statusbar.notification.row.wrapper;
public class NotificationViewWrapper {
    public android.view.View mView;
    public com.android.systemui.statusbar.notification.row.ExpandableNotificationRow mRow;
    public NotificationViewWrapper(android.content.Context context,android.view.View root,
            com.android.systemui.statusbar.notification.row.ExpandableNotificationRow row){mView=root;mRow=row;}
}
