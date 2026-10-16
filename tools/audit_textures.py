# Compares every texture the port ships against the one it came from.
#
# The assets were brought across by a bulk converter, and a converter that picks the wrong source
# file leaves a texture that is present, that every model resolves, and that is simply not the
# right picture — the one fault a "does this file exist" check cannot see. So this compares the
# pixels: the port's textures are 2x nearest-neighbour upscales of upstream's, which is what the
# converter did, and anything else is either a deliberate change or a mistake.
import io
import os
import zipfile

from PIL import Image

UPSTREAM = r"G:\DOWNLOAD-EDGE\ThaumicAdditions-1.12.2-12.7.9.jar"
ROOT = "src/main/resources/assets/thaumadds/textures"


def upstream_path(relative):
    """Upstream's file for a port one, which spells both folders in the plural."""
    for sub in ("block", "item", "misc", "gui"):
        plural = {"block": "blocks", "item": "items"}.get(sub, sub)
        if relative.startswith(sub + "/"):
            return "assets/thaumadditions/textures/" + plural + "/" + relative[len(sub) + 1:]
    return "assets/thaumadditions/textures/" + relative


def main():
    archive = zipfile.ZipFile(UPSTREAM)
    entries = set(archive.namelist())
    differing = []
    unpaired = []
    for folder, _, files in os.walk(ROOT):
        for name in files:
            if not name.endswith(".png"):
                continue
            local = os.path.join(folder, name)
            relative = os.path.relpath(local, ROOT).replace(os.sep, "/")
            source = upstream_path(relative)
            if source not in entries:
                unpaired.append(relative)
                continue
            mine = Image.open(local)
            theirs = Image.open(io.BytesIO(archive.read(source)))
            if mine.size == theirs.size:
                resized = theirs
            elif mine.size == (theirs.width * 2, theirs.height * 2):
                resized = theirs.resize(mine.size, Image.NEAREST)
            else:
                differing.append((relative, "size %s, upstream %s" % (mine.size, theirs.size)))
                continue
            if resized.convert("RGBA").tobytes() != mine.convert("RGBA").tobytes():
                differing.append((relative, "art is not upstream's"))

    print("textures with no upstream file (the port's own, or renamed): %d" % len(unpaired))
    for relative in unpaired:
        print("  ", relative)
    print("textures that are present but not the upstream art: %d" % len(differing))
    for relative, why in differing:
        print("  %-56s %s" % (relative, why))


if __name__ == "__main__":
    main()
