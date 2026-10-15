#!/usr/bin/env python3
"""Bulk-convert the legacy 1.12 `.lang` files and sound resources to modern form.

  * `lang/<locale>.lang`  -> `lang/<locale>.json`
      - `item.<ns>:<name>.name`  -> `item.thaumadds.<name>`
      - `tile.<ns>:<name>.name`  -> `block.thaumadds.<name>`
      - `entity.<ns>.<name>.name`-> `entity.thaumadds.<name>`
      - `fluid.<ns>:<name>`      -> `fluid.thaumadds.<name>`
      - `potion.<name>`          -> `effect.thaumadds.<name>`
      - `itemGroup.<ns>`         -> `itemGroup.thaumadds`
      - everything else keeps its shape, with `thaumadditions` -> `thaumadds`
    Existing keys in the port file win (they are hand-verified).
  * `sounds/*.ogg` and `sounds.json` are copied with the namespace remapped.

Run:  python tools/legacy_text.py [--legacy <dir>] [--locales en_us zh_cn]
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEGACY_ROOT = ROOT.parent / "_ta-analysis/src/main/resources/assets/thaumadditions"
SHARED = ROOT / "src/main/resources/assets/thaumadds"

OLD_NS = "thaumadditions"
NEW_NS = "thaumadds"


def convert_key(key: str) -> str:
    """1.12 lang key -> modern translation key."""
    m = re.fullmatch(r"(item|tile)\.thaumadditions:([^.\s]+)\.name", key)
    if m:
        kind, name = m.groups()
        prefix = "item" if kind == "item" else "block"
        return f"{prefix}.{NEW_NS}.{name}"
    m = re.fullmatch(r"entity\.thaumadditions\.([^.\s]+)\.name", key)
    if m:
        return f"entity.{NEW_NS}.{m.group(1)}"
    m = re.fullmatch(r"fluid\.thaumadditions:([^.\s]+)", key)
    if m:
        return f"fluid.{NEW_NS}.{m.group(1)}"
    m = re.fullmatch(r"potion\.thaumadditions:([^.\s]+)", key)
    if m:
        return f"effect.{NEW_NS}.{m.group(1)}"
    m = re.fullmatch(r"potion\.([^.\s]+)", key)
    if m:
        return f"effect.{NEW_NS}.{m.group(1)}"
    if key == "itemGroup.thaumadditions":
        return "itemGroup.thaumadds"
    # Everything else keeps its shape; only the namespace token is renamed.
    return key.replace(OLD_NS, NEW_NS)


def read_lang(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        out[convert_key(key.strip())] = value
    return out


def convert_lang(locale: str) -> tuple[int, int]:
    src = LEGACY_ROOT / f"lang/{locale}.lang"
    dst = SHARED / f"lang/{locale}.json"
    existing: dict[str, str] = {}
    if dst.exists():
        existing = json.loads(dst.read_text(encoding="utf-8"))
    legacy = read_lang(src)
    added = 0
    for key, value in legacy.items():
        if key not in existing:
            existing[key] = value
            added += 1
    dst.write_text(json.dumps(existing, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
                   encoding="utf-8")
    return added, len(existing)


def convert_sounds() -> int:
    count = 0
    sounds_dir = LEGACY_ROOT / "sounds"
    dst_dir = SHARED / "sounds"
    if sounds_dir.is_dir():
        for f in sorted(sounds_dir.rglob("*")):
            if f.is_file():
                out = dst_dir / f.relative_to(sounds_dir)
                out.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, out)
                count += 1
    src_json = LEGACY_ROOT / "sounds.json"
    if src_json.exists():
        doc = json.loads(src_json.read_text(encoding="utf-8"))
        text = json.dumps(doc, indent=2).replace(OLD_NS, NEW_NS)
        (SHARED / "sounds.json").write_text(text + "\n", encoding="utf-8")
    return count


def main() -> int:
    global LEGACY_ROOT
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", default=str(LEGACY_ROOT))
    ap.add_argument("--locales", nargs="*", default=["en_us", "zh_cn"])
    args = ap.parse_args()
    LEGACY_ROOT = Path(args.legacy)

    for locale in args.locales:
        added, total = convert_lang(locale)
        print(f"{locale}: +{added} keys -> {total} total")
    n = convert_sounds()
    print(f"sounds: copied {n} files + sounds.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
