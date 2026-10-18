"""Look up classes in `dexdump -d` text dumps of SystemUI / SystemUIPlugin (collected from the phone, kept in .local-private).

    python tools/dev/dexindex.py <dump folder> build                 index every Class # block (index.json in the folder)
    python tools/dev/dexindex.py <dump folder> find <regex>          class descriptors matching the regex
    python tools/dev/dexindex.py <dump folder> 'Lcom/x/Y;' [lines]   print one class

Collect a dump: adb pull the APK (pm path com.android.systemui; the plugin is /system_ext/app/SystemUIPlugin/SystemUIPlugin.apk),
then `dexdump -d classes.dex > classesN.txt` for each dex (dexdump is in the SDK build-tools).
"""
import json
import os
import re
import sys


def build(dump):
    index = {}
    for name in sorted(os.listdir(dump)):
        if not name.endswith('.txt'):
            continue
        with open(os.path.join(dump, name), 'rb') as f:
            offset = 0
            start = None
            for line in f:
                if line.startswith(b'Class #'):
                    start = offset
                elif start is not None and line.startswith(b'  Class descriptor  : '):
                    index[line.split(b"'")[1].decode('utf-8', 'replace')] = [name, start]
                    start = None
                offset += len(line)
    with open(os.path.join(dump, 'index.json'), 'w') as out:
        json.dump(index, out)
    return index


def load(dump):
    path = os.path.join(dump, 'index.json')
    if os.path.exists(path):
        with open(path) as f:
            return json.load(f)
    return build(dump)


def show(dump, desc, maxlines=None):
    index = load(dump)
    if desc not in index:
        print('missing', desc)
        return
    name, start = index[desc]
    with open(os.path.join(dump, name), 'rb') as f:
        f.seek(start)
        first = True
        count = 0
        for line in f:
            if line.startswith(b'Class #') and not first:
                break
            first = False
            sys.stdout.write(line.decode('utf-8', 'replace'))
            count += 1
            if maxlines and count >= maxlines:
                break


if __name__ == '__main__':
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    folder, command = sys.argv[1], sys.argv[2]
    if command == 'build':
        print(len(build(folder)))
    elif command == 'find':
        pattern = re.compile(sys.argv[3])
        for key, value in sorted(load(folder).items()):
            if pattern.search(key):
                print(key, value[0])
    else:
        show(folder, command, int(sys.argv[3]) if len(sys.argv) > 3 else None)
