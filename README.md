# Thaumic Additions: Reconstructed — Port (1.20.1 Forge / 1.21.1 NeoForge)

`dragon-forze/ThaumicAdditions` (1.12.2, TC6 addon) ported onto **Thaumcraft 4: Reconstructed
(TC4R) 0.1.0-20740**, multi-version via Stonecutter.

## Nodes

| Node | Loader | MC | Notes |
|---|---|---|---|
| `versions/1.20.1-forge` | LegacyForge (MDG) | 1.20.1 | TC4R release jar is SRG; MDG `modImplementation` remaps it |
| `versions/1.21.1-neoforge` | NeoForge (MDG) | 1.21.1 | TC4R release jar is already official-mapped |

Build both: `./gradlew buildAndCollect` (jars land in `build/distributions/`).

## Layout

- `src/main/java` — shared mod code (chiseled per node; `//? if` conditions)
- `src/hclite/java` — shared **HCLite** core (the reimplemented HammerCore essentials)
- `versions/<node>/src/main/java` — loader adapters (mod entries, client events, KubeJS plugin)
- `versions/<node>/src/hclite/java` — HCLite loader bindings (`@Mod` entry per loader)
- `local-repo/` — TC4R 20740 artifacts (not committed); see its README + `DEPENDENCIES.json`

## HCLite (jar-in-jar companion)

The 1.12 mod depended on HammerCore/HammerLib (no modern version exists). The needed parts are
reimplemented in-repo as a separate mod (`hclite`), built by the `hcliteJar` task and embedded
into both release jars via the Forge/NeoForge jar-in-jar format (`META-INF/jarjar/`). In the dev
runtime both source sets load as mods through MDG's `mods {}` block.

## Foundations (this phase)

- TC4R 20740 dependency (full internals at compile time, like the 1.12 mod had for TC6)
- GeckoLib for entity animations (replaces HammerAnimations)
- KubeJS plugin skeleton on both nodes (K6 class / K7 interface) — replaces the 1.12
  CraftTweaker integration; script bindings come in the compat phase
- ColoredLux / Visuals are reimplemented natively later (VFX work) — no external deps

Out of scope until compat phase: dev-kit addon interop (FM / Nodal Mechanics / Tainted Magic /
Thaumic Tinkerer / Warp Theory), Thaumic Horizons, Thaumic Augmentation.
