# Thaumic Additions

An addon for **Thaumcraft**, for **Minecraft 1.20.1 (Forge)** and **1.21.1 (NeoForge)**.

Thaumic Additions adds a second tier to everything a thaumaturge works with: three metals and the
tools, armour and trinkets made from them, crystals in more shapes than the ones you can grow,
six kinds of jar, a workshop of machines for essentia and vis, and seven runic seals that do your
chores for you.

It began as [`dragon-forze/ThaumicAdditions`](https://github.com/dragon-forze/ThaumicAdditions),
a Thaumcraft 6 addon for 1.12.2. This is the unofficial port of it onto
[Thaumcraft 4: Reconstructed](https://github.com/iPortalTeam/Thaumcraft4Reconstructed), which is
older in setting and newer in code — the two are close enough in what they let a thaumaturge do
that the addon's own machinery came across whole.

---

## What's in it

### Three metals

**Mithrillium**, **Adaminite** and **Mithminite**, each with a nugget, an ingot, a plate and a
block, made from the one below it on an infusion altar. Mithrillium is the whitish one that starts
you off; Adaminite is what you make when mithrillium is not enough; Mithminite is what you make
when adaminite is not enough, and it is what the armour and the scythe are made of.

### Crystals

Crystal blocks hold an aspect and glow with it. Crystal lamps light a room in an aspect's colour.
Crystalised essence is the raw material for both, and a **crystal bag** carries far more of it
than your pockets will.

**Crystal water** is a fluid that makes crystals grow. Pour enough of it around a growing crystal
and it climbs faster, spending the water as it goes.

### Jars

Six kinds, one for each metal plus eldritch and brass, in the sizes Thaumcraft's own warded jars
are not.

### Machines

- **Crystal crusher** — grinds crystallised essence down into salt essence of the same aspect.
- **Aspect combiner** — two aspects in, one compound aspect out.
- **Growth chamber** — feeds a crystal enough essentia and vis to grow more of itself.
- **Crystal bore** — drills for crystals where they are found, and drinks vis doing it.
- **Aura charger** — puts vis back into the aura node nearest it.
- **Aura disperser** — spreads vis around, for when one node is doing all the work.
- **Flux concentrator** — pulls flux out of the air and bottles it.
- **Essentia sink** — a drain that empties jars into whatever you have plumbed to it.
- **Shadow enchanter** — applies enchantments for essentia rather than levels.
- **Entity summoner** — captures a creature in an entity cell and makes more of it, around a
  turning hypercube, with no spawner needed.

### Seals

Seven runes to press into a wall, each with three symbol slots. Fill them and the seal does the
work: waters farmland, tills soil, bonemeals crops, harvests them, harvests and replants, picks
loose items up off the floor. The seventh is a **portal**: bind two seals to one another and
step through.

### Gear

The **void thaumometer** scans what you look at and tells you what aspects it carries. The
**wormhole mirror**, the **shadow beam staff**, the **beheader**, the **essentia pistol**, and
belts that carry you further, higher and faster. Most of it is infusion work.

### The rest

A blue wolf, a Chester who follows you and carries your things, two crops — one grown from vis,
one from the void — a cake that remembers every bite taken from it, and a Tome of Sharing that
hands your knowledge to somebody else.

Everything is written up in the Thaumonomicon under its own category, in English and in Chinese.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.20.1 or 1.21.1 |
| Loader | Forge 47.x (1.20.1) or NeoForge 21.x (1.21.1) |
| Thaumcraft | [Thaumcraft 4: Reconstructed](https://github.com/iPortalTeam/Thaumcraft4Reconstructed) |
| Also needed | Curios and GeckoLib, both of which Thaumcraft itself requires |

## Installing

Drop the jar for your version into your `mods` folder. There is no configuration to write and
nothing to switch on; the mod's own settings, if any are added later, live in
`config/thaumadds-client.json`.

## Building

```
./gradlew buildAndCollect
```

Both jars land in `build/distributions/`. Building one target on its own is
`./gradlew :1.20.1-forge:build` or `./gradlew :1.21.1-neoforge:build`, either of which also runs
the data tests. The sources are shared between the two targets and the loader differences live in
per-target adapters, so most changes are written once.

## Credits

- **dragon-forze** and the DragonForge team, who wrote Thaumic Additions for 1.12.2 — every
  machine, seal, metal and texture here started as theirs.
- The **Thaumcraft 4: Reconstructed** team, for the Thaumcraft this runs on.
- **Azanor**, for Thaumcraft itself.

## License

The original mod's assets and design belong to their authors. This port is distributed under the
same terms as the mod it ports; see the original repository before redistributing anything from it.
