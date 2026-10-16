# Reads 1.12's RecipesTAR.java and hands back every recipe it registers.
#
# The port's recipes were transcribed by hand, and a hand transcription of seventy-odd recipes is
# a transcription with holes in it: the jars, the machines and the essences were all simply never
# written down, so the mod shipped with eighteen things nothing could make. This reads the source
# instead, so the list is complete and can be re-read whenever it changes.
#
# It returns the calls as written — upstream's identifiers and all — and leaves the mapping to
# the port's own names to the caller, which is where the substitutions live.
import io
import json
import os
import re
import sys


def split_arguments(text):
    """The arguments of one call, split on the commas that are not inside brackets or strings."""
    arguments = []
    depth = 0
    quoted = False
    current = []
    for index, character in enumerate(text):
        if quoted:
            current.append(character)
            if character == '"' and text[index - 1] != "\\":
                quoted = False
            continue
        if character == '"':
            quoted = True
            current.append(character)
        elif character in "([{":
            depth += 1
            current.append(character)
        elif character in ")]}":
            depth -= 1
            current.append(character)
        elif character == "," and depth == 0:
            arguments.append("".join(current).strip())
            current = []
        else:
            current.append(character)
    if current:
        arguments.append("".join(current).strip())
    return arguments


def calls(source, name):
    """Every <name>(...) call in the source, as its argument list."""
    out = []
    for match in re.finditer(re.escape(name) + r"\(", source):
        start = match.end()
        depth = 1
        index = start
        quoted = False
        while index < len(source) and depth:
            character = source[index]
            if quoted:
                if character == '"' and source[index - 1] != "\\":
                    quoted = False
            elif character == '"':
                quoted = True
            elif character == "(":
                depth += 1
            elif character == ")":
                depth -= 1
            index += 1
        out.append(split_arguments(source[start:index - 1]))
    return out


def number(text):
    """A literal where a number is expected; the odd wildcard is not one."""
    text = text.strip()
    return int(text) if text.isdigit() else 0


def item_of(expression):
    """One component, as upstream wrote it: an item stack, a field reference or a tag name."""
    expression = expression.strip()
    literal = re.fullmatch(r'"([^"]*)"', expression)
    if literal:
        return {"kind": "tag", "value": literal.group(1)}
    if expression.startswith("new ItemStack("):
        inside = split_arguments(expression[len("new ItemStack("):-1])
        return {"kind": "stack", "value": inside[0].strip(),
                "count": number(inside[1]) if len(inside) > 1 else 1,
                "meta": number(inside[2]) if len(inside) > 2 else 0}
    essence = re.fullmatch(r"AspectUtil\.crystalEssence\((.+)\)", expression)
    if essence:
        return {"kind": "essence", "value": essence.group(1).strip()}
    salt = re.fullmatch(r"AspectUtil\.salt\((.+)\)", expression)
    if salt:
        return {"kind": "salt", "value": salt.group(1).strip()}
    return {"kind": "field", "value": expression}


def aspects_of(expression):
    """The vis cost, as {aspect name: amount}, with primals expanded by the caller."""
    expression = expression.strip()
    primals = re.match(r"AspectUtil\.primals\((\d+)\)", expression)
    out = {}
    if primals:
        out["@primals"] = int(primals.group(1))
    for name, amount in re.findall(r"\.add\(([A-Za-z_.0-9]+),\s*(\d+)\)", expression):
        out[name.split(".")[-1]] = int(amount)
    return out


def main(source_path, target):
    source = io.open(source_path, encoding="utf-8", errors="replace").read()

    recipes = []
    for arguments in calls(source, "addInfusionRecipe"):
        # The file declares its own wrapper of the same name; a call whose instability is not a
        # number is that declaration, not a recipe.
        if len(arguments) < 6 or not arguments[3].strip().isdigit():
            continue
        recipes.append({
            "kind": "infusion",
            "name": arguments[0].strip('"'),
            "result": item_of(arguments[1]),
            "research": arguments[2].strip('"'),
            "instability": int(arguments[3]),
            "central": item_of(arguments[4]),
            "aspects": aspects_of(arguments[5]),
            "components": [item_of(a) for a in arguments[6:]],
        })

    for arguments in calls(source, "addCrucibleRecipe"):
        if len(arguments) < 5 or arguments[1].strip().startswith(("int", "String", "AspectList")):
            continue
        recipes.append({
            "kind": "crucible",
            "name": arguments[0].strip('"'),
            "research": arguments[1].strip('"'),
            "result": item_of(arguments[2]),
            "catalyst": item_of(arguments[3]),
            "aspects": aspects_of(arguments[4]),
        })

    io.open(target, "w", encoding="utf-8", newline="\n").write(
        json.dumps(recipes, indent=1, ensure_ascii=False) + "\n")
    counted = {}
    for recipe in recipes:
        counted[recipe["kind"]] = counted.get(recipe["kind"], 0) + 1
    print("read %d recipes: %s" % (len(recipes), counted))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else
         r"H:\MinecraftMods\_ta-analysis\src\main\java\org\zeith\thaumicadditions\init\RecipesTAR.java",
         sys.argv[2] if len(sys.argv) > 2 else "tools/upstream_recipes.json")
