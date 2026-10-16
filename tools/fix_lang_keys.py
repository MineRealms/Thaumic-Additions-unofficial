# Makes the language files and the code agree on what a key is called.
#
# Two spellings ended up in the same file. The 1.12 language file spelled keys
# gui.thaumadds:knowledge_tome.store — a colon, because that is what setTranslationKey produced —
# and the converter brought them across unchanged. The code written since spells its own keys
# gui.thaumadds.crystal_bag.empty, with a dot. So the port showed a raw key wherever the two met:
# right-clicking a block with the void thaumometer printed chat.thaumadds.thaumometer.target.
#
# This renames the colon keys to the spelling the code uses and adds the handful the code asks for
# that were never written at all. DataIntegrityTest checks the result from then on, so a new
# translatable call with no line behind it fails the build instead of the game.
import io
import json
import os
import re

LANG = "src/main/resources/assets/thaumadds/lang/%s.json"

# Keys the code asks for that no language file ever defined, with what they say. The arguments
# each one takes are the ones its call site passes.
ADDED = {
    "en_us": {
        "chat.thaumadds.thaumometer.target": "Scanned %s:",
        "chat.thaumadds.thaumometer.empty": "Nothing here carries an aspect.",
        "tooltip.thaumadds.aspect": "Aspect: %s",
        "tooltip.thaumadds.essentia": "Essentia: %s",
        "tooltip.thaumadds.knowledge_tome.entries": "Holds %s discoveries.",
        "gui.thaumadds.knowledge_tome.store": "Knowledge stored! %s discoveries are yours.",
        "gui.thaumadds.knowledge_tome.restored": "You regain %s discoveries.",
    },
    "zh_cn": {
        "chat.thaumadds.thaumometer.target": "已扫描 %s：",
        "chat.thaumadds.thaumometer.empty": "这里没有任何要素。",
        "tooltip.thaumadds.aspect": "要素：%s",
        "tooltip.thaumadds.essentia": "源质：%s",
        "tooltip.thaumadds.knowledge_tome.entries": "收录了 %s 项发现。",
        "gui.thaumadds.knowledge_tome.store": "知识已存储！你拥有 %s 项发现。",
        "gui.thaumadds.knowledge_tome.restored": "你重新获得了 %s 项发现。",
    },
}

# One key is not a colon-to-dot rename but a typo from the conversion: the mod id twice.
RENAMED = {"effect.thaumadds.thaumadditions:sonus": "effect.thaumadds.sonus"}

# The prefixes the mod owns. A colon inside one of these is the 1.12 spelling of a dot.
PREFIXES = ("block.", "item.", "tooltip.", "gui.", "chat.", "seal.", "research.", "tc.",
            "damage.", "effect.", "status.", "card.", "tile.", "fluid.", "entity.")


def normalized(key):
    if key in RENAMED:
        return RENAMED[key]
    for prefix in PREFIXES:
        if key.startswith(prefix) and ":" in key:
            head, _, tail = key.partition(":")
            return head + "." + tail
    return key


def main():
    for locale, added in ADDED.items():
        path = LANG % locale
        lang = json.load(io.open(path, encoding="utf-8"))
        before = len(lang)

        renamed = 0
        for key in [k for k in lang if normalized(k) != k]:
            value = lang.pop(key)
            target = normalized(key)
            # A key already written in the new spelling wins; the two say the same thing.
            lang.setdefault(target, value)
            renamed += 1
        for key, value in added.items():
            lang[key] = value

        io.open(path, "w", encoding="utf-8", newline="\n").write(
            json.dumps(dict(sorted(lang.items())), indent=2, ensure_ascii=False) + "\n")
        print("%s: %d -> %d keys (%d renamed, %d added)"
              % (path, before, len(lang), renamed, len(added)))


if __name__ == "__main__":
    main()
