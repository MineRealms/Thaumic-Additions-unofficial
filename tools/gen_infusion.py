# Writes the mod's infusion recipes as datapack JSON.
#
# Same substitution table as the arcane pass, and the same caveat: 1.12's infusion altars were fed
# Thaumcraft 4's plates, mechanisms and Knowledge aspects, and the modern Thaumcraft has none of
# them. What stands in is listed in gen_recipes.py.
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_recipes import ASPECTS, PRIMALS, SUBSTITUTIONS  # noqa: E402

OUT = "src/main/resources/data/thaumadds/recipes/infusion"


def ing(value):
    """One altar component: a substituted item, or a tag when it names one."""
    if value in SUBSTITUTIONS:
        return {"item": SUBSTITUTIONS[value]}
    return {"item": value}


def infusion(name, central, components, result, count, instability, vis, primals):
    entry = {
        "type": "thaumcraft:infusion",
        "central": {"item": central},
        "components": [ing(c) for c in components],
        "result": {"count": count, "item": result},
        "instability": instability,
    }
    costs = {}
    if primals:
        for aspect in PRIMALS:
            costs[aspect] = primals
    for aspect, amount in vis.items():
        if aspect in ASPECTS:
            costs[ASPECTS[aspect]] = costs.get(ASPECTS[aspect], 0) + amount
    if costs:
        entry["aspects"] = costs
    return entry


# Transcribed from 1.12's RecipesTAR.init(), keeping its order. The first three were listed above
# the tools and cover the mod's own metals.
RECIPES = [
    infusion("mithrillium_ingot", "thaumcraft:thaumium_ingot",
             ["thaumcraft:quicksilver", "thaumadds:mithrillium_nugget", "thaumcraft:quicksilver",
              "thaumadds:mithrillium_nugget", "thaumcraft:quicksilver", "thaumadds:mithrillium_nugget"],
             "thaumadds:mithrillium_ingot", 1, 5,
             {"CRYSTAL": 30, "ENERGY": 15, "ELDRITCH": 10, "METAL": 30}, 0),
    infusion("void_elemental_hoe", "thaumcraft:elemental_hoe",
             ["thaumcraft:primordial_pearl", "plate_void", "plate_void"],
             "thaumadds:void_elemental_hoe", 1, 6,
             {"ELDRITCH": 40, "PLANT": 80, "LIFE": 100}, 0),
    infusion("void_elemental_shovel", "thaumcraft:elemental_shovel",
             ["thaumcraft:primordial_pearl", "plate_void", "plate_void"],
             "thaumadds:void_elemental_shovel", 1, 6,
             {"ELDRITCH": 40, "CRAFT": 100, "EARTH": 150}, 0),
    infusion("void_elemental_axe", "thaumcraft:elemental_axe",
             ["thaumcraft:primordial_pearl", "plate_void", "plate_void"],
             "thaumadds:void_elemental_axe", 1, 6,
             {"ELDRITCH": 40, "WATER": 100, "PLANT": 150}, 0),
    infusion("void_elemental_pickaxe", "thaumcraft:elemental_pickaxe",
             ["thaumcraft:primordial_pearl", "plate_void", "plate_void"],
             "thaumadds:void_elemental_pickaxe", 1, 6,
             {"ELDRITCH": 40, "FIRE": 80, "METAL": 300, "SENSES": 50}, 0),
    infusion("void_seed", "minecraft:wheat_seeds",
             ["thaumcraft:primordial_pearl", "thaumcraft:void_seed", "thaumcraft:salis_mundus",
              "thaumcraft:void_seed", "thaumcraft:void_ingot", "thaumcraft:void_seed"],
             "thaumadds:void_seed", 1, 5, {"DARKNESS": 100}, 20),
    infusion("shadow_enchanter", "minecraft:enchanting_table",
             ["thaumadds:mithrillium_plate", "ingotThaumium", "thaumadds:mithrillium_resonator",
              "mechanismComplex", "thaumadds:mithrillium_plate", "ingotThaumium",
              "thaumadds:mithrillium_resonator", "mechanismComplex", "thaumadds:disenchant_fabric"],
             "thaumadds:shadow_enchanter", 1, 10, {"MIND": 500}, 120),
    infusion("mithminite_hood", "thaumadds:adaminite_hood",
             ["thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumcraft:brain_jar", "thaumadds:mithrillium_resonator",
              "minecraft:sea_lantern", "thaumadds:fragnant_pendant", "thaumadds:seal_globe",
              "thaumcraft:arcane_lamp", "thaumcraft:sanity_checker"],
             "thaumadds:mithminite_hood", 1, 10,
             {"PROTECT": 200, "FLUX": 150, "LIGHT": 200, "WATER": 250, "MIND": 250}, 0),
    infusion("mithminite_robe", "thaumadds:adaminite_robe",
             ["thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithrillium_resonator", "thaumadds:levitation_device",
              "fabric", "morphicResonator"],
             "thaumadds:mithminite_robe", 1, 10,
             {"PROTECT": 200, "LIFE": 250, "FLUX": 150, "FIRE": 250}, 0),
    infusion("mithminite_belt", "thaumadds:adaminite_belt",
             ["thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithrillium_resonator", "thaumadds:levitation_device"],
             "thaumadds:mithminite_belt", 1, 10, {"PROTECT": 200, "FLUX": 150}, 0),
    infusion("mithminite_boots", "thaumadds:adaminite_boots",
             ["thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithminite_fabric",
              "thaumadds:mithminite_fabric", "thaumadds:mithrillium_resonator",
              "thaumcraft:boots_of_the_traveller"],
             "thaumadds:mithminite_boots", 1, 10, {"PROTECT": 200, "AIR": 250, "FLUX": 150}, 0),
    infusion("shadow_beam_staff", "thaumcraft:resonator",
             ["thaumadds:mithrillium_plate", "plateIron", "thaumadds:mithrillium_plate", "plateIron",
              "thaumadds:mithrillium_plate", "plateIron"],
             "thaumadds:shadow_beam_staff", 1, 10, {"ELDRITCH": 500}, 50),
    infusion("void_anvil", "minecraft:anvil",
             ["plate_void", "plate_void", "thaumcraft:void_ingot", "plate_void", "plate_void",
              "thaumcraft:void_ingot", "plate_void", "plate_void", "thaumcraft:void_ingot",
              "plate_void", "plate_void", "thaumcraft:void_ingot"],
             "thaumadds:void_anvil", 1, 7, {"ELDRITCH": 200}, 0),
    infusion("crystal_bag", "thaumcraft:focus_pouch",
             ["thaumcraft:air_crystal_cluster", "thaumcraft:earth_crystal_cluster",
              "thaumcraft:entropy_crystal_cluster", "thaumcraft:fire_crystal_cluster",
              "thaumcraft:order_crystal_cluster", "thaumcraft:mixed_crystal_cluster",
              "thaumcraft:water_crystal_cluster"],
             "thaumadds:crystal_bag", 1, 6, {"VOID": 50}, 10),
    infusion("blue_bone", "minecraft:bone",
             ["thaumadds:mithrillium_nugget", "thaumcraft:salis_mundus",
              "thaumadds:mithrillium_nugget", "thaumcraft:salis_mundus"],
             "thaumadds:blue_bone", 1, 1, {"EXCHANGE": 6}, 0),
    infusion("the_beheader", "thaumcraft:thaumium_axe",
             ["thaumadds:mithrillium_plate", "minecraft:skeleton_skull",
              "thaumadds:mithrillium_plate", "plate_copper", "thaumadds:mithrillium_plate",
              "minecraft:skeleton_skull"],
             "thaumadds:the_beheader", 1, 8,
             {"UNDEAD": 100, "ENTROPY": 70, "MAGIC": 85, "AVERSION": 120, "DEATH": 70}, 0),
    infusion("traveller_belt", "thaumcraft:nitor",
             ["thaumcraft:crystal_essence", "thaumcraft:crystal_essence", "fabric", "fabric",
              "minecraft:feather", "minecraft:cod"],
             "thaumadds:traveller_belt", 1, 1, {"MOTION": 100, "FLIGHT": 100}, 0),
    infusion("striding_belt", "thaumadds:traveller_belt",
             ["thaumcraft:amber", "minecraft:feather", "thaumcraft:amber", "minecraft:feather"],
             "thaumadds:striding_belt", 1, 3, {"MOTION": 150, "FLIGHT": 150, "AIR": 200}, 0),
    infusion("meteor_belt", "thaumadds:striding_belt",
             ["thaumcraft:crystal_essence", "minecraft:nether_brick", "thaumcraft:crystal_essence",
              "minecraft:nether_brick"],
             "thaumadds:meteor_belt", 1, 5, {"MOTION": 250, "FLIGHT": 250, "FIRE": 200}, 0),
    infusion("adaminite_sword", "thaumcraft:void_sword",
             ["thaumadds:adaminite_plate", "thaumadds:adaminite_plate",
              "thaumcraft:primordial_pearl", "thaumadds:adaminite_plate"],
             "thaumadds:adaminite_sword", 1, 6, {"MAGIC": 70, "AVERSION": 150, "METAL": 60}, 0),
    infusion("mithminite_blade", "thaumadds:adaminite_sword",
             ["thaumadds:mithminite_plate", "thaumadds:mithminite_plate",
              "thaumadds:mithrillium_resonator", "thaumadds:mithminite_plate"],
             "thaumadds:mithminite_blade", 1, 1, {"AVERSION": 250}, 0),
    infusion("mithminite_handle", "thaumadds:mithminite_nugget",
             ["thaumadds:mithminite_plate", "thaumadds:mithminite_ingot",
              "thaumadds:mithminite_ingot"],
             "thaumadds:mithminite_handle", 1, 1, {"TOOL": 100, "SOUL": 150}, 0),
    infusion("mithminite_scythe", "thaumcraft:primordial_pearl",
             ["thaumadds:mithminite_blade", "thaumadds:mithminite_plate",
              "thaumadds:mithminite_handle", "thaumadds:mithminite_plate"],
             "thaumadds:mithminite_scythe", 1, 1, {"AVERSION": 250, "TOOL": 150, "MAGIC": 100}, 0),
]

os.makedirs(OUT, exist_ok=True)
for entry in RECIPES:
    name = entry["result"]["item"].split(":")[1]
    io.open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n").write(
        json.dumps(entry, indent=2) + "\n")
print("wrote %d infusion recipes to %s" % (len(RECIPES), OUT))
