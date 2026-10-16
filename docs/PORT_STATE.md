# Where the port stands

Thaumic Additions 1.12.2 (12.7.9) → 1.20.1 Forge / 1.21.1 NeoForge, via Stonecutter with shared
sources. This file is the running state: what is done, what is not, and what is known to be
impossible. Written to be read cold.

## How to work on it

- **Build**: `./gradlew :1.20.1-forge:build :1.21.1-neoforge:build --offline`. The `--offline` is
  not optional on this machine right now: Gradle cannot reach `maven.neoforged.net` (TLS handshake
  fails), and the local cache is complete, so offline builds work and online ones do not.
- **Test**: the dev client exposes a Rosetta bridge on port **48793**. Kill any running client
  first — a stale one answers the bridge and makes every probe look like it passed. Then
  `python tools/bridge_exec.py 48793 tools/<probe>.java`.
- **The bridge's compiler does not carry the hclite source set**, so anything on
  `SyncedBlockEntity` (every block entity) has to be reached reflectively; the tile's class cannot
  even be named as a type in a probe.
- **Registry key sets return `ResourceLocation` directly** in these mappings, not `ResourceKey`.

## Done

| Area | Notes |
|---|---|
| Content | 43 blocks, ~100 items, 4 entities, 8 menus, all registered |
| Block entities | 25 upstream machines; all bar the two below have full 1.12 behaviour |
| Models, textures, names | complete; names verified against the asset list |
| Animated textures | 37 `.mcmeta` files, all present |
| Loot tables | 40 of 43 (the three without have reasons; see `tools/gen_loot.py`) |
| Machine GUIs | 5 machines + Chester + crystal bag + pistol |
| JEI | flux concentrator page and aspect-aware subtypes, both nodes |
| Seals | all 7 behaviours, including the portal's network, binding and chunk-forcing |
| Entities | essentia shot, blue wolf, Chester, thrown scythe |
| Client renderers | 11 tile renderers, 2 entity renderers, cake tint, jar liquid, void thaumometer |
| Recipes | 50 (27 arcane, 23 infusion) — see the substitution caveat below |
| Research | 64 entries with 62 page texts, generated from upstream's tree |
| Mining tags | 41 of 43 blocks (cake and the unbreakable structure block have none on purpose) |

## Not done, in the order worth doing

1. **The per-aspect recipe loops** (48 vis seeds, 48 seal symbols) are **deliberately not ported**.
   Their results carry an aspect, and TC4R's recipe format can only patch an item through
   `input_patch`, which writes modern components — while the port's aspect items keep theirs in
   legacy item NBT. The port's items assign themselves a random aspect on creation instead
   (`AspectItems.onCreated`), so a per-aspect recipe would have nothing to set.
2. **The seal's world rendering.** 1.12 drew the seal's frame and the portal's hole in the world
   (`TESRSeal`, `TARSealRenders`, `PortalRenderer`); the port has only the globe screen. The
   textures it needs (`misc/portal*.png`, `misc/seal_0/1/2.png`) are all present.
5. **Fluid `crystal_water`.** Upstream has flow/still textures and `.mcmeta`, and a crucible recipe
   produces it; the port has no fluid.
6. **Smaller client pieces**: the crop's baked model, the jar's in-hand model, the
   `HudHandlerHookTAR` overlay, the charger's thunder/vis-sparkle effects.

## Known impossible, with reasons

| Thing | Why |
|---|---|
| Aura charger adding to the *chunk* | TC4R has no chunk aura; vis lives in aura nodes. Implemented as charging the nearest node through `NodeApi.replaceLoadedState`, which is verified working live. |
| Crystal bore drinking vis | It calls `VisNetworkApi.drain` correctly; a hand-placed node does not serve it and no naturally generated node was found to test against. Unresolved rather than impossible. |
| Essentia pistol loading from a jar | 1.12 read a jar *item*'s contents; TC4R's jars are blocks whose contents live in the block entity. The port loads from crystallised essence instead. |
| Blue wolf suit rendering | Its 11 OBJ meshes are **not in the source tree and not in the released 1.12 jar** — upstream referenced meshes it never shipped, so the suit was invisible in 1.12 too. The textures are 256-square OBJ unwraps, so vanilla's 64×32 armour layout cannot stand in. The suit is skipped. |

## The recipe economy is substituted, not ported

72 recipes upstream; **57 use components TC4R does not have** — plates and mechanisms were
Thaumcraft 4's crafting backbone and simply do not exist here, and eight of the aspect costs were
the mod's own Knowledge aspects. The substitution table, with a reason per row, is at the top of
`tools/gen_recipes.py`, and both generators import it. Every id the 50 shipped recipes name was
checked against both mods' item lists and TC4R's registry: none unresolved.

## Loader drift already behind the platform

Kept here because each one cost time to find:

- `Enchantment` vs `Holder<Enchantment>`; the compatibility check moved from an instance method to
  a static `Enchantment.areCompatible`.
- `Block#use` (1.20.1) vs `Block#useItemOn` (1.21) — so the blocks override neither and each node
  hooks `PlayerInteractEvent.RightClickBlock` instead.
- Opening a menu with data: `NetworkHooks.openScreen` vs `ServerPlayer#openMenu`, and 1.21 hands
  the writer a `RegistryFriendlyByteBuf`.
- `MenuType` construction; `RegisterMenuScreensEvent` exists only on NeoForge.
- `Entity#defineSynchedData` takes a builder on 1.21 — per-node subclasses for both projectiles.
- `ContainerHelper` save/load grew a registry parameter on 1.21.
- `dropCustomDeathLoot` renamed; Chester spills from `die` instead.
- `VertexConsumer`: `vertex/color/uv/uv2/normal` + `endVertex` vs
  `addVertex/setColor/setUv/setLight/setNormal` with no `endVertex`. This is why the OBJ renderer
  is per node while the OBJ parser is shared.
- JEI is 15 on 1.20.1 and 19 on 1.21.1, and the plugins live per node; the recipe data does not.
- `modRuntimeOnly`, not `runtimeOnly`, for a Forge mod in the dev runtime — a plain `runtimeOnly`
  leaves the jar SRG-named and it dies at load on a `NoSuchMethodError`.
- `BonemealableBlock#isValidBonemealTarget` lost an argument on 1.21; ripeness is asked of the
  block state instead.

## Traps found by running it, which reading had missed

Worth remembering for the next port of this kind:

- A machine's slots were written to NBT and marked dirty but never **pushed**; `setChanged` is not
  `sync`. Every machine looked empty on the client.
- Cached fields were filled in the constructor and never re-read on reload, so a synced machine
  reported stale state.
- `EssentiaApi.extract` searches for `EssentiaSource`, which TC4R keeps separate from
  `EssentiaTransport`; jars are transports, so nothing drained.
- A declared `LayerDefinition` texture size must be the size the *model* was authored for, not the
  size of the PNG. The textures are 2× upscales; declaring the file's size samples the wrong texel.
- `ItemRenderer` only takes the `BlockEntityWithoutLevelRenderer` path when the item model is
  `builtin/entity`; a model that inherits an empty block model renders nothing.
- Three of seven seals never resolved because the manager demanded every symbol slot be filled,
  while three of the combinations leave slots wild on purpose.
- No entity had its attributes registered, so every spawn threw on the first health check.
- Thirty-eight of forty-three blocks had no loot table, so mining them produced air.
