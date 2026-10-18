#!/usr/bin/env bash
# Install the Android SDK pieces the project needs (platform 37.0 and build-tools 36.0.0) into $ANDROID_SDK_ROOT
# (default: $C17_CLOUD_WORK/android-sdk). Needs dl.google.com to be reachable. Prints the sdk.dir line for local.properties.
. "$(dirname "$0")/common.sh"
SDK="${ANDROID_SDK_ROOT:-$C17_WORK/android-sdk}"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  curl -sS -m 120 -o "$C17_WORK/repository2-3.xml" https://dl.google.com/android/repository/repository2-3.xml || exit 1
  ARCHIVE=$(python3 - "$C17_WORK/repository2-3.xml" <<'PY'
import re, sys
text = open(sys.argv[1], encoding='utf-8').read()
block = re.search(r'<remotePackage path="cmdline-tools;latest">.*?</remotePackage>', text, re.S).group(0)
print(re.search(r'<url>(commandlinetools-linux[^<]*)</url>', block).group(1))
PY
)
  curl -sS -m 600 -o "$C17_WORK/cmdtools.zip" "https://dl.google.com/android/repository/$ARCHIVE" || exit 1
  rm -rf "$C17_WORK/cmdtools" && mkdir -p "$C17_WORK/cmdtools" "$SDK/cmdline-tools"
  unzip -qo "$C17_WORK/cmdtools.zip" -d "$C17_WORK/cmdtools"
  rm -rf "$SDK/cmdline-tools/latest" && mv "$C17_WORK/cmdtools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-37.0" "build-tools;36.0.0" 2>&1 | c17_quiet | tail -5
echo "sdk.dir=$SDK"
