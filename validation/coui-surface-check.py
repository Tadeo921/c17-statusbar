from pathlib import Path
import argparse
import os
import shutil
import subprocess
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description='Check actual COUI surface math against the pinned Compose artifacts, without a device.')
parser.add_argument('--java', default=shutil.which('java') or str(root / '.local-toolchain/jdk/jdk-17.0.20.1+1/bin/java.exe'))
parser.add_argument('--cache', type=Path, default=root / '.local-toolchain/gradle-home/caches/modules-2/files-2.1')
parser.add_argument('--production-classes', type=Path, help='The build output of compileReleaseKotlin; check its actual implementation.')
parser.add_argument('--output', type=Path, default=root / 'build/c17-dev/coui-surface-check')
args = parser.parse_args()
cache, java, out = args.cache, args.java, args.output
out.mkdir(parents=True, exist_ok=True)
def artifact(group, name, version, suffix):
    files = list((cache / group / name / version).rglob('*' + suffix))
    assert len(files) == 1, (name, files)
    return files[0]
stdlib = artifact('org.jetbrains.kotlin', 'kotlin-stdlib', '2.4.20', '.jar')
compiler = artifact('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.4.20', '.jar')
compiler_cp = [compiler, stdlib,
    artifact('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.4.20', '.jar'),
    artifact('org.jetbrains.kotlin', 'kotlin-reflect', '1.6.10', '.jar'),
    artifact('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.9.0', '.jar'),
    artifact('org.jetbrains', 'annotations', '23.0.0', '.jar')]
classpath = [stdlib, artifact('androidx.collection', 'collection-jvm', '1.5.0', '.jar')]
for group, name in [('androidx.compose.ui','ui-graphics-android'), ('androidx.compose.ui','ui-unit-android'),
                    ('androidx.compose.ui','ui-util-android'), ('androidx.compose.ui','ui-geometry-android')]:
    aar = artifact(group, name, '1.12.0', '.aar')
    jar = out / (name + '.jar')
    with zipfile.ZipFile(aar) as archive: jar.write_bytes(archive.read('classes.jar'))
    classpath.append(jar)
sources = [root / 'validation/CouiSurfaceMathCheck.kt']
friends = []
if args.production_classes:
    production = args.production_classes.resolve()
    assert (production / 'dev/puitheme/CouiSurfaceMathKt.class').is_file(), production
    classpath.insert(0, production)
    friends = ['-Xfriend-paths=' + str(production)]
else:
    sources.insert(0, root / 'app/src/main/java/dev/puitheme/CouiSurfaceMath.kt')
cp = os.pathsep.join(map(str, classpath))
result = subprocess.run([str(java), '-Xms16m', '-Xmx256m', '-XX:+UseSerialGC', '-cp', os.pathsep.join(map(str, compiler_cp)),
    'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect', '-jvm-target', '11',
    '-classpath', cp, '-d', str(out / 'classes'),
    *friends, *map(str, sources)],
    capture_output=True, text=True)
print(result.stdout, end=''); print(result.stderr, end='')
if result.returncode: raise SystemExit(result.returncode)
result = subprocess.run([str(java), '-Xms16m', '-Xmx128m', '-XX:+UseSerialGC', '-cp', str(out / 'classes') + os.pathsep + cp,
    'dev.puitheme.CouiSurfaceMathCheckKt'], capture_output=True, text=True)
print(result.stdout, end=''); print(result.stderr, end='')
raise SystemExit(result.returncode)
