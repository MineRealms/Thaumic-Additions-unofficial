# Thaumic Additions: Reconstructed — Port (1.20.1 Forge / 1.21.1 NeoForge)

`dragon-forze/ThaumicAdditions` (1.12.2, TC6 addon) ported onto **Thaumcraft 4: Reconstructed
(TC4R) 0.1.0-20740**, multi-version via Stonecutter.

## Nodes

| Node | Loader | MC | Notes |
|---|---|---|---|
| `versions/1.20.1-forge` | LegacyForge (MDG) | 1.20.1 | TC4R release jar is SRG; MDG `modImplementation` remaps it |
| `versions/1.21.1-neoforge` | NeoForge (MDG) | 1.21.1 | TC4R release jar is already official-mapped |

Build both: `./gradlew buildAndCollect` — the two release jars land in `build/distributions/`.
Per-node build: `./gradlew :1.20.1-forge:build` / `:1.21.1-neoforge:build` (compiles + JUnit).

## Layout

- `src/main/java` — shared mod code, compiled **raw** on both nodes
- `src/hclite/java` — shared **HCLite** core (the reimplemented HammerCore essentials)
- `versions/<node>/src/main/java` — loader adapters (mod entries, client events, KubeJS plugin)
- `versions/<node>/src/hclite/java` — HCLite loader bindings (`@Mod` entry per loader)
- `local-repo/` — TC4R 20740 artifacts (not committed); see its README + `DEPENDENCIES.json`
- `tools/` — migration/validation scripts (legacy asset + lang converters, one-shot patch scripts)

## Version drift — the adapter rule

There is **no Stonecutter replacement table and no `//?` conditionals in shared code**. The shared
source is compiled raw on the active node, so any API that drifted between 1.20.1 and 1.21.1 (or
Forge and NeoForge) must live behind one of two loader-neutral interfaces with a per-node impl:

- `dev.tc4port.thaumadditions.platform.TaPlatform` — tool tiers, armor materials, the drifted
  `Item` hooks (via `ItemBehavior`), food / crop / puriflower factories, effect & ignite helpers.
- `dev.tc4port.hclite.platform.HCLitePlatform` — block-entity NBT save/load, packet NBT, and item
  custom NBT. Item NBT is `ItemStack.getTag()`/`getOrCreateTag()` on 1.20.1 and the
  `DataComponents.CUSTOM_DATA` / `CustomData` component on 1.21 (`itemTag` / `updateItemTag`).

Block-entity state persists through `BlockEntity.getPersistentData()` (the `NeoForgeData` tag both
loaders serialize): 1.21 added a `HolderLookup.Provider` parameter to `saveAdditional` /
`loadAdditional`, so no shared override can satisfy both signatures.

## HCLite (jar-in-jar companion)

The 1.12 mod depended on HammerCore/HammerLib (no modern version exists). The needed parts are
reimplemented in-repo as a separate mod (`hclite`), built by the `hcliteJar` task and embedded
into both release jars via the Forge/NeoForge jar-in-jar format (`META-INF/jarjar/`). In the dev
runtime both source sets load as mods through MDG's `mods {}` block.

## Progress

| Batch | Content | State |
|---|---|---|
| S0 | multiversion skeleton (Stonecutter + MDG), TC4R 20740 deps, Rosetta bridge in dev runs | done |
| S1 | HCLite core (synced block entity, packet channel, device block) + first content | done |
| S2 b1–b4 | 14 material items, 5 storage/decor blocks, void-elemental tools + adaminite / mithminite / blue-wolf armor | done |
| assets | legacy 1.12 assets normalized to modern resource packs in bulk | done |
| S3 items | all remaining 1.12 items (aspect items, belts, tomes, entity cell, thaumometer, wormhole mirror, shadow beam staff, essentia pistol) | done |
| S3 blocks | all remaining blocks + 16 block entity types (jars, smelters, crystal machines, seal, cake, crops) | compile-clean, runtime pending |
| S3 verify | Rosetta runtime probe sweep on both nodes (registration, models/lang, block place + BE, item behaviour) | pending |

Both nodes currently build clean: `:1.20.1-forge:build` + `:1.21.1-neoforge:build` pass with JUnit.

## Configuration

- GeckoLib drives the entity animations (replaces HammerAnimations); never bundled.
- KubeJS plugin skeleton on both nodes (K6 class / K7 interface) replaces the 1.12 CraftTweaker
  integration; script bindings come in the compat phase.
- ColoredLux / Visuals / VFX are reimplemented natively — no external deps.

Out of scope until the compat phase: dev-kit addon interop (FM / Nodal Mechanics / Tainted Magic /
Thaumic Tinkerer / Warp Theory), Thaumic Horizons, Thaumic Augmentation.
