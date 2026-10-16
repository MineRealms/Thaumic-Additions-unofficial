# Writes the mod's infusion and crucible recipes as datapack JSON.
#
# These used to be transcribed by hand from 1.12's RecipesTAR.java, and a hand transcription of
# forty-odd recipes is a transcription with holes in it: nineteen of them — the jars, the machines,
# the ingots above mithrillium, the cake — were simply never written down, so the mod shipped with
# things nothing could make and no way to tell that from a recipe that was merely hard to find.
#
# So the source is read instead, by parse_upstream_recipes.py, and this turns what it read into
# the port's spelling. The substitutions are the same ones the arcane pass uses, and are listed in
# gen_recipes.py.
import io
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_recipes import ASPECTS, PRIMALS, SUBSTITUTIONS  # noqa: E402

SOURCE = "tools/upstream_recipes.json"
OUT = "src/main/resources/data/thaumadds/recipes/infusion"
CRUCIBLE_OUT = "src/main/resources/data/thaumadds/recipes/crucible"

# Upstream's own registry fields -> the ids the port registers them under. The rule below would
# get most of them by lower-casing; these are the ones where it would not.
OWN = {
    "ENTITY_CELL": "dna_sample",       # the port kept 1.12's name for the item, not the field
    "MITHMINITE_HANDLE": "mithminite_handle",
    "VOID_SEED": "void_seed",
}


def snake(name):
    """A 1.12 identifier as a modern path.

    <p>Two spellings are in play: the mod's own fields are CamelCase (`elementalHoe`), and
    Minecraft's are SCREAMING_SNAKE (`WATER_BUCKET`). Only a capital that follows a lower-case
    letter starts a new word — otherwise every letter of the second kind gets its own underscore.
    """
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


# Thaumcraft 4 fields -> Thaumcraft's modern ids. Meta variants (plate 2, ingots 1) are handled by
# the meta table below, because one field name there covers several items.
TC4 = {
    "amber": "thaumcraft:amber",
    "alumentum": "thaumcraft:alumentum",
    "quicksilver": "thaumcraft:quicksilver",
    "filter": "thaumcraft:essentia_filter",
    "mechanismSimple": "mechanismSimple",
    "mechanismComplex": "mechanismComplex",
    "morphicResonator": "morphicResonator",
    "visResonator": "visResonator",
    "fabric": "fabric",
    "salisMundus": "salisMundus",
    "primordialPearl": "thaumcraft:primordial_pearl",
    "voidSeed": "thaumcraft:void_seed",
    "amber2": "thaumcraft:amber",
    "thaumometer": "thaumcraft:thaumometer",
    "handMirror": "thaumcraft:hand_mirror",
    "elementalHoe": "thaumcraft:elemental_hoe",
    "elementalShovel": "thaumcraft:elemental_shovel",
    "elementalAxe": "thaumcraft:elemental_axe",
    "elementalPick": "thaumcraft:elemental_pickaxe",
    "voidSword": "thaumcraft:void_sword",
    "thaumiumAxe": "thaumcraft:thaumium_axe",
    "focusPouch": "thaumcraft:focus_pouch",
    "bathSalts": "thaumcraft:bath_salts",
    "phial": "thaumcraft:essence_phial",
    "sanityChecker": "thaumcraft:sanity_checker",
    "brain": "thaumcraft:zombie_brain",
    "hungryChest": "thaumcraft:hungry_chest",
    "centrifuge": "thaumcraft:alchemical_centrifuge",
    "bellows": "thaumcraft:arcane_bellows",
    "jarVoid": "thaumcraft:void_jar",
    "jarBrain": "thaumcraft:brain_jar",
    "stoneArcane": "thaumcraft:arcane_stone",
    "slabArcaneStone": "thaumcraft:arcane_stone_slab",
    "plankGreatwood": "thaumcraft:greatwood_planks",
    "lampArcane": "thaumcraft:arcane_lamp",
    "shimmerleaf": "thaumcraft:shimmerleaf",
    "vishroom": "thaumcraft:vishroom",
    "metalBlockThaumium": "thaumcraft:thaumium_block",
    # Thaumcraft has no void metal block in this version; nine of the ingot are the same
    # thing in a block, and are what the altar is fed here.
    "metalBlockVoid": "thaumcraft:void_ingot",
    "airCrystalCluster": "thaumcraft:air_crystal_cluster",
    "travellerBoots": "thaumcraft:boots_of_the_traveller",
    "crystalAir": "thaumcraft:air_crystal_cluster",
    "crystalEarth": "thaumcraft:earth_crystal_cluster",
    "crystalEntropy": "thaumcraft:entropy_crystal_cluster",
    "crystalFire": "thaumcraft:fire_crystal_cluster",
    "crystalOrder": "thaumcraft:order_crystal_cluster",
    "crystalWater": "thaumcraft:water_crystal_cluster",
    "crystalTaint": "thaumcraft:mixed_crystal_cluster",
}

# `ItemsTC.ingots` and `ItemsTC.plate` are one field with a meta per metal.
TC4_META = {
    ("ingots", 0): "minecraft:iron_ingot",
    ("ingots", 1): "thaumcraft:thaumium_ingot",
    ("ingots", 2): "minecraft:gold_ingot",
    ("ingots", 3): "thaumcraft:void_ingot",
    ("plate", 0): "minecraft:iron_ingot",
    ("plate", 1): "thaumcraft:thaumium_ingot",
    ("plate", 2): "minecraft:copper_ingot",
    ("plate", 3): "thaumcraft:void_ingot",
    ("baubles", 0): "thaumcraft:goggles",
    ("baubles", 2): "thaumcraft:hover_girdle",
    ("modules", 1): "thaumcraft:golem_core",
}

# Minecraft's own, where the field name is not the id.
VANILLA = {
    "GOLDEN_APPLE": "minecraft:golden_apple",
    "WHEAT_SEEDS": "minecraft:wheat_seeds",
    "ENCHANTING_TABLE": "minecraft:enchanting_table",
    "ANVIL": "minecraft:anvil",
    "DISPENSER": "minecraft:dispenser",
    "BONE": "minecraft:bone",
    "CAKE": "minecraft:cake",
    "NETHER_STAR": "minecraft:nether_star",
    "FEATHER": "minecraft:feather",
    "FISH": "minecraft:cod",
    "NETHERBRICK": "minecraft:nether_brick",
    "SKULL": "minecraft:skeleton_skull",
    "WATER_BUCKET": "minecraft:water_bucket",
    "GOLD_BLOCK": "minecraft:gold_block",
    "SEA_LANTERN": "minecraft:sea_lantern",
}

# The enchanted golden apple is the same item with meta 1.
VANILLA_META = {("GOLDEN_APPLE", 1): "minecraft:enchanted_golden_apple"}

unmapped = []


def substitute(value):
    """Whatever stands in for a 1.12 component, in the port's spelling."""
    return SUBSTITUTIONS.get(value, value)


def item_of(component):
    """One component or result, as a modern item id."""
    kind = component["kind"]
    value = component["value"]
    meta = component.get("meta", 0)

    if kind == "tag":
        return substitute(value)
    if kind in ("essence", "salt"):
        # Aspect-carrying items. The port's essences take their aspect on creation rather than
        # from the recipe, so the ingredient is the item without it.
        return "thaumadds:salt_essence" if kind == "salt" else "thaumcraft:crystal_essence"
    if value.endswith(".defInst()"):
        value = value[: -len(".defInst()")]

    if value.startswith(("ItemsTAR.", "BlocksTAR.")):
        field = value.split(".", 1)[1]
        if field in OWN:
            return "thaumadds:" + OWN[field]
        if field.endswith("_JAR"):
            return "thaumadds:jar_" + field[: -len("_JAR")].lower()
        return "thaumadds:" + snake(field)

    if value.startswith(("ItemsTC.", "BlocksTC.")):
        field = value.split(".", 1)[1]
        if (field, meta) in TC4_META:
            return substitute(TC4_META[(field, meta)])
        if field in TC4:
            return substitute(TC4[field])
        unmapped.append(value)
        return "thaumadds:unmapped_" + snake(field)

    if value.startswith(("Items.", "Blocks.")):
        field = value.split(".", 1)[1]
        if (field, meta) in VANILLA_META:
            return VANILLA_META[(field, meta)]
        if field in VANILLA:
            return VANILLA[field]
        unmapped.append(value)
        return "minecraft:" + snake(field)

    if value.startswith("FluidUtil.getFilledBucket("):
        # The one upstream result that is a fluid rather than an item: a bucket of the mod's own
        # crystal water. Upstream asked for the filled bucket of a fluid it registered no bucket
        # for; the port registers one, so the call names it.
        return "thaumadds:crystal_water_bucket"

    if value == "primordialPearl":
        return substitute("primordialPearl")
    if value == "nitor":
        return substitute("nitor")

    unmapped.append(value)
    return "thaumadds:unmapped_" + snake(value.split(".")[-1])


def aspects_of(recipe):
    """The vis cost, in the port's aspect vocabulary."""
    costs = {}
    primals = recipe["aspects"].get("@primals", 0)
    if primals:
        for aspect in PRIMALS:
            costs[aspect] = primals
    for aspect, amount in recipe["aspects"].items():
        if aspect == "@primals" or aspect not in ASPECTS:
            continue
        modern = ASPECTS[aspect]
        costs[modern] = costs.get(modern, 0) + amount
    return costs


def infusion(recipe):
    result = recipe["result"]
    components = [item_of(component) for component in recipe["components"]]
    return {
        "type": "thaumcraft:infusion",
        "research": recipe["research"],
        "central": {"item": item_of(recipe["central"])},
        "components": [{"item": component} for component in components],
        "result": {"count": result.get("count", 1), "item": item_of(result)},
        "instability": recipe["instability"],
        "aspects": aspects_of(recipe),
    }


def crucible(recipe):
    return {
        "type": "thaumcraft:crucible",
        "research": recipe["research"],
        "catalyst": {"item": item_of(recipe["catalyst"])},
        "result": {"count": recipe["result"].get("count", 1), "item": item_of(recipe["result"])},
        "aspects": aspects_of(recipe),
    }


def thaumcraft_items():
    """Every item Thaumcraft registers, read from the installed jar's own language file."""
    import zipfile
    for jar in ("local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20740/"
                "thaumcraft-forge-0.1.0-20740.jar",):
        if not os.path.exists(jar):
            continue
        with zipfile.ZipFile(jar) as archive:
            lines = json.loads(
                archive.read("assets/thaumcraft/lang/en_us.json").decode("utf-8"))
        break
    else:
        return None
    return {key[len("item.thaumcraft."):] for key in lines if key.startswith("item.thaumcraft.")}


def validate(items_written):
    """Fails on an id Thaumcraft does not have, which the game would only report as a recipe
    that silently does not exist."""
    known = thaumcraft_items()
    if known is None:
        print("no installed Thaumcraft found; skipping the id check")
        return
    bad = sorted({item for item in items_written
                  if item.startswith("thaumcraft:") and item[len("thaumcraft:"):] not in known})
    if bad:
        raise SystemExit("these Thaumcraft ids do not exist: %s" % bad)


def main():
    recipes = json.load(io.open(SOURCE, encoding="utf-8"))
    written = {"infusion": 0, "crucible": 0}
    used = []
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(CRUCIBLE_OUT, exist_ok=True)
    for recipe in recipes:
        if recipe["name"] == "test":
            # Upstream's own scratch entry, which registers nothing.
            continue
        if recipe["kind"] == "infusion":
            entry = infusion(recipe)
            folder = OUT
        else:
            entry = crucible(recipe)
            folder = CRUCIBLE_OUT
        path = os.path.join(folder, recipe["name"] + ".json")
        io.open(path, "w", encoding="utf-8", newline="\n").write(
            json.dumps(entry, indent=2) + "\n")
        written[recipe["kind"]] += 1

    validate(used)
    print("wrote %d infusion and %d crucible recipes" % (written["infusion"], written["crucible"]))
    if unmapped:
        print("fields with no mapping (they will not resolve):")
        for field in sorted(set(unmapped)):
            print("  ", field)


if __name__ == "__main__":
    main()
