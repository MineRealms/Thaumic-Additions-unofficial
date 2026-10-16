# Writes the mod's arcane crafting recipes as datapack JSON.
#
# 1.12 built these in Java, against Thaumcraft 4's crafting components. The modern Thaumcraft does
# not have most of them: plates and mechanisms were the backbone of that mod's crafting and simply
# do not exist here, and eight of the aspect costs were Thaumic Additions' own Knowledge aspects,
# which TC4R also lacks. Porting the recipes one for one is therefore impossible, and the
# substitutions below are the closest the modern game can offer. Every one is deliberate, and the
# table is here rather than spread through seventy files.
import io
import json
import os

OUT = "src/main/resources/data/thaumadds/recipes/arcane"

# 1.12's component -> what stands in for it, and why.
SUBSTITUTIONS = {
    "plateBrass": "minecraft:copper_ingot",        # brass was a copper alloy in all but name
    "plateIron": "minecraft:iron_ingot",
    "plateThaumium": "thaumcraft:thaumium_ingot",
    "plateMithrillium": "thaumadds:mithrillium_plate",  # the mod's own plates, as tag names
    "plateAdaminite": "thaumadds:adaminite_plate",
    "plateMithminite": "thaumadds:mithminite_plate",
    "plate_iron": "minecraft:iron_ingot",          # ItemsTC.plate, meta 0
    "plate_copper": "minecraft:copper_ingot",      # ItemsTC.plate, meta 2
    "plate_thaumium": "thaumcraft:thaumium_ingot",  # ItemsTC.plate, meta 1
    "plate_void": "thaumcraft:void_ingot",         # ItemsTC.plate, meta 3
    "mechanismSimple": "thaumcraft:nitor",         # the simplest component TC4R does have
    "mechanismComplex": "thaumcraft:resonator",    # and its most complex
    "morphicResonator": "thaumcraft:resonator",
    "visResonator": "thaumcraft:resonator",
    "fabric": "thaumcraft:enchanted_fabric",
    "filter": "thaumcraft:essentia_filter",
    "salisMundus": "thaumcraft:salis_mundus",
    "ingotThaumium": "thaumcraft:thaumium_ingot",
    "ingotVoid": "thaumcraft:void_ingot",
    "nuggetGold": "minecraft:gold_nugget",
    "paneGlass": "minecraft:glass_pane",
    "blockGlass": "minecraft:glass",
    "nitor": "thaumcraft:nitor",
    "scribingTools": "thaumcraft:thaumometer",     # TC4R has no scribing tools
    "curio": "thaumcraft:knowledge_fragment",      # nor curios; a fragment is the nearest trinket
    "plankGreatwood": "thaumcraft:greatwood_planks",
    "plankSilverwood": "thaumcraft:greatwood_planks",  # TC4R has no silverwood planks
    "shimmerleaf": "thaumcraft:shimmerleaf",
    "vishroom": "thaumcraft:vishroom",
    "stoneArcane": "thaumcraft:arcane_stone",
    "slabGreatwood": "thaumcraft:greatwood_slab",
    "slabArcaneStone": "thaumcraft:arcane_stone_slab",
    "jarNormal": "thaumcraft:warded_jar",
    "jarVoid": "thaumcraft:void_jar",
    "levitator": "thaumcraft:arcane_levitator",
    "smelterVoid": "thaumcraft:alchemical_furnace",
    "metalAlchemicalAdvanced": "thaumcraft:advanced_alchemical_construct",
    "thaumonomicon": "thaumcraft:thaumonomicon",
    "thaumometer": "thaumcraft:thaumometer",
    "handMirror": "thaumcraft:hand_mirror",
    "primordialPearl": "thaumcraft:primordial_pearl",
    "alumentum": "thaumcraft:alumentum",
    "voidSeed": "thaumcraft:void_seed",
}

# The mod's own items, which need no substitution.
OWN = "thaumadds:"

# aspect name -> TC4R aspect, for the vis costs. The eight Knowledge aspects are absent on purpose:
# they do not exist here, so those recipes cost less than they did.
ASPECTS = {
    "FIRE": "ignis", "WATER": "aqua", "EARTH": "terra", "AIR": "aer", "ORDER": "ordo",
    "ENTROPY": "perditio", "MAGIC": "praecantatio", "LIFE": "victus", "EXCHANGE": "permutatio",
    "DESIRE": "fames", "FLUX": "vitium", "PLANT": "herba", "METAL": "metallum", "AURA": "auram",
    "ALCHEMY": "permutatio", "PROTECT": "tutamen", "AVERSION": "telum", "ELDRITCH": "alienis",
    "MIND": "cognitio", "COLD": "gelum", "FLIGHT": "volatus", "TOOL": "instrumentum",
    "CRYSTAL": "vitreus", "SOUL": "spiritus", "MAN": "humanus", "BEAST": "bestia",
    "DARKNESS": "tenebrae", "LIGHT": "lux", "SENSES": "sensus", "MECHANISM": "machina",
    "CRAFT": "fabrico", "ENERGY": "potentia", "DEATH": "mortuus", "UNDEAD": "exanimis",
    "MOTION": "motus", "VOID": "vacuos",
    # The Knowledge aspects have no counterpart and are dropped:
    # IMPERIUM CAELES EXITIUM FLUCTUS DRACO VISUM VENTUS INFERNUM
}

# Every primals(n) becomes n of each of the six.
PRIMALS = ["aer", "terra", "ignis", "aqua", "ordo", "perditio"]


def ingredient(value):
    """A key entry, as a substituted item or a direct one."""
    if value in SUBSTITUTIONS:
        return {"item": SUBSTITUTIONS[value]}
    return {"item": value}


def recipe(name, pattern, key, result, count, vis, primals):
    entry = {
        "type": "thaumcraft:arcane_shaped",
        "pattern": pattern,
        "key": {letter: ingredient(value) for letter, value in key.items()},
        "result": {"count": count, "item": result},
    }
    costs = {}
    if primals:
        for aspect in PRIMALS:
            costs[aspect] = primals
    for aspect, amount in vis.items():
        if aspect in ASPECTS:
            costs[ASPECTS[aspect]] = costs.get(ASPECTS[aspect], 0) + amount
    # Always written, even empty: the modern arcane recipe will not load without the key, and the
    # recipes whose whole cost was the Knowledge aspects end up with nothing to put in it.
    entry["vis"] = costs
    return entry


# Transcribed from 1.12's RecipesTAR.init(), in its order.
RECIPES = [
    recipe("mithrillium_smelter", ["bsb", "mcm", "mmm"],
           {"b": "plateBrass", "s": "smelterVoid", "m": "thaumadds:mithrillium_plate",
            "c": "metalAlchemicalAdvanced"},
           "thaumadds:mithrillium_smelter", 1, {"FIRE": 6, "WATER": 2}, 0),
    recipe("adaminite_smelter", ["bsb", "mcm", "mmm"],
           {"b": "plateBrass", "s": "thaumadds:mithrillium_smelter",
            "m": "thaumadds:adaminite_plate", "c": "metalAlchemicalAdvanced"},
           "thaumadds:adaminite_smelter", 1, {"FIRE": 12, "WATER": 6}, 0),
    recipe("mithminite_smelter", ["bsb", "mcm", "mmm"],
           {"b": "plateBrass", "s": "thaumadds:adaminite_smelter",
            "m": "thaumadds:mithminite_plate", "c": "metalAlchemicalAdvanced"},
           "thaumadds:mithminite_smelter", 1, {"FIRE": 24, "WATER": 12}, 0),
    recipe("dna_sample", ["tmt", "rpr", "tmt"],
           {"r": "mechanismSimple", "t": "ingotThaumium", "m": "morphicResonator",
            "p": "plateThaumium"},
           "thaumadds:dna_sample", 1, {}, 6),
    recipe("brass_jar", ["gpg", "gjg", "ggg"],
           {"g": "paneGlass", "p": "plateBrass", "j": "jarNormal"},
           "thaumadds:jar_brass", 1, {}, 0),
    recipe("thaumium_jar", ["gpg", "gjg", "ggg"],
           {"g": "paneGlass", "p": "plate_copper", "j": "thaumadds:jar_brass"},
           "thaumadds:jar_thaumium", 1, {"WATER": 2}, 0),
    recipe("eldritch_jar", ["gpg", "gjg", "ggg"],
           {"g": "paneGlass", "p": "plate_void", "j": "thaumadds:jar_thaumium"},
           "thaumadds:jar_eldritch", 1, {"WATER": 6}, 0),
    recipe("mithrillium_jar", ["gpg", "gjg", "ggg"],
           {"g": "paneGlass", "p": "thaumadds:mithrillium_plate", "j": "thaumadds:jar_eldritch"},
           "thaumadds:jar_mithrillium", 1, {"WATER": 12}, 0),
    recipe("adaminite_jar", ["gpg", "gjg", "ggg"],
           {"g": "paneGlass", "p": "thaumadds:adaminite_plate", "j": "thaumadds:jar_mithrillium"},
           "thaumadds:jar_adaminite", 2, {"WATER": 24}, 0),
    recipe("seal", [" g ", "gwg", " g "],
           {"g": "nuggetGold", "w": "minecraft:white_wool"},
           "thaumadds:seal", 2, {}, 1),
    recipe("twilight_totem", ["sws", "wcw", "sws"],
           {"s": "thaumadds:taintwood_planks", "w": "fabric",
            "c": "thaumcraft:crystal_essence"},
           "thaumadds:twilight_totem", 1, {"ENTROPY": 1, "AIR": 1}, 0),
    recipe("dawn_totem", ["sws", "wcw", "sws"],
           {"s": "plankSilverwood", "w": "thaumcraft:crystal_essence", "c": "shimmerleaf"},
           "thaumadds:dawn_totem", 1, {"ORDER": 1, "AIR": 1}, 0),
    recipe("seal_globe", ["ggg", "grg", "lcl"],
           {"g": "blockGlass", "r": "visResonator", "l": "minecraft:gold_ingot",
            "c": "thaumcraft:crystal_essence"},
           "thaumadds:seal_globe", 1, {}, 1),
    recipe("knowledge_tome", ["s", "c", "t"],
           {"s": "scribingTools", "c": "curio", "t": "thaumonomicon"},
           "thaumadds:knowledge_tome", 1, {}, 1),
    recipe("fragnant_pendant", [" f ", "saf", "mp "],
           {"f": "filter", "s": "salisMundus", "a": "thaumcraft:nitor",
            "m": "thaumcraft:amber", "p": "thaumadds:odour_powder"},
           "thaumadds:fragnant_pendant", 1, {}, 1),
    recipe("bone_eye", [" b ", " eb", "t  "],
           {"b": "minecraft:quartz", "e": "minecraft:spider_eye", "t": "ingotThaumium"},
           "thaumadds:bone_eye", 1, {"ORDER": 1, "AIR": 1}, 0),
    recipe("vis_scribing_tools", [" v ", "vtg", " ga"],
           {"v": "thaumcraft:crystal_essence", "t": "scribingTools", "g": "blockGlass",
            "a": "thaumcraft:essence_phial"},
           "thaumadds:vis_scribing_tools", 1, {}, 1),
    recipe("adaminite_fabric", ["fff", "faf", "fff"],
           {"f": "fabric", "a": "thaumadds:adaminite_ingot"},
           "thaumadds:adaminite_fabric", 2, {}, 4),
    recipe("mithminite_fabric", [" a ", "ama", " a "],
           {"m": "thaumadds:mithminite_ingot", "a": "thaumadds:adaminite_fabric"},
           "thaumadds:mithminite_fabric", 2, {}, 8),
    recipe("levitation_device", ["ini", "cmc", "iai"],
           {"i": "plateIron", "n": "nitor", "c": "mechanismComplex", "m": "levitator",
            "a": "alumentum"},
           "thaumadds:levitation_device", 1, {"AIR": 5, "ORDER": 2}, 0),
    recipe("adaminite_hood", ["fff", "fmf"],
           {"f": "thaumadds:adaminite_fabric", "m": "thaumadds:mithrillium_resonator"},
           "thaumadds:adaminite_hood", 1, {}, 4),
    recipe("adaminite_robe", ["f f", "fmf", "fff"],
           {"f": "thaumadds:adaminite_fabric", "m": "thaumadds:mithrillium_resonator"},
           "thaumadds:adaminite_robe", 1, {}, 4),
    recipe("adaminite_belt", [" f ", "fmf"],
           {"f": "thaumadds:adaminite_fabric", "m": "thaumadds:mithrillium_resonator"},
           "thaumadds:adaminite_belt", 1, {}, 4),
    recipe("adaminite_boots", ["f f", "fmf"],
           {"f": "thaumadds:adaminite_fabric", "m": "thaumadds:mithrillium_resonator"},
           "thaumadds:adaminite_boots", 1, {}, 4),
    recipe("essentia_pistol", ["stt", "gbd", "g  "],
           {"s": "mechanismSimple", "t": "ingotThaumium", "g": "plankGreatwood",
            "b": "minecraft:stone_button", "d": "thaumadds:aura_disperser"},
           "thaumadds:essentia_pistol", 1, {"AIR": 1}, 0),
    recipe("disenchant_fabric", [" f ", "fsf", " f "],
           {"f": "fabric", "s": "salisMundus"},
           "thaumadds:disenchant_fabric", 1, {"EARTH": 1}, 0),
    recipe("crystal_lamp", ["g g", " a ", "g g"],
           {"g": "plankGreatwood", "a": "nitor"},
           "thaumadds:crystal_lamp", 1, {}, 0),
]

os.makedirs(OUT, exist_ok=True)
for entry in RECIPES:
    name = entry["result"]["item"].split(":")[1]
    io.open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n").write(
        json.dumps(entry, indent=2) + "\n")
print("wrote %d arcane recipes to %s" % (len(RECIPES), OUT))
