#!/system/bin/sh
umask 077
MODDIR=${0%/*}
# Validate before executing another script; never follow a replacement link.
case "$MODDIR" in /data/adb/modules/c17_performance) ;; *) exit 20;; esac
for path in /data /data/adb /data/adb/modules "$MODDIR" "$MODDIR/common.sh"; do [ ! -L "$path" ] || exit 20; done
[ -f "$MODDIR/common.sh" ] || exit 20
for file in "$MODDIR" "$MODDIR/common.sh"; do
  [ "$(stat -c %u "$file")" = 0 ] || exit 20
  permission=$(stat -c %a "$file") || exit 20
  # Android mksh treats leading-zero arithmetic differently; inspect the permission digits.
  case "$permission" in ''|*[!0-7]*|?????*|*[2367][0-7]|*[2367]) exit 20;; esac
done
[ "$(id -u)" = 0 ] || exit 21
# The manager's Execute button is the explicit manual request. The existing worker owns
# its task lock, timeout and cancellation; opening the exported page alone never runs it.
if command -v setsid >/dev/null 2>&1; then setsid sh "$MODDIR/common.sh" run </dev/null >/dev/null 2>&1 &
else sh "$MODDIR/common.sh" run </dev/null >/dev/null 2>&1 & fi
printf '已提交优化，请查看实际运行状态。\n'
# A missing APK must not discard the accepted manual task, and this UI launch is bounded.
if command -v timeout >/dev/null 2>&1 && timeout -k 1 12 am start --user 0 -n dev.puitheme.iosstatusbar/dev.puitheme.PerformanceControlActivity >/dev/null 2>&1; then
  printf '已打开 C17 控制中心。\n'
else
  printf '控制中心未能打开，请安装或打开最新版 C17 查看结果。\n'
fi
