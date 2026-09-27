"""
Static audit of Debugify's mixins for the NeoForge (no-refmap) porting trap.

Fabric Loom remaps every mixin member reference (method names, @At target owners, field
owners) from mojmap to intermediary at build time via a refmap. NeoForge runs on official
mojmap names and generates no refmap, so those strings are matched *literally* against the
bytecode of the target class.

That makes two classes of bug possible in a port:
  1. a named method that does not exist under that name in the target class;
  2. an @At target whose owner is a subclass of the class that actually owns the
     invocation site (javac resolves inherited calls against the receiver's erasure,
     e.g. Mob.lookAt, not Monster.lookAt) - this is what broke MC-121706.

This script disassembles every @Mixin target from the real Minecraft jar and reports any
method / @At target that cannot be found literally.
"""
import os
import re
import subprocess
import sys
from collections import defaultdict

SRC = r'D:\debugify-neoforge\src\main\java'
JAR = sys.argv[1] if len(sys.argv) > 1 else r'D:\debugify-neoforge\build\moddev\artifacts\neoforge-21.1.251.jar'

cache = {}
hier_cache = {}


def javap(fqcn, flags):
    key = (fqcn, tuple(flags))
    if key in cache:
        return cache[key]
    try:
        out = subprocess.run(['javap'] + flags + ['-classpath', JAR, fqcn],
                             capture_output=True, text=True, timeout=240).stdout
    except Exception:
        out = ''
    cache[key] = out
    return out


def hierarchy(fqcn):
    """[fqcn, superclass, ...] using javap's extends clause."""
    if fqcn in hier_cache:
        return hier_cache[fqcn]
    out = [fqcn]
    seen = {fqcn}
    cur = fqcn
    for _ in range(24):
        decl = javap(cur, ['-p'])
        m = re.search(r'extends\s+([\w.$]+)', decl.split('\n{', 1)[0] if '{' in decl else decl)
        if not m:
            m = re.search(r'^\s*(?:public |abstract |final |strictfp )*class\s+[\w.$]+\s+extends\s+([\w.$]+)',
                          decl, re.M)
        if not m:
            break
        parent = m.group(1)
        # resolve simple names against net.minecraft package (javap prints FQCN already)
        if '.' not in parent:
            pkg = cur.rsplit('.', 1)[0]
            parent = pkg + '.' + parent
        if parent in seen:
            break
        out.append(parent)
        seen.add(parent)
        cur = parent
    hier_cache[fqcn] = out
    return out


def members(fqcn):
    """Union over the class hierarchy of (declared_method_names, method_refs, field_refs).

    An injection into an inherited method is matched against the *superclass* bytecode, so
    every supertype has to be taken into account.
    """
    declared, methods, fields = set(), set(), set()
    for c in hierarchy(fqcn):
        decl = javap(c, ['-p'])
        code = javap(c, ['-p', '-c'])
        for line in decl.splitlines():
            line = line.strip()
            if line.startswith('static {}'):
                declared.add('<clinit>')
                continue
            if '(' not in line or line.startswith('Compiled'):
                continue
            m = re.search(r'([\w$]+)\(', line)
            if m:
                declared.add(m.group(1))
        for line in code.splitlines():
            # javap omits the owner prefix for references to the class being disassembled
            # ("// Method stopRiding:()V" inside ServerPlayer) and labels interface calls
            # "InterfaceMethod"/"InterfaceField", both of which are valid @At owners.
            m = re.search(r'// (?:Interface)?Method ([\w/$]+\.)?([\w$<>]+):?"?(\([^"]*\)[^"]*)"?', line)
            if m:
                owner = m.group(1) or (c.replace('.', '/') + '.')
                methods.add((owner[:-1], m.group(2), m.group(3)))
                continue
            m = re.search(r'// (?:Interface)?Field ([\w/$]+\.)?([\w$]+):(\S+)', line)
            if m:
                owner = m.group(1) or (c.replace('.', '/') + '.')
                fields.add((owner[:-1], m.group(2), m.group(3)))
                continue
            m = re.search(r'// class ([\w/$]+)', line)
            if m:
                fields.add(('TYPE', m.group(1), ''))
    return declared, methods, fields


problems = []
stats = defaultdict(int)

for root, _, files in os.walk(SRC):
    if os.sep + 'mixins' + os.sep not in root + os.sep:
        continue
    for f in sorted(files):
        if not f.endswith('.java'):
            continue
        path = os.path.join(root, f)
        src = open(path, encoding='utf-8').read()
        rel = os.path.relpath(path, SRC)

        imports = {}
        for m in re.finditer(r'^import\s+([\w.$]+);', src, re.M):
            imports[m.group(1).rsplit('.', 1)[1]] = m.group(1)

        targets = []
        for m in re.finditer(r'@Mixin\(\s*(?:value\s*=\s*)?([\w.$]+)\.class', src):
            targets.append(imports.get(m.group(1), m.group(1)))
        for m in re.finditer(r'@Mixin\(\s*(?:value\s*=\s*)?"([\w/$]+)"', src):
            targets.append(m.group(1).replace('/', '.'))
        if not targets:
            continue

        method_names = set()
        for m in re.finditer(r'method\s*=\s*(?:\{[^}]*\}|"[^"]*")', src, re.S):
            for s in re.findall(r'"([^"]*)"', m.group(0)):
                if s and '/' not in s and '.' not in s:
                    method_names.add(s)

        at_targets = set(re.findall(r'"(L[\w/$]+;[^"]*)"', src))
        type_targets = set(re.findall(r'"(L[\w/$]+;)"', src))

        for t in targets:
            declared, seen_methods, seen_fields = members(t)

            for name in sorted(method_names):
                stats['method refs'] += 1
                if name not in declared:
                    problems.append(f'{rel}: method "{name}" not found in {t}')

            for raw in sorted(at_targets):
                if not raw.startswith('L') or ';' not in raw:
                    stats['unparsed'] += 1
                    continue
                owner, rest = raw[1:].split(';', 1)
                if rest.startswith('<'):
                    rest = rest[1:]
                if ':' in rest:
                    stats['field targets'] += 1
                    name, desc = rest.split(':', 1)
                    if (owner, name, desc) in seen_fields:
                        continue
                    alts = sorted({n for (o, n, d) in seen_fields if n == name})
                    hint = (' -> field(s) with that name: ' + ', '.join(alts)) if alts else ''
                    problems.append(f'{rel}: field target {raw} absent in {t}{hint}')
                else:
                    stats['invoke targets'] += 1
                    mm = re.match(r'([\w$]+)\((.*)\)(.*)$', rest)
                    if not mm:
                        stats['unparsed'] += 1
                        continue
                    name, args, ret = mm.group(1), mm.group(2), mm.group(3)
                    if ret in ('V', 'Z', 'B', 'C', 'S', 'I', 'J', 'F', 'D'):
                        ret_desc = ret
                    elif ret.startswith('L'):
                        ret_desc = ret if ret.endswith(';') else ret + ';'
                    else:
                        stats['unparsed'] += 1
                        continue
                    if (owner, name, f'({args}){ret_desc}') in seen_methods:
                        continue
                    alts = sorted({(o, n, d) for (o, n, d) in seen_methods if n == name})
                    hint = (' -> actual: ' + ', '.join(f'{o}.{n}{d}' for o, n, d in alts[:4])) if alts else ''
                    problems.append(f'{rel}: @At target {raw} absent in {t}{hint}')

            for raw in sorted(type_targets):
                stats['type targets'] += 1
                owner = raw[1:].rstrip(';').replace('/', '.')
                if not javap(owner, ['-p']).strip():
                    problems.append(f'{rel}: type target {raw} does not exist')

print('=== mixin reference audit ===')
for k, v in sorted(stats.items()):
    print(f'  {k}: {v}')
print(f'problems: {len(problems)}')
for p in problems:
    print('  ' + p)
