# Remaining work — after S3 (content registration)

S3 landed every item, block and block entity the 1.12 mod has. What is left is everything that
makes them *do* something and *look* like themselves. This is the plan for those phases, derived
from the upstream inventories in [`tools/recon/`](../tools/recon).

## Where the port stands

| Area | Upstream | Ported | Notes |
|---|---|---|---|
| items | 36 | done | registration, behaviour hooks via `ItemBehavior` |
| blocks | 32 | done | registration; 6 blocks still render as missingno (see S7a) |
| block entities | 25 | 4 classes / 581 lines | topology and capacities only, no ticking |
| `inventory/` (menus + GUIs) | 22 | 0 | |
| `net/` | 21 | 0 | only the HCLite channel exists |
| `client/` | 43 | 0 | no renderer of any kind |
| `entity/` | 5 | 0 | Blue Wolf, Chester, essentia shot |
| `seals/` | 8 | 0 | |
| `recipes/` | 8 | partial | |

## Order

The dependency chain is `S4 → S7`, `S4 → S5 → S6`, `S4 → S8/S9`: every renderer, menu and packet
reads block entity state that S4 creates. **S7a** is pulled ahead of S4 because it is what makes
the six currently-invisible blocks usable, and it does not depend on S4.

### S7a — Renderer plumbing and the five machine models
Six blocks render purple/black today.
- Per-node renderer registration behind a `TaClientPlatform` hook: Forge and NeoForge put
  `EntityRenderersEvent.RegisterRenderers` in different packages, so the shared code cannot name it.
- Convert the five 1.12 `ModelBase` models to 1.20.1 `ModelPart` trees (about 45 boxes total:
  aspect_combiner 5, aura_charger 8, crystal_bore 6, crystal_crusher 3, flux_concentrator 23).
- `BlockEntityRenderer` per machine, plus the empty blockstate each one needs so the block has a
  model to hand the renderer.
- Inventory icons through a `BlockEntityWithoutLevelRenderer`, which is how upstream drew the item
  form too (`TESRCrystalBore.renderItem`).

### S4 — Block entity behaviour and ticking
The 16 types are registered with `build(null)`, so nothing ticks.
- Ticker plumbing. `BlockEntityType.Builder.build(ticker)` has the same shape on both loaders.
- Jars: per-variant suction; the adaminite jar's flux explosion and the mithminite jar's overflow
  pollution.
- Smelters: fuel, smelt time, efficiency and speed, output pushed to alembics above.
- Crystal family: bore (drain vis, drill a crystal down a tier, yield essence), crusher (essentia
  to crushes to salt essence), crystal block (lazily assigned aspect), crystal lamp (breathing
  light).
- Growth chamber, aspect combiner, aura charger, aura disperser, flux concentrator, summoner,
  shadow enchanter, arcane cake.
- Carry over the upstream quirks recorded in `tools/recon/tiles.md` — in particular the
  deliberately asymmetric `rotator.update()` calls, the non-persisted rotation angles, and the
  double-sided smelter burn timer.

### S5 — Menus and GUIs
22 upstream classes. Containers with slots, data slots for progress/energy, and the matching
screens. Inventory: `tools/recon/inv.md`.

### S6 — Networking
21 upstream packets, grouped in `tools/recon/net.md`: GUI sync (6), seals (2), entities and
particles (13). Particle packets become client-bound payloads dispatched to the S7 effects.

### S7b — Remaining client work
Jar/cake/sink/seal renderers, the crop's baked model, the block statemap, particles and the
Thaumonomicon background texture.

### S8 — Entities
Blue Wolf and Chester (GeckoLib drives the animations), plus the essentia shot projectile and the
mithminite scythe's renderer.

### S9 — Seals
Seal combinations, instances, the seal renderer set and the `TileSeal` tick.

### Out of scope
Dev-kit addon interop (Forbidden Magic / Nodal Mechanics / Tainted Magic / Thaumic Tinkerer /
Warp Theory), Thaumic Horizons, Thaumic Augmentation.
