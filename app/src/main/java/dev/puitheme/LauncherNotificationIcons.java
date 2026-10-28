// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 aiingjie
package dev.puitheme;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import java.util.List;

/** Read the current native launcher's icon pipeline, including its applied theme/pack, then give every icon the
 * system's icon outline (corner radius), pack icon or not.
 * No icon-pack files are imported, no launcher preferences or app icons are changed. */
final class LauncherNotificationIcons {
    static final String LAUNCHER="com.android.launcher";
    private LauncherNotificationIcons() { }
    static Bitmap load(Context owner,String packageName)throws Exception {
        NotificationIconOverrides.validatePackage(packageName,"El formato del nombre de paquete de la aplicación no es válido");
        if(owner==null)return null;
        Context launcher=owner.createPackageContext(LAUNCHER,0);
        PackageManager manager=launcher.getPackageManager();
        Intent main=launcherIntent(manager,packageName);
        if(main==null)return null;
        // ColorOS' UxIconPackageManagerExt.loadItemIcon reads activity-level edits and
        // the current theme through the caller's launcher PackageManager context.
        Drawable icon=manager.getActivityIcon(main);
        if(icon==null)return null;
        Drawable.ConstantState state=icon.getConstantState();
        if(state!=null)icon=state.newDrawable(launcher.getResources());
        icon=icon.mutate();
        int width=icon.getIntrinsicWidth(),height=icon.getIntrinsicHeight();
        if(width<=0||height<=0){width=128;height=128;}
        int max=Math.max(width,height);
        int targetWidth=Math.max(1,(int)((long)width*128/max));
        int targetHeight=Math.max(1,(int)((long)height*128/max));
        Bitmap image=Bitmap.createBitmap(targetWidth,targetHeight,Bitmap.Config.ARGB_8888);
        icon.setBounds(0,0,targetWidth,targetHeight);icon.draw(new Canvas(image));
        // Icons the current icon pack does not contain come back without the system's corner radius.
        return SystemIconShape.apply(owner,image);
    }
    static Intent launcherIntent(PackageManager manager,String pkg) {
        Intent request=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg);
        ResolveInfo preferred=manager.resolveActivity(request,0);
        ActivityInfo target=preferred==null?null:preferred.activityInfo;
        if(!launchable(target,pkg)) {
            target=null;List<ResolveInfo> matches=manager.queryIntentActivities(request,0);
            if(matches!=null)for(ResolveInfo candidate:matches)
                if(candidate!=null&&launchable(candidate.activityInfo,pkg)){target=candidate.activityInfo;break;}
        }
        return target==null?null:request.setComponent(new ComponentName(pkg,target.name));
    }
    private static boolean launchable(ActivityInfo info,String pkg) {
        return info!=null&&info.enabled&&info.exported&&pkg.equals(info.packageName)
                &&info.name!=null&&info.applicationInfo!=null&&info.applicationInfo.enabled;
    }
}
