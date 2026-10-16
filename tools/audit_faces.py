# Finds faces whose texture does not resolve.
#
# A model's faces name a texture by "#something", and the something has to be defined either in
# the model or in whatever it inherits from — the mod's own parents included. A face whose name
# resolves to nothing is drawn as the missing texture, which is one wrong face on an otherwise
# fine block, and it is invisible to a check that only asks whether the model file exists.
import io
import json
import os

ROOT = "src/main/resources/assets/thaumadds/models"


def load(path):
    if not os.path.exists(path):
        return None
    return json.load(io.open(path, encoding="utf-8"))


def resolve(model, path, seen=None):
    """The model's own textures, with everything it inherits from poured in underneath."""
    seen = seen or set()
    if path in seen:
        return {}
    seen.add(path)
    textures = {}
    parent = model.get("parent")
    if parent:
        if parent.startswith("thaumadds:"):
            parent_textures = resolve(load(ROOT + "/" + parent.split(":")[1] + ".json"),
                                      parent, seen)
        elif parent.startswith("minecraft:"):
            parent_textures = {}  # vanilla's own; not ours to check
        else:
            parent_textures = resolve(load(ROOT + "/" + parent + ".json"), parent, seen)
        textures.update(parent_textures)
    textures.update(model.get("textures") or {})
    return textures


def follow(textures, value, depth=0):
    """A texture value, following the #indirections the format allows."""
    if depth > 8 or not isinstance(value, str) or not value.startswith("#"):
        return value
    return follow(textures, textures.get(value[1:]), depth + 1)


def main():
    broken = []
    for folder, _, files in os.walk(ROOT):
        for name in sorted(files):
            if not name.endswith(".json"):
                continue
            path = os.path.join(folder, name)
            model = load(path)
            elements = model.get("elements")
            if not elements:
                continue
            textures = resolve(model, os.path.relpath(path, ROOT)[:-5].replace(os.sep, "/"))
            for index, element in enumerate(elements):
                for face, description in (element.get("faces") or {}).items():
                    value = follow(textures, description.get("texture"))
                    if value is None or (isinstance(value, str) and value.startswith("#")):
                        broken.append("%s element %d %s -> %s"
                                      % (os.path.relpath(path, ROOT), index, face,
                                         description.get("texture")))
    print("faces that resolve to nothing: %d" % len(broken))
    for line in broken:
        print("  ", line)


if __name__ == "__main__":
    main()
