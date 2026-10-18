#!/usr/bin/env bash
# Substitute class path for environments without the Android SDK: the framework as Robolectric's android-all jar
# (a stand-in for android.jar), the LibXposed service and libarchive class jars. Everything comes from Maven Central.
. "$(dirname "$0")/common.sh"
L="$C17_WORK/libs"
c17_download org/robolectric/android-all/17-robolectric-15733970/android-all-17-robolectric-15733970.jar "$L/android-all.jar" || exit 1
c17_download io/github/libxposed/service/102.0.0/service-102.0.0.aar "$L/service.aar" || exit 1
c17_download me/zhanghai/android/libarchive/library/1.1.6/library-1.1.6.aar "$L/libarchive.aar" || exit 1
for pair in service:svc libarchive:la; do
  src="${pair%%:*}"; dir="$L/${pair##*:}"
  [ -s "$dir/classes.jar" ] || { mkdir -p "$dir"; (cd "$dir" && unzip -qo "$L/$src.aar" classes.jar); }
done
echo "libraries ready in $L"
