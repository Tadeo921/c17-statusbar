package com.oplus.systemui.notification.row.material;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import kotlinx.coroutines.flow.StateFlow;
/** The OEM extension is an instance obtained from each background's Lazy getter. */
public class NotificationMaterialColorEx {
    public MaterialColorMode rowMode=MaterialColorMode.UNINITIATED;
    public MaterialColorMode currentMode=MaterialColorMode.UNINITIATED;
    public boolean failRead;
    public int rowReads,flowReads;
    public NotificationBlurMode headsModeForCheck=NotificationBlurMode.UNINITIATED;
    public int headsReads;
    public NotificationBlurMode getBlurModeForRow(ExpandableNotificationRow row,boolean forceHeads){headsReads++;if(failRead)throw new IllegalStateException("native scene unavailable");return headsModeForCheck;}
    public MaterialColorMode getColorModeForRow(ExpandableNotificationRow row){rowReads++;if(failRead)throw new IllegalStateException("native palette unavailable");return rowMode;}
    public StateFlow<MaterialColorMode> getCurrentMaterialColor(){flowReads++;if(failRead)throw new IllegalStateException("native palette unavailable");return ()->currentMode;}
}
