#!/system/bin/sh
MODDIR=${0%/*}
# Validate before executing another script; never follow a replacement link.
case "$MODDIR" in /data/adb/modules/c17_performance) ;; *) exit 20;; esac
for path in /data /data/adb /data/adb/modules "$MODDIR" "$MODDIR/common.sh"; do [ ! -L "$path" ] || exit 20; done
[ -f "$MODDIR/common.sh" ] || exit 20
for file in "$MODDIR" "$MODDIR/common.sh"; do
  [ "$(stat -c %u "$file")" = 0 ] || exit 20
  permission=$(stat -c %a "$file") || exit 20
  case "$permission" in ''|*[!0-7]*|?????*) exit 20;; esac
  [ $((0$permission & 022)) -eq 0 ] || exit 20
done
# The fixed provider method only registers a one-shot system job; it accepts no root command.
if command -v timeout >/dev/null 2>&1; then
  timeout -k 1 8 content call --uri content://dev.puitheme.iosstatusbar.settings --method performance_schedule --arg boot >/dev/null 2>&1
fi
exec sh "$MODDIR/common.sh" boot
