# Translates 1.12's research tree into the modern Thaumcraft's datapack form.
#
# Upstream built it in Java with a pair of builders; TC4R reads a JSON catalogue that any namespace
# may contribute to, so the port ships its own at data/thaumadds/thaumcraft/research/default.json
# and the loader merges it with the modern mod's own two hundred entries.
#
# Three things do not carry across, and each is handled rather than guessed at:
#
#   * Upstream's parents point at Thaumcraft's own research, which the modern mod renamed and
#     reorganised. Where a name can be matched with confidence it is; where it cannot, the child
#     hangs off the port's own root rather than dangling. That keeps every entry reachable, at the
#     cost of those entries no longer waiting on the modern mod's progression.
#   * The root entry is flagged AUTO_UNLOCK, because everything else hangs off it and a root that
#     must itself be researched is a tree that never opens. The modern catalogue's own root carries
#     the same flag.
#   * A stage becomes a page: the first is text, and the second lists the recipes it unlocks.
import glob
import io
import json
import os
import re

SOURCE = "H:/MinecraftMods/_ta-analysis/src/main/java/org/zeith/thaumicadditions/init/KnowledgeTAR.java"
LANG_DIR = "H:/MinecraftMods/_ta-analysis/src/main/resources/assets/thaumadditions/lang"
LANG_SOURCE = LANG_DIR + "/en_us.lang"
OUT = "src/main/resources/data/thaumadds/thaumcraft/research/default.json"

# The catalogue's page text is looked up per locale, so each locale's json gets its own copy of
# the text upstream wrote for it. Upstream shipped the same entries translated; the port has to
# carry them across the same way it carried the English.
LOCALES = {
    "en_us": LANG_DIR + "/en_us.lang",
    "zh_cn": LANG_DIR + "/zh_cn.lang",
}

CATEGORY = "THAUMADDITIONS"
ROOT_KEY = "TAR_THAUMADDS"

# One entry draws itself as a named item rather than by its own name, because the thing it is about
# is a Thaumcraft seed rather than anything of the port's.
ICON_BY_KEY = {"TAR_ELDRITCH": "void_seed"}

# Upstream's parents that the modern mod renamed. The rest it reorganised away.
# Upstream named a Thaumcraft 4 entry that is called something else here. The right-hand side has
# to be a key that exists in the installed TC4R; validate_parents below is what keeps that honest,
# because the game's own complaint about a bad one aborts the whole research load and names a
# single parent per attempt.
TC4_PARENTS = {
    "BASEARTIFICE": "BASICARTIFACE",
    "BASEELDRITCH": "ELDRITCHMINOR",
    "ELEMENTALTOOLS": "ELEMENTALPICK",
    "CRYSTALFARMER": "ESSENTIACRYSTAL",
    "WARDEDJARS": "JARVOID",
}

# The installed TC4R, kept in the workspace so this does not need the network.
TC4R_JARS = [
    "local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20740/thaumcraft-forge-0.1.0-20740.jar",
    "local-repo/dev/tc4port/thaumcraft-neoforge/0.1.0-20740/thaumcraft-neoforge-0.1.0-20740.jar",
]


def validate_parents(entries):
    """Fails when an entry hangs off a parent the installed TC4R does not have."""
    import glob
    import zipfile
    known = set()
    for jar in TC4R_JARS:
        if not os.path.exists(jar):
            continue
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                if name.startswith("data/thaumcraft/thaumcraft/research/") and name.endswith(".json"):
                    catalogue = json.loads(archive.read(name).decode("utf-8"))
                    known.update(entry["key"] for entry in catalogue.get("entries", []))
        break
    if not known:
        print("no installed TC4R found; skipping the parent check")
        return
    own = {entry["key"] for entry in entries}
    for entry in entries:
        for parent in entry.get("parents", []):
            if parent not in known and parent not in own:
                raise SystemExit("%s hangs off %s, which is neither ours nor TC4R's"
                                 % (entry["key"], parent))

# Where each upstream meta flag lands in the modern format.
# The icon frames a 1.12 research entry could wear. Only two of the four survive: SPIKY and HEX
# were the other two frame shapes, and the modern research catalogue has just the round one, so
# those entries come out round like everything else.
FLAGS = {"HIDDEN", "ROUND"}

# The name in setBaseInfo is what the entry is called, not always the item it is drawn as.
ICON_ALIASES = {
    "essence_salt": "salt_essence",
    "brass_jar": "jar_brass",
    "thaumium_jar": "jar_thaumium",
    "eldritch_jar": "jar_eldritch",
    "mithrillium_jar": "jar_mithrillium",
    "adaminite_jar": "jar_adaminite",
    "mithminite_jar": "jar_mithminite",
    "mithrillium": "mithrillium_ingot",
    "adaminite": "adaminite_ingot",
    "mithminite": "mithminite_ingot",
    "arcane_cake": "cake",
    "totems": "dawn_totem",
    "seal_symbols": "seal_symbol",
    "thaumadds": "knowledge_tome",
    "mob_summoning": "dna_sample",
    "crystal_water": "crystal_block",
    "enchanted_golden_apple": "cake",
    "void_crop": "void_seed",
    "blue_wolf": "blue_bone",
}


def blobs(text):
    """Every `new REB()...buildAndRegister();` blob, in file order."""
    out = []
    for match in re.finditer(r"new REB\(\)", text):
        end = text.find("buildAndRegister()", match.start())
        if end >= 0:
            out.append(text[match.start():end])
    return out


def key_of(blob):
    found = re.search(r'setBaseInfo\("!?([A-Za-z_0-9]+)"', blob)
    return found.group(1) if found else None


def parse(blob, own_keys, items):
    base = re.search(
        r'setBaseInfo\("!?([A-Za-z_0-9]+)",\s*"([^"]*)",\s*(-?\d+),\s*(-?\d+)', blob)
    if not base:
        return None
    key, icon, column, row = base.group(1), base.group(2), int(base.group(3)), int(base.group(4))

    parents = []
    found = re.search(r"setParents\(([^)]*)\)", blob)
    if found:
        for token in re.findall(r'"([^"]+)"', found.group(1)):
            # "METALLURGY@2" means "two steps past METALLURGY"; the modern format takes the key.
            name = token.split("@")[0]
            if name in TC4_PARENTS:
                parents.append(TC4_PARENTS[name])
            elif name in own_keys:
                parents.append(name)
            elif key != ROOT_KEY and ROOT_KEY not in parents:
                parents.append(ROOT_KEY)

    flags = ["AUTO_UNLOCK"] if key == ROOT_KEY else []
    meta = re.search(r"setMeta\(([^)]*)\)", blob)
    if meta:
        for token in re.findall(r"EnumResearchMeta\.([A-Z]+)", meta.group(1)):
            if token in FLAGS and token not in flags:
                flags.append(token)

    # What the second stage unlocks.
    recipes = []
    for block in re.findall(r"setRecipes\(([^)]*)\)", blob):
        for token in re.findall(r'"([^"]+)"', block):
            name = token.split(":")[-1]
            if name not in recipes:
                recipes.append(name)

    # The entry's own picture: the name it was given, an alias, or the thing its recipes make.
    icon_name = ICON_BY_KEY.get(key, ICON_ALIASES.get(icon, icon))
    if icon_name not in items and recipes:
        icon_name = recipes[0]
    if icon_name not in items:
        return None

    pages = [{
        "type": "TEXT",
        "value": "tc.research_page.%s.1" % key,
        "aspects": {},
        "compound_blueprint": "",
        "recipe_link_policy": "DISABLED",
        "recipe_ids": [],
    }]
    if recipes:
        # Upstream's setRecipes names a recipe by its own name, and the port's files are named
        # after what they make, so the two are matched through the item a recipe produces rather
        # than by the string. A name with no recipe here is dropped rather than left dangling: the
        # book looks a page's recipes up and shows nothing for one that is not there.
        linked = []
        for name in recipes:
            item = ICON_ALIASES.get(name, name)
            found = RECIPE_BY_RESULT.get(item)
            if found and found not in linked:
                linked.append(found)
        # One page per recipe, on the page that matches what kind of recipe it is and links to
        # exactly one: the book refuses to load a page that names several, and a page whose recipe
        # is of another kind shows nothing. A name with no recipe here gets no page at all.
        for identifier, page_type in linked:
            pages.append({
                "type": page_type,
                "value": "",
                "aspects": {},
                "compound_blueprint": "",
                "recipe_link_policy": "COMPLETED_SINGLE",
                "recipe_ids": [identifier],
            })

    return {
        "key": key,
        "category": CATEGORY,
        "aspects": {},
        "display_column": column,
        "display_row": row,
        "complexity": 1,
        "icon": {"type": "STACK", "stack": {"id": "thaumadds:" + icon_name}},
        "flags": flags,
        "parents": parents,
        "hidden_parents": [],
        "siblings": [],
        "object_triggers": [],
        "entity_triggers": [],
        "aspect_triggers": [],
        "warp": 0,
        "pages": pages,
    }


source = io.open(SOURCE, encoding="utf-8", errors="replace").read()
all_blobs = blobs(source)
own_keys = {key_of(b) for b in all_blobs if key_of(b)}
items = {os.path.basename(p)[:-5]
         for p in glob.glob("src/main/resources/assets/thaumadds/models/item/*.json")}

def recipes_by_result():
    """Every recipe the generators have written, keyed by the item it makes.

    The id keeps the folder it was written in, because that is the id it is loaded under."""
    found = {}
    root = "src/main/resources/data/thaumadds/recipes"
    for folder, _, files in os.walk(root):
        for name in files:
            if not name.endswith(".json"):
                continue
            document = json.load(io.open(os.path.join(folder, name), encoding="utf-8"))
            result = (document.get("result") or {}).get("item")
            if not result:
                continue
            relative = os.path.relpath(os.path.join(folder, name), root).replace(os.sep, "/")
            kind = (document.get("type") or "").split(":")[-1]
            page = {
                "thaumcraft:arcane_shaped": "ARCANE_CRAFTING",
                "thaumcraft:arcane_shapeless": "ARCANE_CRAFTING",
                "thaumcraft:infusion": "INFUSION_CRAFTING",
                "thaumcraft:crucible": "CRUCIBLE_CRAFTING",
            }.get(document.get("type"), "NORMAL_CRAFTING")
            found.setdefault(result.split(":")[-1],
                             ("thaumadds:" + relative[:-5], page))
    return found


RECIPE_BY_RESULT = recipes_by_result()

entries, skipped = [], []
for blob in all_blobs:
    parsed = parse(blob, own_keys, items)
    if parsed is None:
        skipped.append(key_of(blob) or "?")
    else:
        entries.append(parsed)

validate_parents(entries)

catalogue = {
    "categories": [{
        "key": CATEGORY,
        "icon": "thaumadds:textures/gui/thaumonomicon_icon.png",
        "background": "thaumcraft:textures/gui/gui_researchback.png",
        "sort_order": 6,
    }],
    "entries": entries,
}
os.makedirs(os.path.dirname(OUT), exist_ok=True)


def read_stages(path):
    """Upstream's research_stage.<mod>:<name>.<n> lines, keyed by name and stage."""
    stages = {}
    if not os.path.exists(path):
        return stages
    for line in io.open(path, encoding="utf-8", errors="replace"):
        if not line.startswith("research_stage."):
            continue
        text_key, _, text = line.partition("=")
        parts = text_key.split(":")[-1].rsplit(".", 1)
        if len(parts) == 1:
            # One entry in upstream's file has no stage number at all; it is its first stage.
            stages[(parts[0], 1)] = text.strip().replace("<BR>", "\n")
        elif parts[1].isdigit():
            stages[(parts[0], int(parts[1]))] = text.strip().replace("<BR>", "\n")
    return stages


def read_names(path):
    """Upstream's research_name.<mod>:<name> lines, which are the entries' titles."""
    names = {}
    if not os.path.exists(path):
        return names
    for line in io.open(path, encoding="utf-8", errors="replace"):
        if not line.startswith("research_name."):
            continue
        text_key, _, text = line.partition("=")
        name = text_key.split(":")[-1]
        if name and text.strip():
            names[name] = text.strip()
    return names


def read_block_names(path):
    """Upstream's tile.<mod>:<name>.name lines, which name the blocks that have no entry title."""
    names = {}
    if not os.path.exists(path):
        return names
    for line in io.open(path, encoding="utf-8", errors="replace"):
        text_key, _, text = line.partition("=")
        if not text_key.startswith("tile.") or not text_key.rstrip().endswith(".name"):
            continue
        name = text_key.split(":")[-1].rsplit(".", 1)[0]
        if name and text.strip():
            names[name] = text.strip()
    return names


# The name each entry was given in setBaseInfo, which is what the text lines are keyed by — the
# entry's own key is not it.
name_by_key = {}
for blob in all_blobs:
    blob_key = key_of(blob)
    base = re.search(r'setBaseInfo\("!?([A-Za-z_0-9]+)",\s*"([^"]*)"', blob)
    if blob_key and base:
        name_by_key[blob_key] = base.group(2)

ENGLISH_STAGES = read_stages(LANG_SOURCE)




def tc4r_research_name(key, locale):
    """Thaumcraft's own title for one of its entries, from the installed jar."""
    import zipfile
    for jar in TC4R_JARS:
        if not os.path.exists(jar):
            continue
        with zipfile.ZipFile(jar) as archive:
            path = "assets/thaumcraft/lang/%s.json" % locale
            if path not in archive.namelist():
                continue
            catalogue = json.loads(archive.read(path).decode("utf-8"))
            return catalogue.get("tc.research_name." + key)
        break
    return None


# Two entries upstream gave no title of their own: one is named after its block, which upstream
# did put in the language file under the block's own name, and the other points straight at a
# Thaumcraft entry instead of carrying prose, so it wears that entry's title.
TITLE_FROM_UPSTREAM_BLOCK = {"TAR_CRYSTAL_LAMP": "crystal_lamp"}
TITLE_FROM_TC4R = {"TAR_ELDRITCH": "ELDRITCHMINOR"}


def fallback_title(entry_key, locale):
    block = TITLE_FROM_UPSTREAM_BLOCK.get(entry_key)
    if block is not None:
        title = read_block_names(LOCALES[locale]).get(block)
        if title:
            return title
    parent = TITLE_FROM_TC4R.get(entry_key)
    if parent is not None:
        return tc4r_research_name(parent, locale)
    return None

# Upstream wrote its entries in stages, and every entry but one had more than one; the modern
# page list is flat, so each stage becomes a page of its own. The catalogue was written with a
# single page, so the rest are inserted here, before the crafting page.
inserted = 0
for entry in entries:
    name = name_by_key.get(entry["key"])
    # Only stages that actually have text become pages. An entry upstream never wrote words for —
    # there is one, and it points at another mod's entry rather than carrying prose of its own —
    # would otherwise name a page key nothing defines, and the book would print the key.
    stages = sorted(stage for (entry_name, stage) in ENGLISH_STAGES
                    if entry_name == name and ENGLISH_STAGES[(entry_name, stage)].strip())
    text_pages = [{
        "type": "TEXT",
        "value": "tc.research_page.%s.%d" % (entry["key"], stage),
        "aspects": {},
        "compound_blueprint": "",
        "recipe_link_policy": "DISABLED",
        "recipe_ids": [],
    } for stage in stages]
    entry["pages"] = text_pages + [page for page in entry["pages"] if page["type"] != "TEXT"]
    inserted += len(text_pages)

# The text itself, into each locale's own lang file. Three lines per entry and one per page:
#
#   tc.research_name.<KEY>      what the entry is called, which the book prints as its title
#   tc.research_text.<KEY>      the line under it; upstream wrote none, so it is empty rather
#                               than absent — the book prints the key when a line is missing
#   tc.research_page.<KEY>.<n>  the page text
#
# The titles are the reason this exists: without them every entry in the book was headed by its
# own key, because 1.12 spelled this research_name.<mod>:<name> and nothing reads that any more.
written = 0
untitled = []
for lang_name, lang_source in sorted(LOCALES.items()):
    stages = read_stages(lang_source)
    titles = read_names(lang_source)
    lang_path = "src/main/resources/assets/thaumadds/lang/%s.json" % lang_name
    lang = json.load(io.open(lang_path, encoding="utf-8"))
    for entry in entries:
        name = name_by_key.get(entry["key"])
        if name is None:
            continue
        title = titles.get(name) or fallback_title(entry["key"], lang_name)
        if title is None:
            untitled.append("%s (%s)" % (entry["key"], name))
        else:
            lang["tc.research_name.%s" % entry["key"]] = title
            written += 1
        lang.setdefault("tc.research_text.%s" % entry["key"], "")
        for stage in sorted(stage for (entry_name, stage) in stages if entry_name == name):
            text = stages[(name, stage)]
            if text:
                lang["tc.research_page.%s.%d" % (entry["key"], stage)] = text
                written += 1
    io.open(lang_path, "w", encoding="utf-8", newline="\n").write(
        json.dumps(dict(sorted(lang.items())), indent=2, ensure_ascii=False) + "\n")

io.open(OUT, "w", encoding="utf-8", newline="\n").write(json.dumps(catalogue, indent=2) + "\n")

print("wrote %d research entries, %d pages, %d lines of text across %d locales"
      % (len(entries), inserted, written, len(LOCALES)))
if untitled:
    print("entries upstream never named:", sorted(set(untitled)))
if skipped:
    print("skipped:", skipped)
