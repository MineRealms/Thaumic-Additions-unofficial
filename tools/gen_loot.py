# Writes the loot tables the port never had.
#
# 1.12 dropped a block's item in code, so nothing in its resources said what a block was worth.
# 1.20.1 asks for a loot table, and a block without one drops nothing at all — which is what
# ninety per cent of this mod's blocks were doing. This writes the obvious table for every block
# that has an item, and a proper crop table for the two that grow something.
import io
import json
import os
import glob

BLOCKS = "src/main/resources/assets/thaumadds/blockstates"
ITEM_MODELS = "src/main/resources/assets/thaumadds/models/item"
LOOT = "src/main/resources/data/thaumadds/loot_tables/blocks"
NAMESPACE = "thaumadds"


def self_drop(name):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": "%s:%s" % (NAMESPACE, name)}],
        }],
    }


def crop(seed, produce, max_age):
    """The usual crop table: the seed always, the produce only when it is ripe."""
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{
                "type": "minecraft:item",
                "name": seed,
                "functions": [
                    {"function": "minecraft:set_count",
                     "count": {"min": 1.0, "max": 2.0, "type": "minecraft:uniform"}},
                    {"function": "minecraft:explosion_decay"},
                ],
            }],
        }, {
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": produce}],
            "conditions": [{
                "condition": "minecraft:block_state_property",
                "block": "%s:%s" % (NAMESPACE, "vis_crop" if seed.endswith("vis_pod") else "void_crop"),
                "properties": {"age": str(max_age)},
            }, {"condition": "minecraft:survives_explosion"}],
        }],
    }


# The two crops, from the seeds and fruit the port already registers.
CROPS = {
    "vis_crop": ("thaumadds:vis_pod", "thaumadds:vis_pod", 7),
    "void_crop": ("thaumadds:void_fruit", "thaumadds:void_fruit", 7),
}
# Blocks that are structure rather than content: they have a blockstate but no item, so there is
# nothing for them to drop.
SKIP = {"aura_totem", "essentia_sink_aux"}

blocks = sorted(os.path.basename(p)[:-5] for p in glob.glob(BLOCKS + "/*.json"))
with_item = {os.path.basename(p)[:-5] for p in glob.glob(ITEM_MODELS + "/*.json")}
existing = {os.path.basename(p)[:-5] for p in glob.glob(LOOT + "/*.json")}

written = 0
for name in blocks:
    if name in SKIP or name in existing:
        continue
    if name in CROPS:
        seed, produce, age = CROPS[name]
        table = crop(seed, produce, age)
    elif name in with_item:
        table = self_drop(name)
    else:
        print("  no item and no rule, left alone:", name)
        continue
    io.open(os.path.join(LOOT, name + ".json"), "w", encoding="utf-8", newline="\n").write(
        json.dumps(table, indent=2) + "\n")
    written += 1

print("wrote %d loot tables; %d blocks now have one of %d"
      % (written, len(existing) + written, len(blocks)))
