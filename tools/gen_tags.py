# Writes the mining tags the blocks never had.
#
# 1.12 gave a block its tool class in the block's own constructor, so nothing in its resources said
# what mined it. 1.20.1 asks for a tag instead, and a block in none of them mines with anything at
# the speed of a bare hand — which is what thirty-eight of this mod's forty-three blocks did.
#
# The table is written out rather than derived. The material lives in each block class's
# constructor, not at the registration site, so deriving it means following the class and reading
# its arguments; at forty-three blocks an explicit list is shorter, and it can be read and checked,
# which a derivation cannot.
import glob
import io
import json
import os

OUT = "src/main/resources/data/minecraft/tags/blocks/mineable"

PICKAXE = [
    # The machines: metal and stone, from the fuel-wasting furnaces down to the anvil.
    "adaminite_smelter", "mithrillium_smelter", "mithminite_smelter",
    "aspect_combiner", "aura_charger", "aura_disperser", "flux_concentrator",
    "crystal_bore", "crystal_crusher", "crystal_lamp", "crystal_block",
    "entity_summoner", "shadow_enchanter", "growth_chamber", "essentia_sink",
    "void_anvil", "seal", "thaumic_lectern",
    # The metal blocks and the lamps.
    "adaminite_block", "mithrillium_block", "mithminite_block",
    "amber_lamp", "chiseled_amber_block",
    # The jars, which are glass and want a pickaxe like glass does.
    "jar_brass", "jar_thaumium", "jar_eldritch", "jar_mithrillium",
    "jar_adaminite", "jar_mithminite",
    # The totems are carved stone.
    "dawn_totem", "twilight_totem",
]

AXE = [
    # Anything made of wood.
    "taintwood_planks", "chiseled_greatwood", "iron_framed_greatwood",
    "brass_plated_silverwood",
]

HOE = [
    # The plants, which want a hoe the way a crop does.
    "puriflower", "vis_crop", "void_crop",
    "taintkin", "taintkin_lit",
]

# Nothing mines it: it is structure, and breaking it is not how it goes away.
NONE = ["essentia_sink_aux", "cake"]

present = {os.path.basename(p)[:-5]
           for p in glob.glob("src/main/resources/assets/thaumadds/blockstates/*.json")}

by_tag = {"pickaxe": [], "axe": [], "hoe": []}
unlisted = []
for name in sorted(present):
    if name in NONE:
        continue
    for tag, members in (("pickaxe", PICKAXE), ("axe", AXE), ("hoe", HOE)):
        if name in members:
            by_tag[tag].append("thaumadds:" + name)
            break
    else:
        unlisted.append(name)

os.makedirs(OUT, exist_ok=True)
for tag, members in by_tag.items():
    path = os.path.join(OUT, tag + ".json")
    existing = []
    if os.path.exists(path):
        existing = json.load(io.open(path, encoding="utf-8")).get("values", [])
    ours = [v for v in existing if isinstance(v, str) and not v.startswith("thaumadds:")]
    merged = sorted(set(ours + members))
    io.open(path, "w", encoding="utf-8", newline="\n").write(
        json.dumps({"replace": False, "values": merged}, indent=2) + "\n")
    print("%-8s %2d entries" % (tag, len(merged)))

if unlisted:
    print("in no list, and so in no tag — decide for each:", unlisted)
