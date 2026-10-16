# Fills the gaps in the port's Simplified Chinese, and drops the keys nothing reads.
#
# Three sources, in order of authority:
#
#   1. Upstream's own zh_cn.lang, for everything it translated. The seal descriptions are the ones
#      that matter here — the port's seal is a different seal, but the sentences are upstream's.
#   2. Hand-written text for the strings the port invented (its GUIs, its JEI lines, its tooltips).
#      Upstream shipped no equivalent, so there is nothing to carry across.
#   3. Nothing: where upstream left English in its own Chinese file, this says so rather than
#      quietly inventing a translation over the top of upstream's choice.
#
# The research page text is not touched here — gen_research.py writes it, from the same language
# files, for both locales.
import io
import json
import re
import zipfile

UPSTREAM_JAR = r"G:\DOWNLOAD-EDGE\ThaumicAdditions-1.12.2-12.7.9.jar"
LANG = "src/main/resources/assets/thaumadds/lang/%s.json"

# Upstream's key -> the port's key, for the strings that are upstream's own.
FROM_UPSTREAM = {
    "seal.thaumadditions:fertilesoil": "seal.thaumadds.fertilesoil",
    "seal.thaumadditions:createsoil": "seal.thaumadds.createsoil",
    "seal.thaumadditions:bonemealcrops": "seal.thaumadds.bonemealcrops",
    "seal.thaumadditions:harvestcrops1": "seal.thaumadds.harvestcrops1",
    "seal.thaumadditions:harvestcrops2": "seal.thaumadds.harvestcrops2",
    "seal.thaumadditions:pickupitems": "seal.thaumadds.pickupitems",
    "seal.thaumadditions:portala": "seal.thaumadds.portal",
    "seal.thaumadditions:portalb": "seal.thaumadds.portalb",
}

# The strings the port invented. Named after the English the port already ships, in the register
# the rest of the file is written in: 秘金 for mithminite, 源质 for essentia, 纹章 for seals,
# 咒波 for flux.
WRITTEN = {
    "entity.thaumadds.essentia_shot": "源质弹",
    "entity.thaumadds.mithminite_scythe": "秘金镰刀",
    "block.thaumadds.crystal_lamp": "水晶灯",
    "research_name.thaumadds:crystal_lamp": "水晶灯",
    "gui.thaumadds.crystal_bag.empty": "空：走过结晶要素即可装填",
    "gui.thaumadds.pistol.load": "喂给它一份结晶要素",
    "gui.thaumadds.seal_globe": "纹章晶球",
    "gui.thaumadds.seal_globe.empty": "（空）",
    "gui.thaumadds.seal_globe.unknown": "这些符文组不成任何纹章。",
    "jei.thaumadds.flux_concentrator": "咒波集中器",
    "tooltip.thaumadds.chester": "把他放下，他就能为你携带三十六个格子。",
    "tooltip.thaumadds.seal_globe": "右键一个纹章，读出它的符文含义。",
    "tc.research_page.TAR_CRYSTAL_LAMP.1": "我一直在琢磨，能不能用我手头的灵气水晶做出一盏灯来。我应当试试把它们的能量导成光子。"
                                            "也许宏伟之木做外壳正合适。",
    "tc.research_page.TAR_CRYSTAL_LAMP.2": "把萤石裹在宏伟之木里，效果出奇地好。真正的奇迹发生在我把灵气水晶放进灯里的时候——"
                                            "它开始发光了。这项新发明似乎还会对红石起反应，通电时就会自己熄灭。",
}

# Keys nothing reads any more. The 1.12 page text came across under research_stage.*, but the
# modern catalogue asks for a page by the value the entry itself names, which is tc.research_page.*.
DEAD_PREFIXES = ("research_stage.",)


def upstream_chinese():
    with zipfile.ZipFile(UPSTREAM_JAR) as jar:
        text = jar.read("assets/thaumadditions/lang/zh_cn.lang").decode("utf-8")
    out = {}
    for line in text.splitlines():
        key, _, value = line.partition("=")
        if value:
            out[key.strip()] = value.strip()
    return out


def main():
    upstream = upstream_chinese()
    for locale in ("en_us", "zh_cn"):
        path = LANG % locale
        lang = json.load(io.open(path, encoding="utf-8"))
        before = len(lang)

        for key in list(lang):
            if key.startswith(DEAD_PREFIXES):
                del lang[key]
        for source, target in FROM_UPSTREAM.items():
            if locale == "zh_cn" and source in upstream:
                lang[target] = upstream[source]
        if locale == "zh_cn":
            # Only fills what is missing or, for the two upstream left in English, what upstream
            # never translated. Anything already translated stays.
            english = json.load(io.open(LANG % "en_us", encoding="utf-8"))
            for key, value in WRITTEN.items():
                if key not in lang or lang[key] == english.get(key):
                    lang[key] = value

        ordered = dict(sorted(lang.items()))
        io.open(path, "w", encoding="utf-8", newline="\n").write(
            json.dumps(ordered, indent=2, ensure_ascii=False) + "\n")
        print("%s: %d keys -> %d" % (path, before, len(ordered)))

    english = json.load(io.open(LANG % "en_us", encoding="utf-8"))
    chinese = json.load(io.open(LANG % "zh_cn", encoding="utf-8"))
    missing = sorted(k for k in english if k not in chinese)
    print("still missing in zh_cn: %d" % len(missing))
    for key in missing:
        print("  ", key)
    untranslated = sorted(k for k in chinese if chinese[k] == english.get(k) and not k.startswith("tc."))
    print("identical to English (upstream left these too): %d" % len(untranslated))
    for key in untranslated:
        print("  ", key)


if __name__ == "__main__":
    main()
