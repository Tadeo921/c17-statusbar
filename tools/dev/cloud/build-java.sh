#!/usr/bin/env bash
# Compile every production Java source to Java 8 bytecode (major 52) against the substitute class path, then scan the class versions.
# This is a stand-in for Gradle's compileReleaseJavaWithJavac; it does not replace the real build, lintVital or packaging.
. "$(dirname "$0")/common.sh"
"$(dirname "$0")/fetch-libs.sh" || exit 1
L="$C17_WORK/libs"; OUT="$C17_WORK/prod"
rm -rf "$OUT" && mkdir -p "$OUT"
find "$C17_ROOT/app/src/main/java" -name '*.java' > "$C17_WORK/prod-sources.txt"
CP="$L/android-all.jar:$C17_ROOT/tools/lib/api-101.0.0.jar:$L/svc/classes.jar:$L/la/classes.jar"
javac -J-Xmx2g --release 8 -encoding UTF-8 -proc:none -nowarn -cp "$CP" -d "$OUT" @"$C17_WORK/prod-sources.txt" 2>&1 | c17_quiet | grep -v '^Note:'
[ "${PIPESTATUS[0]}" = 0 ] || { echo "production compilation FAILED"; exit 1; }
python3 - "$OUT" <<'PY'
import os, struct, sys, collections
versions = collections.Counter()
for directory, _, files in os.walk(sys.argv[1]):
    for name in files:
        if name.endswith('.class'):
            with open(os.path.join(directory, name), 'rb') as handle:
                magic, minor, major = struct.unpack('>IHH', handle.read(8))
            assert magic == 0xCAFEBABE, name
            versions[major] += 1
print('class files by major version:', dict(versions))
sys.exit(0 if set(versions) == {52} else 1)
PY
