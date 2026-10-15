# -*- coding: utf-8 -*-
"""Cross-check every texture a model JSON references against what the port actually ships.

Reports three groups: references to another namespace (thaumcraft / minecraft), references to
the mod's own namespace whose file is absent, and refs that resolve. Run from the project root.
"""
import collections
import glob
import json
import os
import sys

ASSETS = os.path.join("src", "main", "resources", "assets", "thaumadds")

refs = collections.defaultdict(set)
for path in glob.glob(os.path.join(ASSETS, "models", "**", "*.json"), recursive=True):
    rel = os.path.relpath(path, ASSETS).replace(os.sep, "/")
    try:
        doc = json.load(open(path, encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001 - report and continue
        print("PARSE FAIL", rel, exc)
        continue
    for value in (doc.get("textures") or {}).values():
        if isinstance(value, str) and not value.startswith("#"):
            refs[value].add(rel)

local = set()
for path in glob.glob(os.path.join(ASSETS, "textures", "**", "*.png"), recursive=True):
    local.add(os.path.relpath(path, os.path.join(ASSETS, "textures")).replace(os.sep, "/")[:-4])

external, dangling = [], []
for ref, sources in sorted(refs.items()):
    namespace, _, texpath = ref.partition(":")
    if not texpath:  # a bare path is a minecraft one
        namespace, texpath = "minecraft", ref
    if namespace != "thaumadds":
        external.append((ref, sorted(sources)))
    elif texpath not in local:
        dangling.append((ref, sorted(sources)))

print("=== references into another namespace (%d) ===" % len(external))
for ref, sources in external:
    print("   %-46s <- %s" % (ref, ", ".join(sources)))
print()
print("=== own-namespace references with no file (%d) ===" % len(dangling))
for ref, sources in dangling:
    print("   %-46s <- %s" % (ref, ", ".join(sources)))
print()
print("resolved own-namespace references: %d" % (len(refs) - len(external) - len(dangling)))
sys.exit(0)
