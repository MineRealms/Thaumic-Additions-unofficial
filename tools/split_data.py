# Writes the 1.21.1 node's copy of the mod's data files.
#
# The two targets do not spell the same folders, and nothing says so: the shared tree in
# src/main/resources/data is in 1.20.1's spelling, so on 1.21.1 every recipe, loot table and tag
# in it is simply not found — the files are there, the loader never looks in those directories, and
# nothing is logged. The mod loads with no recipes and drops nothing, silently.
#
# 1.21 renamed each registry's folder to the registry's own name, singular:
#
#     recipes/ -> recipe/        loot_tables/ -> loot_table/      structures/ -> structure/
#     tags/blocks/ -> tags/block/    tags/items/ -> tags/item/    tags/fluids/ -> tags/fluid/
#
# and moved a recipe result's item id from "item" to "id", which is not a rename of the key's
# meaning but of its spelling and cannot be a search-and-replace: "item" is also what an
# ingredient is called, and only the result's may move.
#
# The canonical tree is the source of truth and this is derived from it, so the generators keep
# writing in one place. Run it after them.
import io
import json
import os
import shutil

SOURCE = "src/main/resources/data"
TARGET = "versions/1.21.1-neoforge/src/main/resources/data"

RENAMES = [
    ("recipes", "recipe"),
    ("loot_tables", "loot_table"),
    ("structures", "structure"),
    ("tags/blocks", "tags/block"),
    ("tags/items", "tags/item"),
    ("tags/fluids", "tags/fluid"),
    ("tags/entity_types", "tags/entity_type"),
]


def target_path(relative):
    """The 1.21.1 path for a 1.20.1 one, folder by folder."""
    parts = relative.split("/")
    for index in range(len(parts)):
        for old, new in RENAMES:
            old_parts = old.split("/")
            if parts[index:index + len(old_parts)] == old_parts:
                return "/".join(parts[:index] + [new] + parts[index + len(old_parts):])
    return relative


def migrate(path, document):
    """A recipe result's item id is spelled differently on 1.21.1."""
    if "/recipe/" not in "/" + path and not path.startswith("recipe/"):
        return document
    result = document.get("result")
    if isinstance(result, dict) and "item" in result:
        result["id"] = result.pop("item")
    return document


def main():
    if os.path.exists(TARGET):
        shutil.rmtree(TARGET)
    written = 0
    for folder, _, files in os.walk(SOURCE):
        for name in files:
            source = os.path.join(folder, name)
            relative = os.path.relpath(source, SOURCE).replace("\\", "/")
            destination = os.path.join(TARGET, target_path(relative))
            os.makedirs(os.path.dirname(destination), exist_ok=True)
            if name.endswith(".json"):
                document = json.load(io.open(source, encoding="utf-8"))
                document = migrate(target_path(relative), document)
                io.open(destination, "w", encoding="utf-8", newline="\n").write(
                    json.dumps(document, indent=2, ensure_ascii=False) + "\n")
            else:
                shutil.copyfile(source, destination)
            written += 1
    print("wrote %d data files to %s" % (written, TARGET))


if __name__ == "__main__":
    main()
