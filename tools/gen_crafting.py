# Writes the mod's plain crafting recipes: the ones that are not arcane and not infusion.
#
# 1.12 registered eighteen of them in RecipesTAR.crafting() — the nugget/ingot/block chains for
# its three metals, the plates, the tainted planks, the lamp, the lectern. None of them came across
# in the port's first pass, so the metals could be infused but not broken down, and the decorative
# blocks had no source at all.
#
# These are ordinary shaped and shapeless recipes, so they are emitted as such rather than as
# Thaumcraft's; the only thing that needs translating is the ingredients, and that table is in
# gen_recipes.py.
import io
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_recipes import SUBSTITUTIONS  # noqa: E402

OUT = "src/main/resources/data/thaumadds/recipes/crafting"

# Ingredients that name a Thaumcraft 4 component the modern mod spells differently, or does not
# have at all. Each one is a deliberate stand-in, like the arcane table.
EXTRA = {
    "nuggetIron": "minecraft:iron_nugget",
    "nuggetBrass": "thaumcraft:copper_nugget",     # brass was a copper alloy in all but name
    "taintLog": "thaumcraft:crusted_taint",        # Thaumcraft has no taint log; its solid taint
                                                   # is the nearest thing a block can be cut from
    "jarBrace": "minecraft:iron_ingot",            # nor a jar brace; the fitting is iron
    "tubeValve": "thaumcraft:essentia_valve",
    "tableWood": "thaumcraft:table",
    "tube": "thaumcraft:essentia_tube",
    "amberBlock": "thaumcraft:amber_block",
}


def ingredient(value):
    if value in EXTRA:
        return {"item": EXTRA[value]}
    if value in SUBSTITUTIONS:
        return {"item": SUBSTITUTIONS[value]}
    if value.startswith("minecraft:") or value.startswith("thaumcraft:") or value.startswith("thaumadds:"):
        return {"item": value}
    return {"item": value}


def shaped(name, result, count, pattern, key):
    return name, {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": {letter: ingredient(value) for letter, value in key.items()},
        "result": {"count": count, "item": result},
    }


def shapeless(name, result, count, ingredients):
    return name, {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [ingredient(value) for value in ingredients],
        "result": {"count": count, "item": result},
    }


def recipes():
    out = []
    # Nine nuggets to an ingot and back, for each of the three metals.
    for metal in ("mithrillium", "adaminite", "mithminite"):
        out.append(shapeless(metal + "_ingot_from_nuggets", "thaumadds:" + metal + "_ingot", 1,
                             ["thaumadds:" + metal + "_nugget"] * 9))
        out.append(shapeless(metal + "_nugget", "thaumadds:" + metal + "_nugget", 9,
                             ["thaumadds:" + metal + "_ingot"]))
        out.append(shapeless(metal + "_block", "thaumadds:" + metal + "_block", 1,
                             ["thaumadds:" + metal + "_ingot"] * 9))
        out.append(shapeless(metal + "_ingot_from_block", "thaumadds:" + metal + "_ingot", 9,
                             ["thaumadds:" + metal + "_block"]))
        out.append(shaped(metal + "_plate", "thaumadds:" + metal + "_plate", 3,
                          ["ppp"], {"p": "thaumadds:" + metal + "_ingot"}))

    out.append(shaped("chiseled_amber_block", "thaumadds:chiseled_amber_block", 4,
                      [" a ", "a a", " a "], {"a": "thaumcraft:amber_block"}))
    out.append(shaped("chiseled_greatwood", "thaumadds:chiseled_greatwood", 3,
                      [" s ", "sps", " s "],
                      {"s": "thaumcraft:greatwood_slab", "p": "thaumcraft:greatwood_planks"}))
    out.append(shapeless("taintwood_planks", "thaumadds:taintwood_planks", 4, ["taintLog"]))
    out.append(shaped("thaumic_lectern", "thaumadds:thaumic_lectern", 1,
                      ["mtm", "www", " w "],
                      {"w": "thaumcraft:greatwood_slab", "m": "ingotThaumium",
                       "t": "thaumcraft:thaumonomicon"}))
    out.append(shaped("essentia_sink", "thaumadds:essentia_sink", 1,
                      ["lvl", "igt", "isi"],
                      {"l": "jarBrace", "v": "tubeValve", "g": "tableWood", "t": "tube",
                       "i": "plateIron", "s": "thaumcraft:greatwood_slab"}))
    out.append(shaped("iron_framed_greatwood", "thaumadds:iron_framed_greatwood", 5,
                      ["npn", "ppp", "npn"],
                      {"n": "nuggetIron", "p": "thaumcraft:greatwood_planks"}))
    out.append(shaped("brass_plated_silverwood", "thaumadds:brass_plated_silverwood", 5,
                      ["npn", "ppp", "npn"],
                      {"n": "nuggetBrass", "p": "plankSilverwood"}))
    out.append(shaped("amber_lamp", "thaumadds:amber_lamp", 3,
                      ["bnb", "aaa", "bnb"],
                      {"b": "minecraft:iron_bars", "n": "nitor", "a": "amberBlock"}))
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    written = recipes()
    for name, entry in written:
        io.open(os.path.join(OUT, name + ".json"), "w", encoding="utf-8", newline="\n").write(
            json.dumps(entry, indent=2) + "\n")
    print("wrote %d crafting recipes to %s" % (len(written), OUT))
    # Upstream had three more that are not here, on purpose: one makes nine quartz from a
    # Thaumcraft nugget the modern mod does not have, one is the knowledge tome rewriting itself
    # and was behind a config switch, and the crystal block's is one of the per-aspect loops that
    # cannot be expressed — see PORT_STATE.md.


if __name__ == "__main__":
    main()
