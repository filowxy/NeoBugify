"""
Cross version compatibility probe.

The build targets Minecraft 1.21.1 / NeoForge 21.1.x, but neoforge.mods.toml used to declare
minecraft [1.21.1,1.22) and neoforge [21.1.0,), which Maven version ordering widens to every
1.21.x release and every NeoForge line up to 21.11.x. This script takes the mixins' method
references and the access transformer targets and asks Mojang's official mappings of another
version whether those members are still there.

Vanilla mappings are not the whole story (NeoForge patches some members), but a member missing
from vanilla is certainly missing at runtime, which is enough to show whether the declared
range is honest.
"""
import io
import os
import re
import sys
import urllib.request

SRC = r'D:\debugify-neoforge\src\main\java'
RES = r'D:\debugify-neoforge\src\main\resources'
CACHE = r'D:\debugify-neoforge\_verify\maps'
os.makedirs(CACHE, exist_ok=True)


def manifest():
    with urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json',
                                timeout=120) as r:
        import json
        return json.load(r)


def mappings_for(version):
    path = os.path.join(CACHE, version + '.txt')
    if not os.path.exists(path):
        url = next(v['url'] for v in manifest()['versions'] if v['id'] == version)
        with urllib.request.urlopen(url, timeout=120) as r:
            import json
            info = json.load(r)
        data_url = info['downloads']['client_mappings']['url']
        with urllib.request.urlopen(data_url, timeout=300) as r:
            raw = r.read()
        if raw[:2] == b'PK':
            # some versions ship the mappings zipped
            import zipfile
            with zipfile.ZipFile(io.BytesIO(raw)) as z:
                name = [n for n in z.namelist() if n.endswith('.txt')][0]
                raw = z.read(name)
        open(path, 'wb').write(raw)
    return path


def parse_mappings(path):
    """mojmap class -> set of member names.

    Mojang ships the *named* side on the left:  "com.mojang.blaze3d.Blaze3D -> fda:" and
        "    9:10:void youJustLostTheGame() -> a".  Lines starting with '#' are comments
    (sourceFile metadata) and must not disturb the current class.
    """
    out = {}
    cur = None
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.rstrip('\n')
            if not line.strip() or line.lstrip().startswith('#'):
                continue
            if not line.startswith((' ', '\t')):
                left, _, right = line.partition(' -> ')
                name = left.strip()
                if name and ' ' not in name:
                    cur = name
                    out.setdefault(cur, set())
                else:
                    cur = None
            elif cur:
                left, _, _right = line.partition(' -> ')
                toks = left.split()
                if toks:
                    name = toks[-1].split('(')[0]
                    if name:
                        out[cur].add(name)
    return out


# ---- what the mixins reference -------------------------------------------------------
method_refs = set()   # (class, method)
type_refs = set()     # class
for dirpath, _, files in os.walk(SRC):
    if os.sep + 'mixins' + os.sep not in dirpath + os.sep:
        continue
    for f in sorted(files):
        if not f.endswith('.java'):
            continue
        src = open(os.path.join(dirpath, f), encoding='utf-8').read()
        imports = {m.group(1).rsplit('.', 1)[1]: m.group(1)
                   for m in re.finditer(r'^import\s+([\w.$]+);', src, re.M)}
        targets = [imports.get(m.group(1), m.group(1))
                   for m in re.finditer(r'@Mixin\(\s*(?:value\s*=\s*)?([\w.$]+)\.class', src)]
        targets += [m.group(1).replace('/', '.')
                    for m in re.finditer(r'@Mixin\(\s*(?:value\s*=\s*)?"([\w/$]+)"', src)]
        if not targets:
            continue
        names = set()
        for m in re.finditer(r'method\s*=\s*(?:\{[^}]*\}|"[^"]*")', src, re.S):
            names.update(s for s in re.findall(r'"([^"]*)"', m.group(0)) if '/' not in s and '.' not in s)
        for t in targets:
            for n in names:
                method_refs.add((t, n))
        for raw in set(re.findall(r'"(L[\w/$]+;[^"]*)"', src)):
            type_refs.add(raw[1:].split(';', 1)[0].replace('/', '.'))

at_file = os.path.join(RES, 'META-INF', 'accesstransformer.cfg')
at_targets = set()
if os.path.exists(at_file):
    for line in open(at_file, encoding='utf-8'):
        line = line.strip()
        if line and not line.startswith('#'):
            parts = line.split()
            if len(parts) == 2 and parts[1].startswith('net.minecraft'):
                at_targets.add(parts[1].replace('$', '.'))

print('mixins reference', len(method_refs), 'methods and', len(type_refs | at_targets), 'types')
print()

for version in sys.argv[1:]:
    path = mappings_for(version)
    mm = parse_mappings(path)
    missing_cls = sorted(c for c in (type_refs | at_targets) if c not in mm)
    missing_meth = sorted((c, m) for (c, m) in method_refs
                          if c in mm and m not in mm[c])
    gone_cls = sorted(c for (c, m) in missing_meth if c not in mm)
    broken = len(missing_meth) + len(missing_cls)
    total = len(method_refs) + len(type_refs | at_targets)
    print(f'=== {version} ===')
    print(f'  references: {total}   broken: {broken}   ({100.0 * broken / total:.0f}%)')
    if missing_cls:
        print('  classes that no longer exist:')
        for c in missing_cls:
            print('    -', c)
    if gone_cls:
        print('  hosts of missing methods that are also gone:')
        for c in gone_cls:
            print('    -', c)
    for c, m in missing_meth[:40]:
        print(f'    - {c}#{m}')
    if len(missing_meth) > 40:
        print(f'    ... and {len(missing_meth) - 40} more')
    print()
