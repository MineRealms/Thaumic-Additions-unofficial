#!/usr/bin/env python3
"""Legacy (1.12 Forge `forge_marker`) -> modern (1.20.1 / 1.21.1) asset normalizer.

Reads the original Thaumic Additions asset tree and emits a spec-compliant modern
resource-pack tree for the Stonecutter multiversion port:

  * blockstates   : `forge_marker`/`defaults`/`transform` collapsed into plain
                    `variants`; inline `cube_all`/`cross` model definitions are
                    split out into real `models/block/<name>.json` files; the
                    legacy `inventory` variant becomes a `models/item/<name>.json`;
                    a bare `normal` key becomes the empty `""` variant key.
  * block models  : `__coment` / element `name` / `groups` stripped, parents and
                    texture references namespace- and root-remapped.
  * item models   : same cleanup; `overrides` model refs remapped.
  * textures      : `textures/blocks/**` -> `textures/block/**`,
                    `textures/items/**`  -> `textures/item/**`,
                    `textures/armor/<x>_0|_1.png` -> `textures/models/armor/<x>_layer_1|_2.png`;
                    everything else copied verbatim.
  * namespace     : `thaumadditions:` -> `thaumadds:` (thaumcraft:/minecraft: untouched).

Two legacy shapes are loader-specific and are emitted per node instead of into the
shared tree: the OBJ item (`void_thaumometer`) and the fluid block (`crystal_water`).

Run:  python tools/legacy_assets.py [--legacy <dir>] [--dry-run]
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_LEGACY = ROOT.parent / "_ta-analysis/src/main/resources/assets/thaumadditions"

SHARED = ROOT / "src/main/resources/assets/thaumadds"
V_FORGE = ROOT / "versions/1.20.1-forge/src/main/resources/assets/thaumadds"
V_NEO = ROOT / "versions/1.21.1-neoforge/src/main/resources/assets/thaumadds"

TC4R_JAR = ROOT / "local-repo/dev/tc4port/thaumcraft-forge/0.1.0-20740/thaumcraft-forge-0.1.0-20740.jar"

NS = {"thaumadditions": "thaumadds"}
TEXTURE_ROOTS = {"blocks/": "block/", "items/": "item/"}

# Blocks whose legacy asset has no usable modern block-item model.
SKIP_ITEM_MODEL = {"void_crop"}
# Handled outside the generic path (loader-specific / not a plain block model).
SPECIAL_BLOCKSTATES = {"void_thaumometer", "crystal_water"}

report = {
    "blockstates": 0,
    "block_models_generated": [],
    "block_models_copied": 0,
    "item_models": 0,
    "textures": 0,
    "missing_local_textures": set(),
    "missing_local_models": set(),
    "missing_thaumcraft_textures": set(),
    "missing_thaumcraft_models": set(),
    "notes": [],
}


# --------------------------------------------------------------------- helpers
def deep_merge(a, b):
    """Recursive dict merge, `b` wins; lists/scalars are replaced."""
    if not isinstance(a, dict) or not isinstance(b, dict):
        return b
    out = dict(a)
    for k, v in b.items():
        if k in out and isinstance(out[k], dict) and isinstance(v, dict):
            out[k] = deep_merge(out[k], v)
        else:
            out[k] = v
    return out


def remap_ns(s: str) -> str:
    for old, new in NS.items():
        if s.startswith(old + ":"):
            return new + s[len(old):]
    return s


def split_ref(s: str):
    ns, sep, path = s.partition(":")
    if not sep:
        return "minecraft", s
    return ns, path


def tex_ref(s: str) -> str:
    """Remap a texture value (namespace + root). `#var` references pass through."""
    if not isinstance(s, str) or s.startswith("#"):
        return s
    s = remap_ns(s)
    ns, path = split_ref(s)
    for a, b in TEXTURE_ROOTS.items():
        if path.startswith(a):
            path = b + path[len(a):]
            break
    return f"{ns}:{path}"


def model_ref(s: str, default_dir: str = "block") -> str:
    """Remap a model reference; insert the default dir when the path has none."""
    if not isinstance(s, str) or s.startswith("#"):
        return s
    s = remap_ns(s)
    ns, path = split_ref(s)
    if not (path.startswith("block/") or path.startswith("item/")):
        path = f"{default_dir}/{path}"
    return f"{ns}:{path}"


def remap_textures(tex: dict) -> dict:
    return {k: tex_ref(v) for k, v in tex.items()}


def strip_model(doc: dict) -> dict:
    """Drop legacy-only keys that the modern model codec does not understand."""
    doc.pop("__coment", None)
    doc.pop("groups", None)
    for el in doc.get("elements", []):
        el.pop("name", None)
    return doc


def write_json(path: Path, doc, dry_run: bool):
    if dry_run:
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")


def copy_file(src: Path, dst: Path, dry_run: bool):
    if dry_run:
        return
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)


# ------------------------------------------------------------------ conversion
def convert_block_model(path: Path, legacy: Path, dry_run: bool):
    doc = json.loads(path.read_text(encoding="utf-8"))
    doc = strip_model(doc)
    if "parent" in doc:
        doc["parent"] = model_ref(doc["parent"])
    if "textures" in doc:
        doc["textures"] = remap_textures(doc["textures"])
    rel = path.relative_to(legacy / "models")
    write_json(SHARED / "models" / rel, doc, dry_run)
    report["block_models_copied"] += 1


def convert_item_model(path: Path, legacy: Path, dry_run: bool):
    doc = json.loads(path.read_text(encoding="utf-8"))
    doc = strip_model(doc)
    if "parent" in doc:
        doc["parent"] = model_ref(doc["parent"], default_dir="item")
    if "textures" in doc:
        doc["textures"] = remap_textures(doc["textures"])
    for ov in doc.get("overrides", []):
        if "model" in ov:
            ov["model"] = model_ref(ov["model"], default_dir="item")
    rel = path.relative_to(legacy / "models")
    write_json(SHARED / "models" / rel, doc, dry_run)
    report["item_models"] += 1


def variant_entries(val):
    return val if isinstance(val, list) else [val]


def as_vec3(v):
    """1.12 allowed scalar translation/rotation/scale; modern needs a 3-array."""
    if isinstance(v, (int, float)):
        return [v, v, v]
    return v


def convert_display(disp: dict) -> dict:
    """1.12 `display` uses `rotation: [{"x":90},{"y":90}]`; modern wants `[x,y,z]`."""
    out = {}
    for k, v in disp.items():
        if not isinstance(v, dict):
            continue
        nv = {}
        if "translation" in v:
            nv["translation"] = as_vec3(v["translation"])
        if "scale" in v:
            nv["scale"] = as_vec3(v["scale"])
        rot = v.get("rotation")
        if isinstance(rot, list) and rot and isinstance(rot[0], dict):
            acc = {"x": 0, "y": 0, "z": 0}
            for r in rot:
                for axis, val in r.items():
                    acc[axis] = val
            nv["rotation"] = [acc["x"], acc["y"], acc["z"]]
        elif rot is not None:
            nv["rotation"] = as_vec3(rot)
        out[k] = nv
    return out


def build_inline_item_model(merged: dict, self_ref: str) -> dict:
    """Turn a legacy `inventory` variant into a modern `models/item` document."""
    model = merged.get("model", "")
    tex = merged.get("textures", {})
    if "layer0" in tex and "cube" in model:
        # cube_all inventory -> reuse the generated block model (matches batches 2-4)
        return {"parent": self_ref}
    if "layer0" in tex:
        # e.g. forge:item-layer -> item/generated
        return {"parent": "minecraft:item/generated", "textures": {"layer0": tex_ref(tex["layer0"])}}
    return {"parent": self_ref}


def convert_blockstate(path: Path, legacy: Path, dry_run: bool, available_block_models: set):
    """Write the modern blockstate (+ any split-out block model).

    Returns the block-item model plan: `(name, doc)` for an explicit model,
    `(name, None)` for a plain block-parent stub, or `None` when no item model
    is warranted (legacy item model already exists / crop / fluid).
    """
    name = path.stem
    if name in SPECIAL_BLOCKSTATES:
        report["notes"].append(f"{name}: loader-specific, emitted separately (see special_cases)")
        return None
    doc = json.loads(path.read_text(encoding="utf-8"))
    defaults = doc.get("defaults", {})
    variants = doc.get("variants", {})
    is_forge = "forge_marker" in doc
    self_ref = f"thaumadds:block/{name}"

    out_variants = {}
    generated_block_model = None
    item_model_doc = None

    for key, val in variants.items():
        entries = variant_entries(val)
        if key == "inventory":
            merged = deep_merge(defaults, entries[0]) if entries else dict(defaults)
            merged.pop("transform", None)
            item_model_doc = build_inline_item_model(merged, self_ref)
            continue

        out_key = "" if (key == "normal" or "=" not in key) else key
        out_entries = []
        for e in entries:
            merged = deep_merge(defaults, e)
            merged.pop("transform", None)
            merged.pop("custom", None)

            if "textures" in merged or "elements" in merged or "parent" in merged:
                # Inline model definition -> split into a real models/block file.
                parent = model_ref(merged.get("model", "block/block"))
                gen = {"parent": parent}
                if "textures" in merged:
                    gen["textures"] = remap_textures(merged["textures"])
                if parent != self_ref:
                    generated_block_model = gen
                ref = {"model": self_ref}
            else:
                ref = {"model": model_ref(merged["model"])} if "model" in merged else {}
                tgt = ref.get("model")
                if tgt and tgt.startswith("thaumadds:block/") and tgt not in available_block_models:
                    report["missing_local_models"].add(f"{tgt[len('thaumadds:'):]}  (blockstate {name})")

            for rot in ("x", "y", "uvlock", "weight"):
                if rot in merged:
                    ref[rot] = merged[rot]
            out_entries.append(ref)

        out_variants[out_key] = out_entries[0] if len(out_entries) == 1 else out_entries

    write_json(SHARED / "blockstates" / f"{name}.json", {"variants": out_variants}, dry_run)
    report["blockstates"] += 1

    if generated_block_model is not None:
        write_json(SHARED / "models/block" / f"{name}.json", generated_block_model, dry_run)
        report["block_models_generated"].append(name)
        available_block_models.add(self_ref)

    existing_item_model = (legacy / "models/item" / f"{name}.json").exists()
    if existing_item_model or name in SKIP_ITEM_MODEL:
        return None
    return (name, item_model_doc)  # explicit doc, or None for a block-parent stub


# ------------------------------------------------------------------- textures
def convert_textures(legacy: Path, dry_run: bool):
    src = legacy / "textures"
    dst = SHARED / "textures"
    for path in sorted(src.rglob("*")):
        if path.is_dir():
            continue
        rel = path.relative_to(src)
        parts = rel.parts
        if parts[0] == "blocks":
            out = dst / "block" / Path(*parts[1:])
        elif parts[0] == "items":
            out = dst / "item" / Path(*parts[1:])
        elif parts[0] == "armor":
            stem = Path(*parts[1:]).stem
            suffix = Path(*parts[1:]).suffix
            if stem == "_":
                continue  # blank sentinel
            if stem.endswith("_0"):
                out = dst / "models/armor" / (stem[:-2] + "_layer_1" + suffix)
            elif stem.endswith("_1"):
                out = dst / "models/armor" / (stem[:-2] + "_layer_2" + suffix)
            else:
                out = dst / "models/armor" / Path(*parts[1:])
        else:
            out = dst / rel
        copy_file(path, out, dry_run)
        report["textures"] += 1


# --------------------------------------------------------------- special cases
def emit_special_cases(legacy: Path, dry_run: bool):
    # --- void_thaumometer: OBJ item; the loader id differs per node -------------
    obj = legacy / "models/item/void_thaumometer.obj"
    mtl = legacy / "models/item/void_thaumometer.mtl"
    if obj.exists():
        copy_file(obj, SHARED / "models/item/void_thaumometer.obj", dry_run)
    if mtl.exists():
        copy_file(mtl, SHARED / "models/item/void_thaumometer.mtl", dry_run)

    bs = legacy / "blockstates/void_thaumometer.json"
    display = {}
    if bs.exists():
        bdoc = json.loads(bs.read_text(encoding="utf-8"))
        inv = variant_entries(bdoc.get("variants", {}).get("inventory", [{}]))
        display = convert_display((inv[0] or {}).get("transform", {}))

    def obj_doc(loader):
        doc = {
            "loader": loader,
            "model": "thaumadds:models/item/void_thaumometer.obj",
            "flip_v": True,
            "textures": {
                "#body": "thaumadds:item/void_thaumometer",
                # legacy pointed at thaumcraft:items/scanscreen; TC4R ships it under models/
                "#pane": "thaumcraft:models/scanscreen",
            },
        }
        if display:
            doc["display"] = display
        return doc

    write_json(V_FORGE / "models/item/void_thaumometer.json", obj_doc("forge:obj"), dry_run)
    write_json(V_NEO / "models/item/void_thaumometer.json", obj_doc("neoforge:obj"), dry_run)
    report["notes"].append("void_thaumometer: OBJ item model emitted per node (forge:obj / neoforge:obj)")

    # --- crystal_water: fluid, no block model on either loader ------------------
    report["notes"].append("crystal_water: fluid blockstate skipped (fluids do not use block models on 1.20/1.21)")


# ----------------------------------------------------------------- validation
def tc4r_names():
    if not TC4R_JAR.exists():
        return set()
    with zipfile.ZipFile(TC4R_JAR) as z:
        return set(z.namelist())


def validate(legacy: Path):
    tc = tc4r_names()
    models_root = SHARED / "models"
    tex_root = SHARED / "textures"

    def check_tex(value: str, where: str):
        if not isinstance(value, str) or value.startswith("#"):
            return
        ns, path = split_ref(remap_ns(value))
        if ns == "thaumadds":
            if not (tex_root / (path + ".png")).exists():
                report["missing_local_textures"].add(f"{path}  ({where})")
        elif ns == "thaumcraft" and tc:
            if f"assets/thaumcraft/textures/{path}.png" not in tc:
                report["missing_thaumcraft_textures"].add(f"{path}  ({where})")

    def check_model(value: str, where: str):
        if not isinstance(value, str) or value.startswith("#"):
            return
        ns, path = split_ref(remap_ns(value))
        if ns == "thaumadds":
            if not (models_root / (path + ".json")).exists():
                report["missing_local_models"].add(f"{path}  ({where})")
        elif ns == "thaumcraft" and tc:
            if f"assets/thaumcraft/models/{path}.json" not in tc:
                report["missing_thaumcraft_models"].add(f"{path}  ({where})")

    for root in (models_root, V_FORGE / "models", V_NEO / "models"):
        if not root.is_dir():
            continue
        for path in sorted(root.rglob("*.json")):
            doc = json.loads(path.read_text(encoding="utf-8"))
            rel = path.relative_to(models_root).as_posix() if root == models_root else path.name
            for v in doc.get("textures", {}).values():
                check_tex(v, f"model {rel}")
            if "parent" in doc:
                check_model(doc["parent"], f"model {rel}")
            for ov in doc.get("overrides", []):
                if "model" in ov:
                    check_model(ov["model"], f"model {rel} override")

    for path in sorted((SHARED / "blockstates").rglob("*.json")):
        doc = json.loads(path.read_text(encoding="utf-8"))
        for entries in doc.get("variants", {}).values():
            for e in variant_entries(entries):
                if "model" in e:
                    check_model(e["model"], f"blockstate {path.name}")
                for v in e.get("textures", {}).values():
                    check_tex(v, f"blockstate {path.name}")


# ------------------------------------------------------------------------ main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", default=str(DEFAULT_LEGACY))
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    legacy = Path(args.legacy)
    if not legacy.is_dir():
        print(f"legacy asset dir not found: {legacy}", file=sys.stderr)
        return 2

    convert_textures(legacy, args.dry_run)

    for path in sorted((legacy / "models/block").rglob("*.json")):
        convert_block_model(path, legacy, args.dry_run)

    # Every block model that will exist after this run (copied + to-be-generated).
    available_block_models = {
        "thaumadds:block/" + str(p.relative_to(legacy / "models/block").with_suffix("")).replace("\\", "/")
        for p in (legacy / "models/block").rglob("*.json")
    }

    generated_item_models: set[str] = set()
    for path in sorted((legacy / "models/item").rglob("*.json")):
        convert_item_model(path, legacy, args.dry_run)

    pending_item_models = []
    for path in sorted((legacy / "blockstates").rglob("*.json")):
        plan = convert_blockstate(path, legacy, args.dry_run, available_block_models)
        if plan is not None:
            pending_item_models.append(plan)

    # Second pass: item models can only be resolved once every block model is known.
    for name, doc in pending_item_models:
        self_ref = f"thaumadds:block/{name}"
        if doc is None and self_ref not in available_block_models:
            report["missing_local_models"].add(f"block/{name}  (item model for {name})")
            continue
        write_json(SHARED / "models/item" / f"{name}.json", doc or {"parent": self_ref}, args.dry_run)
        generated_item_models.add(name)
        report["item_models"] += 1

    emit_special_cases(legacy, args.dry_run)

    if not args.dry_run:
        validate(legacy)

    print("=" * 72)
    print("legacy -> modern asset normalization")
    print("=" * 72)
    print(f"blockstates written      : {report['blockstates']}")
    print(f"block models copied      : {report['block_models_copied']}")
    print(f"block models generated   : {len(report['block_models_generated'])}  {sorted(report['block_models_generated'])}")
    print(f"item models written      : {report['item_models']} (incl. {len(generated_item_models)} generated)")
    print(f"textures copied          : {report['textures']}")
    for key, label in (
        ("missing_local_textures", "MISSING local textures"),
        ("missing_local_models", "MISSING local models"),
        ("missing_thaumcraft_textures", "MISSING thaumcraft textures"),
        ("missing_thaumcraft_models", "MISSING thaumcraft models"),
    ):
        vals = sorted(report[key])
        print(f"\n{label} ({len(vals)}):")
        for v in vals:
            print(f"  - {v}")
    print("\nnotes:")
    for n in report["notes"]:
        print(f"  - {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
