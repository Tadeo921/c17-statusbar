#!/usr/bin/env bash
# Best-effort type check of ALL Kotlin sources with Kotlin 2.4.20 and its Compose compiler plugin, for environments where Google Maven
# (dl.google.com) is unreachable, so Gradle/AGP cannot run. It is NOT the real build:
#   * Compose is the JetBrains 1.8.2 desktop jars instead of the Android 1.12 artifacts;
#   * androidx.activity/lifecycle/core and a few Android-only Compose APIs are compile-only stubs (kotlin-stubs/);
#   * nothing is packaged, signed or run.
# It catches unresolved references, type errors and composable-context mistakes. It cannot show what the screen looks like.
# Needs: Maven Central access, Gradle (C17_GRADLE, default "gradle") only to download the compiler and libraries.
. "$(dirname "$0")/common.sh"
DIR="$(cd "$(dirname "$0")" && pwd)"
"$DIR/build-java.sh" || exit 1
K="$C17_WORK/kdeps"; mkdir -p "$K"
echo "rootProject.name = 'kdeps'" > "$K/settings.gradle"
cat > "$K/build.gradle" <<'GRADLE'
plugins { id 'java' }
repositories { mavenCentral() }
configurations { compilerCp; cmp; mx }
dependencies {
  compilerCp 'org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.20'
  compilerCp 'org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:2.4.20'
  cmp 'org.jetbrains.compose.desktop:desktop-jvm-linux-x64:1.8.2'
  cmp 'org.jetbrains.compose.material:material-icons-extended-desktop:1.7.3'
  ['miuix-ui', 'miuix-preference', 'miuix-icons', 'miuix-core', 'miuix-squircle', 'miuix-shader'].each { name ->
    mx("top.yukonga.miuix.kmp:${name}-desktop:0.9.4") { transitive = false }
  }
}
tasks.register('fetch') {
  doLast {
    ['compilerCp', 'cmp', 'mx'].each { name ->
      def dir = new File(buildDir, name); dir.mkdirs()
      def lenient = configurations[name].resolvedConfiguration.lenientConfiguration
      lenient.getFiles().each { f -> java.nio.file.Files.copy(f.toPath(), new File(dir, f.name).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING) }
      println name + ': ' + lenient.getFiles().size() + ' files'
    }
  }
}
GRADLE
if [ ! -s "$K/build/compilerCp/kotlin-compiler-embeddable-2.4.20.jar" ]; then
  (cd "$K" && "${C17_GRADLE:-gradle}" fetch --no-daemon -q 2>&1 | c17_quiet) || exit 1
fi
c17_download top/yukonga/miuix/kmp/miuix-blur-android/0.9.4/miuix-blur-android-0.9.4.aar "$K/miuix-blur.aar" || exit 1
[ -s "$K/blur/classes.jar" ] || { mkdir -p "$K/blur"; (cd "$K/blur" && unzip -qo "$K/miuix-blur.aar" classes.jar); }
L="$C17_WORK/libs"; BUILD="$K/build"
STUBS="$C17_WORK/kotlin-stub-classes"; rm -rf "$STUBS" && mkdir -p "$STUBS"
javac --release 8 -nowarn -cp "$L/android-all.jar" -d "$STUBS" $(find "$DIR/kotlin-stubs" -name '*.java') 2>&1 | c17_quiet
CP="$STUBS:$C17_WORK/prod:$L/android-all.jar:$K/blur/classes.jar"
for jar in "$BUILD"/cmp/*.jar; do
  case "$(basename "$jar")" in kotlin-stdlib*|kotlinx-coroutines*|annotations*|skiko-awt-runtime*) ;; *) CP="$CP:$jar";; esac
done
for jar in "$BUILD"/mx/*.jar; do CP="$CP:$jar"; done
CP="$CP:$BUILD/compilerCp/kotlinx-coroutines-core-jvm-1.8.0.jar:$BUILD/compilerCp/kotlin-stdlib-2.4.20.jar"
KC=""; for jar in kotlin-compiler-embeddable-2.4.20 kotlin-stdlib-2.4.20 kotlin-script-runtime-2.4.20 kotlin-reflect-1.6.10 kotlin-daemon-embeddable-2.4.20 kotlinx-coroutines-core-jvm-1.8.0 annotations-13.0; do KC="$KC:$BUILD/compilerCp/$jar.jar"; done
OUT="$C17_WORK/kotlin-classes"; rm -rf "$OUT" && mkdir -p "$OUT"
java -Xmx3g -cp "${KC#:}" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 11 -nowarn \
  -Xplugin="$BUILD/compilerCp/kotlin-compose-compiler-plugin-embeddable-2.4.20.jar" -cp "$CP" -d "$OUT" \
  $(find "$C17_ROOT/app/src/main/java" -name '*.kt') $(find "$DIR/kotlin-stubs" -name '*.kt') 2>&1 | c17_quiet | tee "$C17_WORK/kotlin-check.log"
errors=$(grep -c ' error: ' "$C17_WORK/kotlin-check.log")
echo "Kotlin files: $(find "$C17_ROOT/app/src/main/java" -name '*.kt' | wc -l); errors: $errors"
[ "$errors" = 0 ] && [ "${PIPESTATUS[0]}" = 0 ]
