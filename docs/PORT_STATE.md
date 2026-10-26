# Where the port stands

Thaumic Additions 1.12.2 (12.7.9) → 1.20.1 Forge / 1.21.1 NeoForge, via Stonecutter with shared
sources. This file is the running state: what is done, what is not, and what is known to be
impossible. Written to be read cold.

## How to work on it

- **Build**: `./gradlew :1.20.1-forge:build :1.21.1-neoforge:build --offline`. The `--offline` is
  not optional on this machine right now: Gradle cannot reach `maven.neoforged.net` (TLS handshake
  fails), and the local cache is complete, so offline builds work and online ones do not.
  `maven.blamejared.com` and `modmaven.dev` **are** reachable, so a dependency that is not in the
  cache yet can be fetched by dropping `--offline` for one build.
- **Tests**: `./gradlew test` runs `DataIntegrityTest`, which checks the mod's own JSON against
  itself — both languages carry the same keys, every research page has text, every arcane recipe
  has its `vis`, every blockstate and item model resolves, every loot table drops an item we ship a
  model for. It needs no game and catches the faults that are otherwise a restart each.
- **Live audit**: `python tools/bridge_exec.py 48793 tools/probe_audit.java` walks every registered
  item, block, recipe, loot table and research entry and prints `AUDIT PASS` or the list of things
  that resolve to nothing. This is the one that catches an id nothing registered.
- **Test**: the dev client exposes a Rosetta bridge on port **48793** (48794 for the 1.21.1 client).
  Kill any running client first — a stale one answers the bridge and makes every probe look like it
  passed. Then `python tools/bridge_exec.py 48793 tools/<probe>.java`.
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
| Recipes | 94 (27 arcane, 41 infusion, 23 crafting, 3 crucible) — see the substitution caveat below |
| Research | 64 entries with 62 page texts, generated from upstream's tree |
| Mining tags | 41 of 43 blocks (cake and the unbreakable structure block have none on purpose) |
| Fluid `crystal_water` | registered on both nodes: type, two fluids, block, bucket, crucible recipe, tag made by hand. The bucket is ours — upstream wrote a recipe that filled one it never registered |
| Portal seal's hole | opens and shuts on 1.12's rule, drawn as 1.12's own six sheets of portal art — **and** a window onto the far side, rendered a second time each frame and switched off by `config/thaumadds-client.json` |
| Text | both languages carry the same 381 lines, and every one of the 129 research pages has its text in each. `tools/fill_zh.py` fills the gaps from upstream's own `zh_cn.lang`; `tools/gen_research.py` writes the page text per locale |
| JEI | the flux concentrator's category and catalysts, the aspect item subtypes, and the crystal block's — upstream's four, no more |
| Tests | `DataIntegrityTest` (no game needed) and `tools/probe_audit.java` (live, registry-side) |

## Not done, in the order worth doing

1. **The per-aspect recipe loops** (48 vis seeds, 48 seal symbols) are **deliberately not ported**.
   Their results carry an aspect, and TC4R's recipe format can only patch an item through
   `input_patch`, which writes modern components — while the port's aspect items keep theirs in
   legacy item NBT. The port's items assign themselves a random aspect on creation instead
   (`AspectItems.onCreated`), so a per-aspect recipe would have nothing to set.
2. **The rest of upstream's effects.** Its client package is the full list, and this is what of it
   is not here. Nothing below changes what anything *does*; they are all things you see.

   | Upstream | What it is | State |
   |---|---|---|
   | `util/tesseract/Tesseract`, `Plane` | the hypercube in the entity summoner | ported |
   | `runnable/ThunderRunnable` | lightning arcs, HammerCore's `spawnSimpleThunder` | missing |
   | `runnable/VisSparkleRunnable` | a vis sparkle flying from one point to another | missing |
   | `fx/FXShadowBeam` | the shadow beam staff's beam | missing |
   | `fx/FXTextureAtlasSpriteDigging` | item-crack particles, the crystal crusher's | missing |
   | `fx/FXColoredDrop` | a coloured droplet travelling | missing |
   | `fx/ParticleFX` | HammerCore's general-purpose coloured particle | partly: `TaFx` dusts |
   | `texture/TextureThaumonomiconBG` | the book's animated background | missing; the book uses Thaumcraft's |
   | `HudHandlerHookTAR` | the thaumometer's chunk-vis bar | impossible, see above |
   | `seal/PortalRenderer` | the portal's framebuffer window | impossible, see above |

   The four that carry a machine's own action — the charger's thunder and sparkles, the crusher's
   item cracks, the staff's beam, the coloured droplets the septic machines throw — are all
   expressible with modern particles and are the ones worth doing next.

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
| The portal's far-side window | **Ported.** 1.12 rendered the far side into a framebuffer behind a stencil; the port enters the level's render pass a second time from the frame's stage events, with a camera mirrored through the portal plane, and puts back the dispatchers, projection, culling frustum and viewport afterwards. Two things are not upstream's: no stencil, because the portal is a rectangle and a textured quad over it needs none; and the buffer is 512 square rather than the whole screen. Behind `TaClientConfig.PORTAL_WINDOW`, and it disables itself for the session on any failure, which is what 1.12 did too. |
| Crystal water growing a world crystal | 1.12's block grew a partly grown `BlockCrystal` beside it and spent water doing it. TC4R's crystal clusters are grown by its essentia crystalliser and expose no size, so there is nothing there to grow. The rule is pointed at the growth this port does have instead: `TileGrowthChamber` reads the eight blocks around it, grows up to five times faster for a full ring of sources, and spends one — half the time — per grow. Upstream's ring and odds, a different beneficiary. |
| Thaumcraft's own JEI plugin | The installed TC4R ships a JEI plugin written against JEI 9's `ISubtypeInterpreter`, which JEI 15 renamed. It throws once, JEI logs it and carries on, and the mod's own plugin is unaffected — but Thaumcraft's aspect subtypes do not reach JEI. It is a fault in a binary dependency, not in this port, and cannot be patched from here. |
| A KubeJS plugin with anything in it | Upstream had no CraftTweaker integration to translate: the 1.12 jar carries no `crafttweaker` or `minetweaker` class of any kind. The plugin class the port ships is what makes KubeJS load without complaint, and adding script bindings would be inventing an API upstream never had. |
| Three of upstream's four integrations | Upstream's `compat` package is Thaumcraft, JEI, `visuals`, `lux` and `thaumicaugmentation`. The last three are 1.12-only mods by the same author — a chest renderer, a dynamic lighting mod, and a Thaumcraft expansion — and none of them has a build for either target. Nothing in this workspace or in the Gradle cache carries any of them. The two that exist here, Thaumcraft and JEI, are both ported. |
| The thaumometer's HUD bar | Upstream's `HudHandlerHookTAR` drew the chunk's vis and flux down the side of the screen while a thaumometer was held. It reads `AuraChunk.getVis` and `getFlux`, and there is no chunk aura to read — the same reason the aura charger charges nodes. A bar showing the nearest node exists, but it would be this port's own idea rather than upstream's. |

## The recipes are read from upstream's source, not transcribed

The first pass wrote fifty-one of them by hand from 1.12's `RecipesTAR.java`, and a hand pass over
seventy-odd recipes is a pass with holes in it: the whole `crafting()` method — the nugget, ingot
and block chains for the three metals, the plates, the tainted planks, the lamp, the lectern —
and nineteen of the infusion recipes were simply never written down. Nothing said so, because a
recipe that was never transcribed looks exactly like a recipe that is merely hard to find.

`tools/parse_upstream_recipes.py` reads the source now, and `gen_infusion.py` and `gen_crafting.py`
turn what it read into the port's spelling. **Fourteen** of the hundred and one registered items
still have no recipe, and each one is one of:

- the five per-aspect loops (`crystal_block`, `salt_essence`, `vis_pod`, `seal_symbol`,
  `vis_seeds`), which cannot be expressed — see below;
- four pieces of the blue wolf suit, which upstream had no recipe for either;
- `taintkin`, `taintkin_lit`, `void_fruit` and `zeith_fur`, which are grown or dropped, not made.

`gen_infusion.py` checks every `thaumcraft:` id it writes against the installed Thaumcraft and
refuses to write a recipe naming something that is not there. Ten recipes were being dropped at
load by exactly that mistake — `elemental_pick` for `elemental_pickaxe`, `bellows` for
`arcane_bellows` — and the game reports each one as a recipe that simply does not exist.

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
- `BucketItem` keeps the fluid's `Supplier` on 1.20.1 and takes the `Fluid` itself on 1.21.1, so
  the bucket comes from `TaPlatform#bucketItem`. It matters because the item registry can be
  filled before the fluid one: a bucket that resolves the fluid at registration time either comes
  out empty or, on the loader that refuses an unbound holder, never comes out at all.
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
  registration is not the registration. `tools/probe_audit.java` now asks the registry.
- KubeJS refuses to load against a JEI older than it names, and a mod that refuses to load takes
  the **whole client** with it at mod-loading, before anything of this mod has run. The 1.21.1 node
  pinned JEI 19.21 and KubeJS wanted 19.25; the version in `versions/1.21.1-neoforge/gradle.properties`
  is now the one that satisfies it.
- The integrated server's resource manager holds `data/` and the client's holds `assets/`. Asking
  one for the other's files reports every model in the mod as missing — which is what the first run
  of the live audit did.
- An aspect's name is a translation. Thaumcraft keeps one line per aspect under
  `tc.aspect.<id>`, and printing the registry path instead put `ignis` — lower case, untranslated —
  in every tooltip and in the name of anything carrying one.
- A research entry can have several stages of text, and upstream wrote `@ASPECT` into its language
  files where an aspect belongs, to be swapped out when the stack is asked for its name. Neither
  came across on its own: the port emitted stage one only, and printed the marker.
- An entity type registered without a client renderer does not fail where it is registered. It
  fails the first time one of those entities is in view, as a `NullPointerException` inside
  `EntityRenderDispatcher#shouldRender`, and the crash report names no mod at all. Both of this
  port's projectiles had been left that way: the scythe, and the pistol's shot — which drew nothing
  in 1.12 either, its trail of aspect dust being the whole visual, so it takes vanilla's
  `NoopRenderer` while the scythe gets `ScytheRenderer`.
