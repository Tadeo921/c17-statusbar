#!/usr/bin/env bash
# Compile validation/checks-src against the production classes and run the checks listed in validation/run-checks.ps1
# (or only the names given). Same list and the same repository-root working directory as tools/dev/checks.ps1.
. "$(dirname "$0")/common.sh"
[ -d "$C17_WORK/prod" ] || "$(dirname "$0")/build-java.sh" || exit 1
L="$C17_WORK/libs"; OUT="$C17_WORK/checks"
JSON="$C17_ROOT/validation/test-libs/json-20240303.jar"; XPOSED="$C17_ROOT/tools/lib/api-101.0.0.jar"
BASECP="$C17_WORK/prod:$XPOSED:$JSON:$L/android-all.jar:$L/svc/classes.jar:$L/la/classes.jar"
if [ "${C17_NO_COMPILE:-0}" != 1 ]; then
  rm -rf "$OUT" && mkdir -p "$OUT"
  { find "$C17_ROOT/validation/checks-src" -name '*.java'; echo "$C17_ROOT/validation/SettingsCheck.java"; } > "$C17_WORK/check-sources.txt"
  javac -J-Xmx2g -encoding UTF-8 -proc:none -nowarn -cp "$BASECP" -d "$OUT" @"$C17_WORK/check-sources.txt" 2>&1 | c17_quiet | grep -v '^Note:\|warning\|Unsafe\|\^'
  [ "${PIPESTATUS[0]}" = 0 ] || { echo "check compilation FAILED"; exit 1; }
  # javac quirk: with the framework jar on the class path, the Resources stub's static Theme gets an outer-instance constructor that the JVM
  # refuses to load. Compiling that one stub without the framework jar produces the intended class.
  FIX="$C17_WORK/resources-stub"; rm -rf "$FIX" && mkdir -p "$FIX"
  javac -encoding UTF-8 -proc:none -nowarn -sourcepath "$C17_ROOT/validation/checks-src" -d "$FIX" "$C17_ROOT/validation/checks-src/android/content/res/Resources.java" 2>&1 | c17_quiet | grep -v '^Note:'
  cp "$FIX"/android/content/res/*.class "$OUT/android/content/res/"
fi
if [ $# -gt 0 ]; then NAMES="$*"; else
  NAMES=$(python3 - "$C17_ROOT/validation/run-checks.ps1" <<'PY'
import re, sys
text = open(sys.argv[1], encoding='utf-8').read()
body = re.search(r"foreach\(\$c17Name in @\((.*?)\)\) \{", text, re.S).group(1)
print(' '.join(re.findall(r"'([A-Za-z0-9_]+)'", body)))
PY
)
fi
cd "$C17_ROOT"; total=0; failed=0; names=""
for name in $NAMES; do
  total=$((total+1))
  output=$(timeout 300 java -Xms16m -Xmx256m -XX:+UseSerialGC -cp "$OUT:$BASECP" "dev.puitheme.$name" 2>&1); code=$?
  if [ $code -ne 0 ]; then failed=$((failed+1)); names="$names $name"; echo "FAILED: $name"; echo "$output" | c17_quiet | grep -v '^\s*at ' | head -8 | sed 's/^/    /'; fi
done
echo "TOTAL $total FAILED $failed$names"
[ $failed -eq 0 ]
