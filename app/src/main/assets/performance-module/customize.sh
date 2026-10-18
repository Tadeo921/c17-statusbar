#!/system/bin/sh
# Scripts only: no system overlay, no mount, no SELinux policy change, so no metamodule is needed.
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/common.sh" 0 0 0755
set_perm "$MODPATH/webui.sh" 0 0 0755
# The former cache-backup module is replaced: let its manager run its own uninstall (backup removal) at reboot.
OLD=/data/adb/modules/c17_resident_cache
if [ -d "$OLD" ] && [ ! -L "$OLD" ] && [ ! -L "$OLD/remove" ] && grep -qx 'id=c17_resident_cache' "$OLD/module.prop" 2>/dev/null; then
  touch "$OLD/remove" && ui_print 'C17: the old cache-backup module will be removed at reboot.'
fi
ui_print 'Status C17 附属模块：仅按需优化系统界面与控制中心插件；管理器“执行”打开控制中心。'
