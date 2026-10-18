#!/usr/bin/env bash
# The real Gradle release build of tools/dev/build.ps1 (compileDebugJava, assembleRelease, lintVitalRelease) for a Linux environment, retried
# while Maven Central answers 429. Gradle caches every successful download, so a rate-limited network converges only if the 429 count falls.
# UNSIGNED unless C17_KEYSTORE / C17_STORE_PASSWORD / C17_KEY_PASSWORD / C17_KEY_ALIAS are set (the repository never contains them).
# Needs: JDK 17+, the SDK from install-android-sdk.sh (ANDROID_SDK_ROOT), dl.google.com and Maven Central.
. "$(dirname "$0")/common.sh"
SDK="${ANDROID_SDK_ROOT:-$C17_WORK/android-sdk}"
cd "$C17_ROOT" || exit 1
printf 'sdk.dir=%s\n' "$SDK" > local.properties            # git-ignored; removed again below
mkdir -p build/c17-dev; LOG=build/c17-dev/build-code108.log
trap 'rm -f local.properties' EXIT
for attempt in $(seq 1 "${C17_ATTEMPTS:-20}"); do
  ./gradlew -I "$(dirname "$0")/gradle-central-routing.init.gradle" --max-workers=1 :app:compileDebugJavaWithJavac :app:assembleRelease \
    :app:lintVitalRelease -Pc17LocalPuiAssets=false --console=plain > "$LOG" 2>&1 && { echo "BUILD OK after $attempt attempt(s)"; exit 0; }
  failures=$(grep -c "Received status code 429" "$LOG")
  echo "attempt $attempt failed; HTTP 429 lines: $failures"
  [ "$failures" -eq 0 ] && { echo "not a rate-limit failure; see $LOG"; exit 2; }
  sleep 20
done
exit 1
