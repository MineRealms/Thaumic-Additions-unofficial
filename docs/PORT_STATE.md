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
| Fluid `crystal_water` | registered on both nodes: type, two fluids, block, bucket, crucible recipe, tag made by hand. The bucket is ours — upstream wrote a recipe that filled one it never registered |
| Portal seal's hole | opens and shuts on 1.12's rule, drawn as 1.12's own six sheets of portal art. The far-side window is not ported; see below |

## Not done, in the order worth doing

1. **The per-aspect recipe loops** (48 vis seeds, 48 seal symbols) are **deliberately not ported**.
   Their results carry an aspect, and TC4R's recipe format can only patch an item through
   `input_patch`, which writes modern components — while the port's aspect items keep theirs in
   legacy item NBT. The port's items assign themselves a random aspect on creation instead
   (`AspectItems.onCreated`), so a per-aspect recipe would have nothing to set.
2. **Smaller client pieces**: the jar's in-hand model, the `HudHandlerHookTAR` overlay, and the
   charger's thunder/vis-sparkle effects.

   The crops are *not* on this list: 1.12 baked them with a custom model baker for texture
   variety, and the port ships per-age blockstate models instead, which is the same crops drawn
   the ordinary way.

## Known impossible, with reasons

| Thing | Why |
|---|---|
| Aura charger adding to the *chunk* | TC4R has no chunk aura; vis lives in aura nodes. Implemented as charging the nearest node through `NodeApi.replaceLoadedState`, which is verified working live. |
| Crystal bore drinking vis | It calls `VisNetworkApi.drain` correctly; a hand-placed node does not serve it and no naturally generated node was found to test against. Unresolved rather than impossible. |
| Essentia pistol loading from a jar | 1.12 read a jar *item*'s contents; TC4R's jars are blocks whose contents live in the block entity. The port loads from crystallised essence instead. |
| Blue wolf suit rendering | Its 11 OBJ meshes are **not in the source tree and not in the released 1.12 jar** — upstream referenced meshes it never shipped, so the suit was invisible in 1.12 too. The textures are 256-square OBJ unwraps, so vanilla's 64×32 armour layout cannot stand in. The suit is skipped. |
| The portal's far-side window | 1.12 rendered the far side of the world into a framebuffer behind a stencil and drew that as the portal's surface, so a portal was a true hole. That is a second pass of the whole level through another camera, plus an `EntitySealViewer` entity to drive it; neither target offers a public API for the first and the port has no such entity. The six-sheet hole that ships **is** 1.12's own fallback for exactly this case — the one its `ConfigsTAR.portalGfx` switch existed to turn off — so a portal here looks the way an upstream portal looked with that switch off. |
| Crystal water growing a world crystal | 1.12's block grew a partly grown `BlockCrystal` beside it and spent water doing it. TC4R's crystal clusters are grown by its essentia crystalliser and expose no size, so there is nothing there to grow. The rule is pointed at the growth this port does have instead: `TileGrowthChamber` reads the eight blocks around it, grows up to five times faster for a full ring of sources, and spends one — half the time — per grow. Upstream's ring and odds, a different beneficiary. |

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

- A shared save/load override cannot exist: 1.21 added a registry provider to `saveAdditional` and
  `loadAdditional`. Anything a block entity wants kept goes in `getPersistentData()`, which both
  loaders serialise without help — which is also why a seal's behaviour state is written there
  rather than through hooks the tile has to call from an override.
- A hand-written vertex has five elements, overlay included, and the entity render types carry an
  overlay beside the light. A vertex that fills only four throws `Not filled all elements of the
  vertex` when the buffer is drawn — not when it is written, so it waits until the thing is
  actually on screen. This is what the void thaumometer and the seal's pictograms were doing; they
  had never been rendered, so nothing had ever said so.
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
- An arcane recipe will not load without its `vis` key, even when the cost is nothing. Two recipes
  whose whole cost had been the aspects this port cannot name came out of the generator with no
  key at all and were silently dropped; the generator now always writes it.
- A research entry hangs off a parent, and a parent that does not exist aborts the **whole**
  research load and names one parent per attempt, so finding them one restart at a time is the slow
  way. `gen_research.py` now checks every parent against the installed TC4R before writing, and it
  is what caught `BASEARTIFICE` pointing at nothing.
- The seal block had a recipe, a loot table, an item model and a lang name, and no item — so it
  could not be made, could not be placed and could not be picked up. Everything around a
  registration is not the registration.
