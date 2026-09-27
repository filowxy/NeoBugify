"""
Cross-check that every translation key the code asks for exists in en_us.json, and that
en_us.json has no key the code never asks for. A miss shows up in game as a raw
"neobugify.foo.bar" string, which is easy to miss by eye after a bulk key rename.
"""
import glob
import json
import os
import re

SRC = r'D:\debugify-neoforge\src\main\java'
LANGS = r'D:\debugify-neoforge\src\main\resources\assets\neobugify\lang'

en = json.load(open(os.path.join(LANGS, 'en_us.json'), encoding='utf-8'))
used = set()
for dirpath, _, files in os.walk(SRC):
    for f in files:
        if not f.endswith('.java'):
            continue
        src = open(os.path.join(dirpath, f), encoding='utf-8').read()
        for m in re.finditer(r'"(neobugify\.[A-Za-z0-9_.\-]+)"', src):
            used.add(m.group(1))
        # dynamic prefixes, e.g. "neobugify.fix_explanation." + bugId
        for m in re.finditer(r'"(neobugify\.[a-z_]+)\."', src):
            used.add(m.group(1) + '.*')

missing = []
for key in sorted(used):
    if key.endswith('.*'):
        prefix = key[:-1]
        if not any(k.startswith(prefix) for k in en):
            missing.append(key + '  (dynamic prefix, no key starts with it)')
    elif key not in en:
        missing.append(key)

print('keys used in code :', len(used))
print('keys in en_us.json:', len(en))
print('MISSING:', len(missing))
for m in missing:
    print('  ', m)

unreferenced = [k for k in sorted(en) if k not in used and not any(
    k.startswith(u[:-1]) for u in used if u.endswith('.*'))]
print('en_us keys not referenced by code (informational):', len(unreferenced))
for k in unreferenced[:10]:
    print('  ', k)
