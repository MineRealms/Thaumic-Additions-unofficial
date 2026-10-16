# Compares the port's blocks/item models and blockstates against upstream's, to the extent the two
# formats can be compared at all.
#
# 1.12 wrote blockstates with forge_marker and a "normal"/"inventory" variant list; the port has
# plain modern blockstates, so the files differ by construction and only the *variant keys* are
# worth comparing. Models are closer: the modern format is the old one, so a model that differs
# from upstream's in anything but its namespace is worth a look.
import io
import json
import os
import re
import zipfile

UPSTREAM = r"G:\DOWNLOAD-EDGE\ThaumicAdditions-1.12.2-12.7.9.jar"


def normalize(node):
    """Ignores the namespace and the blocks/ vs block/ spelling, which differ by construction."""
    if isinstance(node, dict):
        return {k: normalize(v) for k, v in node.items()}
    if isinstance(node, list):
        return [normalize(v) for v in node]
    if isinstance(node, str):
        text = re.sub(r"thaumadditions:blocks?/", "@", node)
        text = re.sub(r"thaumadds:block/", "@", text)
        text = re.sub(r"(thaumadditions|thaumadds):", "@", text)
        text = re.sub(r"^minecraft:block/", "", text)
        text = re.sub(r"^block/", "", text)
        return text
    return node


# Keys that carry no meaning in the modern format and are expected to be absent.
IGNORED = ("__coment", "groups", "name", "display", "ambientocclusion", "gui3d")


def strip(node):
    if isinstance(node, dict):
        return {k: strip(v) for k, v in node.items() if k not in IGNORED}
    if isinstance(node, list):
        return [strip(v) for v in node]
    return node


def compare(archive, entries, upstream_folder, local_folder, label):
    differing = []
    for name in sorted(os.listdir(local_folder)):
        if not name.endswith(".json"):
            continue
        source = upstream_folder + "/" + name
        if source not in entries:
            continue
        mine = strip(normalize(json.load(io.open(os.path.join(local_folder, name), encoding="utf-8"))))
        theirs = strip(normalize(json.loads(archive.read(source).decode("utf-8"))))
        if mine != theirs:
            differing.append((name, mine, theirs))
    print("%s that differ from upstream: %d" % (label, len(differing)))
    for name, mine, theirs in differing:
        print("  === %s" % name)
        for key in sorted(set(mine) | set(theirs)):
            if mine.get(key) != theirs.get(key):
                print("      %-12s port=%s" % (key, json.dumps(mine.get(key))[:120]))
                print("      %-12s up  =%s" % ("", json.dumps(theirs.get(key))[:120]))


def main():
    archive = zipfile.ZipFile(UPSTREAM)
    entries = set(archive.namelist())
    compare(archive, entries, "assets/thaumadditions/models/block",
            "src/main/resources/assets/thaumadds/models/block", "block models")
    compare(archive, entries, "assets/thaumadditions/models/item",
            "src/main/resources/assets/thaumadds/models/item", "item models")


if __name__ == "__main__":
    main()
